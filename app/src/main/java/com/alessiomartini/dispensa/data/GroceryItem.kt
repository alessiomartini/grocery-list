package com.alessiomartini.dispensa.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * The [Index] and the [ColumnInfo.defaultValue]s must match MIGRATION_2_3 exactly: Room checks
 * the real database against what it would generate for a fresh install every time it opens it,
 * and a migrated database carries both the index and the DEFAULT clauses SQLite requires to add a
 * NOT NULL column to a table that already has rows.
 */
@Entity(tableName = "grocery_items", indices = [Index("uuid")])
data class GroceryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Stable identity across devices, used to match this row against the sync server's copy. */
    @ColumnInfo(defaultValue = "''")
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val quantity: Int = 1,
    val unit: String = "",
    val category: String = Categories.DEFAULT,
    val status: ItemStatus = ItemStatus.TO_BUY,
    val expiryDate: LocalDate? = null,
    val addedAt: Instant = Instant.now(),
    val statusChangedAt: Instant = Instant.now(),
    /** True once a notification has already been sent for the current expiry date, to avoid repeats. */
    val expiryNotified: Boolean = false,
    /** Bumped on every write; the sync server keeps whichever copy of a row has the higher value. */
    @ColumnInfo(defaultValue = "0")
    val updatedAt: Instant = Instant.now(),
    /**
     * Soft delete: a real SQL delete would leave nothing to tell the sync server about, and the
     * item would come back on the next pull. Filtered out of every read query in [ItemDao].
     */
    @ColumnInfo(defaultValue = "0")
    val deleted: Boolean = false
)
