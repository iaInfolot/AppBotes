package com.selae.signage

import android.app.Application
import android.util.Log

/**
 * SelaeApplication.kt
 *
 * Clase Application personalizada.
 * Punto de entrada global de la app; se puede usar para inicializar
 * librerías de logging, crash reporting, etc.
 */
class SelaeApplication : Application() {

    companion object {
        private const val TAG = "SelaeApplication"
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "App SELAE Signage iniciada")
    }
}
