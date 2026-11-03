package com.mahmoud.clipkid.di

import com.mahmoud.clipkid.core.billing.di.billingModule
import com.mahmoud.clipkid.core.data.di.dataModule
import com.mahmoud.clipkid.core.designsystem.di.designSystemModule
import com.mahmoud.clipkid.core.domain.di.domainModule
import com.mahmoud.clipkid.core.player.di.playerModule
import com.mahmoud.clipkid.feature.child.di.childFeatureModule
import com.mahmoud.clipkid.feature.onboarding.di.onboardingFeatureModule
import com.mahmoud.clipkid.feature.parent.di.parentFeatureModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatform

/** One Koin module per Gradle module (core:model has no DI). */
val appModules: List<Module> = listOf(
    designSystemModule,
    domainModule,
    dataModule,
    playerModule,
    billingModule,
    onboardingFeatureModule,
    childFeatureModule,
    parentFeatureModule,
)

/**
 * Starts Koin once. Android calls it from `ClipKidApplication` (with `androidContext`),
 * iOS from `MainViewController`. Later calls are ignored.
 */
fun initKoin(platformConfig: KoinAppDeclaration = {}): KoinApplication? {
    if (KoinPlatform.getKoinOrNull() != null) return null
    return startKoin {
        platformConfig()
        modules(appModules)
    }
}
