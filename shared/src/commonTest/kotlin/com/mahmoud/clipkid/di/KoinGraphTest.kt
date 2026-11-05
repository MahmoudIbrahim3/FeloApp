package com.mahmoud.clipkid.di

import com.mahmoud.clipkid.core.billing.di.noStoreBillingModule
import com.mahmoud.clipkid.core.billing.fake.NoStoreBillingRepository
import com.mahmoud.clipkid.core.data.catalog.CatalogRemoteDataSource
import com.mahmoud.clipkid.core.data.catalog.FakeCatalogDataSource
import com.mahmoud.clipkid.core.data.di.DataConfig
import com.mahmoud.clipkid.core.data.di.inMemoryPlatformDataModule
import com.mahmoud.clipkid.core.domain.parent.CreateAdultChallengeUseCase
import com.mahmoud.clipkid.core.domain.parent.ParentSession
import com.mahmoud.clipkid.core.domain.parent.ResetPinUseCase
import com.mahmoud.clipkid.core.domain.parent.UnlockWithPinUseCase
import com.mahmoud.clipkid.core.domain.repository.BillingRepository
import com.mahmoud.clipkid.core.domain.repository.CatalogRepository
import com.mahmoud.clipkid.core.domain.repository.FavoritesRepository
import com.mahmoud.clipkid.core.domain.repository.HiddenVideosRepository
import com.mahmoud.clipkid.core.domain.repository.IdGenerator
import com.mahmoud.clipkid.core.domain.repository.NetworkMonitor
import com.mahmoud.clipkid.core.domain.repository.ProfileRepository
import com.mahmoud.clipkid.core.domain.repository.SettingsRepository
import com.mahmoud.clipkid.core.domain.repository.VideoReporter
import com.mahmoud.clipkid.core.domain.repository.WatchHistoryRepository
import com.mahmoud.clipkid.core.domain.usecase.AddChildUseCase
import com.mahmoud.clipkid.core.domain.usecase.CompleteOnboardingUseCase
import com.mahmoud.clipkid.core.domain.usecase.EnablePremiumCollectionsUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetCollectionUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetCollectionsForAgeUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetDailyQuotaUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetFavoritesUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetHomeFeedUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetResumePositionUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetUpNextUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetWeeklyActivityUseCase
import com.mahmoud.clipkid.core.domain.usecase.GrantExtraVideosUseCase
import com.mahmoud.clipkid.core.domain.usecase.HideVideoUseCase
import com.mahmoud.clipkid.core.domain.usecase.ObserveAccessUseCase
import com.mahmoud.clipkid.core.domain.usecase.ReportVideoUseCase
import com.mahmoud.clipkid.core.domain.usecase.StartWatchUseCase
import com.mahmoud.clipkid.core.domain.usecase.SyncCatalogUseCase
import com.mahmoud.clipkid.core.domain.usecase.ToggleFavoriteUseCase
import com.mahmoud.clipkid.core.domain.usecase.UpdateChildSettingsUseCase
import com.mahmoud.clipkid.core.domain.usecase.UpdateWatchProgressUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication

class KoinGraphTest {

    /**
     * The real graph with platform storage swapped for in-memory storage and billing without a
     * store (no Android Context in host tests, never RevenueCat).
     */
    private fun withGraph(block: KoinApplication.() -> Unit) {
        val app = koinApplication {
            allowOverride(true)
            modules(appModules + inMemoryPlatformDataModule() + noStoreBillingModule())
        }
        try {
            app.block()
        } finally {
            app.close()
        }
    }

    @Test
    fun everyGradleModuleContributesOneKoinModule() {
        // designsystem, domain, data, player, billing, onboarding, child, parent
        assertEquals(8, appModules.size)
        assertEquals(appModules.size, appModules.toSet().size)
    }

    @Test
    fun graphLoads() = withGraph {
        // Resolving every definition catches missing bindings.
        koin.getAll<Any>()
    }

    @Test
    fun repositoriesAndUseCasesResolve() = withGraph {
        listOf(
            koin.get<CatalogRepository>(),
            koin.get<ProfileRepository>(),
            koin.get<WatchHistoryRepository>(),
            koin.get<FavoritesRepository>(),
            koin.get<HiddenVideosRepository>(),
            koin.get<SettingsRepository>(),
            koin.get<VideoReporter>(),
            koin.get<IdGenerator>(),
            koin.get<GetHomeFeedUseCase>(),
            koin.get<GetDailyQuotaUseCase>(),
            koin.get<StartWatchUseCase>(),
            koin.get<GetUpNextUseCase>(),
            koin.get<UpdateWatchProgressUseCase>(),
            koin.get<GrantExtraVideosUseCase>(),
            koin.get<HideVideoUseCase>(),
            koin.get<ToggleFavoriteUseCase>(),
            koin.get<SyncCatalogUseCase>(),
            // Phase 2 (child screens) and Phase 3 (onboarding, gate, parent mode).
            koin.get<GetCollectionUseCase>(),
            koin.get<GetFavoritesUseCase>(),
            koin.get<GetCollectionsForAgeUseCase>(),
            koin.get<AddChildUseCase>(),
            koin.get<CompleteOnboardingUseCase>(),
            koin.get<UpdateChildSettingsUseCase>(),
            koin.get<GetWeeklyActivityUseCase>(),
            koin.get<UnlockWithPinUseCase>(),
            koin.get<ResetPinUseCase>(),
            koin.get<CreateAdultChallengeUseCase>(),
            // Phase 4 (player).
            koin.get<GetResumePositionUseCase>(),
            koin.get<ReportVideoUseCase>(),
            koin.get<NetworkMonitor>(),
            koin.get<ParentSession>(),
            // Phase 6 (subscriptions).
            koin.get<BillingRepository>(),
            koin.get<ObserveAccessUseCase>(),
            koin.get<EnablePremiumCollectionsUseCase>(),
        ).forEach { assertTrue(it !is Unit) }
    }

    @Test
    fun testsNeverConfigureRevenueCat() = withGraph {
        assertIs<NoStoreBillingRepository>(koin.get<BillingRepository>())
    }

    @Test
    fun testsAndPreviewsUseTheFakeCatalog_neverTheNetwork() = withGraph {
        assertTrue(koin.get<DataConfig>().useFakeCatalog)
        assertIs<FakeCatalogDataSource>(koin.get<CatalogRemoteDataSource>())
    }
}
