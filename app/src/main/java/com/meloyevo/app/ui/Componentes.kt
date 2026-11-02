package com.meloyevo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.meloyevo.app.data.Ajustes
import com.meloyevo.app.data.ESTADO_VENDIDA
import com.meloyevo.app.data.Prenda
import com.meloyevo.app.ui.theme.Paleta
import com.meloyevo.app.util.Dinero
import com.meloyevo.app.util.Fechas
import com.meloyevo.app.util.Precios
import java.io.File

private val coloresInicial = listOf(
    Paleta.MentaClara to Paleta.MentaOscura,
    Paleta.CoralClaro to Paleta.CoralOscuro,
    Paleta.PollitoClaro to Paleta.PollitoOscuro,
    Paleta.LavandaClara to Paleta.LavandaOscura,
)

/** Foto de la prenda; si no tiene, un recuadro de color con la inicial. */
@Composable
fun FotoPrenda(
    foto: String?,
    nombre: String,
    modifier: Modifier = Modifier,
    tamano: Dp = 72.dp,
    grande: Boolean = false,
    apagada: Boolean = false,
) {
    val context = LocalContext.current
    val forma = RoundedCornerShape(if (grande) 16.dp else 8.dp)
    val mod = if (grande) modifier.clip(forma) else modifier.size(tamano).clip(forma)
    if (foto != null) {
        val archivo = File(context.filesDir, "fotos/${if (grande) "p_" else "t_"}$foto.jpg")
        AsyncImage(
            model = archivo,
            contentDescription = nombre,
            contentScale = ContentScale.Crop,
            colorFilter = if (apagada) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
            modifier = mod.background(Paleta.GrisClaro),
        )
    } else {
        val (fondo, texto) = if (apagada) Paleta.GrisClaro to Paleta.Gris
        else coloresInicial[(nombre.hashCode() and 0x7fffffff) % coloresInicial.size]
        Box(mod.background(fondo), contentAlignment = Alignment.Center) {
            Text(
                nombre.trim().take(1).uppercase().ifEmpty { "?" },
                style = if (grande) MaterialTheme.typography.displayMedium else MaterialTheme.typography.titleLarge,
                color = texto,
            )
        }
    }
}

/** Pastilla de texto (talla, estado, margen...). */
@Composable
fun Etiqueta(texto: String, fondo: Color, color: Color, modifier: Modifier = Modifier) {
    Text(
        texto,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(fondo)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun EtiquetaTalla(talla: String) = Etiqueta("T $talla", Paleta.MentaClara, Paleta.MentaOscura)

@Composable
fun EtiquetaMargen(pct: Int?) {
    if (pct == null) return
    val (fondo, color) = when {
        pct < 0 -> Paleta.RojoClaro to Paleta.Rojo
        pct < 30 -> Paleta.AmbarClaro to Paleta.Ambar
        else -> Paleta.VerdeClaro to Paleta.Verde
    }
    Etiqueta(if (pct >= 0) "+$pct%" else "$pct%", fondo, color)
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TarjetaPrenda(
    p: Prenda,
    ajustes: Ajustes,
    gestion: Boolean,
    onAbrir: () -> Unit,
    onVender: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val vendida = p.estado == ESTADO_VENDIDA
    Card(
        onClick = onAbrir,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (vendida) Paleta.Vendida else Color.White),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Paleta.GrisClaro),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FotoPrenda(p.foto, p.nombre, apagada = vendida)
            Column(Modifier.weight(1f)) {
                Text(
                    Dinero.cop(Precios.precioFinalCop(p, ajustes)),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (vendida) Paleta.Gris else Paleta.Tinta,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        p.nombre,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(6.dp))
                    EtiquetaTalla(p.talla)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (gestion) "${Dinero.usd(p.precioUsdCents)} · costo ${Dinero.usd(p.costoUsdCents)}"
                        else Dinero.usd(p.precioUsdCents),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (gestion && !vendida) {
                        Spacer(Modifier.width(6.dp))
                        EtiquetaMargen(Precios.margenPct(p, ajustes))
                    }
                }
                if (vendida && p.vendidoEn != null) {
                    Spacer(Modifier.size(4.dp))
                    Etiqueta("Vendida ${Fechas.diaCorto(p.vendidoEn)}", Paleta.GrisClaro, Paleta.Gris)
                }
            }
            if (!vendida) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Paleta.CoralClaro)
                        .clickable(onClick = onVender),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.ShoppingBag, contentDescription = "Vender", tint = Paleta.CoralOscuro)
                }
            }
        }
    }
}

/** Franja lavanda visible solo mientras hay datos internos en pantalla. */
@Composable
fun BannerGestion(segundos: Int, onBloquear: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Paleta.LavandaClara)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.LockOpen, null, tint = Paleta.LavandaOscura, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "Datos internos visibles · se bloquea en ${segundos / 60}:${"%02d".format(segundos % 60)}",
            style = MaterialTheme.typography.labelLarge,
            color = Paleta.LavandaOscura,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onBloquear) { Text("Bloquear", color = Paleta.LavandaOscura) }
    }
}

/** Muestra "48000" como "48.000" mientras se escribe. */
object MilesTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val sb = StringBuilder()
        val mapa = IntArray(original.length + 1)
        for (i in original.indices) {
            if (i > 0 && (original.length - i) % 3 == 0) sb.append('.')
            mapa[i] = sb.length
            sb.append(original[i])
        }
        mapa[original.length] = sb.length
        val transformado = sb.toString()
        return TransformedText(AnnotatedString(transformado), object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = mapa[offset.coerceIn(0, original.length)]
            override fun transformedToOriginal(offset: Int): Int {
                val o = offset.coerceIn(0, transformado.length)
                var r = 0
                while (r < original.length && mapa[r] < o) r++
                return r
            }
        })
    }
}

@Composable
fun CampoCop(
    valor: String,
    onValor: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    habilitado: Boolean = true,
    apoyo: String? = null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { onValor(it.filter(Char::isDigit).take(12)) },
        label = { Text(etiqueta) },
        prefix = { Text("$ ") },
        singleLine = true,
        enabled = habilitado,
        visualTransformation = MilesTransformation,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        supportingText = apoyo?.let { a -> @Composable { Text(a) } },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun CampoUsd(
    valor: String,
    onValor: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    apoyo: String? = null,
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { nuevo ->
            val limpio = nuevo.filter { it.isDigit() || it == ',' || it == '.' }
            if (limpio.count { it == ',' || it == '.' } <= 1) onValor(limpio.take(10))
        },
        label = { Text(etiqueta) },
        prefix = { Text("US$ ") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        supportingText = apoyo?.let { a -> @Composable { Text(a) } },
        modifier = modifier.fillMaxWidth(),
    )
}

/** Tarjeta pequeña con un número grande (saldos, totales). */
@Composable
fun TarjetaMetrica(titulo: String, valor: String, modifier: Modifier = Modifier, detalle: String? = null) {
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(0.5.dp, Paleta.GrisClaro, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Text(titulo, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        if (detalle != null) {
            Text(detalle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Estado vacío: una invitación, no una disculpa. */
@Composable
fun EstadoVacio(titulo: String, cuerpo: String, accion: String? = null, onAccion: () -> Unit = {}) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(4.dp))
        Text(
            cuerpo,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        if (accion != null) {
            Spacer(Modifier.size(12.dp))
            TextButton(onClick = onAccion) { Text(accion) }
        }
    }
}
