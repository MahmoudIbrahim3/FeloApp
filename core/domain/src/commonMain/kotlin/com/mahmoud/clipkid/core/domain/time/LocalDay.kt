package com.mahmoud.clipkid.core.domain.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

/** The start of watch history: queries that need "ever" (finished at any time) begin here. */
internal val HISTORY_START: Instant = Instant.fromEpochSeconds(0)

/** One local calendar day as the half-open instant range `[start, end)`. */
data class DayRange(val date: LocalDate, val start: Instant, val end: Instant)

fun Clock.today(timeZone: TimeZone): LocalDate = now().toLocalDateTime(timeZone).date

/** Uses `atStartOfDayIn`, so days that are 23 or 25 hours long (DST) are handled. */
fun LocalDate.rangeIn(timeZone: TimeZone): DayRange =
    DayRange(this, atStartOfDayIn(timeZone), plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone))

fun Clock.todayRange(timeZone: TimeZone): DayRange = today(timeZone).rangeIn(timeZone)

/** Start of the local day [days] before [date]. */
fun startOfDaysBefore(date: LocalDate, days: Int, timeZone: TimeZone): Instant =
    date.minus(days, DateTimeUnit.DAY).atStartOfDayIn(timeZone)

/**
 * Emits today's local date, then again right after every local midnight, so screens that
 * show "today" (quota, home) reset without an app restart.
 */
fun currentDayFlow(clock: Clock, timeZone: TimeZone): Flow<LocalDate> = flow {
    while (true) {
        val now = clock.now()
        val today = now.toLocalDateTime(timeZone).date
        emit(today)
        val nextMidnight = today.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)
        delay((nextMidnight - now).coerceAtLeast(1.milliseconds))
    }
}.distinctUntilChanged()
