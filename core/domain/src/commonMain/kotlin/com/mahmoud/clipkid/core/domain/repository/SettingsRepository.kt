package com.mahmoud.clipkid.core.domain.repository

import com.mahmoud.clipkid.core.domain.parent.PinLockout
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow

/** Device-level settings. The PIN is only ever stored hashed, in secure storage. */
interface SettingsRepository {
    val hasPin: Flow<Boolean>

    suspend fun setPin(pin: String)

    suspend fun verifyPin(pin: String): Boolean

    /** Wrong-PIN counter and lockout end. Kept across restarts so closing the app cannot skip a lockout. */
    suspend fun getPinLockout(): PinLockout

    suspend fun setPinLockout(lockout: PinLockout)

    val onboardingDone: Flow<Boolean>

    suspend fun setOnboardingDone(done: Boolean)

    val activeChildId: Flow<String?>

    suspend fun setActiveChildId(id: String?)

    /**
     * The parent agreed (onboarding, or the paywall for older installs) that videos play through
     * YouTube's player and subscriptions go through Apple/Google and RevenueCat. Until then the app
     * never talks to RevenueCat (Apple Kids: no device data to third parties without consent).
     */
    val consentGiven: Flow<Boolean>

    suspend fun setConsentGiven(given: Boolean)

    /** When the thumbnail disk cache was last cleared (see PruneImageCacheUseCase), or null if never. */
    suspend fun getImageCacheClearedAt(): Instant?

    suspend fun setImageCacheClearedAt(at: Instant)
}
