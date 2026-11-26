package com.landsofjail.assistant.ocr

/**
 * Moteur local de classification multi-écrans.
 * Il exploite les indices OCR connus sans inventer de valeurs visuelles.
 * Le score classe les indices textuels entre eux ; il n'est pas une probabilité
 * calibrée. L'OCR seul ne prouve jamais l'identité visuelle d'un écran.
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
        "gorilla_legion" to listOf("gorilla legion", "gorilla chieftain", "gorilla warlord", "gorilla warrior", "légion de gorilla", "légion gorilla", "gorille sage", "gorille armé", "gorille trésor", "gorille de l’espace", "gorillalegion", "gorilla-legion", "gorilla lejyonu", "goril lejyonu", "легион горилл", "горилла военачальник", "大猩猩军团", "猩猩军团"),
        "trial_cage" to listOf("trial cage", "cage", "damage ranking", "damage dealt", "dégâts infligés", "versuchskäfig", "prüfungskäfig", "schadensrangliste", "verursachter schaden", "deneme kafesi", "verilen hasar", "испытательная клетка", "нанесенный урон", "试炼笼", "造成伤害"),
        "crystal_clash" to listOf("crystal clash", "ironhold", "ironprison", "energy hub", "crystal prison", "kristallkonflikt", "kristallkampf", "kristal çatışması", "kristal savaşı", "кристальная битва", "кристальное столкновение", "水晶冲突", "水晶争夺"),
        "sandsea_conquest" to listOf("sandsea", "vessel station", "lighthouse", "phare", "fuel", "sandmeer", "schiffsstation", "leuchtturm", "kum denizi", "gemi istasyonu", "deniz feneri", "песчаное море", "станция судов", "маяк", "沙海", "船只站", "灯塔"),
        "svs" to listOf("supreme honor", "svs", "war preparation", "préparation de guerre", "kriegsvorbereitung", "kriegsvorbereitungen", "savaş hazırlığı", "savaş hazırlıkları", "подготовка к войне", "战前准备", "战争准备"),
        "turmoil_surge" to listOf("turmoil", "sentry turret", "wave", "vague", "chaoswelle", "geschützturm", "dalga", "nöbetçi kulesi", "волна хаоса", "сторожeвая турель", "混乱浪潮", "哨戒炮塔"),
        "gold_hunter" to listOf("gold hunter", "hunter points", "goldjäger", "jägerpunkte", "altın avcısı", "avcı puanları", "золотой охотник", "очки охотника", "黄金猎人", "猎人积分"),
        "robots" to listOf("robot", "robots", "droid", "drone", "robotics", "robot center", "robot upgrade", "musashimaru", "phantom cat", "ranger", "infercore", "hercules", "halo", "light cone", "atlax", "yokozuna", "bastion", "pluto", "dauntless", "valiant", "scorpio"),
        "power_armor" to listOf("power armor hangar", "power armor", "prisoner armor", "prisoner armor data", "armor data", "solo module", "formation module", "barrier module", "core i", "core ii", "core iii", "core iv", "core v", "noyau solo", "module formation", "module barrière", "armure de prisonnier", "hangar d'armure", "动力装甲", "囚犯装甲", "барьерный модуль"),
        "forticlad_lab" to listOf("forticlad lab", "forticlad laboratory", "laboratoire forticlad", "forticlad technology", "hyperalloy", "hyperalliage", "technologie forticlad", "forticlad çekirdeği", "超合金", "forticlad 实验室"),
        "lab" to listOf("lab", "logistics technology", "economy technology", "military technology", "technologies logistique", "technologie économie", "technologie militaire", "logistiktechnologie", "wirtschaftstechnologie", "militärtechnologie", "lojistik teknolojisi", "ekonomi teknolojisi", "askeri teknoloji", "технология логистики", "технология экономики", "военная технология", "后勤科技", "经济科技", "军事科技"),
        "cosmos_hub" to listOf("cosmos hub", "cosmic core", "orbital core", "centre cosmique", "noyau cosmique", "космическое ядро", "宇宙中心", "宇宙核心"),
        "dungeon" to listOf("dungeon office", "surveillance room", "dungeon", "felon", "felons", "fugitive", "fugitives", "criminal prisoner", "criminal prison", "bureau du donjon", "salle de surveillance", "criminel", "criminels", "fugitif", "prisonnier criminel", "zindan", "kaçak", "suçlu", "подземелье", "преступник", "беглец", "地牢", "罪犯", "逃犯"),
        "equipements" to listOf("hero equipment", "equipment upgrade", "equipment improvement", "equipment", "gear", "arsenal", "equipment exp", "equipment part", "potential coil", "precision gear", "magnet", "exclusive equipment", "exclusive gear", "équipement", "équipements", "équipement du héros", "équipement héros", "équipement exclusif", "amélioration d'équipement", "amélioration équipement", "aimant", "arsenal", "ausrüstung", "heldenausrüstung", "ausrüstungsverbesserung", "ausrüstung verbessern", "exklusive ausrüstung", "magnet", "ekipman", "kahraman ekipmani", "ekipman geliştirme", "özel ekipman", "teçhizat", "mıknatıs", "снаряжение", "улучшение снаряжения", "экипировка", "эксклюзивное снаряжение", "магнит", "装备", "装备升级", "英雄装备", "专属装备", "磁铁", "equipaggiamento", "equipaggiamento eroe", "miglioramento equipaggiamento", "equipaggiamento esclusivo", "magnete", "bobina potenziale", "ingranaggio di precisione"),
        "heros" to listOf("heroes", "hero", "héros", "skills", "compétences", "stars", "étoiles", "helden", "fähigkeiten", "sterne", "kahraman", "kahramanlar", "beceriler", "yıldızlar", "герои", "навыки", "звёзды", "英雄", "技能", "星级"),
        "batiments" to listOf("building", "upgrade", "bâtiment", "construction", "warden office", "warden's office", "director's office", "bureau du directeur", "barracks", "caserne", "laboratory", "laboratoire", "infirmary", "infirmerie", "warehouse", "entrepôt", "kitchen", "cuisine", "yard", "cour", "cell", "cellule", "comms", "gebäude", "ausbau", "bina", "yükseltme", "здание", "улучшение", "建筑", "升级"),
        "troupes" to listOf("shieldbearer", "shieldbearers", "bomber", "bombers", "shooter", "shooters", "troops", "troupes", "march capacity", "capacité de marche", "truppen", "birlikler", "войска", "部队"),
        "ressources" to listOf("food", "wood", "steel", "gasoline", "nourriture", "bois", "acier", "essence", "diamonds", "diamants", "gems", "gemmes", "gold", "nahrung", "holz", "stahl", "benzin", "diamanten", "yiyecek", "odun", "çelik", "elmas", "altın", "еда", "дерево", "сталь", "бензин", "алмазы", "золото", "食物", "木材", "钢材", "汽油", "钻石", "宝石", "金币", "黄金", "cibo", "legno", "acciaio", "benzina", "diamanti", "oro"),

        "satellites" to listOf("satellite", "satellites", "satellite launch", "data disk", "data disks", "planet coin", "overclock", "ascension", "satellit", "satelliten", "uydu", "спутник", "спутники", "卫星", "卫星发射", "数据盘", "行星币"),
        "haven_island" to listOf("haven island", "mansion", "decorations", "treasure cube", "island prosperity", "île havre", "manoir", "décorations", "insel", "malikane", "dekorasyon", "убежище", "особняк", "避风岛", "豪宅", "装饰", "宝藏魔方"),
        "arrest" to listOf("arrest stage", "arrest level", "felon", "felons", "contraband search", "criminal prisoner", "arrest battle", "tutuklama", "tutuklu", "арест", "заключенный", "罪犯", "逮捕", "通缉犯", "违禁品搜查"),
        "trove" to listOf("warden's trove", "wardens trove", "collection piece", "collection pieces", "tomes", "treasure collection", "trésor du directeur", "schatzkammer", "hazine", "сокровищница", "典籍", "收藏品"),
        "alliance" to listOf("alliance technology", "alliance territory", "alliance shop", "alliance level", "alliance banner", "alliance chest", "guild", "allianz", "gilde", "ittifak", "альянс", "технология альянса", "联盟科技", "联盟领地", "联盟商店"),
        "items" to listOf("item details", "item source", "item inventory", "inventory", "chest contents", "drop rate", "objets", "inventaire", "gegenstand", "inventar", "eşya", "envanter", "предмет", "инвентарь", "物品", "背包", "道具", "掉落率"),
        "profil" to listOf("power", "puissance", "eliminations", "éliminations", "profile", "profil", "macht", "güç", "мощь", "профиль", "战力", "档案")
    )

    fun analyze(rawText: String): ScreenRecognitionResult {
        val normalized = normalize(rawText)
        val paddedText = " $normalized "
        val ranked = dictionaries.mapNotNull { (category, labels) ->
            // Comparaison par mots/expressions entiers : « shieldbearers »
            // ne doit pas compter simultanément comme « shieldbearer ».
            val matchedLabels = labels.filter { label ->
                val normalizedLabel = normalize(label)
                val isMatch = normalizedLabel.isNotEmpty() && when {
                    // Les libellés chinois sont souvent concaténés sans espaces dans l'OCR.
                    normalizedLabel.any { it.code in 0x3400..0x9FFF || it.code in 0xF900..0xFAFF } ->
                        normalized.contains(normalizedLabel)
                    else -> paddedText.contains(" $normalizedLabel ")
                }
                // « Lab » seul est trop générique (titre, bouton ou fragment OCR).
                // On ne l'accepte que si au moins une technologie du laboratoire
                // est également lisible dans le même texte.
                val labHasResearchContext = label != "lab" || labels.any { contextLabel ->
                    contextLabel != "lab" && when {
                        normalize(contextLabel).any {
                            it.code in 0x3400..0x9FFF || it.code in 0xF900..0xFAFF
                        } -> normalized.contains(normalize(contextLabel))
                        else -> paddedText.contains(" ${normalize(contextLabel)} ")
                    }
                }
                isMatch && (category != "lab" || labHasResearchContext)
            }.distinct()
            // Un libellé court inclus dans une expression reconnue ne constitue
            // pas un indice indépendant (ex. « robot » dans « robot center »).
            val hits = matchedLabels.filterNot { candidate ->
                val candidateNormalized = normalize(candidate)
                matchedLabels.any { other ->
                    val otherNormalized = normalize(other)
                    val containsCjk = otherNormalized.any {
                        it.code in 0x3400..0x9FFF || it.code in 0xF900..0xFAFF
                    }
                    otherNormalized != candidateNormalized && if (containsCjk) {
                        otherNormalized.contains(candidateNormalized)
                    } else {
                        " $otherNormalized ".contains(" $candidateNormalized ")
                    }
                }
            }
            if (hits.isEmpty()) null else category to hits
        }.sortedByDescending { it.second.size }

        // Le titre explicite « Cosmos Hub » prime sur les libellés génériques
        // Satellite / Data Disk présents dans certaines vues du hub.
        val best = if (paddedText.contains(" cosmos hub ")) {
            ranked.firstOrNull { it.first == "cosmos_hub" } ?: ranked.firstOrNull()
        } else {
            ranked.firstOrNull()
        }
        val second = ranked.firstOrNull { it.first != best?.first }
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
            alternativeCategories = ranked.filter { it.first != best?.first }.take(2).map { it.first },
            // Même plusieurs libellés concordants ne constituent pas une
            // validation visuelle : ce score est heuristique, non calibré.
            needsVisualConfirmation = best != null
        )
    }

    private fun normalize(value: String): String =
        java.text.Normalizer.normalize(value.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()
}
