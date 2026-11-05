package com.mahmoud.clipkid.feature.child.home

import app.cash.turbine.test
import com.mahmoud.clipkid.core.domain.testing.CHILD
import com.mahmoud.clipkid.core.domain.testing.FULL
import com.mahmoud.clipkid.core.domain.testing.child
import com.mahmoud.clipkid.core.domain.testing.event
import com.mahmoud.clipkid.core.domain.testing.kuwait
import com.mahmoud.clipkid.core.model.Entitlement
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
        // science is 7-12 only, so a 4-6 child never sees it.
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
    fun premiumWithoutTheSubscriptionIsALockedCoverAndOpensAskParent() = runViewModelTest {
        val f = ChildFixture(this)
        f.catalog.collections.value = f.catalog.collections.value.map { if (it.id == "stories") it.copy(isPremium = true) else it }
        val vm = viewModel(f)

        val state = assertIs<HomeUiState.Content>(vm.state.value)
        assertEquals(listOf("letters", "numbers"), state.collections.map { it.id })
        assertEquals(listOf("letters" to false, "numbers" to false, "stories" to true), state.covers.map { it.collection.id to it.locked })
        assertTrue(state.shelves.none { it.collection.id == "stories" })
        assertTrue(state.newToday.none { it.collectionId == "stories" })

        vm.effects.test {
            vm.onEvent(HomeEvent.LockedCollectionClicked("stories"))
            assertEquals(HomeEffect.OpenAskParent, awaitItem())
        }
    }

    @Test
    fun subscribingOpensPremiumAtOnce_andExpiryLocksItAgain() = runViewModelTest {
        val f = ChildFixture(this)
        f.catalog.collections.value = f.catalog.collections.value.map { if (it.id == "stories") it.copy(isPremium = true) else it }
        val vm = viewModel(f)

        f.billing.entitlement.value = FULL
        advanceUntilIdle()
        val full = assertIs<HomeUiState.Content>(vm.state.value)
        assertEquals(listOf("letters", "numbers", "stories"), full.collections.map { it.id })
        assertTrue(full.lockedCollections.isEmpty())

        f.billing.entitlement.value = Entitlement.Free
        advanceUntilIdle()
        val expired = assertIs<HomeUiState.Content>(vm.state.value)
        assertEquals(listOf("stories"), expired.lockedCollections.map { it.id })
    }
}
