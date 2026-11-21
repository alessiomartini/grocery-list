package com.alessiomartini.dispensa.data

import java.time.Instant

/**
 * Re-links a row that arrived from outside the app (pantry.py, the Worker, another device on an
 * older version) to the food catalog: tidies the name ([FoodInfo.tidyName]) and moves an
 * uncategorised row into the catalog's category. The link is the name itself, so a raw
 * "latte " or "milk" would otherwise show up without capital, translation and category.
 *
 * Returns [item] itself when nothing changes; otherwise a copy with updatedAt = [now], so the
 * correction is pushed back and the other copies converge on it (it is idempotent, so no ping-pong).
 */
fun linkToCatalog(item: GroceryItem, now: Instant = Instant.now()): GroceryItem {
    if (item.deleted) return item
    val name = FoodInfo.tidyName(item.name)
    val category = if (item.category == Categories.DEFAULT) {
        FoodCatalog.categoryFor(name)?.takeIf { it != Categories.DEFAULT } ?: item.category
    } else item.category
    if (name == item.name && category == item.category) return item
    return item.copy(name = name, category = category, updatedAt = now)
}
