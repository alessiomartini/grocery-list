package com.alessiomartini.dispensa.ui.settings

import androidx.lifecycle.ViewModel
import com.alessiomartini.dispensa.settings.AppSettings
import com.alessiomartini.dispensa.settings.SettingsRepository
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settings

    fun save(apiKey: String, model: String) {
        repository.save(apiKey, model)
    }

    fun setAutoCheckForUpdates(enabled: Boolean) {
        repository.setAutoCheckForUpdates(enabled)
    }
}
