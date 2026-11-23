plugins {
    alias(libs.plugins.feelo.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // CollectionCover renders the domain CoverStyle directly.
            api(projects.core.model)
            // VideoCard loads YouTube thumbnails through Coil (memory/HTTP cache only).
            implementation(libs.coil.compose)
            implementation(libs.koin.core)
        }
    }
}
