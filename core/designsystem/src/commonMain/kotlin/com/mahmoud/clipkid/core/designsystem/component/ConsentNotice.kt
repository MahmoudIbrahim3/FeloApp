package com.mahmoud.clipkid.core.designsystem.component

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
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcon
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.preview.ClipKidPreview
import com.mahmoud.clipkid.core.designsystem.resources.Res
import com.mahmoud.clipkid.core.designsystem.resources.ds_consent_intro
import com.mahmoud.clipkid.core.designsystem.resources.ds_consent_privacy
import com.mahmoud.clipkid.core.designsystem.resources.ds_consent_store_body
import com.mahmoud.clipkid.core.designsystem.resources.ds_consent_store_title
import com.mahmoud.clipkid.core.designsystem.resources.ds_consent_youtube_body
import com.mahmoud.clipkid.core.designsystem.resources.ds_consent_youtube_terms
import com.mahmoud.clipkid.core.designsystem.resources.ds_consent_youtube_title
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
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
    val colors = ClipKidTheme.colors
    val spacing = ClipKidTheme.spacing
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        Text(stringResource(Res.string.ds_consent_intro), style = ClipKidTheme.typography.bodyLarge, color = colors.ink)
        ConsentPoint(
            icon = ClipKidIcons.PlayFilled,
            title = stringResource(Res.string.ds_consent_youtube_title),
            body = stringResource(Res.string.ds_consent_youtube_body),
        )
        ConsentPoint(
            icon = ClipKidIcons.Star,
            title = stringResource(Res.string.ds_consent_store_title),
            body = stringResource(Res.string.ds_consent_store_body),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            ConsentLink(stringResource(Res.string.ds_consent_privacy), onPrivacy)
            Box(Modifier.padding(horizontal = spacing.xs).size(4.dp).background(colors.muted, ClipKidTheme.shapes.pill))
            ConsentLink(stringResource(Res.string.ds_consent_youtube_terms), onYouTubeTerms)
        }
    }
}

@Composable
private fun ConsentPoint(icon: ClipKidIcon, title: String, body: String) {
    val colors = ClipKidTheme.colors
    Row(
        Modifier.fillMaxWidth().background(colors.surface, ClipKidTheme.shapes.cardSmall).padding(ClipKidTheme.spacing.md),
    ) {
        Icon(icon, contentDescription = null, tint = colors.primary, size = 24.dp)
        Spacer(Modifier.size(ClipKidTheme.spacing.sm))
        Column(verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.xxs)) {
            Text(title, style = ClipKidTheme.typography.titleSmall, color = colors.ink)
            Text(body, style = ClipKidTheme.typography.bodyMedium, color = colors.muted)
        }
    }
}

/** Underlined link text, at least 48dp tall. */
@Composable
private fun ConsentLink(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minHeight = ClipKidTheme.sizes.minTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = ClipKidTheme.spacing.xxs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = ClipKidTheme.typography.bodySmall.copy(textDecoration = TextDecoration.Underline),
            color = ClipKidTheme.colors.primary,
        )
    }
}

@Preview
@Composable
private fun ConsentNoticePreview() = ClipKidPreview { ConsentNotice(onPrivacy = {}, onYouTubeTerms = {}) }
