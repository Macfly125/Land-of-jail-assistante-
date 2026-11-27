package com.landsofjail.assistant.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenRecognitionEngineTest {
    @Test
    fun recognizesFrenchResourcePanelButKeepsHeuristicScoreExplicit() {
        val result = ScreenRecognitionEngine.analyze("Nourriture Bois Acier Essence")

        assertEquals("ressources", result.category)
        assertTrue(result.matchedLabels.isNotEmpty())
        assertTrue("Un score OCR heuristique ne vaut pas validation visuelle", result.needsVisualConfirmation)
        assertEquals(0.95f, result.confidence, 0.0001f)
    }

    @Test
    fun requiresVisualConfirmationEvenWhenSeveralResourceLabelsAgree() {
        val result = ScreenRecognitionEngine.analyze("Food Wood Steel Gasoline")

        assertEquals("ressources", result.category)
        assertEquals(0.95f, result.confidence, 0.0001f)
        assertTrue(result.needsVisualConfirmation)
    }

    @Test
    fun classifiesRobotCenterAsBuildingNotRobotProfile() {
        val result = ScreenRecognitionEngine.analyze("Robot Center\nLevel 12")

        assertEquals("batiments", result.category)
        assertTrue(result.needsVisualConfirmation)
    }

    @Test
    fun robotUpgradePanelIsNotMisclassifiedAsRobotProfile() {
        val result = ScreenRecognitionEngine.analyze("Robot Upgrade\nLevel 5")

        assertEquals("batiments", result.category)
        assertTrue(result.needsVisualConfirmation)
    }

    @Test
    fun recognizesRobotNameWithoutClaimingVisualIdentityOrLevel() {
        val result = ScreenRecognitionEngine.analyze("Yokozuna Level 10")

        assertEquals("robots", result.category)
        assertTrue(result.needsVisualConfirmation)
        assertTrue(result.confidence < 0.8f)
    }


    @Test
    fun recognizesShortLabHeaderWithResearchTechnology() {
        val result = ScreenRecognitionEngine.analyze("Lab\nLogistics Technology\nEconomy Technology")

        assertEquals("lab", result.category)
        assertTrue(result.matchedLabels.contains("lab"))
        assertTrue(result.needsVisualConfirmation)
    }

    @Test
    fun doesNotClassifyTheGenericWordLabWithoutResearchContext() {
        val result = ScreenRecognitionEngine.analyze("Lab")

        assertEquals("ocr_local", result.category)
        assertFalse(result.needsVisualConfirmation)
    }

    @Test
    fun doesNotClassifyAnUnqualifiedLaboratoryBuildingAsResearchLab() {
        val result = ScreenRecognitionEngine.analyze("Laboratory\nLevel 12")

        assertEquals("batiments", result.category)
    }

    @Test
    fun doesNotClassifyGenericWaveWithoutTurmoilContext() {
        val result = ScreenRecognitionEngine.analyze("Wave")

        assertEquals("ocr_local", result.category)
        assertFalse(result.needsVisualConfirmation)
    }

    @Test
    fun doesNotClassifyGenericFuelWithoutSandseaContext() {
        val result = ScreenRecognitionEngine.analyze("Fuel")

        assertEquals("ocr_local", result.category)
        assertFalse(result.needsVisualConfirmation)
    }

    @Test
    fun doesNotClassifyGenericUpgradeWithoutBuildingContext() {
        val result = ScreenRecognitionEngine.analyze("Upgrade")

        assertEquals("ocr_local", result.category)
        assertFalse(result.needsVisualConfirmation)
    }

    @Test
    fun explicitCosmosHubWinsAndIsNotRepeatedAsAnAlternative() {
        val result = ScreenRecognitionEngine.analyze(
            "Satellite Launch Data Disk Planet Coin Cosmos Hub"
        )

        assertEquals("cosmos_hub", result.category)
        assertFalse(result.alternativeCategories.contains("cosmos_hub"))
        assertTrue(result.alternativeCategories.contains("satellites"))
    }

    @Test
    fun ambiguousFelonLabelIsNotForcedIntoAnArbitraryCategory() {
        val result = ScreenRecognitionEngine.analyze("Felon")

        assertEquals("ocr_local", result.category)
        assertTrue(result.alternativeCategories.contains("dungeon"))
        assertTrue(result.alternativeCategories.contains("arrest"))
        assertTrue(result.needsVisualConfirmation)
        assertEquals(0.52f, result.confidence, 0.0001f)
    }

    @Test
    fun tiedDungeonAndArrestEvidenceRemainsUnclassified() {
        val result = ScreenRecognitionEngine.analyze("Felon\nCriminal Prisoner")

        assertEquals("ocr_local", result.category)
        assertTrue(result.alternativeCategories.contains("dungeon"))
        assertTrue(result.alternativeCategories.contains("arrest"))
        assertTrue(result.needsVisualConfirmation)
    }


    @Test
    fun allSharedFelonLabelsRemainAmbiguousAcrossLanguages() {
        listOf("Felons", "Criminal Prisoner", "罪犯").forEach { label ->
            val result = ScreenRecognitionEngine.analyze(label)

            assertEquals("ocr_local for $label", "ocr_local", result.category)
            assertTrue("dungeon candidate for $label", result.alternativeCategories.contains("dungeon"))
            assertTrue("arrest candidate for $label", result.alternativeCategories.contains("arrest"))
            assertTrue("visual confirmation for $label", result.needsVisualConfirmation)
        }
    }

    @Test
    fun recognizesRussianCyrillicCategoryLabelsWhenTextIsAlreadyAvailable() {
        val samples = listOf(
            "испытательная клетка" to "trial_cage",
            "кристальная битва" to "crystal_clash",
            "подготовка к войне" to "svs",
            "золотой охотник" to "gold_hunter",
            "технология логистики" to "lab",
            "подземелье" to "dungeon",
            "здание" to "batiments",
            "войска" to "troupes",
            "еда дерево сталь бензин алмазы золото" to "ressources",
            "спутники" to "satellites",
            "убежище особняк" to "haven_island",
            "технология альянса" to "alliance",
            "предмет инвентарь" to "items",
            "профиль мощь" to "profil"
        )

        samples.forEach { (text, expectedCategory) ->
            val result = ScreenRecognitionEngine.analyze(text)
            assertEquals("Mauvaise catégorie pour le libellé russe : $text", expectedCategory, result.category)
            assertTrue("La confirmation visuelle reste obligatoire : $text", result.needsVisualConfirmation)
        }
    }

    @Test
    fun explicitPowerArmorTitleWinsOverGenericProfilePowerLabel() {
        listOf("Power Armor", "Power Armor Hangar", "Prisoner Armor").forEach { sample ->
            val result = ScreenRecognitionEngine.analyze(sample)
            assertEquals("L'écran d'armure ne doit pas devenir un profil : $sample",
                "power_armor", result.category)
            assertTrue(result.needsVisualConfirmation)
        }
    }

    @Test
    fun playerProfilePowerRemainsClassifiedAsProfileWithoutArmorContext() {
        val result = ScreenRecognitionEngine.analyze("Player Profile\nPower 12M\nEliminations 350")
        assertEquals("profil", result.category)
        assertTrue(result.needsVisualConfirmation)
    }

    @Test
    fun unrelatedTextDoesNotProduceAGameScreenCategory() {
        val result = ScreenRecognitionEngine.analyze("Bonjour, test sans libellé de jeu")

        assertEquals("ocr_local", result.category)
        assertEquals(0f, result.confidence, 0.0001f)
        assertFalse("Un texte sans catégorie de jeu ne doit pas déclencher une confirmation visuelle", result.needsVisualConfirmation)
    }
}
