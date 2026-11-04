package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.repository.CatalogRepository
import com.mahmoud.clipkid.core.domain.repository.IdGenerator
import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.SettingsRepository
import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.core.model.ChildProfile
import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.DailyLimit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** What the parent fills in for a child. A nickname only: no other personal data. */
data class NewChild(
    val nickname: String,
    val avatarColor: Long,
    val ageBand: AgeBand,
    val allowedCollectionIds: Set<String>,
    val dailyLimit: DailyLimit,
) {
    val isValid: Boolean get() = isValidNickname(nickname)
}

const val NICKNAME_MAX_LENGTH: Int = 20

fun isValidNickname(nickname: String): Boolean = nickname.trim().length in 1..NICKNAME_MAX_LENGTH

/** The daily-limit choices offered in onboarding and on the dashboard. */
object DailyLimitOptions {
    val videos: List<Int> = listOf(3, 5, 10)
    val minutes: List<Int> = listOf(15, 30, 45, 60)
    val defaultLimit: DailyLimit = DailyLimit.Videos(3)
    const val DEFAULT_MINUTES: Int = 30
}

/** Collections offered for an age band, in sortOrder. Premium ones are included (shown locked). */
class GetCollectionsForAgeUseCase(private val catalog: CatalogRepository) {
    operator fun invoke(ageBand: AgeBand): Flow<List<Collection>> =
        catalog.observeCollections().map { all -> forAge(all, ageBand) }

    companion object {
        fun forAge(all: List<Collection>, ageBand: AgeBand): List<Collection> =
            all.filter { ageBand in it.ageBands }.sortedBy { it.sortOrder }

        /** Onboarding pre-selects every collection of the age band except premium ones. */
        fun defaultSelection(collections: List<Collection>): Set<String> =
            collections.filterNot { it.isPremium }.map { it.id }.toSet()
    }
}

/** Adds a child profile and makes it the active child (the one child mode shows). Returns its id. */
class AddChildUseCase(
    private val profiles: ProfileRepository,
    private val settings: SettingsRepository,
    private val ids: IdGenerator,
    private val catalog: CatalogRepository,
) {
    suspend operator fun invoke(child: NewChild): String {
        require(child.isValid) { "nickname must be 1..$NICKNAME_MAX_LENGTH characters" }
        val premium = catalog.observeCollections().first().filter { it.isPremium }.map { it.id }.toSet()
        val profile = ChildProfile(
            id = ids.newId(),
            nickname = child.nickname.trim(),
            avatarColor = child.avatarColor,
            ageBand = child.ageBand,
            // Premium collections need a subscription (billing phase); never enabled here.
            allowedCollectionIds = child.allowedCollectionIds - premium,
            dailyLimit = child.dailyLimit,
        )
        profiles.upsert(profile)
        settings.setActiveChildId(profile.id)
        return profile.id
    }
}

/**
 * The last onboarding step. Everything is saved here at once (PIN, child, active child,
 * "onboarding done"), so leaving onboarding halfway saves nothing. Returns the child id.
 */
class CompleteOnboardingUseCase(
    private val settings: SettingsRepository,
    private val addChild: AddChildUseCase,
) {
    suspend operator fun invoke(pin: String, child: NewChild): String {
        settings.setPin(pin)
        val id = addChild(child)
        settings.setOnboardingDone(true)
        return id
    }
}

/**
 * Dashboard changes to one child. Child mode observes the profile, so Home updates at once.
 * Premium collections cannot be turned on until billing exists.
 */
class UpdateChildSettingsUseCase(
    private val profiles: ProfileRepository,
    private val catalog: CatalogRepository,
) {
    suspend fun setDailyLimit(childId: String, limit: DailyLimit) {
        val child = profiles.getProfile(childId) ?: return
        if (child.dailyLimit != limit) profiles.upsert(child.copy(dailyLimit = limit))
    }

    /** Returns false when the change was refused (premium collection, or unknown child). */
    suspend fun setCollectionAllowed(childId: String, collectionId: String, allowed: Boolean): Boolean {
        val child = profiles.getProfile(childId) ?: return false
        if (allowed) {
            val collection = catalog.observeCollections().first().firstOrNull { it.id == collectionId }
            if (collection == null || collection.isPremium) return false
        }
        val ids = if (allowed) child.allowedCollectionIds + collectionId else child.allowedCollectionIds - collectionId
        if (ids != child.allowedCollectionIds) profiles.upsert(child.copy(allowedCollectionIds = ids))
        return true
    }
}
