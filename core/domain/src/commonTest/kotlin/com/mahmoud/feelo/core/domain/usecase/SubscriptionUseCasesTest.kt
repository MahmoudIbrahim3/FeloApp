package com.mahmoud.feelo.core.domain.usecase

import app.cash.turbine.test
import com.mahmoud.feelo.core.domain.testing.FULL
import com.mahmoud.feelo.core.domain.testing.FakeBillingRepository
import com.mahmoud.feelo.core.domain.testing.FakeProfileRepository
import com.mahmoud.feelo.core.domain.testing.FakeSettingsRepository
import com.mahmoud.feelo.core.domain.testing.child
import com.mahmoud.feelo.core.model.Entitlement
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubscriptionUseCasesTest {

    private val first = child(id = "a")
    private val second = child(id = "b")
    private val third = child(id = "c")

    @Test
    fun freeUnlocksOnlyTheFirstChild() {
        val access = Access(isFull = false, childIds = listOf("a", "b", "c"))

        assertEquals(setOf("a"), access.unlockedChildIds)
        assertTrue(access.isChildLocked("b"))
        assertEquals("a", access.firstChildId)
        assertFalse(access.canAddChild)
        assertTrue(access.addChildNeedsSubscription)
    }

    @Test
    fun fullUnlocksEveryoneUpToFour() {
        assertTrue(Access(isFull = true, childIds = listOf("a", "b", "c")).canAddChild)

        val atLimit = Access(isFull = true, childIds = listOf("a", "b", "c", "d"))
        assertEquals(setOf("a", "b", "c", "d"), atLimit.unlockedChildIds)
        assertFalse(atLimit.canAddChild)
        assertTrue(atLimit.atFullLimit)
        assertFalse(atLimit.addChildNeedsSubscription)
    }

    @Test
    fun freeWithNoChildCanAddTheFirst() {
        assertTrue(Access(isFull = false, childIds = emptyList()).canAddChild)
    }

    @Test
    fun accessFollowsTheEntitlementAndWaitsForAKnownOne() = runTest {
        val billing = FakeBillingRepository(Entitlement.Unknown)
        val observe = ObserveAccessUseCase(billing, FakeProfileRepository(first, second, third))

        observe().test {
            expectNoEvents()
            billing.entitlement.value = Entitlement.Free
            assertEquals(setOf("a"), awaitItem().unlockedChildIds)
            billing.entitlement.value = FULL
            assertEquals(setOf("a", "b", "c"), awaitItem().unlockedChildIds)
            billing.entitlement.value = Entitlement.Free
            assertEquals(setOf("a"), awaitItem().unlockedChildIds)
        }
    }

    @Test
    fun weeklyActivityIsAFullFeature() {
        assertFalse(Access(isFull = false, childIds = listOf("a")).showsWeeklyActivity)
        assertTrue(Access(isFull = true, childIds = listOf("a")).showsWeeklyActivity)
    }

    // region ConnectBillingUseCase

    private val now = Instant.parse("2026-11-07T09:00:00Z")
    private val clock = object : Clock {
        override fun now(): Instant = this@SubscriptionUseCasesTest.now
    }

    private fun connect(billing: FakeBillingRepository, consent: Boolean) =
        ConnectBillingUseCase(billing, FakeSettingsRepository(consentGiven = consent), clock)

    @Test
    fun parentModeNeverConnectsWithoutConsent() = runTest {
        val billing = FakeBillingRepository()

        assertFalse(connect(billing, consent = false).forParent())
        assertEquals(0, billing.startCalls)
    }

    @Test
    fun parentModeConnectsOnceThenRefreshes() = runTest {
        val billing = FakeBillingRepository()
        val useCase = connect(billing, consent = true)

        assertTrue(useCase.forParent())
        assertTrue(useCase.forParent())

        assertEquals(1, billing.startCalls)
        assertEquals(1, billing.refreshCalls)
    }

    @Test
    fun agreeingRecordsConsentAndConnects() = runTest {
        val billing = FakeBillingRepository()
        val settings = FakeSettingsRepository(consentGiven = false)

        ConnectBillingUseCase(billing, settings, clock).agreeAndConnect()

        assertTrue(settings.consent.value)
        assertTrue(billing.started)
    }

    @Test
    fun childModeDoesNotConnectForAFreeOrRunningSubscription() = runTest {
        val free = FakeBillingRepository(Entitlement.Free)
        connect(free, consent = true).ifSubscriptionEnded()
        assertEquals(0, free.startCalls)

        val running = FakeBillingRepository(FULL.copy(expiresAt = now + 1.days))
        connect(running, consent = true).ifSubscriptionEnded()
        assertEquals(0, running.startCalls)

        val lifetime = FakeBillingRepository(FULL.copy(expiresAt = null))
        connect(lifetime, consent = true).ifSubscriptionEnded()
        assertEquals(0, lifetime.startCalls)
    }

    @Test
    fun anEndedCachedSubscriptionIsCheckedAgainOnlyWithConsent() = runTest {
        val ended = FULL.copy(expiresAt = now - 1.hours)

        val withoutConsent = FakeBillingRepository(ended)
        connect(withoutConsent, consent = false).ifSubscriptionEnded()
        assertEquals(0, withoutConsent.startCalls)

        val withConsent = FakeBillingRepository(ended)
        connect(withConsent, consent = true).ifSubscriptionEnded()
        assertEquals(1, withConsent.startCalls)
    }

    // endregion
}
