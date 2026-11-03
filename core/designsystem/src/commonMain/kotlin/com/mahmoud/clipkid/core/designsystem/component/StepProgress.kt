package com.mahmoud.clipkid.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.preview.ClipKidPreview
import com.mahmoud.clipkid.core.designsystem.preview.ShowcaseColumn
import com.mahmoud.clipkid.core.designsystem.resources.Res
import com.mahmoud.clipkid.core.designsystem.resources.ds_step_of
import com.mahmoud.clipkid.core.designsystem.text.toArabicDigits
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import org.jetbrains.compose.resources.stringResource

/**
 * Onboarding progress: [total] segments, the first [current] filled from the start edge
 * (right in RTL). [current] is 1-based: on step 1 of 3 one segment is filled.
 */
@Composable
fun StepProgress(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier,
) {
    val colors = ClipKidTheme.colors
    val description = stringResource(Res.string.ds_step_of, current.toArabicDigits(), total.toArabicDigits())
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(current.toFloat(), 0f..total.toFloat(), steps = (total - 1).coerceAtLeast(0))
            },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { index ->
            val color by animateColorAsState(
                if (index < current) colors.primary else colors.border,
                label = "stepColor",
            )
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .background(color, ClipKidTheme.shapes.pill),
            )
        }
    }
}

// region Showcase

@Composable
fun StepProgressShowcase() {
    ShowcaseColumn {
        StepProgress(current = 1, total = 3)
        StepProgress(current = 2, total = 3)
        StepProgress(current = 3, total = 3)
    }
}

@Preview
@Composable
private fun StepProgressPreview() = ClipKidPreview { StepProgressShowcase() }

// endregion
