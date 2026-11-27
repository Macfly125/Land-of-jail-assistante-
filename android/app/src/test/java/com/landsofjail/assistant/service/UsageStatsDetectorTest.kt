package com.landsofjail.assistant.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageStatsDetectorTest {

    @Test
    fun returnsMostRecentlyResumedPackage() {
        val result = resolveForegroundPackage(
            listOf(
                UsageEventSnapshot("com.android.launcher", UsageEventKind.RESUMED, 100L),
                UsageEventSnapshot("com.justgame.jails", UsageEventKind.RESUMED, 200L)
            )
        )
        assertEquals("com.justgame.jails", result)
    }

    @Test
    fun doesNotKeepGameForegroundAfterAnotherAppResumes() {
        val result = resolveForegroundPackage(
            listOf(
                UsageEventSnapshot("com.justgame.jails", UsageEventKind.RESUMED, 100L),
                UsageEventSnapshot("com.android.chrome", UsageEventKind.RESUMED, 200L)
            )
        )
        assertEquals("com.android.chrome", result)
    }

    @Test
    fun rejectsPackageWhenItsActivityWasPausedAfterResume() {
        val result = resolveForegroundPackage(
            listOf(
                UsageEventSnapshot("com.justgame.jails", UsageEventKind.RESUMED, 100L),
                UsageEventSnapshot("com.justgame.jails", UsageEventKind.PAUSED, 200L)
            )
        )
        assertNull(result)
    }

    @Test
    fun rejectsForegroundWhenScreenBecameNonInteractive() {
        val result = resolveForegroundPackage(
            listOf(
                UsageEventSnapshot("com.justgame.jails", UsageEventKind.RESUMED, 100L),
                UsageEventSnapshot("android", UsageEventKind.SCREEN_NON_INTERACTIVE, 200L)
            )
        )
        assertNull(result)
    }

    @Test
    fun rejectsForegroundWhenKeyguardWasShown() {
        val result = resolveForegroundPackage(
            listOf(
                UsageEventSnapshot("com.justgame.jails", UsageEventKind.RESUMED, 100L),
                UsageEventSnapshot("com.android.systemui", UsageEventKind.KEYGUARD_SHOWN, 200L)
            )
        )
        assertNull(result)
    }

    @Test
    fun sortsOutOfOrderEventsBeforeResolving() {
        val result = resolveForegroundPackage(
            listOf(
                UsageEventSnapshot("com.android.chrome", UsageEventKind.RESUMED, 200L),
                UsageEventSnapshot("com.justgame.jails", UsageEventKind.RESUMED, 100L)
            )
        )
        assertEquals("com.android.chrome", result)
    }

    @Test
    fun returnsNullWhenNoForegroundEventExists() {
        assertNull(
            resolveForegroundPackage(
                listOf(UsageEventSnapshot("android", UsageEventKind.OTHER, 100L))
            )
        )
    }
}
