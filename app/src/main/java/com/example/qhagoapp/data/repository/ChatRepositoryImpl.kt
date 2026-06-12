package com.example.qhagoapp.data.repository

import com.example.qhagoapp.data.dao.ChatMessageDao
import com.example.qhagoapp.data.model.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

class ChatRepositoryImpl(
    private val chatMessageDao: ChatMessageDao
) : ChatRepository {

    override fun getMessages(currentUserId: String, partnerId: String): Flow<List<ChatMessage>> = 
        chatMessageDao.getMessagesWith(currentUserId, partnerId)

    override fun getActiveChats(currentUserId: String): Flow<List<ChatMessage>> = 
        chatMessageDao.getActiveChats(currentUserId)

    override suspend fun sendMessage(content: String, currentUserId: String, partnerId: String, partnerName: String) {
        val userMessage = ChatMessage(
            content = content, 
            senderId = currentUserId, 
            receiverId = partnerId,
            ownerId = currentUserId,
            partnerName = partnerName,
            isFromUser = true
        )
        chatMessageDao.insertMessage(userMessage)

        delay(1000)

        val responseContent = if (partnerId == "assistant") {
            "I'm your AI assistant for QhagoApp. How can I help you today?"
        } else {
            "Hello! I received your message: '$content'. (Simulated Response)"
        }

        val responseMessage = ChatMessage(
            content = responseContent,
            senderId = partnerId,
            receiverId = currentUserId,
            ownerId = currentUserId,
            partnerName = partnerName,
            isFromUser = false
        )
        chatMessageDao.insertMessage(responseMessage)
    }

    override suspend fun clearChat(currentUserId: String, partnerId: String) {
        chatMessageDao.clearChat(currentUserId, partnerId)
    }
}
