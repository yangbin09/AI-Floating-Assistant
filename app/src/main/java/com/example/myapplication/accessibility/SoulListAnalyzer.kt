package com.example.myapplication.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.example.myapplication.model.SoulUser
import com.example.myapplication.model.SoulUserGender

/**
 * Soul 匹配列表解析器
 * 从匹配列表页面提取用户信息
 */
class SoulListAnalyzer {

    /**
     * 解析匹配列表，提取用户信息
     */
    fun parseMatchList(rootNode: AccessibilityNodeInfo): List<SoulUser> {
        val users = mutableListOf<SoulUser>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            // 检查是否是用户卡片节点
            if (isUserCard(node)) {
                val user = extractUserFromCard(node)
                if (user != null) {
                    users.add(user)
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        return users.distinctBy { it.id }
    }

    private fun isUserCard(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        // 用户卡片通常宽度较大，高度适中
        val widthRatio = bounds.width().toFloat() / 1080f
        val heightRatio = bounds.height().toFloat() / 1920f

        // 宽度占屏幕 70% 以上，高度 10-25%
        return widthRatio > 0.7 && heightRatio in 0.08f..0.30f
    }

    private fun extractUserFromCard(node: AccessibilityNodeInfo): SoulUser? {
        val texts = getNodeTexts(node)
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        // 解析用户信息
        var name = ""
        var age: Int? = null
        var gender: SoulUserGender? = null
        var signature = ""
        val tags = mutableListOf<String>()

        for (text in texts) {
            // 解析名字（通常第一个或较长的文本）
            if (name.isEmpty() && text.length in 2..12 && !text.contains("♂") && !text.contains("♀")) {
                name = text
            }

            // 解析年龄和性别（如 "22♂" 或 "22♀"）
            val ageGenderRegex = Regex("(\\d+)([♂♀])")
            ageGenderRegex.find(text)?.let { match ->
                age = match.groupValues[1].toIntOrNull()
                gender = when (match.groupValues[2]) {
                    "♂" -> SoulUserGender.Male
                    "♀" -> SoulUserGender.Female
                    else -> null
                }
            }

            // 解析签名
            val curlyQuote = '\u201D'
            if (text.contains("个签") || text.startsWith(curlyQuote.toString())) {
                signature = text.replace("个签：", "").replace(curlyQuote.toString(), "").trim()
            }

            // 解析标签（通常用 · 分隔）
            if (text.contains("·") && !text.contains("♂") && !text.contains("♀")) {
                val potentialTags = text.split("·").map { it.trim() }
                    .filter { it.length in 1..10 }
                tags.addAll(potentialTags)
            }
        }

        // 如果没有找到名字，尝试从 bounds 生成一个 ID
        val userId = if (name.isNotEmpty()) {
            name.hashCode().toString() + "_${bounds.left}_${bounds.top}"
        } else {
            "${bounds.left}_${bounds.top}_${System.currentTimeMillis()}"
        }

        return SoulUser(
            id = userId,
            name = name.ifEmpty { "Soul用户" },
            age = age,
            gender = gender ?: SoulUserGender.Unknown,
            signature = signature,
            tags = tags.distinct(),
            avatarUrl = null,
            detectedAt = System.currentTimeMillis()
        )
    }

    private fun getNodeTexts(node: AccessibilityNodeInfo): List<String> {
        val texts = mutableListOf<String>()

        fun traverse(n: AccessibilityNodeInfo) {
            n.text?.let { texts.add(it.toString()) }
            n.contentDescription?.let { texts.add(it.toString()) }
            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { traverse(it) }
            }
        }

        traverse(node)
        return texts
    }

    /**
     * 计算用户"好聊程度"评分
     */
    fun calculateMatchScore(user: SoulUser): Int {
        var score = 50 // 基础分

        // 有签名 +15
        if (user.signature.isNotEmpty()) score += 15

        // 有标签，每个 +5，最高 +20
        score += (user.tags.size * 5).coerceAtMost(20)

        // 有年龄信息 +10
        if (user.age != null) score += 10

        // 标签中包含兴趣爱好相关关键词 +15
        val interestKeywords = listOf("音乐", "电影", "旅行", "美食", "读书", "运动", "游戏", "动漫")
        if (user.tags.any { tag -> interestKeywords.any { kw -> tag.contains(kw) } }) {
            score += 15
        }

        return score.coerceIn(0, 100)
    }
}
