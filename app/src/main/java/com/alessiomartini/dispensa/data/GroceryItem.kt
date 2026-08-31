package com.alessiomartini.dispensa.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "grocery_items")
data class GroceryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: Int = 1,
    val unit: String = "",
    val category: String = Categories.DEFAULT,
    val status: ItemStatus = ItemStatus.TO_BUY,
    val expiryDate: LocalDate? = null,
    val addedAt: Instant = Instant.now(),
    val statusChangedAt: Instant = Instant.now(),
    /** True once a notification has already been sent for the current expiry date, to avoid repeats. */
    val expiryNotified: Boolean = false
)
