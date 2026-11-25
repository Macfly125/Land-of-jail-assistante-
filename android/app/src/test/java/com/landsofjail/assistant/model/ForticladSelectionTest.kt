package com.landsofjail.assistant.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForticladSelectionTest {
    @Test
    fun levelSixRemainsSelectableAndKeepsValidTarget() {
        assertTrue(ForticladSelection.currentLevels().contains(6))
        assertEquals(10, ForticladSelection.targetAfterCurrentChange(6, 10))
        assertEquals(7, ForticladSelection.targetAfterCurrentChange(6, 5))
    }

    @Test
    fun changingCurrentLevelOnlyMovesTargetWhenItIsNoLongerValid() {
        assertEquals(8, ForticladSelection.targetAfterCurrentChange(6, 8))
        assertEquals(7, ForticladSelection.targetAfterCurrentChange(6, 5))
        assertEquals(10, ForticladSelection.targetAfterCurrentChange(9, 8))
    }

    @Test
    fun allCurrentAndPersistedTargetCombinationsNormalizeSafely() {
        for (current in -2..12) {
            val normalizedCurrent = ForticladSelection.normalizeCurrentLevel(current)
            assertTrue(normalizedCurrent in 1..9)
            val targets = ForticladSelection.targetLevels(current)
            assertEquals(normalizedCurrent + 1, targets.first)
            assertEquals(10, targets.last)
            for (savedTarget in -2..12) {
                val normalizedTarget = ForticladSelection.targetAfterCurrentChange(current, savedTarget)
                assertTrue("current=$current target=$savedTarget normalized=$normalizedTarget", normalizedTarget > normalizedCurrent)
                assertTrue(normalizedTarget in (normalizedCurrent + 1)..10)
            }
        }
    }

    @Test
    fun completedProgressIsAlwaysWithinFiveStepLevel() {
        for (progress in -3..8) {
            assertTrue(ForticladSelection.normalizeCompletedSteps(progress) in 0..4)
        }
    }

    @Test
    fun allDropdownRangesStayWithinGameBounds() {
        assertEquals(1, ForticladSelection.currentLevels().first)
        assertEquals(9, ForticladSelection.currentLevels().last)
        assertEquals(0, ForticladSelection.completedSteps().first)
        assertEquals(4, ForticladSelection.completedSteps().last)
        assertEquals(10, ForticladSelection.targetLevels(9).single())
        assertEquals(2, ForticladSelection.targetLevels(1).first)
    }
}
