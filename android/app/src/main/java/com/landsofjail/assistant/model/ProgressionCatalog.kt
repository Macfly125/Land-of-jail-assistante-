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


    const val WARDENS_OFFICE_SOURCE = "https://lojwiki.com/buildings/wardens-office/"

    /**
     * Warden's Office standard upgrades Lv.1 -> Lv.30.
     * Forticlad tiers beyond Lv.30 are deliberately kept out of this table.
     */
    val wardensOffice: List<ProgressionCatalogRecord> = listOf(
        hqRow(2, listOf("wood" to 350L), 6, "Kitchen Lv.1"),
        hqRow(3, listOf("wood" to 1_600L), 60, "Cell 1 Lv.2"),
        hqRow(4, listOf("wood" to 3_600L, "steel" to 700L), 180, "Factory Lv.3"),
        hqRow(5, listOf("wood" to 15_000L, "steel" to 3_000L), 600, "Cell 3 Lv.3"),
        hqRow(6, listOf("wood" to 40_000L, "steel" to 8_000L, "gasoline" to 2_000L), 1_800,
            "Supply Center Lv.1", "Shower Room 1 Lv.5"),
        hqRow(7, listOf("wood" to 140_000L, "steel" to 28_000L, "gasoline" to 7_000L), 3_600, "Playground Lv.5"),
        hqRow(8, listOf("wood" to 240_000L, "steel" to 48_000L, "gasoline" to 12_000L), 9_000, "Shieldbearer Barracks Lv.7"),
        hqRow(9, listOf("wood" to 500_000L, "steel" to 100_000L, "gasoline" to 25_000L), 16_200,
            "Communication Center Lv.8", "Medical Station Lv.1"),
        hqRow(10, listOf("wood" to 900_000L, "steel" to 180_000L, "gasoline" to 45_000L), 21_600,
            "Shooter Barracks Lv.9", "Lab Lv.1"),
        hqRow(11, listOf("food" to 2_500_000L, "wood" to 2_500_000L, "steel" to 500_000L, "gasoline" to 125_000L), 27_000,
            "Communication Center Lv.10", "Bomber Barracks Lv.10"),
        hqRow(12, listOf("food" to 3_000_000L, "wood" to 3_000_000L, "steel" to 600_000L, "gasoline" to 150_000L), 32_400,
            "Communication Center Lv.11", "Command Center Lv.1"),
        hqRow(13, listOf("food" to 4_500_000L, "wood" to 4_500_000L, "steel" to 900_000L, "gasoline" to 225_000L), 39_600,
            "Communication Center Lv.12", "Shieldbearer Barracks Lv.12"),
        hqRow(14, listOf("food" to 6_000_000L, "wood" to 6_000_000L, "steel" to 1_200_000L, "gasoline" to 300_000L), 50_400,
            "Communication Center Lv.13", "Shooter Barracks Lv.13"),
        hqRow(15, listOf("food" to 9_000_000L, "wood" to 9_000_000L, "steel" to 1_800_000L, "gasoline" to 450_000L), 64_800,
            "Communication Center Lv.14", "Bomber Barracks Lv.14"),
        hqRow(16, listOf("food" to 12_000_000L, "wood" to 12_000_000L, "steel" to 2_400_000L, "gasoline" to 600_000L), 108_000,
            "Communication Center Lv.15", "Lab Lv.15"),
        hqRow(17, listOf("food" to 18_000_000L, "wood" to 18_000_000L, "steel" to 3_600_000L, "gasoline" to 900_000L), 129_600,
            "Communication Center Lv.16", "Shieldbearer Barracks Lv.16"),
        hqRow(18, listOf("food" to 25_000_000L, "wood" to 25_000_000L, "steel" to 5_000_000L, "gasoline" to 1_300_000L), 154_800,
            "Communication Center Lv.17", "Shooter Barracks Lv.17"),
        hqRow(19, listOf("food" to 30_000_000L, "wood" to 30_000_000L, "steel" to 6_000_000L, "gasoline" to 1_500_000L), 234_000,
            "Communication Center Lv.18", "Bomber Barracks Lv.18"),
        hqRow(20, listOf("food" to 40_000_000L, "wood" to 40_000_000L, "steel" to 8_000_000L, "gasoline" to 2_000_000L), 295_200,
            "Communication Center Lv.19", "Lab Lv.19"),
        hqRow(21, listOf("food" to 50_000_000L, "wood" to 50_000_000L, "steel" to 10_000_000L, "gasoline" to 2_500_000L), 381_600,
            "Communication Center Lv.20", "Shieldbearer Barracks Lv.20"),
        hqRow(22, listOf("food" to 70_000_000L, "wood" to 70_000_000L, "steel" to 14_000_000L, "gasoline" to 3_500_000L), 576_000,
            "Communication Center Lv.21", "Shooter Barracks Lv.21"),
        hqRow(23, listOf("food" to 90_000_000L, "wood" to 90_000_000L, "steel" to 18_000_000L, "gasoline" to 4_500_000L), 806_400,
            "Communication Center Lv.22", "Bomber Barracks Lv.22"),
        hqRow(24, listOf("food" to 120_000_000L, "wood" to 120_000_000L, "steel" to 24_000_000L, "gasoline" to 6_000_000L), 1_130_400,
            "Communication Center Lv.23", "Lab Lv.23"),
        hqRow(25, listOf("food" to 160_000_000L, "wood" to 160_000_000L, "steel" to 32_000_000L, "gasoline" to 8_000_000L), 1_584_000,
            "Communication Center Lv.24", "Shieldbearer Barracks Lv.24"),
        hqRow(26, listOf("food" to 210_000_000L, "wood" to 210_000_000L, "steel" to 42_000_000L, "gasoline" to 10_500_000L), 1_821_600,
            "Communication Center Lv.25", "Shooter Barracks Lv.25"),
        hqRow(27, listOf("food" to 300_000_000L, "wood" to 300_000_000L, "steel" to 60_000_000L, "gasoline" to 15_000_000L), 2_185_200,
            "Communication Center Lv.26", "Bomber Barracks Lv.26"),
        hqRow(28, listOf("food" to 350_000_000L, "wood" to 350_000_000L, "steel" to 70_000_000L, "gasoline" to 17_500_000L), 2_512_800,
            "Communication Center Lv.27", "Lab Lv.27"),
        hqRow(29, listOf("food" to 450_000_000L, "wood" to 450_000_000L, "steel" to 90_000_000L, "gasoline" to 22_500_000L), 2_890_800,
            "Communication Center Lv.28", "Shieldbearer Barracks Lv.28"),
        hqRow(30, listOf("food" to 600_000_000L, "wood" to 600_000_000L, "steel" to 120_000_000L, "gasoline" to 30_000_000L), 3_470_400,
            "Communication Center Lv.29", "Shooter Barracks Lv.29")
    )



    const val KITCHEN_SOURCE = "https://lojwiki.com/buildings/kitchen/"
    const val CELL_SOURCE = "https://lojwiki.com/buildings/cell/"
    const val FACTORY_SOURCE = "https://lojwiki.com/buildings/factory/"
    const val UTILITY_ROOM_SOURCE = "https://lojwiki.com/buildings/utility-room/"
    const val GUARD_ROOM_SOURCE = "https://lojwiki.com/buildings/guard-room/"

    // Only un-abbreviated cash values are included. Higher levels use rounded
    // K/M display values on the source page and are intentionally left out.
    val kitchenBuilding = cashBuilding(
        "Kitchen", KITCHEN_SOURCE, listOf(
            CashBuildingLevel(2, 206, 13, 2), CashBuildingLevel(3, 247, 21, 3),
            CashBuildingLevel(4, 296, 37, 4), CashBuildingLevel(5, 1_334, 146, 5),
            CashBuildingLevel(6, 3_334, 276, 6)
        )
    )
    val cellBuilding = cashBuilding(
        "Cell", CELL_SOURCE, listOf(
            CashBuildingLevel(2, 66, 4, 2), CashBuildingLevel(3, 79, 7, 3),
            CashBuildingLevel(4, 95, 12, 4), CashBuildingLevel(5, 427, 47, 5),
            CashBuildingLevel(6, 1_067, 89, 6)
        )
    )
    val factoryBuilding = cashBuilding(
        "Factory", FACTORY_SOURCE, listOf(
            CashBuildingLevel(2, 463, 29, 3), CashBuildingLevel(3, 556, 47, 3),
            CashBuildingLevel(4, 667, 82, 4), CashBuildingLevel(5, 3_000, 327, 5),
            CashBuildingLevel(6, 7_501, 621, 6)
        )
    )
    val utilityRoomBuilding = cashBuilding(
        "Utility Room", UTILITY_ROOM_SOURCE, listOf(
            CashBuildingLevel(2, 288, 18, 4), CashBuildingLevel(3, 346, 30, 4),
            CashBuildingLevel(4, 415, 51, 4), CashBuildingLevel(5, 1_867, 204, 5),
            CashBuildingLevel(6, 4_667, 387, 6)
        )
    )
    val guardRoomBuilding = cashBuilding(
        "Guard Room", GUARD_ROOM_SOURCE, listOf(
            CashBuildingLevel(2, 360, 22, 2), CashBuildingLevel(3, 432, 37, 3),
            CashBuildingLevel(4, 519, 64, 4), CashBuildingLevel(5, 2_334, 255, 5),
            CashBuildingLevel(6, 5_834, 483, 6)
        )
    )

    private data class CashBuildingLevel(
        val targetLevel: Int,
        val cash: Long,
        val durationSeconds: Long,
        val requiredWardenOfficeLevel: Int
    )

    private fun cashBuilding(
        buildingName: String,
        sourceUrl: String,
        levels: List<CashBuildingLevel>
    ): List<ProgressionCatalogRecord> = levels.map { level ->
        ProgressionCatalogRecord(
            step = ProgressionStep(
                id = "${buildingName.lowercase().replace(" ", "-")}-${level.targetLevel - 1}-${level.targetLevel}",
                domain = ProgressionDomain.BUILDING,
                itemName = buildingName,
                fromLevel = level.targetLevel - 1,
                toLevel = level.targetLevel,
                costs = listOf(
                    ResourceCost("gold", level.cash, CostConfidence.COMMUNITY_REFERENCE, sourceUrl)
                ),
                durationSeconds = level.durationSeconds,
                dataComplete = true
            ),
            prerequisiteNotes = listOf("Warden's Office Lv.${level.requiredWardenOfficeLevel}"),
            sourceLabel = "LOJ Wiki — $buildingName upgrade costs",
            sourceUrl = sourceUrl,
            checkedOn = SOURCE_CHECKED_ON
        )
    }


    const val LAUNDRY_SOURCE = "https://lojwiki.com/buildings/laundry/"

    // Laundry: only exact, un-abbreviated Cash values through level 7.
    val laundryBuilding = cashBuilding(
        "Laundry", LAUNDRY_SOURCE, listOf(
            CashBuildingLevel(2, 113, 7, 5), CashBuildingLevel(3, 136, 12, 5),
            CashBuildingLevel(4, 163, 20, 5), CashBuildingLevel(5, 733, 80, 5),
            CashBuildingLevel(6, 1_834, 152, 6), CashBuildingLevel(7, 3_667, 293, 7)
        )
    )

    const val INVENTORY_SOURCE = "https://lojwiki.com/buildings/inventory/"

    /**
     * Inventory upgrades Lv.1 -> Lv.7. The source publishes exact resource
     * quantities and base durations for these levels; later rounded values
     * are deliberately not imported.
     */
    val inventoryBuilding: List<ProgressionCatalogRecord> = listOf(
        inventoryRow(2, listOf("wood" to 200L), 9),
        inventoryRow(3, listOf("wood" to 1_000L), 45),
        inventoryRow(4, listOf("wood" to 2_000L, "steel" to 400L), 135),
        inventoryRow(5, listOf("wood" to 8_000L, "steel" to 1_600L), 270),
        inventoryRow(6, listOf("wood" to 20_000L, "steel" to 4_000L, "gasoline" to 1_000L), 540),
        inventoryRow(7, listOf("wood" to 70_000L, "steel" to 14_000L, "gasoline" to 3_500L), 1_080)
    )

    private fun inventoryRow(
        targetLevel: Int,
        costs: List<Pair<String, Long>>,
        baseDurationSeconds: Long
    ): ProgressionCatalogRecord {
        val sourcedCosts = costs.map { (resourceId, quantity) ->
            ResourceCost(resourceId, quantity, CostConfidence.COMMUNITY_REFERENCE, INVENTORY_SOURCE)
        }
        return ProgressionCatalogRecord(
            step = ProgressionStep(
                id = "inventory-${targetLevel - 1}-$targetLevel",
                domain = ProgressionDomain.BUILDING,
                itemName = "Inventory",
                fromLevel = targetLevel - 1,
                toLevel = targetLevel,
                costs = sourcedCosts,
                durationSeconds = baseDurationSeconds,
                dataComplete = true
            ),
            prerequisiteNotes = listOf("Warden's Office Lv.9"),
            sourceLabel = "LOJ Wiki — Inventory upgrade costs",
            sourceUrl = INVENTORY_SOURCE,
            checkedOn = SOURCE_CHECKED_ON
        )
    }

    const val GATE_SOURCE = "https://lojwiki.com/buildings/gate/"

    /**
     * Gate upgrades for levels 1 -> 7 only. The source abbreviates higher
     * resource quantities (e.g. 3.8M), so those levels are intentionally
     * excluded rather than treating rounded display values as exact costs.
     */
    val gateBuilding: List<ProgressionCatalogRecord> = listOf(
        gateRow(2, listOf("wood" to 10_000L, "steel" to 2_000L), 10, 7),
        gateRow(3, listOf("wood" to 180_000L, "steel" to 36_000L, "gasoline" to 9_000L), 1_290, 10),
        gateRow(4, listOf("food" to 800_000L, "wood" to 800_000L, "steel" to 160_000L, "gasoline" to 40_000L), 3_840, 13),
        gateRow(5, listOf("food" to 3_000_000L, "wood" to 3_000_000L, "steel" to 600_000L, "gasoline" to 150_000L), 13_140, 16),
        gateRow(6, listOf("food" to 10_000_000L, "wood" to 10_000_000L, "steel" to 2_000_000L, "gasoline" to 500_000L), 35_520, 20),
        gateRow(7, listOf("food" to 30_000_000L, "wood" to 30_000_000L, "steel" to 6_000_000L, "gasoline" to 1_500_000L), 133_200, 24)
    )

    private fun gateRow(
        targetLevel: Int,
        costs: List<Pair<String, Long>>,
        baseDurationSeconds: Long,
        requiredWardenOfficeLevel: Int
    ): ProgressionCatalogRecord {
        val sourcedCosts = costs.map { (resourceId, quantity) ->
            ResourceCost(resourceId, quantity, CostConfidence.COMMUNITY_REFERENCE, GATE_SOURCE)
        }
        return ProgressionCatalogRecord(
            step = ProgressionStep(
                id = "gate-${targetLevel - 1}-$targetLevel",
                domain = ProgressionDomain.BUILDING,
                itemName = "Gate",
                fromLevel = targetLevel - 1,
                toLevel = targetLevel,
                costs = sourcedCosts,
                durationSeconds = baseDurationSeconds,
                dataComplete = true
            ),
            prerequisiteNotes = listOf("Warden's Office Lv.$requiredWardenOfficeLevel"),
            sourceLabel = "LOJ Wiki — Gate upgrade costs",
            sourceUrl = GATE_SOURCE,
            checkedOn = SOURCE_CHECKED_ON
        )
    }

    private fun hqRow(
        targetLevel: Int,
        costs: List<Pair<String, Long>>,
        baseDurationSeconds: Long,
        vararg prerequisites: String
    ): ProgressionCatalogRecord {
        val sourcedCosts = costs.map { (resourceId, quantity) ->
            ResourceCost(resourceId, quantity, CostConfidence.COMMUNITY_REFERENCE, WARDENS_OFFICE_SOURCE)
        }
        return ProgressionCatalogRecord(
            step = ProgressionStep(
                id = "wardens-office-${targetLevel - 1}-$targetLevel",
                domain = ProgressionDomain.BUILDING,
                itemName = "Warden's Office",
                fromLevel = targetLevel - 1,
                toLevel = targetLevel,
                costs = sourcedCosts,
                durationSeconds = baseDurationSeconds,
                dataComplete = true
            ),
            prerequisiteNotes = prerequisites.toList(),
            sourceLabel = "LOJ Wiki — Warden's Office upgrade costs",
            sourceUrl = WARDENS_OFFICE_SOURCE,
            checkedOn = SOURCE_CHECKED_ON
        )
    }

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
