package com.mahmoud.clipkid.core.domain.di

import com.mahmoud.clipkid.core.domain.parent.CreateAdultChallengeUseCase
import com.mahmoud.clipkid.core.domain.parent.ParentSession
import com.mahmoud.clipkid.core.domain.parent.ResetPinUseCase
import com.mahmoud.clipkid.core.domain.parent.UnlockWithPinUseCase
import com.mahmoud.clipkid.core.domain.usecase.AddChildUseCase
import com.mahmoud.clipkid.core.domain.usecase.CompleteOnboardingUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetCollectionsForAgeUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetDailyQuotaUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetHomeFeedUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetUpNextUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetWeeklyActivityUseCase
import com.mahmoud.clipkid.core.domain.usecase.GrantExtraVideosUseCase
import com.mahmoud.clipkid.core.domain.usecase.HideVideoUseCase
import com.mahmoud.clipkid.core.domain.usecase.RefreshCatalogIfStaleUseCase
import com.mahmoud.clipkid.core.domain.usecase.StartWatchUseCase
import com.mahmoud.clipkid.core.domain.usecase.ToggleFavoriteUseCase
import com.mahmoud.clipkid.core.domain.usecase.UpdateChildSettingsUseCase
import com.mahmoud.clipkid.core.domain.usecase.UpdateWatchProgressUseCase
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.datetime.TimeZone
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

/**
 * Use cases are factories: each screen gets fresh instances, and the time zone is read again
 * whenever one is created (the device zone can change while travelling).
 */
val domainModule: Module = module {
    single<Clock> { Clock.System }
    factory<TimeZone> { TimeZone.currentSystemDefault() }

    factoryOf(::GetDailyQuotaUseCase)
    factoryOf(::GetHomeFeedUseCase)
    factoryOf(::GetUpNextUseCase)
    factoryOf(::StartWatchUseCase)
    factoryOf(::UpdateWatchProgressUseCase)
    factoryOf(::GrantExtraVideosUseCase)
    factoryOf(::HideVideoUseCase)
    factoryOf(::ToggleFavoriteUseCase)
    factory { RefreshCatalogIfStaleUseCase(get(), get()) }

    // Onboarding and parent mode
    factoryOf(::GetCollectionsForAgeUseCase)
    factoryOf(::AddChildUseCase)
    factoryOf(::CompleteOnboardingUseCase)
    factoryOf(::UpdateChildSettingsUseCase)
    factoryOf(::GetWeeklyActivityUseCase)
    factory { UnlockWithPinUseCase(get(), get()) }
    factoryOf(::ResetPinUseCase)
    factory { CreateAdultChallengeUseCase() }
    // One session for the whole app: parent mode is open or not, wherever the parent is.
    single { ParentSession(get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
}
