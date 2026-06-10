package com.example.qhagoapp.utils

import com.example.qhagoapp.data.model.LoggedInUser

object UserSession {
    var user: LoggedInUser? = null
    
    val userId: String?
        get() = user?.userId

    fun isLoggedIn(): Boolean = user != null
}
