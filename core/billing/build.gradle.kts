plugins {
    alias(libs.plugins.clipkid.kmp.library)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
        }
    }
}
