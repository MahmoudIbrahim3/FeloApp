package com.mahmoud.feelo.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.feelo.core.designsystem.component.AgeBandPicker
import com.mahmoud.feelo.core.designsystem.component.AvatarColorPicker
import com.mahmoud.feelo.core.designsystem.component.CollectionPickCard
import com.mahmoud.feelo.core.designsystem.component.ConsentNotice
import com.mahmoud.feelo.core.designsystem.component.DailyLimitPicker
import com.mahmoud.feelo.core.designsystem.component.PinDots
import com.mahmoud.feelo.core.designsystem.component.PinPad
import com.mahmoud.feelo.core.designsystem.component.PrimaryButton
import com.mahmoud.feelo.core.designsystem.component.Text
import com.mahmoud.feelo.core.designsystem.component.ageBandRange
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcons
import com.mahmoud.feelo.core.designsystem.preview.FeeloPreview
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import com.mahmoud.feelo.core.domain.parent.PIN_LENGTH
import com.mahmoud.feelo.core.domain.usecase.DailyLimitOptions
import com.mahmoud.feelo.core.model.AgeBand
import com.mahmoud.feelo.core.model.Collection
import com.mahmoud.feelo.core.model.CoverMark
import com.mahmoud.feelo.core.model.CoverStyle
import com.mahmoud.feelo.core.model.DailyLimit
import com.mahmoud.feelo.feature.onboarding.resources.Res
import com.mahmoud.feelo.feature.onboarding.resources.onb_add_child_finish
import com.mahmoud.feelo.feature.onboarding.resources.onb_child_age_hint
import com.mahmoud.feelo.feature.onboarding.resources.onb_child_age_label
import com.mahmoud.feelo.feature.onboarding.resources.onb_child_avatar_label
import com.mahmoud.feelo.feature.onboarding.resources.onb_child_name_label
import com.mahmoud.feelo.feature.onboarding.resources.onb_child_name_placeholder
import com.mahmoud.feelo.feature.onboarding.resources.onb_child_subtitle
import com.mahmoud.feelo.feature.onboarding.resources.onb_child_title
import com.mahmoud.feelo.feature.onboarding.resources.onb_collections_empty
import com.mahmoud.feelo.feature.onboarding.resources.onb_collections_subtitle
import com.mahmoud.feelo.feature.onboarding.resources.onb_collections_title
import com.mahmoud.feelo.feature.onboarding.resources.onb_consent_agree
import com.mahmoud.feelo.feature.onboarding.resources.onb_consent_caption
import com.mahmoud.feelo.feature.onboarding.resources.onb_consent_subtitle
import com.mahmoud.feelo.feature.onboarding.resources.onb_consent_title
import com.mahmoud.feelo.feature.onboarding.resources.onb_finish
import com.mahmoud.feelo.feature.onboarding.resources.onb_finish_caption
import com.mahmoud.feelo.feature.onboarding.resources.onb_limit_minutes_caption
import com.mahmoud.feelo.feature.onboarding.resources.onb_limit_title
import com.mahmoud.feelo.feature.onboarding.resources.onb_limit_videos_caption
import com.mahmoud.feelo.feature.onboarding.resources.onb_next
import com.mahmoud.feelo.feature.onboarding.resources.onb_pin_confirm_subtitle
import com.mahmoud.feelo.feature.onboarding.resources.onb_pin_confirm_title
import com.mahmoud.feelo.feature.onboarding.resources.onb_pin_mismatch
import com.mahmoud.feelo.feature.onboarding.resources.onb_pin_subtitle
import com.mahmoud.feelo.feature.onboarding.resources.onb_pin_title
import com.mahmoud.feelo.feature.onboarding.resources.onb_save_failed
import org.jetbrains.compose.resources.stringResource

/** Which step a route shows. All steps share one [OnboardingViewModel]. */
enum class OnboardingStep { Welcome, Pin, Consent, Child, Collections }

/** One step of onboarding: collects the shared state and the effects, shows that step. */
@Composable
fun OnboardingRoute(
    step: OnboardingStep,
    viewModel: OnboardingViewModel,
    onEffect: (OnboardingEffect) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.effects.collect(onEffect) }
    when (step) {
        OnboardingStep.Welcome -> WelcomeScreen(viewModel::onEvent)
        OnboardingStep.Pin -> SetPinScreen(state, viewModel::onEvent)
        OnboardingStep.Consent -> ConsentScreen(state, viewModel::onEvent)
        OnboardingStep.Child -> ChildSetupScreen(state, viewModel::onEvent)
        OnboardingStep.Collections -> SetupCollectionsScreen(state, viewModel::onEvent)
    }
}

/** SetPin.png: step 1. Four digits, then the same four again. */
@Composable
fun SetPinScreen(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit, modifier: Modifier = Modifier) {
    val pin = state.pin
    val colors = FeeloTheme.colors
    SetupScaffold(
        step = state.pinStep,
        totalSteps = state.totalSteps,
        title = stringResource(if (pin.confirming) Res.string.onb_pin_confirm_title else Res.string.onb_pin_title),
        subtitle = stringResource(if (pin.confirming) Res.string.onb_pin_confirm_subtitle else Res.string.onb_pin_subtitle),
        onBack = { onEvent(OnboardingEvent.PinBackClicked) },
        modifier = modifier,
        bottom = {
            PinPad(
                onDigit = { onEvent(OnboardingEvent.PinDigit(it)) },
                onBackspace = { onEvent(OnboardingEvent.PinBackspace) },
            )
            Spacer(Modifier.height(FeeloTheme.spacing.sm))
            PrimaryButton(
                text = stringResource(Res.string.onb_next),
                onClick = { onEvent(OnboardingEvent.PinNext) },
                enabled = pin.isComplete,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            PinDots(filled = pin.entered.length, length = PIN_LENGTH)
            Spacer(Modifier.height(FeeloTheme.spacing.md))
            if (pin.mismatch) {
                Text(
                    text = stringResource(Res.string.onb_pin_mismatch),
                    style = FeeloTheme.typography.bodyMedium,
                    color = colors.danger,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}

/**
 * First run only, right after the PIN: what the parent agrees to before the app uses outside
 * services (YouTube's player; Apple/Google and RevenueCat for subscriptions). Nothing is saved
 * until Finish. No design image yet; built from the setup steps' frame.
 */
@Composable
fun ConsentScreen(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit, modifier: Modifier = Modifier) {
    SetupScaffold(
        step = state.consentStep,
        totalSteps = state.totalSteps,
        title = stringResource(Res.string.onb_consent_title),
        subtitle = stringResource(Res.string.onb_consent_subtitle),
        onBack = { onEvent(OnboardingEvent.BackClicked) },
        modifier = modifier,
        bottom = {
            PrimaryButton(
                text = stringResource(Res.string.onb_consent_agree),
                onClick = { onEvent(OnboardingEvent.ConsentAgreed) },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(Res.string.onb_consent_caption),
                style = FeeloTheme.typography.bodySmall,
                color = FeeloTheme.colors.muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = FeeloTheme.spacing.xs),
            )
        },
    ) {
        ConsentNotice(
            onPrivacy = { onEvent(OnboardingEvent.PrivacyClicked) },
            onYouTubeTerms = { onEvent(OnboardingEvent.YouTubeTermsClicked) },
        )
    }
}

/** ChildSetup.png: nickname, avatar color, age band. */
@Composable
fun ChildSetupScreen(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit, modifier: Modifier = Modifier) {
    val spacing = FeeloTheme.spacing
    val focus = LocalFocusManager.current
    SetupScaffold(
        step = state.childStep,
        totalSteps = state.totalSteps,
        title = stringResource(Res.string.onb_child_title),
        subtitle = stringResource(Res.string.onb_child_subtitle),
        onBack = { onEvent(OnboardingEvent.BackClicked) },
        modifier = modifier,
        bottom = {
            PrimaryButton(
                text = stringResource(Res.string.onb_next),
                onClick = { focus.clearFocus(); onEvent(OnboardingEvent.ChildNext) },
                enabled = state.canContinueChild,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Label(stringResource(Res.string.onb_child_name_label))
        NicknameField(
            value = state.nickname,
            onValueChange = { onEvent(OnboardingEvent.NicknameChanged(it)) },
            placeholder = stringResource(Res.string.onb_child_name_placeholder),
            onDone = { focus.clearFocus() },
        )
        Spacer(Modifier.height(spacing.xl))
        Label(stringResource(Res.string.onb_child_avatar_label))
        AvatarColorPicker(
            selected = state.avatarColor,
            onSelect = { onEvent(OnboardingEvent.AvatarPicked(it)) },
            initial = state.initial,
        )
        Spacer(Modifier.height(spacing.xl))
        Label(stringResource(Res.string.onb_child_age_label))
        AgeBandPicker(selected = state.ageBand, onSelect = { onEvent(OnboardingEvent.AgePicked(it)) })
        Spacer(Modifier.height(spacing.sm))
        Text(
            text = stringResource(Res.string.onb_child_age_hint),
            style = FeeloTheme.typography.bodyMedium,
            color = FeeloTheme.colors.muted,
        )
    }
}

/** SetupCollections.png: collections for the age band and the daily limit. */
@Composable
fun SetupCollectionsScreen(state: OnboardingUiState, onEvent: (OnboardingEvent) -> Unit, modifier: Modifier = Modifier) {
    val colors = FeeloTheme.colors
    val spacing = FeeloTheme.spacing
    val firstRun = state.mode == OnboardingMode.FirstRun
    SetupScaffold(
        step = state.collectionsStep,
        totalSteps = state.totalSteps,
        title = stringResource(Res.string.onb_collections_title),
        subtitle = stringResource(Res.string.onb_collections_subtitle, state.ageBand?.let(::ageBandRange).orEmpty()),
        onBack = { onEvent(OnboardingEvent.BackClicked) },
        modifier = modifier,
        bottom = {
            if (state.saveFailed) {
                Text(stringResource(Res.string.onb_save_failed), style = FeeloTheme.typography.bodyMedium, color = colors.danger)
            }
            PrimaryButton(
                text = stringResource(if (firstRun) Res.string.onb_finish else Res.string.onb_add_child_finish),
                onClick = { onEvent(OnboardingEvent.FinishClicked) },
                enabled = state.canFinish,
                icon = if (firstRun) FeeloIcons.Lock else FeeloIcons.Plus,
                modifier = Modifier.fillMaxWidth(),
            )
            if (firstRun) {
                Text(
                    text = stringResource(Res.string.onb_finish_caption),
                    style = FeeloTheme.typography.bodyMedium,
                    color = colors.muted,
                    textAlign = TextAlign.Center,
                )
            }
        },
    ) {
        if (state.collections.isEmpty()) {
            Text(stringResource(Res.string.onb_collections_empty), style = FeeloTheme.typography.bodyLarge, color = colors.muted)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                state.collections.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        row.forEach { option ->
                            CollectionPickCard(
                                title = option.collection.titleAr,
                                style = option.collection.cover,
                                selected = option.selected,
                                onToggle = { on -> onEvent(OnboardingEvent.CollectionToggled(option.collection.id, on)) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(Modifier.height(spacing.xl))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.onb_limit_title),
                style = FeeloTheme.typography.titleMedium,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(
                    if (state.limit is DailyLimit.Minutes) Res.string.onb_limit_minutes_caption else Res.string.onb_limit_videos_caption,
                ),
                style = FeeloTheme.typography.bodyMedium,
                color = colors.muted,
            )
        }
        Spacer(Modifier.height(spacing.sm))
        DailyLimitPicker(
            limit = state.limit,
            onChange = { onEvent(OnboardingEvent.LimitChanged(it)) },
            videoOptions = DailyLimitOptions.videos,
            minuteOptions = DailyLimitOptions.minutes,
            defaultMinutes = DailyLimitOptions.DEFAULT_MINUTES,
        )
        Spacer(Modifier.height(spacing.md))
    }
}

@Composable
private fun Label(text: String) {
    Text(text = text, style = FeeloTheme.typography.titleSmall, color = FeeloTheme.colors.ink)
    Spacer(Modifier.height(FeeloTheme.spacing.sm))
}

/** Single-line nickname field (ChildSetup.png): primary border while focused. */
@Composable
private fun NicknameField(value: String, onValueChange: (String) -> Unit, placeholder: String, onDone: () -> Unit) {
    val colors = FeeloTheme.colors
    val shape = FeeloTheme.shapes.cardSmall
    var focused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = FeeloTheme.typography.bodyLarge.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(colors.surface, shape)
                    .border(if (focused) 2.dp else 1.5.dp, if (focused) colors.primary else colors.border, shape)
                    .padding(horizontal = FeeloTheme.spacing.md),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, style = FeeloTheme.typography.bodyLarge, color = colors.muted)
                }
                inner()
            }
        },
    )
}

// region Previews

// Sample data for previews only (never shown in the app).
private const val PREVIEW_NICKNAME = "عُمر"

private val previewCollections = listOf(
    CollectionOption(Collection("letters", "letters", "الحروف", CoverStyle(0xFFFFE6B0, 0xFF8A5A00, CoverMark.Glyph("أ")), setOf(AgeBand.A6_8), 1), true),
    CollectionOption(Collection("numbers", "numbers", "الأرقام", CoverStyle(0xFFD6E8FF, 0xFF1A4FB5, CoverMark.Glyph("1")), setOf(AgeBand.A6_8), 2), true),
    CollectionOption(Collection("stories", "stories", "حواديت", CoverStyle(0xFFFFE3C7, 0xFFA3530E, CoverMark.Icon("moon")), setOf(AgeBand.A6_8), 3), false),
)

@Preview
@Composable
private fun SetPinPreview() = FeeloPreview {
    SetPinScreen(OnboardingUiState(OnboardingMode.FirstRun, pin = PinStep(entered = "12")), {})
}

@Preview
@Composable
private fun SetPinMismatchPreview() = FeeloPreview {
    SetPinScreen(OnboardingUiState(OnboardingMode.FirstRun, pin = PinStep(mismatch = true)), {})
}

@Preview
@Composable
private fun ChildSetupEmptyPreview() = FeeloPreview { ChildSetupScreen(OnboardingUiState(OnboardingMode.FirstRun), {}) }

@Preview
@Composable
private fun ChildSetupFilledPreview() = FeeloPreview {
    ChildSetupScreen(OnboardingUiState(OnboardingMode.FirstRun, nickname = PREVIEW_NICKNAME, ageBand = AgeBand.A6_8), {})
}

@Preview
@Composable
private fun SetupCollectionsPreview() = FeeloPreview {
    SetupCollectionsScreen(OnboardingUiState(OnboardingMode.FirstRun, nickname = PREVIEW_NICKNAME, ageBand = AgeBand.A6_8, collections = previewCollections), {})
}

@Preview
@Composable
private fun SetupCollectionsEmptyPreview() = FeeloPreview {
    SetupCollectionsScreen(OnboardingUiState(OnboardingMode.AddChild, nickname = PREVIEW_NICKNAME, ageBand = AgeBand.A3_5), {})
}

// endregion
