package com.mahmoud.clipkid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.core.designsystem.theme.ProvideRtl
import com.mahmoud.clipkid.core.designsystem.theme.ProvideWindowSize
import com.mahmoud.clipkid.navigation.ClipKidNavHost

/**
 * App root shared by Android and iOS.
 * RTL is forced here for the whole app; Arabic copy lives in the default `values/strings.xml`
 * and every text style carries the `ar-EG` locale (see ClipKidTypography).
 */
@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        // Thumbnails only; Coil's memory + HTTP cache is the only image cache (CLAUDE.md rule 5).
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }
    ProvideRtl {
        ClipKidTheme {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(ClipKidTheme.colors.background),
            ) {
                ProvideWindowSize {
                    ClipKidNavHost()
                }
            }
        }
    }
}
