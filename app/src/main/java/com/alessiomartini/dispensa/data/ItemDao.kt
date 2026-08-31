package com.alessiomartini.dispensa.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM grocery_items ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<GroceryItem>>

    @Query("SELECT * FROM grocery_items WHERE status = 'IN_PANTRY' AND expiryDate IS NOT NULL ORDER BY expiryDate ASC")
    fun observePantryWithExpiry(): Flow<List<GroceryItem>>

    @Query("SELECT * FROM grocery_items WHERE status = 'IN_PANTRY' AND expiryDate IS NOT NULL AND expiryDate <= :cutoff AND expiryNotified = 0")
    suspend fun findUnnotifiedExpiring(cutoff: Long): List<GroceryItem>

    @Query("UPDATE grocery_items SET expiryNotified = 1 WHERE id IN (:ids)")
    suspend fun markNotified(ids: List<Long>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: GroceryItem): Long

    @Update
    suspend fun update(item: GroceryItem)

    @Delete
    suspend fun delete(item: GroceryItem)
}
