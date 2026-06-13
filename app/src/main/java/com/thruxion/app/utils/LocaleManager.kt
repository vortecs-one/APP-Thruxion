package com.thruxion.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleManager {
    private const val PREF_NAME = "locale_prefs"
    private const val KEY_LANGUAGE = "language"
    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        applyLocale()
    }

    fun getLanguage(): String {
        return sharedPreferences?.getString(KEY_LANGUAGE, "en") ?: "en"
    }

    fun setLanguage(language: String) {
        sharedPreferences?.edit()?.putString(KEY_LANGUAGE, language)?.apply()
        applyLocale()
    }

    private fun applyLocale() {
        val language = getLanguage()
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}
