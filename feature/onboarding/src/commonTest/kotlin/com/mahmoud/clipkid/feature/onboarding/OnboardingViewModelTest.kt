package com.mahmoud.clipkid.feature.onboarding

import app.cash.turbine.test
import com.mahmoud.clipkid.core.domain.testing.FULL
import com.mahmoud.clipkid.core.domain.testing.FakeBillingRepository
import com.mahmoud.clipkid.core.domain.testing.FakeCatalogRepository
import com.mahmoud.clipkid.core.domain.testing.FakeProfileRepository
import com.mahmoud.clipkid.core.domain.testing.FakeSettingsRepository
import com.mahmoud.clipkid.core.domain.testing.SequentialIdGenerator
import com.mahmoud.clipkid.core.domain.testing.collection
import com.mahmoud.clipkid.core.domain.testing.runViewModelTest
import com.mahmoud.clipkid.core.domain.usecase.AddChildUseCase
import com.mahmoud.clipkid.core.domain.usecase.CompleteOnboardingUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetCollectionsForAgeUseCase
import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.core.model.DailyLimit
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
            collection("numbers", 2, ageBands = setOf(AgeBand.A4_6)),
            collection("stories", 3).copy(isPremium = true),
            collection("science", 4, ageBands = setOf(AgeBand.A7_9)),
        ),
        emptyList(),
    )
    private val profiles = FakeProfileRepository()
    private val settings = FakeSettingsRepository()
    private val billing = FakeBillingRepository()
    private val addChild = AddChildUseCase(profiles, settings, SequentialIdGenerator("kid-"), catalog, billing)

    private fun TestScope.viewModel(mode: OnboardingMode = OnboardingMode.FirstRun) = OnboardingViewModel(
        mode,
        GetCollectionsForAgeUseCase(catalog),
        CompleteOnboardingUseCase(settings, addChild),
        addChild,
        billing,
    ).also { runCurrent() }

    private fun OnboardingViewModel.typePin(pin: String) {
        pin.forEach { onEvent(OnboardingEvent.PinDigit(it - '0')) }
    }

    @Test
    fun pinMustBeEnteredTwiceTheSame() = runViewModelTest {
        val vm = viewModel()
        vm.effects.test {
            vm.typePin("2468")
            vm.onEvent(OnboardingEvent.PinNext)
            runCurrent()
            assertTrue(vm.state.value.pin.confirming)
            assertEquals("", vm.state.value.pin.entered)

            vm.typePin("2468")
            vm.onEvent(OnboardingEvent.PinNext)
            assertEquals(OnboardingEffect.OpenChild, awaitItem())
        }
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
    fun ageBandPreSelectsItsCollectionsExceptPremium() = runViewModelTest {
        val vm = viewModel()
        vm.onEvent(OnboardingEvent.AgePicked(AgeBand.A4_6))
        runCurrent()

        val options = vm.state.value.collections
        assertEquals(listOf("letters", "numbers", "stories"), options.map { it.collection.id })
        assertEquals(listOf(true, true, false), options.map { it.selected })
        assertTrue(options.last().locked)
    }

    @Test
    fun premiumCannotBeToggledOnAndChangingTheAgeResetsTheChoice() = runViewModelTest {
        val vm = viewModel()
        vm.onEvent(OnboardingEvent.AgePicked(AgeBand.A4_6))
        runCurrent()
        vm.onEvent(OnboardingEvent.CollectionToggled("stories", on = true))
        vm.onEvent(OnboardingEvent.CollectionToggled("numbers", on = false))
        runCurrent()
        assertEquals(setOf("letters"), vm.state.value.collections.filter { it.selected }.map { it.collection.id }.toSet())

        vm.onEvent(OnboardingEvent.AgePicked(AgeBand.A7_9))
        runCurrent()
        assertEquals(listOf("letters", "science"), vm.state.value.collections.filter { it.selected }.map { it.collection.id })
    }

    @Test
    fun childStepNeedsANicknameAndAnAge() = runViewModelTest {
        val vm = viewModel()
        vm.onEvent(OnboardingEvent.NicknameChanged("   "))
        vm.onEvent(OnboardingEvent.AgePicked(AgeBand.A4_6))
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
        vm.onEvent(OnboardingEvent.NicknameChanged("يوسف"))
        vm.onEvent(OnboardingEvent.AgePicked(AgeBand.A4_6))
        vm.onEvent(OnboardingEvent.LimitChanged(DailyLimit.Minutes(30)))
        runCurrent()
        assertNull(settings.pin.value, "nothing is saved before Finish")

        vm.effects.test {
            assertEquals(OnboardingEffect.OpenChild, awaitItem())
            vm.onEvent(OnboardingEvent.FinishClicked)
            val finished = awaitItem() as OnboardingEffect.Finished

            assertTrue(settings.verifyPin("2468"))
            assertTrue(settings.onboardingDone.first())
            assertEquals(finished.childId, settings.activeChildId.first())
            val saved = assertNotNull(profiles.getProfile(finished.childId))
            assertEquals("يوسف", saved.nickname)
            assertEquals(DailyLimit.Minutes(30), saved.dailyLimit)
            assertEquals(setOf("letters", "numbers"), saved.allowedCollectionIds)
        }
        assertEquals("", vm.state.value.pin.entered, "the PIN is dropped from memory")
    }

    @Test
    fun addChildModeSkipsThePinAndDoesNotTouchOnboardingOrThePin() = runViewModelTest {
        settings.setPin("1111")
        val vm = viewModel(OnboardingMode.AddChild)
        assertEquals(2, vm.state.value.totalSteps)

        vm.onEvent(OnboardingEvent.NicknameChanged("مريم"))
        vm.onEvent(OnboardingEvent.AgePicked(AgeBand.A7_9))
        runCurrent()
        vm.effects.test {
            vm.onEvent(OnboardingEvent.FinishClicked)
            val finished = awaitItem() as OnboardingEffect.Finished
            assertEquals("مريم", profiles.getProfile(finished.childId)?.nickname)
        }
        assertTrue(settings.verifyPin("1111"))
        assertFalse(settings.onboardingDone.first())
    }

    @Test
    fun withTheSubscriptionPremiumIsPreSelectedAndCanBeToggled() = runViewModelTest {
        billing.entitlement.value = FULL
        val vm = viewModel(OnboardingMode.AddChild)
        vm.onEvent(OnboardingEvent.AgePicked(AgeBand.A4_6))
        runCurrent()

        val options = vm.state.value.collections
        assertEquals(listOf(true, true, true), options.map { it.selected })
        assertFalse(options.last().locked)

        vm.onEvent(OnboardingEvent.CollectionToggled("stories", on = false))
        runCurrent()
        assertFalse(vm.state.value.collections.last().selected)
    }
}
