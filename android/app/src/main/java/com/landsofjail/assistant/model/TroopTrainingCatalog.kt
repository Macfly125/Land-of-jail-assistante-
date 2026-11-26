package com.landsofjail.assistant.model

/**
 * Per-unit troop training costs published by the LOJ Wiki.
 * These are base costs before speed bonuses and do not include healing.
 */
enum class TroopTrainingType(val displayName: String, val sourceUrl: String) {
    SHIELDBEARER("Shieldbearer", "https://lojwiki.com/troops/shieldbearer/"),
    BOMBER("Bomber", "https://lojwiki.com/troops/bomber/"),
    SHOOTER("Shooter", "https://lojwiki.com/troops/shooter/")
}

data class TroopTrainingRow(
    val type: TroopTrainingType,
    val tier: Int,
    val troopName: String,
    val unlockRequirement: String,
    val unitCosts: Map<String, Long>,
    val durationSeconds: Long,
    val checkedOn: String = "2026-11-26"
) {
    init {
        require(tier in 1..11)
        require(troopName.isNotBlank() && unlockRequirement.isNotBlank())
        require(unitCosts.isNotEmpty() && unitCosts.values.all { it >= 0L })
        require(durationSeconds >= 0L)
    }
}

data class TroopTrainingPlan(
    val type: TroopTrainingType,
    val tier: Int,
    val troopName: String,
    val quantity: Long,
    val requirements: Map<String, Long>,
    val totalDurationSeconds: Long,
    val unlockRequirement: String,
    val sourceUrl: String,
    val checkedOn: String
)

object TroopTrainingCatalog {
    private const val SEASONAL_XI_UNLOCK =
        "Forticlad Lab research unlock; available from Season 4 onward (server progression dependent)."

    val rows: List<TroopTrainingRow> = listOf(
        row(TroopTrainingType.SHIELDBEARER, 1, "Junior Shieldbearer", "Shieldbearer Barracks Lv.1", 76, 50, 3, 17, 12),
        row(TroopTrainingType.SHIELDBEARER, 2, "Raid Shieldbearer", "Shieldbearer Barracks Lv.4", 122, 81, 5, 24, 17),
        row(TroopTrainingType.SHIELDBEARER, 3, "Riot Shieldbearer", "Shieldbearer Barracks Lv.7", 194, 128, 6, 41, 24),
        row(TroopTrainingType.SHIELDBEARER, 4, "Armored Shieldbearer", "Shieldbearer Barracks Lv.11", 253, 167, 8, 51, 32),
        row(TroopTrainingType.SHIELDBEARER, 5, "Senior Shieldbearer", "Shieldbearer Barracks Lv.13", 329, 217, 9, 66, 44),
        row(TroopTrainingType.SHIELDBEARER, 6, "Special Shieldbearer", "Shieldbearer Barracks Lv.16", 393, 259, 11, 80, 60),
        row(TroopTrainingType.SHIELDBEARER, 7, "Elite Shieldbearer", "Shieldbearer Barracks Lv.19", 589, 389, 17, 119, 83),
        row(TroopTrainingType.SHIELDBEARER, 8, "Brave Shieldbearer", "Shieldbearer Barracks Lv.22", 1178, 776, 32, 238, 113),
        row(TroopTrainingType.SHIELDBEARER, 9, "Valiant Shieldbearer", "Shieldbearer Barracks Lv.26", 2943, 1937, 77, 593, 131),
        row(TroopTrainingType.SHIELDBEARER, 10, "Ace Shieldbearer", "Shieldbearer Barracks Lv.30", 5886, 3872, 153, 1185, 152),
        row(TroopTrainingType.SHIELDBEARER, 11, "Bastion Shieldbearer", SEASONAL_XI_UNLOCK, 20601, 13552, 536, 4148, 180),

        row(TroopTrainingType.BOMBER, 1, "Junior Bomber", "Bomber Barracks Lv.1", 62, 62, 4, 14, 12),
        row(TroopTrainingType.BOMBER, 2, "Raid Bomber", "Bomber Barracks Lv.4", 99, 99, 6, 20, 17),
        row(TroopTrainingType.BOMBER, 3, "Riot Bomber", "Bomber Barracks Lv.7", 157, 157, 8, 31, 24),
        row(TroopTrainingType.BOMBER, 4, "Armored Bomber", "Bomber Barracks Lv.11", 203, 205, 11, 41, 32),
        row(TroopTrainingType.BOMBER, 5, "Senior Bomber", "Bomber Barracks Lv.13", 264, 267, 15, 53, 44),
        row(TroopTrainingType.BOMBER, 6, "Special Bomber", "Bomber Barracks Lv.16", 316, 318, 17, 63, 60),
        row(TroopTrainingType.BOMBER, 7, "Elite Bomber", "Bomber Barracks Lv.19", 473, 477, 23, 94, 83),
        row(TroopTrainingType.BOMBER, 8, "Brave Bomber", "Bomber Barracks Lv.22", 946, 953, 46, 186, 113),
        row(TroopTrainingType.BOMBER, 9, "Valiant Bomber", "Bomber Barracks Lv.26", 2364, 2379, 116, 464, 131),
        row(TroopTrainingType.BOMBER, 10, "Ace Bomber", "Bomber Barracks Lv.30", 4728, 4755, 229, 928, 152),
        row(TroopTrainingType.BOMBER, 11, "Bastion Bomber", SEASONAL_XI_UNLOCK, 16548, 16643, 802, 3248, 180),

        row(TroopTrainingType.SHOOTER, 1, "Junior Shooter", "Shooter Barracks Lv.1", 45, 70, 5, 11, 12),
        row(TroopTrainingType.SHOOTER, 2, "Raid Shooter", "Shooter Barracks Lv.4", 70, 111, 9, 17, 17),
        row(TroopTrainingType.SHOOTER, 3, "Riot Shooter", "Shooter Barracks Lv.7", 113, 177, 11, 28, 24),
        row(TroopTrainingType.SHOOTER, 4, "Armored Shooter", "Shooter Barracks Lv.11", 147, 229, 14, 35, 32),
        row(TroopTrainingType.SHOOTER, 5, "Senior Shooter", "Shooter Barracks Lv.13", 190, 296, 18, 44, 44),
        row(TroopTrainingType.SHOOTER, 6, "Special Shooter", "Shooter Barracks Lv.16", 229, 356, 23, 53, 60),
        row(TroopTrainingType.SHOOTER, 7, "Elite Shooter", "Shooter Barracks Lv.19", 342, 531, 32, 81, 83),
        row(TroopTrainingType.SHOOTER, 8, "Brave Shooter", "Shooter Barracks Lv.22", 683, 1062, 63, 160, 113),
        row(TroopTrainingType.SHOOTER, 9, "Valiant Shooter", "Shooter Barracks Lv.26", 1706, 2656, 158, 398, 131),
        row(TroopTrainingType.SHOOTER, 10, "Ace Shooter", "Shooter Barracks Lv.30", 3410, 5310, 315, 794, 152),
        row(TroopTrainingType.SHOOTER, 11, "Bastion Shooter", SEASONAL_XI_UNLOCK, 11935, 18585, 1103, 2779, 180)
    )

    fun calculate(type: TroopTrainingType, tier: Int, quantity: Long): TroopTrainingPlan {
        require(quantity >= 0L) { "Troop quantity cannot be negative." }
        val row = rows.single { it.type == type && it.tier == tier }
        val requirements = row.unitCosts.mapValues { (_, unitCost) -> multiplySaturated(unitCost, quantity) }
        return TroopTrainingPlan(
            type = type,
            tier = tier,
            troopName = row.troopName,
            quantity = quantity,
            requirements = requirements,
            totalDurationSeconds = multiplySaturated(row.durationSeconds, quantity),
            unlockRequirement = row.unlockRequirement,
            sourceUrl = type.sourceUrl,
            checkedOn = row.checkedOn
        )
    }

    private fun row(
        type: TroopTrainingType,
        tier: Int,
        name: String,
        unlock: String,
        food: Long,
        wood: Long,
        gasoline: Long,
        steel: Long,
        seconds: Long
    ) = TroopTrainingRow(
        type = type,
        tier = tier,
        troopName = name,
        unlockRequirement = unlock,
        unitCosts = linkedMapOf(
            "food" to food,
            "wood" to wood,
            "gasoline" to gasoline,
            "steel" to steel
        ),
        durationSeconds = seconds
    )

    private fun multiplySaturated(value: Long, multiplier: Long): Long =
        if (value != 0L && multiplier > Long.MAX_VALUE / value) Long.MAX_VALUE
        else value * multiplier
}
