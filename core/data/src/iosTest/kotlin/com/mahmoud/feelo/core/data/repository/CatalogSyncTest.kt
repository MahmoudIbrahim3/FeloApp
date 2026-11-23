package com.mahmoud.feelo.core.data.repository

import com.mahmoud.feelo.core.data.catalog.CatalogRemoteDataSource
import com.mahmoud.feelo.core.data.catalog.FakeCatalogDataSource
import com.mahmoud.feelo.core.data.catalog.RemoteVideo
import com.mahmoud.feelo.core.data.catalog.VideoStamp
import com.mahmoud.feelo.core.data.db.FeeloDatabase
import com.mahmoud.feelo.core.data.db.inMemoryFeeloDatabase
import com.mahmoud.feelo.core.data.prefs.InMemoryPreferencesDataStore
import com.mahmoud.feelo.core.model.Collection
import com.mahmoud.feelo.core.model.Video
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

/** Incremental Supabase-style sync against a real in-memory Room database. Runs on the iOS simulator. */
class CatalogSyncTest {

    /** A backend we control: the visible rows and their updated_at, plus a log of what was fetched. */
    private class ScriptedRemote : CatalogRemoteDataSource {
        var collections: List<Collection> = FakeCatalogDataSource.COLLECTIONS
        var videos: Map<String, RemoteVideo> = emptyMap()
        val fetchedIds = mutableListOf<List<String>>()

        override suspend fun fetchCollections() = collections
        override suspend fun fetchVideoManifest() = videos.values.map { VideoStamp(it.video.youtubeId, it.updatedAt) }
        override suspend fun fetchVideos(youtubeIds: List<String>): List<RemoteVideo> {
            fetchedIds += youtubeIds
            return youtubeIds.mapNotNull { videos[it] }
        }
    }

    private val now = Instant.parse("2026-11-06T10:00:00Z")
    private var clockNow = now
    private val clock = object : Clock {
        override fun now(): Instant = clockNow
    }
    private val db: FeeloDatabase = inMemoryFeeloDatabase()
    private val prefs = InMemoryPreferencesDataStore()
    private val remote = ScriptedRemote()
    private val catalog = OfflineFirstCatalogRepository(db.catalogDao(), remote, prefs, clock)

    private val base: Video = FakeCatalogDataSource.VIDEOS.first().copy(refreshedAt = now - 1.days)

    private fun video(id: String, title: String = id, refreshedAt: Instant = now - 1.days) =
        base.copy(youtubeId = id, titleAr = title, refreshedAt = refreshedAt)

    private fun serve(vararg rows: Pair<Video, String>) {
        remote.videos = rows.associate { (video, stamp) -> video.youtubeId to RemoteVideo(video, stamp) }
    }

    @AfterTest
    fun tearDown() = db.close()

    @Test
    fun onlyNewAndChangedRowsAreFetched_andRemovedRowsAreDeleted() = runTest {
        serve(video("aaaaaaaaaaa") to "t1", video("bbbbbbbbbbb") to "t1", video("ccccccccccc") to "t1")
        catalog.refresh(now)
        assertEquals(listOf(listOf("aaaaaaaaaaa", "bbbbbbbbbbb", "ccccccccccc")), remote.fetchedIds)

        remote.fetchedIds.clear()
        serve(
            video("aaaaaaaaaaa") to "t1",
            video("bbbbbbbbbbb", title = "جديد") to "t2",
            video("ddddddddddd") to "t1",
        ) // c was unpublished / became unavailable
        catalog.refresh(now)

        assertEquals(listOf(listOf("bbbbbbbbbbb", "ddddddddddd")), remote.fetchedIds)
        val cached = catalog.observeVideos().first().associateBy { it.youtubeId }
        assertEquals(setOf("aaaaaaaaaaa", "bbbbbbbbbbb", "ddddddddddd"), cached.keys)
        assertEquals("جديد", cached.getValue("bbbbbbbbbbb").titleAr)
    }

    @Test
    fun nothingChanged_nothingFetched() = runTest {
        serve(video("aaaaaaaaaaa") to "t1")
        catalog.refresh(now)
        remote.fetchedIds.clear()

        catalog.refresh(now)

        assertEquals(emptyList(), remote.fetchedIds)
        assertEquals(1, catalog.observeVideos().first().size)
    }

    @Test
    fun theFirstSync_replacesWhateverWasCachedBefore() = runTest {
        // A cache from the fake catalog (before Supabase): the full resync clears it.
        OfflineFirstCatalogRepository(db.catalogDao(), FakeCatalogDataSource(), InMemoryPreferencesDataStore(), clock)
            .refresh(now)
        serve(video("aaaaaaaaaaa") to "t1")

        catalog.refresh(now)

        assertEquals(listOf("aaaaaaaaaaa"), catalog.observeVideos().first().map { it.youtubeId })
    }

    @Test
    fun youTubeDataOlderThan30Days_isNeverShown_evenOffline() = runTest {
        serve(video("aaaaaaaaaaa", refreshedAt = now - 29.days) to "t1", video("bbbbbbbbbbb") to "t1")
        catalog.refresh(now)
        assertEquals(2, catalog.observeVideos().first().size)

        // Two days later, offline: the first video's data is now 31 days old.
        clockNow = now + 2.days
        assertEquals(listOf("bbbbbbbbbbb"), catalog.observeVideos().first().map { it.youtubeId })

        remote.videos = emptyMap()
        val offline = object : CatalogRemoteDataSource by remote {
            override suspend fun fetchCollections(): List<Collection> = error("offline")
        }
        runCatching { OfflineFirstCatalogRepository(db.catalogDao(), offline, prefs, clock).refresh(clockNow) }
        assertEquals(listOf("bbbbbbbbbbb"), db.catalogDao().videoStamps().map { it.youtubeId })
    }

    @Test
    fun collectionsAreReplacedWhole() = runTest {
        serve()
        catalog.refresh(now)
        assertEquals(FakeCatalogDataSource.COLLECTIONS, catalog.observeCollections().first())

        remote.collections = FakeCatalogDataSource.COLLECTIONS.take(2)
        catalog.refresh(now)

        assertEquals(FakeCatalogDataSource.COLLECTIONS.take(2), catalog.observeCollections().first())
    }
}
