package com.mahmoud.clipkid.core.model

/**
 * Today's usage for one child.
 *
 * [used], [total] and [remaining] are in [unit]: videos for a [DailyLimit.Videos] limit,
 * seconds for a [DailyLimit.Minutes] limit. [total] already includes parent-granted extras.
 * [videosWatched] and [secondsWatched] are always filled, for the Parent screen card.
 */
data class DailyQuota(
    val limit: DailyLimit,
    val extraVideosGranted: Int,
    val unit: QuotaUnit,
    val used: Int,
    val total: Int,
    val videosWatched: Int,
    val secondsWatched: Int,
) {
    val remaining: Int get() = (total - used).coerceAtLeast(0)
    val isReached: Boolean get() = used >= total
}

enum class QuotaUnit { Videos, Seconds }
