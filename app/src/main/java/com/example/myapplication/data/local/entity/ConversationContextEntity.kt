package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversation_contexts")
data class ConversationContextEntity(
    @PrimaryKey
    val conversationId: String,
    val recentMessagesJson: String = "[]",   // JSON array of last N messages for AI context
    val lastUpdated: Long = System.currentTimeMillis(),
    val messageCount: Int = 0                // Total messages in this conversation
)
