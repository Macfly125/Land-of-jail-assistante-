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
        "robots" to listOf("robot", "robots", "droid", "drone", "robotics", "robot center", "robot upgrade", "power armor", "power armor hangar", "core module", "power module", "prisoner armor data", "musashimaru", "phantom cat", "ranger", "infercore", "hercules", "halo", "light cone", "atlax", "yokozuna", "bastion", "pluto"),
        "equipements" to listOf("hero equipment", "equipment upgrade", "equipment improvement", "equipment", "gear", "arsenal", "equipment exp", "equipment part", "potential coil", "precision gear", "magnet", "équipement", "équipements", "équipement du héros", "équipement héros", "amélioration d'équipement", "amélioration équipement", "aimant", "arsenal", "ausrüstung", "heldenausrüstung", "ausrüstungsverbesserung", "ausrüstung verbessern", "magnet", "ekipman", "kahraman ekipmani", "ekipman geliştirme", "teçhizat", "mıknatıs", "снаряжение", "улучшение снаряжения", "экипировка", "магнит", "装备", "装备升级", "英雄装备", "磁铁"),
        "heros" to listOf("heroes", "hero", "héros", "skills", "compétences", "stars", "étoiles", "helden", "fähigkeiten", "sterne", "kahraman", "kahramanlar", "beceriler", "yıldızlar", "герои", "навыки", "звёзды", "英雄", "技能", "星级"),
        "batiments" to listOf("building", "upgrade", "bâtiment", "construction", "warden office", "bureau du directeur", "barracks", "caserne", "laboratory", "laboratoire", "infirmary", "infirmerie", "warehouse", "entrepôt", "kitchen", "cuisine", "yard", "cour", "cell", "cellule", "comms", "gebäude", "ausbau", "bina", "yükseltme", "здание", "улучшение", "建筑", "升级"),
        "troupes" to listOf("shieldbearer", "shieldbearers", "bomber", "bombers", "shooter", "shooters", "troops", "troupes", "march capacity", "capacité de marche", "truppen", "birlikler", "войска", "部队"),
        "ressources" to listOf("food", "wood", "steel", "gasoline", "nourriture", "bois", "acier", "essence", "diamonds", "diamants", "gems", "gemmes", "nahrung", "holz", "stahl", "benzin", "diamanten", "yiyecek", "odun", "çelik", "elmas", "еда", "дерево", "сталь", "бензин", "алмазы", "食物", "木材", "钢材", "汽油", "钻石", "宝石", "nahrung", "holz", "stahl", "benzin", "yiyecek", "odun", "çelik", "еда", "дерево", "сталь", "бензин", "алмазы", "食物", "木材", "钢材", "汽油", "钻石", "宝石"),
        "profil" to listOf("power", "puissance", "eliminations", "éliminations", "profile", "profil", "macht", "güç", "мощь", "профиль", "战力", "档案")
    )

    fun analyze(rawText: String): ScreenRecognitionResult {
        val normalized = normalize(rawText)
        val paddedText = " $normalized "
        val ranked = dictionaries.mapNotNull { (category, labels) ->
            // Comparaison par mots/expressions entiers : « shieldbearers »
            // ne doit pas compter simultanément comme « shieldbearer ».
            val hits = labels.filter { label ->
                val normalizedLabel = normalize(label)
                normalizedLabel.isNotEmpty() && when {
                    // Les libellés chinois sont souvent concaténés sans espaces dans l'OCR.
                    normalizedLabel.any { it.code in 0x3400..0x9FFF || it.code in 0xF900..0xFAFF } ->
                        normalized.contains(normalizedLabel)
                    else -> paddedText.contains(" $normalizedLabel ")
                }
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
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
}
