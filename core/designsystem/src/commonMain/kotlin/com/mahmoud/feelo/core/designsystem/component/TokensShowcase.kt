package com.mahmoud.feelo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.feelo.core.designsystem.preview.FeeloPreview
import com.mahmoud.feelo.core.designsystem.preview.ShowcaseColumn
import com.mahmoud.feelo.core.designsystem.resources.Res
import com.mahmoud.feelo.core.designsystem.resources.showcase_type_sample
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import com.mahmoud.feelo.core.designsystem.theme.CoverPalettes
import org.jetbrains.compose.resources.stringResource

/** Every color token as a swatch: base palette, dark palette, then cover pairs. */
@Composable
fun ColorTokensShowcase() {
    val c = FeeloTheme.colors
    val base = listOf(
        c.ink, c.muted, c.background, c.surface, c.primary, c.primaryDark, c.accent,
        c.border, c.divider, c.success, c.danger, c.celebrate,
    )
    val dark = listOf(c.darkBackground, c.darkBackgroundAlt, c.darkSurface, c.darkSurfaceHigh)
    ShowcaseColumn {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (base + dark).forEach { color ->
                Box(
                    Modifier
                        .size(40.dp)
                        .background(color, FeeloTheme.shapes.badge)
                        .border(1.dp, c.divider, FeeloTheme.shapes.badge),
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CoverPalettes.all.forEach { pair ->
                Box(Modifier.size(40.dp).background(pair.bg, FeeloTheme.shapes.badge)) {
                    Box(Modifier.size(16.dp).background(pair.fg, FeeloTheme.shapes.pill))
                }
            }
        }
    }
}

/** Every text style, largest first, with the same Arabic sample (includes Arabic-Indic digits). */
@Composable
fun TypographyShowcase() {
    val t = FeeloTheme.typography
    val sample = stringResource(Res.string.showcase_type_sample)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(
            t.displayLarge, t.displayMedium, t.titleLarge, t.titleMedium, t.titleSmall,
            t.button, t.bodyLarge, t.bodyMedium, t.bodySmall, t.labelLarge, t.labelMedium, t.labelSmall,
        ).forEach { style ->
            Text(text = sample, style = style, maxLines = 1)
        }
    }
}

@Preview
@Composable
private fun ColorTokensPreview() = FeeloPreview { ColorTokensShowcase() }

@Preview
@Composable
private fun TypographyPreview() = FeeloPreview { TypographyShowcase() }
