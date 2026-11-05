package com.mahmoud.clipkid.feature.child.askparent

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.component.Icon
import com.mahmoud.clipkid.core.designsystem.component.PrimaryButton
import com.mahmoud.clipkid.core.designsystem.component.PrimaryButtonTone
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.preview.ClipKidPreview
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.feature.child.resources.Res
import com.mahmoud.clipkid.feature.child.resources.child_ask_parent_back
import com.mahmoud.clipkid.feature.child.resources.child_ask_parent_for_parents
import com.mahmoud.clipkid.feature.child.resources.child_ask_parent_message
import com.mahmoud.clipkid.feature.child.resources.child_ask_parent_title
import org.jetbrains.compose.resources.stringResource

/**
 * A child tapped a locked (premium) collection. A friendly stop in the DoneToday style: no
 * prices, no purchase button. "للأهل" opens the parental gate; the paywall is only behind it.
 */
@Composable
fun AskParentScreen(onBack: () -> Unit, onParents: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ClipKidTheme.colors
    Box(
        modifier.fillMaxSize().background(colors.celebrate).statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .padding(horizontal = ClipKidTheme.spacing.gutter, vertical = ClipKidTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            LockMedallion()
            Spacer(Modifier.height(ClipKidTheme.spacing.xxl))
            Text(
                text = stringResource(Res.string.child_ask_parent_title),
                style = ClipKidTheme.typography.displayLarge,
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(ClipKidTheme.spacing.xs))
            Text(
                text = stringResource(Res.string.child_ask_parent_message),
                style = ClipKidTheme.typography.titleMedium,
                color = colors.ink,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = stringResource(Res.string.child_ask_parent_back),
                onClick = onBack,
                tone = PrimaryButtonTone.Ink,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(ClipKidTheme.spacing.md))
            Text(
                text = stringResource(Res.string.child_ask_parent_for_parents),
                style = ClipKidTheme.typography.labelLarge.copy(textDecoration = TextDecoration.Underline),
                color = colors.ink,
                modifier = Modifier
                    .defaultMinSize(minHeight = ClipKidTheme.sizes.minTouchTarget)
                    .clickable(role = Role.Button, onClick = onParents)
                    .padding(ClipKidTheme.spacing.sm),
            )
        }
    }
}

/** Big lock in a yellow circle. Decorative only. */
@Composable
private fun LockMedallion() {
    val colors = ClipKidTheme.colors
    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(220.dp).background(colors.accent.copy(alpha = 0.2f), ClipKidTheme.shapes.pill))
        Box(Modifier.size(150.dp).background(colors.accent, ClipKidTheme.shapes.pill), contentAlignment = Alignment.Center) {
            Icon(icon = ClipKidIcons.Lock, contentDescription = null, tint = colors.surface, size = 72.dp)
        }
    }
}

@Preview
@Composable
private fun AskParentPreview() = ClipKidPreview { AskParentScreen(onBack = {}, onParents = {}) }
