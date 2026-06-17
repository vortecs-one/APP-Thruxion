package com.thruxion.app.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserRegisterResponse(
    @field:Json(name = "message") val message: String?,
    @field:Json(name = "user_id") val user_id: Int?,
    @field:Json(name = "platform") val platform: String?
)
