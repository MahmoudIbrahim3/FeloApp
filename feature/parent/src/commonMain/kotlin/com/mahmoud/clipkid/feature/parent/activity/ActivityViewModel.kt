package com.mahmoud.clipkid.feature.parent.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahmoud.clipkid.core.domain.usecase.GetWeeklyActivityUseCase
import com.mahmoud.clipkid.core.domain.usecase.HideVideoUseCase
import com.mahmoud.clipkid.core.domain.usecase.ObserveAccessUseCase
import com.mahmoud.clipkid.core.domain.usecase.WeeklyActivity
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface ActivityUiState {
    data object Loading : ActivityUiState

    data class Content(
        val activity: WeeklyActivity,
        /**
         * The 7-day chart and week totals (ClipKid كامل). When false, only today's list shows, with
         * a card that opens the paywall. History is recorded either way.
         */
        val showsWeek: Boolean,
    ) : ActivityUiState
}

sealed interface ActivityEvent {
    /** "اخفيه": the video never shows for this child again and is reported for review. */
    data class HideClicked(val youtubeId: String) : ActivityEvent

    /** Free plan: the locked week card. The parent is already past the PIN. */
    data object SeeWeekClicked : ActivityEvent
    data object BackClicked : ActivityEvent
}

sealed interface ActivityEffect {
    data object Back : ActivityEffect
    data object OpenSubscription : ActivityEffect
}

/** Activity (Activity.png): today's videos for one child, and the last 7 days with the subscription. */
class ActivityViewModel(
    private val childId: String,
    getActivity: GetWeeklyActivityUseCase,
    private val hideVideo: HideVideoUseCase,
    observeAccess: ObserveAccessUseCase,
) : ViewModel() {

    val state: StateFlow<ActivityUiState> = combine(
        getActivity(childId),
        observeAccess().map { it.showsWeeklyActivity }.distinctUntilChanged(),
    ) { activity, showsWeek -> ActivityUiState.Content(activity, showsWeek) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ActivityUiState.Loading)

    private val effectChannel = Channel<ActivityEffect>(Channel.BUFFERED)
    val effects: Flow<ActivityEffect> = effectChannel.receiveAsFlow()

    fun onEvent(event: ActivityEvent) {
        when (event) {
            is ActivityEvent.HideClicked -> viewModelScope.launch { hideVideo(childId, event.youtubeId) }
            ActivityEvent.SeeWeekClicked -> effectChannel.trySend(ActivityEffect.OpenSubscription)
            ActivityEvent.BackClicked -> effectChannel.trySend(ActivityEffect.Back)
        }
    }
}
