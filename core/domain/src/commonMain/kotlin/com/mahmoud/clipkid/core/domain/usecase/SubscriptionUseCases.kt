package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.repository.BillingRepository
import com.mahmoud.clipkid.core.domain.repository.CatalogRepository
import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.observeIsFull
import com.mahmoud.clipkid.core.model.ChildProfile
import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.SubscriptionLimits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * Free vs full for this device.
 *
 * - Full: every child profile is usable, up to [SubscriptionLimits.MAX_FULL_CHILDREN].
 * - Free: only the first profile (the oldest) is usable. Extra profiles made while subscribed are
 *   kept with their settings and history, and come back when the subscription does.
 */
data class Access(
    val isFull: Boolean,
    /** Profile ids, oldest first. */
    val childIds: List<String>,
) {
    val unlockedChildIds: Set<String> =
        (if (isFull) childIds else childIds.take(SubscriptionLimits.MAX_FREE_CHILDREN)).toSet()

    val maxChildren: Int get() = SubscriptionLimits.maxChildren(isFull)

    val canAddChild: Boolean get() = childIds.size < maxChildren

    /** Free and at the free limit: "add child" leads to the paywall instead. */
    val addChildNeedsSubscription: Boolean get() = !isFull && !canAddChild

    /** Full and at the full limit: "add child" is hidden. */
    val atFullLimit: Boolean get() = isFull && !canAddChild

    fun isChildLocked(childId: String): Boolean = childId !in unlockedChildIds

    /** The child that child mode falls back to when the active one is locked. */
    val firstChildId: String? get() = childIds.firstOrNull()
}

class ObserveAccessUseCase(
    private val billing: BillingRepository,
    private val profiles: ProfileRepository,
) {
    operator fun invoke(): Flow<Access> =
        combine(billing.observeIsFull(), profiles.observeProfiles()) { isFull, children ->
            Access(isFull, children.map { it.id })
        }

    suspend fun current(): Access = invoke().first()
}

/**
 * Right after a purchase or restore turns the subscription on: switches on, for every child, the
 * premium collections of that child's age band. The parent can turn any of them off afterwards.
 * Never called on expiry: settings are never rewritten, content just locks again.
 */
class EnablePremiumCollectionsUseCase(
    private val profiles: ProfileRepository,
    private val catalog: CatalogRepository,
) {
    suspend operator fun invoke() {
        val premium = catalog.observeCollections().first().filter { it.isPremium }
        if (premium.isEmpty()) return
        profiles.observeProfiles().first().forEach { child ->
            val updated = withPremiumFor(child, premium)
            if (updated != child) profiles.upsert(updated)
        }
    }

    companion object {
        internal fun withPremiumFor(child: ChildProfile, premium: List<Collection>): ChildProfile {
            val ids = premium.filter { child.ageBand in it.ageBands }.map { it.id }
            val allowed = child.allowedCollectionIds + ids
            return if (allowed == child.allowedCollectionIds) child else child.copy(allowedCollectionIds = allowed)
        }
    }
}
