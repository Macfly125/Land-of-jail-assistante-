package com.landsofjail.assistant.model

import kotlinx.serialization.Serializable

enum class PlayerRole(val labelFr: String, val descriptionFr: String) {
    LEADER_RALLIEMENT("Leader de ralliement", "Optimise la capacité de ralliement et les dégâts de groupe."),
    ASSAUT_DPS("Assaut / DPS", "Maximise les dégâts bruts en escarmouche et duel."),
    DEFENSEUR_GARNISON("Défenseur / Garnison", "Protège la base avec tanks, boucliers et contre-attaques."),
    SOUTIEN_CONTROLE("Soutien / Contrôle", "Soins, réductions de dégâts et effets de contrôle."),
    POLYVALENT_F2P("Polyvalent / F2P", "Progression équilibrée et gestion stricte des ressources.")
}

enum class AdviceMode(val labelFr: String) {
    TRIAL_CAGE("Trial Cage"),
    CRYSTAL_CLASH("Crystal Clash")
}

@Serializable
data class HeroSkill(
    val nom: String,
    val niveau: String,
    val description: String? = null,
    val incertain: Boolean = false,
    val raisonIncertitude: String? = null
)

@Serializable
data class HeroRecord(
    val nom: String,
    val niveau: Int? = null,
    val niveauTexte: String,
    val rarete: String? = null,
    val roleHero: String? = null,
    val competences: List<HeroSkill> = emptyList(),
    val incertain: Boolean = false,
    val raisonIncertitude: String? = null
)

@Serializable
data class ResourceRecord(
    val nom: String,
    val quantiteTexte: String,
    val quantiteNumerique: Double? = null,
    val incertain: Boolean = false,
    val raisonIncertitude: String? = null
)

@Serializable
data class PowerMetric(
    val puissanceTotaleTexte: String,
    val puissanceTotaleNumerique: Long? = null,
    val niveauPrisonOuQG: String? = null,
    val incertain: Boolean = false,
    val raisonIncertitude: String? = null
)

@Serializable
data class UncertainItem(
    val champ: String,
    val valeurObservee: String,
    val raison: String
)

@Serializable
data class ScreenshotAnalysis(
    val id: String,
    val analyzedAtIso: String,
    val imageUriString: String,
    val typeCapture: String,
    val resumeCapture: String,
    val puissance: PowerMetric? = null,
    val ressources: List<ResourceRecord> = emptyList(),
    val heros: List<HeroRecord> = emptyList(),
    val champsIncertains: List<UncertainItem> = emptyList()
)

@Serializable
data class GroundedWebSource(
    val title: String,
    val url: String,
    val detectedDate: String? = null
)

@Serializable
data class AdviceRecommendation(
    val titre: String,
    val detail: String,
    val herosCibles: List<String> = emptyList(),
    val incertain: Boolean = false,
    val raisonIncertitude: String? = null
)

@Serializable
data class TacticalAdviceResult(
    val mode: String,
    val generatedAtIso: String,
    val synthese: String,
    val compositionRecommandee: String,
    val recommandations: List<AdviceRecommendation> = emptyList(),
    val informationsIncertaines: List<String> = emptyList(),
    val sources: List<GroundedWebSource> = emptyList()
)
