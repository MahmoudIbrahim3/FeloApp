package com.mahmoud.feelo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mahmoud.feelo.core.designsystem.preview.FeeloPreview
import com.mahmoud.feelo.core.designsystem.preview.DarkShowcasePanel
import com.mahmoud.feelo.core.designsystem.preview.ShowcaseColumn
import com.mahmoud.feelo.core.designsystem.resources.Res
import com.mahmoud.feelo.core.designsystem.resources.ds_quota_of
import com.mahmoud.feelo.core.designsystem.resources.showcase_one_left
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import com.mahmoud.feelo.core.designsystem.theme.SurfaceTone
import org.jetbrains.compose.resources.stringResource

/**
 * Daily quota as dots: [used] filled (accent) from the start edge, the rest empty.
 * [outlined] adds an ink ring (DoneToday celebration).
 */
@Composable
fun QuotaDots(
    used: Int,
    total: Int,
    modifier: Modifier = Modifier,
    dotSize: Dp = 12.dp,
    tone: SurfaceTone = SurfaceTone.Light,
    outlined: Boolean = false,
) {
    val colors = FeeloTheme.colors
    val empty = when (tone) {
        SurfaceTone.Light -> colors.border
        SurfaceTone.Dark -> colors.darkSurfaceHigh
    }
    val description = stringResource(Res.string.ds_quota_of, used.toString(), total.toString())
    Row(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(dotSize * 0.5f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total.coerceAtLeast(0)) { index ->
            val filled = index < used
            Box(
                Modifier
                    .size(dotSize)
                    .background(if (filled) colors.accent else empty, FeeloTheme.shapes.pill)
                    .then(
                        if (outlined) Modifier.border(dotSize / 10f, colors.ink, FeeloTheme.shapes.pill) else Modifier,
                    ),
            )
        }
    }
}

/**
 * Quota pill: dots on the start side, then a label ("2 من 3" by default, or e.g. "فاضلك فيديو واحد").
 * Informational only (not clickable).
 */
@Composable
fun QuotaPill(
    used: Int,
    total: Int,
    modifier: Modifier = Modifier,
    label: String = stringResource(Res.string.ds_quota_of, used.toString(), total.toString()),
    tone: SurfaceTone = SurfaceTone.Light,
) {
    val colors = FeeloTheme.colors
    val shape = FeeloTheme.shapes.pill
    Row(
        modifier = modifier
            .then(if (tone == SurfaceTone.Light) Modifier.softShadow(shape) else Modifier)
            .background(if (tone == SurfaceTone.Light) colors.surface else colors.darkSurface, shape)
            .heightIn(min = 40.dp)
            .padding(horizontal = FeeloTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QuotaDots(used = used, total = total, tone = tone)
        Spacer(Modifier.width(FeeloTheme.spacing.xs))
        Text(
            text = label,
            style = FeeloTheme.typography.titleSmall,
            color = if (tone == SurfaceTone.Light) colors.ink else colors.onDark,
            maxLines = 1,
        )
    }
}

// region Showcase

@Composable
fun QuotaDotsShowcase() {
    ShowcaseColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            QuotaPill(used = 2, total = 3)
            QuotaDots(used = 1, total = 5)
        }
        QuotaDots(used = 3, total = 3, dotSize = 36.dp, outlined = true)
        DarkShowcasePanel {
            QuotaPill(used = 2, total = 3, label = stringResource(Res.string.showcase_one_left), tone = SurfaceTone.Dark)
        }
    }
}

@Preview
@Composable
private fun QuotaDotsPreview() = FeeloPreview { QuotaDotsShowcase() }

// endregion
