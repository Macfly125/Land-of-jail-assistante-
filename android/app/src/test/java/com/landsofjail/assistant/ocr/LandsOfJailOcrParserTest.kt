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
        assertEquals("Chaque héros doit avoir un portrait réel référencé", 50, entries.count { !it.portraitUrl.isNullOrBlank() })
        assertEquals("Les portraits ne doivent pas être réutilisés par erreur", 50, entries.mapNotNull { it.portraitUrl }.distinct().size)
        assertTrue(entries.first { it.nom == "Tormund" }.portraitUrl?.endsWith("e8e153e31561afad.webp") == true)
        assertTrue(entries.first { it.nom == "Lunarl" }.portraitUrl?.endsWith("852918d66fdf283d.webp") == true)
        assertTrue(entries.first { it.nom == "Sawyer" }.portraitUrl?.endsWith("37fbe6e7af9872d3.webp") == true)
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
            assertEquals("Le portrait du catalogue doit être transmis pour ${entry.nom}", entry.portraitUrl, detected.imageUrl)
            assertEquals("L'avatar doit reprendre le portrait du catalogue pour ${entry.nom}", entry.portraitUrl, detected.avatarUrl)
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
    fun recognizesResourceScreensInGermanTurkishRussianAndChinese() {
        val samples = listOf(
            "Nahrung\\nHolz\\nStahl",
            "Yiyecek\\nOdun\\nÇelik",
            "Еда\\nДерево\\nСталь",
            "食物\\n木材\\n钢材"
        )

        samples.forEach { sample ->
            val result = ScreenRecognitionEngine.analyze(sample)
            assertEquals("Capture de ressources non reconnue : $sample", "ressources", result.category)
            assertTrue("Aucun libellé localisé reconnu pour : $sample", result.matchedLabels.isNotEmpty())
        }
    }

    @Test
    fun recognizesChineseOcrWhenWordsHaveNoSpaces() {
        val heroes = ScreenRecognitionEngine.analyze("英雄技能星级")
        assertEquals("heros", heroes.category)
        assertTrue(heroes.confidence >= 0.8f)

        val resources = ScreenRecognitionEngine.analyze("食物木材钢材")
        assertEquals("ressources", resources.category)
        assertTrue(resources.confidence >= 0.8f)
    }

    @Test
    fun recognizesHeroScreensInGermanTurkishRussianAndChinese() {
        val samples = listOf(
            "Helden\\nFähigkeiten\\nSterne",
            "Kahramanlar\\nBeceriler\\nYıldızlar",
            "Герои\\nНавыки\\nЗвёзды",
            "英雄\\n技能\\n星级"
        )

        samples.forEach { sample ->
            val result = ScreenRecognitionEngine.analyze(sample)
            assertEquals("Capture de héros non reconnue : $sample", "heros", result.category)
            assertTrue("Aucun libellé localisé reconnu pour : $sample", result.matchedLabels.isNotEmpty())
        }
    }

    @Test
    fun recognizesGameEventScreensAcrossSupportedLanguages() {
        val samples = listOf(
            "Trial Cage\\nDamage dealt",
            "Prüfungskäfig\\nVerursachter Schaden",
            "Deneme Kafesi\\nVerilen hasar",
            "Испытательная клетка\\nНанесенный урон",
            "试炼笼\\n造成伤害"
        )
        samples.forEach { sample ->
            val result = ScreenRecognitionEngine.analyze(sample)
            assertEquals("Événement Trial Cage non reconnu : $sample", "trial_cage", result.category)
            assertTrue(result.matchedLabels.isNotEmpty())
        }
    }

    @Test
    fun doesNotClaimVisualRecognitionFromOcrAlone() {
        val result = ScreenRecognitionEngine.analyze("Level 18")
        assertEquals("ocr_local", result.category)
        assertTrue(result.needsVisualConfirmation)
    }


    @Test
    fun parsesLocalizedResourceNamesAcrossAllSupportedLanguages() {
        val samples = listOf(
            "Nahrung: 1200\\nHolz: 2300\\nStahl: 3400",
            "Yiyecek: 1200\\nOdun: 2300\\nÇelik: 3400",
            "Еда: 1200\\nДерево: 2300\\nСталь: 3400",
            "食物：1200\\n木材：2300\\n钢材：3400"
        )
        samples.forEach { sample ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(sample)
            assertEquals("Les trois ressources doivent être lues : $sample", 3, result.resources.size)
            assertTrue(result.resources.all { it.quantiteNumerique != null })
        }
    }

    @Test
    fun readsResourceQuantitiesOnTheLineAfterTheirLabels() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            """Nahrung
1200
Holz
2300
Stahl
3400"""
        )
        assertEquals(3, result.resources.size)
        assertTrue(result.resources.any { it.nom == "Nourriture (Food)" && it.quantiteNumerique == 1200.0 })
        assertTrue(result.resources.any { it.nom == "Bois (Wood)" && it.quantiteNumerique == 2300.0 })
        assertTrue(result.resources.any { it.nom == "Acier (Steel)" && it.quantiteNumerique == 3400.0 })
    }

    @Test
    fun extractsKnownRobotNameAndExplicitLevelWithoutGuessingStats() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Ranger\\nLevel 25")
        assertEquals("robots", result.detectedCategory)
        assertEquals(1, result.robots.size)
        assertEquals("Ranger", result.robots.single().nom)
        assertEquals("25", result.robots.single().niveau)
        assertTrue(result.robots.single().bonus.isBlank())
        assertTrue(result.robots.single().competences.isBlank())
    }

    @Test
    fun readsResourceQuantityAfterAnOcrSymbolLine() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            """Food
🍖
12500"""
        )
        assertEquals(1, result.resources.size)
        assertEquals("Nourriture (Food)", result.resources.single().nom)
        assertEquals(12500.0, result.resources.single().quantiteNumerique ?: 0.0, 0.1)
    }

    @Test
    fun recognizesRobotNameSplitAcrossTwoOcrLines() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            """Phantom
Cat
Level 12"""
        )
        assertEquals(1, result.robots.size)
        assertEquals("Phantom Cat", result.robots.single().nom)
        assertEquals("12", result.robots.single().niveau)
    }

    @Test
    fun recognizesHeroEquipmentUpgradeScreensInSupportedLanguages() {
        val samples = listOf(
            """Hero Equipment
Equipment Upgrade
Magnet""",
            """Heldenausrüstung
Ausrüstungsverbesserung
Magnet""",
            """Kahraman ekipmanı
Ekipman geliştirme
Mıknatıs""",
            """Снаряжение героя
Улучшение снаряжения
Магнит""",
            """英雄装备
装备升级
磁铁""",
            """Équipement du héros
Amélioration d'équipement
Aimant"""
        )
        samples.forEach { sample ->
            val result = ScreenRecognitionEngine.analyze(sample)
            assertEquals("Écran d'amélioration d'équipement non reconnu : $sample", "equipements", result.category)
            assertTrue(result.matchedLabels.isNotEmpty())
        }
    }

    @Test
    fun extractsEquipmentPieceAndExplicitUpgradeLevelWithoutGuessingQuality() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            """Hero Equipment
Equipment Upgrade
Weapon
Level 18
Magnet"""
        )
        assertEquals("equipements", result.detectedCategory)
        assertEquals(1, result.equipment.size)
        assertEquals("Arme", result.equipment.single().piece)
        assertEquals("18", result.equipment.single().niveau)
        assertTrue(result.equipment.single().qualite.isBlank())
        assertTrue(result.equipment.single().materiaux.isNotBlank())
        assertFalse(result.equipment.single().incertain)
    }

    @Test
    fun extractsEquipmentPieceAndLevelAcrossAllSupportedLanguages() {
        val samples = listOf(
            """Weapon
Level 18
Magnet""",
            """Waffe
Stufe 18
Magnet""",
            """Silah
Seviye 18
Mıknatıs""",
            """Оружие
Уровень 18
Магнит""",
            """武器
等级 18
磁铁""",
            """Arme
Niv. 18
Aimant"""
        )

        samples.forEach { sample ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(sample)
            assertEquals("La pièce d'équipement doit être extraite : $sample", 1, result.equipment.size)
            assertEquals("18", result.equipment.single().niveau)
            assertFalse(result.equipment.single().incertain)
            assertTrue(result.equipment.single().materiaux.isNotBlank())
        }
    }

    @Test
    fun doesNotMistakeFrenchConjunctionOrForTheGoldResource() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Or il faut encore de la nourriture : 5M")
        assertTrue(result.resources.none { it.nom == "Cash / Or" })
        assertTrue(result.resources.any { it.nom == "Nourriture (Food)" })
    }

    @Test
    fun recognizesExplicitFrenchGoldLabelAndMarksBillionSuffixAsCompact() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Or: 2B")
        val gold = result.resources.single { it.nom == "Cash / Or" }
        assertEquals(2_000_000_000.0, gold.quantiteNumerique ?: 0.0, 0.1)
        assertTrue(gold.incertain)
    }
}
