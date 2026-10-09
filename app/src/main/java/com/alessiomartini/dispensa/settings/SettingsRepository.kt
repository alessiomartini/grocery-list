package com.alessiomartini.dispensa.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

data class AppSettings(
    val apiKey: String = "",
    val model: String = DEFAULT_MODEL,
    /** Whether the app should silently check for (and install) newer builds on its own. */
    val autoCheckForUpdates: Boolean = true,
    /** Epoch millis of the last update check, or null if never checked - used to throttle auto-checks. */
    val lastUpdateCheckAt: Long? = null,
    /** Names long-pressed off the "Suggested" row; persisted so they don't keep reappearing. */
    val dismissedSuggestions: Set<String> = emptySet(),
    /** Base URL of the pantry-api Worker, e.g. https://pantry-api.<subdomain>.workers.dev. */
    val syncUrl: String = "",
    val syncToken: String = "",
    /** Server time of the last items pull, sent back as `since` next time; 0 means never synced. */
    val itemsSyncedThrough: Long = 0,
    /** Same as [itemsSyncedThrough], for the purchase history. */
    val purchasesSyncedThrough: Long = 0,
    /** Epoch millis of the last sync attempt, successful or not. */
    val lastSyncAt: Long? = null
) {
    val syncConfigured: Boolean get() = syncUrl.isNotBlank() && syncToken.isNotBlank()

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

    private val json = Json { ignoreUnknownKeys = true }

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        _settings.value = AppSettings(
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            model = prefs.getString(KEY_MODEL, AppSettings.DEFAULT_MODEL) ?: AppSettings.DEFAULT_MODEL,
            autoCheckForUpdates = prefs.getBoolean(KEY_AUTO_CHECK_UPDATES, true),
            lastUpdateCheckAt = prefs.getLong(KEY_LAST_UPDATE_CHECK_AT, -1L).takeIf { it >= 0 },
            dismissedSuggestions = readDismissedSuggestions(),
            syncUrl = prefs.getString(KEY_SYNC_URL, "") ?: "",
            syncToken = prefs.getString(KEY_SYNC_TOKEN, "") ?: "",
            itemsSyncedThrough = prefs.getLong(KEY_ITEMS_SYNCED_THROUGH, 0L),
            purchasesSyncedThrough = prefs.getLong(KEY_PURCHASES_SYNCED_THROUGH, 0L),
            lastSyncAt = prefs.getLong(KEY_LAST_SYNC_AT, -1L).takeIf { it >= 0 }
        )
    }

    private fun readDismissedSuggestions(): Set<String> =
        prefs.getString(KEY_DISMISSED_SUGGESTIONS, null)
            ?.let { runCatching { json.decodeFromString(ListSerializer(String.serializer()), it) }.getOrNull() }
            ?.toSet()
            ?: emptySet()

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

    fun dismissSuggestion(name: String) {
        val updated = _settings.value.dismissedSuggestions + name.trim().lowercase()
        prefs.edit()
            .putString(KEY_DISMISSED_SUGGESTIONS, json.encodeToString(ListSerializer(String.serializer()), updated.toList()))
            .apply()
        _settings.value = _settings.value.copy(dismissedSuggestions = updated)
    }

    fun saveSync(url: String, token: String) {
        val trimmedUrl = url.trim().removeSuffix("/")
        val trimmedToken = token.trim()
        prefs.edit()
            .putString(KEY_SYNC_URL, trimmedUrl)
            .putString(KEY_SYNC_TOKEN, trimmedToken)
            .apply()
        _settings.value = _settings.value.copy(syncUrl = trimmedUrl, syncToken = trimmedToken)
    }

    fun setItemsSyncedThrough(timestamp: Long) {
        prefs.edit().putLong(KEY_ITEMS_SYNCED_THROUGH, timestamp).apply()
        _settings.value = _settings.value.copy(itemsSyncedThrough = timestamp)
    }

    fun setPurchasesSyncedThrough(timestamp: Long) {
        prefs.edit().putLong(KEY_PURCHASES_SYNCED_THROUGH, timestamp).apply()
        _settings.value = _settings.value.copy(purchasesSyncedThrough = timestamp)
    }

    fun setLastSyncAt(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_AT, timestamp).apply()
        _settings.value = _settings.value.copy(lastSyncAt = timestamp)
    }

    companion object {
        private const val KEY_SYNC_URL = "sync_url"
        private const val KEY_SYNC_TOKEN = "sync_token"
        private const val KEY_ITEMS_SYNCED_THROUGH = "items_synced_through"
        private const val KEY_PURCHASES_SYNCED_THROUGH = "purchases_synced_through"
        private const val KEY_LAST_SYNC_AT = "last_sync_at"
        private const val KEY_API_KEY = "gemini_api_key"
        private const val KEY_MODEL = "gemini_model"
        private const val KEY_AUTO_CHECK_UPDATES = "auto_check_updates"
        private const val KEY_LAST_UPDATE_CHECK_AT = "last_update_check_at"
        private const val KEY_DISMISSED_SUGGESTIONS = "dismissed_suggestions"
    }
}
