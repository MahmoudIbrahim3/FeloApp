package com.mahmoud.clipkid.feature.child.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahmoud.clipkid.core.domain.repository.CatalogRepository
import com.mahmoud.clipkid.core.domain.repository.FavoritesRepository
import com.mahmoud.clipkid.core.domain.usecase.StartWatchResult
import com.mahmoud.clipkid.core.domain.usecase.StartWatchUseCase
import com.mahmoud.clipkid.core.domain.usecase.ToggleFavoriteUseCase
import com.mahmoud.clipkid.core.domain.usecase.UpdateWatchProgressUseCase
import com.mahmoud.clipkid.core.model.CoverStyle
import com.mahmoud.clipkid.core.model.Video
import com.mahmoud.clipkid.core.model.WatchSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface PlayerUiState {
    data object Loading : PlayerUiState

    /** The video is not visible to this child any more (e.g. hidden while the screen was open). */
    data object Unavailable : PlayerUiState

    data class Ready(
        val video: Video,
        val collectionTitle: String,
        val collectionStyle: CoverStyle,
        val isFavorite: Boolean,
        /** True once "simulate end" ran: the video counts as completed and cannot end twice. */
        val ended: Boolean,
    ) : PlayerUiState
}

sealed interface PlayerEvent {
    data object ToggleFavorite : PlayerEvent

    /** Placeholder until the real player (Phase 4) reports the end of playback. */
    data object SimulateEnd : PlayerEvent
    data object BackClicked : PlayerEvent
}

sealed interface PlayerEffect {
    /** Today's limit is used up before this video could start: show DoneToday instead. */
    data object OpenDoneToday : PlayerEffect

    data class OpenUpNext(val finishedYoutubeId: String) : PlayerEffect
    data object Back : PlayerEffect
}

/**
 * Player screen. On open it asks [StartWatchUseCase] whether playback may start (a video counts
 * when it starts). Until Phase 4 the video is a placeholder, and [PlayerEvent.SimulateEnd] stands
 * in for the end of playback.
 */
class PlayerViewModel(
    private val childId: String,
    private val youtubeId: String,
    private val source: WatchSource,
    private val startWatch: StartWatchUseCase,
    private val updateProgress: UpdateWatchProgressUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val catalog: CatalogRepository,
    private val favorites: FavoritesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val effectChannel = Channel<PlayerEffect>(Channel.BUFFERED)
    val effects: Flow<PlayerEffect> = effectChannel.receiveAsFlow()

    private val ended = MutableStateFlow(false)
    private var eventId: String? = null

    init {
        viewModelScope.launch {
            combine(catalog.observeVideos(), catalog.observeCollections(), favorites.observeFavorites(childId), ended) {
                videos, collections, favs, isEnded ->
                val video = videos.firstOrNull { it.youtubeId == youtubeId }
                val collection = video?.let { v -> collections.firstOrNull { it.id == v.collectionId } }
                if (video == null || collection == null) {
                    PlayerUiState.Unavailable
                } else {
                    PlayerUiState.Ready(
                        video = video,
                        collectionTitle = collection.titleAr,
                        collectionStyle = collection.cover,
                        isFavorite = favs.any { it.youtubeId == youtubeId },
                        ended = isEnded,
                    )
                }
            }.collect { _state.value = it }
        }

        viewModelScope.launch {
            when (val result = startWatch(childId, youtubeId, source)) {
                is StartWatchResult.Allowed -> eventId = result.eventId
                StartWatchResult.LimitReached -> effectChannel.send(PlayerEffect.OpenDoneToday)
            }
        }
    }

    fun onEvent(event: PlayerEvent) {
        when (event) {
            PlayerEvent.ToggleFavorite -> viewModelScope.launch { toggleFavorite(childId, youtubeId) }
            PlayerEvent.SimulateEnd -> simulateEnd()
            PlayerEvent.BackClicked -> effectChannel.trySend(PlayerEffect.Back)
        }
    }

    private fun simulateEnd() {
        val id = eventId ?: return
        val ready = _state.value as? PlayerUiState.Ready ?: return
        // Checked on the flag itself: the UI state can lag behind a second tap.
        if (ended.value) return
        ended.value = true
        viewModelScope.launch {
            updateProgress(id, ready.video.durationSec, completed = true)
            effectChannel.send(PlayerEffect.OpenUpNext(youtubeId))
        }
    }
}
