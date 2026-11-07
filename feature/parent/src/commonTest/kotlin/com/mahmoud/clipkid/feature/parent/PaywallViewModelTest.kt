package com.mahmoud.clipkid.feature.parent

import app.cash.turbine.test
import com.mahmoud.clipkid.core.domain.parent.ParentSession
import com.mahmoud.clipkid.core.domain.testing.CHILD
import com.mahmoud.clipkid.core.domain.testing.FULL
import com.mahmoud.clipkid.core.domain.testing.FakeBillingRepository
import com.mahmoud.clipkid.core.domain.testing.FakeCatalogRepository
import com.mahmoud.clipkid.core.domain.testing.FakeProfileRepository
import com.mahmoud.clipkid.core.domain.testing.TEST_OFFERING
import com.mahmoud.clipkid.core.domain.testing.TestClock
import com.mahmoud.clipkid.core.domain.testing.child
import com.mahmoud.clipkid.core.domain.testing.collection
import com.mahmoud.clipkid.core.domain.testing.kuwait
import com.mahmoud.clipkid.core.domain.testing.runViewModelTest
import com.mahmoud.clipkid.core.domain.testing.testPlan
import com.mahmoud.clipkid.core.domain.usecase.EnablePremiumCollectionsUseCase
import com.mahmoud.clipkid.core.model.BillingError
import com.mahmoud.clipkid.core.model.Entitlement
import com.mahmoud.clipkid.core.model.Offering
import com.mahmoud.clipkid.core.model.PlanPeriod
import com.mahmoud.clipkid.core.model.PurchaseOutcome
import com.mahmoud.clipkid.core.model.LegalLinks
import com.mahmoud.clipkid.feature.parent.paywall.PaywallBusy
import com.mahmoud.clipkid.feature.parent.paywall.PaywallEffect
import com.mahmoud.clipkid.feature.parent.paywall.PaywallEvent
import com.mahmoud.clipkid.feature.parent.paywall.PaywallMessage
import com.mahmoud.clipkid.feature.parent.paywall.PaywallViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaywallViewModelTest {

    private val catalog = FakeCatalogRepository(
        listOf(
            collection("letters", 1),
            collection("numbers", 2),
            collection("quran", 3),
            collection("stories", 4).copy(isPremium = true),
        ),
    )
    private val profiles = FakeProfileRepository(child(allowed = setOf("letters")))
    private val billing = FakeBillingRepository()
    private val links = LegalLinks(terms = "https://example.com/terms", privacy = "https://example.com/privacy")

    private fun TestScope.paywall(): Pair<PaywallViewModel, ParentSession> {
        val session = ParentSession(TestClock(testScheduler, kuwait(2026, 11, 6, 20, 0)), backgroundScope)
        session.unlock()
        val vm = PaywallViewModel(billing, catalog, EnablePremiumCollectionsUseCase(profiles, catalog), session, links)
        runCurrent()
        return vm to session
    }

    @Test
    fun annualIsSelectedByDefault_withTheSavingAndTheStoreTrial() = runViewModelTest {
        val (vm, _) = paywall()
        val state = vm.state.value

        assertFalse(state.loading)
        assertEquals(PlanPeriod.Annual, state.selectedPlan?.period)
        assertEquals(33, state.savingsPercent)
        assertEquals(7, state.selectedPlan?.trial?.days)
        assertEquals(3, state.freeCollections)
        assertNull(state.subscription)
    }

    @Test
    fun pricesAreTheStoresOwn() = runViewModelTest {
        billing.offering = Offering(
            monthly = testPlan(PlanPeriod.Monthly, 3_990_000, currency = "EGP"),
            annual = testPlan(PlanPeriod.Annual, 29_990_000, currency = "EGP", trial = null),
        )
        val (vm, _) = paywall()

        assertEquals(billing.offering?.annual?.price, vm.state.value.selectedPlan?.price)
        assertEquals(37, vm.state.value.savingsPercent)
        assertNull(vm.state.value.selectedPlan?.trial, "not eligible: the button says اشترك, not a trial")
    }

    @Test
    fun choosingMonthlyBuysMonthly() = runViewModelTest {
        val (vm, _) = paywall()
        vm.onEvent(PaywallEvent.SelectPlan(PlanPeriod.Monthly))
        vm.onEvent(PaywallEvent.Subscribe)
        runCurrent()

        assertEquals(listOf(PlanPeriod.Monthly), billing.purchased.map { it.period })
    }

    @Test
    fun aPurchaseTurnsTheSubscriptionOn_andOpensPremiumForEveryChild() = runViewModelTest {
        val (vm, _) = paywall()
        vm.onEvent(PaywallEvent.Subscribe)
        runCurrent()

        assertEquals(FULL, vm.state.value.subscription)
        assertTrue(vm.state.value.justSubscribed)
        assertEquals(setOf("letters", "stories"), profiles.getProfile(CHILD)?.allowedCollectionIds)
    }

    @Test
    fun whileTheStoreSheetIsOpen_theScreenIsBusy_andParentModeSurvivesBackground() = runViewModelTest {
        val gate = CompletableDeferred<Unit>()
        billing.purchaseGate = gate
        val (vm, session) = paywall()

        vm.onEvent(PaywallEvent.Subscribe)
        runCurrent()
        assertEquals(PaywallBusy.Purchasing, vm.state.value.busy)
        vm.onEvent(PaywallEvent.Subscribe) // a second tap is ignored
        session.onAppStopped()
        assertTrue(session.isUnlocked.value)

        gate.complete(Unit)
        runCurrent()
        assertNull(vm.state.value.busy)
        assertEquals(1, billing.purchased.size)
    }

    @Test
    fun cancellingShowsNothing() = runViewModelTest {
        billing.purchaseOutcome = PurchaseOutcome.Cancelled
        val (vm, _) = paywall()
        vm.onEvent(PaywallEvent.Subscribe)
        runCurrent()

        assertNull(vm.state.value.message)
        assertNull(vm.state.value.subscription)
        assertEquals(setOf("letters"), profiles.getProfile(CHILD)?.allowedCollectionIds)
    }

    @Test
    fun pendingAndErrorsShowAMessage() = runViewModelTest {
        val (vm, _) = paywall()
        billing.purchaseOutcome = PurchaseOutcome.Pending
        vm.onEvent(PaywallEvent.Subscribe)
        runCurrent()
        assertEquals(PaywallMessage.Pending, vm.state.value.message)

        billing.purchaseOutcome = PurchaseOutcome.Failed(BillingError.Network)
        vm.onEvent(PaywallEvent.Subscribe)
        runCurrent()
        assertEquals(PaywallMessage.Network, vm.state.value.message)
    }

    @Test
    fun restoreFindsTheSubscriptionOrSaysThereIsNone() = runViewModelTest {
        val (vm, _) = paywall()
        vm.onEvent(PaywallEvent.Restore)
        runCurrent()
        assertEquals(PaywallMessage.NothingToRestore, vm.state.value.message)

        billing.restoreOutcome = PurchaseOutcome.Success(FULL)
        vm.onEvent(PaywallEvent.Restore)
        runCurrent()
        assertEquals(FULL, vm.state.value.subscription)
        assertEquals(setOf("letters", "stories"), profiles.getProfile(CHILD)?.allowedCollectionIds)
    }

    @Test
    fun noOfferingShowsUnavailable_andRetryLoadsAgain() = runViewModelTest {
        billing.offering = null
        val (vm, _) = paywall()
        assertTrue(vm.state.value.unavailable)

        billing.offering = TEST_OFFERING
        vm.onEvent(PaywallEvent.Retry)
        runCurrent()
        assertFalse(vm.state.value.unavailable)
    }

    @Test
    fun theEntitlementIsLive() = runViewModelTest {
        val (vm, _) = paywall()
        billing.entitlement.value = FULL
        runCurrent()
        assertEquals(FULL, vm.state.value.subscription)
        assertFalse(vm.state.value.justSubscribed)

        billing.entitlement.value = Entitlement.Free
        runCurrent()
        assertNull(vm.state.value.subscription)
    }

    @Test
    fun linksAndManageOpenOutsideTheApp() = runViewModelTest {
        billing.entitlement.value = FULL
        val (vm, _) = paywall()
        vm.effects.test {
            vm.onEvent(PaywallEvent.Terms)
            assertEquals(PaywallEffect.OpenUrl(links.terms), awaitItem())
            vm.onEvent(PaywallEvent.Privacy)
            assertEquals(PaywallEffect.OpenUrl(links.privacy), awaitItem())
            vm.onEvent(PaywallEvent.ManageSubscription)
            assertEquals(PaywallEffect.OpenUrl(FULL.managementUrl!!), awaitItem())
            vm.onEvent(PaywallEvent.Close)
            assertEquals(PaywallEffect.Close, awaitItem())
        }
    }
}
