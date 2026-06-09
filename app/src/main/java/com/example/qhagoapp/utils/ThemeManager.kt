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

    // List of listeners to notify when theme changes
    private val listeners = mutableListOf<(Int) -> Unit>()

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
        
        // Notify all listeners before applying theme
        listeners.forEach { it.invoke(currentTheme) }
        
        AppCompatDelegate.setDefaultNightMode(currentTheme)
    }

    fun addOnThemeChangeListener(listener: (Int) -> Unit) {
        listeners.add(listener)
    }

    fun removeOnThemeChangeListener(listener: (Int) -> Unit) {
        listeners.remove(listener)
    }
}
