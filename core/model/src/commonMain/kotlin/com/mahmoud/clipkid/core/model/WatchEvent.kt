package com.mahmoud.clipkid.core.model

import kotlin.time.Instant

/**
 * One playback start. [countsTowardLimit] is false only for a free same-day replay from the
 * Up Next screen (videos mode). In minutes mode every event's [watchedSec] counts.
 *
 * [watchedSec] is the time actually played (seeking doesn't change it), used for the minutes
 * quota and the activity chart. [positionSec] is where playback last was, used to resume and for
 * progress bars. Rows saved before the real player (database v1) have [positionSec] = 0.
 */
data class WatchEvent(
    val id: String,
    val childId: String,
    val youtubeId: String,
    val startedAt: Instant,
    val watchedSec: Int,
    val completed: Boolean,
    val countsTowardLimit: Boolean,
    val positionSec: Int = 0,
) {
    /** How far into the video this playback got: [positionSec], or [watchedSec] for v1 rows. */
    val progressSec: Int get() = if (positionSec > 0) positionSec else watchedSec
}

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
