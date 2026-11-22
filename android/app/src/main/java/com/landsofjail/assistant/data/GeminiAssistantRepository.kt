package com.landsofjail.assistant.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.landsofjail.assistant.model.AdviceMode
import com.landsofjail.assistant.model.AdviceRecommendation
import com.landsofjail.assistant.model.AppLanguage
import com.landsofjail.assistant.model.GroundedWebSource
import com.landsofjail.assistant.model.HeroRecord
import com.landsofjail.assistant.model.PlayerRole
import com.landsofjail.assistant.model.RobotRecord
import com.landsofjail.assistant.model.ScreenshotAnalysis
import com.landsofjail.assistant.model.TacticalAdviceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
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
                error("Analyse OCR locale utilisée.")
            }

            val wrapper = json.decodeFromString<AnalyzeWrapper>(body)
            wrapper.analysis.copy(
                id = UUID.randomUUID().toString(),
                analyzedAtIso = wrapper.analyzedAt,
                imageUriString = imageLabel
            )
        }
    }

    suspend fun fetchHeroCatalog(language: AppLanguage): Result<List<HeroCatalogEntry>> = withContext(Dispatchers.IO) {
        runCatching {
            val apiLang = if (language == AppLanguage.ZH) "zh" else language.code
            val request = Request.Builder()
                .url("$backendBaseUrl/api/heroes/catalog?lang=$apiLang")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: error("Catalogue des héros vide.")
                if (!response.isSuccessful) error("Catalogue héros indisponible (HTTP ${response.code}).")
                json.decodeFromString<HeroCatalogResponse>(body).heroes
            }
        }
    }

    suspend fun fetchHeroDetails(name: String, language: AppLanguage): Result<HeroWikiDetails?> = withContext(Dispatchers.IO) {
        runCatching {
            val apiLang = if (language == AppLanguage.ZH) "zh" else language.code
            val payload = HeroEnrichRequest(names = listOf(name), language = apiLang)
            val request = Request.Builder()
                .url("$backendBaseUrl/api/heroes/enrich")
                .post(json.encodeToString(payload).toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: error("Fiche héros vide.")
                if (!response.isSuccessful) error("Fiche héros indisponible (HTTP ${response.code}).")
                json.decodeFromString<HeroDetailsResponse>(body).details.firstOrNull()
            }
        }
    }

    suspend fun fetchTacticalAdvice(
        mode: AdviceMode,
        language: AppLanguage,
        playerRole: PlayerRole,
        heroes: List<HeroRecord>,
        robots: List<RobotRecord>,
        powerText: String?,
        eventPosition: String = "joiner",
        gorillaName: String? = null
    ): Result<TacticalAdviceResult> = withContext(Dispatchers.IO) {
        runCatching {
            val apiLang = when (language) {
                AppLanguage.ZH -> "zh"
                else -> language.code
            }
            val backendAttempt = runCatching {
                val payload = AdviceRequestPayload(
                    mode = mode.apiId,
                    language = apiLang,
                    playerRole = playerRole.name.lowercase(),
                    playerRoleLabel = playerRole.labelFr,
                    heroes = heroes,
                    robots = robots,
                    powerText = powerText,
                    eventPosition = eventPosition,
                    gorillaName = gorillaName
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
                        error("HTTP ${response.code}")
                    }
                }
            }

            backendAttempt.getOrElse {
                fetchDirectInternetGuides(
                    mode = mode,
                    language = language,
                    playerRole = playerRole,
                    heroes = heroes,
                    gorillaName = gorillaName
                )
            }
        }
    }

    private data class DirectPost(
        val title: String,
        val excerpt: String,
        val url: String,
        val dateStr: String?,
        val imageUrl: String?
    )

    private fun fetchDirectInternetGuides(
        mode: AdviceMode,
        language: AppLanguage,
        playerRole: PlayerRole,
        heroes: List<HeroRecord>,
        gorillaName: String? = null
    ): TacticalAdviceResult {
        val collected = mutableListOf<DirectPost>()
        val seenLinks = mutableSetOf<String>()
        val todayDate = Instant.now().toString().take(10)

        // 1. Sites natifs dans la langue choisie sur lojwiki.com (FR, TR, RU, ZH, DE, EN)
        for (slug in mode.wikiSlugs) {
            val wikiUrl = "https://lojwiki.com${language.wikiPrefix}$slug"
            runCatching {
                val req = Request.Builder()
                    .url(wikiUrl)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/126.0")
                    .build()
                client.newCall(req).execute().use { resp ->
                    val html = resp.body?.string().orEmpty()
                    if (resp.isSuccessful && html.isNotBlank()) {
                        val title = Regex("<title>([^<]+)</title>", RegexOption.IGNORE_CASE)
                            .find(html)?.groupValues?.get(1)
                            ?.replace(Regex("\\s*\\|\\s*Lands of Jail.*$", RegexOption.IGNORE_CASE), "")
                            ?.trim()
                            .orEmpty()
                        val desc = Regex(
                            """<meta\s+name=["']description["']\s+content=["']([^"']+)["']""",
                            RegexOption.IGNORE_CASE
                        ).find(html)?.groupValues?.get(1).orEmpty()
                        val imgs = Regex("""https://img\.lojwiki\.com/[^\s"'<>]+\.(?:webp|png|jpg|jpeg)""", RegexOption.IGNORE_CASE)
                            .findAll(html).map { it.value }.toList()
                        val photo = imgs.firstOrNull { it.contains("/banners/") } ?: imgs.firstOrNull()

                        if (title.isNotBlank() && seenLinks.add(wikiUrl)) {
                            collected.add(
                                DirectPost(
                                    title = cleanHtml(title),
                                    excerpt = cleanHtml(desc),
                                    url = wikiUrl,
                                    dateStr = todayDate,
                                    imageUrl = photo
                                )
                            )
                        }
                    }
                }
            }
        }

        // 2. Guides détaillés + photos depuis lojxlab.com
        val encoded = URLEncoder.encode(mode.searchKeyword, "UTF-8")
        val lojxlabUrls = buildList {
            add("https://lojxlab.com/wp-json/wp/v2/posts?per_page=8&_embed=wp:featuredmedia&search=$encoded&_fields=date,modified,link,title,excerpt,content,_links,_embedded")
            if (heroes.isNotEmpty()) {
                val heroEnc = URLEncoder.encode(heroes.first().nom, "UTF-8")
                add("https://lojxlab.com/wp-json/wp/v2/posts?per_page=4&_embed=wp:featuredmedia&search=$heroEnc&_fields=date,modified,link,title,excerpt,content,_links,_embedded")
            }
        }

        for (u in lojxlabUrls) {
            runCatching {
                val req = Request.Builder()
                    .url(u)
                    .header("Accept", "application/json")
                    .header("User-Agent", "LandsOfJailAssistantAndroid/1.5")
                    .build()
                client.newCall(req).execute().use { resp ->
                    val raw = resp.body?.string() ?: ""
                    if (resp.isSuccessful && raw.trimStart().startsWith("[")) {
                        val arr = json.parseToJsonElement(raw).jsonArray
                        val parsed = arr.mapNotNull { el ->
                            val obj = el as? JsonObject ?: return@mapNotNull null
                            val link = obj["link"]?.jsonPrimitive?.contentOrNull.orEmpty()
                            if (link.isBlank()) return@mapNotNull null
                            val rawDate = (obj["modified"]?.jsonPrimitive?.contentOrNull
                                ?: obj["date"]?.jsonPrimitive?.contentOrNull.orEmpty())
                                .take(10)
                                .ifBlank { null }
                            val rawTitle = cleanHtml(
                                obj["title"]?.jsonObject?.get("rendered")?.jsonPrimitive?.contentOrNull.orEmpty()
                            )
                            val rawExcerpt = cleanHtml(
                                obj["excerpt"]?.jsonObject?.get("rendered")?.jsonPrimitive?.contentOrNull.orEmpty()
                            )
                            val contentHtml =
                                obj["content"]?.jsonObject?.get("rendered")?.jsonPrimitive?.contentOrNull.orEmpty()
                            val photo = extractPostImageUrl(obj, contentHtml)

                            DirectPost(
                                title = translateSafely(rawTitle, language),
                                excerpt = translateSafely(rawExcerpt, language),
                                url = link,
                                dateStr = rawDate,
                                imageUrl = photo
                            )
                        }.sortedByDescending { p ->
                            when {
                                language == AppLanguage.DE && p.url.contains("/de/") -> 4
                                p.url.contains("/blog/") -> 2
                                else -> 1
                            }
                        }

                        for (p in parsed) {
                            if (seenLinks.add(p.url)) {
                                collected.add(p)
                            }
                        }
                    }
                }
            }
        }

        if (collected.isEmpty()) {
            error("Aucun guide Internet accessible. Vérifiez votre connexion.")
        }

        val heroNames = heroes.map { it.nom }
        val modeLabel = mode.titleFor(language)
        val nowIso = Instant.now().toString()

        val recommendations = collected.take(4).map { post ->
            AdviceRecommendation(
                titre = post.title.ifBlank { modeLabel },
                detail = post.excerpt.ifBlank { mode.briefFor(language) },
                herosCibles = heroNames.take(3),
                incertain = false,
                imageUrl = post.imageUrl
            )
        }

        val sources = collected.take(8).map { post ->
            GroundedWebSource(
                title = post.title.ifBlank { post.url },
                url = post.url,
                detectedDate = post.dateStr,
                imageUrl = post.imageUrl
            )
        }

        val tacticalBase = buildString {
            append(mode.briefFor(language))
            if (mode == AdviceMode.GORILLA_LEGION) {
                if (!gorillaName.isNullOrBlank()) {
                    append(" ")
                    append(when (language) {
                        AppLanguage.FR -> "Gorille sélectionné : $gorillaName. Vérifiez le bonus affiché en jeu ; les calendriers communautaires divergent."
                        AppLanguage.EN -> "Selected Gorilla: $gorillaName. Verify the in-game bonus; community schedules conflict."
                        AppLanguage.DE -> "Ausgewählter Gorilla: $gorillaName. Prüfe den Bonus im Spiel; Community-Zeitpläne widersprechen sich."
                        AppLanguage.TR -> "Seçilen Goril: $gorillaName. Topluluk takvimleri farklı olduğundan oyun içi bonusu doğrulayın."
                        AppLanguage.RU -> "Выбранная горилла: $gorillaName. Проверьте бонус в игре: расписания сообщества расходятся."
                        AppLanguage.ZH -> "已选择大猩猩：$gorillaName。社区日程存在差异，请以游戏内显示的加成为准。"
                    })
                } else {
                    append(" ")
                    append(when (language) {
                        AppLanguage.FR -> "Gorille non confirmé : ne déduisez pas le boss ou son bonus du seul jour de la semaine."
                        AppLanguage.EN -> "Gorilla not confirmed: do not infer the boss or bonus from the weekday alone."
                        AppLanguage.DE -> "Gorilla nicht bestätigt: Leite Boss oder Bonus nicht allein aus dem Wochentag ab."
                        AppLanguage.TR -> "Goril doğrulanmadı: Boss veya bonusu yalnızca haftanın gününe göre tahmin etmeyin."
                        AppLanguage.RU -> "Горилла не подтверждена: не определяйте босса или бонус только по дню недели."
                        AppLanguage.ZH -> "尚未确认大猩猩：不要仅凭星期推断首领或加成。"
                    })
                }
            }
        }
        val synthText = when (language) {
            AppLanguage.FR -> "$modeLabel (${playerRole.labelFr}) — $tacticalBase"
            AppLanguage.TR -> "$modeLabel (${playerRole.labelFr}) — $tacticalBase"
            AppLanguage.RU -> "$modeLabel (${playerRole.labelFr}) — $tacticalBase"
            AppLanguage.ZH -> "$modeLabel (${playerRole.labelFr}) — $tacticalBase"
            AppLanguage.DE -> "$modeLabel (${playerRole.labelFr}) — $tacticalBase"
            AppLanguage.EN -> "$modeLabel (${playerRole.labelFr}) — $tacticalBase"
        }

        val compText = if (heroNames.isNotEmpty()) {
            when (language) {
                AppLanguage.FR -> "Héros détectés : ${heroNames.joinToString(", ")}."
                AppLanguage.TR -> "Tespit edilen kahramanlar: ${heroNames.joinToString(", ")}."
                AppLanguage.RU -> "Обнаруженные герои: ${heroNames.joinToString(", ")}."
                AppLanguage.ZH -> "已检测英雄：${heroNames.joinToString(", ")}。"
                AppLanguage.DE -> "Erkannte Helden: ${heroNames.joinToString(", ")}."
                AppLanguage.EN -> "Detected heroes: ${heroNames.joinToString(", ")}."
            }
        } else {
            noHeroesWarning(language)
        }

        return TacticalAdviceResult(
            mode = mode.apiId,
            language = language.code,
            generatedAt = nowIso,
            generatedAtIso = nowIso,
            synthese = synthText,
            compositionRecommandee = compText,
            recommandations = recommendations,
            informationsIncertaines = if (heroNames.isEmpty()) listOf(noHeroesWarning(language)) else emptyList(),
            sources = sources,
            geminiWarning = null
        )
    }

    private fun extractPostImageUrl(postObj: JsonObject, contentHtml: String): String? {
        runCatching {
            val embedded = postObj["_embedded"] as? JsonObject
            val mediaArr = embedded?.get("wp:featuredmedia") as? JsonArray
            val firstMedia = mediaArr?.firstOrNull() as? JsonObject
            if (firstMedia != null) {
                val sizes = firstMedia["media_details"]
                    ?.let { it as? JsonObject }
                    ?.get("sizes")
                    ?.let { it as? JsonObject }

                val candidates = listOf("lojxlab-card", "medium_large", "large", "medium")
                for (key in candidates) {
                    val sizeUrl = (sizes?.get(key) as? JsonObject)
                        ?.get("source_url")
                        ?.let { it as? kotlinx.serialization.json.JsonPrimitive }
                        ?.contentOrNull
                    if (!sizeUrl.isNullOrBlank() && sizeUrl.startsWith("http")) return sizeUrl
                }

                val fallbackUrl = (firstMedia["source_url"] as? kotlinx.serialization.json.JsonPrimitive)
                    ?.contentOrNull
                if (!fallbackUrl.isNullOrBlank() && fallbackUrl.startsWith("http")) return fallbackUrl
            }
        }

        val imgMatch = Regex("""https?://[^\s"'<>]+\.(?:webp|png|jpg|jpeg)""", RegexOption.IGNORE_CASE)
            .find(contentHtml)
        return imgMatch?.value
    }

    private fun translateSafely(text: String, language: AppLanguage): String {
        if (text.isBlank() || language == AppLanguage.EN) return text
        return runCatching {
            val encoded = URLEncoder.encode(text.take(450), "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=${language.code}&dt=t&q=$encoded"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(req).execute().use { resp ->
                val raw = resp.body?.string() ?: return text
                if (!resp.isSuccessful || !raw.trimStart().startsWith("[")) return text
                val outer = json.parseToJsonElement(raw).jsonArray
                val segments = outer.firstOrNull()?.jsonArray ?: return text
                val translated = buildString {
                    for (seg in segments) {
                        val part = (seg as? JsonArray)?.firstOrNull()?.jsonPrimitive?.contentOrNull
                        if (part != null) append(part)
                    }
                }.trim()
                translated.ifBlank { text }
            }
        }.getOrDefault(text)
    }

    private fun noHeroesWarning(lang: AppLanguage): String = when (lang) {
        AppLanguage.FR -> "Aucun héros scanné : appuyez sur « Analyser l’écran » sur votre page Héros."
        AppLanguage.TR -> "Henüz kahraman taranmadı: Kahramanlar ekranında « Ekranı Tara » butonuna basın."
        AppLanguage.RU -> "Герои ещё не отсканированы: нажмите « Сканировать » на экране героев."
        AppLanguage.ZH -> "暂未扫描英雄：请在英雄界面点击「扫描屏幕」。"
        AppLanguage.DE -> "Noch keine Helden gescannt: Tippe auf der Heldenseite auf « Scannen »."
        AppLanguage.EN -> "No heroes scanned yet: tap « Scan screen » on your Heroes screen."
    }

    private fun cleanHtml(input: String): String =
        input
            .replace(Regex("<[^>]*>"), " ")
            .replace("&#8217;", "'")
            .replace("&#8211;", "–")
            .replace("&#038;", "&")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace(Regex("\\s+"), " ")
            .trim()

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
    private data class HeroEnrichRequest(
        val names: List<String>,
        val language: String
    )

    @Serializable
    private data class AdviceRequestPayload(
        val mode: String,
        val language: String,
        val playerRole: String,
        val playerRoleLabel: String,
        val heroes: List<HeroRecord>,
        val robots: List<RobotRecord> = emptyList(),
        val powerText: String? = null,
        val eventPosition: String = "joiner",
        val gorillaName: String? = null
    )
}
