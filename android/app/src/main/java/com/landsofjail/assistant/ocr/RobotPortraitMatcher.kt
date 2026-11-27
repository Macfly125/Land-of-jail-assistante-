package com.landsofjail.assistant.ocr

/**
 * Conservative portrait matcher.
 *
 * The caller must crop the portrait (excluding labels/stars/frame where possible)
 * and resize it to the exact same dimensions as the catalog templates. Pixels are
 * compared in RGB space. This class deliberately does not download or invent
 * catalog portraits: only templates supplied by a verified local catalog can match.
 */
data class RobotPortraitTemplate(
    val robotId: String,
    val displayName: String,
    val width: Int,
    val height: Int,
    val argbPixels: IntArray
) {
    init {
        require(robotId.isNotBlank()) { "robotId must not be blank" }
        require(displayName.isNotBlank()) { "displayName must not be blank" }
        require(width > 0 && height > 0)
        require(argbPixels.size == width * height)
    }
}

data class RobotPortraitMatch(
    val robotId: String?,
    val displayName: String?,
    /** Similarity in [0, 1], where 1 is an exact pixel match. */
    val similarity: Float,
    val runnerUpSimilarity: Float?,
    val isConfirmed: Boolean,
    val reason: String
)

object RobotPortraitMatcher {
    const val DEFAULT_MIN_SIMILARITY = 0.92f
    const val DEFAULT_MIN_MARGIN = 0.07f

    /**
     * Compare a pre-cropped, normalized screenshot against verified templates.
     * Multiple templates may represent alternate crops/states of the same robot.
     * They are consolidated by robotId before calculating the runner-up margin,
     * so variants of one robot cannot make that robot appear ambiguous with itself.
     * If the top candidate is weak or too close to another robot, no identity is
     * returned. A portrait alone never supplies level, rarity, modules or skills.
     */
    fun match(
        width: Int,
        height: Int,
        argbPixels: IntArray,
        templates: List<RobotPortraitTemplate>,
        minSimilarity: Float = DEFAULT_MIN_SIMILARITY,
        minMargin: Float = DEFAULT_MIN_MARGIN
    ): RobotPortraitMatch {
        require(width > 0 && height > 0)
        require(argbPixels.size == width * height)
        require(minSimilarity in 0f..1f)
        require(minMargin in 0f..1f)

        if (templates.isEmpty()) {
            return RobotPortraitMatch(null, null, 0f, null, false, "catalogue_visuel_vide")
        }

        val compatible = templates.filter { it.width == width && it.height == height }
        if (compatible.isEmpty()) {
            return RobotPortraitMatch(null, null, 0f, null, false, "dimensions_incompatibles")
        }

        // Keep the strongest matching template for each real robot identity.
        // A catalog may legitimately contain several verified portrait variants.
        val rankedRobots = compatible
            .groupBy { it.robotId }
            .mapNotNull { (robotId, variants) ->
                // Un même identifiant ne peut pas désigner plusieurs noms :
                // le catalogue est alors incohérent, donc on refuse cette entrée.
                if (variants.map { it.displayName }.distinct().size != 1) {
                    return@mapNotNull null
                }
                val bestVariant = variants
                    .map { template -> template to similarity(argbPixels, template.argbPixels) }
                    .maxByOrNull { it.second } ?: return@mapNotNull null
                Triple(robotId, bestVariant.first, bestVariant.second)
            }
            .sortedByDescending { it.third }

        if (rankedRobots.isEmpty()) {
            return RobotPortraitMatch(
                null,
                null,
                0f,
                null,
                false,
                "catalogue_identites_incoherentes"
            )
        }

        val best = rankedRobots.first()
        val runnerUp = rankedRobots.getOrNull(1)?.third
        val margin = if (runnerUp == null) 1f else best.third - runnerUp
        val confirmed = best.third >= minSimilarity && margin >= minMargin

        return if (confirmed) {
            RobotPortraitMatch(
                best.first,
                best.second.displayName,
                best.third,
                runnerUp,
                true,
                "portrait_correspondance_confirmee"
            )
        } else {
            RobotPortraitMatch(
                null,
                null,
                best.third,
                runnerUp,
                false,
                if (best.third < minSimilarity) "similarite_insuffisante" else "correspondances_trop_proches"
            )
        }
    }

    private fun similarity(first: IntArray, second: IntArray): Float {
        var absoluteDifference = 0L
        for (index in first.indices) {
            val a = first[index]
            val b = second[index]
            absoluteDifference += kotlin.math.abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF))
            absoluteDifference += kotlin.math.abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF))
            absoluteDifference += kotlin.math.abs((a and 0xFF) - (b and 0xFF))
        }
        val maximumDifference = first.size.toLong() * 3L * 255L
        return (1.0 - absoluteDifference.toDouble() / maximumDifference.toDouble()).toFloat()
    }
}
