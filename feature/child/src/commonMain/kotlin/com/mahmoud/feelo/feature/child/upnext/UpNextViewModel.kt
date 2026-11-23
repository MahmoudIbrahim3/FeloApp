package com.mahmoud.feelo.feature.child.upnext

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahmoud.feelo.core.domain.repository.CatalogRepository
import com.mahmoud.feelo.core.domain.usecase.GetDailyQuotaUseCase
import com.mahmoud.feelo.core.domain.usecase.GetUpNextUseCase
import com.mahmoud.feelo.core.domain.usecase.TagPicks
import com.mahmoud.feelo.core.domain.usecase.UpNextResult
import com.mahmoud.feelo.core.model.Collection
import com.mahmoud.feelo.core.model.DailyQuota
import com.mahmoud.feelo.core.model.Video
import com.mahmoud.feelo.core.model.WatchSource
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

sealed interface UpNextUiState {
    data object Loading : UpNextUiState

    data class Ready(
        /** The video that just finished, for the "خلصته!" row. Null if it left the catalog. */
        val finished: Video?,
        val primary: Video?,
        val primaryCollectionTitle: String?,
        val tagPicks: TagPicks?,
        val quota: DailyQuota,
    ) : UpNextUiState
}

sealed interface UpNextEvent {
    /** One of the choices (primary or a tag pick). */
    data class ChoiceClicked(val youtubeId: String) : UpNextEvent

    /** "تاني": replay the video that just finished. */
    data object ReplayClicked : UpNextEvent
    data object HomeClicked : UpNextEvent
}

sealed interface UpNextEffect {
    /** The limit is used up: show DoneToday instead of Up Next. */
    data object OpenDoneToday : UpNextEffect

    data class OpenPlayer(val youtubeId: String, val source: WatchSource) : UpNextEffect
    data object Home : UpNextEffect
}

class UpNextViewModel(
    private val childId: String,
    private val finishedYoutubeId: String,
    private val getUpNext: GetUpNextUseCase,
    private val getQuota: GetDailyQuotaUseCase,
    private val catalog: CatalogRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<UpNextUiState>(UpNextUiState.Loading)
    val state: StateFlow<UpNextUiState> = _state.asStateFlow()

    private val effectChannel = Channel<UpNextEffect>(Channel.BUFFERED)
    val effects: Flow<UpNextEffect> = effectChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            when (val result = getUpNext(childId, finishedYoutubeId)) {
                UpNextResult.LimitReached -> effectChannel.send(UpNextEffect.OpenDoneToday)
                is UpNextResult.Choices -> {
                    val collections = catalog.observeCollections().first()
                    getQuota(childId).collect { quota ->
                        _state.value = toReady(result, collections, quota)
                    }
                }
            }
        }
    }

    fun onEvent(event: UpNextEvent) {
        val effect = when (event) {
            is UpNextEvent.ChoiceClicked -> UpNextEffect.OpenPlayer(event.youtubeId, WatchSource.UpNext)
            UpNextEvent.ReplayClicked -> UpNextEffect.OpenPlayer(finishedYoutubeId, WatchSource.UpNextReplay)
            UpNextEvent.HomeClicked -> UpNextEffect.Home
        }
        effectChannel.trySend(effect)
    }

    companion object {
        internal fun toReady(
            choices: UpNextResult.Choices,
            collections: List<Collection>,
            quota: DailyQuota,
        ) = UpNextUiState.Ready(
            finished = choices.finished,
            primary = choices.primary,
            primaryCollectionTitle = choices.primary?.let { p ->
                collections.firstOrNull { it.id == p.collectionId }?.titleAr
            },
            tagPicks = choices.tagPicks,
            quota = quota,
        )
    }
}
