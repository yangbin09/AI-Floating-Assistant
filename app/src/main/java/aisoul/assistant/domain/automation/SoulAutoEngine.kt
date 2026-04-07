package com.aisoul.assistant.domain.automation

import android.content.Context
import android.util.Log
import com.aisoul.assistant.accessibility.SoulAccessibilityService
import com.aisoul.assistant.data.local.AppDatabase
import com.aisoul.assistant.data.remote.ApiKeyManager
import com.aisoul.assistant.domain.strategy.ChatStageAnalyzer
import com.aisoul.assistant.domain.strategy.ReplyGenerator
import com.aisoul.assistant.model.ChatMessage
import com.aisoul.assistant.model.ChatStage
import com.aisoul.assistant.model.Persona
import com.aisoul.assistant.model.SoulUser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Soul 自动化引擎主控制器
 */
class SoulAutoEngine(
    private val context: Context,
    private val apiKeyManager: ApiKeyManager,
    private val database: AppDatabase
) {
    private val tag = "SoulAutoEngine"

    // 组件
    private val replyGenerator = ReplyGenerator(apiKeyManager)
    private val stageAnalyzer = ChatStageAnalyzer()
    private val autoSender = AutoSender()

    // 状态
    private val _engineState = MutableStateFlow(EngineState())
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    // 配置
    private var currentPersona: Persona = Persona.default()
    private var enabledFeatures = EnabledFeatures()

    // 协程作用域
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /**
     * 启动引擎
     */
    fun start() {
        _engineState.value = _engineState.value.copy(isRunning = true)
        Log.i(tag, "Soul 自动化引擎已启动")
    }

    /**
     * 停止引擎
     */
    fun stop() {
        _engineState.value = _engineState.value.copy(isRunning = false)
        Log.i(tag, "Soul 自动化引擎已停止")
    }

    /**
     * 更新配置
     */
    fun updateConfig(config: AutomationConfig) {
        autoSender.maxMessagesPerHour = config.maxMessagesPerHour
        autoSender.minIntervalSeconds = config.minIntervalSeconds
        autoSender.maxIntervalSeconds = config.maxIntervalSeconds
        autoSender.isEnabled = config.autoSendEnabled
        enabledFeatures = EnabledFeatures(
            autoFind = config.autoFind,
            autoOpener = config.autoOpener,
            autoReply = config.autoReply,
            autoSend = config.autoSend
        )
        currentPersona = Persona.getById(config.personaId) ?: Persona.default()
    }

    /**
     * 处理新消息（被通知触发）
     */
    suspend fun handleNewMessage(userId: String, message: String) {
        if (!_engineState.value.isRunning) return
        if (!enabledFeatures.autoReply) return

        Log.d(tag, "收到新消息 from $userId: $message")

        // 获取用户信息
        val user = getOrCreateUser(userId)

        // 分析聊天阶段
        val messages = getRecentMessages(userId)
        val stage = stageAnalyzer.analyzeByContent(messages.map {
            com.aisoul.assistant.accessibility.ChatBubble(it.content, it.isUser)
        })

        // 生成回复
        val contextStr = messages.takeLast(10).joinToString("\n") {
            if (it.isUser) "我: ${it.content}" else "对方: ${it.content}"
        }

        val result = replyGenerator.generateReplies(
            persona = currentPersona,
            targetUser = user,
            stage = stage,
            contextMessages = contextStr,
            latestMessage = message
        )

        result.onSuccess { replies ->
            Log.d(tag, "生成了 ${replies.size} 条回复建议")
            _engineState.value = _engineState.value.copy(
                suggestedReplies = replies,
                currentUserId = userId,
                currentStage = stage
            )

            // 如果启用自动发送
            if (autoSender.canSend() && enabledFeatures.autoSend) {
                autoSendBestReply(replies.first(), userId)
            }
        }.onFailure { error ->
            Log.e(tag, "生成回复失败: ${error.message}")
            _engineState.value = _engineState.value.copy(errorMessage = error.message)
        }
    }

    /**
     * 自动发送最佳回复
     */
    private suspend fun autoSendBestReply(reply: String, userId: String) {
        if (!autoSender.canSend()) {
            Log.d(tag, "发送条件不满足，跳过发送")
            return
        }

        // 等待随机延迟
        autoSender.waitRandomDelay()

        // 模拟人类打字延迟
        autoSender.randomHumanDelay()

        // 发送消息（通过 AccessibilityService）
        // 注意：这里需要通过服务发送，实际会在 FloatingService 中调用
        Log.i(tag, "自动发送: $reply")

        // 记录发送
        autoSender.recordSend()

        // 保存到数据库
        saveMessage(userId, reply, isUser = true)
    }

    /**
     * 生成开场白
     */
    suspend fun generateOpeners(user: SoulUser): Result<List<String>> {
        return replyGenerator.generateOpeners(currentPersona, user)
    }

    /**
     * 改写消息
     */
    suspend fun rewriteReply(
        original: String,
        style: com.aisoul.assistant.domain.strategy.RewriteStyle
    ): Result<String> {
        return replyGenerator.rewriteMessage(currentPersona, original, style)
    }

    /**
     * 获取或创建用户
     */
    private suspend fun getOrCreateUser(userId: String): SoulUser {
        // TODO: 从数据库或缓存获取
        return SoulUser(
            id = userId,
            name = "Soul用户",
            detectedAt = System.currentTimeMillis()
        )
    }

    /**
     * 获取最近消息
     */
    private suspend fun getRecentMessages(conversationId: String): List<ChatMessage> {
        return try {
            database.chatMessageDao()
                .getRecentMessagesForConversation(conversationId, 10)
                .map { entity ->
                    ChatMessage(
                        id = entity.id.toString(),
                        content = entity.content,
                        isUser = entity.isUser,
                        timestamp = entity.timestamp,
                        senderName = entity.senderName,
                        conversationId = entity.conversationId
                    )
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 保存消息到数据库
     */
    private suspend fun saveMessage(conversationId: String, content: String, isUser: Boolean) {
        val entity = com.aisoul.assistant.data.local.entity.ChatMessageEntity(
            content = content,
            isUser = isUser,
            senderName = "我",
            senderApp = "soul",
            conversationId = conversationId
        )
        database.chatMessageDao().insertMessage(entity)
    }

    /**
     * 清理资源
     */
    fun release() {
        scope.cancel()
        stop()
    }
}

/**
 * 引擎状态
 */
data class EngineState(
    val isRunning: Boolean = false,
    val currentUserId: String? = null,
    val currentStage: ChatStage = ChatStage.UNKNOWN,
    val suggestedReplies: List<String> = emptyList(),
    val errorMessage: String? = null,
    val sentCountToday: Int = 0
)

/**
 * 自动化配置
 */
data class AutomationConfig(
    val autoFind: Boolean = false,
    val autoOpener: Boolean = false,
    val autoReply: Boolean = true,
    val autoSend: Boolean = false,
    val autoSendEnabled: Boolean = true,
    val maxMessagesPerHour: Int = 10,
    val minIntervalSeconds: Int = 5,
    val maxIntervalSeconds: Int = 30,
    val personaId: String = "sincere_gentle",
    val blacklistedKeywords: List<String> = emptyList()
)

/**
 * 启用的功能
 */
data class EnabledFeatures(
    val autoFind: Boolean = false,    // 自动找人
    val autoOpener: Boolean = false,   // 自动开场
    val autoReply: Boolean = true,     // 自动回复
    val autoSend: Boolean = false      // 自动发送（需要用户确认）
)
