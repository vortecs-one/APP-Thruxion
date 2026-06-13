package com.thruxion.app.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserLoginResponse(
    @field:Json(name = "success") val success: Boolean?,
    @field:Json(name = "message") val message: String?,
    @field:Json(name = "user") val user: UserData? = null
)

@JsonClass(generateAdapter = true)
data class UserData(
    @field:Json(name = "id") val id: Int,
    @field:Json(name = "human_id") val human_id: Int,
    @field:Json(name = "email") val email: String,
    @field:Json(name = "role") val role: String
)
