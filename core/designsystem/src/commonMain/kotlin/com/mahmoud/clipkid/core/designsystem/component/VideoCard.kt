package com.mahmoud.clipkid.core.designsystem.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.preview.ClipKidPreview
import com.mahmoud.clipkid.core.designsystem.preview.ShowcaseColumn
import com.mahmoud.clipkid.core.designsystem.resources.Res
import com.mahmoud.clipkid.core.designsystem.resources.ds_duration
import com.mahmoud.clipkid.core.designsystem.resources.showcase_remove_favorite
import com.mahmoud.clipkid.core.designsystem.resources.showcase_video_letter_baa
import com.mahmoud.clipkid.core.designsystem.resources.showcase_video_moon
import com.mahmoud.clipkid.core.designsystem.text.formatDuration
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Video card: real YouTube thumbnail, the official YouTube attribution (always drawn, see
 * [VideoThumbnail]), duration badge, optional watch progress, and the title underneath.
 *
 * Layout in RTL (matches HomeFeed / Favorites): attribution top-end, optional [action]
 * (e.g. favorite heart) top-start, duration bottom-start, progress fills from the start edge.
 *
 * @param progress 0..1 watch progress, or null to hide the bar.
 * @param placeholderColor background shown while the thumbnail loads or when [thumbnailUrl] is null.
 * @param supportingContent optional content under the title (e.g. a collection tag).
 * @param titleTrailing optional short content on the title's line, at the end side (e.g. a watch status),
 *   aligned to the title's first baseline.
 * @param thumbnailShape corner shape of the thumbnail; large full-width cards use `ClipKidTheme.shapes.thumbnail`.
 */
@Composable
fun VideoCard(
    title: String,
    thumbnailUrl: String?,
    durationSec: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    placeholderColor: Color = ClipKidTheme.colors.thumbnailPlaceholder,
    action: (@Composable () -> Unit)? = null,
    supportingContent: (@Composable () -> Unit)? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
    aspectRatio: Float = 16f / 10f,
    thumbnailShape: Shape = ClipKidTheme.shapes.cardSmall,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.97f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
    ) {
        VideoThumbnail(
            thumbnailUrl = thumbnailUrl,
            durationSec = durationSec,
            progress = progress,
            placeholderColor = placeholderColor,
            action = action,
            shape = thumbnailShape,
            modifier = Modifier.fillMaxWidth().aspectRatio(aspectRatio),
        )
        Spacer(Modifier.height(ClipKidTheme.spacing.xs))
        Row(Modifier.fillMaxWidth()) {
            Text(
                text = title,
                style = ClipKidTheme.typography.titleMedium,
                color = ClipKidTheme.colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).alignByBaseline(),
            )
            if (titleTrailing != null) {
                Spacer(Modifier.width(ClipKidTheme.spacing.sm))
                Box(Modifier.alignByBaseline()) { titleTrailing() }
            }
        }
        if (supportingContent != null) {
            Spacer(Modifier.height(ClipKidTheme.spacing.xxs))
            supportingContent()
        }
    }
}

/**
 * The thumbnail part of [VideoCard], reusable by list rows, Up Next and the hero card.
 * Size comes from [modifier].
 *
 * Always draws [YouTubeAttribution] at the top-end corner: every YouTube thumbnail carries the
 * official YouTube Icon (CLAUDE.md rule 6), so no caller can forget it.
 *
 * @param attributionHeight height of the YouTube attribution chip; larger cards may pass more.
 */
@Composable
fun VideoThumbnail(
    thumbnailUrl: String?,
    durationSec: Int?,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    placeholderColor: Color = ClipKidTheme.colors.thumbnailPlaceholder,
    shape: Shape = ClipKidTheme.shapes.cardSmall,
    action: (@Composable () -> Unit)? = null,
    center: (@Composable BoxScope.() -> Unit)? = null,
    attributionHeight: Dp = DefaultYouTubeAttributionHeight,
) {
    val spacing = ClipKidTheme.spacing
    Box(
        modifier = modifier
            .clip(shape)
            .background(placeholderColor),
    ) {
        Icon(
            icon = ClipKidIcons.Image,
            contentDescription = null,
            tint = ClipKidTheme.colors.ink.copy(alpha = 0.25f),
            size = 40.dp,
            modifier = Modifier.align(Alignment.Center),
        )
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        center?.invoke(this)
        YouTubeAttribution(
            height = attributionHeight,
            modifier = Modifier.align(Alignment.TopEnd).padding(spacing.xs),
        )
        if (action != null) {
            Box(Modifier.align(Alignment.TopStart).padding(spacing.xs)) { action() }
        }
        if (durationSec != null) {
            DurationBadge(
                durationSec = durationSec,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = spacing.xs, end = spacing.xs, bottom = spacing.xs + if (progress != null) 6.dp else 0.dp),
            )
        }
        if (progress != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(ClipKidTheme.colors.divider),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .background(ClipKidTheme.colors.accent),
                )
            }
        }
    }
}

/** "3:40" on a dark scrim. */
@Composable
fun DurationBadge(durationSec: Int, modifier: Modifier = Modifier) {
    val text = formatDuration(durationSec)
    val description = stringResource(Res.string.ds_duration, text)
    Box(
        modifier = modifier
            .semantics { contentDescription = description }
            .background(ClipKidTheme.colors.badgeScrim, ClipKidTheme.shapes.badge)
            .padding(horizontal = ClipKidTheme.spacing.xs, vertical = 2.dp),
    ) {
        Text(text = text, style = ClipKidTheme.typography.badge, color = ClipKidTheme.colors.onDark, maxLines = 1)
    }
}

// region Showcase

@Composable
private fun FavoriteHeart() {
    Box(
        Modifier
            .size(ClipKidTheme.sizes.minTouchTarget)
            .clip(ClipKidTheme.shapes.pill)
            .clickable(role = Role.Button, onClick = {}),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size(32.dp).background(ClipKidTheme.colors.surface, ClipKidTheme.shapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                ClipKidIcons.HeartFilled,
                contentDescription = stringResource(Res.string.showcase_remove_favorite),
                tint = ClipKidTheme.colors.danger,
                size = 18.dp,
            )
        }
    }
}

@Composable
fun VideoCardShowcase() {
    ShowcaseColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            VideoCard(
                title = stringResource(Res.string.showcase_video_moon),
                thumbnailUrl = null,
                durationSec = 220,
                onClick = {},
                modifier = Modifier.width(160.dp),
            )
            VideoCard(
                title = stringResource(Res.string.showcase_video_letter_baa),
                thumbnailUrl = null,
                durationSec = 185,
                progress = 0.45f,
                placeholderColor = Color(0xFFD8CFC6),
                onClick = {},
                action = { FavoriteHeart() },
                modifier = Modifier.width(160.dp),
            )
        }
        VideoCard(
            title = stringResource(Res.string.showcase_video_letter_baa),
            thumbnailUrl = null,
            durationSec = 3723,
            progress = 0.7f,
            onClick = {},
            aspectRatio = 2f,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun VideoCardPreview() = ClipKidPreview { VideoCardShowcase() }

// endregion
