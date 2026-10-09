package com.alessiomartini.dispensa.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PurchaseHistoryDao {

    @Insert
    suspend fun insert(record: PurchaseRecord)

    @Query("SELECT * FROM purchase_history ORDER BY purchasedAt DESC")
    fun observeAll(): Flow<List<PurchaseRecord>>

    @Query("SELECT * FROM purchase_history WHERE uuid = :uuid LIMIT 1")
    suspend fun findByUuid(uuid: String): PurchaseRecord?

    /** Rows created after [since] (epoch millis) - what a sync push uploads. */
    @Query("SELECT * FROM purchase_history WHERE updatedAt > :since ORDER BY updatedAt ASC")
    suspend fun findUpdatedSince(since: Long): List<PurchaseRecord>
}
