package com.mahmoud.feelo.feature.child.upnext

import app.cash.turbine.test
import com.mahmoud.feelo.core.domain.testing.CHILD
import com.mahmoud.feelo.core.domain.testing.child
import com.mahmoud.feelo.core.domain.testing.event
import com.mahmoud.feelo.core.domain.testing.kuwait
import com.mahmoud.feelo.core.model.DailyLimit
import com.mahmoud.feelo.core.model.WatchSource
import com.mahmoud.feelo.feature.child.testing.ChildFixture
import com.mahmoud.feelo.feature.child.testing.runViewModelTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class UpNextViewModelTest {

    private fun TestScope.viewModel(f: ChildFixture, finished: String = "L1") =
        UpNextViewModel(CHILD, finished, f.upNext, f.quota, f.catalog).also { advanceUntilIdle() }

    /** L1 finished today, so it is not offered again. */
    private fun ChildFixture.finishedL1() {
        history.events.value = listOf(event("L1", kuwait(2026, 11, 4, 9), watchedSec = 180, completed = true))
    }

    @Test
    fun primaryIsTheNextVideoInTheCollection_andTagPicksShareTheFirstTag() = runViewModelTest {
        val f = ChildFixture(this)
        f.finishedL1()
        val ready = assertIs<UpNextUiState.Ready>(viewModel(f).state.value)

        assertEquals("L1", ready.finished?.youtubeId)
        assertEquals("L2", ready.primary?.youtubeId)
        assertEquals("مجموعة letters", ready.primaryCollectionTitle)
        // L1's first tag is الصباح: S3 (stories) shares it, and the finished video is never offered.
        assertEquals("الصباح", ready.tagPicks?.tag)
        assertEquals(listOf("S3"), ready.tagPicks?.videos?.map { it.youtubeId })
        assertEquals(1, ready.quota.used)
    }

    @Test
    fun neverOffersMoreThanThreeChoices() = runViewModelTest {
        val f = ChildFixture(this)
        f.finishedL1()
        val ready = assertIs<UpNextUiState.Ready>(viewModel(f).state.value)

        val choices = listOfNotNull(ready.primary) + ready.tagPicks?.videos.orEmpty()
        assertTrue(choices.size <= 3)
    }

    @Test
    fun limitReached_opensDoneTodayInsteadOfUpNext() = runViewModelTest {
        val f = ChildFixture(this, child(DailyLimit.Videos(1)))
        f.finishedL1()
        val vm = UpNextViewModel(CHILD, "L1", f.upNext, f.quota, f.catalog)

        vm.effects.test {
            advanceUntilIdle()
            assertEquals(UpNextEffect.OpenDoneToday, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals(UpNextUiState.Loading, vm.state.value)
    }

    @Test
    fun choiceOpensThePlayerFromUpNext() = runViewModelTest {
        val f = ChildFixture(this)
        f.finishedL1()
        val vm = viewModel(f)

        vm.effects.test {
            vm.onEvent(UpNextEvent.ChoiceClicked("L2"))
            assertEquals(UpNextEffect.OpenPlayer("L2", WatchSource.UpNext), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun replayOpensTheFinishedVideoAsAFreeReplay() = runViewModelTest {
        val f = ChildFixture(this)
        f.finishedL1()
        val vm = viewModel(f)

        vm.effects.test {
            vm.onEvent(UpNextEvent.ReplayClicked)
            assertEquals(UpNextEffect.OpenPlayer("L1", WatchSource.UpNextReplay), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun homeClickEmitsHome() = runViewModelTest {
        val f = ChildFixture(this)
        f.finishedL1()
        val vm = viewModel(f)

        vm.effects.test {
            vm.onEvent(UpNextEvent.HomeClicked)
            assertEquals(UpNextEffect.Home, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
