package com.example.myapplication.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey
    val conversationId: String,             // "${senderApp}_${senderName}"
    val senderName: String,
    val senderApp: String,                   // Package name
    val appDisplayName: String,              // Human readable: "WeChat"
    val lastMessageTime: Long = 0,
    val lastMessagePreview: String = "",
    val isAutoReplyEnabled: Boolean = true,
    val unreadCount: Int = 0
)
