plugins {
    alias(libs.plugins.feelo.kmp.library)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Catalog models cross the network (Supabase) and the Room cache.
            api(libs.kotlinx.serialization.core)
            // LocalDate in ExtraGrant; Instant/Clock come from kotlin.time.
            api(libs.kotlinx.datetime)
        }
    }
}
