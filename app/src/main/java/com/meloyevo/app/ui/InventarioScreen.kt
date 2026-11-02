package com.meloyevo.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meloyevo.app.AppViewModel
import com.meloyevo.app.FiltroEstado
import com.meloyevo.app.data.Ajustes
import com.meloyevo.app.data.ESTADO_DISPONIBLE
import com.meloyevo.app.data.Prenda
import com.meloyevo.app.ui.theme.Paleta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventarioScreen(
    vm: AppViewModel,
    onAbrirPrenda: (Long) -> Unit,
    onNuevaPrenda: () -> Unit,
) {
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val gestion by vm.gestion.collectAsStateWithLifecycle()
    val segundos by vm.segundosRestantes.collectAsStateWithLifecycle()
    val lista by vm.prendasFiltradas.collectAsStateWithLifecycle()
    val tallas by vm.tallasPresentes.collectAsStateWithLifecycle()
    val consulta by vm.consulta.collectAsStateWithLifecycle()
    val talla by vm.filtroTalla.collectAsStateWithLifecycle()
    val estado by vm.filtroEstado.collectAsStateWithLifecycle()
    val disponibles by vm.disponibles.collectAsStateWithLifecycle()
    val foco = LocalFocusManager.current

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Meloyevo") },
                    actions = {
                        Text(
                            "$disponibles disp.",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(onClick = vm::alternarModo) {
                            Icon(
                                if (gestion) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (gestion) "Ocultar datos internos" else "Ver datos internos",
                            )
                        }
                        IconButton(onClick = onNuevaPrenda) {
                            Icon(Icons.Outlined.Add, contentDescription = "Agregar prenda")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
                if (gestion) BannerGestion(segundos, vm::bloquear)
            }
        },
        bottomBar = {
            Column(Modifier.imePadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Spacer(Modifier.weight(1f))
                    ExtendedFloatingActionButton(
                        onClick = { foco.clearFocus(); vm.abrirVenta() },
                        icon = { Icon(Icons.Outlined.ShoppingBag, null) },
                        text = { Text("Vender") },
                        containerColor = Paleta.Coral,
                        contentColor = Paleta.CoralOscuro,
                    )
                }
                BarraBusqueda(
                    texto = consulta,
                    onTexto = { vm.consulta.value = it },
                    alBuscar = { foco.clearFocus() },
                )
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                FiltroEstado.entries.forEachIndexed { i, f ->
                    SegmentedButton(
                        selected = estado == f,
                        onClick = { vm.filtroEstado.value = f; vm.filtroTalla.value = null },
                        shape = SegmentedButtonDefaults.itemShape(i, FiltroEstado.entries.size),
                    ) { Text(f.etiqueta) }
                }
            }
            if (tallas.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(tallas, key = { it }) { t ->
                        FilterChip(
                            selected = talla == t,
                            onClick = { vm.filtroTalla.value = if (talla == t) null else t },
                            label = { Text(t) },
                        )
                    }
                }
            }
            if (lista.isEmpty()) {
                val hayFiltros = consulta.isNotBlank() || talla != null
                when {
                    hayFiltros -> EstadoVacio(
                        "Sin coincidencias",
                        if (consulta.isNotBlank()) "Nada coincide con «$consulta»." else "No hay prendas en esa talla.",
                        "Limpiar filtros", vm::limpiarFiltros,
                    )
                    estado == FiltroEstado.VENDIDAS -> EstadoVacio(
                        "Aún no hay ventas", "Las prendas vendidas aparecerán aquí.",
                    )
                    else -> EstadoVacio(
                        "Agrega tu primera prenda",
                        "Toma una foto, ponle nombre, talla y precio. Así la encontrarás en segundos.",
                        "Agregar prenda", onNuevaPrenda,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(lista, key = { it.id }, contentType = { "prenda" }) { p ->
                        FilaDeslizable(
                            p = p,
                            ajustes = ajustes,
                            gestion = gestion,
                            onAbrir = { onAbrirPrenda(p.id) },
                            onVender = { foco.clearFocus(); vm.abrirVenta(p.id) },
                        )
                    }
                }
            }
        }
    }
}

/** Deslizar a la derecha abre la venta (nunca vende directamente). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilaDeslizable(
    p: Prenda,
    ajustes: Ajustes,
    gestion: Boolean,
    onAbrir: () -> Unit,
    onVender: () -> Unit,
) {
    if (p.estado != ESTADO_DISPONIBLE) {
        TarjetaPrenda(p, ajustes, gestion, onAbrir, onVender)
        return
    }
    val vender by rememberUpdatedState(onVender)
    val estadoSwipe = rememberSwipeToDismissBoxState(
        confirmValueChange = { valor ->
            if (valor == SwipeToDismissBoxValue.StartToEnd) vender()
            false // la tarjeta siempre regresa a su lugar
        },
        positionalThreshold = { distancia -> distancia * 0.35f },
    )
    SwipeToDismissBox(
        state = estadoSwipe,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Paleta.Coral)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.ShoppingBag, null, tint = Paleta.CoralOscuro)
                    Spacer(Modifier.width(8.dp))
                    Text("Vender", color = Paleta.CoralOscuro, style = MaterialTheme.typography.titleMedium)
                }
            }
        },
    ) {
        TarjetaPrenda(p, ajustes, gestion, onAbrir, onVender)
    }
}

@Composable
private fun BarraBusqueda(texto: String, onTexto: (String) -> Unit, alBuscar: () -> Unit) {
    OutlinedTextField(
        value = texto,
        onValueChange = onTexto,
        placeholder = { Text("Buscar prenda o talla…") },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        trailingIcon = {
            if (texto.isNotEmpty()) {
                IconButton(onClick = { onTexto("") }) { Icon(Icons.Outlined.Close, contentDescription = "Borrar búsqueda") }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { alBuscar() }),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
