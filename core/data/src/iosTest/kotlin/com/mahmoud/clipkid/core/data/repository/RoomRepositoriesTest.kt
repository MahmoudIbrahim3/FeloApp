package com.mahmoud.clipkid.core.data.repository

import app.cash.turbine.test
import com.mahmoud.clipkid.core.data.catalog.FakeCatalogDataSource
import com.mahmoud.clipkid.core.data.db.ClipKidDatabase
import com.mahmoud.clipkid.core.data.db.inMemoryClipKidDatabase
import com.mahmoud.clipkid.core.data.prefs.InMemoryPreferencesDataStore
import com.mahmoud.clipkid.core.domain.usecase.GetDailyQuotaUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetUpNextUseCase
import com.mahmoud.clipkid.core.domain.usecase.HideVideoUseCase
import com.mahmoud.clipkid.core.domain.usecase.ReportVideoUseCase
import com.mahmoud.clipkid.core.domain.usecase.StartWatchResult
import com.mahmoud.clipkid.core.domain.usecase.StartWatchUseCase
import com.mahmoud.clipkid.core.domain.usecase.UpNextResult
import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.core.model.ChildProfile
import com.mahmoud.clipkid.core.model.DailyLimit
import com.mahmoud.clipkid.core.model.Favorite
import com.mahmoud.clipkid.core.model.HiddenVideo
import com.mahmoud.clipkid.core.model.WatchEvent
import com.mahmoud.clipkid.core.model.WatchSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/** Room repositories against a real in-memory database (bundled SQLite). Runs on the iOS simulator. */
class RoomRepositoriesTest {

    private val db: ClipKidDatabase = inMemoryClipKidDatabase()
    private val profiles = RoomProfileRepository(db.profileDao())
    private val history = RoomWatchHistoryRepository(db.watchHistoryDao())
    private val favorites = RoomFavoritesRepository(db.favoritesDao())
    private val hidden = RoomHiddenVideosRepository(db.hiddenVideosDao())
    private val prefs = InMemoryPreferencesDataStore()
    private val now = Instant.parse("2026-11-04T14:00:00Z")
    private val fixedClock = object : Clock {
        override fun now(): Instant = now
    }
    private val catalog = OfflineFirstCatalogRepository(db.catalogDao(), FakeCatalogDataSource(), prefs, fixedClock)
    private val child = ChildProfile("c1", "يوسف", 0xFFFFB627, AgeBand.A4_6, setOf("letters", "numbers"), DailyLimit.Videos(3))

    @AfterTest
    fun tearDown() = db.close()

    private fun event(id: String, youtubeId: String, at: Instant, childId: String = "c1") =
        WatchEvent(id, childId, youtubeId, at, watchedSec = 0, completed = false, countsTowardLimit = true)

    @Test
    fun profilesUpsertObserveAndDeleteWithTheirData() = runTest {
        profiles.upsert(child)
        profiles.upsert(child.copy(id = "c2", nickname = "مريم"))
        profiles.upsert(child.copy(dailyLimit = DailyLimit.Minutes(30)))
        history.record(event("e1", "REPLACE_L01", now))
        favorites.add(Favorite("c1", "REPLACE_L01", now))
        hidden.hide(HiddenVideo("c1", "REPLACE_L02", now))
        history.addExtraVideos("c1", LocalDate(2026, 11, 4), 1)

        assertEquals(listOf("c1", "c2"), profiles.observeProfiles().first().map { it.id })
        assertEquals(DailyLimit.Minutes(30), profiles.getProfile("c1")?.dailyLimit)

        profiles.delete("c1")

        assertNull(profiles.getProfile("c1"))
        assertEquals(emptyList(), history.getEvents("c1", Instant.DISTANT_PAST, Instant.DISTANT_FUTURE))
        assertFalse(favorites.isFavorite("c1", "REPLACE_L01"))
        assertEquals(emptySet(), hidden.getHiddenIds("c1"))
        assertEquals(0, history.getExtraVideos("c1", LocalDate(2026, 11, 4)))
        assertEquals(listOf("c2"), profiles.observeProfiles().first().map { it.id })
    }

    @Test
    fun eventsAreQueriedByHalfOpenRangeOldestFirst() = runTest {
        history.record(event("b", "V2", now + 1.hours))
        history.record(event("a", "V1", now))
        history.record(event("c", "V3", now + 2.hours))
        history.record(event("other", "V1", now, childId = "c2"))

        val events = history.getEvents("c1", now, now + 2.hours)

        assertEquals(listOf("a", "b"), events.map { it.id })
    }

    @Test
    fun progressUpdatesAreObserved() = runTest {
        history.record(event("e1", "V1", now))
        history.observeEvents("c1", now, now + 1.hours).test {
            assertEquals(0, awaitItem().single().watchedSec)
            history.updateProgress("e1", watchedSec = 90, positionSec = 75, completed = true)
            val updated = awaitItem().single()
            assertEquals(90, updated.watchedSec)
            assertEquals(75, updated.positionSec)
            assertTrue(updated.completed)
        }
    }

    @Test
    fun extrasAddUpPerChildAndDay() = runTest {
        val today = LocalDate(2026, 11, 4)
        history.addExtraVideos("c1", today, 1)
        history.addExtraVideos("c1", today, 3)
        history.addExtraVideos("c1", LocalDate(2026, 11, 5), 1)

        assertEquals(4, history.getExtraVideos("c1", today))
        assertEquals(4, history.observeExtraVideos("c1", today).first())
        assertEquals(0, history.getExtraVideos("c2", today))
    }

    @Test
    fun favoritesNewestFirstAndRemovable() = runTest {
        favorites.add(Favorite("c1", "V1", now))
        favorites.add(Favorite("c1", "V2", now + 1.minutes))

        assertEquals(listOf("V2", "V1"), favorites.observeFavorites("c1").first().map { it.youtubeId })
        assertTrue(favorites.isFavorite("c1", "V1"))

        favorites.remove("c1", "V1")
        assertFalse(favorites.isFavorite("c1", "V1"))
    }

    @Test
    fun hidingTwiceKeepsOneRow() = runTest {
        hidden.hide(HiddenVideo("c1", "V1", now))
        hidden.hide(HiddenVideo("c1", "V1", now + 1.hours))

        assertEquals(setOf("V1"), hidden.observeHiddenIds("c1").first())
    }

    @Test
    fun catalogRefreshReplacesTheCacheAndRecordsTheTime() = runTest {
        assertNull(catalog.lastRefreshedAt())
        assertEquals(emptyList(), catalog.observeVideos().first())

        catalog.refresh(now)
        catalog.refresh(now + 1.hours)

        assertEquals(FakeCatalogDataSource.COLLECTIONS, catalog.observeCollections().first())
        assertEquals(FakeCatalogDataSource.VIDEOS.size, catalog.observeVideos().first().size)
        assertEquals(FakeCatalogDataSource.VIDEOS.toSet(), catalog.observeVideos().first().toSet())
        assertEquals(now + 1.hours, catalog.lastRefreshedAt())
    }

    @Test
    fun useCasesWorkEndToEndOnRoom() = runTest {
        val clock = object : Clock {
            override fun now(): Instant = now
        }
        val tz = TimeZone.of("Asia/Kuwait")
        catalog.refresh(now)
        profiles.upsert(child.copy(dailyLimit = DailyLimit.Videos(2)))
        var id = 0
        val start = StartWatchUseCase(profiles, history, { "e${id++}" }, clock, tz)
        val quota = GetDailyQuotaUseCase(profiles, history, clock, tz)
        val upNext = GetUpNextUseCase(catalog, profiles, history, hidden, quota, clock, tz)

        assertIs<StartWatchResult.Allowed>(start("c1", "REPLACE_L01", WatchSource.Home))
        HideVideoUseCase(hidden, favorites, ReportVideoUseCase(NoOpVideoReporter), clock)("c1", "REPLACE_L02")

        val choices = assertIs<UpNextResult.Choices>(upNext("c1", "REPLACE_L01"))
        assertEquals("REPLACE_L03", choices.primary?.youtubeId)
        assertTrue(choices.all.size <= 3)

        assertEquals(false, assertIs<StartWatchResult.Allowed>(start("c1", "REPLACE_L01", WatchSource.UpNextReplay)).countsTowardLimit)
        assertIs<StartWatchResult.Allowed>(start("c1", "REPLACE_L03", WatchSource.UpNext))
        assertEquals(StartWatchResult.LimitReached, start("c1", "REPLACE_L04", WatchSource.UpNext))
        assertEquals(UpNextResult.LimitReached, upNext("c1", "REPLACE_L03"))
        assertEquals(2, quota.current("c1")?.used)
    }
}
