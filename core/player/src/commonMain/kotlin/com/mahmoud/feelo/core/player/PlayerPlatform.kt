package com.mahmoud.feelo.core.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The platform web view that shows [html] (already built for one video) with
 * [PlayerConfig.BASE_URL] as its base URL, forwards page messages and blocked links to [state],
 * and attaches itself as [state]'s controller while it lives.
 */
@Composable
internal expect fun PlatformPlayerView(html: String, state: YouTubePlayerState, modifier: Modifier)

/** Logged even in release builds: these errors mean a video can't play. */
internal expect fun logPlayerError(message: String)

/** True in debug builds (Android: debuggable app; iOS: debug binary). */
@Composable
expect fun rememberIsDebugBuild(): Boolean
