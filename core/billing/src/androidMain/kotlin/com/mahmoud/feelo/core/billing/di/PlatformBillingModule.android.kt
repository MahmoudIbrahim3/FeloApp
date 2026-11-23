package com.mahmoud.feelo.core.billing.di

import android.content.pm.ApplicationInfo
import com.mahmoud.feelo.core.billing.config.BillingBuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

internal actual val platformStoreApiKey: String get() = BillingBuildConfig.RC_ANDROID_KEY

internal actual val platformBillingModule: Module = module {
    single {
        val debuggable = (androidContext().applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        BillingConfig(platformStoreApiKey, testStoreApiKey, isDebugBuild = debuggable)
    }
}
