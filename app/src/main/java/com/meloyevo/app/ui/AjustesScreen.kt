package com.meloyevo.app.ui

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meloyevo.app.AppViewModel
import com.meloyevo.app.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(vm: AppViewModel, onVolver: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val gestion by vm.gestion.collectAsStateWithLifecycle()
    var dialogoPin by remember { mutableStateOf(false) }
    var confirmarRestaurar by remember { mutableStateOf<android.net.Uri?>(null) }
    var trabajando by remember { mutableStateOf(false) }

    val exportar = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) scope.launch {
            trabajando = true
            val ok = vm.exportarRespaldo(uri)
            trabajando = false
            Toast.makeText(
                context,
                if (ok) "Respaldo guardado. Súbelo a Drive o envíatelo por WhatsApp." else "No se pudo crear el respaldo.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }
    val restaurar = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) confirmarRestaurar = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = { IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { pad ->
        if (!gestion) {
            Column(Modifier.padding(pad)) { PantallaBloqueada("Ajustes protegidos") { vm.pedirGestion() } }
            return@Scaffold
        }
        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Seccion("Redondeo del precio en pesos", "Los precios automáticos se redondean hacia arriba.")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val opciones = listOf(0 to "Exacto", 100 to "100", 500 to "500", 1000 to "1.000")
                opciones.forEachIndexed { i, (paso, etiqueta) ->
                    SegmentedButton(
                        selected = ajustes.redondeo == paso,
                        onClick = { vm.setRedondeo(paso) },
                        shape = SegmentedButtonDefaults.itemShape(i, opciones.size),
                    ) { Text(etiqueta) }
                }
            }
            HorizontalDivider()

            Seccion("Bloqueo automático", "Tiempo sin uso antes de volver a Modo Mostrador.")
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                val opciones = listOf(60 to "1 min", 120 to "2 min", 300 to "5 min")
                opciones.forEachIndexed { i, (seg, etiqueta) ->
                    SegmentedButton(
                        selected = ajustes.autoBloqueoSeg == seg,
                        onClick = { vm.setAutoBloqueo(seg); vm.actividad() },
                        shape = SegmentedButtonDefaults.itemShape(i, opciones.size),
                    ) { Text(etiqueta) }
                }
            }
            HorizontalDivider()

            Seccion(
                "PIN",
                if (ajustes.tienePin) "Se pide para ver costos, caja y resumen."
                else "Sin PIN, el ojo muestra los datos internos con un toque.",
            )
            OutlinedButton(onClick = { dialogoPin = true }, modifier = Modifier.fillMaxWidth()) {
                Text(if (ajustes.tienePin) "Cambiar PIN" else "Crear PIN")
            }
            if (ajustes.tienePin) {
                TextButton(onClick = { vm.setPin(null) }) {
                    Text("Quitar PIN", color = MaterialTheme.colorScheme.error)
                }
            }
            HorizontalDivider()

            Seccion(
                "Respaldo",
                "Un solo archivo con prendas, fotos, ventas y caja. Guárdalo fuera del celular (Drive, WhatsApp, correo).",
            )
            OutlinedButton(
                onClick = {
                    val fecha = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
                    vm.permitirSalida()
                    exportar.launch("meloyevo-respaldo-$fecha.zip")
                },
                enabled = !trabajando,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Crear respaldo") }
            OutlinedButton(
                onClick = { vm.permitirSalida(); restaurar.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                enabled = !trabajando,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Restaurar respaldo") }
            HorizontalDivider()

            Text(
                "Meloyevo ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (dialogoPin) {
        DialogoNuevoPin(
            onGuardar = { vm.setPin(it); dialogoPin = false },
            onCerrar = { dialogoPin = false },
        )
    }

    confirmarRestaurar?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmarRestaurar = null },
            title = { Text("¿Restaurar este respaldo?") },
            text = { Text("Todos los datos actuales se reemplazarán por los del respaldo. La app se reiniciará.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarRestaurar = null
                    scope.launch {
                        trabajando = true
                        val ok = vm.importarRespaldo(uri)
                        trabajando = false
                        if (ok) {
                            reiniciar(context)
                        } else {
                            Toast.makeText(context, "Ese archivo no es un respaldo válido de Meloyevo.", Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text("Restaurar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmarRestaurar = null }) { Text("Cancelar") } },
        )
    }
}

private fun reiniciar(context: android.content.Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    if (intent != null) context.startActivity(intent)
    (context as? Activity)?.finishAffinity()
    exitProcess(0)
}

@Composable
private fun Seccion(titulo: String, descripcion: String) {
    Column {
        Text(titulo, style = MaterialTheme.typography.titleSmall)
        Text(descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DialogoNuevoPin(onGuardar: (String) -> Unit, onCerrar: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var repetir by remember { mutableStateOf("") }
    val valido = pin.length in 4..6 && pin == repetir
    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Nuevo PIN") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(6) },
                    label = { Text("PIN (4 a 6 dígitos)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
                OutlinedTextField(
                    value = repetir,
                    onValueChange = { repetir = it.filter(Char::isDigit).take(6) },
                    label = { Text("Repite el PIN") },
                    singleLine = true,
                    isError = repetir.isNotEmpty() && repetir != pin,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onGuardar(pin) }, enabled = valido) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } },
    )
}
