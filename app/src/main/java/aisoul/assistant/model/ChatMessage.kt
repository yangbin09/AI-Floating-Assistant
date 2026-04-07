package com.aisoul.assistant.model

import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val senderName: String? = null,         // For notification-based messages
    val senderApp: String? = null,           // Package name
    val conversationId: String? = null,        // For grouping
    val isAutoReplied: Boolean = false,      // Mark if auto-replied
    val appDisplayName: String? = null       // WeChat, WhatsApp, etc.
)

data class ConversationItem(
    val conversationId: String,
    val senderName: String,
    val appDisplayName: String,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int,
    val isAutoReplyEnabled: Boolean
)

data class PendingReply(
    val id: String = UUID.randomUUID().toString(),
    val conversationId: String,
    val senderName: String,
    val originalMessage: String,
    val generatedReply: String,
    val timestamp: Long = System.currentTimeMillis()
)
