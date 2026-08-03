package com.thruxion.app.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MessageRequest(
    @field:Json(name = "receiver_id") val receiverId: String,
    @field:Json(name = "content") val content: String, // Encrypted + ZeroWidth
    @field:Json(name = "type") val type: String = "TEXT", // TEXT or IMAGE
    @field:Json(name = "media_url") val mediaUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class MessageResponse(
    @field:Json(name = "id") val id: String,
    @field:Json(name = "sender_id") val senderId: String,
    @field:Json(name = "receiver_id") val receiverId: String,
    @field:Json(name = "content") val content: String,
    @field:Json(name = "type") val type: String,
    @field:Json(name = "media_url") val mediaUrl: String?,
    @field:Json(name = "timestamp") val timestamp: Long
)

@JsonClass(generateAdapter = true)
data class PublicKeyDto(
    @field:Json(name = "user_id") val userId: String,
    @field:Json(name = "public_key") val publicKey: String // Base64
)

@JsonClass(generateAdapter = true)
data class MediaUploadResponse(
    @field:Json(name = "url") val url: String
)
