package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.testing.CHILD
import com.mahmoud.clipkid.core.domain.testing.FakeCatalogRepository
import com.mahmoud.clipkid.core.domain.testing.FakeFavoritesRepository
import com.mahmoud.clipkid.core.domain.testing.FakeHiddenVideoReporter
import com.mahmoud.clipkid.core.domain.testing.FakeHiddenVideosRepository
import com.mahmoud.clipkid.core.domain.testing.FakeWatchHistoryRepository
import com.mahmoud.clipkid.core.domain.testing.FixedClock
import com.mahmoud.clipkid.core.domain.testing.KUWAIT
import com.mahmoud.clipkid.core.domain.testing.OTHER_CHILD
import com.mahmoud.clipkid.core.domain.testing.kuwait
import com.mahmoud.clipkid.core.model.Favorite
import com.mahmoud.clipkid.core.model.HiddenVideo
import com.mahmoud.clipkid.core.model.HiddenVideoReport
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
        val reporter = FakeHiddenVideoReporter()

        HideVideoUseCase(hidden, favorites, reporter, clock)(CHILD, "L1")

        assertEquals(listOf(HiddenVideo(CHILD, "L1", now)), hidden.hidden.value)
        assertFalse(favorites.isFavorite(CHILD, "L1"))
        assertTrue(favorites.isFavorite(OTHER_CHILD, "L1"))
        assertEquals(listOf(HiddenVideoReport("L1", "parent_hidden")), reporter.reports)
        assertEquals(
            """{"youtube_id":"L1","reason":"parent_hidden"}""",
            Json { encodeDefaults = true }.encodeToString(HiddenVideoReport.serializer(), reporter.reports.single()),
        )
    }

    @Test
    fun hideVideo_staysHiddenWhenTheReportFails() = runTest {
        val hidden = FakeHiddenVideosRepository()
        val reporter = FakeHiddenVideoReporter().apply { error = IllegalStateException("offline") }

        HideVideoUseCase(hidden, FakeFavoritesRepository(), reporter, clock)(CHILD, "L1")

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
    fun refreshCatalog_whenNeverFetched() = runTest {
        val catalog = FakeCatalogRepository()

        assertEquals(CatalogRefresh.Refreshed, RefreshCatalogIfStaleUseCase(catalog, clock)())
        assertEquals(now, catalog.lastRefreshed)
    }

    @Test
    fun refreshCatalog_onlyWhenOlderThan24Hours() = runTest {
        val catalog = FakeCatalogRepository().apply { lastRefreshed = now - 23.hours }
        val refresh = RefreshCatalogIfStaleUseCase(catalog, clock)

        assertEquals(CatalogRefresh.Fresh, refresh())
        assertEquals(0, catalog.refreshCalls)

        clock.instant = now + 1.hours
        assertEquals(CatalogRefresh.Refreshed, refresh())
        assertEquals(1, catalog.refreshCalls)
    }

    @Test
    fun refreshCatalog_forceIgnoresAge() = runTest {
        val catalog = FakeCatalogRepository().apply { lastRefreshed = now }

        assertEquals(CatalogRefresh.Refreshed, RefreshCatalogIfStaleUseCase(catalog, clock)(force = true))
    }

    @Test
    fun refreshCatalog_failureKeepsTheCache() = runTest {
        val old = now - 30.hours
        val catalog = FakeCatalogRepository().apply {
            lastRefreshed = old
            refreshError = IllegalStateException("offline")
        }

        assertIs<CatalogRefresh.Failed>(RefreshCatalogIfStaleUseCase(catalog, clock)())
        assertEquals(old, catalog.lastRefreshed)
    }
}
