package com.mahmoud.clipkid.feature.child.player

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.mahmoud.clipkid.core.domain.testing.CHILD
import com.mahmoud.clipkid.core.domain.testing.child
import com.mahmoud.clipkid.core.domain.testing.event
import com.mahmoud.clipkid.core.domain.testing.kuwait
import com.mahmoud.clipkid.core.model.DailyLimit
import com.mahmoud.clipkid.core.model.VideoReport
import com.mahmoud.clipkid.core.model.WatchSource
import com.mahmoud.clipkid.core.player.YouTubePlayerEvent
import com.mahmoud.clipkid.feature.child.testing.ChildFixture
import com.mahmoud.clipkid.feature.child.testing.runViewModelTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlayerViewModelTest {

    private fun TestScope.viewModel(
        f: ChildFixture,
        youtubeId: String = "L1",
        source: WatchSource = WatchSource.Home,
        savedState: SavedStateHandle = SavedStateHandle(),
    ) = PlayerViewModel(
        childId = CHILD,
        youtubeId = youtubeId,
        source = source,
        savedState = savedState,
        startWatch = f.startWatch,
        updateProgress = f.updateProgress,
        getResumePosition = f.resumePosition,
        reportVideo = f.reportVideo,
        network = f.network,
        toggleFavorite = f.toggleFavorite,
        catalog = f.catalog,
        favorites = f.favorites,
    ).also { advanceUntilIdle() }

    private fun PlayerViewModel.player(event: YouTubePlayerEvent) = onEvent(PlayerEvent.FromPlayer(event))

    private val PlayerViewModel.slot: PlayerSlot get() = assertIs<PlayerUiState.Ready>(state.value).player

    @Test
    fun opening_showsThePlayerButCountsNothingYet() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f)

        val ready = assertIs<PlayerUiState.Ready>(vm.state.value)
        assertEquals("L1", ready.video.youtubeId)
        assertEquals("مجموعة letters", ready.collectionTitle)
        assertEquals(PlayerSlot(startSec = 0, autoPlay = true, visible = true, problem = null, attempt = 0), ready.player)
        assertEquals(0, f.history.events.value.size)
    }

    @Test
    fun theFirstPlaying_recordsOneCountedEvent_andTurnsAutoPlayOff() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f)

        vm.player(YouTubePlayerEvent.Ready(180))
        vm.player(YouTubePlayerEvent.Playing(0))
        advanceUntilIdle()
        vm.player(YouTubePlayerEvent.Paused(3))
        vm.player(YouTubePlayerEvent.Playing(3))
        advanceUntilIdle()

        val recorded = f.history.events.value.single()
        assertTrue(recorded.countsTowardLimit)
        assertFalse(vm.slot.autoPlay)
    }

    @Test
    fun whenTodaysLimitIsUsedUp_itOpensDoneToday_withoutAPlayer() = runViewModelTest {
        val f = ChildFixture(this, child(DailyLimit.Videos(1)))
        f.history.events.value = listOf(event("L2", kuwait(2026, 11, 4, 9), watchedSec = 30))

        val vm = PlayerViewModel(
            CHILD, "L1", WatchSource.Home, SavedStateHandle(), f.startWatch, f.updateProgress, f.resumePosition,
            f.reportVideo, f.network, f.toggleFavorite, f.catalog, f.favorites,
        )
        vm.effects.test {
            advanceUntilIdle()
            assertEquals(PlayerEffect.OpenDoneToday, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(PlayerUiState.Loading, vm.state.value)
        assertEquals(1, f.history.events.value.size)
    }

    @Test
    fun ticksAndPauses_saveTimePlayedAndPosition_seeksAreNotCounted() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f)

        vm.player(YouTubePlayerEvent.Playing(0))
        advanceUntilIdle()
        vm.player(YouTubePlayerEvent.Tick(5))
        vm.player(YouTubePlayerEvent.Tick(10))
        vm.player(YouTubePlayerEvent.Tick(70)) // the child skipped ahead
        vm.player(YouTubePlayerEvent.Paused(72))
        advanceUntilIdle()

        val saved = f.history.events.value.single()
        assertEquals(12, saved.watchedSec)
        assertEquals(72, saved.positionSec)
        assertFalse(saved.completed)
        assertEquals(72, vm.slot.startSec)
    }

    @Test
    fun ended_completesThePlayback_removesThePlayer_andOpensUpNextOnce() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f)
        vm.player(YouTubePlayerEvent.Playing(0))
        advanceUntilIdle()
        vm.player(YouTubePlayerEvent.Tick(175))
        vm.player(YouTubePlayerEvent.Playing(175)) // resync after a seek

        vm.effects.test {
            vm.player(YouTubePlayerEvent.Ended)
            vm.player(YouTubePlayerEvent.Ended)
            assertFalse(vm.slot.visible)
            advanceUntilIdle()
            assertEquals(PlayerEffect.OpenUpNext("L1"), awaitItem())
            expectNoEvents()
        }
        val saved = f.history.events.value.single()
        assertTrue(saved.completed)
        assertEquals(180, saved.positionSec)
        assertEquals(5, saved.watchedSec)
    }

    @Test
    fun embeddingDisabled_showsNotAvailable_reportsOnce_andCountsNothing() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f)

        vm.player(YouTubePlayerEvent.Error(150))
        vm.player(YouTubePlayerEvent.Error(101))
        advanceUntilIdle()

        assertEquals(PlayerProblem.NotAvailable(150), vm.slot.problem)
        assertFalse(vm.slot.visible)
        assertEquals(listOf(VideoReport("L1", VideoReport.REASON_EMBED_DISABLED)), f.reporter.reports)
        assertEquals(0, f.history.events.value.size)
    }

    @Test
    fun removedVideo_isReportedAsRemoved() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f)

        vm.player(YouTubePlayerEvent.Error(100))
        advanceUntilIdle()

        assertEquals(listOf(VideoReport("L1", VideoReport.REASON_VIDEO_REMOVED)), f.reporter.reports)
    }

    @Test
    fun missingReferer_isNotAvailableWithoutAReport() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f)

        vm.player(YouTubePlayerEvent.Error(153))
        advanceUntilIdle()

        assertEquals(PlayerProblem.NotAvailable(153), vm.slot.problem)
        assertEquals(emptyList(), f.reporter.reports)
    }

    @Test
    fun offline_canBeRetriedWithAFreshPlayer() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f)

        vm.player(YouTubePlayerEvent.Error(-1))
        assertEquals(PlayerProblem.Offline, vm.slot.problem)

        vm.onEvent(PlayerEvent.RetryClicked)

        assertNull(vm.slot.problem)
        assertTrue(vm.slot.visible)
        assertEquals(1, vm.slot.attempt)
        assertEquals(0, f.history.events.value.size)
    }

    @Test
    fun offlineWhenOpened_showsOfflineAtOnce_andRetriesWhenTheNetworkIsBack() = runViewModelTest {
        val f = ChildFixture(this)
        f.network.isOnline.value = false
        val vm = viewModel(f)

        assertEquals(PlayerProblem.Offline, vm.slot.problem)
        assertFalse(vm.slot.visible)

        f.network.isOnline.value = true
        advanceUntilIdle()

        assertNull(vm.slot.problem)
        assertTrue(vm.slot.visible)
        assertEquals(1, vm.slot.attempt)
        assertTrue(vm.slot.autoPlay)
        assertEquals(0, f.history.events.value.size)
    }

    @Test
    fun aLink_savesProgress_andGoesToTheGate() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f)
        vm.player(YouTubePlayerEvent.Playing(0))
        advanceUntilIdle()
        vm.player(YouTubePlayerEvent.Tick(5))

        vm.effects.test {
            vm.player(YouTubePlayerEvent.LinkRequested("https://www.youtube.com/watch?v=abcdefghijk"))
            advanceUntilIdle()
            assertEquals(PlayerEffect.OpenExternalLink("https://www.youtube.com/watch?v=abcdefghijk"), awaitItem())
        }
        assertEquals(5, f.history.events.value.single().positionSec)
    }

    @Test
    fun recreatedAfterTheGate_resumesPaused_andDoesNotCountAgain() = runViewModelTest {
        val f = ChildFixture(this, child(DailyLimit.Videos(1)))
        val saved = SavedStateHandle()
        val first = viewModel(f, savedState = saved)
        first.player(YouTubePlayerEvent.Playing(0))
        advanceUntilIdle()
        first.player(YouTubePlayerEvent.Tick(5))
        first.player(YouTubePlayerEvent.Paused(8))
        advanceUntilIdle()

        // Same saved state (process death / recreation), limit already used by this playback.
        val second = viewModel(f, savedState = saved)
        assertEquals(PlayerSlot(startSec = 8, autoPlay = false, visible = true, problem = null, attempt = 0), second.slot)
        second.player(YouTubePlayerEvent.Playing(8))
        second.player(YouTubePlayerEvent.Tick(13))
        advanceUntilIdle()

        val event = f.history.events.value.single()
        assertEquals(13, event.watchedSec)
        assertEquals(13, event.positionSec)
    }

    @Test
    fun continueWatching_startsWhereTheChildStopped() = runViewModelTest {
        val f = ChildFixture(this)
        f.history.events.value = listOf(event("L1", kuwait(2026, 11, 4, 9), watchedSec = 60, positionSec = 64))

        val vm = viewModel(f, source = WatchSource.ContinueWatching)

        assertEquals(64, vm.slot.startSec)
        assertTrue(vm.slot.autoPlay)
    }

    @Test
    fun back_savesProgressFirst() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f)
        vm.player(YouTubePlayerEvent.Playing(0))
        advanceUntilIdle()
        vm.player(YouTubePlayerEvent.Tick(5))

        vm.effects.test {
            vm.onEvent(PlayerEvent.BackClicked)
            advanceUntilIdle()
            assertEquals(PlayerEffect.Back, awaitItem())
        }
        assertEquals(5, f.history.events.value.single().watchedSec)
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
