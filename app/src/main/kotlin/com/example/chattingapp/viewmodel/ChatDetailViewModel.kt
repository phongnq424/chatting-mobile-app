package com.example.chattingapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chattingapp.data.repository.AuthRepository
import com.example.chattingapp.domain.model.Message
import com.example.chattingapp.domain.usecase.DeleteMessageUseCase
import com.example.chattingapp.domain.usecase.MarkAsReadUseCase
import com.example.chattingapp.domain.usecase.ObserveMessagesUseCase
import com.example.chattingapp.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatDetailUiState(
    val currentUserId: String = "",
    val isLoading: Boolean = true,
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val deletingMessageIds: Set<String> = emptySet(),
    val errorMessage: String? = null
)

class ChatDetailViewModel(
    private val conversationId: String,
    private val authRepository: AuthRepository,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val markAsReadUseCase: MarkAsReadUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatDetailUiState(
            currentUserId = authRepository.getCurrentUser()?.uid.orEmpty()
        )
    )

    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    init {
        observeMessages()
        markConversationAsRead()
    }

    fun onInputChange(value: String) {
        _uiState.value = _uiState.value.copy(inputText = value)
    }

    fun sendMessage() {
        val currentUser = authRepository.getCurrentUser()
        val text = _uiState.value.inputText.trim()

        if (currentUser == null) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Người dùng chưa đăng nhập"
            )
            return
        }

        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSending = true,
                errorMessage = null
            )

            try {
                sendMessageUseCase(
                    conversationId = conversationId,
                    sender = currentUser,
                    text = text
                )

                _uiState.value = _uiState.value.copy(
                    inputText = "",
                    isSending = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    errorMessage = e.localizedMessage ?: "Gửi tin nhắn thất bại"
                )
            }
        }
    }

    private fun observeMessages() {
        viewModelScope.launch {
            observeMessagesUseCase(conversationId)
                .collect { messages ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        messages = messages,
                        errorMessage = null
                    )
                }
        }
    }

    private fun markConversationAsRead() {
        val currentUser = authRepository.getCurrentUser() ?: return

        viewModelScope.launch {
            runCatching {
                markAsReadUseCase(
                    conversationId = conversationId,
                    userId = currentUser.uid
                )
            }
        }
    }

    fun deleteMessage(message: Message) {
        val currentUserId = _uiState.value.currentUserId

        if (currentUserId.isBlank()) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Người dùng chưa đăng nhập"
            )
            return
        }

        if (message.senderId != currentUserId) {
            _uiState.value = _uiState.value.copy(
                errorMessage = "Bạn chỉ có thể thu hồi tin nhắn của mình"
            )
            return
        }

        if (message.deletedAt != null) return

        if (_uiState.value.deletingMessageIds.contains(message.id)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                deletingMessageIds = _uiState.value.deletingMessageIds + message.id,
                errorMessage = null
            )

            try {
                deleteMessageUseCase(
                    conversationId = conversationId,
                    messageId = message.id,
                    currentUserId = currentUserId
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.localizedMessage ?: "Thu hồi tin nhắn thất bại"
                )
            } finally {
                _uiState.value = _uiState.value.copy(
                    deletingMessageIds = _uiState.value.deletingMessageIds - message.id
                )
            }
        }
    }
}