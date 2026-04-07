package com.aisoul.assistant.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aisoul.assistant.accessibility.SoulAccessibilityService
import com.aisoul.assistant.accessibility.SoulListAnalyzer
import com.aisoul.assistant.data.local.AppDatabase
import com.aisoul.assistant.data.remote.ApiKeyManager
import com.aisoul.assistant.domain.automation.SoulAutoEngine
import com.aisoul.assistant.domain.automation.AutomationConfig
import com.aisoul.assistant.domain.automation.EnabledFeatures
import com.aisoul.assistant.model.Persona
import com.aisoul.assistant.model.SoulUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Soul 匹配页 ViewModel
 */
class SoulMatchViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val apiKeyManager = ApiKeyManager(application)
    private val listAnalyzer = SoulListAnalyzer()
    private val autoEngine = SoulAutoEngine(application, apiKeyManager, database)

    private val _uiState = MutableStateFlow(SoulMatchUiState())
    val uiState: StateFlow<SoulMatchUiState> = _uiState.asStateFlow()

    private val _detectedUsers = MutableStateFlow<List<SoulUser>>(emptyList())
    val detectedUsers: StateFlow<List<SoulUser>> = _detectedUsers.asStateFlow()

    private val _openersMap = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val openersMap: StateFlow<Map<String, List<String>>> = _openersMap.asStateFlow()

    init {
        loadSavedUsers()
    }

    /**
     * 加载已保存的用户列表
     */
    private fun loadSavedUsers() {
        viewModelScope.launch {
            // TODO: 从数据库加载之前匹配过的用户
        }
    }

    /**
     * 刷新用户列表（触发无障碍服务扫描）
     */
    fun refreshUserList() {
        _uiState.value = _uiState.value.copy(isScanning = true)
        // 实际扫描由 AccessibilityService 执行
        // 这里只是更新 UI 状态
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000) // 模拟扫描时间
            _uiState.value = _uiState.value.copy(isScanning = false)
        }
    }

    /**
     * 更新检测到的用户
     */
    fun updateDetectedUsers(users: List<SoulUser>) {
        // 计算每个用户的匹配分数
        val scoredUsers = users.map { user ->
            user.copy(matchScore = listAnalyzer.calculateMatchScore(user))
        }.sortedByDescending { it.matchScore }

        _detectedUsers.value = scoredUsers
        _uiState.value = _uiState.value.copy(
            users = scoredUsers,
            isScanning = false
        )
    }

    /**
     * 生成开场白
     */
    fun generateOpeners(user: SoulUser) {
        _uiState.value = _uiState.value.copy(generatingOpenersFor = user.id)

        viewModelScope.launch {
            val result = autoEngine.generateOpeners(user)
            result.onSuccess { openers ->
                val newMap = _openersMap.value.toMutableMap()
                newMap[user.id] = openers
                _openersMap.value = newMap
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(errorMessage = error.message)
            }
            _uiState.value = _uiState.value.copy(generatingOpenersFor = null)
        }
    }

    /**
     * 选择用户
     */
    fun selectUser(user: SoulUser) {
        _uiState.value = _uiState.value.copy(selectedUser = user)

        // 如果没有开场白，自动生成
        if (!_openersMap.value.containsKey(user.id)) {
            generateOpeners(user)
        }
    }

    /**
     * 清除错误
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * 获取用户开场白
     */
    fun getOpenersForUser(userId: String): List<String> {
        return _openersMap.value[userId] ?: emptyList()
    }
}

/**
 * Soul 匹配页 UI 状态
 */
data class SoulMatchUiState(
    val users: List<SoulUser> = emptyList(),
    val selectedUser: SoulUser? = null,
    val isScanning: Boolean = false,
    val generatingOpenersFor: String? = null,
    val errorMessage: String? = null
)
