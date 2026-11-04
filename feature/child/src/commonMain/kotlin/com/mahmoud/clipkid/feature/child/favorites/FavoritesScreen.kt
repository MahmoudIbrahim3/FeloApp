package com.mahmoud.clipkid.feature.child.favorites

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.component.EmptyState
import com.mahmoud.clipkid.core.designsystem.component.Icon
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.component.VideoCard
import com.mahmoud.clipkid.core.designsystem.component.YouTubeBadgePlaceholder
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.core.designsystem.theme.LocalWindowSize
import com.mahmoud.clipkid.core.designsystem.theme.WindowSize
import com.mahmoud.clipkid.core.domain.usecase.FavoriteItem
import com.mahmoud.clipkid.feature.child.common.ChildTab
import com.mahmoud.clipkid.feature.child.common.ChildTabsScaffold
import com.mahmoud.clipkid.feature.child.resources.Res
import com.mahmoud.clipkid.feature.child.resources.child_favorite_remove
import com.mahmoud.clipkid.feature.child.resources.child_favorites_empty_message
import com.mahmoud.clipkid.feature.child.resources.child_favorites_empty_title
import com.mahmoud.clipkid.feature.child.resources.child_favorites_note
import com.mahmoud.clipkid.feature.child.resources.child_favorites_subtitle
import com.mahmoud.clipkid.feature.child.resources.child_favorites_title
import org.jetbrains.compose.resources.stringResource

/** Favorites tab: route wiring. The lock on the tablet rail opens the parental gate. */
@Composable
fun FavoritesRoute(
    viewModel: FavoritesViewModel,
    onEffect: (FavoritesEffect) -> Unit,
    onSelectTab: (ChildTab) -> Unit,
    onLock: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect(onEffect)
    }
    ChildTabsScaffold(
        selected = ChildTab.Favorites,
        onSelectTab = onSelectTab,
        onLock = onLock,
    ) {
        FavoritesScreen(state, viewModel::onEvent)
    }
}

@Composable
fun FavoritesScreen(
    state: FavoritesUiState,
    onEvent: (FavoritesEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = LocalWindowSize.current == WindowSize.Expanded
    val gutter = ClipKidTheme.spacing.gutter
    val colors = ClipKidTheme.colors
    val columns = if (expanded) 3 else 2

    Box(modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(start = gutter, end = gutter, bottom = ClipKidTheme.spacing.xxl),
            horizontalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.md),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(top = ClipKidTheme.spacing.md)) {
                    Text(
                        text = stringResource(Res.string.child_favorites_title),
                        style = ClipKidTheme.typography.displayLarge,
                        color = colors.ink,
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(Res.string.child_favorites_subtitle),
                        style = ClipKidTheme.typography.bodyLarge,
                        color = colors.muted,
                    )
                }
            }

            when (state) {
                FavoritesUiState.Loading -> Unit
                is FavoritesUiState.Content -> if (state.isEmpty) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            icon = ClipKidIcons.Heart,
                            title = stringResource(Res.string.child_favorites_empty_title),
                            message = stringResource(Res.string.child_favorites_empty_message),
                        )
                    }
                } else {
                    items(state.items, key = { it.video.youtubeId }) { favorite ->
                        FavoriteCard(favorite, onEvent)
                    }
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        NoteCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoriteCard(favorite: FavoriteItem, onEvent: (FavoritesEvent) -> Unit) {
    val cover = favorite.collection.cover
    VideoCard(
        title = favorite.video.titleAr,
        thumbnailUrl = favorite.video.thumbnailUrl,
        durationSec = favorite.video.durationSec,
        onClick = { onEvent(FavoritesEvent.VideoClicked(favorite.video.youtubeId)) },
        youtubeBadge = { YouTubeBadgePlaceholder() },
        modifier = Modifier.fillMaxWidth(),
        action = {
            Box(
                Modifier
                    .size(40.dp)
                    .background(ClipKidTheme.colors.surface, ClipKidTheme.shapes.pill)
                    .clickable { onEvent(FavoritesEvent.Unfavorite(favorite.video.youtubeId)) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon = ClipKidIcons.HeartFilled,
                    contentDescription = stringResource(Res.string.child_favorite_remove),
                    tint = ClipKidTheme.colors.danger,
                    size = 22.dp,
                )
            }
        },
        supportingContent = {
            Box(
                Modifier
                    .background(Color(cover.bg), ClipKidTheme.shapes.pill)
                    .padding(horizontal = ClipKidTheme.spacing.sm, vertical = ClipKidTheme.spacing.xxs),
            ) {
                Text(
                    text = favorite.collection.titleAr,
                    style = ClipKidTheme.typography.labelMedium,
                    color = Color(cover.fg),
                    maxLines = 1,
                )
            }
        },
    )
}

@Composable
private fun NoteCard() {
    val colors = ClipKidTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.surface, ClipKidTheme.shapes.cardSmall)
            .padding(ClipKidTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon = ClipKidIcons.Info, contentDescription = null, tint = colors.muted, size = 22.dp)
        Spacer(Modifier.width(ClipKidTheme.spacing.sm))
        Text(
            text = stringResource(Res.string.child_favorites_note),
            style = ClipKidTheme.typography.bodyMedium,
            color = colors.muted,
            modifier = Modifier.weight(1f),
        )
    }
}
