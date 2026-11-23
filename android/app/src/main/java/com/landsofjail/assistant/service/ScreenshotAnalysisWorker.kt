package com.landsofjail.assistant.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.landsofjail.assistant.MainActivity
import com.landsofjail.assistant.data.GeminiAssistantRepository
import com.landsofjail.assistant.model.PlayerRole
import com.landsofjail.assistant.model.ScreenshotAnalysis
import com.landsofjail.assistant.ocr.LandsOfJailOcrParser
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

class ScreenshotAnalysisWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_URIS = "image_uris"
        const val KEY_PLAYER_ROLE = "player_role"
        private const val PREFS = "lands_of_jail_account_v1"
        private const val KEY_PENDING = "pending_background_analyses"
        private const val CHANNEL_ID = "loj_analysis_finished"
        private val lock = Any()
        private val json = Json { ignoreUnknownKeys = true }
    }

    override suspend fun doWork(): Result {
        val uris = inputData.getStringArray(KEY_URIS)?.toList().orEmpty()
        if (uris.isEmpty()) return Result.failure()
        val role = PlayerRole.entries.firstOrNull {
            it.name == inputData.getString(KEY_PLAYER_ROLE)
        } ?: PlayerRole.POLYVALENT_F2P
        val repository = GeminiAssistantRepository(
            backendBaseUrl = "https://land-of-jail-assistant-api.onrender.com"
        )
        val results = mutableListOf<ScreenshotAnalysis>()
        var failed = 0

        for ((index, uriText) in uris.withIndex()) {
            try {
                val uri = Uri.parse(uriText)
                val bitmap = applicationContext.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                } ?: error("Image illisible")
                val ocrText = LandsOfJailOcrParser.recognizeBitmap(bitmap)
                val local = LandsOfJailOcrParser.parseLocalOcrText(ocrText)
                val remote = repository.analyzeSharedScreenshot(
                    applicationContext,
                    uri,
                    role,
                    "Contrôle héros obligatoire en arrière-plan (${index + 1}/${uris.size}) : inspecte chaque portrait et identifie chaque héros si un écran héros est présent. OCR local : ${ocrText.take(350)}"
                )
                val result = remote.fold(
                    onSuccess = { vision ->
                        val mergedHeroes = (vision.heros + local.heroes)
                            .distinctBy { it.nom.trim().lowercase() }
                        val mergedResources = (vision.ressources + local.resources)
                            .distinctBy { it.nom.trim().lowercase() }
                        vision.copy(
                            id = UUID.randomUUID().toString(),
                            analyzedAtIso = Instant.now().toString(),
                            imageUriString = uriText,
                            heros = mergedHeroes,
                            ressources = mergedResources,
                            champsIncertains = (vision.champsIncertains + local.uncertainItems)
                                .distinctBy { it.champ.lowercase() + "|" + it.valeurObservee.lowercase() }
                        )
                    },
                    onFailure = {
                        ScreenshotAnalysis(
                            id = UUID.randomUUID().toString(),
                            analyzedAtIso = Instant.now().toString(),
                            imageUriString = uriText,
                            typeCapture = local.detectedCategory,
                            resumeCapture = if (local.rawText.isBlank()) "Capture importée, texte non reconnu." else local.ocrSnippet,
                            puissance = local.power,
                            ressources = local.resources,
                            heros = local.heroes,
                            champsIncertains = local.uncertainItems
                        )
                    }
                )
                results.add(result)
                bitmap.recycle()
            } catch (_: Throwable) {
                failed++
            }
        }

        if (results.isNotEmpty()) {
            synchronized(lock) {
                val prefs = applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val previous = runCatching {
                    json.decodeFromString<List<ScreenshotAnalysis>>(
                        prefs.getString(KEY_PENDING, "[]") ?: "[]"
                    )
                }.getOrDefault(emptyList())
                prefs.edit()
                    .putString(KEY_PENDING, json.encodeToString((previous + results).takeLast(50)))
                    .commit()
            }
        }

        showCompletionNotification(results.size, uris.size, failed)
        return if (results.isNotEmpty()) Result.success() else Result.failure()
    }

    private fun showCompletionNotification(success: Int, total: Int, failed: Int) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Analyses de captures", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val openApp = PendingIntent.getActivity(
            applicationContext,
            7214,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val message = when {
            success == total -> "$success capture(s) analysée(s). Ouvre l'application pour consulter les résultats."
            success > 0 -> "$success/$total capture(s) analysée(s), $failed erreur(s). Ouvre l'application pour voir les résultats."
            else -> "L'analyse a échoué. Vérifie ta connexion et réessaie."
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Lands of Jail Assistant")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(applicationContext).notify(7214, notification) }
    }
}
