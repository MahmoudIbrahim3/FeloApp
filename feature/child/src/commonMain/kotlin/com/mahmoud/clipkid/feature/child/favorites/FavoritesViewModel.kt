package com.mahmoud.clipkid.feature.child.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahmoud.clipkid.core.domain.usecase.FavoriteItem
import com.mahmoud.clipkid.core.domain.usecase.GetFavoritesUseCase
import com.mahmoud.clipkid.core.domain.usecase.ToggleFavoriteUseCase
import com.mahmoud.clipkid.core.model.WatchSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface FavoritesUiState {
    data object Loading : FavoritesUiState

    data class Content(val items: List<FavoriteItem>) : FavoritesUiState {
        val isEmpty: Boolean get() = items.isEmpty()
    }
}

sealed interface FavoritesEvent {
    data class VideoClicked(val youtubeId: String) : FavoritesEvent

    /** Heart on a card: removes the favorite. */
    data class Unfavorite(val youtubeId: String) : FavoritesEvent
}

sealed interface FavoritesEffect {
    data class OpenVideo(val youtubeId: String, val source: WatchSource) : FavoritesEffect
}

class FavoritesViewModel(
    private val childId: String,
    getFavorites: GetFavoritesUseCase,
    private val toggleFavorite: ToggleFavoriteUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<FavoritesUiState>(FavoritesUiState.Loading)
    val state: StateFlow<FavoritesUiState> = _state.asStateFlow()

    private val effectChannel = Channel<FavoritesEffect>(Channel.BUFFERED)
    val effects: Flow<FavoritesEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            getFavorites(childId).collect { _state.value = FavoritesUiState.Content(it) }
        }
    }

    fun onEvent(event: FavoritesEvent) {
        when (event) {
            is FavoritesEvent.VideoClicked ->
                effectChannel.trySend(FavoritesEffect.OpenVideo(event.youtubeId, WatchSource.Favorites))
            is FavoritesEvent.Unfavorite ->
                viewModelScope.launch { toggleFavorite(childId, event.youtubeId) }
        }
    }
}
