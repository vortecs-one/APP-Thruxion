package com.example.qhagoapp.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RegisterUserRequest(
    @Json(name = "human_id") val humanId: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)
