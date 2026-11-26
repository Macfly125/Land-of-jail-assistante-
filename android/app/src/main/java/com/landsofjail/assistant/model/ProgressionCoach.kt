package com.landsofjail.assistant.model

import kotlinx.serialization.Serializable

/**
 * Offline-first progression calculator. Only verified cost rows are summed;
 * unknown rows stay explicitly unknown and are never estimated by interpolation.
 */
@Serializable
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

@Serializable
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
    val missingPrerequisiteIds: List<String>,
    val cyclicStepIds: List<String>,
    val totalKnownDurationSeconds: Long?
)

object ProgressionCoach {
    fun calculate(
        steps: List<ProgressionStep>,
        stocks: List<PlayerResourceStock>
    ): ProgressionPlan {
        val byId = steps.associateBy { it.id }
        val missingPrerequisites = steps.flatMap { it.prerequisites }
            .filterNot { it in byId }.distinct()

        // Stable topological ordering: prerequisites are always planned before dependants.
        val remaining = steps.associateBy { it.id }.toMutableMap()
        val ordered = mutableListOf<ProgressionStep>()
        while (remaining.isNotEmpty()) {
            val ready = steps.filter { step ->
                step.id in remaining && step.prerequisites.all { prerequisite ->
                    prerequisite !in byId || ordered.any { it.id == prerequisite }
                }
            }
            if (ready.isEmpty()) break
            ready.forEach { step ->
                ordered += step
                remaining.remove(step.id)
            }
        }
        val cyclicIds = remaining.keys.toList()

        val confirmedStocks = stocks.filter { it.confirmed }
            .groupBy { canonicalProgressionResourceId(it.resourceId) }
            .mapValues { (_, entries) ->
                val latestTimestamp = entries.maxOf { it.updatedAtEpochMillis }
                val latestQuantities = entries
                    .filter { it.updatedAtEpochMillis == latestTimestamp }
                    .map { it.quantity }
                    .distinct()
                latestQuantities.singleOrNull()
            }

        // Normalize localized resource names at the calculation boundary too:
        // callers outside the UI must get the same safe totals.
        // Costs flagged NEEDS_CONFIRMATION are deliberately excluded from totals.
        val allCosts = steps.flatMap { step ->
            step.costs.filter { it.confidence != CostConfidence.NEEDS_CONFIRMATION }
                .map { it.copy(resourceId = canonicalProgressionResourceId(it.resourceId)) }
        }.groupBy { it.resourceId }
        val requirements = allCosts.map { (resourceId, costs) ->
            // Saturate instead of wrapping to a negative requirement on very large totals.
            val required = costs.fold(0L) { total, cost ->
                if (Long.MAX_VALUE - total < cost.quantity) Long.MAX_VALUE
                else total + cost.quantity
            }
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

        val unknownIds = steps.filter { step ->
            !step.dataComplete ||
                step.costs.any { it.confidence == CostConfidence.NEEDS_CONFIRMATION } ||
                step.prerequisites.any { it !in byId }
        }.map { it.id }

        return ProgressionPlan(
            steps = ordered + steps.filter { it.id in cyclicIds },
            requirements = requirements,
            unknownStepIds = unknownIds,
            missingPrerequisiteIds = missingPrerequisites,
            cyclicStepIds = cyclicIds,
            totalKnownDurationSeconds = if (steps.all { it.dataComplete && it.durationSeconds != null } &&
                missingPrerequisites.isEmpty() && cyclicIds.isEmpty())
                steps.fold(0L) { total, step ->
                    val duration = step.durationSeconds!!
                    if (Long.MAX_VALUE - total < duration) Long.MAX_VALUE else total + duration
                } else null
        )
    }

    fun freeSourceAdvice(resourceId: String, unlockedSources: Map<String, List<String>>): List<String> =
        unlockedSources[resourceId].orEmpty().distinct()
}
