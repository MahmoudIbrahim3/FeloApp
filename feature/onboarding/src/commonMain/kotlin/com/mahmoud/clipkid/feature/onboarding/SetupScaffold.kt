package com.mahmoud.clipkid.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.component.IconButtonLarge
import com.mahmoud.clipkid.core.designsystem.component.IconButtonTone
import com.mahmoud.clipkid.core.designsystem.component.StepProgress
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.feature.onboarding.resources.Res
import com.mahmoud.clipkid.feature.onboarding.resources.onb_back
import com.mahmoud.clipkid.feature.onboarding.resources.onb_step_of
import org.jetbrains.compose.resources.stringResource

/** Phone-width column centered on tablets, so setup screens never stretch across 1280dp. */
internal val SetupMaxWidth = 560.dp

/**
 * Shared frame of the setup steps (SetPin / ChildSetup / SetupCollections): back button and
 * "خطوة x من y" on top, the step bar, title and subtitle, scrolling [content], and [bottom]
 * (the main button) pinned above the keyboard and navigation bar.
 */
@Composable
internal fun SetupScaffold(
    step: Int,
    totalSteps: Int,
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = ClipKidTheme.colors
    val spacing = ClipKidTheme.spacing
    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.fillMaxSize().widthIn(max = SetupMaxWidth).padding(horizontal = spacing.gutter)) {
            Row(
                Modifier.fillMaxWidth().padding(top = spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButtonLarge(
                    icon = ClipKidIcons.Back,
                    contentDescription = stringResource(Res.string.onb_back),
                    onClick = onBack,
                    tone = IconButtonTone.Surface,
                    compact = true,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(Res.string.onb_step_of, step.toString(), totalSteps.toString()),
                    style = ClipKidTheme.typography.bodyMedium,
                    color = colors.muted,
                )
            }
            Spacer(Modifier.height(spacing.lg))
            StepProgress(current = step, total = totalSteps)
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier),
            ) {
                Spacer(Modifier.height(spacing.xl))
                Text(text = title, style = ClipKidTheme.typography.displayMedium, color = colors.ink)
                Spacer(Modifier.height(spacing.xs))
                Text(text = subtitle, style = ClipKidTheme.typography.bodyLarge, color = colors.muted)
                Spacer(Modifier.height(spacing.xl))
                content()
            }
            Column(
                Modifier.fillMaxWidth().padding(vertical = spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                content = bottom,
            )
        }
    }
}
