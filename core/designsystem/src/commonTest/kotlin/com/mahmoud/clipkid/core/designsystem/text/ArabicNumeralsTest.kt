package com.mahmoud.clipkid.core.designsystem.text

import kotlin.test.Test
import kotlin.test.assertEquals

class ArabicNumeralsTest {

    @Test
    fun convertsEveryWesternDigit() {
        assertEquals("٠١٢٣٤٥٦٧٨٩", "0123456789".withArabicDigits())
    }

    @Test
    fun keepsNonDigitCharacters() {
        assertEquals("خطوة ٢ من ٣", "خطوة 2 من 3".withArabicDigits())
        assertEquals("", "".withArabicDigits())
    }

    @Test
    fun alreadyArabicDigitsAreUnchanged() {
        assertEquals("١٢٣", "١٢٣".withArabicDigits())
    }

    @Test
    fun intsAndLongs() {
        assertEquals("٠", 0.toArabicDigits())
        assertEquals("١٠", 10.toArabicDigits())
        assertEquals("-٥", (-5).toArabicDigits())
        assertEquals("٣٠٠٠٠٠٠٠٠٠", 3_000_000_000L.toArabicDigits())
    }

    @Test
    fun durationMinutesAndSeconds() {
        assertEquals("٠:٠٠", formatDuration(0))
        assertEquals("٠:٠٩", formatDuration(9))
        assertEquals("٣:٤٠", formatDuration(220))
        assertEquals("١٠:٠٥", formatDuration(605))
    }

    @Test
    fun durationWithHours() {
        assertEquals("١:٠٠:٠٠", formatDuration(3600))
        assertEquals("١:٠٢:٠٣", formatDuration(3723))
    }

    @Test
    fun negativeDurationIsZero() {
        assertEquals("٠:٠٠", formatDuration(-12))
    }
}
