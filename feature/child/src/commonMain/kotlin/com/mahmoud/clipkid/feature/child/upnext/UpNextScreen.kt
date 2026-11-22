package com.mahmoud.clipkid.feature.child.upnext

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.mahmoud.clipkid.core.designsystem.component.QuotaPill
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.component.VideoCard
import com.mahmoud.clipkid.core.designsystem.component.VideoThumbnail
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.core.designsystem.theme.CoverPalettes
import com.mahmoud.clipkid.core.designsystem.theme.SurfaceTone
import com.mahmoud.clipkid.core.model.Video
import com.mahmoud.clipkid.feature.child.common.DarkOutlineButton
import com.mahmoud.clipkid.feature.child.common.quotaLabel
import com.mahmoud.clipkid.feature.child.common.remainingLabel
import com.mahmoud.clipkid.feature.child.resources.Res
import com.mahmoud.clipkid.feature.child.resources.child_back
import com.mahmoud.clipkid.feature.child.resources.child_loading
import com.mahmoud.clipkid.feature.child.resources.child_upnext_finished
import com.mahmoud.clipkid.feature.child.resources.child_upnext_home
import com.mahmoud.clipkid.feature.child.resources.child_upnext_next_in
import com.mahmoud.clipkid.feature.child.resources.child_upnext_replay
import com.mahmoud.clipkid.feature.child.resources.child_upnext_tag
import com.mahmoud.clipkid.feature.child.resources.child_upnext_title
import org.jetbrains.compose.resources.stringResource

/** Up Next screen: route wiring. No autoplay and no countdown. */
@Composable
fun UpNextRoute(
    viewModel: UpNextViewModel,
    onEffect: (UpNextEffect) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect(onEffect)
    }
    UpNextScreen(state, viewModel::onEvent)
}

@Composable
fun UpNextScreen(
    state: UpNextUiState,
    onEvent: (UpNextEvent) -> Unit,
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
        when (state) {
            UpNextUiState.Loading -> Text(
                text = stringResource(Res.string.child_loading),
                style = ClipKidTheme.typography.bodyLarge,
                color = colors.onDarkMuted,
            )
            is UpNextUiState.Ready -> UpNextBody(state, onEvent)
        }
    }
}

@Composable
// ColumnScope: lays out directly in UpNextScreen's Column, so the bottom Spacer can push the home button down.
private fun ColumnScope.UpNextBody(state: UpNextUiState.Ready, onEvent: (UpNextEvent) -> Unit) {
    val colors = ClipKidTheme.colors
    val finished = state.finished

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButtonLarge(
            icon = ClipKidIcons.Close,
            contentDescription = stringResource(Res.string.child_back),
            onClick = { onEvent(UpNextEvent.HomeClicked) },
            tone = IconButtonTone.OnDark,
            compact = true,
        )
        Spacer(Modifier.weight(1f))
        QuotaPill(
            used = state.quota.used,
            total = state.quota.total,
            label = remainingLabel(state.quota),
            tone = SurfaceTone.Dark,
        )
    }

    if (finished != null) {
        FinishedRow(finished, onReplay = { onEvent(UpNextEvent.ReplayClicked) })
    }

    Text(
        text = stringResource(Res.string.child_upnext_title),
        style = ClipKidTheme.typography.displayMedium,
        color = colors.onDark,
    )

    state.primary?.let { primary ->
        PrimaryChoice(
            video = primary,
            collectionTitle = state.primaryCollectionTitle.orEmpty(),
            onClick = { onEvent(UpNextEvent.ChoiceClicked(primary.youtubeId)) },
        )
    }

    state.tagPicks?.let { picks ->
        Column(verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.sm)) {
            Text(
                text = stringResource(Res.string.child_upnext_tag, picks.tag),
                style = ClipKidTheme.typography.titleSmall,
                color = colors.onDarkMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.md)) {
                picks.videos.forEach { video ->
                    VideoCard(
                        title = video.titleAr,
                        thumbnailUrl = video.thumbnailUrl,
                        durationSec = video.durationSec,
                        onClick = { onEvent(UpNextEvent.ChoiceClicked(video.youtubeId)) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    Spacer(Modifier.weight(1f))

    DarkOutlineButton(
        text = stringResource(Res.string.child_upnext_home),
        onClick = { onEvent(UpNextEvent.HomeClicked) },
        icon = ClipKidIcons.Home,
    )
}

/** "خلصته!" with the video that just finished and the free "تاني" replay. */
@Composable
private fun FinishedRow(video: Video, onReplay: () -> Unit) {
    val colors = ClipKidTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.darkSurface, ClipKidTheme.shapes.card)
            .padding(ClipKidTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VideoThumbnail(
            thumbnailUrl = video.thumbnailUrl,
            durationSec = null,
            modifier = Modifier.width(96.dp).aspectRatio(16f / 10f),
        )
        Spacer(Modifier.width(ClipKidTheme.spacing.md))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.xxs)) {
            Text(
                text = stringResource(Res.string.child_upnext_finished),
                style = ClipKidTheme.typography.labelLarge,
                color = Color(0xFFA8E6BF),
            )
            Text(
                text = video.titleAr,
                style = ClipKidTheme.typography.titleSmall,
                color = colors.onDark,
                maxLines = 2,
            )
        }
        Spacer(Modifier.width(ClipKidTheme.spacing.md))
        Row(
            Modifier
                .background(colors.darkSurfaceHigh, ClipKidTheme.shapes.button)
                .clickable(onClick = onReplay)
                .defaultMinSize(minHeight = ClipKidTheme.sizes.minTouchTarget)
                .padding(horizontal = ClipKidTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon = ClipKidIcons.Replay, contentDescription = null, tint = colors.onDark, size = 22.dp)
            Spacer(Modifier.width(ClipKidTheme.spacing.xs))
            Text(
                text = stringResource(Res.string.child_upnext_replay),
                style = ClipKidTheme.typography.button,
                color = colors.onDark,
            )
        }
    }
}

/** Primary choice: large card in the collection's color with the next video in its collection. */
@Composable
private fun PrimaryChoice(video: Video, collectionTitle: String, onClick: () -> Unit) {
    val colors = ClipKidTheme.colors
    val fg = CoverPalettes.Letters.fg
    Column(
        Modifier
            .fillMaxWidth()
            .background(CoverPalettes.Letters.bg, ClipKidTheme.shapes.card)
            .clickable(onClick = onClick)
            .padding(ClipKidTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.sm),
    ) {
        VideoThumbnail(
            thumbnailUrl = video.thumbnailUrl,
            durationSec = video.durationSec,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            center = {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .background(colors.primary, ClipKidTheme.shapes.pill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon = ClipKidIcons.PlayFilled, contentDescription = null, tint = colors.onPrimary, size = 28.dp)
                }
            },
        )
        Text(
            text = stringResource(Res.string.child_upnext_next_in, collectionTitle),
            style = ClipKidTheme.typography.labelMedium,
            color = fg,
        )
        Text(
            text = video.titleAr,
            style = ClipKidTheme.typography.titleLarge,
            color = colors.ink,
            maxLines = 2,
        )
    }
}
