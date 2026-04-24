package com.selae.signage.model

import com.google.gson.annotations.SerializedName

/**
 * JackpotModels.kt
 *
 * Modelos de datos para parsear la respuesta del webservice de InfoLot.
 * Las anotaciones @SerializedName mapean los campos del JSON de la API.
 */

// ============================================================
// RESPUESTA RAÍZ DEL WEBSERVICE
// ============================================================

/**
 * Wrapper de la respuesta completa del endpoint /ws/next-jackpots/
 * Ajusta los nombres de campo según la respuesta real del webservice.
 */
data class JackpotApiResponse(
    @SerializedName("status")
    val status: String? = null,

    @SerializedName("error")
    val error: String? = null,

    // La lista de juegos puede venir bajo distintas claves; se definen las más comunes.
    @SerializedName("data")
    val data: List<GameJackpot>? = null,

    @SerializedName("games")
    val games: List<GameJackpot>? = null,

    @SerializedName("botes")
    val botes: List<GameJackpot>? = null,

    @SerializedName("results")
    val results: List<GameJackpot>? = null
) {
    /**
     * Devuelve la lista de juegos independientemente del nombre de clave.
     */
    fun getGameList(): List<GameJackpot> =
        data ?: games ?: botes ?: results ?: emptyList()
}

// ============================================================
// MODELO DE JUEGO / BOTE
// ============================================================

/**
 * Representa un juego de SELAE con su próximo sorteo y bote.
 */
data class GameJackpot(

    /** Identificador único del juego (usado para construir la URL del logo) */
    @SerializedName("id_game")
    val idGame: String = "",

    /** Nombre del juego (ej: "EuroMillones", "La Primitiva") */
    @SerializedName("name")
    val name: String = "",

    /**
     * Fecha del próximo sorteo.
     * Puede venir como String ISO 8601 ("2024-03-28T20:00:00")
     * o como timestamp ("1711656000").
     */
    @SerializedName("draw_date")
    val drawDate: String? = null,

    @SerializedName("next_draw_date")
    val nextDrawDate: String? = null,

    @SerializedName("fecha")
    val fecha: String? = null,

    @SerializedName("date")
    val date: String? = null,

    /** Importe del bote en euros (puede ser Long o Double según el webservice) */
    @SerializedName("jackpot")
    val jackpot: Double = 0.0,

    @SerializedName("bote")
    val bote: Double = 0.0,

    @SerializedName("amount")
    val amount: Double = 0.0,

    @SerializedName("importe")
    val importe: Double = 0.0,

    /** Lista de premios adicionales (ej: "El Millón" en EuroMillones) */
    @SerializedName("addons")
    val addons: List<JackpotAddon>? = null,

    @SerializedName("extras")
    val extras: List<JackpotAddon>? = null,

    @SerializedName("premios_adicionales")
    val premiosAdicionales: List<JackpotAddon>? = null,

    /** Orden de visualización (opcional) */
    @SerializedName("order")
    val order: Int = 0
) {
    /**
     * Devuelve el importe del bote usando el primer campo no nulo.
     */
    fun getJackpotAmount(): Double =
        when {
            jackpot > 0 -> jackpot
            bote > 0 -> bote
            amount > 0 -> amount
            importe > 0 -> importe
            else -> 0.0
        }

    /**
     * Devuelve la fecha del sorteo usando el primer campo no nulo.
     */
    fun getDrawDateString(): String? =
        drawDate ?: nextDrawDate ?: fecha ?: date

    /**
     * Devuelve la lista de addons usando el primer campo no nulo.
     */
    fun getAddonList(): List<JackpotAddon> =
        addons ?: extras ?: premiosAdicionales ?: emptyList()

    /**
     * Construye la URL del logo para este juego.
     */
    fun getLogoUrl(): String =
        String.format(com.selae.signage.config.AppConfig.LOGO_URL_PATTERN, idGame)
}

// ============================================================
// MODELO DE ADDON (PREMIO ADICIONAL)
// ============================================================

/**
 * Premio adicional dentro de un juego (ej: "El Millón", "Joker").
 */
data class JackpotAddon(

    @SerializedName("id_addon")
    val idAddon: String? = null,

    @SerializedName("name")
    val name: String = "",

    @SerializedName("nombre")
    val nombre: String? = null,

    @SerializedName("jackpot")
    val jackpot: Double = 0.0,

    @SerializedName("amount")
    val amount: Double = 0.0,

    @SerializedName("importe")
    val importe: Double = 0.0
) {
    fun getAddonName(): String = name.ifBlank { nombre ?: "" }

    fun getAddonAmount(): Double =
        when {
            jackpot > 0 -> jackpot
            amount > 0 -> amount
            importe > 0 -> importe
            else -> 0.0
        }
}

// ============================================================
// MODELO DE PETICIÓN AL WEBSERVICE
// ============================================================

/**
 * Cuerpo de la petición POST al endpoint de botes.
 */
data class JackpotRequest(
    @SerializedName("token")
    val token: String,

    @SerializedName("pass")
    val pass: String,

    @SerializedName("app_id")
    val appId: String
)
