package com.aisoul.assistant.data.local.dao

import androidx.room.*
import com.aisoul.assistant.data.local.entity.ConversationContextEntity

@Dao
interface ConversationContextDao {
    @Query("SELECT * FROM conversation_contexts WHERE conversationId = :conversationId")
    suspend fun getContext(conversationId: String): ConversationContextEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateContext(context: ConversationContextEntity)

    @Query("DELETE FROM conversation_contexts WHERE conversationId = :conversationId")
    suspend fun deleteContext(conversationId: String)
}
