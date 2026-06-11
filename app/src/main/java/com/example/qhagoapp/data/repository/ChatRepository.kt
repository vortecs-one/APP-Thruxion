package com.example.qhagoapp.data.repository

import com.example.qhagoapp.data.model.ChatMessage
import com.example.qhagoapp.data.model.Contact
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(currentUserId: String, partnerId: String): Flow<List<ChatMessage>>
    fun getActiveChats(currentUserId: String): Flow<List<ChatMessage>>
    fun getAllContacts(currentUserId: String): Flow<List<Contact>>
    suspend fun sendMessage(content: String, currentUserId: String, partnerId: String)
    suspend fun clearChat(currentUserId: String, partnerId: String)
    suspend fun deleteChat(currentUserId: String, partnerId: String)
}
