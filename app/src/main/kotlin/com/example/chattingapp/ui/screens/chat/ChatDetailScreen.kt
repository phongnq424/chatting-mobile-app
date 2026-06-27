package com.example.chattingapp.ui.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chattingapp.core.common.DateTimeFormatter
import com.example.chattingapp.domain.model.Message
import com.example.chattingapp.viewmodel.ChatDetailViewModel
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: ChatDetailViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Chat",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                text = uiState.inputText,
                isSending = uiState.isSending,
                onTextChange = viewModel::onInputChange,
                onSendClick = viewModel::sendMessage
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                uiState.messages.isEmpty() -> {
                    Text(
                        text = "Chưa có tin nhắn. Hãy gửi lời chào đầu tiên.",
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(
                            items = uiState.messages,
                            key = { it.id }
                        ) { message ->
                            MessageBubble(
                                message = message,
                                isMe = message.senderId == uiState.currentUserId,
                                isDeleting = uiState.deletingMessageIds.contains(message.id),
                                onDeleteMessage = {
                                    viewModel.deleteMessage(message)
                                }
                            )
                        }
                    }
                }
            }

            uiState.errorMessage?.let { error ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(12.dp),
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = error,
                        color = Color.Red,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    isSending: Boolean,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        shadowElevation = 8.dp,
        color = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Type a message...")
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.LightGray
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendClick,
                enabled = text.isNotBlank() && !isSending,
                modifier = Modifier.size(48.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    disabledContainerColor = Color.LightGray,
                    disabledContentColor = Color.White
                )
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send"
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isMe: Boolean,
    isDeleting: Boolean,
    onDeleteMessage: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isDeleted = message.deletedAt != null
    val canDelete = isMe && !isDeleted && !isDeleting

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        if (!isMe) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        Surface(
            modifier = Modifier.combinedClickable(
                enabled = canDelete,
                onClick = {},
                onLongClick = {
                    showDeleteDialog = true
                }
            ),
            color = when {
                isDeleted -> Color(0xFFE0E0E0)
                isMe -> MaterialTheme.colorScheme.primary
                else -> Color.White
            },
            contentColor = when {
                isDeleted -> Color.DarkGray
                isMe -> Color.White
                else -> Color.Black
            },
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            shadowElevation = 1.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = when {
                        isDeleting -> "Đang thu hồi..."
                        isDeleted -> "Tin nhắn đã được thu hồi"
                        else -> message.text
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = if (isDeleted) FontStyle.Italic else FontStyle.Normal
                )

                Text(
                    text = DateTimeFormatter.formatChatTime(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isDeleted -> Color.Gray
                        isMe -> Color.White.copy(alpha = 0.75f)
                        else -> Color.Gray
                    },
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text("Thu hồi tin nhắn?")
            },
            text = {
                Text("Tin nhắn sẽ được hiển thị là đã được thu hồi với tất cả người trong cuộc trò chuyện.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteMessage()
                    }
                ) {
                    Text(
                        text = "Thu hồi",
                        color = Color.Red
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}