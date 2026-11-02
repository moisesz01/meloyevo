package com.meloyevo.app

import android.app.Application
import com.meloyevo.app.data.AjustesStore
import com.meloyevo.app.data.Repositorio

class MeloyevoApp : Application() {
    val repositorio: Repositorio by lazy { Repositorio(this) }
    val ajustes: AjustesStore by lazy { AjustesStore(this) }
}
