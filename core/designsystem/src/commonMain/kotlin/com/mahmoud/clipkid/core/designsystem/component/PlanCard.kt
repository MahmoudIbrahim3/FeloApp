package com.mahmoud.clipkid.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.preview.ClipKidPreview
import com.mahmoud.clipkid.core.designsystem.preview.ShowcaseColumn
import com.mahmoud.clipkid.core.designsystem.resources.Res
import com.mahmoud.clipkid.core.designsystem.resources.showcase_plan_annual
import com.mahmoud.clipkid.core.designsystem.resources.showcase_plan_annual_detail
import com.mahmoud.clipkid.core.designsystem.resources.showcase_plan_annual_price
import com.mahmoud.clipkid.core.designsystem.resources.showcase_plan_badge
import com.mahmoud.clipkid.core.designsystem.resources.showcase_plan_monthly
import com.mahmoud.clipkid.core.designsystem.resources.showcase_plan_monthly_price
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import org.jetbrains.compose.resources.stringResource

/**
 * One subscription plan on the paywall (Paywall.png): plan name and an optional detail line on
 * the start side, the store price on the end side. Selected = 3dp primary border; an optional
 * [badge] (e.g. "وفّر 33%") sits on the top edge at the end side.
 */
@Composable
fun PlanCard(
    title: String,
    price: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    badge: String? = null,
    enabled: Boolean = true,
) {
    val colors = ClipKidTheme.colors
    val shape = ClipKidTheme.shapes.card
    val borderColor by animateColorAsState(if (selected) colors.primary else colors.border, label = "planBorder")
    Box(modifier.padding(top = if (badge != null) 14.dp else 0.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clip(shape)
                .background(colors.surface)
                .border(if (selected) 3.dp else 1.5.dp, borderColor, shape)
                .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
                .alpha(if (enabled) 1f else DisabledAlpha)
                .padding(horizontal = ClipKidTheme.spacing.xl, vertical = ClipKidTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.md),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.xxs)) {
                Text(title, style = ClipKidTheme.typography.titleLarge, color = colors.ink, maxLines = 1)
                if (detail != null) {
                    Text(detail, style = ClipKidTheme.typography.bodyMedium, color = colors.muted)
                }
            }
            Text(price, style = ClipKidTheme.typography.titleLarge, color = colors.ink, maxLines = 1)
        }
        if (badge != null) {
            Text(
                text = badge,
                style = ClipKidTheme.typography.labelLarge,
                color = colors.onPrimary,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-24).dp, y = (-14).dp)
                    .clip(ClipKidTheme.shapes.pill)
                    .background(colors.primary)
                    .padding(horizontal = ClipKidTheme.spacing.md, vertical = ClipKidTheme.spacing.xxs),
            )
        }
    }
}

// region Showcase

@Composable
fun PlanCardShowcase() {
    ShowcaseColumn {
        PlanCard(
            title = stringResource(Res.string.showcase_plan_annual),
            price = stringResource(Res.string.showcase_plan_annual_price),
            selected = true,
            onClick = {},
            detail = stringResource(Res.string.showcase_plan_annual_detail),
            badge = stringResource(Res.string.showcase_plan_badge),
        )
        PlanCard(
            title = stringResource(Res.string.showcase_plan_monthly),
            price = stringResource(Res.string.showcase_plan_monthly_price),
            selected = false,
            onClick = {},
        )
    }
}

@Preview
@Composable
private fun PlanCardPreview() = ClipKidPreview { PlanCardShowcase() }

// endregion
