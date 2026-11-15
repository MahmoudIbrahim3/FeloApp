package com.mahmoud.clipkid.feature.parent.grant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.component.PrimaryButton
import com.mahmoud.clipkid.core.designsystem.component.PrimaryButtonTone
import com.mahmoud.clipkid.core.designsystem.component.SecondaryButton
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.preview.ClipKidPreview
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.core.designsystem.theme.SurfaceTone
import com.mahmoud.clipkid.core.domain.usecase.MINUTES_PER_EXTRA_VIDEO
import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.core.model.ChildProfile
import com.mahmoud.clipkid.core.model.DailyLimit
import com.mahmoud.clipkid.feature.parent.resources.Res
import com.mahmoud.clipkid.feature.parent.resources.dash_grant_minutes
import com.mahmoud.clipkid.feature.parent.resources.dash_grant_one_video
import com.mahmoud.clipkid.feature.parent.resources.dash_grant_three_videos
import com.mahmoud.clipkid.feature.parent.resources.grant_message
import com.mahmoud.clipkid.feature.parent.resources.grant_no
import com.mahmoud.clipkid.feature.parent.resources.grant_title
import org.jetbrains.compose.resources.stringResource

@Composable
fun GrantRoute(viewModel: GrantViewModel, onEffect: (GrantEffect) -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.effects.collect(onEffect) }
    GrantScreen(state, viewModel::onEvent)
}

/** After "عايز تكمّل؟" and the gate: +1 / +3 for today, on the dark parent surface. */
@Composable
fun GrantScreen(state: GrantUiState, onEvent: (GrantEvent) -> Unit, modifier: Modifier = Modifier) {
    val colors = ClipKidTheme.colors
    val spacing = ClipKidTheme.spacing
    Box(
        modifier.fillMaxSize().background(colors.darkBackgroundAlt).statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        val ready = state as? GrantUiState.Ready ?: return@Box
        val minutesMode = ready.child.dailyLimit is DailyLimit.Minutes
        Column(
            Modifier.fillMaxWidth().widthIn(max = 480.dp).padding(horizontal = spacing.gutter),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(stringResource(Res.string.grant_title), style = ClipKidTheme.typography.displayMedium, color = colors.onDark)
            Text(
                stringResource(Res.string.grant_message, ready.child.nickname),
                style = ClipKidTheme.typography.bodyLarge,
                color = colors.onDarkMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(spacing.md))
            PrimaryButton(
                text = label(1, minutesMode),
                onClick = { onEvent(GrantEvent.Grant(1)) },
                tone = PrimaryButtonTone.Accent,
                modifier = Modifier.fillMaxWidth(),
            )
            PrimaryButton(
                text = label(3, minutesMode),
                onClick = { onEvent(GrantEvent.Grant(3)) },
                tone = PrimaryButtonTone.Accent,
                modifier = Modifier.fillMaxWidth(),
            )
            SecondaryButton(
                text = stringResource(Res.string.grant_no),
                onClick = { onEvent(GrantEvent.NoThanksClicked) },
                tone = SurfaceTone.Dark,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun label(videos: Int, minutesMode: Boolean): String = when {
    minutesMode -> stringResource(Res.string.dash_grant_minutes, (videos * MINUTES_PER_EXTRA_VIDEO).toString())
    videos == 1 -> stringResource(Res.string.dash_grant_one_video)
    else -> stringResource(Res.string.dash_grant_three_videos)
}

// Sample data for previews only (never shown in the app).
private const val PREVIEW_CHILD = "يوسف"

@Preview
@Composable
private fun GrantPreview() = ClipKidPreview {
    GrantScreen(GrantUiState.Ready(ChildProfile("c1", PREVIEW_CHILD, 0xFFFFB627, AgeBand.A6_8, emptySet(), DailyLimit.Videos(3))), {})
}
