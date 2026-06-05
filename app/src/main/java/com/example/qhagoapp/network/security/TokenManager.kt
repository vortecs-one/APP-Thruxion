package com.example.qhagoapp.network.security

object TokenManager
{
    private var communicationsToken: String? = null
    private var humansToken: String? = null
    private var userEmail: String? = null

    fun saveCommunicationsToken(token: String) {
        communicationsToken = token
    }

    fun saveHumansToken(token: String) {
        humansToken = token
    }

    fun saveUserEmail(email: String) {
        userEmail = email
    }

    fun getCommunicationsToken(): String? {
        return communicationsToken
    }

    fun getHumansToken(): String? {
        return humansToken
    }

    fun getUserEmail(): String? {
        return userEmail
    }

    fun clearTokens() {
        communicationsToken = null
        humansToken = null
        userEmail = null
    }

}