package com.mahmoud.feelo.feature.child.favorites

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
import com.mahmoud.feelo.core.designsystem.component.EmptyState
import com.mahmoud.feelo.core.designsystem.component.Icon
import com.mahmoud.feelo.core.designsystem.component.Text
import com.mahmoud.feelo.core.designsystem.component.VideoCard
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcons
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import com.mahmoud.feelo.core.designsystem.theme.LocalWindowSize
import com.mahmoud.feelo.core.designsystem.theme.WindowSize
import com.mahmoud.feelo.core.domain.usecase.FavoriteItem
import com.mahmoud.feelo.feature.child.common.ChildTab
import com.mahmoud.feelo.feature.child.common.ChildTabsScaffold
import com.mahmoud.feelo.feature.child.resources.Res
import com.mahmoud.feelo.feature.child.resources.child_favorite_remove
import com.mahmoud.feelo.feature.child.resources.child_favorites_empty_message
import com.mahmoud.feelo.feature.child.resources.child_favorites_empty_title
import com.mahmoud.feelo.feature.child.resources.child_favorites_note
import com.mahmoud.feelo.feature.child.resources.child_favorites_subtitle
import com.mahmoud.feelo.feature.child.resources.child_favorites_title
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
    val gutter = FeeloTheme.spacing.gutter
    val colors = FeeloTheme.colors
    val columns = if (expanded) 3 else 2

    Box(modifier.fillMaxSize().background(colors.background).statusBarsPadding()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = PaddingValues(start = gutter, end = gutter, bottom = FeeloTheme.spacing.xxl),
            horizontalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.md),
            verticalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.md),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(top = FeeloTheme.spacing.md)) {
                    Text(
                        text = stringResource(Res.string.child_favorites_title),
                        style = FeeloTheme.typography.displayLarge,
                        color = colors.ink,
                        maxLines = 1,
                    )
                    Text(
                        text = stringResource(Res.string.child_favorites_subtitle),
                        style = FeeloTheme.typography.bodyLarge,
                        color = colors.muted,
                    )
                }
            }

            when (state) {
                FavoritesUiState.Loading -> Unit
                is FavoritesUiState.Content -> if (state.isEmpty) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            icon = FeeloIcons.Heart,
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
        modifier = Modifier.fillMaxWidth(),
        action = {
            Box(
                Modifier
                    .size(40.dp)
                    .background(FeeloTheme.colors.surface, FeeloTheme.shapes.pill)
                    .clickable { onEvent(FavoritesEvent.Unfavorite(favorite.video.youtubeId)) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon = FeeloIcons.HeartFilled,
                    contentDescription = stringResource(Res.string.child_favorite_remove),
                    tint = FeeloTheme.colors.danger,
                    size = 22.dp,
                )
            }
        },
        supportingContent = {
            Box(
                Modifier
                    .background(Color(cover.bg), FeeloTheme.shapes.pill)
                    .padding(horizontal = FeeloTheme.spacing.sm, vertical = FeeloTheme.spacing.xxs),
            ) {
                Text(
                    text = favorite.collection.titleAr,
                    style = FeeloTheme.typography.labelMedium,
                    color = Color(cover.fg),
                    maxLines = 1,
                )
            }
        },
    )
}

@Composable
private fun NoteCard() {
    val colors = FeeloTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.surface, FeeloTheme.shapes.cardSmall)
            .padding(FeeloTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon = FeeloIcons.Info, contentDescription = null, tint = colors.muted, size = 22.dp)
        Spacer(Modifier.width(FeeloTheme.spacing.sm))
        Text(
            text = stringResource(Res.string.child_favorites_note),
            style = FeeloTheme.typography.bodyMedium,
            color = colors.muted,
            modifier = Modifier.weight(1f),
        )
    }
}
