package com.landsofjail.assistant.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class ProgressionPlayerProfileTest {
    @Test
    fun codecRoundTripsObjectivesLevelsAndConfirmedStocks() {
        val profile = ProgressionPlayerProfile(
            profileId = "main",
            objectives = listOf(
                ProgressionObjective("goal-hq-20", ProgressionDomain.BUILDING, "warden_office", 20, true, 100L),
                ProgressionObjective("goal-lab-10", ProgressionDomain.RESEARCH, "main_lab", 10, false, 110L)
            ),
            levels = listOf(
                ProgressionLevelRecord("warden_office", 16, true, 200L),
                ProgressionLevelRecord("main_lab", 8, false, 210L)
            ),
            resourceStocks = listOf(
                PlayerResourceStock("wood", 500L, true, 300L),
                PlayerResourceStock("steel", 200L, false, 310L)
            ),
            updatedAtEpochMillis = 400L
        )

        val restored = ProgressionProfileCodec.decodeOrDefault(
            ProgressionProfileCodec.encode(profile), "main"
        )
        assertEquals(profile, restored)
        assertEquals("warden_office", restored.primaryObjective?.targetId)
        assertEquals(16, restored.confirmedLevel("warden_office"))
        assertNull(restored.confirmedLevel("main_lab"))
        assertEquals(500L, restored.confirmedStock("wood"))
        assertNull(restored.confirmedStock("steel"))
    }

    @Test
    fun codecReturnsEmptyProfileForCorruptOrForeignData() {
        val corrupt = ProgressionProfileCodec.decodeOrDefault("{broken", "main")
        assertEquals("main", corrupt.profileId)
        assertEquals(emptyList<ProgressionObjective>(), corrupt.objectives)

        val foreign = ProgressionProfileCodec.decodeOrDefault(
            ProgressionProfileCodec.encode(ProgressionPlayerProfile(profileId = "other")),
            "main"
        )
        assertEquals("main", foreign.profileId)
        assertEquals(emptyList<ProgressionObjective>(), foreign.objectives)
    }

    @Test
    fun objectivesCanBeAddedWithoutOverwritingOtherGoals() {
        val initial = ProgressionPlayerProfile(
            profileId = "main",
            objectives = listOf(ProgressionObjective(
                "primary", ProgressionDomain.BUILDING, "warden_office", 18, true
            ))
        )
        val updated = initial.copy(objectives = initial.objectives + ProgressionObjective(
            "secondary", ProgressionDomain.HERO, "ada", 5
        ))
        assertEquals(2, updated.objectives.size)
        assertNotNull(updated.primaryObjective)
        assertEquals("warden_office", updated.primaryObjective?.targetId)
    }
    @Test
    fun stockMergeNeverLetsAnUncertainCaptureReplaceAConfirmedValue() {
        val existing = listOf(PlayerResourceStock("wood", 900L, true, 100L))
        val observed = listOf(PlayerResourceStock("Bois (Wood)", 120L, false, 200L))

        val merged = mergeProgressionStocks(existing, observed)

        assertEquals(1, merged.size)
        assertEquals("wood", merged.single().resourceId)
        assertEquals(900L, merged.single().quantity)
        assertEquals(true, merged.single().confirmed)
        assertEquals(100L, merged.single().updatedAtEpochMillis)
    }

    @Test
    fun stockMergeUsesNewestConfirmedObservationAcrossLocalizedAliases() {
        val existing = listOf(PlayerResourceStock("Nourriture", 500L, true, 100L))
        val observed = listOf(PlayerResourceStock("Food", 750L, true, 200L))

        val merged = mergeProgressionStocks(existing, observed)

        assertEquals(1, merged.size)
        assertEquals("food", merged.single().resourceId)
        assertEquals(750L, merged.single().quantity)
        assertEquals(200L, merged.single().updatedAtEpochMillis)
    }

    @Test
    fun canonicalResourceIdsNormalizeAllSixMainResourceFamilies() {
        val aliases = mapOf(
            "Nourriture (Food)" to "food",
            "Bois (Wood)" to "wood",
            "Acier (Steel)" to "steel",
            "Essence (Gasoline)" to "gasoline",
            "Cash / Or" to "gold",
            "Diamants (Gems)" to "diamonds"
        )

        aliases.forEach { (input, expected) ->
            assertEquals(input, expected, canonicalProgressionResourceId(input))
        }
    }
}
