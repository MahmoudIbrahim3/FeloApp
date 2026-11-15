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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.component.EmptyState
import com.mahmoud.clipkid.core.designsystem.component.IconButtonLarge
import com.mahmoud.clipkid.core.designsystem.component.IconButtonTone
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.component.VideoCard
import com.mahmoud.clipkid.core.designsystem.component.YouTubeBadgePlaceholder
import com.mahmoud.clipkid.core.designsystem.component.years
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.text.toArabicDigits
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.core.designsystem.theme.LocalWindowSize
import com.mahmoud.clipkid.core.designsystem.theme.WindowSize
import com.mahmoud.clipkid.core.domain.usecase.CollectionItem
import com.mahmoud.clipkid.core.domain.usecase.CollectionPage
import com.mahmoud.clipkid.core.domain.usecase.VideoStatus
import com.mahmoud.clipkid.core.model.AgeBand
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
    val colors = ClipKidTheme.colors
    val cover = page.collection.cover
    val fg = Color(cover.fg)
    val bg = Color(cover.bg)
    val total = page.items.size
    val fraction = if (total == 0) 0f else page.watchedCount.toFloat() / total
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
        item(key = "banner", span = { GridItemSpan(maxLineSpan) }) {
            Column(
                Modifier
                    .bleedHorizontally(gutter)
                    .fillMaxWidth()
                    .background(bg, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(horizontal = gutter)
                    .padding(bottom = ClipKidTheme.spacing.xl),
                verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.sm),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButtonLarge(
                        icon = ClipKidIcons.Back,
                        contentDescription = stringResource(Res.string.child_back),
                        onClick = { onEvent(CollectionEvent.BackClicked) },
                        tone = IconButtonTone.Surface,
                        compact = true,
                    )
                    Spacer(Modifier.width(ClipKidTheme.spacing.md))
                    Text(
                        text = page.collection.titleAr,
                        style = ClipKidTheme.typography.displayMedium,
                        color = colors.ink,
                        maxLines = 2,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    text = stringResource(
                        Res.string.child_collection_meta,
                        total.toArabicDigits(),
                        ageRangeLabel(page.collection.ageBands),
                    ),
                    style = ClipKidTheme.typography.bodyMedium,
                    color = colors.ink,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(
                            Res.string.child_collection_progress,
                            page.watchedCount.toArabicDigits(),
                            total.toArabicDigits(),
                        ),
                        style = ClipKidTheme.typography.labelLarge,
                        color = fg,
                    )
                    Spacer(Modifier.width(ClipKidTheme.spacing.md))
                    ProgressBar(fraction, fg, Modifier.weight(1f))
                }
            }
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
    VideoCard(
        title = item.video.titleAr,
        thumbnailUrl = item.video.thumbnailUrl,
        durationSec = item.video.durationSec,
        onClick = onClick,
        youtubeBadge = { YouTubeBadgePlaceholder() },
        progress = if (item.status is VideoStatus.InProgress) item.progress else null,
        aspectRatio = 16f / 9f,
        thumbnailShape = ClipKidTheme.shapes.thumbnail,
        supportingContent = {
            Text(text = label, style = ClipKidTheme.typography.labelLarge, color = color, maxLines = 1)
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Lets the banner ignore the grid's side padding and reach both screen edges. */
private fun Modifier.bleedHorizontally(bleed: Dp): Modifier = layout { measurable, constraints ->
    val bleedPx = bleed.roundToPx()
    val width = constraints.maxWidth + bleedPx * 2
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-bleedPx, 0) }
}

@Composable
private fun ProgressBar(fraction: Float, fg: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(8.dp)
            .background(ClipKidTheme.colors.surface.copy(alpha = 0.7f), ClipKidTheme.shapes.pill),
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(8.dp)
                .background(fg, ClipKidTheme.shapes.pill),
        )
    }
}

/** "٣–٨" from the first and last age band of a collection. */
internal fun ageRangeLabel(bands: Set<AgeBand>): String {
    val ordered = bands.sortedBy { it.ordinal }
    val first = ordered.firstOrNull() ?: return ""
    val last = ordered.last()
    return "${first.years().first.toArabicDigits()}–${last.years().second.toArabicDigits()}"
}
