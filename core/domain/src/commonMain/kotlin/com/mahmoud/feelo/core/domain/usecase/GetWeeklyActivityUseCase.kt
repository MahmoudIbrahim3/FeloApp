package com.mahmoud.feelo.core.domain.usecase

import com.mahmoud.feelo.core.domain.repository.CatalogRepository
import com.mahmoud.feelo.core.domain.repository.HiddenVideosRepository
import com.mahmoud.feelo.core.domain.repository.ProfileRepository
import com.mahmoud.feelo.core.domain.repository.WatchHistoryRepository
import com.mahmoud.feelo.core.domain.time.currentDayFlow
import com.mahmoud.feelo.core.domain.time.rangeIn
import com.mahmoud.feelo.core.domain.time.startOfDaysBefore
import com.mahmoud.feelo.core.model.ChildProfile
import com.mahmoud.feelo.core.model.Collection
import com.mahmoud.feelo.core.model.DailyLimit
import com.mahmoud.feelo.core.model.QuotaUnit
import com.mahmoud.feelo.core.model.Video
import com.mahmoud.feelo.core.model.WatchEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

/** One day of the chart. */
data class DayActivity(
    val date: LocalDate,
    /** Videos that counted toward the limit. */
    val videos: Int,
    val seconds: Int,
)

/** One row of "اتفرج على إيه النهارده". */
data class WatchedToday(
    val video: Video,
    val collectionTitle: String,
    /** Local time of the latest start today. */
    val startedAt: LocalTime,
    /** True if any playback of it today reached the end. */
    val completed: Boolean,
)

data class WeeklyActivity(
    val child: ChildProfile,
    /** Exactly [ACTIVITY_DAYS] days, oldest first; the last one is today. */
    val days: List<DayActivity>,
    /** Bars follow the limit: videos per day in videos mode, minutes per day in minutes mode. */
    val unit: QuotaUnit,
    /** The dashed line, in videos or minutes (the base limit, without extras). */
    val limitLine: Int,
    /** Today's videos, latest first. Hidden videos are left out. */
    val today: List<WatchedToday>,
) {
    val weekVideos: Int get() = days.sumOf { it.videos }

    /** Average watched minutes per day over the whole window, rounded down. */
    val averageMinutesPerDay: Int get() = days.sumOf { it.seconds } / days.size.coerceAtLeast(1) / 60

    /** The bar value of one day, in [unit] (videos or whole minutes). */
    fun barValue(day: DayActivity): Int = if (unit == QuotaUnit.Videos) day.videos else day.seconds / 60
}

const val ACTIVITY_DAYS: Int = 7

/** The last 7 local days for one child, for the Activity screen. Re-emits at local midnight. */
class GetWeeklyActivityUseCase(
    private val catalog: CatalogRepository,
    private val profiles: ProfileRepository,
    private val history: WatchHistoryRepository,
    private val hidden: HiddenVideosRepository,
    private val clock: Clock,
    private val timeZone: TimeZone,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(childId: String): Flow<WeeklyActivity> =
        currentDayFlow(clock, timeZone).flatMapLatest { today ->
            val from = startOfDaysBefore(today, ACTIVITY_DAYS - 1, timeZone)
            val until = today.rangeIn(timeZone).end
            combine(
                profiles.observeProfile(childId).filterNotNull(),
                catalog.observeCollections(),
                catalog.observeVideos(),
                hidden.observeHiddenIds(childId),
                history.observeEvents(childId, from, until),
            ) { child, collections, videos, hiddenIds, events ->
                build(child, collections, videos, hiddenIds, events, today, timeZone)
            }
        }

    companion object {
        internal fun build(
            child: ChildProfile,
            collections: List<Collection>,
            videos: List<Video>,
            hiddenIds: Set<String>,
            events: List<WatchEvent>,
            today: LocalDate,
            timeZone: TimeZone,
        ): WeeklyActivity {
            val byDate = events.groupBy { it.startedAt.toLocalDateTime(timeZone).date }
            val days = (ACTIVITY_DAYS - 1 downTo 0).map { back ->
                val date = today.minus(back, DateTimeUnit.DAY)
                val dayEvents = byDate[date].orEmpty()
                DayActivity(
                    date = date,
                    videos = dayEvents.count { it.countsTowardLimit },
                    seconds = dayEvents.sumOf { it.watchedSec.coerceAtLeast(0) },
                )
            }
            val videoById = videos.associateBy { it.youtubeId }
            val titleById = collections.associate { it.id to it.titleAr }
            val todayRows = byDate[today].orEmpty()
                .filter { it.youtubeId !in hiddenIds }
                .groupBy { it.youtubeId }
                .mapNotNull { (id, plays) ->
                    val video = videoById[id] ?: return@mapNotNull null
                    val latest = plays.maxBy { it.startedAt }
                    WatchedToday(
                        video = video,
                        collectionTitle = titleById[video.collectionId].orEmpty(),
                        startedAt = latest.startedAt.toLocalDateTime(timeZone).time,
                        completed = plays.any { it.completed },
                    ) to latest.startedAt
                }
                .sortedByDescending { it.second }
                .map { it.first }
            val (unit, line) = when (val limit = child.dailyLimit) {
                is DailyLimit.Videos -> QuotaUnit.Videos to limit.count
                is DailyLimit.Minutes -> QuotaUnit.Seconds to limit.minutes
            }
            return WeeklyActivity(child, days, unit, line, todayRows)
        }
    }
}
