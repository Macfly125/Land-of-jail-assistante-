package com.landsofjail.assistant.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceNameMatcherTest {
    @Test
    fun matchesResourceNamesInAllSupportedLanguages() {
        val cases = listOf(
            "Nourriture" to "food",
            "Food" to "food",
            "Cibo" to "food",
            "Bois" to "wood",
            "Wood" to "wood",
            "Legno" to "wood",
            "Acier" to "steel",
            "Steel" to "steel",
            "Acciaio" to "steel",
            "Essence" to "gasoline",
            "Gasoline" to "gasoline",
            "Benzina" to "gasoline"
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
