package com.thruxion.app.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val senderId: String, 
    val receiverId: String, 
    val ownerId: String,   
    val partnerName: String, // Added to persist the display name in the chat list
    val timestamp: Long = System.currentTimeMillis(),
    val isFromUser: Boolean,
    @Ignore
    var isOversecDecrypted: Boolean = false
) {
    // Required for Room because of @Ignore
    constructor(
        id: String, content: String, senderId: String, receiverId: String,
        ownerId: String, partnerName: String, timestamp: Long, isFromUser: Boolean
    ) : this(id, content, senderId, receiverId, ownerId, partnerName, timestamp, isFromUser, false)
}
