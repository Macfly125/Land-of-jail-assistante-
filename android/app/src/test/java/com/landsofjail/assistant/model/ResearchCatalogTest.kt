package com.landsofjail.assistant.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResearchCatalogTest {
    @Test
    fun classicResearchTreeCountsAndSourcesAreRecordedWithoutInventingLevelRows() {
        assertEquals(46, ResearchTree.LOGISTICS.technologyCount)
        assertEquals(28, ResearchTree.ECONOMY.technologyCount)
        assertEquals(102, ResearchTree.MILITARY.technologyCount)
        assertTrue(ResearchTree.entries.filter { it != ResearchTree.FORTICLAD }
            .all { it.sourceUrl.startsWith("https://lojwiki.com/lab/") })
        assertTrue(ResearchCatalog.overviews.filter { it.tree != ResearchTree.FORTICLAD }
            .all { it.levelCostsImported.not() && it.prerequisitesImported.not() })
    }

    @Test
    fun forticladIsSeparateAndSeasonDependent() {
        assertEquals(9, ResearchTree.FORTICLAD.technologyCount)
        assertTrue(ResearchTree.FORTICLAD.seasonDependent)
        assertNull(ResearchTree.FORTICLAD.aggregateBaseDurationSeconds)
        assertTrue(ResearchTree.FORTICLAD.sourceUrl.endsWith("/forticlad-lab/"))
    }

    @Test
    fun aggregateDurationIsMetadataNotAnIndividualTechnologyCost() {
        assertTrue(ResearchTree.LOGISTICS.aggregateBaseDurationSeconds!! > 0L)
        assertTrue(ResearchTree.ECONOMY.aggregateBaseDurationSeconds!! > 0L)
        assertTrue(ResearchTree.MILITARY.aggregateBaseDurationSeconds!! > 0L)
        assertFalse(ResearchCatalog.overview(ResearchTree.LOGISTICS).levelCostsImported)
    }
}
