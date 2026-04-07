package com.example.myapplication.domain.strategy

import com.example.myapplication.data.remote.ApiKeyManager
import com.example.myapplication.data.remote.DeepSeekApiClient
import com.example.myapplication.data.remote.model.DeepSeekMessage
import com.example.myapplication.data.remote.model.DeepSeekRequest
import com.example.myapplication.model.Persona
import com.example.myapplication.model.SoulUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 回复生成器
 * 负责调用 AI 生成各种回复内容
 */
class ReplyGenerator(
    private val apiKeyManager: ApiKeyManager
) {
    private val promptBuilder = PersonaPromptBuilder()
    private val stageAnalyzer = ChatStageAnalyzer()

    /**
     * 生成开场白列表
     */
    suspend fun generateOpeners(
        persona: Persona,
        targetUser: SoulUser
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyManager.claudeApiKey
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(Exception("API Key 未配置"))
        }

        val prompt = promptBuilder.buildOpenerPrompt(persona, targetUser)

        val client = DeepSeekApiClient(apiKey)
        val request = DeepSeekRequest(
            model = apiKeyManager.selectedModel,
            maxTokens = 300,
            messages = listOf(DeepSeekMessage("user", prompt)),
            temperature = 0.9
        )

        client.sendMessage(request).map { response ->
            val content = response.choices.firstOrNull()?.message?.content ?: ""
            parseOpeners(content)
        }
    }

    /**
     * 生成回复建议列表
     */
    suspend fun generateReplies(
        persona: Persona,
        targetUser: SoulUser,
        stage: com.example.myapplication.model.ChatStage,
        contextMessages: String,
        latestMessage: String
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyManager.claudeApiKey
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(Exception("API Key 未配置"))
        }

        val prompt = promptBuilder.buildReplyPrompt(
            persona, targetUser, stage, contextMessages, latestMessage
        )

        val client = DeepSeekApiClient(apiKey)
        val request = DeepSeekRequest(
            model = apiKeyManager.selectedModel,
            maxTokens = 400,
            messages = listOf(DeepSeekMessage("user", prompt)),
            temperature = 0.9
        )

        client.sendMessage(request).map { response ->
            val content = response.choices.firstOrNull()?.message?.content ?: ""
            parseReplies(content)
        }
    }

    /**
     * 改写消息
     */
    suspend fun rewriteMessage(
        persona: Persona,
        originalMessage: String,
        style: RewriteStyle
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyManager.claudeApiKey
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(Exception("API Key 未配置"))
        }

        val prompt = promptBuilder.buildRewritePrompt(persona, originalMessage, style)

        val client = DeepSeekApiClient(apiKey)
        val request = DeepSeekRequest(
            model = apiKeyManager.selectedModel,
            maxTokens = 100,
            messages = listOf(DeepSeekMessage("user", prompt)),
            temperature = 0.8
        )

        client.sendMessage(request).map { response ->
            val content = response.choices.firstOrNull()?.message?.content ?: ""
            parseRewrittenMessage(content)
        }
    }

    /**
     * 分析对方情感
     */
    suspend fun analyzeSentiment(message: String): Result<SentimentResult> =
        withContext(Dispatchers.IO) {
            val apiKey = apiKeyManager.claudeApiKey
            if (apiKey.isNullOrBlank()) {
                return@withContext Result.failure(Exception("API Key 未配置"))
            }

            val prompt = promptBuilder.buildSentimentPrompt(message)

            val client = DeepSeekApiClient(apiKey)
            val request = DeepSeekRequest(
                model = apiKeyManager.selectedModel,
                maxTokens = 150,
                messages = listOf(DeepSeekMessage("user", prompt)),
                temperature = 0.3
            )

            client.sendMessage(request).map { response ->
                val content = response.choices.firstOrNull()?.message?.content ?: ""
                parseSentiment(content)
            }
        }

    /**
     * 解析开场白列表
     */
    private fun parseOpeners(content: String): List<String> {
        val lines = content.split("\n").filter { it.isNotBlank() }
        val openers = mutableListOf<String>()

        for (line in lines) {
            // 匹配 "开场白A：内容" 或 "A. 内容" 格式
            val regex = Regex("""[:：]\s*(.+)""")
            regex.find(line)?.let { match ->
                val opener = match.groupValues[1].trim()
                    .replace(Regex("""^[A-C][.、:：]\s*"""), "")
                    .replace(Regex("""^[（(][^)]+[)）]\s*"""), "")
                if (opener.isNotEmpty() && opener.length <= 30) {
                    openers.add(opener)
                }
            }
        }

        // 如果解析失败，返回原始内容清理后的结果
        if (openers.isEmpty()) {
            return content.lines()
                .map { it.replace(Regex("^[A-C][.、:：]"), "").trim() }
                .filter { it.length in 2..30 }
                .take(3)
        }

        return openers.take(3)
    }

    /**
     * 解析回复列表
     */
    private fun parseReplies(content: String): List<String> {
        val lines = content.split("\n").filter { it.isNotBlank() }
        val replies = mutableListOf<String>()

        for (line in lines) {
            val regex = Regex("""[:：]\s*(.+)""")
            regex.find(line)?.let { match ->
                val reply = match.groupValues[1].trim()
                    .replace(Regex("""^[A-C][.、:：]\s*"""), "")
                    .replace(Regex("""^[（(][^)]+[)）]\s*"""), "")
                if (reply.isNotEmpty() && reply.length <= 40) {
                    replies.add(reply)
                }
            }
        }

        if (replies.isEmpty()) {
            return content.lines()
                .map { it.replace(Regex("^[A-C][.、:：]"), "").trim() }
                .filter { it.length in 2..40 }
                .take(3)
        }

        return replies.take(3)
    }

    /**
     * 解析改写后的消息
     */
    private fun parseRewrittenMessage(content: String): String {
        val regex = Regex("""改写后[:：]\s*(.+)""")
        regex.find(content)?.let { match ->
            return match.groupValues[1].trim()
        }

        // 尝试直接返回清理后的内容
        return content.lines()
            .firstOrNull { it.isNotBlank() }
            ?.replace(Regex("""^[A-C][.、:：]\s*"""), "")
            ?.trim() ?: ""
    }

    /**
     * 解析情感分析结果
     */
    private fun parseSentiment(content: String): SentimentResult {
        val sentimentRegex = Regex("""情感倾向[：:]\s*(\S+)""")
        val statusRegex = Regex("""状态[：:]\s*(\S+)""")
        val signalRegex = Regex("""信号[：:]\s*(.+)""")

        return SentimentResult(
            sentiment = sentimentRegex.find(content)?.groupValues?.get(1) ?: "中性",
            status = statusRegex.find(content)?.groupValues?.get(1) ?: "一般",
            signal = signalRegex.find(content)?.groupValues?.get(1)
        )
    }
}

data class SentimentResult(
    val sentiment: String,  // 正面/负面/中性
    val status: String,    // 积极/一般/消极
    val signal: String?    // 情感信号描述
)
