package com.mahmoud.clipkid.feature.child.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.component.EmptyState
import com.mahmoud.clipkid.core.designsystem.component.Icon
import com.mahmoud.clipkid.core.designsystem.component.IconButtonLarge
import com.mahmoud.clipkid.core.designsystem.component.IconButtonTone
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.component.VideoListRow
import com.mahmoud.clipkid.core.designsystem.component.YouTubeBadgePlaceholder
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.text.toArabicDigits
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.core.domain.usecase.CollectionItem
import com.mahmoud.clipkid.core.domain.usecase.CollectionPage
import com.mahmoud.clipkid.core.domain.usecase.VideoStatus
import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.core.model.CoverMark
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = ClipKidTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.md),
    ) {
        item(key = "banner") {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(bg, RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                    .padding(horizontal = ClipKidTheme.spacing.gutter)
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
                    Spacer(Modifier.weight(1f))
                    CoverMarkView(cover.mark, fg)
                }
                Text(
                    text = page.collection.titleAr,
                    style = ClipKidTheme.typography.displayMedium,
                    color = colors.ink,
                    maxLines = 1,
                )
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
            item(key = "empty") {
                EmptyState(
                    icon = ClipKidIcons.Book,
                    title = stringResource(Res.string.child_collection_empty_title),
                    message = stringResource(Res.string.child_collection_empty_message),
                )
            }
        } else {
            items(page.items, key = { it.video.youtubeId }) { item ->
                CollectionRow(item) { onEvent(CollectionEvent.VideoClicked(item.video.youtubeId)) }
            }
        }
    }
}

@Composable
private fun CollectionRow(item: CollectionItem, onClick: () -> Unit) {
    val colors = ClipKidTheme.colors
    val (label, color) = when (item.status) {
        VideoStatus.Watched -> stringResource(Res.string.child_status_watched) to colors.success
        is VideoStatus.InProgress -> stringResource(Res.string.child_status_in_progress) to colors.primary
        VideoStatus.New -> stringResource(Res.string.child_status_new) to colors.muted
    }
    VideoListRow(
        title = item.video.titleAr,
        thumbnailUrl = item.video.thumbnailUrl,
        durationSec = item.video.durationSec,
        status = label,
        statusColor = color,
        onClick = onClick,
        youtubeBadge = { YouTubeBadgePlaceholder() },
        progress = if (item.status is VideoStatus.InProgress) item.progress else null,
        selected = item.status is VideoStatus.InProgress,
        modifier = Modifier.padding(horizontal = ClipKidTheme.spacing.gutter),
    )
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

/** The cover mark: a glyph in the display font, or a bundled icon. */
@Composable
internal fun CoverMarkView(mark: CoverMark, fg: Color) {
    when (mark) {
        is CoverMark.Glyph -> Text(
            text = mark.text,
            style = ClipKidTheme.typography.displayLarge,
            color = fg,
            maxLines = 1,
        )
        is CoverMark.Icon -> Icon(
            icon = ClipKidIcons.forCoverKey(mark.key),
            contentDescription = null,
            tint = fg,
            size = 56.dp,
        )
    }
}

/** "٢–٦" from the first and last age band of a collection. */
internal fun ageRangeLabel(bands: Set<AgeBand>): String {
    val ordered = bands.sortedBy { it.ordinal }
    val first = ordered.firstOrNull() ?: return ""
    val last = ordered.last()
    return "${first.fromAge().toArabicDigits()}–${last.toAge().toArabicDigits()}"
}

private fun AgeBand.fromAge(): Int = when (this) {
    AgeBand.A2_3 -> 2
    AgeBand.A4_6 -> 4
    AgeBand.A7_9 -> 7
    AgeBand.A10_12 -> 10
}

private fun AgeBand.toAge(): Int = when (this) {
    AgeBand.A2_3 -> 3
    AgeBand.A4_6 -> 6
    AgeBand.A7_9 -> 9
    AgeBand.A10_12 -> 12
}
