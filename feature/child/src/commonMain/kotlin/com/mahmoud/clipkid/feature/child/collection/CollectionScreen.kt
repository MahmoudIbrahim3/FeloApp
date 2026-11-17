package com.mahmoud.clipkid.feature.child.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.component.EmptyState
import com.mahmoud.clipkid.core.designsystem.component.IconButtonLarge
import com.mahmoud.clipkid.core.designsystem.component.IconButtonTone
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.component.years
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.core.designsystem.theme.LocalWindowSize
import com.mahmoud.clipkid.core.designsystem.theme.WindowSize
import com.mahmoud.clipkid.core.domain.usecase.CollectionItem
import com.mahmoud.clipkid.core.domain.usecase.CollectionPage
import com.mahmoud.clipkid.core.domain.usecase.VideoStatus
import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.feature.child.common.WideVideoCard
import com.mahmoud.clipkid.feature.child.resources.Res
import com.mahmoud.clipkid.feature.child.resources.child_back
import com.mahmoud.clipkid.feature.child.resources.child_collection_empty_message
import com.mahmoud.clipkid.feature.child.resources.child_collection_empty_title
import com.mahmoud.clipkid.feature.child.resources.child_collection_meta
import com.mahmoud.clipkid.feature.child.resources.child_collection_progress
import com.mahmoud.clipkid.feature.child.resources.child_status_in_progress
import com.mahmoud.clipkid.feature.child.resources.child_status_new
import com.mahmoud.clipkid.feature.child.resources.child_status_watched
import org.jetbrains.compose.resources.stringResource

/** Collection screen: route wiring. */
@Composable
fun CollectionRoute(
    viewModel: CollectionViewModel,
    onEffect: (CollectionEffect) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect(onEffect)
    }
    CollectionScreen(state, viewModel::onEvent)
}

@Composable
fun CollectionScreen(
    state: CollectionUiState,
    onEvent: (CollectionEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClipKidTheme.colors
    Box(modifier.fillMaxSize().background(colors.background)) {
        when (state) {
            CollectionUiState.Loading -> Unit
            CollectionUiState.Unavailable -> CollectionEmpty(onEvent)
            is CollectionUiState.Content -> CollectionBody(state.page, onEvent)
        }
    }
}

@Composable
private fun CollectionEmpty(onEvent: (CollectionEvent) -> Unit) {
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(ClipKidTheme.spacing.gutter)) {
        IconButtonLarge(
            icon = ClipKidIcons.Back,
            contentDescription = stringResource(Res.string.child_back),
            onClick = { onEvent(CollectionEvent.BackClicked) },
        )
        EmptyState(
            icon = ClipKidIcons.Book,
            title = stringResource(Res.string.child_collection_empty_title),
            message = stringResource(Res.string.child_collection_empty_message),
        )
    }
}

@Composable
private fun CollectionBody(page: CollectionPage, onEvent: (CollectionEvent) -> Unit) {
    val gutter = ClipKidTheme.spacing.gutter
    // Phone: one full-width card per row (YouTube Kids style). Tablet: two per row,
    // so a 16:9 thumbnail never fills the whole screen height.
    val columns = if (LocalWindowSize.current == WindowSize.Expanded) 2 else 1

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = gutter, end = gutter, bottom = ClipKidTheme.spacing.xxl),
        horizontalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.xl),
    ) {
        item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
            CollectionHeader(page, onEvent)
        }

        if (page.items.isEmpty()) {
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                EmptyState(
                    icon = ClipKidIcons.Book,
                    title = stringResource(Res.string.child_collection_empty_title),
                    message = stringResource(Res.string.child_collection_empty_message),
                )
            }
        } else {
            items(page.items, key = { it.video.youtubeId }) { item ->
                CollectionVideoCard(item) { onEvent(CollectionEvent.VideoClicked(item.video.youtubeId)) }
            }
        }
    }
}

/** Full-width card: big 16:9 thumbnail, title and watch status underneath. */
@Composable
private fun CollectionVideoCard(item: CollectionItem, onClick: () -> Unit) {
    val colors = ClipKidTheme.colors
    val (label, color) = when (item.status) {
        VideoStatus.Watched -> stringResource(Res.string.child_status_watched) to colors.success
        is VideoStatus.InProgress -> stringResource(Res.string.child_status_in_progress) to colors.primary
        VideoStatus.New -> stringResource(Res.string.child_status_new) to colors.muted
    }
    WideVideoCard(
        video = item.video,
        onClick = onClick,
        progress = if (item.status is VideoStatus.InProgress) item.progress else null,
        titleTrailing = {
            Text(text = label, style = ClipKidTheme.typography.labelLarge, color = color, maxLines = 1)
        },
    )
}

/**
 * Calm header card: inset from the screen edges with even padding and all corners rounded,
 * a softened tint of the collection color, back button and title on one line, meta under
 * the title, and a gentle progress bar.
 */
@Composable
private fun CollectionHeader(page: CollectionPage, onEvent: (CollectionEvent) -> Unit) {
    val colors = ClipKidTheme.colors
    val spacing = ClipKidTheme.spacing
    val cover = page.collection.cover
    val fg = Color(cover.fg)
    // Half-way between the collection color and white: keeps its identity, drops the loudness.
    val tint = lerp(Color(cover.bg), colors.surface, 0.5f)
    val total = page.items.size
    val fraction = if (total == 0) 0f else page.watchedCount.toFloat() / total

    Column(
        Modifier
            .padding(top = spacing.md)
            .fillMaxWidth()
            .background(tint, ClipKidTheme.shapes.card)
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.lg),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButtonLarge(
                icon = ClipKidIcons.Back,
                contentDescription = stringResource(Res.string.child_back),
                onClick = { onEvent(CollectionEvent.BackClicked) },
                tone = IconButtonTone.Surface,
                compact = true,
            )
            Spacer(Modifier.width(spacing.md))
            Column(Modifier.weight(1f)) {
                Text(
                    text = page.collection.titleAr,
                    style = ClipKidTheme.typography.titleLarge,
                    color = colors.ink,
                    maxLines = 2,
                )
                Text(
                    text = stringResource(
                        Res.string.child_collection_meta,
                        total.toString(),
                        ageRangeLabel(page.collection.ageBands),
                    ),
                    style = ClipKidTheme.typography.bodySmall,
                    color = colors.muted,
                    maxLines = 1,
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressBar(fraction, fg, Modifier.weight(1f))
            Spacer(Modifier.width(spacing.sm))
            Text(
                text = stringResource(
                    Res.string.child_collection_progress,
                    page.watchedCount.toString(),
                    total.toString(),
                ),
                style = ClipKidTheme.typography.labelMedium,
                color = colors.ink,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ProgressBar(fraction: Float, fg: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(10.dp)
            .background(fg.copy(alpha = 0.15f), ClipKidTheme.shapes.pill),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(10.dp)
                .background(fg, ClipKidTheme.shapes.pill),
        )
    }
}

/** "3–8" from the first and last age band of a collection. */
internal fun ageRangeLabel(bands: Set<AgeBand>): String {
    val ordered = bands.sortedBy { it.ordinal }
    val first = ordered.firstOrNull() ?: return ""
    val last = ordered.last()
    return "${first.years().first}–${last.years().second}"
}
