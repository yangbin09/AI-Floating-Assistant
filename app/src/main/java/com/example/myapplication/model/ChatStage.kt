package com.example.myapplication.model

/**
 * 聊天阶段枚举
 */
enum class ChatStage {
    /**
     * 冷启动 - 刚认识，还没开始聊
     */
    COLD_START,

    /**
     * 破冰 - 刚开始对话，需要建立基本连接
     */
    ICE_BREAK,

    /**
     * 初步互动 - 开始有来有往的对话
     */
    INITIAL_INTERACTION,

    /**
     * 建立熟悉感 - 双方开始熟悉，可以聊些日常
     */
    FAMILIARITY,

    /**
     * 深化关系 - 关系较深，可以聊更私密话题
     */
    DEEPENING,

    /**
     * 未知 - 无法判断阶段
     */
    UNKNOWN;

    fun displayName(): String = when (this) {
        COLD_START -> "冷启动"
        ICE_BREAK -> "破冰中"
        INITIAL_INTERACTION -> "初步互动"
        FAMILIARITY -> "熟悉中"
        DEEPENING -> "深入交流"
        UNKNOWN -> "未知"
    }

    /**
     * 获取阶段描述
     */
    fun description(): String = when (this) {
        COLD_START -> "刚刚认识，还没有开始对话"
        ICE_BREAK -> "刚开始聊天，需要建立基本信任"
        INITIAL_INTERACTION -> "已经破冰，开始初步了解"
        FAMILIARITY -> "双方已经熟悉，可以聊日常"
        DEEPENING -> "关系较深，可以聊更私密话题"
        UNKNOWN -> "无法判断当前阶段"
    }
}

/**
 * 聊天上下文
 */
data class ChatContext(
    val userId: String,
    val userName: String,
    val stage: ChatStage = ChatStage.UNKNOWN,
    val recentMessages: List<ChatMessage> = emptyList(),
    val lastMessageTime: Long = 0,
    val messageCount: Int = 0
)
