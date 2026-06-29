package com.eyecare.lookaway.ui.theme

import android.view.View
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.os.ConfigurationCompat
import androidx.core.text.TextUtilsCompat
import com.eyecare.lookaway.data.ThemeMode
import java.util.Locale

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

    // Drive layout direction from the active locale so RTL flips immediately when
    // the language changes (Compose otherwise keeps the window's original direction).
    val configuration = LocalConfiguration.current
    val locale = ConfigurationCompat.getLocales(configuration).get(0) ?: Locale.getDefault()
    val layoutDirection =
        if (TextUtilsCompat.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL) {
            LayoutDirection.Rtl
        } else {
            LayoutDirection.Ltr
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides layoutDirection, content = content)
    }
}
