package com.meloyevo.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val ESTADO_DISPONIBLE = "DISPONIBLE"
const val ESTADO_VENDIDA = "VENDIDA"

const val MONEDA_COP = "COP"
const val MONEDA_USD = "USD"

const val MOV_VENTA = "VENTA"
const val MOV_ENTRADA = "ENTRADA"
const val MOV_SALIDA = "SALIDA"
const val MOV_CAMBIO = "CAMBIO"
const val MOV_AJUSTE = "AJUSTE"
const val MOV_REVERSION = "REVERSION"

/**
 * Una prenda = una unidad física (disponible o vendida).
 * Montos en USD se guardan en centavos; montos en COP en pesos enteros.
 * precioCopFijo = null significa precio automático (sigue a la tasa global).
 */
@Entity(tableName = "prendas", indices = [Index("estado")])
data class Prenda(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val talla: String,
    val costoUsdCents: Long,
    val precioUsdCents: Long,
    val precioCopFijo: Long? = null,
    val foto: String? = null,
    val estado: String = ESTADO_DISPONIBLE,
    val creadoEn: Long = System.currentTimeMillis(),
    val vendidoEn: Long? = null,
)

/** total: en pesos si moneda = COP, en centavos si moneda = USD. */
@Entity(tableName = "ventas", indices = [Index("fecha")])
data class Venta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: Long,
    val moneda: String,
    val total: Long,
    val tasa: Double,
    val gananciaCop: Long,
    val revertida: Boolean = false,
)

/** Copia de los datos de la prenda en el momento de la venta. */
@Entity(tableName = "venta_items", indices = [Index("ventaId")])
data class VentaItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ventaId: Long,
    val prendaId: Long,
    val nombre: String,
    val talla: String,
    val precioCobrado: Long,
    val precioLista: Long,
    val costoUsdCents: Long,
)

/** monto con signo: positivo entra a caja, negativo sale. */
@Entity(tableName = "movimientos", indices = [Index("fecha"), Index("ventaId")])
data class Movimiento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fecha: Long,
    val tipo: String,
    val moneda: String,
    val monto: Long,
    val nota: String = "",
    val ventaId: Long? = null,
)
