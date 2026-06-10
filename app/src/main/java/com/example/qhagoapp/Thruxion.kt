package com.example.qhagoapp

import android.app.Application
import com.example.qhagoapp.data.model.LoggedInUser
import com.example.qhagoapp.network.security.TokenManager
import com.example.qhagoapp.utils.LocaleManager
import com.example.qhagoapp.utils.ThemeManager
import com.example.qhagoapp.utils.UserSession

class Thruxion : Application()
{
    override fun onCreate()
    {
        super.onCreate()
        TokenManager.init(this)
        ThemeManager.init(this)
        LocaleManager.init(this)

        // Initialize UserSession from TokenManager if logged in
        if (TokenManager.hasValidSession()) {
            val userId = TokenManager.getUserId()
            val email = TokenManager.getUserEmail() ?: "Unknown"
            if (userId != -1) {
                UserSession.user = LoggedInUser(userId.toString(), email)
            } else if (email == "demo@qhago.com") {
                // Handle demo session restoration
                UserSession.user = LoggedInUser("demo_user", "Demo User")
            }
        }
    }

}