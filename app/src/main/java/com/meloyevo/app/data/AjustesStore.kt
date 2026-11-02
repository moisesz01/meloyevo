package com.meloyevo.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest
import java.security.SecureRandom

data class Ajustes(
    /** Pesos colombianos por 1 dólar. */
    val tasa: Double,
    val tasaActualizadaEn: Long,
    /** Redondeo hacia arriba del precio COP automático: 0, 100, 500 o 1000. */
    val redondeo: Int,
    val tienePin: Boolean,
    /** Segundos sin uso antes de volver a Modo Mostrador. */
    val autoBloqueoSeg: Int,
)

class AjustesStore(context: Context) {
    private val prefs = context.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    private val _estado = MutableStateFlow(leer())
    val estado: StateFlow<Ajustes> = _estado.asStateFlow()

    private fun leer() = Ajustes(
        tasa = prefs.getFloat("tasa", 4000f).toDouble().let {
            // Se guarda también como texto para no perder decimales
            prefs.getString("tasa_txt", null)?.toDoubleOrNull() ?: it
        },
        tasaActualizadaEn = prefs.getLong("tasa_fecha", 0L),
        redondeo = prefs.getInt("redondeo", 1000),
        tienePin = prefs.getString("pin_hash", null) != null,
        autoBloqueoSeg = prefs.getInt("auto_bloqueo", 120),
    )

    private fun refrescar() {
        _estado.value = leer()
    }

    fun setTasa(tasa: Double) {
        prefs.edit()
            .putFloat("tasa", tasa.toFloat())
            .putString("tasa_txt", tasa.toString())
            .putLong("tasa_fecha", System.currentTimeMillis())
            .apply()
        refrescar()
    }

    fun setRedondeo(paso: Int) {
        prefs.edit().putInt("redondeo", paso).apply()
        refrescar()
    }

    fun setAutoBloqueo(segundos: Int) {
        prefs.edit().putInt("auto_bloqueo", segundos).apply()
        refrescar()
    }

    /** pin = null elimina el PIN. */
    fun setPin(pin: String?) {
        if (pin == null) {
            prefs.edit().remove("pin_hash").remove("pin_salt").apply()
        } else {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val saltHex = salt.toHex()
            prefs.edit()
                .putString("pin_salt", saltHex)
                .putString("pin_hash", hash(pin, saltHex))
                .apply()
        }
        refrescar()
    }

    fun verificarPin(pin: String): Boolean {
        val guardado = prefs.getString("pin_hash", null) ?: return true
        val salt = prefs.getString("pin_salt", "") ?: ""
        return hash(pin, salt) == guardado
    }

    /** Datos que viajan en el respaldo (el PIN no se incluye). */
    fun exportar(): Map<String, String> = mapOf(
        "tasa" to _estado.value.tasa.toString(),
        "tasa_fecha" to _estado.value.tasaActualizadaEn.toString(),
        "redondeo" to _estado.value.redondeo.toString(),
    )

    fun importar(datos: Map<String, String>) {
        val e = prefs.edit()
        datos["tasa"]?.toDoubleOrNull()?.let {
            e.putFloat("tasa", it.toFloat()).putString("tasa_txt", it.toString())
        }
        datos["tasa_fecha"]?.toLongOrNull()?.let { e.putLong("tasa_fecha", it) }
        datos["redondeo"]?.toIntOrNull()?.let { e.putInt("redondeo", it) }
        e.commit()
        refrescar()
    }

    private fun hash(pin: String, salt: String): String =
        MessageDigest.getInstance("SHA-256").digest((salt + pin).toByteArray()).toHex()

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
}
