package com.alessiomartini.dispensa.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alessiomartini.dispensa.data.FoodCatalog
import com.alessiomartini.dispensa.data.FoodCatalogItem
import com.alessiomartini.dispensa.data.GroceryItem
import com.alessiomartini.dispensa.data.ItemRepository
import com.alessiomartini.dispensa.data.PurchaseRecord
import com.alessiomartini.dispensa.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random

class ListViewModel(
    private val repository: ItemRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    /** Reshuffled once per ViewModel instance, so the catalog-fallback suggestions vary across app sessions. */
    private val shuffleSeed = System.currentTimeMillis()

    val items: StateFlow<List<GroceryItem>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dismissedSuggestions: StateFlow<Set<String>> = settingsRepository.settings
        .map { it.dismissedSuggestions }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    /**
     * "You might also need" candidates: items you've actually bought before (ranked by how often),
     * then the static catalog as filler, minus whatever's already on a list or was dismissed. Both
     * sources keep the row from going stale/empty the way a fixed 24-item catalog slice alone would.
     */
    val suggestions: StateFlow<List<FoodCatalogItem>> = combine(
        items,
        repository.observePurchaseHistory(),
        dismissedSuggestions
    ) { currentItems, history, dismissed ->
        buildSuggestions(currentItems, history, dismissed, shuffleSeed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun dismissSuggestion(name: String) {
        settingsRepository.dismissSuggestion(name)
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

    /** Undoes a mistaken tap by writing back the exact item state from before it. */
    fun restoreItem(item: GroceryItem) {
        viewModelScope.launch { repository.restoreSnapshot(item) }
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

private fun buildSuggestions(
    currentItems: List<GroceryItem>,
    history: List<PurchaseRecord>,
    dismissed: Set<String>,
    shuffleSeed: Long
): List<FoodCatalogItem> {
    val excluded = (currentItems.map { it.name } + dismissed).map { it.trim().lowercase() }.toSet()

    val fromHistory = history
        .groupBy { it.name.trim().lowercase() }
        .filterKeys { it !in excluded }
        .map { (_, records) -> records.maxByOrNull { it.purchasedAt }!! to records.size }
        .sortedByDescending { (_, count) -> count }
        .map { (latest, _) ->
            FoodCatalogItem(
                name = latest.name,
                category = latest.category,
                icon = FoodCatalog.iconFor(latest.name, latest.category)
            )
        }

    val historyNames = fromHistory.map { it.name.lowercase() }.toSet()
    val fromCatalog = FoodCatalog.quickAddCandidates(excluded, limit = Int.MAX_VALUE)
        .filter { it.name.lowercase() !in historyNames }
        .shuffled(Random(shuffleSeed))

    return (fromHistory + fromCatalog).take(30)
}
