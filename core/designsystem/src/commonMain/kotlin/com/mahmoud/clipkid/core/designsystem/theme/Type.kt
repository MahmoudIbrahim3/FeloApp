package com.mahmoud.clipkid.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.mahmoud.clipkid.core.designsystem.resources.Res
import com.mahmoud.clipkid.core.designsystem.resources.baloo_bhaijaan2_bold
import com.mahmoud.clipkid.core.designsystem.resources.baloo_bhaijaan2_extrabold
import com.mahmoud.clipkid.core.designsystem.resources.cairo_bold
import com.mahmoud.clipkid.core.designsystem.resources.cairo_regular
import com.mahmoud.clipkid.core.designsystem.resources.cairo_semibold
import org.jetbrains.compose.resources.Font

/**
 * Baloo Bhaijaan 2 (display: 700/800) for headings, buttons and numerals.
 * Cairo (body: 400/600/700) for running text and labels.
 * Every style carries the Arabic (Egypt) locale for correct shaping and line breaking.
 */
@Immutable
data class ClipKidTypography(
    val displayLarge: TextStyle,
    val displayMedium: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
    /** Button labels. */
    val button: TextStyle,
    /** Small badges on thumbnails (duration). */
    val badge: TextStyle,
    /** Big digits (PIN pad). */
    val numeral: TextStyle,
) {
    companion object {
        val ArabicLocale = LocaleList("ar-EG")

        fun create(display: FontFamily, body: FontFamily): ClipKidTypography {
            fun style(family: FontFamily, weight: FontWeight, size: TextUnit, lineHeight: TextUnit) = TextStyle(
                fontFamily = family,
                fontWeight = weight,
                fontSize = size,
                lineHeight = lineHeight,
                localeList = ArabicLocale,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.None,
                ),
            )
            return ClipKidTypography(
                displayLarge = style(display, FontWeight.ExtraBold, 32.sp, 44.sp),
                displayMedium = style(display, FontWeight.ExtraBold, 28.sp, 40.sp),
                titleLarge = style(display, FontWeight.ExtraBold, 22.sp, 32.sp),
                titleMedium = style(display, FontWeight.Bold, 18.sp, 28.sp),
                titleSmall = style(display, FontWeight.Bold, 16.sp, 24.sp),
                bodyLarge = style(body, FontWeight.Normal, 17.sp, 28.sp),
                bodyMedium = style(body, FontWeight.Normal, 15.sp, 24.sp),
                bodySmall = style(body, FontWeight.Normal, 13.sp, 20.sp),
                labelLarge = style(body, FontWeight.Bold, 16.sp, 24.sp),
                labelMedium = style(body, FontWeight.SemiBold, 14.sp, 20.sp),
                labelSmall = style(body, FontWeight.SemiBold, 12.sp, 16.sp),
                button = style(display, FontWeight.Bold, 17.sp, 26.sp),
                badge = style(display, FontWeight.Bold, 13.sp, 18.sp),
                numeral = style(display, FontWeight.ExtraBold, 26.sp, 34.sp),
            )
        }

        /** Used before fonts load and outside [ClipKidTheme]. */
        val Fallback: ClipKidTypography = create(FontFamily.Default, FontFamily.Default)
    }
}

@Composable
internal fun rememberClipKidTypography(): ClipKidTypography {
    val baloo = FontFamily(
        Font(Res.font.baloo_bhaijaan2_bold, FontWeight.Bold),
        Font(Res.font.baloo_bhaijaan2_extrabold, FontWeight.ExtraBold),
    )
    val cairo = FontFamily(
        Font(Res.font.cairo_regular, FontWeight.Normal),
        Font(Res.font.cairo_semibold, FontWeight.SemiBold),
        Font(Res.font.cairo_bold, FontWeight.Bold),
    )
    return remember(baloo, cairo) { ClipKidTypography.create(display = baloo, body = cairo) }
}

internal val LocalClipKidTypography = staticCompositionLocalOf { ClipKidTypography.Fallback }
