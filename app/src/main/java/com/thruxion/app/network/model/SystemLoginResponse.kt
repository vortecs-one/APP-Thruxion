package com.thruxion.app.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SystemLoginResponse(
    @field:Json(name = "token") val token: String
)
