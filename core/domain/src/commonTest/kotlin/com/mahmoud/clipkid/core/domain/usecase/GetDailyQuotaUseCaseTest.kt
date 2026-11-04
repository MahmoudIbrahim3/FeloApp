package com.mahmoud.clipkid.core.domain.usecase

import app.cash.turbine.test
import com.mahmoud.clipkid.core.domain.testing.CHILD
import com.mahmoud.clipkid.core.domain.testing.FakeProfileRepository
import com.mahmoud.clipkid.core.domain.testing.FakeWatchHistoryRepository
import com.mahmoud.clipkid.core.domain.testing.FixedClock
import com.mahmoud.clipkid.core.domain.testing.KUWAIT
import com.mahmoud.clipkid.core.domain.testing.TestClock
import com.mahmoud.clipkid.core.domain.testing.child
import com.mahmoud.clipkid.core.domain.testing.event
import com.mahmoud.clipkid.core.domain.testing.kuwait
import com.mahmoud.clipkid.core.model.DailyLimit
import com.mahmoud.clipkid.core.model.QuotaUnit
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class GetDailyQuotaUseCaseTest {

    private val now = kuwait(2026, 11, 4, 18, 0)

    @Test
    fun videosMode_countsOnlyTodaysCountedStarts() = runTest {
        val history = FakeWatchHistoryRepository(
            listOf(
                event("L1", kuwait(2026, 11, 4, 9, 0), watchedSec = 120, completed = true),
                event("L2", kuwait(2026, 11, 4, 10, 0), watchedSec = 60),
                // Free Up Next replay: not counted.
                event("L1", kuwait(2026, 11, 4, 10, 5), watchedSec = 120, counts = false),
                // Yesterday: not today.
                event("L3", kuwait(2026, 11, 3, 23, 59), watchedSec = 300),
            ),
        )
        val useCase = GetDailyQuotaUseCase(FakeProfileRepository(child(DailyLimit.Videos(3))), history, FixedClock(now), KUWAIT)

        val quota = useCase.current(CHILD)!!

        assertEquals(QuotaUnit.Videos, quota.unit)
        assertEquals(2, quota.used)
        assertEquals(3, quota.total)
        assertEquals(1, quota.remaining)
        assertFalse(quota.isReached)
        assertEquals(2, quota.videosWatched)
        assertEquals(300, quota.secondsWatched)
    }

    @Test
    fun dayBoundaryIsLocalMidnightNotUtc() = runTest {
        // 00:30 in Kuwait is still 21:30 UTC of the previous day.
        val history = FakeWatchHistoryRepository(listOf(event("L1", kuwait(2026, 11, 4, 0, 30))))
        val useCase = GetDailyQuotaUseCase(
            FakeProfileRepository(child(DailyLimit.Videos(3))),
            history,
            FixedClock(kuwait(2026, 11, 4, 23, 59)),
            KUWAIT,
        )

        assertEquals(1, useCase.current(CHILD)!!.used)
    }

    @Test
    fun extraVideosRaiseTheVideoLimitForTodayOnly() = runTest {
        val history = FakeWatchHistoryRepository()
        val clock = FixedClock(now)
        val profiles = FakeProfileRepository(child(DailyLimit.Videos(3)))
        val grant = GrantExtraVideosUseCase(history, clock, KUWAIT)
        val useCase = GetDailyQuotaUseCase(profiles, history, clock, KUWAIT)

        grant(CHILD, 1)
        grant(CHILD, 3)
        assertEquals(7, useCase.current(CHILD)!!.total)
        assertEquals(4, useCase.current(CHILD)!!.extraVideosGranted)

        clock.instant = kuwait(2026, 11, 5, 0, 1)
        assertEquals(3, useCase.current(CHILD)!!.total)
    }

    @Test
    fun minutesMode_sumsAllWatchedSecondsAndConvertsExtrasToMinutes() = runTest {
        val history = FakeWatchHistoryRepository(
            listOf(
                event("L1", kuwait(2026, 11, 4, 9, 0), watchedSec = 600),
                // Replays count in minutes mode: time is time.
                event("L1", kuwait(2026, 11, 4, 9, 15), watchedSec = 480, counts = false),
            ),
        )
        val clock = FixedClock(now)
        val useCase = GetDailyQuotaUseCase(FakeProfileRepository(child(DailyLimit.Minutes(30))), history, clock, KUWAIT)

        var quota = useCase.current(CHILD)!!
        assertEquals(QuotaUnit.Seconds, quota.unit)
        assertEquals(1_080, quota.used)
        assertEquals(1_800, quota.total)
        assertEquals(720, quota.remaining)

        GrantExtraVideosUseCase(history, clock, KUWAIT)(CHILD, 1)
        quota = useCase.current(CHILD)!!
        assertEquals((30 + MINUTES_PER_EXTRA_VIDEO) * 60, quota.total)
        assertEquals(1, quota.videosWatched)
    }

    @Test
    fun reachedWhenUsedEqualsTotal() {
        val quota = GetDailyQuotaUseCase.computeQuota(
            DailyLimit.Videos(2),
            extraVideos = 0,
            todayEvents = listOf(event("L1", now), event("L2", now)),
        )
        assertTrue(quota.isReached)
        assertEquals(0, quota.remaining)
    }

    @Test
    fun zeroLimitIsReachedImmediately() {
        val quota = GetDailyQuotaUseCase.computeQuota(DailyLimit.Minutes(0), extraVideos = 0, todayEvents = emptyList())
        assertTrue(quota.isReached)
    }

    @Test
    fun unknownChildHasNoQuota() = runTest {
        val useCase = GetDailyQuotaUseCase(FakeProfileRepository(), FakeWatchHistoryRepository(), FixedClock(now), KUWAIT)
        assertNull(useCase.current(CHILD))
    }

    @Test
    fun flowResetsAtLocalMidnight() = runTest {
        val history = FakeWatchHistoryRepository(
            listOf(
                event("L1", kuwait(2026, 11, 4, 20, 0)),
                event("L2", kuwait(2026, 11, 4, 21, 0)),
                event("L3", kuwait(2026, 11, 4, 22, 0)),
            ),
        )
        history.extras.value = mapOf((CHILD to LocalDate(2026, 11, 4)) to 1)
        val clock = TestClock(testScheduler, kuwait(2026, 11, 4, 23, 58))
        val useCase = GetDailyQuotaUseCase(FakeProfileRepository(child(DailyLimit.Videos(2))), history, clock, KUWAIT)

        useCase(CHILD).test {
            val tonight = awaitItem()
            assertEquals(3, tonight.used)
            assertEquals(3, tonight.total)
            assertTrue(tonight.isReached)

            testScheduler.advanceTimeBy(3.minutes)

            val tomorrow = awaitItem()
            assertEquals(0, tomorrow.used)
            // Yesterday's extra does not carry over.
            assertEquals(2, tomorrow.total)
            assertFalse(tomorrow.isReached)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun flowUpdatesWhenAVideoStarts() = runTest {
        val history = FakeWatchHistoryRepository()
        val clock = TestClock(testScheduler, now)
        val profiles = FakeProfileRepository(child(DailyLimit.Videos(3)))
        val useCase = GetDailyQuotaUseCase(profiles, history, clock, KUWAIT)

        useCase(CHILD).test {
            assertEquals(0, awaitItem().used)
            history.record(event("L1", clock.now()))
            assertEquals(1, awaitItem().used)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
