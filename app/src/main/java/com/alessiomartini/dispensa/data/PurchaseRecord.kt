package com.alessiomartini.dispensa.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * One purchase event, recorded whenever an item is marked as bought. Kept even if the
 * [GroceryItem] itself is later edited or deleted, so purchase-frequency stats survive.
 */
@Entity(tableName = "purchase_history")
data class PurchaseRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String,
    val purchasedAt: LocalDate
)
