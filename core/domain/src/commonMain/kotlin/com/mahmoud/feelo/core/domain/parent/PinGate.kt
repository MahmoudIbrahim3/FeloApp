package com.mahmoud.feelo.core.domain.parent

import com.mahmoud.feelo.core.domain.repository.SettingsRepository
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Wrong-PIN state, kept in settings so closing the app does not skip a lockout.
 * [lockedUntil] is set when the last allowed attempt fails; the counter restarts from 0 then.
 */
data class PinLockout(
    val failedAttempts: Int = 0,
    val lockedUntil: Instant? = null,
) {
    companion object {
        val None = PinLockout()
    }
}

sealed interface PinCheck {
    data object Unlocked : PinCheck

    /** Wrong PIN; [attemptsLeft] more tries before a lockout. */
    data class Wrong(val attemptsLeft: Int) : PinCheck

    /** Too many wrong tries: no PIN is checked until [until]. */
    data class LockedOut(val until: Instant) : PinCheck
}

/**
 * Checks the parent PIN with a lockout: after [maxAttempts] wrong PINs in a row, every try is
 * refused (even the right PIN) for [lockoutDuration]. A right PIN resets the counter.
 */
class UnlockWithPinUseCase(
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val maxAttempts: Int = MAX_ATTEMPTS,
    private val lockoutDuration: Duration = LOCKOUT,
) {
    suspend operator fun invoke(pin: String): PinCheck {
        val now = clock.now()
        activeLockout(settings.getPinLockout(), now)?.let { return PinCheck.LockedOut(it) }

        if (settings.verifyPin(pin)) {
            settings.setPinLockout(PinLockout.None)
            return PinCheck.Unlocked
        }
        val failed = settings.getPinLockout().failedAttempts + 1
        return if (failed >= maxAttempts) {
            val until = now + lockoutDuration
            settings.setPinLockout(PinLockout(failedAttempts = 0, lockedUntil = until))
            PinCheck.LockedOut(until)
        } else {
            settings.setPinLockout(PinLockout(failedAttempts = failed, lockedUntil = null))
            PinCheck.Wrong(attemptsLeft = maxAttempts - failed)
        }
    }

    /** The end of a lockout still running now, or null. For the gate to show the timer on open. */
    suspend fun currentLockout(): Instant? = activeLockout(settings.getPinLockout(), clock.now())

    /**
     * A lockout only counts while it ends within [lockoutDuration] from now. If the device clock
     * was moved back, a stored end far in the future would otherwise lock the parent out for long.
     */
    private fun activeLockout(state: PinLockout, now: Instant): Instant? {
        val until = state.lockedUntil ?: return null
        return until.takeIf { now < it && it - now <= lockoutDuration }
    }

    companion object {
        const val MAX_ATTEMPTS = 5
        val LOCKOUT: Duration = 30.seconds
    }
}

/** Sets a new PIN after the adult check, and clears any lockout. */
class ResetPinUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(newPin: String) {
        require(isValidPin(newPin)) { "PIN must be $PIN_LENGTH digits" }
        settings.setPin(newPin)
        settings.setPinLockout(PinLockout.None)
    }
}

const val PIN_LENGTH: Int = 4

fun isValidPin(pin: String): Boolean = pin.length == PIN_LENGTH && pin.all { it in '0'..'9' }
