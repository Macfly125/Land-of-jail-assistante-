package com.landsofjail.assistant.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * Persistent, local-first player progression profile.
 * Values are only marked confirmed when the player or a validated in-game
 * capture explicitly confirms them. Empty values are intentionally unknown.
 */
@Serializable
data class ProgressionObjective(
    val id: String,
    val domain: ProgressionDomain,
    val targetId: String,
    val targetLevel: Int,
    val isPrimary: Boolean = false,
    val createdAtEpochMillis: Long = 0L
) {
    init {
        require(id.isNotBlank() && targetId.isNotBlank())
        require(targetLevel >= 0)
    }
}

@Serializable
data class ProgressionLevelRecord(
    val itemId: String,
    val level: Int,
    val confirmed: Boolean,
    val updatedAtEpochMillis: Long
) {
    init {
        require(itemId.isNotBlank() && level >= 0)
    }
}

@Serializable
data class ProgressionPlayerProfile(
    val schemaVersion: Int = 1,
    val profileId: String,
    val objectives: List<ProgressionObjective> = emptyList(),
    val levels: List<ProgressionLevelRecord> = emptyList(),
    val resourceStocks: List<PlayerResourceStock> = emptyList(),
    val updatedAtEpochMillis: Long = 0L
) {
    init {
        require(profileId.isNotBlank())
        require(schemaVersion == 1)
        require(objectives.map { it.id }.distinct().size == objectives.size)
    }

    val primaryObjective: ProgressionObjective?
        get() = objectives.firstOrNull { it.isPrimary } ?: objectives.firstOrNull()

    fun confirmedLevel(itemId: String): Int? =
        levels.filter { it.itemId == itemId && it.confirmed }
            .maxByOrNull { it.updatedAtEpochMillis }?.level

    fun confirmedStock(resourceId: String): Long? =
        resourceStocks.filter { it.resourceId == resourceId && it.confirmed }
            .maxByOrNull { it.updatedAtEpochMillis }?.quantity
}

object ProgressionProfileCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(profile: ProgressionPlayerProfile): String = json.encodeToString(profile)

    fun decodeOrDefault(encoded: String?, profileId: String): ProgressionPlayerProfile {
        if (encoded.isNullOrBlank()) return ProgressionPlayerProfile(profileId = profileId)
        return runCatching {
            json.decodeFromString<ProgressionPlayerProfile>(encoded)
                .takeIf { it.profileId == profileId } ?: ProgressionPlayerProfile(profileId = profileId)
        }.getOrElse { ProgressionPlayerProfile(profileId = profileId) }
    }
}
