package com.aisoul.assistant.viewmodel

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aisoul.assistant.data.local.AppDatabase
import com.aisoul.assistant.data.local.entity.ChatMessageEntity
import com.aisoul.assistant.data.remote.ApiKeyManager
import com.aisoul.assistant.data.remote.DeepSeekApiClient
import com.aisoul.assistant.data.remote.model.DeepSeekMessage
import com.aisoul.assistant.data.remote.model.DeepSeekRequest
import com.aisoul.assistant.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ServiceStatus(
    val isFloatingBallEnabled: Boolean = false,
    val isNotificationListenerEnabled: Boolean = false,
    val isAiConfigured: Boolean = false,
    val isAutoReplyEnabled: Boolean = false,
    val monitoredApps: List<String> = emptyList()
)

data class DashboardUiState(
    val serviceStatus: ServiceStatus = ServiceStatus(),
    val apiKeyMasked: String = "",
    val selectedModel: String = "deepseek-chat",
    val isAiExpanded: Boolean = false,
    val isAutoReplyExpanded: Boolean = false,
    val recentMessages: List<ChatMessage> = emptyList(),
    val isTestingAi: Boolean = false,
    val testResult: String? = null,
    val isFloatingServiceRunning: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val apiKeyManager = ApiKeyManager(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        refreshState()
        loadRecentMessages()
    }

    fun refreshState() {
        val context = getApplication<Application>()
        val hasApiKey = apiKeyManager.hasApiKey()
        val maskedKey = if (hasApiKey) {
            val key = apiKeyManager.claudeApiKey ?: ""
            if (key.length > 8) "****${key.takeLast(8)}" else "****"
        } else ""

        _uiState.value = _uiState.value.copy(
            serviceStatus = ServiceStatus(
                isFloatingBallEnabled = false, // Will be updated by service
                isNotificationListenerEnabled = false, // Need to check system
                isAiConfigured = hasApiKey,
                isAutoReplyEnabled = false,
                monitoredApps = listOf("微信", "QQ", "钉钉")
            ),
            apiKeyMasked = maskedKey,
            selectedModel = apiKeyManager.selectedModel
        )
    }

    private fun loadRecentMessages() {
        viewModelScope.launch {
            database.chatMessageDao().getRecentMessages(limit = 10).collect { entities ->
                val messages = entities.map { it.toChatMessage() }
                _uiState.value = _uiState.value.copy(recentMessages = messages)
            }
        }
    }

    fun testAiConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingAi = true, testResult = null)

            val apiKey = apiKeyManager.claudeApiKey
            if (apiKey.isNullOrBlank()) {
                _uiState.value = _uiState.value.copy(
                    isTestingAi = false,
                    testResult = "❌ API Key 未配置"
                )
                return@launch
            }

            try {
                val client = DeepSeekApiClient(apiKey)
                val request = DeepSeekRequest(
                    model = apiKeyManager.selectedModel,
                    maxTokens = 50,
                    messages = listOf(
                        DeepSeekMessage("user", "你好，请回复'测试成功'")
                    )
                )

                val result = withContext(Dispatchers.IO) {
                    client.sendMessage(request)
                }

                result.fold(
                    onSuccess = { response ->
                        val reply = response.choices.firstOrNull()?.message?.content ?: "无回复"
                        _uiState.value = _uiState.value.copy(
                            isTestingAi = false,
                            testResult = "✅ $reply"
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            isTestingAi = false,
                            testResult = "❌ ${error.message}"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTestingAi = false,
                    testResult = "❌ ${e.message}"
                )
            }
        }
    }

    fun toggleAiExpanded() {
        _uiState.value = _uiState.value.copy(
            isAiExpanded = !_uiState.value.isAiExpanded
        )
    }

    fun toggleAutoReplyExpanded() {
        _uiState.value = _uiState.value.copy(
            isAutoReplyExpanded = !_uiState.value.isAutoReplyExpanded
        )
    }

    fun setFloatingServiceRunning(running: Boolean) {
        _uiState.value = _uiState.value.copy(isFloatingServiceRunning = running)
    }

    fun updateNotificationListenerStatus(isEnabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            serviceStatus = _uiState.value.serviceStatus.copy(isNotificationListenerEnabled = isEnabled)
        )
    }

    fun updateApiKey(apiKey: String) {
        apiKeyManager.claudeApiKey = apiKey
        refreshState()
    }

    fun updateModel(model: String) {
        apiKeyManager.selectedModel = model
        _uiState.value = _uiState.value.copy(selectedModel = model)
    }

    fun toggleAutoReply() {
        val current = _uiState.value.serviceStatus.isAutoReplyEnabled
        _uiState.value = _uiState.value.copy(
            serviceStatus = _uiState.value.serviceStatus.copy(isAutoReplyEnabled = !current)
        )
    }

    private fun ChatMessageEntity.toChatMessage(): ChatMessage {
        return ChatMessage(
            id = id.toString(),
            content = content,
            isUser = isUser,
            timestamp = timestamp,
            senderName = senderName,
            senderApp = senderApp,
            conversationId = conversationId
        )
    }

    companion object {
        val SUPPORTED_MODELS = listOf(
            "deepseek-chat" to "DeepSeek Chat",
            "deepseek-coder" to "DeepSeek Coder"
        )
    }
}
