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
     * If the top candidate is weak or too close to the runner-up, no identity is
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

        val ranked = compatible
            .map { template -> template to similarity(argbPixels, template.argbPixels) }
            .sortedByDescending { it.second }

        val best = ranked.first()
        val runnerUp = ranked.getOrNull(1)?.second
        val margin = if (runnerUp == null) 1f else best.second - runnerUp
        val confirmed = best.second >= minSimilarity && margin >= minMargin

        return if (confirmed) {
            RobotPortraitMatch(
                best.first.robotId,
                best.first.displayName,
                best.second,
                runnerUp,
                true,
                "portrait_correspondance_confirmee"
            )
        } else {
            RobotPortraitMatch(
                null,
                null,
                best.second,
                runnerUp,
                false,
                if (best.second < minSimilarity) "similarite_insuffisante" else "correspondances_trop_proches"
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
