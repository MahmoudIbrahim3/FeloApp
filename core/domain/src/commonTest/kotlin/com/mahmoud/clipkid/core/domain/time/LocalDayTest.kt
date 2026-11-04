package com.mahmoud.clipkid.core.domain.time

import app.cash.turbine.test
import com.mahmoud.clipkid.core.domain.testing.KUWAIT
import com.mahmoud.clipkid.core.domain.testing.TestClock
import com.mahmoud.clipkid.core.domain.testing.kuwait
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

class LocalDayTest {

    @Test
    fun rangeIsLocalMidnightToMidnight() {
        val range = LocalDate(2026, 11, 4).rangeIn(KUWAIT)
        assertEquals(kuwait(2026, 11, 4), range.start)
        assertEquals(kuwait(2026, 11, 5), range.end)
        assertEquals(24.hours, range.end - range.start)
    }

    @Test
    fun dstDayIsTwentyThreeHours() {
        val newYork = TimeZone.of("America/New_York")
        val range = LocalDate(2026, 3, 8).rangeIn(newYork)
        assertEquals(23.hours, range.end - range.start)
    }

    @Test
    fun currentDayFlowEmitsAgainRightAfterMidnight() = runTest {
        val clock = TestClock(testScheduler, kuwait(2026, 11, 4, 23, 0))

        currentDayFlow(clock, KUWAIT).test {
            assertEquals(LocalDate(2026, 11, 4), awaitItem())
            testScheduler.advanceTimeBy(1.hours - 1.milliseconds)
            expectNoEvents()
            testScheduler.advanceTimeBy(2.milliseconds)
            assertEquals(LocalDate(2026, 11, 5), awaitItem())
            testScheduler.advanceTimeBy(24.hours)
            assertEquals(LocalDate(2026, 11, 6), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
