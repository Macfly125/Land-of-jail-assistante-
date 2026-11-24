package com.landsofjail.assistant.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForticladSelectionTest {
    @Test
    fun levelSixRemainsSelectableAndKeepsValidTarget() {
        assertTrue(ForticladSelection.currentLevels().contains(6))
        assertEquals(7, ForticladSelection.targetAfterCurrentChange(6, 10))
        assertEquals(10, ForticladSelection.targetAfterCurrentChange(6, 10))
    }

    @Test
    fun changingCurrentLevelOnlyMovesTargetWhenItIsNoLongerValid() {
        assertEquals(8, ForticladSelection.targetAfterCurrentChange(6, 8))
        assertEquals(7, ForticladSelection.targetAfterCurrentChange(6, 5))
        assertEquals(10, ForticladSelection.targetAfterCurrentChange(9, 8))
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
