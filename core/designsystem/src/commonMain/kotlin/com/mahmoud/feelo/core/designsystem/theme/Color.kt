package com.mahmoud.feelo.core.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Feelo color tokens. Raw values come from CLAUDE.md; the few derived tokens
 * (marked "derived") are alpha variants of those values, not new colors.
 */
@Immutable
data class FeeloColors(
    val ink: Color = Color(0xFF17243A),
    val muted: Color = Color(0xFF56657A),
    val background: Color = Color(0xFFF4F7FB),
    val surface: Color = Color(0xFFFFFFFF),
    val primary: Color = Color(0xFF2563D9),
    val primaryDark: Color = Color(0xFF1A4FB5),
    val onPrimary: Color = Color(0xFFFFFFFF),
    val accent: Color = Color(0xFFFFB627),
    val onAccent: Color = Color(0xFF17243A),
    val border: Color = Color(0xFFC9D3E0),
    val divider: Color = Color(0xFFE3E9F1),
    val success: Color = Color(0xFF1F7A4A),
    val danger: Color = Color(0xFFB23A26),
    val celebrate: Color = Color(0xFFFFF4D6),
    /** Warm background of the child tabs (Home, Favorites). */
    val childBackground: Color = Color(0xFFFFF8EC),

    /** Player / post-video background. */
    val darkBackground: Color = Color(0xFF0F1724),
    /** Parental gate background. */
    val darkBackgroundAlt: Color = Color(0xFF17243A),
    val darkSurface: Color = Color(0xFF1E2B40),
    val darkSurfaceHigh: Color = Color(0xFF24364F),
    val onDark: Color = Color(0xFFFFFFFF),
    /** derived: secondary text on dark surfaces. */
    val onDarkMuted: Color = Color(0xFFFFFFFF).copy(alpha = 0.64f),

    /** derived: scrim behind badges drawn on thumbnails. */
    val badgeScrim: Color = Color(0xFF17243A).copy(alpha = 0.85f),
    /** derived: thumbnail background while the image loads. */
    val thumbnailPlaceholder: Color = Color(0xFFC9D3E0),
)

/** Background / foreground pair of a collection cover. */
@Immutable
data class CoverColors(val bg: Color, val fg: Color)

/** The six collection cover palettes from CLAUDE.md. */
object CoverPalettes {
    val Letters = CoverColors(bg = Color(0xFFFFE6B0), fg = Color(0xFF8A5A00))
    val Numbers = CoverColors(bg = Color(0xFFD6E8FF), fg = Color(0xFF1A4FB5))
    val QuranAndDuas = CoverColors(bg = Color(0xFFD9F2E4), fg = Color(0xFF1F7A4A))
    val Manners = CoverColors(bg = Color(0xFFFFDCD6), fg = Color(0xFFB23A26))
    val Science = CoverColors(bg = Color(0xFFE6DEFF), fg = Color(0xFF5B3DC4))
    val Stories = CoverColors(bg = Color(0xFFFFE3C7), fg = Color(0xFFA3530E))

    val all: List<CoverColors> = listOf(Letters, Numbers, QuranAndDuas, Manners, Science, Stories)
}

/** Whether a component sits on a light screen or on a dark (player / gate) screen. */
enum class SurfaceTone { Light, Dark }

internal val LocalFeeloColors = staticCompositionLocalOf { FeeloColors() }
