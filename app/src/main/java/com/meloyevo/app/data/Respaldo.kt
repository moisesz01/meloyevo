package com.meloyevo.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Respaldo de un solo archivo .zip: base de datos + fotos + tasa y redondeo. */
object Respaldo {
    private const val DB_ZIP = "meloyevo.db"
    private const val AJUSTES_ZIP = "ajustes.json"
    private const val FOTOS_ZIP = "fotos/"

    suspend fun exportar(context: Context, destino: Uri, ajustes: AjustesStore): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val db = AppDatabase.get(context)
                // Vuelca el diario WAL al archivo principal para que la copia esté completa
                db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }
                val dbFile = context.getDatabasePath(AppDatabase.NOMBRE)
                val fotos = File(context.filesDir, "fotos").listFiles().orEmpty()

                context.contentResolver.openOutputStream(destino)?.use { os ->
                    ZipOutputStream(os.buffered()).use { zip ->
                        zip.putNextEntry(ZipEntry(DB_ZIP))
                        dbFile.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()

                        zip.putNextEntry(ZipEntry(AJUSTES_ZIP))
                        zip.write(JSONObject(ajustes.exportar()).toString().toByteArray())
                        zip.closeEntry()

                        for (f in fotos) {
                            zip.putNextEntry(ZipEntry(FOTOS_ZIP + f.name))
                            f.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                } ?: return@withContext false
                true
            } catch (e: Exception) {
                false
            }
        }

    /**
     * Reemplaza todos los datos actuales por los del respaldo.
     * Después de un resultado true, la app debe reiniciarse.
     */
    suspend fun importar(context: Context, origen: Uri, ajustes: AjustesStore): Boolean =
        withContext(Dispatchers.IO) {
            val temp = File(context.cacheDir, "restaurar").apply { deleteRecursively(); mkdirs() }
            var bdCerrada = false
            try {
                context.contentResolver.openInputStream(origen)?.use { input ->
                    ZipInputStream(input.buffered()).use { zip ->
                        var entrada = zip.nextEntry
                        while (entrada != null) {
                            val destino = File(temp, entrada.name)
                            // Evita rutas maliciosas fuera de la carpeta temporal
                            if (!destino.canonicalPath.startsWith(temp.canonicalPath + File.separator)) {
                                return@withContext false
                            }
                            if (entrada.isDirectory) destino.mkdirs() else {
                                destino.parentFile?.mkdirs()
                                destino.outputStream().use { zip.copyTo(it) }
                            }
                            zip.closeEntry()
                            entrada = zip.nextEntry
                        }
                    }
                } ?: return@withContext false

                val dbNueva = File(temp, DB_ZIP)
                if (!dbNueva.exists() || dbNueva.length() == 0L) return@withContext false

                AppDatabase.cerrar()
                bdCerrada = true
                val dbFile = context.getDatabasePath(AppDatabase.NOMBRE)
                File(dbFile.path + "-wal").delete()
                File(dbFile.path + "-shm").delete()
                dbNueva.copyTo(dbFile, overwrite = true)

                val carpetaFotos = File(context.filesDir, "fotos")
                carpetaFotos.deleteRecursively()
                carpetaFotos.mkdirs()
                File(temp, "fotos").listFiles().orEmpty().forEach {
                    it.copyTo(File(carpetaFotos, it.name), overwrite = true)
                }

                File(temp, AJUSTES_ZIP).takeIf { it.exists() }?.let { f ->
                    val json = JSONObject(f.readText())
                    ajustes.importar(json.keys().asSequence().associateWith { json.getString(it) })
                }
                true
            } catch (e: Exception) {
                // Si la base ya se cerró, hay que reiniciar la app de todos modos
                bdCerrada
            } finally {
                temp.deleteRecursively()
            }
        }
}
