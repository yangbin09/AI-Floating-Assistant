package com.example.myapplication.model

/**
 * 自动化设置
 */
data class AutomationSettings(
    // 全局开关
    val isEnabled: Boolean = false,

    // 功能开关
    val autoFindEnabled: Boolean = false,    // 自动找人
    val autoOpenerEnabled: Boolean = false,  // 自动开场白
    val autoReplyEnabled: Boolean = true,    // 自动回复建议
    val autoSendEnabled: Boolean = false,    // 自动发送（半自动模式）

    // 频率控制
    val maxMessagesPerHour: Int = 10,
    val minIntervalSeconds: Int = 5,
    val maxIntervalSeconds: Int = 30,

    // 人设
    val selectedPersonaId: String = "sincere_gentle",

    // 黑名单关键词
    val blacklistedKeywords: List<String> = emptyList(),

    // 监控的 App
    val monitoredApps: List<String> = listOf("soul")
) {
    fun getSelectedPersona(): Persona {
        return Persona.getById(selectedPersonaId) ?: Persona.default()
    }
}

/**
 * 预设的自动化场景
 */
enum class AutomationScenario(
    val displayName: String,
    val description: String,
    val settings: AutomationSettings
) {
    CONSERVATIVE(
        displayName = "保守模式",
        description = "低频率，优先确认后发送，适合新手",
        settings = AutomationSettings(
            maxMessagesPerHour = 5,
            minIntervalSeconds = 10,
            maxIntervalSeconds = 60,
            autoSendEnabled = false
        )
    ),
    BALANCED(
        displayName = "平衡模式",
        description = "中等频率，半自动发送",
        settings = AutomationSettings(
            maxMessagesPerHour = 10,
            minIntervalSeconds = 5,
            maxIntervalSeconds = 30,
            autoSendEnabled = false
        )
    ),
    AGGRESSIVE(
        displayName = "激进模式",
        description = "较高频率，自动发送，适合老手",
        settings = AutomationSettings(
            maxMessagesPerHour = 20,
            minIntervalSeconds = 3,
            maxIntervalSeconds = 15,
            autoSendEnabled = true
        )
    )
}
