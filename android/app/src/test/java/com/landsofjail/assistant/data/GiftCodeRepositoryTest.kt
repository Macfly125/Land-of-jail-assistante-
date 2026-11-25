package com.landsofjail.assistant.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GiftCodeRepositoryTest {
    @Test
    fun initialCatalogSeparatesCommunityListedUnverifiedAndExpiredCodes() {
        val snapshot = GiftCodeRepository.initialSnapshot()
        val vip = snapshot.entries.firstOrNull { it.code == "VIP777" }
        val expired = snapshot.entries.firstOrNull { it.code == "ZYKtwu9hchxuSv" }
        val unverified = snapshot.entries.firstOrNull { it.code == "RuStore777" }
        val upcomingExpiry = snapshot.entries.firstOrNull { it.code == "N4HDYuJxkQKur5" }

        assertNotNull(vip)
        assertEquals("COMMUNITY_LISTED", vip?.status)
        assertEquals("21/09/2026", vip?.firstSeen)
        assertEquals("01/11/2026", vip?.lastChecked)
        assertTrue(vip?.expiresAt.isNullOrBlank())

        assertNotNull(expired)
        assertEquals("EXPIRED", expired?.status)
        assertEquals("26/09/2026", expired?.expiresAt)

        assertNotNull(unverified)
        assertEquals("UNVERIFIED", unverified?.status)
        assertNotNull(upcomingExpiry)
        assertEquals("NEEDS_REVIEW", upcomingExpiry?.status)
        assertEquals("30/11/2026", upcomingExpiry?.expiresAt)
    }


    @Test
    fun frenchGuideLojIsRegisteredAsThePrimaryGuideSource() {
        assertEquals("https://guideloj.com/fr/codes", GiftCodeRepository.GUIDE_URL)
        assertEquals("https://guideloj.com/fr/home", GiftCodeRepository.GUIDE_HOME_URL)
        val snapshot = GiftCodeRepository.initialSnapshot()
        val code = snapshot.entries.first { it.code == "loj3579" }
        assertTrue(code.reward.contains("5 cartes de ravitaillement avancées"))
        assertTrue(code.reward.contains("3 accélérateurs universels"))
    }

    @Test
    fun catalogPreservesCaseSensitiveCodesAndContradictoryReports() {
        val snapshot = GiftCodeRepository.initialSnapshot()
        val lowerCaseCode = snapshot.entries.firstOrNull { it.code == "loj777" }
        val conflicting = snapshot.entries.firstOrNull { it.code == "LOJCANDY49" }
        val thirdParty = snapshot.entries.firstOrNull { it.code == "GuZQdq1KnWQJMx" }

        assertNotNull(lowerCaseCode)
        assertNotNull(conflicting)
        assertEquals("NEEDS_REVIEW", conflicting?.status)
        assertEquals("02/11/2025", conflicting?.expiresAt)
        assertEquals("UNVERIFIED", thirdParty?.status)
        assertFalse(snapshot.entries.any { it.code == "LOJ777" })
    }
}
