package com.alessiomartini.dispensa.ui.expiry

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alessiomartini.dispensa.data.GroceryItem
import com.alessiomartini.dispensa.data.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ExpiryViewModel(repository: ItemRepository) : ViewModel() {
    val items: StateFlow<List<GroceryItem>> = repository.observePantryWithExpiry()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
