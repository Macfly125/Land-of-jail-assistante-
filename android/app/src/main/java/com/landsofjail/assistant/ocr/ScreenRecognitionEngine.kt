package com.landsofjail.assistant.ocr

/**
 * Moteur local de classification multi-écrans.
 * Il exploite les indices OCR connus sans inventer de valeurs visuelles.
 * La confiance est volontairement conservatrice : l'OCR seul ne prouve pas
 * qu'un élément graphique a été reconnu.
 */
data class ScreenRecognitionResult(
    val category: String,
    val confidence: Float,
    val matchedLabels: List<String>,
    val alternativeCategories: List<String>,
    val needsVisualConfirmation: Boolean
)

object ScreenRecognitionEngine {
    private val dictionaries = linkedMapOf(
        "trial_cage" to listOf("trial cage", "cage", "damage ranking", "damage dealt", "dégâts infligés"),
        "crystal_clash" to listOf("crystal clash", "ironhold", "ironprison", "energy hub", "crystal prison"),
        "sandsea_conquest" to listOf("sandsea", "vessel station", "lighthouse", "phare", "fuel"),
        "svs" to listOf("supreme honor", "svs", "war preparation", "préparation de guerre"),
        "turmoil_surge" to listOf("turmoil", "sentry turret", "wave", "vague"),
        "gold_hunter" to listOf("gold hunter", "hunter points"),
        "robots" to listOf("robot", "droid", "drone", "robotics"),
        "heros" to listOf("heroes", "hero", "héros", "skills", "compétences", "stars", "étoiles"),
        "batiments" to listOf("building", "upgrade", "bâtiment", "construction", "warden office", "bureau du directeur", "barracks", "caserne", "laboratory", "laboratoire", "infirmary", "infirmerie", "warehouse", "entrepôt", "kitchen", "cuisine", "yard", "cour", "cell", "cellule", "comms"),
        "troupes" to listOf("shieldbearer", "shieldbearers", "bomber", "bombers", "shooter", "shooters", "troops", "troupes", "march capacity", "capacité de marche"),
        "ressources" to listOf("food", "wood", "steel", "gasoline", "nourriture", "bois", "acier", "essence", "diamonds", "diamants", "gems", "gemmes"),
        "profil" to listOf("power", "puissance", "eliminations", "éliminations", "profile", "profil")
    )

    fun analyze(rawText: String): ScreenRecognitionResult {
        val normalized = normalize(rawText)
        val paddedText = " $normalized "
        val ranked = dictionaries.mapNotNull { (category, labels) ->
            // Comparaison par mots/expressions entiers : « shieldbearers »
            // ne doit pas compter simultanément comme « shieldbearer ».
            val hits = labels.filter { label ->
                val normalizedLabel = normalize(label)
                normalizedLabel.isNotEmpty() &&
                    paddedText.contains(" $normalizedLabel ")
            }.distinct()
            if (hits.isEmpty()) null else category to hits
        }.sortedByDescending { it.second.size }

        val best = ranked.firstOrNull()
        val second = ranked.getOrNull(1)
        val confidence = when {
            best == null -> 0f
            best.second.size >= 3 && (second == null || best.second.size > second.second.size) -> 0.95f
            best.second.size >= 2 && (second == null || best.second.size > second.second.size) -> 0.84f
            best.second.size > (second?.second?.size ?: 0) -> 0.68f
            else -> 0.52f
        }
        return ScreenRecognitionResult(
            category = best?.first ?: "ocr_local",
            confidence = confidence,
            matchedLabels = best?.second.orEmpty(),
            alternativeCategories = ranked.drop(1).take(2).map { it.first },
            needsVisualConfirmation = confidence < 0.8f
        )
    }

    private fun normalize(value: String): String =
        java.text.Normalizer.normalize(value.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()
}
