package com.mahmoud.feelo.core.domain.usecase

import com.mahmoud.feelo.core.domain.repository.IdGenerator
import com.mahmoud.feelo.core.domain.repository.ProfileRepository
import com.mahmoud.feelo.core.domain.repository.WatchHistoryRepository
import com.mahmoud.feelo.core.domain.time.todayRange
import com.mahmoud.feelo.core.model.DailyLimit
import com.mahmoud.feelo.core.model.WatchEvent
import com.mahmoud.feelo.core.model.WatchSource
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

sealed interface StartWatchResult {
    /** Playback may start. [eventId] is used to report progress. */
    data class Allowed(val eventId: String, val countsTowardLimit: Boolean) : StartWatchResult

    /** Today's limit is used up: show DoneToday. Nothing was recorded. */
    data object LimitReached : StartWatchResult
}

/**
 * A video counts when playback starts. The player screen calls [canStart] when it opens (so the
 * child goes straight to DoneToday at the limit) and [invoke] on the first real "playing" event
 * from the YouTube player, which records the [WatchEvent]. A video that never starts playing
 * (removed, not embeddable, offline) is never counted.
 *
 * Videos mode: the "تاني" replay on Up Next ([WatchSource.UpNextReplay]) of a video that already
 * counted today is free: it is recorded with `countsTowardLimit = false` and allowed even at the
 * limit. Every other start counts, including Favorites and reopening the same video from Home.
 *
 * Minutes mode: a start is allowed while any time is left; the replay is treated like any start
 * because all watched time counts.
 */
class StartWatchUseCase(
    private val profiles: ProfileRepository,
    private val history: WatchHistoryRepository,
    private val ids: IdGenerator,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    /** Whether [invoke] would allow this start right now. Records nothing. */
    suspend fun canStart(childId: String, youtubeId: String, source: WatchSource): Boolean =
        decide(childId, youtubeId, source) != null

    suspend operator fun invoke(childId: String, youtubeId: String, source: WatchSource): StartWatchResult {
        val countsTowardLimit = decide(childId, youtubeId, source) ?: return StartWatchResult.LimitReached
        val event = WatchEvent(
            id = ids.newId(),
            childId = childId,
            youtubeId = youtubeId,
            startedAt = clock.now(),
            watchedSec = 0,
            completed = false,
            countsTowardLimit = countsTowardLimit,
        )
        history.record(event)
        return StartWatchResult.Allowed(event.id, event.countsTowardLimit)
    }

    /** null = limit reached; otherwise whether the start counts toward the limit. */
    private suspend fun decide(childId: String, youtubeId: String, source: WatchSource): Boolean? {
        val child = requireNotNull(profiles.getProfile(childId)) { "Unknown child $childId" }
        val day = clock.todayRange(timeZone)
        val todayEvents = history.getEvents(childId, day.start, day.end)

        val freeReplay = source == WatchSource.UpNextReplay &&
            child.dailyLimit is DailyLimit.Videos &&
            todayEvents.any { it.youtubeId == youtubeId && it.countsTowardLimit }
        if (freeReplay) return false

        val extras = history.getExtraVideos(childId, day.date)
        val quota = GetDailyQuotaUseCase.computeQuota(child.dailyLimit, extras, todayEvents)
        return if (quota.isReached) null else true
    }
}
