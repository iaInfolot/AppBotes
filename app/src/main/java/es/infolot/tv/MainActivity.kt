package es.infolot.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.*

class MainActivity : Activity() {

    private lateinit var webView: WebView

    companion object {
        const val APP_URL = "https://iainfolot.github.io/AppBotes/infolot-tv-app.html"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()

        webView = WebView(this).apply {
            // Enable D-Pad / remote control navigation
            isFocusable          = true
            isFocusableInTouchMode = true
            requestFocus()
        }
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled                = true
            domStorageEnabled                = true
            databaseEnabled                  = true
            cacheMode                        = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode             = true
            useWideViewPort                  = true
            builtInZoomControls              = false
            displayZoomControls              = false
            allowFileAccess                  = false
            allowContentAccess               = false
            // Allow mixed content from HTTPS pages (needed for some CDN assets)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                // Retry after 30s on network error
                if (request.isForMainFrame) {
                    view.postDelayed({ view.reload() }, 30_000L)
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                // Inject JS to make all interactive elements focusable for D-Pad
                view.evaluateJavascript("""
                    (function() {
                        // Make buttons and interactive elements D-Pad focusable
                        document.querySelectorAll('button, select, input, a, [onclick]').forEach(function(el) {
                            if (!el.hasAttribute('tabindex')) {
                                el.setAttribute('tabindex', '0');
                            }
                        });
                        
                        // Focus first interactive element
                        var first = document.querySelector('button, select, input');
                        if (first) first.focus();
                        
                        // Add keyboard Enter key support for D-Pad center button
                        document.addEventListener('keydown', function(e) {
                            if (e.keyCode === 13 || e.keyCode === 23) { // Enter or D-Pad center
                                var el = document.activeElement;
                                if (el && el !== document.body) {
                                    el.click();
                                }
                            }
                        }, true);
                    })();
                """.trimIndent(), null)
            }
        }

        webView.webChromeClient = WebChromeClient()

        // Restore state or load fresh
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(APP_URL)
        }
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
        webView.requestFocus()
        hideSystemUI()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return when (keyCode) {
            // Block back button — kiosk mode
            KeyEvent.KEYCODE_BACK -> true

            // D-Pad navigation — pass through to WebView
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                webView.dispatchKeyEvent(KeyEvent(event?.action ?: KeyEvent.ACTION_DOWN, keyCode))
                true
            }

            else -> super.onKeyDown(keyCode, event)
        }
    }
}
