package com.landsofjail.assistant.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LandsOfJailOcrParserTest {

    @Test
    fun heroCatalogHasUniqueNamesAndSeasonSevenPortrait() {
        val entries = com.landsofjail.assistant.model.HeroCatalog.entries
        assertEquals(50, entries.size)
        assertEquals(entries.size, entries.map { it.nom.lowercase() }.distinct().size)
        val franAndPike = entries.first { it.nom == "Fran & Pike" }
        assertTrue(franAndPike.portraitUrl?.contains("7d74a030adb8f3e8.webp") == true)
    }

    @Test
    fun recognizesEveryCanonicalHeroNameWithoutInventingPlayerStats() {
        val entries = com.landsofjail.assistant.model.HeroCatalog.entries
        assertEquals(50, entries.size)

        entries.forEach { entry ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(entry.nom)
            assertTrue(
                "Le héros du catalogue n'est pas reconnu : ${entry.nom}",
                result.heroes.any { it.nom == entry.nom }
            )
            val detected = result.heroes.first { it.nom == entry.nom }
            assertNull("Le niveau ne doit pas être inventé pour ${entry.nom}", detected.niveau)
            assertNull("Les étoiles ne doivent pas être inventées pour ${entry.nom}", detected.etoiles)
        }
    }

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
    fun detectsCatalogHeroesWithoutInventingPersonalLevelsOrStars() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Ada\nEdwin")

        assertEquals(2, result.heroes.size)
        val ada = result.heroes.first { it.nom == "Ada" }
        assertEquals("S5", ada.saison)
        assertEquals("Shooter", ada.typeTroupe)
        assertNull(ada.niveau)
        assertNull(ada.etoiles)
        assertTrue(ada.incertain)
    }

    @Test
    fun doesNotAssignTheNextHeroStarsToThePreviousHero() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Ada\nEdwin\n3 stars")

        val ada = result.heroes.first { it.nom == "Ada" }
        val edwin = result.heroes.first { it.nom == "Edwin" }
        assertNull(ada.etoiles)
        assertEquals("3/5", edwin.etoiles)
        assertTrue(ada.incertain)
    }

    @Test
    fun recognizesLocalizedAndSlightlyMisreadHeroNames() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Phénix\nCésar\nDuraga\nTyronn")

        assertEquals("Héros détectés : ${result.heroes.joinToString { it.nom }}", 4, result.heroes.size)
        assertTrue(result.heroes.any { it.nom == "Phoenix" })
        assertTrue(result.heroes.any { it.nom == "Caesar" })
        assertTrue(result.heroes.any { it.nom == "Durga" })
        assertTrue(result.heroes.any { it.nom == "Tyronn" })
    }

    @Test
    fun recognizesFrenchWikiHeroAliasesWithoutCreatingDuplicates() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Berry\nChuchotement\nVéronique\nCésar")

        assertEquals(4, result.heroes.size)
        assertTrue(result.heroes.any { it.nom == "Beary" })
        assertTrue(result.heroes.any { it.nom == "Whisper" })
        assertTrue(result.heroes.any { it.nom == "Veronica" })
        assertTrue(result.heroes.any { it.nom == "Caesar" })
    }

    @Test
    fun doesNotConfuseExactFrenchAliasWithSimilarHeroNames() {
        val berry = LandsOfJailOcrParser.parseLocalOcrText("Berry")
        assertEquals(listOf("Beary"), berry.heroes.map { it.nom })

        val terry = LandsOfJailOcrParser.parseLocalOcrText("Terry")
        assertEquals(listOf("Terry Bogard"), terry.heroes.map { it.nom })
    }

    @Test
    fun doesNotGuessShortHeroNamesFromSimilarWords() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Adda\nEddie")

        assertTrue(result.heroes.isEmpty())
    }

    @Test
    fun identifiesRobotScreensWithoutInventingRobotStats() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Robot\nUpgrade level")

        assertEquals("robots", result.detectedCategory)
        assertTrue(result.heroes.isEmpty())
        assertTrue(result.resources.isEmpty())
    }

    @Test
    fun emptyOcrDoesNotInventPlayerData() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("   \n ")

        assertNull(result.power)
        assertTrue(result.heroes.isEmpty())
        assertTrue(result.resources.isEmpty())
        assertEquals("ocr_local", result.detectedCategory)
        assertTrue(result.uncertainItems.isNotEmpty())
    }
    @Test
    fun recognizesBuildingScreensSeparatelyFromResources() {
        val result = ScreenRecognitionEngine.analyze(
            "Warden Office Level 18\\nUpgrade building\\nRequired resources"
        )
        assertEquals("batiments", result.category)
        assertTrue(result.confidence >= 0.8f)
        assertTrue(result.matchedLabels.isNotEmpty())
    }

    @Test
    fun recognizesTroopScreensAndRequestsVisualConfirmationWhenEvidenceIsWeak() {
        val result = ScreenRecognitionEngine.analyze("Shieldbearers")
        assertEquals("troupes", result.category)
        assertEquals(listOf("shieldbearers"), result.matchedLabels)
        assertTrue(result.confidence < 0.8f)
        assertTrue(result.needsVisualConfirmation)
    }

    @Test
    fun distinguishesHeroAndResourceScreensFromBuildingScreens() {
        assertEquals("heros", ScreenRecognitionEngine.analyze("Heroes\\nSkills\\nStars").category)
        assertEquals("ressources", ScreenRecognitionEngine.analyze("Food\\nWood\\nSteel").category)
    }

    @Test
    fun doesNotClaimVisualRecognitionFromOcrAlone() {
        val result = ScreenRecognitionEngine.analyze("Level 18")
        assertEquals("ocr_local", result.category)
        assertTrue(result.needsVisualConfirmation)
    }

}
