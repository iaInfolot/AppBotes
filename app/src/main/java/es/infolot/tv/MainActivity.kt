package es.infolot.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.webkit.*
import org.json.JSONObject
import java.net.Inet4Address
import java.net.NetworkInterface

class MainActivity : Activity() {

    private lateinit var webView: WebView

    // Actualizado desde JS (applyOrientation, infolot-tv-app.html) cada vez que
    // cambia la orientación — 0/90/180/270. El contenido se rota ese mismo
    // ángulo por CSS, así que hay que remapear las teclas de dirección (ver
    // dispatchKeyEvent) para que la navegación nativa del WebView siga yendo
    // hacia donde el usuario espera.
    @Volatile private var rotationDegrees = 0

    private inner class OrientationBridge {
        @JavascriptInterface
        fun setRotation(degrees: Int) {
            rotationDegrees = degrees
        }

        // Consultado desde la pantalla de emparejamiento (infolot-tv-app.html,
        // populatePairingInfo) para mostrar datos de diagnóstico a soporte
        // cuando un PV no consigue activar su código — cosas que un WebView
        // no puede leer por sí mismo con JS puro.
        @JavascriptInterface
        fun getDeviceInfo(): String {
            val json = JSONObject()
            json.put("model", "${Build.MANUFACTURER} ${Build.MODEL}")
            json.put("androidVersion", Build.VERSION.RELEASE)
            json.put("sdkInt", Build.VERSION.SDK_INT)
            json.put("network", currentNetworkType())
            val wifi = wifiSignal()
            json.put("signalLevel", wifi?.first ?: -1)
            if (wifi != null) json.put("signalDbm", wifi.second)
            json.put("ip", localIpAddress())
            return json.toString()
        }
    }

    private fun currentNetworkType(): String {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "Sin conexión"
            when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Datos móviles"
                else -> "Desconocida"
            }
        } catch (e: Exception) {
            "—"
        }
    }

    // Solo intensidad de señal (RSSI/nivel), sin SSID: leer el nombre de la
    // red WiFi conectada exige permiso de ubicación desde Android 8.1, y no
    // queríamos añadir ese diálogo de permisos a un dispositivo de kiosco.
    // Devuelve (nivel 0-4, rssi en dBm), o null si no hay WiFi conectada —
    // el dibujo del icono (punto + arcos) se hace en JS a partir del nivel.
    private fun wifiSignal(): Pair<Int, Int>? {
        return try {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return null
            if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return null
            val wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager
            val rssi = wifiManager.connectionInfo.rssi
            val level = WifiManager.calculateSignalLevel(rssi, 5)
            Pair(level, rssi)
        } catch (e: Exception) {
            null
        }
    }

    private fun localIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                for (addr in intf.inetAddresses) {
                    if (addr is Inet4Address) return addr.hostAddress ?: "—"
                }
            }
        } catch (e: Exception) {
        }
        return "—"
    }

    companion object {
        const val APP_URL = "https://iainfolot.github.io/AppBotes/infolot-tv-app.html"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        webView = WebView(this).apply {
            // Enable D-Pad / remote control navigation
            isFocusable          = true
            isFocusableInTouchMode = true
            requestFocus()
        }
        webView.addJavascriptInterface(OrientationBridge(), "AndroidBridge")
        setContentView(webView)
        // Tiene que ir después de setContentView(): antes de attachar la
        // decor view a la ventana, window.insetsController puede lanzar NPE
        // en vez de devolver null (visto en algún OEM con Android 11+).
        hideSystemUI()

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
                                    // preventDefault: en <button>/<a> el WebView ya dispara un
                                    // click nativo al pulsar Enter/DPAD_CENTER; sin esto, el
                                    // click() manual de abajo se suma y duplica el evento.
                                    e.preventDefault();
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
        // Clear cache to ensure latest HTML from GitHub Pages is always loaded
        webView.clearCache(true)
        webView.clearHistory()
        // Add timestamp to bust CDN/proxy cache
        val bustUrl = APP_URL + "?v=" + System.currentTimeMillis()
        webView.loadUrl(bustUrl)
    }

    private fun hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            try {
                window.insetsController?.let { controller ->
                    controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } catch (e: Exception) {
            }
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

    // El WebView consume las teclas de dirección él mismo (navegación nativa
    // por foco) antes de que lleguen a onKeyDown, así que el remapeo tiene que
    // interceptarse aquí — dispatchKeyEvent es lo primero que Android llama,
    // antes de entregar el evento a la vista enfocada.
    //
    // El contenido está rotado por CSS según rotationDegrees, así que la
    // navegación nativa del WebView (que decide el siguiente foco según la
    // posición ya rotada en pantalla) queda girada ese mismo ángulo respecto
    // a lo que el usuario espera. Se remapean aquí las direcciones para
    // compensar ese giro.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (rotationDegrees != 0) {
            val remapped = remapKeyForRotation(event.keyCode, rotationDegrees)
            if (remapped != event.keyCode) {
                webView.dispatchKeyEvent(
                    KeyEvent(event.downTime, event.eventTime, event.action, remapped, event.repeatCount)
                )
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // Cada 90° de giro desplaza una posición en este orden cíclico (arriba →
    // derecha → abajo → izquierda → arriba...). degrees/90 da cuántas
    // posiciones desplazar: 1 para 90°, 2 para 180°, 3 para 270°.
    private fun remapKeyForRotation(keyCode: Int, degrees: Int): Int {
        val order = listOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT
        )
        val idx = order.indexOf(keyCode)
        if (idx == -1) return keyCode
        val shift = (degrees / 90) % 4
        return order[(idx + shift) % 4]
    }
}
