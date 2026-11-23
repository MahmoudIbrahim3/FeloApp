package com.mahmoud.feelo.core.domain.testing

import com.mahmoud.feelo.core.model.AgeBand
import com.mahmoud.feelo.core.model.ChildProfile
import com.mahmoud.feelo.core.model.Collection
import com.mahmoud.feelo.core.model.CoverMark
import com.mahmoud.feelo.core.model.CoverStyle
import com.mahmoud.feelo.core.model.DailyLimit
import com.mahmoud.feelo.core.model.Video
import com.mahmoud.feelo.core.model.WatchEvent
import kotlin.time.Instant

const val CHILD = "child-1"
const val OTHER_CHILD = "child-2"

val ALL_BANDS: Set<AgeBand> = AgeBand.entries.toSet()

fun collection(
    id: String,
    sortOrder: Int,
    ageBands: Set<AgeBand> = ALL_BANDS,
) = Collection(
    id = id,
    slug = id,
    titleAr = "مجموعة $id",
    cover = CoverStyle(0xFFFFE6B0, 0xFF8A5A00, CoverMark.Glyph("أ")),
    ageBands = ageBands,
    sortOrder = sortOrder,
)

fun video(
    id: String,
    collectionId: String,
    sortOrder: Int,
    tags: List<String> = emptyList(),
    ageBands: Set<AgeBand> = ALL_BANDS,
    durationSec: Int = 180,
    embeddable: Boolean = true,
    madeForKids: Boolean = true,
) = Video(
    youtubeId = id,
    collectionId = collectionId,
    titleAr = "فيديو $id",
    durationSec = durationSec,
    thumbnailUrl = "https://i.ytimg.com/vi/$id/hqdefault.jpg",
    tags = tags,
    ageBands = ageBands,
    sortOrder = sortOrder,
    madeForKids = madeForKids,
    embeddable = embeddable,
    refreshedAt = Instant.fromEpochSeconds(0),
)

fun child(
    limit: DailyLimit = DailyLimit.Videos(3),
    ageBand: AgeBand = AgeBand.A6_8,
    allowed: Set<String> = setOf("letters", "numbers", "stories", "science"),
    id: String = CHILD,
) = ChildProfile(
    id = id,
    nickname = "يوسف",
    avatarColor = 0xFFFFB627,
    ageBand = ageBand,
    allowedCollectionIds = allowed,
    dailyLimit = limit,
)

fun event(
    youtubeId: String,
    startedAt: Instant,
    watchedSec: Int = 0,
    completed: Boolean = false,
    counts: Boolean = true,
    childId: String = CHILD,
    id: String = "$youtubeId@${startedAt.epochSeconds}",
    positionSec: Int = 0,
) = WatchEvent(id, childId, youtubeId, startedAt, watchedSec, completed, counts, positionSec)

const val ANIMALS = "الحيوانات"
const val COLORS = "الألوان"
const val MORNING = "الصباح"

/**
 * letters (all ages): L1..L5. numbers (6-12): N1..N3. stories (6-12): S1..S3.
 * science (9-12 only, so hidden from a 6-8 child): C1, C2. N3 is 9-12 only too.
 */
object Catalog {
    val collections = listOf(
        collection("stories", sortOrder = 3, ageBands = setOf(AgeBand.A6_8, AgeBand.A9_12)),
        collection("letters", sortOrder = 1),
        collection("numbers", sortOrder = 2, ageBands = setOf(AgeBand.A6_8, AgeBand.A9_12)),
        collection("science", sortOrder = 4, ageBands = setOf(AgeBand.A9_12)),
    )

    val videos = listOf(
        video("L1", "letters", 1, tags = listOf(MORNING)),
        video("L2", "letters", 2, tags = listOf(ANIMALS, COLORS)),
        video("L3", "letters", 3, tags = listOf(ANIMALS)),
        video("L4", "letters", 4, tags = listOf(COLORS)),
        video("L5", "letters", 5),
        video("N1", "numbers", 1, tags = listOf(ANIMALS)),
        video("N2", "numbers", 2, tags = listOf(COLORS)),
        video("N3", "numbers", 3, ageBands = setOf(AgeBand.A9_12)),
        video("S1", "stories", 1, tags = listOf(ANIMALS)),
        video("S2", "stories", 2, tags = listOf(ANIMALS, MORNING)),
        video("S3", "stories", 3, tags = listOf(MORNING)),
        video("C1", "science", 1, tags = listOf(ANIMALS)),
        video("C2", "science", 2, tags = listOf(COLORS)),
    )

    fun repository() = FakeCatalogRepository(collections, videos)
}
