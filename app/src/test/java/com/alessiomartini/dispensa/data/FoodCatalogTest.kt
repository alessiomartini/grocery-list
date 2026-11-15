package com.alessiomartini.dispensa.data

import org.junit.Assert.assertEquals
import org.junit.Test

/** Names as printed on Lidl receipts (2024-2026) must resolve to the intended catalog item. */
class FoodCatalogTest {
    @Test fun lidlReceiptNames() {
        mapOf(
            "Sojadrink ongezoet" to "Soy drink",
            "Tomaten gezeefd 10,5" to "Canned tomatoes",
            "Trostomaten" to "Tomato",
            "Andijvie" to "Endive",
            "Bananen" to "Banana",
            "Zuivelspread naturel" to "Butter",
            "ACE multivitamine" to "Multivitamin",
            "Ribbelchips naturel" to "Chips",
            "Kaasbroodje goudse" to "Bakery snack",
            "Franse brie 60%" to "Cheese",
        ).forEach { (receipt, expected) -> assertEquals(receipt, expected, FoodCatalog.find(receipt)?.name) }
    }

    @Test fun pantryNames() {
        mapOf(
            "Tomato paste" to "Tomato paste",
            "Cream cheese" to "Cream cheese",
            "Chili pepper" to "Chili pepper",
            "White pepper" to "Peppercorn mix",
            "Instant coffee" to "Instant coffee",
            "piselli" to "Peas",
            "Heksenkaas" to "Pecorino",
            "Chickpeas" to "Chickpeas",
            "Sweet chili sauce" to "Hot sauce",
        ).forEach { (name, expected) -> assertEquals(name, expected, FoodCatalog.find(name)?.name) }
    }
}
