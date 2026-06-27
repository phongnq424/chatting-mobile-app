package com.example.chattingapp.data.repository

import android.util.Log
import com.example.chattingapp.data.mapper.toDomain
import com.example.chattingapp.data.model.MessageDto
import com.example.chattingapp.domain.model.Message
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MessageRepository {

    private val db = FirebaseFirestore.getInstance()

    fun observeMessages(
        conversationId: String,
        limit: Long = 50L
    ): Flow<List<Message>> = callbackFlow {
        val listener = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MessageRepository", "observeMessages failed", error)

                    trySend(emptyList())
                    close()

                    return@addSnapshotListener
                }

                val messages = snapshot
                    ?.documents
                    ?.mapNotNull { document ->
                        document.toObject(MessageDto::class.java)
                            ?.toDomain(fallbackId = document.id)
                    }
                    ?: emptyList()

                trySend(messages)
            }

        awaitClose {
            listener.remove()
        }
    }

    suspend fun sendTextMessage(
        conversationId: String,
        sender: FirebaseUser,
        text: String
    ) {
        val cleanText = text.trim()

        if (cleanText.isBlank()) return

        val conversationRef = db.collection("conversations")
            .document(conversationId)

        val messageRef = conversationRef
            .collection("messages")
            .document()

        val messageId = messageRef.id

        val messageData = mapOf(
            "id" to messageId,
            "conversationId" to conversationId,
            "senderId" to sender.uid,
            "senderName" to (sender.displayName ?: "Ẩn danh"),
            "senderPhotoUrl" to (sender.photoUrl?.toString() ?: ""),
            "type" to "TEXT",
            "text" to cleanText,
            "attachments" to emptyList<Map<String, Any>>(),
            "createdAt" to FieldValue.serverTimestamp(),
            "editedAt" to null,
            "deletedAt" to null,
            "status" to "SENT",
            "readBy" to mapOf(
                sender.uid to FieldValue.serverTimestamp()
            )
        )

        val conversationUpdate = mapOf(
            "lastMessageId" to messageId,
            "lastMessageText" to cleanText,
            "lastMessageAt" to FieldValue.serverTimestamp(),
            "lastSenderId" to sender.uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.runBatch { batch ->
            batch.set(messageRef, messageData)
            batch.update(conversationRef, conversationUpdate)
        }.await()
    }

    suspend fun editMessage(
        conversationId: String,
        messageId: String,
        newText: String
    ) {
        val cleanText = newText.trim()

        if (cleanText.isBlank()) return

        db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(messageId)
            .update(
                mapOf(
                    "text" to cleanText,
                    "editedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }

    suspend fun softDeleteMessage(
        conversationId: String,
        messageId: String,
        currentUserId: String
    ) {
        val conversationRef = db.collection("conversations")
            .document(conversationId)

        val messageRef = conversationRef
            .collection("messages")
            .document(messageId)

        val messageSnapshot = messageRef.get().await()
        val message = messageSnapshot.toObject(MessageDto::class.java)
            ?: throw IllegalStateException("Tin nhắn không tồn tại")

        if (message.senderId != currentUserId) {
            throw IllegalStateException("Bạn chỉ có thể thu hồi tin nhắn của mình")
        }

        if (message.deletedAt != null) return

        val updates = mapOf(
            "text" to "",
            "attachments" to emptyList<Map<String, Any>>(),
            "deletedAt" to FieldValue.serverTimestamp()
        )

        val conversationSnapshot = conversationRef.get().await()
        val lastMessageId = conversationSnapshot.getString("lastMessageId")

        db.runBatch { batch ->
            batch.update(messageRef, updates)

            if (lastMessageId == messageId) {
                batch.update(
                    conversationRef,
                    mapOf(
                        "lastMessageText" to "Tin nhắn đã được thu hồi",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }.await()
    }

    suspend fun markMessageAsRead(
        conversationId: String,
        messageId: String,
        userId: String
    ) {
        db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .document(messageId)
            .update("readBy.$userId", FieldValue.serverTimestamp())
            .await()
    }
}