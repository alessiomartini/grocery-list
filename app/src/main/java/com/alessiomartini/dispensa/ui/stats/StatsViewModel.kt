package com.alessiomartini.dispensa.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alessiomartini.dispensa.data.FoodCatalog
import com.alessiomartini.dispensa.data.ItemRepository
import com.alessiomartini.dispensa.data.PurchaseRecord
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ItemStat(
    val name: String,
    val category: String,
    val icon: String,
    val purchaseCount: Int,
    val lastPurchased: LocalDate,
    /** Average days between consecutive purchases; null if it's only been bought once. */
    val avgDaysBetweenPurchases: Double?
)

class StatsViewModel(repository: ItemRepository) : ViewModel() {

    val stats: StateFlow<List<ItemStat>> = repository.observePurchaseHistory()
        .map(::toStats)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

/** Groups purchase records case-insensitively by name and derives frequency stats per item. */
private fun toStats(records: List<PurchaseRecord>): List<ItemStat> =
    records
        .groupBy { it.name.trim().lowercase() }
        .map { (_, group) ->
            val sorted = group.sortedBy { it.purchasedAt }
            val latest = sorted.last()
            val avgDays = if (sorted.size >= 2) {
                ChronoUnit.DAYS.between(sorted.first().purchasedAt, latest.purchasedAt)
                    .toDouble() / (sorted.size - 1)
            } else {
                null
            }
            ItemStat(
                name = latest.name,
                category = latest.category,
                icon = FoodCatalog.iconFor(latest.name, latest.category),
                purchaseCount = sorted.size,
                lastPurchased = latest.purchasedAt,
                avgDaysBetweenPurchases = avgDays
            )
        }
        .sortedWith(compareByDescending<ItemStat> { it.purchaseCount }.thenBy { it.name.lowercase() })
