package com.alessiomartini.dispensa.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alessiomartini.dispensa.data.GroceryItem
import com.alessiomartini.dispensa.data.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class ListViewModel(private val repository: ItemRepository) : ViewModel() {

    val items: StateFlow<List<GroceryItem>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Names the user long-pressed off the "Suggested" row; not persisted, resets next launch. */
    private val _dismissedSuggestions = MutableStateFlow<Set<String>>(emptySet())
    val dismissedSuggestions: StateFlow<Set<String>> = _dismissedSuggestions.asStateFlow()

    fun dismissSuggestion(name: String) {
        _dismissedSuggestions.value = _dismissedSuggestions.value + name.trim().lowercase()
    }

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
}
