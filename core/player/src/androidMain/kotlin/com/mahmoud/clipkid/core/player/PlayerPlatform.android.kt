package com.mahmoud.clipkid.core.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

private const val TAG = "ClipKidPlayer"

internal actual fun logPlayerError(message: String) {
    Log.e(TAG, message)
}

@Composable
actual fun rememberIsDebugBuild(): Boolean {
    val context = LocalContext.current
    return remember(context) { context.isDebuggable() }
}

private fun Context.isDebuggable(): Boolean = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
internal actual fun PlatformPlayerView(html: String, state: YouTubePlayerState, modifier: Modifier) {
    val context = LocalContext.current
    val fullscreen = remember(context) { FullscreenHost(context.findActivity()) }
    val player = remember(context) { AndroidPlayer(context, state, fullscreen) }

    DisposableEffect(player) {
        state.attach(player)
        onDispose {
            state.detach(player)
            fullscreen.exit()
            player.destroy()
        }
    }

    LaunchedEffect(player, html) { player.load(html) }

    // The common YouTubePlayer pauses the video on ON_STOP; this also pauses the web view's work.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { player.webView.onPause() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { player.webView.onResume() }

    // The video ended (we leave for Up Next) or failed: never stay in fullscreen.
    LaunchedEffect(state.playback) {
        if (state.playback == PlaybackState.Ended || state.playback is PlaybackState.Error) fullscreen.exit()
        player.webView.keepScreenOn = state.playback == PlaybackState.Playing
    }

    BackHandler(enabled = fullscreen.isActive) { fullscreen.exit() }

    AndroidView(factory = { player.webView }, modifier = modifier)
}

/** One WebView that shows one video. Main-thread only, except the JS bridge (posted to main). */
private class AndroidPlayer(
    context: Context,
    private val state: YouTubePlayerState,
    private val fullscreen: FullscreenHost,
) : PlayerController {
    private val main = Handler(Looper.getMainLooper())
    private var destroyed = false
    private var startedPlaying = false

    @SuppressLint("SetJavaScriptEnabled")
    val webView: WebView = WebView(context).apply {
        setBackgroundColor(Color.BLACK)
        // Without this, some devices composite the <video> element's hardware surface
        // incorrectly inside Compose's AndroidView: audio plays but the picture stays black.
        setLayerType(View.LAYER_TYPE_HARDWARE, null)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // Only for the first play: the video the child just tapped starts without a second
            // tap. Set back to true once it plays (see onPageMessage).
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
            setSupportZoom(false)
            builtInZoomControls = false
            // New windows (links with target=_blank) go through onCreateWindow, where they are caught.
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = false
        }
        addJavascriptInterface(JsBridge(), PlayerBridge.ANDROID_INTERFACE)
        webViewClient = PlayerWebViewClient()
        webChromeClient = PlayerChromeClient()
        if (context.isDebuggable()) WebView.setWebContentsDebuggingEnabled(true)
    }

    fun load(html: String) {
        if (destroyed) return
        // The base URL makes the WebView send a Referer to YouTube (missing Referer = error 153).
        webView.loadDataWithBaseURL(PlayerConfig.BASE_URL, html, "text/html", "utf-8", null)
    }

    override fun evaluate(script: String) {
        if (!destroyed) webView.evaluateJavascript(script, null)
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        main.removeCallbacksAndMessages(null)
        webView.apply {
            removeJavascriptInterface(PlayerBridge.ANDROID_INTERFACE)
            stopLoading()
            loadUrl("about:blank")
            (parent as? ViewGroup)?.removeView(this)
            removeAllViews()
            destroy()
        }
    }

    private fun onPageMessage(raw: String) {
        if (destroyed) return
        state.onPageMessage(raw)
        if (!startedPlaying && state.playback == PlaybackState.Playing) {
            startedPlaying = true
            webView.settings.mediaPlaybackRequiresUserGesture = true
        }
    }

    private fun onBlockedLink(url: String) {
        if (!destroyed) state.onLinkRequested(url)
    }

    /** Called by the page on the JavaBridge thread. */
    private inner class JsBridge {
        @JavascriptInterface
        fun postMessage(json: String) {
            main.post { onPageMessage(json) }
        }
    }

    private inner class PlayerWebViewClient : WebViewClient() {
        /**
         * The page itself never navigates: every main-frame navigation (a link with target=_top,
         * a script redirect) is cancelled, and a tapped one is offered to the app. Navigations
         * inside YouTube's iframe stay with YouTube.
         * `loadDataWithBaseURL` doesn't come through here, so the player page itself still loads.
         */
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (!request.isForMainFrame) return false
            if (request.hasGesture()) onBlockedLink(request.url.toString())
            return true
        }
    }

    private inner class PlayerChromeClient : WebChromeClient() {
        /**
         * A link that opens a new window ("Watch on YouTube", logo, title, channel, end screen,
         * ad). A throwaway WebView receives it, reports the URL and is destroyed; nothing opens.
         * Windows the page tries to open without a tap are refused.
         */
        override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean {
            if (destroyed || !isUserGesture) return false
            val catcher = WebView(view.context)
            var handled = false
            fun capture(url: String?) {
                if (handled) return
                handled = true
                url?.takeIf { it != "about:blank" }?.let { main.post { onBlockedLink(it) } }
                main.post { catcher.destroy() }
            }
            catcher.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(v: WebView, request: WebResourceRequest): Boolean {
                    capture(request.url.toString())
                    return true
                }

                override fun onPageStarted(v: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                    if (url != null && url != "about:blank") {
                        v.stopLoading()
                        capture(url)
                    }
                }
            }
            // Clean up if the page never loads anything into the new window.
            main.postDelayed({ if (!handled) capture(null) }, 5_000)
            val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
            transport.webView = catcher
            resultMsg.sendToTarget()
            return true
        }

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            if (destroyed) {
                callback.onCustomViewHidden()
                return
            }
            fullscreen.show(view, callback)
        }

        override fun onHideCustomView() {
            fullscreen.onHiddenByPlayer()
        }
    }
}

/**
 * The player's own fullscreen (its fullscreen button): the video view YouTube hands over is shown
 * on top of the activity, landscape, with the system bars hidden. Undone on exit.
 */
private class FullscreenHost(private val activity: Activity?) {
    var isActive by mutableStateOf(false)
        private set

    private var view: View? = null
    private var callback: WebChromeClient.CustomViewCallback? = null
    private var previousOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    fun show(customView: View, customViewCallback: WebChromeClient.CustomViewCallback) {
        val act = activity
        if (act == null || view != null) {
            customViewCallback.onCustomViewHidden()
            return
        }
        val decor = act.window.decorView as FrameLayout
        customView.setBackgroundColor(Color.BLACK)
        decor.addView(customView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        WindowCompat.getInsetsController(act.window, decor).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        previousOrientation = act.requestedOrientation
        act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        view = customView
        callback = customViewCallback
        isActive = true
    }

    /** Back, video ended, or the player is going away: leave fullscreen and tell the WebView. */
    fun exit() {
        val cb = callback
        if (tearDown()) cb?.onCustomViewHidden()
    }

    /** The player left fullscreen itself (its own button). */
    fun onHiddenByPlayer() {
        tearDown()
    }

    private fun tearDown(): Boolean {
        val act = activity ?: return false
        val customView = view ?: return false
        (act.window.decorView as FrameLayout).removeView(customView)
        WindowCompat.getInsetsController(act.window, act.window.decorView).show(WindowInsetsCompat.Type.systemBars())
        act.requestedOrientation = previousOrientation
        view = null
        callback = null
        isActive = false
        return true
    }
}
