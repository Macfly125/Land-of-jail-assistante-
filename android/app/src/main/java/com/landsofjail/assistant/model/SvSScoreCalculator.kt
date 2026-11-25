package com.landsofjail.assistant.model

/**
 * Pure SvS score helpers. Values are estimates and must come from the
 * currently configured event scoring table.
 */
object SvSScoreCalculator {
    fun totalPoints(quantity: Long, pointsPerUnit: Long): Long {
        if (quantity <= 0L || pointsPerUnit <= 0L) return 0L
        return if (quantity > Long.MAX_VALUE / pointsPerUnit) {
            Long.MAX_VALUE
        } else {
            quantity * pointsPerUnit
        }
    }

    /** Minimum whole units needed to reach a positive target score. */
    fun unitsNeeded(targetPoints: Long, pointsPerUnit: Long): Long {
        if (targetPoints <= 0L || pointsPerUnit <= 0L) return 0L
        val quotient = targetPoints / pointsPerUnit
        return quotient + if (targetPoints % pointsPerUnit == 0L) 0L else 1L
    }
}
