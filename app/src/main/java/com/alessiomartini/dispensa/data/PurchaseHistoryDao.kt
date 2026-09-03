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
}
