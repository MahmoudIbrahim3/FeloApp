package com.mahmoud.feelo.feature.child.home

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
import androidx.compose.ui.unit.sp
import com.mahmoud.feelo.core.designsystem.component.CollectionCover
import com.mahmoud.feelo.core.designsystem.component.EmptyState
import com.mahmoud.feelo.core.designsystem.component.Icon
import com.mahmoud.feelo.core.designsystem.component.IconButtonLarge
import com.mahmoud.feelo.core.designsystem.component.IconButtonTone
import com.mahmoud.feelo.core.designsystem.component.PrimaryButton
import com.mahmoud.feelo.core.designsystem.component.QuotaDots
import com.mahmoud.feelo.core.designsystem.component.ShelfHeader
import com.mahmoud.feelo.core.designsystem.component.Text
import com.mahmoud.feelo.core.designsystem.component.VideoThumbnail
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcons
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import com.mahmoud.feelo.core.designsystem.theme.LocalWindowSize
import com.mahmoud.feelo.core.designsystem.theme.WindowSize
import com.mahmoud.feelo.core.domain.usecase.ContinueWatching
import com.mahmoud.feelo.core.model.QuotaUnit
import com.mahmoud.feelo.core.model.Video
import com.mahmoud.feelo.core.model.WatchSource
import com.mahmoud.feelo.feature.child.common.ChildAvatar
import com.mahmoud.feelo.feature.child.common.ChildTab
import com.mahmoud.feelo.feature.child.common.ChildTabsScaffold
import com.mahmoud.feelo.feature.child.common.WideVideoCard
import com.mahmoud.feelo.feature.child.common.remainingLabel
import com.mahmoud.feelo.feature.child.resources.Res
import com.mahmoud.feelo.feature.child.resources.child_greeting
import com.mahmoud.feelo.feature.child.resources.child_home_continue_label
import com.mahmoud.feelo.feature.child.resources.child_home_empty_message
import com.mahmoud.feelo.feature.child.resources.child_home_empty_title
import com.mahmoud.feelo.feature.child.resources.child_home_lock_description
import com.mahmoud.feelo.feature.child.resources.child_home_new_today
import com.mahmoud.feelo.feature.child.resources.child_home_play
import com.mahmoud.feelo.feature.child.resources.child_loading
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
            content?.let { ChildAvatar(it.childName, it.avatarColor, size = 48.dp) }
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
    Box(modifier.fillMaxSize().background(FeeloTheme.colors.childBackground), contentAlignment = Alignment.Center) {
        Text(
            text = stringResource(Res.string.child_loading),
            style = FeeloTheme.typography.bodyLarge,
            color = FeeloTheme.colors.muted,
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
    val gutter = FeeloTheme.spacing.gutter
    val spacing = FeeloTheme.spacing
    val fullLine: LazyGridItemSpanScope.() -> GridItemSpan = { GridItemSpan(maxLineSpan) }
    // Rows are 12dp apart; every block except a section title adds 12dp under itself (24dp
    // between blocks), and a section title adds 8dp above itself. So a title sits close to
    // its own videos (12dp) and farther from what comes before it (32dp).
    val blockGap = Modifier.padding(bottom = spacing.sm)
    val titleGap = Modifier.padding(top = spacing.xs)

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (expanded) 3 else 1),
        modifier = modifier.fillMaxSize().background(FeeloTheme.colors.childBackground).statusBarsPadding(),
        contentPadding = PaddingValues(start = gutter, end = gutter, top = spacing.md, bottom = spacing.xxl),
        horizontalArrangement = Arrangement.spacedBy(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        item(key = "top", span = fullLine) {
            // Tablet: the rail already shows the avatar and the lock, so the
            // header keeps only the greeting and today's quota.
            HomeHeader(state, onEvent, showAvatarAndLock = !expanded, modifier = blockGap)
        }

        if (state.isEmpty) {
            item(key = "empty", span = fullLine) {
                EmptyState(
                    icon = FeeloIcons.Play,
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

/**
 * Avatar, then the greeting with today's quota under it (dots for a video limit, "فاضلك X دقيقة"
 * for a minutes limit), then the lock to parent mode on the end side (left in RTL). Avatar
 * and lock only when no rail shows them.
 */
@Composable
private fun HomeHeader(
    state: HomeUiState.Content,
    onEvent: (HomeEvent) -> Unit,
    showAvatarAndLock: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = FeeloTheme.colors
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showAvatarAndLock) {
            ChildAvatar(state.childName, state.avatarColor, size = 48.dp)
            Spacer(Modifier.width(FeeloTheme.spacing.md))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.xxs),
        ) {
            Text(
                text = stringResource(Res.string.child_greeting, state.childName),
                // A step under displayMedium (28sp) so the greeting fits beside the two 48dp circles.
                style = FeeloTheme.typography.displayMedium.copy(fontSize = 24.sp, lineHeight = 34.sp),
                color = colors.ink,
                maxLines = 1,
            )
            when (state.quota.unit) {
                QuotaUnit.Videos -> QuotaDots(
                    used = state.quota.used,
                    total = state.quota.total,
                    dotSize = 10.dp,
                )
                // A minutes limit has no countable steps (total is in seconds): text instead.
                QuotaUnit.Seconds -> Text(
                    text = remainingLabel(state.quota),
                    style = FeeloTheme.typography.titleSmall,
                    color = colors.muted,
                    maxLines = 1,
                )
            }
        }
        if (showAvatarAndLock) {
            Spacer(Modifier.width(FeeloTheme.spacing.sm))
            IconButtonLarge(
                icon = FeeloIcons.Lock,
                contentDescription = stringResource(Res.string.child_home_lock_description),
                onClick = { onEvent(HomeEvent.LockClicked) },
                tone = IconButtonTone.Surface,
                // Round and the avatar's size (48dp), so both ends of the header match.
                compact = true,
                shape = FeeloTheme.shapes.pill,
            )
        }
    }
}

/** Tablet hero: horizontal card, thumbnail on the start side, title and the play button beside it. */
@Composable
private fun ContinueHeroWide(cw: ContinueWatching, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = FeeloTheme.colors
    val shape = FeeloTheme.shapes.card
    Row(
        modifier
            .background(colors.surface, shape)
            .clickable(onClick = onClick)
            .padding(FeeloTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThumbWithPlay(cw.video, cw.progress, Modifier.width(300.dp).aspectRatio(16f / 9f))
        Spacer(Modifier.width(FeeloTheme.spacing.lg))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.sm)) {
            ContinueChip()
            Text(
                text = cw.video.titleAr,
                style = FeeloTheme.typography.titleLarge,
                color = colors.ink,
                maxLines = 2,
            )
            PrimaryButton(
                text = stringResource(Res.string.child_home_play),
                onClick = onClick,
                icon = FeeloIcons.Play,
                modifier = Modifier.width(160.dp),
            )
        }
    }
}

@Composable
private fun ContinueChip() {
    val colors = FeeloTheme.colors
    Box(
        Modifier
            .background(colors.accent, FeeloTheme.shapes.pill)
            .padding(horizontal = FeeloTheme.spacing.md, vertical = FeeloTheme.spacing.xs),
    ) {
        Text(
            text = stringResource(Res.string.child_home_continue_label),
            style = FeeloTheme.typography.labelMedium,
            color = colors.onAccent,
            maxLines = 1,
        )
    }
}

@Composable
private fun ThumbWithPlay(video: Video, progress: Float, modifier: Modifier) {
    val colors = FeeloTheme.colors
    VideoThumbnail(
        thumbnailUrl = video.thumbnailUrl,
        durationSec = video.durationSec,
        progress = progress,
        shape = FeeloTheme.shapes.thumbnail,
        modifier = modifier,
        center = {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .background(colors.surface, FeeloTheme.shapes.pill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon = FeeloIcons.PlayFilled,
                    contentDescription = null,
                    tint = colors.primary,
                    size = 28.dp,
                )
            }
        },
    )
}
