package com.meloyevo.app.data

import android.content.Context
import androidx.room.withTransaction
import com.meloyevo.app.util.Precios
import kotlinx.coroutines.flow.Flow
import kotlin.math.roundToLong

/** Una línea del carrito de venta, ya con el precio que se va a cobrar. */
data class LineaVenta(
    val prenda: Prenda,
    /** En pesos si la venta es COP, en centavos si es USD. */
    val precioCobrado: Long,
    val precioLista: Long,
)

class Repositorio(context: Context) {
    private val db = AppDatabase.get(context)
    private val prendaDao = db.prendas()
    private val ventaDao = db.ventas()
    private val movDao = db.movimientos()
    val imagenes = ImageStore(context)

    // ---------- Prendas ----------
    val prendas: Flow<List<Prenda>> = prendaDao.observarTodas()
    val nombresUsados: Flow<List<String>> = prendaDao.observarNombres()

    fun prenda(id: Long): Flow<Prenda?> = prendaDao.observar(id)
    suspend fun obtenerPrenda(id: Long) = prendaDao.obtener(id)
    suspend fun guardarPrenda(p: Prenda): Long =
        if (p.id == 0L) prendaDao.insertar(p) else { prendaDao.actualizar(p); p.id }

    suspend fun puedeEliminar(id: Long) = prendaDao.contarVentasDe(id) == 0

    /** La foto puede estar compartida (prendas iguales en otra talla): se limpia solo si queda huérfana. */
    suspend fun eliminarPrenda(p: Prenda) {
        prendaDao.eliminar(p.id)
        limpiarFotosHuerfanas()
    }

    /** Borra fotos que ya no pertenecen a ninguna prenda (p. ej. tras editar). */
    suspend fun limpiarFotosHuerfanas() {
        val enUso = prendaDao.fotosEnUso().toSet()
        imagenes.carpeta.listFiles()?.forEach { f ->
            val nombre = f.name.removePrefix("p_").removePrefix("t_").removeSuffix(".jpg")
            if (nombre !in enUso) f.delete()
        }
    }

    // ---------- Ventas ----------
    val ventas: Flow<List<Venta>> = ventaDao.observarVentas()
    val itemsVendidos: Flow<List<VentaItem>> = ventaDao.observarItems()

    suspend fun itemsDe(ventaId: Long) = ventaDao.itemsDe(ventaId)

    /**
     * Todo en una transacción: o se guarda la venta completa
     * (prendas vendidas + registro + caja) o no se guarda nada.
     */
    suspend fun registrarVenta(lineas: List<LineaVenta>, moneda: String, tasa: Double): Long =
        db.withTransaction {
            val ahora = System.currentTimeMillis()
            val total = lineas.sumOf { it.precioCobrado }
            val totalCop = if (moneda == MONEDA_USD) Precios.usdACop(total, tasa) else total
            val costoCop = (lineas.sumOf { it.prenda.costoUsdCents } / 100.0 * tasa).roundToLong()
            val ventaId = ventaDao.insertarVenta(
                Venta(fecha = ahora, moneda = moneda, total = total, tasa = tasa, gananciaCop = totalCop - costoCop),
            )
            ventaDao.insertarItems(lineas.map {
                VentaItem(
                    ventaId = ventaId,
                    prendaId = it.prenda.id,
                    nombre = it.prenda.nombre,
                    talla = it.prenda.talla,
                    precioCobrado = it.precioCobrado,
                    precioLista = it.precioLista,
                    costoUsdCents = it.prenda.costoUsdCents,
                )
            })
            prendaDao.marcarEstado(lineas.map { it.prenda.id }, ESTADO_VENDIDA, ahora)
            movDao.insertar(
                Movimiento(
                    fecha = ahora, tipo = MOV_VENTA, moneda = moneda, monto = total, ventaId = ventaId,
                    nota = lineas.joinToString(", ") { "${it.prenda.nombre} T${it.prenda.talla}" },
                ),
            )
            ventaId
        }

    /** "Deshacer" inmediato: borra la venta como si no hubiera ocurrido. */
    suspend fun deshacerVenta(ventaId: Long) = db.withTransaction {
        val items = ventaDao.itemsDe(ventaId)
        prendaDao.marcarEstado(items.map { it.prendaId }, ESTADO_DISPONIBLE, null)
        movDao.eliminarDeVenta(ventaId)
        ventaDao.eliminarItems(ventaId)
        ventaDao.eliminarVenta(ventaId)
    }

    /** Devolución posterior: deja constancia y descuenta de la caja. */
    suspend fun revertirVenta(ventaId: Long) = db.withTransaction {
        val venta = ventaDao.obtenerVenta(ventaId) ?: return@withTransaction
        if (venta.revertida) return@withTransaction
        val items = ventaDao.itemsDe(ventaId)
        prendaDao.marcarEstado(items.map { it.prendaId }, ESTADO_DISPONIBLE, null)
        ventaDao.marcarRevertida(ventaId)
        movDao.insertar(
            Movimiento(
                fecha = System.currentTimeMillis(), tipo = MOV_REVERSION, moneda = venta.moneda,
                monto = -venta.total, ventaId = ventaId,
                nota = "Devolución: " + items.joinToString(", ") { it.nombre },
            ),
        )
        Unit
    }

    // ---------- Caja ----------
    val movimientos: Flow<List<Movimiento>> = movDao.observarTodos()
    val saldoCop: Flow<Long> = movDao.observarSaldo(MONEDA_COP)
    val saldoUsd: Flow<Long> = movDao.observarSaldo(MONEDA_USD)

    suspend fun registrarMovimiento(tipo: String, moneda: String, montoPositivo: Long, nota: String) {
        val signo = if (tipo == MOV_SALIDA) -1 else 1
        movDao.insertar(
            Movimiento(
                fecha = System.currentTimeMillis(), tipo = tipo, moneda = moneda,
                monto = signo * montoPositivo, nota = nota,
            ),
        )
    }

    /** Cambio de divisa: sale una moneda y entra la otra. */
    suspend fun registrarCambio(monedaEntrega: String, entrega: Long, recibe: Long, nota: String) {
        val ahora = System.currentTimeMillis()
        val monedaRecibe = if (monedaEntrega == MONEDA_USD) MONEDA_COP else MONEDA_USD
        movDao.insertarVarios(listOf(
            Movimiento(fecha = ahora, tipo = MOV_CAMBIO, moneda = monedaEntrega, monto = -entrega, nota = nota),
            Movimiento(fecha = ahora, tipo = MOV_CAMBIO, moneda = monedaRecibe, monto = recibe, nota = nota),
        ))
    }

    /** Cuadre: registra la diferencia entre lo contado y lo esperado. */
    suspend fun cuadrar(contadoCop: Long, contadoUsd: Long) = db.withTransaction {
        val ahora = System.currentTimeMillis()
        val difCop = contadoCop - movDao.saldo(MONEDA_COP)
        val difUsd = contadoUsd - movDao.saldo(MONEDA_USD)
        if (difCop != 0L) movDao.insertar(Movimiento(fecha = ahora, tipo = MOV_AJUSTE, moneda = MONEDA_COP, monto = difCop, nota = "Cuadre de caja"))
        if (difUsd != 0L) movDao.insertar(Movimiento(fecha = ahora, tipo = MOV_AJUSTE, moneda = MONEDA_USD, monto = difUsd, nota = "Cuadre de caja"))
    }
}
