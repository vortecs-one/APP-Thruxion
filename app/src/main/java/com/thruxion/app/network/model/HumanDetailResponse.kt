package com.thruxion.app.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HumanDetailResponse(
    @field:Json(name = "success") val success: Boolean?,
    @field:Json(name = "message") val message: String? = null,
    @field:Json(name = "data") val data: HumanResponse?
)
