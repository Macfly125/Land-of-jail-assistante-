package com.landsofjail.assistant.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceNameMatcherTest {
    @Test
    fun matchesResourceNamesInAllSupportedLanguages() {
        val cases = listOf(
            // French
            "Nourriture" to "food", "Bois" to "wood", "Acier" to "steel", "Essence" to "gasoline",
            // English
            "Food" to "food", "Wood" to "wood", "Steel" to "steel", "Gasoline" to "gasoline",
            // German
            "Nahrung" to "food", "Holz" to "wood", "Stahl" to "steel", "Benzin" to "gasoline",
            // Turkish
            "Yiyecek" to "food", "Odun" to "wood", "Çelik" to "steel", "Benzin" to "gasoline",
            // Russian
            "Еда" to "food", "Дерево" to "wood", "Сталь" to "steel", "Бензин" to "gasoline",
            // Chinese
            "食物" to "food", "木材" to "wood", "钢材" to "steel", "汽油" to "gasoline",
            // Italian
            "Cibo" to "food", "Legno" to "wood", "Acciaio" to "steel", "Benzina" to "gasoline",
            // Composite labels shown by the app
            "Nourriture (Food)" to "food", "Acier (Steel)" to "steel"
        )
        cases.forEach { (name, key) ->
            assertTrue("$name should match $key", ResourceNameMatcher.matchesWardenCost(name, key))
        }
    }

    @Test
    fun doesNotMatchUnknownOrEmptyResourceNames() {
        assertFalse(ResourceNameMatcher.matchesWardenCost("", "food"))
        assertFalse(ResourceNameMatcher.matchesWardenCost("Diamants", "food"))
        assertFalse(ResourceNameMatcher.matchesWardenCost("Nourriture", "unknown"))
    }
}
