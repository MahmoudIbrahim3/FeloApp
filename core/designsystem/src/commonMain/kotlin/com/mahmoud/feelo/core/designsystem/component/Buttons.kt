package com.mahmoud.feelo.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.tooling.preview.Preview
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcon
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcons
import com.mahmoud.feelo.core.designsystem.preview.FeeloPreview
import com.mahmoud.feelo.core.designsystem.preview.DarkShowcasePanel
import com.mahmoud.feelo.core.designsystem.preview.ShowcaseColumn
import com.mahmoud.feelo.core.designsystem.resources.Res
import com.mahmoud.feelo.core.designsystem.resources.ds_back
import com.mahmoud.feelo.core.designsystem.resources.showcase_close
import com.mahmoud.feelo.core.designsystem.resources.showcase_enter
import com.mahmoud.feelo.core.designsystem.resources.showcase_go_home
import com.mahmoud.feelo.core.designsystem.resources.showcase_lock
import com.mahmoud.feelo.core.designsystem.resources.showcase_next
import com.mahmoud.feelo.core.designsystem.resources.showcase_open_child_mode
import com.mahmoud.feelo.core.designsystem.resources.showcase_play
import com.mahmoud.feelo.core.designsystem.resources.showcase_subscription
import com.mahmoud.feelo.core.designsystem.resources.showcase_watch_history
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import com.mahmoud.feelo.core.designsystem.theme.LocalContentColor
import com.mahmoud.feelo.core.designsystem.theme.SurfaceTone
import org.jetbrains.compose.resources.stringResource

/** Fill color of a [PrimaryButton]: blue (default), yellow (gate "دخول"), ink (DoneToday). */
enum class PrimaryButtonTone { Primary, Accent, Ink }

/**
 * Main call to action. 56dp tall (primary child action size).
 * Width comes from [modifier]; most screens use `Modifier.fillMaxWidth()`.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: PrimaryButtonTone = PrimaryButtonTone.Primary,
    icon: FeeloIcon? = null,
    enabled: Boolean = true,
) {
    val colors = FeeloTheme.colors
    val (background, content) = when (tone) {
        PrimaryButtonTone.Primary -> colors.primary to colors.onPrimary
        PrimaryButtonTone.Accent -> colors.accent to colors.onAccent
        PrimaryButtonTone.Ink -> colors.ink to colors.onPrimary
    }
    ButtonFrame(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        background = background,
        contentColor = content,
        border = null,
        elevation = 0.dp,
    ) {
        ButtonLabel(text = text, icon = icon)
    }
}

/**
 * Secondary action. [SurfaceTone.Light]: white with a soft shadow (Parent screen);
 * [SurfaceTone.Dark]: outlined on dark screens (Up Next).
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: SurfaceTone = SurfaceTone.Light,
    icon: FeeloIcon? = null,
    iconTint: Color = Color.Unspecified,
    enabled: Boolean = true,
) {
    val colors = FeeloTheme.colors
    when (tone) {
        SurfaceTone.Light -> ButtonFrame(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            background = colors.surface,
            contentColor = colors.ink,
            border = null,
            elevation = 2.dp,
        ) { ButtonLabel(text = text, icon = icon, iconTint = iconTint) }

        SurfaceTone.Dark -> ButtonFrame(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            background = Color.Transparent,
            contentColor = colors.onDark,
            border = BorderStroke(2.dp, colors.darkSurfaceHigh),
            elevation = 0.dp,
        ) { ButtonLabel(text = text, icon = icon, iconTint = iconTint) }
    }
}

/** Look of an [IconButtonLarge]. */
enum class IconButtonTone {
    /** White square, ink icon (back / lock on light screens). */
    Surface,

    /** Ink square, white icon (Parent dashboard lock). */
    Ink,

    /** Dark surface square, white icon (gate / player). */
    OnDark,
}

/**
 * Square icon button. 56dp by default (child actions); [compact] = 48dp,
 * the minimum touch target, for parent-side chrome. [shape] = `FeeloTheme.shapes.pill` makes it
 * a circle (the child Home lock, matching the round avatar).
 */
@Composable
fun IconButtonLarge(
    icon: FeeloIcon,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: IconButtonTone = IconButtonTone.Surface,
    compact: Boolean = false,
    iconTint: Color = Color.Unspecified,
    enabled: Boolean = true,
    shape: Shape = FeeloTheme.shapes.iconButton,
) {
    val colors = FeeloTheme.colors
    val sizes = FeeloTheme.sizes
    val (background, content) = when (tone) {
        IconButtonTone.Surface -> colors.surface to colors.ink
        IconButtonTone.Ink -> colors.ink to colors.onPrimary
        IconButtonTone.OnDark -> colors.darkSurfaceHigh to colors.onDark
    }
    val interaction = remember { MutableInteractionSource() }
    val size = if (compact) sizes.iconButtonCompact else sizes.iconButton
    Box(
        modifier = modifier
            .size(size)
            .pressScale(interaction)
            .then(if (tone == IconButtonTone.Surface) Modifier.softShadow(shape) else Modifier)
            .clip(shape)
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClickLabel = contentDescription,
                onClick = onClick,
            )
            .disabledFade(enabled),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon = icon,
            contentDescription = contentDescription,
            tint = if (iconTint != Color.Unspecified) iconTint else content,
            size = if (compact) 22.dp else 26.dp,
        )
    }
}

internal const val DisabledAlpha = 0.4f

/**
 * Fades a disabled control. Always the same layer, only its alpha changes: `Modifier.alpha()`
 * adds a layer below 1f and removes it at 1f, and removing it while a parent layer (the press
 * squish) stays could leave that parent drawing nothing, so a control that just became enabled
 * looked invisible while still taking taps.
 */
internal fun Modifier.disabledFade(enabled: Boolean): Modifier =
    graphicsLayer { alpha = if (enabled) 1f else DisabledAlpha }

/** The subtle card shadow used across light surfaces in the reference screens. */
internal fun Modifier.softShadow(shape: Shape, elevation: Dp = 2.dp): Modifier = shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = Color(0xFF17243A).copy(alpha = 0.08f),
    spotColor = Color(0xFF17243A).copy(alpha = 0.12f),
)

@Composable
private fun ButtonFrame(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    background: Color,
    contentColor: Color,
    border: BorderStroke?,
    elevation: Dp,
    content: @Composable RowScope.() -> Unit,
) {
    val shape = FeeloTheme.shapes.button
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = FeeloTheme.sizes.buttonHeight, minWidth = 120.dp)
            .pressScale(interaction, pressedScale = 0.98f)
            .then(if (elevation > 0.dp) Modifier.softShadow(shape, elevation) else Modifier)
            .clip(shape)
            .background(background)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .disabledFade(enabled)
            .padding(horizontal = FeeloTheme.spacing.md, vertical = FeeloTheme.spacing.sm),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }
    }
}

@Composable
private fun ButtonLabel(text: String, icon: FeeloIcon?, iconTint: Color = Color.Unspecified) {
    if (icon != null) {
        Icon(
            icon = icon,
            contentDescription = null,
            tint = if (iconTint != Color.Unspecified) iconTint else LocalContentColor.current,
            size = 22.dp,
        )
        Spacer(Modifier.width(FeeloTheme.spacing.xs))
    }
    val style = FeeloTheme.typography.button
    Text(
        text = text,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Clip,
        autoSize = TextAutoSize.StepBased(minFontSize = 13.sp, maxFontSize = style.fontSize, stepSize = 0.5.sp),
    )
}

// region Showcase

@Composable
fun PrimaryButtonShowcase() {
    ShowcaseColumn {
        PrimaryButton(stringResource(Res.string.showcase_next), onClick = {}, modifier = Modifier.fillMaxWidth())
        PrimaryButton(
            text = stringResource(Res.string.showcase_open_child_mode),
            onClick = {},
            icon = FeeloIcons.Lock,
            modifier = Modifier.fillMaxWidth(),
        )
        PrimaryButton(stringResource(Res.string.showcase_play), onClick = {}, icon = FeeloIcons.PlayFilled)
        PrimaryButton(stringResource(Res.string.showcase_next), onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
        PrimaryButton(
            text = stringResource(Res.string.showcase_go_home),
            onClick = {},
            tone = PrimaryButtonTone.Ink,
            modifier = Modifier.fillMaxWidth(),
        )
        DarkShowcasePanel {
            PrimaryButton(
                text = stringResource(Res.string.showcase_enter),
                onClick = {},
                tone = PrimaryButtonTone.Accent,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun SecondaryButtonShowcase() {
    ShowcaseColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(
                text = stringResource(Res.string.showcase_watch_history),
                onClick = {},
                icon = FeeloIcons.Chart,
                iconTint = FeeloTheme.colors.primary,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                text = stringResource(Res.string.showcase_subscription),
                onClick = {},
                icon = FeeloIcons.Star,
                iconTint = FeeloTheme.colors.accent,
                modifier = Modifier.weight(1f),
            )
        }
        DarkShowcasePanel {
            SecondaryButton(
                text = stringResource(Res.string.showcase_go_home),
                onClick = {},
                tone = SurfaceTone.Dark,
                icon = FeeloIcons.Home,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun IconButtonLargeShowcase() {
    ShowcaseColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButtonLarge(FeeloIcons.Back, stringResource(Res.string.ds_back), onClick = {})
            IconButtonLarge(FeeloIcons.Lock, stringResource(Res.string.showcase_lock), onClick = {})
            IconButtonLarge(FeeloIcons.Lock, stringResource(Res.string.showcase_lock), onClick = {}, tone = IconButtonTone.Ink)
            IconButtonLarge(FeeloIcons.Back, stringResource(Res.string.ds_back), onClick = {}, compact = true)
        }
        DarkShowcasePanel {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButtonLarge(FeeloIcons.Back, stringResource(Res.string.ds_back), onClick = {}, tone = IconButtonTone.OnDark)
                IconButtonLarge(FeeloIcons.Close, stringResource(Res.string.showcase_close), onClick = {}, tone = IconButtonTone.OnDark)
            }
        }
    }
}

@Preview
@Composable
private fun PrimaryButtonPreview() = FeeloPreview { PrimaryButtonShowcase() }

@Preview
@Composable
private fun SecondaryButtonPreview() = FeeloPreview { SecondaryButtonShowcase() }

@Preview
@Composable
private fun IconButtonLargePreview() = FeeloPreview { IconButtonLargeShowcase() }

// endregion
