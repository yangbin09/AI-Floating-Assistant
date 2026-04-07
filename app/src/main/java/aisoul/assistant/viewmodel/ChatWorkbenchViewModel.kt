package com.aisoul.assistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aisoul.assistant.data.local.AppDatabase
import com.aisoul.assistant.data.remote.ApiKeyManager
import com.aisoul.assistant.domain.automation.SoulAutoEngine
import com.aisoul.assistant.domain.strategy.RewriteStyle
import com.aisoul.assistant.domain.strategy.ReplyGenerator
import com.aisoul.assistant.model.ChatMessage
import com.aisoul.assistant.model.ChatStage
import com.aisoul.assistant.model.Persona
import com.aisoul.assistant.model.SoulUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 聊天工作台 ViewModel
 */
class ChatWorkbenchViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val apiKeyManager = ApiKeyManager(application)
    private val replyGenerator = ReplyGenerator(apiKeyManager)
    private val autoEngine = SoulAutoEngine(application, apiKeyManager, database)

    private val _uiState = MutableStateFlow(ChatWorkbenchUiState())
    val uiState: StateFlow<ChatWorkbenchUiState> = _uiState.asStateFlow()

    private val _suggestedReplies = MutableStateFlow<List<String>>(emptyList())
    val suggestedReplies: StateFlow<List<String>> = _suggestedReplies.asStateFlow()

    init {
        loadPersonas()
    }

    private fun loadPersonas() {
        _uiState.value = _uiState.value.copy(
            availablePersonas = Persona.PRESETS,
            selectedPersona = Persona.default()
        )
    }

    /**
     * 设置当前聊天用户
     */
    fun setCurrentUser(user: SoulUser) {
        _uiState.value = _uiState.value.copy(
            currentUser = user,
            chatStage = ChatStage.UNKNOWN,
            suggestedReplies = emptyList()
        )
    }

    /**
     * 加载聊天历史
     */
    fun loadChatHistory(conversationId: String) {
        viewModelScope.launch {
            val entities = database.chatMessageDao()
                .getRecentMessagesForConversation(conversationId, 20)

            val messages = entities.map { entity ->
                ChatMessage(
                    id = entity.id.toString(),
                    content = entity.content,
                    isUser = entity.isUser,
                    timestamp = entity.timestamp,
                    senderName = entity.senderName,
                    conversationId = entity.conversationId
                )
            }

            _uiState.value = _uiState.value.copy(
                chatHistory = messages,
                conversationId = conversationId
            )
        }
    }

    /**
     * 添加新消息
     */
    fun addMessage(content: String, isFromMe: Boolean) {
        val newMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            content = content,
            isUser = isFromMe,
            timestamp = System.currentTimeMillis()
        )

        val newHistory = _uiState.value.chatHistory + newMessage
        _uiState.value = _uiState.value.copy(chatHistory = newHistory)
    }

    /**
     * 生成回复建议
     */
    fun generateReplies(opponentMessage: String) {
        val user = _uiState.value.currentUser ?: return
        val persona = _uiState.value.selectedPersona

        _uiState.value = _uiState.value.copy(isGenerating = true)

        viewModelScope.launch {
            val contextStr = _uiState.value.chatHistory.takeLast(10).joinToString("\n") {
                if (it.isUser) "我: ${it.content}" else "对方: ${it.content}"
            }

            val result = replyGenerator.generateReplies(
                persona = persona,
                targetUser = user,
                stage = _uiState.value.chatStage,
                contextMessages = contextStr,
                latestMessage = opponentMessage
            )

            result.onSuccess { replies ->
                _suggestedReplies.value = replies
                _uiState.value = _uiState.value.copy(
                    suggestedReplies = replies,
                    isGenerating = false
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    errorMessage = error.message
                )
            }
        }
    }

    /**
     * 改写回复
     */
    fun rewriteReply(original: String, style: RewriteStyle) {
        val persona = _uiState.value.selectedPersona

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRewriting = true)

            val result = replyGenerator.rewriteMessage(persona, original, style)

            result.onSuccess { rewritten ->
                val currentReplies = _uiState.value.suggestedReplies.toMutableList()
                val index = currentReplies.indexOf(original)
                if (index >= 0) {
                    currentReplies[index] = rewritten
                    _uiState.value = _uiState.value.copy(suggestedReplies = currentReplies)
                }
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message)
            }

            _uiState.value = _uiState.value.copy(isRewriting = false)
        }
    }

    /**
     * 切换人设
     */
    fun switchPersona(persona: Persona) {
        _uiState.value = _uiState.value.copy(selectedPersona = persona)
        // 如果有建议回复，重新生成
        if (_suggestedReplies.value.isNotEmpty()) {
            val lastOpponentMsg = _uiState.value.chatHistory.lastOrNull { !it.isUser }?.content
            if (lastOpponentMsg != null) {
                generateReplies(lastOpponentMsg)
            }
        }
    }

    /**
     * 选择一个回复
     */
    fun selectReply(reply: String) {
        _uiState.value = _uiState.value.copy(selectedReply = reply)
    }

    /**
     * 发送选中的回复
     */
    fun sendSelectedReply() {
        val reply = _uiState.value.selectedReply ?: return
        addMessage(reply, isFromMe = true)
        _uiState.value = _uiState.value.copy(selectedReply = null)
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}

/**
 * 聊天工作台 UI 状态
 */
data class ChatWorkbenchUiState(
    val currentUser: SoulUser? = null,
    val conversationId: String? = null,
    val chatHistory: List<ChatMessage> = emptyList(),
    val chatStage: ChatStage = ChatStage.UNKNOWN,
    val suggestedReplies: List<String> = emptyList(),
    val selectedReply: String? = null,
    val selectedPersona: Persona = Persona.default(),
    val availablePersonas: List<Persona> = emptyList(),
    val isGenerating: Boolean = false,
    val isRewriting: Boolean = false,
    val errorMessage: String? = null
)
