package com.alessiomartini.dispensa.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoodInfoTest {
    @Test fun bilingualName() {
        assertEquals("Latte / Melk", FoodInfo.displayName("milk", listOf("it", "nl")))
        assertEquals("Latte / Melk", FoodInfo.displayName("Latte", listOf("it", "nl")))
        assertEquals("Penne rigate", FoodInfo.displayName("Penne rigate", listOf("it", "nl")))
    }

    @Test fun season() {
        assertEquals(true, FoodInfo.inSeason("Strawberry", 6))
        assertEquals(false, FoodInfo.inSeason("Fragola", 1))
        assertNull(FoodInfo.inSeason("Banana", 6))
        assertNull(FoodInfo.inSeason("Milk", 6))
    }

    @Test fun handTypedNamesStillMatchCatalogue() {
        val langs = listOf("it", "nl")
        // Plural, extra spaces, accents and case must not break the link with the catalogue entry.
        assertEquals("Pomodoro / Tomaat", FoodInfo.displayName("pomodori", langs))
        assertEquals("Mela / Appel", FoodInfo.displayName("  MELE ", langs))
        assertEquals("Caffè / Koffie", FoodInfo.displayName("caffe", langs))
        assertEquals("Pasta sfoglia / Bladerdeeg", FoodInfo.displayName("pasta   sfoglia", langs))
        assertEquals("Uova / Eieren", FoodInfo.displayName("egg", langs))
        assertEquals(FoodCatalog.items.first { it.name == "Tomato" }.icon, FoodCatalog.iconFor("pomodori"))
        // "mela" and "melk" must stay distinct.
        assertEquals("Latte / Melk", FoodInfo.displayName("melk", langs))
    }

    @Test fun unknownNameIsCapitalisedOnly() {
        assertEquals("Penne rigate", FoodInfo.displayName("penne rigate", listOf("it", "nl")))
    }
}
