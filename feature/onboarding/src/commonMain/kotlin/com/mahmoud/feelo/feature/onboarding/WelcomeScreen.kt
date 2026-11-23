package com.mahmoud.feelo.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.feelo.core.designsystem.component.Icon
import com.mahmoud.feelo.core.designsystem.component.PrimaryButton
import com.mahmoud.feelo.core.designsystem.component.Text
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcon
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcons
import com.mahmoud.feelo.core.designsystem.preview.FeeloPreview
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import com.mahmoud.feelo.core.designsystem.theme.CoverPalettes
import com.mahmoud.feelo.feature.onboarding.resources.Res
import com.mahmoud.feelo.feature.onboarding.resources.onb_brand
import com.mahmoud.feelo.feature.onboarding.resources.onb_mosaic_letter
import com.mahmoud.feelo.feature.onboarding.resources.onb_start
import com.mahmoud.feelo.feature.onboarding.resources.onb_welcome_caption
import com.mahmoud.feelo.feature.onboarding.resources.onb_welcome_point_limit
import com.mahmoud.feelo.feature.onboarding.resources.onb_welcome_point_no_feed
import com.mahmoud.feelo.feature.onboarding.resources.onb_welcome_point_reviewed
import com.mahmoud.feelo.feature.onboarding.resources.onb_welcome_subtitle
import com.mahmoud.feelo.feature.onboarding.resources.onb_welcome_title
import org.jetbrains.compose.resources.stringResource

/** Onboard.png: what Feelo is, and "يلا نبدأ". */
@Composable
fun WelcomeScreen(onEvent: (OnboardingEvent) -> Unit, modifier: Modifier = Modifier) {
    val colors = FeeloTheme.colors
    val spacing = FeeloTheme.spacing
    Box(
        modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.fillMaxSize().widthIn(max = SetupMaxWidth).padding(horizontal = spacing.gutter)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(spacing.xl))
                Text(
                    text = stringResource(Res.string.onb_brand),
                    style = FeeloTheme.typography.displayMedium,
                    color = colors.primary,
                )
                Spacer(Modifier.height(spacing.xl))
                Mosaic()
                Spacer(Modifier.height(spacing.xl))
                Text(stringResource(Res.string.onb_welcome_title), style = FeeloTheme.typography.displayLarge, color = colors.ink)
                Spacer(Modifier.height(spacing.sm))
                Text(stringResource(Res.string.onb_welcome_subtitle), style = FeeloTheme.typography.bodyLarge, color = colors.muted)
                Spacer(Modifier.height(spacing.xl))
                Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
                    Point(FeeloIcons.Check, colors.success, CoverPalettes.QuranAndDuas.bg, stringResource(Res.string.onb_welcome_point_reviewed))
                    Point(FeeloIcons.Timer, CoverPalettes.Letters.fg, CoverPalettes.Letters.bg, stringResource(Res.string.onb_welcome_point_limit))
                    Point(FeeloIcons.ListFilter, colors.primary, CoverPalettes.Numbers.bg, stringResource(Res.string.onb_welcome_point_no_feed))
                }
                Spacer(Modifier.height(spacing.xl))
            }
            Column(
                Modifier.fillMaxWidth().padding(vertical = spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                PrimaryButton(
                    text = stringResource(Res.string.onb_start),
                    onClick = { onEvent(OnboardingEvent.StartClicked) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(Res.string.onb_welcome_caption),
                    style = FeeloTheme.typography.bodyMedium,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/** The six colored tiles at the top of Onboard.png. Decorative. */
@Composable
private fun Mosaic() {
    val colors = FeeloTheme.colors
    val letter = stringResource(Res.string.onb_mosaic_letter)
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surface, FeeloTheme.shapes.card)
            .padding(FeeloTheme.spacing.md)
            .clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.md),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.md)) {
            Tile(CoverPalettes.Letters.bg) { Text(letter, style = FeeloTheme.typography.displayLarge, color = CoverPalettes.Letters.fg) }
            Tile(colors.primary) { Icon(FeeloIcons.PlayFilled, contentDescription = null, tint = colors.onPrimary, size = 32.dp) }
            Tile(CoverPalettes.Numbers.bg) {
                Text("1", style = FeeloTheme.typography.displayLarge, color = CoverPalettes.Numbers.fg)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.md)) {
            Tile(CoverPalettes.QuranAndDuas.bg) {}
            Tile(colors.accent) { Icon(FeeloIcons.Star, contentDescription = null, tint = colors.ink, size = 32.dp) }
            Tile(CoverPalettes.Manners.bg) {}
        }
    }
}

@Composable
private fun RowScope.Tile(color: Color, content: @Composable () -> Unit) {
    Box(
        Modifier.weight(1f).aspectRatio(0.87f).background(color, FeeloTheme.shapes.card),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Point(icon: FeeloIcon, tint: Color, background: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(36.dp).background(background, FeeloTheme.shapes.iconButton), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, size = 20.dp)
        }
        Spacer(Modifier.size(FeeloTheme.spacing.md))
        Text(text, style = FeeloTheme.typography.bodyLarge, color = FeeloTheme.colors.ink)
    }
}

@Preview
@Composable
private fun WelcomePreview() = FeeloPreview { WelcomeScreen(onEvent = {}) }
