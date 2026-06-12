package com.example.qhagoapp.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(
    @field:Json(name = "current_password")
    val currentPassword: String,
    @field:Json(name = "new_password")
    val newPassword: String
)
