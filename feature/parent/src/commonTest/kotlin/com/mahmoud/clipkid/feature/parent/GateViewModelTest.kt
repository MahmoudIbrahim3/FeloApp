package com.mahmoud.clipkid.feature.parent

import app.cash.turbine.test
import com.mahmoud.clipkid.core.domain.parent.CreateAdultChallengeUseCase
import com.mahmoud.clipkid.core.domain.parent.ResetPinUseCase
import com.mahmoud.clipkid.core.domain.parent.UnlockWithPinUseCase
import com.mahmoud.clipkid.core.domain.testing.FakeSettingsRepository
import com.mahmoud.clipkid.core.domain.testing.TestClock
import com.mahmoud.clipkid.core.domain.testing.kuwait
import com.mahmoud.clipkid.core.domain.testing.runViewModelTest
import com.mahmoud.clipkid.feature.parent.gate.GateEffect
import com.mahmoud.clipkid.feature.parent.gate.GateError
import com.mahmoud.clipkid.feature.parent.gate.GateEvent
import com.mahmoud.clipkid.feature.parent.gate.GateStage
import com.mahmoud.clipkid.feature.parent.gate.GateViewModel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class GateViewModelTest {

    private val settings = FakeSettingsRepository(pin = "2468")

    private fun TestScope.gate(): GateViewModel {
        val clock = TestClock(testScheduler, kuwait(2026, 11, 4, 20, 0))
        return GateViewModel(
            UnlockWithPinUseCase(settings, clock),
            ResetPinUseCase(settings),
            CreateAdultChallengeUseCase(Random(7)),
            settings,
            clock,
        ).also { runCurrent() }
    }

    private fun GateViewModel.enter(pin: String) {
        pin.forEach { onEvent(GateEvent.Digit(it - '0')) }
        onEvent(GateEvent.Submit)
    }

    @Test
    fun rightPinPasses() = runViewModelTest {
        val vm = gate()
        vm.effects.test {
            vm.enter("2468")
            assertEquals(GateEffect.Passed, awaitItem())
        }
    }

    @Test
    fun wrongPinShowsTheAttemptsLeft() = runViewModelTest {
        val vm = gate()
        vm.enter("1111")
        runCurrent()
        assertEquals(GateError.WrongPin, vm.state.value.error)
        assertEquals(4, vm.state.value.attemptsLeft)
        assertEquals("", vm.state.value.entered)
    }

    @Test
    fun fifthWrongPinShowsAThirtySecondCountdownAndBlocksTheKeypad() = runViewModelTest {
        val vm = gate()
        repeat(5) { vm.enter("1111"); runCurrent() }

        assertEquals(30, vm.state.value.lockoutSecondsLeft)
        vm.onEvent(GateEvent.Digit(2))
        assertEquals("", vm.state.value.entered, "digits are ignored while locked")

        advanceTimeBy(10.seconds)
        runCurrent()
        assertEquals(20, vm.state.value.lockoutSecondsLeft)

        advanceTimeBy(20.seconds)
        runCurrent()
        assertEquals(0, vm.state.value.lockoutSecondsLeft)
        vm.effects.test {
            vm.enter("2468")
            assertEquals(GateEffect.Passed, awaitItem())
        }
    }

    @Test
    fun reopeningTheGateDuringALockoutShowsTheRemainingTime() = runViewModelTest {
        repeat(5) { gate().enter("1111"); runCurrent() }
        advanceTimeBy(12.seconds)

        val reopened = gate()
        assertEquals(18, reopened.state.value.lockoutSecondsLeft)
    }

    @Test
    fun forgotPinNeedsTheRightProductThenANewPinTwice() = runViewModelTest {
        val vm = gate()
        vm.onEvent(GateEvent.ForgotPinClicked)
        assertIs<GateStage.AdultCheck>(vm.state.value.stage)

        vm.onEvent(GateEvent.AnswerChanged("1"))
        vm.onEvent(GateEvent.AnswerSubmit)
        assertEquals(GateError.WrongAnswer, vm.state.value.error)
        val second = assertIs<GateStage.AdultCheck>(vm.state.value.stage)
        assertTrue(second.answer.isEmpty(), "a wrong answer gives a fresh question")

        vm.onEvent(GateEvent.AnswerChanged(second.challenge.answer.toString()))
        vm.onEvent(GateEvent.AnswerSubmit)
        assertIs<GateStage.NewPin>(vm.state.value.stage)

        vm.enter("1357")
        vm.enter("9999")
        assertEquals(GateError.NewPinMismatch, vm.state.value.error)

        vm.effects.test {
            vm.enter("1357")
            vm.enter("1357")
            assertEquals(GateEffect.Passed, awaitItem())
        }
        assertTrue(settings.verifyPin("1357"))
    }

    @Test
    fun withoutAPinTheGateStartsAtTheAdultCheck() = runViewModelTest {
        settings.pin.value = null
        assertIs<GateStage.AdultCheck>(gate().state.value.stage)
    }

    @Test
    fun closeLeavesWithoutPassing() = runViewModelTest {
        val vm = gate()
        vm.effects.test {
            vm.onEvent(GateEvent.CloseClicked)
            assertEquals(GateEffect.Closed, awaitItem())
        }
    }
}
