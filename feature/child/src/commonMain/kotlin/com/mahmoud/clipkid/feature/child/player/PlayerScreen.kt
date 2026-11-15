package com.mahmoud.clipkid.feature.child.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.component.Icon
import com.mahmoud.clipkid.core.designsystem.component.IconButtonLarge
import com.mahmoud.clipkid.core.designsystem.component.IconButtonTone
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.text.formatDuration
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.core.player.PlaybackState
import com.mahmoud.clipkid.core.player.YouTubePlayer
import com.mahmoud.clipkid.core.player.YouTubePlayerState
import com.mahmoud.clipkid.core.player.rememberIsDebugBuild
import com.mahmoud.clipkid.core.player.rememberYouTubePlayerState
import com.mahmoud.clipkid.feature.child.common.DarkOutlineButton
import com.mahmoud.clipkid.feature.child.resources.Res
import com.mahmoud.clipkid.feature.child.resources.child_back
import com.mahmoud.clipkid.feature.child.resources.child_favorite_add
import com.mahmoud.clipkid.feature.child.resources.child_favorite_remove
import com.mahmoud.clipkid.feature.child.resources.child_loading
import com.mahmoud.clipkid.feature.child.resources.child_player_debug_error
import com.mahmoud.clipkid.feature.child.resources.child_player_go_back
import com.mahmoud.clipkid.feature.child.resources.child_player_offline
import com.mahmoud.clipkid.feature.child.resources.child_player_offline_message
import com.mahmoud.clipkid.feature.child.resources.child_player_retry
import com.mahmoud.clipkid.feature.child.resources.child_player_rotate_hint
import com.mahmoud.clipkid.feature.child.resources.child_player_unavailable
import com.mahmoud.clipkid.feature.child.resources.child_player_unavailable_message
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** YouTube's minimum embedded player viewport (RMF): 200×200. */
private val MIN_PLAYER_SIZE = 200.dp

/** Below this height in landscape we're on a phone: only the back button and the player show. */
private val PHONE_LANDSCAPE_MAX_HEIGHT = 500.dp

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

/**
 * Clips.png. The YouTube player is never covered, clipped or drawn over: everything else sits
 * above or below it. On a phone in landscape only the back button and the player remain.
 */
@Composable
fun PlayerScreen(
    state: PlayerUiState,
    onEvent: (PlayerEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClipKidTheme.colors
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(colors.darkBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val phoneLandscape = maxWidth > maxHeight && maxHeight < PHONE_LANDSCAPE_MAX_HEIGHT
        val compactWidth = maxWidth < 600.dp
        // Read here: inside the Column below, BoxWithConstraints' maxWidth/maxHeight aren't reachable
        // implicitly (both scopes are @LayoutScopeMarker).
        val portrait = maxHeight > maxWidth
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ClipKidTheme.spacing.gutter, vertical = ClipKidTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButtonLarge(
                    icon = ClipKidIcons.Back,
                    contentDescription = stringResource(Res.string.child_back),
                    onClick = { onEvent(PlayerEvent.BackClicked) },
                    tone = IconButtonTone.OnDark,
                    compact = true,
                )
            }

            when (state) {
                PlayerUiState.Loading -> CenteredMessage(stringResource(Res.string.child_loading))
                PlayerUiState.Unavailable -> CenteredMessage(stringResource(Res.string.child_player_unavailable))
                is PlayerUiState.Ready -> PlayerBody(
                    state = state,
                    onEvent = onEvent,
                    phoneLandscape = phoneLandscape,
                    showRotateHint = !phoneLandscape && compactWidth && portrait,
                )
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Text(
        text = text,
        style = ClipKidTheme.typography.bodyLarge,
        color = ClipKidTheme.colors.onDarkMuted,
        modifier = Modifier.padding(horizontal = ClipKidTheme.spacing.gutter, vertical = ClipKidTheme.spacing.lg),
    )
}

@Composable
private fun ColumnScope.PlayerBody(
    state: PlayerUiState.Ready,
    onEvent: (PlayerEvent) -> Unit,
    phoneLandscape: Boolean,
    showRotateHint: Boolean,
) {
    val slot = state.player
    // One player per attempt. Re-entering the screen (after the parental gate) creates a new one
    // that starts where the child was, paused.
    val playerState = rememberYouTubePlayerState(slot.startSec, slot.autoPlay, key = slot.attempt)
    LaunchedEffect(playerState) {
        playerState.events.collect { onEvent(PlayerEvent.FromPlayer(it)) }
    }

    // Same place in the tree in portrait and landscape, so rotating never reloads the player.
    PlayerFrame(
        fillHeight = phoneLandscape,
        modifier = if (phoneLandscape) Modifier.weight(1f).fillMaxWidth() else Modifier.fillMaxWidth(),
    ) {
        val problem = slot.problem
        when {
            problem != null -> ProblemCard(problem, onEvent)
            slot.visible -> YouTubePlayer(
                videoId = state.video.youtubeId,
                state = playerState,
                modifier = Modifier.fillMaxSize(),
            )
            // Ended: the player is gone so YouTube's end screen never shows while Up Next opens.
            else -> Box(Modifier.fillMaxSize().background(Color.Black))
        }
    }

    if (!phoneLandscape) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = ClipKidTheme.spacing.gutter),
        ) {
            Spacer(Modifier.height(ClipKidTheme.spacing.lg))
            ProgressSection(playerState, state.video.durationSec)
            Spacer(Modifier.height(ClipKidTheme.spacing.xl))
            VideoInfo(state, onEvent)
            Spacer(Modifier.weight(1f))
            if (showRotateHint) RotateHint()
        }
    }
}

/**
 * Sizes the player: full width at 16:9 in portrait (at least 200 dp tall, so it never drops below
 * 200×200), or as large as fits at 16:9 when [fillHeight]. Nothing is drawn over [content].
 */
@Composable
private fun PlayerFrame(fillHeight: Boolean, modifier: Modifier, content: @Composable () -> Unit) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val (width, height) = if (fillHeight) {
            val h = maxHeight.coerceAtLeast(MIN_PLAYER_SIZE)
            minOf(maxWidth, h * 16f / 9f).coerceAtLeast(MIN_PLAYER_SIZE) to h
        } else {
            maxWidth.coerceAtLeast(MIN_PLAYER_SIZE) to (maxWidth * 9f / 16f).coerceAtLeast(MIN_PLAYER_SIZE)
        }
        Box(Modifier.width(width).height(height)) { content() }
    }
}

@Composable
private fun ProblemCard(problem: PlayerProblem, onEvent: (PlayerEvent) -> Unit) {
    val colors = ClipKidTheme.colors
    val debug = rememberIsDebugBuild()
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.darkSurface)
            .padding(ClipKidTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val offline = problem is PlayerProblem.Offline
        Text(
            text = stringResource(if (offline) Res.string.child_player_offline else Res.string.child_player_unavailable),
            style = ClipKidTheme.typography.titleLarge,
            color = colors.onDark,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                if (offline) Res.string.child_player_offline_message else Res.string.child_player_unavailable_message,
            ),
            style = ClipKidTheme.typography.bodyMedium,
            color = colors.onDarkMuted,
            textAlign = TextAlign.Center,
        )
        if (debug && problem is PlayerProblem.NotAvailable) {
            Text(
                text = stringResource(Res.string.child_player_debug_error, problem.code.toString()),
                style = ClipKidTheme.typography.labelMedium,
                color = colors.accent,
            )
        }
        if (offline) {
            DarkOutlineButton(
                text = stringResource(Res.string.child_player_retry),
                onClick = { onEvent(PlayerEvent.RetryClicked) },
                icon = ClipKidIcons.Replay,
            )
        } else {
            DarkOutlineButton(
                text = stringResource(Res.string.child_player_go_back),
                onClick = { onEvent(PlayerEvent.BackClicked) },
                icon = ClipKidIcons.Back,
            )
        }
    }
}

/** Our own bar under the player: the last reported time, moved on every second while playing. */
@Composable
private fun ProgressSection(player: YouTubePlayerState, catalogDurationSec: Int) {
    val colors = ClipKidTheme.colors
    val durationSec = player.durationSec.takeIf { it > 0 } ?: catalogDurationSec
    val playing = player.playback == PlaybackState.Playing
    val shownSec by produceState(player.currentSec, player.currentSec, playing, durationSec) {
        value = player.currentSec
        while (playing) {
            delay(1_000)
            value = (value + 1).coerceAtMost(durationSec)
        }
    }
    val fraction = if (durationSec <= 0) 0f else (shownSec.toFloat() / durationSec).coerceIn(0f, 1f)

    Column(verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.xs)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(colors.darkSurfaceHigh, ClipKidTheme.shapes.pill),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(colors.accent, ClipKidTheme.shapes.pill),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = formatDuration(shownSec),
                style = ClipKidTheme.typography.labelMedium,
                color = colors.onDarkMuted,
            )
            Text(
                text = formatDuration(durationSec),
                style = ClipKidTheme.typography.labelMedium,
                color = colors.onDarkMuted,
            )
        }
    }
}

@Composable
private fun VideoInfo(state: PlayerUiState.Ready, onEvent: (PlayerEvent) -> Unit) {
    val colors = ClipKidTheme.colors
    val cover = state.collectionStyle
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
                text = state.video.titleAr,
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
}

@Composable
private fun RotateHint() {
    val colors = ClipKidTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = ClipKidTheme.spacing.lg),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.child_player_rotate_hint),
            style = ClipKidTheme.typography.bodyMedium,
            color = colors.onDarkMuted,
        )
        Spacer(Modifier.width(ClipKidTheme.spacing.xs))
        Icon(icon = ClipKidIcons.Info, contentDescription = null, tint = colors.onDarkMuted, size = 18.dp)
    }
}
