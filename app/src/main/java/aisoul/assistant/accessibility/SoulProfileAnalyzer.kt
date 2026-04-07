package com.aisoul.assistant.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import com.aisoul.assistant.model.SoulUser
import com.aisoul.assistant.model.SoulUserGender

/**
 * Soul 个人主页解析器
 * 从个人主页提取用户详细信息
 */
class SoulProfileAnalyzer {

    /**
     * 解析个人主页
     */
    fun parseProfile(rootNode: AccessibilityNodeInfo): SoulUser? {
        val texts = getAllTexts(rootNode)
        val bounds = Rect()
        rootNode.getBoundsInScreen(bounds)

        var name = ""
        var age: Int? = null
        var gender: SoulUserGender? = null
        var signature = ""
        val tags = mutableListOf<String>()

        var inSignatureSection = false
        var inTagSection = false

        for (i in texts.indices) {
            val text = texts[i]

            // 解析名字（通常不带特殊字符）
            if (name.isEmpty() && text.length in 2..12 &&
                !text.contains("♂") && !text.contains("♀") &&
                !text.contains("岁") && !text.contains("签")
            ) {
                name = text
            }

            // 解析年龄和性别
            val ageGenderRegex = Regex("(\\d+)([♂♀])")
            ageGenderRegex.find(text)?.let { match ->
                age = match.groupValues[1].toIntOrNull()
                gender = when (match.groupValues[2]) {
                    "♂" -> SoulUserGender.Male
                    "♀" -> SoulUserGender.Female
                    else -> null
                }
            }

            // 解析签名区域
            if (text.contains("个签") || text.contains("签名")) {
                inSignatureSection = true
                inTagSection = false
                continue
            }
            if (inSignatureSection && text.isNotEmpty() && !text.contains("相册") && !text.contains("星球")) {
                val curlyQuote = '\u201D'
                signature = text.replace(curlyQuote.toString(), "").trim()
                inSignatureSection = false
            }

            // 解析标签（个人资料中的标签如"巨蟹座"、"170cm"等）
            val tagIndicators = listOf("座", "cm", "院校", "公司", "家乡", "星座", "血型")
            if (tagIndicators.any { text.contains(it) } && text.length < 20) {
                tags.add(text.trim())
            }

            // 解析兴趣爱好标签（通常用 · 分隔）
            if (text.contains("·") || text.contains("兴趣")) {
                inTagSection = true
            }
            if (inTagSection && text.contains("·")) {
                val potentialTags = text.split("·").map { it.trim() }
                    .filter { it.length in 1..15 }
                tags.addAll(potentialTags)
            }

            // 解析相册图片
            if (text.contains("相册") || text.contains("照片")) {
                // 统计图片数量
                val photoCount = texts.count { it.contains("张") || it.contains("照片") }
                if (photoCount > 0) {
                    // 占位，后续可以从节点属性获取
                }
            }
        }

        // 如果没有找到名字，生成一个
        val userId = name.ifEmpty {
            "${bounds.left}_${bounds.top}_${System.currentTimeMillis()}"
        }.let { "${it}_profile" }

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

    /**
     * 提取兴趣标签（用于生成开场白）
     */
    fun extractInterestTags(user: SoulUser): List<String> {
        val interestKeywords = listOf(
            "音乐", "电影", "旅行", "美食", "读书", "运动", "游戏",
            "动漫", "摄影", "绘画", "健身", "跑步", "游泳", "篮球",
            "足球", "网球", "瑜伽", "冥想", "咖啡", "茶", "烘焙",
            "穿搭", "美妆", "追星", "演唱会", "话剧", "展览"
        )

        return user.tags.filter { tag ->
            interestKeywords.any { kw -> tag.contains(kw) }
        }
    }

    private fun getAllTexts(node: AccessibilityNodeInfo): List<String> {
        val texts = mutableListOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(node)

        while (queue.isNotEmpty()) {
            val n = queue.removeFirst()
            n.text?.let { texts.add(it.toString()) }
            n.contentDescription?.let { texts.add(it.toString()) }
            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { queue.add(it) }
            }
        }

        return texts
    }
}
