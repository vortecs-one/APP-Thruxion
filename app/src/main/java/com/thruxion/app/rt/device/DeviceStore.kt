package com.thruxion.app.rt.device

import android.content.Context
import android.content.SharedPreferences

class DeviceStore(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getDeviceId(sub: String): String? {
        return prefs.getString("${KEY_DEVICE_ID}_$sub", null)
    }

    fun saveDeviceId(sub: String, deviceId: String) {
        prefs.edit()
            .putString("${KEY_DEVICE_ID}_$sub", deviceId)
            .remove("${KEY_REVOKED}_$sub")
            .apply()
    }

    fun removeDeviceId(sub: String) {
        prefs.edit()
            .remove("${KEY_DEVICE_ID}_$sub")
            .apply()
    }

    fun isRevoked(sub: String): Boolean {
        return prefs.getBoolean("${KEY_REVOKED}_$sub", false)
    }

    fun setRevoked(sub: String, revoked: Boolean) {
        val editor = prefs.edit().putBoolean("${KEY_REVOKED}_$sub", revoked)
        if (revoked) {
            editor.remove("${KEY_DEVICE_ID}_$sub")
        }
        editor.apply()
    }

    companion object {
        private const val PREF_NAME = "rt_device_store"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_REVOKED = "device_revoked"
    }
}
