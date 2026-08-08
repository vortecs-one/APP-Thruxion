package com.thruxion.app.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * Handles OAuth2 authentication with Huawei Cloud.
 */
object HuaweiAuthManager {
    
    private const val CLIENT_ID = "118565965"
    private const val CLIENT_SECRET = "ef1f21366f067670560a3691d38b129ba779c57335442e04c97bcc866b6477fc"
    private const val REDIRECT_URI = "https://thruxion.com/huawei-auth"
    
    private const val AUTH_URL = "https://oauth-login.cloud.huawei.com/oauth2/v3/authorize"
    private const val TOKEN_URL = "https://oauth-login.cloud.huawei.com/oauth2/v3/token"
    private const val SCOPES = "https://www.huawei.com/healthkit/step.read " +
            "https://www.huawei.com/healthkit/heartrate.read " +
            "https://www.huawei.com/healthkit/distance.read " +
            "https://www.huawei.com/healthkit/calories.read " +
            "https://www.huawei.com/healthkit/activity.read"

    private fun getEncryptedPrefs(context: Context): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "huawei_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun startLogin(context: Context) {
        val uri = Uri.parse(AUTH_URL).buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("access_type", "offline")
            .build()

        val customTabsIntent = CustomTabsIntent.Builder().build()
        customTabsIntent.launchUrl(context, uri)
    }

    suspend fun handleAuthRedirect(context: Context, uri: Uri): Boolean {
        val uriStr = uri.toString()
        if (uriStr.startsWith(REDIRECT_URI) || uriStr.startsWith("com.thruxion.app://huawei-auth")) {
            val code = uri.getQueryParameter("code")
            if (code != null) {
                return exchangeCodeForToken(context, code)
            }
        }
        return false
    }

    private suspend fun exchangeCodeForToken(context: Context, code: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val requestBody = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .add("redirect_uri", REDIRECT_URI)
                .build()

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val json = JSONObject(responseBody)
                saveTokens(context, json)
                return@withContext true
            }
        } catch (e: Exception) {
            Log.e("HuaweiAuth", "Error exchanging code", e)
        }
        false
    }

    suspend fun getValidAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        val prefs = getEncryptedPrefs(context)
        val accessToken = prefs.getString("access_token", null)
        val expiresAt = prefs.getLong("expires_at", 0)

        if (accessToken != null && System.currentTimeMillis() < expiresAt) {
            return@withContext accessToken
        }

        // Try refreshing
        val refreshToken = prefs.getString("refresh_token", null) ?: return@withContext null
        return@withContext performTokenRefresh(context, refreshToken)
    }

    private fun performTokenRefresh(context: Context, refreshToken: String): String? {
        try {
            val client = OkHttpClient()
            val requestBody = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", CLIENT_ID)
                .add("client_secret", CLIENT_SECRET)
                .build()

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val json = JSONObject(responseBody)
                saveTokens(context, json)
                return json.optString("access_token")
            }
        } catch (e: Exception) {
            Log.e("HuaweiAuth", "Error refreshing token", e)
        }
        return null
    }

    private fun saveTokens(context: Context, json: JSONObject) {
        val accessToken = json.getString("access_token")
        val refreshToken = json.optString("refresh_token")
        val expiresIn = json.getLong("expires_in")
        val expiresAt = System.currentTimeMillis() + (expiresIn * 1000) - 60000

        getEncryptedPrefs(context).edit().apply {
            putString("access_token", accessToken)
            if (refreshToken.isNotEmpty()) putString("refresh_token", refreshToken)
            putLong("expires_at", expiresAt)
            apply()
        }
    }

    fun disconnect(context: Context) {
        getEncryptedPrefs(context).edit().clear().apply()
    }
}
