plugins {
    alias(libs.plugins.feelo.kmp.compose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.koin.core)
            // JSON messages from the player page and the page config.
            implementation(libs.kotlinx.serialization.json)
            // ON_STOP → pause, for both platforms.
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        androidMain.dependencies {
            // BackHandler: system back leaves the player's fullscreen first.
            implementation(libs.androidx.activity.compose)
            // WindowInsetsControllerCompat: hide the system bars in fullscreen on API 28–29 too.
            implementation(libs.androidx.core)
        }
    }
}
