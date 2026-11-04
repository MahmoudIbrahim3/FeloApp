package com.mahmoud.clipkid.feature.child.gate

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
import com.mahmoud.clipkid.core.designsystem.component.IconButtonLarge
import com.mahmoud.clipkid.core.designsystem.component.IconButtonTone
import com.mahmoud.clipkid.core.designsystem.component.PrimaryButton
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.feature.child.resources.Res
import com.mahmoud.clipkid.feature.child.resources.child_back
import com.mahmoud.clipkid.feature.child.resources.child_gate_message
import com.mahmoud.clipkid.feature.child.resources.child_gate_title
import org.jetbrains.compose.resources.stringResource

/**
 * Parental gate STUB: the real gate (math or PIN check) arrives with the parent feature.
 * It only offers a way back for now, so no child-reachable screen is unguarded by design.
 */
@Composable
fun GateScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClipKidTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = ClipKidTheme.spacing.gutter, vertical = ClipKidTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.lg),
    ) {
        Row(Modifier.fillMaxWidth()) {
            IconButtonLarge(
                icon = ClipKidIcons.Back,
                contentDescription = stringResource(Res.string.child_back),
                onClick = onBack,
                tone = IconButtonTone.Surface,
                compact = true,
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(Res.string.child_gate_title),
            style = ClipKidTheme.typography.displayLarge,
            color = colors.ink,
        )
        Text(
            text = stringResource(Res.string.child_gate_message),
            style = ClipKidTheme.typography.bodyLarge,
            color = colors.muted,
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(Res.string.child_back),
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
