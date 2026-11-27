package com.landsofjail.assistant.model

/**
 * Community reference costs for one paid Forticlad stage.
 *
 * A Forticlad level contains five paid stages. Values are estimates copied
 * from GuideLOJ's public table and are never treated as in-game verified.
 * Missing rows are deliberately unknown; callers must not extrapolate them.
 */
data class ForticladStageCost(
    val buildingId: String,
    val targetLevel: Int,
    val forticladCores: Long,
    val advancedCores: Long,
    val sourceUrl: String = ForticladCatalog.SOURCE_URL,
    val confidence: CostConfidence = CostConfidence.COMMUNITY_REFERENCE
)

data class ForticladCostEstimate(
    val buildingId: String,
    val fromLevel: Int,
    val targetLevel: Int,
    val completedStagesAtCurrentLevel: Int,
    val forticladCores: Long,
    val advancedCores: Long,
    val includedStages: Int,
    val confidence: CostConfidence,
    val sourceUrl: String
)

object ForticladCatalog {
    const val SOURCE_URL = "https://guideloj.com/en/calculators/forticlad"
    const val RULES_SOURCE_URL = "https://lojwiki.com/calculator/forticlad/"
    const val FORTICLAD_LAB_SOURCE_URL = "https://lojwiki.com/buildings/forticlad-lab/"
    const val MEDICAL_STATION_SOURCE_URL = "https://lojwiki.com/buildings/medical-station/"
    const val CHECKED_ON = "2026-11-27"
    const val STAGES_PER_LEVEL = 5

    /** All eight buildings are eligible; only rows with published costs appear below. */
    val eligibleBuildingIds = setOf(
        "wardens_office", "shieldbearer_barracks", "bomber_barracks",
        "shooter_barracks", "communication_center", "medical_station",
        "command_center", "forticlad_lab"
    )

    private fun row(id: String, level: Int, fc: Long, afc: Long = 0L, source: String = SOURCE_URL) =
        ForticladStageCost(id, level, fc, afc, sourceUrl = source)

    /**
     * Per-stage costs. GuideLOJ publishes Medical Station only through FC8
     * and does not publish Forticlad Lab costs in this table. FC9/FC10
     * Medical Station rows are also absent. Do not fill these gaps by analogy.
     */
    val stageCosts: List<ForticladStageCost> = listOf(
        row("wardens_office", 1, 250), row("shieldbearer_barracks", 1, 130),
        row("bomber_barracks", 1, 130), row("shooter_barracks", 1, 130),
        row("communication_center", 1, 50), row("command_center", 1, 50),
        row("medical_station", 1, 50),

        row("wardens_office", 2, 320), row("shieldbearer_barracks", 2, 150),
        row("bomber_barracks", 2, 150), row("shooter_barracks", 2, 150),
        row("communication_center", 2, 60), row("command_center", 2, 60),
        row("medical_station", 2, 60),

        row("wardens_office", 3, 490), row("shieldbearer_barracks", 3, 230),
        row("bomber_barracks", 3, 230), row("shooter_barracks", 3, 230),
        row("communication_center", 3, 80), row("command_center", 3, 80),
        row("medical_station", 3, 80),

        row("wardens_office", 4, 600), row("shieldbearer_barracks", 4, 250),
        row("bomber_barracks", 4, 250), row("shooter_barracks", 4, 250),
        row("communication_center", 4, 110), row("command_center", 4, 110),
        row("medical_station", 4, 110),

        row("wardens_office", 5, 720), row("shieldbearer_barracks", 5, 300),
        row("bomber_barracks", 5, 300), row("shooter_barracks", 5, 300),
        row("communication_center", 5, 130), row("command_center", 5, 130),
        row("medical_station", 5, 130),

        row("wardens_office", 6, 360, 24), row("shieldbearer_barracks", 6, 160, 10),
        row("bomber_barracks", 6, 160, 10), row("shooter_barracks", 6, 160, 10),
        row("communication_center", 6, 90, 5), row("command_center", 6, 70, 5),
        row("medical_station", 6, 70, 5),

        row("wardens_office", 7, 400, 36), row("shieldbearer_barracks", 7, 180, 15),
        row("bomber_barracks", 7, 180, 15), row("shooter_barracks", 7, 180, 15),
        row("communication_center", 7, 100, 8), row("command_center", 7, 80, 8),
        row("medical_station", 7, 80, 8),

        row("wardens_office", 8, 460, 48), row("shieldbearer_barracks", 8, 210, 22),
        row("bomber_barracks", 8, 210, 22), row("shooter_barracks", 8, 210, 22),
        row("communication_center", 8, 110, 12), row("command_center", 8, 90, 12),
        row("medical_station", 8, 90, 12),

        row("wardens_office", 9, 510, 72), row("shieldbearer_barracks", 9, 230, 32),
        row("bomber_barracks", 9, 230, 32), row("shooter_barracks", 9, 230, 32),
        row("communication_center", 9, 130, 17), row("command_center", 9, 100, 14),

        row("wardens_office", 10, 630, 168), row("shieldbearer_barracks", 10, 290, 75),
        row("bomber_barracks", 10, 290, 75), row("shooter_barracks", 10, 290, 75),
        row("communication_center", 10, 160, 41), row("command_center", 10, 130, 34),
        row("medical_station", 9, 100, 14, MEDICAL_STATION_SOURCE_URL),
        row("medical_station", 10, 130, 34, MEDICAL_STATION_SOURCE_URL),

        // Forticlad Lab costs are published on its dedicated LOJ Wiki building page.
        row("forticlad_lab", 1, 150, source = FORTICLAD_LAB_SOURCE_URL),
        row("forticlad_lab", 2, 150, source = FORTICLAD_LAB_SOURCE_URL),
        row("forticlad_lab", 3, 200, source = FORTICLAD_LAB_SOURCE_URL),
        row("forticlad_lab", 4, 250, source = FORTICLAD_LAB_SOURCE_URL),
        row("forticlad_lab", 5, 300, source = FORTICLAD_LAB_SOURCE_URL),
        row("forticlad_lab", 6, 70, 5, FORTICLAD_LAB_SOURCE_URL),
        row("forticlad_lab", 7, 80, 8, FORTICLAD_LAB_SOURCE_URL),
        row("forticlad_lab", 8, 90, 10, FORTICLAD_LAB_SOURCE_URL),
        row("forticlad_lab", 9, 100, 14, FORTICLAD_LAB_SOURCE_URL),
        row("forticlad_lab", 10, 130, 34, FORTICLAD_LAB_SOURCE_URL)
    )

    private val byBuildingAndTarget = stageCosts.associateBy { it.buildingId to it.targetLevel }

    /**
     * Returns null if any required stage is absent or invalid. Current level
     * is the last fully completed FC level (0 means ordinary level 30).
     * completedStages counts paid stages already completed toward current+1.
     */
    fun estimate(
        buildingId: String,
        currentLevel: Int,
        completedStages: Int,
        targetLevel: Int
    ): ForticladCostEstimate? {
        if (buildingId !in eligibleBuildingIds) return null
        if (currentLevel !in 0..9 || targetLevel !in 1..10 || targetLevel <= currentLevel) return null
        if (completedStages !in 0 until STAGES_PER_LEVEL) return null

        var fc = 0L
        var afc = 0L
        var stages = 0
        for (level in (currentLevel + 1)..targetLevel) {
            val cost = byBuildingAndTarget[buildingId to level] ?: return null
            val count = if (level == currentLevel + 1) STAGES_PER_LEVEL - completedStages
                else STAGES_PER_LEVEL
            fc += cost.forticladCores * count
            afc += cost.advancedCores * count
            stages += count
        }
        return ForticladCostEstimate(
            buildingId = buildingId,
            fromLevel = currentLevel,
            targetLevel = targetLevel,
            completedStagesAtCurrentLevel = completedStages,
            forticladCores = fc,
            advancedCores = afc,
            includedStages = stages,
            confidence = CostConfidence.COMMUNITY_REFERENCE,
            sourceUrl = byBuildingAndTarget[buildingId to (currentLevel + 1)]?.sourceUrl ?: SOURCE_URL
        )
    }
}
