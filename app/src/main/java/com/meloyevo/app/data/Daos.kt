package com.meloyevo.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PrendaDao {
    @Query("SELECT * FROM prendas ORDER BY creadoEn DESC")
    fun observarTodas(): Flow<List<Prenda>>

    @Query("SELECT * FROM prendas WHERE id = :id")
    fun observar(id: Long): Flow<Prenda?>

    @Query("SELECT * FROM prendas WHERE id = :id")
    suspend fun obtener(id: Long): Prenda?

    @Query("SELECT * FROM prendas WHERE id IN (:ids)")
    suspend fun obtenerVarias(ids: List<Long>): List<Prenda>

    @Insert
    suspend fun insertar(prenda: Prenda): Long

    @Update
    suspend fun actualizar(prenda: Prenda)

    @Query("DELETE FROM prendas WHERE id = :id")
    suspend fun eliminar(id: Long)

    @Query("UPDATE prendas SET estado = :estado, vendidoEn = :fecha WHERE id IN (:ids)")
    suspend fun marcarEstado(ids: List<Long>, estado: String, fecha: Long?)

    @Query("SELECT DISTINCT nombre FROM prendas ORDER BY nombre COLLATE NOCASE")
    fun observarNombres(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM venta_items WHERE prendaId = :prendaId")
    suspend fun contarVentasDe(prendaId: Long): Int

    @Query("SELECT foto FROM prendas WHERE foto IS NOT NULL")
    suspend fun fotosEnUso(): List<String>
}

@Dao
interface VentaDao {
    @Insert
    suspend fun insertarVenta(venta: Venta): Long

    @Insert
    suspend fun insertarItems(items: List<VentaItem>)

    @Query("SELECT * FROM ventas ORDER BY fecha DESC")
    fun observarVentas(): Flow<List<Venta>>

    @Query("SELECT * FROM venta_items ORDER BY id DESC")
    fun observarItems(): Flow<List<VentaItem>>

    @Query("SELECT * FROM ventas WHERE id = :id")
    suspend fun obtenerVenta(id: Long): Venta?

    @Query("SELECT * FROM venta_items WHERE ventaId = :ventaId")
    suspend fun itemsDe(ventaId: Long): List<VentaItem>

    @Query("UPDATE ventas SET revertida = 1 WHERE id = :id")
    suspend fun marcarRevertida(id: Long)

    @Query("DELETE FROM ventas WHERE id = :id")
    suspend fun eliminarVenta(id: Long)

    @Query("DELETE FROM venta_items WHERE ventaId = :ventaId")
    suspend fun eliminarItems(ventaId: Long)
}

@Dao
interface MovimientoDao {
    @Insert
    suspend fun insertar(movimiento: Movimiento): Long

    @Insert
    suspend fun insertarVarios(movimientos: List<Movimiento>)

    @Query("SELECT * FROM movimientos ORDER BY fecha DESC, id DESC")
    fun observarTodos(): Flow<List<Movimiento>>

    @Query("SELECT COALESCE(SUM(monto), 0) FROM movimientos WHERE moneda = :moneda")
    fun observarSaldo(moneda: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(monto), 0) FROM movimientos WHERE moneda = :moneda")
    suspend fun saldo(moneda: String): Long

    @Query("DELETE FROM movimientos WHERE ventaId = :ventaId")
    suspend fun eliminarDeVenta(ventaId: Long)
}
