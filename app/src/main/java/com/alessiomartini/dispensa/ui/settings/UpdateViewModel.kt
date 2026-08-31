package com.alessiomartini.dispensa.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alessiomartini.dispensa.network.UpdateCheckResult
import com.alessiomartini.dispensa.network.UpdateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val version: String, val notes: String, val downloadUrl: String) : UpdateUiState
    data object Downloading : UpdateUiState
    data class ReadyToInstall(val file: File) : UpdateUiState
    data class Error(val message: String) : UpdateUiState
}

class UpdateViewModel(private val updateRepository: UpdateRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    fun checkForUpdate() {
        viewModelScope.launch {
            _uiState.value = UpdateUiState.Checking
            _uiState.value = when (val result = updateRepository.checkForUpdate()) {
                is UpdateCheckResult.Available ->
                    UpdateUiState.Available(result.version, result.notes, result.downloadUrl)
                is UpdateCheckResult.UpToDate -> UpdateUiState.UpToDate
                is UpdateCheckResult.Error -> UpdateUiState.Error(result.message)
            }
        }
    }

    fun downloadUpdate(downloadUrl: String) {
        viewModelScope.launch {
            _uiState.value = UpdateUiState.Downloading
            _uiState.value = try {
                UpdateUiState.ReadyToInstall(updateRepository.downloadApk(downloadUrl))
            } catch (e: Exception) {
                UpdateUiState.Error(e.message ?: "Download fallito")
            }
        }
    }

    fun install(file: File) {
        if (updateRepository.canInstallPackages()) {
            updateRepository.installApk(file)
        } else {
            updateRepository.openInstallPermissionSettings()
        }
    }
}
