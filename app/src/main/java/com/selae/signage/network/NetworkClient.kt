package com.selae.signage.network

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.selae.signage.config.AppConfig
import com.selae.signage.model.GameJackpot
import com.selae.signage.model.JackpotApiResponse
import com.selae.signage.model.JackpotRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * NetworkClient.kt
 *
 * Gestiona todas las comunicaciones con el webservice de InfoLot.
 * Usa OkHttp para las peticiones HTTP y Gson para serialización.
 * Implementa caché local mediante SharedPreferences.
 */
class NetworkClient(private val context: Context) {

    companion object {
        private const val TAG = "SelaeNetworkClient"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    // Cliente HTTP configurado con timeouts
    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(AppConfig.HTTP_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConfig.HTTP_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // Gson para serializar/deserializar JSON
    private val gson: Gson by lazy {
        GsonBuilder()
            .setLenient()
            .create()
    }

    // SharedPreferences para caché local
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(AppConfig.PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ============================================================
    // CLASE DE RESULTADO SELLADO (Result wrapper)
    // ============================================================

    sealed class FetchResult {
        data class Success(
            val games: List<GameJackpot>,
            val fromCache: Boolean = false
        ) : FetchResult()

        data class Error(
            val message: String,
            val cachedGames: List<GameJackpot>? = null
        ) : FetchResult()
    }

    // ============================================================
    // MÉTODO PRINCIPAL: OBTENER BOTES
    // ============================================================

    /**
     * Obtiene los próximos botes del webservice.
     * Si hay error de red, devuelve los datos de caché si existen.
     * Esta función es suspendida y debe llamarse desde una corrutina.
     */
    suspend fun fetchNextJackpots(): FetchResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Iniciando petición al webservice...")

            // Construir el cuerpo de la petición
            val requestBody = JackpotRequest(
                token = AppConfig.TOKEN,
                pass = AppConfig.PASS,
                appId = AppConfig.APP_ID
            )
            val jsonBody = gson.toJson(requestBody)
            Log.d(TAG, "Request body: $jsonBody")

            // Construir la petición HTTP POST
            val request = Request.Builder()
                .url(AppConfig.BASE_URL + AppConfig.ENDPOINT_NEXT_JACKPOTS)
                .post(jsonBody.toRequestBody(JSON_MEDIA_TYPE))
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "application/json")
                .build()

            // Ejecutar la petición
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d(TAG, "HTTP Status: ${response.code}")
            Log.d(TAG, "Response: ${responseBody?.take(500)}")

            if (!response.isSuccessful || responseBody.isNullOrBlank()) {
                Log.w(TAG, "Respuesta no exitosa o vacía: ${response.code}")
                return@withContext buildCachedResult("Error HTTP ${response.code}")
            }

            // Parsear la respuesta JSON
            val apiResponse = try {
                gson.fromJson(responseBody, JackpotApiResponse::class.java)
            } catch (parseEx: Exception) {
                Log.e(TAG, "Error parseando JSON: ${parseEx.message}")
                // Intentar parsear como array directo de GameJackpot
                tryParseAsDirectArray(responseBody)
                    ?: return@withContext buildCachedResult("Error al parsear respuesta")
            }

            val games = apiResponse.getGameList()
            if (games.isEmpty()) {
                Log.w(TAG, "Lista de juegos vacía en la respuesta")
                return@withContext buildCachedResult("Respuesta sin datos de botes")
            }

            // Guardar en caché
            saveToCache(responseBody)
            Log.d(TAG, "Datos obtenidos correctamente: ${games.size} juegos")

            FetchResult.Success(games = sortGamesByDate(games), fromCache = false)

        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "Sin conexión a internet: ${e.message}")
            buildCachedResult("Sin conexión a internet")
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(TAG, "Timeout de conexión: ${e.message}")
            buildCachedResult("Tiempo de conexión agotado")
        } catch (e: Exception) {
            Log.e(TAG, "Error inesperado: ${e.message}", e)
            buildCachedResult("Error: ${e.message}")
        }
    }

    // ============================================================
    // MÉTODOS AUXILIARES
    // ============================================================

    /**
     * Intenta parsear la respuesta directamente como array de GameJackpot
     * (algunos webservices devuelven el array sin wrapper).
     */
    private fun tryParseAsDirectArray(json: String): JackpotApiResponse? {
        return try {
            val type = object : com.google.gson.reflect.TypeToken<List<GameJackpot>>() {}.type
            val list: List<GameJackpot> = gson.fromJson(json, type)
            JackpotApiResponse(data = list)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Ordena los juegos por fecha de sorteo más próxima.
     */
    private fun sortGamesByDate(games: List<GameJackpot>): List<GameJackpot> {
        return games.sortedWith(compareBy { game ->
            val dateStr = game.getDrawDateString() ?: return@compareBy Long.MAX_VALUE
            parseDateToTimestamp(dateStr)
        })
    }

    /**
     * Convierte una fecha string a timestamp para ordenación.
     * Soporta ISO 8601 y timestamps Unix.
     */
    private fun parseDateToTimestamp(dateStr: String): Long {
        return try {
            // Primero intentar como timestamp Unix
            dateStr.toLong()
        } catch (e: NumberFormatException) {
            try {
                // Intentar como fecha ISO 8601
                val cleanDate = dateStr.replace("T", " ").take(19)
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                sdf.parse(cleanDate)?.time ?: Long.MAX_VALUE
            } catch (e2: Exception) {
                try {
                    // Solo fecha sin hora
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    sdf.parse(dateStr.take(10))?.time ?: Long.MAX_VALUE
                } catch (e3: Exception) {
                    Long.MAX_VALUE
                }
            }
        }
    }

    /**
     * Construye un FetchResult.Error con los datos de caché si existen.
     */
    private fun buildCachedResult(errorMessage: String): FetchResult {
        val cached = loadFromCache()
        return if (cached != null) {
            Log.d(TAG, "Usando caché local (${cached.size} juegos)")
            FetchResult.Error(message = errorMessage, cachedGames = cached)
        } else {
            FetchResult.Error(message = errorMessage, cachedGames = null)
        }
    }

    /**
     * Guarda la respuesta JSON cruda en SharedPreferences.
     */
    private fun saveToCache(json: String) {
        prefs.edit()
            .putString(AppConfig.PREFS_KEY_LAST_RESPONSE, json)
            .putLong(AppConfig.PREFS_KEY_LAST_UPDATE, System.currentTimeMillis())
            .apply()
        Log.d(TAG, "Datos guardados en caché")
    }

    /**
     * Carga los datos cacheados desde SharedPreferences.
     * Devuelve null si no hay caché o si falla el parseo.
     */
    fun loadFromCache(): List<GameJackpot>? {
        val json = prefs.getString(AppConfig.PREFS_KEY_LAST_RESPONSE, null) ?: return null
        return try {
            val apiResponse = gson.fromJson(json, JackpotApiResponse::class.java)
            val games = apiResponse.getGameList()
            if (games.isNotEmpty()) sortGamesByDate(games) else null
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando caché: ${e.message}")
            null
        }
    }

    /**
     * Obtiene el timestamp de la última actualización exitosa.
     */
    fun getLastUpdateTimestamp(): Long =
        prefs.getLong(AppConfig.PREFS_KEY_LAST_UPDATE, 0L)
}
