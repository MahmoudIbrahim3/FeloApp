package com.mahmoud.feelo.core.designsystem.theme

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Compact = phone (bottom bar). Expanded = tablet (navigation rail, larger cards, more columns). */
enum class WindowSize { Compact, Expanded }

/** Width from which the layout is [WindowSize.Expanded]. */
val ExpandedWindowMinWidth: Dp = 840.dp

val LocalWindowSize = compositionLocalOf { WindowSize.Compact }

/** Measures the window once at the root and provides [LocalWindowSize] to everything below it. */
@Composable
fun ProvideWindowSize(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val size = if (maxWidth >= ExpandedWindowMinWidth) WindowSize.Expanded else WindowSize.Compact
        CompositionLocalProvider(LocalWindowSize provides size) {
            content()
        }
    }
}
