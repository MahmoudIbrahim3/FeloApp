package com.mahmoud.clipkid.core.domain.testing

import com.mahmoud.clipkid.core.domain.parent.PinLockout
import com.mahmoud.clipkid.core.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory settings. Keeps the PIN in plain text: tests only (the real one stores a hash). */
class FakeSettingsRepository(
    pin: String? = null,
    onboardingDone: Boolean = false,
    activeChildId: String? = null,
    consentGiven: Boolean = false,
) : SettingsRepository {
    val pin = MutableStateFlow(pin)
    val lockout = MutableStateFlow(PinLockout.None)
    private val done = MutableStateFlow(onboardingDone)
    private val active = MutableStateFlow(activeChildId)
    val consent = MutableStateFlow(consentGiven)
    var verifyCalls: Int = 0
        private set

    override val hasPin: Flow<Boolean> = this.pin.map { it != null }

    override suspend fun setPin(pin: String) {
        this.pin.value = pin
    }

    override suspend fun verifyPin(pin: String): Boolean {
        verifyCalls++
        return this.pin.value == pin
    }

    override suspend fun getPinLockout(): PinLockout = lockout.value

    override suspend fun setPinLockout(lockout: PinLockout) {
        this.lockout.value = lockout
    }

    override val onboardingDone: Flow<Boolean> = done

    override suspend fun setOnboardingDone(done: Boolean) {
        this.done.value = done
    }

    override val activeChildId: Flow<String?> = active

    override suspend fun setActiveChildId(id: String?) {
        active.value = id
    }

    override val consentGiven: Flow<Boolean> = consent

    override suspend fun setConsentGiven(given: Boolean) {
        consent.value = given
    }
}
