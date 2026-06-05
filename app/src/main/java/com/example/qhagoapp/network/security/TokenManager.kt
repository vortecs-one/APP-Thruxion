package com.example.qhagoapp.network.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object TokenManager {
    private const val PREF_NAME = "secure_prefs"
    private const val KEY_COMM_TOKEN = "comm_token"
    private const val KEY_HUMANS_TOKEN = "humans_token"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_HUMAN_ID = "human_id"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"

    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveCommunicationsToken(token: String) {
        sharedPreferences?.edit()?.putString(KEY_COMM_TOKEN, token)?.apply()
    }

    fun saveHumansToken(token: String) {
        sharedPreferences?.edit()?.putString(KEY_HUMANS_TOKEN, token)?.apply()
    }

    fun saveUserEmail(email: String) {
        sharedPreferences?.edit()?.putString(KEY_USER_EMAIL, email)?.apply()
    }

    fun saveHumanId(id: Int) {
        sharedPreferences?.edit()?.putInt(KEY_HUMAN_ID, id)?.apply()
    }

    fun setLoggedIn(loggedIn: Boolean) {
        sharedPreferences?.edit()?.putBoolean(KEY_IS_LOGGED_IN, loggedIn)?.apply()
    }

    fun getCommunicationsToken(): String? {
        return sharedPreferences?.getString(KEY_COMM_TOKEN, null)
    }

    fun getHumansToken(): String? {
        return sharedPreferences?.getString(KEY_HUMANS_TOKEN, null)
    }

    fun getUserEmail(): String? {
        return sharedPreferences?.getString(KEY_USER_EMAIL, null)
    }

    fun getHumanId(): Int {
        return sharedPreferences?.getInt(KEY_HUMAN_ID, -1) ?: -1
    }

    fun clearTokens() {
        sharedPreferences?.edit()?.clear()?.apply()
    }

    fun hasValidSession(): Boolean {
        return sharedPreferences?.getBoolean(KEY_IS_LOGGED_IN, false) == true
    }
}