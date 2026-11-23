package com.mahmoud.feelo.core.designsystem.text

import kotlin.test.Test
import kotlin.test.assertEquals

class DurationTest {

    @Test
    fun durationMinutesAndSeconds() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:09", formatDuration(9))
        assertEquals("3:40", formatDuration(220))
        assertEquals("10:05", formatDuration(605))
    }

    @Test
    fun durationWithHours() {
        assertEquals("1:00:00", formatDuration(3600))
        assertEquals("1:02:03", formatDuration(3723))
    }

    @Test
    fun negativeDurationIsZero() {
        assertEquals("0:00", formatDuration(-12))
    }
}
