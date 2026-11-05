package com.mahmoud.clipkid.core.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSLog
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidEnterBackgroundNotification
import platform.UIKit.UIColor
import platform.WebKit.WKAudiovisualMediaTypeNone
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKNavigationType
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUIDelegateProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWindowFeatures
import platform.darwin.NSObject
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.Platform
import kotlin.native.ref.WeakReference

internal actual fun logPlayerError(message: String) {
    NSLog("ClipKidPlayer: %@", message)
}

@OptIn(ExperimentalNativeApi::class)
@Composable
actual fun rememberIsDebugBuild(): Boolean = remember { Platform.isDebugBinary }

@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun PlatformPlayerView(html: String, state: YouTubePlayerState, modifier: Modifier) {
    val player = remember { IosPlayer(state) }

    DisposableEffect(player) {
        state.attach(player)
        // Belt and braces next to the common ON_STOP: never keep playing in the background.
        val observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = UIApplicationDidEnterBackgroundNotification,
            `object` = null,
            queue = NSOperationQueue.mainQueue,
        ) { _ -> state.pause() }
        onDispose {
            NSNotificationCenter.defaultCenter.removeObserver(observer)
            UIApplication.sharedApplication.idleTimerDisabled = false
            state.detach(player)
            player.release()
        }
    }

    LaunchedEffect(player, html) { player.load(html) }

    // Keep the screen awake only while a video plays.
    LaunchedEffect(state.playback) {
        UIApplication.sharedApplication.idleTimerDisabled = state.playback == PlaybackState.Playing
    }

    UIKitView(factory = { player.webView }, modifier = modifier)
}

/** One WKWebView that shows one video. Main-thread only (WebKit calls its delegates on main). */
@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
private class IosPlayer(private val state: YouTubePlayerState) : PlayerController {
    private var released = false

    /** The first main-frame load is our own page (loadHTMLString); every later one is blocked. */
    private var pageLoadAllowed = true

    private val messageProxy = ScriptMessageProxy(WeakReference(this))
    private val navigationDelegate = NavigationDelegate(WeakReference(this))
    private val uiDelegate = UiDelegate(WeakReference(this))

    val webView: WKWebView = run {
        val configuration = WKWebViewConfiguration().apply {
            allowsInlineMediaPlayback = true
            // The video the child tapped starts as soon as the player is ready (one video per view).
            mediaTypesRequiringUserActionForPlayback = WKAudiovisualMediaTypeNone
            // YouTube's fullscreen button (element fullscreen, iOS 16.4+; used on iPad).
            preferences.elementFullscreenEnabled = true
            userContentController.addScriptMessageHandler(messageProxy, PlayerBridge.IOS_HANDLER)
        }
        WKWebView(frame = CGRectZero.readValue(), configuration = configuration).apply {
            navigationDelegate = this@IosPlayer.navigationDelegate
            UIDelegate = this@IosPlayer.uiDelegate
            // A long-press preview could open a link outside the app.
            allowsLinkPreview = false
            opaque = false
            backgroundColor = UIColor.blackColor
            scrollView.backgroundColor = UIColor.blackColor
            scrollView.scrollEnabled = false
            scrollView.bounces = false
            if (Platform.isDebugBinary) inspectable = true
        }
    }

    fun load(html: String) {
        if (released) return
        pageLoadAllowed = true
        // The base URL is sent as Referer to YouTube (missing Referer = error 153).
        webView.loadHTMLString(html, baseURL = NSURL.URLWithString(PlayerConfig.BASE_URL))
    }

    override fun evaluate(script: String) {
        if (!released) webView.evaluateJavaScript(script, completionHandler = null)
    }

    fun release() {
        if (released) return
        released = true
        webView.stopLoading()
        webView.configuration.userContentController.removeScriptMessageHandlerForName(PlayerBridge.IOS_HANDLER)
        webView.navigationDelegate = null
        webView.UIDelegate = null
        webView.loadHTMLString("", baseURL = null)
    }

    fun onPageMessage(raw: String) {
        if (!released) state.onPageMessage(raw)
    }

    fun decide(action: WKNavigationAction): WKNavigationActionPolicy {
        val url = action.request.URL?.absoluteString
        val targetFrame = action.targetFrame
        return when {
            // Inside YouTube's iframe: YouTube's own player, ads and end screen.
            targetFrame != null && !targetFrame.mainFrame -> WKNavigationActionPolicy.WKNavigationActionPolicyAllow
            // Our own page, once.
            targetFrame != null && pageLoadAllowed && action.navigationType == WKNavigationType.WKNavigationTypeOther -> {
                pageLoadAllowed = false
                WKNavigationActionPolicy.WKNavigationActionPolicyAllow
            }
            else -> {
                // A new window (target=_blank, targetFrame == null) or the page trying to leave.
                if (url != null && (targetFrame == null || action.navigationType == WKNavigationType.WKNavigationTypeLinkActivated)) {
                    state.onLinkRequested(url)
                }
                WKNavigationActionPolicy.WKNavigationActionPolicyCancel
            }
        }
    }

    fun onNewWindow(action: WKNavigationAction) {
        action.request.URL?.absoluteString?.let { if (!released) state.onLinkRequested(it) }
    }
}

/** Weak, so WKUserContentController (which retains its handlers) doesn't keep the player alive. */
@OptIn(ExperimentalNativeApi::class)
private class ScriptMessageProxy(private val player: WeakReference<IosPlayer>) : NSObject(), WKScriptMessageHandlerProtocol {
    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage,
    ) {
        val body = didReceiveScriptMessage.body as? String ?: return
        player.get()?.onPageMessage(body)
    }
}

@OptIn(ExperimentalNativeApi::class)
private class NavigationDelegate(private val player: WeakReference<IosPlayer>) : NSObject(), WKNavigationDelegateProtocol {
    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
        val policy = player.get()?.decide(decidePolicyForNavigationAction)
            ?: WKNavigationActionPolicy.WKNavigationActionPolicyCancel
        decisionHandler(policy)
    }
}

@OptIn(ExperimentalForeignApi::class, ExperimentalNativeApi::class)
private class UiDelegate(private val player: WeakReference<IosPlayer>) : NSObject(), WKUIDelegateProtocol {
    /** Never opens a new web view; the link goes to the app (and its parental gate) instead. */
    override fun webView(
        webView: WKWebView,
        createWebViewWithConfiguration: WKWebViewConfiguration,
        forNavigationAction: WKNavigationAction,
        windowFeatures: WKWindowFeatures,
    ): WKWebView? {
        player.get()?.onNewWindow(forNavigationAction)
        return null
    }
}
