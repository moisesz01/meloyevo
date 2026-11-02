package com.meloyevo.app

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.meloyevo.app.data.Ajustes
import com.meloyevo.app.data.ESTADO_DISPONIBLE
import com.meloyevo.app.data.ESTADO_VENDIDA
import com.meloyevo.app.data.LineaVenta
import com.meloyevo.app.data.MONEDA_COP
import com.meloyevo.app.data.MONEDA_USD
import com.meloyevo.app.data.Movimiento
import com.meloyevo.app.data.Prenda
import com.meloyevo.app.data.Respaldo
import com.meloyevo.app.data.Venta
import com.meloyevo.app.data.VentaItem
import com.meloyevo.app.util.Precios
import com.meloyevo.app.util.Tallas
import com.meloyevo.app.util.Texto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class FiltroEstado(val etiqueta: String) {
    DISPONIBLES("Disponibles"), VENDIDAS("Vendidas"), TODAS("Todas")
}

/** Línea del carrito: la rebaja (si la hay) se guarda por moneda. */
data class LineaCarrito(
    val prendaId: Long,
    val rebajaCop: Long? = null,
    val rebajaUsdCents: Long? = null,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val contenedor = app as MeloyevoApp
    val repo = contenedor.repositorio
    private val ajustesStore = contenedor.ajustes
    val ajustes: StateFlow<Ajustes> = ajustesStore.estado

    // ================= Inventario =================
    val prendas: StateFlow<List<Prenda>> =
        repo.prendas.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val nombresUsados: StateFlow<List<String>> =
        repo.nombresUsados.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val consulta = MutableStateFlow("")
    val filtroTalla = MutableStateFlow<String?>(null)
    val filtroEstado = MutableStateFlow(FiltroEstado.DISPONIBLES)

    private fun cumpleEstado(p: Prenda, f: FiltroEstado) = when (f) {
        FiltroEstado.DISPONIBLES -> p.estado == ESTADO_DISPONIBLE
        FiltroEstado.VENDIDAS -> p.estado == ESTADO_VENDIDA
        FiltroEstado.TODAS -> true
    }

    val prendasFiltradas: StateFlow<List<Prenda>> =
        combine(prendas, consulta, filtroTalla, filtroEstado) { lista, q, talla, estado ->
            lista.asSequence()
                .filter { cumpleEstado(it, estado) }
                .filter { talla == null || it.talla == talla }
                .filter { Texto.coincide(it, q) }
                .let { s -> if (estado == FiltroEstado.VENDIDAS) s.sortedByDescending { it.vendidoEn ?: 0L } else s }
                .toList()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val tallasPresentes: StateFlow<List<String>> =
        combine(prendas, filtroEstado) { lista, estado ->
            Tallas.ordenar(lista.filter { cumpleEstado(it, estado) }.map { it.talla })
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val disponibles: StateFlow<Int> =
        prendas.map { l -> l.count { it.estado == ESTADO_DISPONIBLE } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun limpiarFiltros() {
        consulta.value = ""
        filtroTalla.value = null
    }

    suspend fun importarFoto(uri: Uri): String? = repo.imagenes.importar(uri)

    suspend fun guardarPrenda(p: Prenda): Long {
        val id = repo.guardarPrenda(p)
        repo.limpiarFotosHuerfanas()
        return id
    }

    suspend fun puedeEliminar(id: Long) = repo.puedeEliminar(id)

    fun eliminarPrenda(p: Prenda) = viewModelScope.launch { repo.eliminarPrenda(p) }

    // ================= Modo Gestión =================
    private val _gestion = MutableStateFlow(false)
    val gestion: StateFlow<Boolean> = _gestion.asStateFlow()
    private val _segundos = MutableStateFlow(0)
    val segundosRestantes: StateFlow<Int> = _segundos.asStateFlow()
    private var bloqueaEn = 0L
    private var reloj: Job? = null

    /** Acción que espera a que se ingrese el PIN. */
    private val _pinPendiente = MutableStateFlow<(() -> Unit)?>(null)
    val pinPendiente: StateFlow<(() -> Unit)?> = _pinPendiente.asStateFlow()

    /**
     * Abrir la cámara, la galería o el selector de archivos saca a la app de primer plano.
     * Esa salida, pedida por la propia app, no debe bloquear el Modo Gestión.
     */
    private var salidaPermitida = false
    fun permitirSalida() { salidaPermitida = true }
    fun cancelarSalida() { salidaPermitida = false }
    fun alSalirDeLaApp() {
        if (salidaPermitida) salidaPermitida = false else bloquear()
    }

    fun actividad() {
        if (_gestion.value) bloqueaEn = System.currentTimeMillis() + ajustes.value.autoBloqueoSeg * 1000L
    }

    fun bloquear() {
        _gestion.value = false
        reloj?.cancel()
        reloj = null
    }

    private fun desbloquear() {
        _gestion.value = true
        actividad()
        reloj?.cancel()
        reloj = viewModelScope.launch {
            while (true) {
                val resta = bloqueaEn - System.currentTimeMillis()
                if (resta <= 0) {
                    bloquear()
                    break
                }
                _segundos.value = ((resta + 999) / 1000).toInt()
                delay(1000)
            }
        }
    }

    /** Ejecuta la acción en Modo Gestión; pide PIN primero si hace falta. */
    fun pedirGestion(accion: () -> Unit = {}) {
        when {
            _gestion.value -> { actividad(); accion() }
            !ajustes.value.tienePin -> { desbloquear(); accion() }
            else -> _pinPendiente.value = accion
        }
    }

    fun alternarModo() {
        if (_gestion.value) bloquear() else pedirGestion()
    }

    fun intentarPin(pin: String): Boolean {
        if (!ajustesStore.verificarPin(pin)) return false
        val accion = _pinPendiente.value
        _pinPendiente.value = null
        desbloquear()
        accion?.invoke()
        return true
    }

    fun cancelarPin() {
        _pinPendiente.value = null
    }

    // ================= Venta =================
    private val _ventaAbierta = MutableStateFlow(false)
    val ventaAbierta: StateFlow<Boolean> = _ventaAbierta.asStateFlow()
    private val _carrito = MutableStateFlow<List<LineaCarrito>>(emptyList())
    val carrito: StateFlow<List<LineaCarrito>> = _carrito.asStateFlow()
    val monedaVenta = MutableStateFlow(MONEDA_COP)

    fun abrirVenta(prendaId: Long? = null) {
        _carrito.value = if (prendaId != null) listOf(LineaCarrito(prendaId)) else emptyList()
        monedaVenta.value = MONEDA_COP
        _ventaAbierta.value = true
    }

    fun cerrarVenta() {
        _ventaAbierta.value = false
        _carrito.value = emptyList()
    }

    fun agregarAlCarrito(id: Long) {
        if (_carrito.value.none { it.prendaId == id }) _carrito.value = _carrito.value + LineaCarrito(id)
    }

    fun quitarDelCarrito(id: Long) {
        _carrito.value = _carrito.value.filterNot { it.prendaId == id }
    }

    /** valor = null quita la rebaja. Se aplica en la moneda actual de la venta. */
    fun rebajar(id: Long, valor: Long?) {
        val usd = monedaVenta.value == MONEDA_USD
        _carrito.value = _carrito.value.map {
            if (it.prendaId != id) it
            else if (usd) it.copy(rebajaUsdCents = valor) else it.copy(rebajaCop = valor)
        }
    }

    fun precioLista(p: Prenda, moneda: String): Long =
        if (moneda == MONEDA_USD) p.precioUsdCents else Precios.precioFinalCop(p, ajustes.value)

    fun precioCobrado(linea: LineaCarrito, p: Prenda, moneda: String): Long =
        (if (moneda == MONEDA_USD) linea.rebajaUsdCents else linea.rebajaCop) ?: precioLista(p, moneda)

    /** Devuelve el id de la venta registrada, o null si no había nada que vender. */
    suspend fun confirmarVenta(): Long? {
        val moneda = monedaVenta.value
        val porId = prendas.value.associateBy { it.id }
        val lineas = _carrito.value.mapNotNull { l ->
            val p = porId[l.prendaId]?.takeIf { it.estado == ESTADO_DISPONIBLE } ?: return@mapNotNull null
            LineaVenta(p, precioCobrado(l, p, moneda), precioLista(p, moneda))
        }
        if (lineas.isEmpty()) return null
        val id = repo.registrarVenta(lineas, moneda, ajustes.value.tasa)
        cerrarVenta()
        return id
    }

    fun deshacerVenta(id: Long) = viewModelScope.launch { repo.deshacerVenta(id) }
    fun revertirVenta(id: Long) = viewModelScope.launch { repo.revertirVenta(id) }

    // ================= Caja y resumen =================
    val saldoCop: StateFlow<Long> = repo.saldoCop.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val saldoUsd: StateFlow<Long> = repo.saldoUsd.stateIn(viewModelScope, SharingStarted.Eagerly, 0L)
    val movimientos: StateFlow<List<Movimiento>> =
        repo.movimientos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val ventas: StateFlow<List<Venta>> =
        repo.ventas.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val itemsVendidos: StateFlow<List<VentaItem>> =
        repo.itemsVendidos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun registrarMovimiento(tipo: String, moneda: String, monto: Long, nota: String) =
        viewModelScope.launch { repo.registrarMovimiento(tipo, moneda, monto, nota) }

    fun registrarCambio(monedaEntrega: String, entrega: Long, recibe: Long, nota: String) =
        viewModelScope.launch { repo.registrarCambio(monedaEntrega, entrega, recibe, nota) }

    fun cuadrar(contadoCop: Long, contadoUsd: Long) =
        viewModelScope.launch { repo.cuadrar(contadoCop, contadoUsd) }

    // ================= Ajustes =================
    fun setTasa(tasa: Double) = ajustesStore.setTasa(tasa)
    fun setRedondeo(paso: Int) = ajustesStore.setRedondeo(paso)
    fun setAutoBloqueo(seg: Int) = ajustesStore.setAutoBloqueo(seg)
    fun setPin(pin: String?) = ajustesStore.setPin(pin)
    fun verificarPin(pin: String) = ajustesStore.verificarPin(pin)

    suspend fun exportarRespaldo(uri: Uri) = Respaldo.exportar(getApplication(), uri, ajustesStore)
    suspend fun importarRespaldo(uri: Uri) = Respaldo.importar(getApplication(), uri, ajustesStore)
}
