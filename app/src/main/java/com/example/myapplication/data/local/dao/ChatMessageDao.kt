package com.example.myapplication.data.local.dao

import androidx.room.*
import com.example.myapplication.data.local.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(conversationId: String, limit: Int): List<ChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET isAutoReplied = 1 WHERE id = :messageId")
    suspend fun markAsAutoReplied(messageId: Long)

    @Query("DELETE FROM chat_messages WHERE timestamp < :cutoffTime")
    suspend fun deleteOldMessages(cutoffTime: Long)

    @Query("SELECT COUNT(*) FROM chat_messages WHERE senderApp = :appPackage AND isUser = 1 AND isAutoReplied = 0")
    suspend fun getUnrepliedCount(appPackage: String): Int
}
