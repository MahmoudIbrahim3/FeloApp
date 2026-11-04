package com.mahmoud.clipkid.feature.child.player

import app.cash.turbine.test
import com.mahmoud.clipkid.core.domain.testing.CHILD
import com.mahmoud.clipkid.core.domain.testing.child
import com.mahmoud.clipkid.core.domain.testing.event
import com.mahmoud.clipkid.core.domain.testing.kuwait
import com.mahmoud.clipkid.core.model.DailyLimit
import com.mahmoud.clipkid.core.model.WatchSource
import com.mahmoud.clipkid.feature.child.testing.ChildFixture
import com.mahmoud.clipkid.feature.child.testing.runViewModelTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PlayerViewModelTest {

    private fun TestScope.viewModel(
        f: ChildFixture,
        youtubeId: String = "L1",
        source: WatchSource = WatchSource.Home,
    ) = PlayerViewModel(
        childId = CHILD,
        youtubeId = youtubeId,
        source = source,
        startWatch = f.startWatch,
        updateProgress = f.updateProgress,
        toggleFavorite = f.toggleFavorite,
        catalog = f.catalog,
        favorites = f.favorites,
    ).also { advanceUntilIdle() }

    @Test
    fun opening_countsTheVideoAndShowsItsCollection() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f)

        val ready = assertIs<PlayerUiState.Ready>(vm.state.value)
        assertEquals("L1", ready.video.youtubeId)
        assertEquals("مجموعة letters", ready.collectionTitle)
        assertEquals(false, ready.isFavorite)
        assertEquals(false, ready.ended)
        assertEquals(1, f.history.events.value.size)
        assertTrue(f.history.events.value.single().countsTowardLimit)
    }

    @Test
    fun whenTodaysLimitIsUsedUp_itOpensDoneTodayAndRecordsNothing() = runViewModelTest {
        val f = ChildFixture(this, child(DailyLimit.Videos(1)))
        f.history.events.value = listOf(event("L2", kuwait(2026, 11, 4, 9), watchedSec = 30))

        val vm = PlayerViewModel(
            CHILD, "L1", WatchSource.Home, f.startWatch, f.updateProgress, f.toggleFavorite, f.catalog, f.favorites,
        )
        vm.effects.test {
            advanceUntilIdle()
            assertEquals(PlayerEffect.OpenDoneToday, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(1, f.history.events.value.size)
    }

    @Test
    fun simulateEnd_completesThePlaybackAndOpensUpNext() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f)
        val durationSec = assertIs<PlayerUiState.Ready>(vm.state.value).video.durationSec

        vm.effects.test {
            vm.onEvent(PlayerEvent.SimulateEnd)
            advanceUntilIdle()
            assertEquals(PlayerEffect.OpenUpNext("L1"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        val saved = f.history.events.value.single()
        assertTrue(saved.completed)
        assertEquals(durationSec, saved.watchedSec)
        assertTrue(assertIs<PlayerUiState.Ready>(vm.state.value).ended)
    }

    @Test
    fun simulateEndRunsOnlyOnce() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f)

        vm.onEvent(PlayerEvent.SimulateEnd)
        vm.onEvent(PlayerEvent.SimulateEnd)
        advanceUntilIdle()

        assertEquals(1, f.history.events.value.size)
        assertTrue(f.history.events.value.single().completed)
    }

    @Test
    fun toggleFavoriteFlipsTheHeart() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f)

        vm.onEvent(PlayerEvent.ToggleFavorite)
        advanceUntilIdle()
        assertEquals(true, assertIs<PlayerUiState.Ready>(vm.state.value).isFavorite)

        vm.onEvent(PlayerEvent.ToggleFavorite)
        advanceUntilIdle()
        assertEquals(false, assertIs<PlayerUiState.Ready>(vm.state.value).isFavorite)
    }

    @Test
    fun unknownVideoIsUnavailable() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f, youtubeId = "NOPE")
        advanceUntilIdle()

        assertEquals(PlayerUiState.Unavailable, vm.state.value)
    }
}
