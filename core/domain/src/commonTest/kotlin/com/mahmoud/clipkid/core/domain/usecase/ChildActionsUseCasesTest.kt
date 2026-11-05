package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.testing.CHILD
import com.mahmoud.clipkid.core.domain.testing.FakeCatalogRepository
import com.mahmoud.clipkid.core.domain.testing.FakeFavoritesRepository
import com.mahmoud.clipkid.core.domain.testing.FakeVideoReporter
import com.mahmoud.clipkid.core.domain.testing.FakeHiddenVideosRepository
import com.mahmoud.clipkid.core.domain.testing.FakeWatchHistoryRepository
import com.mahmoud.clipkid.core.domain.testing.FixedClock
import com.mahmoud.clipkid.core.domain.testing.KUWAIT
import com.mahmoud.clipkid.core.domain.testing.OTHER_CHILD
import com.mahmoud.clipkid.core.domain.testing.kuwait
import com.mahmoud.clipkid.core.model.Favorite
import com.mahmoud.clipkid.core.model.HiddenVideo
import com.mahmoud.clipkid.core.model.VideoReport
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class ChildActionsUseCasesTest {

    private val now = kuwait(2026, 11, 4, 17, 0)
    private val clock = FixedClock(now)

    @Test
    fun grantExtraVideos_addsToTodayOfThatChild() = runTest {
        val history = FakeWatchHistoryRepository()
        val grant = GrantExtraVideosUseCase(history, clock, KUWAIT)

        grant(CHILD, 1)
        grant(CHILD, 3)

        assertEquals(4, history.getExtraVideos(CHILD, LocalDate(2026, 11, 4)))
        assertEquals(0, history.getExtraVideos(OTHER_CHILD, LocalDate(2026, 11, 4)))
        assertFailsWith<IllegalArgumentException> { grant(CHILD, 0) }
    }

    @Test
    fun hideVideo_savesLocally_removesFavorite_andReportsOnlyIdAndReason() = runTest {
        val hidden = FakeHiddenVideosRepository()
        val favorites = FakeFavoritesRepository()
        favorites.add(Favorite(CHILD, "L1", now))
        favorites.add(Favorite(OTHER_CHILD, "L1", now))
        val reporter = FakeVideoReporter()

        HideVideoUseCase(hidden, favorites, ReportVideoUseCase(reporter), clock)(CHILD, "L1")

        assertEquals(listOf(HiddenVideo(CHILD, "L1", now)), hidden.hidden.value)
        assertFalse(favorites.isFavorite(CHILD, "L1"))
        assertTrue(favorites.isFavorite(OTHER_CHILD, "L1"))
        assertEquals(listOf(VideoReport("L1", "parent_hidden")), reporter.reports)
        assertEquals(
            """{"youtube_id":"L1","reason":"parent_hidden"}""",
            Json { encodeDefaults = true }.encodeToString(VideoReport.serializer(), reporter.reports.single()),
        )
    }

    @Test
    fun hideVideo_staysHiddenWhenTheReportFails() = runTest {
        val hidden = FakeHiddenVideosRepository()
        val reporter = FakeVideoReporter().apply { error = IllegalStateException("offline") }

        HideVideoUseCase(hidden, FakeFavoritesRepository(), ReportVideoUseCase(reporter), clock)(CHILD, "L1")

        assertEquals(setOf("L1"), hidden.getHiddenIds(CHILD))
    }

    @Test
    fun toggleFavorite_addsThenRemoves() = runTest {
        val favorites = FakeFavoritesRepository()
        val toggle = ToggleFavoriteUseCase(favorites, clock)

        assertTrue(toggle(CHILD, "L1"))
        assertEquals(listOf(Favorite(CHILD, "L1", now)), favorites.favorites.value)
        assertFalse(toggle(CHILD, "L1"))
        assertTrue(favorites.favorites.value.isEmpty())
    }

    @Test
    fun syncCatalog_runsEveryTime_withoutAMaxAge() = runTest {
        val catalog = FakeCatalogRepository().apply { lastRefreshed = now - 1.hours }
        val sync = SyncCatalogUseCase(catalog, clock)

        assertEquals(CatalogSync.Synced, sync())
        assertEquals(CatalogSync.Synced, sync())
        assertEquals(2, catalog.refreshCalls)
        assertEquals(now, catalog.lastRefreshed)
    }

    @Test
    fun syncCatalog_failureKeepsTheCache() = runTest {
        val old = now - 30.hours
        val catalog = FakeCatalogRepository().apply {
            lastRefreshed = old
            refreshError = IllegalStateException("offline")
        }

        assertIs<CatalogSync.Failed>(SyncCatalogUseCase(catalog, clock)())
        assertEquals(old, catalog.lastRefreshed)
    }
}
