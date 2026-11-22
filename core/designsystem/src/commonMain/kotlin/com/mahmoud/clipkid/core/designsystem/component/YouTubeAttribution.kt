package com.mahmoud.clipkid.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.resources.Res
import com.mahmoud.clipkid.core.designsystem.resources.ds_youtube_attribution
import com.mahmoud.clipkid.core.designsystem.resources.yt_icon_red
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Width / height of the official YouTube Icon artboard (602.187 × 515.868), clear space included. */
private const val ARTBOARD_RATIO = 602.187f / 515.868f

/**
 * YouTube attribution with the official YouTube Icon (CLAUDE.md rule 6).
 *
 * Branding Guidelines: the icon labels individual items, keeps its colours and shape, and sits on
 * "a single, solid background color". So the red icon is drawn as-is (an [Image], never a tinted
 * [Icon]) on a solid white chip the size of the official artboard, whose margin is YouTube's
 * own clear space. It is not clickable by itself: it sits inside the card's tap target, which
 * opens the video in our YouTube player.
 *
 * @param height chip height. The red mark is about 54% of it. Check YouTube's minimum size on
 *   brand.youtube before lowering it.
 */
@Composable
fun YouTubeAttribution(
    modifier: Modifier = Modifier,
    height: Dp = DefaultYouTubeAttributionHeight,
) {
    Image(
        painter = painterResource(Res.drawable.yt_icon_red),
        contentDescription = stringResource(Res.string.ds_youtube_attribution),
        contentScale = ContentScale.Fit,
        modifier = modifier
            .height(height)
            .aspectRatio(ARTBOARD_RATIO)
            .background(ClipKidTheme.colors.surface, ClipKidTheme.shapes.badge),
    )
}

val DefaultYouTubeAttributionHeight: Dp = 28.dp
