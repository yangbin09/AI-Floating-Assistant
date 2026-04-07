package com.aisoul.assistant.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Soul 聊天窗口解析器
 * 从聊天页面提取消息内容
 */
class SoulChatAnalyzer {

    /**
     * 解析聊天消息
     */
    fun parseChatMessages(rootNode: AccessibilityNodeInfo): List<ChatBubble> {
        val messages = mutableListOf<ChatBubble>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(rootNode)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            if (isMessageBubble(node)) {
                val bubble = extractMessageBubble(node)
                if (bubble != null) {
                    messages.add(bubble)
                }
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }

        return messages.sortedBy { it.timestamp }
    }

    private fun isMessageBubble(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        // 消息气泡的特征：宽度适中，高度较小
        val widthRatio = bounds.width().toFloat() / 1080f
        val heightRatio = bounds.height().toFloat() / 1920f

        // 气泡宽度 40-85%，高度小于 15%
        return widthRatio in 0.35f..0.85f && heightRatio < 0.15f
    }

    private fun extractMessageBubble(node: AccessibilityNodeInfo): ChatBubble? {
        val texts = getNodeTexts(node)
        val content = texts.joinToString(" ").trim()

        if (content.isEmpty()) return null

        // 判断是自己发的还是对方发的
        val isFromMe = isMyMessage(node)

        return ChatBubble(
            content = content,
            isFromMe = isFromMe,
            timestamp = System.currentTimeMillis() // 实际应该从节点获取
        )
    }

    private fun isMyMessage(node: AccessibilityNodeInfo): Boolean {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        // 自己发的消息通常靠右，对方发的靠左
        // 屏幕宽度假设 1080
        return bounds.left > 540
    }

    private fun getNodeTexts(node: AccessibilityNodeInfo): List<String> {
        val texts = mutableListOf<String>()

        fun traverse(n: AccessibilityNodeInfo) {
            n.text?.let { text ->
                val str = text.toString().trim()
                if (str.isNotEmpty() && str.length < 500) {
                    texts.add(str)
                }
            }
            for (i in 0 until n.childCount) {
                n.getChild(i)?.let { traverse(it) }
            }
        }

        traverse(node)
        return texts
    }

    /**
     * 获取对方最新一条消息
     */
    fun getLatestOpponentMessage(messages: List<ChatBubble>): String? {
        return messages
            .filter { !it.isFromMe }
            .maxByOrNull { it.timestamp }
            ?.content
    }

    /**
     * 获取聊天上下文（最近 N 条消息）
     */
    fun getChatContext(messages: List<ChatBubble>, limit: Int = 10): String {
        return messages
            .takeLast(limit)
            .joinToString("\n") { bubble ->
                if (bubble.isFromMe) "我: ${bubble.content}"
                else "对方: ${bubble.content}"
            }
    }
}
