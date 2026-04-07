package com.aisoul.assistant.domain.automation

import android.util.Log
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * 自动发送控制器
 * 负责控制发送频率、随机延迟等
 */
class AutoSender {
    private val tag = "AutoSender"

    // 配置
    var maxMessagesPerHour: Int = 10
    var minIntervalSeconds: Int = 5
    var maxIntervalSeconds: Int = 30
    var isEnabled: Boolean = true

    // 状态
    private val sentMessages = mutableListOf<Long>()
    private var lastSendTime: Long = 0

    /**
     * 检查是否可以发送
     */
    fun canSend(): Boolean {
        if (!isEnabled) {
            Log.d(tag, "发送功能未启用")
            return false
        }

        // 检查每小时发送上限
        val currentTime = System.currentTimeMillis()
        val oneHourAgo = currentTime - 3600000

        // 清理旧记录
        sentMessages.removeAll { it < oneHourAgo }

        if (sentMessages.size >= maxMessagesPerHour) {
            Log.d(tag, "已达到每小时发送上限: $maxMessagesPerHour")
            return false
        }

        // 检查最小间隔
        val timeSinceLastSend = currentTime - lastSendTime
        if (timeSinceLastSend < minIntervalSeconds * 1000) {
            Log.d(tag, "距离上次发送时间太短: ${timeSinceLastSend}ms")
            return false
        }

        return true
    }

    /**
     * 等待随机延迟
     */
    suspend fun waitRandomDelay() {
        val delaySeconds = Random.nextInt(minIntervalSeconds, maxIntervalSeconds + 1)
        Log.d(tag, "等待 ${delaySeconds} 秒后发送...")
        delay(delaySeconds * 1000L)
    }

    /**
     * 记录发送
     */
    fun recordSend() {
        lastSendTime = System.currentTimeMillis()
        sentMessages.add(lastSendTime)
        Log.d(tag, "发送记录已添加，今日已发送: ${sentMessages.size}")
    }

    /**
     * 获取剩余可发送数量
     */
    fun getRemainingQuota(): Int {
        val currentTime = System.currentTimeMillis()
        val oneHourAgo = currentTime - 3600000
        sentMessages.removeAll { it < oneHourAgo }
        return maxMessagesPerHour - sentMessages.size
    }

    /**
     * 重置计数器
     */
    fun reset() {
        sentMessages.clear()
        lastSendTime = 0
    }

    /**
     * 生成随机化延迟（模拟人工操作）
     */
    suspend fun randomHumanDelay() {
        // 模拟打字时间：每字 0.3-0.8 秒
        // 通常一条消息 5-20 字
        val charDelay = Random.nextLong(300, 800)
        val baseDelay = charDelay * Random.nextInt(5, 20)
        delay(baseDelay)
    }
}
