package com.mahmoud.clipkid.core.designsystem.component

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
import com.mahmoud.clipkid.core.designsystem.preview.ClipKidPreview
import com.mahmoud.clipkid.core.designsystem.preview.ShowcaseColumn
import com.mahmoud.clipkid.core.designsystem.resources.Res
import com.mahmoud.clipkid.core.designsystem.resources.showcase_by_time
import com.mahmoud.clipkid.core.designsystem.text.toArabicDigits
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Single choice among a few options (daily limit: ٣ / ٥ / ١٠ / بالوقت).
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
    val colors = ClipKidTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(ClipKidTheme.sizes.buttonHeight)
            .clip(ClipKidTheme.shapes.button)
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
                    .clip(ClipKidTheme.shapes.segment)
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
                    style = if (selected) ClipKidTheme.typography.titleMedium else ClipKidTheme.typography.bodyLarge,
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
    val options = listOf(3.toArabicDigits(), 5.toArabicDigits(), 10.toArabicDigits(), stringResource(Res.string.showcase_by_time))
    ShowcaseColumn {
        SegmentedControl(options = options, selectedIndex = selected, onSelect = { selected = it })
    }
}

@Preview
@Composable
private fun SegmentedControlPreview() = ClipKidPreview { SegmentedControlShowcase() }

// endregion
