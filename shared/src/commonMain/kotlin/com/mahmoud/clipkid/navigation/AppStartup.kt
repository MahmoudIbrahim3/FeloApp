package com.mahmoud.clipkid.navigation

import com.mahmoud.clipkid.core.domain.repository.CatalogRepository
import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.SettingsRepository
import com.mahmoud.clipkid.core.domain.usecase.SyncCatalogUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.Koin
import kotlin.time.Duration.Companion.seconds

/** Where the app opens. */
sealed interface AppStart {
    /** Fresh install (or onboarding never finished): onboarding. */
    data object Onboarding : AppStart

    /** Every later launch: child Home of the active child. */
    data class Child(val childId: String) : AppStart
}

/** Outlives any screen: a catalog sync started at launch finishes even if the UI is recreated. */
private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** How long the very first launch (nothing cached yet) waits for the catalog before opening. */
private val FIRST_SYNC_WAIT = 15.seconds

/**
 * Runs once at app start, before the first screen: syncs the catalog (only changed rows; offline
 * is fine, the cache keeps working) and decides where the app opens.
 *
 * With a cached catalog the sync runs in the background and screens update when it lands. With
 * an empty cache (first launch) the app waits for it, at most [FIRST_SYNC_WAIT]; the sync keeps
 * going after that and the lists fill in when it finishes.
 */
suspend fun prepareApp(koin: Koin): AppStart {
    val sync = koin.get<SyncCatalogUseCase>()
    val job = appScope.launch { sync() }
    if (koin.get<CatalogRepository>().observeCollections().first().isEmpty()) {
        withTimeoutOrNull(FIRST_SYNC_WAIT) { job.join() }
    }

    val settings = koin.get<SettingsRepository>()
    if (!settings.onboardingDone.first()) return AppStart.Onboarding

    val profiles = koin.get<ProfileRepository>()
    val active = settings.activeChildId.first()?.takeIf { profiles.getProfile(it) != null }
    // Onboarding done but no child left (should not happen): set one up again.
    val childId = active ?: profiles.observeProfiles().first().firstOrNull()?.id ?: return AppStart.Onboarding
    if (active != childId) settings.setActiveChildId(childId)
    return AppStart.Child(childId)
}
