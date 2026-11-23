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

    suspend fun fetchTacticalAdvice(
        mode: AdviceMode,
        language: AppLanguage,
        playerRole: PlayerRole,
        heroes: List<HeroRecord>,
        powerText: String?,
        robots: List<RobotRecord> = emptyList()
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
                    powerText = powerText,
                    robots = robots
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
                    robots = robots
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
        robots: List<RobotRecord>
    ): TacticalAdviceResult {
        val collected = mutableListOf<DirectPost>()
        val seenLinks = mutableSetOf<String>()

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
                                    dateStr = null,
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

        // 3. Sources communautaires ciblées par événement (guides, vidéos référencées, héros et robots).
        // Les pages sont relues en direct : aucune date ou compétence n'est inventée hors du contenu source.
        val curatedUrls = buildList {
            add("https://lojxlab.com/heropedia/")
            add("https://lojxlab.com/guides/blog/")
            when (mode) {
                AdviceMode.TRIAL_CAGE -> {
                    add("https://guideloj.com/fr/events/alliance/trial-cage-rally")
                    add("https://lojxlab.com/topic/cage/")
                    add("https://lojxlab.com/library/lands-of-jail-robot-upgrade-cost/")
                    add("https://loj.dookee.ru/en/knowledge/cage/")
                    add("https://lojstrats.com/")
                }
                AdviceMode.CRYSTAL_CLASH -> {
                    add("https://guideloj.com/fr/events/alliance/crystal-clash")
                    add("https://lojxlab.com/guides/blog/")
                }
                AdviceMode.SANDSEA_CONQUEST ->
                    add("https://guideloj.com/fr/events/alliance/sandsea-conquest")
                AdviceMode.SVS ->
                    add("https://lojxlab.com/topic/svs/")
                AdviceMode.TURMOIL_SURGE ->
                    add("https://guideloj.com/fr/events/alliance/turmoil-surge")
                AdviceMode.GOLD_HUNTER ->
                    add("https://lojxlab.com/topic/gold-hunter/")
            }
        }.distinct()

        for (sourceUrl in curatedUrls) {
            if (sourceUrl in seenLinks) continue
            runCatching {
                val request = Request.Builder()
                    .url(sourceUrl)
                    .header("User-Agent", "LandsOfJailAssistantAndroid/1.6")
                    .build()
                client.newCall(request).execute().use { response ->
                    val html = response.body?.string().orEmpty()
                    if (response.isSuccessful && html.isNotBlank()) {
                        val title = (
                            Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                                .find(html)?.groupValues?.get(1)
                                ?: Regex("<h1[^>]*>(.*?)</h1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                                    .find(html)?.groupValues?.get(1)
                            ).orEmpty().let(::cleanHtml)
                        val metaDescription = Regex(
                            """<meta[^>]+name=["']description["'][^>]+content=["']([^"']+)["']""",
                            RegexOption.IGNORE_CASE
                        ).find(html)?.groupValues?.get(1).orEmpty()
                        val firstParagraph = Regex(
                            "<p[^>]*>(.*?)</p>",
                            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
                        ).find(html)?.groupValues?.get(1).orEmpty()
                        val excerpt = cleanHtml(metaDescription).ifBlank {
                            cleanHtml(firstParagraph).take(650)
                        }
                        if (title.isNotBlank() && seenLinks.add(sourceUrl)) {
                            collected.add(
                                DirectPost(
                                    title = translateSafely(title, language),
                                    excerpt = translateSafely(excerpt, language),
                                    url = sourceUrl,
                                    dateStr = null,
                                    imageUrl = null
                                )
                            )
                        }
                    }
                }
            }
        }

        if (collected.isEmpty()) {
            error("Aucun guide Internet accessible. Vérifiez votre connexion.")
        }

        val modeLabel = mode.titleFor(language)
        val nowIso = Instant.now().toString()

        val recommendations = collected.take(4).map { post ->
            AdviceRecommendation(
                titre = post.title.ifBlank { modeLabel },
                detail = post.excerpt.ifBlank { mode.briefFor(language) },
                // Aucune association héros-guide n'est prouvée par le simple titre/extrait d'une page.
                herosCibles = emptyList(),
                incertain = true,
                raisonIncertitude = "Guide externe trouvé, mais compatibilité avec vos héros non vérifiée.",
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

        val heroNames = heroes.map { it.nom.trim() }.filter { it.isNotBlank() }.distinct()
        val tacticalBase = mode.briefFor(language)
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
            informationsIncertaines = buildList {
                add(
                    when (language) {
                        AppLanguage.FR -> "MODE DE SECOURS : conseils génériques issus de guides externes. Leur pertinence pour votre événement, votre serveur et vos héros n'est pas vérifiée. Ne les prenez pas comme une composition optimale garantie."
                        AppLanguage.TR -> "YEDEK MOD: Harici rehberlerden alınan genel tavsiyeler. Etkinliğiniz, sunucunuz ve kahramanlarınız için uygunluğu doğrulanmadı; garantili en iyi dizilim olarak kabul etmeyin."
                        AppLanguage.RU -> "РЕЗЕРВНЫЙ РЕЖИМ: общие советы из внешних руководств. Их соответствие вашему событию, серверу и героям не проверено. Не считайте их гарантированно оптимальным составом."
                        AppLanguage.ZH -> "备用模式：建议来自外部攻略的通用信息，尚未验证是否适用于当前活动、服务器和英雄。请勿将其视为保证最优阵容。"
                        AppLanguage.DE -> "ERSATZMODUS: Allgemeine Hinweise aus externen Guides. Die Eignung für Ihr Event, Ihren Server und Ihre Helden wurde nicht geprüft. Nicht als garantiert optimale Aufstellung verstehen."
                        AppLanguage.EN -> "FALLBACK MODE: Generic advice from external guides. Compatibility with your event, server and heroes has not been verified. Do not treat this as a guaranteed optimal lineup."
                    }
                )
                if (heroNames.isEmpty()) add(noHeroesWarning(language))
                if (robots.isNotEmpty()) add(
                    when (language) {
                        AppLanguage.FR -> "Mode de secours : les robots du profil sont enregistrés, mais leurs bonus ne peuvent pas être classés de façon fiable sans analyse des règles de l’événement. Aucune recommandation robot personnalisée n’est inventée."
                        AppLanguage.TR -> "Yedek mod: profil robotları kaydedildi, ancak etkinlik kuralları analiz edilmeden güvenilir biçimde sıralanamaz."
                        AppLanguage.RU -> "Резервный режим: роботы профиля сохранены, но без анализа правил события их нельзя надежно ранжировать."
                        AppLanguage.ZH -> "备用模式：已保存档案中的机器人，但未分析活动规则前无法可靠排序。"
                        AppLanguage.DE -> "Ersatzmodus: Profil-Roboter sind gespeichert, können ohne Analyse der Eventregeln aber nicht zuverlässig bewertet werden."
                        AppLanguage.EN -> "Fallback mode: profile robots are saved, but cannot be reliably ranked without analyzing the event rules."
                    }
                )
            },
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
    private data class AdviceRequestPayload(
        val mode: String,
        val language: String,
        val playerRole: String,
        val playerRoleLabel: String,
        val heroes: List<HeroRecord>,
        val powerText: String? = null,
        val robots: List<RobotRecord> = emptyList()
    )
}
