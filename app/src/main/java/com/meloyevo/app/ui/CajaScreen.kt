package com.meloyevo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meloyevo.app.AppViewModel
import com.meloyevo.app.data.ESTADO_DISPONIBLE
import com.meloyevo.app.data.MONEDA_COP
import com.meloyevo.app.data.MONEDA_USD
import com.meloyevo.app.data.MOV_AJUSTE
import com.meloyevo.app.data.MOV_CAMBIO
import com.meloyevo.app.data.MOV_ENTRADA
import com.meloyevo.app.data.MOV_REVERSION
import com.meloyevo.app.data.MOV_SALIDA
import com.meloyevo.app.data.MOV_VENTA
import com.meloyevo.app.data.Movimiento
import com.meloyevo.app.ui.theme.Paleta
import com.meloyevo.app.util.Dinero
import com.meloyevo.app.util.Fechas
import com.meloyevo.app.util.Precios

/** Pantalla que se muestra en lugar de datos internos cuando está en Modo Mostrador. */
@Composable
fun PantallaBloqueada(titulo: String, onDesbloquear: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.Lock, null, tint = Paleta.Lavanda, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Text(
            "Contiene datos internos. Desbloquéala cuando el cliente no esté mirando.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onDesbloquear) { Text("Desbloquear") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CajaScreen(vm: AppViewModel) {
    val gestion by vm.gestion.collectAsStateWithLifecycle()
    val segundos by vm.segundosRestantes.collectAsStateWithLifecycle()
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val saldoCop by vm.saldoCop.collectAsStateWithLifecycle()
    val saldoUsd by vm.saldoUsd.collectAsStateWithLifecycle()
    val movimientos by vm.movimientos.collectAsStateWithLifecycle()
    val ventas by vm.ventas.collectAsStateWithLifecycle()
    val prendas by vm.prendas.collectAsStateWithLifecycle()

    var dialogoTasa by remember { mutableStateOf(false) }
    var hojaMovimiento by remember { mutableStateOf(false) }
    var dialogoCuadre by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Caja") },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
                if (gestion) BannerGestion(segundos, vm::bloquear)
            }
        },
        bottomBar = {
            if (gestion) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(onClick = { hojaMovimiento = true }, modifier = Modifier.weight(1.4f).height(48.dp)) {
                            Text("Registrar movimiento", maxLines = 1)
                        }
                        OutlinedButton(onClick = { dialogoCuadre = true }, modifier = Modifier.weight(1f).height(48.dp)) {
                            Text("Cuadrar")
                        }
                    }
                }
            }
        },
    ) { pad ->
        if (!gestion) {
            Column(Modifier.padding(pad)) { PantallaBloqueada("Caja protegida") { vm.pedirGestion() } }
            return@Scaffold
        }
        val hoy = Fechas.inicioDelDia()
        val ventasHoy = ventas.filter { it.fecha >= hoy && !it.revertida }
        val totalHoyCop = ventasHoy.sumOf { if (it.moneda == MONEDA_USD) Precios.usdACop(it.total, it.tasa) else it.total }
        val gananciaHoy = ventasHoy.sumOf { it.gananciaCop }
        val porDia = remember(movimientos) { movimientos.groupBy { Fechas.inicioDelDia(it.fecha) } }

        LazyColumn(
            Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "tasa") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(0.5.dp, Paleta.GrisClaro, RoundedCornerShape(12.dp))
                        .clickable { dialogoTasa = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Tasa: 1 US$ = $ ${Dinero.tasaTexto(ajustes.tasa)}", style = MaterialTheme.typography.titleMedium)
                        val dias = Fechas.hace(ajustes.tasaActualizadaEn)
                        Text(
                            "Actualizada $dias",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (ajustes.tasaActualizadaEn == 0L || System.currentTimeMillis() - ajustes.tasaActualizadaEn > 7 * 86_400_000L)
                                Paleta.Ambar else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Outlined.Edit, contentDescription = "Editar tasa", tint = MaterialTheme.colorScheme.primary)
                }
            }
            item(key = "saldos") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TarjetaMetrica("Efectivo USD", Dinero.usd(saldoUsd), Modifier.weight(1f))
                    TarjetaMetrica("Efectivo COP", Dinero.cop(saldoCop), Modifier.weight(1f))
                }
            }
            item(key = "resumen") {
                Column {
                    Text(
                        "Total equivalente ${Dinero.cop(saldoCop + Precios.usdACop(saldoUsd, ajustes.tasa))}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        "Hoy: ${ventasHoy.size} ${if (ventasHoy.size == 1) "venta" else "ventas"} · ${Dinero.cop(totalHoyCop)} · ganancia ${Dinero.cop(gananciaHoy)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (movimientos.isEmpty()) {
                item(key = "vacio") {
                    Text(
                        "Registra el efectivo con el que abres la caja como una entrada («Saldo inicial»). Las ventas se suman solas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
            porDia.forEach { (dia, lista) ->
                item(key = "dia$dia") {
                    Text(
                        if (dia == hoy) "Hoy" else Fechas.diaLargo(dia),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(lista, key = { "m${it.id}" }) { FilaMovimiento(it) }
            }
        }
    }

    if (dialogoTasa) {
        val disponibles = prendas.filter { it.estado == ESTADO_DISPONIBLE }
        DialogoTasa(
            tasaActual = ajustes.tasa,
            automaticas = disponibles.count { it.precioCopFijo == null },
            fijadas = disponibles.count { it.precioCopFijo != null },
            onAplicar = { vm.setTasa(it); dialogoTasa = false },
            onCerrar = { dialogoTasa = false },
        )
    }
    if (hojaMovimiento) {
        HojaMovimiento(vm, ajustes.tasa) { hojaMovimiento = false }
    }
    if (dialogoCuadre) {
        DialogoCuadre(
            esperadoCop = saldoCop, esperadoUsd = saldoUsd,
            onRegistrar = { c, u -> vm.cuadrar(c, u); dialogoCuadre = false },
            onCerrar = { dialogoCuadre = false },
        )
    }
}

@Composable
private fun FilaMovimiento(m: Movimiento) {
    val titulo = when (m.tipo) {
        MOV_VENTA -> "Venta"
        MOV_ENTRADA -> "Entrada"
        MOV_SALIDA -> "Salida"
        MOV_CAMBIO -> if (m.monto < 0) "Cambio · entregado" else "Cambio · recibido"
        MOV_AJUSTE -> "Ajuste de cuadre"
        MOV_REVERSION -> "Devolución"
        else -> m.tipo
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                if (m.nota.isNotBlank() && m.tipo != MOV_AJUSTE) "$titulo · ${m.nota}" else titulo,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(Fechas.hora(m.fecha), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Text(
            (if (m.monto >= 0) "+ " else "") + Dinero.monto(m.moneda, m.monto),
            style = MaterialTheme.typography.titleMedium,
            color = if (m.monto >= 0) Paleta.Verde else Paleta.Rojo,
        )
    }
}

@Composable
private fun DialogoTasa(
    tasaActual: Double,
    automaticas: Int,
    fijadas: Int,
    onAplicar: (Double) -> Unit,
    onCerrar: () -> Unit,
) {
    var texto by remember { mutableStateOf(Dinero.tasaTexto(tasaActual)) }
    val nueva = Dinero.parseTasa(texto)
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Tasa de cambio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it.take(12) },
                    label = { Text("Pesos por 1 dólar") },
                    prefix = { Text("$ ") },
                    singleLine = true,
                    isError = nueva == null,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                    ),
                )
                Text(
                    "$automaticas ${if (automaticas == 1) "prenda cambiará" else "prendas cambiarán"} de precio" +
                        if (fijadas > 0) ". $fijadas con precio fijado no cambian." else ".",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { nueva?.let(onAplicar) }, enabled = nueva != null) { Text("Aplicar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HojaMovimiento(vm: AppViewModel, tasa: Double, onCerrar: () -> Unit) {
    val estado = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tipo by remember { mutableStateOf(MOV_ENTRADA) }
    var moneda by remember { mutableStateOf(MONEDA_COP) }
    var monto by remember { mutableStateOf("") }
    var recibe by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }

    fun valor(m: String, t: String) = if (m == MONEDA_USD) Dinero.parseUsdCents(t) else Dinero.parseCop(t)
    val otraMoneda = if (moneda == MONEDA_USD) MONEDA_COP else MONEDA_USD
    val sugerido = valor(moneda, monto)?.let {
        if (moneda == MONEDA_USD) Precios.usdACop(it, tasa) else (it / tasa * 100).toLong()
    }

    ModalBottomSheet(onDismissRequest = onCerrar, sheetState = estado) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Registrar movimiento", style = MaterialTheme.typography.titleLarge)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(MOV_ENTRADA to "Entrada", MOV_SALIDA to "Salida", MOV_CAMBIO to "Cambio").forEachIndexed { i, (t, e) ->
                    SegmentedButton(
                        selected = tipo == t,
                        onClick = { tipo = t },
                        shape = SegmentedButtonDefaults.itemShape(i, 3),
                    ) { Text(e) }
                }
            }
            Text(
                if (tipo == MOV_CAMBIO) "Moneda que entregas" else "Moneda",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                listOf(MONEDA_COP to "Pesos", MONEDA_USD to "Dólares").forEachIndexed { i, (m, e) ->
                    SegmentedButton(
                        selected = moneda == m,
                        onClick = { moneda = m; monto = ""; recibe = "" },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                    ) { Text(e) }
                }
            }
            val etiquetaMonto = if (tipo == MOV_CAMBIO) "Entregas" else "Monto"
            if (moneda == MONEDA_USD) CampoUsd(monto, { monto = it }, etiquetaMonto)
            else CampoCop(monto, { monto = it }, etiquetaMonto)

            if (tipo == MOV_CAMBIO) {
                val apoyo = sugerido?.let { "Con tu tasa serían ${Dinero.monto(otraMoneda, it)}" }
                if (otraMoneda == MONEDA_USD) CampoUsd(recibe, { recibe = it }, "Recibes", apoyo = apoyo)
                else CampoCop(recibe, { recibe = it }, "Recibes", apoyo = apoyo)
            }
            OutlinedTextField(
                value = nota,
                onValueChange = { nota = it.take(60) },
                label = { Text("Nota (opcional)") },
                placeholder = { Text(if (tipo == MOV_SALIDA) "Bolsas, retiro personal…" else "Saldo inicial…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            val montoValor = valor(moneda, monto)
            val recibeValor = if (tipo == MOV_CAMBIO) (valor(otraMoneda, recibe) ?: sugerido) else null
            val valido = montoValor != null && montoValor > 0 && (tipo != MOV_CAMBIO || (recibeValor != null && recibeValor > 0))
            Button(
                onClick = {
                    if (montoValor == null) return@Button
                    if (tipo == MOV_CAMBIO && recibeValor != null) vm.registrarCambio(moneda, montoValor, recibeValor, nota.trim())
                    else vm.registrarMovimiento(tipo, moneda, montoValor, nota.trim())
                    onCerrar()
                },
                enabled = valido,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("Guardar") }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DialogoCuadre(
    esperadoCop: Long,
    esperadoUsd: Long,
    onRegistrar: (Long, Long) -> Unit,
    onCerrar: () -> Unit,
) {
    var cop by remember { mutableStateOf("") }
    var usd by remember { mutableStateOf("") }
    val contadoCop = Dinero.parseCop(cop)
    val contadoUsd = Dinero.parseUsdCents(usd)
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Cuadrar caja") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Escribe lo que contaste realmente.", style = MaterialTheme.typography.bodyMedium)
                CampoCop(cop, { cop = it }, "Pesos contados", apoyo = "Esperado: ${Dinero.cop(esperadoCop)}")
                LineaDiferencia(contadoCop?.minus(esperadoCop), MONEDA_COP)
                CampoUsd(usd, { usd = it }, "Dólares contados", apoyo = "Esperado: ${Dinero.usd(esperadoUsd)}")
                LineaDiferencia(contadoUsd?.minus(esperadoUsd), MONEDA_USD)
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onRegistrar(contadoCop ?: esperadoCop, contadoUsd ?: esperadoUsd) },
                enabled = contadoCop != null || contadoUsd != null,
            ) { Text("Registrar ajuste") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}

@Composable
private fun LineaDiferencia(dif: Long?, moneda: String) {
    if (dif == null) return
    val (texto, color) = when {
        dif == 0L -> "Cuadra exacto" to Paleta.Verde
        dif > 0 -> "Sobran ${Dinero.monto(moneda, dif)}" to Paleta.Ambar
        else -> "Faltan ${Dinero.monto(moneda, -dif)}" to Paleta.Rojo
    }
    Text(texto, style = MaterialTheme.typography.titleSmall, color = color)
}
