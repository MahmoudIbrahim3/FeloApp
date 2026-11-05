package com.mahmoud.clipkid.gallery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.component.ChipShowcase
import com.mahmoud.clipkid.core.designsystem.component.CollectionCoverShowcase
import com.mahmoud.clipkid.core.designsystem.component.ColorTokensShowcase
import com.mahmoud.clipkid.core.designsystem.component.EmptyStateShowcase
import com.mahmoud.clipkid.core.designsystem.component.IconButtonLargeShowcase
import com.mahmoud.clipkid.core.designsystem.component.PinPadShowcase
import com.mahmoud.clipkid.core.designsystem.component.PlanCardShowcase
import com.mahmoud.clipkid.core.designsystem.component.PrimaryButtonShowcase
import com.mahmoud.clipkid.core.designsystem.component.QuotaDotsShowcase
import com.mahmoud.clipkid.core.designsystem.component.SecondaryButtonShowcase
import com.mahmoud.clipkid.core.designsystem.component.SegmentedControlShowcase
import com.mahmoud.clipkid.core.designsystem.component.ShelfShowcase
import com.mahmoud.clipkid.core.designsystem.component.StepProgressShowcase
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.component.ToggleShowcase
import com.mahmoud.clipkid.core.designsystem.component.TypographyShowcase
import com.mahmoud.clipkid.core.designsystem.component.VideoCardShowcase
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.shared.resources.Res
import com.mahmoud.clipkid.shared.resources.gallery_chip
import com.mahmoud.clipkid.shared.resources.gallery_collection_cover
import com.mahmoud.clipkid.shared.resources.gallery_colors
import com.mahmoud.clipkid.shared.resources.gallery_empty_state
import com.mahmoud.clipkid.shared.resources.gallery_icon_button
import com.mahmoud.clipkid.shared.resources.gallery_pin_pad
import com.mahmoud.clipkid.shared.resources.gallery_plan_card
import com.mahmoud.clipkid.shared.resources.gallery_primary_button
import com.mahmoud.clipkid.shared.resources.gallery_quota_dots
import com.mahmoud.clipkid.shared.resources.gallery_secondary_button
import com.mahmoud.clipkid.shared.resources.gallery_segmented
import com.mahmoud.clipkid.shared.resources.gallery_shelf
import com.mahmoud.clipkid.shared.resources.gallery_step_progress
import com.mahmoud.clipkid.shared.resources.gallery_subtitle
import com.mahmoud.clipkid.shared.resources.gallery_title
import com.mahmoud.clipkid.shared.resources.gallery_toggle
import com.mahmoud.clipkid.shared.resources.gallery_typography
import com.mahmoud.clipkid.shared.resources.gallery_video_card
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * TEMPORARY Phase 0 start destination: every design-system component, for review on
 * Android and iOS. Remove once onboarding becomes the start destination.
 */
@Composable
fun ComponentGallery(modifier: Modifier = Modifier) {
    val insets = WindowInsets.safeDrawing.asPaddingValues()
    val direction = LocalLayoutDirection.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = insets.calculateTopPadding() + 16.dp,
            bottom = insets.calculateBottomPadding() + 32.dp,
            start = insets.calculateStartPadding(direction),
            end = insets.calculateEndPadding(direction),
        ),
    ) {
        item {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    stringResource(Res.string.gallery_title),
                    style = ClipKidTheme.typography.displayMedium,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    stringResource(Res.string.gallery_subtitle),
                    style = ClipKidTheme.typography.bodyMedium,
                    color = ClipKidTheme.colors.muted,
                )
            }
        }
        section(Res.string.gallery_colors) { ColorTokensShowcase() }
        section(Res.string.gallery_typography) { TypographyShowcase() }
        section(Res.string.gallery_primary_button) { PrimaryButtonShowcase() }
        section(Res.string.gallery_secondary_button) { SecondaryButtonShowcase() }
        section(Res.string.gallery_icon_button) { IconButtonLargeShowcase() }
        section(Res.string.gallery_chip) { ChipShowcase() }
        section(Res.string.gallery_segmented) { SegmentedControlShowcase() }
        section(Res.string.gallery_toggle) { ToggleShowcase() }
        section(Res.string.gallery_video_card) { VideoCardShowcase() }
        section(Res.string.gallery_collection_cover) { CollectionCoverShowcase() }
        section(Res.string.gallery_plan_card) { PlanCardShowcase() }
        section(Res.string.gallery_shelf, edgeToEdge = true) { ShelfShowcase() }
        section(Res.string.gallery_quota_dots) { QuotaDotsShowcase() }
        section(Res.string.gallery_pin_pad) { PinPadShowcase() }
        section(Res.string.gallery_step_progress) { StepProgressShowcase() }
        section(Res.string.gallery_empty_state) { EmptyStateShowcase() }
    }
}

private fun LazyListScope.section(
    title: StringResource,
    edgeToEdge: Boolean = false,
    content: @Composable () -> Unit,
) {
    item(key = title.key) {
        Column(Modifier.fillMaxWidth().padding(top = 28.dp)) {
            Text(
                text = stringResource(title),
                style = ClipKidTheme.typography.labelMedium,
                color = ClipKidTheme.colors.muted,
                modifier = Modifier.padding(horizontal = 20.dp).semantics { heading() },
            )
            Spacer(Modifier.height(10.dp))
            Column(if (edgeToEdge) Modifier else Modifier.padding(horizontal = 20.dp)) {
                content()
            }
        }
    }
}
