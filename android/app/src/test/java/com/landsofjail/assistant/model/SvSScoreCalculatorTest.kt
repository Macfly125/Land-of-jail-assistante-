package com.landsofjail.assistant.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SvSScoreCalculatorTest {
    @Test
    fun calculatesTotalPoints() {
        assertEquals(9000L, SvSScoreCalculator.totalPoints(3L, 3000L))
        assertEquals(0L, SvSScoreCalculator.totalPoints(0L, 3000L))
        assertEquals(0L, SvSScoreCalculator.totalPoints(5L, 0L))
    }

    @Test
    fun calculatesMinimumWholeUnitsForTarget() {
        assertEquals(3L, SvSScoreCalculator.unitsNeeded(9000L, 3000L))
        assertEquals(4L, SvSScoreCalculator.unitsNeeded(9001L, 3000L))
        assertEquals(1L, SvSScoreCalculator.unitsNeeded(1L, 60000L))
        assertEquals(0L, SvSScoreCalculator.unitsNeeded(0L, 60000L))
        assertEquals(0L, SvSScoreCalculator.unitsNeeded(100L, 0L))
    }

    @Test
    fun totalPointsSaturatesInsteadOfOverflowing() {
        assertEquals(Long.MAX_VALUE, SvSScoreCalculator.totalPoints(Long.MAX_VALUE, 2L))
    }

    @Test
    fun unitCountDoesNotOverflowAtMaximumTarget() {
        assertEquals(3074457345618259L, SvSScoreCalculator.unitsNeeded(Long.MAX_VALUE, 3000L))
    }
}
