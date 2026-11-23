package com.mahmoud.feelo.core.domain.parent

import kotlin.random.Random

/**
 * "نسيت الرقم السري؟": a multiplication a young child is unlikely to solve, answered by typing
 * (no multiple choice to guess from). Passing it allows setting a new PIN.
 */
data class AdultChallenge(val left: Int, val right: Int) {
    val answer: Int get() = left * right

    /** Accepts Western and Arabic-Indic digits, as an Arabic keyboard may type either. */
    fun isCorrect(input: String): Boolean = normalizeDigits(input.trim()).toIntOrNull() == answer
}

/** A new question each time: 6..9 × 11..19 (answers 66..171). */
class CreateAdultChallengeUseCase(private val random: Random = Random.Default) {
    operator fun invoke(): AdultChallenge =
        AdultChallenge(left = random.nextInt(6, 10), right = random.nextInt(11, 20))
}

/** Maps Arabic-Indic (٠-٩) and Eastern Arabic-Indic (۰-۹) digits to 0-9; other characters stay. */
fun normalizeDigits(text: String): String = buildString(text.length) {
    for (c in text) {
        append(
            when (c) {
                in '٠'..'٩' -> '0' + (c - '٠')
                in '۰'..'۹' -> '0' + (c - '۰')
                else -> c
            },
        )
    }
}
