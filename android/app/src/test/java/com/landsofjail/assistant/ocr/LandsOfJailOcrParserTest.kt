package com.landsofjail.assistant.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LandsOfJailOcrParserTest {

    @Test
    fun parsesGroupedPowerWithoutTreatingThousandsAsDecimals() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Puissance : 8,450,120")

        assertNotNull(result.power)
        assertEquals(8_450_120L, result.power?.puissanceTotaleNumerique)
        assertFalse(result.power?.incertain ?: true)
    }

    @Test
    fun parsesCompactPowerSuffix() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Power: 12.4M")

        assertEquals(12_400_000L, result.power?.puissanceTotaleNumerique)
    }

    @Test
    fun identifiesTrialCageScreens() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Trial Cage\nDamage ranking")

        assertEquals("trial_cage", result.detectedCategory)
    }

    @Test
    fun extractsNamedResourcesAndKeepsCompactValuesExplicit() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Food: 1.2M\nWood: 850K")

        assertEquals(2, result.resources.size)
        assertTrue(result.resources.any { it.nom == "Nourriture (Food)" && it.quantiteNumerique == 1_200_000.0 })
        assertTrue(result.resources.any { it.nom == "Bois (Wood)" && it.quantiteNumerique == 850_000.0 })
    }

    @Test
    fun emptyOcrDoesNotInventPlayerData() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("   \n ")

        assertEquals(null, result.power)
        assertTrue(result.heroes.isEmpty())
        assertTrue(result.resources.isEmpty())
        assertEquals("ocr_local", result.detectedCategory)
        assertTrue(result.uncertainItems.isNotEmpty())
    }
}
