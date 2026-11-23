package com.mahmoud.feelo.feature.parent.gate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahmoud.feelo.core.domain.parent.AdultChallenge
import com.mahmoud.feelo.core.domain.parent.CreateAdultChallengeUseCase
import com.mahmoud.feelo.core.domain.parent.PIN_LENGTH
import com.mahmoud.feelo.core.domain.parent.PinCheck
import com.mahmoud.feelo.core.domain.parent.ResetPinUseCase
import com.mahmoud.feelo.core.domain.parent.UnlockWithPinUseCase
import com.mahmoud.feelo.core.domain.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/** Where the gate is: PIN entry, the "forgot PIN" adult check, or setting a new PIN. */
sealed interface GateStage {
    data object Pin : GateStage

    data class AdultCheck(val challenge: AdultChallenge, val answer: String = "") : GateStage

    /** New PIN after the adult check; [first] is set while confirming it. */
    data class NewPin(val first: String? = null) : GateStage
}

enum class GateError { WrongPin, WrongAnswer, NewPinMismatch }

data class GateUiState(
    val stage: GateStage = GateStage.Pin,
    val entered: String = "",
    val error: GateError? = null,
    /** Tries left after a wrong PIN, for the error text. */
    val attemptsLeft: Int? = null,
    /** Seconds left in a lockout; 0 when not locked. Counts down every second. */
    val lockoutSecondsLeft: Int = 0,
    val busy: Boolean = false,
) {
    val isLockedOut: Boolean get() = lockoutSecondsLeft > 0
    val canSubmitPin: Boolean get() = entered.length == PIN_LENGTH && !isLockedOut && !busy
}

sealed interface GateEvent {
    data class Digit(val digit: Int) : GateEvent
    data object Backspace : GateEvent

    /** "دخول" (or "التالي" / "حفظ" on the new-PIN stage). */
    data object Submit : GateEvent
    data object ForgotPinClicked : GateEvent
    data class AnswerChanged(val value: String) : GateEvent
    data object AnswerSubmit : GateEvent
    data object CloseClicked : GateEvent
}

sealed interface GateEffect {
    /** The parent proved it is them: the caller may continue. */
    data object Passed : GateEffect

    /** Closed without passing: back to the caller, nothing unlocked. */
    data object Closed : GateEffect
}

/**
 * The parental gate (Gate.png). PIN entry with the 5-tries / 30 s lockout from
 * [UnlockWithPinUseCase]; "نسيت الرقم السري؟" → a typed multiplication → a new PIN entered twice.
 * When no PIN exists (e.g. restored backup without the secure key), it starts at the adult check.
 */
class GateViewModel(
    private val unlockWithPin: UnlockWithPinUseCase,
    private val resetPin: ResetPinUseCase,
    private val createChallenge: CreateAdultChallengeUseCase,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(GateUiState())
    val state: StateFlow<GateUiState> = _state.asStateFlow()

    private val effectChannel = Channel<GateEffect>(Channel.BUFFERED)
    val effects: Flow<GateEffect> = effectChannel.receiveAsFlow()

    private var countdown: Job? = null

    init {
        viewModelScope.launch {
            if (!settings.hasPin.first()) {
                _state.update { it.copy(stage = GateStage.AdultCheck(createChallenge())) }
            } else {
                unlockWithPin.currentLockout()?.let(::startCountdown)
            }
        }
    }

    fun onEvent(event: GateEvent) {
        when (event) {
            is GateEvent.Digit -> _state.update {
                if (it.entered.length >= PIN_LENGTH || it.isLockedOut) it else it.copy(entered = it.entered + event.digit, error = null)
            }
            GateEvent.Backspace -> _state.update { it.copy(entered = it.entered.dropLast(1)) }
            GateEvent.Submit -> when (val stage = _state.value.stage) {
                GateStage.Pin -> submitPin()
                is GateStage.NewPin -> submitNewPin(stage)
                is GateStage.AdultCheck -> Unit
            }
            GateEvent.ForgotPinClicked -> _state.update {
                it.copy(stage = GateStage.AdultCheck(createChallenge()), entered = "", error = null, attemptsLeft = null)
            }
            is GateEvent.AnswerChanged -> _state.update { s ->
                val stage = s.stage as? GateStage.AdultCheck ?: return@update s
                s.copy(stage = stage.copy(answer = event.value.take(ANSWER_MAX_LENGTH)), error = null)
            }
            GateEvent.AnswerSubmit -> submitAnswer()
            GateEvent.CloseClicked -> effectChannel.trySend(GateEffect.Closed)
        }
    }

    private fun submitPin() {
        val s = _state.value
        if (!s.canSubmitPin) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            when (val result = unlockWithPin(s.entered)) {
                PinCheck.Unlocked -> {
                    _state.update { it.copy(busy = false, entered = "") }
                    effectChannel.send(GateEffect.Passed)
                }
                is PinCheck.Wrong -> _state.update {
                    it.copy(busy = false, entered = "", error = GateError.WrongPin, attemptsLeft = result.attemptsLeft)
                }
                is PinCheck.LockedOut -> {
                    _state.update { it.copy(busy = false, entered = "", error = null, attemptsLeft = null) }
                    startCountdown(result.until)
                }
            }
        }
    }

    private fun submitAnswer() {
        val s = _state.value
        val stage = s.stage as? GateStage.AdultCheck ?: return
        _state.update {
            if (stage.challenge.isCorrect(stage.answer)) {
                it.copy(stage = GateStage.NewPin(), entered = "", error = null)
            } else {
                // A new question each time, so guessing one answer does not help.
                it.copy(stage = GateStage.AdultCheck(createChallenge()), error = GateError.WrongAnswer)
            }
        }
    }

    private fun submitNewPin(stage: GateStage.NewPin) {
        val s = _state.value
        if (s.entered.length != PIN_LENGTH || s.busy) return
        val first = stage.first
        when {
            first == null -> _state.update { it.copy(stage = GateStage.NewPin(first = it.entered), entered = "", error = null) }
            first != s.entered -> _state.update { it.copy(stage = GateStage.NewPin(), entered = "", error = GateError.NewPinMismatch) }
            else -> {
                _state.update { it.copy(busy = true) }
                viewModelScope.launch {
                    resetPin(first)
                    _state.update { it.copy(busy = false, entered = "", stage = GateStage.Pin) }
                    countdown?.cancel()
                    _state.update { it.copy(lockoutSecondsLeft = 0) }
                    effectChannel.send(GateEffect.Passed)
                }
            }
        }
    }

    private fun startCountdown(until: Instant) {
        countdown?.cancel()
        countdown = viewModelScope.launch {
            while (true) {
                val left = ceil((until - clock.now()).inWholeMilliseconds / 1000.0).toInt().coerceAtLeast(0)
                _state.update { it.copy(lockoutSecondsLeft = left) }
                if (left == 0) break
                delay(1.seconds)
            }
        }
    }

    companion object {
        private const val ANSWER_MAX_LENGTH = 4
    }
}
