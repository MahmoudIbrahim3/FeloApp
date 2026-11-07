package com.mahmoud.clipkid.core.data.remote

import com.mahmoud.clipkid.core.data.catalog.RemoteVideo
import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.CoverMark
import com.mahmoud.clipkid.core.model.CoverStyle
import com.mahmoud.clipkid.core.model.Video
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class SupabaseRowsTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun collectionRow_asPostgrestReturnsIt() {
        val row = json.decodeFromString(
            CollectionRow.serializer(),
            """{"id":"5b0f…","slug":"letters","title_ar":"الحروف","cover_bg":"#FFE6B0","cover_fg":"#8a5a00",
               "cover_glyph":"أ ب","cover_icon":null,"age_bands":["2-3","4-6"],"sort_order":1,
               "is_published":true,"updated_at":"2026-11-06T03:17:00.123456+00:00"}""",
        )
        assertEquals(
            Collection(
                id = "5b0f…",
                slug = "letters",
                titleAr = "الحروف",
                cover = CoverStyle(0xFFFFE6B0, 0xFF8A5A00, CoverMark.Glyph("أ ب")),
                ageBands = setOf(AgeBand.A2_3, AgeBand.A4_6),
                sortOrder = 1,
            ),
            row.toModel(),
        )
    }

    @Test
    fun collectionRow_iconMark_andBadRowsAreSkipped() {
        val base = CollectionRow("c", "quran-duas", "قرآن", "#D9F2E4", "#1F7A4A", null, "book", listOf("2-3"), 3)
        assertEquals(CoverMark.Icon("book"), base.toModel()?.cover?.mark)
        assertNull(base.copy(coverBg = "green").toModel())
        assertNull(base.copy(coverIcon = null).toModel())
        assertNull(base.copy(ageBands = listOf("13-15")).toModel())
    }

    @Test
    fun videoRow_asPostgrestReturnsIt() {
        val row = json.decodeFromString(
            VideoRow.serializer(),
            """{"youtube_id":"dQw4w9WgXcQ","collection_id":"c1","title_ar":"أغنية حرف الألف","tags":["أغاني"," "],
               "age_bands":["2-3","4-6"],"sort_order":2,"status":"approved","yt_duration_sec":185,
               "yt_thumbnail_url":"https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg","yt_made_for_kids":true,
               "yt_embeddable":true,"yt_refreshed_at":"2026-11-06T03:17:12.345678+00:00",
               "updated_at":"2026-11-06T03:17:12.345678+00:00"}""",
        )
        assertEquals(
            RemoteVideo(
                Video(
                    youtubeId = "dQw4w9WgXcQ",
                    collectionId = "c1",
                    titleAr = "أغنية حرف الألف",
                    durationSec = 185,
                    thumbnailUrl = "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg",
                    tags = listOf("أغاني"),
                    ageBands = setOf(AgeBand.A2_3, AgeBand.A4_6),
                    sortOrder = 2,
                    madeForKids = true,
                    embeddable = true,
                    refreshedAt = Instant.parse("2026-11-06T03:17:12.345678Z"),
                ),
                updatedAt = "2026-11-06T03:17:12.345678+00:00",
            ),
            row.toRemoteVideo(),
        )
    }

    @Test
    fun videoRow_withoutYouTubeData_isSkipped() {
        val row = VideoRow(
            youtubeId = "dQw4w9WgXcQ", collectionId = "c1", titleAr = "x", ageBands = listOf("2-3"),
            durationSec = 10, thumbnailUrl = "https://x", madeForKids = true, embeddable = true,
            refreshedAt = "2026-11-06T03:17:12Z", updatedAt = "t",
        )
        assertEquals("dQw4w9WgXcQ", row.toRemoteVideo()?.video?.youtubeId)
        assertNull(row.copy(durationSec = null).toRemoteVideo())
        assertNull(row.copy(thumbnailUrl = "").toRemoteVideo())
        assertNull(row.copy(refreshedAt = null).toRemoteVideo())
        assertNull(row.copy(refreshedAt = "yesterday").toRemoteVideo())
        assertNull(row.copy(ageBands = emptyList()).toRemoteVideo())
    }

    @Test
    fun hexColors() {
        assertEquals(0xFFFFE6B0, parseHexColor("#FFE6B0"))
        assertEquals(0xFF1A4FB5, parseHexColor("1a4fb5"))
        assertNull(parseHexColor("#FFF"))
        assertNull(parseHexColor("#GGGGGG"))
    }

    @Test
    fun reportRow_alwaysSendsTheReason() {
        assertEquals(
            """{"youtube_id":"dQw4w9WgXcQ","reason":"parent_hidden"}""",
            Json.encodeToString(VideoReportRow.serializer(), VideoReportRow("dQw4w9WgXcQ", "parent_hidden")),
        )
    }
}
