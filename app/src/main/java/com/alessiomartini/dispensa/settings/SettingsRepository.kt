package com.alessiomartini.dispensa.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppSettings(
    val apiKey: String = "",
    val model: String = DEFAULT_MODEL
) {
    companion object {
        const val DEFAULT_MODEL = "claude-sonnet-5"
    }
}

/**
 * Stores the user's own Anthropic API key locally, encrypted with a key held in the
 * Android Keystore. The key never leaves the device except in direct calls to the
 * Anthropic API made from [com.alessiomartini.dispensa.network.RecipeSuggestionRepository].
 */
class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            "secret_shared_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        _settings.value = AppSettings(
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            model = prefs.getString(KEY_MODEL, AppSettings.DEFAULT_MODEL) ?: AppSettings.DEFAULT_MODEL
        )
    }

    fun save(apiKey: String, model: String) {
        prefs.edit()
            .putString(KEY_API_KEY, apiKey.trim())
            .putString(KEY_MODEL, model.trim().ifBlank { AppSettings.DEFAULT_MODEL })
            .apply()
        _settings.value = AppSettings(apiKey.trim(), model.trim().ifBlank { AppSettings.DEFAULT_MODEL })
    }

    companion object {
        private const val KEY_API_KEY = "anthropic_api_key"
        private const val KEY_MODEL = "anthropic_model"
    }
}
