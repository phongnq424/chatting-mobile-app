package com.example.chattingapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chattingapp.domain.model.Conversation
import com.example.chattingapp.domain.usecase.ObserveConversationsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class ConversationListUiState(
    val isLoading: Boolean = true,
    val conversations: List<Conversation> = emptyList(),
    val errorMessage: String? = null
)

class ConversationListViewModel(
    private val currentUserId: String,
    private val observeConversationsUseCase: ObserveConversationsUseCase
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
            conversations = emptyList(),
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
                        conversations = emptyList(),
                        errorMessage = "Không thể tải danh sách cuộc trò chuyện"
                    )
                }
                .collect { conversations ->
                    _uiState.value = ConversationListUiState(
                        isLoading = false,
                        conversations = conversations,
                        errorMessage = null
                    )
                }
        }
    }

    fun stopObservingConversations() {
        observeConversationsJob?.cancel()
        observeConversationsJob = null

        _uiState.value = ConversationListUiState(
            isLoading = false,
            conversations = emptyList(),
            errorMessage = null
        )
    }

    override fun onCleared() {
        observeConversationsJob?.cancel()
        super.onCleared()
    }
}