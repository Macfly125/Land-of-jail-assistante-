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

internal enum class UsageEventKind {
    RESUMED,
    PAUSED,
    STOPPED,
    SCREEN_NON_INTERACTIVE,
    KEYGUARD_SHOWN,
    OTHER
}

internal data class UsageEventSnapshot(
    val packageName: String?,
    val kind: UsageEventKind,
    val timestamp: Long
)

/**
 * Resolves the latest foreground activity from chronological UsageEvents.
 * A stale lastTimeUsed value is deliberately not treated as proof that an app
 * is still visible: queryUsageStats is aggregated and can describe an app that
 * was opened earlier but has since been left.
 */
internal fun resolveForegroundPackage(events: List<UsageEventSnapshot>): String? {
    val lastResumeIndex = events.indexOfLast { it.kind == UsageEventKind.RESUMED }
    if (lastResumeIndex < 0) return null

    val resumed = events[lastResumeIndex]
    val packageName = resumed.packageName?.takeIf { it.isNotBlank() } ?: return null
    val laterEvents = events.drop(lastResumeIndex + 1)

    val appMovedToBackground = laterEvents.any {
        it.packageName == packageName &&
            (it.kind == UsageEventKind.PAUSED || it.kind == UsageEventKind.STOPPED)
    }
    val deviceBecameInactive = laterEvents.any {
        it.kind == UsageEventKind.SCREEN_NON_INTERACTIVE ||
            it.kind == UsageEventKind.KEYGUARD_SHOWN
    }

    return packageName.takeUnless { appMovedToBackground || deviceBecameInactive }
}

object UsageStatsDetector {

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
     * Reads actual foreground/background events. The caller can use a wide
     * initial window, then a short rolling window while retaining the last
     * confirmed package between polls.
     */
    fun getRecentForegroundPackage(context: Context, windowMs: Long = 120_000L): String? {
        if (!hasUsageStatsPermission(context)) return null
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val events = usm.queryEvents(endTime - windowMs.coerceAtLeast(1_000L), endTime)
            ?: return null
        val snapshots = mutableListOf<UsageEventSnapshot>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val kind = when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> UsageEventKind.RESUMED
                UsageEvents.Event.ACTIVITY_PAUSED -> UsageEventKind.PAUSED
                else -> when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        event.eventType == UsageEvents.Event.ACTIVITY_STOPPED -> UsageEventKind.STOPPED
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                        event.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UsageEventKind.SCREEN_NON_INTERACTIVE
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                        event.eventType == UsageEvents.Event.KEYGUARD_SHOWN -> UsageEventKind.KEYGUARD_SHOWN
                    else -> UsageEventKind.OTHER
                }
            }
            snapshots += UsageEventSnapshot(event.packageName, kind, event.timeStamp)
        }
        return resolveForegroundPackage(snapshots)
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
