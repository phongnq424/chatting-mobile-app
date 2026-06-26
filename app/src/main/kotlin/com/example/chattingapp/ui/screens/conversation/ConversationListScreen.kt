package com.example.chattingapp.ui.screens.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.chattingapp.core.common.DateTimeFormatter
import com.example.chattingapp.domain.model.Conversation
import com.example.chattingapp.domain.model.ConversationType
import com.example.chattingapp.viewmodel.ConversationListViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    viewModel: ConversationListViewModel,
    onOpenConversation: (String) -> Unit,
    onCreateConversation: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    val currentUserId = remember {
        FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Chats",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                ),
                navigationIcon = {
                    IconButton(onClick = onCreateConversation) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search user"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Menu"
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Hồ sơ cá nhân") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            onClick = {
                                showMenu = false
                                onNavigateToProfile()
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "Đăng xuất",
                                    color = Color.Red
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = null,
                                    tint = Color.Red
                                )
                            },
                            onClick = {
                                showMenu = false
                                onLogout()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateConversation,
                containerColor = Color(0xFF9181F4),
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New chat"
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
                .navigationBarsPadding()
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF9181F4)
                    )
                }

                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage ?: "",
                        color = Color.Red,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.conversations.isEmpty() -> {
                    EmptyConversationState(
                        modifier = Modifier.align(Alignment.Center),
                        onCreateConversation = onCreateConversation
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.conversations,
                            key = { it.id }
                        ) { conversation ->
                            ConversationItem(
                                conversation = conversation,
                                currentUserId = currentUserId,
                                onClick = {
                                    onOpenConversation(conversation.id)
                                }
                            )
                            HorizontalDivider(thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    currentUserId: String,
    onClick: () -> Unit
) {
    val displayTitle = rememberConversationDisplayTitle(
        conversation = conversation,
        currentUserId = currentUserId
    )

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = {
            Surface(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                color = Color(0xFF9181F4)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = displayTitle.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        headlineContent = {
            Text(
                text = displayTitle,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = conversation.lastMessageText.ifBlank { "Chưa có tin nhắn" },
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            Text(
                text = DateTimeFormatter.formatChatTime(conversation.lastMessageAt),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    )
}

@Composable
private fun rememberConversationDisplayTitle(
    conversation: Conversation,
    currentUserId: String
): String {
    var displayTitle by remember(
        conversation.id,
        conversation.title,
        conversation.type,
        currentUserId
    ) {
        mutableStateOf(conversation.defaultDisplayTitle())
    }

    LaunchedEffect(
        conversation.id,
        conversation.type,
        currentUserId
    ) {
        if (conversation.type != ConversationType.DIRECT) {
            displayTitle = conversation.title.ifBlank { "Nhóm chat" }
            return@LaunchedEffect
        }

        val otherUserId = conversation.memberIds.firstOrNull { it != currentUserId }

        if (otherUserId.isNullOrBlank()) {
            displayTitle = conversation.title.ifBlank { "Người dùng" }
            return@LaunchedEffect
        }

        FirebaseFirestore
            .getInstance()
            .collection("users")
            .document(otherUserId)
            .get()
            .addOnSuccessListener { document ->
                val name = document.getString("displayName")
                    ?: document.getString("name")
                    ?: document.getString("email")

                displayTitle = name
                    ?.takeIf { it.isNotBlank() }
                    ?: conversation.title.ifBlank { "Người dùng" }
            }
            .addOnFailureListener {
                displayTitle = conversation.title.ifBlank { "Người dùng" }
            }
    }

    return displayTitle
}

@Composable
private fun EmptyConversationState(
    modifier: Modifier = Modifier,
    onCreateConversation: () -> Unit
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Chưa có cuộc trò chuyện",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Bấm nút + để tìm bạn bè bằng email và bắt đầu nhắn tin.",
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .clickable(onClick = onCreateConversation)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color(0xFF9181F4)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Tạo cuộc trò chuyện",
                color = Color(0xFF9181F4),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun Conversation.defaultDisplayTitle(): String {
    return when (type) {
        ConversationType.DIRECT -> {
            title.ifBlank { "Người dùng" }
        }

        ConversationType.GROUP -> {
            title.ifBlank { "Nhóm chat" }
        }
    }
}