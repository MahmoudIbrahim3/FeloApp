plugins {
    alias(libs.plugins.clipkid.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            // Repositories expose Flows; use cases combine them.
            api(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
        commonTest.dependencies {
            // Checks the exact JSON sent when a parent hides a video.
            implementation(libs.kotlinx.serialization.json)
            implementation(projects.core.testing)
        }
    }
}
