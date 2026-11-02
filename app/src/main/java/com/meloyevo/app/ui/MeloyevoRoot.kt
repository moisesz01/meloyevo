package com.meloyevo.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.meloyevo.app.AppViewModel
import com.meloyevo.app.ui.theme.Paleta
import kotlinx.coroutines.launch

private data class Destino(val ruta: String, val etiqueta: String, val icono: ImageVector)

private val destinos = listOf(
    Destino("inventario", "Inventario", Icons.Outlined.Checkroom),
    Destino("caja", "Caja", Icons.Outlined.Payments),
    Destino("resumen", "Resumen", Icons.Outlined.Insights),
)

@Composable
fun MeloyevoRoot(vm: AppViewModel) {
    val nav = rememberNavController()
    val entrada by nav.currentBackStackEntryAsState()
    val rutaActual = entrada?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val tecladoAbierto = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val mostrarBarra = rutaActual in destinos.map { it.ruta } && !tecladoAbierto

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (mostrarBarra) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    destinos.forEach { d ->
                        NavigationBarItem(
                            selected = rutaActual == d.ruta,
                            onClick = { irA(nav, d.ruta) },
                            icon = { Icon(d.icono, null) },
                            label = { Text(d.etiqueta) },
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Paleta.MentaClara),
                        )
                    }
                }
            }
        },
    ) { pad ->
        NavHost(
            navController = nav,
            startDestination = "inventario",
            modifier = Modifier.padding(pad).consumeWindowInsets(pad),
        ) {
            composable("inventario") {
                InventarioScreen(
                    vm,
                    onAbrirPrenda = { id -> nav.navigate("detalle/$id") },
                    onNuevaPrenda = { vm.pedirGestion { nav.navigate("editar/0") } },
                )
            }
            composable("caja") { CajaScreen(vm) }
            composable("resumen") { ResumenScreen(vm, onAjustes = { nav.navigate("ajustes") }) }
            composable("ajustes") { AjustesScreen(vm, onVolver = { nav.popBackStack() }) }
            composable(
                "detalle/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e ->
                val id = e.arguments?.getLong("id") ?: 0L
                DetalleScreen(
                    vm, id,
                    onVolver = { nav.popBackStack() },
                    onEditar = { nav.navigate("editar/$id") },
                )
            }
            composable(
                "editar/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { e ->
                val id = e.arguments?.getLong("id") ?: 0L
                EditarScreen(
                    vm, id,
                    onVolver = { nav.popBackStack() },
                    onEliminada = { nav.popBackStack("inventario", inclusive = false) },
                )
            }
        }
    }

    // Hoja de venta: disponible desde cualquier pantalla
    VentaSheet(vm) { ventaId, total ->
        scope.launch {
            val r = snackbar.showSnackbar(
                message = "Venta registrada · $total",
                actionLabel = "Deshacer",
                duration = SnackbarDuration.Long,
            )
            if (r == SnackbarResult.ActionPerformed) vm.deshacerVenta(ventaId)
        }
    }

    // Diálogo de PIN cuando una acción requiere Modo Gestión
    val pendiente by vm.pinPendiente.collectAsStateWithLifecycle()
    if (pendiente != null) DialogoPin(vm)
}

private fun irA(nav: NavHostController, ruta: String) {
    nav.navigate(ruta) {
        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun DialogoPin(vm: AppViewModel) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val foco = remember { FocusRequester() }
    fun probar() {
        if (!vm.intentarPin(pin)) {
            error = true
            pin = ""
        }
    }
    AlertDialog(
        onDismissRequest = vm::cancelarPin,
        title = { Text("Modo Gestión") },
        text = {
            Column {
                Text("Escribe tu PIN para ver los datos internos.")
                OutlinedTextField(
                    value = pin,
                    onValueChange = { nuevo ->
                        pin = nuevo.filter(Char::isDigit).take(6)
                        error = false
                    },
                    singleLine = true,
                    isError = error,
                    supportingText = if (error) {
                        { Text("PIN incorrecto") }
                    } else null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center, letterSpacing = 8.sp),
                    modifier = Modifier.focusRequester(foco).padding(top = 12.dp),
                )
                // Dentro del contenido del diálogo: el campo ya está adjunto cuando esto corre
                LaunchedEffect(Unit) { foco.requestFocus() }
            }
        },
        confirmButton = { TextButton(onClick = { probar() }, enabled = pin.length >= 4) { Text("Desbloquear") } },
        dismissButton = { TextButton(onClick = vm::cancelarPin) { Text("Cancelar") } },
    )
}
