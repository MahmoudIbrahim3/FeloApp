package com.mahmoud.clipkid.feature.parent.subscription

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.mahmoud.clipkid.core.designsystem.component.IconButtonLarge
import com.mahmoud.clipkid.core.designsystem.component.IconButtonTone
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.preview.ClipKidPreview
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.feature.parent.resources.Res
import com.mahmoud.clipkid.feature.parent.resources.parent_back
import com.mahmoud.clipkid.feature.parent.resources.sub_soon_message
import com.mahmoud.clipkid.feature.parent.resources.sub_soon_title
import org.jetbrains.compose.resources.stringResource

/** Placeholder until the billing phase adds the paywall (Paywall.png). Reached only through the gate. */
@Composable
fun SubscriptionSoonScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ClipKidTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = ClipKidTheme.spacing.gutter, vertical = ClipKidTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.md),
    ) {
        Row(Modifier.fillMaxWidth()) {
            IconButtonLarge(
                icon = ClipKidIcons.Back,
                contentDescription = stringResource(Res.string.parent_back),
                onClick = onBack,
                tone = IconButtonTone.Surface,
                compact = true,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(stringResource(Res.string.sub_soon_title), style = ClipKidTheme.typography.displayMedium, color = colors.ink)
        Text(stringResource(Res.string.sub_soon_message), style = ClipKidTheme.typography.bodyLarge, color = colors.muted)
        Spacer(Modifier.weight(2f))
    }
}

@Preview
@Composable
private fun SubscriptionSoonPreview() = ClipKidPreview { SubscriptionSoonScreen(onBack = {}) }
