plugins {
    alias(libs.plugins.clipkid.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(projects.core.model)
            implementation(libs.koin.core)
        }
    }
}
