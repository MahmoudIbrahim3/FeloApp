package com.mahmoud.feelo.feature.parent

import app.cash.turbine.test
import com.mahmoud.feelo.core.domain.testing.CHILD
import com.mahmoud.feelo.core.domain.testing.Catalog
import com.mahmoud.feelo.core.domain.testing.FULL
import com.mahmoud.feelo.core.domain.testing.FakeBillingRepository
import com.mahmoud.feelo.core.domain.testing.FakeProfileRepository
import com.mahmoud.feelo.core.domain.testing.FakeSettingsRepository
import com.mahmoud.feelo.core.domain.testing.FakeWatchHistoryRepository
import com.mahmoud.feelo.core.domain.testing.KUWAIT
import com.mahmoud.feelo.core.domain.testing.OTHER_CHILD
import com.mahmoud.feelo.core.domain.testing.TestClock
import com.mahmoud.feelo.core.domain.testing.child
import com.mahmoud.feelo.core.domain.testing.kuwait
import com.mahmoud.feelo.core.domain.testing.runViewModelTest
import com.mahmoud.feelo.core.domain.usecase.ConnectBillingUseCase
import com.mahmoud.feelo.core.domain.usecase.GetCollectionsForAgeUseCase
import com.mahmoud.feelo.core.domain.usecase.GetDailyQuotaUseCase
import com.mahmoud.feelo.core.domain.usecase.GrantExtraVideosUseCase
import com.mahmoud.feelo.core.domain.usecase.ObserveAccessUseCase
import com.mahmoud.feelo.core.domain.usecase.UpdateChildSettingsUseCase
import com.mahmoud.feelo.core.model.AgeBand
import com.mahmoud.feelo.core.model.DailyLimit
import com.mahmoud.feelo.core.model.Entitlement
import com.mahmoud.feelo.feature.parent.dashboard.AddChildAction
import com.mahmoud.feelo.feature.parent.dashboard.DashboardEffect
import com.mahmoud.feelo.feature.parent.dashboard.DashboardEvent
import com.mahmoud.feelo.feature.parent.dashboard.DashboardUiState
import com.mahmoud.feelo.feature.parent.dashboard.DashboardViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent

class DashboardViewModelTest {

    private val catalog = Catalog.repository()
    private val profiles = FakeProfileRepository(
        child(DailyLimit.Videos(3), allowed = setOf("letters", "numbers")),
        child(DailyLimit.Minutes(30), ageBand = AgeBand.A9_12, id = OTHER_CHILD),
    )
    private val settings = FakeSettingsRepository(pin = "2468", onboardingDone = true, activeChildId = CHILD)
    private val history = FakeWatchHistoryRepository()

    /** Subscribed by default: the older tests use both children. */
    private val billing = FakeBillingRepository(FULL)

    private fun TestScope.dashboard(): Pair<DashboardViewModel, GetDailyQuotaUseCase> {
        val clock = TestClock(testScheduler, kuwait(2026, 11, 4, 17, 0))
        val quota = GetDailyQuotaUseCase(profiles, history, clock, KUWAIT)
        val vm = DashboardViewModel(
            profiles,
            settings,
            quota,
            GetCollectionsForAgeUseCase(catalog),
            UpdateChildSettingsUseCase(profiles, catalog),
            GrantExtraVideosUseCase(history, clock, KUWAIT),
            ObserveAccessUseCase(billing, profiles),
            ConnectBillingUseCase(billing, settings, clock),
        )
        runCurrent()
        return vm to quota
    }

    private val DashboardViewModel.content get() = assertIs<DashboardUiState.Content>(state.value)

    @Test
    fun showsTheActiveChildWithCollectionsOfItsAge() = runViewModelTest {
        val (vm, _) = dashboard()
        assertEquals(CHILD, vm.content.selected.id)
        assertEquals(2, vm.content.children.size)
        assertEquals(listOf("letters", "numbers", "stories"), vm.content.collections.map { it.collection.id })
        assertEquals(listOf(true, true, false), vm.content.collections.map { it.allowed })
    }

    @Test
    fun changingTheLimitUpdatesTheChildsQuotaAtOnce() = runViewModelTest {
        val (vm, quota) = dashboard()
        vm.onEvent(DashboardEvent.LimitChanged(DailyLimit.Videos(10)))
        runCurrent()

        assertEquals(DailyLimit.Videos(10), vm.content.selected.dailyLimit)
        // The same flow child Home reads.
        assertEquals(10, quota(CHILD).first().total)
    }

    @Test
    fun togglingACollectionIsSavedOnTheProfile() = runViewModelTest {
        val (vm, _) = dashboard()
        vm.onEvent(DashboardEvent.CollectionToggled("numbers", allowed = false))
        vm.onEvent(DashboardEvent.CollectionToggled("stories", allowed = true))
        runCurrent()

        assertEquals(setOf("letters", "stories"), profiles.getProfile(CHILD)?.allowedCollectionIds)
        assertEquals(listOf(true, false, true), vm.content.collections.map { it.allowed })
    }

    @Test
    fun grantAddsToTodaysTotal() = runViewModelTest {
        val (vm, _) = dashboard()
        vm.onEvent(DashboardEvent.GrantExtra(3))
        runCurrent()
        assertEquals(6, vm.content.quota.total)
        assertEquals(3, vm.content.quota.extraVideosGranted)
    }

    @Test
    fun switchingChildMakesItTheActiveChild() = runViewModelTest {
        val (vm, _) = dashboard()
        vm.onEvent(DashboardEvent.SelectChild(OTHER_CHILD))
        runCurrent()

        assertEquals(OTHER_CHILD, settings.activeChildId.first())
        assertEquals(OTHER_CHILD, vm.content.selected.id)
        assertEquals(listOf("letters", "numbers", "stories", "science"), vm.content.collections.map { it.collection.id })
    }

    @Test
    fun freeLocksTheSecondChild_andItAndAddChildOpenThePaywall() = runViewModelTest {
        billing.entitlement.value = Entitlement.Free
        val (vm, _) = dashboard()

        assertEquals(setOf(OTHER_CHILD), vm.content.lockedChildIds)
        assertEquals(AddChildAction.Subscribe, vm.content.addChild)
        assertFalse(vm.content.isSubscribed)

        vm.effects.test {
            vm.onEvent(DashboardEvent.SelectChild(OTHER_CHILD))
            assertEquals(DashboardEffect.OpenSubscription, awaitItem())
            vm.onEvent(DashboardEvent.AddChildClicked)
            assertEquals(DashboardEffect.OpenSubscription, awaitItem())
        }
        runCurrent()
        assertEquals(CHILD, settings.activeChildId.first())
    }

    @Test
    fun freeCanSwitchOnAnyCollection() = runViewModelTest {
        billing.entitlement.value = Entitlement.Free
        val (vm, _) = dashboard()

        vm.onEvent(DashboardEvent.CollectionToggled("stories", allowed = true))
        runCurrent()
        assertEquals(setOf("letters", "numbers", "stories"), profiles.getProfile(CHILD)?.allowedCollectionIds)
        assertTrue(vm.content.collections.first { it.collection.id == "stories" }.allowed)
    }

    @Test
    fun theDashboardConnectsToTheStoreOnlyAfterConsent() = runViewModelTest {
        dashboard()
        assertEquals(0, billing.startCalls)

        settings.setConsentGiven(true)
        dashboard()
        assertEquals(1, billing.startCalls)
    }

    @Test
    fun aLockedActiveChildFallsBackToTheFirst() = runViewModelTest {
        settings.setActiveChildId(OTHER_CHILD)
        billing.entitlement.value = Entitlement.Free
        val (vm, _) = dashboard()
        runCurrent()

        assertEquals(CHILD, vm.content.selected.id)
        assertEquals(CHILD, settings.activeChildId.first())
    }

    @Test
    fun fullAtFourChildrenHidesAddChild() = runViewModelTest {
        profiles.upsert(child(id = "c3"))
        profiles.upsert(child(id = "c4"))
        val (vm, _) = dashboard()

        assertTrue(vm.content.isSubscribed)
        assertEquals(AddChildAction.Hidden, vm.content.addChild)
        assertTrue(vm.content.lockedChildIds.isEmpty())
    }

    @Test
    fun aboutOpensTheAboutScreen() = runViewModelTest {
        val (vm, _) = dashboard()
        vm.effects.test {
            vm.onEvent(DashboardEvent.AboutClicked)
            assertEquals(DashboardEffect.OpenAbout, awaitItem())
        }
    }
}
