package com.mahmoud.feelo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcon
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcons
import com.mahmoud.feelo.core.designsystem.preview.FeeloPreview
import com.mahmoud.feelo.core.designsystem.resources.Res
import com.mahmoud.feelo.core.designsystem.resources.ds_consent_intro
import com.mahmoud.feelo.core.designsystem.resources.ds_consent_privacy
import com.mahmoud.feelo.core.designsystem.resources.ds_consent_store_body
import com.mahmoud.feelo.core.designsystem.resources.ds_consent_store_title
import com.mahmoud.feelo.core.designsystem.resources.ds_consent_youtube_body
import com.mahmoud.feelo.core.designsystem.resources.ds_consent_youtube_terms
import com.mahmoud.feelo.core.designsystem.resources.ds_consent_youtube_title
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import org.jetbrains.compose.resources.stringResource

/**
 * What the parent agrees to before the app uses outside services (Apple Kids: no device data to
 * third parties without the parent's consent): the YouTube player, and the store with RevenueCat
 * for subscriptions. One wording, used by the onboarding consent step and the paywall.
 */
@Composable
fun ConsentNotice(
    onPrivacy: () -> Unit,
    onYouTubeTerms: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FeeloTheme.colors
    val spacing = FeeloTheme.spacing
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text(stringResource(Res.string.ds_consent_intro), style = FeeloTheme.typography.bodyLarge, color = colors.ink)
        ConsentPoint(
            icon = FeeloIcons.PlayFilled,
            title = stringResource(Res.string.ds_consent_youtube_title),
            body = stringResource(Res.string.ds_consent_youtube_body),
        )
        ConsentPoint(
            icon = FeeloIcons.Star,
            title = stringResource(Res.string.ds_consent_store_title),
            body = stringResource(Res.string.ds_consent_store_body),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            ConsentLink(stringResource(Res.string.ds_consent_privacy), onPrivacy)
            Box(Modifier.padding(horizontal = spacing.xs).size(4.dp).background(colors.muted, FeeloTheme.shapes.pill))
            ConsentLink(stringResource(Res.string.ds_consent_youtube_terms), onYouTubeTerms)
        }
    }
}

@Composable
private fun ConsentPoint(icon: FeeloIcon, title: String, body: String) {
    val colors = FeeloTheme.colors
    Row(
        Modifier.fillMaxWidth().background(colors.surface, FeeloTheme.shapes.cardSmall).padding(FeeloTheme.spacing.md),
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, size = 24.dp)
        Spacer(Modifier.size(FeeloTheme.spacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.xxs)) {
            Text(title, style = FeeloTheme.typography.titleSmall, color = colors.ink)
            Text(body, style = FeeloTheme.typography.bodyMedium, color = colors.muted)
        }
    }
}

/** Underlined link text, at least 48dp tall. */
@Composable
private fun ConsentLink(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minHeight = FeeloTheme.sizes.minTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = FeeloTheme.spacing.xxs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = FeeloTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
            color = FeeloTheme.colors.primary,
        )
    }
}

@Preview
@Composable
private fun ConsentNoticePreview() = FeeloPreview { ConsentNotice(onPrivacy = {}, onYouTubeTerms = {}) }
