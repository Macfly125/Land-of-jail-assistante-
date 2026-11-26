package com.landsofjail.assistant.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class LegacyUiTranslationDictionaryTest {
    private fun sourceText(): String {
        val relativePath = "src/main/java/com/landsofjail/assistant/ui/LandsOfJailApp.kt"
        val candidates = listOf(File(relativePath), File("android/app/$relativePath"))
        return candidates.firstOrNull { it.isFile }?.readText()
            ?: error("Cannot locate LandsOfJailApp.kt for localization audit")
    }

    @Test
    fun everyLegacyUiDictionaryEntryHasSevenLanguages() {
        val source = sourceText()
        val start = source.indexOf("val dictionary = mapOf(")
        assertTrue("Legacy UI dictionary must exist", start >= 0)
        val end = source.indexOf("\n    )\n    var translated", start)
        assertTrue("Legacy UI dictionary must have a closing boundary", end > start)

        val entries = source.substring(start, end)
            .lineSequence()
            .filter { it.contains("to listOf(") }
            .toList()

        assertEquals("All 143 legacy strings must remain covered", 143, entries.size)
        entries.forEachIndexed { index, line ->
            val listStart = line.indexOf("to listOf(") + "to listOf(".length
            val values = Regex("\\\"(?:[^\\\"\\\\]|\\\\.)*\\\"")
                .findAll(line.substring(listStart))
                .count()
            assertEquals("Dictionary entry $index must contain FR/EN/DE/TR/RU/ZH/IT", 7, values)
        }
    }

    @Test
    fun italianLegacyFallbackNeverSelectsTheEnglishValue() {
        val source = sourceText()
        assertFalse(source.contains("if (language == AppLanguage.IT) dictionary[source]?.getOrNull(1)"))
    }
}
