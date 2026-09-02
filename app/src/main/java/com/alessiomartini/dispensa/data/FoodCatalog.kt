package com.alessiomartini.dispensa.data

import java.time.LocalDate

data class FoodCatalogItem(
    val name: String,
    val category: String,
    val icon: String,
    /** Typical days from purchase until it goes bad, if known. Null for non-perishables. */
    val shelfLifeDays: Int? = null,
    val extraKeywords: List<String> = emptyList()
) {
    val keywords: List<String> = (extraKeywords + name).map { it.lowercase() }.distinct()
}

/**
 * A small local list of common grocery items, used to guess an icon, category, and typical
 * expiry date for whatever the user types, and to surface "you might also need" suggestions.
 * Matching is substring-based on purpose (no NLP/network call) so it stays instant and works
 * offline.
 */
object FoodCatalog {

    val items: List<FoodCatalogItem> = listOf(
        // Fruits and vegetables
        FoodCatalogItem("Apple", "Fruits and vegetables", "🍎", 21),
        FoodCatalogItem("Banana", "Fruits and vegetables", "🍌", 5),
        FoodCatalogItem("Orange", "Fruits and vegetables", "🍊", 14),
        FoodCatalogItem("Lemon", "Fruits and vegetables", "🍋", 21),
        FoodCatalogItem("Grapes", "Fruits and vegetables", "🍇", 7),
        FoodCatalogItem("Strawberry", "Fruits and vegetables", "🍓", 4, listOf("strawberries")),
        FoodCatalogItem("Watermelon", "Fruits and vegetables", "🍉", 7),
        FoodCatalogItem("Pineapple", "Fruits and vegetables", "🍍", 5),
        FoodCatalogItem("Avocado", "Fruits and vegetables", "🥑", 5),
        FoodCatalogItem("Tomato", "Fruits and vegetables", "🍅", 7, listOf("tomatoes")),
        FoodCatalogItem("Potato", "Fruits and vegetables", "🥔", 30, listOf("potatoes")),
        FoodCatalogItem("Carrot", "Fruits and vegetables", "🥕", 21, listOf("carrots")),
        FoodCatalogItem("Onion", "Fruits and vegetables", "🧅", 30, listOf("onions")),
        FoodCatalogItem("Garlic", "Fruits and vegetables", "🧄", 60),
        FoodCatalogItem("Broccoli", "Fruits and vegetables", "🥦", 5),
        FoodCatalogItem("Lettuce", "Fruits and vegetables", "🥬", 5, listOf("salad greens")),
        FoodCatalogItem("Cucumber", "Fruits and vegetables", "🥒", 7),
        FoodCatalogItem("Bell pepper", "Fruits and vegetables", "🫑", 10, listOf("peppers")),
        FoodCatalogItem("Mushroom", "Fruits and vegetables", "🍄", 5, listOf("mushrooms")),
        FoodCatalogItem("Corn", "Fruits and vegetables", "🌽", 3),

        // Dairy and eggs
        FoodCatalogItem("Milk", "Dairy and eggs", "🥛", 7),
        FoodCatalogItem("Eggs", "Dairy and eggs", "🥚", 21, listOf("egg")),
        FoodCatalogItem("Cheese", "Dairy and eggs", "🧀", 30),
        FoodCatalogItem("Butter", "Dairy and eggs", "🧈", 60),
        FoodCatalogItem("Yogurt", "Dairy and eggs", "🥣", 14, listOf("yoghurt")),
        FoodCatalogItem("Cream", "Dairy and eggs", "🥛", 10),

        // Meat and fish
        FoodCatalogItem("Chicken", "Meat and fish", "🍗", 2),
        FoodCatalogItem("Beef", "Meat and fish", "🥩", 3),
        FoodCatalogItem("Pork", "Meat and fish", "🥓", 3),
        FoodCatalogItem("Bacon", "Meat and fish", "🥓", 7),
        FoodCatalogItem("Fish", "Meat and fish", "🐟", 2),
        FoodCatalogItem("Salmon", "Meat and fish", "🐟", 2),
        FoodCatalogItem("Shrimp", "Meat and fish", "🍤", 2, listOf("prawns")),
        FoodCatalogItem("Sausage", "Meat and fish", "🌭", 7, listOf("sausages")),
        FoodCatalogItem("Ham", "Meat and fish", "🍖", 5),

        // Bread and cereals
        FoodCatalogItem("Bread", "Bread and cereals", "🍞", 5),
        FoodCatalogItem("Pasta", "Bread and cereals", "🍝", 365),
        FoodCatalogItem("Rice", "Bread and cereals", "🍚", 365),
        FoodCatalogItem("Cereal", "Bread and cereals", "🥣", 180, listOf("cereals")),
        FoodCatalogItem("Oats", "Bread and cereals", "🌾", 180, listOf("oatmeal")),
        FoodCatalogItem("Flour", "Bread and cereals", "🌾", 180),
        FoodCatalogItem("Bagel", "Bread and cereals", "🥯", 5, listOf("bagels")),
        FoodCatalogItem("Croissant", "Bread and cereals", "🥐", 3, listOf("croissants")),

        // Pantry staples
        FoodCatalogItem("Salt", "Pantry staples", "🧂", 1825),
        FoodCatalogItem("Sugar", "Pantry staples", "🍯", 730),
        FoodCatalogItem("Olive oil", "Pantry staples", "🫒", 365, listOf("cooking oil")),
        FoodCatalogItem("Vinegar", "Pantry staples", "🍶", 730),
        FoodCatalogItem("Honey", "Pantry staples", "🍯", 1825),
        FoodCatalogItem("Canned tomatoes", "Pantry staples", "🥫", 730),
        FoodCatalogItem("Beans", "Pantry staples", "🥫", 730),
        FoodCatalogItem("Jam", "Pantry staples", "🍓", 365, listOf("marmalade")),
        FoodCatalogItem("Peanut butter", "Pantry staples", "🥜", 180),

        // Frozen foods
        FoodCatalogItem("Ice cream", "Frozen foods", "🍦", 90),
        FoodCatalogItem("Frozen pizza", "Frozen foods", "🍕", 180),
        FoodCatalogItem("Frozen vegetables", "Frozen foods", "🧊", 270),

        // Beverages
        FoodCatalogItem("Water", "Beverages", "💧", 365),
        FoodCatalogItem("Juice", "Beverages", "🧃", 14),
        FoodCatalogItem("Coffee", "Beverages", "☕", 180),
        FoodCatalogItem("Tea", "Beverages", "🍵", 365),
        FoodCatalogItem("Wine", "Beverages", "🍷", 1825),
        FoodCatalogItem("Beer", "Beverages", "🍺", 180),
        FoodCatalogItem("Soda", "Beverages", "🥤", 180, listOf("soft drink", "pop")),

        // Household and hygiene (non-perishable: no shelf life)
        FoodCatalogItem("Toilet paper", "Household and hygiene", "🧻"),
        FoodCatalogItem("Paper towels", "Household and hygiene", "🧻"),
        FoodCatalogItem("Dish soap", "Household and hygiene", "🧴"),
        FoodCatalogItem("Laundry detergent", "Household and hygiene", "🧴"),
        FoodCatalogItem("Toothpaste", "Household and hygiene", "🪥"),
        FoodCatalogItem("Shampoo", "Household and hygiene", "🧴"),
        FoodCatalogItem("Trash bags", "Household and hygiene", "🗑️", extraKeywords = listOf("garbage bags")),
        FoodCatalogItem("Sponge", "Household and hygiene", "🧽", extraKeywords = listOf("sponges"))
    )

    private const val DEFAULT_ICON = "🛒"

    private val categoryFallbackIcons = mapOf(
        "Fruits and vegetables" to "🥦",
        "Dairy and eggs" to "🧀",
        "Meat and fish" to "🍖",
        "Bread and cereals" to "🍞",
        "Pantry staples" to "🥫",
        "Frozen foods" to "🧊",
        "Beverages" to "🥤",
        "Household and hygiene" to "🧴"
    )

    /** Fallback shelf life (days) by category, used when the exact item isn't in the catalog. */
    private val categoryDefaultShelfLifeDays = mapOf(
        "Fruits and vegetables" to 7,
        "Dairy and eggs" to 10,
        "Meat and fish" to 3,
        "Bread and cereals" to 14,
        "Pantry staples" to 180,
        "Frozen foods" to 120,
        "Beverages" to 180
        // Household and hygiene, Other: no sensible default, left unmapped.
    )

    /** Best catalog match for a (possibly multi-word) item name the user typed, if any. */
    fun find(itemName: String): FoodCatalogItem? {
        val normalized = itemName.trim().lowercase()
        if (normalized.isEmpty()) return null
        return items
            .flatMap { item -> item.keywords.filter { normalized.contains(it) }.map { it to item } }
            .maxByOrNull { (keyword, _) -> keyword.length }
            ?.second
    }

    fun iconFor(itemName: String, category: String? = null): String {
        find(itemName)?.let { return it.icon }
        category?.let { categoryFallbackIcons[it] }?.let { return it }
        return DEFAULT_ICON
    }

    fun categoryFor(itemName: String): String? = find(itemName)?.category

    /**
     * A sensible default expiry date for [itemName], based on its typical shelf life (or its
     * category's, if the exact item isn't in the catalog). Null means "don't suggest one" -
     * either nothing matched, or the item doesn't meaningfully expire.
     */
    fun suggestedExpiryDate(itemName: String, category: String? = null, from: LocalDate = LocalDate.now()): LocalDate? {
        val shelfLifeDays = find(itemName)?.shelfLifeDays
            ?: category?.let { categoryDefaultShelfLifeDays[it] }
            ?: return null
        return from.plusDays(shelfLifeDays.toLong())
    }

    /** Catalog entries whose name/keywords contain [query], for autocomplete while typing. */
    fun suggestions(query: String, limit: Int = 6): List<FoodCatalogItem> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return emptyList()
        return items
            .filter { item -> item.keywords.any { it.contains(normalized) } }
            .take(limit)
    }

    /** Common items not already in [existingNames], to suggest adding to the shopping list. */
    fun quickAddCandidates(existingNames: Collection<String>, limit: Int = 24): List<FoodCatalogItem> {
        val existing = existingNames.map { it.trim().lowercase() }.toSet()
        return items
            .filter { item -> item.name.lowercase() !in existing }
            .take(limit)
    }
}
