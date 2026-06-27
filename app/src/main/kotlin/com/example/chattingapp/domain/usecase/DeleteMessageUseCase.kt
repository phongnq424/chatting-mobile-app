package com.example.chattingapp.domain.usecase

import com.example.chattingapp.data.repository.MessageRepository

class DeleteMessageUseCase(
    private val messageRepository: MessageRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        messageId: String,
        currentUserId: String
    ) {
        messageRepository.softDeleteMessage(
            conversationId = conversationId,
            messageId = messageId,
            currentUserId = currentUserId
        )
    }
}