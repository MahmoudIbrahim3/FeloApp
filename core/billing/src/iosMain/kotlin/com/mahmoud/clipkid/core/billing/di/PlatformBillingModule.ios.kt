package com.mahmoud.clipkid.core.billing.di

import com.mahmoud.clipkid.core.billing.config.BillingBuildConfig
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform

internal actual val platformStoreApiKey: String get() = BillingBuildConfig.RC_IOS_KEY

@OptIn(ExperimentalNativeApi::class)
internal actual val platformBillingModule: Module = module {
    single { BillingConfig(platformStoreApiKey, testStoreApiKey, isDebugBuild = Platform.isDebugBinary) }
}
