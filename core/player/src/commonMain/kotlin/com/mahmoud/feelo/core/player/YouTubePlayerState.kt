package com.mahmoud.feelo.core.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** Runs a script on the player page. Attached by the platform player while its web view lives. */
internal fun interface PlayerController {
    fun evaluate(script: String)
}

/**
 * State and commands of one [YouTubePlayer]. Create one per player instance (it is tied to one
 * web view): [startSec] and [autoPlay] are read once, when the page is built.
 *
 * [autoPlay] starts the video the child just tapped as soon as the player is ready. It is never
 * used to start a different video: the next one always needs another tap.
 *
 * Commands sent before the player is ready are kept and applied on `Ready`; a [pause] before
 * `Ready` (e.g. the app went to the background) cancels the autoplay.
 */
@Stable
class YouTubePlayerState(
    val startSec: Int = 0,
    val autoPlay: Boolean = true,
) {
    var playback: PlaybackState by mutableStateOf(PlaybackState.Loading)
        private set

    /** Last position the page reported (on every state change and every tick). */
    var currentSec: Int by mutableIntStateOf(startSec.coerceAtLeast(0))
        private set

    /** 0 until the player knows it. */
    var durationSec: Int by mutableIntStateOf(0)
        private set

    private val eventChannel = Channel<YouTubePlayerEvent>(Channel.UNLIMITED)

    /** One-off events in order. Collect from one place only (the screen). */
    val events: Flow<YouTubePlayerEvent> = eventChannel.receiveAsFlow()

    private var controller: PlayerController? = null
    private var isReady = false
    private var pendingPlay: Boolean? = null
    private var pendingSeek: Int? = null
    private var lastLink: Pair<String, TimeSource.Monotonic.ValueTimeMark>? = null

    fun play() = command(play = true)

    fun pause() = command(play = false)

    fun seekTo(sec: Int) {
        val target = sec.coerceAtLeast(0)
        val c = controller
        if (isReady && c != null) c.evaluate(PlayerBridge.seekTo(target)) else pendingSeek = target
    }

    private fun command(play: Boolean) {
        val c = controller
        if (isReady && c != null) {
            c.evaluate(if (play) PlayerBridge.play() else PlayerBridge.pause())
        } else {
            pendingPlay = play
        }
    }

    // region Called by the platform player (main thread)

    internal fun attach(controller: PlayerController) {
        this.controller = controller
        isReady = false
    }

    internal fun detach(controller: PlayerController) {
        if (this.controller === controller) {
            this.controller = null
            isReady = false
        }
    }

    /** A raw JSON message from the page. */
    internal fun onPageMessage(raw: String) {
        PlayerBridge.parse(raw)?.let(::onBridgeMessage)
    }

    internal fun onBridgeMessage(message: BridgeMessage) {
        if (playback is PlaybackState.Error) return
        when (message) {
            is BridgeMessage.Ready -> {
                isReady = true
                if (message.durationSec > 0) durationSec = message.durationSec
                currentSec = message.timeSec
                playback = PlaybackState.Ready
                send(YouTubePlayerEvent.Ready(durationSec))
                applyPending()
            }
            is BridgeMessage.StateChange -> {
                if (message.durationSec > 0) durationSec = message.durationSec
                currentSec = message.timeSec
                onStateChange(message.state)
            }
            is BridgeMessage.Tick -> {
                currentSec = message.timeSec
                send(YouTubePlayerEvent.Tick(message.timeSec))
            }
            is BridgeMessage.Error -> onError(message.code)
        }
    }

    private fun onStateChange(state: Int) {
        when (state) {
            YtState.PLAYING -> {
                playback = PlaybackState.Playing
                send(YouTubePlayerEvent.Playing(currentSec))
            }
            YtState.PAUSED -> {
                playback = PlaybackState.Paused
                send(YouTubePlayerEvent.Paused(currentSec))
            }
            YtState.ENDED -> {
                if (playback == PlaybackState.Ended) return
                playback = PlaybackState.Ended
                if (durationSec > 0) currentSec = durationSec
                send(YouTubePlayerEvent.Ended)
            }
            YtState.BUFFERING -> playback = PlaybackState.Buffering
            YtState.UNSTARTED, YtState.CUED -> if (playback == PlaybackState.Loading) playback = PlaybackState.Ready
        }
    }

    /** The id failed [PlayerConfig.isValidVideoId]: nothing is loaded. */
    internal fun onInvalidVideoId() = onError(PlayerConfig.ERROR_INVALID_PARAM)

    /** No `onReady` in time: treated like a failed script load (usually offline). */
    internal fun onReadyTimeout() {
        if (playback == PlaybackState.Loading) onError(PlayerConfig.ERROR_TIMEOUT)
    }

    private fun onError(code: Int) {
        if (playback is PlaybackState.Error) return
        if (code == PlayerConfig.ERROR_MISSING_REFERER) {
            logPlayerError(
                "YouTube error 153: the player request has no Referer. The page must be loaded with " +
                    "base URL ${PlayerConfig.BASE_URL} (loadDataWithBaseURL / loadHTMLString:baseURL:).",
            )
        } else {
            logPlayerError("YouTube player error $code")
        }
        playback = PlaybackState.Error(code)
        send(YouTubePlayerEvent.Error(code))
    }

    /** A blocked navigation. The same URL within 1 s (both platforms can report one tap twice) is dropped. */
    internal fun onLinkRequested(url: String) {
        if (!PlayerConfig.isOpenableLink(url)) return
        val now = TimeSource.Monotonic.markNow()
        val last = lastLink
        if (last != null && last.first == url && now - last.second < 1.seconds) return
        lastLink = url to now
        send(YouTubePlayerEvent.LinkRequested(url))
    }

    // endregion

    private fun applyPending() {
        val c = controller ?: return
        pendingSeek?.let { c.evaluate(PlayerBridge.seekTo(it)) }
        pendingSeek = null
        val shouldPlay = pendingPlay ?: autoPlay
        pendingPlay = null
        if (shouldPlay) c.evaluate(PlayerBridge.play())
    }

    private fun send(event: YouTubePlayerEvent) {
        eventChannel.trySend(event)
    }
}

/**
 * A state for one player instance. Pass a different [key] to start over with a new player
 * (e.g. "try again" after an offline error).
 */
@Composable
fun rememberYouTubePlayerState(startSec: Int, autoPlay: Boolean, key: Any? = null): YouTubePlayerState =
    remember(key) { YouTubePlayerState(startSec, autoPlay) }
