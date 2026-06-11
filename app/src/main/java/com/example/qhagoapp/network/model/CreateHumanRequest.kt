package com.example.qhagoapp.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CreateHumanRequest(
    @Json(name = "unique_id") val uniqueId: String,
    @Json(name = "legal_id") val legalId: String,
    @Json(name = "name") val name: String,
    @Json(name = "lastname") val lastname: String,
    @Json(name = "birthdate") val birthdate: String,
    @Json(name = "gender") val gender: String
)
