package com.mahmoud.feelo.feature.parent.grant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahmoud.feelo.core.domain.repository.ProfileRepository
import com.mahmoud.feelo.core.domain.usecase.GrantExtraVideosUseCase
import com.mahmoud.feelo.core.model.ChildProfile
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface GrantUiState {
    data object Loading : GrantUiState
    data class Ready(val child: ChildProfile) : GrantUiState
}

sealed interface GrantEvent {
    data class Grant(val videos: Int) : GrantEvent
    data object NoThanksClicked : GrantEvent
}

sealed interface GrantEffect {
    /** Granted (or declined): back to child mode, locked. */
    data object Done : GrantEffect
}

/** "عايز تكمّل؟" after the gate: +1 / +3 for today, then straight back to the child. */
class GrantViewModel(
    private val childId: String,
    profiles: ProfileRepository,
    private val grantExtras: GrantExtraVideosUseCase,
) : ViewModel() {

    private var granting = false

    val state: StateFlow<GrantUiState> = profiles.observeProfile(childId)
        .map { child -> if (child == null) GrantUiState.Loading else GrantUiState.Ready(child) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, GrantUiState.Loading)

    private val effectChannel = Channel<GrantEffect>(Channel.BUFFERED)
    val effects: Flow<GrantEffect> = effectChannel.receiveAsFlow()

    fun onEvent(event: GrantEvent) {
        when (event) {
            is GrantEvent.Grant -> {
                // One grant per visit: a double tap must not give +2 or +6.
                if (granting) return
                granting = true
                viewModelScope.launch {
                    grantExtras(childId, event.videos)
                    effectChannel.send(GrantEffect.Done)
                }
            }
            GrantEvent.NoThanksClicked -> effectChannel.trySend(GrantEffect.Done)
        }
    }
}
