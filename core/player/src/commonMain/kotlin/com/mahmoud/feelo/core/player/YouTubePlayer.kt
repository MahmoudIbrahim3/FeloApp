package com.mahmoud.feelo.core.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mahmoud.feelo.core.player.resources.Res
import kotlinx.coroutines.delay

/**
 * The official YouTube embedded player (IFrame API) in a web view, loaded with
 * [PlayerConfig.BASE_URL] as base URL so YouTube receives a Referer.
 *
 * - Player parameters: `rel=0`, `playsinline=1`, `enablejsapi=1`, `origin` = base URL.
 * - Nothing is drawn over the player and its controls, branding and ads are left as they are.
 * - Every attempt to leave the page (logo, title, end screen, ads) is blocked and reported as
 *   [YouTubePlayerEvent.LinkRequested], so the app can route it through the parental gate.
 * - The video pauses when the app goes to the background; it does not resume by itself.
 *
 * The caller keeps the viewport at least 200×200 dp.
 */
@Composable
fun YouTubePlayer(videoId: String, state: YouTubePlayerState, modifier: Modifier = Modifier) {
    val valid = remember(videoId) { PlayerConfig.isValidVideoId(videoId) }
    val html by produceState<String?>(initialValue = null, videoId, state) {
        value = if (valid) {
            PlayerBridge.buildHtml(Res.readBytes(PlayerBridge.HTML_PATH).decodeToString(), videoId, state.startSec)
        } else {
            null
        }
    }

    LaunchedEffect(state, valid) {
        if (!valid) {
            state.onInvalidVideoId()
        } else {
            delay(PlayerConfig.READY_TIMEOUT)
            state.onReadyTimeout()
        }
    }

    // Leaving the app pauses; coming back leaves it paused (no background playback).
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { state.pause() }

    Box(modifier.background(Color.Black)) {
        html?.let { PlatformPlayerView(it, state, Modifier.fillMaxSize()) }
    }
}
