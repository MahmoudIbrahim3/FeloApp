import com.codingfeline.buildkonfig.compiler.FieldSpec
import java.util.Properties

plugins {
    alias(libs.plugins.feelo.kmp.library)
    // RevenueCat public SDK keys as build-time constants (from local.properties or env).
    alias(libs.plugins.buildKonfig)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Implements BillingRepository.
            implementation(projects.core.domain)
            implementation(libs.koin.core)
            // Subscriptions through RevenueCat (Play Billing on Android, StoreKit on iOS).
            implementation(libs.revenuecat.purchases.core)
        }
        androidMain.dependencies {
            // androidContext() to tell debug from release builds.
            implementation(libs.koin.android)
        }
    }
}

// RevenueCat public SDK keys. Not secret, but never committed: put them in local.properties
// (gitignored)
//   revenuecat.androidApiKey=goog_...
//   revenuecat.iosApiKey=appl_...
//   revenuecat.testStoreApiKey=test_...   (optional, debug builds only)
// or set FEELO_RC_ANDROID_KEY / FEELO_RC_IOS_KEY / FEELO_RC_TEST_KEY (CI). Empty values
// still build; the app then uses a fake store and logs a warning.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun billingSetting(property: String, env: String): String =
    providers.environmentVariable(env).orNull?.takeIf { it.isNotBlank() }
        ?: localProperties.getProperty(property)?.trim().orEmpty()

buildkonfig {
    packageName = "com.mahmoud.feelo.core.billing.config"
    objectName = "BillingBuildConfig"
    defaultConfigs {
        buildConfigField(FieldSpec.Type.STRING, "RC_ANDROID_KEY", billingSetting("revenuecat.androidApiKey", "FEELO_RC_ANDROID_KEY"))
        buildConfigField(FieldSpec.Type.STRING, "RC_IOS_KEY", billingSetting("revenuecat.iosApiKey", "FEELO_RC_IOS_KEY"))
        buildConfigField(FieldSpec.Type.STRING, "RC_TEST_STORE_KEY", billingSetting("revenuecat.testStoreApiKey", "FEELO_RC_TEST_KEY"))
    }
}
