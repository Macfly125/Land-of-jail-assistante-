package com.landsofjail.assistant.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

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
    fun alliancePortalGiftCodeFeedIsRegisteredAndExplicitCodeMarkupIsExtracted() {
        val html = """
            <h1>Gift codes</h1>
            <h2><code>4AWwVMpUETEgzM</code></h2>
            <p>Published 26.11.2026</p>
            <p>Published expiry date: 30.11.2026. 23:59 Server Time</p>
        """.trimIndent()

        assertEquals("https://loj.dookee.ru/en/codes/", GiftCodeRepository.ALLIANCE_PORTAL_URL)
        val parsed = GiftCodeRepository.extractCandidatesFromHtml(html)
        assertTrue(parsed.any { it.first == "4AWwVMpUETEgzM" })
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
    @Test
    fun parserFindsCodesInContextualListsButIgnoresOrdinaryUppercaseText() {
        val html = """
            <h2>Active gift codes</h2>
            <ul><li>Gift codes: LOJNOW and JAIL2026</li></ul>
            <p>Our latest rewards include DIAMONDS and HEROES.</p>
            <li>Expired codes: OLD2025</li>
        """.trimIndent()

        val parsed = GiftCodeRepository.extractCandidatesFromHtml(html)
            .associateBy { it.first }

        assertTrue(parsed.containsKey("LOJNOW"))
        assertTrue(parsed.containsKey("JAIL2026"))
        assertTrue(parsed.containsKey("OLD2025"))
        assertFalse(parsed.containsKey("DIAMONDS"))
        assertFalse(parsed.containsKey("HEROES"))
    }

    @Test
    fun parserRecognizesFrenchActiveAndExpiredGiftCodeHeadings() {
        val html = """
            <h2>Codes cadeaux actifs</h2>
            <ul><li>LOJ2026FR</li><li>JAIL2026FR</li></ul>
            <h2>Récompenses récentes</h2>
            <ul><li>HERO2026FR</li></ul>
            <h2>Codes expirés</h2>
            <ul><li>OLD2026FR</li></ul>
        """.trimIndent()

        val parsed = GiftCodeRepository.extractCandidatesFromHtml(html).associateBy { it.first }

        assertTrue(parsed.containsKey("LOJ2026FR"))
        assertTrue(parsed.containsKey("JAIL2026FR"))
        assertFalse(parsed.containsKey("HERO2026FR"))
        assertTrue(parsed.containsKey("OLD2026FR"))
        assertTrue(parsed.getValue("OLD2026FR").second.contains("codes expir"))
    }

    @Test
    fun parserKeepsExplicitCodeTagsEvenWithoutNearbyHeading() {
        val html = "<div><code>AbC123xy</code></div>"
        val parsed = GiftCodeRepository.extractCandidatesFromHtml(html)
        assertTrue(parsed.any { it.first == "AbC123xy" })
    }

    @Test
    fun laterExpiredSectionReportDowngradesListedCodeToReviewButNeverClaimsExpired() {
        val listed = GiftCodeEntry(
            code = "LOJ777",
            status = "COMMUNITY_LISTED",
            source = "GuideLOJ",
            note = "Code rapporté par une source communautaire."
        )
        val reviewed = GiftCodeRepository.mergeSourceCandidate(
            listed, "LOJ777", "LOJ XLab", "26/11/2026", looksExpired = true
        )
        assertEquals("NEEDS_REVIEW", reviewed.status)
        assertTrue(reviewed.source.contains("GuideLOJ"))
        assertTrue(reviewed.source.contains("LOJ XLab"))
        assertTrue(reviewed.note.contains("codes expirés"))

        val alreadyExpired = GiftCodeRepository.mergeSourceCandidate(
            reviewed.copy(status = "EXPIRED"), "LOJ777", "Pocket Gamer", "", looksExpired = true
        )
        assertEquals("EXPIRED", alreadyExpired.status)

        val ordinaryMention = GiftCodeRepository.mergeSourceCandidate(
            listed, "LOJ777", "Pocket Gamer", "", looksExpired = false
        )
        assertEquals("COMMUNITY_LISTED", ordinaryMention.status)
    }

    @Test
    fun onlyPublishedExpiryDatesCanAutomaticallyMoveCodesToExpired() {
        val entries = listOf(
            GiftCodeEntry("EXPIRED1", "NEEDS_REVIEW", "source", expiresAt = "24/11/2026"),
            GiftCodeEntry("TODAY1", "NEEDS_REVIEW", "source", expiresAt = "25/11/2026"),
            GiftCodeEntry("FUTURE1", "NEEDS_REVIEW", "source", expiresAt = "30/11/2026"),
            GiftCodeEntry("NO_DATE", "UNVERIFIED", "source"),
            GiftCodeEntry("NO_GUESS", "COMMUNITY_LISTED", "source", note = "Aucune expiration annoncée")
        )

        val updated = GiftCodeRepository.applyPublishedExpiryDates(
            entries,
            LocalDate.of(2026, 11, 25)
        ).associateBy { it.code }

        assertEquals("EXPIRED", updated.getValue("EXPIRED1").status)
        assertEquals("EXPIRED", updated.getValue("TODAY1").status)
        assertEquals("NEEDS_REVIEW", updated.getValue("FUTURE1").status)
        assertEquals("UNVERIFIED", updated.getValue("NO_DATE").status)
        assertEquals("COMMUNITY_LISTED", updated.getValue("NO_GUESS").status)
        assertTrue(updated.getValue("EXPIRED1").note.contains("expiration"))
    }

    @Test
    fun alreadyUsedIsPlayerStatePreservedWithoutChangingSourceValidity() {
        val initial = GiftCodeSnapshot(entries = listOf(
            GiftCodeEntry("LOJ777", "COMMUNITY_LISTED", "Community source")
        ))
        val used = GiftCodeRepository.markAsUsed(initial, "loj777", true)
        val entry = used.entries.single()
        assertEquals("ALREADY_USED", entry.playerStatus)
        assertEquals("COMMUNITY_LISTED", entry.status)

        val reset = GiftCodeRepository.markAsUsed(used, "LOJ777", false)
        assertEquals("", reset.entries.single().playerStatus)
        assertEquals("COMMUNITY_LISTED", reset.entries.single().status)
    }

    @Test
    fun parsesSupportedExpiryDateFormatsWithoutGuessingInvalidDates() {
        val entries = listOf(
            GiftCodeEntry("SLASH", "UNVERIFIED", "source", expiresAt = "24/11/2026"),
            GiftCodeEntry("DOT", "UNVERIFIED", "source", expiresAt = "24.11.2026"),
            GiftCodeEntry("DASH", "UNVERIFIED", "source", expiresAt = "24-11-2026"),
            GiftCodeEntry("ISO", "UNVERIFIED", "source", expiresAt = "2026-11-24"),
            GiftCodeEntry("INVALID", "UNVERIFIED", "source", expiresAt = "31/02/2026")
        )
        val updated = GiftCodeRepository.applyPublishedExpiryDates(
            entries,
            LocalDate.of(2026, 11, 25)
        ).associateBy { it.code }

        listOf("SLASH", "DOT", "DASH", "ISO").forEach {
            assertEquals("EXPIRED", updated.getValue(it).status)
        }
        assertEquals("UNVERIFIED", updated.getValue("INVALID").status)
    }

    @Test
    fun sourceCheckDatesUseStrictCalendarValidation() {
        assertEquals(LocalDate.of(2026, 11, 26), GiftCodeRepository.parseSourceDate("26/11/2026"))
        assertEquals(LocalDate.of(2026, 11, 26), GiftCodeRepository.parseSourceDate("Nov 26, 2026"))
        assertEquals(LocalDate.of(2026, 11, 26), GiftCodeRepository.parseSourceDate("26 novembre 2026"))
        assertNull(GiftCodeRepository.parseSourceDate("31/02/2026"))
        assertNull(GiftCodeRepository.parseSourceDate("32/11/2026"))
        assertNull(GiftCodeRepository.parseSourceDate("not a date"))
    }

    @Test
    fun parserInheritsGiftCodeContextFromSectionHeadingsOnlyWithinThatSection() {
        val html = """
            <h2>Active Gift Codes</h2>
            <ul><li>LOJ2026A</li><li>JAIL2026B</li></ul>
            <h2>Latest rewards</h2>
            <ul><li>DIAMONDS2026</li><li>HEROES2026</li></ul>
            <h2>Expired Codes</h2>
            <ul><li>OLD2026A</li></ul>
        """.trimIndent()

        val parsed = GiftCodeRepository.extractCandidatesFromHtml(html).associateBy { it.first }

        assertTrue(parsed.containsKey("LOJ2026A"))
        assertTrue(parsed.containsKey("JAIL2026B"))
        assertFalse(parsed.containsKey("DIAMONDS2026"))
        assertFalse(parsed.containsKey("HEROES2026"))
        assertTrue(parsed.getValue("OLD2026A").second.contains("expired codes"))
    }

}
