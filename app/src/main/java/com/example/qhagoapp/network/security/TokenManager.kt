package com.example.qhagoapp.network.security

object TokenManager
{
    private var communicationsToken: String? = null
    private var humansToken: String? = null

    fun saveCommunicationsToken(token: String) {
        communicationsToken = token
    }

    fun saveHumansToken(token: String) {
        humansToken = token
    }

    fun getCommunicationsToken(): String? {
        return communicationsToken
    }

    fun getHumansToken(): String? {
        return humansToken
    }

    fun clearTokens() {
        communicationsToken = null
        humansToken = null
    }

}