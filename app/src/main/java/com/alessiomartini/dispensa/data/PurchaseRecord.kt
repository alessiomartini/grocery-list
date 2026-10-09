package com.alessiomartini.dispensa.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * One purchase event, recorded whenever an item is marked as bought. Kept even if the
 * [GroceryItem] itself is later edited or deleted, so purchase-frequency stats survive.
 *
 * The [Index] and [ColumnInfo.defaultValue]s must match MIGRATION_2_3 - see [GroceryItem].
 */
@Entity(tableName = "purchase_history", indices = [Index("uuid")])
data class PurchaseRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Stable identity across devices, used to match this row against the sync server's copy. */
    @ColumnInfo(defaultValue = "''")
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val purchasedAt: LocalDate,
    /** Records are never edited after creation, so this is set once and never bumped. */
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Instant = Instant.now()
)
