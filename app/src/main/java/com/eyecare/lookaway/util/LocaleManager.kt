package com.eyecare.lookaway.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * App language handling without AppCompat. The choice is stored synchronously in
 * SharedPreferences so it can be read in `attachBaseContext` (before anything
 * else runs), and [wrap] returns a context whose resources use that language —
 * including the correct layout direction (RTL for Arabic).
 */
object LocaleManager {

    const val SYSTEM = "system"

    private const val PREFS = "locale"
    private const val KEY_LANG = "lang"

    /** Supported language tags the UI offers (besides [SYSTEM]). */
    val supported = listOf("en", "ar")

    fun getLanguage(context: Context): String =
        prefs(context).getString(KEY_LANG, SYSTEM) ?: SYSTEM

    fun setLanguage(context: Context, lang: String) {
        prefs(context).edit().putString(KEY_LANG, lang).apply()
    }

    /** Returns a context localized to the stored choice (or [context] for system). */
    fun wrap(context: Context): Context {
        val lang = getLanguage(context)
        if (lang == SYSTEM) return context
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
