package com.mahmoud.feelo.feature.child.player

import androidx.compose.foundation.background
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mahmoud.feelo.core.designsystem.component.Icon
import com.mahmoud.feelo.core.designsystem.component.IconButtonLarge
import com.mahmoud.feelo.core.designsystem.component.IconButtonTone
import com.mahmoud.feelo.core.designsystem.component.Text
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcons
import com.mahmoud.feelo.core.designsystem.text.formatDuration
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import com.mahmoud.feelo.core.player.PlaybackState
import com.mahmoud.feelo.core.player.YouTubePlayer
import com.mahmoud.feelo.core.player.YouTubePlayerState
import com.mahmoud.feelo.core.player.rememberIsDebugBuild
import com.mahmoud.feelo.core.player.rememberYouTubePlayerState
import com.mahmoud.feelo.feature.child.common.DarkOutlineButton
import com.mahmoud.feelo.feature.child.resources.Res
import com.mahmoud.feelo.feature.child.resources.child_back
import com.mahmoud.feelo.feature.child.resources.child_favorite_add
import com.mahmoud.feelo.feature.child.resources.child_favorite_remove
import com.mahmoud.feelo.feature.child.resources.child_loading
import com.mahmoud.feelo.feature.child.resources.child_player_debug_error
import com.mahmoud.feelo.feature.child.resources.child_player_go_back
import com.mahmoud.feelo.feature.child.resources.child_player_offline
import com.mahmoud.feelo.feature.child.resources.child_player_offline_message
import com.mahmoud.feelo.feature.child.resources.child_player_retry
import com.mahmoud.feelo.feature.child.resources.child_player_rotate_hint
import com.mahmoud.feelo.feature.child.resources.child_player_unavailable
import com.mahmoud.feelo.feature.child.resources.child_player_unavailable_message
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource

/** YouTube's minimum embedded player viewport (RMF): 200×200. */
private val MIN_PLAYER_SIZE = 200.dp

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
    val colors = FeeloTheme.colors
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(colors.darkBackground)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Landscape (phone or tablet): the player fills the height under the top bar, and the
        // top bar carries the title and the favorite button instead of the card under the player.
        // Read here: inside the Column below, BoxWithConstraints' maxWidth/maxHeight aren't reachable
        // implicitly (both scopes are @LayoutScopeMarker).
        val landscape = maxWidth > maxHeight
        val compactWidth = maxWidth < 600.dp
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = FeeloTheme.spacing.gutter, vertical = FeeloTheme.spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButtonLarge(
                    icon = FeeloIcons.Back,
                    contentDescription = stringResource(Res.string.child_back),
                    onClick = { onEvent(PlayerEvent.BackClicked) },
                    tone = IconButtonTone.OnDark,
                    compact = true,
                )
                if (landscape && state is PlayerUiState.Ready) {
                    Spacer(Modifier.width(FeeloTheme.spacing.md))
                    Text(
                        text = state.video.titleAr,
                        style = FeeloTheme.typography.titleMedium,
                        color = colors.onDark,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(FeeloTheme.spacing.md))
                    FavoriteToggle(
                        isFavorite = state.isFavorite,
                        onToggle = { onEvent(PlayerEvent.ToggleFavorite) },
                    )
                }
            }

            when (state) {
                PlayerUiState.Loading -> CenteredMessage(stringResource(Res.string.child_loading))
                PlayerUiState.Unavailable -> CenteredMessage(stringResource(Res.string.child_player_unavailable))
                is PlayerUiState.Ready -> PlayerBody(
                    state = state,
                    onEvent = onEvent,
                    landscape = landscape,
                    showRotateHint = !landscape && compactWidth,
                )
            }
        }
    }
}

@Composable
private fun CenteredMessage(text: String) {
    Text(
        text = text,
        style = FeeloTheme.typography.bodyLarge,
        color = FeeloTheme.colors.onDarkMuted,
        modifier = Modifier.padding(horizontal = FeeloTheme.spacing.gutter, vertical = FeeloTheme.spacing.lg),
    )
}

@Composable
private fun ColumnScope.PlayerBody(
    state: PlayerUiState.Ready,
    onEvent: (PlayerEvent) -> Unit,
    landscape: Boolean,
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
        fillHeight = landscape,
        modifier = if (landscape) Modifier.weight(1f).fillMaxWidth() else Modifier.fillMaxWidth(),
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

    if (!landscape) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = FeeloTheme.spacing.gutter),
        ) {
            Spacer(Modifier.height(FeeloTheme.spacing.lg))
            ProgressSection(playerState, state.video.durationSec)
            Spacer(Modifier.height(FeeloTheme.spacing.lg))
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
    val colors = FeeloTheme.colors
    val debug = rememberIsDebugBuild()
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.darkSurface)
            .padding(FeeloTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.sm, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val offline = problem is PlayerProblem.Offline
        Text(
            text = stringResource(if (offline) Res.string.child_player_offline else Res.string.child_player_unavailable),
            style = FeeloTheme.typography.titleLarge,
            color = colors.onDark,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                if (offline) Res.string.child_player_offline_message else Res.string.child_player_unavailable_message,
            ),
            style = FeeloTheme.typography.bodyMedium,
            color = colors.onDarkMuted,
            textAlign = TextAlign.Center,
        )
        if (debug && problem is PlayerProblem.NotAvailable) {
            Text(
                text = stringResource(Res.string.child_player_debug_error, problem.code.toString()),
                style = FeeloTheme.typography.labelMedium,
                color = colors.accent,
            )
        }
        if (offline) {
            DarkOutlineButton(
                text = stringResource(Res.string.child_player_retry),
                onClick = { onEvent(PlayerEvent.RetryClicked) },
                icon = FeeloIcons.Replay,
            )
        } else {
            DarkOutlineButton(
                text = stringResource(Res.string.child_player_go_back),
                onClick = { onEvent(PlayerEvent.BackClicked) },
                icon = FeeloIcons.Back,
            )
        }
    }
}

/** Our own bar under the player: the last reported time, moved on every second while playing. */
@Composable
private fun ProgressSection(player: YouTubePlayerState, catalogDurationSec: Int) {
    val colors = FeeloTheme.colors
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

    Column(verticalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.xs)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(colors.darkSurfaceHigh, FeeloTheme.shapes.pill),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxHeight()
                    .background(colors.accent, FeeloTheme.shapes.pill),
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = formatDuration(shownSec),
                style = FeeloTheme.typography.labelMedium,
                color = colors.onDarkMuted,
            )
            Text(
                text = formatDuration(durationSec),
                style = FeeloTheme.typography.labelMedium,
                color = colors.onDarkMuted,
            )
        }
    }
}

/**
 * The video's card under the progress bar: collection chip and title on the start side (right in
 * RTL), favorite toggle on the end side, both from the top so a 2–3 line title never pushes the
 * heart around.
 */
@Composable
private fun VideoInfo(state: PlayerUiState.Ready, onEvent: (PlayerEvent) -> Unit) {
    val colors = FeeloTheme.colors
    val spacing = FeeloTheme.spacing
    val cover = state.collectionStyle
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.darkSurface, FeeloTheme.shapes.card)
            .padding(spacing.lg),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Box(
                Modifier
                    .background(Color(cover.bg), FeeloTheme.shapes.pill)
                    .padding(horizontal = spacing.md, vertical = spacing.xs),
            ) {
                Text(
                    text = state.collectionTitle,
                    style = FeeloTheme.typography.labelLarge,
                    color = Color(cover.fg),
                    maxLines = 1,
                )
            }
            Text(
                text = state.video.titleAr,
                style = FeeloTheme.typography.titleLarge,
                color = colors.onDark,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.width(spacing.md))
        FavoriteToggle(
            isFavorite = state.isFavorite,
            onToggle = { onEvent(PlayerEvent.ToggleFavorite) },
        )
    }
}

/** 56dp heart button (primary child action): filled red on a soft red tile when it's a favorite. */
@Composable
private fun FavoriteToggle(isFavorite: Boolean, onToggle: () -> Unit) {
    val colors = FeeloTheme.colors
    val shape = FeeloTheme.shapes.iconButton
    val label = stringResource(if (isFavorite) Res.string.child_favorite_remove else Res.string.child_favorite_add)
    Box(
        Modifier
            .size(FeeloTheme.sizes.childTouchTarget)
            .clip(shape)
            .background(if (isFavorite) colors.danger.copy(alpha = 0.18f) else colors.darkSurfaceHigh)
            .toggleable(value = isFavorite, role = Role.Switch, onValueChange = { onToggle() }),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon = if (isFavorite) FeeloIcons.HeartFilled else FeeloIcons.Heart,
            contentDescription = label,
            tint = if (isFavorite) colors.danger else colors.onDark,
            size = 26.dp,
        )
    }
}

@Composable
private fun RotateHint() {
    val colors = FeeloTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = FeeloTheme.spacing.lg),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.child_player_rotate_hint),
            style = FeeloTheme.typography.bodyMedium,
            color = colors.onDarkMuted,
        )
        Spacer(Modifier.width(FeeloTheme.spacing.xs))
        Icon(icon = FeeloIcons.Info, contentDescription = null, tint = colors.onDarkMuted, size = 18.dp)
    }
}
