package com.mahmoud.clipkid.core.domain.repository

import kotlinx.coroutines.flow.Flow

/** Device-level settings. The PIN is only ever stored hashed, in secure storage. */
interface SettingsRepository {
    val hasPin: Flow<Boolean>

    suspend fun setPin(pin: String)

    suspend fun verifyPin(pin: String): Boolean

    val onboardingDone: Flow<Boolean>

    suspend fun setOnboardingDone(done: Boolean)

    val activeChildId: Flow<String?>

    suspend fun setActiveChildId(id: String?)
}
