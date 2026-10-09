package com.alessiomartini.dispensa.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alessiomartini.dispensa.network.SyncRepository
import com.alessiomartini.dispensa.network.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SyncUiState {
    data object Idle : SyncUiState
    data object Syncing : SyncUiState
    data class Success(val itemsSynced: Int, val purchasesSynced: Int) : SyncUiState
    data object NotConfigured : SyncUiState
    data class Error(val message: String) : SyncUiState
}

class SyncViewModel(private val syncRepository: SyncRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<SyncUiState>(SyncUiState.Idle)
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    fun syncNow() {
        viewModelScope.launch {
            _uiState.value = SyncUiState.Syncing
            _uiState.value = when (val result = syncRepository.syncNow()) {
                is SyncResult.Success -> SyncUiState.Success(result.itemsSynced, result.purchasesSynced)
                is SyncResult.NotConfigured -> SyncUiState.NotConfigured
                is SyncResult.Error -> SyncUiState.Error(result.message)
            }
        }
    }
}
