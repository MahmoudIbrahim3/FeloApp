package com.mahmoud.clipkid.core.domain.usecase

import app.cash.turbine.test
import com.mahmoud.clipkid.core.domain.testing.CHILD
import com.mahmoud.clipkid.core.domain.testing.Catalog
import com.mahmoud.clipkid.core.domain.testing.FakeCatalogRepository
import com.mahmoud.clipkid.core.domain.testing.FakeHiddenVideosRepository
import com.mahmoud.clipkid.core.domain.testing.FakeProfileRepository
import com.mahmoud.clipkid.core.domain.testing.FakeWatchHistoryRepository
import com.mahmoud.clipkid.core.domain.testing.KUWAIT
import com.mahmoud.clipkid.core.domain.testing.TestClock
import com.mahmoud.clipkid.core.domain.testing.child
import com.mahmoud.clipkid.core.domain.testing.collection
import com.mahmoud.clipkid.core.domain.testing.event
import com.mahmoud.clipkid.core.domain.testing.kuwait
import com.mahmoud.clipkid.core.domain.testing.video
import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.core.model.ChildProfile
import com.mahmoud.clipkid.core.model.DailyLimit
import com.mahmoud.clipkid.core.model.HiddenVideo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class GetHomeFeedUseCaseTest {

    private val start = kuwait(2026, 11, 4, 17, 0)
    private val history = FakeWatchHistoryRepository()
    private val hidden = FakeHiddenVideosRepository()

    private fun TestScope.homeFeed(
        profile: ChildProfile = child(DailyLimit.Videos(3)),
        catalog: FakeCatalogRepository = Catalog.repository(),
        origin: Instant = start,
    ) = GetHomeFeedUseCase(catalog, FakeProfileRepository(profile), history, hidden, TestClock(testScheduler, origin), KUWAIT)

    @Test
    fun oneShelfPerAllowedCollectionMatchingTheAgeBand_inSortOrder() = runTest {
        val feed = homeFeed()(CHILD).first()

        // science is 7-12 only; the child is 4-6.
        assertEquals(listOf("letters", "numbers", "stories"), feed.collections.map { it.id })
        assertEquals(listOf("letters", "numbers", "stories"), feed.shelves.map { it.collection.id })
        assertEquals(listOf("L1", "L2", "L3", "L4", "L5"), feed.shelves[0].videos.map { it.youtubeId })
        // N3 is 7-9 only.
        assertEquals(listOf("N1", "N2"), feed.shelves[1].videos.map { it.youtubeId })
    }

    @Test
    fun collectionsTheParentDidNotAllowAreLeftOut() = runTest {
        val feed = homeFeed(child(allowed = setOf("stories")))(CHILD).first()

        assertEquals(listOf("stories"), feed.shelves.map { it.collection.id })
        assertTrue(feed.newToday.all { it.collectionId == "stories" })
    }

    @Test
    fun olderChildSeesTheirBand() = runTest {
        val feed = homeFeed(child(ageBand = AgeBand.A7_9))(CHILD).first()

        assertEquals(listOf("letters", "numbers", "stories", "science"), feed.shelves.map { it.collection.id })
        assertEquals(listOf("N1", "N2", "N3"), feed.shelves[1].videos.map { it.youtubeId })
    }

    @Test
    fun emptyShelvesAreDropped() = runTest {
        val catalog = FakeCatalogRepository(
            Catalog.collections + collection("empty", sortOrder = 0),
            Catalog.videos,
        )
        val feed = homeFeed(child(allowed = setOf("empty", "letters")), catalog)(CHILD).first()

        assertEquals(listOf("empty", "letters"), feed.collections.map { it.id })
        assertEquals(listOf("letters"), feed.shelves.map { it.collection.id })
    }

    @Test
    fun hiddenVideosAppearNowhere() = runTest {
        history.events.value = listOf(event("L2", start - 1.hours, watchedSec = 60))
        hidden.hidden.value = listOf(HiddenVideo(CHILD, "L2", start), HiddenVideo(CHILD, "N1", start))

        val feed = homeFeed()(CHILD).first()

        val everywhere = feed.shelves.flatMap { it.videos } + feed.newToday + listOfNotNull(feed.continueWatching?.video)
        assertTrue(everywhere.none { it.youtubeId == "L2" || it.youtubeId == "N1" })
        assertNull(feed.continueWatching)
    }

    @Test
    fun feedUpdatesWhenAParentHidesAVideo() = runTest {
        homeFeed()(CHILD).test {
            assertTrue(awaitItem().shelves[0].videos.any { it.youtubeId == "L1" })
            hidden.hide(HiddenVideo(CHILD, "L1", start))
            assertTrue(awaitItem().shelves[0].videos.none { it.youtubeId == "L1" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun continueWatching_isTheMostRecentUnfinishedVideo() = runTest {
        history.events.value = listOf(
            event("L1", start - 5.hours, watchedSec = 100),
            event("S1", start - 2.hours, watchedSec = 90),
            event("L3", start - 1.hours, watchedSec = 180, completed = true),
        )

        val cw = homeFeed()(CHILD).first().continueWatching

        assertEquals("S1", cw?.video?.youtubeId)
        assertEquals(90, cw?.watchedSec)
        assertEquals(0.5f, cw?.progress)
    }

    @Test
    fun continueWatching_ignoresAVideoFinishedLaterOrBarelyStarted() = runTest {
        history.events.value = listOf(
            event("L1", start - 5.hours, watchedSec = 100),
            event("L1", start - 3.hours, watchedSec = 180, completed = true),
            event("L2", start - 1.hours, watchedSec = 4),
        )

        assertNull(homeFeed()(CHILD).first().continueWatching)
    }

    @Test
    fun continueWatching_looksBackSevenDaysOnly() = runTest {
        history.events.value = listOf(event("L1", kuwait(2026, 10, 27, 23, 0), watchedSec = 100))
        assertNull(homeFeed()(CHILD).first().continueWatching)

        history.events.value = listOf(event("L1", kuwait(2026, 10, 28, 0, 30), watchedSec = 100))
        assertEquals("L1", homeFeed()(CHILD).first().continueWatching?.video?.youtubeId)
    }

    @Test
    fun newToday_isAtMostSixVisibleVideos() = runTest {
        val feed = homeFeed()(CHILD).first()
        val visible = feed.shelves.flatMap { it.videos }.map { it.youtubeId }.toSet()

        assertEquals(GetHomeFeedUseCase.NEW_TODAY_SIZE, feed.newToday.size)
        assertTrue(feed.newToday.all { it.youtubeId in visible })
        assertEquals(feed.newToday.distinct(), feed.newToday)
    }

    @Test
    fun newToday_staysTheSameAllDay_andChangesAfterMidnight() = runTest {
        homeFeed(origin = kuwait(2026, 11, 4, 22, 0))(CHILD).test {
            val morning = awaitItem().newToday.map { it.youtubeId }

            // Watching something today must not reshuffle the shelf.
            history.record(event(morning.first(), kuwait(2026, 11, 4, 22, 0), watchedSec = 180, completed = true))
            assertEquals(morning, awaitItem().newToday.map { it.youtubeId })

            testScheduler.advanceTimeBy(2.hours + 1.minutes)
            val tomorrow = awaitItem().newToday.map { it.youtubeId }
            assertNotEquals(morning, tomorrow)
            // Completed before today, so it is left out tomorrow.
            assertTrue(morning.first() !in tomorrow)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun newToday_leavesOutVideosCompletedBeforeToday_andFallsBackWhenAllSeen() = runTest {
        val catalog = FakeCatalogRepository(
            listOf(collection("letters", 1)),
            listOf(video("A", "letters", 1), video("B", "letters", 2), video("C", "letters", 3)),
        )
        val profile = child(allowed = setOf("letters"))
        history.events.value = listOf(
            event("A", kuwait(2026, 11, 3, 10, 0), watchedSec = 180, completed = true),
            event("B", kuwait(2026, 11, 3, 11, 0), watchedSec = 180, completed = true),
            // Unfinished: still "new".
            event("C", kuwait(2026, 11, 3, 12, 0), watchedSec = 20),
        )
        assertEquals(listOf("C"), homeFeed(profile, catalog)(CHILD).first().newToday.map { it.youtubeId })

        history.events.value += event("C", kuwait(2026, 11, 3, 13, 0), watchedSec = 180, completed = true)
        assertEquals(setOf("A", "B", "C"), homeFeed(profile, catalog)(CHILD).first().newToday.map { it.youtubeId }.toSet())
    }

    @Test
    fun dailySeedIsStableAndDependsOnChildAndDate() {
        val d = LocalDate(2026, 11, 4)
        assertEquals(GetHomeFeedUseCase.dailySeed("a", d), GetHomeFeedUseCase.dailySeed("a", d))
        assertNotEquals(GetHomeFeedUseCase.dailySeed("a", d), GetHomeFeedUseCase.dailySeed("b", d))
        assertNotEquals(GetHomeFeedUseCase.dailySeed("a", d), GetHomeFeedUseCase.dailySeed("a", LocalDate(2026, 11, 5)))
    }
}
