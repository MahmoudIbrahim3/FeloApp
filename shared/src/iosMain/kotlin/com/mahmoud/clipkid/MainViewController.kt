package com.mahmoud.clipkid

import androidx.compose.ui.window.ComposeUIViewController
import com.mahmoud.clipkid.di.initKoin
import platform.UIKit.UIViewController

@Suppress("FunctionName", "unused") // Called from Swift (ContentView.swift).
fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}
