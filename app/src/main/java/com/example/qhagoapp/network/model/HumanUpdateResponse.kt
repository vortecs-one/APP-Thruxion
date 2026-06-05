package com.example.qhagoapp.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HumanUpdateResponse(
    @Json(name = "success") val success: Boolean?,
    @Json(name = "message") val message: String?,
    @Json(name = "human") val human: HumanResponse?
)
