package com.landsofjail.assistant.model

/**
 * Source-backed reference rows for progression planning.
 *
 * These are community-reference values extracted from the LOJ Wiki table,
 * not values verified against this player's live game client. Keep the
 * provenance visible and never label them as in-game verified.
 */
data class ProgressionCatalogRecord(
    val step: ProgressionStep,
    val prerequisiteNotes: List<String>,
    val sourceLabel: String,
    val sourceUrl: String,
    val checkedOn: String
)

object ProgressionCatalog {
    const val LAB_BUILDING_SOURCE = "https://lojwiki.com/buildings/lab/"
    const val SOURCE_CHECKED_ON = "2026-11-25"

    /**
     * Lab building upgrades Lv.1 -> Lv.30. Costs and base times are copied
     * level-by-level from the source table; time bonuses are not applied.
     * Resource IDs use stable English keys for calculator matching.
     */
    val labBuilding: List<ProgressionCatalogRecord> = listOf(
        row(2, listOf("wood" to 300L), 9, 9),
        row(3, listOf("wood" to 1_500L), 45, 9),
        row(4, listOf("wood" to 3_000L, "steel" to 600L), 135, 9),
        row(5, listOf("wood" to 14_000L, "steel" to 2_800L), 270, 9),
        row(6, listOf("wood" to 35_000L, "steel" to 7_000L, "gasoline" to 1_750L), 540, 9),
        row(7, listOf("wood" to 120_000L, "steel" to 24_000L, "gasoline" to 6_000L), 1_080, 9),
        row(8, listOf("wood" to 200_000L, "steel" to 40_000L, "gasoline" to 10_000L), 1_620, 9),
        row(9, listOf("wood" to 450_000L, "steel" to 90_000L, "gasoline" to 22_500L), 2_430, 9),
        row(10, listOf("wood" to 800_000L, "steel" to 160_000L, "gasoline" to 40_000L), 3_240, 10),
        row(11, listOf("food" to 1_000_000L, "wood" to 1_000_000L, "steel" to 200_000L, "gasoline" to 50_000L), 4_020, 11),
        row(12, listOf("food" to 1_400_000L, "wood" to 1_400_000L, "steel" to 280_000L, "gasoline" to 70_000L), 4_860, 12),
        row(13, listOf("food" to 2_000_000L, "wood" to 2_000_000L, "steel" to 400_000L, "gasoline" to 100_000L), 5_940, 13),
        row(14, listOf("food" to 2_500_000L, "wood" to 2_500_000L, "steel" to 500_000L, "gasoline" to 125_000L), 7_560, 14),
        row(15, listOf("food" to 3_500_000L, "wood" to 3_500_000L, "steel" to 700_000L, "gasoline" to 175_000L), 9_720, 15),
        row(16, listOf("food" to 4_500_000L, "wood" to 4_500_000L, "steel" to 900_000L, "gasoline" to 225_000L), 16_440, 16),
        row(17, listOf("food" to 7_500_000L, "wood" to 7_500_000L, "steel" to 1_500_000L, "gasoline" to 375_000L), 19_740, 17),
        row(18, listOf("food" to 10_000_000L, "wood" to 10_000_000L, "steel" to 2_000_000L, "gasoline" to 500_000L), 24_660, 18),
        row(19, listOf("food" to 12_500_000L, "wood" to 12_500_000L, "steel" to 2_500_000L, "gasoline" to 625_000L), 35_520, 19),
        row(20, listOf("food" to 17_500_000L, "wood" to 17_500_000L, "steel" to 3_500_000L, "gasoline" to 875_000L), 44_400, 20),
        row(21, listOf("food" to 20_000_000L, "wood" to 20_000_000L, "steel" to 4_000_000L, "gasoline" to 1_000_000L), 57_720, 21),
        row(22, listOf("food" to 30_000_000L, "wood" to 30_000_000L, "steel" to 6_000_000L, "gasoline" to 1_500_000L), 86_640, 22),
        row(23, listOf("food" to 35_000_000L, "wood" to 35_000_000L, "steel" to 7_000_000L, "gasoline" to 1_800_000L), 118_800, 23),
        row(24, listOf("food" to 50_000_000L, "wood" to 50_000_000L, "steel" to 10_000_000L, "gasoline" to 2_500_000L), 169_200, 24),
        row(25, listOf("food" to 65_000_000L, "wood" to 65_000_000L, "steel" to 13_000_000L, "gasoline" to 3_300_000L), 237_600, 25),
        row(26, listOf("food" to 85_000_000L, "wood" to 85_000_000L, "steel" to 17_000_000L, "gasoline" to 4_300_000L), 270_000, 26),
        row(27, listOf("food" to 120_000_000L, "wood" to 120_000_000L, "steel" to 24_000_000L, "gasoline" to 6_000_000L), 334_800, 27),
        row(28, listOf("food" to 160_000_000L, "wood" to 160_000_000L, "steel" to 32_000_000L, "gasoline" to 8_000_000L), 374_400, 28),
        row(29, listOf("food" to 200_000_000L, "wood" to 200_000_000L, "steel" to 40_000_000L, "gasoline" to 10_000_000L), 433_980, 29),
        row(30, listOf("food" to 230_000_000L, "wood" to 230_000_000L, "steel" to 46_000_000L, "gasoline" to 11_500_000L), 520_800, 30)
    )

    private fun row(
        targetLevel: Int,
        costs: List<Pair<String, Long>>,
        baseDurationSeconds: Long,
        requiredWardenOfficeLevel: Int
    ): ProgressionCatalogRecord {
        val costsWithProvenance = costs.map { (resourceId, quantity) ->
            ResourceCost(
                resourceId = resourceId,
                quantity = quantity,
                confidence = CostConfidence.COMMUNITY_REFERENCE,
                sourceUrl = LAB_BUILDING_SOURCE
            )
        }
        return ProgressionCatalogRecord(
            step = ProgressionStep(
                id = "lab-${targetLevel - 1}-$targetLevel",
                domain = ProgressionDomain.BUILDING,
                itemName = "Laboratory",
                fromLevel = targetLevel - 1,
                toLevel = targetLevel,
                costs = costsWithProvenance,
                durationSeconds = baseDurationSeconds,
                dataComplete = true
            ),
            prerequisiteNotes = listOf("Warden's Office Lv.$requiredWardenOfficeLevel"),
            sourceLabel = "LOJ Wiki — Lab building costs",
            sourceUrl = LAB_BUILDING_SOURCE,
            checkedOn = SOURCE_CHECKED_ON
        )
    }
}
