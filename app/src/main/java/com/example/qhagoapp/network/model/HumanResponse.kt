package com.example.qhagoapp.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HumanResponse(
    @Json(name = "id") val id: Int?,
    @Json(name = "unique_id") val unique_id: String?,
    @Json(name = "legal_id") val legal_id: String?,
    @Json(name = "name") val name: String?,
    @Json(name = "lastname") val lastname: String?,
    @Json(name = "birthdate") val birthdate: String?,
    @Json(name = "gender") val gender: String?,
    @Json(name = "created_at") val created_at: String?,
    @Json(name = "updated_at") val updated_at: String?,
    @Json(name = "users") val users: List<UserSummary>?,
    @Json(name = "skills") val skills: List<Any>? = null,
    @Json(name = "certificates") val certificates: List<Any>? = null,
    @Json(name = "facial_recognitions") val facial_recognitions: List<Any>? = null,
    @Json(name = "cards") val cards: List<Any>? = null,
    @Json(name = "space_time") val space_time: List<Any>? = null
)

@JsonClass(generateAdapter = true)
data class UserSummary(
    @Json(name = "id") val id: Int?,
    @Json(name = "email") val email: String?,
    @Json(name = "created_at") val created_at: String?
)
