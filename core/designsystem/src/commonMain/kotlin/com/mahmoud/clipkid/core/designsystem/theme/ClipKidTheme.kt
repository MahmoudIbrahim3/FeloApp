package com.mahmoud.clipkid.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** Color used by [com.mahmoud.clipkid.core.designsystem.component.Text] and Icon when none is given. */
val LocalContentColor = compositionLocalOf { Color(0xFF17243A) }

/** Text style used by [com.mahmoud.clipkid.core.designsystem.component.Text] when none is given. */
val LocalTextStyle = compositionLocalOf { ClipKidTypography.Fallback.bodyLarge }

/**
 * Provides ClipKid tokens. It does not change layout direction: RTL is forced once at the
 * app root (see [ProvideRtl]) so every screen and preview shares one source of truth.
 */
@Composable
fun ClipKidTheme(
    colors: ClipKidColors = ClipKidColors(),
    content: @Composable () -> Unit,
) {
    val typography = rememberClipKidTypography()
    CompositionLocalProvider(
        LocalClipKidColors provides colors,
        LocalClipKidTypography provides typography,
        LocalClipKidShapes provides ClipKidShapes(),
        LocalClipKidSpacing provides ClipKidSpacing(),
        LocalClipKidSizes provides ClipKidSizes(),
        LocalContentColor provides colors.ink,
        LocalTextStyle provides typography.bodyLarge,
        content = content,
    )
}

/** Forces right-to-left layout for everything inside. */
@Composable
fun ProvideRtl(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl, content = content)
}

object ClipKidTheme {
    val colors: ClipKidColors
        @Composable @ReadOnlyComposable get() = LocalClipKidColors.current
    val typography: ClipKidTypography
        @Composable @ReadOnlyComposable get() = LocalClipKidTypography.current
    val shapes: ClipKidShapes
        @Composable @ReadOnlyComposable get() = LocalClipKidShapes.current
    val spacing: ClipKidSpacing
        @Composable @ReadOnlyComposable get() = LocalClipKidSpacing.current
    val sizes: ClipKidSizes
        @Composable @ReadOnlyComposable get() = LocalClipKidSizes.current
}
