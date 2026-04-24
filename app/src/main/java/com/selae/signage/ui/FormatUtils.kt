package com.selae.signage.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

/**
 * FormatUtils.kt
 *
 * Utilidades de formateo para fechas (en español) e importes en euros.
 */
object FormatUtils {

    private val LOCALE_ES = Locale("es", "ES")

    // Nombres de días en español
    private val DIAS_SEMANA = mapOf(
        "Monday" to "Lunes",
        "Tuesday" to "Martes",
        "Wednesday" to "Miércoles",
        "Thursday" to "Jueves",
        "Friday" to "Viernes",
        "Saturday" to "Sábado",
        "Sunday" to "Domingo"
    )

    // Nombres de meses en español
    private val MESES = mapOf(
        "January" to "enero",
        "February" to "febrero",
        "March" to "marzo",
        "April" to "abril",
        "May" to "mayo",
        "June" to "junio",
        "July" to "julio",
        "August" to "agosto",
        "September" to "septiembre",
        "October" to "octubre",
        "November" to "noviembre",
        "December" to "diciembre"
    )

    /**
     * Formatea una fecha string del webservice al formato español legible.
     * Ejemplo de salida: "Jueves 27 de marzo"
     *
     * @param dateStr Fecha como ISO 8601 ("2024-03-28T20:00:00") o timestamp Unix ("1711656000")
     * @return Fecha formateada en español, o la cadena original si no se puede parsear
     */
    fun formatDrawDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "Próximamente"

        val date = parseDate(dateStr) ?: return dateStr

        // Formato para extraer día de semana, número de día y mes
        val dayOfWeekFmt = SimpleDateFormat("EEEE", Locale.ENGLISH)
        val dayNumFmt = SimpleDateFormat("d", Locale.ENGLISH)
        val monthFmt = SimpleDateFormat("MMMM", Locale.ENGLISH)

        val dayOfWeekEn = dayOfWeekFmt.format(date)
        val dayNum = dayNumFmt.format(date)
        val monthEn = monthFmt.format(date)

        val dayOfWeekEs = DIAS_SEMANA[dayOfWeekEn] ?: dayOfWeekEn
        val monthEs = MESES[monthEn] ?: monthEn

        return "$dayOfWeekEs $dayNum de $monthEs"
    }

    /**
     * Formatea un importe en euros con separadores de miles (punto) y sin decimales.
     * Ejemplo: 63000000.0 → "63.000.000 €"
     */
    fun formatJackpot(amount: Double): String {
        if (amount <= 0) return "Bote no disponible"

        val symbols = DecimalFormatSymbols(LOCALE_ES).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,###", symbols)
        return "${formatter.format(amount.toLong())} €"
    }

    /**
     * Formatea un timestamp de última actualización para mostrar en pantalla.
     */
    fun formatLastUpdate(timestamp: Long): String {
        if (timestamp == 0L) return "Sin datos recientes"
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return "Actualizado a las ${sdf.format(Date(timestamp))}"
    }

    // ─── Parsers internos ──────────────────────────────────────

    private fun parseDate(dateStr: String): Date? {
        // 1. Intentar como timestamp Unix (segundos o milisegundos)
        dateStr.toLongOrNull()?.let { ts ->
            return if (ts > 1_000_000_000_000L) Date(ts) else Date(ts * 1000L)
        }

        // 2. Intentar distintos formatos ISO
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            "yyyy-MM-dd"
        )

        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                    timeZone = TimeZone.getDefault()
                }
                sdf.parse(dateStr.take(pattern.length))?.let { return it }
            } catch (_: Exception) { }
        }

        return null
    }
}
