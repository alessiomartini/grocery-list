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
    val model: String = DEFAULT_MODEL,
    /** Whether the app should silently check for (and install) newer builds on its own. */
    val autoCheckForUpdates: Boolean = true,
    /** Epoch millis of the last update check, or null if never checked - used to throttle auto-checks. */
    val lastUpdateCheckAt: Long? = null
) {
    companion object {
        const val DEFAULT_MODEL = "gemini-2.0-flash"
    }
}

/**
 * Stores the user's own Gemini API key locally, encrypted with a key held in the Android
 * Keystore. The key never leaves the device except in direct calls to the Gemini API made from
 * [com.alessiomartini.dispensa.network.RecipeSuggestionRepository].
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
            model = prefs.getString(KEY_MODEL, AppSettings.DEFAULT_MODEL) ?: AppSettings.DEFAULT_MODEL,
            autoCheckForUpdates = prefs.getBoolean(KEY_AUTO_CHECK_UPDATES, true),
            lastUpdateCheckAt = prefs.getLong(KEY_LAST_UPDATE_CHECK_AT, -1L).takeIf { it >= 0 }
        )
    }

    fun save(apiKey: String, model: String) {
        prefs.edit()
            .putString(KEY_API_KEY, apiKey.trim())
            .putString(KEY_MODEL, model.trim().ifBlank { AppSettings.DEFAULT_MODEL })
            .apply()
        _settings.value = _settings.value.copy(
            apiKey = apiKey.trim(),
            model = model.trim().ifBlank { AppSettings.DEFAULT_MODEL }
        )
    }

    fun setAutoCheckForUpdates(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CHECK_UPDATES, enabled).apply()
        _settings.value = _settings.value.copy(autoCheckForUpdates = enabled)
    }

    fun setLastUpdateCheckAt(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_UPDATE_CHECK_AT, timestamp).apply()
        _settings.value = _settings.value.copy(lastUpdateCheckAt = timestamp)
    }

    companion object {
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_MODEL = "gemini_model"
        private const val KEY_AUTO_CHECK_UPDATES = "auto_check_updates"
        private const val KEY_LAST_UPDATE_CHECK_AT = "last_update_check_at"
    }
}
