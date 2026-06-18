package com.thruxion.app.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HandoffResponse(
    @field:Json(name = "success") val success: Boolean,
    @field:Json(name = "token") val token: String?,
    @field:Json(name = "expiresAt") val expiresAt: String?,
    @field:Json(name = "handoffUrl") val handoffUrl: String?
)
