package com.example.qhagoapp.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.example.qhagoapp.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

data class PinnedChat(
    val id: String,
    val name: String,
    val description: String,
    val color: Color
)

val DEFAULT_PINNED_CHATS = listOf(
    PinnedChat("assistant", "AI", "ThruxionAI", Color(0xFF128C7E)),
    PinnedChat("sos", "S.O.S", "S.O.S", Color(0xFFD32F2F)),
    PinnedChat("qhago", "Qhago?", "Qhago?", Color(0xFF1976D2)),
    PinnedChat("mywitness", "MyWitness", "MyWitness", Color(0xFF7B1FA2))
)

@Composable
fun ChatMain(viewModel: ChatViewModel, onClose: () -> Unit) {
    val uiMode by viewModel.uiMode.collectAsState()

    when (uiMode) {
        ChatUiMode.LIST -> ChatListScreen(viewModel, onClose)
        ChatUiMode.DETAIL -> ChatDetailScreen(viewModel, onBack = { viewModel.navigateToList() }, onClose)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(viewModel: ChatViewModel, onClose: () -> Unit) {
    val activeChats by viewModel.activeChats.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    var chatToDelete by remember { mutableStateOf<String?>(null) }

    if (chatToDelete != null) {
        AlertDialog(
            onDismissRequest = { chatToDelete = null },
            title = { Text("Delete Chat") },
            text = { Text("Are you sure you want to delete this conversation?") },
            confirmButton = {
                TextButton(onClick = {
                    chatToDelete?.let { viewModel.deleteChat(it) }
                    chatToDelete = null
                }) { Text("Delete", color = Color.Red) }
            },
            dismissButton = {
                TextButton(onClick = { chatToDelete = null }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // WhatsApp Header for List
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF075E54),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ThruxionChat",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // Pinned Chats Section
            items(DEFAULT_PINNED_CHATS) { pinned ->
                PinnedChatItemRow(pinned) {
                    viewModel.navigateToDetail(pinned.id, pinned.name)
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
            }

            // Separator for Active Chats if any
            if (activeChats.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Chats",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }
            }

            // Filter out pinned chats from active chats to avoid duplicates
            val filteredActive = activeChats.filter { chat ->
                val partnerId = if (chat.isFromUser) chat.receiverId else chat.senderId
                DEFAULT_PINNED_CHATS.none { it.id == partnerId }
            }

            items(filteredActive, key = { it.id }) { chat ->
                val partnerId = if (chat.isFromUser) chat.receiverId else chat.senderId
                
                // Try to find contact name by remoteUserId OR name if it contains Lawyer
                val contact = contacts.find { 
                    it.remoteUserId == partnerId || 
                    (partnerId.contains("Lawyer", ignoreCase = true) && it.name == partnerId)
                }
                
                val contactName = contact?.name ?: if (partnerId.length > 20) "User ${partnerId.take(8)}" else partnerId

                val swipeState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value == SwipeToDismissBoxValue.EndToStart) {
                            chatToDelete = partnerId
                            false
                        } else false
                    }
                )

                SwipeToDismissBox(
                    state = swipeState,
                    enableDismissFromStartToEnd = false,
                    backgroundContent = {
                        val color = if (swipeState.dismissDirection == SwipeToDismissBoxValue.EndToStart) Color.Red else Color.Transparent
                        Box(
                            modifier = Modifier.fillMaxSize().background(color).padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                        }
                    }
                ) {
                    ChatItemRow(
                        lastMessage = chat,
                        displayName = contactName,
                        onClick = { viewModel.navigateToDetail(partnerId, contactName) }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun PinnedChatItemRow(pinned: PinnedChat, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(pinned.color),
            contentAlignment = Alignment.Center
        ) {
            Text(pinned.name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = pinned.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(
                text = pinned.description,
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ChatItemRow(
    lastMessage: ChatMessage,
    displayName: String,
    onClick: () -> Unit
) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentAlignment = Alignment.Center
        ) {
            Text(displayName.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = displayName, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(text = timeFormatter.format(Date(lastMessage.timestamp)), fontSize = 11.sp, color = Color.Gray)
            }
            Text(
                text = lastMessage.content,
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ChatDetailScreen(viewModel: ChatViewModel, onBack: () -> Unit, onClose: () -> Unit) {
    val messages by viewModel.messages.collectAsState()
    val chatName by viewModel.partnerName.collectAsState()
    val chatPartnerId by viewModel.partnerId.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(messages.size, imeVisible) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE5DDD5))
    ) {
        // WhatsApp Detail Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF075E54),
            tonalElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (chatPartnerId == "assistant") "AI" else chatName.take(1).uppercase(),
                        color = Color.White, 
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = chatName, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(text = "Online", color = Color(0xFFB1F3EB), style = MaterialTheme.typography.bodySmall)
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages) { message -> WhatsAppMessageBubble(message) }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)),
                placeholder = { Text("Type a message") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                maxLines = 4
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier.size(48.dp),
                containerColor = Color(0xFF128C7E),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun WhatsAppMessageBubble(message: ChatMessage) {
    val isUser = message.isFromUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) Color(0xFFE2FFC7) else Color.White
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalAlignment = alignment
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(8.dp, 8.dp, if (isUser) 8.dp else 0.dp, if (isUser) 0.dp else 8.dp),
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Column {
                    Text(text = message.content, color = Color.Black, fontSize = 16.sp)
                    Text(
                        text = timeFormatter.format(Date(message.timestamp)),
                        color = Color.Gray, fontSize = 11.sp, modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}
