package com.mahmoud.clipkid.feature.parent.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahmoud.clipkid.core.domain.usecase.GetWeeklyActivityUseCase
import com.mahmoud.clipkid.core.domain.usecase.HideVideoUseCase
import com.mahmoud.clipkid.core.domain.usecase.WeeklyActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ActivityUiState {
    data object Loading : ActivityUiState
    data class Content(val activity: WeeklyActivity) : ActivityUiState
}

sealed interface ActivityEvent {
    /** "اخفيه": the video never shows for this child again and is reported for review. */
    data class HideClicked(val youtubeId: String) : ActivityEvent
    data object BackClicked : ActivityEvent
}

sealed interface ActivityEffect {
    data object Back : ActivityEffect
}

/** Activity (Activity.png): the last 7 days and today's videos for one child. */
class ActivityViewModel(
    private val childId: String,
    getActivity: GetWeeklyActivityUseCase,
    private val hideVideo: HideVideoUseCase,
) : ViewModel() {

    val state: StateFlow<ActivityUiState> = getActivity(childId)
        .map<WeeklyActivity, ActivityUiState> { ActivityUiState.Content(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ActivityUiState.Loading)

    private val effectChannel = Channel<ActivityEffect>(Channel.BUFFERED)
    val effects: Flow<ActivityEffect> = effectChannel.receiveAsFlow()

    fun onEvent(event: ActivityEvent) {
        when (event) {
            is ActivityEvent.HideClicked -> viewModelScope.launch { hideVideo(childId, event.youtubeId) }
            ActivityEvent.BackClicked -> effectChannel.trySend(ActivityEffect.Back)
        }
    }
}
