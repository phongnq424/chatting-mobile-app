package com.example.chattingapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chattingapp.data.repository.UserRepository
import com.example.chattingapp.domain.model.Conversation
import com.example.chattingapp.domain.model.ConversationType
import com.example.chattingapp.domain.usecase.ObserveConversationsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

data class ConversationListItemUiModel(
    val conversation: Conversation,
    val displayTitle: String,
    val photoUrl: String
)

data class ConversationListUiState(
    val isLoading: Boolean = true,
    val items: List<ConversationListItemUiModel> = emptyList(),
    val errorMessage: String? = null
)

class ConversationListViewModel(
    private val currentUserId: String,
    private val observeConversationsUseCase: ObserveConversationsUseCase,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationListUiState())
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    private var observeConversationsJob: Job? = null

    init {
        observeConversations()
    }

    fun observeConversations() {
        observeConversationsJob?.cancel()

        _uiState.value = ConversationListUiState(
            isLoading = true,
            items = emptyList(),
            errorMessage = null
        )

        observeConversationsJob = viewModelScope.launch {
            observeConversationsUseCase(currentUserId)
                .catch { error ->
                    if (error is CancellationException) throw error

                    Log.e(
                        "ConversationListViewModel",
                        "observeConversations failed for userId=$currentUserId",
                        error
                    )

                    _uiState.value = ConversationListUiState(
                        isLoading = false,
                        items = emptyList(),
                        errorMessage = "Không thể tải danh sách cuộc trò chuyện"
                    )
                }
                .collect { conversations ->
                    val items = buildConversationItems(conversations)

                    _uiState.value = ConversationListUiState(
                        isLoading = false,
                        items = items,
                        errorMessage = null
                    )
                }
        }
    }

    private suspend fun buildConversationItems(
        conversations: List<Conversation>
    ): List<ConversationListItemUiModel> = supervisorScope {
        conversations.map { conversation ->
            async {
                buildConversationItem(conversation)
            }
        }.awaitAll()
    }

    private suspend fun buildConversationItem(
        conversation: Conversation
    ): ConversationListItemUiModel {
        return when (conversation.type) {
            ConversationType.DIRECT -> {
                val otherUserId = conversation.memberIds.firstOrNull { it != currentUserId }

                if (otherUserId.isNullOrBlank()) {
                    ConversationListItemUiModel(
                        conversation = conversation,
                        displayTitle = conversation.title.ifBlank { "Người dùng" },
                        photoUrl = conversation.photoUrl
                    )
                } else {
                    val otherUser = runCatching {
                        userRepository.getUserById(otherUserId)
                    }.getOrNull()

                    ConversationListItemUiModel(
                        conversation = conversation,
                        displayTitle = otherUser?.displayName
                            ?.takeIf { it.isNotBlank() }
                            ?: otherUser?.email
                                ?.takeIf { it.isNotBlank() }
                            ?: conversation.title.ifBlank { "Người dùng" },
                        photoUrl = otherUser?.photoUrl.orEmpty()
                    )
                }
            }

            ConversationType.GROUP -> {
                ConversationListItemUiModel(
                    conversation = conversation,
                    displayTitle = conversation.title.ifBlank { "Nhóm chat" },
                    photoUrl = conversation.photoUrl
                )
            }
        }
    }

    fun stopObservingConversations() {
        observeConversationsJob?.cancel()
        observeConversationsJob = null

        _uiState.value = ConversationListUiState(
            isLoading = false,
            items = emptyList(),
            errorMessage = null
        )
    }

    override fun onCleared() {
        observeConversationsJob?.cancel()
        super.onCleared()
    }
}