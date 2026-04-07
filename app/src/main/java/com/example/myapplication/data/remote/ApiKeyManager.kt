package com.example.myapplication.data.remote

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ApiKeyManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "ai_assistant_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    var claudeApiKey: String?
        get() = securePrefs.getString(KEY_CLAUDE_API, null)
        set(value) = securePrefs.edit().putString(KEY_CLAUDE_API, value).apply()

    var selectedModel: String
        get() = securePrefs.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
        set(value) = securePrefs.edit().putString(KEY_MODEL, value).apply()

    fun hasApiKey(): Boolean = !claudeApiKey.isNullOrBlank()

    companion object {
        private const val KEY_CLAUDE_API = "claude_api_key"
        private const val KEY_MODEL = "selected_model"
        const val DEFAULT_MODEL = "claude-sonnet-4-20250514"
    }
}
