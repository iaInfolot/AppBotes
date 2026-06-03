package es.infolot.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.*

class MainActivity : Activity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUI()

        // Comprobar si el dispositivo está activado
        val credentials = EncryptedPrefs.getCredentials(this)
        if (credentials == null) {
            // Sin credenciales → pantalla de activación
            startActivity(Intent(this, ActivationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }

        // Dispositivo activado → cargar la app web normalmente
        webView = WebView(this).apply {
            isFocusable           = true
            isFocusableInTouchMode = true
            requestFocus()
        }
        setContentView(webView)

        webView.settings.apply {
            javaScriptEnabled                = true
            domStorageEnabled                = true
            databaseEnabled                  = true
            cacheMode                        = WebSettings.LOAD_NO_CACHE
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode             = true
            useWideViewPort                  = true
            builtInZoomControls              = false
            displayZoomControls              = false
            allowFileAccess                  = false
            allowContentAccess               = false
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
                if (request.isForMainFrame) {
                    view.postDelayed({ view.reload() }, 30_000L)
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                view.evaluateJavascript("""
                    (function() {
                        document.querySelectorAll('button, select, input, a, [onclick]').forEach(function(el) {
                            if (!el.hasAttribute('tabindex')) el.setAttribute('tabindex', '0');
                        });
                        var first = document.querySelector('button, select, input');
                        if (first) first.focus();
                        document.addEventListener('keydown', function(e) {
                            if (e.keyCode === 13 || e.keyCode === 23) {
                                var el = document.activeElement;
                                if (el && el !== document.body) el.click();
                            }
                        }, true);
                    })();
                """.trimIndent(), null)
            }
        }

        webView.webChromeClient = WebChromeClient()

        webView.clearCache(true)
        webView.clearHistory()
        val bustUrl = AppConfig.APP_URL + "?v=" + System.currentTimeMillis()
        webView.loadUrl(bustUrl)
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
            KeyEvent.KEYCODE_BACK -> true
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
