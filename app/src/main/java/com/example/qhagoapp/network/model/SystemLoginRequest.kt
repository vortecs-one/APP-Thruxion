package com.example.qhagoapp.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SystemLoginRequest(
    @Json(name = "username") val username: String,
    @Json(name = "password") val password: String
)
