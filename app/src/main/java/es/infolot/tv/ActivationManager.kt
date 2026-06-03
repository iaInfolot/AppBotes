package es.infolot.tv

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Gestiona la generación del código de activación y el polling
 * al servidor hasta recibir las credenciales del cliente.
 */
object ActivationManager {

    /**
     * Genera un código de activación de 6 caracteres determinista
     * a partir del Android ID del dispositivo.
     * Evita caracteres ambiguos: sin 0, O, 1, I, l.
     */
    @SuppressLint("HardwareIds")
    fun getActivationCode(context: Context): String {
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "fallback-device-id"

        val hash = MessageDigest.getInstance("SHA-256")
            .digest(androidId.toByteArray())

        val charset = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return hash.take(6)
            .map { charset[it.toInt().and(0xFF) % charset.length] }
            .joinToString("")
    }

    /**
     * Resultado posible de una consulta al servidor de activación.
     */
    sealed class ActivationResult {
        object Pending   : ActivationResult()
        object Throttled : ActivationResult()
        object NotFound  : ActivationResult()
        data class Activated(
            val token: String,
            val pass: String,
            val appId: String
        ) : ActivationResult()
        data class Error(val message: String) : ActivationResult()
    }

    /**
     * Consulta el servidor con el código de activación del dispositivo.
     * Debe llamarse desde un hilo de background (corrutina IO).
     */
    fun checkActivation(activationCode: String): ActivationResult {
        return try {
            val url = URL("${AppConfig.ACTIVATION_SERVER_URL}/$activationCode")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod  = "GET"
                connectTimeout = 10_000
                readTimeout    = 10_000
                setRequestProperty("Accept", "application/json")
            }

            when (val code = connection.responseCode) {
                200 -> {
                    val body = connection.inputStream.bufferedReader().readText()
                    val json = JSONObject(body)
                    when (json.optString("status")) {
                        "activated" -> ActivationResult.Activated(
                            token = json.getString("token"),
                            pass  = json.getString("pass"),
                            appId = json.getString("app_id")
                        )
                        else -> ActivationResult.Pending
                    }
                }
                404  -> ActivationResult.NotFound
                429  -> ActivationResult.Throttled
                else -> ActivationResult.Error("HTTP $code")
            }
        } catch (e: Exception) {
            ActivationResult.Error(e.message ?: "Error de red")
        }
    }
}
