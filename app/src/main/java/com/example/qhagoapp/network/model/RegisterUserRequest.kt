package com.example.qhagoapp.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterUserRequest(
    @field:Json(name = "human_id") val humanId: String,
    @field:Json(name = "email") val email: String,
    @field:Json(name = "password") val password: String
)
