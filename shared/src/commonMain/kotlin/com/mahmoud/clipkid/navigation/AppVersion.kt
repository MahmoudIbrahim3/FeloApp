package com.mahmoud.clipkid.navigation

import androidx.compose.runtime.Composable

/**
 * The installed app's version name (Android `versionName`, iOS `CFBundleShortVersionString`),
 * shown on the About screen. Empty if the platform can't tell.
 */
@Composable
internal expect fun rememberAppVersion(): String
