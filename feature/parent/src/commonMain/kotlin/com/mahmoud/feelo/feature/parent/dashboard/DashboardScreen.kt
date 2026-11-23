package com.mahmoud.feelo.feature.parent.dashboard

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.mahmoud.feelo.core.designsystem.component.Chip
import com.mahmoud.feelo.core.designsystem.component.DailyLimitPicker
import com.mahmoud.feelo.core.designsystem.component.Icon
import com.mahmoud.feelo.core.designsystem.component.IconButtonLarge
import com.mahmoud.feelo.core.designsystem.component.IconButtonTone
import com.mahmoud.feelo.core.designsystem.component.ProgressTrack
import com.mahmoud.feelo.core.designsystem.component.SecondaryButton
import com.mahmoud.feelo.core.designsystem.component.Text
import com.mahmoud.feelo.core.designsystem.component.Toggle
import com.mahmoud.feelo.core.designsystem.component.ageBandLabel
import com.mahmoud.feelo.core.designsystem.component.ageBandRange
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcon
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcons
import com.mahmoud.feelo.core.designsystem.preview.FeeloPreview
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import com.mahmoud.feelo.core.domain.usecase.DailyLimitOptions
import com.mahmoud.feelo.core.domain.usecase.GetDailyQuotaUseCase
import com.mahmoud.feelo.core.domain.usecase.MINUTES_PER_EXTRA_VIDEO
import com.mahmoud.feelo.core.model.AgeBand
import com.mahmoud.feelo.core.model.ChildProfile
import com.mahmoud.feelo.core.model.Collection
import com.mahmoud.feelo.core.model.CoverMark
import com.mahmoud.feelo.core.model.CoverStyle
import com.mahmoud.feelo.core.model.DailyLimit
import com.mahmoud.feelo.core.model.DailyQuota
import com.mahmoud.feelo.feature.parent.resources.Res
import com.mahmoud.feelo.feature.parent.resources.dash_about
import com.mahmoud.feelo.feature.parent.resources.dash_activity
import com.mahmoud.feelo.feature.parent.resources.dash_add_child
import com.mahmoud.feelo.feature.parent.resources.dash_child_chip
import com.mahmoud.feelo.feature.parent.resources.dash_child_locked
import com.mahmoud.feelo.feature.parent.resources.dash_collections_caption
import com.mahmoud.feelo.feature.parent.resources.dash_collections_title
import com.mahmoud.feelo.feature.parent.resources.dash_grant_minutes
import com.mahmoud.feelo.feature.parent.resources.dash_grant_one_video
import com.mahmoud.feelo.feature.parent.resources.dash_grant_three_videos
import com.mahmoud.feelo.feature.parent.resources.dash_grant_title
import com.mahmoud.feelo.feature.parent.resources.dash_granted
import com.mahmoud.feelo.feature.parent.resources.dash_limit_minutes_caption
import com.mahmoud.feelo.feature.parent.resources.dash_limit_title
import com.mahmoud.feelo.feature.parent.resources.dash_limit_videos_caption
import com.mahmoud.feelo.feature.parent.resources.dash_lock
import com.mahmoud.feelo.feature.parent.resources.dash_minutes_of
import com.mahmoud.feelo.feature.parent.resources.dash_minutes_watched
import com.mahmoud.feelo.feature.parent.resources.dash_no_children
import com.mahmoud.feelo.feature.parent.resources.dash_subscribed
import com.mahmoud.feelo.feature.parent.resources.dash_subscription
import com.mahmoud.feelo.feature.parent.resources.dash_subtitle
import com.mahmoud.feelo.feature.parent.resources.dash_title
import com.mahmoud.feelo.feature.parent.resources.dash_today
import com.mahmoud.feelo.feature.parent.resources.dash_videos_of
import com.mahmoud.feelo.feature.parent.resources.dash_videos_watched
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DashboardRoute(viewModel: DashboardViewModel, onEffect: (DashboardEffect) -> Unit) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(viewModel) { viewModel.effects.collect(onEffect) }
    DashboardScreen(state, viewModel::onEvent)
}

/** Parent.png. One column, centered and width-capped on tablets. */
@Composable
fun DashboardScreen(state: DashboardUiState, onEvent: (DashboardEvent) -> Unit, modifier: Modifier = Modifier) {
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
            Header(onLock = { onEvent(DashboardEvent.LockClicked) })
            when (state) {
                DashboardUiState.Loading -> Unit
                DashboardUiState.NoChildren -> {
                    Text(stringResource(Res.string.dash_no_children), style = FeeloTheme.typography.bodyLarge, color = colors.muted)
                    AddChildButton { onEvent(DashboardEvent.AddChildClicked) }
                }
                is DashboardUiState.Content -> Content(state, onEvent)
            }
        }
    }
}

@Composable
private fun Header(onLock: () -> Unit) {
    val colors = FeeloTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.dash_title), style = FeeloTheme.typography.displayMedium, color = colors.ink)
            Text(stringResource(Res.string.dash_subtitle), style = FeeloTheme.typography.bodyMedium, color = colors.muted)
        }
        IconButtonLarge(
            icon = FeeloIcons.Lock,
            contentDescription = stringResource(Res.string.dash_lock),
            onClick = onLock,
            tone = IconButtonTone.Ink,
        )
    }
}

@Composable
private fun Content(state: DashboardUiState.Content, onEvent: (DashboardEvent) -> Unit) {
    ChildSwitcher(state, onEvent)
    TodayCard(state.quota, onGrant = { onEvent(DashboardEvent.GrantExtra(it)) })
    Card {
        CardHeader(
            title = stringResource(Res.string.dash_limit_title),
            caption = stringResource(
                if (state.selected.dailyLimit is DailyLimit.Minutes) Res.string.dash_limit_minutes_caption else Res.string.dash_limit_videos_caption,
            ),
        )
        DailyLimitPicker(
            limit = state.selected.dailyLimit,
            onChange = { onEvent(DashboardEvent.LimitChanged(it)) },
            videoOptions = DailyLimitOptions.videos,
            minuteOptions = DailyLimitOptions.minutes,
            defaultMinutes = DailyLimitOptions.DEFAULT_MINUTES,
        )
    }
    Card {
        CardHeader(
            title = stringResource(Res.string.dash_collections_title),
            caption = stringResource(Res.string.dash_collections_caption, ageBandRange(state.selected.ageBand)),
        )
        state.collections.forEachIndexed { index, toggle ->
            if (index > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(FeeloTheme.colors.divider))
            CollectionRow(toggle) { on -> onEvent(DashboardEvent.CollectionToggled(toggle.collection.id, on)) }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.md)) {
        LinkCard(FeeloIcons.Chart, FeeloTheme.colors.primary, stringResource(Res.string.dash_activity), Modifier.weight(1f)) {
            onEvent(DashboardEvent.ActivityClicked)
        }
        if (state.isSubscribed) {
            LinkCard(FeeloIcons.Check, FeeloTheme.colors.success, stringResource(Res.string.dash_subscribed), Modifier.weight(1f)) {
                onEvent(DashboardEvent.SubscriptionClicked)
            }
        } else {
            LinkCard(FeeloIcons.Star, FeeloTheme.colors.accent, stringResource(Res.string.dash_subscription), Modifier.weight(1f)) {
                onEvent(DashboardEvent.SubscriptionClicked)
            }
        }
    }
    LinkCard(FeeloIcons.Info, FeeloTheme.colors.muted, stringResource(Res.string.dash_about), Modifier.fillMaxWidth()) {
        onEvent(DashboardEvent.AboutClicked)
    }
}

/** Child chips. Free plan: children after the first carry a lock and open the paywall. */
@Composable
private fun ChildSwitcher(state: DashboardUiState.Content, onEvent: (DashboardEvent) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.sm), verticalAlignment = Alignment.CenterVertically) {
        items(state.children, key = { it.id }) { child ->
            val locked = child.id in state.lockedChildIds
            val label = stringResource(Res.string.dash_child_chip, child.nickname, ageBandLabel(child.ageBand))
            val lockedLabel = stringResource(Res.string.dash_child_locked, label)
            Chip(
                text = label,
                selected = child.id == state.selected.id,
                onClick = { onEvent(DashboardEvent.SelectChild(child.id)) },
                leadingIcon = if (locked) FeeloIcons.Lock else null,
                modifier = if (locked) Modifier.semantics { contentDescription = lockedLabel } else Modifier,
            )
        }
        if (state.addChild != AddChildAction.Hidden) {
            item { AddChildButton { onEvent(DashboardEvent.AddChildClicked) } }
        }
    }
}

/** Dashed "+" circle at the end of the child chips. */
@Composable
private fun AddChildButton(onClick: () -> Unit) {
    val colors = FeeloTheme.colors
    val description = stringResource(Res.string.dash_add_child)
    Box(
        Modifier
            .size(FeeloTheme.sizes.childTouchTarget)
            .drawBehind {
                drawCircle(
                    color = colors.border,
                    radius = size.minDimension / 2 - 1.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(FeeloIcons.Plus, contentDescription = null, tint = colors.muted)
    }
}

/** "النهارده": usage against the limit, and +1 / +3 for today. */
@Composable
private fun TodayCard(quota: DailyQuota, onGrant: (Int) -> Unit) {
    val colors = FeeloTheme.colors
    val minutesMode = quota.limit is DailyLimit.Minutes
    val minutesWatched = quota.secondsWatched / 60
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(Res.string.dash_today), style = FeeloTheme.typography.titleMedium, color = colors.ink, modifier = Modifier.weight(1f))
            Text(
                text = if (minutesMode) {
                    pluralStringResource(Res.plurals.dash_videos_watched, quota.videosWatched, quota.videosWatched.toString())
                } else {
                    stringResource(Res.string.dash_minutes_watched, minutesWatched.toString())
                },
                style = FeeloTheme.typography.bodyMedium,
                color = colors.muted,
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            val (big, small) = if (minutesMode) {
                (quota.used / 60) to stringResource(Res.string.dash_minutes_of, (quota.total / 60).toString())
            } else {
                quota.used to stringResource(Res.string.dash_videos_of, quota.total.toString())
            }
            Text(big.toString(), style = FeeloTheme.typography.displayLarge, color = colors.ink)
            Spacer(Modifier.size(FeeloTheme.spacing.xs))
            Text(small, style = FeeloTheme.typography.bodyLarge, color = colors.muted, modifier = Modifier.padding(bottom = 6.dp))
        }
        ProgressTrack(fraction = if (quota.total <= 0) 1f else quota.used.toFloat() / quota.total)
        Spacer(Modifier.height(FeeloTheme.spacing.xs))
        Text(
            text = if (quota.extraVideosGranted > 0) {
                stringResource(Res.string.dash_granted, grantedAmount(quota.extraVideosGranted, minutesMode))
            } else {
                stringResource(Res.string.dash_grant_title)
            },
            style = FeeloTheme.typography.bodyMedium,
            color = colors.muted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.sm)) {
            SecondaryButton(text = grantLabel(1, minutesMode), onClick = { onGrant(1) }, modifier = Modifier.weight(1f))
            SecondaryButton(text = grantLabel(3, minutesMode), onClick = { onGrant(3) }, modifier = Modifier.weight(1f))
        }
    }
}

/** "+1 فيديو" / "+3 فيديوهات" (the only two grants), or in minutes mode "+10 دقيقة" / "+30 دقيقة". */
@Composable
private fun grantLabel(videos: Int, minutesMode: Boolean): String = when {
    minutesMode -> stringResource(Res.string.dash_grant_minutes, (videos * MINUTES_PER_EXTRA_VIDEO).toString())
    videos == 1 -> stringResource(Res.string.dash_grant_one_video)
    else -> stringResource(Res.string.dash_grant_three_videos)
}

/** Extras already granted today: "3 فيديوهات" or "30 دقيقة". */
@Composable
private fun grantedAmount(videos: Int, minutesMode: Boolean): String =
    if (minutesMode) {
        stringResource(Res.string.dash_minutes_watched, (videos * MINUTES_PER_EXTRA_VIDEO).toString())
    } else {
        pluralStringResource(Res.plurals.dash_videos_watched, videos, videos.toString())
    }

/** One collection switch. */
@Composable
private fun CollectionRow(toggle: CollectionToggle, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            toggle.collection.titleAr,
            style = FeeloTheme.typography.bodyLarge,
            color = FeeloTheme.colors.ink,
            modifier = Modifier.weight(1f),
        )
        Toggle(checked = toggle.allowed, onCheckedChange = onChange)
    }
}

@Composable
private fun LinkCard(icon: FeeloIcon, tint: Color, text: String, modifier: Modifier, onClick: () -> Unit) {
    val colors = FeeloTheme.colors
    Row(
        modifier
            .defaultMinSize(minHeight = 64.dp)
            .background(colors.surface, FeeloTheme.shapes.cardSmall)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = FeeloTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, size = 22.dp)
        Spacer(Modifier.size(FeeloTheme.spacing.xs))
        Text(text, style = FeeloTheme.typography.titleSmall, color = colors.ink)
    }
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

@Composable
private fun CardHeader(title: String, caption: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = FeeloTheme.typography.titleMedium, color = FeeloTheme.colors.ink, modifier = Modifier.weight(1f))
        Text(caption, style = FeeloTheme.typography.bodyMedium, color = FeeloTheme.colors.muted)
    }
}

// region Previews

// Sample data for previews only (never shown in the app).
private const val PREVIEW_CHILD = "يوسف"
private const val PREVIEW_COLLECTION = "الحروف"

private val previewChild = ChildProfile("c1", PREVIEW_CHILD, 0xFFFFB627, AgeBand.A6_8, setOf("letters"), DailyLimit.Minutes(30))
private val previewContent = DashboardUiState.Content(
    children = listOf(previewChild, previewChild.copy(id = "c2", ageBand = AgeBand.A9_12)),
    selected = previewChild,
    quota = GetDailyQuotaUseCase.computeQuota(DailyLimit.Minutes(30), 0, emptyList()).copy(used = 18 * 60, videosWatched = 2, secondsWatched = 18 * 60),
    collections = listOf(
        CollectionToggle(Collection("letters", "letters", PREVIEW_COLLECTION, CoverStyle(0xFFFFE6B0, 0xFF8A5A00, CoverMark.Glyph("أ")), setOf(AgeBand.A6_8), 1), true),
        CollectionToggle(Collection("stories", "stories", PREVIEW_COLLECTION, CoverStyle(0xFFFFE3C7, 0xFFA3530E, CoverMark.Icon("moon")), setOf(AgeBand.A6_8), 2), false),
    ),
    lockedChildIds = setOf("c2"),
    addChild = AddChildAction.Subscribe,
)

@Preview
@Composable
private fun DashboardPreview() = FeeloPreview { DashboardScreen(previewContent, {}) }

@Preview
@Composable
private fun DashboardLoadingPreview() = FeeloPreview { DashboardScreen(DashboardUiState.Loading, {}) }

@Preview
@Composable
private fun DashboardNoChildrenPreview() = FeeloPreview { DashboardScreen(DashboardUiState.NoChildren, {}) }

// endregion
