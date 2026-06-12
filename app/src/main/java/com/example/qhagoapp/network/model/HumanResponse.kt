package com.example.qhagoapp.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class HumanResponse(
    @field:Json(name = "id") val id: Int?,
    @field:Json(name = "unique_id") val unique_id: String?,
    @field:Json(name = "legal_id") val legal_id: String?,
    @field:Json(name = "name") val name: String?,
    @field:Json(name = "lastname") val lastname: String?,
    @field:Json(name = "birthdate") val birthdate: String?,
    @field:Json(name = "gender") val gender: String?,
    @field:Json(name = "created_at") val created_at: String?,
    @field:Json(name = "updated_at") val updated_at: String?,
    @field:Json(name = "users") val users: List<UserSummary>?,
    @field:Json(name = "skills") val skills: List<Any>? = null,
    @field:Json(name = "certificates") val certificates: List<Any>? = null,
    @field:Json(name = "facial_recognitions") val facial_recognitions: List<Any>? = null,
    @field:Json(name = "cards") val cards: List<Any>? = null,
    @field:Json(name = "space_time") val space_time: List<Any>? = null
)

@JsonClass(generateAdapter = true)
data class UserSummary(
    @field:Json(name = "id") val id: Int?,
    @field:Json(name = "email") val email: String?,
    @field:Json(name = "created_at") val created_at: String?
)
