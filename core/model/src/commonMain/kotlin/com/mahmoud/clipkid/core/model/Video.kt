package com.mahmoud.clipkid.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * A reviewed YouTube video inside one collection. YouTube fields (title, thumbnail, duration,
 * madeForKids, embeddable) are fetched and refreshed by the backend only (rule 5).
 *
 * [tags] are Arabic display labels (e.g. "الحيوانات"); Up Next shows them as "ولا حاجة عن {tag}؟".
 */
@Serializable
data class Video(
    @SerialName("youtube_id") val youtubeId: String,
    @SerialName("collection_id") val collectionId: String,
    @SerialName("title_ar") val titleAr: String,
    @SerialName("duration_sec") val durationSec: Int,
    @SerialName("thumbnail_url") val thumbnailUrl: String,
    val tags: List<String> = emptyList(),
    @SerialName("age_bands") val ageBands: Set<AgeBand>,
    @SerialName("sort_order") val sortOrder: Int,
    @SerialName("made_for_kids") val madeForKids: Boolean,
    val embeddable: Boolean,
    @SerialName("refreshed_at") val refreshedAt: Instant,
)
