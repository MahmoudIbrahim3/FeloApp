package com.mahmoud.clipkid.core.domain.repository

import com.mahmoud.clipkid.core.domain.parent.PinLockout
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
}
