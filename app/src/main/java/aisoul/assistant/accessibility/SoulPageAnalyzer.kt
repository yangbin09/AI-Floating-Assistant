package com.aisoul.assistant.accessibility

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Soul 页面检测器
 * 根据界面元素判断当前所在页面
 */
class SoulPageAnalyzer {

    // 假设屏幕宽度为 1080px（实际应从 DisplayMetrics 获取）
    private val screenWidth = 1080f

    fun detectPage(rootNode: AccessibilityNodeInfo): SoulPage {
        // 检测匹配列表页
        if (isMatchListPage(rootNode)) {
            return SoulPage.MatchList
        }

        // 检测聊天页面
        if (isChatPage(rootNode)) {
            return SoulPage.Chat
        }

        // 检测个人主页
        if (isProfilePage(rootNode)) {
            return SoulPage.Profile
        }

        // 检测聊天列表页
        if (isChatListPage(rootNode)) {
            return SoulPage.ChatList
        }

        // 检测星球页面
        if (isStarPage(rootNode)) {
            return SoulPage.StarList
        }

        return SoulPage.Unknown
    }

    private fun isMatchListPage(rootNode: AccessibilityNodeInfo): Boolean {
        // 匹配列表页特征：通常有"寻聊"或"匹配"相关的标签
        val matchIndicators = listOf(
            "寻聊", "匹配", "soul", "星球"
        )

        // 检查是否有匹配相关的按钮或标签
        val allText = getAllText(rootNode)
        return matchIndicators.any { indicator ->
            allText.any { text -> text.contains(indicator) }
        } && hasListItems(rootNode)
    }

    private fun isChatPage(rootNode: AccessibilityNodeInfo): Boolean {
        // 聊天页面特征：输入框 + 发送按钮 + 聊天气泡
        val hasInputBox = rootNode.findAccessibilityNodeInfosByViewId("cn.soulapp:id/input").isNotEmpty() ||
                rootNode.findAccessibilityNodeInfosByViewId("cn.soulapp:id/edit_text").isNotEmpty()
        val hasSendButton = rootNode.findAccessibilityNodeInfosByText("发送").isNotEmpty() ||
                rootNode.findAccessibilityNodeInfosByViewId("cn.soulapp:id/send").isNotEmpty()
        val hasChatBubbles = hasChatBubbles(rootNode)

        return (hasInputBox || hasSendButton) && hasChatBubbles
    }

    private fun isProfilePage(rootNode: AccessibilityNodeInfo): Boolean {
        // 个人主页特征：通常有"相册"、"瞬间"、"签名"等标签
        val profileIndicators = listOf(
            "相册", "瞬间", "签名", "个人资料", "编辑资料"
        )

        val allText = getAllText(rootNode)
        return profileIndicators.any { indicator ->
            allText.any { text -> text.contains(indicator) }
        }
    }

    private fun isChatListPage(rootNode: AccessibilityNodeInfo): Boolean {
        // 聊天列表页特征：多个对话项，有用户头像和最后消息
        val hasMultipleItems = countListItems(rootNode) > 1
        val hasChatIndicators = getAllText(rootNode).any { text ->
            text.contains("消息") || text.contains("聊天")
        }

        return hasMultipleItems && hasChatIndicators
    }

    private fun isStarPage(rootNode: AccessibilityNodeInfo): Boolean {
        val starIndicators = listOf("星球", "广场", "推荐", "发现")
        val allText = getAllText(rootNode)
        return starIndicators.any { indicator ->
            allText.any { text -> text.contains(indicator) }
        } && hasListItems(rootNode)
    }

    private fun hasListItems(rootNode: AccessibilityNodeInfo): Boolean {
        return countListItems(rootNode) > 0
    }

    private fun countListItems(rootNode: AccessibilityNodeInfo): Int {
        // 统计可能的列表项数量
        var count = 0
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                if (isLikelyListItem(child)) {
                    count++
                }
                queue.add(child)
            }
        }

        return count
    }

    private fun isLikelyListItem(node: AccessibilityNodeInfo): Boolean {
        // 列表项通常有特定的尺寸比例
        val bounds = android.graphics.Rect()
        node.getBoundsInScreen(bounds)

        // 宽度接近屏幕宽度，高度适中（40-150dp，假设3x密度）
        val widthRatio = bounds.width().toFloat() / screenWidth
        val heightInDp = bounds.height().toFloat() / 3f

        return widthRatio > 0.7f && heightInDp in 40f..150f
    }

    private fun hasChatBubbles(rootNode: AccessibilityNodeInfo): Boolean {
        // 检测是否有聊天气泡（左右交替的消息样式）
        var bubbleCount = 0
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            // 检查是否是消息气泡（通常有特定的背景或尺寸）
            val className = node.className?.toString() ?: ""
            if (className.contains("RelativeLayout") || className.contains("LinearLayout")) {
                val bounds = android.graphics.Rect()
                node.getBoundsInScreen(bounds)

                // 气泡通常有一定的宽度比例
                val widthRatio = bounds.width().toFloat() / screenWidth
                if (widthRatio in 0.4f..0.85f) {
                    bubbleCount++
                    if (bubbleCount >= 2) return true
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        return bubbleCount >= 2
    }

    private fun getAllText(rootNode: AccessibilityNodeInfo): List<String> {
        val texts = mutableListOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            node.text?.let { texts.add(it.toString()) }
            node.contentDescription?.let { texts.add(it.toString()) }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        return texts
    }
}
