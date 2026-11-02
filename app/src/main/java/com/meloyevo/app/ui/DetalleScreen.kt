package com.meloyevo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meloyevo.app.AppViewModel
import com.meloyevo.app.data.ESTADO_DISPONIBLE
import com.meloyevo.app.ui.theme.Paleta
import com.meloyevo.app.util.Dinero
import com.meloyevo.app.util.Fechas
import com.meloyevo.app.util.Precios

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleScreen(vm: AppViewModel, id: Long, onVolver: () -> Unit, onEditar: () -> Unit) {
    val prenda by remember(id) { vm.repo.prenda(id) }.collectAsState(initial = null)
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val gestion by vm.gestion.collectAsStateWithLifecycle()
    val segundos by vm.segundosRestantes.collectAsStateWithLifecycle()
    var fotoCompleta by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        IconButton(onClick = onVolver) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
                if (gestion) BannerGestion(segundos, vm::bloquear)
            }
        },
        bottomBar = {
            val p = prenda
            if (p != null) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { vm.pedirGestion(onEditar) },
                            modifier = Modifier.weight(1f).height(52.dp),
                        ) {
                            Icon(Icons.Outlined.Edit, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Editar")
                        }
                        if (p.estado == ESTADO_DISPONIBLE) {
                            Button(
                                onClick = { vm.abrirVenta(p.id) },
                                modifier = Modifier.weight(1f).height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Paleta.Coral, contentColor = Paleta.CoralOscuro,
                                ),
                            ) {
                                Icon(Icons.Outlined.ShoppingBag, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Vender")
                            }
                        }
                    }
                }
            }
        },
    ) { pad ->
        val p = prenda ?: return@Scaffold
        val vendida = p.estado != ESTADO_DISPONIBLE
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            FotoPrenda(
                p.foto, p.nombre, grande = true, apagada = vendida,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clickable(enabled = p.foto != null) { fotoCompleta = true },
            )
            Spacer(Modifier.height(16.dp))
            Text(Dinero.cop(Precios.precioFinalCop(p, ajustes)), style = MaterialTheme.typography.headlineMedium)
            Text(p.nombre, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EtiquetaTalla(p.talla)
                if (vendida) {
                    Etiqueta(
                        "Vendida" + (p.vendidoEn?.let { " · ${Fechas.diaCorto(it)}" } ?: ""),
                        Paleta.GrisClaro, Paleta.Gris,
                    )
                } else {
                    Etiqueta("Disponible", Paleta.VerdeClaro, Paleta.Verde)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                Dinero.usd(p.precioUsdCents),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (gestion) {
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Paleta.LavandaClara.copy(alpha = 0.45f), MaterialTheme.shapes.medium)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Costos y margen", style = MaterialTheme.typography.titleSmall, color = Paleta.LavandaOscura)
                    FilaDato("Costo", "${Dinero.usd(p.costoUsdCents)} · ${Dinero.cop(Precios.costoCop(p, ajustes))}")
                    FilaDato("Precio venta USD", Dinero.usd(p.precioUsdCents))
                    FilaDato(
                        "Precio COP calculado",
                        "${Dinero.cop(Precios.copCalculado(p, ajustes))} → ${Dinero.cop(Precios.copRedondeado(p, ajustes))}",
                    )
                    FilaDato("Precio", if (p.precioCopFijo != null) "Fijado a mano" else "Automático (sigue la tasa)")
                    HorizontalDivider(color = Paleta.LavandaClara)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ganancia", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(Dinero.cop(Precios.gananciaCop(p, ajustes)), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        EtiquetaMargen(Precios.margenPct(p, ajustes))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (fotoCompleta && p.foto != null) {
            Dialog(
                onDismissRequest = { fotoCompleta = false },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable { fotoCompleta = false },
                    contentAlignment = Alignment.Center,
                ) {
                    FotoPrenda(
                        p.foto, p.nombre, grande = true,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Row {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}
