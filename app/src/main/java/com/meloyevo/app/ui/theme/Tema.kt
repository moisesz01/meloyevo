package com.meloyevo.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Paleta {
    val Menta = Color(0xFF2E7D6B)
    val MentaClara = Color(0xFFD7EFE8)
    val MentaOscura = Color(0xFF1E5A4D)
    val Coral = Color(0xFFF28C70)
    val CoralClaro = Color(0xFFFFDAD0)
    val CoralOscuro = Color(0xFF7A2A16)
    val Pollito = Color(0xFFF6C744)
    val PollitoClaro = Color(0xFFFFF1C7)
    val PollitoOscuro = Color(0xFF6B4E00)
    val Lavanda = Color(0xFF6B5CA5)
    val LavandaClara = Color(0xFFECE8F7)
    val LavandaOscura = Color(0xFF3C3489)
    val Crema = Color(0xFFFFF9F2)
    val Tinta = Color(0xFF1F2328)
    val Gris = Color(0xFF5F5E5A)
    val GrisClaro = Color(0xFFE8E2D8)
    val Vendida = Color(0xFFF4F1EC)
    val Verde = Color(0xFF1B5E36)
    val VerdeClaro = Color(0xFFDDF0E3)
    val Ambar = Color(0xFF7A5200)
    val AmbarClaro = Color(0xFFFCEBC5)
    val Rojo = Color(0xFFA32D2D)
    val RojoClaro = Color(0xFFFCE3E3)
}

private val esquema = lightColorScheme(
    primary = Paleta.Menta,
    onPrimary = Color.White,
    primaryContainer = Paleta.MentaClara,
    onPrimaryContainer = Paleta.MentaOscura,
    secondary = Paleta.Coral,
    onSecondary = Paleta.CoralOscuro,
    secondaryContainer = Paleta.CoralClaro,
    onSecondaryContainer = Paleta.CoralOscuro,
    tertiary = Paleta.Lavanda,
    onTertiary = Color.White,
    tertiaryContainer = Paleta.LavandaClara,
    onTertiaryContainer = Paleta.LavandaOscura,
    background = Paleta.Crema,
    onBackground = Paleta.Tinta,
    surface = Paleta.Crema,
    onSurface = Paleta.Tinta,
    surfaceVariant = Paleta.GrisClaro,
    onSurfaceVariant = Paleta.Gris,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFCF8),
    surfaceContainer = Color(0xFFFAF3EA),
    surfaceContainerHigh = Color(0xFFF5EDE2),
    surfaceContainerHighest = Color(0xFFEFE6DA),
    outline = Color(0xFFB4B2A9),
    outlineVariant = Paleta.GrisClaro,
    error = Paleta.Rojo,
    errorContainer = Paleta.RojoClaro,
)

private val tipografia = Typography().let { t ->
    t.copy(
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Medium),
        titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.Medium),
    )
}

/** Tema claro fijo, sin color dinámico: se ve igual en cualquier versión de Android. */
@Composable
fun MeloyevoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = esquema, typography = tipografia, content = content)
}
