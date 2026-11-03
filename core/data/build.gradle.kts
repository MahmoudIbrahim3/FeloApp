plugins {
    alias(libs.plugins.clipkid.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(libs.koin.core)
        }
    }
}
