plugins {
    alias(libs.plugins.feelo.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Features depend only on core modules, never on each other.
            implementation(projects.core.designsystem)
            implementation(projects.core.domain)
            // The official YouTube embedded player on the Player screen.
            implementation(projects.core.player)
            implementation(libs.koin.core)
            // ViewModel base class for every child screen (Koin resolves them in shared).
            implementation(libs.androidx.lifecycle.viewmodelCompose)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
