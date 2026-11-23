package com.mahmoud.feelo.feature.parent

import app.cash.turbine.test
import com.mahmoud.feelo.core.domain.testing.CHILD
import com.mahmoud.feelo.core.domain.testing.Catalog
import com.mahmoud.feelo.core.domain.testing.FULL
import com.mahmoud.feelo.core.domain.testing.FakeBillingRepository
import com.mahmoud.feelo.core.domain.testing.FakeFavoritesRepository
import com.mahmoud.feelo.core.domain.testing.FakeHiddenVideosRepository
import com.mahmoud.feelo.core.domain.testing.FakeProfileRepository
import com.mahmoud.feelo.core.domain.testing.FakeVideoReporter
import com.mahmoud.feelo.core.domain.testing.FakeWatchHistoryRepository
import com.mahmoud.feelo.core.domain.testing.KUWAIT
import com.mahmoud.feelo.core.domain.testing.TestClock
import com.mahmoud.feelo.core.domain.testing.child
import com.mahmoud.feelo.core.domain.testing.event
import com.mahmoud.feelo.core.domain.testing.kuwait
import com.mahmoud.feelo.core.domain.testing.runViewModelTest
import com.mahmoud.feelo.core.domain.usecase.GetWeeklyActivityUseCase
import com.mahmoud.feelo.core.domain.usecase.HideVideoUseCase
import com.mahmoud.feelo.core.domain.usecase.ObserveAccessUseCase
import com.mahmoud.feelo.core.domain.usecase.ReportVideoUseCase
import com.mahmoud.feelo.core.model.Entitlement
import com.mahmoud.feelo.feature.parent.activity.ActivityEffect
import com.mahmoud.feelo.feature.parent.activity.ActivityEvent
import com.mahmoud.feelo.feature.parent.activity.ActivityUiState
import com.mahmoud.feelo.feature.parent.activity.ActivityViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent

/** Today's list is free; the 7-day chart is a Feelo كامل feature. */
class ActivityViewModelTest {

    private val profiles = FakeProfileRepository(child())
    private val history = FakeWatchHistoryRepository()
    private val hidden = FakeHiddenVideosRepository()
    private val billing = FakeBillingRepository()

    private fun TestScope.activity(): ActivityViewModel {
        val clock = TestClock(testScheduler, kuwait(2026, 11, 4, 17, 0))
        val vm = ActivityViewModel(
            childId = CHILD,
            getActivity = GetWeeklyActivityUseCase(Catalog.repository(), profiles, history, hidden, clock, KUWAIT),
            hideVideo = HideVideoUseCase(hidden, FakeFavoritesRepository(), ReportVideoUseCase(FakeVideoReporter()), clock),
            observeAccess = ObserveAccessUseCase(billing, profiles),
        )
        runCurrent()
        return vm
    }

    private val ActivityViewModel.content get() = assertIs<ActivityUiState.Content>(state.value)

    @Test
    fun freeShowsTodayButNotTheWeek() = runViewModelTest {
        history.events.value = listOf(
            event("L1", kuwait(2026, 11, 4, 9), watchedSec = 60),
            event("L2", kuwait(2026, 11, 1, 9), watchedSec = 60),
        )
        val vm = activity()

        assertFalse(vm.content.showsWeek)
        assertEquals(listOf("L1"), vm.content.activity.today.map { it.video.youtubeId })
    }

    @Test
    fun fullShowsTheWeek_andExpiryHidesItAgainWithoutLosingHistory() = runViewModelTest {
        billing.entitlement.value = FULL
        history.events.value = listOf(event("L2", kuwait(2026, 11, 1, 9), watchedSec = 60))
        val vm = activity()
        assertTrue(vm.content.showsWeek)
        assertEquals(1, vm.content.activity.weekVideos)

        billing.entitlement.value = Entitlement.Free
        runCurrent()
        assertFalse(vm.content.showsWeek)
        assertEquals(1, vm.content.activity.weekVideos, "recorded either way: it comes back with the subscription")
    }

    @Test
    fun theLockedWeekCardOpensThePaywall() = runViewModelTest {
        val vm = activity()
        vm.effects.test {
            vm.onEvent(ActivityEvent.SeeWeekClicked)
            assertEquals(ActivityEffect.OpenSubscription, awaitItem())
        }
    }
}
