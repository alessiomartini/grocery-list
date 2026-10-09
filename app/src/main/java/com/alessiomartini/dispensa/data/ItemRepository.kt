package com.alessiomartini.dispensa.data

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

class ItemRepository(private val dao: ItemDao, private val purchaseHistoryDao: PurchaseHistoryDao) {

    fun observeAll(): Flow<List<GroceryItem>> = dao.observeAll()

    fun observePantryWithExpiry(): Flow<List<GroceryItem>> = dao.observePantryWithExpiry()

    fun observePurchaseHistory(): Flow<List<PurchaseRecord>> = purchaseHistoryDao.observeAll()

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
                expiryNotified = false,
                updatedAt = Instant.now()
            )
        )
        purchaseHistoryDao.insert(
            PurchaseRecord(name = item.name, category = item.category, purchasedAt = LocalDate.now())
        )
    }

    /** Item ran out at home: moves back to "to buy" so it resurfaces on the shopping list, like unchecking a Keep item. */
    suspend fun markAsFinished(item: GroceryItem) {
        dao.update(
            item.copy(
                status = ItemStatus.TO_BUY,
                expiryDate = null,
                statusChangedAt = Instant.now(),
                expiryNotified = false,
                updatedAt = Instant.now()
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
                expiryNotified = false,
                updatedAt = Instant.now()
            )
        )
    }

    /** Soft delete, so the deletion can still be pushed to the sync server - see [GroceryItem.deleted]. */
    suspend fun delete(item: GroceryItem) = dao.update(item.copy(deleted = true, updatedAt = Instant.now()))

    /**
     * Writes back an exact prior snapshot of the item - used to undo a mistaken tap. Bypasses
     * the purchase-history logging in [markAsBought] since undoing isn't a real purchase.
     * updatedAt is the one field not restored: the undo is itself a new write, and the snapshot's
     * older timestamp would make a server that already saw the mistaken tap reject it as stale.
     */
    suspend fun restoreSnapshot(item: GroceryItem) = dao.update(item.copy(updatedAt = Instant.now()))

    /**
     * A category is stored when an item is added, so anything added before [FoodCatalog] learned
     * its name stays under [Categories.DEFAULT] forever - even though its icon, looked up live,
     * already shows the catalog knows it. Moves those into the catalog's category.
     *
     * Only DEFAULT is touched: any other category is either the catalog's own guess or one the
     * user picked, and neither should be overridden. (Someone who deliberately files a known food
     * under "Other" will see it moved back; that's the price of not tracking manual edits.)
     */
    suspend fun recategorizeUncategorized() {
        for (item in dao.findInCategory(Categories.DEFAULT)) {
            val category = FoodCatalog.categoryFor(item.name)?.takeIf { it != Categories.DEFAULT } ?: continue
            dao.update(item.copy(category = category, updatedAt = Instant.now()))
        }
    }

    suspend fun findItemsExpiringBy(date: LocalDate): List<GroceryItem> =
        dao.findUnnotifiedExpiring(date.toEpochDay())

    /**
     * Doesn't bump updatedAt: the flag only prevents a repeat notification on this device, so it
     * isn't worth syncing. At worst another device shows the same expiry notification once.
     */
    suspend fun markNotified(items: List<GroceryItem>) {
        if (items.isEmpty()) return
        dao.markNotified(items.map { it.id })
    }
}
