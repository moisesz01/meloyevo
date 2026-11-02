package com.meloyevo.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meloyevo.app.AppViewModel
import com.meloyevo.app.data.ESTADO_DISPONIBLE
import com.meloyevo.app.data.MONEDA_USD
import com.meloyevo.app.data.Venta
import com.meloyevo.app.data.VentaItem
import com.meloyevo.app.ui.theme.Paleta
import com.meloyevo.app.util.Dinero
import com.meloyevo.app.util.Fechas
import com.meloyevo.app.util.Precios

private enum class Periodo(val etiqueta: String) { HOY("Hoy"), SEMANA("Semana"), MES("Mes") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumenScreen(vm: AppViewModel, onAjustes: () -> Unit) {
    val gestion by vm.gestion.collectAsStateWithLifecycle()
    val segundos by vm.segundosRestantes.collectAsStateWithLifecycle()
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val ventas by vm.ventas.collectAsStateWithLifecycle()
    val items by vm.itemsVendidos.collectAsStateWithLifecycle()
    val prendas by vm.prendas.collectAsStateWithLifecycle()
    var periodo by rememberSaveable { mutableStateOf(Periodo.HOY) }
    var ventaAbierta by remember { mutableStateOf<Venta?>(null) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Resumen") },
                    actions = {
                        IconButton(onClick = { vm.pedirGestion(onAjustes) }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Ajustes")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
                if (gestion) BannerGestion(segundos, vm::bloquear)
            }
        },
    ) { pad ->
        if (!gestion) {
            Column(Modifier.padding(pad)) { PantallaBloqueada("Resumen protegido") { vm.pedirGestion() } }
            return@Scaffold
        }
        val desde = when (periodo) {
            Periodo.HOY -> Fechas.inicioDelDia()
            Periodo.SEMANA -> Fechas.inicioDeSemana()
            Periodo.MES -> Fechas.inicioDeMes()
        }
        val enPeriodo = ventas.filter { it.fecha >= desde }
        val validas = enPeriodo.filter { !it.revertida }
        val itemsPorVenta = remember(items) { items.groupBy { it.ventaId } }
        val vendidoCop = validas.sumOf { if (it.moneda == MONEDA_USD) Precios.usdACop(it.total, it.tasa) else it.total }
        val ganancia = validas.sumOf { it.gananciaCop }
        val unidades = validas.sumOf { itemsPorVenta[it.id]?.size ?: 0 }
        val disponibles = prendas.filter { it.estado == ESTADO_DISPONIBLE }
        val valorVenta = disponibles.sumOf { Precios.precioFinalCop(it, ajustes) }
        val valorCosto = disponibles.sumOf { Precios.costoCop(it, ajustes) }

        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "periodo") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    Periodo.entries.forEachIndexed { i, p ->
                        SegmentedButton(
                            selected = periodo == p,
                            onClick = { periodo = p },
                            shape = SegmentedButtonDefaults.itemShape(i, Periodo.entries.size),
                        ) { Text(p.etiqueta) }
                    }
                }
            }
            item(key = "metricas") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TarjetaMetrica("Vendido", Dinero.cop(vendidoCop), Modifier.weight(1f), "${validas.size} ventas · $unidades prendas")
                        TarjetaMetrica("Ganancia", Dinero.cop(ganancia), Modifier.weight(1f),
                            if (vendidoCop > 0) "${ganancia * 100 / vendidoCop}% del vendido" else null)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TarjetaMetrica("Inventario", "${disponibles.size} prendas", Modifier.weight(1f), "a la venta")
                        TarjetaMetrica("Valor inventario", Dinero.cop(valorVenta), Modifier.weight(1f), "costo ${Dinero.cop(valorCosto)}")
                    }
                }
            }
            item(key = "titulo") {
                Text(
                    "Historial de ventas",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (enPeriodo.isEmpty()) {
                item(key = "vacio") {
                    Text(
                        "No hay ventas en este periodo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(enPeriodo, key = { it.id }) { v ->
                FilaVenta(v, itemsPorVenta[v.id].orEmpty()) { ventaAbierta = v }
                HorizontalDivider(color = Paleta.GrisClaro)
            }
        }
    }

    ventaAbierta?.let { v ->
        val lista = items.filter { it.ventaId == v.id }
        AlertDialog(
            onDismissRequest = { ventaAbierta = null },
            title = { Text("Venta · ${Fechas.completa(v.fecha)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    lista.forEach {
                        Row {
                            Text("${it.nombre} · T ${it.talla}", modifier = Modifier.weight(1f))
                            Text(Dinero.monto(v.moneda, it.precioCobrado))
                        }
                    }
                    HorizontalDivider()
                    Row {
                        Text("Total", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                        Text(Dinero.monto(v.moneda, v.total), style = MaterialTheme.typography.titleSmall)
                    }
                    Text("Ganancia ${Dinero.cop(v.gananciaCop)} · tasa ${Dinero.tasaTexto(v.tasa)}", style = MaterialTheme.typography.bodySmall)
                    if (v.revertida) Etiqueta("Devuelta", Paleta.RojoClaro, Paleta.Rojo)
                }
            },
            confirmButton = { TextButton(onClick = { ventaAbierta = null }) { Text("Cerrar") } },
            dismissButton = {
                if (!v.revertida) {
                    TextButton(onClick = { vm.revertirVenta(v.id); ventaAbierta = null }) {
                        Text("Revertir venta", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
        )
    }
}

@Composable
private fun FilaVenta(v: Venta, items: List<VentaItem>, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                items.joinToString(", ") { it.nombre }.ifEmpty { "Venta" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${Fechas.diaCorto(v.fecha)}, ${Fechas.hora(v.fecha)} · ganancia ${Dinero.cop(v.gananciaCop)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(Dinero.monto(v.moneda, v.total), style = MaterialTheme.typography.titleMedium)
            if (v.revertida) Etiqueta("Devuelta", Paleta.RojoClaro, Paleta.Rojo)
        }
    }
}
