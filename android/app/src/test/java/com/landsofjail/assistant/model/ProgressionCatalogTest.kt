package com.landsofjail.assistant.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionCatalogTest {
    @Test
    fun laboratoryCatalogContainsEveryLevelFromTwoToThirtyInOrder() {
        val rows = ProgressionCatalog.labBuilding

        assertEquals(29, rows.size)
        assertEquals((2..30).toList(), rows.map { it.step.toLevel })
        assertTrue(rows.all { it.step.fromLevel == it.step.toLevel - 1 })
        assertTrue(rows.all { it.step.itemName == "Laboratory" })
    }

    @Test
    fun everyLaboratoryCostHasSourceDateAndCommunityConfidence() {
        ProgressionCatalog.labBuilding.forEach { row ->
            assertEquals(ProgressionCatalog.LAB_BUILDING_SOURCE, row.sourceUrl)
            assertEquals(ProgressionCatalog.SOURCE_CHECKED_ON, row.checkedOn)
            assertTrue(row.prerequisiteNotes.isNotEmpty())
            assertTrue(row.step.costs.isNotEmpty())
            assertTrue(row.step.costs.all {
                it.confidence == CostConfidence.COMMUNITY_REFERENCE &&
                    it.sourceUrl == ProgressionCatalog.LAB_BUILDING_SOURCE
            })
            assertFalse(row.step.costs.any {
                it.confidence == CostConfidence.VERIFIED_IN_GAME
            })
        }
    }

    @Test
    fun knownLabRangeTotalsOnlyItsPublishedCostsAndBaseTimes() {
        val firstFiveLevels = ProgressionCatalog.labBuilding
            .filter { it.step.toLevel in 2..6 }
            .map { it.step }
        val plan = ProgressionCoach.calculate(firstFiveLevels, emptyList())

        assertEquals(53_800L, plan.requirements.single { it.resourceId == "wood" }.required)
        assertEquals(10_400L, plan.requirements.single { it.resourceId == "steel" }.required)
        assertEquals(1_750L, plan.requirements.single { it.resourceId == "gasoline" }.required)
        assertEquals(999L, plan.totalKnownDurationSeconds)
        assertTrue(plan.unknownStepIds.isEmpty())
    }

    @Test
    fun laboratoryPrerequisiteNotesAreNotMisrepresentedAsCalculatedSteps() {
        val levelTen = ProgressionCatalog.labBuilding.single { it.step.toLevel == 10 }

        assertEquals(listOf("Warden's Office Lv.10"), levelTen.prerequisiteNotes)
        assertTrue(levelTen.step.prerequisites.isEmpty())
    }
    @Test
    fun gateCatalogContainsOnlyPreciselyPublishedLevelsTwoToSeven() {
        val rows = ProgressionCatalog.gateBuilding

        assertEquals((2..10).toList(), rows.map { it.step.toLevel })
        assertTrue(rows.all { it.step.fromLevel == it.step.toLevel - 1 })
        assertTrue(rows.all { it.step.itemName == "Gate" })
        assertTrue(rows.take(6).all { it.step.dataComplete })
        assertTrue(rows.drop(6).all { !it.step.dataComplete })
        assertTrue(rows.all { it.sourceUrl == ProgressionCatalog.GATE_SOURCE })
        assertTrue(rows.all { it.checkedOn == ProgressionCatalog.SOURCE_CHECKED_ON })
        assertTrue(rows.all { it.prerequisiteNotes.isNotEmpty() })
        assertTrue(rows.all { row ->
            row.step.costs.isNotEmpty() && row.step.costs.all {
                it.confidence == CostConfidence.COMMUNITY_REFERENCE &&
                    it.sourceUrl == ProgressionCatalog.GATE_SOURCE
            }
        })
        assertEquals(listOf(27, 29, 30), rows.drop(6).map {
            it.prerequisiteNotes.single().substringAfter("Lv.").toInt()
        })
        assertTrue(rows.drop(6).all { row -> row.step.costs.none { it.resourceId == "gasoline" } })
    }

    @Test
    fun gateLevelThreeHasPublishedCostsAndBaseDuration() {
        val levelThree = ProgressionCatalog.gateBuilding.single { it.step.toLevel == 3 }.step

        assertEquals(1_290L, levelThree.durationSeconds)
        assertEquals(180_000L, levelThree.costs.single { it.resourceId == "wood" }.quantity)
        assertEquals(36_000L, levelThree.costs.single { it.resourceId == "steel" }.quantity)
        assertEquals(9_000L, levelThree.costs.single { it.resourceId == "gasoline" }.quantity)
    }

    @Test
    fun gateHigherLevelsNeverTreatRoundedGasolineAmountsAsExact() {
        val higherLevels = ProgressionCatalog.gateBuilding.filter { it.step.toLevel > 7 }
        assertEquals(listOf(8, 9, 10), higherLevels.map { it.step.toLevel })
        assertTrue(higherLevels.all { !it.step.dataComplete })
        assertTrue(higherLevels.all { row -> row.step.costs.none { it.resourceId == "gasoline" } })
    }


    @Test
    fun gateLevelsEightToTenExposeOnlyExactPublishedCostsAndKeepGasolineUnknown() {
        val rows = ProgressionCatalog.gateBuilding.drop(6)
        assertEquals(listOf(8, 9, 10), rows.map { it.step.toLevel })
        assertEquals(listOf(262_500L, 347_160L, 414_000L), rows.map { it.step.durationSeconds })
        assertEquals(
            listOf(
                mapOf("food" to 75_000_000L, "wood" to 75_000_000L, "steel" to 15_000_000L),
                mapOf("food" to 120_000_000L, "wood" to 120_000_000L, "steel" to 24_000_000L),
                mapOf("food" to 155_000_000L, "wood" to 155_000_000L, "steel" to 31_000_000L)
            ),
            rows.map { row -> row.step.costs.associate { it.resourceId to it.quantity } }
        )
        val plan = ProgressionCoach.calculate(rows.map { it.step }, emptyList())
        assertEquals(listOf("gate-7-8", "gate-8-9", "gate-9-10"), plan.unknownStepIds)
        assertTrue(plan.requirements.none { it.resourceId == "gasoline" })
        assertEquals(350_000_000L, plan.requirements.single { it.resourceId == "food" }.required)
        assertEquals(350_000_000L, plan.requirements.single { it.resourceId == "wood" }.required)
        assertEquals(70_000_000L, plan.requirements.single { it.resourceId == "steel" }.required)
        assertEquals(null, plan.totalKnownDurationSeconds)
    }

    @Test
    fun fiveAdditionalBuildingCatalogsContainOnlyPublishedExactEarlyLevels() {
        val catalogs = listOf(
            Triple(ProgressionCatalog.kitchenBuilding, ProgressionCatalog.KITCHEN_SOURCE, 7),
            Triple(ProgressionCatalog.cellBuilding, ProgressionCatalog.CELL_SOURCE, 9),
            Triple(ProgressionCatalog.factoryBuilding, ProgressionCatalog.FACTORY_SOURCE, 6),
            Triple(ProgressionCatalog.utilityRoomBuilding, ProgressionCatalog.UTILITY_ROOM_SOURCE, 7),
            Triple(ProgressionCatalog.guardRoomBuilding, ProgressionCatalog.GUARD_ROOM_SOURCE, 6)
        )

        catalogs.forEach { (rows, source, maxLevel) ->
            assertEquals((2..maxLevel).toList(), rows.map { it.step.toLevel })
            assertTrue(rows.all { it.step.fromLevel == it.step.toLevel - 1 })
            assertTrue(rows.all { it.step.costs.size == 1 })
            assertTrue(rows.all { it.step.costs.single().resourceId == "gold" })
            assertTrue(rows.all { it.step.costs.single().confidence == CostConfidence.COMMUNITY_REFERENCE })
            assertTrue(rows.all { it.step.costs.single().sourceUrl == source })
            assertTrue(rows.all { it.sourceUrl == source && it.checkedOn == ProgressionCatalog.SOURCE_CHECKED_ON })
            assertTrue(rows.all { it.prerequisiteNotes.isNotEmpty() && it.step.durationSeconds != null })
        }
    }

    @Test
    fun cashOnlyBuildingCatalogsDoNotInventRoundedHigherLevelCosts() {
        val catalogLimits = listOf(
            ProgressionCatalog.kitchenBuilding to 7,
            ProgressionCatalog.cellBuilding to 9,
            ProgressionCatalog.factoryBuilding to 6,
            ProgressionCatalog.utilityRoomBuilding to 7,
            ProgressionCatalog.guardRoomBuilding to 6
        )
        assertTrue(catalogLimits.all { (rows, maxLevel) -> rows.none { it.step.toLevel > maxLevel } })
        assertEquals(listOf(6_668L), ProgressionCatalog.kitchenBuilding.filter { it.step.toLevel == 7 }.map { it.step.costs.single().quantity })
        assertEquals(listOf(2_134L, 7_041L, 8_449L), ProgressionCatalog.cellBuilding.filter { it.step.toLevel >= 7 }.map { it.step.costs.single().quantity })
        assertEquals(listOf(9_335L), ProgressionCatalog.utilityRoomBuilding.filter { it.step.toLevel == 7 }.map { it.step.costs.single().quantity })
    }


    @Test
    fun inventoryCatalogContainsOnlyExactSourcedLevelsTwoToSeven() {
        val rows = ProgressionCatalog.inventoryBuilding
        assertEquals((2..7).toList(), rows.map { it.step.toLevel })
        assertTrue(rows.all { it.step.fromLevel == it.step.toLevel - 1 })
        assertTrue(rows.all { it.step.itemName == "Inventory" })
        assertTrue(rows.all { it.sourceUrl == ProgressionCatalog.INVENTORY_SOURCE })
        assertTrue(rows.all { it.checkedOn == ProgressionCatalog.SOURCE_CHECKED_ON })
        assertTrue(rows.all { it.prerequisiteNotes == listOf("Warden's Office Lv.9") })
        assertTrue(rows.all { it.step.durationSeconds != null && it.step.costs.isNotEmpty() })
        assertTrue(rows.all { row -> row.step.costs.all {
            it.confidence == CostConfidence.COMMUNITY_REFERENCE &&
                it.sourceUrl == ProgressionCatalog.INVENTORY_SOURCE
        } })
        val levelFour = rows.single { it.step.toLevel == 4 }.step.costs.associate { it.resourceId to it.quantity }
        assertEquals(mapOf("wood" to 2_000L, "steel" to 400L), levelFour)
        assertTrue(rows.none { it.step.toLevel > 7 })
    }


    @Test
    fun laundryCatalogPreservesItsNonLinearWardenOfficePrerequisites() {
        val rows = ProgressionCatalog.laundryBuilding
        assertEquals((2..7).toList(), rows.map { it.step.toLevel })
        assertEquals(listOf(5, 5, 5, 5, 6, 7), rows.map {
            it.prerequisiteNotes.single().substringAfter("Lv.").toInt()
        })
        assertEquals(listOf(113L, 136L, 163L, 733L, 1_834L, 3_667L),
            rows.map { it.step.costs.single().quantity })
        assertEquals(listOf(7L, 12L, 20L, 80L, 152L, 293L),
            rows.map { it.step.durationSeconds })
        assertTrue(rows.all { it.step.costs.single().resourceId == "gold" })
        assertTrue(rows.all { it.sourceUrl == ProgressionCatalog.LAUNDRY_SOURCE })
        assertTrue(rows.all { it.checkedOn == ProgressionCatalog.SOURCE_CHECKED_ON })
        assertTrue(rows.none { it.step.toLevel > 7 })
    }


    @Test
    fun playgroundCatalogStopsBeforeRoundedCashValues() {
        val rows = ProgressionCatalog.playgroundBuilding
        assertEquals((2..6).toList(), rows.map { it.step.toLevel })
        assertEquals(listOf(319L, 383L, 459L, 2_067L, 5_167L),
            rows.map { it.step.costs.single().quantity })
        assertEquals(listOf(20L, 33L, 57L, 226L, 428L),
            rows.map { it.step.durationSeconds })
        assertEquals((2..6).toList(), rows.map {
            it.prerequisiteNotes.single().substringAfter("Lv.").toInt()
        })
        assertTrue(rows.all { it.step.costs.single().resourceId == "gold" })
        assertTrue(rows.all { it.sourceUrl == ProgressionCatalog.PLAYGROUND_SOURCE })
        assertTrue(rows.all { it.checkedOn == ProgressionCatalog.SOURCE_CHECKED_ON })
        assertTrue(rows.none { it.step.toLevel > 6 })
    }


    @Test
    fun showerRoomCatalogStopsBeforeRoundedCashValues() {
        val rows = ProgressionCatalog.showerRoomBuilding
        assertEquals((2..7).toList(), rows.map { it.step.toLevel })
        assertEquals(listOf(174L, 208L, 250L, 1_125L, 2_813L, 5_626L),
            rows.map { it.step.costs.single().quantity })
        assertEquals(listOf(11L, 18L, 31L, 123L, 233L, 450L),
            rows.map { it.step.durationSeconds })
        assertEquals((2..7).toList(), rows.map {
            it.prerequisiteNotes.single().substringAfter("Lv.").toInt()
        })
        assertTrue(rows.all { it.step.costs.single().resourceId == "gold" })
        assertTrue(rows.all { it.sourceUrl == ProgressionCatalog.SHOWER_ROOM_SOURCE })
        assertTrue(rows.all { it.checkedOn == ProgressionCatalog.SOURCE_CHECKED_ON })
        assertTrue(rows.none { it.step.toLevel > 7 })
    }

    @Test
    fun cafeteriaCatalogContainsExactCashCostsThroughLevelSeven() {
        val rows = ProgressionCatalog.cafeteriaBuilding
        assertEquals((2..7).toList(), rows.map { it.step.toLevel })
        assertEquals(listOf(185L, 222L, 267L, 1_200L, 3_000L, 6_001L),
            rows.map { it.step.costs.single().quantity })
        assertEquals(listOf(12L, 19L, 33L, 131L, 249L, 480L),
            rows.map { it.step.durationSeconds })
        assertEquals((2..7).toList(), rows.map {
            it.prerequisiteNotes.single().substringAfter("Lv.").toInt()
        })
        assertTrue(rows.all { it.step.costs.single().resourceId == "gold" })
        assertTrue(rows.all { it.sourceUrl == ProgressionCatalog.CAFETERIA_SOURCE })
        assertTrue(rows.all { it.checkedOn == ProgressionCatalog.SOURCE_CHECKED_ON })
        assertTrue(rows.none { it.step.toLevel > 7 })
    }

    @Test
    fun medicalStationCatalogContainsOnlyExactEarlyUpgradeCosts() {
        val rows = ProgressionCatalog.medicalStationBuilding
        assertEquals((2..5).toList(), rows.map { it.step.toLevel })
        assertEquals(listOf(9L, 40L, 125L, 250L), rows.map { it.step.durationSeconds })
        assertEquals(listOf(
            mapOf("wood" to 150L),
            mapOf("wood" to 900L),
            mapOf("wood" to 2_000L, "steel" to 400L),
            mapOf("wood" to 9_000L, "steel" to 1_800L)
        ), rows.map { row -> row.step.costs.associate { it.resourceId to it.quantity } })
        assertTrue(rows.all { it.prerequisiteNotes == listOf("Warden's Office Lv.8") })
        assertTrue(rows.all { it.sourceUrl == ProgressionCatalog.MEDICAL_STATION_SOURCE })
        assertTrue(rows.all { it.checkedOn == ProgressionCatalog.SOURCE_CHECKED_ON })
        assertTrue(rows.none { it.step.toLevel > 5 })
    }

    @Test
    fun communicationCenterCatalogContainsOnlyExactEarlyUpgradeCosts() {
        val rows = ProgressionCatalog.communicationCenterBuilding
        assertEquals((2..5).toList(), rows.map { it.step.toLevel })
        assertEquals(listOf(10L, 60L, 120L, 400L), rows.map { it.step.durationSeconds })
        assertEquals(listOf(
            mapOf("wood" to 200L),
            mapOf("wood" to 1_000L),
            mapOf("wood" to 2_000L, "steel" to 400L),
            mapOf("wood" to 8_000L, "steel" to 1_600L)
        ), rows.map { row -> row.step.costs.associate { it.resourceId to it.quantity } })
        assertTrue(rows.all { it.prerequisiteNotes == listOf("Warden's Office Lv.8") })
        assertTrue(rows.all { it.sourceUrl == ProgressionCatalog.COMMUNICATION_CENTER_SOURCE })
        assertTrue(rows.all { it.checkedOn == ProgressionCatalog.SOURCE_CHECKED_ON })
        assertTrue(rows.none { it.step.toLevel > 5 })
    }

    @Test
    fun commandCenterCatalogStopsBeforeAbbreviatedCashAmounts() {
        val rows = ProgressionCatalog.commandCenterBuilding
        assertEquals((2..4).toList(), rows.map { it.step.toLevel })
        assertEquals(listOf(8L, 34L, 105L), rows.map { it.step.durationSeconds })
        assertEquals(listOf(
            mapOf("wood" to 200L),
            mapOf("wood" to 1_200L),
            mapOf("wood" to 2_400L, "steel" to 480L)
        ), rows.map { row -> row.step.costs.associate { it.resourceId to it.quantity } })
        assertTrue(rows.all { it.prerequisiteNotes == listOf("Warden's Office Lv.10") })
        assertTrue(rows.all { it.sourceUrl == ProgressionCatalog.COMMAND_CENTER_SOURCE })
        assertTrue(rows.none { it.step.toLevel > 4 })
    }

    @Test
    fun threeBarracksCatalogsUseTheirOwnWardenPrerequisites() {
        val catalogs = listOf(
            Triple(ProgressionCatalog.shieldbearerBarrackBuilding, ProgressionCatalog.SHIELDBEARER_BARRACK_SOURCE, 7),
            Triple(ProgressionCatalog.shooterBarrackBuilding, ProgressionCatalog.SHOOTER_BARRACK_SOURCE, 8),
            Triple(ProgressionCatalog.bomberBarrackBuilding, ProgressionCatalog.BOMBER_BARRACK_SOURCE, 9)
        )
        catalogs.forEach { (rows, source, wardenLevel) ->
            assertEquals((2..4).toList(), rows.map { it.step.toLevel })
            assertEquals(listOf(9L, 45L, 135L), rows.map { it.step.durationSeconds })
            assertEquals(listOf(
                mapOf("wood" to 300L),
                mapOf("wood" to 1_300L),
                mapOf("wood" to 3_000L, "steel" to 600L)
            ), rows.map { row -> row.step.costs.associate { it.resourceId to it.quantity } })
            assertTrue(rows.all { it.prerequisiteNotes == listOf("Warden's Office Lv.$wardenLevel") })
            assertTrue(rows.all { it.sourceUrl == source && it.checkedOn == ProgressionCatalog.SOURCE_CHECKED_ON })
            assertTrue(rows.none { it.step.toLevel > 4 })
        }
    }

    @Test
    fun wardensOfficeCatalogCoversStandardLevelsTwoToThirtyWithExplicitPrerequisites() {
        val rows = ProgressionCatalog.wardensOffice

        assertEquals((2..30).toList(), rows.map { it.step.toLevel })
        assertTrue(rows.all { it.step.fromLevel == it.step.toLevel - 1 })
        assertTrue(rows.all { it.step.itemName == "Warden's Office" })
        assertTrue(rows.all { it.step.dataComplete })
        assertTrue(rows.all { it.sourceUrl == ProgressionCatalog.WARDENS_OFFICE_SOURCE })
        assertTrue(rows.all { it.checkedOn == ProgressionCatalog.SOURCE_CHECKED_ON })
        assertTrue(rows.all { it.prerequisiteNotes.isNotEmpty() })
        assertTrue(rows.all { row ->
            row.step.costs.isNotEmpty() && row.step.costs.all {
                it.confidence == CostConfidence.COMMUNITY_REFERENCE &&
                    it.sourceUrl == ProgressionCatalog.WARDENS_OFFICE_SOURCE
            }
        })
    }

    @Test
    fun wardensOfficeFirstLevelsKeepExactPublishedCostsAndBaseDurations() {
        val firstFour = ProgressionCatalog.wardensOffice
            .filter { it.step.toLevel in 2..5 }

        assertEquals(listOf(6L, 60L, 180L, 600L), firstFour.map { it.step.durationSeconds })
        assertEquals(
            listOf(
                mapOf("wood" to 350L),
                mapOf("wood" to 1_600L),
                mapOf("wood" to 3_600L, "steel" to 700L),
                mapOf("wood" to 15_000L, "steel" to 3_000L)
            ),
            firstFour.map { row -> row.step.costs.associate { it.resourceId to it.quantity } }
        )
    }

    @Test
    fun wardensOfficePrerequisitesRemainNotesRatherThanInventedCatalogSteps() {
        val levelSix = ProgressionCatalog.wardensOffice.single { it.step.toLevel == 6 }
        val levelTen = ProgressionCatalog.wardensOffice.single { it.step.toLevel == 10 }

        assertEquals(listOf("Supply Center Lv.1", "Shower Room 1 Lv.5"), levelSix.prerequisiteNotes)
        assertEquals(emptyList<String>(), levelSix.step.prerequisites)
        assertEquals(listOf("Shooter Barracks Lv.9", "Lab Lv.1"), levelTen.prerequisiteNotes)
        assertEquals(emptyList<String>(), levelTen.step.prerequisites)
    }

    @Test
    fun allPublishedCatalogDurationsAndCostsStayNonNegative() {
        val catalogs = listOf(
            ProgressionCatalog.labBuilding,
            ProgressionCatalog.wardensOffice,
            ProgressionCatalog.gateBuilding,
            ProgressionCatalog.kitchenBuilding,
            ProgressionCatalog.cellBuilding,
            ProgressionCatalog.factoryBuilding,
            ProgressionCatalog.utilityRoomBuilding,
            ProgressionCatalog.guardRoomBuilding,
            ProgressionCatalog.inventoryBuilding,
            ProgressionCatalog.laundryBuilding,
            ProgressionCatalog.playgroundBuilding,
            ProgressionCatalog.showerRoomBuilding,
            ProgressionCatalog.cafeteriaBuilding,
            ProgressionCatalog.medicalStationBuilding,
            ProgressionCatalog.communicationCenterBuilding,
            ProgressionCatalog.commandCenterBuilding,
            ProgressionCatalog.shieldbearerBarrackBuilding,
            ProgressionCatalog.shooterBarrackBuilding,
            ProgressionCatalog.bomberBarrackBuilding
        )

        catalogs.flatten().forEach { row ->
            assertTrue(row.step.durationSeconds == null || row.step.durationSeconds >= 0)
            assertTrue(row.step.costs.all { it.quantity >= 0 })
            assertTrue(row.step.toLevel > row.step.fromLevel)
        }
    }

    @Test
    fun robotLevelCatalogKeepsThePublishedBandsAndFlagsTheConflictingCell() {
        val rows = ProgressionCatalog.robotLeveling
        assertEquals(listOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100), rows.map { it.step.toLevel })
        assertTrue(rows.all { it.step.domain == ProgressionDomain.ROBOT })
        assertTrue(rows.all { it.sourceUrl == ProgressionCatalog.ROBOT_UPGRADE_SOURCE })
        assertTrue(rows.all { it.checkedOn == ProgressionCatalog.ROBOT_SOURCE_CHECKED_ON })

        val conflicting = rows.single { it.step.fromLevel == 60 }
        assertFalse(conflicting.step.dataComplete)
        assertEquals(CostConfidence.NEEDS_CONFIRMATION,
            conflicting.step.costs.single { it.resourceId == "advanced_power_modules" }.confidence)

        val plan = ProgressionCoach.calculate(rows.map { it.step }, emptyList())
        assertEquals(listOf(conflicting.step.id), plan.unknownStepIds)
        assertEquals(141_050L, plan.requirements.single { it.resourceId == "prisoner armor data" }.required)
        assertEquals(1_040L, plan.requirements.single { it.resourceId == "power modules" }.required)
        assertEquals(285L, plan.requirements.single { it.resourceId == "advanced power modules" }.required)
    }

    @Test
    fun robotCoreCatalogTotalsOneModuleAndKeepsModuleScopeExplicit() {
        val rows = ProgressionCatalog.robotCore
        assertEquals((1..5).toList(), rows.map { it.step.toLevel })
        assertTrue(rows.all { it.step.domain == ProgressionDomain.ROBOT_CORE })
        assertTrue(rows.all { it.prerequisiteNotes.single().contains("Per module") })
        val plan = ProgressionCoach.calculate(rows.map { it.step }, emptyList())
        assertEquals(116_460L, plan.requirements.single { it.resourceId == "core chips" }.required)
    }

    @Test
    fun heroEquipmentCatalogMatchesPublishedPromotionTotals() {
        val rows = ProgressionCatalog.heroEquipmentLeveling
        assertEquals(10, rows.size)
        assertEquals(listOf(20, 40, 60, 80, 100, 120, 140, 160, 180, 200),
            rows.map { it.step.toLevel })
        assertTrue(rows.all { it.step.domain == ProgressionDomain.EQUIPMENT })
        assertTrue(rows.all { it.sourceUrl == ProgressionCatalog.HERO_EQUIPMENT_SOURCE })
        val plan = ProgressionCoach.calculate(rows.map { it.step }, emptyList())
        assertEquals(1_213_840L, plan.requirements.single { it.resourceId == "equipment exp" }.required)
        assertEquals(7_380L, plan.requirements.single { it.resourceId == "magnets" }.required)
        val withReform = ProgressionCoach.calculate(
            (rows.map { it.step } + ProgressionCatalog.heroEquipmentReform.map { it.step }),
            emptyList()
        )
        assertEquals(18_380L, withReform.requirements.single { it.resourceId == "magnets" }.required)
        assertEquals(300L, plan.requirements.single { it.resourceId == "potential coils" }.required)
    }

    @Test
    fun heroEquipmentReformIsSeparateAndUsesPrecisionGears() {
        val plan = ProgressionCoach.calculate(
            ProgressionCatalog.heroEquipmentReform.map { it.step },
            emptyList()
        )
        assertEquals(11_000L, plan.requirements.single { it.resourceId == "magnets" }.required)
        assertEquals(4_200L, plan.requirements.single { it.resourceId == "precision gears" }.required)
    }

    @Test
    fun heroShardProgressionTotalsOneRecruitmentThroughFiveStars() {
        val rows = ProgressionCatalog.heroShardProgression
        assertEquals(31, rows.size)
        assertTrue(rows.all { it.step.domain == ProgressionDomain.HERO })
        assertTrue(rows.all { it.sourceUrl == ProgressionCatalog.HERO_SHARDS_SOURCE })
        val plan = ProgressionCoach.calculate(rows.map { it.step }, emptyList())
        assertEquals(1_075L, plan.requirements.single { it.resourceId == "hero shards" }.required)
    }


    @Test
    fun sourcedPrerequisitesRequireAConfirmedStartingLevelAndStayWithinTarget() {
        val catalog = ProgressionCatalog.wardensOffice.take(3)

        assertTrue(sourcedPrerequisitesForRange(catalog, null, 4).isEmpty())
        assertTrue(sourcedPrerequisitesForRange(catalog, 3, 3).isEmpty())
        assertEquals(
            listOf(4 to "Factory Lv.3"),
            sourcedPrerequisitesForRange(catalog, 3, 4)
        )
    }
}
