package com.aisoul.assistant.model

/**
 * Soul 用户模型
 */
data class SoulUser(
    val id: String,
    val name: String,
    val age: Int? = null,
    val gender: SoulUserGender = SoulUserGender.Unknown,
    val signature: String = "",
    val tags: List<String> = emptyList(),
    val avatarUrl: String? = null,
    val detectedAt: Long = System.currentTimeMillis(),
    val matchScore: Int = 0 // AI 评分
)

enum class SoulUserGender {
    Male,
    Female,
    Unknown;

    fun displayText(): String = when (this) {
        Male -> "♂"
        Female -> "♀"
        Unknown -> ""
    }
}
