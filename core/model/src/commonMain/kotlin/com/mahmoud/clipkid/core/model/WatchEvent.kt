package com.mahmoud.clipkid.core.model

import kotlin.time.Instant

/**
 * One playback start. [countsTowardLimit] is false only for a free same-day replay from the
 * Up Next screen (videos mode). In minutes mode every event's [watchedSec] counts.
 */
data class WatchEvent(
    val id: String,
    val childId: String,
    val youtubeId: String,
    val startedAt: Instant,
    val watchedSec: Int,
    val completed: Boolean,
    val countsTowardLimit: Boolean,
)

/** Where a playback was started from. Decides whether a replay is free. */
enum class WatchSource {
    Home,
    ContinueWatching,
    NewToday,
    Collection,
    Favorites,
    UpNext,

    /** The "تاني" button on Up Next for the video that just finished. */
    UpNextReplay,
}
