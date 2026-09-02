package com.alessiomartini.dispensa.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alessiomartini.dispensa.data.GroceryItem
import com.alessiomartini.dispensa.data.ItemRepository
import com.alessiomartini.dispensa.network.ReceiptScanRepository
import com.alessiomartini.dispensa.network.ReceiptScanResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

sealed interface ReceiptScanUiState {
    data object Idle : ReceiptScanUiState
    data object Scanning : ReceiptScanUiState
    data class Done(val count: Int) : ReceiptScanUiState
    data object NoApiKey : ReceiptScanUiState
    data class Error(val message: String) : ReceiptScanUiState
}

class ListViewModel(
    private val repository: ItemRepository,
    private val receiptScanRepository: ReceiptScanRepository
) : ViewModel() {

    val items: StateFlow<List<GroceryItem>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _receiptScanState = MutableStateFlow<ReceiptScanUiState>(ReceiptScanUiState.Idle)
    val receiptScanState: StateFlow<ReceiptScanUiState> = _receiptScanState.asStateFlow()

    fun addItem(name: String, quantity: Int, unit: String, category: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addToShoppingList(name, quantity.coerceAtLeast(1), unit, category)
        }
    }

    /** Checkbox ticked: item was bought and enters the pantry, with an optional expiry date. */
    fun markAsBought(item: GroceryItem, expiryDate: LocalDate?) {
        viewModelScope.launch { repository.markAsBought(item, expiryDate) }
    }

    /** Checkbox unticked: item ran out at home and goes back to the shopping list. */
    fun markAsFinished(item: GroceryItem) {
        viewModelScope.launch { repository.markAsFinished(item) }
    }

    fun updateItem(
        item: GroceryItem,
        name: String,
        quantity: Int,
        unit: String,
        category: String,
        expiryDate: LocalDate?
    ) {
        viewModelScope.launch { repository.updateItem(item, name, quantity, unit, category, expiryDate) }
    }

    fun deleteItem(item: GroceryItem) {
        viewModelScope.launch { repository.delete(item) }
    }

    /** Scans a receipt photo and adds every recognized item straight to the pantry. */
    fun scanReceipt(photoFile: File) {
        viewModelScope.launch {
            _receiptScanState.value = ReceiptScanUiState.Scanning
            _receiptScanState.value = when (val result = receiptScanRepository.scanReceipt(photoFile)) {
                is ReceiptScanResult.Success -> {
                    result.items.forEach { repository.addToPantry(it.name, it.quantity) }
                    ReceiptScanUiState.Done(result.items.size)
                }
                is ReceiptScanResult.NoApiKey -> ReceiptScanUiState.NoApiKey
                is ReceiptScanResult.Error -> ReceiptScanUiState.Error(result.message)
            }
        }
    }

    fun dismissReceiptScanState() {
        _receiptScanState.value = ReceiptScanUiState.Idle
    }
}
