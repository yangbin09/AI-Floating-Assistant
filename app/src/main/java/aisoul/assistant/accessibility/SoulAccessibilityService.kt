package com.aisoul.assistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.pm.PackageManager
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.aisoul.assistant.model.SoulUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Soul 无障碍服务
 * 用于自动读取 Soul 界面并模拟用户操作
 */
class SoulAccessibilityService : AccessibilityService() {

    private val _currentPage = MutableStateFlow<SoulPage>(SoulPage.Unknown)
    val currentPage: StateFlow<SoulPage> = _currentPage.asStateFlow()

    private val _detectedUsers = MutableStateFlow<List<SoulUser>>(emptyList())
    val detectedUsers: StateFlow<List<SoulUser>> = _detectedUsers.asStateFlow()

    private val _currentChatMessages = MutableStateFlow<List<ChatBubble>>(emptyList())
    val currentChatMessages: StateFlow<List<ChatBubble>> = _currentChatMessages.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

    private val pageAnalyzer = SoulPageAnalyzer()
    private val listAnalyzer = SoulListAnalyzer()
    private val chatAnalyzer = SoulChatAnalyzer()
    private val profileAnalyzer = SoulProfileAnalyzer()

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isServiceRunning.value = true

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 100
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val packageName = event.packageName?.toString() ?: return
        if (packageName != SOUL_PACKAGE_NAME) return

        val source = event.source ?: return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val page = pageAnalyzer.detectPage(source)
                _currentPage.value = page
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                handlePageContent(source)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                handleClick(event)
            }
        }

        // Deprecated in API 29+, node recycling is now automatic
        @Suppress("DEPRECATION")
        source.recycle()
    }

    private fun handlePageContent(source: AccessibilityNodeInfo) {
        when (_currentPage.value) {
            is SoulPage.MatchList -> {
                val users = listAnalyzer.parseMatchList(source)
                _detectedUsers.value = users
            }
            is SoulPage.Chat -> {
                val messages = chatAnalyzer.parseChatMessages(source)
                _currentChatMessages.value = messages
            }
            is SoulPage.Profile -> {
                val user = profileAnalyzer.parseProfile(source)
                user?.let { _detectedUsers.value = listOf(it) }
            }
            else -> {}
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleClick(event: AccessibilityEvent) {
        // 处理点击事件，用于日志记录
    }

    /**
     * 点击指定节点
     */
    fun clickNode(node: AccessibilityNodeInfo) {
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    /**
     * 点击指定文本的节点
     */
    fun clickByText(text: String): Boolean {
        val rootNode = rootInActiveWindow ?: return false
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        if (nodes.isNotEmpty()) {
            val clicked = nodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
            for (node in nodes) {
                @Suppress("DEPRECATION")
                node.recycle()
            }
            @Suppress("DEPRECATION")
            rootNode.recycle()
            return clicked
        }
        @Suppress("DEPRECATION")
        rootNode.recycle()
        return false
    }

    /**
     * 滚动列表（向上滚动）
     */
    fun scrollUp() {
        val rootNode = rootInActiveWindow ?: return
        rootNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
        @Suppress("DEPRECATION")
        rootNode.recycle()
    }

    /**
     * 滚动列表（向下滚动）
     */
    fun scrollDown() {
        val rootNode = rootInActiveWindow ?: return
        rootNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
        @Suppress("DEPRECATION")
        rootNode.recycle()
    }

    /**
     * 输入文本到焦点输入框
     */
    fun inputText(text: String) {
        val rootNode = rootInActiveWindow ?: return
        val inputFields = rootNode.findAccessibilityNodeInfosByViewId("$SOUL_PACKAGE_NAME:id/input")
        if (inputFields.isNotEmpty()) {
            val arguments = android.os.Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            inputFields[0].performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
        @Suppress("DEPRECATION")
        rootNode.recycle()
    }

    /**
     * 点击发送按钮
     */
    fun clickSend() {
        clickByText("发送")
    }

    /**
     * 判断 Soul 是否安装并可访问
     */
    fun isSoulInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo(SOUL_PACKAGE_NAME, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override fun onInterrupt() {
        _isServiceRunning.value = false
    }

    override fun onDestroy() {
        super.onDestroy()
        _isServiceRunning.value = false
    }

    companion object {
        const val SOUL_PACKAGE_NAME = "cn.soulapp"
    }
}

/**
 * Soul 页面类型
 */
sealed class SoulPage {
    object Unknown : SoulPage()
    object Splash : SoulPage()
    object Main : SoulPage()
    object MatchList : SoulPage()      // 匹配列表/寻聊列表
    object Chat : SoulPage()           // 聊天窗口
    object Profile : SoulPage()        // 个人主页
    object StarList : SoulPage()       // 星球列表
    object ChatList : SoulPage()       // 聊天列表
}

/**
 * 聊天消息气泡
 */
data class ChatBubble(
    val content: String,
    val isFromMe: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
