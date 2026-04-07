package com.example.myapplication.service.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.entity.ChatMessageEntity
import com.example.myapplication.data.local.entity.ContactEntity
import kotlinx.coroutines.*

class ChatNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var database: AppDatabase
    private val extractor = NotificationDataExtractor()

    private var isInitialized = false

    override fun onCreate() {
        super.onCreate()
        initialize()
    }

    private fun initialize() {
        if (!isInitialized) {
            database = AppDatabase.getInstance(applicationContext)
            isInitialized = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        // Skip our own notifications
        if (sbn.packageName == applicationContext.packageName) {
            return
        }

        initialize()

        serviceScope.launch {
            val notification = extractor.extractFromNotification(sbn) ?: return@launch

            // Store message in database
            val messageEntity = ChatMessageEntity(
                content = notification.messageContent,
                isUser = true,  // Received message
                senderName = notification.senderName,
                senderApp = notification.packageName,
                conversationId = notification.conversationId,
                timestamp = notification.timestamp
            )
            database.chatMessageDao().insertMessage(messageEntity)

            // Update contact
            val contact = ContactEntity(
                conversationId = notification.conversationId,
                senderName = notification.senderName,
                senderApp = notification.packageName,
                appDisplayName = notification.appDisplayName,
                lastMessageTime = notification.timestamp,
                lastMessagePreview = notification.messageContent.take(50),
                isAutoReplyEnabled = true,
                unreadCount = 1
            )
            database.contactDao().insertOrUpdateContact(contact)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Optionally handle notification dismissal
    }

    companion object {
        fun getEnabledListenerPackages(context: android.content.Context): List<String> {
            return try {
                val listener = context.getSystemService(NOTIFICATION_SERVICE) as? android.app.NotificationManager
                listener?.getEnabledListenerPackages()?.toList() ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
}
