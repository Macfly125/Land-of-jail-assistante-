package com.landsofjail.assistant.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.landsofjail.assistant.model.AdviceMode
import com.landsofjail.assistant.model.HeroRecord
import com.landsofjail.assistant.model.PlayerRole
import com.landsofjail.assistant.model.ScreenshotAnalysis
import com.landsofjail.assistant.model.TacticalAdviceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID
import java.util.concurrent.TimeUnit

class GeminiAssistantRepository(
    private val backendBaseUrl: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
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
            if (!response.isSuccessful) error("Erreur HTTP ${response.code}: $body")

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
                val body = response.body?.string() ?: error("Réponse vide du serveur.")
                if (!response.isSuccessful) error("Erreur HTTP ${response.code}: $body")
                val wrapper = json.decodeFromString<AdviceWrapper>(body)
                wrapper.advice
            }
        }
    }

    @kotlinx.serialization.Serializable
    private data class AnalyzeWrapper(
        val analysis: ScreenshotAnalysis,
        val analyzedAt: String
    )

    @kotlinx.serialization.Serializable
    private data class AdviceWrapper(
        val advice: TacticalAdviceResult
    )

    @kotlinx.serialization.Serializable
    private data class AdviceRequestPayload(
        val mode: String,
        val playerRole: String,
        val playerRoleLabel: String,
        val heroes: List<HeroRecord>,
        val powerText: String? = null
    )
}
