package com.mahmoud.clipkid.core.domain.usecase

import com.mahmoud.clipkid.core.domain.testing.CHILD
import com.mahmoud.clipkid.core.domain.testing.FULL
import com.mahmoud.clipkid.core.domain.testing.FakeBillingRepository
import com.mahmoud.clipkid.core.domain.testing.FakeCatalogRepository
import com.mahmoud.clipkid.core.domain.testing.FakeProfileRepository
import com.mahmoud.clipkid.core.domain.testing.FakeSettingsRepository
import com.mahmoud.clipkid.core.domain.testing.SequentialIdGenerator
import com.mahmoud.clipkid.core.domain.testing.child
import com.mahmoud.clipkid.core.domain.testing.collection
import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.core.model.DailyLimit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class ChildSetupUseCasesTest {

    private val collections = listOf(
        collection("numbers", sortOrder = 2, ageBands = setOf(AgeBand.A6_8, AgeBand.A9_12)),
        collection("letters", sortOrder = 1),
        collection("stories", sortOrder = 3),
        collection("science", sortOrder = 4, ageBands = setOf(AgeBand.A9_12)),
    )
    private val catalog = FakeCatalogRepository(collections, emptyList())
    private val profiles = FakeProfileRepository()
    private val settings = FakeSettingsRepository()
    private val billing = FakeBillingRepository()
    private val addChild = AddChildUseCase(profiles, settings, SequentialIdGenerator("kid-"), billing)

    private fun newChild(nickname: String = "  ليلى ", allowed: Set<String> = setOf("letters", "numbers")) = NewChild(
        nickname = nickname,
        avatarColor = 0xFF2563D9,
        ageBand = AgeBand.A6_8,
        allowedCollectionIds = allowed,
        dailyLimit = DailyLimit.Videos(5),
    )

    @Test
    fun collectionsForAgeAreFilteredAndSorted_andAllPreSelected() = runTest {
        val forAge = GetCollectionsForAgeUseCase(catalog)(AgeBand.A6_8).first()

        assertEquals(listOf("letters", "numbers", "stories"), forAge.map { it.id })
        assertEquals(setOf("letters", "numbers", "stories"), GetCollectionsForAgeUseCase.defaultSelection(forAge))
    }

    @Test
    fun addChildSavesTheTrimmedNicknameAndMakesItActive() = runTest {
        val id = addChild(newChild())

        val saved = assertNotNull(profiles.getProfile(id))
        assertEquals("ليلى", saved.nickname)
        assertEquals(DailyLimit.Videos(5), saved.dailyLimit)
        assertEquals(id, settings.activeChildId.first())
    }

    @Test
    fun addChildKeepsEveryChosenCollectionOnTheFreePlan() = runTest {
        val id = addChild(newChild(allowed = setOf("letters", "stories")))
        assertEquals(setOf("letters", "stories"), profiles.getProfile(id)?.allowedCollectionIds)
    }

    @Test
    fun nicknameMustNotBeBlankOrTooLong() = runTest {
        assertFailsWith<IllegalArgumentException> { addChild(newChild(nickname = "   ")) }
        assertFailsWith<IllegalArgumentException> { addChild(newChild(nickname = "a".repeat(NICKNAME_MAX_LENGTH + 1))) }
        assertTrue(isValidNickname("a".repeat(NICKNAME_MAX_LENGTH)))
    }

    @Test
    fun completingOnboardingSavesPinChildAndTheDoneFlag() = runTest {
        assertFalse(settings.onboardingDone.first())

        val id = CompleteOnboardingUseCase(settings, addChild)("1357", newChild())

        assertTrue(settings.verifyPin("1357"))
        assertTrue(settings.consentGiven.first(), "onboarding can't finish without the consent step")
        assertTrue(settings.onboardingDone.first())
        assertEquals(id, settings.activeChildId.first())
    }

    @Test
    fun invalidChildSavesNothingAtTheEndOfOnboarding() = runTest {
        assertFailsWith<IllegalArgumentException> {
            CompleteOnboardingUseCase(settings, addChild)("1357", newChild(nickname = ""))
        }
        assertFalse(settings.onboardingDone.first())
        assertNull(settings.activeChildId.first())
    }

    @Test
    fun dashboardChangesTheLimitAndTheCollections() = runTest {
        profiles.upsert(child(limit = DailyLimit.Videos(3), allowed = setOf("letters")))
        val update = UpdateChildSettingsUseCase(profiles, catalog)

        update.setDailyLimit(CHILD, DailyLimit.Minutes(30))
        assertTrue(update.setCollectionAllowed(CHILD, "numbers", allowed = true))
        assertTrue(update.setCollectionAllowed(CHILD, "letters", allowed = false))

        val saved = assertNotNull(profiles.getProfile(CHILD))
        assertEquals(DailyLimit.Minutes(30), saved.dailyLimit)
        assertEquals(setOf("numbers"), saved.allowedCollectionIds)
    }

    @Test
    fun dashboardTurnsOnAnyCollectionOnTheFreePlanButNotUnknownOnes() = runTest {
        profiles.upsert(child(allowed = setOf("letters")))
        val update = UpdateChildSettingsUseCase(profiles, catalog)

        assertTrue(update.setCollectionAllowed(CHILD, "stories", allowed = true))
        assertFalse(update.setCollectionAllowed(CHILD, "nope", allowed = true))
        assertEquals(setOf("letters", "stories"), profiles.getProfile(CHILD)?.allowedCollectionIds)
    }

    @Test
    fun freeAllowsOneChildAndFullAllowsFour() = runTest {
        addChild(newChild(nickname = "١"))
        val free = assertFailsWith<ChildLimitReachedException> { addChild(newChild(nickname = "٢")) }
        assertEquals(1, free.max)

        billing.entitlement.value = FULL
        repeat(3) { addChild(newChild(nickname = "ب$it")) }
        val full = assertFailsWith<ChildLimitReachedException> { addChild(newChild(nickname = "٥")) }
        assertEquals(4, full.max)
        assertEquals(4, profiles.observeProfiles().first().size)
    }
}
