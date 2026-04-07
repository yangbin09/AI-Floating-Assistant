package com.example.myapplication.domain.strategy

import com.example.myapplication.model.ChatStage
import com.example.myapplication.model.Persona
import com.example.myapplication.model.SoulUser

/**
 * 人设 Prompt 构建器
 * 根据人设和场景生成对应的 Prompt
 */
class PersonaPromptBuilder {

    /**
     * 构建开场白生成的 Prompt
     */
    fun buildOpenerPrompt(
        persona: Persona,
        targetUser: SoulUser,
        stage: ChatStage = ChatStage.COLD_START
    ): String {
        val interestTags = targetUser.tags.take(3).joinToString("、")
        val signature = targetUser.signature.ifEmpty { "暂无签名" }

        return """
${persona.promptTemplate}

当前场景：
- 你在 Soul 上与 ${targetUser.name} 相遇
- 对方${if (targetUser.age != null) " ${targetUser.age}岁" else ""}${targetUser.gender.displayText()}
- 对方签名：$signature
- 对方兴趣标签：$interestTags

当前阶段：${stage.displayName()}

要求：
1. 生成 3 条不同风格的开场白
2. 开场白要自然、有趣，能够引起对方回复兴趣
3. 可以结合对方的兴趣标签找话题
4. 不要太油腻或太模板化
5. 每条控制在 20 字以内

格式：
开场白A（风格描述）：内容
开场白B（风格描述）：内容
开场白C（风格描述）：内容
        """.trimIndent()
    }

    /**
     * 构建续聊回复的 Prompt
     */
    fun buildReplyPrompt(
        persona: Persona,
        targetUser: SoulUser,
        stage: ChatStage,
        contextMessages: String,
        latestOpponentMessage: String
    ): String {
        val interestTags = targetUser.tags.take(3).joinToString("、")
        val signature = targetUser.signature.ifEmpty { "暂无签名" }

        return """
${persona.promptTemplate}

当前场景：
- 你正在与 ${targetUser.name} 聊天
- 对方${if (targetUser.age != null) " ${targetUser.age}岁" else ""}${targetUser.gender.displayText()}
- 对方签名：$signature
- 对方兴趣标签：$interestTags
- 当前聊天阶段：${stage.displayName()}（${stage.description()}）

聊天上下文：
$contextMessages

对方最新消息：
$latestOpponentMessage

要求：
1. 生成 3 条不同风格的回复建议
2. 回复要符合当前阶段的特点
3. 自然、简洁、有趣
4. 不要太油腻或太模板化
5. 每条控制在 25 字以内
6. 可以适当延续话题

格式：
回复A（稳重型）：内容
回复B（有趣型）：内容
回复C（推进型）：内容
        """.trimIndent()
    }

    /**
     * 构建改写 Prompt
     */
    fun buildRewritePrompt(
        persona: Persona,
        originalMessage: String,
        style: RewriteStyle
    ): String {
        return """
${persona.promptTemplate}

当前场景：
- 你需要将一条消息改写成指定风格

原文：
$originalMessage

目标风格：${style.displayName()}
风格说明：${style.description()}

要求：
1. 保持原意
2. 符合目标风格
3. 自然不生硬
4. 控制在 25 字以内

输出格式：
改写后：内容
        """.trimIndent()
    }

    /**
     * 分析对方消息的情感倾向
     */
    fun buildSentimentPrompt(message: String): String {
        return """
你是一个情感分析专家。请分析以下消息的情感倾向。

消息内容：
$message

请判断：
1. 情感是正面、负面还是中性？
2. 对方目前的状态是积极、消极还是一般？
3. 是否有明确的情感信号（如开心、难过、生气等）？

输出格式：
情感倾向：[正面/负面/中性]
状态：[积极/一般/消极]
信号：[如有请描述]
        """.trimIndent()
    }
}

enum class RewriteStyle {
    MORE_HUMOROUS,    // 更幽默
    MORE_GENTLE,      // 更温柔
    MORE_AGGRESSIVE,  // 更推进
    MORE_PLAYFUL,     // 更调皮
    MORE_SINCERE;     // 更真诚

    fun displayName(): String = when (this) {
        MORE_HUMOROUS -> "更幽默"
        MORE_GENTLE -> "更温柔"
        MORE_AGGRESSIVE -> "更推进"
        MORE_PLAYFUL -> "更调皮"
        MORE_SINCERE -> "更真诚"
    }

    fun description(): String = when (this) {
        MORE_HUMOROUS -> "加入幽默元素，让对话更轻松有趣"
        MORE_GENTLE -> "语气更柔和温暖，表达关心和体贴"
        MORE_AGGRESSIVE -> "更主动推进话题，引领对话方向"
        MORE_PLAYFUL -> "带点小撩小暧昧，增加趣味"
        MORE_SINCERE -> "更真诚走心，表达真实感受"
    }
}
