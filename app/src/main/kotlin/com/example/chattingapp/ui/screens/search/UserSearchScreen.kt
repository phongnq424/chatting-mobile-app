package com.example.chattingapp.ui.screens.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.chattingapp.domain.model.User
import com.example.chattingapp.ui.common.rememberDebouncedClick
import com.example.chattingapp.viewmodel.UserSearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSearchScreen(
    viewModel: UserSearchViewModel,
    onBack: () -> Unit,
    onConversationCreated: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var creatingUserId by remember { mutableStateOf<String?>(null) }

    val isBusy = uiState.isLoading || creatingUserId != null

    val onBackDebounced = rememberDebouncedClick {
        if (!isBusy) {
            onBack()
        }
    }

    val onSearchDebounced = rememberDebouncedClick {
        if (!isBusy) {
            viewModel.searchUsers()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        if (uiState.errorMessage != null) {
            creatingUserId = null
        }
    }

    LaunchedEffect(uiState.createdConversationId) {
        val conversationId = uiState.createdConversationId

        if (conversationId != null) {
            creatingUserId = null
            viewModel.clearCreatedConversation()
            onConversationCreated(conversationId)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Tạo cuộc trò chuyện",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        enabled = !isBusy,
                        onClick = onBackDebounced
                    ) {
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
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8F9FA))
                .padding(paddingValues)
                .navigationBarsPadding()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy,
                label = {
                    Text("Nhập email người dùng")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    IconButton(
                        enabled = !isBusy,
                        onClick = onSearchDebounced
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.size(12.dp))

            Button(
                onClick = onSearchDebounced,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (uiState.isLoading && creatingUserId == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Tìm kiếm")
                }
            }

            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.size(16.dp))

            uiState.users.forEach { user ->
                UserItem(
                    user = user,
                    enabled = !isBusy,
                    isCreating = creatingUserId == user.id,
                    onClick = {
                        if (!isBusy) {
                            creatingUserId = user.id
                            viewModel.createDirectConversation(user)
                        }
                    }
                )

                HorizontalDivider(thickness = 0.5.dp)
            }
        }
    }
}

@Composable
private fun UserItem(
    user: User,
    enabled: Boolean,
    isCreating: Boolean,
    onClick: () -> Unit
) {
    val onClickDebounced = rememberDebouncedClick {
        onClick()
    }

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                onClick = onClickDebounced
            ),
        leadingContent = {
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary
            ) {
                if (user.photoUrl.isNotBlank()) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            }
        },
        headlineContent = {
            Text(
                text = user.displayName,
                fontWeight = FontWeight.SemiBold
            )
        },
        supportingContent = {
            Text(
                text = user.email,
                color = Color.Gray
            )
        },
        trailingContent = {
            if (isCreating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Nhắn tin",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    )
}