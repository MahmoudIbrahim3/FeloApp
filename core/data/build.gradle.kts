plugins {
    alias(libs.plugins.clipkid.kmp.library)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(libs.koin.core)
            // Local cache and history (Room 3 + bundled SQLite on both platforms).
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            // Small settings (onboarding done, active child, catalog refresh time).
            implementation(libs.androidx.datastore.preferences.core)
            // JSON columns (tags, age bands, cover) in the Room cache.
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            // androidContext() for Room, DataStore and the Keystore-backed SecureStore.
            implementation(libs.koin.android)
        }
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    listOf("kspAndroid", "kspIosArm64", "kspIosSimulatorArm64").forEach { add(it, libs.androidx.room.compiler) }
}
