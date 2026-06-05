package com.example.qhagoapp.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREF_NAME = "theme_prefs"
    private const val KEY_IS_DARK_MODE = "is_dark_mode"
    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        applyTheme()
    }

    fun isDarkMode(): Boolean {
        return sharedPreferences?.getBoolean(KEY_IS_DARK_MODE, false) ?: false
    }

    fun setDarkMode(isDarkMode: Boolean) {
        sharedPreferences?.edit()?.putBoolean(KEY_IS_DARK_MODE, isDarkMode)?.commit()
        applyTheme()
    }

    private fun applyTheme() {
        if (isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
