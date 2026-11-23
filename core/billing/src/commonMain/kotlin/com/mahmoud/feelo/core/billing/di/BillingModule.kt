package com.mahmoud.feelo.core.billing.di

import com.mahmoud.feelo.core.billing.config.BillingBuildConfig
import com.mahmoud.feelo.core.billing.fake.NoStoreBillingRepository
import com.mahmoud.feelo.core.billing.revenuecat.RevenueCatBillingRepository
import com.mahmoud.feelo.core.domain.repository.BillingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Which RevenueCat key the app uses.
 *
 * The Test Store key (RevenueCat's simulated store, no Apple/Google account needed) is used only
 * in debug builds, so a release can never sell through it. Remove it from local.properties to
 * test against the real sandbox stores in a debug build.
 */
data class BillingConfig(
    /** This platform's public SDK key: `goog_…` on Android, `appl_…` on iOS. */
    val storeApiKey: String,
    val testStoreApiKey: String,
    val isDebugBuild: Boolean,
) {
    val apiKey: String
        get() = if (isDebugBuild && testStoreApiKey.isNotBlank()) testStoreApiKey else storeApiKey

    val usesRevenueCat: Boolean get() = apiKey.isNotBlank()
}

/** Provides [BillingConfig]: the platform's key and whether this is a debug build. */
internal expect val platformBillingModule: Module

/** Picks this platform's key from [BillingBuildConfig]. */
internal expect val platformStoreApiKey: String

internal val testStoreApiKey: String get() = BillingBuildConfig.RC_TEST_STORE_KEY

val billingModule: Module = module {
    includes(platformBillingModule)
    single<BillingRepository> {
        val config = get<BillingConfig>()
        if (config.usesRevenueCat) {
            RevenueCatBillingRepository(
                apiKey = config.apiKey,
                debugLogs = config.isDebugBuild,
                // RevenueCat expects its calls on the main thread.
                scope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
                clock = get(),
                cache = get(),
            )
        } else {
            println(
                "Feelo: no RevenueCat key (revenuecat.androidApiKey / revenuecat.iosApiKey in local.properties). " +
                    "Billing is off: the app stays on the free plan.",
            )
            NoStoreBillingRepository()
        }
    }
}

/** Tests and previews: never configures RevenueCat. Load after [billingModule] with overrides allowed. */
fun noStoreBillingModule(): Module = module {
    single { BillingConfig(storeApiKey = "", testStoreApiKey = "", isDebugBuild = true) }
}
