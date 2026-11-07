package com.mahmoud.clipkid.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A hand-curated group of videos (e.g. الحروف). Comes from the backend catalog. */
@Serializable
data class Collection(
    val id: String,
    val slug: String,
    @SerialName("title_ar") val titleAr: String,
    val cover: CoverStyle,
    @SerialName("age_bands") val ageBands: Set<AgeBand>,
    @SerialName("sort_order") val sortOrder: Int,
)
