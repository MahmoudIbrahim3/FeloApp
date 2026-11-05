package com.mahmoud.clipkid.core.domain.usecase

import app.cash.turbine.test
import com.mahmoud.clipkid.core.domain.testing.FULL
import com.mahmoud.clipkid.core.domain.testing.FakeBillingRepository
import com.mahmoud.clipkid.core.domain.testing.FakeCatalogRepository
import com.mahmoud.clipkid.core.domain.testing.FakeProfileRepository
import com.mahmoud.clipkid.core.domain.testing.child
import com.mahmoud.clipkid.core.domain.testing.collection
import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.core.model.Entitlement
import kotlinx.coroutines.test.runTest
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
    fun enablePremiumAddsThePremiumCollectionsOfEachChildsAgeBand() = runTest {
        val catalog = FakeCatalogRepository(
            listOf(
                collection("letters", 1),
                collection("stories", 2, ageBands = setOf(AgeBand.A4_6, AgeBand.A7_9)).copy(isPremium = true),
                collection("science", 3, ageBands = setOf(AgeBand.A10_12)).copy(isPremium = true),
            ),
        )
        val profiles = FakeProfileRepository(
            child(id = "small", ageBand = AgeBand.A4_6, allowed = setOf("letters")),
            child(id = "big", ageBand = AgeBand.A10_12, allowed = emptySet()),
        )
        val enable = EnablePremiumCollectionsUseCase(profiles, catalog)

        enable()
        enable() // idempotent

        assertEquals(setOf("letters", "stories"), profiles.getProfile("small")?.allowedCollectionIds)
        assertEquals(setOf("science"), profiles.getProfile("big")?.allowedCollectionIds)
    }
}
