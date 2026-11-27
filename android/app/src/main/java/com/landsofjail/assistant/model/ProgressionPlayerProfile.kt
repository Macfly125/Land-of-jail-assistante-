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

    fun confirmedLevel(itemId: String): Int? {
        val confirmed = levels.filter { it.itemId == itemId && it.confirmed }
        if (confirmed.isEmpty()) return null
        val latestTimestamp = confirmed.maxOf { it.updatedAtEpochMillis }
        val latest = confirmed.filter { it.updatedAtEpochMillis == latestTimestamp }
        val distinctLevels = latest.map { it.level }.distinct()
        return distinctLevels.singleOrNull()
    }

    fun confirmedStock(resourceId: String): Long? {
        val canonicalId = canonicalProgressionResourceId(resourceId)
        val confirmed = resourceStocks.filter {
            canonicalProgressionResourceId(it.resourceId) == canonicalId && it.confirmed
        }
        if (confirmed.isEmpty()) return null
        val latestTimestamp = confirmed.maxOf { it.updatedAtEpochMillis }
        val latest = confirmed.filter { it.updatedAtEpochMillis == latestTimestamp }
        val distinctQuantities = latest.map { it.quantity }.distinct()
        return distinctQuantities.singleOrNull()
    }
}

/**
 * Merge fresh OCR observations without letting uncertain values erase a
 * previously confirmed stock. An empty observation preserves saved values.
 */
fun canonicalProgressionResourceId(rawId: String): String {
    // Preserve non-Latin labels before the ASCII fallback below removes them.
    when (rawId.trim().lowercase()) {
        "еда", "食物" -> return "food"
        "дерево", "木材" -> return "wood"
        "сталь", "钢材" -> return "steel"
        "бензин", "汽油" -> return "gasoline"
        "наличные", "现金" -> return "gold"
        "алмазы", "钻石" -> return "diamonds"
        "bargeld", "nakit" -> return "gold"
        "ядра forticlad" -> return "forticlad cores"
        "улучшенные ядра forticlad" -> return "advanced forticlad cores"
        "forticlad 核心" -> return "forticlad cores"
        "高级核心" -> return "advanced forticlad cores"
        "diamanten", "elmas", "diamanti" -> return "diamonds"
        "contanti" -> return "gold"
    }
    val key = rawId.trim().lowercase()
        .replace("é", "e").replace("è", "e").replace("ê", "e")
        .replace("à", "a").replace("â", "a").replace("î", "i")
        .replace("ô", "o").replace("û", "u").replace("ç", "c")
        .replace("ı", "i").replace("ş", "s").replace("ğ", "g").replace("ü", "u").replace("ö", "o")
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
        "prisoner armor data", "donnees armure prisonnier", "donnees d armure prisonnier" -> "prisoner armor data"
        "power modules", "module de puissance", "modules de puissance" -> "power modules"
        "advanced power modules", "modules de puissance avances", "module de puissance avance" -> "advanced power modules"
        "forticlad cores", "forticlad core", "forticlad nuclei", "noyaux forticlad", "noyau forticlad", "forticlad kerne", "forticlad cekirdekleri", "nuclei forticlad" -> "forticlad cores"
        "advanced forticlad cores", "advanced forticlad core", "advanced forticlad nuclei", "noyaux forticlad avances", "noyaux avances forticlad", "advanced cores", "erweiterte forticlad kerne", "gelismis cekirdekler", "gelismis forticlad cekirdekleri", "nuclei forticlad avanzati" -> "advanced forticlad cores"
        "core chips", "puces de noyau", "puces noyau" -> "core chips"
        "equipment exp", "exp equipement", "experience equipement" -> "equipment exp"
        "magnets", "aimants", "magneti" -> "magnets"
        "potential coils", "bobines de potentiel" -> "potential coils"
        "precision gears", "engrenages de precision" -> "precision gears"
        "hero shards", "fragments de heros" -> "hero shards"
        else -> key
    }
}

fun mergeProgressionStocks(
    existing: List<PlayerResourceStock>,
    observed: List<PlayerResourceStock>
): List<PlayerResourceStock> {
    // Merge by canonical resource ID. Confirmed observations take precedence
    // over uncertain OCR, then the newest confirmed observation wins.
    // If equally recent confirmed observations disagree, keep the quantity
    // only as an unconfirmed hint: a tie must never silently become fact.
    return (existing + observed)
        .groupBy { canonicalProgressionResourceId(it.resourceId) }
        .map { (key, entries) ->
            val confirmed = entries.filter { it.confirmed }
            val candidates = if (confirmed.isNotEmpty()) confirmed else entries
            val latestTimestamp = candidates.maxOf { it.updatedAtEpochMillis }
            val latest = candidates.filter { it.updatedAtEpochMillis == latestTimestamp }
            val conflictingConfirmedValues = confirmed.isNotEmpty() &&
                latest.map { it.quantity }.distinct().size > 1
            val chosen = latest.sortedWith(
                compareBy<PlayerResourceStock> { it.quantity }
                    .thenBy { it.resourceId.lowercase() }
                    .thenBy { it.resourceId }
            ).first()
            chosen.copy(
                resourceId = key,
                confirmed = chosen.confirmed && !conflictingConfirmedValues
            )
        }
        .sortedBy { it.resourceId }
}


/** Replace the manually confirmed Forticlad quantities in the shared player inventory. */
fun upsertForticladInventoryStocks(existing: List<PlayerResourceStock>, forticladCores: Long, advancedCores: Long, updatedAtEpochMillis: Long): List<PlayerResourceStock> {
    val ids = setOf("forticlad cores", "advanced forticlad cores")
    return (existing.filterNot { canonicalProgressionResourceId(it.resourceId) in ids } + listOf(
        PlayerResourceStock("forticlad cores", forticladCores.coerceAtLeast(0L), true, updatedAtEpochMillis),
        PlayerResourceStock("advanced forticlad cores", advancedCores.coerceAtLeast(0L), true, updatedAtEpochMillis)
    )).sortedBy { canonicalProgressionResourceId(it.resourceId) }
}

/**
 * Insert or replace a goal without accidentally losing the primary-goal flag.
 * Also normalizes legacy profiles to exactly one primary objective.
 */
fun upsertProgressionObjective(
    objectives: List<ProgressionObjective>,
    objective: ProgressionObjective
): List<ProgressionObjective> {
    val replaced = objectives.firstOrNull { it.id == objective.id }
    val shouldBePrimary = replaced?.isPrimary ?: objectives.none { it.isPrimary }
    val updated = objectives.filterNot { it.id == objective.id } +
        objective.copy(isPrimary = shouldBePrimary)
    val primaryId = updated.firstOrNull { it.isPrimary }?.id
        ?: updated.firstOrNull()?.id
    return updated.map { it.copy(isPrimary = it.id == primaryId) }
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
