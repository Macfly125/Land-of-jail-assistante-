package com.landsofjail.assistant.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TroopTrainingCatalogTest {
    @Test
    fun catalogContainsAllElevenTiersForEachTroopType() {
        TroopTrainingType.values().forEach { type ->
            val rows = TroopTrainingCatalog.rows.filter { it.type == type }
            assertEquals((1..11).toList(), rows.map { it.tier })
            assertTrue(rows.all { it.unitCosts.keys == linkedSetOf("food", "wood", "gasoline", "steel") })
            assertTrue(rows.all { it.sourceUrlForTest() == type.sourceUrl })
        }
    }

    @Test
    fun calculatesExactPerUnitCostsAndDurationForShieldbearers() {
        val plan = TroopTrainingCatalog.calculate(TroopTrainingType.SHIELDBEARER, 1, 100L)

        assertEquals(7_600L, plan.requirements["food"])
        assertEquals(5_000L, plan.requirements["wood"])
        assertEquals(300L, plan.requirements["gasoline"])
        assertEquals(1_700L, plan.requirements["steel"])
        assertEquals(1_200L, plan.totalDurationSeconds)
        assertEquals("Shieldbearer Barracks Lv.1", plan.unlockRequirement)
    }

    @Test
    fun calculatesEachTroopTypeIndependently() {
        val bomber = TroopTrainingCatalog.calculate(TroopTrainingType.BOMBER, 10, 2L)
        val shooter = TroopTrainingCatalog.calculate(TroopTrainingType.SHOOTER, 10, 2L)

        assertEquals(9_456L, bomber.requirements["food"])
        assertEquals(9_510L, bomber.requirements["wood"])
        assertEquals(458L, bomber.requirements["gasoline"])
        assertEquals(1_856L, bomber.requirements["steel"])
        assertEquals(6_820L, shooter.requirements["food"])
        assertEquals(10_620L, shooter.requirements["wood"])
        assertEquals(630L, shooter.requirements["gasoline"])
        assertEquals(1_588L, shooter.requirements["steel"])
    }

    @Test
    fun tierElevenIsExplicitlySeasonAndResearchGated() {
        TroopTrainingType.values().forEach { type ->
            val row = TroopTrainingCatalog.rows.single { it.type == type && it.tier == 11 }
            assertTrue(row.unlockRequirement.contains("Season 4"))
            assertTrue(row.unlockRequirement.contains("Forticlad Lab"))
        }
    }

    @Test
    fun zeroQuantityAndOverflowAreHandledWithoutNegativeTotals() {
        val zero = TroopTrainingCatalog.calculate(TroopTrainingType.SHOOTER, 11, 0L)
        assertTrue(zero.requirements.values.all { it == 0L })
        assertEquals(0L, zero.totalDurationSeconds)

        val huge = TroopTrainingCatalog.calculate(TroopTrainingType.SHIELDBEARER, 11, Long.MAX_VALUE)
        assertEquals(Long.MAX_VALUE, huge.requirements["food"])
        assertEquals(Long.MAX_VALUE, huge.totalDurationSeconds)
    }

    private fun TroopTrainingRow.sourceUrlForTest(): String = type.sourceUrl
}
