package com.mahmoud.feelo.core.designsystem.text

/**
 * Video duration as "m:ss" (or "h:mm:ss" from one hour up), in Western digits like every
 * number in the UI. `220` -> "3:40". Negative input is treated as zero.
 */
fun formatDuration(totalSeconds: Int): String {
    val total = totalSeconds.coerceAtLeast(0)
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val seconds = total % 60
    return if (hours > 0) {
        "$hours:${minutes.twoDigits()}:${seconds.twoDigits()}"
    } else {
        "$minutes:${seconds.twoDigits()}"
    }
}

private fun Int.twoDigits(): String = toString().padStart(2, '0')
