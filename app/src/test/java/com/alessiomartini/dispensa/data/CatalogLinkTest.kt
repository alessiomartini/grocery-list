package com.alessiomartini.dispensa.data

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class CatalogLinkTest {
    private val now = Instant.parse("2026-11-21T10:00:00Z")

    @Test fun tidyNameMatchesCatalogIgnoringCaseAccentsAndSpacing() {
        assertEquals("Milk", FoodInfo.tidyName("latte"))
        assertEquals("Milk", FoodInfo.tidyName("  LATTE "))
        assertEquals("Milk", FoodInfo.tidyName("melk"))
        assertEquals("Instant coffee", FoodInfo.tidyName("caffe  solubile"))
    }

    @Test fun tidyNameCapitalisesUnknownNames() {
        assertEquals("Zuppa del giorno", FoodInfo.tidyName("zuppa  del giorno"))
    }

    @Test fun displayNameCapitalisesUnknownNames() {
        assertEquals("Zuppa del giorno", FoodInfo.displayName("zuppa del giorno", listOf("it")))
        assertEquals("Latte / Melk", FoodInfo.displayName("LATTE", listOf("it", "nl")))
    }

    @Test fun linkToCatalogFixesNameAndCategory() {
        val raw = GroceryItem(name = "latte", category = Categories.DEFAULT, status = ItemStatus.TO_BUY)
        val linked = linkToCatalog(raw, now)
        assertEquals("Milk", linked.name)
        assertEquals("Dairy and eggs", linked.category)
        assertEquals(now, linked.updatedAt)
    }

    @Test fun linkToCatalogIsIdempotentAndKeepsUserCategory() {
        val ok = GroceryItem(name = "Milk", category = "Dairy and eggs", status = ItemStatus.TO_BUY)
        assertSame(ok, linkToCatalog(ok, now))
        val custom = GroceryItem(name = "Milk", category = "Beverages", status = ItemStatus.TO_BUY)
        assertEquals("Beverages", linkToCatalog(custom, now).category)
        val again = linkToCatalog(linkToCatalog(GroceryItem(name = "latte", category = Categories.DEFAULT, status = ItemStatus.TO_BUY), now), now)
        assertEquals("Milk", again.name)
    }
}
