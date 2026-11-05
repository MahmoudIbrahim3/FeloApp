package com.mahmoud.clipkid.core.player

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class YouTubePlayerStateTest {

    private class RecordingController : PlayerController {
        val scripts = mutableListOf<String>()
        override fun evaluate(script: String) {
            scripts += script
        }
    }

    private fun ready(duration: Int = 180) = BridgeMessage.Ready(durationSec = duration, timeSec = 0)
    private fun state(code: Int, time: Int = 0) = BridgeMessage.StateChange(code, timeSec = time, durationSec = 180)

    @Test
    fun autoPlay_startsTheTappedVideoOnReady() {
        val player = YouTubePlayerState(autoPlay = true)
        val controller = RecordingController()
        player.attach(controller)

        player.onBridgeMessage(ready())

        assertEquals(listOf(PlayerBridge.play()), controller.scripts)
        assertEquals(PlaybackState.Ready, player.playback)
        assertEquals(180, player.durationSec)
    }

    @Test
    fun withoutAutoPlay_nothingStarts() {
        val player = YouTubePlayerState(startSec = 40, autoPlay = false)
        val controller = RecordingController()
        player.attach(controller)

        player.onBridgeMessage(ready())

        assertEquals(emptyList(), controller.scripts)
    }

    @Test
    fun pauseBeforeReady_cancelsTheAutoPlay() {
        val player = YouTubePlayerState(autoPlay = true)
        val controller = RecordingController()
        player.attach(controller)

        player.pause() // e.g. the app went to the background while loading
        player.onBridgeMessage(ready())

        assertEquals(emptyList(), controller.scripts)
    }

    @Test
    fun commandsBeforeReady_areAppliedOnReady_seekFirst() {
        val player = YouTubePlayerState(autoPlay = false)
        val controller = RecordingController()
        player.attach(controller)

        player.seekTo(30)
        player.play()
        assertEquals(emptyList(), controller.scripts)

        player.onBridgeMessage(ready())
        assertEquals(listOf(PlayerBridge.seekTo(30), PlayerBridge.play()), controller.scripts)

        player.pause()
        assertEquals(PlayerBridge.pause(), controller.scripts.last())
    }

    @Test
    fun detachedController_receivesNothing() {
        val player = YouTubePlayerState(autoPlay = false)
        val controller = RecordingController()
        player.attach(controller)
        player.onBridgeMessage(ready())
        player.detach(controller)

        player.play()

        assertEquals(emptyList(), controller.scripts)
    }

    @Test
    fun pageMessages_becomeEventsInOrder() = runTest {
        val player = YouTubePlayerState()
        player.attach(RecordingController())

        player.events.test {
            player.onPageMessage("""{"type":"ready","duration":180,"time":0}""")
            player.onBridgeMessage(state(YtState.BUFFERING))
            player.onBridgeMessage(state(YtState.PLAYING, time = 0))
            player.onBridgeMessage(BridgeMessage.Tick(5))
            player.onBridgeMessage(state(YtState.PAUSED, time = 7))
            player.onBridgeMessage(state(YtState.PLAYING, time = 7))
            player.onBridgeMessage(state(YtState.ENDED, time = 180))
            player.onBridgeMessage(state(YtState.ENDED, time = 180)) // reported twice: one Ended

            assertEquals(YouTubePlayerEvent.Ready(180), awaitItem())
            assertEquals(YouTubePlayerEvent.Playing(0), awaitItem())
            assertEquals(YouTubePlayerEvent.Tick(5), awaitItem())
            assertEquals(YouTubePlayerEvent.Paused(7), awaitItem())
            assertEquals(YouTubePlayerEvent.Playing(7), awaitItem())
            assertEquals(YouTubePlayerEvent.Ended, awaitItem())
            expectNoEvents()
        }
        assertEquals(PlaybackState.Ended, player.playback)
        assertEquals(180, player.currentSec)
    }

    @Test
    fun anError_isFinal() = runTest {
        val player = YouTubePlayerState()
        player.events.test {
            player.onBridgeMessage(BridgeMessage.Error(150))
            player.onBridgeMessage(ready())
            player.onReadyTimeout()

            assertEquals(YouTubePlayerEvent.Error(150), awaitItem())
            expectNoEvents()
        }
        assertEquals(PlaybackState.Error(150), player.playback)
    }

    @Test
    fun readyTimeout_onlyWhileLoading() = runTest {
        val loading = YouTubePlayerState()
        loading.onReadyTimeout()
        assertEquals(PlaybackState.Error(PlayerConfig.ERROR_TIMEOUT), loading.playback)

        val ready = YouTubePlayerState()
        ready.onBridgeMessage(ready())
        ready.onReadyTimeout()
        assertEquals(PlaybackState.Ready, ready.playback)
    }

    @Test
    fun invalidId_isErrorTwo() {
        val player = YouTubePlayerState()
        player.onInvalidVideoId()
        assertEquals(PlaybackState.Error(2), player.playback)
    }

    @Test
    fun links_onlyWeb_andTheSameTapOnce() = runTest {
        val player = YouTubePlayerState()
        player.events.test {
            player.onLinkRequested("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
            player.onLinkRequested("https://www.youtube.com/watch?v=dQw4w9WgXcQ") // same tap, reported twice
            player.onLinkRequested("intent://evil")
            player.onLinkRequested("https://www.youtube.com/channel/abc")

            assertEquals(YouTubePlayerEvent.LinkRequested("https://www.youtube.com/watch?v=dQw4w9WgXcQ"), awaitItem())
            assertEquals(YouTubePlayerEvent.LinkRequested("https://www.youtube.com/channel/abc"), awaitItem())
            expectNoEvents()
        }
    }
}
