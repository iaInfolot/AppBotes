# proguard-rules.pro

# Puente JS↔Kotlin del WebView (AndroidBridge en MainActivity.kt) — R8 no ve
# las llamadas que le hace el JS a estos métodos (son solo por nombre, en
# tiempo de ejecución), así que sin este keep los elimina o renombra por no
# tener referencias estáticas, rompiendo window.AndroidBridge.* en el HTML.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
