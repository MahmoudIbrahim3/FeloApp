package com.mahmoud.clipkid.core.designsystem.component

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.preview.ClipKidPreview
import com.mahmoud.clipkid.core.designsystem.preview.DarkShowcasePanel
import com.mahmoud.clipkid.core.designsystem.preview.ShowcaseColumn
import com.mahmoud.clipkid.core.designsystem.resources.Res
import com.mahmoud.clipkid.core.designsystem.resources.ds_backspace
import com.mahmoud.clipkid.core.designsystem.resources.ds_pin_progress
import com.mahmoud.clipkid.core.designsystem.text.toArabicDigits
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.core.designsystem.theme.SurfaceTone
import org.jetbrains.compose.resources.stringResource

/**
 * Numeric keypad for setting and entering the parent PIN.
 *
 * Always laid out left-to-right (1 2 3 on top, 0 in the middle of the last row,
 * backspace on the right), as phone keypads are, even inside the RTL app.
 * Keys are 64dp tall; digits use Arabic-Indic numerals.
 */
@Composable
fun PinPad(
    onDigit: (Int) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    tone: SurfaceTone = SurfaceTone.Light,
    enabled: Boolean = true,
) {
    val spacing = ClipKidTheme.spacing.sm
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
            listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                    row.forEach { digit -> DigitKey(digit, tone, enabled, onDigit) }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(spacing)) {
                Spacer(Modifier.weight(1f))
                DigitKey(0, tone, enabled, onDigit)
                BackspaceKey(tone, enabled, onBackspace)
            }
        }
    }
}

@Composable
private fun RowScope.DigitKey(digit: Int, tone: SurfaceTone, enabled: Boolean, onDigit: (Int) -> Unit) {
    val colors = ClipKidTheme.colors
    val shape = ClipKidTheme.shapes.button
    val interaction = remember { MutableInteractionSource() }
    val label = digit.toArabicDigits()
    Box(
        modifier = Modifier
            .weight(1f)
            .height(ClipKidTheme.sizes.pinKeyHeight)
            .pressScale(interaction)
            .then(if (tone == SurfaceTone.Light) Modifier.softShadow(shape) else Modifier)
            .clip(shape)
            .background(if (tone == SurfaceTone.Light) colors.surface else colors.darkSurfaceHigh)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = { onDigit(digit) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = ClipKidTheme.typography.numeral,
            color = if (tone == SurfaceTone.Light) colors.ink else colors.onDark,
        )
    }
}

@Composable
private fun RowScope.BackspaceKey(tone: SurfaceTone, enabled: Boolean, onBackspace: () -> Unit) {
    val colors = ClipKidTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .weight(1f)
            .height(ClipKidTheme.sizes.pinKeyHeight)
            .clip(ClipKidTheme.shapes.button)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onBackspace,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon = ClipKidIcons.Backspace,
            contentDescription = stringResource(Res.string.ds_backspace),
            tint = if (tone == SurfaceTone.Light) colors.ink else colors.onDark,
            size = 28.dp,
        )
    }
}

/** PIN entry progress: [filled] of [length] dots, filled left to right. */
@Composable
fun PinDots(
    filled: Int,
    modifier: Modifier = Modifier,
    length: Int = 4,
    tone: SurfaceTone = SurfaceTone.Light,
    dotSize: Dp = 20.dp,
) {
    val colors = ClipKidTheme.colors
    val fill = if (tone == SurfaceTone.Light) colors.ink else colors.onDark
    val ring = if (tone == SurfaceTone.Light) colors.muted.copy(alpha = 0.45f) else colors.onDark.copy(alpha = 0.35f)
    val description = stringResource(Res.string.ds_pin_progress, filled.toArabicDigits(), length.toArabicDigits())
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = modifier.clearAndSetSemantics { contentDescription = description },
            horizontalArrangement = Arrangement.spacedBy(dotSize * 0.9f),
        ) {
            repeat(length) { index ->
                Box(
                    Modifier
                        .size(dotSize)
                        .background(if (index < filled) fill else Color.Transparent, ClipKidTheme.shapes.pill)
                        .then(if (index < filled) Modifier else Modifier.border(2.dp, ring, ClipKidTheme.shapes.pill)),
                )
            }
        }
    }
}

// region Showcase

@Composable
fun PinPadShowcase() {
    var pin by remember { mutableStateOf("") }
    var gatePin by remember { mutableStateOf("123") }
    ShowcaseColumn(horizontalAlignmentCenter = true) {
        PinDots(filled = pin.length)
        PinPad(
            onDigit = { if (pin.length < 4) pin += it },
            onBackspace = { pin = pin.dropLast(1) },
        )
        DarkShowcasePanel {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PinDots(filled = gatePin.length, tone = SurfaceTone.Dark)
            }
            PinPad(
                onDigit = { if (gatePin.length < 4) gatePin += it },
                onBackspace = { gatePin = gatePin.dropLast(1) },
                tone = SurfaceTone.Dark,
            )
        }
    }
}

@Preview
@Composable
private fun PinPadPreview() = ClipKidPreview { PinPadShowcase() }

// endregion
