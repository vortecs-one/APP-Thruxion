package com.example.qhagoapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val senderId: String, 
    val receiverId: String, // Explicitly non-null (use "assistant" for AI)
    val ownerId: String,   // The ID of the local user who owns this chat history
    val timestamp: Long = System.currentTimeMillis(),
    val isFromUser: Boolean
)
