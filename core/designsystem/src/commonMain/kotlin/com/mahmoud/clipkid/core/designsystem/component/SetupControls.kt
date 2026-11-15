package com.mahmoud.clipkid.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.preview.ClipKidPreview
import com.mahmoud.clipkid.core.designsystem.preview.ShowcaseColumn
import com.mahmoud.clipkid.core.designsystem.resources.Res
import com.mahmoud.clipkid.core.designsystem.resources.ds_age_years
import com.mahmoud.clipkid.core.designsystem.resources.ds_age_years_ten_plus
import com.mahmoud.clipkid.core.designsystem.resources.ds_avatar_color
import com.mahmoud.clipkid.core.designsystem.resources.ds_by_time
import com.mahmoud.clipkid.core.designsystem.resources.ds_minutes_short
import com.mahmoud.clipkid.core.designsystem.resources.showcase_child_1
import com.mahmoud.clipkid.core.designsystem.resources.showcase_glyph_letters
import com.mahmoud.clipkid.core.designsystem.resources.showcase_letters
import com.mahmoud.clipkid.core.designsystem.resources.showcase_stories
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.core.designsystem.theme.CoverPalettes
import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.core.model.CoverMark
import com.mahmoud.clipkid.core.model.CoverStyle
import com.mahmoud.clipkid.core.model.DailyLimit
import org.jetbrains.compose.resources.stringResource

/**
 * Avatar colors of ChildSetup.png, in order. Stored as ARGB on the profile, so this list can
 * grow without breaking saved profiles. Yellow and blue are the accent and primary tokens.
 */
object AvatarColors {
    val all: List<Long> = listOf(0xFFFFB627, 0xFF2563D9, 0xFF3DAA6E, 0xFF7B5CE0, 0xFFE2604A)
    val default: Long get() = all.first()
}

/** The first and last age of a band, e.g. 6 and 8. */
fun AgeBand.years(): Pair<Int, Int> = when (this) {
    AgeBand.A3_5 -> 3 to 5
    AgeBand.A6_8 -> 6 to 8
    AgeBand.A9_12 -> 9 to 12
}

/** "6–8 سنين", or "9–12 سنة" (Arabic uses the singular after ten). */
@Composable
fun ageBandLabel(band: AgeBand): String {
    val (from, to) = band.years()
    val res = if (to >= 10) Res.string.ds_age_years_ten_plus else Res.string.ds_age_years
    return stringResource(res, from.toString(), to.toString())
}

/** "6–8" without the unit, for sentences like "مناسبة لسن 6–8". */
fun ageBandRange(band: AgeBand): String {
    val (from, to) = band.years()
    return "$from–$to"
}

/**
 * Daily limit: 3 / 5 / 10 videos or "بالوقت". Choosing بالوقت shows a second row of minute
 * options. Used in onboarding (SetupCollections.png) and on the dashboard (Parent.png).
 */
@Composable
fun DailyLimitPicker(
    limit: DailyLimit,
    onChange: (DailyLimit) -> Unit,
    videoOptions: List<Int>,
    minuteOptions: List<Int>,
    defaultMinutes: Int,
    modifier: Modifier = Modifier,
) {
    val byTime = stringResource(Res.string.ds_by_time)
    val labels = videoOptions.map { it.toString() } + byTime
    val selected = when (limit) {
        is DailyLimit.Videos -> videoOptions.indexOf(limit.count).takeIf { it >= 0 } ?: -1
        is DailyLimit.Minutes -> videoOptions.size
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.sm)) {
        SegmentedControl(
            options = labels,
            selectedIndex = selected,
            onSelect = { index ->
                val next = if (index < videoOptions.size) {
                    DailyLimit.Videos(videoOptions[index])
                } else {
                    (limit as? DailyLimit.Minutes) ?: DailyLimit.Minutes(defaultMinutes)
                }
                if (next != limit) onChange(next)
            },
        )
        if (limit is DailyLimit.Minutes) {
            SegmentedControl(
                options = minuteOptions.map { stringResource(Res.string.ds_minutes_short, it.toString()) },
                selectedIndex = minuteOptions.indexOf(limit.minutes),
                onSelect = { index -> onChange(DailyLimit.Minutes(minuteOptions[index])) },
            )
        }
    }
}

/** Five rounded color squares; the selected one has an ink border and shows [initial]. */
@Composable
fun AvatarColorPicker(
    selected: Long,
    onSelect: (Long) -> Unit,
    initial: String,
    modifier: Modifier = Modifier,
    colors: List<Long> = AvatarColors.all,
) {
    val tokens = ClipKidTheme.colors
    Row(
        modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        colors.forEachIndexed { index, argb ->
            val isSelected = argb == selected
            val description = stringResource(Res.string.ds_avatar_color, (index + 1).toString())
            Box(
                Modifier
                    .size(56.dp)
                    .clip(ClipKidTheme.shapes.cardSmall)
                    .background(Color(argb))
                    .then(if (isSelected) Modifier.border(3.dp, tokens.ink, ClipKidTheme.shapes.cardSmall) else Modifier)
                    .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(argb) })
                    .semantics { contentDescription = description },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected && initial.isNotBlank()) {
                    Text(text = initial, style = ClipKidTheme.typography.titleLarge, color = tokens.ink)
                }
            }
        }
    }
}

/**
 * One row of the three age bands (ChildSetup.png shows the old 2×2 grid of four). Selected = ink
 * fill.
 */
@Composable
fun AgeBandPicker(
    selected: AgeBand?,
    onSelect: (AgeBand) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.sm),
    ) {
        AgeBand.entries.forEach { band ->
            AgeBandOption(ageBandLabel(band), band == selected, { onSelect(band) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AgeBandOption(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val colors = ClipKidTheme.colors
    val shape = ClipKidTheme.shapes.button
    val background by animateColorAsState(if (selected) colors.ink else colors.surface, label = "ageBg")
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .height(52.dp)
            .pressScale(interaction)
            .clip(shape)
            .background(background)
            .then(if (selected) Modifier else Modifier.border(1.5.dp, colors.border, shape))
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = if (selected) ClipKidTheme.typography.titleMedium else ClipKidTheme.typography.bodyLarge,
            color = if (selected) colors.onPrimary else colors.ink,
        )
    }
}

/**
 * A collection to switch on or off during setup (SetupCollections.png): cover color with a
 * primary border and a check when on, white with an empty ring when off.
 */
@Composable
fun CollectionPickCard(
    title: String,
    style: CoverStyle,
    selected: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = ClipKidTheme.colors
    val shape = ClipKidTheme.shapes.card
    val on = selected
    val background by animateColorAsState(if (on) Color(style.bg) else colors.surface, label = "pickBg")
    Row(
        modifier
            .height(72.dp)
            .clip(shape)
            .background(background)
            .then(if (on) Modifier.border(3.dp, colors.primary, shape) else Modifier)
            .toggleable(value = on, role = Role.Checkbox, onValueChange = onToggle)
            .padding(horizontal = ClipKidTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = ClipKidTheme.typography.titleMedium,
            color = if (on) colors.ink else colors.muted,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        when {
            on -> Box(
                Modifier.size(24.dp).background(colors.primary, ClipKidTheme.shapes.pill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(ClipKidIcons.Check, contentDescription = null, tint = colors.onPrimary, size = 16.dp)
            }
            else -> Box(Modifier.size(24.dp).border(2.dp, colors.border, ClipKidTheme.shapes.pill))
        }
    }
}

/** Rounded progress bar (Parent.png "النهارده" card). [fraction] is clamped to 0..1. */
@Composable
fun ProgressTrack(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = ClipKidTheme.colors.accent,
    track: Color = ClipKidTheme.colors.divider,
) {
    Box(modifier.fillMaxWidth().height(10.dp).background(track, ClipKidTheme.shapes.pill)) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(10.dp)
                .background(color, ClipKidTheme.shapes.pill),
        )
    }
}

// region Showcase

@Composable
fun SetupControlsShowcase() {
    var limit by remember { mutableStateOf<DailyLimit>(DailyLimit.Videos(3)) }
    var color by remember { mutableStateOf(AvatarColors.default) }
    var band by remember { mutableStateOf<AgeBand?>(AgeBand.A6_8) }
    var letters by remember { mutableStateOf(true) }
    ShowcaseColumn {
        DailyLimitPicker(limit, { limit = it }, listOf(3, 5, 10), listOf(15, 30, 45, 60), 30)
        AvatarColorPicker(color, { color = it }, initial = stringResource(Res.string.showcase_child_1).take(1))
        AgeBandPicker(band, { band = it })
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CollectionPickCard(
                title = stringResource(Res.string.showcase_letters),
                style = CoverPalettes.Letters.toCoverStyle(CoverMark.Glyph(stringResource(Res.string.showcase_glyph_letters))),
                selected = letters,
                onToggle = { letters = it },
                modifier = Modifier.weight(1f),
            )
            CollectionPickCard(
                title = stringResource(Res.string.showcase_stories),
                style = CoverPalettes.Stories.toCoverStyle(CoverMark.Icon("moon")),
                selected = false,
                onToggle = {},
                modifier = Modifier.weight(1f),
            )
        }
        ProgressTrack(0.6f, Modifier.defaultMinSize(minHeight = 10.dp))
    }
}

@Preview
@Composable
private fun SetupControlsPreview() = ClipKidPreview { SetupControlsShowcase() }

// endregion
