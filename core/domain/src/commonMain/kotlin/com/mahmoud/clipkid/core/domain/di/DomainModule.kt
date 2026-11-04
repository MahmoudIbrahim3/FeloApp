package com.mahmoud.clipkid.core.domain.di

import com.mahmoud.clipkid.core.domain.usecase.GetDailyQuotaUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetHomeFeedUseCase
import com.mahmoud.clipkid.core.domain.usecase.GetUpNextUseCase
import com.mahmoud.clipkid.core.domain.usecase.GrantExtraVideosUseCase
import com.mahmoud.clipkid.core.domain.usecase.HideVideoUseCase
import com.mahmoud.clipkid.core.domain.usecase.RefreshCatalogIfStaleUseCase
import com.mahmoud.clipkid.core.domain.usecase.StartWatchUseCase
import com.mahmoud.clipkid.core.domain.usecase.ToggleFavoriteUseCase
import com.mahmoud.clipkid.core.domain.usecase.UpdateWatchProgressUseCase
import kotlinx.datetime.TimeZone
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import kotlin.time.Clock

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
}
