package com.mahmoud.feelo.core.designsystem.component

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
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcon
import com.mahmoud.feelo.core.designsystem.preview.FeeloPreview
import com.mahmoud.feelo.core.designsystem.preview.ShowcaseColumn
import com.mahmoud.feelo.core.designsystem.resources.Res
import com.mahmoud.feelo.core.designsystem.resources.showcase_age_range
import com.mahmoud.feelo.core.designsystem.resources.showcase_age_range_years
import com.mahmoud.feelo.core.designsystem.resources.showcase_child_1
import com.mahmoud.feelo.core.designsystem.resources.showcase_child_2
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
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
    leadingIcon: FeeloIcon? = null,
    enabled: Boolean = true,
) {
    val colors = FeeloTheme.colors
    val shape = FeeloTheme.shapes.pill
    val background by animateColorAsState(if (selected) colors.ink else colors.surface, label = "chipBg")
    val content by animateColorAsState(if (selected) colors.onPrimary else colors.ink, label = "chipFg")
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = FeeloTheme.sizes.minTouchTarget, minWidth = FeeloTheme.sizes.minTouchTarget)
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
            .padding(horizontal = FeeloTheme.spacing.lg, vertical = FeeloTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leadingIcon != null) {
            Icon(leadingIcon, contentDescription = null, tint = content, size = 20.dp)
            Spacer(Modifier.width(FeeloTheme.spacing.xs))
        }
        Text(text = text, style = FeeloTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

// region Showcase

@Composable
fun ChipShowcase() {
    var child by remember { mutableIntStateOf(0) }
    var age by remember { mutableIntStateOf(1) }
    val ages = listOf(
        stringResource(Res.string.showcase_age_range, 3.toString(), 5.toString()),
        stringResource(Res.string.showcase_age_range, 6.toString(), 8.toString()),
        stringResource(Res.string.showcase_age_range_years, 9.toString(), 12.toString()),
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
private fun ChipPreview() = FeeloPreview { ChipShowcase() }

// endregion
