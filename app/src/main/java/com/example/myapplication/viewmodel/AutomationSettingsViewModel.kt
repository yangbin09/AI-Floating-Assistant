package com.example.myapplication.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.myapplication.model.AutomationScenario
import com.example.myapplication.model.AutomationSettings
import com.example.myapplication.model.Persona
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 自动化设置 ViewModel
 */
class AutomationSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("automation_settings", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(AutomationSettingsUiState())
    val uiState: StateFlow<AutomationSettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        val settings = AutomationSettings(
            isEnabled = prefs.getBoolean("is_enabled", false),
            autoFindEnabled = prefs.getBoolean("auto_find", false),
            autoOpenerEnabled = prefs.getBoolean("auto_opener", false),
            autoReplyEnabled = prefs.getBoolean("auto_reply", true),
            autoSendEnabled = prefs.getBoolean("auto_send", false),
            maxMessagesPerHour = prefs.getInt("max_per_hour", 10),
            minIntervalSeconds = prefs.getInt("min_interval", 5),
            maxIntervalSeconds = prefs.getInt("max_interval", 30),
            selectedPersonaId = prefs.getString("persona_id", "sincere_gentle") ?: "sincere_gentle",
            blacklistedKeywords = prefs.getStringSet("blacklist", emptySet())?.toList() ?: emptyList()
        )

        _uiState.value = AutomationSettingsUiState(
            settings = settings,
            availablePersonas = Persona.PRESETS,
            availableScenarios = AutomationScenario.entries
        )
    }

    fun saveSettings() {
        val settings = _uiState.value.settings

        prefs.edit().apply {
            putBoolean("is_enabled", settings.isEnabled)
            putBoolean("auto_find", settings.autoFindEnabled)
            putBoolean("auto_opener", settings.autoOpenerEnabled)
            putBoolean("auto_reply", settings.autoReplyEnabled)
            putBoolean("auto_send", settings.autoSendEnabled)
            putInt("max_per_hour", settings.maxMessagesPerHour)
            putInt("min_interval", settings.minIntervalSeconds)
            putInt("max_interval", settings.maxIntervalSeconds)
            putString("persona_id", settings.selectedPersonaId)
            putStringSet("blacklist", settings.blacklistedKeywords.toSet())
            apply()
        }
    }

    fun updateEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(isEnabled = enabled)
        )
        saveSettings()
    }

    fun updateAutoFind(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(autoFindEnabled = enabled)
        )
        saveSettings()
    }

    fun updateAutoOpener(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(autoOpenerEnabled = enabled)
        )
        saveSettings()
    }

    fun updateAutoReply(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(autoReplyEnabled = enabled)
        )
        saveSettings()
    }

    fun updateAutoSend(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(autoSendEnabled = enabled)
        )
        saveSettings()
    }

    fun updateMaxMessagesPerHour(value: Int) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(maxMessagesPerHour = value)
        )
        saveSettings()
    }

    fun updateInterval(min: Int, max: Int) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(
                minIntervalSeconds = min,
                maxIntervalSeconds = max
            )
        )
        saveSettings()
    }

    fun updatePersona(personaId: String) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(selectedPersonaId = personaId)
        )
        saveSettings()
    }

    fun addBlacklistKeyword(keyword: String) {
        if (keyword.isBlank()) return
        val current = _uiState.value.settings.blacklistedKeywords
        if (!current.contains(keyword)) {
            _uiState.value = _uiState.value.copy(
                settings = _uiState.value.settings.copy(
                    blacklistedKeywords = current + keyword
                )
            )
            saveSettings()
        }
    }

    fun removeBlacklistKeyword(keyword: String) {
        val current = _uiState.value.settings.blacklistedKeywords
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(
                blacklistedKeywords = current - keyword
            )
        )
        saveSettings()
    }

    fun applyScenario(scenario: AutomationScenario) {
        _uiState.value = _uiState.value.copy(
            settings = _uiState.value.settings.copy(
                maxMessagesPerHour = scenario.settings.maxMessagesPerHour,
                minIntervalSeconds = scenario.settings.minIntervalSeconds,
                maxIntervalSeconds = scenario.settings.maxIntervalSeconds,
                autoSendEnabled = scenario.settings.autoSendEnabled
            )
        )
        saveSettings()
    }
}

data class AutomationSettingsUiState(
    val settings: AutomationSettings = AutomationSettings(),
    val availablePersonas: List<Persona> = emptyList(),
    val availableScenarios: List<AutomationScenario> = emptyList()
)
