package com.example.myapplication.domain.usecase

import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.entity.ConversationContextEntity
import com.example.myapplication.data.remote.ApiKeyManager
import com.example.myapplication.data.remote.DeepSeekApiClient
import com.example.myapplication.data.remote.model.DeepSeekMessage
import com.example.myapplication.data.remote.model.DeepSeekRequest
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

        // Call DeepSeek API
        val client = DeepSeekApiClient(apiKey)
        val request = DeepSeekRequest(
            model = apiKeyManager.selectedModel,
            maxTokens = 500,
            messages = contextMessages,
            temperature = 1.0
        )

        return client.sendMessage(request).map { response ->
            response.choices.firstOrNull()?.message?.content ?: "好的"
        }
    }

    private suspend fun buildContextMessages(
        conversationId: String,
        senderName: String,
        appDisplayName: String,
        newMessage: String
    ): List<DeepSeekMessage> {
        // Get recent messages from database
        val recentMessages = database.chatMessageDao()
            .getRecentMessagesForConversation(conversationId, maxContextMessages)
            .reversed()

        // Build system prompt
        val systemPrompt = buildSystemPrompt(senderName, appDisplayName)

        val messages = mutableListOf<DeepSeekMessage>()

        // Add system prompt
        messages.add(DeepSeekMessage("system", systemPrompt))

        // Add conversation history
        recentMessages.forEach { entity ->
            val role = if (entity.isUser) "user" else "assistant"
            messages.add(DeepSeekMessage(role, entity.content))
        }

        // Add current message
        messages.add(DeepSeekMessage("user", newMessage))

        // Update context in database
        updateConversationContext(conversationId, messages)

        return messages
    }

    private fun buildSystemPrompt(senderName: String, appDisplayName: String): String {
        return """
你是${appDisplayName}上的${senderName}的AI助手，正在与${senderName}对话。

请遵循以下规则：
1. 回复要简短自然，通常1-3句话
2. 如果不知道如何回答，可以礼貌地说不太确定
3. 保持友好和乐于助人的态度
4. 使用与对方相同的语言回复
5. 不要编造不确定的事实信息
6. 如果是敏感话题，建议他们寻求专业帮助
7. 你的角色是${appDisplayName}上的聊天助手，要表现得自然
        """.trimIndent()
    }

    private suspend fun updateConversationContext(
        conversationId: String,
        messages: List<DeepSeekMessage>
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
