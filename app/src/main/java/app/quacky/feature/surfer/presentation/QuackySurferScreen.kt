package app.quacky.feature.surfer.presentation

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.quacky.core.components.ToolScaffold
import app.quacky.core.designsystem.theme.QuackyBackground
import app.quacky.core.haptics.QuackyHaptics
import app.quacky.core.haptics.rememberQuackyHaptics
import app.quacky.core.registry.ToolRegistry

/**
 * JavaScript Interface bridge to connect the offline 3D Three.js Subway Surfers
 * engine with Android device haptic feedback.
 */
class QuackySurferWebBridge(private val haptics: QuackyHaptics) {
    @JavascriptInterface
    fun onHaptic(type: String) {
        when (type) {
            "tick" -> haptics.tick()
            "click" -> haptics.click()
            "heavy" -> haptics.heavy()
        }
    }
}

/**
 * Quacky 3D Subway Surfer Runner Screen.
 *
 * Runs a full, authentic 3D Subway Surfers clone built on Three.js:
 * - Real 3D WebGL perspective engine at 60 FPS
 * - 3-lane endless tracks with realistic commuter trains & graffiti tags
 * - Low hurdles (swipe up / jump), overhead clearance signs (swipe down / roll), and roof ramps
 * - Quacky Duck mascot character in yellow hoodie, backwards snapback cap & athletic sneakers
 * - Inspector and police dog pursuit
 * - 3D power-ups: Magnet, Jetpack sky trail, Super Sneakers, 2x Star, and Cyberpunk Hoverboard
 * - WebAudio synthesized sounds and real-time Android haptics
 * - 100% offline with zero network dependencies
 */
@Composable
fun QuackySurferScreen(
    viewModel: QuackySurferViewModel,
    onBack: () -> Unit,
    onOpenHowToUse: () -> Unit,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {}
) {
    val haptics = rememberQuackyHaptics()
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webViewInstance?.onPause()
                Lifecycle.Event.ON_RESUME -> webViewInstance?.onResume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewInstance?.let { wv ->
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.destroy()
            }
            webViewInstance = null
        }
    }

    ToolScaffold(
        tool = ToolRegistry.QUACKY_SURFER,
        onBack = onBack,
        isPinned = isPinned,
        onTogglePin = onTogglePin,
        onHelpClick = onOpenHowToUse,
        onResetClick = {
            haptics.click()
            webViewInstance?.reload()
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(QuackyBackground)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(AndroidColor.BLACK)
                        setLayerType(View.LAYER_TYPE_HARDWARE, null)

                        @SuppressLint("SetJavaScriptEnabled")
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = true
                            allowContentAccess = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(false)
                            mediaPlaybackRequiresUserGesture = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                return false
                            }
                        }
                        webChromeClient = WebChromeClient()

                        addJavascriptInterface(QuackySurferWebBridge(haptics), "QuackyBridge")

                        loadUrl("file:///android_asset/subway_surfer/index.html")
                        webViewInstance = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
