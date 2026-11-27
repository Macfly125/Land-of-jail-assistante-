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
    fun researchGoalAliasesResolveOnlyToKnownTrees() {
        assertEquals(ResearchTree.LOGISTICS, ResearchCatalog.treeForTarget("Logistics"))
        assertEquals(ResearchTree.LOGISTICS, ResearchCatalog.treeForTarget("Logistique"))
        assertEquals(ResearchTree.ECONOMY, ResearchCatalog.treeForTarget("économie"))
        assertEquals(ResearchTree.MILITARY, ResearchCatalog.treeForTarget("military"))
        assertEquals(ResearchTree.FORTICLAD, ResearchCatalog.treeForTarget("Forticlad Lab"))
        assertNull(ResearchCatalog.treeForTarget("unknown research"))
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

    @Test
    fun publishedAggregateSummariesAreAvailableForAllFourTrees() {
        assertEquals(4, ResearchCatalog.aggregateSummaries.size)
        assertEquals(1_924_872L, ResearchCatalog.aggregateSummary(ResearchTree.LOGISTICS).totalPower)
        assertEquals(196_769L, ResearchCatalog.aggregateSummary(ResearchTree.ECONOMY).totalPower)
        assertEquals(7_347_780L, ResearchCatalog.aggregateSummary(ResearchTree.MILITARY).totalPower)
        assertEquals(18_348_000L, ResearchCatalog.aggregateSummary(ResearchTree.FORTICLAD).totalPower)
        assertEquals(
            515L * 24L * 60L * 60L + 19L * 60L * 60L,
            ResearchCatalog.aggregateSummary(ResearchTree.FORTICLAD).maximumDurationSeconds
        )
    }

    @Test
    fun aggregateCostSummariesRemainDisplayOnlyAndNeverEnableLevelPlanning() {
        ResearchTree.entries.forEach { tree ->
            assertTrue(ResearchCatalog.aggregateSummary(tree).costSummary.isNotBlank())
            assertFalse(ResearchCatalog.overview(tree).levelCostsImported)
            assertFalse(ResearchCatalog.overview(tree).prerequisitesImported)
        }
    }
}
