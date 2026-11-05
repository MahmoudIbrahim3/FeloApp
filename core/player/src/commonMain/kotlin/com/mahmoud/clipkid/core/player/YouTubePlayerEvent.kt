package com.mahmoud.clipkid.core.player

/** What the player is doing. */
sealed interface PlaybackState {
    /** The page or the IFrame API is still loading. */
    data object Loading : PlaybackState

    /** The player is ready (or cued) and not playing yet. */
    data object Ready : PlaybackState
    data object Buffering : PlaybackState
    data object Playing : PlaybackState
    data object Paused : PlaybackState
    data object Ended : PlaybackState

    /** See [PlayerConfig] for the codes. */
    data class Error(val code: Int) : PlaybackState
}

/** One-off things the player reports, in order. Times are whole seconds. */
sealed interface YouTubePlayerEvent {
    data class Ready(val durationSec: Int) : YouTubePlayerEvent
    data class Playing(val positionSec: Int) : YouTubePlayerEvent
    data class Paused(val positionSec: Int) : YouTubePlayerEvent

    /** Every [PlayerConfig.TICK_INTERVAL] while playing. */
    data class Tick(val positionSec: Int) : YouTubePlayerEvent
    data object Ended : YouTubePlayerEvent
    data class Error(val code: Int) : YouTubePlayerEvent

    /**
     * A link in the player (YouTube logo, title, "Watch on YouTube", channel, end screen, ad)
     * tried to open. It was blocked; the app decides whether to open it (behind the parental gate).
     */
    data class LinkRequested(val url: String) : YouTubePlayerEvent
}
