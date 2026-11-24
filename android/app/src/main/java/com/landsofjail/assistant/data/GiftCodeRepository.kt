package com.landsofjail.assistant.data

import kotlinx.serialization.Serializable
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.Locale

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
    const val GUIDE_URL = "https://guideloj.com/en/codes"
    const val DISCORD_URL = "https://discord.gg/YGJqgcGpn"
    const val REDEEM_URL = "https://www.gamermall.net/"

    private val officialCodes = listOf(
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
        "N4HDYuJxkQKur5" to "30/11/2025"
    )
    private val unverifiedCodes = listOf("RuStore777", "HeartofWar777", "R8K3L7T5", "C5F2R9A1")

    fun initialSnapshot(): GiftCodeSnapshot {
        val now = Instant.now().toString()
        val entries = buildList {
            officialCodes.forEach {
                add(GiftCodeEntry(it, "OFFICIAL_LISTED", "LOJ XLab · Discord officiel #gift-codes",
                    "21/09/2026", "13/10/2026", note = "Publié par l’équipe du jeu selon LOJ XLab ; date d’expiration non annoncée."))
            }
            expiredCodes.forEach { (code, expiry) ->
                add(GiftCodeEntry(code, "EXPIRED", "LOJ XLab · historique", expiresAt = expiry,
                    note = "Code classé expiré par la source communautaire."))
            }
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
        return GiftCodeSnapshot(now, entries = entries)
    }

    suspend fun refresh(previous: GiftCodeSnapshot): GiftCodeSnapshot {
        val now = Instant.now().toString()
        val errors = mutableListOf<String>()
        val found = linkedMapOf<String, GiftCodeEntry>()
        previous.entries.forEach { found[it.code.lowercase(Locale.ROOT)] = it }

        val sources = listOf(
            "LOJ XLab" to XLAB_URL,
            "GuideLOJ" to GUIDE_URL,
            "Pocket Gamer" to "https://www.pocketgamer.com/lands-of-jail/codes/",
            "MrGuider" to "https://www.mrguider.org/codes/lands-of-jail-codes/"
        )
        for ((sourceName, url) in sources) {
            try {
                val html = fetch(url)
                val checkedDate = Regex("(?i)(?:checked|updated|last checked)\\s*[:\\-]?\\s*(?:on\\s*)?([0-9]{1,2}\\s+[A-Za-z]+\\s+20[0-9]{2}|[A-Za-z]+\\s+[0-9]{1,2},?\\s+20[0-9]{2})")
                    .find(stripTags(html))?.groupValues?.getOrNull(1).orEmpty()
                val codeMatches = Regex("(?is)<code[^>]*>(.*?)</code>").findAll(html)
                for (match in codeMatches) {
                    val code = decodeHtml(stripTags(match.groupValues[1])).trim()
                    if (!code.matches(Regex("[A-Za-z0-9]{6,20}"))) continue
                    val key = code.lowercase(Locale.ROOT)
                    val existing = found[key]
                    if (existing != null) {
                        val mergedSource = (existing.source.split(" · ") + sourceName).distinct().joinToString(" · ")
                        found[key] = existing.copy(source = mergedSource, lastChecked = checkedDate.ifBlank { existing.lastChecked })
                    } else {
                        val contextStart = (match.range.first - 220).coerceAtLeast(0)
                        val contextEnd = (match.range.last + 420).coerceAtMost(html.length)
                        val nearby = stripTags(html.substring(contextStart, contextEnd)).lowercase(Locale.ROOT)
                        val looksExpired = nearby.contains("ended codes") || nearby.contains("expired")
                        found[key] = GiftCodeEntry(
                            code = code,
                            status = if (looksExpired) "NEEDS_REVIEW" else "UNVERIFIED",
                            source = sourceName,
                            firstSeen = checkedDate,
                            lastChecked = checkedDate,
                            note = if (looksExpired) "La page mentionne une expiration à proximité ; confirmation nécessaire."
                                else "Code repéré sur une source communautaire ; à tester dans le jeu."
                        )
                    }
                }
            } catch (error: Exception) {
                errors += "$sourceName : ${error.message ?: "source inaccessible"}"
            }
        }

        val merged = found.values.sortedWith(
            compareBy<GiftCodeEntry> {
                when (it.status) {
                    "OFFICIAL_LISTED" -> 0
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
