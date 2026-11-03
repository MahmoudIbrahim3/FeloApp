package com.mahmoud.clipkid.core.designsystem.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.preview.ClipKidPreview
import com.mahmoud.clipkid.core.designsystem.resources.Res
import com.mahmoud.clipkid.core.designsystem.resources.ds_see_all
import com.mahmoud.clipkid.core.designsystem.resources.showcase_letters
import com.mahmoud.clipkid.core.designsystem.resources.showcase_new_today
import com.mahmoud.clipkid.core.designsystem.resources.showcase_video_count_fruits
import com.mahmoud.clipkid.core.designsystem.resources.showcase_video_moon
import com.mahmoud.clipkid.core.designsystem.resources.showcase_video_morning_dua
import com.mahmoud.clipkid.core.designsystem.resources.showcase_video_rabbit
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Titled horizontal row of items.
 *
 * The header is inset by [gutter]; the [LazyRow] spans the full width and only its
 * content is padded, so cards scroll past the end edge (left in RTL) like the reference
 * screens. Place it in a container WITHOUT horizontal padding.
 */
@Composable
fun Shelf(
    title: String,
    modifier: Modifier = Modifier,
    accentColor: Color = ClipKidTheme.colors.primary,
    onSeeAll: (() -> Unit)? = null,
    seeAllLabel: String = stringResource(Res.string.ds_see_all),
    gutter: Dp = ClipKidTheme.spacing.gutter,
    itemSpacing: Dp = ClipKidTheme.spacing.sm,
    content: LazyListScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth()) {
        ShelfHeader(
            title = title,
            accentColor = accentColor,
            onSeeAll = onSeeAll,
            seeAllLabel = seeAllLabel,
            modifier = Modifier.padding(horizontal = gutter),
        )
        Spacer(Modifier.height(ClipKidTheme.spacing.xs))
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = gutter),
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            content = content,
        )
    }
}

/** Accent bar + title on the start side, "see all" link on the end side. */
@Composable
fun ShelfHeader(
    title: String,
    modifier: Modifier = Modifier,
    accentColor: Color = ClipKidTheme.colors.primary,
    onSeeAll: (() -> Unit)? = null,
    seeAllLabel: String = stringResource(Res.string.ds_see_all),
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = ClipKidTheme.sizes.minTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 8.dp, height = 24.dp)
                .background(accentColor, ClipKidTheme.shapes.pill),
        )
        Spacer(Modifier.width(ClipKidTheme.spacing.sm))
        Text(
            text = title,
            style = ClipKidTheme.typography.titleLarge,
            color = ClipKidTheme.colors.ink,
            maxLines = 1,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        if (onSeeAll != null) {
            val interaction = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .heightIn(min = ClipKidTheme.sizes.minTouchTarget)
                    .clip(ClipKidTheme.shapes.pill)
                    .clickable(
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        role = Role.Button,
                        onClick = onSeeAll,
                    )
                    .padding(horizontal = ClipKidTheme.spacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(seeAllLabel, style = ClipKidTheme.typography.labelLarge, color = ClipKidTheme.colors.primary)
                Spacer(Modifier.width(ClipKidTheme.spacing.xxs))
                Icon(ClipKidIcons.Forward, contentDescription = null, tint = ClipKidTheme.colors.primary, size = 18.dp)
            }
        }
    }
}

// region Showcase

/** Note: rendered edge-to-edge (no horizontal padding) to show the bleed. */
@Composable
fun ShelfShowcase() {
    val videos = listOf(
        Triple(stringResource(Res.string.showcase_video_moon), 220, Color(0xFFC9D3DE)),
        Triple(stringResource(Res.string.showcase_video_morning_dua), 130, Color(0xFFD8CFC6)),
        Triple(stringResource(Res.string.showcase_video_count_fruits), 185, Color(0xFFC6D3C8)),
        Triple(stringResource(Res.string.showcase_video_rabbit), 310, Color(0xFFD3CADD)),
    )
    Column(verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.xl)) {
        Shelf(title = stringResource(Res.string.showcase_new_today), onSeeAll = {}) {
            items(videos) { (title, duration, tint) ->
                VideoCard(
                    title = title,
                    thumbnailUrl = null,
                    durationSec = duration,
                    placeholderColor = tint,
                    onClick = {},
                    youtubeBadge = { YouTubeBadgePlaceholder() },
                    modifier = Modifier.width(160.dp),
                )
            }
        }
        Shelf(title = stringResource(Res.string.showcase_letters), accentColor = ClipKidTheme.colors.accent, onSeeAll = {}) {
            items(videos.reversed()) { (title, duration, tint) ->
                VideoCard(
                    title = title,
                    thumbnailUrl = null,
                    durationSec = duration,
                    placeholderColor = tint,
                    onClick = {},
                    youtubeBadge = { YouTubeBadgePlaceholder() },
                    modifier = Modifier.width(160.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun ShelfPreview() = ClipKidPreview { Box(Modifier.padding(vertical = 8.dp)) { ShelfShowcase() } }

// endregion
