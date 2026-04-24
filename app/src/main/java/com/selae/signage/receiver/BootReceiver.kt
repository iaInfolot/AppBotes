package com.selae.signage.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.selae.signage.ui.MainActivity

/**
 * BootReceiver.kt
 *
 * BroadcastReceiver que se activa cuando el dispositivo arranca.
 * Lanza automáticamente la MainActivity de señalización.
 *
 * Registrado en AndroidManifest.xml con el intent BOOT_COMPLETED.
 * Requiere el permiso RECEIVE_BOOT_COMPLETED.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SelaeBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        Log.d(TAG, "Intent recibido: $action")

        // Verificar que el intent es de arranque del sistema
        val isBootIntent = action == Intent.ACTION_BOOT_COMPLETED
            || action == "android.intent.action.QUICKBOOT_POWERON"
            || action == "com.htc.intent.action.QUICKBOOT_POWERON"

        if (!isBootIntent) {
            Log.w(TAG, "Intent ignorado (no es BOOT_COMPLETED): $action")
            return
        }

        Log.i(TAG, "Sistema arrancado — lanzando MainActivity de señalización")

        try {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                // Flags para lanzar desde fuera de una Activity
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                // Asegurar que no haya animación de entrada
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            context.startActivity(launchIntent)
            Log.i(TAG, "MainActivity lanzada correctamente")
        } catch (e: Exception) {
            Log.e(TAG, "Error al lanzar MainActivity: ${e.message}", e)
        }
    }
}
