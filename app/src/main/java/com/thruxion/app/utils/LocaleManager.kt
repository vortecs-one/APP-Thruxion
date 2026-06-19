package com.thruxion.app.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.thruxion.app.network.security.TokenManager

object LocaleManager {
    private const val PREF_NAME = "locale_prefs"
    private const val KEY_LANGUAGE = "language"
    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        applyLocale()
    }

    private fun getLanguageKey(): String {
        val userEmail = TokenManager.getUserEmail()
        return if (userEmail != null) {
            "${KEY_LANGUAGE}_$userEmail"
        } else {
            KEY_LANGUAGE
        }
    }

    fun getLanguage(): String {
        // Try user-specific language first, fallback to global app language
        val userKey = getLanguageKey()
        return sharedPreferences?.getString(userKey, null) 
            ?: sharedPreferences?.getString(KEY_LANGUAGE, "en") 
            ?: "en"
    }

    fun setLanguage(language: String) {
        val key = getLanguageKey()
        sharedPreferences?.edit()?.putString(key, language)?.apply()
        
        // Also update global language if no user is logged in
        if (TokenManager.getUserEmail() == null) {
            sharedPreferences?.edit()?.putString(KEY_LANGUAGE, language)?.apply()
        }

        applyLocale()
    }

    private fun applyLocale() {
        val language = getLanguage()
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(language)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}
