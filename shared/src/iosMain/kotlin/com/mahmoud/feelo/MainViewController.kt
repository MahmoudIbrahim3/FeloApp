package com.mahmoud.feelo

import androidx.compose.ui.window.ComposeUIViewController
import com.mahmoud.feelo.di.initKoin
import platform.UIKit.UIViewController

@Suppress("FunctionName", "unused") // Called from Swift (ContentView.swift).
fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}
