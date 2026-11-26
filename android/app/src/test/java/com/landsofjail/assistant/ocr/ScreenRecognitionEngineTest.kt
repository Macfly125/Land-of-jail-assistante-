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
    fun explicitCosmosHubWinsAndIsNotRepeatedAsAnAlternative() {
        val result = ScreenRecognitionEngine.analyze(
            "Satellite Launch Data Disk Planet Coin Cosmos Hub"
        )

        assertEquals("cosmos_hub", result.category)
        assertFalse(result.alternativeCategories.contains("cosmos_hub"))
        assertTrue(result.alternativeCategories.contains("satellites"))
    }

    @Test
    fun ambiguousFelonLabelRequiresVisualConfirmation() {
        val result = ScreenRecognitionEngine.analyze("Felon")

        assertTrue(result.category == "dungeon" || result.category == "arrest")
        assertTrue(result.alternativeCategories.isNotEmpty())
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
