package es.infolot.tv

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Red de seguridad para dispositivos donde la app no está configurada como
// pantalla de inicio (Home) — ver requestSetAsHome() en MainActivity.kt,
// que es el mecanismo fiable de verdad en Android 9+. En Android 8 o
// anterior (donde no existe la restricción de "background activity start"
// que bloquea lanzar una Activity desde un BroadcastReceiver) este
// startActivity() directo sigue funcionando sin problema.
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                val launch = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(launch)
            }
        }
    }
}
