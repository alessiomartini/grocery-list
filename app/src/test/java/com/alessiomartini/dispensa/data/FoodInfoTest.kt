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
}
