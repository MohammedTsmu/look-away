package com.eyecare.lookaway.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** A small, friendly palette of accent seeds the user can pick from. */
data class Accent(val name: String, val seed: Color, val onSeed: Color)

val Accents: List<Accent> = listOf(
    Accent("Teal", Color(0xFF0E7C7B), Color.White),
    Accent("Ocean", Color(0xFF1565C0), Color.White),
    Accent("Indigo", Color(0xFF4F46E5), Color.White),
    Accent("Forest", Color(0xFF2E7D32), Color.White),
    Accent("Sunset", Color(0xFFEF6C00), Color.White),
    Accent("Rose", Color(0xFFD81B60), Color.White),
)

fun accentAt(index: Int): Accent = Accents.getOrElse(index) { Accents.first() }

fun lightSchemeFor(accent: Accent): ColorScheme = lightColorScheme(
    primary = accent.seed,
    onPrimary = accent.onSeed,
    primaryContainer = accent.seed.copy(alpha = 0.16f).compositeOverWhite(),
    secondary = accent.seed.darken(0.1f),
    tertiary = accent.seed.darken(0.2f),
)

fun darkSchemeFor(accent: Accent): ColorScheme = darkColorScheme(
    primary = accent.seed.lighten(0.18f),
    onPrimary = Color(0xFF06201F),
    primaryContainer = accent.seed.darken(0.25f),
    secondary = accent.seed.lighten(0.1f),
    tertiary = accent.seed.lighten(0.2f),
)

private fun Color.lighten(amount: Float): Color = Color(
    red = (red + (1f - red) * amount).coerceIn(0f, 1f),
    green = (green + (1f - green) * amount).coerceIn(0f, 1f),
    blue = (blue + (1f - blue) * amount).coerceIn(0f, 1f),
    alpha = alpha,
)

private fun Color.darken(amount: Float): Color = Color(
    red = (red * (1f - amount)).coerceIn(0f, 1f),
    green = (green * (1f - amount)).coerceIn(0f, 1f),
    blue = (blue * (1f - amount)).coerceIn(0f, 1f),
    alpha = alpha,
)

private fun Color.compositeOverWhite(): Color {
    val a = alpha
    return Color(
        red = red * a + (1f - a),
        green = green * a + (1f - a),
        blue = blue * a + (1f - a),
        alpha = 1f,
    )
}
