package com.mahmoud.clipkid.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcon
import com.mahmoud.clipkid.core.designsystem.theme.LocalContentColor
import org.jetbrains.compose.resources.painterResource

/** Tinted vector icon. Mirrors [ClipKidIcon.autoMirror] icons in RTL. */
@Composable
fun Icon(
    icon: ClipKidIcon,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
) {
    val mirror = icon.autoMirror && LocalLayoutDirection.current == LayoutDirection.Rtl
    Image(
        painter = painterResource(icon.resource),
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .then(if (mirror) Modifier.graphicsLayer(scaleX = -1f) else Modifier),
        colorFilter = ColorFilter.tint(tint),
    )
}
