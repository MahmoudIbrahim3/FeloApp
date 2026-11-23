package com.mahmoud.feelo.core.domain.usecase

import com.mahmoud.feelo.core.domain.repository.ProfileRepository
import com.mahmoud.feelo.core.domain.repository.WatchHistoryRepository
import com.mahmoud.feelo.core.domain.time.currentDayFlow
import com.mahmoud.feelo.core.domain.time.rangeIn
import com.mahmoud.feelo.core.domain.time.todayRange
import com.mahmoud.feelo.core.model.DailyLimit
import com.mahmoud.feelo.core.model.DailyQuota
import com.mahmoud.feelo.core.model.QuotaUnit
import com.mahmoud.feelo.core.model.WatchEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.datetime.TimeZone
import kotlin.time.Clock

/** In minutes mode, each extra video a parent grants adds this many minutes to today's limit. */
const val MINUTES_PER_EXTRA_VIDEO: Int = 10

/**
 * Today's used / total / remaining for one child. The day is the local calendar day,
 * so usage resets at local midnight (the Flow re-emits by itself).
 *
 * - Videos(n): counts events with `countsTowardLimit` (free Up Next replays are not counted);
 *   total = n + extras.
 * - Minutes(m): sums `watchedSec` of every event today, replays included;
 *   total = (m + extras × [MINUTES_PER_EXTRA_VIDEO]) minutes, in seconds.
 */
class GetDailyQuotaUseCase(
    private val profiles: ProfileRepository,
    private val history: WatchHistoryRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(childId: String): Flow<DailyQuota> =
        currentDayFlow(clock, timeZone).flatMapLatest { date ->
            val day = date.rangeIn(timeZone)
            combine(
                profiles.observeProfile(childId).filterNotNull(),
                history.observeEvents(childId, day.start, day.end),
                history.observeExtraVideos(childId, date),
            ) { child, events, extras -> computeQuota(child.dailyLimit, extras, events) }
        }

    /** One-shot read for today, or null when the child does not exist. */
    suspend fun current(childId: String): DailyQuota? {
        val child = profiles.getProfile(childId) ?: return null
        val day = clock.todayRange(timeZone)
        val events = history.getEvents(childId, day.start, day.end)
        val extras = history.getExtraVideos(childId, day.date)
        return computeQuota(child.dailyLimit, extras, events)
    }

    companion object {
        /** Pure quota math over today's events. */
        fun computeQuota(limit: DailyLimit, extraVideos: Int, todayEvents: List<WatchEvent>): DailyQuota {
            val videosWatched = todayEvents.count { it.countsTowardLimit }
            val secondsWatched = todayEvents.sumOf { it.watchedSec.coerceAtLeast(0) }
            val extras = extraVideos.coerceAtLeast(0)
            return when (limit) {
                is DailyLimit.Videos -> DailyQuota(
                    limit = limit,
                    extraVideosGranted = extras,
                    unit = QuotaUnit.Videos,
                    used = videosWatched,
                    total = limit.count + extras,
                    videosWatched = videosWatched,
                    secondsWatched = secondsWatched,
                )
                is DailyLimit.Minutes -> DailyQuota(
                    limit = limit,
                    extraVideosGranted = extras,
                    unit = QuotaUnit.Seconds,
                    used = secondsWatched,
                    total = (limit.minutes + extras * MINUTES_PER_EXTRA_VIDEO) * 60,
                    videosWatched = videosWatched,
                    secondsWatched = secondsWatched,
                )
            }
        }
    }
}
