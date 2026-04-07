package com.aisoul.assistant.domain.strategy

import com.aisoul.assistant.accessibility.ChatBubble
import com.aisoul.assistant.model.ChatStage
import com.aisoul.assistant.model.ChatContext

/**
 * 聊天阶段分析器
 * 根据聊天消息判断当前处于哪个阶段
 */
class ChatStageAnalyzer {

    /**
     * 分析聊天阶段
     */
    fun analyzeStage(context: ChatContext): ChatStage {
        val messageCount = context.messageCount
        val recentMessages = context.recentMessages.takeLast(10)

        // 根据消息数量初步判断
        return when {
            messageCount == 0 -> ChatStage.COLD_START
            messageCount <= 2 -> ChatStage.ICE_BREAK
            messageCount <= 5 -> ChatStage.INITIAL_INTERACTION
            messageCount <= 15 -> ChatStage.FAMILIARITY
            else -> ChatStage.DEEPENING
        }
    }

    /**
     * 根据消息内容深度分析阶段
     */
    fun analyzeByContent(messages: List<ChatBubble>): ChatStage {
        if (messages.isEmpty()) return ChatStage.COLD_START

        val userMessages = messages.filter { it.isFromMe }
        val opponentMessages = messages.filter { !it.isFromMe }

        // 检查是否有破冰信号
        val hasGreeting = messages.any { msg ->
            msg.content.contains(Regex("(你好|嗨|哈喽|hey|hi|在吗|在嘛|你好呀|hi~)"))
        }

        // 检查是否有问答互动
        val hasQuestion = messages.any { msg ->
            msg.content.contains(Regex("(吗|呢|吧|呀|么|什么|怎么|为什么|哪里|谁|多少)"))
        }

        // 检查是否有分享个人信息
        val hasPersonalShare = messages.any { msg ->
            msg.content.contains(Regex("(我(的)?|喜欢|讨厌|觉得|认为|工作|学习|生活|住|在|是)"))
        }

        // 检查是否有情感表达
        val hasEmotional = messages.any { msg ->
            msg.content.contains(Regex("(哈哈|呵呵|好开心|好难过|喜欢|爱你|想你|么么哒)"))
        }

        // 综合判断
        return when {
            messages.size <= 2 -> {
                if (hasGreeting) ChatStage.ICE_BREAK else ChatStage.COLD_START
            }
            messages.size <= 5 -> {
                when {
                    hasQuestion && hasPersonalShare -> ChatStage.INITIAL_INTERACTION
                    hasGreeting -> ChatStage.ICE_BREAK
                    else -> ChatStage.INITIAL_INTERACTION
                }
            }
            messages.size <= 15 -> {
                when {
                    hasEmotional -> ChatStage.FAMILIARITY
                    hasPersonalShare && hasQuestion -> ChatStage.FAMILIARITY
                    else -> ChatStage.INITIAL_INTERACTION
                }
            }
            else -> ChatStage.DEEPENING
        }
    }

    /**
     * 根据阶段获取合适的回复策略提示
     */
    fun getStageHint(stage: ChatStage): String = when (stage) {
        ChatStage.COLD_START -> "对方还没有开始聊天，建议发送轻松的开场白"
        ChatStage.ICE_BREAK -> "破冰阶段，建议用轻松友好的方式继续对话"
        ChatStage.INITIAL_INTERACTION -> "已经开始初步互动，可以更自然地聊天"
        ChatStage.FAMILIARITY -> "双方已经熟悉，可以聊日常和分享感受"
        ChatStage.DEEPENING -> "关系较深，可以聊更深入的话题"
        ChatStage.UNKNOWN -> "无法判断阶段，建议正常聊天"
    }

    /**
     * 判断是否需要转换阶段
     */
    fun shouldUpgradeStage(currentStage: ChatStage, newMessage: String): Boolean {
        // 检查是否有推进关系的信号
        val upgradeSignals = listOf(
            "喜欢你", "想你", "爱你", "么么哒", "亲爱的", "宝贝",
            "我们", "一起", "约", "见面", "下次", "下次聊",
            "真开心", "真好", "太好了", "哈哈", "呵呵", "笑死"
        )

        return upgradeSignals.any { signal -> newMessage.contains(signal) }
    }
}
