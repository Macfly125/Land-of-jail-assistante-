package com.landsofjail.assistant.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ForticladCatalogTest {
    @Test
    fun catalogRowsAreUniqueSourcedAndCommunityOnly() {
        val keys = ForticladCatalog.stageCosts.map { it.buildingId to it.targetLevel }
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(ForticladCatalog.stageCosts.all {
            it.targetLevel in 1..10 &&
                it.forticladCores > 0 &&
                it.advancedCores >= 0 &&
                it.confidence == CostConfidence.COMMUNITY_REFERENCE &&
                it.sourceUrl == ForticladCatalog.SOURCE_URL
        })
        assertEquals(8, ForticladCatalog.eligibleBuildingIds.size)
        assertFalse(ForticladCatalog.stageCosts.any { it.buildingId == "forticlad_lab" })
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
    fun missingMedicalStationAndForticladLabRowsStayUnknown() {
        assertNull(ForticladCatalog.estimate("medical_station", 8, 0, 9))
        assertNull(ForticladCatalog.estimate("medical_station", 9, 0, 10))
        assertNull(ForticladCatalog.estimate("forticlad_lab", 0, 0, 1))
    }

    @Test
    fun invalidRangesAndStageCountsAreRejected() {
        assertNull(ForticladCatalog.estimate("wardens_office", 0, 5, 1))
        assertNull(ForticladCatalog.estimate("wardens_office", 4, 0, 4))
        assertNull(ForticladCatalog.estimate("unknown_building", 0, 0, 1))
    }
}
