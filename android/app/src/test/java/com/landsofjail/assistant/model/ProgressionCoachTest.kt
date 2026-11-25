package com.landsofjail.assistant.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressionCoachTest {
    @Test
    fun totalsCostsAcrossIntermediateStepsAndSubtractsOnlyConfirmedStock() {
        val steps = listOf(
            ProgressionStep("hq-6-7", ProgressionDomain.BUILDING, "Bureau", 6, 7,
                listOf(ResourceCost("wood", 100, CostConfidence.VERIFIED_IN_GAME))),
            ProgressionStep("hq-7-8", ProgressionDomain.BUILDING, "Bureau", 7, 8,
                listOf(ResourceCost("wood", 150, CostConfidence.COMMUNITY_REFERENCE),
                    ResourceCost("steel", 40, CostConfidence.COMMUNITY_REFERENCE)))
        )
        val plan = ProgressionCoach.calculate(steps, listOf(
            PlayerResourceStock("wood", 120, true, 10),
            PlayerResourceStock("steel", 100, false, 20)
        ))

        assertEquals(250L, plan.requirements.first { it.resourceId == "wood" }.required)
        assertEquals(130L, plan.requirements.first { it.resourceId == "wood" }.missing)
        assertNull(plan.requirements.first { it.resourceId == "steel" }.missing)
        assertEquals(40L, plan.requirements.first { it.resourceId == "steel" }.required)
    }

    @Test
    fun unknownDataIsFlaggedAndDurationIsNotInvented() {
        val step = ProgressionStep("robot-1-2", ProgressionDomain.ROBOT, "Robot", 1, 2,
            emptyList(), dataComplete = false)
        val plan = ProgressionCoach.calculate(listOf(step), emptyList())

        assertEquals(listOf("robot-1-2"), plan.unknownStepIds)
        assertNull(plan.totalKnownDurationSeconds)
    }

    @Test
    fun uncertainCostsAreNotAddedToConfirmedTotals() {
        val step = ProgressionStep("hq-8-9", ProgressionDomain.BUILDING, "Bureau", 8, 9,
            listOf(
                ResourceCost("wood", 100, CostConfidence.VERIFIED_IN_GAME),
                ResourceCost("steel", 900, CostConfidence.NEEDS_CONFIRMATION)
            ))
        val plan = ProgressionCoach.calculate(listOf(step), emptyList())

        assertEquals(100L, plan.requirements.single().required)
        assertEquals("wood", plan.requirements.single().resourceId)
        assertEquals(listOf("hq-8-9"), plan.unknownStepIds)
    }

    @Test
    fun prerequisitesAreOrderedAndMissingOrCyclicLinksAreReported() {
        val dependent = ProgressionStep("lab-2", ProgressionDomain.RESEARCH, "Lab", 1, 2,
            emptyList(), prerequisites = listOf("lab-1"))
        val prerequisite = ProgressionStep("lab-1", ProgressionDomain.BUILDING, "Lab building", 1, 2,
            emptyList())
        val plan = ProgressionCoach.calculate(listOf(dependent, prerequisite), emptyList())
        assertEquals(listOf("lab-1", "lab-2"), plan.steps.map { it.id })
        assertTrue(plan.missingPrerequisiteIds.isEmpty())
        assertTrue(plan.cyclicStepIds.isEmpty())

        val missing = ProgressionCoach.calculate(
            listOf(dependent.copy(prerequisites = listOf("not-in-plan"))), emptyList())
        assertEquals(listOf("not-in-plan"), missing.missingPrerequisiteIds)
        assertNull(missing.totalKnownDurationSeconds)

        val cycle = ProgressionCoach.calculate(
            listOf(
                prerequisite.copy(prerequisites = listOf("lab-2")),
                dependent.copy(prerequisites = listOf("lab-1"))
            ), emptyList())
        assertEquals(setOf("lab-1", "lab-2"), cycle.cyclicStepIds.toSet())
    }

    @Test
    fun freeSourceAdviceReturnsOnlyConfiguredSourcesWithoutInventingAny() {
        assertEquals(listOf("Alliance shop", "Daily missions"),
            ProgressionCoach.freeSourceAdvice("steel", mapOf("steel" to listOf("Alliance shop", "Daily missions"))))
        assertEquals(emptyList<String>(), ProgressionCoach.freeSourceAdvice("unknown", emptyMap()))
    }
}
