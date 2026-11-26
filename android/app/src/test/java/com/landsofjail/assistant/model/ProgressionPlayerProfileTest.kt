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
    fun confirmedLevelPrefersNewestConfirmedRecordAndIgnoresUncertainUpdates() {
        val profile = ProgressionPlayerProfile(
            profileId = "main",
            levels = listOf(
                ProgressionLevelRecord("warden_office", 12, true, 100L),
                ProgressionLevelRecord("warden_office", 14, false, 300L),
                ProgressionLevelRecord("warden_office", 13, true, 200L)
            )
        )

        assertEquals(13, profile.confirmedLevel("warden_office"))
    }

    @Test
    fun confirmedLevelIsUnknownWhenOnlyUnconfirmedObservationsExist() {
        val profile = ProgressionPlayerProfile(
            profileId = "main",
            levels = listOf(
                ProgressionLevelRecord("main_lab", 9, false, 500L)
            )
        )

        assertNull(profile.confirmedLevel("main_lab"))
    }

    @Test
    fun confirmedStockLookupReturnsUnknownWhenLatestConfirmedAliasesConflict() {
        val profile = ProgressionPlayerProfile(
            profileId = "main",
            resourceStocks = listOf(
                PlayerResourceStock("wood", 700L, true, 500L),
                PlayerResourceStock("Bois", 900L, true, 500L),
                PlayerResourceStock("Holz", 600L, true, 400L)
            )
        )

        assertNull(profile.confirmedStock("wood"))
    }

    @Test
    fun confirmedLevelLookupReturnsUnknownWhenLatestConfirmedRecordsConflict() {
        val profile = ProgressionPlayerProfile(
            profileId = "main",
            levels = listOf(
                ProgressionLevelRecord("warden_office", 18, true, 500L),
                ProgressionLevelRecord("warden_office", 19, true, 500L),
                ProgressionLevelRecord("warden_office", 17, true, 400L)
            )
        )

        assertNull(profile.confirmedLevel("warden_office"))
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
    fun conflictingConfirmedAliasesAtSameTimestampBecomeUnconfirmedRegardlessOfOrder() {
        val first = PlayerResourceStock("wood", 700L, true, 500L)
        val second = PlayerResourceStock("Bois", 900L, true, 500L)

        val forward = mergeProgressionStocks(listOf(first, second), emptyList())
        val reverse = mergeProgressionStocks(listOf(second, first), emptyList())

        assertEquals(forward, reverse)
        assertEquals(1, forward.size)
        assertEquals(false, forward.single().confirmed)
        assertEquals(500L, forward.single().updatedAtEpochMillis)
    }

    @Test
    fun newestConfirmedStockWinsOverNewerUncertainObservation() {
        val merged = mergeProgressionStocks(
            listOf(PlayerResourceStock("wood", 700L, true, 500L)),
            listOf(PlayerResourceStock("Bois", 900L, false, 900L))
        )

        assertEquals(700L, merged.single().quantity)
        assertEquals(true, merged.single().confirmed)
        assertEquals(500L, merged.single().updatedAtEpochMillis)
    }

    @Test
    fun confirmedStockLookupAcceptsLocalizedAliasesAndPrefersLatestConfirmedValue() {
        val profile = ProgressionPlayerProfile(
            profileId = "main",
            resourceStocks = listOf(
                PlayerResourceStock("Bois", 400L, true, 100L),
                PlayerResourceStock("wood", 650L, true, 200L),
                PlayerResourceStock("Holz", 999L, false, 300L)
            )
        )

        assertEquals(650L, profile.confirmedStock("Bois"))
        assertEquals(650L, profile.confirmedStock("WOOD"))
        assertNull(profile.confirmedStock("acier"))
    }

    @Test
    fun stockMergeWithNoNewObservationPreservesExistingConfirmedAndUnknownValues() {
        val existing = listOf(
            PlayerResourceStock("wood", 700L, true, 100L),
            PlayerResourceStock("steel", 250L, false, 110L)
        )

        val merged = mergeProgressionStocks(existing, emptyList()).associateBy { it.resourceId }
        assertEquals(700L, merged.getValue("wood").quantity)
        assertEquals(true, merged.getValue("wood").confirmed)
        assertEquals(250L, merged.getValue("steel").quantity)
        assertEquals(false, merged.getValue("steel").confirmed)
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
    @Test
    fun deletingObjectivePreservesExactlyOnePrimaryGoal() {
        val objectives = listOf(
            ProgressionObjective("first", ProgressionDomain.BUILDING, "gate", 8, false),
            ProgressionObjective("primary", ProgressionDomain.BUILDING, "warden_office", 20, true),
            ProgressionObjective("secondary", ProgressionDomain.HERO, "ada", 5, false)
        )

        val afterDeletingSecondary = removeProgressionObjective(objectives, "secondary")
        assertEquals(listOf("primary"), afterDeletingSecondary.filter { it.isPrimary }.map { it.id })

        val afterDeletingPrimary = removeProgressionObjective(objectives, "primary")
        assertEquals(listOf("first"), afterDeletingPrimary.filter { it.isPrimary }.map { it.id })

        val afterDeletingLast = removeProgressionObjective(afterDeletingPrimary, "secondary")
            .let { removeProgressionObjective(it, "first") }
        assertEquals(emptyList<ProgressionObjective>(), afterDeletingLast)
    }

    @Test
    fun deletingFromLegacyMultiplePrimaryObjectivesNormalizesToOne() {
        val objectives = listOf(
            ProgressionObjective("one", ProgressionDomain.BUILDING, "gate", 8, true),
            ProgressionObjective("two", ProgressionDomain.BUILDING, "lab", 10, true)
        )
        val remaining = removeProgressionObjective(objectives, "missing-id")
        assertEquals(listOf("one"), remaining.filter { it.isPrimary }.map { it.id })
    }

    @Test
    fun updatingExistingPrimaryGoalKeepsItPrimary() {
        val existing = listOf(
            ProgressionObjective("primary", ProgressionDomain.BUILDING, "warden_office", 20, true),
            ProgressionObjective("secondary", ProgressionDomain.HERO, "ada", 5, false)
        )
        val updated = upsertProgressionObjective(
            existing,
            ProgressionObjective("primary", ProgressionDomain.BUILDING, "warden_office", 22)
        )

        assertEquals(listOf("primary"), updated.filter { it.isPrimary }.map { it.id })
        assertEquals(22, updated.first { it.id == "primary" }.targetLevel)
    }

    @Test
    fun updatingSecondaryGoalDoesNotStealPrimaryStatus() {
        val existing = listOf(
            ProgressionObjective("primary", ProgressionDomain.BUILDING, "warden_office", 20, true),
            ProgressionObjective("secondary", ProgressionDomain.HERO, "ada", 5, false)
        )
        val updated = upsertProgressionObjective(
            existing,
            ProgressionObjective("secondary", ProgressionDomain.HERO, "ada", 7)
        )

        assertEquals(listOf("primary"), updated.filter { it.isPrimary }.map { it.id })
        assertEquals(7, updated.first { it.id == "secondary" }.targetLevel)
    }

    @Test
    fun firstGoalBecomesPrimaryAndLegacyMultiplePrimaryGoalsAreNormalized() {
        val first = upsertProgressionObjective(
            emptyList(),
            ProgressionObjective("first", ProgressionDomain.BUILDING, "gate", 8)
        )
        assertEquals(listOf("first"), first.filter { it.isPrimary }.map { it.id })

        val normalized = upsertProgressionObjective(
            listOf(
                ProgressionObjective("one", ProgressionDomain.BUILDING, "gate", 8, true),
                ProgressionObjective("two", ProgressionDomain.HERO, "ada", 5, true)
            ),
            ProgressionObjective("three", ProgressionDomain.ROBOT, "robot-a", 3)
        )
        assertEquals(listOf("one"), normalized.filter { it.isPrimary }.map { it.id })
    }

    @Test
    fun canonicalResourceIdsSupportRussianChineseAndLocalizedLabels() {
        val aliases = mapOf(
            "Еда" to "food",
            "食物" to "food",
            "Дерево" to "wood",
            "木材" to "wood",
            "Сталь" to "steel",
            "钢材" to "steel",
            "Бензин" to "gasoline",
            "汽油" to "gasoline",
            "Наличные" to "gold",
            "现金" to "gold",
            "Bargeld" to "gold",
            "Nakit" to "gold",
            "Алмазы" to "diamonds",
            "钻石" to "diamonds",
            "Diamanten" to "diamonds",
            "Elmas" to "diamonds",
            "Diamanti" to "diamonds",
            "Contanti" to "gold"
        )
        aliases.forEach { (input, expected) ->
            assertEquals(input, expected, canonicalProgressionResourceId(input))
        }
    }

}
