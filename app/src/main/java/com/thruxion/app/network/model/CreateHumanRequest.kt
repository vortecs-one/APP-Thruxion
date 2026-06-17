package com.thruxion.app.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateHumanRequest(
    @field:Json(name = "unique_id") val unique_id: String,
    @field:Json(name = "legal_id") val legal_id: String,
    @field:Json(name = "document_type") val document_type: String? = null,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "lastname") val lastname: String,
    @field:Json(name = "birthdate") val birthdate: String,
    @field:Json(name = "gender") val gender: String
)
