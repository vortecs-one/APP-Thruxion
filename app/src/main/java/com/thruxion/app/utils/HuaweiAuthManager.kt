package com.thruxion.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Handles OAuth2 authentication with Huawei Cloud.
 */
object HuaweiAuthManager {
    
    // REDACTED: These should be provided by the user in the Developer Console
    private const val CLIENT_ID = "YOUR_HUAWEI_APP_ID"
    private const val REDIRECT_URI = "com.thruxion.app://huawei-auth"
    
    private const val AUTH_URL = "https://oauth-login.cloud.huawei.com/oauth2/v3/authorize"
    private const val SCOPES = "https://www.huawei.com/healthkit/step.read https://www.huawei.com/healthkit/heartrate.read"

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

    fun handleAuthRedirect(context: Context, intent: Intent) {
        val data: Uri? = intent.data
        if (data != null && data.toString().startsWith(REDIRECT_URI)) {
            val code = data.getQueryParameter("code")
            if (code != null) {
                // Exchange code for token (Needs server-side or secure Retrofit call)
                saveAuthCode(context, code)
            }
        }
    }

    private fun saveAuthCode(context: Context, code: String) {
        context.getSharedPreferences("huawei_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("auth_code", code)
            .apply()
    }
}
