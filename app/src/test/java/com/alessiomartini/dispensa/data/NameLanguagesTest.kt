package com.alessiomartini.dispensa.data

import com.alessiomartini.dispensa.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class NameLanguagesTest {
    @Test fun multilingualOnShowsAllConfigured() {
        val s = AppSettings(nameLanguages = listOf("it", "nl", "en"))
        assertEquals("Latte / Melk / Milk", FoodInfo.displayName("milk", s.shownNameLanguages))
    }

    @Test fun multilingualOffShowsFirstOnly() {
        val s = AppSettings(nameLanguages = listOf("it", "nl", "en"), multilingualNames = false)
        assertEquals("Latte", FoodInfo.displayName("milk", s.shownNameLanguages))
    }
}
