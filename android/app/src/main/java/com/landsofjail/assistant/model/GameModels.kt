package com.landsofjail.assistant.model

import kotlinx.serialization.Serializable

enum class AppLanguage(val code: String, val label: String, val wikiPrefix: String) {
    FR("fr", "FR", "/fr"),
    TR("tr", "TR", "/tr"),
    RU("ru", "RU", "/ru"),
    ZH("zh-CN", "ZH", "/zh-cn"),
    DE("de", "DE", "/de"),
    EN("en", "EN", "")
}

enum class PlayerRole(val labelFr: String, val descriptionFr: String) {
    LEADER_RALLIEMENT("Leader de ralliement", "Optimise la capacité de ralliement et les dégâts de groupe."),
    ASSAUT_DPS("Assaut / DPS", "Maximise les dégâts bruts en escarmouche et duel."),
    DEFENSEUR_GARNISON("Défenseur / Garnison", "Protège la base avec tanks, boucliers et contre-attaques."),
    SOUTIEN_CONTROLE("Soutien / Contrôle", "Soins, réductions de dégâts et effets de contrôle."),
    POLYVALENT_F2P("Polyvalent / F2P", "Progression équilibrée et gestion stricte des ressources.")
}

enum class AdviceMode(
    val apiId: String,
    val searchKeyword: String,
    val wikiSlugs: List<String>,
    val labelFr: String,
    val labelTr: String,
    val labelRu: String,
    val labelZh: String,
    val labelDe: String,
    val labelEn: String
) {
    TRIAL_CAGE(
        apiId = "trial_cage",
        searchKeyword = "cage",
        wikiSlugs = listOf("/events/ultimate-trial/", "/guides/ultimate-trial/", "/events/"),
        labelFr = "Trial Cage",
        labelTr = "Deneme Kafesi",
        labelRu = "Испытание клетки",
        labelZh = "终极试炼",
        labelDe = "Trial Cage",
        labelEn = "Trial Cage"
    ),
    CRYSTAL_CLASH(
        apiId = "crystal_clash",
        searchKeyword = "crystal clash",
        wikiSlugs = listOf("/events/crystal-clash/", "/events/"),
        labelFr = "Crystal Clash",
        labelTr = "Kristal Çatışması",
        labelRu = "Кристальная схватка",
        labelZh = "水晶争夺战",
        labelDe = "Kristallkampf",
        labelEn = "Crystal Clash"
    ),
    SANDSEA_CONQUEST(
        apiId = "sandsea_conquest",
        searchKeyword = "sandsea",
        wikiSlugs = listOf("/events/sandsea-conquest/", "/events/"),
        labelFr = "Conquête de sable",
        labelTr = "Kum Denizi Fethi",
        labelRu = "Песчаное море",
        labelZh = "沙海征服",
        labelDe = "Sandmeer",
        labelEn = "Sandsea Conquest"
    ),
    SVS(
        apiId = "svs",
        searchKeyword = "svs",
        wikiSlugs = listOf("/events/supreme-honor/", "/events/"),
        labelFr = "SvS / Honneur",
        labelTr = "SvS / Yüce Onur",
        labelRu = "SvS / Высшее отличие",
        labelZh = "跨服战 SvS",
        labelDe = "SvS / Ehre",
        labelEn = "SvS / Supreme Honor"
    ),
    TURMOIL_SURGE(
        apiId = "turmoil_surge",
        searchKeyword = "turmoil",
        wikiSlugs = listOf("/events/turmoil-surge/", "/events/"),
        labelFr = "Turmoil Surge",
        labelTr = "Kargaşa Dalgası",
        labelRu = "Волна смуты",
        labelZh = "动乱危机",
        labelDe = "Turmoil Surge",
        labelEn = "Turmoil Surge"
    ),
    GORILLA_LEGION(
        apiId = "gorilla_legion",
        searchKeyword = "gorilla legion",
        wikiSlugs = listOf("/events/gorilla-legion/", "/events/"),
        labelFr = "Gorilla Legion",
        labelTr = "Goril Lejyonu",
        labelRu = "Легион горилл",
        labelZh = "大猩猩军团",
        labelDe = "Gorilla-Legion",
        labelEn = "Gorilla Legion"
    ),
    GOLD_HUNTER(
        apiId = "gold_hunter",
        searchKeyword = "gold hunter",
        wikiSlugs = listOf("/events/gold-hunter/", "/guides/gold-hunter/", "/events/"),
        labelFr = "Gold Hunter",
        labelTr = "Altın Avcısı",
        labelRu = "Охотник за золотом",
        labelZh = "黄金猎人",
        labelDe = "Goldjäger",
        labelEn = "Gold Hunter"
    );

    fun titleFor(lang: AppLanguage): String = when (lang) {
        AppLanguage.FR -> labelFr
        AppLanguage.TR -> labelTr
        AppLanguage.RU -> labelRu
        AppLanguage.ZH -> labelZh
        AppLanguage.DE -> labelDe
        AppLanguage.EN -> labelEn
    }

    fun briefFor(lang: AppLanguage): String = when (this) {
        TRIAL_CAGE -> when (lang) {
            AppLanguage.FR -> "Objectif : maximiser les dégâts infligés. Priorisez attaque, létalité, dégâts de compétence, réduction de défense ennemie et attaques de zone. Les bonus défensifs ne sont pas prioritaires s’ils n’augmentent pas les dégâts."
            AppLanguage.TR -> "Ön safa Shieldbearers, arkaya Shooters koyun. Ralli Lideri ile Katılımcı becerilerini ayırın."
            AppLanguage.RU -> "Ставьте Shieldbearers вперёд, а Shooters назад. Разделяйте героев лидера и участников."
            AppLanguage.ZH -> "前排放置盾兵英雄，后排放置射手输出，区分集结队长与跟车规则。"
            AppLanguage.DE -> "Shieldbearers vorne, Shooters mit DPS hinten. Rally-Leader und Joiner trennen."
            AppLanguage.EN -> "Place Shieldbearers frontline and Shooters DPS backline. Separate Rally Leaders from Joiners (4-skill rule)."
        }
        CRYSTAL_CLASH -> when (lang) {
            AppLanguage.FR -> "Capturez Crystal Prisons, Ironholds & Energy Hubs en Phase 1, poussez Central Ironprison en Phase 2 et utilisez les fenêtres de soin (~12 min)."
            AppLanguage.TR -> "1. Aşamada Crystal Prisons ve Energy Hubs alın, 2. Aşamada Central Ironprison’a yüklenin ve iyileşme sürelerini kullanın."
            AppLanguage.RU -> "В 1-й фазе берите Crystal Prisons и Energy Hubs, во 2-й — Central Ironprison, используя окна лечения."
            AppLanguage.ZH -> "第1阶段夺取Crystal Prisons与Energy Hubs，第2阶段强攻Central Ironprison并利用治疗窗口。"
            AppLanguage.DE -> "Phase 1 Crystal Prisons & Energy Hubs sichern, Phase 2 Central Ironprison angreifen, Heilzyklen nutzen."
            AppLanguage.EN -> "Secure Crystal Prisons, Ironholds & Energy Hubs in Phase 1, push Central Ironprison in Phase 2, use 12-min healing cycles."
        }
        SANDSEA_CONQUEST -> when (lang) {
            AppLanguage.FR -> "Économisez le Carburant (Fuel) sur les routes, protégez les Vessel Stations, capturez les Phares et poussez vers S100 avec le Tacticien."
            AppLanguage.TR -> "Yakıtı (Fuel) koruyun, Gemi İstasyonlarını savunun, Deniz Fenerlerini alın ve Taktikçi ile S100'e ilerleyin."
            AppLanguage.RU -> "Экономьте топливо, защищайте станции судов, захватывайте маяки и атакуйте S100 с тактиком."
            AppLanguage.ZH -> "节省队伍燃料，保护船坞站点，占领灯塔，配合战术官推进至S100。"
            AppLanguage.DE -> "Treibstoff sparen, Vessel Stations schützen, Leuchttürme einnehmen und koordiniert auf S100 vorrücken."
            AppLanguage.EN -> "Save squad Fuel on sand roads, protect Vessel Stations, capture Lighthouses and coordinate pushes toward S100."
        }
        SVS -> when (lang) {
            AppLanguage.FR -> "Optimisez la puissance avant le matchmaking, gardez accélérations & éclats pour la Préparation, puis défendez/attaquez en Phase Guerre."
            AppLanguage.TR -> "Eşleşme öncesi gücü düzenleyin, hızlandırma ve parçaları Hazırlık Aşamasına saklayın, Savaşta saldırın/savunun."
            AppLanguage.RU -> "Оптимизируйте силу до подбора, копите ускорения и осколки к фазе подготовки, затем воюйте в фазе войны."
            AppLanguage.ZH -> "匹配前优化战力，加速道具和碎片留给准备阶段，战争阶段协同攻防。"
            AppLanguage.DE -> "Macht vor Matchmaking prüfen, Beschleuniger/Splitter für Vorbereitung sparen, dann Kriegsphase."
            AppLanguage.EN -> "Manage matchmaking power, save speedups & hero shards for Preparation Phase, then fight in War Phase."
        }
        TURMOIL_SURGE -> when (lang) {
            AppLanguage.FR -> "20 vagues (~40 min) : les vagues 10 & 20 visent les Tourelles (Sentry Turrets), et les vagues 7, 14 & 17 ciblent les joueurs en ligne. Échangez vos garnisons."
            AppLanguage.TR -> "20 dalga: 10. ve 20. dalgalar Kulelere, 7/14/17. dalgalar aktif oyunculara saldırır. Müttefiklerle garnizon takası yapın."
            AppLanguage.RU -> "20 волн: 10 и 20 бьют по турелям, 7/14/17 — по игрокам онлайн. Обменивайтесь подкреплениями с союзниками."
            AppLanguage.ZH -> "20波进攻：第10、20波攻击炮塔，第7、14、17波攻击在线玩家，与盟友互换驻防。"
            AppLanguage.DE -> "20 Wellen: 10 & 20 treffen Sentry Turrets; 7, 14 & 17 aktive Spieler. Garnisonen mit Verbündeten tauschen."
            AppLanguage.EN -> "20 waves (~40 min): waves 10 & 20 hit Sentry Turrets; Frenzy waves 7, 14 & 17 hit online players. Swap ally garrisons."
        }
        GORILLA_LEGION -> when (lang) {
            AppLanguage.FR -> "Boss individuel à dégâts : vérifiez le gorille et son bonus actif. Commencez par 0/15/85 (Shield/Bomber/Shooter), puis comparez 0/0/100 ou le ratio du guide du jour. Les sources divergent : testez à héros, équipement et effectif constants."
            AppLanguage.TR -> "Bireysel hasar bossu: goril türünü ve aktif bonusu doğrulayın. 0/15/85 ile başlayıp 0/0/100 veya günlük oranla karşılaştırın."
            AppLanguage.RU -> "Индивидуальный босс на урон: проверьте гориллу и бонус. Начните с 0/15/85, сравните с 0/0/100 или дневным вариантом."
            AppLanguage.ZH -> "个人伤害首领：确认大猩猩类型和当天加成。先测0/15/85，再与0/0/100或当日比例比较。"
            AppLanguage.DE -> "Einzelboss: Gorilla und Tagesbonus prüfen. Mit 0/15/85 starten und 0/0/100 oder Tagesverhältnis vergleichen."
            AppLanguage.EN -> "Individual damage boss: verify gorilla and daily bonus. Start at 0/15/85, then compare 0/0/100 or the day-specific ratio."
        }
        GOLD_HUNTER -> when (lang) {
            AppLanguage.FR -> "Réservez éclats, livres de compétences, matériaux d'Arsenal et gros bâtiments exclusivement pour les paliers d'événement."
            AppLanguage.TR -> "Parçaları, beceri kitaplarını, ekipman malzemelerini ve büyük yükseltmeleri sadece etkinlik günlerinde harcayın."
            AppLanguage.RU -> "Храните осколки, книги навыков, материалы арсенала и крупные стройки для этапов события."
            AppLanguage.ZH -> "将英雄碎片、技能书、军械材料与大型建筑升级留到活动窗口再集中使用。"
            AppLanguage.DE -> "Splitter, Skillbücher, Ausrüstungsmaterialien und große Ausbauten für Event-Tage aufsparen."
            AppLanguage.EN -> "Save hero shards, skill books, Arsenal materials and long upgrades exclusively for event milestone days."
        }
    }
}

@Serializable
data class HeroSkill(
    val nom: String,
    val niveau: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val incertain: Boolean = false,
    val raisonIncertitude: String? = null
)

@Serializable
data class HeroRecord(
    val nom: String,
    val niveau: Int? = null,
    val niveauTexte: String,
    val rarete: String? = null,
    val etoiles: String? = null,
    val roleHero: String? = null,
    val puissanceHero: String? = null,
    val imageUrl: String? = null,
    val avatarUrl: String? = null,
    val titre: String? = null,
    val typeTroupe: String? = null,
    val categorie: String? = null,
    val saison: String? = null,
    val puissanceMax5Etoiles: String? = null,
    val resumeBio: String? = null,
    val idealPour: String? = null,
    val wikiUrl: String? = null,
    val competences: List<HeroSkill> = emptyList(),
    val incertain: Boolean = false,
    val raisonIncertitude: String? = null
)

@Serializable
data class HeroCatalogEntry(
    val slug: String,
    val nom: String,
    val avatarUrl: String? = null,
    val imageUrl: String? = null,
    val rarete: String? = null,
    val typeTroupe: String? = null,
    val categorie: String? = null,
    val wikiUrl: String = ""
)

@Serializable
data class HeroWikiDetails(
    val slug: String,
    val nom: String,
    val titre: String? = null,
    val avatarUrl: String? = null,
    val imageUrl: String? = null,
    val rarete: String? = null,
    val typeTroupe: String? = null,
    val categorie: String? = null,
    val saison: String? = null,
    val puissanceMax5Etoiles: String? = null,
    val resumeBio: String? = null,
    val idealPour: String? = null,
    val wikiUrl: String = "",
    val competences: List<HeroSkill> = emptyList()
)

@Serializable
data class HeroCatalogResponse(val heroes: List<HeroCatalogEntry> = emptyList())

@Serializable
data class HeroDetailsResponse(val details: List<HeroWikiDetails> = emptyList())

@Serializable
data class RobotSkill(
    val nom: String,
    val niveau: String = "Inconnu",
    val description: String? = null,
    val incertain: Boolean = false,
    val raisonIncertitude: String? = null
)

@Serializable
data class RobotRecord(
    val nom: String,
    val niveau: String? = null,
    val puissance: String? = null,
    val rarete: String? = null,
    val competences: List<RobotSkill> = emptyList(),
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
    val id: String = "",
    val analyzedAtIso: String = "",
    val imageUriString: String = "",
    val typeCapture: String = "autre",
    val resumeCapture: String = "",
    val puissance: PowerMetric? = null,
    val ressources: List<ResourceRecord> = emptyList(),
    val heros: List<HeroRecord> = emptyList(),
    val robots: List<RobotRecord> = emptyList(),
    val champsIncertains: List<UncertainItem> = emptyList()
)

@Serializable
data class GroundedWebSource(
    val title: String,
    val url: String,
    val detectedDate: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class AdviceRecommendation(
    val titre: String,
    val detail: String,
    val herosCibles: List<String> = emptyList(),
    val incertain: Boolean = false,
    val raisonIncertitude: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class TacticalAdviceResult(
    val mode: String = "",
    val language: String? = null,
    val generatedAt: String = "",
    val generatedAtIso: String = "",
    val synthese: String = "",
    val compositionRecommandee: String = "",
    val robotRecommande: String? = null,
    val ratioTroupes: String? = null,
    val recommandations: List<AdviceRecommendation> = emptyList(),
    val informationsIncertaines: List<String> = emptyList(),
    val sources: List<GroundedWebSource> = emptyList(),
    val geminiWarning: String? = null
)


@Serializable
data class SavedPlayerProfile(
    val id: String,
    val name: String,
    val languageCode: String = "fr",
    val playerRoleName: String = "POLYVALENT_F2P",
    val analyses: List<ScreenshotAnalysis> = emptyList(),
    val heroes: List<HeroRecord> = emptyList(),
    val robots: List<RobotRecord> = emptyList(),
    val resources: List<ResourceRecord> = emptyList(),
    val currentPower: PowerMetric? = null,
    val gorillaRuns: List<GorillaDamageRecord> = emptyList()
)

@Serializable
data class GorillaDamageRecord(
    val id: String,
    val date: String,
    val gorilla: String,
    val damage: Long,
    val shield: Int,
    val bomber: Int,
    val shooter: Int,
    val heroes: String = "",
    val robot: String = "",
    val notes: String = "",
    val bonus: String = ""
)
