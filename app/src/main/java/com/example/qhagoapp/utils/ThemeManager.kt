package com.example.qhagoapp.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    private const val PREF_NAME = "theme_prefs"
    private const val KEY_IS_DARK_MODE = "is_dark_mode"
    private var sharedPreferences: SharedPreferences? = null

    // Global variable to track the current theme state
    var currentTheme: Int = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        private set

    // Simple listener to notify when theme changes
    private var onThemeChangeListener: ((Int) -> Unit)? = null

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        updateCurrentThemeVariable()
        // Apply saved theme on startup
        AppCompatDelegate.setDefaultNightMode(currentTheme)
    }

    private fun updateCurrentThemeVariable() {
        val isDark = sharedPreferences?.getBoolean(KEY_IS_DARK_MODE, false) ?: false
        currentTheme = if (isDark) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
    }

    fun isDarkMode(): Boolean {
        return currentTheme == AppCompatDelegate.MODE_NIGHT_YES
    }

    fun setDarkMode(isDarkMode: Boolean) {
        val newMode = if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        if (currentTheme == newMode) return

        sharedPreferences?.edit()?.putBoolean(KEY_IS_DARK_MODE, isDarkMode)?.commit()
        currentTheme = newMode
        
        // Notify listener before applying theme (which might recreate activity)
        onThemeChangeListener?.invoke(currentTheme)
        
        AppCompatDelegate.setDefaultNightMode(currentTheme)
    }

    fun setOnThemeChangeListener(listener: (Int) -> Unit) {
        onThemeChangeListener = listener
    }
}
