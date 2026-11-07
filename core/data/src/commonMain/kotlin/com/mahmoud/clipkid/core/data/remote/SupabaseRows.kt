package com.mahmoud.clipkid.core.data.remote

import com.mahmoud.clipkid.core.data.catalog.RemoteVideo
import com.mahmoud.clipkid.core.data.catalog.VideoStamp
import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.CoverMark
import com.mahmoud.clipkid.core.model.CoverStyle
import com.mahmoud.clipkid.core.model.Video
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

// Rows as PostgREST returns them (supabase/migrations/20261106000001_catalog.sql). Mapping is
// defensive: a row the app can't show (bad colour, unknown age band, missing YouTube data) is
// skipped instead of breaking the whole sync.

@Serializable
internal data class CollectionRow(
    val id: String,
    val slug: String,
    @SerialName("title_ar") val titleAr: String,
    @SerialName("cover_bg") val coverBg: String,
    @SerialName("cover_fg") val coverFg: String,
    @SerialName("cover_glyph") val coverGlyph: String? = null,
    @SerialName("cover_icon") val coverIcon: String? = null,
    @SerialName("age_bands") val ageBands: List<String> = emptyList(),
    @SerialName("sort_order") val sortOrder: Int = 0,
) {
    companion object {
        const val COLUMNS = "id,slug,title_ar,cover_bg,cover_fg,cover_glyph,cover_icon,age_bands,sort_order"
    }
}

@Serializable
internal data class VideoStampRow(
    @SerialName("youtube_id") val youtubeId: String,
    @SerialName("updated_at") val updatedAt: String,
) {
    fun toStamp() = VideoStamp(youtubeId, updatedAt)

    companion object {
        const val COLUMNS = "youtube_id,updated_at"
    }
}

@Serializable
internal data class VideoRow(
    @SerialName("youtube_id") val youtubeId: String,
    @SerialName("collection_id") val collectionId: String,
    @SerialName("title_ar") val titleAr: String,
    val tags: List<String> = emptyList(),
    @SerialName("age_bands") val ageBands: List<String> = emptyList(),
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("yt_duration_sec") val durationSec: Int? = null,
    @SerialName("yt_thumbnail_url") val thumbnailUrl: String? = null,
    @SerialName("yt_made_for_kids") val madeForKids: Boolean? = null,
    @SerialName("yt_embeddable") val embeddable: Boolean? = null,
    @SerialName("yt_refreshed_at") val refreshedAt: String? = null,
    @SerialName("updated_at") val updatedAt: String,
) {
    companion object {
        const val COLUMNS = "youtube_id,collection_id,title_ar,tags,age_bands,sort_order," +
            "yt_duration_sec,yt_thumbnail_url,yt_made_for_kids,yt_embeddable,yt_refreshed_at,updated_at"
    }
}

/** Insert-only; no defaults, so every field is always sent. */
@Serializable
internal data class VideoReportRow(
    @SerialName("youtube_id") val youtubeId: String,
    val reason: String,
)

private val AGE_BANDS = mapOf("2-3" to AgeBand.A2_3, "4-6" to AgeBand.A4_6, "7-9" to AgeBand.A7_9, "10-12" to AgeBand.A10_12)

internal fun parseAgeBands(values: List<String>): Set<AgeBand> = values.mapNotNull { AGE_BANDS[it.trim()] }.toSet()

/** "#FFE6B0" → 0xFFFFE6B0 (opaque ARGB). */
internal fun parseHexColor(value: String): Long? {
    val hex = value.trim().removePrefix("#")
    if (hex.length != 6) return null
    val rgb = hex.toLongOrNull(16) ?: return null
    return 0xFF000000L or rgb
}

internal fun CollectionRow.toModel(): Collection? {
    val bg = parseHexColor(coverBg) ?: return null
    val fg = parseHexColor(coverFg) ?: return null
    val mark = when {
        !coverGlyph.isNullOrBlank() -> CoverMark.Glyph(coverGlyph.trim())
        !coverIcon.isNullOrBlank() -> CoverMark.Icon(coverIcon.trim())
        else -> return null
    }
    val bands = parseAgeBands(ageBands).ifEmpty { return null }
    return Collection(
        id = id,
        slug = slug,
        titleAr = titleAr,
        cover = CoverStyle(bg, fg, mark),
        ageBands = bands,
        sortOrder = sortOrder,
    )
}

internal fun VideoRow.toRemoteVideo(): RemoteVideo? {
    val duration = durationSec ?: return null
    val thumbnail = thumbnailUrl?.takeIf { it.isNotBlank() } ?: return null
    val refreshed = refreshedAt?.let { runCatching { Instant.parse(it) }.getOrNull() } ?: return null
    val bands = parseAgeBands(ageBands).ifEmpty { return null }
    val video = Video(
        youtubeId = youtubeId,
        collectionId = collectionId,
        titleAr = titleAr,
        durationSec = duration,
        thumbnailUrl = thumbnail,
        tags = tags.map { it.trim() }.filter { it.isNotEmpty() },
        ageBands = bands,
        sortOrder = sortOrder,
        madeForKids = madeForKids == true,
        embeddable = embeddable == true,
        refreshedAt = refreshed,
    )
    return RemoteVideo(video, updatedAt)
}
