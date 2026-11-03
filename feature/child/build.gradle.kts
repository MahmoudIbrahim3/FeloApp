plugins {
    alias(libs.plugins.clipkid.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Features depend only on core modules, never on each other.
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
            implementation(libs.koin.core)
        }
    }
}
