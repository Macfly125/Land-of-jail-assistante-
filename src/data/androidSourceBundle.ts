export interface AndroidSourceFile {
  path: string;
  language: string;
  description: string;
  content: string;
}

export const ANDROID_KOTLIN_FILES: AndroidSourceFile[] = [
  {
    path: 'app/src/main/AndroidManifest.xml',
    language: 'xml',
    description:
      'Déclare PACKAGE_USAGE_STATS, le service GameForegroundMonitorService, les notifications « Analyser l’écran », le bouton flottant optionnel, et ScreenCaptureConsentActivity (MediaProjection). Aucun AccessibilityService.',
    content: `<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission
        android:name="android.permission.PACKAGE_USAGE_STATS"
        tools:ignore="ProtectedPermissions" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

    <application
        android:allowBackup="true"
        android:label="Lands of Jail Assistant"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.DeviceDefault.NoActionBar">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTop">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.intent.action.SEND" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="image/*" />
            </intent-filter>
        </activity>

        <activity
            android:name=".capture.ScreenCaptureConsentActivity"
            android:exported="false"
            android:launchMode="singleInstance"
            android:theme="@android:style/Theme.Translucent.NoTitleBar" />

        <service
            android:name=".service.GameForegroundMonitorService"
            android:exported="false"
            android:foregroundServiceType="specialUse|mediaProjection" />
    </application>
</manifest>`,
  },
  {
    path: 'app/src/main/java/com/landsofjail/assistant/service/UsageStatsDetector.kt',
    language: 'kotlin',
    description:
      'Vérifie AppOpsManager.OPSTR_GET_USAGE_STATS, ouvre Settings.ACTION_USAGE_ACCESS_SETTINGS et interroge UsageStatsManager.queryEvents + queryUsageStats pour détecter com.justgame.jails au premier plan.',
    content: `package com.landsofjail.assistant.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings

object UsageStatsDetector {
    const val DEFAULT_GAME_PACKAGE_KEYWORD = "com.justgame.jails"

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun getRecentForegroundPackage(context: Context, windowMs: Long = 120_000L): String? {
        if (!hasUsageStatsPermission(context)) return null
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val events = usm.queryEvents(endTime - windowMs, endTime)
        val event = UsageEvents.Event()
        var latestPkg: String? = null
        var latestTs = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val isFg = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            } else {
                @Suppress("DEPRECATION")
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            }
            if (isFg && event.timeStamp >= latestTs) {
                latestTs = event.timeStamp
                latestPkg = event.packageName
            }
        }
        return latestPkg ?: usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, endTime - 600_000L, endTime)
            ?.maxByOrNull { it.lastTimeUsed }?.packageName
    }
}`,
  },
  {
    path: 'app/src/main/java/com/landsofjail/assistant/service/GameForegroundMonitorService.kt',
    language: 'kotlin',
    description:
      'Service au premier plan qui affiche la notification et le bouton flottant « Analyser l’écran » dès le lancement du jeu, avec bouton « Arrêter la détection ».',
    content: `// Fichier complet dans /android/app/src/main/java/com/landsofjail/assistant/service/GameForegroundMonitorService.kt
// Utilise UsageStatsDetector.getRecentForegroundPackage() toutes les 2,5 s.
// À l'ouverture de Lands of Jail : affiche une notification haute priorité avec action « Analyser l’écran »
// (et un bouton flottant optionnel si SYSTEM_ALERT_WINDOW est accordé).
// Le bouton / action « Arrêter la détection » stoppe immédiatement le service et retire le bouton flottant.`,
  },
  {
    path: 'app/src/main/java/com/landsofjail/assistant/capture/ScreenCaptureConsentActivity.kt',
    language: 'kotlin',
    description:
      'Activité déclenchée uniquement par l’utilisateur sur « Analyser l’écran » : demande createScreenCaptureIntent() (MediaProjection), capture UNE image pour l’OCR et stoppe aussitôt la projection.',
    content: `// Fichier complet dans /android/app/src/main/java/com/landsofjail/assistant/capture/ScreenCaptureConsentActivity.kt
// 1. Appelle projectionManager.createScreenCaptureIntent() (boîte de dialogue système visible).
// 2. Si l'utilisateur accepte, capture une unique image via VirtualDisplay + ImageReader.
// 3. Ferme immédiatement VirtualDisplay, ImageReader et MediaProjection.
// 4. Transmet le Bitmap pour l'OCR ML Kit puis ouvre le tableau de bord et les conseils Trial Cage / Crystal Clash.`,
  },
  {
    path: 'app/src/main/java/com/landsofjail/assistant/ocr/LandsOfJailOcrParser.kt',
    language: 'kotlin',
    description:
      'Reconnaissance de texte ML Kit sur l’appareil (TextRecognition) + marquage systématique des informations incertaines.',
    content: `// Fichier complet dans /android/app/src/main/java/com/landsofjail/assistant/ocr/LandsOfJailOcrParser.kt
// Extrait le texte de la capture autorisée via ML Kit et signale les valeurs ambiguës.`,
  },
  {
    path: 'app/src/main/java/com/landsofjail/assistant/ui/LandsOfJailApp.kt',
    language: 'kotlin',
    description:
      'Interface Jetpack Compose Material 3 complète avec panneau UsageStatsManager, bouton « Arrêter la détection », bouton « Analyser l’écran », tableau de bord et conseils datés/sourcés.',
    content: `// Fichier complet dans /android/app/src/main/java/com/landsofjail/assistant/ui/LandsOfJailApp.kt`,
  },
];
