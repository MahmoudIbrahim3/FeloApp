package com.mahmoud.feelo.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

/** Color used by [com.mahmoud.feelo.core.designsystem.component.Text] and Icon when none is given. */
val LocalContentColor = compositionLocalOf { Color(0xFF17243A) }

/** Text style used by [com.mahmoud.feelo.core.designsystem.component.Text] when none is given. */
val LocalTextStyle = compositionLocalOf { FeeloTypography.Fallback.bodyLarge }

/**
 * Provides Feelo tokens. It does not change layout direction: RTL is forced once at the
 * app root (see [ProvideRtl]) so every screen and preview shares one source of truth.
 */
@Composable
fun FeeloTheme(
    colors: FeeloColors = FeeloColors(),
    content: @Composable () -> Unit,
) {
    val typography = rememberFeeloTypography()
    CompositionLocalProvider(
        LocalFeeloColors provides colors,
        LocalFeeloTypography provides typography,
        LocalFeeloShapes provides FeeloShapes(),
        LocalFeeloSpacing provides FeeloSpacing(),
        LocalFeeloSizes provides FeeloSizes(),
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

object FeeloTheme {
    val colors: FeeloColors
        @Composable @ReadOnlyComposable get() = LocalFeeloColors.current
    val typography: FeeloTypography
        @Composable @ReadOnlyComposable get() = LocalFeeloTypography.current
    val shapes: FeeloShapes
        @Composable @ReadOnlyComposable get() = LocalFeeloShapes.current
    val spacing: FeeloSpacing
        @Composable @ReadOnlyComposable get() = LocalFeeloSpacing.current
    val sizes: FeeloSizes
        @Composable @ReadOnlyComposable get() = LocalFeeloSizes.current
}
