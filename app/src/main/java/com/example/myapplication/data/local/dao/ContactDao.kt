package com.example.myapplication.data.local.dao

import androidx.room.*
import com.example.myapplication.data.local.entity.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY lastMessageTime DESC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE conversationId = :conversationId")
    suspend fun getContact(conversationId: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE senderApp = :appPackage")
    fun getContactsByApp(appPackage: String): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateContact(contact: ContactEntity)

    @Query("UPDATE contacts SET unreadCount = unreadCount + 1, lastMessageTime = :time, lastMessagePreview = :preview WHERE conversationId = :conversationId")
    suspend fun incrementUnreadCount(conversationId: String, time: Long, preview: String)

    @Query("UPDATE contacts SET unreadCount = 0 WHERE conversationId = :conversationId")
    suspend fun clearUnreadCount(conversationId: String)

    @Query("UPDATE contacts SET isAutoReplyEnabled = :enabled WHERE conversationId = :conversationId")
    suspend fun setAutoReplyEnabled(conversationId: String, enabled: Boolean)
}
