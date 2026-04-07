package com.example.myapplication.domain

import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.entity.ChatMessageEntity
import com.example.myapplication.data.remote.ApiKeyManager
import com.example.myapplication.domain.usecase.GenerateAutoReplyUseCase
import com.example.myapplication.service.notification.ChatNotification
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

enum class AutoReplyMode {
    IMMEDIATE,      // Auto-reply without user confirmation
    CONFIRM_FIRST   // Show notification, let user confirm before sending
}

data class AutoReplyResult(
    val conversationId: String,
    val senderName: String,
    val originalMessage: String,
    val generatedReply: String,
    val wasSent: Boolean,
    val timestamp: Long
)

class AutoReplyManager(
    private val database: AppDatabase,
    private val apiKeyManager: ApiKeyManager,
    private val notificationListener: ChatNotificationListenerService
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val generateReplyUseCase = GenerateAutoReplyUseCase(database, apiKeyManager)

    private val _autoReplyResults = MutableSharedFlow<AutoReplyResult>()
    val autoReplyResults: SharedFlow<AutoReplyResult> = _autoReplyResults

    var replyMode: AutoReplyMode = AutoReplyMode.CONFIRM_FIRST

    fun startListening() {
        scope.launch {
            notificationListener.onNotificationPosted(null) // This won't actually be called here
        }
    }

    suspend fun handleNewMessage(notification: ChatNotification) {
        // Check if auto-reply is enabled for this contact
        val contact = database.contactDao().getContact(notification.conversationId)
        if (contact == null || !contact.isAutoReplyEnabled) {
            return
        }

        // Check if API key is configured
        if (!apiKeyManager.hasApiKey()) {
            return
        }

        // Generate reply
        val replyResult = generateReplyUseCase.generateReply(
            conversationId = notification.conversationId,
            senderName = notification.senderName,
            appDisplayName = notification.appDisplayName,
            newMessage = notification.messageContent
        )

        replyResult.onSuccess { reply ->
            // Store the AI reply
            val aiMessageEntity = ChatMessageEntity(
                content = reply,
                isUser = false,  // AI reply
                senderName = notification.senderName,
                senderApp = notification.packageName,
                conversationId = notification.conversationId,
                timestamp = System.currentTimeMillis()
            )
            database.chatMessageDao().insertMessage(aiMessageEntity)

            val result = AutoReplyResult(
                conversationId = notification.conversationId,
                senderName = notification.senderName,
                originalMessage = notification.messageContent,
                generatedReply = reply,
                wasSent = replyMode == AutoReplyMode.IMMEDIATE,
                timestamp = System.currentTimeMillis()
            )

            _autoReplyResults.emit(result)
        }
    }

    fun stop() {
        scope.cancel()
    }
}
