package com.mahmoud.feelo.feature.parent.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.feelo.core.designsystem.component.Icon
import com.mahmoud.feelo.core.designsystem.component.IconButtonLarge
import com.mahmoud.feelo.core.designsystem.component.IconButtonTone
import com.mahmoud.feelo.core.designsystem.component.Text
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcons
import com.mahmoud.feelo.core.designsystem.preview.FeeloPreview
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import com.mahmoud.feelo.feature.parent.resources.Res
import com.mahmoud.feelo.feature.parent.resources.about_contact
import com.mahmoud.feelo.feature.parent.resources.about_google_privacy
import com.mahmoud.feelo.feature.parent.resources.about_links_title
import com.mahmoud.feelo.feature.parent.resources.about_privacy
import com.mahmoud.feelo.feature.parent.resources.about_summary_network
import com.mahmoud.feelo.feature.parent.resources.about_summary_no_tracking
import com.mahmoud.feelo.feature.parent.resources.about_summary_on_device
import com.mahmoud.feelo.feature.parent.resources.about_summary_title
import com.mahmoud.feelo.feature.parent.resources.about_summary_youtube
import com.mahmoud.feelo.feature.parent.resources.about_terms
import com.mahmoud.feelo.feature.parent.resources.about_title
import com.mahmoud.feelo.feature.parent.resources.about_version
import com.mahmoud.feelo.feature.parent.resources.about_youtube_terms
import com.mahmoud.feelo.feature.parent.resources.parent_back
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun AboutRoute(viewModel: AboutViewModel, onEffect: (AboutEffect) -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.effects.collect(onEffect) }
    AboutScreen(state, viewModel::onEvent)
}

/**
 * "عن التطبيق والخصوصية" (no design image; built from the Activity screen's frame): a short privacy
 * summary that works offline, then links to the full pages. One column, width-capped on tablets.
 */
@Composable
fun AboutScreen(state: AboutUiState, onEvent: (AboutEvent) -> Unit, modifier: Modifier = Modifier) {
    val colors = FeeloTheme.colors
    val spacing = FeeloTheme.spacing
    Box(
        modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.gutter, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButtonLarge(
                    icon = FeeloIcons.Back,
                    contentDescription = stringResource(Res.string.parent_back),
                    onClick = { onEvent(AboutEvent.BackClicked) },
                    tone = IconButtonTone.Surface,
                    compact = true,
                )
                Spacer(Modifier.size(spacing.md))
                Text(
                    stringResource(Res.string.about_title),
                    style = FeeloTheme.typography.displayMedium,
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
            }
            Card {
                CardTitle(stringResource(Res.string.about_summary_title))
                SummaryLine(Res.string.about_summary_no_tracking)
                SummaryLine(Res.string.about_summary_on_device)
                SummaryLine(Res.string.about_summary_network)
                SummaryLine(Res.string.about_summary_youtube)
            }
            Card {
                CardTitle(stringResource(Res.string.about_links_title))
                LinkRow(stringResource(Res.string.about_privacy)) { onEvent(AboutEvent.PrivacyClicked) }
                Divider()
                LinkRow(stringResource(Res.string.about_terms)) { onEvent(AboutEvent.TermsClicked) }
                Divider()
                LinkRow(stringResource(Res.string.about_youtube_terms)) { onEvent(AboutEvent.YouTubeTermsClicked) }
                Divider()
                LinkRow(stringResource(Res.string.about_google_privacy)) { onEvent(AboutEvent.GooglePrivacyClicked) }
                Divider()
                LinkRow(stringResource(Res.string.about_contact), detail = state.supportEmail) { onEvent(AboutEvent.ContactClicked) }
            }
            if (state.appVersion.isNotEmpty()) {
                Text(
                    stringResource(Res.string.about_version, state.appVersion),
                    style = FeeloTheme.typography.bodySmall,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SummaryLine(text: StringResource) {
    val colors = FeeloTheme.colors
    Row(verticalAlignment = Alignment.Top) {
        Icon(FeeloIcons.Check, contentDescription = null, tint = colors.success, size = 20.dp, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.size(FeeloTheme.spacing.sm))
        Text(stringResource(text), style = FeeloTheme.typography.bodyMedium, color = colors.ink, modifier = Modifier.weight(1f))
    }
}

/** One link, at least 56dp tall, with a chevron at the end (mirrored in RTL). */
@Composable
private fun LinkRow(title: String, detail: String? = null, onClick: () -> Unit) {
    val colors = FeeloTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = FeeloTheme.typography.titleSmall, color = colors.ink)
            if (detail != null) Text(detail, style = FeeloTheme.typography.bodySmall, color = colors.muted)
        }
        Icon(FeeloIcons.Forward, contentDescription = null, tint = colors.muted, size = 20.dp)
    }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(FeeloTheme.colors.divider))
}

@Composable
private fun CardTitle(text: String) {
    Text(text, style = FeeloTheme.typography.titleMedium, color = FeeloTheme.colors.ink)
}

@Composable
private fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(FeeloTheme.colors.surface, FeeloTheme.shapes.card)
            .padding(FeeloTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.sm),
        content = content,
    )
}

// region Previews

// Sample data for previews only (never shown in the app).
private const val PREVIEW_EMAIL = "support@example.com"

@Preview
@Composable
private fun AboutPreview() = FeeloPreview { AboutScreen(AboutUiState(appVersion = "1.0", supportEmail = PREVIEW_EMAIL), {}) }

// endregion
