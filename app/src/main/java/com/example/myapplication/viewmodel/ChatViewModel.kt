package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.model.ChatMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isAiTyping: Boolean = false
)

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val messageList = mutableListOf<ChatMessage>()

    init {
        addWelcomeMessage()
    }

    private fun addWelcomeMessage() {
        val welcome = ChatMessage(
            content = "你好！我是你的 AI 助手。我可以帮你总结文章、翻译内容或回答问题。",
            isUser = false
        )
        messageList.add(welcome)
        updateState()
    }

    fun updateInputText(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        if (text.isBlank() || _uiState.value.isAiTyping) return

        // Add user message
        val userMessage = ChatMessage(content = text, isUser = true)
        messageList.add(userMessage)
        _uiState.value = _uiState.value.copy(inputText = "")

        // Simulate AI response
        viewModelScope.launch {
            simulateAiResponse()
        }
        updateState()
    }

    private suspend fun simulateAiResponse() {
        _uiState.value = _uiState.value.copy(isAiTyping = true)

        // Add placeholder for AI thinking
        val aiMessageId = UUID.randomUUID().toString()
        val aiMessage = ChatMessage(id = aiMessageId, content = "...", isUser = false)
        messageList.add(aiMessage)
        updateState()

        delay(1000) // Simulate network delay

        // Get actual response
        val fullResponse = getAiResponse(messageList.filterIsInstance<ChatMessage>().last { it.isUser }.content)
        messageList.removeIf { it.id == aiMessageId }

        // Add real AI response with streaming effect
        var currentContent = ""
        for (char in fullResponse) {
            currentContent += char
            val index = messageList.indexOfFirst { it.id == aiMessageId }
            if (index != -1) {
                messageList[index] = messageList[index].copy(content = currentContent)
            } else {
                messageList.add(ChatMessage(id = aiMessageId, content = currentContent, isUser = false))
            }
            updateState()
            delay(30) // Typewriter effect speed
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
                "这是一个模拟的 AI 回复。在这个演示中，我们还没有接入真实的后端 API，但我已经准备好为你服务了！你可以尝试集成 OpenAI 或其他 LLM API 来获得真实体验。"
        }
    }

    private fun updateState() {
        _uiState.value = _uiState.value.copy(messages = messageList.toList())
    }
}
