package com.landsofjail.assistant.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset
import java.util.Locale

class AllianceVerificationTimestampTest {
    @Test
    fun formatsServerTimestampUsingRequestedLocaleAndZone() {
        val formatted = formatAllianceVerificationTimestamp(
            value = "2026-11-27T21:30:00Z",
            locale = Locale.FRANCE,
            zoneId = ZoneOffset.UTC
        )

        org.junit.Assert.assertTrue(formatted.contains("2026"))
        org.junit.Assert.assertTrue(formatted.contains("21:30"))
    }

    @Test
    fun supportsOffsetTimestampAndConvertsToLocalZone() {
        val formatted = formatAllianceVerificationTimestamp(
            value = "2026-11-27T22:30:00+01:00",
            locale = Locale.FRANCE,
            zoneId = ZoneOffset.UTC
        )

        org.junit.Assert.assertTrue(formatted.contains("2026"))
        org.junit.Assert.assertTrue(formatted.contains("21:30"))
    }

    @Test
    fun keepsOriginalValueWhenTimestampCannotBeParsed() {
        val invalid = "date-indisponible"

        assertEquals(invalid, formatAllianceVerificationTimestamp(invalid))
    }
}
