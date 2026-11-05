package com.mahmoud.clipkid.feature.child.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahmoud.clipkid.core.domain.repository.CatalogRepository
import com.mahmoud.clipkid.core.domain.repository.FavoritesRepository
import com.mahmoud.clipkid.core.domain.usecase.GetResumePositionUseCase
import com.mahmoud.clipkid.core.domain.repository.NetworkMonitor
import com.mahmoud.clipkid.core.domain.usecase.ReportVideoUseCase
import com.mahmoud.clipkid.core.domain.usecase.StartWatchResult
import com.mahmoud.clipkid.core.domain.usecase.StartWatchUseCase
import com.mahmoud.clipkid.core.domain.usecase.ToggleFavoriteUseCase
import com.mahmoud.clipkid.core.domain.usecase.VideoReportReason
import com.mahmoud.clipkid.core.domain.usecase.UpdateWatchProgressUseCase
import com.mahmoud.clipkid.core.model.CoverStyle
import com.mahmoud.clipkid.core.model.Video
import com.mahmoud.clipkid.core.model.WatchSource
import com.mahmoud.clipkid.core.player.PlayerConfig
import com.mahmoud.clipkid.core.player.YouTubePlayerEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** What stands in for the player when it can't show the video. */
sealed interface PlayerProblem {
    /** Removed (100), embedding disabled (101 / 150), or any other player error. */
    data class NotAvailable(val code: Int) : PlayerProblem

    /** The player couldn't load (offline, or no answer in time). Can be retried. */
    data object Offline : PlayerProblem
}

/** How the screen should set up the YouTube player right now. */
data class PlayerSlot(
    /** Where a newly created player starts: the resume point, then the last known position. */
    val startSec: Int,
    /** Only before the first play: the video the child tapped starts by itself, once. */
    val autoPlay: Boolean,
    /** False once the video ended, so YouTube's end screen is never shown. */
    val visible: Boolean,
    val problem: PlayerProblem?,
    /** Changes on "try again", so the screen creates a fresh player. */
    val attempt: Int,
)

sealed interface PlayerUiState {
    data object Loading : PlayerUiState

    /** The video is not visible to this child any more (e.g. hidden while the screen was open). */
    data object Unavailable : PlayerUiState

    data class Ready(
        val video: Video,
        val collectionTitle: String,
        val collectionStyle: CoverStyle,
        val isFavorite: Boolean,
        val player: PlayerSlot,
    ) : PlayerUiState
}

sealed interface PlayerEvent {
    data object ToggleFavorite : PlayerEvent
    data object BackClicked : PlayerEvent
    data object RetryClicked : PlayerEvent

    /** Everything the YouTube player reports, forwarded as is. */
    data class FromPlayer(val event: YouTubePlayerEvent) : PlayerEvent
}

sealed interface PlayerEffect {
    /** Today's limit is used up before this video could start: show DoneToday instead. */
    data object OpenDoneToday : PlayerEffect

    data class OpenUpNext(val finishedYoutubeId: String) : PlayerEffect

    /** A link in the player wants to open: only through the parental gate. */
    data class OpenExternalLink(val url: String) : PlayerEffect
    data object Back : PlayerEffect
}

/**
 * Player screen.
 *
 * - On open it checks [StartWatchUseCase.canStart]; at the limit it goes to DoneToday and the
 *   player is never created.
 * - The video counts when YouTube reports it playing for the first time ([StartWatchUseCase]
 *   records the WatchEvent then). A video that fails to load is never counted.
 * - Progress (time actually played + position) is saved on every tick, pause, link and back.
 * - On `Ended` the player is removed and Up Next opens (it shows DoneToday at the limit).
 *
 * The event id and progress live in [SavedStateHandle], so leaving for the parental gate,
 * rotation or process death never counts the same playback twice.
 *
 * Offline: with no network when the screen opens, "مفيش نت" shows at once (no player is
 * created); when the network comes back, a fresh player is tried automatically.
 */
class PlayerViewModel(
    private val childId: String,
    private val youtubeId: String,
    private val source: WatchSource,
    private val savedState: SavedStateHandle,
    private val startWatch: StartWatchUseCase,
    private val updateProgress: UpdateWatchProgressUseCase,
    private val getResumePosition: GetResumePositionUseCase,
    private val reportVideo: ReportVideoUseCase,
    private val network: NetworkMonitor,
    private val toggleFavorite: ToggleFavoriteUseCase,
    private val catalog: CatalogRepository,
    private val favorites: FavoritesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<PlayerUiState>(PlayerUiState.Loading)
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val effectChannel = Channel<PlayerEffect>(Channel.BUFFERED)
    val effects: Flow<PlayerEffect> = effectChannel.receiveAsFlow()

    /** null until the start check and resume lookup are done: no player before that. */
    private val slot = MutableStateFlow<PlayerSlot?>(null)

    private var eventId: String? = savedState[KEY_EVENT_ID]
    private var watchedSec: Int = savedState[KEY_WATCHED] ?: 0
    private var positionSec: Int = savedState[KEY_POSITION] ?: 0
    private var completed = false
    private var ended = false
    private var recording = false
    private var reported = false

    /** Last position heard from the player while it was playing; null when not playing. */
    private var playingFrom: Int? = null
    private val writes = Mutex()

    init {
        viewModelScope.launch {
            combine(
                catalog.observeVideos(),
                catalog.observeCollections(),
                favorites.observeFavorites(childId),
                slot,
            ) { videos, collections, favs, playerSlot ->
                val video = videos.firstOrNull { it.youtubeId == youtubeId }
                val collection = video?.let { v -> collections.firstOrNull { it.id == v.collectionId } }
                when {
                    video == null || collection == null -> PlayerUiState.Unavailable
                    playerSlot == null -> PlayerUiState.Loading
                    else -> PlayerUiState.Ready(
                        video = video,
                        collectionTitle = collection.titleAr,
                        collectionStyle = collection.cover,
                        isFavorite = favs.any { it.youtubeId == youtubeId },
                        player = playerSlot,
                    )
                }
            }.collect { _state.value = it }
        }

        viewModelScope.launch { prepare() }

        // Back online while showing "مفيش نت": try again with a fresh player.
        viewModelScope.launch {
            network.isOnline.collect { online ->
                if (online && slot.value?.problem == PlayerProblem.Offline) retry()
            }
        }
    }

    private suspend fun prepare() {
        val alreadyStarted = eventId != null
        if (!alreadyStarted && !startWatch.canStart(childId, youtubeId, source)) {
            effectChannel.send(PlayerEffect.OpenDoneToday)
            return
        }
        val startSec = if (alreadyStarted) {
            positionSec
        } else {
            val duration = catalog.observeVideos().first().firstOrNull { it.youtubeId == youtubeId }?.durationSec ?: 0
            getResumePosition(childId, youtubeId, duration, source).also { positionSec = it }
        }
        val offline = !network.isOnline.value
        slot.value = PlayerSlot(
            startSec = startSec,
            autoPlay = !alreadyStarted,
            visible = !offline,
            problem = if (offline) PlayerProblem.Offline else null,
            attempt = 0,
        )
    }

    fun onEvent(event: PlayerEvent) {
        when (event) {
            PlayerEvent.ToggleFavorite -> viewModelScope.launch { toggleFavorite(childId, youtubeId) }
            PlayerEvent.BackClicked -> viewModelScope.launch {
                saveProgress()
                effectChannel.send(PlayerEffect.Back)
            }
            PlayerEvent.RetryClicked -> retry()
            is PlayerEvent.FromPlayer -> onPlayer(event.event)
        }
    }

    private fun retry() {
        slot.update { it?.copy(problem = null, visible = true, attempt = it.attempt + 1, startSec = positionSec) }
    }

    private fun onPlayer(event: YouTubePlayerEvent) {
        if (ended) return
        when (event) {
            is YouTubePlayerEvent.Ready -> Unit
            is YouTubePlayerEvent.Playing -> {
                advanceTo(event.positionSec)
                playingFrom = event.positionSec
                onFirstPlay()
            }
            is YouTubePlayerEvent.Tick -> {
                advanceTo(event.positionSec)
                persistLater()
            }
            is YouTubePlayerEvent.Paused -> {
                advanceTo(event.positionSec)
                playingFrom = null
                persistLater()
            }
            YouTubePlayerEvent.Ended -> onEnded()
            is YouTubePlayerEvent.Error -> onError(event.code)
            is YouTubePlayerEvent.LinkRequested -> viewModelScope.launch {
                saveProgress()
                effectChannel.send(PlayerEffect.OpenExternalLink(event.url))
            }
        }
    }

    /**
     * Adds the time played since the last report. A jump of more than [MAX_STEP_SEC] forward is a
     * seek (not counted); a jump backwards is a seek or replay. Either way the position follows.
     */
    private fun advanceTo(sec: Int) {
        val from = playingFrom
        if (from != null) {
            val step = sec - from
            if (step in 1..MAX_STEP_SEC) watchedSec += step
            playingFrom = sec
        }
        positionSec = sec
        // A new player (after the gate, "try again" or a recreation) resumes here.
        slot.update { it?.copy(startSec = sec) }
        savedState[KEY_WATCHED] = watchedSec
        savedState[KEY_POSITION] = positionSec
    }

    private fun onFirstPlay() {
        if (eventId != null || recording) return
        recording = true
        slot.update { it?.copy(autoPlay = false) }
        viewModelScope.launch {
            when (val result = startWatch(childId, youtubeId, source)) {
                is StartWatchResult.Allowed -> {
                    eventId = result.eventId
                    savedState[KEY_EVENT_ID] = result.eventId
                    saveProgress()
                }
                StartWatchResult.LimitReached -> {
                    // Only possible if the quota changed since the screen opened.
                    slot.update { it?.copy(visible = false) }
                    effectChannel.send(PlayerEffect.OpenDoneToday)
                }
            }
            recording = false
        }
    }

    private fun onEnded() {
        ended = true
        val from = playingFrom
        val duration = (_state.value as? PlayerUiState.Ready)?.video?.durationSec ?: positionSec
        if (from != null && duration > from && duration - from <= MAX_STEP_SEC) watchedSec += duration - from
        playingFrom = null
        positionSec = maxOf(positionSec, duration)
        completed = true
        // Remove the player at once: YouTube's end screen is never shown, even during the transition.
        slot.update { it?.copy(visible = false) }
        viewModelScope.launch {
            saveProgress()
            effectChannel.send(PlayerEffect.OpenUpNext(youtubeId))
        }
    }

    private fun onError(code: Int) {
        playingFrom = null
        val problem = when (code) {
            PlayerConfig.ERROR_SCRIPT_FAILED, PlayerConfig.ERROR_TIMEOUT -> PlayerProblem.Offline
            else -> PlayerProblem.NotAvailable(code)
        }
        slot.update { it?.copy(problem = problem, visible = false) }
        val reason = when (code) {
            ERROR_REMOVED -> VideoReportReason.Removed
            ERROR_NOT_EMBEDDABLE, ERROR_NOT_EMBEDDABLE_TOO -> VideoReportReason.EmbedDisabled
            else -> null
        }
        if (reason != null && !reported) {
            reported = true
            viewModelScope.launch { reportVideo(youtubeId, reason) }
        }
        persistLater()
    }

    private fun persistLater() {
        viewModelScope.launch { saveProgress() }
    }

    /** Writes the latest values; serialised so an older snapshot never overwrites a newer one. */
    private suspend fun saveProgress() {
        writes.withLock {
            val id = eventId ?: return
            updateProgress(id, watchedSec, positionSec, completed)
        }
    }

    companion object {
        /** Ticks come every 5 s; anything bigger between two reports is a seek. */
        const val MAX_STEP_SEC = 7

        const val ERROR_REMOVED = 100
        const val ERROR_NOT_EMBEDDABLE = 101
        const val ERROR_NOT_EMBEDDABLE_TOO = 150

        private const val KEY_EVENT_ID = "player.eventId"
        private const val KEY_WATCHED = "player.watchedSec"
        private const val KEY_POSITION = "player.positionSec"
    }
}
