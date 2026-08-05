package com.thruxion.app.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.thruxion.app.data.dao.ChatMessageDao
import com.thruxion.app.data.dao.ContactDao
import com.thruxion.app.data.model.ChatMessage
import com.thruxion.app.data.model.Contact
import com.thruxion.app.network.api.CommunicationsApiService
import com.thruxion.app.network.model.MessageRequest
import com.thruxion.app.network.model.PublicKeyDto
import com.thruxion.app.utils.AsymmetricCryptoManager
import com.thruxion.app.utils.CryptoManager
import com.thruxion.app.utils.ZeroWidthEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

class ChatRepositoryImpl(
    private val chatMessageDao: ChatMessageDao,
    private val contactDao: ContactDao,
    private val apiService: CommunicationsApiService,
    private val contentResolver: ContentResolver,
    private val cryptoManager: CryptoManager? = null
) : ChatRepository {

    private val publicKeyCache = mutableMapOf<String, String>()

    init {
        // Upload our public key on initialization
        // In a real app, this should be triggered after login
    }

    override fun getMessages(currentUserId: String, partnerId: String): Flow<List<ChatMessage>> = 
        chatMessageDao.getMessagesWith(currentUserId, partnerId).map { messages ->
            messages.map { msg ->
                if (ZeroWidthEncoder.hasHiddenData(msg.content)) {
                    // Use a blocking call here for simplicity in Flow mapping, 
                    // but in a production app we'd use a more reactive approach
                    kotlinx.coroutines.runBlocking { decryptIfNeeded(msg) }
                } else msg
            }
        }

    override fun getActiveChats(currentUserId: String): Flow<List<ChatMessage>> = 
        chatMessageDao.getActiveChats(currentUserId).map { chats ->
            chats.map { msg ->
                if (ZeroWidthEncoder.hasHiddenData(msg.content)) {
                    kotlinx.coroutines.runBlocking { decryptIfNeeded(msg) }
                } else msg
            }
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
                val encrypted = cryptoManager.encryptWithSecret(content.toByteArray(), sharedSecret) 
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
        if (partnerId == "assistant") return null
        publicKeyCache[partnerId]?.let { return it }

        val contact = contactDao.getContactByRemoteId(partnerId)
        val publicKey = contact?.publicKey
        if (publicKey != null) {
            publicKeyCache[partnerId] = publicKey
            return publicKey
        }

        return try {
            val response = apiService.getPublicKey(partnerId)
            if (response.isSuccessful) {
                response.body()?.publicKey?.also {
                    publicKeyCache[partnerId] = it
                }
            } else null
        } catch (e: Exception) { null }
    }

    override suspend fun sendImage(imageUri: String, currentUserId: String, partnerId: String, partnerName: String, encrypt: Boolean) {
        val imageBytes = withContext(Dispatchers.IO) {
            contentResolver.openInputStream(Uri.parse(imageUri))?.use { it.readBytes() }
        } ?: return

        var finalMediaUrl = imageUri
        var finalContent = "[Image]"

        if (encrypt && cryptoManager != null) {
            val partnerPublicKey = getPartnerPublicKey(partnerId)
            if (partnerPublicKey != null) {
                val sharedSecret = AsymmetricCryptoManager.deriveSharedSecret(partnerPublicKey)
                val encryptedBytes = cryptoManager.encryptWithSecret(imageBytes, sharedSecret)
                
                val uploadedUrl = uploadEncryptedMedia(encryptedBytes)
                if (uploadedUrl != null) {
                    finalMediaUrl = uploadedUrl
                    val decoy = "[Image] for $partnerName"
                    finalContent = ZeroWidthEncoder.encode(uploadedUrl.toByteArray(), decoy)
                }
            }
        }

        val userMessage = ChatMessage(
            content = finalContent,
            senderId = currentUserId,
            receiverId = partnerId,
            ownerId = currentUserId,
            partnerName = partnerName,
            isFromUser = true,
            type = "IMAGE",
            mediaUrl = finalMediaUrl
        )
        chatMessageDao.insertMessage(userMessage)
        
        if (finalMediaUrl != imageUri) {
            try {
                val request = MessageRequest(partnerId, finalContent)
                apiService.sendMessage(request)
            } catch (e: Exception) {}
        }
    }

    private suspend fun uploadEncryptedMedia(data: ByteArray): String? {
        return try {
            val mediaType = "application/octet-stream".toMediaType()
            val requestBody = data.toRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData("file", "encrypted_media.bin", requestBody)
            val response = apiService.uploadMedia(part)
            if (response.isSuccessful) response.body()?.url else null
        } catch (e: Exception) { null }
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

    private suspend fun decryptIfNeeded(message: ChatMessage): ChatMessage {
        if (cryptoManager != null && ZeroWidthEncoder.hasHiddenData(message.content)) {
            try {
                val encodedData = ZeroWidthEncoder.decode(message.content)
                if (encodedData != null) {
                    val partnerId = if (message.isFromUser) message.receiverId else message.senderId
                    val publicKey = getPartnerPublicKey(partnerId)
                    
                    if (publicKey != null) {
                        val sharedSecret = AsymmetricCryptoManager.deriveSharedSecret(publicKey)
                        val decryptedBytes = cryptoManager.decryptWithSecret(encodedData, sharedSecret)
                        
                        val decrypted = if (message.type == "IMAGE") {
                            // For images, the content is the encrypted URL
                            String(decryptedBytes)
                        } else {
                            String(decryptedBytes)
                        }

                        return if (message.type == "IMAGE") {
                            message.copy(mediaUrl = decrypted, content = "[Image]").apply { isOversecDecrypted = true }
                        } else {
                            message.copy(content = decrypted).apply { isOversecDecrypted = true }
                        }
                    }
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
