package com.example.qhagoapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val senderId: String, // "user" or "ai" or other user ids
    val timestamp: Long = System.currentTimeMillis(),
    val isFromUser: Boolean
)
