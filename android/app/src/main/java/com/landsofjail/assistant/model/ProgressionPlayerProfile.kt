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

    fun confirmedStock(resourceId: String): Long? {
        val canonicalId = canonicalProgressionResourceId(resourceId)
        return resourceStocks.filter {
            canonicalProgressionResourceId(it.resourceId) == canonicalId && it.confirmed
        }.maxByOrNull { it.updatedAtEpochMillis }?.quantity
    }
}

/**
 * Merge fresh OCR observations without letting uncertain values erase a
 * previously confirmed stock. An empty observation preserves saved values.
 */
fun canonicalProgressionResourceId(rawId: String): String {
    val key = rawId.trim().lowercase()
        .replace("é", "e").replace("è", "e").replace("ê", "e")
        .replace("à", "a").replace("â", "a").replace("î", "i")
        .replace("ô", "o").replace("û", "u").replace("ç", "c")
        .replace(Regex("[^a-z0-9]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
    return when (key) {
        "food", "nourriture", "alimento", "essen", "cibo", "nahrung", "yiyecek",
        "nourriture food" -> "food"
        "wood", "bois", "holz", "madera", "legno", "odun", "bois wood" -> "wood"
        "steel", "acier", "stahl", "acero", "acciaio", "celik", "acier steel" -> "steel"
        "gasoline", "gas", "essence", "benzine", "benzin", "gasolina", "benzina",
        "essence gasoline" -> "gasoline"
        "gold", "or", "cash", "argent", "oro", "altin", "gold cash or", "cash or" -> "gold"
        "diamond", "diamonds", "diamant", "diamants", "gem", "gems", "gemme",
        "diamants gems" -> "diamonds"
        else -> key
    }
}

fun mergeProgressionStocks(
    existing: List<PlayerResourceStock>,
    observed: List<PlayerResourceStock>
): List<PlayerResourceStock> {
    // Collapse legacy aliases deterministically: confirmed beats uncertain,
    // then the newest observation wins. Never depend on input/list ordering.
    val merged = existing
        .groupBy { canonicalProgressionResourceId(it.resourceId) }
        .mapValuesTo(mutableMapOf()) { (key, entries) ->
            entries.maxWithOrNull(
                compareBy<PlayerResourceStock> { it.confirmed }
                    .thenBy { it.updatedAtEpochMillis }
            )!!.copy(resourceId = key)
        }

    observed.forEach { candidate ->
        val key = canonicalProgressionResourceId(candidate.resourceId)
        val normalized = candidate.copy(resourceId = key)
        val previous = merged[key]
        val shouldReplace = when {
            previous == null -> true
            normalized.confirmed && !previous.confirmed -> true
            !normalized.confirmed && previous.confirmed -> false
            else -> normalized.updatedAtEpochMillis >= previous.updatedAtEpochMillis
        }
        if (shouldReplace) merged[key] = normalized
    }
    return merged.values.sortedBy { it.resourceId }
}

/**
 * Remove one objective and keep exactly one primary objective when any remain.
 * An existing primary is preserved; otherwise the first remaining objective
 * becomes primary.
 */
fun removeProgressionObjective(
    objectives: List<ProgressionObjective>,
    objectiveId: String
): List<ProgressionObjective> {
    val remaining = objectives.filterNot { it.id == objectiveId }
    val primaryId = remaining.firstOrNull { it.isPrimary }?.id
        ?: remaining.firstOrNull()?.id
    return remaining.map { it.copy(isPrimary = it.id == primaryId) }
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
