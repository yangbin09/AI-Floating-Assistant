package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val isUser: Boolean,                    // true = received message, false = AI reply
    val timestamp: Long = System.currentTimeMillis(),
    val senderName: String,                  // Contact name (e.g., "Zhang San")
    val senderApp: String,                   // Package name (e.g., "com.tencent.mm")
    val conversationId: String,              // Unique per contact per app: "${senderApp}_${senderName}"
    val isAutoReplied: Boolean = false,     // Whether this was auto-replied
    val originalMessageId: Long? = null      // If this is a reply, link to original
)
