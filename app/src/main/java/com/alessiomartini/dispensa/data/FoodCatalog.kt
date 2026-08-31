package com.alessiomartini.dispensa.data

data class FoodCatalogItem(
    val name: String,
    val category: String,
    val icon: String,
    val extraKeywords: List<String> = emptyList()
) {
    val keywords: List<String> = (extraKeywords + name).map { it.lowercase() }.distinct()
}

/**
 * A small local list of common grocery items, used to guess an icon and category for whatever
 * the user types, and to surface "you might also need" suggestions. Matching is substring-based
 * on purpose (no NLP/network call) so it stays instant and works offline.
 */
object FoodCatalog {

    val items: List<FoodCatalogItem> = listOf(
        // Fruits and vegetables
        FoodCatalogItem("Apple", "Fruits and vegetables", "🍎"),
        FoodCatalogItem("Banana", "Fruits and vegetables", "🍌"),
        FoodCatalogItem("Orange", "Fruits and vegetables", "🍊"),
        FoodCatalogItem("Lemon", "Fruits and vegetables", "🍋"),
        FoodCatalogItem("Grapes", "Fruits and vegetables", "🍇"),
        FoodCatalogItem("Strawberry", "Fruits and vegetables", "🍓", listOf("strawberries")),
        FoodCatalogItem("Watermelon", "Fruits and vegetables", "🍉"),
        FoodCatalogItem("Pineapple", "Fruits and vegetables", "🍍"),
        FoodCatalogItem("Avocado", "Fruits and vegetables", "🥑"),
        FoodCatalogItem("Tomato", "Fruits and vegetables", "🍅", listOf("tomatoes")),
        FoodCatalogItem("Potato", "Fruits and vegetables", "🥔", listOf("potatoes")),
        FoodCatalogItem("Carrot", "Fruits and vegetables", "🥕", listOf("carrots")),
        FoodCatalogItem("Onion", "Fruits and vegetables", "🧅", listOf("onions")),
        FoodCatalogItem("Garlic", "Fruits and vegetables", "🧄"),
        FoodCatalogItem("Broccoli", "Fruits and vegetables", "🥦"),
        FoodCatalogItem("Lettuce", "Fruits and vegetables", "🥬", listOf("salad greens")),
        FoodCatalogItem("Cucumber", "Fruits and vegetables", "🥒"),
        FoodCatalogItem("Bell pepper", "Fruits and vegetables", "🫑", listOf("peppers")),
        FoodCatalogItem("Mushroom", "Fruits and vegetables", "🍄", listOf("mushrooms")),
        FoodCatalogItem("Corn", "Fruits and vegetables", "🌽"),

        // Dairy and eggs
        FoodCatalogItem("Milk", "Dairy and eggs", "🥛"),
        FoodCatalogItem("Eggs", "Dairy and eggs", "🥚", listOf("egg")),
        FoodCatalogItem("Cheese", "Dairy and eggs", "🧀"),
        FoodCatalogItem("Butter", "Dairy and eggs", "🧈"),
        FoodCatalogItem("Yogurt", "Dairy and eggs", "🥣", listOf("yoghurt")),
        FoodCatalogItem("Cream", "Dairy and eggs", "🥛"),

        // Meat and fish
        FoodCatalogItem("Chicken", "Meat and fish", "🍗"),
        FoodCatalogItem("Beef", "Meat and fish", "🥩"),
        FoodCatalogItem("Pork", "Meat and fish", "🥓"),
        FoodCatalogItem("Bacon", "Meat and fish", "🥓"),
        FoodCatalogItem("Fish", "Meat and fish", "🐟"),
        FoodCatalogItem("Salmon", "Meat and fish", "🐟"),
        FoodCatalogItem("Shrimp", "Meat and fish", "🍤", listOf("prawns")),
        FoodCatalogItem("Sausage", "Meat and fish", "🌭", listOf("sausages")),
        FoodCatalogItem("Ham", "Meat and fish", "🍖"),

        // Bread and cereals
        FoodCatalogItem("Bread", "Bread and cereals", "🍞"),
        FoodCatalogItem("Pasta", "Bread and cereals", "🍝"),
        FoodCatalogItem("Rice", "Bread and cereals", "🍚"),
        FoodCatalogItem("Cereal", "Bread and cereals", "🥣", listOf("cereals")),
        FoodCatalogItem("Oats", "Bread and cereals", "🌾", listOf("oatmeal")),
        FoodCatalogItem("Flour", "Bread and cereals", "🌾"),
        FoodCatalogItem("Bagel", "Bread and cereals", "🥯", listOf("bagels")),
        FoodCatalogItem("Croissant", "Bread and cereals", "🥐", listOf("croissants")),

        // Pantry staples
        FoodCatalogItem("Salt", "Pantry staples", "🧂"),
        FoodCatalogItem("Sugar", "Pantry staples", "🍯"),
        FoodCatalogItem("Olive oil", "Pantry staples", "🫒", listOf("cooking oil")),
        FoodCatalogItem("Vinegar", "Pantry staples", "🍶"),
        FoodCatalogItem("Honey", "Pantry staples", "🍯"),
        FoodCatalogItem("Canned tomatoes", "Pantry staples", "🥫"),
        FoodCatalogItem("Beans", "Pantry staples", "🥫"),
        FoodCatalogItem("Jam", "Pantry staples", "🍓", listOf("marmalade")),
        FoodCatalogItem("Peanut butter", "Pantry staples", "🥜"),

        // Frozen foods
        FoodCatalogItem("Ice cream", "Frozen foods", "🍦"),
        FoodCatalogItem("Frozen pizza", "Frozen foods", "🍕"),
        FoodCatalogItem("Frozen vegetables", "Frozen foods", "🧊"),

        // Beverages
        FoodCatalogItem("Water", "Beverages", "💧"),
        FoodCatalogItem("Juice", "Beverages", "🧃"),
        FoodCatalogItem("Coffee", "Beverages", "☕"),
        FoodCatalogItem("Tea", "Beverages", "🍵"),
        FoodCatalogItem("Wine", "Beverages", "🍷"),
        FoodCatalogItem("Beer", "Beverages", "🍺"),
        FoodCatalogItem("Soda", "Beverages", "🥤", listOf("soft drink", "pop")),

        // Household and hygiene
        FoodCatalogItem("Toilet paper", "Household and hygiene", "🧻"),
        FoodCatalogItem("Paper towels", "Household and hygiene", "🧻"),
        FoodCatalogItem("Dish soap", "Household and hygiene", "🧴"),
        FoodCatalogItem("Laundry detergent", "Household and hygiene", "🧴"),
        FoodCatalogItem("Toothpaste", "Household and hygiene", "🪥"),
        FoodCatalogItem("Shampoo", "Household and hygiene", "🧴"),
        FoodCatalogItem("Trash bags", "Household and hygiene", "🗑️", listOf("garbage bags")),
        FoodCatalogItem("Sponge", "Household and hygiene", "🧽", listOf("sponges"))
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

    /** Catalog entries whose name/keywords contain [query], for autocomplete while typing. */
    fun suggestions(query: String, limit: Int = 6): List<FoodCatalogItem> {
        val normalized = query.trim().lowercase()
        if (normalized.isEmpty()) return emptyList()
        return items
            .filter { item -> item.keywords.any { it.contains(normalized) } }
            .take(limit)
    }

    /** Common items not already in [existingNames], to suggest adding to the shopping list. */
    fun quickAddCandidates(existingNames: Collection<String>, limit: Int = 12): List<FoodCatalogItem> {
        val existing = existingNames.map { it.trim().lowercase() }.toSet()
        return items
            .filter { item -> item.name.lowercase() !in existing }
            .take(limit)
    }
}
