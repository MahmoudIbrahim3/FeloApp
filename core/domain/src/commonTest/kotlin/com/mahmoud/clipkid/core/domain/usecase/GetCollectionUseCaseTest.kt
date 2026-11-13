package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.testing.CHILD
import com.mahmoud.clipkid.core.domain.testing.Catalog
import com.mahmoud.clipkid.core.domain.testing.FakeFavoritesRepository
import com.mahmoud.clipkid.core.domain.testing.FakeHiddenVideosRepository
import com.mahmoud.clipkid.core.domain.testing.FakeProfileRepository
import com.mahmoud.clipkid.core.domain.testing.FakeWatchHistoryRepository
import com.mahmoud.clipkid.core.domain.testing.FixedClock
import com.mahmoud.clipkid.core.domain.testing.KUWAIT
import com.mahmoud.clipkid.core.domain.testing.child
import com.mahmoud.clipkid.core.domain.testing.event
import com.mahmoud.clipkid.core.domain.testing.kuwait
import com.mahmoud.clipkid.core.model.ChildProfile
import com.mahmoud.clipkid.core.model.Favorite
import com.mahmoud.clipkid.core.model.HiddenVideo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class VideoStatusTest {

    @Test
    fun noEventsIsNew() = assertEquals(VideoStatus.New, VideoStatus.of(emptyList()))

    @Test
    fun anyCompletedPlaybackIsWatchedEvenIfReplayedPartially() {
        val events = listOf(
            event("L1", kuwait(2026, 11, 3, 10), watchedSec = 180, completed = true),
            event("L1", kuwait(2026, 11, 4, 10), watchedSec = 20),
        )
        assertEquals(VideoStatus.Watched, VideoStatus.of(events))
    }

    @Test
    fun unfinishedPlaybackIsInProgressAtTheFurthestPoint() {
        val events = listOf(
            event("L1", kuwait(2026, 11, 4, 10), watchedSec = 40),
            event("L1", kuwait(2026, 11, 4, 11), watchedSec = 90),
        )
        assertEquals(VideoStatus.InProgress(90), VideoStatus.of(events))
    }

    @Test
    fun startedWithZeroSecondsIsStillNew() {
        assertEquals(VideoStatus.New, VideoStatus.of(listOf(event("L1", kuwait(2026, 11, 4, 10)))))
    }
}

class GetCollectionUseCaseTest {

    private val now = kuwait(2026, 11, 4, 17, 0)
    private val history = FakeWatchHistoryRepository()
    private val hidden = FakeHiddenVideosRepository()

    private fun useCase(profile: ChildProfile = child()) =
        GetCollectionUseCase(
            catalog = Catalog.repository(),
            profiles = FakeProfileRepository(profile),
            history = history,
            hidden = hidden,
            clock = FixedClock(now),
        )

    @Test
    fun itemsAreVisibleVideosInSortOrderWithStatus() = runTest {
        history.events.value = listOf(
            event("L1", kuwait(2026, 11, 4, 9), watchedSec = 180, completed = true),
            event("L2", kuwait(2026, 11, 4, 10), watchedSec = 30),
        )
        val page = useCase()(CHILD, "letters").first()!!

        assertEquals(listOf("L1", "L2", "L3", "L4", "L5"), page.items.map { it.video.youtubeId })
        assertEquals(VideoStatus.Watched, page.items[0].status)
        assertEquals(VideoStatus.InProgress(30), page.items[1].status)
        assertEquals(VideoStatus.New, page.items[2].status)
        assertEquals(1, page.watchedCount)
    }

    @Test
    fun inProgressProgressIsWatchedOverDuration() = runTest {
        val l1 = Catalog.videos.first { it.youtubeId == "L1" }
        history.events.value = listOf(event("L1", kuwait(2026, 11, 4, 9), watchedSec = l1.durationSec / 2))
        val item = useCase()(CHILD, "letters").first()!!.items.first()

        assertEquals(0.5f, item.progress, absoluteTolerance = 0.01f)
    }

    @Test
    fun hiddenVideosAreLeftOutOfTheCollection() = runTest {
        hidden.hidden.value = listOf(HiddenVideo(CHILD, "L3", now))
        val ids = useCase()(CHILD, "letters").first()!!.items.map { it.video.youtubeId }

        assertEquals(listOf("L1", "L2", "L4", "L5"), ids)
    }

    @Test
    fun collectionOutsideTheChildsAgeBandOrAllowanceIsNull() = runTest {
        // science is 9-12 only; the child is 6-8.
        assertNull(useCase()(CHILD, "science").first())
        // numbers is allowed but not for this child's allowance.
        assertNull(useCase(child(allowed = setOf("letters")))(CHILD, "numbers").first())
    }
}

class GetFavoritesUseCaseTest {

    @Test
    fun favoritesAreNewestFirstAndOnlyVisibleVideosShow() = runTest {
        val favorites = FakeFavoritesRepository()
        val now = kuwait(2026, 11, 4, 17, 0)
        favorites.add(Favorite(CHILD, "L1", now))
        favorites.add(Favorite(CHILD, "S1", now))
        favorites.add(Favorite(CHILD, "C1", now)) // science: not visible to a 6-8 child
        favorites.add(Favorite(CHILD, "L2", now))
        val useCase = GetFavoritesUseCase(
            catalog = Catalog.repository(),
            profiles = FakeProfileRepository(child()),
            favorites = favorites,
            hidden = FakeHiddenVideosRepository(),
        )

        val items: List<FavoriteItem> = useCase(CHILD).first()
        assertEquals(listOf("L1", "S1", "L2"), items.map { it.video.youtubeId })
        assertEquals(listOf("letters", "stories", "letters"), items.map { it.collection.id })
    }
}
