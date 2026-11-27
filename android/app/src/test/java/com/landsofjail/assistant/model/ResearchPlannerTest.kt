package com.landsofjail.assistant.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResearchPlannerTest {
    @Test
    fun planIncludesEveryIntermediateLevelAndMissingPrerequisiteBeforeTarget() {
        val catalog = listOf(
            row("logistics", 1, power = 10),
            row("logistics", 2, power = 20),
            row("military", 1, power = 30, prerequisites = listOf(ResearchRequirement("logistics", 2)))
        )

        val result = ResearchPlanner.build(
            catalog,
            ResearchPlanRequest(targets = mapOf("military" to 1), currentLevels = emptyMap(), labLevel = 30)
        )

        assertEquals(
            listOf("logistics#1", "logistics#2", "military#1"),
            result.steps.map { it.data.key }
        )
        assertEquals(60L, result.steps.sumOf { it.data.power })
    }

    @Test
    fun currentPrerequisiteLevelsAreNotScheduledAgain() {
        val catalog = listOf(
            row("logistics", 1),
            row("logistics", 2),
            row("military", 1, prerequisites = listOf(ResearchRequirement("logistics", 2)))
        )
        val result = ResearchPlanner.build(
            catalog,
            ResearchPlanRequest(
                targets = mapOf("military" to 1),
                currentLevels = mapOf("logistics" to 1),
                labLevel = 30
            )
        )
        assertEquals(listOf("logistics#2", "military#1"), result.steps.map { it.data.key })
    }

    @Test
    fun timeUsesResearchSpeedReturnBonusAllianceHelpAndSpeedupsInOrder() {
        val catalog = listOf(row("logistics", 1, duration = 1_000L))
        val result = ResearchPlanner.build(
            catalog,
            ResearchPlanRequest(
                targets = mapOf("logistics" to 1),
                currentLevels = emptyMap(),
                labLevel = 30,
                researchSpeedBonus = 0.25,
                returningPlayerTechnologyBonus = true,
                allianceHelpCount = 2,
                secondsPerAllianceHelp = 30,
                speedups = ResearchSpeedupInventory(
                    researchMinutes = mapOf(5 to 1, 1 to 1),
                    universalMinutes = mapOf(1 to 1)
                )
            )
        )

        val step = result.steps.single()
        assertEquals(695L, step.requiredSeconds)
        assertEquals(60L, step.allianceHelpSeconds)
        assertEquals(360L, step.researchSpeedupSeconds)
        assertEquals(60L, step.universalSpeedupSeconds)
        assertEquals(96L, step.diamondCost)
        assertEquals(96L, result.totalDiamonds)
    }

    @Test
    fun costsAreSummedAndLabRequirementsAreReportedWithoutBeingHidden() {
        val catalog = listOf(
            row("economy", 1, lab = 10, cost = ResearchResources(food = 100, wood = 50)),
            row("economy", 2, lab = 12, cost = ResearchResources(food = 200, steel = 25))
        )
        val result = ResearchPlanner.build(
            catalog,
            ResearchPlanRequest(
                targets = mapOf("economy" to 2),
                currentLevels = emptyMap(),
                labLevel = 9
            )
        )

        assertEquals(ResearchResources(food = 300, wood = 50, steel = 25), result.totalCost)
        assertEquals(mapOf("economy#1" to 10, "economy#2" to 12), result.missingLabLevels)
    }

    @Test(expected = IllegalArgumentException::class)
    fun missingResearchRowsAreRejectedInsteadOfEstimated() {
        ResearchPlanner.build(
            emptyList(),
            ResearchPlanRequest(targets = mapOf("unknown" to 1), currentLevels = emptyMap(), labLevel = 30)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun dependencyCyclesAreRejected() {
        val catalog = listOf(
            row("a", 1, prerequisites = listOf(ResearchRequirement("b", 1))),
            row("b", 1, prerequisites = listOf(ResearchRequirement("a", 1)))
        )
        ResearchPlanner.build(
            catalog,
            ResearchPlanRequest(targets = mapOf("a" to 1), currentLevels = emptyMap(), labLevel = 30)
        )
    }

    private fun row(
        id: String,
        level: Int,
        lab: Int = 1,
        duration: Long = 100L,
        power: Long = 0L,
        cost: ResearchResources = ResearchResources(),
        prerequisites: List<ResearchRequirement> = emptyList()
    ) = ResearchLevelData(
        technologyId = id,
        level = level,
        labLevel = lab,
        baseDurationSeconds = duration,
        power = power,
        cost = cost,
        prerequisites = prerequisites
    )
}
