package com.mahmoud.clipkid.feature.child.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahmoud.clipkid.core.domain.usecase.ContinueWatching
import com.mahmoud.clipkid.core.domain.usecase.GetDailyQuotaUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetHomeFeedUseCase
import com.mahmoud.clipkid.core.domain.usecase.HomeFeed
import com.mahmoud.clipkid.core.domain.usecase.Shelf
import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.DailyQuota
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

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(
        val childName: String,
        val avatarColor: Long,
        val quota: DailyQuota,
        val continueWatching: ContinueWatching?,
        val collections: List<Collection>,
        /** Premium collections without the subscription: shown with a lock, open "اسأل بابا أو ماما". */
        val lockedCollections: List<Collection> = emptyList(),
        val newToday: List<Video>,
        val shelves: List<Shelf>,
    ) : HomeUiState {
        /** Nothing is visible to this child right now: show the empty state instead of rails. */
        val isEmpty: Boolean get() = newToday.isEmpty() && continueWatching == null

        /** The covers row: open and locked collections together, in catalog order. */
        val covers: List<CoverItem>
            get() = (collections.map { CoverItem(it, locked = false) } + lockedCollections.map { CoverItem(it, locked = true) })
                .sortedWith(compareBy<CoverItem> { it.collection.sortOrder }.thenBy { it.collection.id })
    }
}

data class CoverItem(val collection: Collection, val locked: Boolean)

sealed interface HomeEvent {
    data class VideoClicked(val youtubeId: String, val source: WatchSource) : HomeEvent
    data class CollectionClicked(val collectionId: String) : HomeEvent

    /** A premium collection without the subscription. Never the paywall: a child-friendly stop. */
    data class LockedCollectionClicked(val collectionId: String) : HomeEvent
    data object LockClicked : HomeEvent
}

sealed interface HomeEffect {
    data class OpenVideo(val youtubeId: String, val source: WatchSource) : HomeEffect
    data class OpenCollection(val collectionId: String) : HomeEffect
    data object OpenAskParent : HomeEffect
    data object OpenGate : HomeEffect
}

class HomeViewModel(
    private val childId: String,
    private val getHomeFeed: GetHomeFeedUseCase,
    private val getQuota: GetDailyQuotaUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val effectChannel = Channel<HomeEffect>(Channel.BUFFERED)
    val effects: Flow<HomeEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(getHomeFeed(childId), getQuota(childId)) { feed, quota -> toContent(feed, quota) }
                .collect { _state.value = it }
        }
    }

    fun onEvent(event: HomeEvent) {
        val effect = when (event) {
            is HomeEvent.VideoClicked -> HomeEffect.OpenVideo(event.youtubeId, event.source)
            is HomeEvent.CollectionClicked -> HomeEffect.OpenCollection(event.collectionId)
            is HomeEvent.LockedCollectionClicked -> HomeEffect.OpenAskParent
            HomeEvent.LockClicked -> HomeEffect.OpenGate
        }
        effectChannel.trySend(effect)
    }

    companion object {
        internal fun toContent(feed: HomeFeed, quota: DailyQuota) = HomeUiState.Content(
            childName = feed.child.nickname,
            avatarColor = feed.child.avatarColor,
            quota = quota,
            continueWatching = feed.continueWatching,
            collections = feed.collections,
            lockedCollections = feed.lockedCollections,
            newToday = feed.newToday,
            shelves = feed.shelves,
        )
    }
}
