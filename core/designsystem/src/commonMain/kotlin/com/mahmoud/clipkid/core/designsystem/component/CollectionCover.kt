package com.mahmoud.clipkid.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.preview.ClipKidPreview
import com.mahmoud.clipkid.core.designsystem.preview.ShowcaseColumn
import com.mahmoud.clipkid.core.designsystem.resources.Res
import com.mahmoud.clipkid.core.designsystem.resources.ds_locked
import com.mahmoud.clipkid.core.designsystem.resources.showcase_glyph_letters
import com.mahmoud.clipkid.core.designsystem.resources.showcase_letters
import com.mahmoud.clipkid.core.designsystem.resources.showcase_manners
import com.mahmoud.clipkid.core.designsystem.resources.showcase_numbers
import com.mahmoud.clipkid.core.designsystem.resources.showcase_quran
import com.mahmoud.clipkid.core.designsystem.resources.showcase_science
import com.mahmoud.clipkid.core.designsystem.resources.showcase_stories
import com.mahmoud.clipkid.core.designsystem.text.withArabicDigits
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.core.designsystem.theme.CoverColors
import com.mahmoud.clipkid.core.designsystem.theme.CoverPalettes
import com.mahmoud.clipkid.core.model.CoverMark
import com.mahmoud.clipkid.core.model.CoverStyle
import org.jetbrains.compose.resources.stringResource

/**
 * Collection cover: unified color tile with a glyph or an icon — never a thumbnail.
 * The tile is [size] square (84dp default, ≥ 56dp child touch target) with the title under it.
 * [locked]: a premium collection without the subscription; a lock badge sits on the tile.
 */
@Composable
fun CollectionCover(
    style: CoverStyle,
    title: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = ClipKidTheme.sizes.collectionCover,
    locked: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val lockedDescription = title?.let { stringResource(Res.string.ds_locked, it) }
    Column(
        modifier = modifier
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .then(
                if (locked && lockedDescription != null) {
                    Modifier.semantics(mergeDescendants = true) { contentDescription = lockedDescription }
                } else {
                    Modifier
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            CoverTile(style = style, size = size)
            if (locked) LockBadge(Modifier.align(Alignment.TopEnd).padding(ClipKidTheme.spacing.xxs))
        }
        if (title != null) {
            Spacer(Modifier.height(ClipKidTheme.spacing.xs))
            Text(text = title, style = ClipKidTheme.typography.labelLarge, color = ClipKidTheme.colors.ink, maxLines = 1)
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
            .clip(ClipKidTheme.shapes.cardSmall)
            .background(Color(style.bg)),
        contentAlignment = Alignment.Center,
    ) {
        when (val mark = style.mark) {
            is CoverMark.Glyph -> Text(
                text = mark.text,
                style = ClipKidTheme.typography.displayLarge.merge(
                    TextStyle(fontSize = (size.value * 0.36f).sp, lineHeight = (size.value * 0.5f).sp),
                ),
                color = fg,
                maxLines = 1,
            )

            is CoverMark.Icon -> Icon(
                icon = ClipKidIcons.forCoverKey(mark.key),
                contentDescription = null,
                tint = fg,
                size = size * 0.42f,
            )
        }
    }
}

/** White circle with a lock: a premium collection that needs the subscription. */
@Composable
fun LockBadge(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val colors = ClipKidTheme.colors
    Box(
        modifier
            .size(size)
            .softShadow(ClipKidTheme.shapes.pill)
            .clip(ClipKidTheme.shapes.pill)
            .background(colors.surface),
        contentAlignment = Alignment.Center,
    ) {
        Icon(ClipKidIcons.Lock, contentDescription = null, tint = colors.ink, size = size * 0.55f)
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
        CoverPalettes.Numbers.toCoverStyle(CoverMark.Glyph("123".withArabicDigits())) to
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
                CollectionCover(style = style, title = title, onClick = {}, locked = i == covers.lastIndex)
            }
        }
    }
}

@Preview
@Composable
private fun CollectionCoverPreview() = ClipKidPreview { CollectionCoverShowcase() }

// endregion
