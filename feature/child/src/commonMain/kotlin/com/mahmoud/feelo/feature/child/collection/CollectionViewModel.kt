package com.mahmoud.feelo.feature.child.collection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahmoud.feelo.core.domain.usecase.CollectionPage
import com.mahmoud.feelo.core.domain.usecase.GetCollectionUseCase
import com.mahmoud.feelo.core.model.WatchSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface CollectionUiState {
    data object Loading : CollectionUiState

    /** Not visible to this child (not allowed, or another age band). Shown as an empty screen. */
    data object Unavailable : CollectionUiState

    data class Content(val page: CollectionPage) : CollectionUiState
}

sealed interface CollectionEvent {
    data class VideoClicked(val youtubeId: String) : CollectionEvent
    data object BackClicked : CollectionEvent
}

sealed interface CollectionEffect {
    data class OpenVideo(val youtubeId: String, val source: WatchSource) : CollectionEffect
    data object Back : CollectionEffect
}

class CollectionViewModel(
    childId: String,
    collectionId: String,
    getCollection: GetCollectionUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<CollectionUiState>(CollectionUiState.Loading)
    val state: StateFlow<CollectionUiState> = _state.asStateFlow()

    private val effectChannel = Channel<CollectionEffect>(Channel.BUFFERED)
    val effects: Flow<CollectionEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            getCollection(childId, collectionId).collect { page ->
                _state.value = page?.let { CollectionUiState.Content(it) } ?: CollectionUiState.Unavailable
            }
        }
    }

    fun onEvent(event: CollectionEvent) {
        val effect = when (event) {
            is CollectionEvent.VideoClicked -> CollectionEffect.OpenVideo(event.youtubeId, WatchSource.Collection)
            CollectionEvent.BackClicked -> CollectionEffect.Back
        }
        effectChannel.trySend(effect)
    }
}
