package com.mahmoud.clipkid.core.designsystem.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mahmoud.clipkid.core.designsystem.theme.ClipKidTheme
import com.mahmoud.clipkid.core.designsystem.theme.LocalContentColor
import com.mahmoud.clipkid.core.designsystem.theme.ProvideRtl
import androidx.compose.runtime.CompositionLocalProvider

/** Wraps previews the same way the app root does: RTL + theme + screen background. */
@Composable
fun ClipKidPreview(content: @Composable () -> Unit) {
    ProvideRtl {
        ClipKidTheme {
            Box(Modifier.background(ClipKidTheme.colors.background).padding(16.dp)) {
                content()
            }
        }
    }
}

/** Vertical stack used by component showcases. */
@Composable
fun ShowcaseColumn(
    modifier: Modifier = Modifier,
    horizontalAlignmentCenter: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = if (horizontalAlignmentCenter) Alignment.CenterHorizontally else Alignment.Start,
        content = content,
    )
}

/** Dark (player / gate) panel used by showcases of dark component variants. */
@Composable
fun DarkShowcasePanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    CompositionLocalProvider(LocalContentColor provides ClipKidTheme.colors.onDark) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(ClipKidTheme.colors.darkBackgroundAlt, ClipKidTheme.shapes.card)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}
