package com.alessiomartini.dispensa.data

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

class ItemRepository(private val dao: ItemDao) {

    fun observeAll(): Flow<List<GroceryItem>> = dao.observeAll()

    fun observePantryWithExpiry(): Flow<List<GroceryItem>> = dao.observePantryWithExpiry()

    suspend fun addToShoppingList(
        name: String,
        quantity: Int,
        unit: String,
        category: String
    ) {
        dao.upsert(
            GroceryItem(
                name = name.trim(),
                quantity = quantity,
                unit = unit.trim(),
                category = category,
                status = ItemStatus.TO_BUY
            )
        )
    }

    /** Item bought: moves from "to buy" to "in pantry", optionally with an expiry date. */
    suspend fun markAsBought(item: GroceryItem, expiryDate: LocalDate?) {
        dao.update(
            item.copy(
                status = ItemStatus.IN_PANTRY,
                expiryDate = expiryDate,
                statusChangedAt = Instant.now(),
                expiryNotified = false
            )
        )
    }

    /** Item ran out at home: moves back to "to buy" so it resurfaces on the shopping list, like unchecking a Keep item. */
    suspend fun markAsFinished(item: GroceryItem) {
        dao.update(
            item.copy(
                status = ItemStatus.TO_BUY,
                expiryDate = null,
                statusChangedAt = Instant.now(),
                expiryNotified = false
            )
        )
    }

    /** Full edit (name/quantity/unit/category/expiry) in one write, so partial updates can't clobber each other. */
    suspend fun updateItem(
        item: GroceryItem,
        name: String,
        quantity: Int,
        unit: String,
        category: String,
        expiryDate: LocalDate?
    ) {
        dao.update(
            item.copy(
                name = name.trim(),
                quantity = quantity,
                unit = unit.trim(),
                category = category,
                expiryDate = expiryDate,
                expiryNotified = false
            )
        )
    }

    suspend fun delete(item: GroceryItem) = dao.delete(item)

    suspend fun findItemsExpiringBy(date: LocalDate): List<GroceryItem> =
        dao.findUnnotifiedExpiring(date.toEpochDay())

    suspend fun markNotified(items: List<GroceryItem>) {
        if (items.isEmpty()) return
        dao.markNotified(items.map { it.id })
    }
}
