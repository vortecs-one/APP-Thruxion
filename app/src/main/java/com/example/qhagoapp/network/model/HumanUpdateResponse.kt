package com.example.qhagoapp.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HumanUpdateResponse(
    @field:Json(name = "success") val success: Boolean?,
    @field:Json(name = "message") val message: String?,
    @field:Json(name = "human") val human: HumanResponse?
)
