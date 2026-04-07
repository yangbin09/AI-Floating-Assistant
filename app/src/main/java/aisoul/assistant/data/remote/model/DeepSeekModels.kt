package com.aisoul.assistant.data.remote.model

import com.google.gson.annotations.SerializedName

// DeepSeek API Request/Response models

data class DeepSeekMessage(
    val role: String,        // "system", "user", "assistant"
    val content: String
)

data class DeepSeekRequest(
    val model: String = "deepseek-chat",
    @SerializedName("max_tokens")
    val maxTokens: Int = 1024,
    val messages: List<DeepSeekMessage>,
    val temperature: Double = 1.0
)

data class DeepSeekResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<DeepSeekChoice>,
    val usage: DeepSeekUsage?
)

data class DeepSeekChoice(
    val index: Int,
    val message: DeepSeekMessage,
    @SerializedName("finish_reason")
    val finishReason: String?
)

data class DeepSeekUsage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int?,
    @SerializedName("completion_tokens")
    val completionTokens: Int?,
    @SerializedName("total_tokens")
    val totalTokens: Int?
)
