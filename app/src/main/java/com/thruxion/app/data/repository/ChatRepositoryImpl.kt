package com.thruxion.app.data.repository

import com.thruxion.app.data.dao.ChatMessageDao
import com.thruxion.app.data.model.ChatMessage
import com.thruxion.app.utils.CryptoManager
import com.thruxion.app.utils.ZeroWidthEncoder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepositoryImpl(
    private val chatMessageDao: ChatMessageDao,
    private val cryptoManager: CryptoManager? = null
) : ChatRepository {

    override fun getMessages(currentUserId: String, partnerId: String): Flow<List<ChatMessage>> = 
        chatMessageDao.getMessagesWith(currentUserId, partnerId).map { messages ->
            messages.map { decryptIfNeeded(it) }
        }

    override fun getActiveChats(currentUserId: String): Flow<List<ChatMessage>> = 
        chatMessageDao.getActiveChats(currentUserId).map { chats ->
            chats.map { decryptIfNeeded(it) }
        }

    override suspend fun sendMessage(content: String, currentUserId: String, partnerId: String, partnerName: String, encrypt: Boolean) {
        val finalContent = if (encrypt && cryptoManager != null) {
            val encrypted = cryptoManager.encrypt(content)
            val decoy = "Message encrypted with Oversec"
            ZeroWidthEncoder.encode(encrypted, decoy)
        } else {
            content
        }

        val userMessage = ChatMessage(
            content = finalContent, 
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

    private fun decryptIfNeeded(message: ChatMessage): ChatMessage {
        if (cryptoManager != null && ZeroWidthEncoder.hasHiddenData(message.content)) {
            try {
                val encodedData = ZeroWidthEncoder.decode(message.content)
                if (encodedData != null) {
                    val decrypted = cryptoManager.decrypt(encodedData)
                    return message.copy(content = decrypted).apply { isOversecDecrypted = true }
                }
            } catch (e: Exception) {
                // Decryption failed (e.g. wrong key), return original message
            }
        }
        return message
    }

    override suspend fun clearChat(currentUserId: String, partnerId: String) {
        chatMessageDao.clearChat(currentUserId, partnerId)
    }
}
