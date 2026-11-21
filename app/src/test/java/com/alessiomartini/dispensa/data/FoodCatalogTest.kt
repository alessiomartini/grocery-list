package com.alessiomartini.dispensa.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
            "Chickpeas" to "Chickpeas",
            "Sweet chili sauce" to "Hot sauce",
        ).forEach { (name, expected) -> assertEquals(name, expected, FoodCatalog.find(name)?.name) }
    }
}

/** Italian plural/gender variants are only a fallback after the exact/substring match. */
class FoodCatalogInflectionTest {
    @Test fun pluralsMatchSingularItems() {
        mapOf(
            "pomodori" to "Tomato",
            "Mele" to "Apple",
            "banane" to "Banana",
            "peperoni" to "Bell pepper",
            "carote" to "Carrot",
            "zucchine" to "Zucchini",
            "mele verdi" to "Apple",
            "strawberries" to "Strawberry",
        ).forEach { (name, expected) -> assertEquals(name, expected, FoodCatalog.find(name)?.name) }
    }

    @Test fun singularMatchesPluralNamedItems() {
        mapOf("fungo" to "Mushroom", "uovo" to "Eggs", "lentil" to "Lentils")
            .forEach { (name, expected) -> assertEquals(name, expected, FoodCatalog.find(name)?.name) }
    }

    @Test fun exactMatchWins() {
        assertEquals("Sun-dried tomatoes", FoodCatalog.find("pomodori secchi")?.name)
        assertEquals("Mushroom", FoodCatalog.find("funghi")?.name)
        assertEquals("Eggs", FoodCatalog.find("uova")?.name)
        assertEquals("Tomato", FoodInfo.tidyName("pomodori"))
    }

    @Test fun inflectionRules() {
        assertEquals("fungo", FoodInfo.inflectionVariants("funghi").first())
        assertEquals("albicocca", FoodInfo.inflectionVariants("albicocche").first())
        assertEquals("pomodoro", FoodInfo.inflectionVariants("pomodori").first())
        assertEquals("pera", FoodInfo.inflectionVariants("pere").first())
    }

    @Test fun noFalsePositives() {
        assertNull(FoodCatalog.find("tavoli"))
        assertNull(FoodCatalog.find("sedie"))
        assertEquals(emptyList<String>(), FoodInfo.inflectionVariants("ali"))
        assertEquals("Sci", FoodInfo.tidyName("sci"))
    }
}
