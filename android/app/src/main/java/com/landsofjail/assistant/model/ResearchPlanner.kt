package com.landsofjail.assistant.model

/**
 * Exact per-level research data. Populate only from a verified source row;
 * aggregate tree totals must never be converted into these records.
 */
data class ResearchResources(
    val food: Long = 0L,
    val wood: Long = 0L,
    val steel: Long = 0L,
    val gasoline: Long = 0L,
    val goldCard: Long = 0L,
    val hyperalloy: Long = 0L
) {
    init {
        require(listOf(food, wood, steel, gasoline, goldCard, hyperalloy).all { it >= 0L })
    }

    operator fun plus(other: ResearchResources) = ResearchResources(
        food + other.food,
        wood + other.wood,
        steel + other.steel,
        gasoline + other.gasoline,
        goldCard + other.goldCard,
        hyperalloy + other.hyperalloy
    )
}

data class ResearchRequirement(val technologyId: String, val level: Int) {
    init {
        require(technologyId.isNotBlank())
        require(level >= 1)
    }
}

data class ResearchLevelData(
    val technologyId: String,
    val level: Int,
    val labLevel: Int,
    val baseDurationSeconds: Long,
    val power: Long,
    val cost: ResearchResources,
    val prerequisites: List<ResearchRequirement> = emptyList()
) {
    init {
        require(technologyId.isNotBlank())
        require(level >= 1 && labLevel >= 1)
        require(baseDurationSeconds >= 0L && power >= 0L)
    }

    val key: String get() = "$technologyId#$level"
}

data class ResearchSpeedupInventory(
    val researchMinutes: Map<Int, Int> = emptyMap(),
    val universalMinutes: Map<Int, Int> = emptyMap()
) {
    init {
        (researchMinutes.values + universalMinutes.values).forEach { require(it >= 0) }
        require(researchMinutes.keys.all { it in RESEARCH_SPEEDUP_MINUTES })
        require(universalMinutes.keys.all { it in UNIVERSAL_SPEEDUP_MINUTES })
    }

    companion object {
        val RESEARCH_SPEEDUP_MINUTES = setOf(1, 5, 30, 60, 180, 480)
        val UNIVERSAL_SPEEDUP_MINUTES = setOf(1, 5, 60, 180, 480)
    }
}

data class ResearchPlanRequest(
    val targets: Map<String, Int>,
    val currentLevels: Map<String, Int>,
    val labLevel: Int,
    /** Research-speed bonus as a decimal: 0.25 means +25%. */
    val researchSpeedBonus: Double = 0.0,
    val returningPlayerTechnologyBonus: Boolean = false,
    val allianceHelpCount: Int = 0,
    val secondsPerAllianceHelp: Long = 0L,
    val speedups: ResearchSpeedupInventory = ResearchSpeedupInventory()
) {
    init {
        require(targets.isNotEmpty())
        require(targets.values.all { it >= 0 })
        require(currentLevels.values.all { it >= 0 })
        require(labLevel >= 1)
        require(researchSpeedBonus >= 0.0 && researchSpeedBonus.isFinite())
        require(allianceHelpCount >= 0 && secondsPerAllianceHelp >= 0L)
    }
}

data class PlannedResearchStep(
    val data: ResearchLevelData,
    val requiredSeconds: Long,
    val allianceHelpSeconds: Long,
    val researchSpeedupSeconds: Long,
    val universalSpeedupSeconds: Long,
    val diamondCost: Long
)

data class ResearchPlanResult(
    val steps: List<PlannedResearchStep>,
    val totalCost: ResearchResources,
    val totalBaseSeconds: Long,
    val totalRequiredSeconds: Long,
    val totalAllianceHelpSeconds: Long,
    val totalResearchSpeedupSeconds: Long,
    val totalUniversalSpeedupSeconds: Long,
    val totalDiamonds: Long,
    val missingLabLevels: Map<String, Int>
)

/**
 * Builds a dependency-complete, sequential plan. The caller supplies verified
 * level rows; missing rows or prerequisites are errors rather than guesses.
 */
object ResearchPlanner {
    private val speedupPriority = listOf(480, 180, 60, 30, 5, 1)

    fun build(
        catalog: List<ResearchLevelData>,
        request: ResearchPlanRequest
    ): ResearchPlanResult {
        val rows = catalog.associateBy { it.key }
        require(rows.size == catalog.size) { "Duplicate research level rows" }
        val ordered = mutableListOf<ResearchLevelData>()
        val visiting = mutableSetOf<String>()
        val included = mutableSetOf<String>()
        val current = request.currentLevels

        fun includeThrough(technologyId: String, targetLevel: Int) {
            val currentLevel = current[technologyId] ?: 0
            require(targetLevel >= currentLevel) {
                "Required level is below current level for $technologyId"
            }
            for (level in (currentLevel + 1)..targetLevel) {
                val key = "$technologyId#$level"
                if (key in included) continue
                require(key !in visiting) { "Research prerequisite cycle at $key" }
                val row = requireNotNull(rows[key]) { "Missing verified research row: $key" }
                visiting += key
                row.prerequisites.forEach { includeThrough(it.technologyId, it.level) }
                visiting -= key
                included += key
                ordered += row
            }
        }

        request.targets.forEach { (technologyId, targetLevel) ->
            includeThrough(technologyId, targetLevel)
        }

        val missingLab = ordered.filter { it.labLevel > request.labLevel }
            .associate { it.key to it.labLevel }
        val resources = ordered.fold(ResearchResources()) { total, row -> total + row.cost }

        var researchInventory = request.speedups.researchMinutes.toMutableMap()
        var universalInventory = request.speedups.universalMinutes.toMutableMap()
        var totalBase = 0L
        var totalRequired = 0L
        var totalHelp = 0L
        var totalResearchUsed = 0L
        var totalUniversalUsed = 0L
        var totalDiamonds = 0L

        val steps = ordered.map { row ->
            val base = row.baseDurationSeconds
            val speedAdjusted = floor(base / (1.0 + request.researchSpeedBonus) /
                if (request.returningPlayerTechnologyBonus) 1.15 else 1.0).toLong()
                .coerceAtLeast(0L)
            val help = minOf(
                speedAdjusted,
                request.allianceHelpCount.toLong() * request.secondsPerAllianceHelp
            )
            var remaining = speedAdjusted - help
            val afterHelp = remaining
            val researchUsed = spendSpeedups(remaining, researchInventory)
            remaining -= researchUsed
            val universalUsed = spendSpeedups(remaining, universalInventory)
            remaining -= universalUsed
            val diamonds = ceil(remaining * (4.0 / 9.0)).toLong()

            totalBase += base
            totalRequired += afterHelp
            totalHelp += help
            totalResearchUsed += researchUsed
            totalUniversalUsed += universalUsed
            totalDiamonds += diamonds

            PlannedResearchStep(
                data = row,
                requiredSeconds = afterHelp,
                allianceHelpSeconds = help,
                researchSpeedupSeconds = researchUsed,
                universalSpeedupSeconds = universalUsed,
                diamondCost = diamonds
            )
        }

        return ResearchPlanResult(
            steps = steps,
            totalCost = resources,
            totalBaseSeconds = totalBase,
            totalRequiredSeconds = totalRequired,
            totalAllianceHelpSeconds = totalHelp,
            totalResearchSpeedupSeconds = totalResearchUsed,
            totalUniversalSpeedupSeconds = totalUniversalUsed,
            totalDiamonds = totalDiamonds,
            missingLabLevels = missingLab
        )
    }

    private fun spendSpeedups(
        seconds: Long,
        inventory: MutableMap<Int, Int>
    ): Long {
        var remaining = seconds
        var used = 0L
        for (minutes in speedupPriority) {
            val itemSeconds = minutes * 60L
            val count = inventory[minutes] ?: 0
            val fitting = minOf(count.toLong(), remaining / itemSeconds).toInt()
            if (fitting > 0) {
                inventory[minutes] = count - fitting
                val reduction = fitting * itemSeconds
                used += reduction
                remaining -= reduction
            }
        }
        return used
    }
}
