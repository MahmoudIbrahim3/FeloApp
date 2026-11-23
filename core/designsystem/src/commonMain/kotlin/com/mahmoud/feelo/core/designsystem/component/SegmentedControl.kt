package com.mahmoud.feelo.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import com.mahmoud.feelo.core.designsystem.preview.FeeloPreview
import com.mahmoud.feelo.core.designsystem.preview.ShowcaseColumn
import com.mahmoud.feelo.core.designsystem.resources.Res
import com.mahmoud.feelo.core.designsystem.resources.showcase_by_time
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Single choice among a few options (daily limit: 3 / 5 / 10 / بالوقت).
 * 56dp tall track; each segment is 48dp tall and fills an equal share of the width.
 * The first option sits at the start edge (right in RTL).
 */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = FeeloTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FeeloTheme.sizes.buttonHeight)
            .clip(FeeloTheme.shapes.button)
            .background(colors.divider)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val background by animateColorAsState(
                if (selected) colors.primary else colors.divider,
                label = "segmentBg",
            )
            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(FeeloTheme.shapes.segment)
                    .background(background)
                    .selectable(
                        selected = selected,
                        enabled = enabled,
                        role = Role.RadioButton,
                        interactionSource = interaction,
                        indication = LocalIndication.current,
                        onClick = { onSelect(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = if (selected) FeeloTheme.typography.titleMedium else FeeloTheme.typography.bodyLarge,
                    color = if (selected) colors.onPrimary else colors.ink,
                    maxLines = 1,
                )
            }
        }
    }
}

// region Showcase

@Composable
fun SegmentedControlShowcase() {
    var selected by remember { mutableIntStateOf(0) }
    val options = listOf(3.toString(), 5.toString(), 10.toString(), stringResource(Res.string.showcase_by_time))
    ShowcaseColumn {
        SegmentedControl(options = options, selectedIndex = selected, onSelect = { selected = it })
    }
}

@Preview
@Composable
private fun SegmentedControlPreview() = FeeloPreview { SegmentedControlShowcase() }

// endregion
