package com.landsofjail.assistant.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RobotPortraitMatcherTest {
    private fun template(id: String, name: String, pixels: IntArray) =
        RobotPortraitTemplate(id, name, 2, 2, pixels)

    @Test
    fun confirmsAnExactUniquePortraitMatch() {
        val target = intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt(), 0xFFFFFFFF.toInt())
        val other = intArrayOf(0xFF101010.toInt(), 0xFF202020.toInt(), 0xFF303030.toInt(), 0xFF404040.toInt())

        val result = RobotPortraitMatcher.match(
            2, 2, target,
            listOf(template("robot-a", "Robot A", target), template("robot-b", "Robot B", other))
        )

        assertTrue(result.isConfirmed)
        assertEquals("robot-a", result.robotId)
        assertEquals("Robot A", result.displayName)
        assertEquals(1f, result.similarity, 0.0001f)
    }

    @Test
    fun groupsAlternatePortraitTemplatesByRobotIdentity() {
        val target = intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt(), 0xFFFFFFFF.toInt())
        val alternate = intArrayOf(0xFFEE0000.toInt(), 0xFF00EE00.toInt(), 0xFF0000EE.toInt(), 0xFFEEEEEE.toInt())
        val unrelated = intArrayOf(0xFF101010.toInt(), 0xFF202020.toInt(), 0xFF303030.toInt(), 0xFF404040.toInt())

        val result = RobotPortraitMatcher.match(
            2, 2, target,
            listOf(
                template("robot-a", "Robot A", target),
                template("robot-a", "Robot A", alternate),
                template("robot-b", "Robot B", unrelated)
            )
        )

        assertTrue(result.isConfirmed)
        assertEquals("robot-a", result.robotId)
        assertEquals("Robot A", result.displayName)
        assertEquals(1f, result.similarity, 0.0001f)
    }

    @Test
    fun ignoresCatalogIdentityWithConflictingDisplayNames() {
        val target = intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt(), 0xFFFFFFFF.toInt())
        val unrelated = intArrayOf(0xFF101010.toInt(), 0xFF202020.toInt(), 0xFF303030.toInt(), 0xFF404040.toInt())

        val result = RobotPortraitMatcher.match(
            2, 2, target,
            listOf(
                template("robot-a", "Ranger", target),
                template("robot-a", "Phantom Cat", target),
                template("robot-b", "Robot B", unrelated)
            )
        )

        assertFalse(result.isConfirmed)
        assertNull(result.robotId)
        assertNull(result.displayName)
        assertEquals("similarite_insuffisante", result.reason)
    }

    @Test
    fun rejectsCatalogWhenEveryRobotIdentityHasConflictingNames() {
        val target = intArrayOf(0xFFFF0000.toInt(), 0xFF00FF00.toInt(), 0xFF0000FF.toInt(), 0xFFFFFFFF.toInt())
        val result = RobotPortraitMatcher.match(
            2, 2, target,
            listOf(
                template("robot-a", "Ranger", target),
                template("robot-a", "Phantom Cat", target)
            )
        )

        assertFalse(result.isConfirmed)
        assertNull(result.robotId)
        assertNull(result.displayName)
        assertEquals("catalogue_identites_incoherentes", result.reason)
    }

    @Test
    fun refusesToGuessWhenTwoDifferentRobotsHaveEquallySimilarPortraits() {
        val target = intArrayOf(0xFF112233.toInt(), 0xFF223344.toInt(), 0xFF334455.toInt(), 0xFF445566.toInt())

        val result = RobotPortraitMatcher.match(
            2, 2, target,
            listOf(template("robot-a", "Robot A", target), template("robot-b", "Robot B", target))
        )

        assertFalse(result.isConfirmed)
        assertNull(result.robotId)
        assertNull(result.displayName)
        assertEquals("correspondances_trop_proches", result.reason)
    }

    @Test
    fun refusesAWeakMatchInsteadOfReturningTheClosestRobot() {
        val target = intArrayOf(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
        val unrelated = intArrayOf(0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF000000.toInt())

        val result = RobotPortraitMatcher.match(2, 2, target, listOf(template("robot-a", "Robot A", unrelated)))

        assertFalse(result.isConfirmed)
        assertNull(result.robotId)
        assertEquals("similarite_insuffisante", result.reason)
    }

    @Test
    fun refusesToMatchWhenCatalogHasNoUsableTemplates() {
        val target = intArrayOf(0, 0, 0, 0)

        val empty = RobotPortraitMatcher.match(2, 2, target, emptyList())
        val wrongSize = RobotPortraitMatcher.match(
            2, 2, target,
            listOf(RobotPortraitTemplate("robot-a", "Robot A", 1, 1, intArrayOf(0)))
        )

        assertEquals("catalogue_visuel_vide", empty.reason)
        assertEquals("dimensions_incompatibles", wrongSize.reason)
        assertNull(empty.robotId)
        assertNull(wrongSize.robotId)
    }
}
