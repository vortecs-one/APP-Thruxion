package com.example.qhagoapp.data.repository

import com.example.qhagoapp.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(): Flow<List<ChatMessage>>
    suspend fun sendMessage(content: String)
    suspend fun clearChat()
}
