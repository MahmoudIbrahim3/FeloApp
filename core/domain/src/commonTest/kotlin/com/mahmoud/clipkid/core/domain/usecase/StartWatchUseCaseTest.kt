package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.testing.CHILD
import com.mahmoud.clipkid.core.domain.testing.FakeProfileRepository
import com.mahmoud.clipkid.core.domain.testing.FakeWatchHistoryRepository
import com.mahmoud.clipkid.core.domain.testing.FixedClock
import com.mahmoud.clipkid.core.domain.testing.KUWAIT
import com.mahmoud.clipkid.core.domain.testing.SequentialIdGenerator
import com.mahmoud.clipkid.core.domain.testing.child
import com.mahmoud.clipkid.core.domain.testing.event
import com.mahmoud.clipkid.core.domain.testing.kuwait
import com.mahmoud.clipkid.core.model.DailyLimit
import com.mahmoud.clipkid.core.model.WatchSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class StartWatchUseCaseTest {

    private val clock = FixedClock(kuwait(2026, 11, 4, 17, 0))
    private val history = FakeWatchHistoryRepository()

    private fun setUp(limit: DailyLimit): Pair<StartWatchUseCase, GetDailyQuotaUseCase> {
        val profiles = FakeProfileRepository(child(limit))
        return StartWatchUseCase(profiles, history, SequentialIdGenerator(), clock, KUWAIT) to
            GetDailyQuotaUseCase(profiles, history, clock, KUWAIT)
    }

    @Test
    fun startRecordsACountedEventAtNow() = runTest {
        val (start, _) = setUp(DailyLimit.Videos(3))

        val result = start(CHILD, "L1", WatchSource.Home)

        assertEquals(StartWatchResult.Allowed("event-1", countsTowardLimit = true), result)
        val recorded = history.events.value.single()
        assertEquals("L1", recorded.youtubeId)
        assertEquals(clock.instant, recorded.startedAt)
        assertEquals(0, recorded.watchedSec)
        assertEquals(true, recorded.countsTowardLimit)
    }

    @Test
    fun upNextReplaySameDayIsNotCounted() = runTest {
        val (start, quota) = setUp(DailyLimit.Videos(3))

        start(CHILD, "L1", WatchSource.Collection)
        val replay = start(CHILD, "L1", WatchSource.UpNextReplay)
        val replayAgain = start(CHILD, "L1", WatchSource.UpNextReplay)

        assertEquals(false, assertIs<StartWatchResult.Allowed>(replay).countsTowardLimit)
        assertEquals(false, assertIs<StartWatchResult.Allowed>(replayAgain).countsTowardLimit)
        assertEquals(1, quota.current(CHILD)!!.used)
        assertEquals(3, history.events.value.size)
    }

    @Test
    fun freeReplayIsAllowedEvenWhenTheLastVideoUsedTheLimit() = runTest {
        val (start, _) = setUp(DailyLimit.Videos(1))

        start(CHILD, "L1", WatchSource.Home)

        assertIs<StartWatchResult.Allowed>(start(CHILD, "L1", WatchSource.UpNextReplay))
        assertEquals(StartWatchResult.LimitReached, start(CHILD, "L2", WatchSource.UpNext))
    }

    @Test
    fun upNextReplayOfAVideoNotCountedTodayCounts() = runTest {
        history.events.value = listOf(event("L1", kuwait(2026, 11, 3, 18, 0)))
        val (start, quota) = setUp(DailyLimit.Videos(3))

        val result = start(CHILD, "L1", WatchSource.UpNextReplay)

        assertEquals(true, assertIs<StartWatchResult.Allowed>(result).countsTowardLimit)
        assertEquals(1, quota.current(CHILD)!!.used)
    }

    @Test
    fun reopeningTheSameVideoFromHomeCountsAgain() = runTest {
        val (start, quota) = setUp(DailyLimit.Videos(3))

        start(CHILD, "L1", WatchSource.Home)
        val again = start(CHILD, "L1", WatchSource.Home)

        assertEquals(true, assertIs<StartWatchResult.Allowed>(again).countsTowardLimit)
        assertEquals(2, quota.current(CHILD)!!.used)
    }

    @Test
    fun videosOpenedFromFavoritesCount() = runTest {
        val (start, quota) = setUp(DailyLimit.Videos(3))

        start(CHILD, "L1", WatchSource.Home)
        val fromFavorites = start(CHILD, "L1", WatchSource.Favorites)

        assertEquals(true, assertIs<StartWatchResult.Allowed>(fromFavorites).countsTowardLimit)
        assertEquals(2, quota.current(CHILD)!!.used)
    }

    @Test
    fun limitReachedRecordsNothing() = runTest {
        val (start, _) = setUp(DailyLimit.Videos(2))

        start(CHILD, "L1", WatchSource.Home)
        start(CHILD, "L2", WatchSource.UpNext)
        val third = start(CHILD, "L3", WatchSource.UpNext)

        assertEquals(StartWatchResult.LimitReached, third)
        assertEquals(2, history.events.value.size)
    }

    @Test
    fun grantedExtrasUnlockMoreStartsToday() = runTest {
        val (start, _) = setUp(DailyLimit.Videos(1))
        start(CHILD, "L1", WatchSource.Home)
        assertEquals(StartWatchResult.LimitReached, start(CHILD, "L2", WatchSource.Home))

        GrantExtraVideosUseCase(history, clock, KUWAIT)(CHILD, 1)

        assertIs<StartWatchResult.Allowed>(start(CHILD, "L2", WatchSource.Home))
        assertEquals(StartWatchResult.LimitReached, start(CHILD, "L3", WatchSource.Home))
    }

    @Test
    fun yesterdaysStartsDoNotCount() = runTest {
        history.events.value = listOf(
            event("L1", kuwait(2026, 11, 3, 20, 0)),
            event("L2", kuwait(2026, 11, 3, 23, 59, 59)),
        )
        val (start, _) = setUp(DailyLimit.Videos(2))

        assertIs<StartWatchResult.Allowed>(start(CHILD, "L3", WatchSource.Home))
    }

    @Test
    fun minutesMode_startAllowedWhileAnyTimeIsLeft() = runTest {
        val (start, _) = setUp(DailyLimit.Minutes(30))
        history.events.value = listOf(event("L1", kuwait(2026, 11, 4, 16, 0), watchedSec = 30 * 60 - 1))

        // One second left: a 3-minute video may still start and play to the end.
        assertIs<StartWatchResult.Allowed>(start(CHILD, "L2", WatchSource.UpNext))
    }

    @Test
    fun minutesMode_limitReachedOnceTimeIsUp_andReplaysAreNotFree() = runTest {
        val (start, _) = setUp(DailyLimit.Minutes(30))
        val first = assertIs<StartWatchResult.Allowed>(start(CHILD, "L1", WatchSource.Home))

        UpdateWatchProgressUseCase(history)(first.eventId, watchedSec = 30 * 60, completed = true)

        assertEquals(StartWatchResult.LimitReached, start(CHILD, "L1", WatchSource.UpNextReplay))
        assertEquals(StartWatchResult.LimitReached, start(CHILD, "L2", WatchSource.Home))
    }

    @Test
    fun minutesMode_replayIsRecordedAsCounting() = runTest {
        val (start, _) = setUp(DailyLimit.Minutes(30))
        start(CHILD, "L1", WatchSource.Home)

        val replay = start(CHILD, "L1", WatchSource.UpNextReplay)

        assertEquals(true, assertIs<StartWatchResult.Allowed>(replay).countsTowardLimit)
    }

    @Test
    fun minutesMode_extrasAddTenMinutesEach() = runTest {
        val (start, _) = setUp(DailyLimit.Minutes(30))
        history.events.value = listOf(event("L1", kuwait(2026, 11, 4, 16, 0), watchedSec = 30 * 60))
        assertEquals(StartWatchResult.LimitReached, start(CHILD, "L2", WatchSource.Home))

        GrantExtraVideosUseCase(history, clock, KUWAIT)(CHILD, 1)

        assertIs<StartWatchResult.Allowed>(start(CHILD, "L2", WatchSource.Home))
    }

    @Test
    fun unknownChildFails() = runTest {
        val start = StartWatchUseCase(FakeProfileRepository(), history, SequentialIdGenerator(), clock, KUWAIT)
        assertFailsWith<IllegalArgumentException> { start(CHILD, "L1", WatchSource.Home) }
    }
}
