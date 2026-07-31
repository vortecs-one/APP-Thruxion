package com.thruxion.app.data.repository

import com.thruxion.app.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(currentUserId: String, partnerId: String): Flow<List<ChatMessage>>
    fun getActiveChats(currentUserId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(content: String, currentUserId: String, partnerId: String, partnerName: String, encrypt: Boolean = false)
    suspend fun clearChat(currentUserId: String, partnerId: String)
}
