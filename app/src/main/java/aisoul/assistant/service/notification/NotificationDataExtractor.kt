package com.aisoul.assistant.service.notification

import android.service.notification.StatusBarNotification

data class ChatNotification(
    val packageName: String,
    val appDisplayName: String,
    val senderName: String,
    val messageContent: String,
    val conversationId: String,
    val timestamp: Long,
    val isGroup: Boolean = false,
    val groupName: String? = null
)

class NotificationDataExtractor {

    fun extractFromNotification(sbn: StatusBarNotification): ChatNotification? {
        val packageName = sbn.packageName

        // Only process supported apps
        if (!AppPackageMapping.isSupported(packageName)) {
            return null
        }

        val extras = sbn.notification.extras

        // Try to extract sender and message
        val sender = extractSender(extras, packageName)
        val message = extractMessage(extras)

        if (sender == null || message.isNullOrBlank()) {
            return null
        }

        val conversationId = "${packageName}_${sender}"

        return ChatNotification(
            packageName = packageName,
            appDisplayName = AppPackageMapping.getDisplayName(packageName),
            senderName = sender,
            messageContent = message,
            conversationId = conversationId,
            timestamp = sbn.postTime,
            isGroup = isGroupNotification(extras, packageName),
            groupName = extractGroupName(extras, packageName)
        )
    }

    private fun extractSender(extras: android.os.Bundle, packageName: String): String? {
        return when (packageName) {
            "com.tencent.mm" -> {
                // WeChat: extras.getString("android.title") is sender name
                extras.getString("android.title")?.takeIf { it != "微信" }
            }
            "com.whatsapp" -> {
                extras.getString("android.title")
            }
            "org.telegram.messenger" -> {
                extras.getString("android.title")
            }
            "com.tencent.mobileqq" -> {
                extras.getString("android.title")
            }
            else -> extras.getString("android.title")
        }
    }

    private fun extractMessage(extras: android.os.Bundle): String? {
        return extras.getString("android.bigText")
            ?: extras.getString("android.text")
            ?: extras.getCharSequence("android.bigText")?.toString()
            ?: extras.getCharSequence("android.text")?.toString()
    }

    private fun isGroupNotification(extras: android.os.Bundle, packageName: String): Boolean {
        return when (packageName) {
            "com.tencent.mm" -> {
                // WeChat group chats have "(n)" pattern in title
                val title = extras.getString("android.title") ?: ""
                Regex("\\(\\d+\\)").containsMatchIn(title)
            }
            else -> false
        }
    }

    private fun extractGroupName(extras: android.os.Bundle, packageName: String): String? {
        return when (packageName) {
            "com.tencent.mm" -> {
                val title = extras.getString("android.title") ?: ""
                // Extract group name from "GroupName (n)" pattern
                Regex("^(.+)\\s*\\(\\d+\\)$").find(title)?.groupValues?.get(1)
            }
            else -> null
        }
    }
}
