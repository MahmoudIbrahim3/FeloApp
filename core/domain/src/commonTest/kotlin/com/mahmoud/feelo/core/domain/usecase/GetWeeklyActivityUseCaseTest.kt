package com.mahmoud.feelo.core.domain.usecase

import com.mahmoud.feelo.core.domain.testing.Catalog
import com.mahmoud.feelo.core.domain.testing.KUWAIT
import com.mahmoud.feelo.core.domain.testing.child
import com.mahmoud.feelo.core.domain.testing.event
import com.mahmoud.feelo.core.domain.testing.kuwait
import com.mahmoud.feelo.core.model.DailyLimit
import com.mahmoud.feelo.core.model.QuotaUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class GetWeeklyActivityUseCaseTest {

    private val today = LocalDate(2026, 11, 4)

    private fun build(limit: DailyLimit = DailyLimit.Videos(3), hidden: Set<String> = emptySet()) =
        GetWeeklyActivityUseCase.build(
            child = child(limit),
            collections = Catalog.collections,
            videos = Catalog.videos,
            hiddenIds = hidden,
            events = listOf(
                event("L1", kuwait(2026, 10, 29, 10, 0), watchedSec = 120, completed = true), // 6 days ago
                event("L2", kuwait(2026, 11, 3, 18, 0), watchedSec = 180, completed = true), // yesterday
                event("L3", kuwait(2026, 11, 4, 16, 15), watchedSec = 180, completed = true),
                event("L4", kuwait(2026, 11, 4, 16, 22), watchedSec = 60),
                // Free replay: seconds count, the video does not.
                event("L3", kuwait(2026, 11, 4, 16, 30), watchedSec = 180, completed = true, counts = false),
            ),
            today = today,
            timeZone = KUWAIT,
        )

    @Test
    fun sevenDaysOldestFirstEndingToday() {
        val activity = build()

        assertEquals(7, activity.days.size)
        assertEquals(LocalDate(2026, 10, 29), activity.days.first().date)
        assertEquals(today, activity.days.last().date)
        assertEquals(listOf(1, 0, 0, 0, 0, 1, 2), activity.days.map { it.videos })
        assertEquals(420, activity.days.last().seconds)
        assertEquals(4, activity.weekVideos)
        assertEquals((120 + 180 + 420) / 7 / 60, activity.averageMinutesPerDay)
    }

    @Test
    fun barsFollowTheLimitUnit() {
        val videos = build(DailyLimit.Videos(3))
        assertEquals(QuotaUnit.Videos, videos.unit)
        assertEquals(3, videos.limitLine)
        assertEquals(2, videos.barValue(videos.days.last()))

        val minutes = build(DailyLimit.Minutes(30))
        assertEquals(QuotaUnit.Seconds, minutes.unit)
        assertEquals(30, minutes.limitLine)
        assertEquals(7, minutes.barValue(minutes.days.last()))
    }

    @Test
    fun todayListsEachVideoOnceLatestFirst() {
        val rows = build().today

        assertEquals(listOf("L3", "L4"), rows.map { it.video.youtubeId })
        assertEquals(LocalTime(16, 30), rows[0].startedAt)
        assertEquals(true, rows[0].completed)
        assertEquals(false, rows[1].completed)
        assertEquals("مجموعة letters", rows[0].collectionTitle)
    }

    @Test
    fun hiddenVideosLeaveTodaysList() {
        assertEquals(listOf("L4"), build(hidden = setOf("L3")).today.map { it.video.youtubeId })
    }
}
