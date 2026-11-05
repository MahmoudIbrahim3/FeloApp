package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.testing.CHILD
import com.mahmoud.clipkid.core.domain.testing.FakeVideoReporter
import com.mahmoud.clipkid.core.domain.testing.FakeWatchHistoryRepository
import com.mahmoud.clipkid.core.domain.testing.FixedClock
import com.mahmoud.clipkid.core.domain.testing.KUWAIT
import com.mahmoud.clipkid.core.domain.testing.event
import com.mahmoud.clipkid.core.domain.testing.kuwait
import com.mahmoud.clipkid.core.model.VideoReport
import com.mahmoud.clipkid.core.model.WatchSource
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetResumePositionUseCaseTest {

    private val clock = FixedClock(kuwait(2026, 11, 4, 17, 0))
    private val history = FakeWatchHistoryRepository()
    private val resume = GetResumePositionUseCase(history, clock, KUWAIT)

    @Test
    fun continueWatching_resumesAtTheLatestUnfinishedPosition() = runTest {
        history.events.value = listOf(
            event("L1", kuwait(2026, 11, 3, 10), watchedSec = 50, positionSec = 40),
            event("L1", kuwait(2026, 11, 4, 9), watchedSec = 100, positionSec = 95),
        )
        assertEquals(95, resume(CHILD, "L1", durationSec = 180, WatchSource.ContinueWatching))
        assertEquals(95, resume(CHILD, "L1", durationSec = 180, WatchSource.Collection))
    }

    @Test
    fun otherEntryPoints_startAtZero() = runTest {
        history.events.value = listOf(event("L1", kuwait(2026, 11, 4, 9), watchedSec = 100, positionSec = 95))
        listOf(WatchSource.Home, WatchSource.NewToday, WatchSource.Favorites, WatchSource.UpNext, WatchSource.UpNextReplay)
            .forEach { assertEquals(0, resume(CHILD, "L1", durationSec = 180, it), "$it") }
    }

    @Test
    fun completedOrNearlyDoneOrTooOld_startsAtZero() = runTest {
        history.events.value = listOf(event("L1", kuwait(2026, 11, 4, 9), watchedSec = 180, positionSec = 180, completed = true))
        assertEquals(0, resume(CHILD, "L1", 180, WatchSource.ContinueWatching))

        history.events.value = listOf(event("L1", kuwait(2026, 11, 4, 9), watchedSec = 175, positionSec = 172))
        assertEquals(0, resume(CHILD, "L1", 180, WatchSource.ContinueWatching))

        history.events.value = listOf(event("L1", kuwait(2026, 10, 27, 9), watchedSec = 90, positionSec = 90))
        assertEquals(0, resume(CHILD, "L1", 180, WatchSource.ContinueWatching))
    }

    @Test
    fun rowsFromBeforeTheRealPlayer_useWatchedSec() = runTest {
        history.events.value = listOf(event("L1", kuwait(2026, 11, 4, 9), watchedSec = 60))
        assertEquals(60, resume(CHILD, "L1", 180, WatchSource.ContinueWatching))
    }
}

class ReportVideoUseCaseTest {

    @Test
    fun reportsOnlyTheIdAndReason() = runTest {
        val reporter = FakeVideoReporter()
        val report = ReportVideoUseCase(reporter)

        report("L1", VideoReportReason.Removed)
        report("L2", VideoReportReason.EmbedDisabled)
        report("L3", VideoReportReason.ParentHidden)

        assertEquals(
            listOf(VideoReport("L1", "video_removed"), VideoReport("L2", "embed_disabled"), VideoReport("L3", "parent_hidden")),
            reporter.reports,
        )
    }

    @Test
    fun aFailingReportIsSwallowed() = runTest {
        val reporter = FakeVideoReporter().apply { error = IllegalStateException("offline") }
        ReportVideoUseCase(reporter)("L1", VideoReportReason.Removed)
    }
}
