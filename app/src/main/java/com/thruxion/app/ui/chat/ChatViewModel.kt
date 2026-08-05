package com.thruxion.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.thruxion.app.data.model.ChatMessage
import com.thruxion.app.data.repository.ChatRepository
import com.thruxion.app.network.security.TokenManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ChatUiMode {
    LIST, DETAIL
}

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {

    private val _uiMode = MutableStateFlow(ChatUiMode.LIST)
    val uiMode: StateFlow<ChatUiMode> = _uiMode

    private val _partnerId = MutableStateFlow<String>("assistant")
    val partnerId: StateFlow<String> = _partnerId

    private val _partnerName = MutableStateFlow<String>("Qhago Assistant")
    val partnerName: StateFlow<String> = _partnerName

    private val _isEncryptionEnabled = MutableStateFlow(true)
    val isEncryptionEnabled: StateFlow<Boolean> = _isEncryptionEnabled

    private val currentUserId: String 
        get() = TokenManager.getUserId().toString()

    @OptIn(ExperimentalCoroutinesApi::class)
    val messages: StateFlow<List<ChatMessage>> = _partnerId
        .flatMapLatest { id -> repository.getMessages(currentUserId, id) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeChats: StateFlow<List<ChatMessage>> = repository.getActiveChats(currentUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun navigateToDetail(id: String?, name: String?) {
        _partnerId.value = id ?: "assistant"
        _partnerName.value = name ?: (if (id == "assistant") "Qhago Assistant" else "Unknown User")
        _uiMode.value = ChatUiMode.DETAIL
    }

    fun navigateToList() {
        _uiMode.value = ChatUiMode.LIST
    }

    fun toggleEncryption() {
        _isEncryptionEnabled.value = !_isEncryptionEnabled.value
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.sendMessage(
                content, 
                currentUserId, 
                _partnerId.value, 
                _partnerName.value,
                _isEncryptionEnabled.value
            )
        }
    }

    fun sendImage(imageUri: String) {
        viewModelScope.launch {
            repository.sendImage(
                imageUri,
                currentUserId,
                _partnerId.value,
                _partnerName.value,
                _isEncryptionEnabled.value
            )
        }
    }
}

class ChatViewModelFactory(private val repository: ChatRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChatViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
