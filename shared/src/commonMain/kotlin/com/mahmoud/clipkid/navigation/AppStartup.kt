package com.mahmoud.clipkid.navigation

import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.SettingsRepository
import com.mahmoud.clipkid.core.domain.usecase.RefreshCatalogIfStaleUseCase
import kotlinx.coroutines.flow.first
import org.koin.core.Koin

/** Where the app opens. */
sealed interface AppStart {
    /** Fresh install (or onboarding never finished): onboarding. */
    data object Onboarding : AppStart

    /** Every later launch: child Home of the active child. */
    data class Child(val childId: String) : AppStart
}

/**
 * Runs once at app start, before the first screen: refreshes the catalog when it is stale
 * (offline is fine, the cache keeps working) and decides where the app opens.
 */
suspend fun prepareApp(koin: Koin): AppStart {
    koin.get<RefreshCatalogIfStaleUseCase>().invoke()

    val settings = koin.get<SettingsRepository>()
    if (!settings.onboardingDone.first()) return AppStart.Onboarding

    val profiles = koin.get<ProfileRepository>()
    val active = settings.activeChildId.first()?.takeIf { profiles.getProfile(it) != null }
    // Onboarding done but no child left (should not happen): set one up again.
    val childId = active ?: profiles.observeProfiles().first().firstOrNull()?.id ?: return AppStart.Onboarding
    if (active != childId) settings.setActiveChildId(childId)
    return AppStart.Child(childId)
}
