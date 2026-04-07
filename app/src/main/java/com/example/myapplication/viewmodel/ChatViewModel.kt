package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.entity.ChatMessageEntity
import com.example.myapplication.data.remote.ApiKeyManager
import com.example.myapplication.domain.AutoReplyMode
import com.example.myapplication.domain.AutoReplyResult
import com.example.myapplication.domain.AutoReplyManager
import com.example.myapplication.model.ChatMessage
import com.example.myapplication.model.ConversationItem
import com.example.myapplication.model.PendingReply
import com.example.myapplication.service.notification.ChatNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isAiTyping: Boolean = false,
    val conversations: List<ConversationItem> = emptyList(),
    val activeConversation: String? = null,
    val pendingReplies: List<PendingReply> = emptyList(),
    val isConnected: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val apiKeyManager = ApiKeyManager(application)
    private lateinit var autoReplyManager: AutoReplyManager

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
        _uiState.value = _uiState.value.copy(inputText = "")

        // If in a specific conversation, also save to DB
        if (activeConversation != null) {
            viewModelScope.launch {
                val conversation = _uiState.value.conversations.find { it.conversationId == activeConversation }
                val entity = ChatMessageEntity(
                    content = text,
                    isUser = true,
                    senderName = conversation?.senderName,
                    senderApp = null,
                    conversationId = activeConversation
                )
                database.chatMessageDao().insertMessage(entity)
            }
        }

        viewModelScope.launch {
            simulateAiResponse()
        }
        updateState()
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

    private suspend fun simulateAiResponse() {
        _uiState.value = _uiState.value.copy(isAiTyping = true)

        val aiMessageId = UUID.randomUUID().toString()
        val aiMessage = ChatMessage(id = aiMessageId, content = "...", isUser = false)
        messageList.add(aiMessage)
        updateState()

        delay(1000)

        val fullResponse = getAiResponse(messageList.filterIsInstance<ChatMessage>().last { it.isUser }.content)
        messageList.removeIf { it.id == aiMessageId }

        var currentContent = ""
        for (char in fullResponse) {
            currentContent += char
            updateState()
            delay(30)
        }

        _uiState.value = _uiState.value.copy(isAiTyping = false)
    }

    private fun getAiResponse(userMessage: String): String {
        return when {
            userMessage.contains("你好", ignoreCase = true) ->
                "你好！有什么我可以帮助你的吗？"
            userMessage.contains("帮助", ignoreCase = true) ->
                "我可以帮你：\n1. 总结文章内容\n2. 翻译文本\n3. 回答问题\n4. 提供建议\n\n请告诉我你需要什么帮助！"
            userMessage.contains("总结", ignoreCase = true) ->
                "请发送你想要总结的文章或文本，我会为你提取关键信息。"
            userMessage.contains("翻译", ignoreCase = true) ->
                "请发送你想要翻译的文本，并告诉我目标语言（如：英文、中文、日文等）。"
            else ->
                "这是一个模拟的 AI 回复。在这个演示中，我们还没有接入真实的后端 API。"
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
