package com.alessiomartini.dispensa.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alessiomartini.dispensa.data.GroceryItem
import com.alessiomartini.dispensa.data.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ListViewModel(private val repository: ItemRepository) : ViewModel() {

    val items: StateFlow<List<GroceryItem>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun updateExpiryDate(item: GroceryItem, expiryDate: LocalDate?) {
        viewModelScope.launch { repository.updateExpiryDate(item, expiryDate) }
    }

    fun deleteItem(item: GroceryItem) {
        viewModelScope.launch { repository.delete(item) }
    }
}
