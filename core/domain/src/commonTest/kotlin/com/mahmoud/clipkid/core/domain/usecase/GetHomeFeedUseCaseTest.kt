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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate

class GetHomeFeedUseCaseTest {

    private val start = kuwait(2026, 11, 4, 17, 0)
    private val history = FakeWatchHistoryRepository()
    private val hidden = FakeHiddenVideosRepository()

    private fun TestScope.homeFeed(
        profile: ChildProfile = child(DailyLimit.Videos(3)),
        catalog: FakeCatalogRepository = Catalog.repository(),
        origin: Instant = start,
    ) = GetHomeFeedUseCase(catalog, FakeProfileRepository(profile), history, hidden, TestClock(testScheduler, origin), KUWAIT)

    /**
     * What the shelves should hold: each collection's videos in [order] (not finished first),
     * minus what Home already shows above them, at most SHELF_SIZE, empty shelves dropped.
     */
    private fun expectedShelves(feed: HomeFeed, order: Map<String, List<String>>): Map<String, List<String>> {
        val shown = feed.newToday.map { it.youtubeId }.toSet() + listOfNotNull(feed.continueWatching?.video?.youtubeId)
        return order
            .mapValues { (_, ids) -> ids.filterNot { it in shown }.take(GetHomeFeedUseCase.SHELF_SIZE) }
            .filterValues { it.isNotEmpty() }
    }

    private fun HomeFeed.shelfIds(): Map<String, List<String>> =
        shelves.associate { shelf -> shelf.collection.id to shelf.videos.map { it.youtubeId } }

    private fun HomeFeed.everythingShown(): List<String> =
        listOfNotNull(continueWatching?.video?.youtubeId) + newToday.map { it.youtubeId } +
            shelves.flatMap { shelf -> shelf.videos.map { it.youtubeId } }

    @Test
    fun oneShelfPerAllowedCollectionMatchingTheAgeBand_inSortOrder() = runTest {
        val feed = homeFeed()(CHILD).first()

        // science is 9-12 only; the child is 6-8. N3 is 9-12 only.
        assertEquals(listOf("letters", "numbers", "stories"), feed.collections.map { it.id })
        val expected = expectedShelves(
            feed,
            linkedMapOf(
                "letters" to listOf("L1", "L2", "L3", "L4", "L5"),
                "numbers" to listOf("N1", "N2"),
                "stories" to listOf("S1", "S2", "S3"),
            ),
        )
        assertEquals(expected.keys.toList(), feed.shelves.map { it.collection.id })
        assertEquals(expected, feed.shelfIds())
    }

    @Test
    fun collectionsTheParentDidNotAllowAreLeftOut() = runTest {
        val feed = homeFeed(child(allowed = setOf("stories")))(CHILD).first()

        assertTrue(feed.shelves.all { it.collection.id == "stories" })
        assertTrue(feed.newToday.all { it.collectionId == "stories" })
    }

    @Test
    fun olderChildSeesTheirBand() = runTest {
        val feed = homeFeed(child(ageBand = AgeBand.A9_12))(CHILD).first()

        assertEquals(listOf("letters", "numbers", "stories", "science"), feed.collections.map { it.id })
        val expected = expectedShelves(
            feed,
            linkedMapOf(
                "letters" to listOf("L1", "L2", "L3", "L4", "L5"),
                "numbers" to listOf("N1", "N2", "N3"),
                "stories" to listOf("S1", "S2", "S3"),
                "science" to listOf("C1", "C2"),
            ),
        )
        assertEquals(expected.keys.toList(), feed.shelves.map { it.collection.id })
        assertEquals(expected, feed.shelfIds())
    }

    @Test
    fun emptyShelvesAreDropped() = runTest {
        val catalog = FakeCatalogRepository(
            Catalog.collections + collection("empty", sortOrder = 0),
            Catalog.videos,
        )
        val feed = homeFeed(child(allowed = setOf("empty", "letters")), catalog)(CHILD).first()

        assertEquals(listOf("empty", "letters"), feed.collections.map { it.id })
        // letters has 5 videos: 3 go to "جديد النهارده", the other 2 stay on its shelf.
        assertEquals(listOf("letters"), feed.shelves.map { it.collection.id })
        assertEquals(2, feed.shelves.single().videos.size)
    }

    @Test
    fun hiddenVideosAppearNowhere() = runTest {
        history.events.value = listOf(event("L2", start - 1.hours, watchedSec = 60))
        hidden.hidden.value = listOf(HiddenVideo(CHILD, "L2", start), HiddenVideo(CHILD, "N1", start))

        val feed = homeFeed()(CHILD).first()

        assertTrue(feed.everythingShown().none { it == "L2" || it == "N1" })
        assertNull(feed.continueWatching)
    }

    @Test
    fun feedUpdatesWhenAParentHidesAVideo() = runTest {
        homeFeed()(CHILD).test {
            // 10 visible videos, 3 + 3 + 2 on Home: L1 (first of the first shelf) is always shown.
            assertTrue("L1" in awaitItem().everythingShown())
            hidden.hide(HiddenVideo(CHILD, "L1", start))
            assertTrue("L1" !in awaitItem().everythingShown())
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
    fun newToday_isThreeVisibleVideos() = runTest {
        val feed = homeFeed()(CHILD).first()
        val visible = setOf("L1", "L2", "L3", "L4", "L5", "N1", "N2", "S1", "S2", "S3")

        assertEquals(3, GetHomeFeedUseCase.NEW_TODAY_SIZE)
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
    fun shelf_isTheNextUnfinishedVideosInCollectionOrder() {
        val videos = (1..5).map { video("L$it", "letters", it) }
        val shelf = GetHomeFeedUseCase.shelf(collection("letters", 1), videos, setOf("L1", "L3"), emptySet())

        assertEquals(listOf("L2", "L4", "L5"), shelf?.videos?.map { it.youtubeId })
    }

    @Test
    fun shelf_topsUpWithFinishedVideosFromTheStart_andStartsOverWhenAllAreFinished() {
        val letters = collection("letters", 1)
        val videos = (1..5).map { video("L$it", "letters", it) }
        val ids = { finished: Set<String> ->
            GetHomeFeedUseCase.shelf(letters, videos, finished, emptySet())?.videos?.map { it.youtubeId }
        }

        assertEquals(listOf("L5", "L1", "L2"), ids(setOf("L1", "L2", "L3", "L4")))
        assertEquals(listOf("L1", "L2", "L3"), ids(videos.map { it.youtubeId }.toSet()))
    }

    @Test
    fun shelf_skipsVideosShownElsewhere_andIsNullWhenNothingIsLeft() {
        val letters = collection("letters", 1)
        val videos = (1..5).map { video("L$it", "letters", it) }

        assertEquals(
            listOf("L3", "L4", "L5"),
            GetHomeFeedUseCase.shelf(letters, videos, emptySet(), setOf("L1", "L2"))?.videos?.map { it.youtubeId },
        )
        assertNull(GetHomeFeedUseCase.shelf(letters, videos.take(2), emptySet(), setOf("L1", "L2")))
        assertNull(GetHomeFeedUseCase.shelf(letters, emptyList(), emptySet(), emptySet()))
    }

    @Test
    fun shelves_countVideosFinishedLongAgo() = runTest {
        // Finished 30 days ago: outside the 7-day window, but still done for the shelf.
        history.events.value = listOf(event("L1", kuwait(2026, 10, 5, 10, 0), watchedSec = 180, completed = true))

        val feed = homeFeed()(CHILD).first()

        val expected = expectedShelves(
            feed,
            linkedMapOf(
                "letters" to listOf("L2", "L3", "L4", "L5", "L1"),
                "numbers" to listOf("N1", "N2"),
                "stories" to listOf("S1", "S2", "S3"),
            ),
        )
        assertEquals(expected, feed.shelfIds())
    }

    @Test
    fun nothingAppearsTwiceOnHome() = runTest {
        history.events.value = listOf(
            event("S1", start - 2.hours, watchedSec = 90),
            event("L1", start - 3.hours, watchedSec = 180, completed = true),
        )

        val feed = homeFeed()(CHILD).first()
        val shown = feed.everythingShown()

        assertEquals("S1", feed.continueWatching?.video?.youtubeId)
        assertEquals(shown.distinct(), shown)
        assertTrue(feed.shelves.all { it.videos.size <= GetHomeFeedUseCase.SHELF_SIZE })
    }

    @Test
    fun dailySeedIsStableAndDependsOnChildAndDate() {
        val d = LocalDate(2026, 11, 4)
        assertEquals(GetHomeFeedUseCase.dailySeed("a", d), GetHomeFeedUseCase.dailySeed("a", d))
        assertNotEquals(GetHomeFeedUseCase.dailySeed("a", d), GetHomeFeedUseCase.dailySeed("b", d))
        assertNotEquals(GetHomeFeedUseCase.dailySeed("a", d), GetHomeFeedUseCase.dailySeed("a", LocalDate(2026, 11, 5)))
    }
}
