package com.mahmoud.feelo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcons
import com.mahmoud.feelo.core.designsystem.preview.FeeloPreview
import com.mahmoud.feelo.core.designsystem.preview.ShowcaseColumn
import com.mahmoud.feelo.core.designsystem.resources.Res
import com.mahmoud.feelo.core.designsystem.resources.showcase_glyph_letters
import com.mahmoud.feelo.core.designsystem.resources.showcase_letters
import com.mahmoud.feelo.core.designsystem.resources.showcase_manners
import com.mahmoud.feelo.core.designsystem.resources.showcase_numbers
import com.mahmoud.feelo.core.designsystem.resources.showcase_quran
import com.mahmoud.feelo.core.designsystem.resources.showcase_science
import com.mahmoud.feelo.core.designsystem.resources.showcase_stories
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import com.mahmoud.feelo.core.designsystem.theme.CoverColors
import com.mahmoud.feelo.core.designsystem.theme.CoverPalettes
import com.mahmoud.feelo.core.model.CoverMark
import com.mahmoud.feelo.core.model.CoverStyle
import org.jetbrains.compose.resources.stringResource

/**
 * Collection cover: unified color tile with a glyph or an icon — never a thumbnail.
 * The tile is [size] square (84dp default, ≥ 56dp child touch target) with the title under it.
 * The whole cover is exactly [size] wide, so a row of covers is evenly spaced whatever the
 * titles; a title longer than the tile ends with "…".
 */
@Composable
fun CollectionCover(
    style: CoverStyle,
    title: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = FeeloTheme.sizes.collectionCover,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .width(size)
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CoverTile(style = style, size = size)
        if (title != null) {
            Spacer(Modifier.height(FeeloTheme.spacing.xs))
            Text(
                text = title,
                style = FeeloTheme.typography.labelLarge,
                color = FeeloTheme.colors.ink,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The colored tile alone (also used by the Collection header). */
@Composable
fun CoverTile(style: CoverStyle, size: Dp, modifier: Modifier = Modifier) {
    val fg = Color(style.fg)
    Box(
        modifier = modifier
            .size(size)
            .clip(FeeloTheme.shapes.cardSmall)
            .background(Color(style.bg)),
        contentAlignment = Alignment.Center,
    ) {
        when (val mark = style.mark) {
            is CoverMark.Glyph -> Text(
                text = mark.text,
                style = FeeloTheme.typography.displayLarge.merge(
                    TextStyle(fontSize = (size.value * 0.36f).sp, lineHeight = (size.value * 0.5f).sp),
                ),
                color = fg,
                maxLines = 1,
            )

            is CoverMark.Icon -> Icon(
                icon = FeeloIcons.forCoverKey(mark.key),
                contentDescription = null,
                tint = fg,
                size = size * 0.42f,
            )
        }
    }
}

/** Builds a [CoverStyle] from one of the [CoverPalettes]. */
fun CoverColors.toCoverStyle(mark: CoverMark): CoverStyle =
    CoverStyle(bg = bg.toArgbLong(), fg = fg.toArgbLong(), mark = mark)

private fun Color.toArgbLong(): Long {
    fun channel(v: Float) = (v * 255f + 0.5f).toLong()
    return (channel(alpha) shl 24) or (channel(red) shl 16) or (channel(green) shl 8) or channel(blue)
}

// region Showcase

@Composable
fun CollectionCoverShowcase() {
    val covers = listOf(
        CoverPalettes.Letters.toCoverStyle(CoverMark.Glyph(stringResource(Res.string.showcase_glyph_letters))) to
            stringResource(Res.string.showcase_letters),
        CoverPalettes.Numbers.toCoverStyle(CoverMark.Glyph("123")) to
            stringResource(Res.string.showcase_numbers),
        CoverPalettes.QuranAndDuas.toCoverStyle(CoverMark.Icon("book")) to stringResource(Res.string.showcase_quran),
        CoverPalettes.Manners.toCoverStyle(CoverMark.Icon("heart")) to stringResource(Res.string.showcase_manners),
        CoverPalettes.Science.toCoverStyle(CoverMark.Icon("leaf")) to stringResource(Res.string.showcase_science),
        CoverPalettes.Stories.toCoverStyle(CoverMark.Icon("moon")) to stringResource(Res.string.showcase_stories),
    )
    ShowcaseColumn {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(covers.size) { i ->
                val (style, title) = covers[i]
                CollectionCover(style = style, title = title, onClick = {})
            }
        }
    }
}

@Preview
@Composable
private fun CollectionCoverPreview() = FeeloPreview { CollectionCoverShowcase() }

// endregion
