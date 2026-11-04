package com.mahmoud.clipkid.navigation

import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.SettingsRepository
import com.mahmoud.clipkid.core.domain.usecase.RefreshCatalogIfStaleUseCase
import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.core.model.ChildProfile
import com.mahmoud.clipkid.core.model.DailyLimit
import kotlinx.coroutines.flow.first
import org.koin.core.Koin

/** Until onboarding exists: one demo child so Home has something to show. Replaced by onboarding. */
private val DEBUG_CHILD = ChildProfile(
    id = "debug-child",
    nickname = "يوسف",
    avatarColor = 0xFFFFB627,
    ageBand = AgeBand.A4_6,
    allowedCollectionIds = setOf("letters", "numbers", "quran-duas", "manners", "science", "stories"),
    dailyLimit = DailyLimit.Videos(3),
)

/**
 * Runs once at app start, before the first screen: refreshes the catalog when it is stale
 * (offline is fine, the cache keeps working), makes sure a child exists, and returns the active child.
 */
suspend fun prepareChildApp(koin: Koin): String {
    koin.get<RefreshCatalogIfStaleUseCase>().invoke()

    val profiles = koin.get<ProfileRepository>()
    val settings = koin.get<SettingsRepository>()

    val active = settings.activeChildId.first()?.takeIf { profiles.getProfile(it) != null }
    val childId = active
        ?: profiles.observeProfiles().first().firstOrNull()?.id
        ?: DEBUG_CHILD.also { profiles.upsert(it) }.id
    if (active != childId) settings.setActiveChildId(childId)
    return childId
}
