package com.mahmoud.clipkid.core.designsystem.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner shapes: cards 20–24dp, buttons 18dp, chips fully rounded. */
@Immutable
data class ClipKidShapes(
    val card: Shape = RoundedCornerShape(24.dp),
    val cardSmall: Shape = RoundedCornerShape(20.dp),
    /** Full-width video thumbnails (Collection screen): a gentler curve, YouTube Kids style. */
    val thumbnail: Shape = RoundedCornerShape(12.dp),
    val button: Shape = RoundedCornerShape(18.dp),
    val iconButton: Shape = RoundedCornerShape(16.dp),
    val segment: Shape = RoundedCornerShape(14.dp),
    val badge: Shape = RoundedCornerShape(8.dp),
    val pill: Shape = CircleShape,
)

/** 4dp spacing grid. */
@Immutable
data class ClipKidSpacing(
    val xxs: Dp = 4.dp,
    val xs: Dp = 8.dp,
    val sm: Dp = 12.dp,
    val md: Dp = 16.dp,
    val lg: Dp = 20.dp,
    val xl: Dp = 24.dp,
    val xxl: Dp = 32.dp,
    val xxxl: Dp = 40.dp,
    /** Horizontal screen padding on phones (20dp in the reference screens). */
    val gutter: Dp = 16.dp,
)

/** Component sizes. Touch targets: ≥ 48dp everywhere, ≥ 56dp for primary child actions. */
@Immutable
data class ClipKidSizes(
    val minTouchTarget: Dp = 48.dp,
    val childTouchTarget: Dp = 56.dp,
    val buttonHeight: Dp = 56.dp,
    val iconButton: Dp = 56.dp,
    val iconButtonCompact: Dp = 48.dp,
    val icon: Dp = 24.dp,
    val pinKeyHeight: Dp = 64.dp,
    val collectionCover: Dp = 84.dp,
)

internal val LocalClipKidShapes = staticCompositionLocalOf { ClipKidShapes() }
internal val LocalClipKidSpacing = staticCompositionLocalOf { ClipKidSpacing() }
internal val LocalClipKidSizes = staticCompositionLocalOf { ClipKidSizes() }
