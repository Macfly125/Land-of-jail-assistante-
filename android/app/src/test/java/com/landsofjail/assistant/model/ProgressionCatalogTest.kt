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
}
