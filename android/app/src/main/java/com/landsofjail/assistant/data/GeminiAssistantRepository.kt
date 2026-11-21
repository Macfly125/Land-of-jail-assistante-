package com.landsofjail.assistant.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.landsofjail.assistant.model.AdviceMode
import com.landsofjail.assistant.model.AdviceRecommendation
import com.landsofjail.assistant.model.GroundedWebSource
import com.landsofjail.assistant.model.HeroRecord
import com.landsofjail.assistant.model.PlayerRole
import com.landsofjail.assistant.model.ScreenshotAnalysis
import com.landsofjail.assistant.model.TacticalAdviceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeUnit

class GeminiAssistantRepository(
    private val backendBaseUrl: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun analyzeSharedScreenshot(
        context: Context,
        imageUri: Uri,
        playerRole: PlayerRole,
        note: String?
    ): Result<ScreenshotAnalysis> = withContext(Dispatchers.IO) {
        runCatching {
            val mimeType = context.contentResolver.getType(imageUri) ?: "image/png"
            val bytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                ?: error("Impossible de lire la capture partagée.")
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            analyzeBase64Screenshot(base64, mimeType, imageUri.toString(), playerRole, note)
        }
    }

    suspend fun analyzeBase64Screenshot(
        imageBase64: String,
        mimeType: String,
        imageLabel: String,
        playerRole: PlayerRole,
        note: String?
    ): ScreenshotAnalysis = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("imageBase64", imageBase64)
            put("mimeType", mimeType)
            put("playerRole", playerRole.labelFr)
            if (!note.isNullOrBlank()) put("note", note)
        }

        val request = Request.Builder()
            .url("$backendBaseUrl/api/analyze-screenshot")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: error("Réponse vide du serveur.")
            if (!response.isSuccessful || !body.trimStart().startsWith("{")) {
                error("Serveur indisponible (${response.code}) ; analyse OCR locale utilisée.")
            }

            val wrapper = json.decodeFromString<AnalyzeWrapper>(body)
            wrapper.analysis.copy(
                id = UUID.randomUUID().toString(),
                analyzedAtIso = wrapper.analyzedAt,
                imageUriString = imageLabel
            )
        }
    }

    suspend fun fetchTacticalAdvice(
        mode: AdviceMode,
        playerRole: PlayerRole,
        heroes: List<HeroRecord>,
        powerText: String?
    ): Result<TacticalAdviceResult> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Tenter le serveur backend (/api/advice)
            val backendAttempt = runCatching {
                val payload = AdviceRequestPayload(
                    mode = if (mode == AdviceMode.TRIAL_CAGE) "trial_cage" else "crystal_clash",
                    playerRole = playerRole.name.lowercase(),
                    playerRoleLabel = playerRole.labelFr,
                    heroes = heroes,
                    powerText = powerText
                )
                val request = Request.Builder()
                    .url("$backendBaseUrl/api/advice")
                    .post(json.encodeToString(payload).toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string() ?: ""
                    if (response.isSuccessful && body.trimStart().startsWith("{")) {
                        json.decodeFromString<AdviceWrapper>(body).advice
                    } else {
                        error("Backend HTTP ${response.code}")
                    }
                }
            }

            backendAttempt.getOrElse { backendErr ->
                // 2. Repli en direct depuis le téléphone sur l'API publique lojxlab.com
                fetchDirectInternetGuides(
                    mode = mode,
                    playerRole = playerRole,
                    heroes = heroes,
                    backendReason = backendErr.message ?: "Serveur distant inaccessible"
                )
            }
        }
    }

    private fun fetchDirectInternetGuides(
        mode: AdviceMode,
        playerRole: PlayerRole,
        heroes: List<HeroRecord>,
        backendReason: String
    ): TacticalAdviceResult {
        val searchTerm = if (mode == AdviceMode.TRIAL_CAGE) "cage" else "crystal clash"
        val encoded = URLEncoder.encode(searchTerm, "UTF-8")
        val urls = buildList {
            add("https://lojxlab.com/wp-json/wp/v2/posts?per_page=8&search=$encoded&_fields=date,modified,link,title,excerpt")
            if (heroes.isNotEmpty()) {
                val heroEnc = URLEncoder.encode(heroes.first().nom, "UTF-8")
                add("https://lojxlab.com/wp-json/wp/v2/posts?per_page=4&search=$heroEnc&_fields=date,modified,link,title,excerpt")
            }
        }

        val collectedPosts = mutableListOf<WpPost>()
        val seenLinks = mutableSetOf<String>()

        for (u in urls) {
            runCatching {
                val req = Request.Builder()
                    .url(u)
                    .header("Accept", "application/json")
                    .header("User-Agent", "LandsOfJailAssistantAndroid/1.2")
                    .build()
                client.newCall(req).execute().use { resp ->
                    val raw = resp.body?.string() ?: ""
                    if (resp.isSuccessful && raw.trimStart().startsWith("[")) {
                        val posts = json.decodeFromString<List<WpPost>>(raw)
                        val sorted = posts.sortedByDescending { p ->
                            when {
                                p.link.contains("/fr/") -> 2
                                p.link.contains("/blog/") -> 1
                                else -> 0
                            }
                        }
                        for (p in sorted) {
                            if (p.link.isNotBlank() && seenLinks.add(p.link)) {
                                collectedPosts.add(p)
                            }
                        }
                    }
                }
            }
        }

        if (collectedPosts.isEmpty()) {
            error("Impossible de joindre le serveur ($backendReason) ni lojxlab.com. Vérifiez votre connexion Internet.")
        }

        val heroNames = heroes.map { it.nom }
        val modeLabel = mode.labelFr
        val nowIso = Instant.now().toString()

        val recommendations = collectedPosts.take(4).map { post ->
            AdviceRecommendation(
                titre = cleanHtml(post.title.rendered).ifBlank { "Guide $modeLabel" },
                detail = cleanHtml(post.excerpt.rendered).ifBlank { "Ouvrez le lien source ci-dessous pour lire le guide complet." },
                herosCibles = heroNames.take(3),
                incertain = heroNames.isEmpty(),
                raisonIncertitude = if (heroNames.isEmpty()) {
                    "Aucun héros scanné : lancez d’abord « Analyser l’écran » sur votre page Héros."
                } else null
            )
        }

        val sources = collectedPosts.take(6).map { post ->
            val dateStr = (post.modified.ifBlank { post.date }).take(10).ifBlank { null }
            GroundedWebSource(
                title = cleanHtml(post.title.rendered).ifBlank { post.link },
                url = post.link,
                detectedDate = dateStr
            )
        }

        val tacticalBase = if (mode == AdviceMode.TRIAL_CAGE) {
            "Priorisez vos Shieldbearers en première ligne pour encaisser et vos Shooters DPS derrière. Séparez les héros Leader de ralliement des Rejoigneurs."
        } else {
            "Crystal Clash se gagne aux Points de Cristal d'Alliance : sécurisez Crystal Prisons, Ironholds et Energy Hubs avant de pousser le Central Ironprison, et utilisez les pauses soin."
        }

        return TacticalAdviceResult(
            mode = if (mode == AdviceMode.TRIAL_CAGE) "trial_cage" else "crystal_clash",
            generatedAt = nowIso,
            generatedAtIso = nowIso,
            synthese = "Guides Internet récupérés en direct pour $modeLabel (rôle : ${playerRole.labelFr}). $tacticalBase",
            compositionRecommandee = if (heroNames.isNotEmpty()) {
                "Héros détectés : ${heroNames.joinToString(", ")}. Adaptez l'ordre selon votre rôle (${playerRole.labelFr})."
            } else {
                "Aucun héros détecté pour le moment : capturez votre écran Héros pour adapter la composition."
            },
            recommandations = recommendations,
            informationsIncertaines = buildList {
                add("Sources récupérées directement depuis lojxlab.com depuis votre téléphone ($backendReason).")
                if (heroNames.isEmpty()) {
                    add("Vos héros n'ont pas encore été scannés par OCR : conseils généraux affichés.")
                }
            },
            sources = sources,
            geminiWarning = "Accès direct Internet utilisé ($backendReason). Les articles et dates proviennent en direct de lojxlab.com."
        )
    }

    private fun cleanHtml(input: String): String =
        input
            .replace(Regex("<[^>]*>"), " ")
            .replace("&#8217;", "'")
            .replace("&#8211;", "–")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace(Regex("\\s+"), " ")
            .trim()

    @Serializable
    private data class WpRendered(val rendered: String = "")

    @Serializable
    private data class WpPost(
        val date: String = "",
        val modified: String = "",
        val link: String = "",
        val title: WpRendered = WpRendered(),
        val excerpt: WpRendered = WpRendered()
    )

    @Serializable
    private data class AnalyzeWrapper(
        val analysis: ScreenshotAnalysis,
        val analyzedAt: String
    )

    @Serializable
    private data class AdviceWrapper(
        val advice: TacticalAdviceResult
    )

    @Serializable
    private data class AdviceRequestPayload(
        val mode: String,
        val playerRole: String,
        val playerRoleLabel: String,
        val heroes: List<HeroRecord>,
        val powerText: String? = null
    )
}
