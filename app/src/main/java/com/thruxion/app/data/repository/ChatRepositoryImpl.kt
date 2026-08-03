package com.thruxion.app.data.repository

import com.thruxion.app.data.dao.ChatMessageDao
import com.thruxion.app.data.dao.ContactDao
import com.thruxion.app.data.model.ChatMessage
import com.thruxion.app.network.api.CommunicationsApiService
import com.thruxion.app.network.model.MessageRequest
import com.thruxion.app.network.model.PublicKeyDto
import com.thruxion.app.utils.AsymmetricCryptoManager
import com.thruxion.app.utils.CryptoManager
import com.thruxion.app.utils.ZeroWidthEncoder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepositoryImpl(
    private val chatMessageDao: ChatMessageDao,
    private val contactDao: ContactDao,
    private val apiService: CommunicationsApiService,
    private val cryptoManager: CryptoManager? = null
) : ChatRepository {

    init {
        // Upload our public key on initialization
        // In a real app, this should be triggered after login
    }

    override fun getMessages(currentUserId: String, partnerId: String): Flow<List<ChatMessage>> = 
        chatMessageDao.getMessagesWith(currentUserId, partnerId).map { messages ->
            messages.map { decryptIfNeeded(it) }
        }

    override fun getActiveChats(currentUserId: String): Flow<List<ChatMessage>> = 
        chatMessageDao.getActiveChats(currentUserId).map { chats ->
            chats.map { decryptIfNeeded(it) }
        }

    override suspend fun sendMessage(content: String, currentUserId: String, partnerId: String, partnerName: String, encrypt: Boolean) {
        if (partnerId == "assistant") {
            simulateAssistantResponse(content, currentUserId, partnerId, partnerName, encrypt)
            return
        }

        val finalContent = if (encrypt && cryptoManager != null) {
            val partnerPublicKey = getPartnerPublicKey(partnerId)
            if (partnerPublicKey != null) {
                val sharedSecret = AsymmetricCryptoManager.deriveSharedSecret(partnerPublicKey)
                val encrypted = cryptoManager.encrypt(content) 
                val decoy = "Encrypted message for $partnerName"
                ZeroWidthEncoder.encode(encrypted, decoy)
            } else {
                content
            }
        } else {
            content
        }

        // OPTIMISTIC UI: Insert locally first
        val userMessage = ChatMessage(
            content = finalContent,
            senderId = currentUserId,
            receiverId = partnerId,
            ownerId = currentUserId,
            partnerName = partnerName,
            isFromUser = true
        )
        chatMessageDao.insertMessage(userMessage)

        try {
            val request = MessageRequest(partnerId, finalContent)
            val response = apiService.sendMessage(request)
            if (response.isSuccessful) {
                // Optionally update the message ID from the server response
            }
        } catch (e: Exception) {
            // Log network error, but message is already in local DB
        }
    }

    private suspend fun getPartnerPublicKey(partnerId: String): String? {
        // 1. Check local DB (Contact)
        // val contact = contactDao.getContactByRemoteId(partnerId)
        // if (contact?.publicKey != null) return contact.publicKey

        // 2. Fetch from server
        return try {
            val response = apiService.getPublicKey(partnerId)
            if (response.isSuccessful) {
                response.body()?.publicKey
            } else null
        } catch (e: Exception) { null }
    }

    override suspend fun sendImage(imageUri: String, currentUserId: String, partnerId: String, partnerName: String, encrypt: Boolean) {
        // 1. Encrypt image if needed
        // val imageBytes = contentResolver.openInputStream(Uri.parse(imageUri)).readBytes()
        // val finalUri = if (encrypt) uploadEncryptedImage(imageBytes) else uploadPlainImage(imageBytes)

        val userMessage = ChatMessage(
            content = "[Image]",
            senderId = currentUserId,
            receiverId = partnerId,
            ownerId = currentUserId,
            partnerName = partnerName,
            isFromUser = true,
            type = "IMAGE",
            mediaUrl = imageUri // Local URI for now
        )
        chatMessageDao.insertMessage(userMessage)
    }

    private suspend fun simulateAssistantResponse(content: String, currentUserId: String, partnerId: String, partnerName: String, encrypt: Boolean) {
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
