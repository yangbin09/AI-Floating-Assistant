package com.aisoul.assistant.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aisoul.assistant.data.local.AppDatabase
import com.aisoul.assistant.data.local.entity.ChatMessageEntity
import com.aisoul.assistant.data.remote.ApiKeyManager
import com.aisoul.assistant.data.remote.DeepSeekApiClient
import com.aisoul.assistant.data.remote.model.DeepSeekMessage
import com.aisoul.assistant.data.remote.model.DeepSeekRequest
import com.aisoul.assistant.model.ChatMessage
import com.aisoul.assistant.model.ConversationItem
import com.aisoul.assistant.model.PendingReply
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isAiTyping: Boolean = false,
    val conversations: List<ConversationItem> = emptyList(),
    val activeConversation: String? = null,
    val pendingReplies: List<PendingReply> = emptyList(),
    val isConnected: Boolean = false,
    val errorMessage: String? = null
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val apiKeyManager = ApiKeyManager(application)

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val messageList = mutableListOf<ChatMessage>()

    init {
        observeContacts()
        addWelcomeMessage()
    }

    private fun observeContacts() {
        viewModelScope.launch {
            database.contactDao().getAllContacts().collect { contacts ->
                val conversations = contacts.map { entity ->
                    ConversationItem(
                        conversationId = entity.conversationId,
                        senderName = entity.senderName,
                        appDisplayName = entity.appDisplayName,
                        lastMessage = entity.lastMessagePreview,
                        lastMessageTime = entity.lastMessageTime,
                        unreadCount = entity.unreadCount,
                        isAutoReplyEnabled = entity.isAutoReplyEnabled
                    )
                }
                _uiState.value = _uiState.value.copy(conversations = conversations)
            }
        }
    }

    fun selectConversation(conversationId: String) {
        _uiState.value = _uiState.value.copy(activeConversation = conversationId)

        viewModelScope.launch {
            // Load messages for this conversation
            val entities = database.chatMessageDao().getMessagesForConversation(conversationId).first()
            messageList.clear()
            messageList.addAll(entities.map { it.toChatMessage() })
            updateState()

            // Clear unread count
            database.contactDao().clearUnreadCount(conversationId)
        }
    }

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isAiTyping) return

        val activeConversation = _uiState.value.activeConversation

        // Add user message
        val userMessage = ChatMessage(
            content = text,
            isUser = true,
            conversationId = activeConversation
        )
        messageList.add(userMessage)
        _uiState.value = _uiState.value.copy(inputText = "", errorMessage = null)

        // If in a specific conversation, also save to DB
        if (activeConversation != null) {
            viewModelScope.launch {
                val conversation = _uiState.value.conversations.find { it.conversationId == activeConversation }
                val entity = ChatMessageEntity(
                    content = text,
                    isUser = true,
                    senderName = conversation?.senderName ?: "User",
                    senderApp = "",
                    conversationId = activeConversation
                )
                database.chatMessageDao().insertMessage(entity)
            }
        }

        // 调用真正的 DeepSeek API
        viewModelScope.launch {
            callDeepSeekApi(text)
        }
        updateState()
    }

    /**
     * 调用真正的 DeepSeek API
     */
    private suspend fun callDeepSeekApi(userMessage: String) {
        Log.d(TAG, "=== 开始调用 DeepSeek API ===")
        _uiState.value = _uiState.value.copy(isAiTyping = true)

        // 检查 API Key
        val apiKey = apiKeyManager.claudeApiKey
        if (apiKey.isNullOrBlank()) {
            Log.e(TAG, "API Key 未配置!")
            addSimulatedAiMessage("请先在设置中配置 DeepSeek API Key")
            _uiState.value = _uiState.value.copy(isAiTyping = false, errorMessage = "API Key 未配置")
            return
        }

        Log.d(TAG, "API Key: ${apiKey.take(8)}...")

        // 添加 "正在输入" 的 AI 消息
        val aiMessageId = UUID.randomUUID().toString()
        val aiMessage = ChatMessage(id = aiMessageId, content = "正在思考...", isUser = false)
        messageList.add(aiMessage)
        updateState()

        try {
            // 构建消息列表
            val messages = buildMessages(userMessage)
            Log.d(TAG, "发送消息数: ${messages.size}")
            messages.forEachIndexed { index, msg ->
                Log.d(TAG, "消息[$index]: role=${msg.role}, content=${msg.content.take(50)}...")
            }

            // 调用 API
            val client = DeepSeekApiClient(apiKey)
            val request = DeepSeekRequest(
                model = apiKeyManager.selectedModel,
                maxTokens = 500,
                messages = messages,
                temperature = 1.0
            )

            Log.d(TAG, "开始请求 DeepSeek API...")
            val result = withContext(Dispatchers.IO) {
                client.sendMessage(request)
            }

            // 处理响应
            result.fold(
                onSuccess = { response ->
                    Log.d(TAG, "API 调用成功!")
                    val reply = response.choices.firstOrNull()?.message?.content
                    Log.d(TAG, "AI 回复: $reply")

                    // 移除 "正在思考" 消息
                    messageList.removeIf { it.id == aiMessageId }

                    if (!reply.isNullOrBlank()) {
                        // 添加 AI 回复（带打字机效果）
                        addAiMessageWithTypingEffect(reply)
                    } else {
                        addSimulatedAiMessage("AI 未能生成有效回复")
                    }
                },
                onFailure = { error ->
                    Log.e(TAG, "API 调用失败: ${error.message}", error)
                    // 移除 "正在思考" 消息
                    messageList.removeIf { it.id == aiMessageId }
                    addSimulatedAiMessage("API 调用失败: ${error.message}")
                    _uiState.value = _uiState.value.copy(errorMessage = error.message)
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "异常: ${e.message}", e)
            messageList.removeIf { it.id == aiMessageId }
            addSimulatedAiMessage("发生错误: ${e.message}")
            _uiState.value = _uiState.value.copy(errorMessage = e.message)
        } finally {
            _uiState.value = _uiState.value.copy(isAiTyping = false)
        }
    }

    /**
     * 构建发送给 API 的消息列表
     */
    private fun buildMessages(newUserMessage: String): List<DeepSeekMessage> {
        val messages = mutableListOf<DeepSeekMessage>()

        // 系统提示
        val systemPrompt = """
你是一个友好、智能的AI助手，正在通过悬浮球应用与用户对话。

请遵循以下规则：
1. 回复要简短自然，通常1-3句话
2. 如果不知道如何回答，可以礼貌地说不太确定
3. 保持友好和乐于助人的态度
4. 使用与用户相同的语言回复
5. 不要编造不确定的事实信息
        """.trimIndent()
        messages.add(DeepSeekMessage("system", systemPrompt))

        // 最近的消息历史（最多5条）
        val recentMessages = messageList.takeLast(10)
        recentMessages.forEach { msg ->
            val role = if (msg.isUser) "user" else "assistant"
            messages.add(DeepSeekMessage(role, msg.content))
        }

        // 当前新消息
        messages.add(DeepSeekMessage("user", newUserMessage))

        return messages
    }

    /**
     * 带打字机效果的 AI 消息
     */
    private suspend fun addAiMessageWithTypingEffect(content: String) {
        val aiMessageId = UUID.randomUUID().toString()

        // 创建初始消息
        val aiMessage = ChatMessage(id = aiMessageId, content = "", isUser = false)
        messageList.add(aiMessage)
        updateState()

        // 打字机效果 - 正确累积文本
        var displayedContent = ""
        for (char in content) {
            displayedContent += char
            val partialMessage = aiMessage.copy(content = displayedContent)
            messageList.removeIf { it.id == aiMessageId }
            messageList.add(partialMessage)
            updateState()
            delay(30)
        }

        // 保存完整内容到数据库
        saveAiMessageToDatabase(content)
    }

    /**
     * 添加模拟的 AI 消息（用于错误情况）
     */
    private fun addSimulatedAiMessage(content: String) {
        val aiMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            content = content,
            isUser = false
        )
        messageList.add(aiMessage)
        updateState()
    }

    private fun saveAiMessageToDatabase(content: String) {
        val activeConversation = _uiState.value.activeConversation
        if (activeConversation != null) {
            viewModelScope.launch {
                val conversation = _uiState.value.conversations.find { it.conversationId == activeConversation }
                val entity = ChatMessageEntity(
                    content = content,
                    isUser = false,
                    senderName = conversation?.senderName ?: "AI",
                    senderApp = "",
                    conversationId = activeConversation
                )
                database.chatMessageDao().insertMessage(entity)
            }
        }
    }

    // User confirms a pending auto-reply
    fun confirmAutoReply(pendingReply: PendingReply) {
        viewModelScope.launch {
            // TODO: Actually send the message via appropriate service
            // Remove from pending
            _uiState.value = _uiState.value.copy(
                pendingReplies = _uiState.value.pendingReplies.filter { it.id != pendingReply.id }
            )
        }
    }

    // User dismisses a pending auto-reply
    fun dismissAutoReply(pendingReply: PendingReply) {
        _uiState.value = _uiState.value.copy(
            pendingReplies = _uiState.value.pendingReplies.filter { it.id != pendingReply.id }
        )
    }

    fun toggleAutoReplyForConversation(conversationId: String) {
        viewModelScope.launch {
            val contact = database.contactDao().getContact(conversationId) ?: return@launch
            database.contactDao().setAutoReplyEnabled(conversationId, !contact.isAutoReplyEnabled)
        }
    }

    private fun updateState() {
        _uiState.value = _uiState.value.copy(messages = messageList.toList())
    }

    private fun addWelcomeMessage() {
        val welcome = ChatMessage(
            content = "你好！我是你的 AI 助手。我可以帮你总结文章、翻译内容或回答问题。",
            isUser = false
        )
        messageList.add(welcome)
        updateState()
    }

    companion object {
        private const val TAG = "ChatViewModel"
    }
}

// Extension to convert from Entity
private fun ChatMessageEntity.toChatMessage(): ChatMessage {
    return ChatMessage(
        id = id.toString(),
        content = content,
        isUser = isUser,
        timestamp = timestamp,
        senderName = senderName,
        senderApp = senderApp,
        conversationId = conversationId,
        isAutoReplied = isAutoReplied
    )
}
