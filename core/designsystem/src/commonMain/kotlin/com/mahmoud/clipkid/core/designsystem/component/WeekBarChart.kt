package com.mahmoud.clipkid.core.designsystem.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.preview.ClipKidPreview
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme

/**
 * Seven-day bar chart drawn with Canvas (Activity.png). Bars run from the start edge (oldest,
 * right in RTL) to the end edge (the last bar, highlighted). A dashed line marks [limit], with
 * [limitLabel] at the end side above it.
 *
 * The chart is one semantics node: pass a [contentDescription] that reads the numbers out.
 */
@Composable
fun WeekBarChart(
    values: List<Int>,
    labels: List<String>,
    limit: Int,
    limitLabel: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    chartHeight: Dp = 240.dp,
) {
    require(values.size == labels.size) { "one label per bar" }
    val colors = ClipKidTheme.colors
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val top = (maxOf(values.maxOrNull() ?: 0, limit, 1) * 1.15f)

    Column(modifier.clearAndSetSemantics { this.contentDescription = contentDescription }) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(chartHeight)) {
            val limitY = maxHeight * (1f - limit / top)
            Canvas(Modifier.fillMaxSize()) {
                val n = values.size.coerceAtLeast(1)
                val gap = 14.dp.toPx()
                val barWidth = (size.width - gap * (n - 1)) / n
                val radius = CornerRadius(14.dp.toPx())
                values.forEachIndexed { index, value ->
                    val slot = if (rtl) n - 1 - index else index
                    val x = slot * (barWidth + gap)
                    val h = (size.height * (value / top)).coerceAtLeast(if (value > 0) radius.y * 2 else 4.dp.toPx())
                    drawRoundRect(
                        color = if (index == values.lastIndex) colors.primary else colors.primary.copy(alpha = 0.3f),
                        topLeft = Offset(x, size.height - h),
                        size = Size(barWidth, h),
                        cornerRadius = radius,
                    )
                }
                if (limit > 0) {
                    val y = size.height * (1f - limit / top)
                    // Leave room for the label at the end side.
                    val labelRoom = 110.dp.toPx()
                    val start = if (rtl) labelRoom else 0f
                    val end = if (rtl) size.width else size.width - labelRoom
                    drawLine(
                        color = colors.danger,
                        start = Offset(start, y),
                        end = Offset(end, y),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                    )
                }
            }
            if (limit > 0) {
                Box(Modifier.fillMaxWidth().offset(y = limitY - 12.dp), contentAlignment = Alignment.CenterEnd) {
                    Text(text = limitLabel, style = ClipKidTheme.typography.labelMedium, color = colors.danger)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = ClipKidTheme.spacing.sm)) {
            labels.forEachIndexed { index, label ->
                Text(
                    text = label,
                    style = if (index == labels.lastIndex) ClipKidTheme.typography.titleSmall else ClipKidTheme.typography.bodyMedium,
                    color = if (index == labels.lastIndex) colors.ink else colors.muted,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview
@Composable
private fun WeekBarChartPreview() = ClipKidPreview {
    WeekBarChart(
        values = listOf(24, 30, 18, 30, 26, 14, 18),
        labels = (1..7).map { it.toString() },
        limit = 30,
        limitLabel = 30.toString(),
        contentDescription = "",
    )
}
