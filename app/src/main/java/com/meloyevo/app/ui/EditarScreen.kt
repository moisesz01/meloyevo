package com.meloyevo.app.ui

import android.content.ActivityNotFoundException
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meloyevo.app.AppViewModel
import com.meloyevo.app.data.Prenda
import com.meloyevo.app.ui.theme.Paleta
import com.meloyevo.app.util.Dinero
import com.meloyevo.app.util.Precios
import com.meloyevo.app.util.Tallas
import com.meloyevo.app.util.Texto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditarScreen(vm: AppViewModel, id: Long, onVolver: () -> Unit, onEliminada: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ajustes by vm.ajustes.collectAsStateWithLifecycle()
    val gestion by vm.gestion.collectAsStateWithLifecycle()
    val segundos by vm.segundosRestantes.collectAsStateWithLifecycle()
    val nombresUsados by vm.nombresUsados.collectAsStateWithLifecycle()
    val esNueva = id == 0L

    var original by remember { mutableStateOf<Prenda?>(null) }
    var cargado by rememberSaveable { mutableStateOf(esNueva) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var talla by rememberSaveable { mutableStateOf("") }
    var otraTalla by rememberSaveable { mutableStateOf(false) }
    var costo by rememberSaveable { mutableStateOf("") }
    var precioUsd by rememberSaveable { mutableStateOf("") }
    var fijado by rememberSaveable { mutableStateOf(false) }
    var copFijo by rememberSaveable { mutableStateOf("") }
    var foto by rememberSaveable { mutableStateOf<String?>(null) }
    var camaraAbierta by rememberSaveable { mutableStateOf(false) }
    var procesando by remember { mutableStateOf(false) }
    var errores by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirmarBorrado by remember { mutableStateOf(false) }
    var puedeBorrar by remember { mutableStateOf(false) }

    LaunchedEffect(id) {
        if (!esNueva) {
            val p = vm.repo.obtenerPrenda(id)
            original = p
            puedeBorrar = vm.puedeEliminar(id)
            if (!cargado && p != null) {
                nombre = p.nombre
                talla = p.talla
                otraTalla = p.talla !in Tallas.sugeridas
                costo = Dinero.usdEditable(p.costoUsdCents)
                precioUsd = Dinero.usdEditable(p.precioUsdCents)
                fijado = p.precioCopFijo != null
                copFijo = p.precioCopFijo?.toString() ?: ""
                foto = p.foto
                cargado = true
            }
        }
    }
    // Escribir también cuenta como "uso": evita el bloqueo automático a mitad de la edición
    LaunchedEffect(nombre, talla, costo, precioUsd, copFijo, fijado) { vm.actividad() }

    // ----- Fotos -----
    val capturaUri: Uri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", vm.repo.imagenes.archivoCaptura())
    }
    fun procesar(uri: Uri) {
        scope.launch {
            procesando = true
            val nueva = vm.importarFoto(uri)
            if (nueva != null) foto = nueva
            else Toast.makeText(context, "No se pudo leer la foto. Intenta de nuevo.", Toast.LENGTH_SHORT).show()
            procesando = false
        }
    }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) procesar(capturaUri)
    }
    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) procesar(uri)
    }
    fun abrirCamara() {
        try {
            vm.permitirSalida()
            camara.launch(capturaUri)
        } catch (e: ActivityNotFoundException) {
            vm.cancelarSalida()
            Toast.makeText(context, "No hay app de cámara. Elige una foto de la galería.", Toast.LENGTH_SHORT).show()
        }
    }
    // Prenda nueva: la cámara se abre de inmediato (la foto es el primer paso)
    LaunchedEffect(Unit) {
        if (esNueva && !camaraAbierta && gestion) {
            camaraAbierta = true
            abrirCamara()
        }
    }

    // ----- Cálculos en vivo -----
    val costoCents = Dinero.parseUsdCents(costo)
    val precioCents = Dinero.parseUsdCents(precioUsd)
    val calculado = precioCents?.let { Precios.usdACop(it, ajustes.tasa) }
    val redondeado = calculado?.let { Precios.redondear(it, ajustes.redondeo) }
    val precioFinal = if (fijado) Dinero.parseCop(copFijo) else redondeado
    val ganancia = if (precioFinal != null) precioFinal - Precios.usdACop(costoCents ?: 0, ajustes.tasa) else null
    val margen = if (ganancia != null && precioFinal != null && precioFinal > 0) (ganancia * 100 / precioFinal).toInt() else null

    fun construir(): Prenda? {
        errores = true
        val n = nombre.trim()
        val t = talla.trim()
        if (n.isEmpty() || t.isEmpty() || precioCents == null) return null
        if (fijado && Dinero.parseCop(copFijo) == null) return null
        val base = original
        return if (base != null) base.copy(
            nombre = n, talla = t, costoUsdCents = costoCents ?: 0, precioUsdCents = precioCents,
            precioCopFijo = if (fijado) Dinero.parseCop(copFijo) else null, foto = foto,
        ) else Prenda(
            nombre = n, talla = t, costoUsdCents = costoCents ?: 0, precioUsdCents = precioCents,
            precioCopFijo = if (fijado) Dinero.parseCop(copFijo) else null, foto = foto,
        )
    }

    fun guardar(otraMas: Boolean) {
        val p = construir() ?: run {
            Toast.makeText(context, "Completa nombre, talla y precio.", Toast.LENGTH_SHORT).show()
            return
        }
        scope.launch {
            vm.guardarPrenda(p)
            if (otraMas) {
                // Conserva foto, nombre y precios; solo cambia la talla
                talla = ""
                otraTalla = false
                errores = false
                Toast.makeText(context, "Guardada. Elige la siguiente talla.", Toast.LENGTH_SHORT).show()
            } else {
                onVolver()
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(if (esNueva) "Nueva prenda" else "Editar prenda") },
                    navigationIcon = {
                        IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") }
                    },
                    actions = {
                        if (!esNueva && puedeBorrar && gestion) {
                            Box {
                                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Más opciones") }
                                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("Eliminar prenda") },
                                        onClick = { menu = false; confirmarBorrado = true },
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                )
                if (gestion) BannerGestion(segundos, vm::bloquear)
            }
        },
        bottomBar = {
            if (gestion) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (esNueva) {
                            OutlinedButton(
                                onClick = { guardar(otraMas = true) },
                                enabled = !procesando,
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                            ) { Text("Guardar y agregar otra talla") }
                        }
                        Button(
                            onClick = { guardar(otraMas = false) },
                            enabled = !procesando,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                        ) { Text(if (esNueva) "Guardar" else "Guardar cambios") }
                    }
                }
            }
        },
    ) { pad ->
        if (!gestion) {
            // Si se bloquea a mitad de la edición, los costos se ocultan pero lo escrito se conserva
            Box(Modifier.padding(pad).fillMaxSize()) {
                EstadoVacio(
                    "Edición protegida",
                    "Los costos solo se muestran en Modo Gestión. Lo que escribiste sigue guardado aquí.",
                    "Desbloquear",
                ) { vm.pedirGestion() }
            }
            return@Scaffold
        }
        if (!cargado) return@Scaffold

        Column(
            Modifier
                .padding(pad)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Foto
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    FotoPrenda(foto, nombre.ifBlank { "?" }, tamano = 96.dp)
                    if (procesando) CircularProgressIndicator(Modifier.size(32.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(onClick = { abrirCamara() }) {
                        Icon(Icons.Outlined.PhotoCamera, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (foto == null) "Tomar foto" else "Cambiar foto")
                    }
                    TextButton(onClick = { vm.permitirSalida(); galeria.launch("image/*") }) {
                        Icon(Icons.Outlined.PhotoLibrary, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Desde galería")
                    }
                }
            }

            // Nombre con sugerencias de nombres ya usados
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it.take(60) },
                label = { Text("Nombre") },
                singleLine = true,
                isError = errores && nombre.isBlank(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            val sugerencias = remember(nombre, nombresUsados) {
                val q = Texto.normalizar(nombre)
                if (q.length < 2) emptyList()
                else nombresUsados.filter { Texto.normalizar(it).contains(q) && it != nombre }.take(4)
            }
            if (sugerencias.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sugerencias.forEach { s -> AssistChip(onClick = { nombre = s }, label = { Text(s) }) }
                }
            }

            // Talla
            Text(
                "Talla",
                style = MaterialTheme.typography.labelLarge,
                color = if (errores && talla.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Tallas.sugeridas.forEach { t ->
                    FilterChip(
                        selected = !otraTalla && talla == t,
                        onClick = { otraTalla = false; talla = t },
                        label = { Text(t) },
                    )
                }
                FilterChip(
                    selected = otraTalla,
                    onClick = { otraTalla = true; if (talla in Tallas.sugeridas) talla = "" },
                    label = { Text("+ Otra") },
                )
            }
            if (otraTalla) {
                OutlinedTextField(
                    value = talla,
                    onValueChange = { talla = it.take(12) },
                    label = { Text("Talla") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // Precios
            CampoUsd(costo, { costo = it }, "Costo")
            CampoUsd(
                precioUsd, { precioUsd = it }, "Precio de venta",
                apoyo = if (errores && precioCents == null) "Obligatorio" else null,
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Paleta.MentaClara.copy(alpha = 0.5f), MaterialTheme.shapes.medium)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("Precio final en pesos", style = MaterialTheme.typography.titleSmall)
                Text(
                    if (calculado != null) "Calculado: ${Dinero.cop(calculado)} (tasa ${Dinero.tasaTexto(ajustes.tasa)})"
                    else "Escribe el precio en dólares para calcularlo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (redondeado != null) {
                    Text("Redondeado: ${Dinero.cop(redondeado)}", style = MaterialTheme.typography.titleMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text("Fijar precio a mano", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(
                        checked = fijado,
                        onCheckedChange = {
                            fijado = it
                            if (it && copFijo.isBlank()) copFijo = redondeado?.toString() ?: ""
                        },
                    )
                }
                if (fijado) {
                    CampoCop(
                        copFijo, { copFijo = it }, "Precio fijo",
                        apoyo = "No cambia aunque actualices la tasa",
                    )
                }
                if (ganancia != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Ganancia ${Dinero.cop(ganancia)}", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        EtiquetaMargen(margen)
                    }
                    if (ganancia < 0) {
                        Text(
                            "Venderías con pérdida.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Paleta.Ambar,
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            title = { Text("¿Eliminar esta prenda?") },
            text = { Text("Se borra del inventario. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarBorrado = false
                    original?.let { vm.eliminarPrenda(it) }
                    onEliminada()
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmarBorrado = false }) { Text("Cancelar") } },
        )
    }
}
