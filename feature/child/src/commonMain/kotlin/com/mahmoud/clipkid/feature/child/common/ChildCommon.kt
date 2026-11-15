package com.mahmoud.clipkid.feature.child.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.component.Text
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.core.model.DailyQuota
import com.mahmoud.clipkid.core.model.QuotaUnit
import com.mahmoud.clipkid.feature.child.resources.Res
import com.mahmoud.clipkid.feature.child.resources.child_quota_minutes_pill
import com.mahmoud.clipkid.feature.child.resources.child_quota_minutes_remaining
import com.mahmoud.clipkid.feature.child.resources.child_quota_none
import com.mahmoud.clipkid.feature.child.resources.child_quota_pill
import com.mahmoud.clipkid.feature.child.resources.child_quota_remaining_many
import com.mahmoud.clipkid.feature.child.resources.child_quota_remaining_one
import org.jetbrains.compose.resources.stringResource

/** "2 من 3" for videos, "4 من 10 دقيقة" for minutes. */
@Composable
fun quotaLabel(quota: DailyQuota): String = when (quota.unit) {
    QuotaUnit.Videos -> stringResource(
        Res.string.child_quota_pill,
        quota.used.toString(),
        quota.total.toString(),
    )
    QuotaUnit.Seconds -> stringResource(
        Res.string.child_quota_minutes_pill,
        (quota.used / 60).toString(),
        (quota.total / 60).toString(),
    )
}

/** "فاضلك فيديو واحد" / "فاضلك 2 فيديو" / "خلصت فيديوهات النهارده". */
@Composable
fun remainingLabel(quota: DailyQuota): String {
    val remaining = quota.remaining
    if (quota.isReached) return stringResource(Res.string.child_quota_none)
    return when (quota.unit) {
        QuotaUnit.Videos ->
            if (remaining == 1) stringResource(Res.string.child_quota_remaining_one)
            else stringResource(Res.string.child_quota_remaining_many, remaining.toString())
        QuotaUnit.Seconds ->
            stringResource(Res.string.child_quota_minutes_remaining, (remaining / 60).coerceAtLeast(1).toString())
    }
}

/** Round avatar: the child's first letter on their chosen color. */
@Composable
fun ChildAvatar(nickname: String, avatarColor: Long, size: Dp = 56.dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .background(Color(avatarColor), ClipKidTheme.shapes.iconButton),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = nickname.take(1),
            style = ClipKidTheme.typography.displayMedium,
            color = ClipKidTheme.colors.ink,
        )
    }
}
