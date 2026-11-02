package com.meloyevo.app.util

import com.meloyevo.app.data.Ajustes
import com.meloyevo.app.data.MONEDA_USD
import com.meloyevo.app.data.Prenda
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/** Formato fijo: COP "$ 48.000" · USD "US$ 12" o "US$ 12,50". */
object Dinero {
    private fun miles(v: Long): String {
        val s = abs(v).toString()
        val sb = StringBuilder()
        for ((i, c) in s.withIndex()) {
            if (i > 0 && (s.length - i) % 3 == 0) sb.append('.')
            sb.append(c)
        }
        return (if (v < 0) "-" else "") + sb
    }

    fun cop(pesos: Long): String = if (pesos < 0) "- $ ${miles(-pesos)}" else "$ ${miles(pesos)}"

    fun usd(cents: Long): String {
        val signo = if (cents < 0) "- " else ""
        val a = abs(cents)
        val enteros = miles(a / 100)
        val dec = a % 100
        return if (dec == 0L) "${signo}US$ $enteros" else "${signo}US$ $enteros,${"%02d".format(dec)}"
    }

    fun monto(moneda: String, valor: Long) = if (moneda == MONEDA_USD) usd(valor) else cop(valor)

    /** Para campos de texto USD: 1250 -> "12,50"; 1200 -> "12". */
    fun usdEditable(cents: Long): String {
        val dec = cents % 100
        return if (dec == 0L) (cents / 100).toString() else "${cents / 100},${"%02d".format(dec)}"
    }

    /** Acepta "12", "12,5", "12.50". */
    fun parseUsdCents(texto: String): Long? {
        val limpio = texto.trim().replace(" ", "").replace(',', '.')
        if (limpio.isEmpty()) return null
        return try {
            BigDecimal(limpio).movePointRight(2).setScale(0, RoundingMode.HALF_UP).toLong()
                .takeIf { it >= 0 }
        } catch (e: NumberFormatException) {
            null
        }
    }

    fun parseCop(texto: String): Long? = texto.filter { it.isDigit() }.take(12).toLongOrNull()

    /** "3940,5" o "3940.5" -> 3940.5 */
    fun parseTasa(texto: String): Double? {
        var t = texto.trim().replace(" ", "").replace("$", "")
        t = when {
            t.contains(',') -> t.replace(".", "").replace(',', '.')
            // "3.940" = tres mil novecientos cuarenta; "3940.5" = decimal
            t.count { it == '.' } == 1 && t.substringAfter('.').length == 3 -> t.replace(".", "")
            t.count { it == '.' } > 1 -> t.replace(".", "")
            else -> t
        }
        return t.toDoubleOrNull()?.takeIf { it > 0 }
    }

    fun tasaTexto(tasa: Double): String {
        val centesimas = (tasa * 100).roundToLong()
        val entero = centesimas / 100
        val dec = centesimas % 100
        return if (dec == 0L) cop(entero).removePrefix("$ ") else "${cop(entero).removePrefix("$ ")},${"%02d".format(dec)}"
    }
}

object Precios {
    fun usdACop(cents: Long, tasa: Double): Long = (cents / 100.0 * tasa).roundToLong()

    fun redondear(valor: Long, paso: Int): Long =
        if (paso <= 0) valor else ((valor + paso - 1) / paso) * paso

    fun copCalculado(p: Prenda, a: Ajustes): Long = usdACop(p.precioUsdCents, a.tasa)

    fun copRedondeado(p: Prenda, a: Ajustes): Long = redondear(copCalculado(p, a), a.redondeo)

    fun precioFinalCop(p: Prenda, a: Ajustes): Long = p.precioCopFijo ?: copRedondeado(p, a)

    fun costoCop(p: Prenda, a: Ajustes): Long = usdACop(p.costoUsdCents, a.tasa)

    fun gananciaCop(p: Prenda, a: Ajustes): Long = precioFinalCop(p, a) - costoCop(p, a)

    /** Margen sobre el precio de venta, en %. null si el precio es 0. */
    fun margenPct(p: Prenda, a: Ajustes): Int? {
        val precio = precioFinalCop(p, a)
        if (precio <= 0) return null
        return (gananciaCop(p, a) * 100 / precio).toInt()
    }
}

object Texto {
    fun normalizar(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.ROOT)
            .trim()

    /** "body 6" encuentra prendas cuyo nombre contiene "body" y talla "6". */
    fun coincide(p: Prenda, consulta: String): Boolean {
        val palabras = normalizar(consulta).split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (palabras.isEmpty()) return true
        val nombre = normalizar(p.nombre)
        val talla = normalizar(p.talla)
        return palabras.all { w ->
            nombre.contains(w) || talla == w || talla.replace(" ", "") == w ||
                (w.startsWith("t") && talla == w.removePrefix("t"))
        }
    }
}

object Tallas {
    val sugeridas = listOf(
        "0-3M", "3-6M", "6-9M", "9-12M", "12-18M", "18-24M",
        "2", "3", "4", "5", "6", "8", "10", "12", "14", "16",
    )

    fun ordenar(tallas: Collection<String>): List<String> =
        tallas.distinct().sortedWith(compareBy({
            val i = sugeridas.indexOf(it)
            if (i < 0) Int.MAX_VALUE else i
        }, { it }))
}

object Fechas {
    private val loc = Locale("es", "CO")
    fun hora(t: Long): String = SimpleDateFormat("h:mm a", loc).format(Date(t))
    fun diaCorto(t: Long): String = SimpleDateFormat("d MMM", loc).format(Date(t))
    fun diaLargo(t: Long): String = SimpleDateFormat("EEEE d 'de' MMMM", loc).format(Date(t))
        .replaceFirstChar { it.uppercase() }
    fun completa(t: Long): String = SimpleDateFormat("d MMM yyyy, h:mm a", loc).format(Date(t))

    fun inicioDelDia(t: Long = System.currentTimeMillis()): Long =
        Calendar.getInstance().apply {
            timeInMillis = t
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun inicioDeSemana(): Long = Calendar.getInstance().apply {
        timeInMillis = inicioDelDia()
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        if (timeInMillis > System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, -7)
    }.timeInMillis

    fun inicioDeMes(): Long = Calendar.getInstance().apply {
        timeInMillis = inicioDelDia()
        set(Calendar.DAY_OF_MONTH, 1)
    }.timeInMillis

    fun hace(t: Long): String {
        if (t <= 0) return "nunca"
        val dias = ((inicioDelDia() - inicioDelDia(t)) / 86_400_000L).toInt()
        return when {
            dias <= 0 -> "hoy"
            dias == 1 -> "ayer"
            else -> "hace $dias días"
        }
    }
}
