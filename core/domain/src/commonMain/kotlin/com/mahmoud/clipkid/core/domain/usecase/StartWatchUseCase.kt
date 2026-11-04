package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.repository.IdGenerator
import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.WatchHistoryRepository
import com.mahmoud.clipkid.core.domain.time.todayRange
import com.mahmoud.clipkid.core.model.DailyLimit
import com.mahmoud.clipkid.core.model.WatchEvent
import com.mahmoud.clipkid.core.model.WatchSource
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

sealed interface StartWatchResult {
    /** Playback may start. [eventId] is used to report progress. */
    data class Allowed(val eventId: String, val countsTowardLimit: Boolean) : StartWatchResult

    /** Today's limit is used up: show DoneToday. Nothing was recorded. */
    data object LimitReached : StartWatchResult
}

/**
 * Called when playback starts; a video counts when it starts.
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
    suspend operator fun invoke(childId: String, youtubeId: String, source: WatchSource): StartWatchResult {
        val child = requireNotNull(profiles.getProfile(childId)) { "Unknown child $childId" }
        val day = clock.todayRange(timeZone)
        val todayEvents = history.getEvents(childId, day.start, day.end)

        val freeReplay = source == WatchSource.UpNextReplay &&
            child.dailyLimit is DailyLimit.Videos &&
            todayEvents.any { it.youtubeId == youtubeId && it.countsTowardLimit }

        if (!freeReplay) {
            val extras = history.getExtraVideos(childId, day.date)
            val quota = GetDailyQuotaUseCase.computeQuota(child.dailyLimit, extras, todayEvents)
            if (quota.isReached) return StartWatchResult.LimitReached
        }

        val event = WatchEvent(
            id = ids.newId(),
            childId = childId,
            youtubeId = youtubeId,
            startedAt = clock.now(),
            watchedSec = 0,
            completed = false,
            countsTowardLimit = !freeReplay,
        )
        history.record(event)
        return StartWatchResult.Allowed(event.id, event.countsTowardLimit)
    }
}
