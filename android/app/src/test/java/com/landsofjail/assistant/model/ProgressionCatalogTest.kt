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
            ProgressionCatalog.kitchenBuilding to ProgressionCatalog.KITCHEN_SOURCE,
            ProgressionCatalog.cellBuilding to ProgressionCatalog.CELL_SOURCE,
            ProgressionCatalog.factoryBuilding to ProgressionCatalog.FACTORY_SOURCE,
            ProgressionCatalog.utilityRoomBuilding to ProgressionCatalog.UTILITY_ROOM_SOURCE,
            ProgressionCatalog.guardRoomBuilding to ProgressionCatalog.GUARD_ROOM_SOURCE
        )

        catalogs.forEach { (rows, source) ->
            assertEquals((2..6).toList(), rows.map { it.step.toLevel })
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
        val catalogs = listOf(
            ProgressionCatalog.kitchenBuilding,
            ProgressionCatalog.cellBuilding,
            ProgressionCatalog.factoryBuilding,
            ProgressionCatalog.utilityRoomBuilding,
            ProgressionCatalog.guardRoomBuilding
        )
        assertTrue(catalogs.all { rows -> rows.none { it.step.toLevel > 6 } })
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

}
