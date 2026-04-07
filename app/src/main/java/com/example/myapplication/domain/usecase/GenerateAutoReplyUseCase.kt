package com.example.myapplication.domain.usecase

import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.entity.ConversationContextEntity
import com.example.myapplication.data.remote.ApiKeyManager
import com.example.myapplication.data.remote.ClaudeApiClient
import com.example.myapplication.data.remote.model.ClaudeMessage
import com.example.myapplication.data.remote.model.ClaudeRequest
import com.google.gson.Gson

class GenerateAutoReplyUseCase(
    private val database: AppDatabase,
    private val apiKeyManager: ApiKeyManager
) {
    private val gson = Gson()
    private val maxContextMessages = 10

    suspend fun generateReply(
        conversationId: String,
        senderName: String,
        appDisplayName: String,
        newMessage: String
    ): Result<String> {
        // Check if API key is available
        val apiKey = apiKeyManager.claudeApiKey
        if (apiKey.isNullOrBlank()) {
            return Result.failure(Exception("API key not configured"))
        }

        // Build conversation context
        val contextMessages = buildContextMessages(conversationId, senderName, appDisplayName, newMessage)

        // Call Claude API
        val client = ClaudeApiClient(apiKey)
        val request = ClaudeRequest(
            model = apiKeyManager.selectedModel,
            max_tokens = 500,
            messages = contextMessages
        )

        return client.sendMessage(request).map { response ->
            response.content.firstOrNull()?.text ?: "好的"
        }
    }

    private suspend fun buildContextMessages(
        conversationId: String,
        senderName: String,
        appDisplayName: String,
        newMessage: String
    ): List<ClaudeMessage> {
        // Get recent messages from database
        val recentMessages = database.chatMessageDao()
            .getRecentMessages(conversationId, maxContextMessages)
            .reversed()

        // Build system prompt
        val systemPrompt = buildSystemPrompt(senderName, appDisplayName)

        val messages = mutableListOf<ClaudeMessage>()

        // Add system prompt
        messages.add(ClaudeMessage("system", systemPrompt))

        // Add conversation history
        recentMessages.forEach { entity ->
            val role = if (entity.isUser) "user" else "assistant"
            messages.add(ClaudeMessage(role, entity.content))
        }

        // Add current message
        messages.add(ClaudeMessage("user", newMessage))

        // Update context in database
        updateConversationContext(conversationId, messages)

        return messages
    }

    private fun buildSystemPrompt(senderName: String, appDisplayName: String): String {
        return """
你是${senderName}的AI助手，正在通过${appDisplayName}与${senderName}对话。

请遵循以下规则：
1. 回复要简短自然，通常1-3句话
2. 如果不知道如何回答，可以礼貌地说不太确定
3. 保持友好和乐于助人的态度
4. 使用与${senderName}相同的语言回复
5. 不要编造不确定的事实信息
6. 如果是敏感话题，建议他们寻求专业帮助
        """.trimIndent()
    }

    private suspend fun updateConversationContext(
        conversationId: String,
        messages: List<ClaudeMessage>
    ) {
        // Store last N messages as JSON for future context
        val recentMessages = messages.takeLast(maxContextMessages + 1) // +1 for current
        val contextEntity = ConversationContextEntity(
            conversationId = conversationId,
            recentMessagesJson = gson.toJson(recentMessages),
            lastUpdated = System.currentTimeMillis(),
            messageCount = recentMessages.size
        )
        database.conversationContextDao().insertOrUpdateContext(contextEntity)
    }
}
