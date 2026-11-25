package com.landsofjail.assistant.data

import kotlinx.serialization.Serializable
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Serializable
data class GiftCodeEntry(
    val code: String,
    val status: String,
    val source: String,
    val firstSeen: String = "",
    val lastChecked: String = "",
    val expiresAt: String = "",
    val reward: String = "",
    val note: String = ""
)

@Serializable
data class GiftCodeSnapshot(
    val updatedAt: String = "",
    val sourceErrors: List<String> = emptyList(),
    val entries: List<GiftCodeEntry> = emptyList()
)

object GiftCodeRepository {
    const val XLAB_URL = "https://lojxlab.com/promo-codes/"
    const val GUIDE_URL = "https://guideloj.com/fr/codes"
    const val GUIDE_HOME_URL = "https://guideloj.com/fr/home"
    const val DISCORD_URL = "https://discord.gg/YGJqgcGpn"
    const val REDEEM_URL = "https://www.gamermall.net/global/loj/cdkey"

    private val communityListedCodes = listOf(
        "VIP777", "LOJ3648", "LOJNOW", "LOJ1478",
        "loj777", "loj888", "loj5791", "loj3579"
    )
    private val expiredCodes = listOf(
        "ZYKtwu9hchxuSv" to "26/09/2026",
        "BACK2PLAY" to "29/07/2026",
        "HappyMay1st" to "03/05/2026",
        "AprilFool2026" to "03/04/2026",
        "LOJ1stAnniversary" to "23/03/2026",
        "2026HERDAY" to "11/03/2026",
        "LOJLOVE2026" to "18/02/2026",
        "XMASG2025" to "28/12/2025",
    )
    private val unverifiedCodes = listOf("RuStore777", "HeartofWar777", "R8K3L7T5", "C5F2R9A1")

    fun initialSnapshot(): GiftCodeSnapshot {
        val entries = buildList {
            communityListedCodes.forEach {
                val rewards = when (it.lowercase(Locale.ROOT)) {
                    "vip777" -> "3 000 diamants, 10 fragments de héros épiques, 30 pièces d’équipement (100 EXP), 50 coffres de ressources au choix II et 30 objets d’endurance (10)."
                    "loj3648" -> "3 000 diamants, 3 fragments de héros légendaires et 4 accélérateurs (1 h) de construction, d’entraînement et de recherche."
                    "lojnow" -> "3 000 diamants, 20 cartes de ravitaillement avancées, 10 objets d’EXP héros (10 000), 30 coffres de ressources au choix II et 5 accélérateurs universels (1 h)."
                    "loj1478" -> "3 000 diamants, 10 équipements de précision, 10 objets d’EXP héros (10 000), 5 accélérateurs de marche II et 30 coffres de ressources au choix II."
                    "loj777" -> "1 000 diamants, 2 fragments de héros légendaires, 2 accélérateurs (1 h) de construction, d’entraînement et de recherche."
                    "loj888" -> "1 000 diamants, 5 fragments de héros épiques, 5 pièces d’équipement (100 EXP), 10 coffres de ressources au choix II et 15 objets d’endurance (10)."
                    "loj5791" -> "1 000 diamants, 3 équipements de précision, 10 coffres de ressources au choix II et 5 accélérateurs universels (1 h)."
                    else -> "1 000 diamants, 5 cartes de ravitaillement avancées, 10 coffres de ressources au choix II et 3 accélérateurs universels (1 h)."
                }
                add(GiftCodeEntry(it, "COMMUNITY_LISTED", "LOJ XLab · annonce communautaire du canal #gift-codes",
                    "21/09/2026", "01/11/2026", reward = rewards,
                    note = "LOJ XLab rapporte une publication du 21/09/2026 et des tests communautaires le 13/10/2026 ; sa dernière vérification publiée date du 01/11/2026. Ce code n’est pas vérifié en direct par l’application : confirmer le résultat dans le jeu. Aucune expiration annoncée par cette source."))
            }
            expiredCodes.forEach { (code, expiry) ->
                add(GiftCodeEntry(code, "EXPIRED", "LOJ XLab · historique", expiresAt = expiry,
                    note = "Code classé expiré par la source communautaire."))
            }
            // Pocket Gamer affiche encore N4HDYuJxkQKur5 avec une échéance
            // au 30/11/2026 ; l'ancien catalogue contenait par erreur 2025.
            add(GiftCodeEntry("N4HDYuJxkQKur5", "NEEDS_REVIEW", "Pocket Gamer · 22/11/2026",
                firstSeen = "22/11/2026", expiresAt = "30/11/2026",
                note = "Échéance publiée au 30/11/2026 par Pocket Gamer ; disponibilité non testée par l'application."))
            unverifiedCodes.forEach {
                add(GiftCodeEntry(it, "UNVERIFIED", "LOJ XLab · autres sites",
                    note = "Non publié dans la liste officielle ; disponibilité non confirmée."))
            }
            listOf(
                GiftCodeEntry("LOJCANDY49", "NEEDS_REVIEW", "Pocket Gamer / MrGuider",
                    firstSeen = "31/10/2025", expiresAt = "02/11/2025",
                    note = "Sources contradictoires : une liste récente le dit actif, une autre indique une ancienne expiration. À tester."),
                GiftCodeEntry("GuZQdq1KnWQJMx", "UNVERIFIED", "Pocket Gamer · 22/11/2026",
                    firstSeen = "22/11/2026", note = "Récompenses annoncées : 9 000 diamants, 7 éclats légendaires et accélérations. Non confirmé par l’équipe du jeu."),
                GiftCodeEntry("WELOJAIL111", "UNVERIFIED", "Pocket Gamer · 22/11/2026",
                    firstSeen = "22/11/2026", note = "Récompenses annoncées : cartes d’approvisionnement, diamants et pièces d’équipement. Non confirmé officiellement."),
                GiftCodeEntry("JAILGIFT2025", "UNVERIFIED", "Pocket Gamer · 22/11/2026",
                    firstSeen = "22/11/2026", note = "Code cité par un site tiers ; validité à vérifier."),
                GiftCodeEntry("VUBhpc4ec7cNwP", "EXPIRED", "MrGuider · 30/10/2026",
                    firstSeen = "30/10/2026", expiresAt = "02/11/2026",
                    note = "Expiration publiée au 2 novembre 2026, heure serveur non précisée."),
                GiftCodeEntry("KOF777", "UNVERIFIED", "MrGuider",
                    note = "Code listé par un site tiers, sans validation officielle."),
                GiftCodeEntry("KOF888", "UNVERIFIED", "MrGuider",
                    note = "Code listé par un site tiers, sans validation officielle."),
                GiftCodeEntry("KOF1234", "UNVERIFIED", "MrGuider",
                    note = "Code listé par un site tiers, sans validation officielle.")
            ).forEach { add(it) }
        }
        return GiftCodeSnapshot(entries = entries)
    }

    suspend fun refresh(previous: GiftCodeSnapshot): GiftCodeSnapshot {
        val now = Instant.now().toString()
        val errors = mutableListOf<String>()
        val found = linkedMapOf<String, GiftCodeEntry>()
        previous.entries.forEach { found[it.code.lowercase(Locale.ROOT)] = it }

        val sources = listOf(
            "LOJ XLab" to XLAB_URL,
            "GuideLOJ FR · codes" to GUIDE_URL,
            "GuideLOJ FR · accueil" to GUIDE_HOME_URL,
            "Pocket Gamer" to "https://www.pocketgamer.com/lands-of-jail/codes/",
            "MrGuider" to "https://www.mrguider.org/codes/lands-of-jail-codes/"
        )
        val downloaded = coroutineScope {
            sources.map { (sourceName, url) ->
                async(Dispatchers.IO) { sourceName to runCatching { fetch(url) } }
            }.awaitAll()
        }
        for ((sourceName, result) in downloaded) {
            val html = result.getOrNull()
            if (html == null) {
                errors += "$sourceName : ${result.exceptionOrNull()?.message ?: "source inaccessible"}"
                continue
            }
            val checkedDate = Regex("(?i)(?:last checked|checked|updated|derni[eè]re v[eé]rification|derni[eè]re mise [àa] jour|mis[e] [àa] jour)\\s*[:\\-]?\\s*(?:on\\s*)?([0-9]{1,2}[./-][0-9]{1,2}[./-]20[0-9]{2}|[0-9]{1,2}\\s+[A-Za-zÀ-ÿ]+\\s+20[0-9]{2}|[A-Za-zÀ-ÿ]+\\s+[0-9]{1,2},?\\s+20[0-9]{2})")
                .find(stripTags(html))?.groupValues?.getOrNull(1).orEmpty()
            for ((code, nearby) in extractCandidatesFromHtml(html)) {
                val key = code.lowercase(Locale.ROOT)
                val existing = found[key]
                if (existing != null) {
                    val mergedSource = (existing.source.split(" · ") + sourceName).distinct().joinToString(" · ")
                    found[key] = existing.copy(source = mergedSource, lastChecked = newerDate(existing.lastChecked, checkedDate))
                } else {
                    val looksExpired = nearby.contains("ended codes") || nearby.contains("expired") ||
                        nearby.contains("codes expir") || nearby.contains("codes termin")
                    found[key] = GiftCodeEntry(
                        code = code,
                        status = if (looksExpired) "NEEDS_REVIEW" else "UNVERIFIED",
                        source = sourceName,
                        lastChecked = checkedDate,
                        note = if (looksExpired) "La page mentionne une expiration à proximité ; confirmation nécessaire."
                            else "Code repéré sur une source communautaire ; à tester dans le jeu."
                    )
                }
            }
        }

        val merged = found.values.sortedWith(
            compareBy<GiftCodeEntry> {
                when (it.status) {
                    "COMMUNITY_LISTED" -> 0
                    "UNVERIFIED", "NEEDS_REVIEW" -> 1
                    "EXPIRED" -> 2
                    else -> 3
                }
            }.thenBy { it.code.lowercase(Locale.ROOT) }
        )
        return if (errors.size == sources.size) {
            previous.copy(sourceErrors = errors)
        } else {
            GiftCodeSnapshot(updatedAt = now, sourceErrors = errors, entries = merged)
        }
    }

    private fun newerDate(existing: String, candidate: String): String {
        if (candidate.isBlank()) return existing
        if (existing.isBlank()) return candidate
        fun millis(value: String): Long? = runCatching {
            val patterns = listOf(
                java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy", Locale.FRENCH),
                java.time.format.DateTimeFormatter.ofPattern("d.M.yyyy", Locale.FRENCH),
                java.time.format.DateTimeFormatter.ofPattern("d-M-yyyy", Locale.FRENCH),
                java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH),
                java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH),
                java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)
            )
            patterns.firstNotNullOfOrNull { pattern ->
                runCatching { java.time.LocalDate.parse(value.trim(), pattern).toEpochDay() }.getOrNull()
            }
        }.getOrNull()
        val oldValue = millis(existing)
        val newValue = millis(candidate)
        return if (oldValue != null && newValue != null && newValue > oldValue) candidate else existing
    }

    /**
     * Reads explicit code elements and conservative plain-list fallbacks.
     * Plain text is accepted only inside a list/paragraph/table cell that
     * explicitly discusses gift/promo codes; ordinary page text is ignored.
     */
    internal fun extractCandidatesFromHtml(html: String): List<Pair<String, String>> {
        val candidates = linkedMapOf<String, Pair<String, String>>()
        val codeElements = Regex("(?is)<code[^>]*>(.*?)</code>")
        codeElements.findAll(html).forEach { match ->
            val code = decodeHtml(stripTags(match.groupValues[1])).trim()
            if (code.matches(Regex("[A-Za-z0-9]{6,20}"))) {
                val start = (match.range.first - 220).coerceAtLeast(0)
                val end = (match.range.last + 420).coerceAtMost(html.length)
                val context = stripTags(html.substring(start, end)).lowercase(Locale.ROOT)
                candidates[code.lowercase(Locale.ROOT)] = code to context
            }
        }

        val blocks = Regex("(?is)<(?:li|p|td)[^>]*>(.*?)</(?:li|p|td)>")
        val token = Regex("(?<![A-Za-z0-9])[A-Za-z0-9]{6,20}(?![A-Za-z0-9])")
        val codeContext = Regex("(?i)\\b(gift\\s*codes?|promo\\s*codes?|coupon\\s*codes?|codes?\\s*(?:cadeaux?|for\\s+lands|of\\s+lands)|lands?\\s+of\\s+jail\\s+codes?)\\b")
        val excluded = setOf("ACTIVE", "EXPIRED", "LATEST", "REWARDS", "GIFTCO", "COUPON")
        blocks.findAll(html).forEach blockLoop@ { block ->
            val plain = decodeHtml(stripTags(block.groupValues[1]))
            if (!codeContext.containsMatchIn(plain)) return@blockLoop
            val context = plain.lowercase(Locale.ROOT)
            token.findAll(plain).forEach tokenLoop@ { foundToken ->
                val code = foundToken.value
                val normalized = code.uppercase(Locale.ROOT)
                // The fallback requires a digit or the game's LOJ prefix to avoid
                // treating ordinary uppercase words as redeemable codes.
                if (normalized in excluded || !(code.any(Char::isDigit) || normalized.startsWith("LOJ"))) return@tokenLoop
                if (!candidates.containsKey(code.lowercase(Locale.ROOT))) candidates[code.lowercase(Locale.ROOT)] = code to context
            }
        }
        return candidates.values.toList()
    }

    private fun fetch(address: String): String {
        val connection = URL(address).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 7_000
        connection.readTimeout = 7_000
        connection.setRequestProperty("User-Agent", "LandsOfJailAssistant/1.0 (gift-code checker)")
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml")
        return try {
            val status = connection.responseCode
            if (status !in 200..299) error("HTTP $status")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun stripTags(value: String): String =
        value.replace(Regex("(?is)<script.*?</script>|<style.*?</style>"), " ")
            .replace(Regex("(?s)<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")

    private fun decodeHtml(value: String): String = value
        .replace("&amp;", "&").replace("&quot;", "\"")
        .replace("&#039;", "'").replace("&lt;", "<").replace("&gt;", ">")
        .replace("&nbsp;", " ")
}
