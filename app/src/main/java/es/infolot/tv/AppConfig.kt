package es.infolot.tv

object AppConfig {
    // URL base del servidor de activación de Infolot
    const val ACTIVATION_SERVER_URL = "https://[dominio-infolot]/api/v1/activate"

    // Intervalo normal de polling mientras espera activación (30 segundos)
    const val POLLING_INTERVAL_MS = 30_000L

    // Intervalo reducido tras recibir HTTP 429 (5 minutos)
    const val POLLING_INTERVAL_THROTTLED_MS = 300_000L

    // URL de la app web (pantalla de botes)
    const val APP_URL = "https://iainfolot.github.io/AppBotes/infolot-tv-app.html"

    // Datos de contacto de soporte mostrados en la pantalla de activación
    const val SUPPORT_PHONE = "966 295 825"
    const val SUPPORT_EMAIL = "soporte@infolot.es"
}
