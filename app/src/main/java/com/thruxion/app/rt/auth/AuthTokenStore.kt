package com.thruxion.app.rt.auth

import android.content.Context
import android.content.SharedPreferences
import net.openid.appauth.AuthState

class AuthTokenStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun saveAuthState(authState: AuthState) {
        val json = authState.jsonSerializeString()
        val encrypted = KeyStoreHelper.encrypt(json)
        prefs.edit().putString(KEY_AUTH_STATE, encrypted).apply()
    }

    fun loadAuthState(): AuthState? {
        val encrypted = prefs.getString(KEY_AUTH_STATE, null) ?: return null
        val json = KeyStoreHelper.decrypt(encrypted) ?: return null
        return try {
            AuthState.jsonDeserialize(json)
        } catch (e: Exception) {
            null
        }
    }

    fun clearAuthState() {
        prefs.edit().remove(KEY_AUTH_STATE).apply()
    }

    companion object {
        private const val PREF_NAME = "rt_auth_store"
        private const val KEY_AUTH_STATE = "encrypted_auth_state"
    }
}
