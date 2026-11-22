package com.mahmoud.clipkid.feature.child.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
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
import com.mahmoud.clipkid.core.designsystem.component.CollectionCover
import com.mahmoud.clipkid.core.designsystem.component.EmptyState
import com.mahmoud.clipkid.core.designsystem.component.Icon
import com.mahmoud.clipkid.core.designsystem.component.IconButtonLarge
import com.mahmoud.clipkid.core.designsystem.component.IconButtonTone
import com.mahmoud.clipkid.core.designsystem.component.PrimaryButton
import com.mahmoud.clipkid.core.designsystem.component.QuotaPill
import com.mahmoud.clipkid.core.designsystem.component.ShelfHeader
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.component.VideoThumbnail
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.core.designsystem.theme.LocalWindowSize
import com.mahmoud.clipkid.core.designsystem.theme.WindowSize
import com.mahmoud.clipkid.core.domain.usecase.ContinueWatching
import com.mahmoud.clipkid.core.model.Video
import com.mahmoud.clipkid.core.model.WatchSource
import com.mahmoud.clipkid.feature.child.common.ChildAvatar
import com.mahmoud.clipkid.feature.child.common.ChildTab
import com.mahmoud.clipkid.feature.child.common.ChildTabsScaffold
import com.mahmoud.clipkid.feature.child.common.WideVideoCard
import com.mahmoud.clipkid.feature.child.common.quotaLabel
import com.mahmoud.clipkid.feature.child.resources.Res
import com.mahmoud.clipkid.feature.child.resources.child_greeting
import com.mahmoud.clipkid.feature.child.resources.child_home_continue_label
import com.mahmoud.clipkid.feature.child.resources.child_home_empty_message
import com.mahmoud.clipkid.feature.child.resources.child_home_empty_title
import com.mahmoud.clipkid.feature.child.resources.child_home_lock_description
import com.mahmoud.clipkid.feature.child.resources.child_home_new_today
import com.mahmoud.clipkid.feature.child.resources.child_home_play
import com.mahmoud.clipkid.feature.child.resources.child_loading
import org.jetbrains.compose.resources.stringResource

/** Home tab: route wiring. The ViewModel comes from the composition root (shared). */
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    onEffect: (HomeEffect) -> Unit,
    onSelectTab: (ChildTab) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect(onEffect)
    }
    val content = state as? HomeUiState.Content
    ChildTabsScaffold(
        selected = ChildTab.Home,
        onSelectTab = onSelectTab,
        onLock = { viewModel.onEvent(HomeEvent.LockClicked) },
        railHeader = {
            content?.let { ChildAvatar(it.childName, it.avatarColor, size = 56.dp) }
        },
    ) {
        when (val s = state) {
            HomeUiState.Loading -> HomeLoading()
            is HomeUiState.Content -> HomeContent(s, viewModel::onEvent)
        }
    }
}

@Composable
fun HomeLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(Res.string.child_loading),
            style = ClipKidTheme.typography.bodyLarge,
            color = ClipKidTheme.colors.muted,
        )
    }
}

/**
 * Home content, stateless. One vertical list, YouTube Kids style: header, the collection
 * covers (the only sideways row), the continue card, today's 3 picks, then one section per
 * collection (its name, "الكل", and its next 3 videos). No video appears twice, and the list
 * ends after the last section (no infinite feed). Tablet: the same order in three columns, so
 * every 3-video section is one row; the header stays one line and the continue card is a short
 * horizontal one, so at least two rows of videos fit on screen.
 */
@Composable
fun HomeContent(
    state: HomeUiState.Content,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = LocalWindowSize.current == WindowSize.Expanded
    val gutter = ClipKidTheme.spacing.gutter
    val spacing = ClipKidTheme.spacing
    val fullLine: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
    // Rows are 12dp apart; every block except a section title adds 12dp under itself (24dp
    // between blocks), and a section title adds 8dp above itself. So a title sits close to
    // its own videos (12dp) and farther from what comes before it (32dp).
    val blockGap = Modifier.padding(bottom = spacing.sm)
    val titleGap = Modifier.padding(top = spacing.xs)

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (expanded) 3 else 1),
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = gutter, end = gutter, top = spacing.md, bottom = spacing.xxl),
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item(key = "top", span = fullLine) {
            // Tablet: the rail already shows the avatar and the lock, so the line keeps
            // only the greeting and the quota pill.
            HomeHeader(state, onEvent, showAvatarAndLock = !expanded, modifier = blockGap)
        }

        if (state.isEmpty) {
            item(key = "empty", span = fullLine) {
                EmptyState(
                    icon = ClipKidIcons.Play,
                    title = stringResource(Res.string.child_home_empty_title),
                    message = stringResource(Res.string.child_home_empty_message),
                )
            }
            return@LazyVerticalGrid
        }

        item(key = "collections", span = fullLine) {
            // Scrolls edge to edge, so it ignores the grid's side padding.
            LazyRow(
                modifier = blockGap.bleedHorizontally(gutter),
                contentPadding = PaddingValues(horizontal = gutter),
                horizontalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                items(state.collections, key = { it.id }) { collection ->
                    CollectionCover(
                        style = collection.cover,
                        title = collection.titleAr,
                        onClick = { onEvent(HomeEvent.CollectionClicked(collection.id)) },
                    )
                }
            }
        }

        state.continueWatching?.let { cw ->
            item(key = "continue", span = fullLine) {
                val onClick = { onEvent(HomeEvent.VideoClicked(cw.video.youtubeId, WatchSource.ContinueWatching)) }
                if (expanded) {
                    // A full-width 16:9 card would fill a tablet screen: a short horizontal one instead.
                    ContinueHeroWide(cw = cw, onClick = onClick, modifier = blockGap.fillMaxWidth())
                } else {
                    WideVideoCard(
                        video = cw.video,
                        progress = cw.progress,
                        onClick = onClick,
                        titleTrailing = { ContinueChip() },
                        modifier = blockGap,
                    )
                }
            }
        }

        if (state.newToday.isNotEmpty()) {
            item(key = "new-today-title", span = fullLine) {
                ShelfHeader(title = stringResource(Res.string.child_home_new_today), modifier = titleGap)
            }
            gridItems(state.newToday, key = { it.youtubeId }) { video ->
                WideVideoCard(
                    video = video,
                    onClick = { onEvent(HomeEvent.VideoClicked(video.youtubeId, WatchSource.NewToday)) },
                    modifier = blockGap,
                )
            }
        }

        state.shelves.forEach { shelf ->
            val collection = shelf.collection
            item(key = "shelf-${collection.id}", span = fullLine) {
                ShelfHeader(
                    title = collection.titleAr,
                    accentColor = Color(collection.cover.fg),
                    onSeeAll = { onEvent(HomeEvent.CollectionClicked(collection.id)) },
                    modifier = titleGap,
                )
            }
            gridItems(shelf.videos, key = { "${collection.id}/${it.youtubeId}" }) { video ->
                WideVideoCard(
                    video = video,
                    onClick = { onEvent(HomeEvent.VideoClicked(video.youtubeId, WatchSource.Home)) },
                    modifier = blockGap,
                )
            }
        }
    }
}

/** Lets a full-width row ignore the grid's side padding and reach both screen edges. */
private fun Modifier.bleedHorizontally(bleed: Dp): Modifier = layout { measurable, constraints ->
    val bleedPx = bleed.roundToPx()
    val width = constraints.maxWidth + bleedPx * 2
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-bleedPx, 0) }
}

/** One line: avatar, greeting, quota pill and lock (avatar and lock only when no rail shows them). */
@Composable
private fun HomeHeader(
    state: HomeUiState.Content,
    onEvent: (HomeEvent) -> Unit,
    showAvatarAndLock: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showAvatarAndLock) {
            ChildAvatar(state.childName, state.avatarColor, size = 56.dp)
            Spacer(Modifier.width(ClipKidTheme.spacing.md))
        }
        Text(
            text = stringResource(Res.string.child_greeting, state.childName),
            style = ClipKidTheme.typography.displayMedium,
            color = ClipKidTheme.colors.ink,
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        QuotaPill(
            used = state.quota.used,
            total = state.quota.total,
            label = quotaLabel(state.quota),
        )
        if (showAvatarAndLock) {
            Spacer(Modifier.width(ClipKidTheme.spacing.sm))
            IconButtonLarge(
                icon = ClipKidIcons.Lock,
                contentDescription = stringResource(Res.string.child_home_lock_description),
                onClick = { onEvent(HomeEvent.LockClicked) },
                tone = IconButtonTone.Surface,
                compact = true,
            )
        }
    }
}

/** Tablet hero: horizontal card, thumbnail on the start side, title and the play button beside it. */
@Composable
private fun ContinueHeroWide(cw: ContinueWatching, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ClipKidTheme.colors
    val shape = ClipKidTheme.shapes.card
    Row(
        modifier
            .background(colors.surface, shape)
            .clickable(onClick = onClick)
            .padding(ClipKidTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThumbWithPlay(cw.video, cw.progress, Modifier.width(300.dp).aspectRatio(16f / 9f))
        Spacer(Modifier.width(ClipKidTheme.spacing.lg))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.sm)) {
            ContinueChip()
            Text(
                text = cw.video.titleAr,
                style = ClipKidTheme.typography.titleLarge,
                color = colors.ink,
                maxLines = 2,
            )
            PrimaryButton(
                text = stringResource(Res.string.child_home_play),
                onClick = onClick,
                icon = ClipKidIcons.Play,
                modifier = Modifier.width(160.dp),
            )
        }
    }
}

@Composable
private fun ContinueChip() {
    val colors = ClipKidTheme.colors
    Box(
        Modifier
            .background(colors.accent, ClipKidTheme.shapes.pill)
            .padding(horizontal = ClipKidTheme.spacing.md, vertical = ClipKidTheme.spacing.xs),
    ) {
        Text(
            text = stringResource(Res.string.child_home_continue_label),
            style = ClipKidTheme.typography.labelMedium,
            color = colors.onAccent,
            maxLines = 1,
        )
    }
}

@Composable
private fun ThumbWithPlay(video: Video, progress: Float, modifier: Modifier) {
    val colors = ClipKidTheme.colors
    VideoThumbnail(
        thumbnailUrl = video.thumbnailUrl,
        durationSec = video.durationSec,
        progress = progress,
        shape = ClipKidTheme.shapes.thumbnail,
        modifier = modifier,
        center = {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .background(colors.surface, ClipKidTheme.shapes.pill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon = ClipKidIcons.PlayFilled,
                    contentDescription = null,
                    tint = colors.primary,
                    size = 28.dp,
                )
            }
        },
    )
}
