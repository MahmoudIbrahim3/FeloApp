plugins {
    alias(libs.plugins.feelo.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Features depend only on core modules, never on each other.
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
            implementation(libs.koin.core)
            // ViewModel base class for every screen (built in shared, the composition root).
            implementation(libs.androidx.lifecycle.viewmodelCompose)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
