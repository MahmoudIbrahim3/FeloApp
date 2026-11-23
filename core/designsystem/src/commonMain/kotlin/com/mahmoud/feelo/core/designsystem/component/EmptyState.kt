package com.mahmoud.feelo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcon
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcons
import com.mahmoud.feelo.core.designsystem.preview.FeeloPreview
import com.mahmoud.feelo.core.designsystem.resources.Res
import com.mahmoud.feelo.core.designsystem.resources.showcase_empty_message
import com.mahmoud.feelo.core.designsystem.resources.showcase_empty_title
import com.mahmoud.feelo.core.designsystem.resources.showcase_go_home
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import org.jetbrains.compose.resources.stringResource

/** Centered icon, title, message and an optional action, for empty lists. */
@Composable
fun EmptyState(
    icon: FeeloIcon,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = FeeloTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(FeeloTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(96.dp)
                .background(colors.celebrate, FeeloTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = colors.accent, size = 44.dp)
        }
        Spacer(Modifier.height(FeeloTheme.spacing.lg))
        Text(
            text = title,
            style = FeeloTheme.typography.titleLarge,
            color = colors.ink,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(FeeloTheme.spacing.xs))
        Text(
            text = message,
            style = FeeloTheme.typography.bodyMedium,
            color = colors.muted,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(FeeloTheme.spacing.xl))
            action()
        }
    }
}

// region Showcase

@Composable
fun EmptyStateShowcase() {
    EmptyState(
        icon = FeeloIcons.Heart,
        title = stringResource(Res.string.showcase_empty_title),
        message = stringResource(Res.string.showcase_empty_message),
        modifier = Modifier.background(FeeloTheme.colors.surface, FeeloTheme.shapes.card),
        action = {
            SecondaryButton(stringResource(Res.string.showcase_go_home), onClick = {}, icon = FeeloIcons.Home)
        },
    )
}

@Preview
@Composable
private fun EmptyStatePreview() = FeeloPreview { EmptyStateShowcase() }

// endregion
