package com.example.qhagoapp.utils

import com.example.qhagoapp.data.model.LoggedInUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserSession {
    private val _user = MutableStateFlow<LoggedInUser?>(null)
    val userFlow: StateFlow<LoggedInUser?> = _user.asStateFlow()

    var user: LoggedInUser?
        get() = _user.value
        set(value) {
            _user.value = value
        }
    
    val userId: String?
        get() = user?.userId

    fun isLoggedIn(): Boolean = user != null
}
