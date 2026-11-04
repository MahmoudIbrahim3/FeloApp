package com.mahmoud.clipkid.feature.child.common

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.component.Icon
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcon
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme

/** Outlined button for the dark screens (player, Up Next). Min 56dp tall, the child's primary size. */
@Composable
fun DarkOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ClipKidIcon? = null,
) {
    val colors = ClipKidTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ClipKidTheme.sizes.buttonHeight)
            .border(2.dp, colors.onDarkMuted, ClipKidTheme.shapes.button)
            .clickable(onClick = onClick)
            .padding(horizontal = ClipKidTheme.spacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(icon = it, contentDescription = null, tint = colors.onDark, size = 24.dp)
            Spacer(Modifier.width(ClipKidTheme.spacing.xs))
        }
        Text(text = text, style = ClipKidTheme.typography.button, color = colors.onDark, maxLines = 1)
    }
}
