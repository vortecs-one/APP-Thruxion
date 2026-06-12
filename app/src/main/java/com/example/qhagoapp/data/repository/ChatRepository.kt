package com.example.qhagoapp.data.repository

import com.example.qhagoapp.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(currentUserId: String, partnerId: String): Flow<List<ChatMessage>>
    fun getActiveChats(currentUserId: String): Flow<List<ChatMessage>>
    suspend fun sendMessage(content: String, currentUserId: String, partnerId: String, partnerName: String)
    suspend fun clearChat(currentUserId: String, partnerId: String)
}
