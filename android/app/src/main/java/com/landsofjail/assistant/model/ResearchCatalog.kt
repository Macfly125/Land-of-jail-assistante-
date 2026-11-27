package com.landsofjail.assistant.model

/**
 * Source-backed inventory of the four research trees.
 *
 * Aggregate values are display-only summaries from the source. They must
 * never be used as per-level costs or as inputs to a player-specific plan.
 */
enum class ResearchTree(
    val sourceUrl: String,
    val technologyCount: Int,
    val aggregateBaseDurationSeconds: Long?,
    val seasonDependent: Boolean
) {
    LOGISTICS(
        "https://lojwiki.com/lab/logistics/",
        46,
        1104L * 24L * 60L * 60L + 6L * 60L * 60L,
        false
    ),
    ECONOMY(
        "https://lojwiki.com/lab/economy/",
        28,
        166L * 24L * 60L * 60L + 6L * 60L * 60L,
        false
    ),
    MILITARY(
        "https://lojwiki.com/lab/military/",
        102,
        3911L * 24L * 60L * 60L + 18L * 60L * 60L,
        false
    ),
    FORTICLAD(
        "https://lojwiki.com/lab/forticlad-lab/",
        9,
        null,
        true
    )
}

/** Published whole-tree figures; costs are intentionally display strings. */
data class ResearchAggregateSummary(
    val totalPower: Long,
    val costSummary: String,
    val minimumDurationSeconds: Long,
    val maximumDurationSeconds: Long = minimumDurationSeconds
) {
    init {
        require(totalPower >= 0L)
        require(costSummary.isNotBlank())
        require(minimumDurationSeconds >= 0L)
        require(maximumDurationSeconds >= minimumDurationSeconds)
    }
}

data class ResearchTreeOverview(
    val tree: ResearchTree,
    val checkedOn: String = "2026-11-27",
    val levelCostsImported: Boolean = false,
    val prerequisitesImported: Boolean = false
) {
    init {
        require(checkedOn.isNotBlank())
        // Aggregate tree statistics are informative only. They are not a
        // substitute for individual technology-level requirements.
        require(!levelCostsImported || prerequisitesImported)
    }
}

object ResearchCatalog {
    private const val DAY = 24L * 60L * 60L
    private const val HOUR = 60L * 60L

    val overviews = ResearchTree.entries.map(::ResearchTreeOverview)

    /**
     * Rounded aggregate costs as published by LOJ Wiki. These values are
     * informational only and are not suitable for calculating partial plans.
     */
    val aggregateSummaries: Map<ResearchTree, ResearchAggregateSummary> = mapOf(
        ResearchTree.LOGISTICS to ResearchAggregateSummary(
            totalPower = 1_924_872L,
            costSummary = "499.4M Food · 499.4M Wood · 99.9M Steel · 25M Gasoline · 8.3M Gold Card",
            minimumDurationSeconds = 1104L * DAY + 6L * HOUR
        ),
        ResearchTree.ECONOMY to ResearchAggregateSummary(
            totalPower = 196_769L,
            costSummary = "327M Food · 327M Wood · 65.4M Steel · 16.3M Gasoline · 6.5M Gold Card",
            minimumDurationSeconds = 166L * DAY + 6L * HOUR
        ),
        ResearchTree.MILITARY to ResearchAggregateSummary(
            totalPower = 7_347_780L,
            costSummary = "1.8B Food · 1.8B Wood · 360.3M Steel · 90.1M Gasoline · 32.7M Gold Card",
            minimumDurationSeconds = 3911L * DAY + 18L * HOUR
        ),
        ResearchTree.FORTICLAD to ResearchAggregateSummary(
            totalPower = 18_348_000L,
            costSummary = "48,150 Hyperalloy",
            minimumDurationSeconds = 467L * DAY + 9L * HOUR,
            maximumDurationSeconds = 515L * DAY + 19L * HOUR
        )
    )

    fun treeForTarget(rawTarget: String): ResearchTree? = when (
        rawTarget.trim().lowercase().replace(' ', '_').replace('-', '_')
    ) {
        "logistics", "logistique", "logistik" -> ResearchTree.LOGISTICS
        "economy", "économie", "economie", "wirtschaft" -> ResearchTree.ECONOMY
        "military", "militaire", "militär", "militar" -> ResearchTree.MILITARY
        "forticlad", "forticlad_lab", "laboratoire_forticlad" -> ResearchTree.FORTICLAD
        else -> null
    }

    fun overview(tree: ResearchTree): ResearchTreeOverview =
        overviews.single { it.tree == tree }

    fun aggregateSummary(tree: ResearchTree): ResearchAggregateSummary =
        requireNotNull(aggregateSummaries[tree]) { "Missing aggregate summary for $tree" }
}
