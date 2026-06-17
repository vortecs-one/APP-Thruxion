package com.thruxion.app

import android.app.Application
import com.thruxion.app.data.model.LoggedInUser
import com.thruxion.app.network.security.TokenManager
import com.thruxion.app.utils.LocaleManager
import com.thruxion.app.utils.ThemeManager
import com.thruxion.app.utils.UserSession

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
            val platform = TokenManager.getPlatform()
            if (userId != -1) {
                UserSession.user = LoggedInUser(userId.toString(), email, platform)
            } else if (email == "demo@qhago.com") {
                // Handle demo session restoration
                UserSession.user = LoggedInUser("demo_user", "Demo User", "demo")
            }
        }
    }

}