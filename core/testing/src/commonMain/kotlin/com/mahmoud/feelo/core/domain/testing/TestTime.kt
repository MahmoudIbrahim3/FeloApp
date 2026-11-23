package com.mahmoud.feelo.core.domain.testing

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/** UTC+3, no DST: local midnight is 21:00 UTC. */
val KUWAIT: TimeZone = TimeZone.of("Asia/Kuwait")

fun kuwait(year: Int, month: Int, day: Int, hour: Int = 0, minute: Int = 0, second: Int = 0): Instant =
    LocalDateTime(year, month, day, hour, minute, second).toInstant(KUWAIT)

/**
 * A clock that follows the coroutine test scheduler: `advanceTimeBy` moves it, so delays inside
 * use cases (waiting for midnight) and the clock stay in sync.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TestClock(private val scheduler: TestCoroutineScheduler, private val origin: Instant) : Clock {
    override fun now(): Instant = origin + scheduler.currentTime.milliseconds
}

/** A fixed clock for one-shot use cases that don't wait. */
class FixedClock(var instant: Instant) : Clock {
    override fun now(): Instant = instant
}
