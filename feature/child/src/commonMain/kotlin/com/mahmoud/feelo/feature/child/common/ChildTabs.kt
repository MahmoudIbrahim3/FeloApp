package com.mahmoud.feelo.feature.child.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mahmoud.feelo.core.designsystem.component.IconButtonLarge
import com.mahmoud.feelo.core.designsystem.component.IconButtonTone
import com.mahmoud.feelo.core.designsystem.component.Icon
import com.mahmoud.feelo.core.designsystem.component.Text
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcon
import com.mahmoud.feelo.core.designsystem.icon.FeeloIcons
import com.mahmoud.feelo.core.designsystem.theme.FeeloTheme
import com.mahmoud.feelo.core.designsystem.theme.LocalWindowSize
import com.mahmoud.feelo.core.designsystem.theme.WindowSize
import com.mahmoud.feelo.feature.child.resources.Res
import com.mahmoud.feelo.feature.child.resources.child_home_lock_description
import com.mahmoud.feelo.feature.child.resources.child_nav_favorites
import com.mahmoud.feelo.feature.child.resources.child_nav_home
import org.jetbrains.compose.resources.stringResource

/** The two tabs of the child app. Everything else is a full-screen route without tabs. */
enum class ChildTab { Home, Favorites }

/**
 * Tab shell for Home and Favorites. Phone: bottom bar. Tablet: navigation rail on the start side
 * (right in RTL) with the round lock (parent mode) at the bottom. Other screens do not use this shell.
 */
@Composable
fun ChildTabsScaffold(
    selected: ChildTab,
    onSelectTab: (ChildTab) -> Unit,
    onLock: () -> Unit,
    modifier: Modifier = Modifier,
    railHeader: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    if (LocalWindowSize.current == WindowSize.Expanded) {
        Row(modifier.fillMaxSize()) {
            ChildRail(selected, onSelectTab, onLock, railHeader)
            Box(Modifier.weight(1f).fillMaxHeight()) { content() }
        }
    } else {
        Column(modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
            ChildBottomBar(selected, onSelectTab)
        }
    }
}

@Composable
private fun ChildBottomBar(selected: ChildTab, onSelectTab: (ChildTab) -> Unit) {
    val colors = FeeloTheme.colors
    Column(Modifier.fillMaxWidth().background(colors.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            ChildTab.entries.forEach { tab ->
                TabItem(
                    tab = tab,
                    selected = tab == selected,
                    onClick = { onSelectTab(tab) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ChildRail(
    selected: ChildTab,
    onSelectTab: (ChildTab) -> Unit,
    onLock: () -> Unit,
    header: @Composable () -> Unit,
) {
    val colors = FeeloTheme.colors
    Column(
        Modifier
            .width(96.dp)
            .fillMaxHeight()
            .background(colors.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(vertical = FeeloTheme.spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(FeeloTheme.spacing.sm),
    ) {
        header()
        ChildTab.entries.forEach { tab ->
            TabItem(
                tab = tab,
                selected = tab == selected,
                onClick = { onSelectTab(tab) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = FeeloTheme.spacing.xs),
            )
        }
        Spacer(Modifier.weight(1f))
        IconButtonLarge(
            icon = FeeloIcons.Lock,
            contentDescription = stringResource(Res.string.child_home_lock_description),
            onClick = onLock,
            tone = IconButtonTone.Surface,
            compact = true,
            shape = FeeloTheme.shapes.pill,
        )
    }
}

@Composable
private fun TabItem(
    tab: ChildTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FeeloTheme.colors
    val tint: Color = if (selected) colors.primary else colors.muted
    val interaction = remember { MutableInteractionSource() }
    val label = stringResource(if (tab == ChildTab.Home) Res.string.child_nav_home else Res.string.child_nav_favorites)
    val icon: FeeloIcon = when (tab) {
        ChildTab.Home -> FeeloIcons.Home
        ChildTab.Favorites -> if (selected) FeeloIcons.HeartFilled else FeeloIcons.Heart
    }
    Column(
        modifier
            .defaultMinSize(minHeight = FeeloTheme.sizes.minTouchTarget)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = FeeloTheme.spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon = icon, contentDescription = null, tint = tint, size = 26.dp)
        Spacer(Modifier.height(FeeloTheme.spacing.xxs))
        Text(text = label, style = FeeloTheme.typography.labelMedium, color = tint)
    }
}
