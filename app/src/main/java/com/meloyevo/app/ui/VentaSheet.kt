package com.meloyevo.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meloyevo.app.AppViewModel
import com.meloyevo.app.data.ESTADO_DISPONIBLE
import com.meloyevo.app.data.MONEDA_COP
import com.meloyevo.app.data.MONEDA_USD
import com.meloyevo.app.ui.theme.Paleta
import com.meloyevo.app.util.Dinero
import com.meloyevo.app.util.Texto
import kotlinx.coroutines.launch

/**
 * Hoja de venta rápida. Nunca muestra costos ni ganancia:
 * es la pantalla que más probablemente verá el cliente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentaSheet(vm: AppViewModel, onConfirmada: (ventaId: Long, total: String) -> Unit) {
    val abierta by vm.ventaAbierta.collectAsStateWithLifecycle()
    if (!abierta) return

    val estadoHoja = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val carrito by vm.carrito.collectAsStateWithLifecycle()
    val prendas by vm.prendas.collectAsStateWithLifecycle()
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val moneda by vm.monedaVenta.collectAsStateWithLifecycle()

    val porId = remember(prendas) { prendas.associateBy { it.id } }
    val lineas = carrito.mapNotNull { l -> porId[l.prendaId]?.let { l to it } }
    val total = lineas.sumOf { (l, p) -> vm.precioCobrado(l, p, moneda) }

    var buscando by remember { mutableStateOf(carrito.isEmpty()) }
    var consulta by remember { mutableStateOf("") }
    var editando by remember { mutableStateOf<Long?>(null) }
    var textoRebaja by remember { mutableStateOf("") }
    var recibido by remember(moneda) { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = vm::cerrarVenta, sheetState = estadoHoja) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Venta", style = MaterialTheme.typography.titleLarge)

            // ---- Prendas en la venta ----
            lineas.forEach { (linea, p) ->
                val lista = vm.precioLista(p, moneda)
                val cobrado = vm.precioCobrado(linea, p, moneda)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FotoPrenda(p.foto, p.nombre, tamano = 48.dp)
                    Column(Modifier.weight(1f)) {
                        Text(p.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
                        Text("Talla ${p.talla}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.clickable {
                            editando = p.id
                            textoRebaja = if (moneda == MONEDA_USD) Dinero.usdEditable(cobrado) else cobrado.toString()
                        },
                    ) {
                        Text(Dinero.monto(moneda, cobrado), style = MaterialTheme.typography.titleMedium)
                        if (cobrado != lista) {
                            Text(
                                Dinero.monto(moneda, lista),
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = { vm.quitarDelCarrito(p.id); if (editando == p.id) editando = null }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Quitar ${p.nombre}")
                    }
                }
                if (editando == p.id) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            if (moneda == MONEDA_USD) CampoUsd(textoRebaja, { textoRebaja = it }, "Precio a cobrar")
                            else CampoCop(textoRebaja, { textoRebaja = it }, "Precio a cobrar")
                        }
                        TextButton(onClick = {
                            val v = if (moneda == MONEDA_USD) Dinero.parseUsdCents(textoRebaja) else Dinero.parseCop(textoRebaja)
                            vm.rebajar(p.id, v?.takeIf { it != lista })
                            editando = null
                        }) { Text("Listo") }
                    }
                }
            }

            // ---- Buscar y agregar ----
            if (buscando) {
                val foco = remember { FocusRequester() }
                OutlinedTextField(
                    value = consulta,
                    onValueChange = { consulta = it },
                    placeholder = { Text("Buscar prenda o talla…") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().focusRequester(foco),
                )
                LaunchedEffect(Unit) { foco.requestFocus() }
                val enCarrito = carrito.map { it.prendaId }.toSet()
                val resultados = prendas.asSequence()
                    .filter { it.estado == ESTADO_DISPONIBLE && it.id !in enCarrito && Texto.coincide(it, consulta) }
                    .take(20).toList()
                if (resultados.isEmpty()) {
                    Text(
                        "No hay prendas disponibles que coincidan.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                resultados.forEach { p ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                vm.agregarAlCarrito(p.id)
                                consulta = ""
                                buscando = false
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FotoPrenda(p.foto, p.nombre, tamano = 48.dp)
                        Column(Modifier.weight(1f)) {
                            Text(p.nombre, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Talla ${p.talla}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(Dinero.monto(moneda, vm.precioLista(p, moneda)), style = MaterialTheme.typography.titleSmall)
                    }
                }
            } else {
                TextButton(onClick = { buscando = true }) {
                    Icon(Icons.Outlined.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Agregar otra prenda")
                }
            }

            if (lineas.isNotEmpty()) {
                HorizontalDivider()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Total", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(Dinero.monto(moneda, total), style = MaterialTheme.typography.titleLarge)
                }
                if (moneda == MONEDA_USD) {
                    Text(
                        "Equivale a ${Dinero.cop(com.meloyevo.app.util.Precios.usdACop(total, ajustes.tasa))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Text("Cobro en", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    listOf(MONEDA_COP to "Pesos", MONEDA_USD to "Dólares").forEachIndexed { i, (m, etiqueta) ->
                        SegmentedButton(
                            selected = moneda == m,
                            onClick = { vm.monedaVenta.value = m; editando = null },
                            shape = SegmentedButtonDefaults.itemShape(i, 2),
                        ) { Text(etiqueta) }
                    }
                }

                // Recibido y vuelto (opcional)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        if (moneda == MONEDA_USD) CampoUsd(recibido, { recibido = it }, "Recibido (opcional)")
                        else CampoCop(recibido, { recibido = it }, "Recibido (opcional)")
                    }
                    val recibidoValor = if (moneda == MONEDA_USD) Dinero.parseUsdCents(recibido) else Dinero.parseCop(recibido)
                    if (recibidoValor != null) {
                        val vuelto = recibidoValor - total
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                if (vuelto >= 0) "Vuelto" else "Falta",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (vuelto >= 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                            )
                            Text(Dinero.monto(moneda, kotlin.math.abs(vuelto)), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }

                Button(
                    onClick = {
                        if (guardando) return@Button
                        guardando = true
                        val totalTexto = Dinero.monto(moneda, total)
                        scope.launch {
                            val id = vm.confirmarVenta()
                            guardando = false
                            if (id != null) onConfirmada(id, totalTexto)
                        }
                    },
                    enabled = !guardando && editando == null,
                    colors = ButtonDefaults.buttonColors(containerColor = Paleta.Coral, contentColor = Paleta.CoralOscuro),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    Text("Confirmar venta ${Dinero.monto(moneda, total)}", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
