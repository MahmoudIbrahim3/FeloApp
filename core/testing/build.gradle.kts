plugins {
    alias(libs.plugins.clipkid.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Hand-written fakes for the domain repositories, shared by core:domain and feature tests.
            // Test-only: no app module depends on it at runtime.
            api(projects.core.domain)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
