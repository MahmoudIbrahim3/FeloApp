package com.mahmoud.clipkid.core.designsystem.text

/**
 * Arabic-Indic digit helpers. All numbers shown in the UI go through these
 * (CLAUDE.md: "Display numbers with Arabic-Indic digits").
 */

private const val ARABIC_INDIC_ZERO = '٠' // ٠

/** Replaces Western digits 0–9 with Arabic-Indic digits ٠–٩. Other characters are kept. */
fun String.withArabicDigits(): String = buildString(length) {
    for (c in this@withArabicDigits) {
        append(if (c in '0'..'9') ARABIC_INDIC_ZERO + (c - '0') else c)
    }
}

/** `12` -> "١٢". */
fun Int.toArabicDigits(): String = toString().withArabicDigits()

/** `12L` -> "١٢". */
fun Long.toArabicDigits(): String = toString().withArabicDigits()

/**
 * Video duration as "m:ss" (or "h:mm:ss" from one hour up) in Arabic-Indic digits.
 * `220` -> "٣:٤٠". Negative input is treated as zero.
 */
fun formatDuration(totalSeconds: Int): String {
    val total = totalSeconds.coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    val text = if (hours > 0) {
        "$hours:${minutes.twoDigits()}:${seconds.twoDigits()}"
    } else {
        "$minutes:${seconds.twoDigits()}"
    }
    return text.withArabicDigits()
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')
