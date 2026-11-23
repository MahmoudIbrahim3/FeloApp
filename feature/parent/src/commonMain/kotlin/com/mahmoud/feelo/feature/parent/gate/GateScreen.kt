package com.mahmoud.feelo.feature.parent.gate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.feelo.core.designsystem.component.Icon
import com.mahmoud.feelo.core.designsystem.component.IconButtonLarge
import com.mahmoud.feelo.core.designsystem.component.IconButtonTone
import com.mahmoud.feelo.core.designsystem.component.PinDots
import com.mahmoud.feelo.core.designsystem.component.PinPad
import com.mahmoud.feelo.core.designsystem.component.PrimaryButton
import com.mahmoud.feelo.core.designsystem.component.PrimaryButtonTone
import com.mahmoud.feelo.core.designsystem.component.Text
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcons
import com.mahmoud.feelo.core.designsystem.preview.FeeloPreview
import com.mahmoud.feelo.core.designsystem.text.formatDuration
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import com.mahmoud.feelo.core.designsystem.theme.SurfaceTone
import com.mahmoud.feelo.core.domain.parent.AdultChallenge
import com.mahmoud.feelo.core.domain.parent.PIN_LENGTH
import com.mahmoud.feelo.feature.parent.resources.Res
import com.mahmoud.feelo.feature.parent.resources.gate_adult_answer_label
import com.mahmoud.feelo.feature.parent.resources.gate_adult_message
import com.mahmoud.feelo.feature.parent.resources.gate_adult_question
import com.mahmoud.feelo.feature.parent.resources.gate_adult_title
import com.mahmoud.feelo.feature.parent.resources.gate_adult_wrong
import com.mahmoud.feelo.feature.parent.resources.gate_confirm
import com.mahmoud.feelo.feature.parent.resources.gate_enter
import com.mahmoud.feelo.feature.parent.resources.gate_forgot
import com.mahmoud.feelo.feature.parent.resources.gate_locked_message
import com.mahmoud.feelo.feature.parent.resources.gate_locked_title
import com.mahmoud.feelo.feature.parent.resources.gate_new_pin_confirm_title
import com.mahmoud.feelo.feature.parent.resources.gate_new_pin_mismatch
import com.mahmoud.feelo.feature.parent.resources.gate_new_pin_title
import com.mahmoud.feelo.feature.parent.resources.gate_next
import com.mahmoud.feelo.feature.parent.resources.gate_parents_only
import com.mahmoud.feelo.feature.parent.resources.gate_pin_title
import com.mahmoud.feelo.feature.parent.resources.gate_save
import com.mahmoud.feelo.feature.parent.resources.gate_wrong_pin
import com.mahmoud.feelo.feature.parent.resources.parent_close
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun GateRoute(viewModel: GateViewModel, onEffect: (GateEffect) -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.effects.collect(onEffect) }
    GateScreen(state, viewModel::onEvent)
}

/** Gate.png: dark PIN screen, then (forgot PIN) the adult check and a new PIN. */
@Composable
fun GateScreen(state: GateUiState, onEvent: (GateEvent) -> Unit, modifier: Modifier = Modifier) {
    val colors = FeeloTheme.colors
    val spacing = FeeloTheme.spacing
    Box(
        modifier
            .fillMaxSize()
            .background(colors.darkBackgroundAlt)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.fillMaxSize().widthIn(max = 480.dp).padding(horizontal = spacing.gutter)) {
            Row(Modifier.fillMaxWidth().padding(top = spacing.md), verticalAlignment = Alignment.CenterVertically) {
                IconButtonLarge(
                    icon = FeeloIcons.Back,
                    contentDescription = stringResource(Res.string.parent_close),
                    onClick = { onEvent(GateEvent.CloseClicked) },
                    tone = IconButtonTone.OnDark,
                    compact = true,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = stringResource(Res.string.gate_parents_only),
                    style = FeeloTheme.typography.labelLarge,
                    color = colors.onAccent,
                    modifier = Modifier
                        .background(colors.accent, FeeloTheme.shapes.pill)
                        .padding(horizontal = spacing.md, vertical = spacing.xs),
                )
            }
            when (val stage = state.stage) {
                GateStage.Pin -> PinStage(state, onEvent, Modifier.weight(1f))
                is GateStage.AdultCheck -> AdultCheckStage(stage, state, onEvent, Modifier.weight(1f))
                is GateStage.NewPin -> NewPinStage(stage, state, onEvent, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PinStage(state: GateUiState, onEvent: (GateEvent) -> Unit, modifier: Modifier) {
    val colors = FeeloTheme.colors
    val locked = state.isLockedOut
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(FeeloTheme.spacing.xl))
        LockMedallion()
        Spacer(Modifier.height(FeeloTheme.spacing.lg))
        Text(
            text = stringResource(if (locked) Res.string.gate_locked_title else Res.string.gate_pin_title),
            style = FeeloTheme.typography.displayMedium,
            color = colors.onDark,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(FeeloTheme.spacing.lg))
        if (locked) {
            Text(
                text = stringResource(Res.string.gate_locked_message, formatDuration(state.lockoutSecondsLeft)),
                style = FeeloTheme.typography.bodyLarge,
                color = colors.onDarkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        } else {
            PinDots(filled = state.entered.length, length = PIN_LENGTH, tone = SurfaceTone.Dark)
            Spacer(Modifier.height(FeeloTheme.spacing.md))
            ErrorText(
                when {
                    state.error == GateError.WrongPin && state.attemptsLeft != null ->
                        pluralStringResource(Res.plurals.gate_wrong_pin, state.attemptsLeft, state.attemptsLeft.toString())
                    else -> null
                },
            )
        }
        Spacer(Modifier.weight(1f))
        PinPad(
            onDigit = { onEvent(GateEvent.Digit(it)) },
            onBackspace = { onEvent(GateEvent.Backspace) },
            tone = SurfaceTone.Dark,
            enabled = !locked,
        )
        Spacer(Modifier.height(FeeloTheme.spacing.lg))
        PrimaryButton(
            text = stringResource(Res.string.gate_enter),
            onClick = { onEvent(GateEvent.Submit) },
            tone = PrimaryButtonTone.Accent,
            enabled = state.canSubmitPin,
            modifier = Modifier.fillMaxWidth(),
        )
        ForgotLink(onClick = { onEvent(GateEvent.ForgotPinClicked) })
    }
}

@Composable
private fun AdultCheckStage(stage: GateStage.AdultCheck, state: GateUiState, onEvent: (GateEvent) -> Unit, modifier: Modifier) {
    val colors = FeeloTheme.colors
    val spacing = FeeloTheme.spacing
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(spacing.xl))
        LockMedallion()
        Spacer(Modifier.height(spacing.lg))
        Text(stringResource(Res.string.gate_adult_title), style = FeeloTheme.typography.displayMedium, color = colors.onDark)
        Spacer(Modifier.height(spacing.xs))
        Text(
            stringResource(Res.string.gate_adult_message),
            style = FeeloTheme.typography.bodyLarge,
            color = colors.onDarkMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(spacing.xl))
        Text(
            text = challengeText(stage.challenge),
            style = FeeloTheme.typography.displayLarge,
            color = colors.onDark,
        )
        Spacer(Modifier.height(spacing.lg))
        AnswerField(
            value = stage.answer,
            onValueChange = { onEvent(GateEvent.AnswerChanged(it)) },
            onDone = { onEvent(GateEvent.AnswerSubmit) },
        )
        Spacer(Modifier.height(spacing.sm))
        ErrorText(if (state.error == GateError.WrongAnswer) stringResource(Res.string.gate_adult_wrong) else null)
        Spacer(Modifier.height(spacing.lg))
        PrimaryButton(
            text = stringResource(Res.string.gate_confirm),
            onClick = { onEvent(GateEvent.AnswerSubmit) },
            tone = PrimaryButtonTone.Accent,
            enabled = stage.answer.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(spacing.md))
    }
}

@Composable
private fun NewPinStage(stage: GateStage.NewPin, state: GateUiState, onEvent: (GateEvent) -> Unit, modifier: Modifier) {
    val colors = FeeloTheme.colors
    val confirming = stage.first != null
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(FeeloTheme.spacing.xl))
        LockMedallion()
        Spacer(Modifier.height(FeeloTheme.spacing.lg))
        Text(
            text = stringResource(if (confirming) Res.string.gate_new_pin_confirm_title else Res.string.gate_new_pin_title),
            style = FeeloTheme.typography.displayMedium,
            color = colors.onDark,
        )
        Spacer(Modifier.height(FeeloTheme.spacing.lg))
        PinDots(filled = state.entered.length, length = PIN_LENGTH, tone = SurfaceTone.Dark)
        Spacer(Modifier.height(FeeloTheme.spacing.md))
        ErrorText(if (state.error == GateError.NewPinMismatch) stringResource(Res.string.gate_new_pin_mismatch) else null)
        Spacer(Modifier.weight(1f))
        PinPad(onDigit = { onEvent(GateEvent.Digit(it)) }, onBackspace = { onEvent(GateEvent.Backspace) }, tone = SurfaceTone.Dark)
        Spacer(Modifier.height(FeeloTheme.spacing.lg))
        PrimaryButton(
            text = stringResource(if (confirming) Res.string.gate_save else Res.string.gate_next),
            onClick = { onEvent(GateEvent.Submit) },
            tone = PrimaryButtonTone.Accent,
            enabled = state.entered.length == PIN_LENGTH && !state.busy,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(FeeloTheme.spacing.lg))
    }
}

@Composable
private fun challengeText(challenge: AdultChallenge): String =
    stringResource(Res.string.gate_adult_question, challenge.left.toString(), challenge.right.toString())

@Composable
private fun LockMedallion() {
    val colors = FeeloTheme.colors
    Box(Modifier.size(64.dp).background(colors.darkSurfaceHigh, FeeloTheme.shapes.card), contentAlignment = Alignment.Center) {
        Icon(FeeloIcons.Lock, contentDescription = null, tint = colors.accent, size = 30.dp)
    }
}

@Composable
private fun ErrorText(text: String?) {
    // Reserve the line so the layout does not jump when an error appears.
    Box(Modifier.defaultMinSize(minHeight = 24.dp), contentAlignment = Alignment.Center) {
        if (text != null) {
            Text(
                text = text,
                style = FeeloTheme.typography.bodyMedium,
                color = FeeloTheme.colors.accent,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun ForgotLink(onClick: () -> Unit) {
    Text(
        text = stringResource(Res.string.gate_forgot),
        style = FeeloTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.Underline),
        color = FeeloTheme.colors.onDarkMuted,
        modifier = Modifier
            .defaultMinSize(minHeight = FeeloTheme.sizes.minTouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(FeeloTheme.spacing.sm),
    )
}

/** Typed answer, digits keyboard. Arabic-Indic digits are accepted too (see AdultChallenge). */
@Composable
private fun AnswerField(value: String, onValueChange: (String) -> Unit, onDone: () -> Unit) {
    val colors = FeeloTheme.colors
    val shape = FeeloTheme.shapes.cardSmall
    val label = stringResource(Res.string.gate_adult_answer_label)
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = FeeloTheme.typography.displayMedium.copy(color = colors.onDark, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(colors.darkSurface, shape)
                    .border(1.5.dp, colors.darkSurfaceHigh, shape)
                    .padding(horizontal = FeeloTheme.spacing.md),
                contentAlignment = Alignment.Center,
            ) { inner() }
        },
    )
}

// region Previews

@Preview
@Composable
private fun GatePinPreview() = FeeloPreview { GateScreen(GateUiState(entered = "123"), {}) }

@Preview
@Composable
private fun GateWrongPinPreview() = FeeloPreview {
    GateScreen(GateUiState(error = GateError.WrongPin, attemptsLeft = 2), {})
}

@Preview
@Composable
private fun GateLockedPreview() = FeeloPreview { GateScreen(GateUiState(lockoutSecondsLeft = 28), {}) }

@Preview
@Composable
private fun GateAdultCheckPreview() = FeeloPreview {
    GateScreen(GateUiState(stage = GateStage.AdultCheck(AdultChallenge(7, 14), answer = "98")), {})
}

@Preview
@Composable
private fun GateNewPinPreview() = FeeloPreview {
    GateScreen(GateUiState(stage = GateStage.NewPin(first = "1234"), entered = "12"), {})
}

// endregion
