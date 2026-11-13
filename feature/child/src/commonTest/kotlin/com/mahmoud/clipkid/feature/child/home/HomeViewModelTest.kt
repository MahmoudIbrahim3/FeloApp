package com.mahmoud.clipkid.feature.child.home

import app.cash.turbine.test
import com.mahmoud.clipkid.core.domain.testing.CHILD
import com.mahmoud.clipkid.core.domain.testing.child
import com.mahmoud.clipkid.core.domain.testing.event
import com.mahmoud.clipkid.core.domain.testing.kuwait
import com.mahmoud.clipkid.core.model.WatchSource
import com.mahmoud.clipkid.feature.child.testing.ChildFixture
import com.mahmoud.clipkid.feature.child.testing.runViewModelTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

class HomeViewModelTest {

    private fun TestScope.viewModel(f: ChildFixture) =
        HomeViewModel(CHILD, f.homeFeed, f.quota).also { advanceUntilIdle() }

    @Test
    fun contentHasNameQuotaAndOneShelfPerVisibleCollection() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f)

        val state = assertIs<HomeUiState.Content>(vm.state.value)
        assertEquals("يوسف", state.childName)
        assertEquals(0, state.quota.used)
        assertEquals(3, state.quota.total)
        // science is 9-12 only, so a 6-8 child never sees it.
        assertEquals(listOf("letters", "numbers", "stories"), state.collections.map { it.id })
        assertEquals(listOf("letters", "numbers", "stories"), state.shelves.map { it.collection.id })
        assertEquals(6, state.newToday.size)
        assertTrue(!state.isEmpty)
    }

    @Test
    fun continueWatchingIsTheLatestUnfinishedPlayback() = runViewModelTest {
        val f = ChildFixture(this)
        f.history.events.value = listOf(event("L2", kuwait(2026, 11, 4, 9), watchedSec = 60))
        val vm = viewModel(f)

        val state = assertIs<HomeUiState.Content>(vm.state.value)
        val continueWatching = assertNotNull(state.continueWatching)
        assertEquals("L2", continueWatching.video.youtubeId)
        assertEquals(60, continueWatching.watchedSec)
    }

    @Test
    fun quotaCountsVideosStartedToday() = runViewModelTest {
        val f = ChildFixture(this)
        f.history.events.value = listOf(event("L1", kuwait(2026, 11, 4, 9), watchedSec = 10))
        val vm = viewModel(f)

        val quota = assertIs<HomeUiState.Content>(vm.state.value).quota
        assertEquals(1, quota.used)
        assertEquals(2, quota.remaining)
    }

    @Test
    fun childWithNothingAllowedSeesTheEmptyState() = runViewModelTest {
        val f = ChildFixture(this, child(allowed = emptySet()))
        val vm = viewModel(f)

        assertTrue(assertIs<HomeUiState.Content>(vm.state.value).isEmpty)
    }

    @Test
    fun lockEmitsOpenGate() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f)

        vm.effects.test {
            vm.onEvent(HomeEvent.LockClicked)
            assertEquals(HomeEffect.OpenGate, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun videoClickEmitsOpenVideoWithItsSource() = runViewModelTest {
        val f = ChildFixture(this)
        val vm = viewModel(f)

        vm.effects.test {
            vm.onEvent(HomeEvent.VideoClicked("L1", WatchSource.NewToday))
            assertEquals(HomeEffect.OpenVideo("L1", WatchSource.NewToday), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun everyAllowedCollectionIsOpenOnTheFreePlan() = runViewModelTest {
        val f = ChildFixture(this) // FakeBillingRepository starts free
        val vm = viewModel(f)

        val state = assertIs<HomeUiState.Content>(vm.state.value)
        assertEquals(listOf("letters", "numbers", "stories"), state.collections.map { it.id })
        assertTrue(state.shelves.any { it.collection.id == "stories" })
    }
}
