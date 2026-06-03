package es.infolot.tv

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Wrapper para EncryptedSharedPreferences.
 * Almacena las credenciales SELAE de forma segura con AES-256-GCM
 * gestionado por Android Keystore.
 */
object EncryptedPrefs {

    private const val PREFS_FILE = "infolot_secure_prefs"
    private const val KEY_TOKEN   = "credential_token"
    private const val KEY_PASS    = "credential_pass"
    private const val KEY_APP_ID  = "credential_app_id"

    private fun getPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Guarda las tres credenciales recibidas tras la activación. */
    fun saveCredentials(context: Context, token: String, pass: String, appId: String) {
        getPrefs(context).edit()
            .putString(KEY_TOKEN,  token)
            .putString(KEY_PASS,   pass)
            .putString(KEY_APP_ID, appId)
            .apply()
    }

    /** Devuelve las credenciales si existen, o null si el dispositivo no está activado. */
    fun getCredentials(context: Context): Credentials? {
        val prefs = getPrefs(context)
        val token  = prefs.getString(KEY_TOKEN,  null)
        val pass   = prefs.getString(KEY_PASS,   null)
        val appId  = prefs.getString(KEY_APP_ID, null)
        return if (!token.isNullOrBlank() && !pass.isNullOrBlank() && !appId.isNullOrBlank()) {
            Credentials(token, pass, appId)
        } else null
    }

    /** Elimina todas las credenciales (útil para revocar o resetear el dispositivo). */
    fun clearCredentials(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    data class Credentials(
        val token: String,
        val pass: String,
        val appId: String
    )
}
