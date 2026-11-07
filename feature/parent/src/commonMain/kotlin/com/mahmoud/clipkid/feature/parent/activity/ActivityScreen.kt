package com.mahmoud.clipkid.feature.parent.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.component.Icon
import com.mahmoud.clipkid.core.designsystem.component.IconButtonLarge
import com.mahmoud.clipkid.core.designsystem.component.IconButtonTone
import com.mahmoud.clipkid.core.designsystem.component.PrimaryButton
import com.mahmoud.clipkid.core.designsystem.component.SecondaryButton
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.component.VideoThumbnail
import com.mahmoud.clipkid.core.designsystem.component.WeekBarChart
import com.mahmoud.clipkid.core.designsystem.component.YouTubeBadgePlaceholder
import com.mahmoud.clipkid.core.designsystem.icon.ClipKidIcons
import com.mahmoud.clipkid.core.designsystem.preview.ClipKidPreview
import com.mahmoud.clipkid.core.designsystem.text.toArabicDigits
import com.mahmoud.clipkid.core.designsystem.text.withArabicDigits
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.core.domain.usecase.DayActivity
import com.mahmoud.clipkid.core.domain.usecase.WatchedToday
import com.mahmoud.clipkid.core.domain.usecase.WeeklyActivity
import com.mahmoud.clipkid.core.model.AgeBand
import com.mahmoud.clipkid.core.model.ChildProfile
import com.mahmoud.clipkid.core.model.DailyLimit
import com.mahmoud.clipkid.core.model.QuotaUnit
import com.mahmoud.clipkid.feature.parent.resources.Res
import com.mahmoud.clipkid.feature.parent.resources.act_avg_label
import com.mahmoud.clipkid.feature.parent.resources.act_avg_value
import com.mahmoud.clipkid.feature.parent.resources.act_chart_day
import com.mahmoud.clipkid.feature.parent.resources.act_chart_description
import com.mahmoud.clipkid.feature.parent.resources.act_hide
import com.mahmoud.clipkid.feature.parent.resources.act_hide_note
import com.mahmoud.clipkid.feature.parent.resources.act_limit_minutes
import com.mahmoud.clipkid.feature.parent.resources.act_limit_videos
import com.mahmoud.clipkid.feature.parent.resources.act_list_separator
import com.mahmoud.clipkid.feature.parent.resources.act_not_finished
import com.mahmoud.clipkid.feature.parent.resources.act_row_meta
import com.mahmoud.clipkid.feature.parent.resources.act_subtitle
import com.mahmoud.clipkid.feature.parent.resources.act_subtitle_today
import com.mahmoud.clipkid.feature.parent.resources.act_week_locked_button
import com.mahmoud.clipkid.feature.parent.resources.act_week_locked_message
import com.mahmoud.clipkid.feature.parent.resources.act_week_locked_title
import com.mahmoud.clipkid.feature.parent.resources.act_time_am
import com.mahmoud.clipkid.feature.parent.resources.act_time_pm
import com.mahmoud.clipkid.feature.parent.resources.act_title
import com.mahmoud.clipkid.feature.parent.resources.act_today_attribution
import com.mahmoud.clipkid.feature.parent.resources.act_today_empty
import com.mahmoud.clipkid.feature.parent.resources.act_today_title
import com.mahmoud.clipkid.feature.parent.resources.act_week_videos
import com.mahmoud.clipkid.feature.parent.resources.dash_videos_watched
import com.mahmoud.clipkid.feature.parent.resources.day_fri
import com.mahmoud.clipkid.feature.parent.resources.day_mon
import com.mahmoud.clipkid.feature.parent.resources.day_sat
import com.mahmoud.clipkid.feature.parent.resources.day_sun
import com.mahmoud.clipkid.feature.parent.resources.day_thu
import com.mahmoud.clipkid.feature.parent.resources.day_tue
import com.mahmoud.clipkid.feature.parent.resources.day_wed
import com.mahmoud.clipkid.feature.parent.resources.parent_back
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun ActivityRoute(viewModel: ActivityViewModel, onEffect: (ActivityEffect) -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.effects.collect(onEffect) }
    ActivityScreen(state, viewModel::onEvent)
}

/**
 * Activity.png: 7-day chart with the dashed limit line, and today's videos with "اخفيه".
 * Free plan: today's videos only, and a card that opens the paywall in place of the chart.
 */
@Composable
fun ActivityScreen(state: ActivityUiState, onEvent: (ActivityEvent) -> Unit, modifier: Modifier = Modifier) {
    val colors = ClipKidTheme.colors
    val spacing = ClipKidTheme.spacing
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
            val content = state as? ActivityUiState.Content
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButtonLarge(
                    icon = ClipKidIcons.Back,
                    contentDescription = stringResource(Res.string.parent_back),
                    onClick = { onEvent(ActivityEvent.BackClicked) },
                    tone = IconButtonTone.Surface,
                    compact = true,
                )
                Spacer(Modifier.size(spacing.md))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.act_title), style = ClipKidTheme.typography.displayMedium, color = colors.ink)
                    if (content != null) {
                        Text(
                            stringResource(
                                if (content.showsWeek) Res.string.act_subtitle else Res.string.act_subtitle_today,
                                content.activity.child.nickname,
                            ),
                            style = ClipKidTheme.typography.bodyMedium,
                            color = colors.muted,
                        )
                    }
                }
            }
            if (content != null) {
                val activity = content.activity
                if (content.showsWeek) {
                    ChartCard(activity)
                } else {
                    WeekLockedCard(onClick = { onEvent(ActivityEvent.SeeWeekClicked) })
                }
                TodaySection(activity.today, onHide = { onEvent(ActivityEvent.HideClicked(it)) })
                HideNote()
            }
        }
    }
}

@Composable
private fun ChartCard(activity: WeeklyActivity) {
    val colors = ClipKidTheme.colors
    val dayLabels = activity.days.map { dayName(it.date) }
    val chartDescription = stringResource(
        Res.string.act_chart_description,
        activity.days.zip(dayLabels)
            .map { (day, label) -> stringResource(Res.string.act_chart_day, label, activity.barValue(day).toArabicDigits()) }
            .joinToString(stringResource(Res.string.act_list_separator)),
    )
    Column(
        Modifier.fillMaxWidth().background(colors.surface, ClipKidTheme.shapes.card).padding(ClipKidTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(ClipKidTheme.spacing.lg),
    ) {
        Row {
            Stat(
                label = stringResource(Res.string.act_avg_label),
                value = stringResource(Res.string.act_avg_value, activity.averageMinutesPerDay.toArabicDigits()),
                modifier = Modifier.weight(1f),
            )
            Stat(
                label = stringResource(Res.string.act_week_videos),
                value = activity.weekVideos.toArabicDigits(),
                modifier = Modifier.weight(1f),
            )
        }
        WeekBarChart(
            values = activity.days.map(activity::barValue),
            labels = dayLabels,
            limit = activity.limitLine,
            limitLabel = stringResource(
                if (activity.unit == QuotaUnit.Seconds) Res.string.act_limit_minutes else Res.string.act_limit_videos,
                activity.limitLine.toArabicDigits(),
            ),
            contentDescription = chartDescription,
        )
    }
}

/** Free plan: in place of the chart. No week numbers are shown here. */
@Composable
private fun WeekLockedCard(onClick: () -> Unit) {
    val colors = ClipKidTheme.colors
    val spacing = ClipKidTheme.spacing
    Column(
        Modifier.fillMaxWidth().background(colors.surface, ClipKidTheme.shapes.card).padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(ClipKidIcons.Chart, contentDescription = null, tint = colors.primary, size = 28.dp)
            Spacer(Modifier.size(spacing.sm))
            Text(stringResource(Res.string.act_week_locked_title), style = ClipKidTheme.typography.titleMedium, color = colors.ink)
        }
        Text(stringResource(Res.string.act_week_locked_message), style = ClipKidTheme.typography.bodyMedium, color = colors.muted)
        PrimaryButton(
            text = stringResource(Res.string.act_week_locked_button),
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Column(modifier) {
        Text(label, style = ClipKidTheme.typography.bodyMedium, color = ClipKidTheme.colors.muted)
        Text(value, style = ClipKidTheme.typography.displayMedium, color = ClipKidTheme.colors.ink)
    }
}

@Composable
private fun TodaySection(rows: List<WatchedToday>, onHide: (String) -> Unit) {
    val colors = ClipKidTheme.colors
    Row(Modifier.padding(top = ClipKidTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(Res.string.act_today_title), style = ClipKidTheme.typography.titleMedium, color = colors.ink, modifier = Modifier.weight(1f))
        if (rows.isNotEmpty()) {
            Text(
                stringResource(
                    Res.string.act_today_attribution,
                    pluralStringResource(Res.plurals.dash_videos_watched, rows.size, rows.size.toArabicDigits()),
                ),
                style = ClipKidTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
    }
    if (rows.isEmpty()) {
        Text(stringResource(Res.string.act_today_empty), style = ClipKidTheme.typography.bodyLarge, color = colors.muted)
    }
    rows.forEach { row -> WatchedRow(row, onHide = { onHide(row.video.youtubeId) }) }
}

@Composable
private fun WatchedRow(row: WatchedToday, onHide: () -> Unit) {
    val colors = ClipKidTheme.colors
    val time = timeLabel(row.startedAt)
    val meta = stringResource(Res.string.act_row_meta, row.collectionTitle, time).let {
        if (row.completed) it else stringResource(Res.string.act_row_meta, it, stringResource(Res.string.act_not_finished))
    }
    Row(
        Modifier.fillMaxWidth().background(colors.surface, ClipKidTheme.shapes.cardSmall).padding(ClipKidTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VideoThumbnail(
            thumbnailUrl = row.video.thumbnailUrl,
            durationSec = null,
            youtubeBadge = { YouTubeBadgePlaceholder() },
            modifier = Modifier.size(width = 96.dp, height = 64.dp),
        )
        Spacer(Modifier.size(ClipKidTheme.spacing.md))
        Column(Modifier.weight(1f)) {
            Text(row.video.titleAr, style = ClipKidTheme.typography.titleSmall, color = colors.ink, maxLines = 2)
            Text(meta, style = ClipKidTheme.typography.bodySmall, color = colors.muted, maxLines = 1)
        }
        Spacer(Modifier.size(ClipKidTheme.spacing.sm))
        SecondaryButton(text = stringResource(Res.string.act_hide), onClick = onHide, icon = ClipKidIcons.EyeOff)
    }
}

@Composable
private fun HideNote() {
    val colors = ClipKidTheme.colors
    Row(
        Modifier.fillMaxWidth().background(colors.celebrate, ClipKidTheme.shapes.cardSmall).padding(ClipKidTheme.spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(ClipKidIcons.Info, contentDescription = null, tint = colors.ink, size = 22.dp)
        Spacer(Modifier.size(ClipKidTheme.spacing.sm))
        Text(stringResource(Res.string.act_hide_note), style = ClipKidTheme.typography.bodyMedium, color = colors.ink)
    }
}

@Composable
private fun dayName(date: LocalDate): String = stringResource(DAY_NAMES.getValue(date.dayOfWeek))

private val DAY_NAMES: Map<DayOfWeek, StringResource> = mapOf(
    DayOfWeek.MONDAY to Res.string.day_mon,
    DayOfWeek.TUESDAY to Res.string.day_tue,
    DayOfWeek.WEDNESDAY to Res.string.day_wed,
    DayOfWeek.THURSDAY to Res.string.day_thu,
    DayOfWeek.FRIDAY to Res.string.day_fri,
    DayOfWeek.SATURDAY to Res.string.day_sat,
    DayOfWeek.SUNDAY to Res.string.day_sun,
)

/** "٤:١٥ م": 12-hour clock with ص / م, Arabic-Indic digits. */
@Composable
private fun timeLabel(time: LocalTime): String {
    val hour12 = (time.hour % 12).let { if (it == 0) 12 else it }
    val clock = "$hour12:${time.minute.toString().padStart(2, '0')}".withArabicDigits()
    return stringResource(if (time.hour < 12) Res.string.act_time_am else Res.string.act_time_pm, clock)
}

// region Previews

// Sample data for previews only (never shown in the app).
private const val PREVIEW_CHILD = "يوسف"

@Preview
@Composable
private fun ActivityPreview() = ClipKidPreview {
    val child = ChildProfile("c1", PREVIEW_CHILD, 0xFFFFB627, AgeBand.A4_6, emptySet(), DailyLimit.Minutes(30))
    val today = LocalDate(2026, 11, 4)
    val days = (6 downTo 0).map { back -> DayActivity(today.minus(back, DateTimeUnit.DAY), 3, (14 + back * 2) * 60) }
    ActivityScreen(ActivityUiState.Content(WeeklyActivity(child, days, QuotaUnit.Seconds, 30, emptyList()), showsWeek = true), {})
}

@Preview
@Composable
private fun ActivityFreePreview() = ClipKidPreview {
    val child = ChildProfile("c1", PREVIEW_CHILD, 0xFFFFB627, AgeBand.A4_6, emptySet(), DailyLimit.Videos(3))
    val today = LocalDate(2026, 11, 4)
    val days = (6 downTo 0).map { back -> DayActivity(today.minus(back, DateTimeUnit.DAY), 0, 0) }
    ActivityScreen(ActivityUiState.Content(WeeklyActivity(child, days, QuotaUnit.Videos, 3, emptyList()), showsWeek = false), {})
}

@Preview
@Composable
private fun ActivityLoadingPreview() = ClipKidPreview { ActivityScreen(ActivityUiState.Loading, {}) }

// endregion
