package com.example.qhagoapp.data.repository

import com.example.qhagoapp.data.dao.ChatMessageDao
import com.example.qhagoapp.data.model.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

class ChatRepositoryImpl(
    private val chatMessageDao: ChatMessageDao
) : ChatRepository {

    override fun getMessages(): Flow<List<ChatMessage>> = chatMessageDao.getAllMessages()

    override suspend fun sendMessage(content: String) {
        // Save user message
        val userMessage = ChatMessage(content = content, senderId = "user", isFromUser = true)
        chatMessageDao.insertMessage(userMessage)

        // Simulate AI thinking
        delay(1000)

        // Fake AI response
        val aiResponse = ChatMessage(
            content = "I'm your AI assistant for QhagoApp. How can I help you today? (Phase 1 Fake Provider)",
            senderId = "ai",
            isFromUser = false
        )
        chatMessageDao.insertMessage(aiResponse)
    }

    override suspend fun clearChat() {
        chatMessageDao.clearChat()
    }
}
