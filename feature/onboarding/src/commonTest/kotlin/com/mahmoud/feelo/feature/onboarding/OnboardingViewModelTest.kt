package com.mahmoud.feelo.feature.onboarding

import app.cash.turbine.test
import com.mahmoud.feelo.core.domain.testing.FakeBillingRepository
import com.mahmoud.feelo.core.domain.testing.FakeCatalogRepository
import com.mahmoud.feelo.core.domain.testing.FakeProfileRepository
import com.mahmoud.feelo.core.domain.testing.FakeSettingsRepository
import com.mahmoud.feelo.core.domain.testing.SequentialIdGenerator
import com.mahmoud.feelo.core.domain.testing.collection
import com.mahmoud.feelo.core.domain.testing.runViewModelTest
import com.mahmoud.feelo.core.domain.usecase.AddChildUseCase
import com.mahmoud.feelo.core.domain.usecase.CompleteOnboardingUseCase
import com.mahmoud.feelo.core.domain.usecase.GetCollectionsForAgeUseCase
import com.mahmoud.feelo.core.model.AgeBand
import com.mahmoud.feelo.core.model.DailyLimit
import com.mahmoud.feelo.core.model.LegalLinks
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent

class OnboardingViewModelTest {

    private val catalog = FakeCatalogRepository(
        listOf(
            collection("letters", 1),
            collection("numbers", 2, ageBands = setOf(AgeBand.A6_8)),
            collection("stories", 3),
            collection("science", 4, ageBands = setOf(AgeBand.A9_12)),
        ),
        emptyList(),
    )
    private val profiles = FakeProfileRepository()
    private val settings = FakeSettingsRepository()
    private val billing = FakeBillingRepository()
    private val addChild = AddChildUseCase(profiles, settings, SequentialIdGenerator("kid-"), billing)

    private val links = LegalLinks(terms = "https://example.com/terms", privacy = "https://example.com/privacy", supportEmail = "help@example.com")

    private fun TestScope.viewModel(mode: OnboardingMode = OnboardingMode.FirstRun) = OnboardingViewModel(
        mode,
        GetCollectionsForAgeUseCase(catalog),
        CompleteOnboardingUseCase(settings, addChild),
        addChild,
        links,
    ).also { runCurrent() }

    private fun OnboardingViewModel.typePin(pin: String) {
        pin.forEach { onEvent(OnboardingEvent.PinDigit(it - '0')) }
    }

    @Test
    fun pinMustBeEnteredTwiceTheSame_thenTheConsentStep() = runViewModelTest {
        val vm = viewModel()
        vm.effects.test {
            vm.typePin("2468")
            vm.onEvent(OnboardingEvent.PinNext)
            runCurrent()
            assertTrue(vm.state.value.pin.confirming)
            assertEquals("", vm.state.value.pin.entered)

            vm.typePin("2468")
            vm.onEvent(OnboardingEvent.PinNext)
            assertEquals(OnboardingEffect.OpenConsent, awaitItem())
        }
    }

    @Test
    fun consentStepOpensTheLinksAndAgreeingLeadsToTheChildStep() = runViewModelTest {
        val vm = viewModel()
        assertEquals(4, vm.state.value.totalSteps)
        assertEquals(2, vm.state.value.consentStep)
        assertEquals(3, vm.state.value.childStep)

        vm.effects.test {
            vm.onEvent(OnboardingEvent.PrivacyClicked)
            assertEquals(OnboardingEffect.OpenUrl("https://example.com/privacy"), awaitItem())
            vm.onEvent(OnboardingEvent.YouTubeTermsClicked)
            assertEquals(OnboardingEffect.OpenUrl(LegalLinks.YOUTUBE_TERMS), awaitItem())

            vm.onEvent(OnboardingEvent.ConsentAgreed)
            assertEquals(OnboardingEffect.OpenChild, awaitItem())
        }
        assertTrue(vm.state.value.consentAgreed)
        assertFalse(settings.consent.value, "nothing is saved before Finish")
    }

    @Test
    fun firstRunCannotFinishWithoutConsent() = runViewModelTest {
        val vm = viewModel()
        vm.onEvent(OnboardingEvent.NicknameChanged("يوسف"))
        vm.onEvent(OnboardingEvent.AgePicked(AgeBand.A6_8))
        runCurrent()
        assertFalse(vm.state.value.canFinish)

        vm.onEvent(OnboardingEvent.ConsentAgreed)
        runCurrent()
        assertTrue(vm.state.value.canFinish)
    }

    @Test
    fun mismatchedConfirmationStartsThePinOver() = runViewModelTest {
        val vm = viewModel()
        vm.typePin("2468")
        vm.onEvent(OnboardingEvent.PinNext)
        vm.typePin("1357")
        vm.onEvent(OnboardingEvent.PinNext)
        runCurrent()

        val pin = vm.state.value.pin
        assertTrue(pin.mismatch)
        assertFalse(pin.confirming)
        assertEquals("", pin.entered)
    }

    @Test
    fun pinStopsAtFourDigitsAndNextNeedsFour() = runViewModelTest {
        val vm = viewModel()
        vm.typePin("12345")
        runCurrent()
        assertEquals("1234", vm.state.value.pin.entered)

        vm.onEvent(OnboardingEvent.PinBackspace)
        vm.onEvent(OnboardingEvent.PinNext)
        runCurrent()
        assertFalse(vm.state.value.pin.confirming)
    }

    @Test
    fun backWhileConfirmingReturnsToTheFirstEntry() = runViewModelTest {
        val vm = viewModel()
        vm.typePin("2468")
        vm.onEvent(OnboardingEvent.PinNext)
        vm.onEvent(OnboardingEvent.PinBackClicked)
        runCurrent()
        assertFalse(vm.state.value.pin.confirming)
    }

    @Test
    fun ageBandPreSelectsAllItsCollectionsOnTheFreePlan() = runViewModelTest {
        val vm = viewModel()
        vm.onEvent(OnboardingEvent.AgePicked(AgeBand.A6_8))
        runCurrent()

        val options = vm.state.value.collections
        assertEquals(listOf("letters", "numbers", "stories"), options.map { it.collection.id })
        assertEquals(listOf(true, true, true), options.map { it.selected })
    }

    @Test
    fun collectionsCanBeToggledAndChangingTheAgeResetsTheChoice() = runViewModelTest {
        val vm = viewModel()
        vm.onEvent(OnboardingEvent.AgePicked(AgeBand.A6_8))
        runCurrent()
        vm.onEvent(OnboardingEvent.CollectionToggled("stories", on = false))
        vm.onEvent(OnboardingEvent.CollectionToggled("numbers", on = false))
        runCurrent()
        assertEquals(setOf("letters"), vm.state.value.collections.filter { it.selected }.map { it.collection.id }.toSet())

        vm.onEvent(OnboardingEvent.AgePicked(AgeBand.A9_12))
        runCurrent()
        assertEquals(listOf("letters", "stories", "science"), vm.state.value.collections.filter { it.selected }.map { it.collection.id })
    }

    @Test
    fun childStepNeedsANicknameAndAnAge() = runViewModelTest {
        val vm = viewModel()
        vm.onEvent(OnboardingEvent.NicknameChanged("   "))
        vm.onEvent(OnboardingEvent.AgePicked(AgeBand.A6_8))
        runCurrent()
        assertFalse(vm.state.value.canContinueChild)

        vm.onEvent(OnboardingEvent.NicknameChanged("يوسف"))
        runCurrent()
        assertTrue(vm.state.value.canContinueChild)
        assertEquals("ي", vm.state.value.initial)
    }

    @Test
    fun finishSavesEverythingAtOnceAndOpensChildMode() = runViewModelTest {
        val vm = viewModel()
        vm.typePin("2468")
        vm.onEvent(OnboardingEvent.PinNext)
        vm.typePin("2468")
        vm.onEvent(OnboardingEvent.PinNext)
        vm.onEvent(OnboardingEvent.ConsentAgreed)
        vm.onEvent(OnboardingEvent.NicknameChanged("يوسف"))
        vm.onEvent(OnboardingEvent.AgePicked(AgeBand.A6_8))
        vm.onEvent(OnboardingEvent.LimitChanged(DailyLimit.Minutes(30)))
        runCurrent()
        assertNull(settings.pin.value, "nothing is saved before Finish")

        vm.effects.test {
            assertEquals(OnboardingEffect.OpenConsent, awaitItem())
            assertEquals(OnboardingEffect.OpenChild, awaitItem())
            vm.onEvent(OnboardingEvent.FinishClicked)
            val finished = awaitItem() as OnboardingEffect.Finished

            assertTrue(settings.verifyPin("2468"))
            assertTrue(settings.consent.value)
            assertTrue(settings.onboardingDone.first())
            assertEquals(finished.childId, settings.activeChildId.first())
            val saved = assertNotNull(profiles.getProfile(finished.childId))
            assertEquals("يوسف", saved.nickname)
            assertEquals(DailyLimit.Minutes(30), saved.dailyLimit)
            assertEquals(setOf("letters", "numbers", "stories"), saved.allowedCollectionIds)
        }
        assertEquals("", vm.state.value.pin.entered, "the PIN is dropped from memory")
    }

    @Test
    fun addChildModeSkipsThePinAndDoesNotTouchOnboardingOrThePin() = runViewModelTest {
        settings.setPin("1111")
        val vm = viewModel(OnboardingMode.AddChild)
        assertEquals(2, vm.state.value.totalSteps)

        vm.onEvent(OnboardingEvent.NicknameChanged("مريم"))
        vm.onEvent(OnboardingEvent.AgePicked(AgeBand.A9_12))
        runCurrent()
        vm.effects.test {
            vm.onEvent(OnboardingEvent.FinishClicked)
            val finished = awaitItem() as OnboardingEffect.Finished
            assertEquals("مريم", profiles.getProfile(finished.childId)?.nickname)
        }
        assertTrue(settings.verifyPin("1111"))
        assertFalse(settings.onboardingDone.first())
    }
}
