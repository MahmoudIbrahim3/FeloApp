package com.mahmoud.clipkid.feature.child.done

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahmoud.clipkid.core.domain.usecase.GetDailyQuotaUseCase
import com.mahmoud.clipkid.core.model.DailyQuota
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface DoneTodayUiState {
    data object Loading : DoneTodayUiState
    data class Ready(val quota: DailyQuota) : DoneTodayUiState
}

sealed interface DoneTodayEvent {
    data object HomeClicked : DoneTodayEvent

    /** "عايز تكمّل؟ اسأل بابا أو ماما": opens the parental gate. */
    data object AskParentClicked : DoneTodayEvent
}

sealed interface DoneTodayEffect {
    data object Home : DoneTodayEffect
    data object OpenGate : DoneTodayEffect
}

class DoneTodayViewModel(
    private val childId: String,
    getQuota: GetDailyQuotaUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<DoneTodayUiState>(DoneTodayUiState.Loading)
    val state: StateFlow<DoneTodayUiState> = _state.asStateFlow()

    private val effectChannel = Channel<DoneTodayEffect>(Channel.BUFFERED)
    val effects: Flow<DoneTodayEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            getQuota(childId).collect { _state.value = DoneTodayUiState.Ready(it) }
        }
    }

    fun onEvent(event: DoneTodayEvent) {
        val effect = when (event) {
            DoneTodayEvent.HomeClicked -> DoneTodayEffect.Home
            DoneTodayEvent.AskParentClicked -> DoneTodayEffect.OpenGate
        }
        effectChannel.trySend(effect)
    }
}
