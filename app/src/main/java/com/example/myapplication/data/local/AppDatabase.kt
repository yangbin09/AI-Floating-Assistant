package com.example.myapplication.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.myapplication.data.local.dao.ChatMessageDao
import com.example.myapplication.data.local.dao.ContactDao
import com.example.myapplication.data.local.dao.ConversationContextDao
import com.example.myapplication.data.local.entity.ChatMessageEntity
import com.example.myapplication.data.local.entity.ContactEntity
import com.example.myapplication.data.local.entity.ConversationContextEntity

@Database(
    entities = [
        ChatMessageEntity::class,
        ContactEntity::class,
        ConversationContextEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun contactDao(): ContactDao
    abstract fun conversationContextDao(): ConversationContextDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_assistant_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
