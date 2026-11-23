package com.landsofjail.assistant.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings

object UsageStatsDetector {

    // Vrai identifiant Google Play de Lands of Jail (Just Game Technology)
    const val DEFAULT_GAME_PACKAGE_KEYWORD = "com.justgame.jails"

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Retourne le package actuellement au premier plan via UsageStatsManager.
     * Combine queryEvents (fenêtre large 2 min) et queryUsageStats (dernier usage actif)
     * afin de ne pas perdre l'état quand le joueur reste longtemps dans le jeu sans changer d'écran.
     */
    fun getRecentForegroundPackage(context: Context, windowMs: Long = 120_000L): String? {
        if (!hasUsageStatsPermission(context)) return null
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - windowMs

        val events = usm.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var latestPackage: String? = null
        var latestTimestamp = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val isForegroundEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            } else {
                @Suppress("DEPRECATION")
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            }
            if (isForegroundEvent && event.timeStamp >= latestTimestamp) {
                latestTimestamp = event.timeStamp
                latestPackage = event.packageName
            }
        }

        if (!latestPackage.isNullOrBlank()) {
            return latestPackage
        }

        // Repli sur queryUsageStats si aucun nouvel événement ACTIVITY_RESUMED n'a eu lieu récemment
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            endTime - 600_000L,
            endTime
        )
        return stats
            ?.filter { it.lastTimeUsed > 0L }
            ?.maxByOrNull { it.lastTimeUsed }
            ?.packageName
    }

    fun isLandsOfJailPackage(packageName: String?, targetFilter: String): Boolean {
        if (packageName.isNullOrBlank()) return false
        val normalizedPkg = packageName.lowercase()
        val normalizedFilter = targetFilter.trim().lowercase()
        val knownKeywords = listOf(
            "com.justgame.jails",
            "justgame.jails",
            "landsofjail",
            "lands.of.jail"
        )
        if (knownKeywords.any { normalizedPkg == it || normalizedPkg.contains(it) }) {
            return true
        }
        return normalizedFilter.isNotEmpty() &&
            (normalizedPkg == normalizedFilter || normalizedPkg.contains(normalizedFilter))
    }
}
