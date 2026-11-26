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

/**
 * Returns only source-stated prerequisites for complete published steps
 * between a confirmed current level and the requested target. An unknown
 * starting level must never imply that the whole catalogue range applies.
 */
fun sourcedPrerequisitesForRange(
    catalog: List<ProgressionCatalogRecord>,
    currentLevel: Int?,
    targetLevel: Int
): List<Pair<Int, String>> {
    if (currentLevel == null || targetLevel <= currentLevel) return emptyList()
    return catalog
        .filter { it.step.toLevel > currentLevel && it.step.toLevel <= targetLevel }
        .flatMap { record -> record.prerequisiteNotes.map { note -> record.step.toLevel to note } }
        .distinct()
}

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
            CashBuildingLevel(6, 3_334, 276, 6), CashBuildingLevel(7, 6_668, 533, 7)
        )
    )
    val cellBuilding = cashBuilding(
        "Cell", CELL_SOURCE, listOf(
            CashBuildingLevel(2, 66, 4, 2), CashBuildingLevel(3, 79, 7, 3),
            CashBuildingLevel(4, 95, 12, 4), CashBuildingLevel(5, 427, 47, 5),
            CashBuildingLevel(6, 1_067, 89, 6), CashBuildingLevel(7, 2_134, 171, 7),
            CashBuildingLevel(8, 7_041, 243, 8), CashBuildingLevel(9, 8_449, 335, 9)
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
            CashBuildingLevel(6, 4_667, 387, 6), CashBuildingLevel(7, 9_335, 746, 7)
        )
    )
    val guardRoomBuilding = cashBuilding(
        "Guard Room", GUARD_ROOM_SOURCE, listOf(
            CashBuildingLevel(2, 360, 22, 2), CashBuildingLevel(3, 432, 37, 3),
            CashBuildingLevel(4, 519, 64, 4), CashBuildingLevel(5, 2_334, 255, 5),
            CashBuildingLevel(6, 5_834, 483, 6)
        )
    )

    const val COMMAND_CENTER_SOURCE = "https://lojwiki.com/buildings/command-center/"
    const val SHIELDBEARER_BARRACK_SOURCE = "https://lojwiki.com/buildings/shieldbearer-barracks/"
    const val SHOOTER_BARRACK_SOURCE = "https://lojwiki.com/buildings/shooter-barracks/"
    const val BOMBER_BARRACK_SOURCE = "https://lojwiki.com/buildings/bomber-barracks/"

    // Exact, fully written quantities only. Level 5 starts with abbreviated 10K values.
    val commandCenterBuilding = multiResourceBuilding(
        "Command Center", COMMAND_CENTER_SOURCE, listOf(
            MultiResourceBuildingLevel(2, listOf("wood" to 200L), 8, 10),
            MultiResourceBuildingLevel(3, listOf("wood" to 1_200L), 34, 10),
            MultiResourceBuildingLevel(4, listOf("wood" to 2_400L, "steel" to 480L), 105, 10)
        )
    )
    val shieldbearerBarrackBuilding = multiResourceBuilding(
        "Shieldbearer Barracks", SHIELDBEARER_BARRACK_SOURCE, listOf(
            MultiResourceBuildingLevel(2, listOf("wood" to 300L), 9, 7),
            MultiResourceBuildingLevel(3, listOf("wood" to 1_300L), 45, 7),
            MultiResourceBuildingLevel(4, listOf("wood" to 3_000L, "steel" to 600L), 135, 7)
        )
    )
    val shooterBarrackBuilding = multiResourceBuilding(
        "Shooter Barracks", SHOOTER_BARRACK_SOURCE, listOf(
            MultiResourceBuildingLevel(2, listOf("wood" to 300L), 9, 8),
            MultiResourceBuildingLevel(3, listOf("wood" to 1_300L), 45, 8),
            MultiResourceBuildingLevel(4, listOf("wood" to 3_000L, "steel" to 600L), 135, 8)
        )
    )
    val bomberBarrackBuilding = multiResourceBuilding(
        "Bomber Barracks", BOMBER_BARRACK_SOURCE, listOf(
            MultiResourceBuildingLevel(2, listOf("wood" to 300L), 9, 9),
            MultiResourceBuildingLevel(3, listOf("wood" to 1_300L), 45, 9),
            MultiResourceBuildingLevel(4, listOf("wood" to 3_000L, "steel" to 600L), 135, 9)
        )
    )

    const val MEDICAL_STATION_SOURCE = "https://lojwiki.com/buildings/medical-station/"
    const val COMMUNICATION_CENTER_SOURCE = "https://lojwiki.com/buildings/communication-center/"

    // Only exact, fully written resource amounts are included; later K/M rows are omitted.
    val medicalStationBuilding = multiResourceBuilding(
        "Medical Station", MEDICAL_STATION_SOURCE, listOf(
            MultiResourceBuildingLevel(2, listOf("wood" to 150L), 9, 8),
            MultiResourceBuildingLevel(3, listOf("wood" to 900L), 40, 8),
            MultiResourceBuildingLevel(4, listOf("wood" to 2_000L, "steel" to 400L), 125, 8),
            MultiResourceBuildingLevel(5, listOf("wood" to 9_000L, "steel" to 1_800L), 250, 8)
        )
    )

    val communicationCenterBuilding = multiResourceBuilding(
        "Communication Center", COMMUNICATION_CENTER_SOURCE, listOf(
            MultiResourceBuildingLevel(2, listOf("wood" to 200L), 10, 8),
            MultiResourceBuildingLevel(3, listOf("wood" to 1_000L), 60, 8),
            MultiResourceBuildingLevel(4, listOf("wood" to 2_000L, "steel" to 400L), 120, 8),
            MultiResourceBuildingLevel(5, listOf("wood" to 8_000L, "steel" to 1_600L), 400, 8)
        )
    )

    private data class MultiResourceBuildingLevel(
        val targetLevel: Int,
        val costs: List<Pair<String, Long>>,
        val durationSeconds: Long,
        val requiredWardenOfficeLevel: Int
    )

    private fun multiResourceBuilding(
        buildingName: String,
        sourceUrl: String,
        levels: List<MultiResourceBuildingLevel>
    ): List<ProgressionCatalogRecord> = levels.map { level ->
        ProgressionCatalogRecord(
            step = ProgressionStep(
                id = "${buildingName.lowercase().replace(" ", "-")}-${level.targetLevel - 1}-${level.targetLevel}",
                domain = ProgressionDomain.BUILDING,
                itemName = buildingName,
                fromLevel = level.targetLevel - 1,
                toLevel = level.targetLevel,
                costs = level.costs.map { (resourceId, quantity) ->
                    ResourceCost(resourceId, quantity, CostConfidence.COMMUNITY_REFERENCE, sourceUrl)
                },
                durationSeconds = level.durationSeconds,
                dataComplete = true
            ),
            prerequisiteNotes = listOf("Warden's Office Lv.${level.requiredWardenOfficeLevel}"),
            sourceLabel = "LOJ Wiki — $buildingName upgrade costs",
            sourceUrl = sourceUrl,
            checkedOn = SOURCE_CHECKED_ON
        )
    }

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


    const val CAFETERIA_SOURCE = "https://lojwiki.com/buildings/cafeteria/"

    // Cafeteria: only exact, un-abbreviated Cash values through level 7.
    val cafeteriaBuilding = cashBuilding(
        "Cafeteria", CAFETERIA_SOURCE, listOf(
            CashBuildingLevel(2, 185, 12, 2), CashBuildingLevel(3, 222, 19, 3),
            CashBuildingLevel(4, 267, 33, 4), CashBuildingLevel(5, 1_200, 131, 5),
            CashBuildingLevel(6, 3_000, 249, 6), CashBuildingLevel(7, 6_001, 480, 7)
        )
    )

    const val SHOWER_ROOM_SOURCE = "https://lojwiki.com/buildings/shower-room/"

    // Shower Room: exact un-abbreviated cash amounts through level 7; rounded K values are excluded.
    val showerRoomBuilding = cashBuilding(
        "Shower Room", SHOWER_ROOM_SOURCE, listOf(
            CashBuildingLevel(2, 174, 11, 2), CashBuildingLevel(3, 208, 18, 3),
            CashBuildingLevel(4, 250, 31, 4), CashBuildingLevel(5, 1_125, 123, 5),
            CashBuildingLevel(6, 2_813, 233, 6), CashBuildingLevel(7, 5_626, 450, 7)
        )
    )

    const val PLAYGROUND_SOURCE = "https://lojwiki.com/buildings/playground/"

    // Playground: exact Cash values only through level 6; level 7 is rounded.
    val playgroundBuilding = cashBuilding(
        "Playground", PLAYGROUND_SOURCE, listOf(
            CashBuildingLevel(2, 319, 20, 2), CashBuildingLevel(3, 383, 33, 3),
            CashBuildingLevel(4, 459, 57, 4), CashBuildingLevel(5, 2_067, 226, 5),
            CashBuildingLevel(6, 5_167, 428, 6)
        )
    )

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
    const val ROBOT_UPGRADE_SOURCE = "https://lojxlab.com/library/lands-of-jail-robot-upgrade-cost/"
    const val ROBOT_SOURCE_CHECKED_ON = "2026-10-07"

    /**
     * Robot level bands published by LOJ XLab. The source reports one cost per
     * 10-level band, not per individual level. A current level inside a band
     * therefore cannot be prorated safely by this catalog.
     */
    val robotLeveling: List<ProgressionCatalogRecord> = listOf(
        robotLevelRow(1, 10, 1_200, 20, 0),
        robotLevelRow(10, 20, 2_310, 30, 0),
        robotLevelRow(20, 30, 3_600, 40, 10),
        robotLevelRow(30, 40, 5_320, 60, 15),
        robotLevelRow(40, 50, 7_560, 80, 20),
        robotLevelRow(50, 60, 10_260, 100, 30),
        // XLab records a source conflict for this one cell: 40 vs 140.
        // Keep it visible as uncertain and exclude it from calculated totals.
        robotLevelRow(60, 70, 19_400, 130, 40, advancedNeedsConfirmation = true),
        robotLevelRow(70, 80, 24_800, 160, 50),
        robotLevelRow(80, 90, 30_300, 190, 70),
        robotLevelRow(90, 100, 36_300, 230, 90)
    )

    /** Costs for one robot module (Solo, Barrier or Formation), Core I to V. */
    val robotCore: List<ProgressionCatalogRecord> = listOf(
        robotCoreRow(1, 3_640),
        robotCoreRow(2, 7_180),
        robotCoreRow(3, 19_320),
        robotCoreRow(4, 36_360),
        robotCoreRow(5, 49_960)
    )

    const val HERO_EQUIPMENT_SOURCE = "https://lojwiki.com/calculator/gear/"

    /** Equipment EXP, magnets and potential coils per published 20-level band. */
    val heroEquipmentLeveling: List<ProgressionCatalogRecord> = listOf(
        equipmentRow(0, 20, 2_300, 20, 0),
        equipmentRow(20, 40, 6_980, 40, 0),
        equipmentRow(40, 60, 19_220, 120, 0),
        equipmentRow(60, 80, 41_140, 200, 0),
        equipmentRow(80, 100, 77_000, 400, 0),
        equipmentRow(100, 120, 117_000, 600, 20),
        equipmentRow(120, 140, 157_000, 1_000, 40),
        equipmentRow(140, 160, 197_000, 1_000, 60),
        equipmentRow(160, 180, 258_000, 2_000, 80),
        equipmentRow(180, 200, 338_200, 2_000, 100)
    )

    /** Reformation is a separate track from equipment level promotion. */
    val heroEquipmentReform: List<ProgressionCatalogRecord> = listOf(
        ProgressionCatalogRecord(
            step = ProgressionStep(
                id = "hero-equipment-reform-0-20",
                domain = ProgressionDomain.EQUIPMENT,
                itemName = "Hero Equipment Reform",
                fromLevel = 0,
                toLevel = 20,
                costs = listOf(
                    ResourceCost("magnets", 11_000, CostConfidence.COMMUNITY_REFERENCE, HERO_EQUIPMENT_SOURCE),
                    ResourceCost("precision_gears", 4_200, CostConfidence.COMMUNITY_REFERENCE, HERO_EQUIPMENT_SOURCE)
                ),
                dataComplete = true
            ),
            prerequisiteNotes = listOf("Reformation is separate from equipment level promotion."),
            sourceLabel = "LOJ Wiki — Hero equipment calculator",
            sourceUrl = HERO_EQUIPMENT_SOURCE,
            checkedOn = SOURCE_CHECKED_ON
        )
    )

    const val HERO_SHARDS_SOURCE = "https://lojwiki.com/calculator/shards/"

    /** All heroes share this published shard cost sequence: recruit through 5 stars. */
    val heroShardProgression: List<ProgressionCatalogRecord> = listOf(
        10L to "Recruit",
        1L to "0-Star Tier 1",
        1L to "0-Star Tier 2",
        2L to "0-Star Tier 3",
        2L to "0-Star Tier 4",
        2L to "0-Star Tier 5",
        2L to "1-Star Tier 0",
        5L to "1-Star Tier 1",
        5L to "1-Star Tier 2",
        5L to "1-Star Tier 3",
        5L to "1-Star Tier 4",
        5L to "1-Star Tier 5",
        15L to "2-Star Tier 0",
        15L to "2-Star Tier 1",
        15L to "2-Star Tier 2",
        15L to "2-Star Tier 3",
        15L to "2-Star Tier 4",
        15L to "2-Star Tier 5",
        40L to "3-Star Tier 0",
        40L to "3-Star Tier 1",
        40L to "3-Star Tier 2",
        40L to "3-Star Tier 3",
        40L to "3-Star Tier 4",
        40L to "3-Star Tier 5",
        100L to "4-Star Tier 0",
        100L to "4-Star Tier 1",
        100L to "4-Star Tier 2",
        100L to "4-Star Tier 3",
        100L to "4-Star Tier 4",
        100L to "4-Star Tier 5",
        100L to "5-Star Tier 0"
    ).mapIndexed { index, (quantity, stageName) ->
        ProgressionCatalogRecord(
            step = ProgressionStep(
                id = "hero-shards-${index}-${index + 1}",
                domain = ProgressionDomain.HERO,
                itemName = stageName,
                fromLevel = index,
                toLevel = index + 1,
                costs = listOf(
                    ResourceCost("hero_shards", quantity, CostConfidence.COMMUNITY_REFERENCE, HERO_SHARDS_SOURCE)
                ),
                dataComplete = true
            ),
            prerequisiteNotes = emptyList(),
            sourceLabel = "LOJ Wiki — Hero shard calculator",
            sourceUrl = HERO_SHARDS_SOURCE,
            checkedOn = SOURCE_CHECKED_ON
        )
    }

    private fun robotLevelRow(
        fromLevel: Int,
        toLevel: Int,
        armorData: Long,
        powerModules: Long,
        advancedPowerModules: Long,
        advancedNeedsConfirmation: Boolean = false
    ): ProgressionCatalogRecord {
        val costs = mutableListOf(
            ResourceCost("prisoner_armor_data", armorData, CostConfidence.COMMUNITY_REFERENCE, ROBOT_UPGRADE_SOURCE),
            ResourceCost("power_modules", powerModules, CostConfidence.COMMUNITY_REFERENCE, ROBOT_UPGRADE_SOURCE)
        )
        if (advancedPowerModules > 0) {
            costs += ResourceCost(
                "advanced_power_modules",
                advancedPowerModules,
                if (advancedNeedsConfirmation) CostConfidence.NEEDS_CONFIRMATION else CostConfidence.COMMUNITY_REFERENCE,
                ROBOT_UPGRADE_SOURCE
            )
        }
        return ProgressionCatalogRecord(
            step = ProgressionStep(
                id = "robot-level-$fromLevel-$toLevel",
                domain = ProgressionDomain.ROBOT,
                itemName = "Robot Leveling",
                fromLevel = fromLevel,
                toLevel = toLevel,
                costs = costs,
                dataComplete = !advancedNeedsConfirmation
            ),
            prerequisiteNotes = if (advancedNeedsConfirmation) listOf(
                "LOJ XLab notes a conflict for Advanced Power Modules at Lv.60–70 (40 vs 140); confirm in-game."
            ) else emptyList(),
            sourceLabel = "LOJ XLab — Robot upgrade costs",
            sourceUrl = ROBOT_UPGRADE_SOURCE,
            checkedOn = ROBOT_SOURCE_CHECKED_ON
        )
    }

    private fun robotCoreRow(tier: Int, chips: Long): ProgressionCatalogRecord =
        ProgressionCatalogRecord(
            step = ProgressionStep(
                id = "robot-core-${tier - 1}-$tier",
                domain = ProgressionDomain.ROBOT_CORE,
                itemName = "Robot Core module",
                fromLevel = tier - 1,
                toLevel = tier,
                costs = listOf(
                    ResourceCost("core_chips", chips, CostConfidence.COMMUNITY_REFERENCE, ROBOT_UPGRADE_SOURCE)
                ),
                dataComplete = true
            ),
            prerequisiteNotes = listOf("Per module: Solo, Barrier or Formation."),
            sourceLabel = "LOJ XLab — Robot Core evolution",
            sourceUrl = ROBOT_UPGRADE_SOURCE,
            checkedOn = ROBOT_SOURCE_CHECKED_ON
        )

    private fun equipmentRow(
        fromLevel: Int,
        toLevel: Int,
        equipmentExp: Long,
        magnets: Long,
        potentialCoils: Long
    ): ProgressionCatalogRecord {
        val costs = mutableListOf(
            ResourceCost("equipment_exp", equipmentExp, CostConfidence.COMMUNITY_REFERENCE, HERO_EQUIPMENT_SOURCE),
            ResourceCost("magnets", magnets, CostConfidence.COMMUNITY_REFERENCE, HERO_EQUIPMENT_SOURCE)
        )
        if (potentialCoils > 0) {
            costs += ResourceCost("potential_coils", potentialCoils, CostConfidence.COMMUNITY_REFERENCE, HERO_EQUIPMENT_SOURCE)
        }
        return ProgressionCatalogRecord(
            step = ProgressionStep(
                id = "hero-equipment-$fromLevel-$toLevel",
                domain = ProgressionDomain.EQUIPMENT,
                itemName = "Hero Equipment",
                fromLevel = fromLevel,
                toLevel = toLevel,
                costs = costs,
                dataComplete = true
            ),
            prerequisiteNotes = emptyList(),
            sourceLabel = "LOJ Wiki — Hero equipment calculator",
            sourceUrl = HERO_EQUIPMENT_SOURCE,
            checkedOn = SOURCE_CHECKED_ON
        )
    }

}
