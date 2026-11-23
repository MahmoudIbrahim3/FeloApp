package com.mahmoud.feelo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import com.mahmoud.feelo.core.designsystem.theme.ProvideRtl
import com.mahmoud.feelo.core.designsystem.theme.ProvideWindowSize
import com.mahmoud.feelo.navigation.FeeloNavHost

/**
 * App root shared by Android and iOS.
 * RTL is forced here for the whole app; Arabic copy lives in the default `values/strings.xml`
 * and every text style carries the `ar-EG` locale (see FeeloTypography).
 */
@Composable
fun App() {
    setSingletonImageLoaderFactory { context ->
        // Thumbnails only; Coil's memory + disk cache is the only image cache (CLAUDE.md rule 5).
        // The disk cache is wiped every 7 days at launch (PruneImageCacheUseCase, compliance A10).
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory()) }
            .crossfade(true)
            .build()
    }
    ProvideRtl {
        FeeloTheme {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(FeeloTheme.colors.background),
            ) {
                ProvideWindowSize {
                    FeeloNavHost()
                }
            }
        }
    }
}
