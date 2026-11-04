package com.mahmoud.clipkid.core.domain.parent

import com.mahmoud.clipkid.core.domain.testing.FakeSettingsRepository
import com.mahmoud.clipkid.core.domain.testing.FixedClock
import com.mahmoud.clipkid.core.domain.testing.kuwait
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class UnlockWithPinUseCaseTest {

    private val clock = FixedClock(kuwait(2026, 11, 4, 20, 0))
    private val settings = FakeSettingsRepository(pin = "2468")
    private val unlock = UnlockWithPinUseCase(settings, clock)

    private suspend fun failTimes(n: Int) = repeat(n) { unlock("0000") }

    @Test
    fun rightPinUnlocks() = runTest {
        assertEquals(PinCheck.Unlocked, unlock("2468"))
    }

    @Test
    fun wrongPinsCountDownTheAttemptsLeft() = runTest {
        assertEquals(PinCheck.Wrong(attemptsLeft = 4), unlock("1111"))
        assertEquals(PinCheck.Wrong(attemptsLeft = 3), unlock("1111"))
        assertEquals(PinCheck.Wrong(attemptsLeft = 2), unlock("1111"))
        assertEquals(PinCheck.Wrong(attemptsLeft = 1), unlock("1111"))
    }

    @Test
    fun fifthWrongPinLocksForThirtySeconds() = runTest {
        failTimes(4)
        val result = unlock("1111")
        assertEquals(PinCheck.LockedOut(clock.instant + 30.seconds), result)
    }

    @Test
    fun duringTheLockoutEvenTheRightPinIsRefusedAndNotChecked() = runTest {
        failTimes(5)
        val checksBefore = settings.verifyCalls
        clock.instant += 29.seconds

        assertIs<PinCheck.LockedOut>(unlock("2468"))
        assertEquals(checksBefore, settings.verifyCalls)
    }

    @Test
    fun afterTheLockoutTheRightPinWorksAndTheCounterStartsAgain() = runTest {
        failTimes(5)
        clock.instant += 30.seconds

        assertEquals(PinCheck.Wrong(attemptsLeft = 4), unlock("1111"))
        assertEquals(PinCheck.Unlocked, unlock("2468"))
        assertEquals(PinLockout.None, settings.lockout.value)
    }

    @Test
    fun rightPinResetsTheCounter() = runTest {
        failTimes(4)
        assertEquals(PinCheck.Unlocked, unlock("2468"))
        assertEquals(PinCheck.Wrong(attemptsLeft = 4), unlock("1111"))
    }

    @Test
    fun lockoutIsKeptAcrossAppRestarts() = runTest {
        failTimes(5)
        // A new use case (e.g. after the app was killed) reads the saved lockout.
        val afterRestart = UnlockWithPinUseCase(settings, clock)

        assertIs<PinCheck.LockedOut>(afterRestart("2468"))
        assertEquals(clock.instant + 30.seconds, afterRestart.currentLockout())
    }

    @Test
    fun lockoutEndingFarInTheFutureIsIgnored_whenTheClockWasMovedBack() = runTest {
        failTimes(5)
        clock.instant -= 10.minutes

        assertNull(unlock.currentLockout())
        assertEquals(PinCheck.Unlocked, unlock("2468"))
    }

    @Test
    fun resetPinSetsTheNewPinAndClearsTheLockout() = runTest {
        failTimes(5)
        ResetPinUseCase(settings)("1357")

        assertEquals(PinLockout.None, settings.lockout.value)
        assertEquals(PinCheck.Unlocked, unlock("1357"))
        assertEquals(PinCheck.Wrong(attemptsLeft = 4), unlock("2468"))
    }

    @Test
    fun resetPinNeedsFourDigits() = runTest {
        val reset = ResetPinUseCase(settings)
        assertFailsWith<IllegalArgumentException> { reset("123") }
        assertFailsWith<IllegalArgumentException> { reset("12a4") }
    }
}
