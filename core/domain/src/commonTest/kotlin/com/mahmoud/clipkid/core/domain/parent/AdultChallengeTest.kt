package com.mahmoud.clipkid.core.domain.parent

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdultChallengeTest {

    @Test
    fun acceptsTheProductTypedWithWesternOrArabicDigits() {
        val challenge = AdultChallenge(7, 14)
        assertTrue(challenge.isCorrect("98"))
        assertTrue(challenge.isCorrect(" ٩٨ "))
        assertTrue(challenge.isCorrect("۹۸"))
        assertFalse(challenge.isCorrect("97"))
        assertFalse(challenge.isCorrect(""))
        assertFalse(challenge.isCorrect("9 8"))
    }

    @Test
    fun questionsStayInTheAdultRange() {
        val create = CreateAdultChallengeUseCase(Random(42))
        repeat(200) {
            val c = create()
            assertTrue(c.left in 6..9 && c.right in 11..19, "$c")
        }
    }

    @Test
    fun normalizeDigitsKeepsOtherCharacters() {
        assertEquals("12a3", normalizeDigits("١٢a٣"))
    }
}
