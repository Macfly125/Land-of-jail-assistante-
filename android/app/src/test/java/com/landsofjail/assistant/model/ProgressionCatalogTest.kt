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

        assertEquals((2..7).toList(), rows.map { it.step.toLevel })
        assertTrue(rows.all { it.step.fromLevel == it.step.toLevel - 1 })
        assertTrue(rows.all { it.step.itemName == "Gate" })
        assertTrue(rows.all { it.step.dataComplete })
        assertTrue(rows.all { it.sourceUrl == ProgressionCatalog.GATE_SOURCE })
        assertTrue(rows.all { it.checkedOn == ProgressionCatalog.SOURCE_CHECKED_ON })
        assertTrue(rows.all { it.prerequisiteNotes.isNotEmpty() })
        assertTrue(rows.all { row ->
            row.step.costs.isNotEmpty() && row.step.costs.all {
                it.confidence == CostConfidence.COMMUNITY_REFERENCE &&
                    it.sourceUrl == ProgressionCatalog.GATE_SOURCE
            }
        })
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
    fun gateHigherLevelsAreNotEstimatedFromRoundedWikiAmounts() {
        assertTrue(ProgressionCatalog.gateBuilding.none { it.step.toLevel > 7 })
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

}
