package com.example.myapplication.data.remote.model

import com.google.gson.annotations.SerializedName

// Claude API Request/Response models

data class ClaudeMessage(
    val role: String,        // "user" or "assistant"
    val content: String
)

data class ClaudeRequest(
    val model: String = "claude-sonnet-4-20250514",
    @SerializedName("max_tokens")
    val maxTokens: Int = 1024,
    val messages: List<ClaudeMessage>
)

data class ClaudeResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeContentBlock>,
    val model: String,
    @SerializedName("stop_reason")
    val stopReason: String?
)

data class ClaudeContentBlock(
    val type: String,
    val text: String?
)
