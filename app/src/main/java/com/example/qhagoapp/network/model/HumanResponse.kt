package com.example.qhagoapp.network.model

import com.squareup.moshi.Json

data class HumanResponse(
    val id: Int,
    val unique_id: String,
    val legal_id: String?,
    val name: String?,
    val lastname: String?,
    val birthdate: String?,
    val gender: String?,
    val created_at: String?,
    val updated_at: String?,
    val users: List<UserSummary>?,
    val skills: List<Any>? = null,
    val certificates: List<Any>? = null,
    val facial_recognitions: List<Any>? = null,
    val cards: List<Any>? = null,
    val space_time: List<Any>? = null
)

data class UserSummary(
    val id: Int,
    val email: String,
    val created_at: String
)