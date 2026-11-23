package com.mahmoud.feelo.core.data.catalog

import com.mahmoud.feelo.core.model.Collection
import com.mahmoud.feelo.core.model.CoverMark
import com.mahmoud.feelo.core.model.Video
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeCatalogDataSourceTest {

    private data class Catalog(val collections: List<Collection>, val videos: List<Video>)

    private val catalog = FakeCatalogDataSource.let { Catalog(it.COLLECTIONS, it.VIDEOS) }

    @Test
    fun hasTheSixCollectionsFromClaudeMdWithTheirCoverColors() {
        val covers = catalog.collections.associate { it.slug to (it.cover.bg to it.cover.fg) }
        assertEquals(
            mapOf(
                "letters" to (0xFFFFE6B0 to 0xFF8A5A00),
                "numbers" to (0xFFD6E8FF to 0xFF1A4FB5),
                "quran-duas" to (0xFFD9F2E4 to 0xFF1F7A4A),
                "manners" to (0xFFFFDCD6 to 0xFFB23A26),
                "science" to (0xFFE6DEFF to 0xFF5B3DC4),
                "stories" to (0xFFFFE3C7 to 0xFFA3530E),
            ),
            covers,
        )
        assertEquals((1..6).toList(), catalog.collections.map { it.sortOrder })
    }

    @Test
    fun coverIconsUseBundledKeys() {
        val bundled = setOf("book", "heart", "leaf", "moon", "star")
        catalog.collections.map { it.cover.mark }.filterIsInstance<CoverMark.Icon>().forEach {
            assertTrue(it.key in bundled, it.key)
        }
    }

    @Test
    fun eachCollectionHasEightToTenVideosInOrder() {
        val byCollection = catalog.videos.groupBy { it.collectionId }
        assertEquals(catalog.collections.map { it.id }.toSet(), byCollection.keys)
        byCollection.forEach { (id, videos) ->
            assertTrue(videos.size in 8..10, "$id has ${videos.size}")
            assertEquals((1..videos.size).toList(), videos.map { it.sortOrder })
        }
    }

    @Test
    fun placeholderIdsAreUniqueElevenCharsAndEasyToFind() {
        val ids = catalog.videos.map { it.youtubeId }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach {
            assertEquals(11, it.length, it)
            assertTrue(it.startsWith("REPLACE_"), it)
        }
    }

    @Test
    fun videosAreValidForTheirCollection() {
        val collections = catalog.collections.associateBy { it.id }
        catalog.videos.forEach { video ->
            val collection = collections.getValue(video.collectionId)
            assertTrue(video.ageBands.isNotEmpty() && collection.ageBands.containsAll(video.ageBands), video.youtubeId)
            assertTrue(video.titleAr.isNotBlank())
            assertTrue(video.durationSec in 60..600)
            assertTrue(video.embeddable && video.madeForKids)
            assertTrue(video.thumbnailUrl.endsWith("/${video.youtubeId}/hqdefault.jpg"))
        }
    }

    @Test
    fun tagsConnectCollectionsSoUpNextHasSuggestions() {
        val collectionsPerTag = catalog.videos
            .flatMap { v -> v.tags.map { it to v.collectionId } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }
        assertTrue(collectionsPerTag.values.count { it.size >= 2 } >= 3)
    }

    @Test
    fun fetchReturnsTheSameCatalog() = runTest {
        val source = FakeCatalogDataSource()
        val manifest = source.fetchVideoManifest()
        assertEquals(catalog.collections, source.fetchCollections())
        assertEquals(catalog.videos.map { it.youtubeId }, manifest.map { it.youtubeId })
        assertEquals(catalog.videos, source.fetchVideos(manifest.map { it.youtubeId }).map { it.video })
    }

    @Test
    fun withAClock_videosAreStampedAsRefreshedNow() = runTest {
        val now = Instant.parse("2027-03-01T10:00:00Z")
        val source = FakeCatalogDataSource(clock = object : Clock { override fun now() = now })

        val videos = source.fetchVideos(listOf("REPLACE_L01", "REPLACE_N02", "NOT_LISTED_"))

        assertEquals(listOf("REPLACE_L01", "REPLACE_N02"), videos.map { it.video.youtubeId })
        assertTrue(videos.all { it.video.refreshedAt == now })
    }
}
