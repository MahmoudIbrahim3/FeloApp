package com.mahmoud.clipkid.feature.child.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.component.Icon
import com.mahmoud.clipkid.core.designsystem.component.IconButtonLarge
import com.mahmoud.clipkid.core.designsystem.component.IconButtonTone
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.text.formatDuration
import com.mahmoud.clipkid.core.designsystem.text.withArabicDigits
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.feature.child.common.DarkOutlineButton
import com.mahmoud.clipkid.feature.child.resources.Res
import com.mahmoud.clipkid.feature.child.resources.child_back
import com.mahmoud.clipkid.feature.child.resources.child_favorite_add
import com.mahmoud.clipkid.feature.child.resources.child_favorite_remove
import com.mahmoud.clipkid.feature.child.resources.child_loading
import com.mahmoud.clipkid.feature.child.resources.child_player_placeholder
import com.mahmoud.clipkid.feature.child.resources.child_player_simulate_end
import com.mahmoud.clipkid.feature.child.resources.child_player_unavailable
import org.jetbrains.compose.resources.stringResource

/** Player screen: route wiring. */
@Composable
fun PlayerRoute(
    viewModel: PlayerViewModel,
    onEffect: (PlayerEffect) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect(onEffect)
    }
    PlayerScreen(state, viewModel::onEvent)
}

@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onEvent: (PlayerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClipKidTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.darkBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = ClipKidTheme.spacing.gutter, vertical = ClipKidTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.lg),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButtonLarge(
                icon = ClipKidIcons.Back,
                contentDescription = stringResource(Res.string.child_back),
                onClick = { onEvent(PlayerEvent.BackClicked) },
                tone = IconButtonTone.OnDark,
                compact = true,
            )
        }

        when (state) {
            PlayerUiState.Loading -> Text(
                text = stringResource(Res.string.child_loading),
                style = ClipKidTheme.typography.bodyLarge,
                color = colors.onDarkMuted,
            )
            PlayerUiState.Unavailable -> Text(
                text = stringResource(Res.string.child_player_unavailable),
                style = ClipKidTheme.typography.bodyLarge,
                color = colors.onDarkMuted,
            )
            is PlayerUiState.Ready -> PlayerBody(state, onEvent)
        }
    }
}

@Composable
private fun PlayerBody(state: PlayerUiState.Ready, onEvent: (PlayerEvent) -> Unit) {
    val colors = ClipKidTheme.colors
    val video = state.video
    val cover = state.collectionStyle

    // Player slot: 16:9. The real YouTube player goes here in Phase 4 (never smaller than 200x200).
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(colors.darkSurface, ClipKidTheme.shapes.card),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(Res.string.child_player_placeholder),
            style = ClipKidTheme.typography.bodyMedium,
            color = colors.onDarkMuted,
        )
    }

    // Progress: nothing has played yet in the placeholder, so the bar stays empty.
    Column(verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.xs)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(colors.darkSurfaceHigh, ClipKidTheme.shapes.pill),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = formatDuration(0).withArabicDigits(),
                style = ClipKidTheme.typography.labelMedium,
                color = colors.onDarkMuted,
            )
            Text(
                text = formatDuration(video.durationSec).withArabicDigits(),
                style = ClipKidTheme.typography.labelMedium,
                color = colors.onDarkMuted,
            )
        }
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.xs),
        ) {
            Box(
                Modifier
                    .background(Color(cover.bg), ClipKidTheme.shapes.pill)
                    .padding(horizontal = ClipKidTheme.spacing.md, vertical = ClipKidTheme.spacing.xxs),
            ) {
                Text(
                    text = state.collectionTitle,
                    style = ClipKidTheme.typography.labelMedium,
                    color = Color(cover.fg),
                    maxLines = 1,
                )
            }
            Text(
                text = video.titleAr,
                style = ClipKidTheme.typography.titleLarge,
                color = colors.onDark,
                maxLines = 2,
            )
        }
        Spacer(Modifier.width(ClipKidTheme.spacing.md))
        val favoriteLabel = stringResource(
            if (state.isFavorite) Res.string.child_favorite_remove else Res.string.child_favorite_add,
        )
        Box(
            Modifier
                .size(56.dp)
                .background(colors.darkSurface, ClipKidTheme.shapes.iconButton)
                .clickable { onEvent(PlayerEvent.ToggleFavorite) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon = if (state.isFavorite) ClipKidIcons.HeartFilled else ClipKidIcons.Heart,
                contentDescription = favoriteLabel,
                tint = if (state.isFavorite) colors.danger else colors.onDark,
                size = 26.dp,
            )
        }
    }

    Spacer(Modifier.weight(1f))

    // Placeholder control, removed when the real player reports the end (Phase 4).
    DarkOutlineButton(
        text = stringResource(Res.string.child_player_simulate_end),
        onClick = { onEvent(PlayerEvent.SimulateEnd) },
        icon = ClipKidIcons.Replay,
    )
}
