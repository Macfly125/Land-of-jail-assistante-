package com.landsofjail.assistant.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForticladCatalogTest {
    @Test
    fun firstForticladUpgradeFromBaseLevelThirtyCosts1250Cores() {
        val estimate = ForticladCatalog.estimate(
            buildingId = "wardens_office",
            currentLevel = 0,
            completedStages = 0,
            targetLevel = 1
        )

        assertEquals(1_250L, estimate?.forticladCores)
        assertEquals(0L, estimate?.advancedCores)
        assertEquals(5, estimate?.includedStages)
        assertEquals(CostConfidence.COMMUNITY_REFERENCE, estimate?.confidence)
    }
    @Test
    fun catalogRowsAreUniqueSourcedAndCommunityOnly() {
        val keys = ForticladCatalog.stageCosts.map { it.buildingId to it.targetLevel }
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(ForticladCatalog.stageCosts.all {
            it.targetLevel in 1..10 &&
                it.forticladCores > 0 &&
                it.advancedCores >= 0 &&
                it.confidence == CostConfidence.COMMUNITY_REFERENCE &&
                it.sourceUrl in setOf(
                    ForticladCatalog.SOURCE_URL,
                    ForticladCatalog.FORTICLAD_LAB_SOURCE_URL,
                    ForticladCatalog.MEDICAL_STATION_SOURCE_URL
                )
        })
        assertEquals(8, ForticladCatalog.eligibleBuildingIds.size)
        assertTrue(ForticladCatalog.stageCosts.any { it.buildingId == "forticlad_lab" })
        assertEquals(10, ForticladCatalog.stageCosts.count { it.buildingId == "forticlad_lab" })
        assertEquals(10, ForticladCatalog.stageCosts.count { it.buildingId == "medical_station" })
    }

    @Test
    fun estimateCountsOnlyRemainingStagesAndEveryIntermediateLevel() {
        val estimate = ForticladCatalog.estimate(
            buildingId = "wardens_office",
            currentLevel = 0,
            completedStages = 2,
            targetLevel = 2
        )

        // Three remaining FC1 stages (3 x 250) plus five FC2 stages (5 x 320).
        assertEquals(2_350L, estimate?.forticladCores)
        assertEquals(0L, estimate?.advancedCores)
        assertEquals(8, estimate?.includedStages)
        assertEquals(CostConfidence.COMMUNITY_REFERENCE, estimate?.confidence)
    }

    @Test
    fun advancedCoresAreAppliedStartingAtForticladLevelSix() {
        val estimate = ForticladCatalog.estimate(
            buildingId = "wardens_office",
            currentLevel = 5,
            completedStages = 2,
            targetLevel = 6
        )

        assertEquals(1_080L, estimate?.forticladCores)
        assertEquals(72L, estimate?.advancedCores)
        assertEquals(3, estimate?.includedStages)
    }

    @Test
    fun newlyPublishedMedicalStationAndForticladLabRowsAreAvailable() {
        val medical = ForticladCatalog.estimate("medical_station", 8, 0, 10)
        assertEquals(1_150L, medical?.forticladCores)
        assertEquals(240L, medical?.advancedCores)
        assertEquals(ForticladCatalog.MEDICAL_STATION_SOURCE_URL, medical?.sourceUrl)

        val lab = ForticladCatalog.estimate("forticlad_lab", 0, 0, 1)
        assertEquals(750L, lab?.forticladCores)
        assertEquals(0L, lab?.advancedCores)
        assertEquals(ForticladCatalog.FORTICLAD_LAB_SOURCE_URL, lab?.sourceUrl)

        val labSix = ForticladCatalog.estimate("forticlad_lab", 5, 0, 6)
        assertEquals(350L, labSix?.forticladCores)
        assertEquals(25L, labSix?.advancedCores)
    }

    @Test
    fun inventoryCalculatesMissingCoresWithoutNegativeValues() {
        val estimate = ForticladCatalog.estimate("forticlad_lab", 5, 0, 6)!!
        assertEquals(ForticladInventory(250L, 0L), ForticladInventory(100L, 30L).missingFor(estimate))
        assertEquals(ForticladInventory(0L, 0L), ForticladInventory(500L, 50L).missingFor(estimate))
    }

    @Test
    fun inventoryParsingAcceptsDigitsAndSafelyDefaultsInvalidOrOverflowValues() {
        assertEquals(ForticladInventory(12345L, 0L), ForticladInventory.parse("12 345", "abc"))
        assertEquals(ForticladInventory(0L, 0L), ForticladInventory.parse("999999999999999999999999", "-10"))
    }

    @Test
    fun invalidRangesAndStageCountsAreRejected() {
        assertNull(ForticladCatalog.estimate("wardens_office", 0, 5, 1))
        assertNull(ForticladCatalog.estimate("wardens_office", 4, 0, 4))
        assertNull(ForticladCatalog.estimate("unknown_building", 0, 0, 1))
    }
}
