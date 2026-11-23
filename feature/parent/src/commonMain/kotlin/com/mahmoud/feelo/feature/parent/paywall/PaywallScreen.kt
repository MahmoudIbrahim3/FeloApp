package com.mahmoud.feelo.feature.parent.paywall

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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.feelo.core.designsystem.component.ConsentNotice
import com.mahmoud.feelo.core.designsystem.component.Icon
import com.mahmoud.feelo.core.designsystem.component.IconButtonLarge
import com.mahmoud.feelo.core.designsystem.component.IconButtonTone
import com.mahmoud.feelo.core.designsystem.component.PlanCard
import com.mahmoud.feelo.core.designsystem.component.PrimaryButton
import com.mahmoud.feelo.core.designsystem.component.SecondaryButton
import com.mahmoud.feelo.core.designsystem.component.Text
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcons
import com.mahmoud.feelo.core.designsystem.preview.FeeloPreview
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import com.mahmoud.feelo.core.model.BillingStore
import com.mahmoud.feelo.core.model.Entitlement
import com.mahmoud.feelo.core.model.Offering
import com.mahmoud.feelo.core.model.Plan
import com.mahmoud.feelo.core.model.PlanPeriod
import com.mahmoud.feelo.core.model.SubscriptionLimits
import com.mahmoud.feelo.core.model.TrialPeriod
import com.mahmoud.feelo.core.model.TrialUnit
import com.mahmoud.feelo.feature.parent.resources.Res
import com.mahmoud.feelo.feature.parent.resources.paywall_after_trial_month
import com.mahmoud.feelo.feature.parent.resources.paywall_after_trial_year
import com.mahmoud.feelo.feature.parent.resources.paywall_badge
import com.mahmoud.feelo.feature.parent.resources.paywall_benefit_activity
import com.mahmoud.feelo.feature.parent.resources.paywall_benefit_children
import com.mahmoud.feelo.feature.parent.resources.paywall_close
import com.mahmoud.feelo.feature.parent.resources.paywall_consent_agree
import com.mahmoud.feelo.feature.parent.resources.paywall_consent_title
import com.mahmoud.feelo.feature.parent.resources.paywall_cta_busy
import com.mahmoud.feelo.feature.parent.resources.paywall_cta_subscribe
import com.mahmoud.feelo.feature.parent.resources.paywall_cta_trial
import com.mahmoud.feelo.feature.parent.resources.paywall_done
import com.mahmoud.feelo.feature.parent.resources.paywall_ends_on
import com.mahmoud.feelo.feature.parent.resources.paywall_free_tier
import com.mahmoud.feelo.feature.parent.resources.paywall_videos_free
import com.mahmoud.feelo.feature.parent.resources.paywall_loading
import com.mahmoud.feelo.feature.parent.resources.paywall_manage
import com.mahmoud.feelo.feature.parent.resources.paywall_msg_already_owned
import com.mahmoud.feelo.feature.parent.resources.paywall_msg_failed
import com.mahmoud.feelo.feature.parent.resources.paywall_msg_network
import com.mahmoud.feelo.feature.parent.resources.paywall_msg_not_allowed
import com.mahmoud.feelo.feature.parent.resources.paywall_msg_not_available
import com.mahmoud.feelo.feature.parent.resources.paywall_msg_nothing_to_restore
import com.mahmoud.feelo.feature.parent.resources.paywall_msg_pending
import com.mahmoud.feelo.feature.parent.resources.paywall_per_month
import com.mahmoud.feelo.feature.parent.resources.paywall_plan_annual
import com.mahmoud.feelo.feature.parent.resources.paywall_plan_monthly
import com.mahmoud.feelo.feature.parent.resources.paywall_price_month
import com.mahmoud.feelo.feature.parent.resources.paywall_price_year
import com.mahmoud.feelo.feature.parent.resources.paywall_privacy
import com.mahmoud.feelo.feature.parent.resources.paywall_renew_app_store
import com.mahmoud.feelo.feature.parent.resources.paywall_renew_play
import com.mahmoud.feelo.feature.parent.resources.paywall_renews_on
import com.mahmoud.feelo.feature.parent.resources.paywall_restore
import com.mahmoud.feelo.feature.parent.resources.paywall_retry
import com.mahmoud.feelo.feature.parent.resources.paywall_save
import com.mahmoud.feelo.feature.parent.resources.paywall_subscribed_title
import com.mahmoud.feelo.feature.parent.resources.paywall_terms
import com.mahmoud.feelo.feature.parent.resources.paywall_title
import com.mahmoud.feelo.feature.parent.resources.paywall_trial_status
import com.mahmoud.feelo.feature.parent.resources.paywall_unavailable_message
import com.mahmoud.feelo.feature.parent.resources.paywall_unavailable_title
import com.mahmoud.feelo.feature.parent.resources.paywall_welcome_message
import com.mahmoud.feelo.feature.parent.resources.paywall_welcome_title
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@Composable
fun PaywallRoute(viewModel: PaywallViewModel, onEffect: (PaywallEffect) -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.effects.collect(onEffect) }
    PaywallScreen(state, viewModel::onEvent)
}

/**
 * Paywall.png: close and restore on top, benefits, annual + monthly plans from the store, the
 * trial button and the auto-renew text with terms and privacy at the bottom. Centered and
 * width-capped on tablets.
 */
@Composable
fun PaywallScreen(state: PaywallUiState, onEvent: (PaywallEvent) -> Unit, modifier: Modifier = Modifier) {
    val colors = FeeloTheme.colors
    val spacing = FeeloTheme.spacing
    Box(
        modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier.fillMaxSize().widthIn(max = 560.dp).padding(horizontal = spacing.gutter, vertical = spacing.md),
        ) {
            TopBar(showRestore = state.subscription == null && !state.needsConsent, enabled = state.busy == null, onEvent = onEvent)
            val subscription = state.subscription
            when {
                subscription != null -> SubscribedContent(subscription, state.justSubscribed, onEvent)
                else -> PlansContent(state, onEvent)
            }
        }
    }
}

@Composable
private fun TopBar(showRestore: Boolean, enabled: Boolean, onEvent: (PaywallEvent) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButtonLarge(
            icon = FeeloIcons.Close,
            contentDescription = stringResource(Res.string.paywall_close),
            onClick = { onEvent(PaywallEvent.Close) },
            tone = IconButtonTone.Surface,
            compact = true,
        )
        Spacer(Modifier.weight(1f))
        if (showRestore) {
            TextLink(stringResource(Res.string.paywall_restore), enabled = enabled) { onEvent(PaywallEvent.Restore) }
        }
    }
}

@Composable
private fun ColumnScope.PlansContent(state: PaywallUiState, onEvent: (PaywallEvent) -> Unit) {
    val colors = FeeloTheme.colors
    val spacing = FeeloTheme.spacing
    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Text(
            text = stringResource(Res.string.paywall_badge),
            style = FeeloTheme.typography.labelLarge,
            color = colors.onAccent,
            modifier = Modifier
                .background(colors.accent, FeeloTheme.shapes.pill)
                .padding(horizontal = spacing.md, vertical = spacing.xs),
        )
        Text(
            text = stringResource(Res.string.paywall_title),
            style = FeeloTheme.typography.displayMedium,
            color = colors.ink,
            modifier = Modifier.semantics { heading() },
        )
        Benefit(stringResource(Res.string.paywall_benefit_children, state.maxChildren.toString()))
        Benefit(stringResource(Res.string.paywall_benefit_activity))
        // Every video is free (YouTube Developer Policies III.F.3.a); said plainly, so no parent
        // thinks the subscription buys content.
        Text(stringResource(Res.string.paywall_videos_free), style = FeeloTheme.typography.bodyMedium, color = colors.muted)
        Spacer(Modifier.height(spacing.xs))
        val offering = state.offering
        when {
            state.needsConsent -> ConsentCard(onEvent)
            state.loading -> Text(stringResource(Res.string.paywall_loading), style = FeeloTheme.typography.bodyLarge, color = colors.muted)
            offering == null -> Unavailable(enabled = state.busy == null) { onEvent(PaywallEvent.Retry) }
            else -> Plans(offering, state, onEvent)
        }
    }
    if (state.offering != null) {
        BottomActions(state, onEvent)
    }
}

/** Installs from before the onboarding consent step: agree first, then the store is contacted. */
@Composable
private fun ConsentCard(onEvent: (PaywallEvent) -> Unit) {
    val colors = FeeloTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.md)) {
        Text(stringResource(Res.string.paywall_consent_title), style = FeeloTheme.typography.titleMedium, color = colors.ink)
        ConsentNotice(
            onPrivacy = { onEvent(PaywallEvent.Privacy) },
            onYouTubeTerms = { onEvent(PaywallEvent.YouTubeTerms) },
        )
        PrimaryButton(
            text = stringResource(Res.string.paywall_consent_agree),
            onClick = { onEvent(PaywallEvent.AgreeConsent) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Benefit(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.sm)) {
        Icon(FeeloIcons.Check, contentDescription = null, tint = FeeloTheme.colors.success, size = 24.dp)
        Text(text, style = FeeloTheme.typography.bodyLarge, color = FeeloTheme.colors.ink)
    }
}

@Composable
private fun Plans(offering: Offering, state: PaywallUiState, onEvent: (PaywallEvent) -> Unit) {
    val enabled = state.busy == null
    offering.annual?.let { plan ->
        PlanCard(
            title = stringResource(Res.string.paywall_plan_annual),
            price = plan.price,
            selected = state.selectedPlan?.period == PlanPeriod.Annual,
            onClick = { onEvent(PaywallEvent.SelectPlan(PlanPeriod.Annual)) },
            detail = plan.pricePerMonth?.let { stringResource(Res.string.paywall_per_month, it) },
            badge = state.savingsPercent?.let { stringResource(Res.string.paywall_save, it.toString()) },
            enabled = enabled,
        )
    }
    offering.monthly?.let { plan ->
        PlanCard(
            title = stringResource(Res.string.paywall_plan_monthly),
            price = plan.price,
            selected = state.selectedPlan?.period == PlanPeriod.Monthly,
            onClick = { onEvent(PaywallEvent.SelectPlan(PlanPeriod.Monthly)) },
            enabled = enabled,
        )
    }
}

@Composable
private fun Unavailable(enabled: Boolean, onRetry: () -> Unit) {
    val colors = FeeloTheme.colors
    Column(
        Modifier.fillMaxWidth().background(colors.surface, FeeloTheme.shapes.card).padding(FeeloTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.sm),
    ) {
        Text(stringResource(Res.string.paywall_unavailable_title), style = FeeloTheme.typography.titleMedium, color = colors.ink)
        Text(stringResource(Res.string.paywall_unavailable_message), style = FeeloTheme.typography.bodyMedium, color = colors.muted)
        SecondaryButton(text = stringResource(Res.string.paywall_retry), onClick = onRetry, enabled = enabled, modifier = Modifier.fillMaxWidth())
    }
}

/** Message, the main button, and the legal text (price after the trial, auto-renew, terms, privacy). */
@Composable
private fun BottomActions(state: PaywallUiState, onEvent: (PaywallEvent) -> Unit) {
    val colors = FeeloTheme.colors
    val spacing = FeeloTheme.spacing
    val plan = state.selectedPlan
    // Local copy: Plan is from core:model, so Kotlin can't smart-cast plan.trial after a null check.
    val trial = plan?.trial
    Column(Modifier.fillMaxWidth().padding(top = spacing.xs), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        state.message?.let { message ->
            Text(
                text = stringResource(message.text()),
                style = FeeloTheme.typography.bodyMedium,
                color = if (message.isError) colors.danger else colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        PrimaryButton(
            text = when {
                state.busy != null -> stringResource(Res.string.paywall_cta_busy)
                trial != null -> trialLabel(trial)
                else -> stringResource(Res.string.paywall_cta_subscribe)
            },
            onClick = { onEvent(PaywallEvent.Subscribe) },
            enabled = state.busy == null && plan != null,
            modifier = Modifier.fillMaxWidth(),
        )
        if (plan != null) {
            LegalText(plan)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            TextLink(stringResource(Res.string.paywall_terms), small = true) { onEvent(PaywallEvent.Terms) }
            Box(Modifier.padding(horizontal = spacing.xs).size(4.dp).background(colors.muted, FeeloTheme.shapes.pill))
            TextLink(stringResource(Res.string.paywall_privacy), small = true) { onEvent(PaywallEvent.Privacy) }
        }
    }
}

@Composable
private fun trialLabel(trial: TrialPeriod): String =
    pluralStringResource(Res.plurals.paywall_cta_trial, trial.days, trial.days.toString())

@Composable
private fun LegalText(plan: Plan) {
    val price = when {
        plan.trial != null && plan.period == PlanPeriod.Annual -> stringResource(Res.string.paywall_after_trial_year, plan.price)
        plan.trial != null -> stringResource(Res.string.paywall_after_trial_month, plan.price)
        plan.period == PlanPeriod.Annual -> stringResource(Res.string.paywall_price_year, plan.price)
        else -> stringResource(Res.string.paywall_price_month, plan.price)
    }
    val renew = stringResource(
        when (platformStore) {
            PlatformStore.GooglePlay -> Res.string.paywall_renew_play
            PlatformStore.AppStore -> Res.string.paywall_renew_app_store
        },
    )
    val free = stringResource(Res.string.paywall_free_tier)
    Text(
        text = "$price $renew\n$free",
        style = FeeloTheme.typography.bodySmall,
        color = FeeloTheme.colors.muted,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** Already subscribed (or just subscribed): status, renewal date, manage, done. */
@Composable
private fun ColumnScope.SubscribedContent(subscription: Entitlement.Full, justSubscribed: Boolean, onEvent: (PaywallEvent) -> Unit) {
    val colors = FeeloTheme.colors
    val spacing = FeeloTheme.spacing
    Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = spacing.xl),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Column(
            Modifier.fillMaxWidth().background(colors.celebrate, FeeloTheme.shapes.card).padding(spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Icon(FeeloIcons.Star, contentDescription = null, tint = colors.accent, size = 40.dp)
            Text(
                text = stringResource(if (justSubscribed) Res.string.paywall_welcome_title else Res.string.paywall_subscribed_title),
                style = FeeloTheme.typography.displayMedium,
                color = colors.ink,
                modifier = Modifier.semantics { heading() },
            )
            if (justSubscribed) {
                Text(stringResource(Res.string.paywall_welcome_message, SubscriptionLimits.MAX_FULL_CHILDREN.toString()), style = FeeloTheme.typography.bodyLarge, color = colors.ink)
            }
            if (subscription.isTrial) {
                Text(stringResource(Res.string.paywall_trial_status), style = FeeloTheme.typography.bodyLarge, color = colors.muted)
            }
            subscription.expiresAt?.let { date ->
                Text(
                    text = stringResource(
                        if (subscription.willRenew) Res.string.paywall_renews_on else Res.string.paywall_ends_on,
                        date.toDisplayDate(),
                    ),
                    style = FeeloTheme.typography.bodyLarge,
                    color = colors.muted,
                )
            }
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        if (subscription.managementUrl != null) {
            SecondaryButton(
                text = stringResource(Res.string.paywall_manage),
                onClick = { onEvent(PaywallEvent.ManageSubscription) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        PrimaryButton(text = stringResource(Res.string.paywall_done), onClick = { onEvent(PaywallEvent.Close) }, modifier = Modifier.fillMaxWidth())
    }
}

/** Underlined primary-colored text button, at least 48dp tall. */
@Composable
private fun TextLink(text: String, small: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier
            .defaultMinSize(minHeight = FeeloTheme.sizes.minTouchTarget)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = FeeloTheme.spacing.xxs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = (if (small) FeeloTheme.typography.bodySmall else FeeloTheme.typography.bodyLarge)
                .copy(textDecoration = TextDecoration.Underline),
            color = if (enabled) FeeloTheme.colors.primary else FeeloTheme.colors.muted,
        )
    }
}

/** "15/12/2026" in the device's time zone. */
private fun Instant.toDisplayDate(): String {
    val date = toLocalDateTime(TimeZone.currentSystemDefault()).date
    return "${date.day}/${date.month.ordinal + 1}/${date.year}"
}

private fun PaywallMessage.text(): StringResource = when (this) {
    PaywallMessage.Pending -> Res.string.paywall_msg_pending
    PaywallMessage.NothingToRestore -> Res.string.paywall_msg_nothing_to_restore
    PaywallMessage.Network -> Res.string.paywall_msg_network
    PaywallMessage.NotAllowed -> Res.string.paywall_msg_not_allowed
    PaywallMessage.NotAvailable -> Res.string.paywall_msg_not_available
    PaywallMessage.AlreadyOwned -> Res.string.paywall_msg_already_owned
    PaywallMessage.Failed -> Res.string.paywall_msg_failed
}

// region Previews

// Sample prices for previews only (never shown in the app: real prices come from the store).
private const val PREVIEW_MONTHLY = "1.500 KWD"
private const val PREVIEW_ANNUAL = "12.000 KWD"
private const val PREVIEW_ANNUAL_MONTH = "1.000 KWD"

private val previewOffering = Offering(
    monthly = Plan("\$rc_monthly", PlanPeriod.Monthly, PREVIEW_MONTHLY, 1_500_000, "KWD", null, TrialPeriod(1, TrialUnit.Week)),
    annual = Plan("\$rc_annual", PlanPeriod.Annual, PREVIEW_ANNUAL, 12_000_000, "KWD", PREVIEW_ANNUAL_MONTH, TrialPeriod(1, TrialUnit.Week)),
)

@Preview
@Composable
private fun PaywallPreview() = FeeloPreview {
    PaywallScreen(PaywallUiState(loading = false, offering = previewOffering), {})
}

@Preview
@Composable
private fun PaywallConsentPreview() = FeeloPreview { PaywallScreen(PaywallUiState(loading = false, needsConsent = true), {}) }

@Preview
@Composable
private fun PaywallUnavailablePreview() = FeeloPreview { PaywallScreen(PaywallUiState(loading = false), {}) }

@Preview
@Composable
private fun PaywallSubscribedPreview() = FeeloPreview {
    PaywallScreen(
        PaywallUiState(
            loading = false,
            subscription = Entitlement.Full(Instant.parse("2027-01-15T00:00:00Z"), true, true, BillingStore.PlayStore, "https://play.google.com/store/account/subscriptions"),
            justSubscribed = true,
        ),
        {},
    )
}

// endregion
