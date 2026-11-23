package com.mahmoud.feelo.navigation

import com.mahmoud.feelo.core.domain.testing.CHILD
import com.mahmoud.feelo.core.domain.testing.Catalog
import com.mahmoud.feelo.core.domain.testing.FakeBillingRepository
import com.mahmoud.feelo.core.domain.testing.FakeHiddenVideosRepository
import com.mahmoud.feelo.core.domain.testing.FakeProfileRepository
import com.mahmoud.feelo.core.domain.testing.FakeSettingsRepository
import com.mahmoud.feelo.core.domain.testing.FakeWatchHistoryRepository
import com.mahmoud.feelo.core.domain.testing.KUWAIT
import com.mahmoud.feelo.core.domain.testing.TestClock
import com.mahmoud.feelo.core.domain.testing.child
import com.mahmoud.feelo.core.domain.testing.kuwait
import com.mahmoud.feelo.core.domain.testing.runViewModelTest
import com.mahmoud.feelo.core.domain.usecase.ConnectBillingUseCase
import com.mahmoud.feelo.core.domain.usecase.GetCollectionsForAgeUseCase
import com.mahmoud.feelo.core.domain.usecase.GetDailyQuotaUseCase
import com.mahmoud.feelo.core.domain.usecase.GetHomeFeedUseCase
import com.mahmoud.feelo.core.domain.usecase.GrantExtraVideosUseCase
import com.mahmoud.feelo.core.domain.usecase.ObserveAccessUseCase
import com.mahmoud.feelo.core.domain.usecase.UpdateChildSettingsUseCase
import com.mahmoud.feelo.core.model.DailyLimit
import com.mahmoud.feelo.feature.child.home.HomeUiState
import com.mahmoud.feelo.feature.child.home.HomeViewModel
import com.mahmoud.feelo.feature.parent.dashboard.DashboardEvent
import com.mahmoud.feelo.feature.parent.dashboard.DashboardViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runCurrent

/** Acceptance: a change on the parent dashboard shows on child Home at once, with no restart. */
class DashboardToChildHomeTest {

    @Test
    fun limitAndCollectionChangesReachChildHomeImmediately() = runViewModelTest {
        val clock = TestClock(testScheduler, kuwait(2026, 11, 4, 17, 0))
        val catalog = Catalog.repository()
        val profiles = FakeProfileRepository(child(DailyLimit.Videos(3), allowed = setOf("letters", "numbers")))
        val history = FakeWatchHistoryRepository()
        val settings = FakeSettingsRepository(pin = "2468", onboardingDone = true, activeChildId = CHILD)
        val quota = GetDailyQuotaUseCase(profiles, history, clock, KUWAIT)
        val billing = FakeBillingRepository()

        val home = HomeViewModel(CHILD, GetHomeFeedUseCase(catalog, profiles, history, FakeHiddenVideosRepository(), clock, KUWAIT), quota)
        val dashboard = DashboardViewModel(
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
        fun homeContent() = assertIs<HomeUiState.Content>(home.state.value)
        assertEquals(listOf("letters", "numbers"), homeContent().collections.map { it.id })
        assertEquals(3, homeContent().quota.total)

        dashboard.onEvent(DashboardEvent.LimitChanged(DailyLimit.Videos(5)))
        dashboard.onEvent(DashboardEvent.CollectionToggled("stories", allowed = true))
        dashboard.onEvent(DashboardEvent.CollectionToggled("numbers", allowed = false))
        runCurrent()

        assertEquals(5, homeContent().quota.total)
        assertEquals(listOf("letters", "stories"), homeContent().collections.map { it.id })
    }
}
