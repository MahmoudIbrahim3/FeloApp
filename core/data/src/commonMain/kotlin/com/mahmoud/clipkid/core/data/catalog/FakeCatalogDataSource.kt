package com.mahmoud.clipkid.core.data.catalog

import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.core.model.AgeBand.A3_5
import com.mahmoud.clipkid.core.model.AgeBand.A6_8
import com.mahmoud.clipkid.core.model.AgeBand.A9_12
import com.mahmoud.clipkid.core.model.Collection
import com.mahmoud.clipkid.core.model.CoverMark
import com.mahmoud.clipkid.core.model.CoverStyle
import com.mahmoud.clipkid.core.model.Video
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Hand-written catalog for tests, previews, and builds without Supabase config (see
 * `DataConfig`): the 6 collections from CLAUDE.md with 9 videos each.
 *
 * Every YouTube id is an 11-character placeholder `REPLACE_<letter><nn>` (L letters, N numbers,
 * Q Quran & duas, M manners, C science, T stories). Search for `REPLACE_` to swap in real,
 * reviewed videos. Thumbnails will not load until then.
 *
 * For testing the real player before the backend exists, put real ids in
 * [DEBUG_VIDEO_ID_OVERRIDES]: they replace the placeholder ids in catalog order.
 */
class FakeCatalogDataSource(
    /**
     * When set, every fetch stamps the videos as refreshed now, so the app's 30-day rule never
     * empties the fake catalog. Tests leave it null and get the fixed [VIDEOS].
     */
    private val clock: Clock? = null,
) : CatalogRemoteDataSource {
    override suspend fun fetchCollections(): List<Collection> = COLLECTIONS

    override suspend fun fetchVideoManifest(): List<VideoStamp> = VIDEOS.map { VideoStamp(it.youtubeId, FAKE_UPDATED_AT) }

    override suspend fun fetchVideos(youtubeIds: List<String>): List<RemoteVideo> {
        val wanted = youtubeIds.toSet()
        val now = clock?.now()
        return VIDEOS.filter { it.youtubeId in wanted }
            .map { RemoteVideo(if (now == null) it else it.copy(refreshedAt = now), FAKE_UPDATED_AT) }
    }

    companion object {
        private val REFRESHED_AT = Instant.parse("2026-11-01T00:00:00Z")

        /** The fake never changes, so every row has the same server stamp. */
        const val FAKE_UPDATED_AT = "fake-v1"

        /**
         * Debug only (this whole catalog is a development fake). Real, embeddable, made-for-kids
         * YouTube ids that replace the `REPLACE_xx` placeholders in order: the first id goes to
         * letters #1, the 10th to numbers #1, and so on. Each id must be 11 characters
         * (`[A-Za-z0-9_-]`). Add one video with embedding disabled to see the "not available" state.
         *
         * TODO(Mahmoud): fill in before testing the player on a device. Keep it empty in commits
         * that ship.
         */
        val DEBUG_VIDEO_ID_OVERRIDES: List<String> = emptyList()

        // Shared tags (Arabic display labels) so Up Next can suggest across collections.
        private const val ANIMALS = "الحيوانات"
        private const val FOOD = "الأكل"
        private const val SONGS = "أغاني"
        private const val MORNING = "الصباح"
        private const val BEDTIME = "النوم"
        private const val SHORT_SURAHS = "سور قصيرة"
        private const val DUAS = "أدعية"
        private const val SPACE = "الفضاء"
        private const val NATURE = "الطبيعة"
        private const val HYGIENE = "النضافة"

        private val SMALL = setOf(A3_5)
        private val YOUNG = setOf(A3_5, A6_8)
        private val ALL = AgeBand.entries.toSet()
        private val OLDER = setOf(A6_8, A9_12)

        val COLLECTIONS: List<Collection> = listOf(
            collection("letters", "الحروف", 0xFFFFE6B0, 0xFF8A5A00, CoverMark.Glyph("أ ب"), SMALL, 1),
            collection("numbers", "الأرقام", 0xFFD6E8FF, 0xFF1A4FB5, CoverMark.Glyph("123"), YOUNG, 2),
            collection("quran-duas", "قرآن وأدعية", 0xFFD9F2E4, 0xFF1F7A4A, CoverMark.Icon("book"), ALL, 3),
            collection("manners", "أخلاق وسلوك", 0xFFFFDCD6, 0xFFB23A26, CoverMark.Icon("heart"), YOUNG, 4),
            collection("science", "علوم", 0xFFE6DEFF, 0xFF5B3DC4, CoverMark.Icon("leaf"), OLDER, 5),
            collection("stories", "حواديت", 0xFFFFE3C7, 0xFFA3530E, CoverMark.Icon("moon"), OLDER, 6),
        )

        val VIDEOS: List<Video> = placeholderVideos().mapIndexed { index, video ->
            val id = DEBUG_VIDEO_ID_OVERRIDES.getOrNull(index) ?: return@mapIndexed video
            video.copy(youtubeId = id, thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg")
        }

        private fun placeholderVideos(): List<Video> = buildList {
            videos(
                "letters", 'L', SMALL,
                "أغنية حرف الألف" to listOf(SONGS),
                "أغنية حرف الباء" to listOf(SONGS),
                "حرف التاء والتفاحة" to listOf(FOOD),
                "حرف الثاء والثعلب" to listOf(ANIMALS),
                "حرف الجيم والجمل" to listOf(ANIMALS),
                "حرف الحاء والحصان" to listOf(ANIMALS),
                "حرف الخاء والخروف" to listOf(ANIMALS, SONGS),
                "حرف الدال والديك" to listOf(ANIMALS, MORNING),
                "حرف الذال والذرة" to listOf(FOOD),
            )
            videos(
                "numbers", 'N', YOUNG,
                "نعد من 1 لـ 5" to listOf(SONGS),
                "نعد من 6 لـ 10" to listOf(SONGS),
                "نعد الحيوانات في المزرعة" to listOf(ANIMALS),
                "الأشكال حوالينا" to emptyList(),
                "نجمع بالتفاح" to listOf(FOOD),
                "الكبير والصغير" to listOf(ANIMALS),
                "نعد بالعشرات" to emptyList(),
                "الساعة كام؟" to listOf(MORNING),
                "نعد النجوم" to listOf(SPACE, BEDTIME),
            )
            videos(
                "quran-duas", 'Q', ALL,
                "سورة الفاتحة" to listOf(SHORT_SURAHS),
                "سورة الإخلاص" to listOf(SHORT_SURAHS),
                "سورة الفلق" to listOf(SHORT_SURAHS),
                "سورة الناس" to listOf(SHORT_SURAHS),
                "سورة الكوثر" to listOf(SHORT_SURAHS),
                "دعاء الصباح" to listOf(DUAS, MORNING),
                "دعاء قبل النوم" to listOf(DUAS, BEDTIME),
                "دعاء قبل الأكل" to listOf(DUAS, FOOD),
                "دعاء الخروج من البيت" to listOf(DUAS),
            )
            videos(
                "manners", 'M', YOUNG,
                "نقول شكراً ومن فضلك" to emptyList(),
                "نغسل إيدينا" to listOf(HYGIENE, FOOD),
                "نرتب أوضتنا" to emptyList(),
                "نساعد ماما" to emptyList(),
                "الصدق حلو" to emptyList(),
                "نسلّم على الناس" to listOf(MORNING),
                "نشارك لعبنا" to emptyList(),
                "نرفق بالحيوان" to listOf(ANIMALS),
                "نغسل سناننا قبل النوم" to listOf(HYGIENE, BEDTIME),
            )
            videos(
                "science", 'C', OLDER,
                "ليه القمر بيتغير شكله؟" to listOf(SPACE),
                "الكواكب حوالين الشمس" to listOf(SPACE),
                "رحلة نقطة المية" to listOf(NATURE),
                "النبات بيكبر إزاي؟" to listOf(NATURE, FOOD),
                "الحيوانات اللي بتطير" to listOf(ANIMALS, NATURE),
                "جسمنا من جوه" to listOf(HYGIENE),
                "ليه السما زرقا؟" to listOf(NATURE),
                "المغناطيس العجيب" to emptyList(),
                "البركان بيثور إزاي؟" to listOf(NATURE),
            )
            videos(
                "stories", 'T', OLDER,
                "الأرنب والسلحفاة" to listOf(ANIMALS),
                "الأسد والفأر" to listOf(ANIMALS),
                "النملة والجرادة" to listOf(ANIMALS),
                "النجمة الصغيرة" to listOf(BEDTIME, SPACE),
                "جحا وحماره" to listOf(ANIMALS),
                "الولد الصادق" to emptyList(),
                "الشجرة الكريمة" to listOf(NATURE),
                "رحلة السمكة الصغيرة" to listOf(ANIMALS, NATURE),
                "الديك الشجاع" to listOf(ANIMALS, MORNING),
            )
        }

        private fun collection(
            id: String,
            title: String,
            bg: Long,
            fg: Long,
            mark: CoverMark,
            bands: Set<AgeBand>,
            sortOrder: Int,
        ) = Collection(id, id, title, CoverStyle(bg, fg, mark), bands, sortOrder)

        private fun MutableList<Video>.videos(
            collectionId: String,
            code: Char,
            bands: Set<AgeBand>,
            vararg titlesAndTags: Pair<String, List<String>>,
        ) {
            titlesAndTags.forEachIndexed { index, (title, tags) ->
                val number = index + 1
                val youtubeId = "REPLACE_$code${number.toString().padStart(2, '0')}"
                add(
                    Video(
                        youtubeId = youtubeId,
                        collectionId = collectionId,
                        titleAr = title,
                        // 2:00 to 6:40, varied so duration badges and minute limits look realistic.
                        durationSec = 120 + ((number * 47 + code.code * 13) % 280),
                        thumbnailUrl = "https://i.ytimg.com/vi/$youtubeId/hqdefault.jpg",
                        tags = tags,
                        ageBands = bands,
                        sortOrder = number,
                        madeForKids = true,
                        embeddable = true,
                        refreshedAt = REFRESHED_AT,
                    ),
                )
            }
        }
    }
}
