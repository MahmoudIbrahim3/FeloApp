package com.mahmoud.feelo.feature.child.common

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
import com.mahmoud.feelo.core.designsystem.component.Icon
import com.mahmoud.feelo.core.designsystem.component.Text
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcon
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme

/** Outlined button for the dark screens (player, Up Next). Min 56dp tall, the child's primary size. */
@Composable
fun DarkOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: FeeloIcon? = null,
) {
    val colors = FeeloTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = FeeloTheme.sizes.buttonHeight)
            .border(2.dp, colors.onDarkMuted, FeeloTheme.shapes.button)
            .clickable(onClick = onClick)
            .padding(horizontal = FeeloTheme.spacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let {
            Icon(icon = it, contentDescription = null, tint = colors.onDark, size = 24.dp)
            Spacer(Modifier.width(FeeloTheme.spacing.xs))
        }
        Text(text = text, style = FeeloTheme.typography.button, color = colors.onDark, maxLines = 1)
    }
}
