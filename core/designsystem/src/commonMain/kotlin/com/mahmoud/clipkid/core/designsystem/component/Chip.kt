package com.mahmoud.clipkid.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcon
import com.mahmoud.clipkid.core.designsystem.preview.ClipKidPreview
import com.mahmoud.clipkid.core.designsystem.preview.ShowcaseColumn
import com.mahmoud.clipkid.core.designsystem.resources.Res
import com.mahmoud.clipkid.core.designsystem.resources.showcase_age_range
import com.mahmoud.clipkid.core.designsystem.resources.showcase_age_range_years
import com.mahmoud.clipkid.core.designsystem.resources.showcase_child_1
import com.mahmoud.clipkid.core.designsystem.resources.showcase_child_2
import com.mahmoud.clipkid.core.designsystem.text.toArabicDigits
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Fully rounded selectable chip (child switcher, age band picker).
 * Selected = ink fill; unselected = white with a border. Min height 48dp.
 */
@Composable
fun Chip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ClipKidIcon? = null,
    enabled: Boolean = true,
) {
    val colors = ClipKidTheme.colors
    val shape = ClipKidTheme.shapes.pill
    val background by animateColorAsState(if (selected) colors.ink else colors.surface, label = "chipBg")
    val content by animateColorAsState(if (selected) colors.onPrimary else colors.ink, label = "chipFg")
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = ClipKidTheme.sizes.minTouchTarget, minWidth = ClipKidTheme.sizes.minTouchTarget)
            .pressScale(interaction)
            .clip(shape)
            .background(background)
            .then(if (selected) Modifier else Modifier.border(1.5.dp, colors.border, shape))
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.Tab,
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = ClipKidTheme.spacing.lg, vertical = ClipKidTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = content, size = 20.dp)
            Spacer(Modifier.width(ClipKidTheme.spacing.xs))
        }
        Text(text = text, style = ClipKidTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

// region Showcase

@Composable
fun ChipShowcase() {
    var child by remember { mutableIntStateOf(0) }
    var age by remember { mutableIntStateOf(1) }
    val ages = listOf(
        stringResource(Res.string.showcase_age_range, 3.toArabicDigits(), 5.toArabicDigits()),
        stringResource(Res.string.showcase_age_range, 6.toArabicDigits(), 8.toArabicDigits()),
        stringResource(Res.string.showcase_age_range_years, 9.toArabicDigits(), 12.toArabicDigits()),
    )
    ShowcaseColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(stringResource(Res.string.showcase_child_1), selected = child == 0, onClick = { child = 0 })
            Chip(stringResource(Res.string.showcase_child_2), selected = child == 1, onClick = { child = 1 })
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ages.forEachIndexed { index, label ->
                Chip(label, selected = age == index, onClick = { age = index })
            }
        }
    }
}

@Preview
@Composable
private fun ChipPreview() = ClipKidPreview { ChipShowcase() }

// endregion
