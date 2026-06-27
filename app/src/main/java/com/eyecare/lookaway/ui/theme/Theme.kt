package com.eyecare.lookaway.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import com.eyecare.lookaway.data.ThemeMode

@Composable
fun LookAwayTheme(
    themeMode: ThemeMode,
    accentIndex: Int,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accent = accentAt(accentIndex)
    val colorScheme = if (dark) darkSchemeFor(accent) else lightSchemeFor(accent)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content,
    )
}
