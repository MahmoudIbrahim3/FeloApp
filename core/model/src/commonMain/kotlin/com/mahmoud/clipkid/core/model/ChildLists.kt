package com.mahmoud.clipkid.core.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

data class Favorite(
    val childId: String,
    val youtubeId: String,
    val addedAt: Instant,
)

/** A video a parent hid for one child. It never appears for that child again. */
data class HiddenVideo(
    val childId: String,
    val youtubeId: String,
    val hiddenAt: Instant,
)

/** Extra videos a parent granted to one child for one local day (summed across grants). */
data class ExtraGrant(
    val childId: String,
    val date: LocalDate,
    val videos: Int,
)
