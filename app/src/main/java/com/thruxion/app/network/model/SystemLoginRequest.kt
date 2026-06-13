package com.thruxion.app.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SystemLoginRequest(
    @field:Json(name = "username") val username: String,
    @field:Json(name = "password") val password: String
)
