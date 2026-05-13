package com.example.qhagoapp.network.model

data class UserLoginResponse(
    val success: Boolean,
    val message: String,
    val user: UserData
)

data class UserData(
    val id: Int,
    val human_id: Int,
    val email: String,
    val role: String
)