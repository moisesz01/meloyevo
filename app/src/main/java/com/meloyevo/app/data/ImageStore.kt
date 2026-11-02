package com.meloyevo.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Guarda cada foto en dos tamaños dentro de la app (no en la galería):
 * p_<id>.jpg 600x600 para el detalle y t_<id>.jpg 200x200 para la lista.
 */
class ImageStore(private val context: Context) {

    val carpeta: File get() = File(context.filesDir, "fotos").apply { mkdirs() }

    fun foto(nombre: String) = File(carpeta, "p_$nombre.jpg")
    fun miniatura(nombre: String) = File(carpeta, "t_$nombre.jpg")

    /** Archivo temporal donde la app de cámara deja la foto. */
    fun archivoCaptura(): File =
        File(context.cacheDir, "captures").apply { mkdirs() }.let { File(it, "captura.jpg") }

    suspend fun importar(uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val cr = context.contentResolver
            val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, limites) }
            if (limites.outWidth <= 0 || limites.outHeight <= 0) return@withContext null

            // Decodifica reducido para no agotar la RAM del teléfono
            var muestra = 1
            while (limites.outWidth / (muestra * 2) >= 1000 && limites.outHeight / (muestra * 2) >= 1000) {
                muestra *= 2
            }
            val original = cr.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = muestra })
            } ?: return@withContext null

            val grados = cr.openInputStream(uri)?.use {
                when (ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL,
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f

            val lado = minOf(original.width, original.height)
            val matriz = Matrix().apply { if (grados != 0f) postRotate(grados) }
            val cuadrada = Bitmap.createBitmap(
                original,
                (original.width - lado) / 2,
                (original.height - lado) / 2,
                lado, lado, matriz, true,
            )
            if (cuadrada !== original) original.recycle()

            val nombre = UUID.randomUUID().toString().replace("-", "").take(16)
            guardar(cuadrada, 600, 82, foto(nombre))
            guardar(cuadrada, 200, 75, miniatura(nombre))
            cuadrada.recycle()
            nombre
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private fun guardar(fuente: Bitmap, tam: Int, calidad: Int, destino: File) {
        val escalada = if (fuente.width > tam) Bitmap.createScaledBitmap(fuente, tam, tam, true) else fuente
        FileOutputStream(destino).use { escalada.compress(Bitmap.CompressFormat.JPEG, calidad, it) }
        if (escalada !== fuente) escalada.recycle()
    }

    fun eliminar(nombre: String?) {
        if (nombre == null) return
        foto(nombre).delete()
        miniatura(nombre).delete()
    }
}
