package com.meloyevo.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Prenda::class, Venta::class, VentaItem::class, Movimiento::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prendas(): PrendaDao
    abstract fun ventas(): VentaDao
    abstract fun movimientos(): MovimientoDao

    companion object {
        const val NOMBRE = "meloyevo.db"

        @Volatile
        private var instancia: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    NOMBRE,
                ).build().also { instancia = it }
            }

        /** Usado al restaurar un respaldo: la app se reinicia justo después. */
        fun cerrar() {
            synchronized(this) {
                instancia?.close()
                instancia = null
            }
        }
    }
}
