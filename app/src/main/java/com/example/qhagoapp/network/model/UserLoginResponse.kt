package com.example.qhagoapp.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserLoginResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String,
    @Json(name = "user") val user: UserData
)

@JsonClass(generateAdapter = true)
data class UserData(
    @Json(name = "id") val id: Int,
    @Json(name = "human_id") val human_id: Int,
    @Json(name = "email") val email: String,
    @Json(name = "role") val role: String
)
