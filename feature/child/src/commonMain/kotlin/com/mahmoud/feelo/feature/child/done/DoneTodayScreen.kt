package com.mahmoud.feelo.feature.child.done

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mahmoud.feelo.core.designsystem.component.Icon
import com.mahmoud.feelo.core.designsystem.component.PrimaryButton
import com.mahmoud.feelo.core.designsystem.component.PrimaryButtonTone
import com.mahmoud.feelo.core.designsystem.component.QuotaDots
import com.mahmoud.feelo.core.designsystem.component.Text
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcons
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import com.mahmoud.feelo.core.designsystem.theme.CoverPalettes
import com.mahmoud.feelo.core.model.QuotaUnit
import com.mahmoud.feelo.feature.child.resources.Res
import com.mahmoud.feelo.feature.child.resources.child_done_ask_parent
import com.mahmoud.feelo.feature.child.resources.child_done_home
import com.mahmoud.feelo.feature.child.resources.child_done_next
import com.mahmoud.feelo.feature.child.resources.child_done_subtitle
import com.mahmoud.feelo.feature.child.resources.child_done_title
import org.jetbrains.compose.resources.stringResource

/** DoneToday: shown when today's limit is used up. */
@Composable
fun DoneTodayRoute(
    viewModel: DoneTodayViewModel,
    onEffect: (DoneTodayEffect) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect(onEffect)
    }
    DoneTodayScreen(state, viewModel::onEvent)
}

@Composable
fun DoneTodayScreen(
    state: DoneTodayUiState,
    onEvent: (DoneTodayEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FeeloTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .background(colors.celebrate)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = FeeloTheme.spacing.gutter, vertical = FeeloTheme.spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Celebration()
        Spacer(Modifier.height(FeeloTheme.spacing.xxl))
        Text(
            text = stringResource(Res.string.child_done_title),
            style = FeeloTheme.typography.displayLarge,
            color = colors.ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(FeeloTheme.spacing.xs))
        Text(
            text = stringResource(Res.string.child_done_subtitle),
            style = FeeloTheme.typography.titleMedium,
            color = colors.ink,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(FeeloTheme.spacing.lg))
        if (state is DoneTodayUiState.Ready && state.quota.unit == QuotaUnit.Videos) {
            QuotaDots(used = state.quota.total, total = state.quota.total, outlined = true)
        }
        Spacer(Modifier.height(FeeloTheme.spacing.lg))
        Text(
            text = stringResource(Res.string.child_done_next),
            style = FeeloTheme.typography.bodyLarge,
            color = colors.muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton(
            text = stringResource(Res.string.child_done_home),
            onClick = { onEvent(DoneTodayEvent.HomeClicked) },
            tone = PrimaryButtonTone.Ink,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FeeloTheme.spacing.md))
        Text(
            text = stringResource(Res.string.child_done_ask_parent),
            style = FeeloTheme.typography.labelLarge.copy(textDecoration = TextDecoration.Underline),
            color = colors.ink,
            modifier = Modifier
                .defaultMinSize(minHeight = FeeloTheme.sizes.minTouchTarget)
                .clickable { onEvent(DoneTodayEvent.AskParentClicked) }
                .padding(FeeloTheme.spacing.sm),
        )
    }
}

/** Star medallion with a few confetti dots. Decorative only. */
@Composable
private fun Celebration() {
    val colors = FeeloTheme.colors
    Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(260.dp).background(colors.accent.copy(alpha = 0.2f), FeeloTheme.shapes.pill))
        Box(Modifier.size(180.dp).background(colors.accent, FeeloTheme.shapes.pill), contentAlignment = Alignment.Center) {
            Icon(icon = FeeloIcons.Star, contentDescription = null, tint = colors.surface, size = 96.dp)
        }
        Confetti(colors.primary, Modifier.offset(x = (-110).dp, y = (-96).dp))
        Confetti(colors.danger, Modifier.offset(x = 100.dp, y = (-90).dp))
        Confetti(CoverPalettes.Science.fg, Modifier.offset(x = (-120).dp, y = 90.dp))
        Confetti(colors.success, Modifier.offset(x = 100.dp, y = 100.dp))
    }
}

@Composable
private fun Confetti(color: Color, modifier: Modifier) {
    Box(modifier.size(18.dp).background(color, FeeloTheme.shapes.badge))
}
