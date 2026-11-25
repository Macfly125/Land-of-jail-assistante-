package com.landsofjail.assistant.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun freeSourceAdviceReturnsOnlyConfiguredSourcesWithoutInventingAny() {
        assertEquals(listOf("Alliance shop", "Daily missions"),
            ProgressionCoach.freeSourceAdvice("steel", mapOf("steel" to listOf("Alliance shop", "Daily missions"))))
        assertEquals(emptyList<String>(), ProgressionCoach.freeSourceAdvice("unknown", emptyMap()))
    }
}
