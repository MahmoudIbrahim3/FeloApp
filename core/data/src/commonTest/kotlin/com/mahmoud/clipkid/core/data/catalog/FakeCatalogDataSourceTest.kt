package com.mahmoud.clipkid.core.data.catalog

import com.mahmoud.clipkid.core.model.CoverMark
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeCatalogDataSourceTest {

    private val catalog = FakeCatalogDataSource.let { RemoteCatalog(it.COLLECTIONS, it.VIDEOS) }

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
        assertEquals(catalog, FakeCatalogDataSource().fetchCatalog())
    }
}
