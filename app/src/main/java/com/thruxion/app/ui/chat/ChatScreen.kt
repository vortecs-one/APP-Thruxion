package com.thruxion.app.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
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
import com.thruxion.app.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

data class PinnedChat(
    val id: String,
    val name: String,
    val color: Color
)

val DEFAULT_PINNED_CHATS = listOf(
    PinnedChat("assistant", "AI", Color(0xFF007F95)),
    PinnedChat("sos", "S.O.S", Color(0xFFD32F2F)),
    PinnedChat("qhago", "Q?", Color(0xFF1976D2)),
    PinnedChat("mywitness", "Mw", Color(0xFF7B1FA2))
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF007F95),
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
            item {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    items(DEFAULT_PINNED_CHATS) { pinned ->
                        PinnedChatItemCircle(pinned) {
                            viewModel.navigateToDetail(pinned.id, pinned.name)
                        }
                        if (pinned != DEFAULT_PINNED_CHATS.last())
                            Spacer(modifier = Modifier.width(20.dp))
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
            }

            if (activeChats.isNotEmpty())
            {
                item {
                    Text(
                        text = "Recent Chats",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray
                    )
                }
            }

            val filteredActive = activeChats.filter { chat ->
                val partnerId = if (chat.isFromUser) chat.receiverId else chat.senderId
                DEFAULT_PINNED_CHATS.none { it.id == partnerId }
            }

            items(filteredActive, key = { it.id }) { chat ->
                val partnerId = if (chat.isFromUser) chat.receiverId else chat.senderId
                
                ChatItemRow(
                    lastMessage = chat,
                    displayName = chat.partnerName,
                    onClick = { viewModel.navigateToDetail(partnerId, chat.partnerName) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray)
            }
        }
    }
}

@Composable
fun PinnedChatItemCircle(pinned: PinnedChat, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(64.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(pinned.color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = pinned.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = if (pinned.name.length > 2) 14.sp else 18.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (pinned.id == "qhago") "Qhago?" else if (pinned.id == "mywitness") "MyWitness" else pinned.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.DarkGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    val isEncryptionEnabled by viewModel.isEncryptionEnabled.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(messages.size, imeVisible) {
        if (messages.isNotEmpty())
            listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F2F5))
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF007F95),
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
                    Text(text = "Online", color = Color(0xFFE3F2FD), style = MaterialTheme.typography.bodySmall)
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
            IconButton(
                onClick = { viewModel.toggleEncryption() },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Toggle Encryption",
                    tint = if (isEncryptionEnabled) Color(0xFF007F95) else Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
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
                containerColor = Color(0xFF007F95),
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
    val bubbleColor = if (isUser) Color(0xFFE0F2F1) else Color.White
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (message.isOversecDecrypted) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Oversec Encrypted",
                                tint = Color(0xFF007F95),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(text = message.content, color = Color.Black, fontSize = 16.sp)
                    }
                    Text(
                        text = timeFormatter.format(Date(message.timestamp)),
                        color = Color.Gray, fontSize = 11.sp, modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}
