package com.landsofjail.assistant.model

/**
 * Source-backed inventory of the four research trees.
 *
 * This is metadata only: aggregate totals and technology counts must never be
 * used as per-level costs. Detailed level rows are not imported yet.
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

data class ResearchTreeOverview(
    val tree: ResearchTree,
    val checkedOn: String = "2026-11-26",
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
    val overviews = ResearchTree.entries.map(::ResearchTreeOverview)

    fun overview(tree: ResearchTree): ResearchTreeOverview =
        overviews.single { it.tree == tree }
}
