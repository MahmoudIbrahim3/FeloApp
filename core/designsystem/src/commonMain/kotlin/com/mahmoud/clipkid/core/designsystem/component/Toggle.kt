package com.mahmoud.clipkid.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.preview.ClipKidPreview
import com.mahmoud.clipkid.core.designsystem.preview.ShowcaseColumn
import com.mahmoud.clipkid.core.designsystem.resources.Res
import com.mahmoud.clipkid.core.designsystem.resources.showcase_letters
import com.mahmoud.clipkid.core.designsystem.resources.showcase_numbers
import com.mahmoud.clipkid.core.designsystem.resources.showcase_stories
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import org.jetbrains.compose.resources.stringResource

/**
 * On/off switch. Track 52×32dp inside a ≥ 56×48dp touch area.
 * Matches Parent.png: on = thumb on the right with a primary track, off = thumb on the left,
 * regardless of layout direction (the reference screen does not mirror the switch in RTL).
 */
@Composable
fun Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = ClipKidTheme.colors
    val bias by animateFloatAsState(if (checked) 1f else -1f, label = "toggleThumb")
    val track by animateColorAsState(if (checked) colors.primary else colors.border, label = "toggleTrack")
    Box(
        modifier = modifier
            .defaultMinSize(minWidth = ClipKidTheme.sizes.childTouchTarget, minHeight = ClipKidTheme.sizes.minTouchTarget)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange,
            )
            .alpha(if (enabled) 1f else DisabledAlpha),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 52.dp, height = 32.dp)
                .clip(ClipKidTheme.shapes.pill)
                .background(track)
                .padding(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(BiasAbsoluteAlignment(horizontalBias = bias, verticalBias = 0f))
                    .size(24.dp)
                    .shadow(1.dp, ClipKidTheme.shapes.pill)
                    .background(colors.surface, ClipKidTheme.shapes.pill),
            )
        }
    }
}

// region Showcase

@Composable
fun ToggleShowcase() {
    val labels = listOf(
        stringResource(Res.string.showcase_letters),
        stringResource(Res.string.showcase_numbers),
        stringResource(Res.string.showcase_stories),
    )
    val states = remember { mutableStateOf(listOf(true, true, false)) }
    ShowcaseColumn {
        Column(
            Modifier
                .fillMaxWidth()
                .background(ClipKidTheme.colors.surface, ClipKidTheme.shapes.card)
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            labels.forEachIndexed { index, label ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, Modifier.weight(1f), style = ClipKidTheme.typography.bodyLarge)
                    Toggle(
                        checked = states.value[index],
                        onCheckedChange = { on -> states.value = states.value.toMutableList().also { it[index] = on } },
                    )
                }
            }
        }
        Row {
            Toggle(checked = true, onCheckedChange = {}, enabled = false)
            Toggle(checked = false, onCheckedChange = {}, enabled = false)
        }
    }
}

@Preview
@Composable
private fun TogglePreview() = ClipKidPreview { ToggleShowcase() }

// endregion
