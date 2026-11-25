package com.landsofjail.assistant.model

/**
 * Offline-first progression calculator. Only verified cost rows are summed;
 * unknown rows stay explicitly unknown and are never estimated by interpolation.
 */
enum class ProgressionDomain {
    BUILDING, HERO, RESEARCH, ROBOT, ROBOT_CORE, EQUIPMENT, TROOPS
}

enum class CostConfidence {
    VERIFIED_IN_GAME, COMMUNITY_REFERENCE, NEEDS_CONFIRMATION
}

data class ResourceCost(
    val resourceId: String,
    val quantity: Long,
    val confidence: CostConfidence,
    val sourceUrl: String? = null
) {
    init {
        require(resourceId.isNotBlank())
        require(quantity >= 0)
    }
}

data class ProgressionStep(
    val id: String,
    val domain: ProgressionDomain,
    val itemName: String,
    val fromLevel: Int,
    val toLevel: Int,
    val costs: List<ResourceCost>,
    val prerequisites: List<String> = emptyList(),
    val durationSeconds: Long? = null,
    val dataComplete: Boolean = true
) {
    init {
        require(id.isNotBlank() && itemName.isNotBlank())
        require(fromLevel >= 0 && toLevel > fromLevel)
        require(durationSeconds == null || durationSeconds >= 0)
    }
}

data class PlayerResourceStock(
    val resourceId: String,
    val quantity: Long,
    val confirmed: Boolean,
    val updatedAtEpochMillis: Long
) {
    init {
        require(resourceId.isNotBlank() && quantity >= 0)
    }
}

data class ResourceRequirement(
    val resourceId: String,
    val required: Long,
    val ownedConfirmed: Long?,
    val missing: Long?,
    val confidence: CostConfidence,
    val sources: List<String>
)

data class ProgressionPlan(
    val steps: List<ProgressionStep>,
    val requirements: List<ResourceRequirement>,
    val unknownStepIds: List<String>,
    val totalKnownDurationSeconds: Long?
)

object ProgressionCoach {
    fun calculate(
        steps: List<ProgressionStep>,
        stocks: List<PlayerResourceStock>
    ): ProgressionPlan {
        val confirmedStocks = stocks.filter { it.confirmed }
            .groupBy { it.resourceId }
            .mapValues { (_, entries) -> entries.maxByOrNull { it.updatedAtEpochMillis }!!.quantity }

        val allCosts = steps.flatMap { it.costs }.groupBy { it.resourceId }
        val requirements = allCosts.map { (resourceId, costs) ->
            val required = costs.sumOf { it.quantity }
            val owned = confirmedStocks[resourceId]
            ResourceRequirement(
                resourceId = resourceId,
                required = required,
                ownedConfirmed = owned,
                missing = owned?.let { (required - it).coerceAtLeast(0L) },
                confidence = costs.maxByOrNull { it.confidence.ordinal }!!.confidence,
                sources = costs.mapNotNull { it.sourceUrl }.distinct()
            )
        }.sortedByDescending { it.missing ?: it.required }

        return ProgressionPlan(
            steps = steps,
            requirements = requirements,
            unknownStepIds = steps.filterNot { it.dataComplete }.map { it.id },
            totalKnownDurationSeconds = if (steps.all { it.durationSeconds != null })
                steps.sumOf { it.durationSeconds!! } else null
        )
    }

    fun freeSourceAdvice(resourceId: String, unlockedSources: Map<String, List<String>>): List<String> =
        unlockedSources[resourceId].orEmpty().distinct()
}
