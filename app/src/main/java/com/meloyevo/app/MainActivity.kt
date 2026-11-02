package com.meloyevo.app

import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.meloyevo.app.ui.MeloyevoRoot
import com.meloyevo.app.ui.theme.MeloyevoTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val crema = Color.parseColor("#FFF9F2")
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(crema, crema),
        )
        super.onCreate(savedInstanceState)
        setContent {
            MeloyevoTheme { MeloyevoRoot(vm) }
        }
    }

    /** Cualquier toque reinicia la cuenta regresiva del bloqueo automático. */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) vm.actividad()
        return super.dispatchTouchEvent(ev)
    }

    override fun onResume() {
        super.onResume()
        vm.cancelarSalida()
    }

    /** Al salir de la app (o apagar la pantalla) se vuelve a Modo Mostrador. */
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) vm.alSalirDeLaApp()
    }
}
