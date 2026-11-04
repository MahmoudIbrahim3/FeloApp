package com.mahmoud.clipkid.core.domain.parent

import com.mahmoud.clipkid.core.domain.testing.TestClock
import com.mahmoud.clipkid.core.domain.testing.kuwait
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ParentSessionTest {

    private fun TestScope.session() = ParentSession(TestClock(testScheduler, kuwait(2026, 11, 4, 20, 0)), backgroundScope)

    @Test
    fun startsLocked() = runTest {
        assertFalse(session().isUnlocked.value)
    }

    @Test
    fun locksAfterTwoMinutesWithoutATouch() = runTest {
        val session = session()
        session.unlock()
        runCurrent()

        advanceTimeBy(2.minutes - 1.milliseconds)
        runCurrent()
        assertTrue(session.isUnlocked.value)

        advanceTimeBy(2.milliseconds)
        runCurrent()
        assertFalse(session.isUnlocked.value)
    }

    @Test
    fun aTouchRestartsTheCountdown() = runTest {
        val session = session()
        session.unlock()
        runCurrent()

        advanceTimeBy(90.seconds)
        session.touch()
        advanceTimeBy(90.seconds)
        runCurrent()
        assertTrue(session.isUnlocked.value, "90 s after the last touch is still open")

        advanceTimeBy(31.seconds)
        runCurrent()
        assertFalse(session.isUnlocked.value)
    }

    @Test
    fun lockIsImmediate() = runTest {
        val session = session()
        session.unlock()
        session.lock()
        assertFalse(session.isUnlocked.value)
    }

    @Test
    fun touchWhileLockedDoesNotUnlock() = runTest {
        val session = session()
        session.touch()
        assertFalse(session.isUnlocked.value)
    }

    @Test
    fun unlockingAgainStartsAFreshCountdown() = runTest {
        val session = session()
        session.unlock()
        runCurrent()
        advanceTimeBy(100.seconds)
        session.lock()
        session.unlock()
        runCurrent()

        advanceTimeBy(100.seconds)
        runCurrent()
        assertTrue(session.isUnlocked.value)
    }
}
