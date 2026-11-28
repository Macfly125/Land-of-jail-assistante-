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
    fun addsOnlyConfirmedRobotPortraitMatchesWithoutInventingDetails() {
        val confirmed = RobotPortraitMatch(
            robotId = "phantom_cat",
            displayName = "Phantom Cat",
            similarity = 0.97f,
            runnerUpSimilarity = 0.81f,
            isConfirmed = true,
            reason = "portrait_correspondance_confirmee"
        )
        val ambiguous = RobotPortraitMatch(
            robotId = null,
            displayName = null,
            similarity = 0.95f,
            runnerUpSimilarity = 0.93f,
            isConfirmed = false,
            reason = "correspondances_trop_proches"
        )

        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "Robot Center",
            visualRobotMatches = listOf(confirmed, ambiguous)
        )

        assertEquals(1, result.robots.size)
        val robot = result.robots.single()
        assertEquals("phantom_cat", robot.id)
        assertEquals("Phantom Cat", robot.nom)
        assertEquals("portrait_local", robot.source)
        assertTrue(robot.incertain)
        assertTrue(robot.niveau.isBlank())
        assertTrue(robot.bonus.isBlank())
        assertTrue(robot.competences.isBlank())
    }

    @Test
    fun doesNotDuplicateRobotAlreadyRecognizedByOcrAndPortrait() {
        val match = RobotPortraitMatch(
            robotId = "phantom_cat",
            displayName = "Phantom Cat",
            similarity = 0.99f,
            runnerUpSimilarity = 0.70f,
            isConfirmed = true,
            reason = "portrait_correspondance_confirmee"
        )
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "Phantom Cat",
            visualRobotMatches = listOf(match)
        )
        assertEquals(1, result.robots.size)
        assertEquals("ocr_local", result.robots.single().source)
    }

    @Test
    fun recognizesPlayerProfilePowerBadgeAboveEliminationsWithoutTakingEliminationCount() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "Eugène\nID: 1046814571\n504.5M\nÉliminations · 2M\nAlliance: YRo\nServeur: 311",
            textBoxes = listOf(
                OcrTextBox("ID: 1046814571", 430, 970, 690, 1010),
                OcrTextBox("504.5M", 490, 1030, 675, 1080),
                OcrTextBox("Éliminations", 430, 1084, 640, 1120),
                OcrTextBox("2M", 690, 1084, 760, 1120),
                OcrTextBox("Alliance: YRo", 430, 1140, 760, 1180),
                OcrTextBox("Serveur: 311", 430, 1190, 650, 1230)
            ),
            imageWidth = 1152,
            imageHeight = 1536
        )

        assertEquals("504.5M", result.power?.puissanceTotaleTexte)
        assertEquals(504_500_000L, result.power?.puissanceTotaleNumerique)
        assertFalse(result.power?.incertain ?: true)
    }

    @Test
    fun recognizesProfilePowerBadgeEvenWhenEliminationsLabelIsNotReadButProfileMetadataIs() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "504.5M\nAlliance: YRo\nServeur: 311",
            textBoxes = listOf(
                OcrTextBox("ID: 1046814571", 430, 970, 690, 1010),
                OcrTextBox("504.5M", 490, 1030, 675, 1080),
                OcrTextBox("Éliminations", 430, 1084, 640, 1120),
                OcrTextBox("Alliance: YRo", 430, 1140, 760, 1180),
                OcrTextBox("Serveur: 311", 430, 1190, 650, 1230)
            ),
            imageWidth = 1152,
            imageHeight = 1536
        )

        assertEquals(504_500_000L, result.power?.puissanceTotaleNumerique)
    }

    @Test
    fun doesNotInferPlayerPowerFromEliminationsWithoutPlayerProfileAnchors() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "504.5M\nÉliminations · 2M",
            textBoxes = listOf(
                OcrTextBox("504.5M", 490, 1030, 675, 1080),
                OcrTextBox("Éliminations", 430, 1084, 640, 1120),
                OcrTextBox("2M", 690, 1084, 760, 1120)
            ),
            imageWidth = 1152,
            imageHeight = 1536
        )

        assertNull(result.power?.puissanceTotaleNumerique)
    }

    @Test
    fun recognizesLocalizedProfilePowerBadgeWithRussianProfileLabels() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "ID: 1046814571\n504.5M\nУбийства · 2M\nАльянс: YRo\nСервер: 311",
            textBoxes = listOf(
                OcrTextBox("ID: 1046814571", 430, 970, 690, 1010),
                OcrTextBox("504.5M", 490, 1030, 675, 1080),
                OcrTextBox("Убийства", 430, 1084, 640, 1120),
                OcrTextBox("2M", 690, 1084, 760, 1120),
                OcrTextBox("Альянс: YRo", 430, 1140, 760, 1180),
                OcrTextBox("Сервер: 311", 430, 1190, 650, 1230)
            ),
            imageWidth = 1152,
            imageHeight = 1536
        )

        assertEquals("504.5M", result.power?.puissanceTotaleTexte)
        assertEquals(504_500_000L, result.power?.puissanceTotaleNumerique)
        assertFalse(result.power?.incertain ?: true)
    }

    @Test
    fun recognizesLocalizedTextOnlyProfilePowerWithKSuffix() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "ID: 1046814571\n730K\nУбийства · 2M\nАльянс: YRo\nСервер: 311"
        )

        assertEquals("730K", result.power?.puissanceTotaleTexte)
        assertEquals(730_000L, result.power?.puissanceTotaleNumerique)
    }

    @Test
    fun recognizesHomeScreenPowerBadgeByItsPositionWithoutReadingFoodAsPower() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "VIP8",
            textBoxes = listOf(
                OcrTextBox("509 256 511", 18, 166, 203, 215),
                OcrTextBox("177.1M", 238, 122, 353, 158)
            ),
            imageWidth = 698,
            imageHeight = 1536
        )

        assertEquals("509 256 511", result.power?.puissanceTotaleTexte)
        assertEquals(509_256_511L, result.power?.puissanceTotaleNumerique)
        assertFalse(result.power?.incertain ?: true)
        assertTrue("La nourriture ne doit pas devenir une puissance", result.resources.isEmpty())
    }

    @Test
    fun separatesHomePowerFromFoodCounterAcrossScreenshotResolutions() {
        val baseWidth = 698
        val baseHeight = 1536
        val baseBoxes = listOf(
            OcrTextBox("509 256 511", 18, 166, 203, 215),
            OcrTextBox("177.1M", 238, 122, 353, 158)
        )

        listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { scale ->
            val scaledBoxes = baseBoxes.map { box ->
                OcrTextBox(
                    text = box.text,
                    left = (box.left * scale).toInt(),
                    top = (box.top * scale).toInt(),
                    right = (box.right * scale).toInt(),
                    bottom = (box.bottom * scale).toInt()
                )
            }
            val result = LandsOfJailOcrParser.parseLocalOcrText(
                rawText = "VIP8",
                textBoxes = scaledBoxes,
                imageWidth = (baseWidth * scale).toInt(),
                imageHeight = (baseHeight * scale).toInt()
            )

            assertEquals("La puissance doit rester distincte à l'échelle $scale", 509_256_511L,
                result.power?.puissanceTotaleNumerique)
            assertEquals("La nourriture ne doit jamais devenir la puissance à l'échelle $scale",
                "509 256 511", result.power?.puissanceTotaleTexte)
            assertTrue("La valeur de nourriture ne doit pas être inventée comme stock sans libellé à l'échelle $scale",
                result.resources.isEmpty())
        }
    }

    @Test
    fun doesNotTreatTopResourceCounterAsHomeScreenPower() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "VIP8",
            textBoxes = listOf(OcrTextBox("177.1M", 238, 122, 353, 158)),
            imageWidth = 698,
            imageHeight = 1536
        )

        assertNull(result.power)
    }

    @Test
    fun extractsForticladHeadquartersLevelOnlyWhenExplicitlyLabeled() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("QG Forticlad\nNiveau 6")

        assertEquals("Niv. 6", result.power?.niveauPrisonOuQG)
        assertEquals("Non mesurée", result.power?.puissanceTotaleTexte)
    }

    @Test
    fun recognizesHeadquartersLevelWhenLevelLabelPrecedesBuildingName() {
        val samples = listOf(
            "Lv. 12 Warden's Office" to "Niv. 12",
            "Level 18 Director's Office" to "Niv. 18",
            "Livello 7 Ufficio del direttore" to "Niv. 7",
            "Уровень 21 офис начальника" to "Niv. 21",
            "等级 9 典狱长办公室" to "Niv. 9"
        )

        samples.forEach { (sample, expected) ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(sample)
            assertEquals("Le niveau du QG doit être extrait : $sample",
                expected, result.power?.niveauPrisonOuQG)
            assertEquals("La puissance ne doit pas être inventée : $sample",
                "Non mesurée", result.power?.puissanceTotaleTexte)
        }
    }

    @Test
    fun doesNotUseAnotherBuildingLevelAsHeadquartersLevel() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "Level 12 Kitchen\\nLevel 8 Warden's Office"
        )

        assertEquals("Niv. 8", result.power?.niveauPrisonOuQG)
    }

    @Test
    fun doesNotTreatPrisonerTextAsTheHeadquartersBuildingName() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Prisoner Lv. 6")

        assertNull(result.power?.niveauPrisonOuQG)
    }

    @Test
    fun parsesGroupedPowerWithoutTreatingThousandsAsDecimals() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Puissance : 8,450,120")

        assertNotNull(result.power)
        assertEquals(8_450_120L, result.power?.puissanceTotaleNumerique)
        assertFalse(result.power?.incertain ?: true)
    }

    @Test
    fun requiresAnExactPowerLabelAndDoesNotMatchWordsContainingPower() {
        val samples = listOf(
            "Powerful Prisoner\nFood: 12.4M",
            "Superpower\nWood: 8.2M",
            "Empowerment\nSteel: 3.1M",
            "CPR: 450"
        )
        samples.forEach { sample ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(sample)
            assertNull("Un mot contenant seulement power/cp ne doit pas créer une puissance : $sample", result.power)
        }
    }

    @Test
    fun acceptsExactLocalizedPowerLabelsBesideResourceCounters() {
        val samples = listOf(
            "Puissance: 8,450,120\nFood: 12.4M" to 8_450_120L,
            "Power: 8.6M\nFood: 12.4M" to 8_600_000L,
            "Comb. Power: 7.2M\nWood: 4.1M" to 7_200_000L,
            "CP: 950000\nSteel: 3.1M" to 950_000L
        )
        samples.forEach { (sample, expected) ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(sample)
            assertEquals("La puissance explicite doit rester distincte : $sample", expected, result.power?.puissanceTotaleNumerique)
            assertTrue(result.resources.isNotEmpty())
        }
    }

    @Test
    fun acceptsLocalizedPowerLabelsAcrossSupportedGameLanguages() {
        val samples = listOf(
            "Macht: 8.6M\nFood: 12.4M" to 8_600_000L,
            "Güç: 7.2M\nWood: 4.1M" to 7_200_000L,
            "мощь: 950000\nSteel: 3.1M" to 950_000L,
            "战力: 12M\nFood: 1200" to 12_000_000L
        )
        samples.forEach { (sample, expected) ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(sample)
            assertEquals("Le libellé de puissance localisé doit être reconnu : $sample",
                expected, result.power?.puissanceTotaleNumerique)
            assertTrue("La ressource ne doit pas être absorbée par la puissance : $sample",
                result.resources.isNotEmpty())
        }
    }

    @Test
    fun parsesCompactPowerSuffix() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Power: 12.4M")

        assertEquals(12_400_000L, result.power?.puissanceTotaleNumerique)
    }

    @Test
    fun rejectsMalformedNumericSeparatorsInsteadOfSilentlyRemovingThem() {
        val malformed = listOf("1.2.3", "12,34,56", "1,23.456", "1..234", "1,234.5.6M")
        malformed.forEach { value ->
            val result = LandsOfJailOcrParser.parseLocalOcrText("Food: $value")
            assertTrue("Une valeur mal formée ne doit pas créer un stock fiable : $value",
                result.resources.isEmpty() || result.resources.single().quantiteNumerique == null)
        }
    }

    @Test
    fun skipsMalformedResourceValueAndStillAcceptsLaterValidValue() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            """Food: 1.2.3
Food: 1200"""
        )

        val food = result.resources.single { it.nom == "Nourriture (Food)" }
        assertEquals("1200", food.quantiteTexte)
        assertEquals(1200.0, food.quantiteNumerique ?: 0.0, 0.1)
    }

    @Test
    fun doesNotCreateResourceRecordsForMalformedQuantities() {
        val samples = listOf(
            "Food: 1.2.3",
            "Wood: 12,34,56",
            "Steel: 1,23.456",
            "Gasoline: 1..234"
        )
        samples.forEach { sample ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(sample)
            assertTrue("Une quantité invalide ne doit pas créer de ressource : $sample",
                result.resources.isEmpty())
        }
    }

    @Test
    fun acceptsValidGroupedAndDecimalResourceNumbersWithoutSuffix() {
        val samples = listOf(
            "Food: 8,450,120" to 8_450_120.0,
            "Wood: 8.450.120" to 8_450_120.0,
            "Steel: 1 234 567" to 1_234_567.0,
            "Gasoline: 1234,5" to 1234.5
        )
        samples.forEach { (sample, expected) ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(sample)
            assertEquals(sample, expected,
                result.resources.singleOrNull()?.quantiteNumerique ?: Double.NaN, 0.1)
        }
    }

    @Test
    fun parsesMixedLocaleSeparatorsWithBothDecimalConventions() {
        val us = LandsOfJailOcrParser.parseLocalOcrText("Food: 1,234.5M")
        val european = LandsOfJailOcrParser.parseLocalOcrText("Wood: 1.234,5M")

        assertEquals(1_234_500_000.0,
            us.resources.single { it.nom == "Nourriture (Food)" }.quantiteNumerique ?: 0.0, 0.1)
        assertEquals(1_234_500_000.0,
            european.resources.single { it.nom == "Bois (Wood)" }.quantiteNumerique ?: 0.0, 0.1)
    }

    @Test
    fun parsesMixedLocaleSeparatorsForExplicitPlayerPower() {
        val us = LandsOfJailOcrParser.parseLocalOcrText("Power: 1,234.5M")
        val european = LandsOfJailOcrParser.parseLocalOcrText("Puissance: 1.234,5M")

        assertEquals(1_234_500_000L, us.power?.puissanceTotaleNumerique)
        assertEquals(1_234_500_000L, european.power?.puissanceTotaleNumerique)
    }

    @Test
    fun recognizesGorillaLegionEventAcrossAllSupportedLanguages() {
        val samples = listOf(
            "Gorilla Legion\nGorilla Warlord",
            "Légion de Gorilla\nGorille sage",
            "Gorilla-Legion\nGorilla-Kriegsherr",
            "Goril Lejyonu\nGoril Savaş Lordu",
            "Легион горилл\nГорилла военачальник",
            "大猩猩军团\n猩猩军团"
        )
        samples.forEach { sample ->
            val result = ScreenRecognitionEngine.analyze(sample)
            assertEquals("Événement Gorilla non reconnu : $sample", "gorilla_legion", result.category)
            assertTrue(result.matchedLabels.isNotEmpty())
        }
    }

    @Test
    fun gorillaLegionIsAvailableAsAnEventAdvisorInAllSixLanguages() {
        val mode = com.landsofjail.assistant.model.AdviceMode.GORILLA_LEGION
        assertEquals("gorilla_legion", mode.apiId)
        listOf(
            com.landsofjail.assistant.model.AppLanguage.FR,
            com.landsofjail.assistant.model.AppLanguage.EN,
            com.landsofjail.assistant.model.AppLanguage.DE,
            com.landsofjail.assistant.model.AppLanguage.TR,
            com.landsofjail.assistant.model.AppLanguage.RU,
            com.landsofjail.assistant.model.AppLanguage.ZH
        ).forEach { language ->
            assertTrue(mode.titleFor(language).isNotBlank())
            assertTrue(mode.briefFor(language).isNotBlank())
        }
    }

    @Test
    fun identifiesTrialCageScreens() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Trial Cage\nDamage ranking")

        assertEquals("trial_cage", result.detectedCategory)
    }

    @Test
    fun extractsAllResourceTypesShownInPlayerResourceStatistics() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            """
            Argent liquide: 1.0B
            Nourriture: 9.2B
            Bois: 6.6B
            Acier: 2.0B
            Essence: 1.1B
            Carte en or: 21.1M
            Endurance: 1 752
            EXP d'équipement: 53.1K
            XP de héros: 121.0M
            """.trimIndent()
        )

        val expected = mapOf(
            "Cash / Or" to 1_000_000_000.0,
            "Nourriture (Food)" to 9_200_000_000.0,
            "Bois (Wood)" to 6_600_000_000.0,
            "Acier (Steel)" to 2_000_000_000.0,
            "Essence (Gasoline)" to 1_100_000_000.0,
            "Carte en or (Gold Card)" to 21_100_000.0,
            "Endurance (Stamina)" to 1_752.0,
            "EXP d'équipement (Equipment EXP)" to 53_100.0,
            "XP de héros (Hero XP)" to 121_000_000.0
        )
        expected.forEach { (name, value) ->
            val resource = result.resources.singleOrNull { it.nom == name }
            assertNotNull("Ressource manquante ou dupliquée : $name", resource)
            assertEquals(name, value, resource!!.quantiteNumerique ?: Double.NaN, 0.1)
        }
        assertEquals("Le tableau contient neuf catégories distinctes", 9, result.resources.size)
    }

    @Test
    fun readsBothStatisticsColumnsWithoutShiftingMissingValues() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            """
            Ressources Total des objets Ressources totales
            Argent liquide 1.0B 1.1B
            Nourriture 9.2B
            Bois 6.6B 7.0B
            Acier 2.0B 2.0B
            """.trimIndent()
        )
        assertEquals(3, result.resourceStatsRows.size)
        val wood = result.resourceStatsRows.single { it.nom == "Bois (Wood)" }
        assertEquals("6.6B", wood.totalObjetsTexte)
        assertEquals("7.0B", wood.ressourcesTotalesTexte)
        assertEquals(6_600_000_000.0, wood.totalObjetsNumerique!!, 0.1)
        assertEquals(7_000_000_000.0, wood.ressourcesTotalesNumerique!!, 0.1)
        assertFalse("Une ligne à une seule valeur ne doit pas décaler les colonnes", result.resourceStatsRows.any { it.nom == "Nourriture (Food)" })
    }

    @Test
    fun alignsStatisticsValuesUsingOcrColumnCoordinates() {
        val boxes = listOf(
            OcrTextBox("Total des objets", 140, 10, 250, 35),
            OcrTextBox("Ressources totales", 285, 10, 420, 35),
            OcrTextBox("Bois", 20, 90, 90, 120),
            OcrTextBox("6.6B", 155, 90, 210, 120),
            OcrTextBox("7.0B", 310, 90, 365, 120)
        )
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "Total des objets Ressources totales Bois",
            textBoxes = boxes,
            imageWidth = 450,
            imageHeight = 160
        )
        val wood = result.resourceStatsRows.single { it.nom == "Bois (Wood)" }
        assertEquals("6.6B", wood.totalObjetsTexte)
        assertEquals("7.0B", wood.ressourcesTotalesTexte)
    }

    @Test
    fun doesNotParseTwoColumnStatsWithoutBothHeaders() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Bois 6.6B 7.0B")
        assertTrue(result.resourceStatsRows.isEmpty())
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
    fun recognizesAllFourteenDocumentedRobotNamesWithoutInventingLevels() {
        val names = listOf(
            "Light Cone" to "Light Cone", "Hercules" to "Hercules α",
            "Pluto" to "Pluto", "Ranger" to "Ranger", "Infercore" to "Infercore",
            "Atlax" to "Atlax", "Phantom Cat" to "Phantom Cat", "Halo" to "Halo",
            "Yokozuna" to "Yokozuna", "Musashimaru" to "Musashimaru",
            "Bastion" to "Bastion", "Dauntless" to "Dauntless",
            "Valiant" to "Valiant", "Scorpio" to "Scorpio"
        )
        names.forEach { (name, expectedName) ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(name)
            assertTrue("Robot non reconnu : $name", result.robots.any { it.nom.equals(expectedName, true) })
            val robot = result.robots.first { it.nom.equals(expectedName, true) }
            assertTrue("Niveau inventé pour $name", robot.niveau.isBlank())
            assertTrue("La fiche sans niveau doit rester incertaine : $name", robot.incertain)
        }
    }

    @Test
    fun classifiesAllDocumentedRobotNamesAsRobotScreens() {
        listOf(
            "Light Cone", "Hercules", "Pluto", "Ranger", "Infercore", "Atlax",
            "Phantom Cat", "Halo", "Yokozuna", "Musashimaru", "Bastion",
            "Dauntless", "Valiant", "Scorpio"
        ).forEach { name ->
            assertEquals(
                "Mauvaise catégorie pour $name",
                "robots",
                ScreenRecognitionEngine.analyze(name).category
            )
        }
    }

    @Test
    fun identifiesNamedRobotScreensWithoutInventingRobotStats() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Yokozuna\nUpgrade level")

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
    fun recognizesWardenAndDirectorsOfficeLevelsWithApostrophesAndLocalizedLevelLabels() {
        val samples = listOf(
            "Warden's Office\nLevel 18\nPower: 12,345" to "Niv. 18",
            "Warden’s Office Lv. 21\nPower: 12,345" to "Niv. 21",
            "Director's Office\nLvl 16\nPower: 12,345" to "Niv. 16",
            "Bureau du directeur\nNiv. 14\nPuissance: 12,345" to "Niv. 14",
            "QG\nNiveau 9\nPuissance: 8,500" to "Niv. 9",
            "HQ 12\nPower: 15,000" to "Niv. 12"
        )
        samples.forEach { (sample, expectedLevel) ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(sample)
            assertEquals("Le niveau du bâtiment doit être exact : $sample",
                expectedLevel, result.power?.niveauPrisonOuQG)
        }
        assertEquals(
            "Niv. 18",
            LandsOfJailOcrParser.parseLocalOcrText("Warden's Office Level 18\nPower: 12,345")
                .power?.niveauPrisonOuQG
        )
    }

    @Test
    fun classifiesFrenchAndEnglishHeadquartersLabelsAsBuildingScreens() {
        val samples = listOf(
            "QG",
            "QG\nNiveau 9",
            "HQ",
            "HQ 12",
            "Prison"
        )
        samples.forEach { sample ->
            val result = ScreenRecognitionEngine.analyze(sample)
            assertEquals("Le QG / HQ doit être classé comme bâtiment : $sample", "batiments", result.category)
            assertTrue(result.needsVisualConfirmation)
        }
    }

    @Test
    fun extractsHeadquartersLevelEvenWhenPowerIsNotVisible() {
        val samples = listOf(
            "QG\nNiveau 9" to "Niv. 9",
            "HQ\nLevel 12" to "Niv. 12",
            "Prison\nNiv. 7" to "Niv. 7"
        )
        samples.forEach { (sample, expected) ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(sample)
            assertEquals("Le niveau du QG doit être conservé sans puissance : $sample",
                expected, result.power?.niveauPrisonOuQG)
            assertEquals("Non mesurée", result.power?.puissanceTotaleTexte)
        }
    }

    @Test
    fun classifiesWardenOfficeWithApostropheAsBuildingScreen() {
        listOf("Warden's Office", "Warden’s Office", "Director's Office").forEach { label ->
            val result = ScreenRecognitionEngine.analyze(label)
            assertEquals("Le Bureau du directeur doit être classé comme bâtiment : $label", "batiments", result.category)
        }
    }

    @Test
    fun recognizesBuildingScreensSeparatelyFromResources() {
        val result = ScreenRecognitionEngine.analyze(
            "Warden Office Level 18\nUpgrade building\nRequired resources"
        )
        assertEquals("batiments", result.category)
        assertTrue(result.confidence >= 0.8f)
        assertTrue(result.matchedLabels.isNotEmpty())
    }

    @Test
    fun nestedRobotBuildingLabelsDoNotArtificiallyIncreaseScreenConfidence() {
        val singlePhrase = ScreenRecognitionEngine.analyze("Robot Center")

        assertEquals("batiments", singlePhrase.category)
        assertEquals(listOf("robot center"), singlePhrase.matchedLabels)
        assertTrue(singlePhrase.confidence < 0.8f)
        assertTrue(singlePhrase.needsVisualConfirmation)

        val independentPhrases = ScreenRecognitionEngine.analyze("Robot Center\nRobot Upgrade")
        assertEquals("batiments", independentPhrases.category)
        assertEquals(listOf("robot center", "robot upgrade"), independentPhrases.matchedLabels)
        assertTrue(independentPhrases.confidence >= 0.8f)
        assertTrue(independentPhrases.needsVisualConfirmation)
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
        assertEquals("heros", ScreenRecognitionEngine.analyze("Heroes\nSkills\nStars").category)
        assertEquals("ressources", ScreenRecognitionEngine.analyze("Food\nWood\nSteel").category)
    }

    @Test
    fun recognizesResourceScreensInGermanTurkishRussianAndChinese() {
        val samples = listOf(
            "Nahrung\nHolz\nStahl",
            "Yiyecek\nOdun\nÇelik",
            "Еда\nДерево\nСталь",
            "食物\n木材\n钢材"
        )

        samples.forEach { sample ->
            val result = ScreenRecognitionEngine.analyze(sample)
            assertEquals("Capture de ressources non reconnue : $sample", "ressources", result.category)
            assertTrue("Aucun libellé localisé reconnu pour : $sample", result.matchedLabels.isNotEmpty())
        }
    }

    @Test
    fun classifiesGoldOnlyResourceScreensAcrossLanguages() {
        val samples = listOf("Gold", "Altın", "Золото", "金币", "黄金")
        samples.forEach { sample ->
            val result = ScreenRecognitionEngine.analyze(sample)
            assertEquals("L'écran de ressource or doit être reconnu : $sample", "ressources", result.category)
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
            "Helden\nFähigkeiten\nSterne",
            "Kahramanlar\nBeceriler\nYıldızlar",
            "Герои\nНавыки\nЗвёзды",
            "英雄\n技能\n星级"
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
            "Trial Cage\nDamage dealt",
            "Prüfungskäfig\nVerursachter Schaden",
            "Deneme Kafesi\nVerilen hasar",
            "Испытательная клетка\nНанесенный урон",
            "试炼笼\n造成伤害"
        )
        samples.forEach { sample ->
            val result = ScreenRecognitionEngine.analyze(sample)
            assertEquals("Événement Trial Cage non reconnu : $sample", "trial_cage", result.category)
            assertTrue(result.matchedLabels.isNotEmpty())
        }
    }

    @Test
    fun classifiesNewDevelopmentScreensWithoutConfusingTheirCategories() {
        val cases = listOf(
            "Power Armor Hangar\nSolo Module" to "power_armor",
            "Forticlad Lab\nHyperalloy" to "forticlad_lab",
            "Logistics Technology\nEconomy Technology" to "lab",
            "Cosmos Hub\nSatellite\nData Disk" to "cosmos_hub",
            "Dungeon Office\nSurveillance Room\nFelons" to "dungeon"
        )
        cases.forEach { (sample, expected) ->
            assertEquals("Mauvaise catégorie pour : $sample", expected, ScreenRecognitionEngine.analyze(sample).category)
        }
    }

    @Test
    fun doesNotClaimVisualRecognitionFromOcrAlone() {
        val result = ScreenRecognitionEngine.analyze("Level 18")
        assertEquals("ocr_local", result.category)
        assertFalse("Sans catégorie détectée, aucune confirmation visuelle ne peut être demandée", result.needsVisualConfirmation)
    }


    @Test
    fun parsesLocalizedResourceNamesAcrossAllSupportedLanguages() {
        val samples = listOf(
            "Nahrung: 1200\nHolz: 2300\nStahl: 3400",
            "Yiyecek: 1200\nOdun: 2300\nÇelik: 3400",
            "Еда: 1200\nДерево: 2300\nСталь: 3400",
            "食物：1200\n木材：2300\n钢材：3400"
        )
        samples.forEach { sample ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(sample)
            assertEquals("Les trois ressources doivent être lues : $sample", 3, result.resources.size)
            assertTrue(result.resources.all { it.quantiteNumerique != null })
        }
    }

    @Test
    fun doesNotMistakeNamedTopBarResourceAmountsForPlayerPower() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "Food: 12.4M\nWood: 8.2M\nSteel: 3.1M"
        )

        assertEquals(3, result.resources.size)
        assertTrue(result.resources.any { it.nom == "Nourriture (Food)" })
        assertTrue(result.resources.any { it.nom == "Bois (Wood)" })
        assertTrue(result.resources.any { it.nom == "Acier (Steel)" })
        assertNull(result.power)
    }

    @Test
    fun doesNotReuseFoodQuantityAsPlayerPowerBeforeEliminations() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "Food: 12.4M\nEliminations: 350"
        )

        assertEquals(1, result.resources.size)
        assertEquals("Nourriture (Food)", result.resources.single().nom)
        assertEquals(12_400_000.0, result.resources.single().quantiteNumerique ?: 0.0, 0.1)
        assertNull("Une quantité Food ne doit jamais devenir la puissance", result.power)
    }

    @Test
    fun keepsExplicitPowerSeparateFromFoodQuantity() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "Food: 12.4M\nPower: 8.6M\nEliminations: 350"
        )

        assertEquals(12_400_000.0, result.resources.single().quantiteNumerique ?: 0.0, 0.1)
        assertEquals(8_600_000L, result.power?.puissanceTotaleNumerique)
    }

    @Test
    fun keepsTopBarCountersUnidentifiedWhenResourceNamesAreNotOcrText() {
        val values = listOf("1.2M", "850K", "42M", "3.4M", "120", "7.8M")
        val boxes = values.mapIndexed { index, value ->
            OcrTextBox(value, left = 40 + index * 120, top = 50, right = 95 + index * 120, bottom = 82)
        }
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "Main screen",
            textBoxes = boxes,
            imageWidth = 900,
            imageHeight = 1600
        )
        assertTrue("Aucun compteur sans libellé ne doit devenir une ressource", result.resources.isEmpty())
        assertNull("Les compteurs de la barre ne sont pas la puissance du joueur", result.power)
        val hudCounters = result.uncertainItems.filter { it.champ.startsWith("Compteur HUD non identifié") }
        assertEquals(6, hudCounters.size)
        assertEquals(values, hudCounters.map { it.valeurObservee })
    }

    @Test
    fun keepsOnlyOneAlignedTopHudRowAndDoesNotMapItToResources() {
        val values = listOf("1.2M", "850K", "42M", "3.4M", "120", "7.8M")
        val boxes = values.mapIndexed { index, value ->
            OcrTextBox(value, 40 + index * 120, 50 + index % 2, 95 + index * 120, 82 + index % 2)
        } + listOf(
            OcrTextBox("999", 100, 420, 150, 452),
            OcrTextBox("888", 220, 900, 270, 932)
        )
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "Main screen",
            textBoxes = boxes,
            imageWidth = 900,
            imageHeight = 1600
        )
        assertTrue(result.resources.isEmpty())
        val hudCounters = result.uncertainItems.filter { it.champ.startsWith("Compteur HUD non identifié") }
        assertEquals(6, hudCounters.size)
        assertEquals(values, hudCounters.map { it.valeurObservee })
    }

    @Test
    fun refusesPositionalMappingWhenOneOfTheSixResourceSlotsIsMissing() {
        val boxes = listOf("1.2M", "850K", "42M", "3.4M", "120").mapIndexed { index, value ->
            OcrTextBox(value, left = 40 + index * 120, top = 50, right = 95 + index * 120, bottom = 82)
        }
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "Main screen",
            textBoxes = boxes,
            imageWidth = 900,
            imageHeight = 1600
        )
        assertTrue(result.resources.isEmpty())
        assertNull("Une rangée supérieure incomplète ne doit pas devenir une puissance", result.power)
    }

    @Test
    fun refusesPositionalMappingWhenAnExtraNumberSharesTheTopRow() {
        val boxes = listOf("1.2M", "850K", "42M", "3.4M", "120", "7.8M", "99").mapIndexed { index, value ->
            OcrTextBox(value, left = 40 + index * 100, top = 50, right = 95 + index * 100, bottom = 82)
        }
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "Main screen",
            textBoxes = boxes,
            imageWidth = 900,
            imageHeight = 1600
        )
        assertTrue("Une rangée ambiguë ne doit pas produire de faux stocks", result.resources.isEmpty())
        assertNull("Une rangée supérieure ambiguë ne doit pas devenir une puissance", result.power)
    }

    @Test
    fun doesNotMixPositionGuessesWithResourcesAlreadyReadByName() {
        val boxes = listOf(
            OcrTextBox("1.2M", 40, 50, 95, 82),
            OcrTextBox("850K", 160, 50, 215, 82),
            OcrTextBox("42M", 280, 50, 335, 82),
            OcrTextBox("3.4M", 400, 50, 455, 82)
        )
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "Food: 5000",
            textBoxes = boxes,
            imageWidth = 900,
            imageHeight = 1600
        )
        assertEquals(1, result.resources.size)
        assertEquals("Nourriture (Food)", result.resources.single().nom)
        assertEquals(5000.0, result.resources.single().quantiteNumerique ?: 0.0, 0.1)
    }

    @Test
    fun doesNotTreatAnIsolatedFuelLabelAsAConfirmedResource() {
        val inline = LandsOfJailOcrParser.parseLocalOcrText("Fuel: 1200")
        val multiline = LandsOfJailOcrParser.parseLocalOcrText("Fuel\n1200")

        assertTrue(inline.resources.none { it.nom == "Essence (Gasoline)" })
        assertTrue(multiline.resources.none { it.nom == "Essence (Gasoline)" })
    }

    @Test
    fun doesNotPromoteSandseaFuelToGasolineWhenOtherResourcesAreVisible() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "Sandsea Conquest\nFuel: 1200\nFood: 5000"
        )
        assertTrue("Fuel de Sandsea ne doit pas devenir un stock d'essence",
            result.resources.none { it.nom == "Essence (Gasoline)" })
        assertTrue("La nourriture explicite doit rester reconnue",
            result.resources.any { it.nom == "Nourriture (Food)" && it.quantiteNumerique == 5000.0 })
    }

    @Test
    fun acceptsFuelAsResourceWhenAnotherResourceConfirmsTheResourcePanel() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "Fuel: 1200\nFood: 5000"
        )
        assertTrue("Fuel doit pouvoir être reconnu dans un panneau de ressources",
            result.resources.any { it.nom == "Essence (Gasoline)" && it.quantiteNumerique == 1200.0 })
        assertTrue(result.resources.any { it.nom == "Nourriture (Food)" && it.quantiteNumerique == 5000.0 })
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
        val result = LandsOfJailOcrParser.parseLocalOcrText("Ranger\nLevel 25")
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
    fun readsRobotLevelsInTurkishRussianAndChinese() {
        val samples = listOf(
            "Ranger\nSeviye 27" to "27",
            "Ranger\nУровень 28" to "28",
            "Ranger\n等级 29" to "29"
        )
        samples.forEach { (sample, expectedLevel) ->
            val robot = LandsOfJailOcrParser.parseLocalOcrText(sample).robots.single()
            assertEquals(expectedLevel, robot.niveau)
        }
    }

    @Test
    fun readsRobotLevelsInItalianAndGerman() {
        val samples = listOf(
            "Ranger\\nLivello 30" to "30",
            "Ranger\\nStufe 31" to "31"
        )
        samples.forEach { (sample, expectedLevel) ->
            val robot = LandsOfJailOcrParser.parseLocalOcrText(sample).robots.single()
            assertEquals(expectedLevel, robot.niveau)
        }
    }

    @Test
    fun readsTotalPowerWithSpanishAndJapaneseLabels() {
        val spanish = LandsOfJailOcrParser.parseLocalOcrText("Potencia: 82,5M")
        assertEquals(82_500_000L, spanish.power?.puissanceTotaleNumerique)

        val japanese = LandsOfJailOcrParser.parseLocalOcrText("戦力: 72.3M")
        assertEquals(72_300_000L, japanese.power?.puissanceTotaleNumerique)
    }

    @Test
    fun readsTotalPowerWithItalianAndGermanLabelsWithoutConfusingFood() {
        val italian = LandsOfJailOcrParser.parseLocalOcrText("Potenza: 509 256 511")
        assertEquals(509_256_511L, italian.power?.puissanceTotaleNumerique)

        val german = LandsOfJailOcrParser.parseLocalOcrText("Kampfkraft: 177,1M")
        assertEquals(177_100_000L, german.power?.puissanceTotaleNumerique)

        val foodOnly = LandsOfJailOcrParser.parseLocalOcrText("Cibo: 177,1M")
        assertNull("La nourriture ne doit jamais être interprétée comme puissance", foodOnly.power)
    }

    @Test
    fun readsRobotPowerAndSkillsInItalianAndGerman() {
        val italian = LandsOfJailOcrParser.parseLocalOcrText(
            "Ranger\\nPotenza: 120\\nAbilità: Colpo rapido"
        ).robots.single()
        assertEquals("120", italian.puissance)
        assertTrue(italian.competences.contains("Colpo rapido"))

        val german = LandsOfJailOcrParser.parseLocalOcrText(
            "Ranger\\nKampfkraft: 135\\nFähigkeiten: Präzisionsschuss"
        ).robots.single()
        assertEquals("135", german.puissance)
        assertTrue(german.competences.contains("Präzisionsschuss"))
    }

    @Test
    fun doesNotBorrowTheNextRobotsLevelWhenSeveralRobotCardsAreVisible() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "Ranger\nPhantom Cat\nLevel 90"
        )

        val ranger = result.robots.single { it.nom == "Ranger" }
        val phantomCat = result.robots.single { it.nom == "Phantom Cat" }

        assertTrue("Ranger ne doit pas emprunter le niveau du robot suivant", ranger.niveau.isBlank())
        assertTrue(ranger.incertain)
        assertEquals("90", phantomCat.niveau)
        assertFalse(phantomCat.incertain)
    }

    @Test
    fun doesNotTurnBuildingPanelActionsOrHungerIntoAccountResourcesOrPower() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "Kitchen Lv.1\nStove\nBuild 135/10\nHunger 0+18.6\nTime 0s+8s"
        )

        assertNull(result.power)
        assertTrue(result.resources.isEmpty())
        assertTrue(result.robots.isEmpty())
        assertEquals("batiments", result.detectedCategory)
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
    fun keepsExplicitEquipmentMaterialQuantityWhenItAppearsOnTheNextLine() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            """Hero Equipment
Equipment Upgrade
Weapon
Level 18
Magnet
125"""
        )

        assertEquals(1, result.equipment.size)
        val equipment = result.equipment.single()
        assertEquals("Arme", equipment.piece)
        assertEquals("18", equipment.niveau)
        assertTrue(equipment.materiaux.contains("Magnet"))
        assertTrue(equipment.materiaux.contains("125"))
    }

    @Test
    fun recognizesGoldResourceLabelsInTurkishRussianAndChinese() {
        val samples = listOf(
            "Altın: 2500",
            "Золото: 3500",
            "金币：4500",
            "黄金：5500"
        )

        samples.forEach { sample ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(sample)
            val gold = result.resources.singleOrNull { it.nom == "Cash / Or" }
            assertNotNull("L'or doit être reconnu : $sample", gold)
            assertNotNull("La quantité doit être lue : $sample", gold?.quantiteNumerique)
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

    @Test
    fun recognizesGoldResourceLabelsInAllSixSupportedLanguages() {
        val samples = listOf(
            "Gold: 1200",
            "Or: 1200",
            "Gold: 1200",
            "Altın: 1200",
            "Золото: 1200",
            "金币：1200"
        )
        samples.forEach { sample ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(sample)
            assertTrue("La ressource Or doit être reconnue : $sample", result.resources.any {
                it.nom == "Cash / Or" && it.quantiteNumerique == 1200.0
            })
        }
    }

    @Test
    fun classifiesGoldResourceScreensInEnglishTurkishRussianAndChinese() {
        val samples = listOf(
            "Gold 1200",
            "Altın 1200",
            "Золото 1200",
            "金币 1200"
        )
        samples.forEach { sample ->
            val result = ScreenRecognitionEngine.analyze(sample)
            assertEquals("La capture de ressource doit être classée : $sample", "ressources", result.category)
        }
    }


    @Test
    fun recognizesAdditionalGameSystemsAndScreensAcrossSupportedScripts() {
        val samples = listOf(
            "Power Armor Hangar\nSolo Module\nBarrier Module" to "power_armor",
            "Satellite Launch\nData Disks\nPlanet Coins" to "satellites",
            "Haven Island\nMansion\nDecorations\nTreasure Cube" to "haven_island",
            "Arrest Stage\nFelon\nContraband Search" to "arrest",
            "Warden's Trove\nCollection Pieces\nTomes" to "trove",
            "Alliance Territory\nAlliance Technology\nAlliance Shop" to "alliance",
            "Item Inventory\nChest Contents\nDrop Rate" to "items",
            "动力装甲\n囚犯装甲\n核心技能" to "power_armor",
            "卫星\n数据盘\n行星币" to "satellites",
            "避风岛\n豪宅\n装饰\n宝藏魔方" to "haven_island",
            "逮捕\n罪犯\n违禁品搜查" to "arrest",
            "宝库\n典籍\n收藏品" to "trove",
            "联盟\n联盟科技\n联盟商店" to "alliance"
        )
        samples.forEach { (sample, expected) ->
            assertEquals("Mauvaise catégorie pour : $sample", expected, ScreenRecognitionEngine.analyze(sample).category)
        }
    }

    @Test
    fun parsesItalianResourceNamesAndQuantities() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "Cibo: 1200\nLegno: 2300\nAcciaio: 3400\nBenzina: 4500\nDiamanti: 55\nOro: 66"
        )
        val recognized = result.resources.associate { it.nom to it.quantiteNumerique }
        assertTrue("Cibo non riconosciuto: $recognized", recognized["Nourriture (Food)"] == 1200.0)
        assertTrue("Legno non riconosciuto: $recognized", recognized["Bois (Wood)"] == 2300.0)
        assertTrue("Acciaio non riconosciuto: $recognized", recognized["Acier (Steel)"] == 3400.0)
        assertTrue("Benzina non riconosciuta: $recognized", recognized["Essence (Gasoline)"] == 4500.0)
        assertTrue("Diamanti non riconosciuti: $recognized", recognized["Diamants (Gems)"] == 55.0)
        assertTrue("Oro non riconosciuto: $recognized", recognized["Cash / Or"] == 66.0)
    }

    @Test
    fun recognizesItalianResourceScreens() {
        val result = ScreenRecognitionEngine.analyze("Cibo\nLegno\nAcciaio\nBenzina")
        assertEquals("ressources", result.category)
        assertTrue(result.confidence >= 0.8f)
    }
    @Test
    fun recognizesItalianHeroEquipmentUpgradeAndPieceLabels() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            """Equipaggiamento eroe
Miglioramento equipaggiamento
Arma
Livello 18
Magnete x 120"""
        )
        assertEquals("equipements", result.detectedCategory)
        assertEquals(1, result.equipment.size)
        assertEquals("Arme", result.equipment.single().piece)
        assertEquals("18", result.equipment.single().niveau)
        assertTrue(result.equipment.single().materiaux.contains("Magnete"))
        assertFalse(result.equipment.single().incertain)
    }

    @Test
    fun keepsEquipmentUpgradeCaptureWhenThePieceCannotBeRead() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            """Hero Equipment
Equipment Upgrade
Magnet x 120
Potential Coil x 8"""
        )
        assertEquals("equipements", result.detectedCategory)
        assertEquals(1, result.equipment.size)
        assertEquals("Équipement de héros", result.equipment.single().piece)
        assertTrue(result.equipment.single().incertain)
        assertTrue(result.equipment.single().texteObserve.contains("Magnet x 120"))
        assertTrue(result.equipment.single().raisonIncertitude?.contains("pièce ou niveau") == true)
    }

    @Test
    fun recognizesItalianAndExclusiveEquipmentScreenLabels() {
        val samples = listOf(
            "Equipaggiamento eroe\nMiglioramento equipaggiamento\nMagnete",
            "Equipaggiamento esclusivo\nIngranaggio di precisione"
        )
        samples.forEach { sample ->
            assertEquals("equipements", ScreenRecognitionEngine.analyze(sample).category)
        }
    }

    @Test
    fun recognizesEquipmentPiecesAndExplicitLevelsAcrossAllSevenLanguages() {
        val samples = listOf(
            "Arme +5" to "Arme",
            "Weapon +6" to "Arme",
            "Waffe +7" to "Arme",
            "Silah +8" to "Arme",
            "Оружие +9" to "Arme",
            "武器 +10" to "Arme",
            "Arma +11" to "Arme",
            "Rüstung +12" to "Armure",
            "Zırh +13" to "Armure",
            "Перчатка +14" to "Gants",
            "手套 +15" to "Gants",
            "Guanto +16" to "Gants",
            "Stiefel +17" to "Bottes",
            "靴 +18" to "Bottes"
        )
        samples.forEach { (line, expectedPiece) ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(line)
            assertEquals("Pièce incorrecte pour $line", expectedPiece, result.equipment.singleOrNull()?.piece)
            assertEquals("Niveau incorrect pour $line", line.substringAfter('+'), result.equipment.singleOrNull()?.niveau)
            assertFalse("Le niveau explicite doit être fiable pour $line", result.equipment.singleOrNull()?.incertain ?: true)
        }
    }

    @Test
    fun doesNotTreatWordsContainingEquipmentAliasesAsEquipmentPieces() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "Weaponized research\nArmor-plated vehicle\nBootcamp"
        )
        assertTrue(result.equipment.isEmpty())
    }


    @Test
    fun readsWardenOfficeLevelWhenTheBadgeNumberPrecedesTheBuildingName() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "509 256 511\n6 Bureau du directeur\nCapacité des prisonniers 40/40\nNiveau de danger 9"
        )
        assertEquals("Niv. 6", result.power?.niveauPrisonOuQG)
    }

    @Test
    fun readsExplicitWardenLevelOnTheLineImmediatelyAfterBuildingName() {
        val samples = listOf(
            "Bureau du directeur\nNiveau 6\nCapacité des prisonniers 40/40",
            "Warden Office\nLevel 18\nPrisoner capacity 40/40",
            "QG\nNiv. 12\nCapacité 40/40"
        )
        val expected = listOf("Niv. 6", "Niv. 18", "Niv. 12")
        samples.forEachIndexed { index, sample ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(sample)
            assertEquals("Niveau incorrect pour : $sample", expected[index], result.power?.niveauPrisonOuQG)
        }
    }

    @Test
    fun doesNotReadAnUnlabelledNumberOnTheLineAfterBuildingNameAsItsLevel() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "Bureau du directeur\n40/40\nNiveau de danger 9"
        )
        assertNull(result.power?.niveauPrisonOuQG)
    }

    @Test
    fun readsCashResourceWhenTheGameCallsItArgentLiquide() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Argent liquide: 1.0B")
        val cash = result.resources.single { it.nom == "Cash / Or" }
        assertEquals("1.0B", cash.quantiteTexte)
        assertEquals(1_000_000_000.0, cash.quantiteNumerique ?: 0.0, 0.1)
    }

    @Test
    fun doesNotMistakeWardenCapacityForItsLevel() {
        val samples = listOf(
            "Warden Office 40/40\nPower: 12,345",
            "Bureau du directeur 40/40\nCapacité des prisonniers 40/40\nPower: 12,345",
            "Prison 35/40\nNiveau de danger 9\nPower: 12,345"
        )
        samples.forEach { sample ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(sample)
            assertNull("Une capacité ne doit pas devenir le niveau du bâtiment : $sample",
                result.power?.niveauPrisonOuQG)
        }
    }

    @Test
    fun doesNotUsePrisonerDangerLevelAsWardenOfficeLevel() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "509 256 511\nBureau du directeur\nCapacité des prisonniers 40/40\nNiveau de danger 9"
        )
        assertNull(result.power?.niveauPrisonOuQG)
    }


    @Test
    fun classifiesTheCompleteSupportedScreenCategoryMatrixWithoutCrossCategoryDrift() {
        val cases = listOf(
            "Gorilla Legion\nGorilla Warlord\nGorilla Chieftain" to "gorilla_legion",
            "Trial Cage\nDamage Ranking\nDamage Dealt" to "trial_cage",
            "Crystal Clash\nIronhold\nEnergy Hub" to "crystal_clash",
            "Sandsea Conquest\nVessel Station\nLighthouse" to "sandsea_conquest",
            "Supreme Honor\nWar Preparation\nSVS" to "svs",
            "Turmoil Surge\nSentry Turret\nWave" to "turmoil_surge",
            "Gold Hunter\nHunter Points" to "gold_hunter",
            "Robot Center\nRobot Upgrade\nPhantom Cat" to "batiments",
            "Power Armor Hangar\nSolo Module\nBarrier Module" to "power_armor",
            "Forticlad Lab\nHyperalloy\nForticlad Technology" to "forticlad_lab",
            "Logistics Technology\nEconomy Technology\nMilitary Technology" to "lab",
            "Cosmos Hub\nCosmic Core\nOrbital Core" to "cosmos_hub",
            "Dungeon Office\nSurveillance Room\nCriminal Prison" to "dungeon",
            "Hero Equipment\nEquipment Upgrade\nPotential Coil" to "equipements",
            "Heroes\nSkills\nStars" to "heros",
            "Warden's Office\nBuilding Upgrade\nBarracks" to "batiments",
            "Shieldbearers\nBombers\nShooters\nTroops" to "troupes",
            "Food\nWood\nSteel\nGasoline" to "ressources",
            "Satellite Launch\nData Disks\nPlanet Coins" to "satellites",
            "Haven Island\nMansion\nDecorations\nTreasure Cube" to "haven_island",
            "Arrest Stage\nArrest Battle\nContraband Search" to "arrest",
            "Warden's Trove\nCollection Pieces\nTomes" to "trove",
            "Alliance Territory\nAlliance Technology\nAlliance Shop" to "alliance",
            "Item Inventory\nChest Contents\nDrop Rate" to "items",
            "Player Profile\nPower\nEliminations" to "profil"
        )

        cases.forEach { (sample, expected) ->
            val result = ScreenRecognitionEngine.analyze(sample)
            assertEquals(
                "La catégorie '$expected' est mal classée pour la capture textuelle : $sample",
                expected,
                result.category
            )
            assertTrue(
                "La classification '$expected' doit demander une confirmation visuelle si elle est peu sûre",
                result.confidence >= 0.52f
            )
        }
    }


    @Test
    fun recognizesItalianCoreGameScreenCategories() {
        val samples = listOf(
            "Centro robot\nMiglioramento robot\nPhantom Cat" to "batiments",
            "Laboratorio Forticlad\nTecnologia Forticlad\nHyperalloy" to "forticlad_lab",
            "Tecnologia logistica\nTecnologia economica\nTecnologia militare" to "lab",
            "Eroi\nAbilità\nStelle" to "heros",
            "Ufficio del direttore\nMiglioramento edificio\nCaserma" to "batiments",
            "Truppe\nCapacità di marcia\nShieldbearers" to "troupes"
        )
        samples.forEach { (sample, expected) ->
            assertEquals(
                "La schermata italiana deve essere classificata correttamente: $sample",
                expected,
                ScreenRecognitionEngine.analyze(sample).category
            )
        }
    }


    @Test
    fun doesNotInventHeroesFromArbitraryNamesFollowedByLevel() {
        val building = LandsOfJailOcrParser.parseLocalOcrText("Warehouse\nLevel 12")
        val generic = LandsOfJailOcrParser.parseLocalOcrText("Training Center Lv 8")
        val knownHero = LandsOfJailOcrParser.parseLocalOcrText("Ada\nLevel 12")

        assertTrue("Un bâtiment ne doit pas être ajouté à l'inventaire des héros", building.heroes.isEmpty())
        assertTrue("Un libellé générique ne doit pas devenir un héros", generic.heroes.isEmpty())
        assertEquals("Les héros connus du catalogue doivent rester détectés", listOf("Ada"), knownHero.heroes.map { it.nom })
    }

    @Test
    fun doesNotReadWordsContainingResourceNamesAsResourceLabels() {
        val samples = listOf(
            "Wooden Statue\n1200",
            "Foodcourt Upgrade\n850",
            "Steelworks Level 4\n300"
        )
        samples.forEach { sample ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(sample)
            assertTrue("Aucun faux stock ne doit être créé pour : $sample", result.resources.isEmpty())
        }
    }

    @Test
    fun stillReadsResourceLabelsImmediatelyFollowedByQuantities() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "食物1200\n木材850\n钢材300\n汽油450"
        )
        assertTrue("Les libellés chinois accolés aux nombres doivent rester reconnus",
            result.resources.any { it.nom == "Nourriture (Food)" && it.quantiteTexte == "1200" })
        assertTrue(result.resources.any { it.nom == "Bois (Wood)" && it.quantiteTexte == "850" })
    }

    @Test
    fun topResourceRowPreventsProfilePowerFallbackBeforeEliminations() {
        val boxes = listOf("1.2M", "850K", "42M", "3.4M", "120", "7.8M").mapIndexed { index, value ->
            OcrTextBox(value, left = 40 + index * 120, top = 50, right = 95 + index * 120, bottom = 82)
        }
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "12.4M\nEliminations: 350",
            textBoxes = boxes,
            imageWidth = 900,
            imageHeight = 1600
        )

        assertNull("Une rangée de ressources détectée ne doit jamais devenir la puissance du profil", result.power)
        assertTrue("Sans icônes ou libellés, aucun stock ne doit être attribué", result.resources.isEmpty())
        val hudCounters = result.uncertainItems.filter { it.champ.startsWith("Compteur HUD non identifié") }
        assertEquals(6, hudCounters.size)
        assertEquals(listOf("1.2M", "850K", "42M", "3.4M", "120", "7.8M"), hudCounters.map { it.valeurObservee })
    }


    @Test
    fun mapsExactlySixTopCountersInLeftToRightOrderAndKeepsThemUncertain() {
        val values = listOf("1.2M", "850K", "42M", "3.4M", "120", "7.8M")
        val boxes = values.mapIndexed { index, value ->
            OcrTextBox(value, left = 40 + index * 120, top = 50, right = 95 + index * 120, bottom = 82)
        }
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "VIP8",
            textBoxes = boxes,
            imageWidth = 900,
            imageHeight = 1600
        )

        assertTrue("Les compteurs sans icônes reconnues ne doivent pas recevoir de noms inventés", result.resources.isEmpty())
        assertEquals(values, result.uncertainItems.map { it.valeurObservee })
        assertEquals(6, result.uncertainItems.size)
        assertTrue("Chaque compteur doit rester explicitement non identifié",
            result.uncertainItems.all { it.raison.contains("attribution automatique interdite") })
        assertNull("Les compteurs de la barre ne doivent pas devenir une puissance", result.power)
    }

    @Test
    fun refusesPartialOrOverfullTopCounterRowsInsteadOfShiftingResourceNames() {
        val five = listOf("1.2M", "850K", "42M", "3.4M", "120")
        val seven = listOf("1.2M", "850K", "42M", "3.4M", "120", "7.8M", "99")
        listOf(five, seven).forEach { values ->
            val boxes = values.mapIndexed { index, value ->
                OcrTextBox(value, left = 40 + index * 100, top = 50, right = 90 + index * 100, bottom = 82)
            }
            val result = LandsOfJailOcrParser.parseLocalOcrText(
                rawText = "VIP8",
                textBoxes = boxes,
                imageWidth = 900,
                imageHeight = 1600
            )
            assertTrue("Une rangée partielle ou surchargée ne doit pas être associée", result.resources.isEmpty())
            assertNull("Une rangée ambiguë ne doit pas devenir une puissance", result.power)
        }
    }

    @Test
    fun enhancedOcrCoordinatesAreMappedBackToOriginalScreenshot() {
        val enhancedBoxes = listOf(
            OcrTextBox("Food: 12.4M", 150, 75, 450, 123),
            OcrTextBox("Power: 8.6M", 600, 300, 900, 348)
        )

        val mapped = OcrImagePreprocessor.mapBoxesToOriginal(enhancedBoxes)

        assertEquals(OcrTextBox("Food: 12.4M", 100, 50, 300, 82), mapped[0])
        assertEquals(OcrTextBox("Power: 8.6M", 400, 200, 600, 232), mapped[1])
    }

    @Test
    fun enhancedOcrCoordinateMappingRejectsInvalidScale() {
        assertTrue(runCatching {
            OcrImagePreprocessor.mapBoxesToOriginal(emptyList(), 0f)
        }.isFailure)
    }


    @Test
    fun robotGuideCatalogContainsTwelveRobotsAndGroundedRecommendations() {
        val catalog = com.landsofjail.assistant.model.RobotCatalog.entries
        assertEquals(12, catalog.size)
        assertEquals("Shooter", com.landsofjail.assistant.model.RobotCatalog.find("Phantom Cat")?.troopType)
        assertEquals("90–100", com.landsofjail.assistant.model.RobotCatalog.find("Musashimaru")?.recommendedLevel)
        assertEquals("Bomber", com.landsofjail.assistant.model.RobotCatalog.find("Bastion")?.troopType)
        assertEquals("Shieldbearer", com.landsofjail.assistant.model.RobotCatalog.find("Atlax")?.troopType)
        assertTrue(com.landsofjail.assistant.model.RobotCatalog.find("Bastion")?.recommendedLevel.isNullOrBlank())
    }

    @Test
    fun robotGuideNamesAreRecognizedByLocalOcr() {
        com.landsofjail.assistant.model.RobotCatalog.entries.forEach { entry ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(entry.name)
            assertTrue("Robot du guide non reconnu : ${entry.name}", result.robots.any { it.nom == entry.name })
        }
    }

    @Test
    fun positionalHomePowerBadgeOverridesUnlabeledTopBarNumberWhenCoordinatesExist() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "177.1M\nVIP8",
            textBoxes = listOf(
                OcrTextBox("177.1M", 238, 122, 353, 158),
                OcrTextBox("509 256 511", 18, 166, 203, 215)
            ),
            imageWidth = 698,
            imageHeight = 1536
        )

        assertEquals("Le compteur de nourriture ne doit pas bloquer la lecture du badge de puissance",
            509_256_511L, result.power?.puissanceTotaleNumerique)
        assertEquals("La nourriture non libellée ne doit pas être inventée comme stock", 0, result.resources.size)
    }

    @Test
    fun doesNotUseUnlabeledTextNumberAsPowerWhenOcrCoordinatesAreAvailable() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            rawText = "177.1M\nVIP8",
            textBoxes = listOf(OcrTextBox("177.1M", 238, 122, 353, 158)),
            imageWidth = 698,
            imageHeight = 1536
        )

        assertNull("Sans badge de puissance ni libellé, le compteur HUD doit rester non classé", result.power)
    }
    @Test
    fun doesNotClassifyGenericCageWordAsTrialCage() {
        val result = ScreenRecognitionEngine.analyze("Cage\nUpgrade\nLevel 12")
        assertEquals("ocr_local", result.category)
        assertTrue("trial_cage" !in result.alternativeCategories)
    }

    @Test
    fun requiresTrialCageOrDamageContextForCageEventClassification() {
        val samples = listOf(
            "Trial Cage",
            "Cage\nDamage Ranking",
            "Cage\nDamage Dealt",
            "Prüfungskäfig\nSchadensrangliste",
            "Deneme Kafesi\nVerilen hasar",
            "Испытательная клетка\nНанесенный урон",
            "试炼笼\n造成伤害"
        )
        samples.forEach { sample ->
            assertEquals("L'événement doit rester reconnu : $sample", "trial_cage", ScreenRecognitionEngine.analyze(sample).category)
        }
    }

    @Test
    fun doesNotStoreForticladLabLevelAsHeadquartersLevel() {
        val result = LandsOfJailOcrParser.parseLocalOcrText("Forticlad Lab\nLevel 6")

        assertEquals("forticlad_lab", result.detectedCategory)
        assertNull("Le niveau du laboratoire Forticlad ne doit pas devenir le niveau du QG", result.power)
    }
 
    @Test
    fun doesNotBorrowBuildingLevelForRobotDetails() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "Phantom Cat\nKitchen\nLevel 6"
        )

        val robot = result.robots.single { it.nom == "Phantom Cat" }
        assertTrue("Le niveau de la cuisine ne doit pas être attribué au robot", robot.niveau.isBlank())
        assertTrue(robot.incertain)
        assertNull("Le niveau de la cuisine ne doit pas devenir celui du QG", result.power)
    }

    @Test
    fun localizedBuildingPanelsStopRobotLevelContext() {
        val buildingLabels = listOf(
            "Cucina", "Küche", "Mutfak", "Кухня", "厨房",
            "Lagerhaus", "Depo", "Склад", "仓库",
            "Kaserne", "Kışla", "Казармы", "兵营"
        )

        buildingLabels.forEach { label ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(
                "Phantom Cat\n$label\nLevel 6"
            )
            val robot = result.robots.single { it.nom == "Phantom Cat" }
            assertTrue("Le niveau du panneau '$label' ne doit pas être attribué au robot", robot.niveau.isBlank())
            assertTrue(robot.incertain)
        }
    }

    @Test
    fun keepsRobotLevelWhenItBelongsToTheRobotCard() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "Phantom Cat\nLevel 90\nPower 120"
        )

        val robot = result.robots.single { it.nom == "Phantom Cat" }
        assertEquals("90", robot.niveau)
        assertEquals("120", robot.puissance)
    }

    @Test
    fun extractsSeveralNamedResourcesFromOneOcrLineWithoutMixingValues() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "Food: 1200 Wood: 3400 Steel: 5600 Gasoline: 7800 Gems: 90 Gold: 450"
        )

        assertEquals(6, result.resources.size)
        assertEquals(1200.0, result.resources.single { it.nom == "Nourriture (Food)" }.quantiteNumerique ?: 0.0, 0.1)
        assertEquals(3400.0, result.resources.single { it.nom == "Bois (Wood)" }.quantiteNumerique ?: 0.0, 0.1)
        assertEquals(5600.0, result.resources.single { it.nom == "Acier (Steel)" }.quantiteNumerique ?: 0.0, 0.1)
        assertEquals(7800.0, result.resources.single { it.nom == "Essence (Gasoline)" }.quantiteNumerique ?: 0.0, 0.1)
        assertEquals(90.0, result.resources.single { it.nom == "Diamants (Gems)" }.quantiteNumerique ?: 0.0, 0.1)
        assertEquals(450.0, result.resources.single { it.nom == "Cash / Or" }.quantiteNumerique ?: 0.0, 0.1)
    }

    @Test
    fun extractsAllSixResourceTypesFromRussianLabels() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "еда: 1200\nдерево: 3400\nсталь: 5600\nбензин: 7800\nалмазы: 90\nзолото: 450"
        )

        assertEquals(6, result.resources.size)
        assertEquals(1200.0, result.resources.single { it.nom == "Nourriture (Food)" }.quantiteNumerique ?: 0.0, 0.1)
        assertEquals(3400.0, result.resources.single { it.nom == "Bois (Wood)" }.quantiteNumerique ?: 0.0, 0.1)
        assertEquals(5600.0, result.resources.single { it.nom == "Acier (Steel)" }.quantiteNumerique ?: 0.0, 0.1)
        assertEquals(7800.0, result.resources.single { it.nom == "Essence (Gasoline)" }.quantiteNumerique ?: 0.0, 0.1)
        assertEquals(90.0, result.resources.single { it.nom == "Diamants (Gems)" }.quantiteNumerique ?: 0.0, 0.1)
        assertEquals(450.0, result.resources.single { it.nom == "Cash / Or" }.quantiteNumerique ?: 0.0, 0.1)
    }

    @Test
    fun extractsAllSixResourceTypesFromChineseLabels() {
        val result = LandsOfJailOcrParser.parseLocalOcrText(
            "食物: 1200\n木材: 3400\n钢材: 5600\n汽油: 7800\n钻石: 90\n金币: 450"
        )

        assertEquals(6, result.resources.size)
        assertEquals(1200.0, result.resources.single { it.nom == "Nourriture (Food)" }.quantiteNumerique ?: 0.0, 0.1)
        assertEquals(3400.0, result.resources.single { it.nom == "Bois (Wood)" }.quantiteNumerique ?: 0.0, 0.1)
        assertEquals(5600.0, result.resources.single { it.nom == "Acier (Steel)" }.quantiteNumerique ?: 0.0, 0.1)
        assertEquals(7800.0, result.resources.single { it.nom == "Essence (Gasoline)" }.quantiteNumerique ?: 0.0, 0.1)
        assertEquals(90.0, result.resources.single { it.nom == "Diamants (Gems)" }.quantiteNumerique ?: 0.0, 0.1)
        assertEquals(450.0, result.resources.single { it.nom == "Cash / Or" }.quantiteNumerique ?: 0.0, 0.1)
    }


    @Test
    fun acceptsOnlyProperlyGroupedWhitespaceInResourceQuantities() {
        val valid = LandsOfJailOcrParser.parseLocalOcrText("Food: 1 234 567")
        assertEquals(1_234_567.0,
            valid.resources.single { it.nom == "Nourriture (Food)" }.quantiteNumerique ?: 0.0, 0.1)

        val decimal = LandsOfJailOcrParser.parseLocalOcrText("Food: 1 234,5")
        assertEquals(1_234.5,
            decimal.resources.single { it.nom == "Nourriture (Food)" }.quantiteNumerique ?: 0.0, 0.1)

        val malformed = listOf("12 34", "1 23 456", "1234 567")
        malformed.forEach { value ->
            val result = LandsOfJailOcrParser.parseLocalOcrText("Food: $value")
            assertTrue("Un groupement d'espaces invalide doit être rejeté : $value",
                result.resources.isEmpty() || result.resources.single().quantiteNumerique == null)
        }
    }


    @Test
    fun parsesSpaceGroupedDecimalsWithSuffixForResourcesAndPower() {
        val resourceSingleGroup = LandsOfJailOcrParser.parseLocalOcrText("Food: 1 234,5M")
        val resourceMultipleGroups = LandsOfJailOcrParser.parseLocalOcrText("Wood: 1 234 567,8M")
        val powerSingleGroup = LandsOfJailOcrParser.parseLocalOcrText("Power: 1 234,5M")
        val powerMultipleGroups = LandsOfJailOcrParser.parseLocalOcrText("Puissance: 1 234 567,8M")

        assertEquals(1_234_500_000.0,
            resourceSingleGroup.resources.single().quantiteNumerique ?: 0.0, 0.1)
        assertEquals(1_234_567_800_000.0,
            resourceMultipleGroups.resources.single().quantiteNumerique ?: 0.0, 0.1)
        assertEquals(1_234_500_000L, powerSingleGroup.power?.puissanceTotaleNumerique)
        assertEquals(1_234_567_800_000L, powerMultipleGroups.power?.puissanceTotaleNumerique)
    }

    @Test
    fun rejectsMalformedSpaceGroupedDecimalsWithSuffix() {
        val malformed = listOf("12 34,5M", "1 23 456,7M", "1 2345,6M")
        malformed.forEach { value ->
            val resource = LandsOfJailOcrParser.parseLocalOcrText("Food: $value")
            val power = LandsOfJailOcrParser.parseLocalOcrText("Power: $value")
            assertTrue("Groupement invalide accepté comme ressource : $value",
                resource.resources.isEmpty() || resource.resources.single().quantiteNumerique == null)
            assertNull("Groupement invalide accepté comme puissance : $value", power.power)
        }
    }


    @Test
    fun rejectsNumericValuesOutsideSupportedLongRange() {
        val oversizedPower = LandsOfJailOcrParser.parseLocalOcrText(
            "Power: 9,223,372,036,854,775,808"
        )
        assertNull("Une puissance hors plage ne doit pas être convertie en Long.MAX_VALUE",
            oversizedPower.power?.puissanceTotaleNumerique)
        assertTrue("Une puissance hors plage doit être marquée incertaine",
            oversizedPower.power?.incertain == true)

        val oversizedResource = LandsOfJailOcrParser.parseLocalOcrText(
            "Food: 9,223,372,036,854,775,808"
        )
        assertTrue("Un stock hors plage doit être rejeté",
            oversizedResource.resources.isEmpty())

        val suffixOverflow = LandsOfJailOcrParser.parseLocalOcrText(
            "Wood: 999,999,999,999,999,999B"
        )
        assertTrue("Un multiplicateur qui déborde doit être rejeté",
            suffixOverflow.resources.isEmpty())
    }


    @Test
    fun preservesDotGroupedPowerOverflowAsUncertainButRejectsMalformedGrouping() {
        val oversizedDotGroupedPower = LandsOfJailOcrParser.parseLocalOcrText(
            "Power: 9.223.372.036.854.775.808"
        )
        assertNull(oversizedDotGroupedPower.power?.puissanceTotaleNumerique)
        assertTrue(oversizedDotGroupedPower.power?.incertain == true)

        val malformedPower = LandsOfJailOcrParser.parseLocalOcrText(
            "Power: 9,22,372,036,854,775,808"
        )
        assertNull("Un groupement de milliers invalide ne doit pas créer de puissance",
            malformedPower.power)
    }

    @Test
    fun extractsWardenOfficeLevelsFromGermanTurkishRussianChineseAndItalianLabels() {
        val samples = listOf(
            "Büro des Direktors\nStufe 18" to "Niv. 18",
            "Müdürün Ofisi\nSeviye 16" to "Niv. 16",
            "Офис начальника\nУровень 12" to "Niv. 12",
            "典狱长办公室\n等级 20" to "Niv. 20",
            "Ufficio del Direttore\nLivello 14" to "Niv. 14"
        )

        samples.forEach { (sample, expectedLevel) ->
            val result = LandsOfJailOcrParser.parseLocalOcrText(sample)
            assertEquals("Le niveau du QG doit être extrait : $sample",
                expectedLevel, result.power?.niveauPrisonOuQG)
            assertEquals("L'écran doit être classé comme bâtiment : $sample",
                "batiments", result.detectedCategory)
        }
    }

}
