package com.selae.signage.config

/**
 * AppConfig.kt
 *
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  ARCHIVO DE CONFIGURACIÓN POR CLIENTE                        ║
 * ║  Modifica TOKEN, PASS y APP_ID antes de compilar la APK.     ║
 * ║  Cada cliente recibe una APK compilada con sus credenciales. ║
 * ╚══════════════════════════════════════════════════════════════╝
 */
object AppConfig {

    // ============================================================
    // CREDENCIALES DEL CLIENTE — REEMPLAZAR ANTES DE COMPILAR
    // ============================================================

    /** Token de autenticación proporcionado por SELAE/InfoLot */
    const val TOKEN: String = "5dc3cbd2-5f8b-4c19-a9ab-44de846eaba5"

    /** Contraseña de autenticación */
    const val PASS: String = ",54-J%aM-]eV.e94"

    /** Identificador de la aplicación en el webservice */
    const val APP_ID: String = "2218649e-3e5a-4e37-a81e-ab1956816aad"

    // ============================================================
    // CONFIGURACIÓN DE RED
    // ============================================================

    /** URL base del webservice de InfoLot */
    const val BASE_URL: String = "https://webservice.infolot.es/"

    /** Endpoint para obtener los próximos botes */
    const val ENDPOINT_NEXT_JACKPOTS: String = "ws/next-jackpots/"

    /**
     * Patrón de URL para los logos de cada juego.
     * Se formatea con String.format(LOGO_URL_PATTERN, idGame)
     */
    const val LOGO_URL_PATTERN: String = "https://webservice.infolot.es/logos/%s.png"

    // ============================================================
    // CONFIGURACIÓN DE COMPORTAMIENTO
    // ============================================================

    /** Intervalo de refresco automático en minutos (por defecto: 30) */
    const val REFRESH_INTERVAL_MINUTES: Long = 30L

    /** Tiempo de espera para conexión HTTP en segundos */
    const val HTTP_CONNECT_TIMEOUT_SECONDS: Long = 15L

    /** Tiempo de espera para lectura HTTP en segundos */
    const val HTTP_READ_TIMEOUT_SECONDS: Long = 30L

    // ============================================================
    // CLAVE PARA CACHÉ LOCAL (SharedPreferences)
    // ============================================================

    const val PREFS_NAME: String = "selae_cache"
    const val PREFS_KEY_LAST_RESPONSE: String = "last_jackpots_json"
    const val PREFS_KEY_LAST_UPDATE: String = "last_update_timestamp"
}
