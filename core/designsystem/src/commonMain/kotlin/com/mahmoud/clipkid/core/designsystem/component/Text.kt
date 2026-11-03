package com.mahmoud.clipkid.core.designsystem.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.mahmoud.clipkid.core.designsystem.theme.LocalContentColor
import com.mahmoud.clipkid.core.designsystem.theme.LocalTextStyle

/** Text with ClipKid defaults ([LocalTextStyle], [LocalContentColor]). */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    /** Shrinks the text to fit (e.g. long labels on half-width buttons). */
    autoSize: TextAutoSize? = null,
) {
    val resolvedColor = color.takeOrElse(style.color).takeOrElse(LocalContentColor.current)
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(
            TextStyle(color = resolvedColor, textAlign = textAlign ?: style.textAlign),
        ),
        maxLines = maxLines,
        overflow = overflow,
        softWrap = true,
        autoSize = autoSize,
    )
}

private fun Color.takeOrElse(other: Color): Color = if (this != Color.Unspecified) this else other
