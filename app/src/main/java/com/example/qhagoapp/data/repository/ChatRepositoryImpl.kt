package com.example.qhagoapp.data.repository

import com.example.qhagoapp.data.dao.ChatMessageDao
import com.example.qhagoapp.data.dao.ContactDao
import com.example.qhagoapp.data.model.ChatMessage
import com.example.qhagoapp.data.model.Contact
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow

class ChatRepositoryImpl(
    private val chatMessageDao: ChatMessageDao,
    private val contactDao: ContactDao
) : ChatRepository {

    override fun getMessages(currentUserId: String, partnerId: String): Flow<List<ChatMessage>> = 
        chatMessageDao.getMessagesWith(currentUserId, partnerId)

    override fun getActiveChats(currentUserId: String): Flow<List<ChatMessage>> = 
        chatMessageDao.getActiveChats(currentUserId)

    override fun getAllContacts(currentUserId: String): Flow<List<Contact>> = 
        contactDao.getAllContacts(currentUserId)

    override suspend fun sendMessage(content: String, currentUserId: String, partnerId: String) {
        val userMessage = ChatMessage(
            content = content, 
            senderId = currentUserId, 
            receiverId = partnerId,
            ownerId = currentUserId,
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
            isFromUser = false
        )
        chatMessageDao.insertMessage(responseMessage)
    }

    override suspend fun clearChat(currentUserId: String, partnerId: String) {
        chatMessageDao.clearChat(currentUserId, partnerId)
    }

    override suspend fun deleteChat(currentUserId: String, partnerId: String) {
        chatMessageDao.clearChat(currentUserId, partnerId)
    }
}
