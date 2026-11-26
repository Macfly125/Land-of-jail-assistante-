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
            val values = line.substring(listStart).count { it == '"' } / 2
            assertEquals("Dictionary entry $index must contain FR/EN/DE/TR/RU/ZH/IT", 7, values)
        }
    }

    @Test
    fun everyDynamicFragmentLanguageHasTheSameSixtyOneEntries() {
        val source = sourceText()
        val fragmentsStart = source.indexOf("val fragments = when (language)")
        assertTrue("Dynamic fragment translations must exist", fragmentsStart >= 0)

        listOf("EN", "DE", "TR", "RU", "ZH", "IT").forEach { language ->
            val marker = "AppLanguage.$language -> mapOf("
            val start = source.indexOf(marker, fragmentsStart)
            assertTrue("$language fragment map must exist", start >= 0)
            val open = source.indexOf("(", start)
            var depth = 0
            var quote = false
            var escaped = false
            var close = -1
            for (index in open until source.length) {
                val char = source[index]
                if (quote) {
                    if (escaped) escaped = false
                    else if (char.code == 92) escaped = true
                    else if (char == '"') quote = false
                    continue
                }
                if (char == '"') quote = true
                else if (char == '(') depth++
                else if (char == ')' && --depth == 0) {
                    close = index
                    break
                }
            }
            assertTrue("$language fragment map must close", close > open)
            val block = source.substring(open + 1, close)
            var pairCount = 0
            var inString = false
            var escapedValue = false
            for (index in block.indices) {
                val char = block[index]
                if (inString) {
                    if (escapedValue) escapedValue = false
                    else if (char.code == 92) escapedValue = true
                    else if (char == '"') inString = false
                } else if (char == '"') {
                    inString = true
                } else if (block.startsWith(" to ", index)) {
                    pairCount++
                }
            }
            assertEquals("$language must have all 61 dynamic fragments", 61, pairCount)
        }
    }

    @Test
    fun italianLegacyFallbackNeverSelectsTheEnglishValue() {
        val source = sourceText()
        assertFalse(source.contains("if (language == AppLanguage.IT) dictionary[source]?.getOrNull(1)"))
    }
}
