package com.mahmoud.feelo.feature.child.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mahmoud.feelo.core.designsystem.component.VideoCard
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import com.mahmoud.feelo.core.model.Video

/**
 * The child-mode video card (YouTube Kids style): full-width 16:9 thumbnail with YouTube
 * attribution and duration, the title under it, and optional short content on the title's line.
 * Used by Home and Collection so both screens look the same.
 */
@Composable
internal fun WideVideoCard(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    progress: Float? = null,
    titleTrailing: (@Composable () -> Unit)? = null,
) {
    VideoCard(
        title = video.titleAr,
        thumbnailUrl = video.thumbnailUrl,
        durationSec = video.durationSec,
        onClick = onClick,
        progress = progress,
        aspectRatio = 16f / 9f,
        thumbnailShape = FeeloTheme.shapes.thumbnail,
        titleTrailing = titleTrailing,
        modifier = modifier.fillMaxWidth(),
    )
}
