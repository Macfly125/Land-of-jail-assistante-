package com.landsofjail.assistant.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProgressionCoachLocalizationTest {
    private fun sourceText(): String {
        val relativePath = "src/main/java/com/landsofjail/assistant/ui/ProgressionCoachScreen.kt"
        val candidates = listOf(File(relativePath), File("android/app/$relativePath"))
        return candidates.firstOrNull { it.isFile }?.readText()
            ?: error("Cannot locate ProgressionCoachScreen.kt for localization audit")
    }

    @Test
    fun troopTrainingDurationUsesChineseRatherThanRussianInChineseLocale() {
        val source = sourceText()
        val marker = "Cumulative base time (one queue)"
        val start = source.indexOf(marker)
        assertTrue("Troop training duration translations must exist", start >= 0)
        val block = source.substring(start, (start + 2400).coerceAtMost(source.length))
        val chinese = block.lineSequence().firstOrNull {
            it.contains("累计基础训练时间")
        }
        assertTrue("Chinese duration must have a Chinese translation", chinese != null)
        assertFalse(
            "Chinese duration must not contain Cyrillic text",
            chinese.orEmpty().any { it in '\u0400'..'\u04FF' }
        )
    }
}
