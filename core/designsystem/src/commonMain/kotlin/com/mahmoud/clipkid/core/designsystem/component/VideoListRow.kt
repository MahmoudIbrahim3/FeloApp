package com.mahmoud.clipkid.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme

/**
 * Collection list row: text on the start side, thumbnail on the end side (RTL: right and left).
 * [selected] draws the primary outline used for the video in progress.
 *
 * @param youtubeBadge YouTube attribution slot (CLAUDE.md rule 6), same as [VideoCard].
 */
@Composable
fun VideoListRow(
    title: String,
    thumbnailUrl: String?,
    durationSec: Int,
    status: String,
    statusColor: Color,
    onClick: () -> Unit,
    youtubeBadge: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    selected: Boolean = false,
) {
    val colors = ClipKidTheme.colors
    val spacing = ClipKidTheme.spacing
    val shape = ClipKidTheme.shapes.cardSmall
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .background(colors.surface, shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.divider, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = ClipKidTheme.typography.titleSmall,
                color = colors.ink,
                maxLines = 2,
            )
            Spacer(Modifier.height(spacing.xxs))
            Text(
                text = status,
                style = ClipKidTheme.typography.labelMedium,
                color = statusColor,
            )
        }
        Spacer(Modifier.width(spacing.md))
        VideoThumbnail(
            thumbnailUrl = thumbnailUrl,
            durationSec = durationSec,
            youtubeBadge = youtubeBadge,
            progress = progress,
            modifier = Modifier.width(112.dp).aspectRatio(16f / 10f),
        )
    }
}
