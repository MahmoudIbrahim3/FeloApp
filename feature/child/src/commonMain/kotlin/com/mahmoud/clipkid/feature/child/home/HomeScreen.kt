package com.mahmoud.clipkid.feature.child.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.component.CollectionCover
import com.mahmoud.clipkid.core.designsystem.component.EmptyState
import com.mahmoud.clipkid.core.designsystem.component.Icon
import com.mahmoud.clipkid.core.designsystem.component.IconButtonLarge
import com.mahmoud.clipkid.core.designsystem.component.IconButtonTone
import com.mahmoud.clipkid.core.designsystem.component.PrimaryButton
import com.mahmoud.clipkid.core.designsystem.component.QuotaDots
import com.mahmoud.clipkid.core.designsystem.component.QuotaPill
import com.mahmoud.clipkid.core.designsystem.component.Shelf
import com.mahmoud.clipkid.core.designsystem.component.ShelfHeader
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.component.VideoCard
import com.mahmoud.clipkid.core.designsystem.component.VideoThumbnail
import com.mahmoud.clipkid.core.designsystem.component.YouTubeBadgePlaceholder
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.core.designsystem.theme.LocalWindowSize
import com.mahmoud.clipkid.core.designsystem.theme.WindowSize
import com.mahmoud.clipkid.core.domain.usecase.ContinueWatching
import com.mahmoud.clipkid.core.domain.usecase.Shelf as VideoShelf
import com.mahmoud.clipkid.core.model.Video
import com.mahmoud.clipkid.core.model.WatchSource
import com.mahmoud.clipkid.feature.child.common.ChildAvatar
import com.mahmoud.clipkid.feature.child.common.ChildTab
import com.mahmoud.clipkid.feature.child.common.ChildTabsScaffold
import com.mahmoud.clipkid.feature.child.common.quotaLabel
import com.mahmoud.clipkid.feature.child.common.remainingLabel
import com.mahmoud.clipkid.feature.child.resources.Res
import com.mahmoud.clipkid.feature.child.resources.child_greeting
import com.mahmoud.clipkid.feature.child.resources.child_home_collections
import com.mahmoud.clipkid.feature.child.resources.child_home_continue_label
import com.mahmoud.clipkid.feature.child.resources.child_home_empty_message
import com.mahmoud.clipkid.feature.child.resources.child_home_empty_title
import com.mahmoud.clipkid.feature.child.resources.child_home_lock_description
import com.mahmoud.clipkid.feature.child.resources.child_home_new_today
import com.mahmoud.clipkid.feature.child.resources.child_home_play
import com.mahmoud.clipkid.feature.child.resources.child_home_tablet_quota_title
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

/** Home content, stateless. Phone: header, full-width hero, rails. Tablet: hero beside the quota card. */
@Composable
fun HomeContent(
    state: HomeUiState.Content,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = LocalWindowSize.current == WindowSize.Expanded
    val gutter = ClipKidTheme.spacing.gutter
    val cardWidth = if (expanded) 280.dp else 200.dp

    LazyColumn(
        modifier = modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = ClipKidTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.lg),
    ) {
        item(key = "top") {
            if (expanded) {
                TabletTop(state, onEvent, gutter)
            } else {
                PhoneHeader(state, onEvent, gutter)
            }
        }

        if (!expanded) {
            state.continueWatching?.let { cw ->
                item(key = "hero") {
                    ContinueHero(
                        cw = cw,
                        onClick = { onEvent(HomeEvent.VideoClicked(cw.video.youtubeId, WatchSource.ContinueWatching)) },
                        modifier = Modifier.padding(horizontal = gutter),
                    )
                }
            }
        }

        if (state.isEmpty) {
            item(key = "empty") {
                EmptyState(
                    icon = ClipKidIcons.Play,
                    title = stringResource(Res.string.child_home_empty_title),
                    message = stringResource(Res.string.child_home_empty_message),
                )
            }
        } else {
            item(key = "collections-title") {
                ShelfHeader(
                    title = stringResource(Res.string.child_home_collections),
                    modifier = Modifier.padding(horizontal = gutter),
                )
            }
            item(key = "collections") {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = gutter),
                    horizontalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.md),
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

            if (state.newToday.isNotEmpty()) {
                item(key = "new-today") {
                    Shelf(title = stringResource(Res.string.child_home_new_today)) {
                        items(state.newToday, key = { it.youtubeId }) { video ->
                            HomeVideoCard(video, WatchSource.NewToday, cardWidth, onEvent)
                        }
                    }
                }
            }

            state.shelves.forEach { shelf: VideoShelf ->
                item(key = "shelf-${shelf.collection.id}") {
                    Shelf(
                        title = shelf.collection.titleAr,
                        accentColor = Color(shelf.collection.cover.fg),
                        onSeeAll = { onEvent(HomeEvent.CollectionClicked(shelf.collection.id)) },
                    ) {
                        items(shelf.videos, key = { it.youtubeId }) { video ->
                            HomeVideoCard(video, WatchSource.Home, cardWidth, onEvent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhoneHeader(state: HomeUiState.Content, onEvent: (HomeEvent) -> Unit, gutter: androidx.compose.ui.unit.Dp) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = gutter),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ChildAvatar(state.childName, state.avatarColor, size = 56.dp)
        Spacer(Modifier.width(ClipKidTheme.spacing.md))
        Text(
            text = stringResource(Res.string.child_greeting),
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

@Composable
private fun TabletTop(state: HomeUiState.Content, onEvent: (HomeEvent) -> Unit, gutter: androidx.compose.ui.unit.Dp) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = gutter),
        horizontalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.md),
    ) {
        state.continueWatching?.let { cw ->
            ContinueHeroWide(
                cw = cw,
                onClick = { onEvent(HomeEvent.VideoClicked(cw.video.youtubeId, WatchSource.ContinueWatching)) },
                modifier = Modifier.weight(2f),
            )
        }
        QuotaCard(state, Modifier.weight(1f))
    }
}

@Composable
private fun QuotaCard(state: HomeUiState.Content, modifier: Modifier = Modifier) {
    val colors = ClipKidTheme.colors
    val shape = ClipKidTheme.shapes.card
    Column(
        modifier
            .background(colors.surface, shape)
            .padding(ClipKidTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.sm),
    ) {
        Text(
            text = stringResource(Res.string.child_greeting),
            style = ClipKidTheme.typography.displayMedium,
            color = colors.ink,
            maxLines = 1,
        )
        Text(
            text = stringResource(Res.string.child_home_tablet_quota_title),
            style = ClipKidTheme.typography.bodyMedium,
            color = colors.muted,
        )
        if (state.quota.unit == com.mahmoud.clipkid.core.model.QuotaUnit.Videos) {
            QuotaDots(used = state.quota.used, total = state.quota.total)
        }
        Text(
            text = remainingLabel(state.quota),
            style = ClipKidTheme.typography.titleMedium,
            color = colors.ink,
        )
    }
}

/** Phone hero: full-width thumbnail with progress, title and the continue chip. */
@Composable
private fun ContinueHero(cw: ContinueWatching, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ClipKidTheme.colors
    val shape = ClipKidTheme.shapes.card
    Column(
        modifier
            .fillMaxWidth()
            .background(colors.surface, shape)
            .clickable(onClick = onClick)
            .padding(ClipKidTheme.spacing.sm),
        verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.sm),
    ) {
        ThumbWithPlay(cw.video, cw.progress, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        Row(
            Modifier.padding(horizontal = ClipKidTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = cw.video.titleAr,
                style = ClipKidTheme.typography.titleMedium,
                color = colors.ink,
                modifier = Modifier.weight(1f),
                maxLines = 2,
            )
            Spacer(Modifier.width(ClipKidTheme.spacing.sm))
            ContinueChip()
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
        youtubeBadge = { YouTubeBadgePlaceholder() },
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

@Composable
private fun HomeVideoCard(
    video: Video,
    source: WatchSource,
    width: androidx.compose.ui.unit.Dp,
    onEvent: (HomeEvent) -> Unit,
) {
    VideoCard(
        title = video.titleAr,
        thumbnailUrl = video.thumbnailUrl,
        durationSec = video.durationSec,
        onClick = { onEvent(HomeEvent.VideoClicked(video.youtubeId, source)) },
        youtubeBadge = { YouTubeBadgePlaceholder() },
        thumbnailShape = ClipKidTheme.shapes.thumbnail,
        modifier = Modifier.width(width),
    )
}
