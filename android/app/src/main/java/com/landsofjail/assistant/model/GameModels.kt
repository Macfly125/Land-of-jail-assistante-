package com.landsofjail.assistant.model

import kotlinx.serialization.Serializable

enum class AppLanguage(val code: String, val label: String, val wikiPrefix: String) {
    FR("fr", "FR", "/fr"),
    TR("tr", "TR", "/tr"),
    RU("ru", "RU", "/ru"),
    ZH("zh-CN", "ZH", "/zh-cn"),
    DE("de", "DE", "/de"),
    EN("en", "EN", ""),
    IT("it", "IT", "/it")
}

enum class PlayerRole(val labelFr: String, val descriptionFr: String) {
    LEADER_RALLIEMENT("Leader de ralliement", "Optimise la capacité de ralliement et les dégâts de groupe."),
    ASSAUT_DPS("Assaut / DPS", "Maximise les dégâts bruts en escarmouche et duel."),
    DEFENSEUR_GARNISON("Défenseur / Garnison", "Protège la base avec tanks, boucliers et contre-attaques."),
    SOUTIEN_CONTROLE("Soutien / Contrôle", "Soins, réductions de dégâts et effets de contrôle."),
    POLYVALENT_F2P("Polyvalent / F2P", "Progression équilibrée et gestion stricte des ressources.");

    fun labelFor(language: AppLanguage): String = when (this) {
        LEADER_RALLIEMENT -> when (language) {
            AppLanguage.FR -> labelFr
            AppLanguage.EN -> "Rally Leader"
            AppLanguage.IT -> "Capo della spedizione"
            AppLanguage.DE -> "Rallye-Anführer"
            AppLanguage.TR -> "Ralli Lideri"
            AppLanguage.RU -> "Лидер сбора"
            AppLanguage.ZH -> "集结队长"
        }
        ASSAUT_DPS -> when (language) {
            AppLanguage.FR -> labelFr
            AppLanguage.EN -> "Assault / DPS"
            AppLanguage.IT -> "Assalto / DPS"
            AppLanguage.DE -> "Angriff / DPS"
            AppLanguage.TR -> "Taarruz / DPS"
            AppLanguage.RU -> "Штурм / урон"
            AppLanguage.ZH -> "突击 / 输出"
        }
        DEFENSEUR_GARNISON -> when (language) {
            AppLanguage.FR -> labelFr
            AppLanguage.EN -> "Defender / Garrison"
            AppLanguage.IT -> "Difensore / Guarnigione"
            AppLanguage.DE -> "Verteidiger / Garnison"
            AppLanguage.TR -> "Savunma / Garnizon"
            AppLanguage.RU -> "Защита / гарнизон"
            AppLanguage.ZH -> "防守 / 驻防"
        }
        SOUTIEN_CONTROLE -> when (language) {
            AppLanguage.FR -> labelFr
            AppLanguage.EN -> "Support / Control"
            AppLanguage.IT -> "Supporto / Controllo"
            AppLanguage.DE -> "Unterstützung / Kontrolle"
            AppLanguage.TR -> "Destek / Kontrol"
            AppLanguage.RU -> "Поддержка / контроль"
            AppLanguage.ZH -> "支援 / 控制"
        }
        POLYVALENT_F2P -> when (language) {
            AppLanguage.FR -> labelFr
            AppLanguage.EN -> "Versatile / F2P"
            AppLanguage.IT -> "Versatile / F2P"
            AppLanguage.DE -> "Vielseitig / F2P"
            AppLanguage.TR -> "Çok yönlü / F2P"
            AppLanguage.RU -> "Универсальный / F2P"
            AppLanguage.ZH -> "多面手 / F2P"
        }
    }

    fun descriptionFor(language: AppLanguage): String = when (this) {
        LEADER_RALLIEMENT -> when (language) {
            AppLanguage.FR -> descriptionFr
            AppLanguage.EN -> "Optimizes rally capacity and group damage."
            AppLanguage.IT -> "Ottimizza la capienza delle spedizioni e i danni di gruppo."
            AppLanguage.DE -> "Optimiert Rallye-Kapazität und Gruppenschaden."
            AppLanguage.TR -> "Ralli kapasitesini ve grup hasarını artırır."
            AppLanguage.RU -> "Повышает вместимость сбора и групповой урон."
            AppLanguage.ZH -> "提升集结容量与团队伤害。"
        }
        ASSAUT_DPS -> when (language) {
            AppLanguage.FR -> descriptionFr
            AppLanguage.EN -> "Maximizes raw damage in skirmishes and duels."
            AppLanguage.IT -> "Massimizza i danni diretti negli scontri e nei duelli."
            AppLanguage.DE -> "Maximiert direkten Schaden in Gefechten und Duellen."
            AppLanguage.TR -> "Çatışma ve düellolarda ham hasarı en üst düzeye çıkarır."
            AppLanguage.RU -> "Максимизирует прямой урон в стычках и дуэлях."
            AppLanguage.ZH -> "最大化遭遇战与决斗中的直接伤害。"
        }
        DEFENSEUR_GARNISON -> when (language) {
            AppLanguage.FR -> descriptionFr
            AppLanguage.EN -> "Protects the base with tanks, shields and counterattacks."
            AppLanguage.IT -> "Protegge la base con unità resistenti, scudi e contrattacchi."
            AppLanguage.DE -> "Schützt die Basis mit Tanks, Schilden und Gegenangriffen."
            AppLanguage.TR -> "Tanklar, kalkanlar ve karşı saldırılarla üssü korur."
            AppLanguage.RU -> "Защищает базу танками, щитами и контратаками."
            AppLanguage.ZH -> "利用坦克、护盾和反击保护基地。"
        }
        SOUTIEN_CONTROLE -> when (language) {
            AppLanguage.FR -> descriptionFr
            AppLanguage.EN -> "Provides healing, damage reduction and control effects."
            AppLanguage.IT -> "Fornisce cure, riduzione dei danni ed effetti di controllo."
            AppLanguage.DE -> "Bietet Heilung, Schadensminderung und Kontrolleffekte."
            AppLanguage.TR -> "İyileştirme, hasar azaltma ve kontrol etkileri sağlar."
            AppLanguage.RU -> "Обеспечивает лечение, снижение урона и контроль."
            AppLanguage.ZH -> "提供治疗、减伤和控制效果。"
        }
        POLYVALENT_F2P -> when (language) {
            AppLanguage.FR -> descriptionFr
            AppLanguage.EN -> "Balances progression with strict resource management."
            AppLanguage.IT -> "Bilancia la progressione con una gestione rigorosa delle risorse."
            AppLanguage.DE -> "Gleicht den Fortschritt durch striktes Ressourcenmanagement aus."
            AppLanguage.TR -> "Dengeli ilerleme ve sıkı kaynak yönetimi sağlar."
            AppLanguage.RU -> "Обеспечивает сбалансированное развитие и строгую экономию ресурсов."
            AppLanguage.ZH -> "平衡发展并严格管理资源。"
        }
    }
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
    ),
    GORILLA_LEGION(
        apiId = "gorilla_legion",
        searchKeyword = "gorilla legion",
        wikiSlugs = listOf("/events/gorilla-legion/", "/events/"),
        labelFr = "Légion de Gorilla",
        labelTr = "Goril Lejyonu",
        labelRu = "Легион горилл",
        labelZh = "大猩猩军团",
        labelDe = "Gorilla-Legion",
        labelEn = "Gorilla Legion"
    );

    fun titleFor(lang: AppLanguage): String = when (lang) {
        AppLanguage.FR -> labelFr
        AppLanguage.TR -> labelTr
        AppLanguage.RU -> labelRu
        AppLanguage.ZH -> labelZh
        AppLanguage.DE -> labelDe
        AppLanguage.EN -> labelEn
        AppLanguage.IT -> labelEn
    }

    fun briefFor(lang: AppLanguage): String = when (this) {
        TRIAL_CAGE -> when (lang) {
            AppLanguage.FR -> "Cage = course aux dégâts. Ratios de départ (Shieldbearers / Bombers / Shooters) : chef sans Ada 0/10/90 ; chef avec Ada 1/9/90 (environ 1 000 Shieldbearers selon la taille de marche) ; participant mixte 0/10/90 ou 0/0/100 en Shooters. Le participant envoie un seul héros : seule sa première compétence War compte, dans la limite de 4 compétences de participants. Prioriser ATQ, létalité, dégâts et baisse de DEF ennemie ; ne pas choisir des bonus PV/DEF/survie pour le score. Vérifier les bonus réellement appliqués et comparer les rapports : ratios de départ, pas garantie d'optimalité."
            AppLanguage.TR -> "Kafes hasar yarışıdır. Başlangıç oranları (Shieldbearer / Bomber / Shooter): Ada'sız lider 0/10/90; Ada lider 1/9/90 (kol yürüyüşüne göre yaklaşık 1.000 Shieldbearer); karışık birlikle katılımcı 0/10/90 veya yalnız Shooter 0/0/100. Katılımcı tek kahraman gönderir; yalnızca ilk War becerisi sayılır ve en fazla 4 katılımcı becerisi uygulanır. ATK, öldürücülük, hasar ve düşman DEF azaltımını önceliklendirin; puan için HP/DEF/sağkalım bonuslarını seçmeyin. Uygulanan bonusları ve savaş raporlarını kontrol edin; bunlar başlangıç oranlarıdır."
            AppLanguage.RU -> "Клетка — гонка урона. Стартовые доли (Shieldbearer / Bomber / Shooter): лидер без Ada 0/10/90; лидер с Ada 1/9/90 (около 1 000 Shieldbearer в зависимости от размера марша); участник со смешанными войсками 0/10/90 или 0/0/100 только Shooter. Участник отправляет одного героя: учитывается только его первый War-навык, максимум 4 навыка участников. Приоритет — ATK, летальность, урон и снижение DEF врага; бонусы HP/DEF/выживаемости не выбирайте ради очков урона. Проверяйте применённые бонусы и боевые отчёты; это стартовые, а не гарантированно лучшие доли."
            AppLanguage.ZH -> "试炼之笼是伤害竞赛。起始兵种比例（Shieldbearer / Bomber / Shooter）：无 Ada 队长 0/10/90；Ada 队长 1/9/90（按行军规模约 1,000 名 Shieldbearer）；混合兵种队员 0/10/90，纯 Shooter 队员 0/0/100。队员只派一名英雄，且只计算其第一项 War 技能，最多计入 4 个队员技能。优先攻击、杀伤、伤害和降低敌方防御；不要为了伤害分数选择生命/防御/生存加成。请核对实际生效加成并比较战报；这些只是起始比例。"
            AppLanguage.DE -> "Trial Cage ist ein Schadensrennen. Startverhältnisse (Shieldbearer / Bomber / Shooter): Anführer ohne Ada 0/10/90; mit Ada 1/9/90 (etwa 1.000 Shieldbearer abhängig von der Marschgröße); gemischter Joiner 0/10/90 oder nur Shooter 0/0/100. Joiner senden nur einen Helden; nur dessen erste War-Fähigkeit zählt, maximal vier Joiner-Fähigkeiten. ATK, Letalität, Schaden und gegnerische DEF-Senkung priorisieren; HP/DEF/Überleben nicht für den Schadenswert wählen. Tatsächlich angewendete Boni und Kampfberichte prüfen; Startwerte, keine Garantie."
            AppLanguage.EN -> "Trial Cage is a damage race. Starting ratios (Shieldbearer / Bomber / Shooter): leader without Ada 0/10/90; leader with Ada 1/9/90 (about 1,000 Shieldbearers depending on march size); mixed joiner 0/10/90 or Shooter-only 0/0/100. A joiner sends one hero: only that hero's first War skill counts, up to four joiner skills. Prioritize ATK, Lethality, damage and enemy DEF reduction; do not pick HP/DEF/survival buffs for the damage score. Verify applied bonuses and compare battle reports; these are starting ratios, not guaranteed optimal."
            AppLanguage.IT -> "La Gabbia di prova è una gara di danni. Rapporti iniziali (Shieldbearer / Bomber / Shooter): capo senza Ada 0/10/90; capo con Ada 1/9/90 (circa 1.000 Shieldbearer in base alla dimensione della marcia); partecipante misto 0/10/90 o solo Shooter 0/0/100. Ogni partecipante invia un eroe: conta solo la sua prima abilità War, fino a quattro abilità dei partecipanti. Dai priorità ad ATK, letalità, danni e riduzione della DEF nemica; non scegliere bonus HP/DEF/sopravvivenza per il punteggio danni. Verifica i bonus applicati e confronta i rapporti di battaglia: sono rapporti iniziali, non una garanzia di ottimalità."
        }
        CRYSTAL_CLASH -> when (lang) {
            AppLanguage.FR -> "Capturez Crystal Prisons, Ironholds & Energy Hubs en Phase 1, poussez Central Ironprison en Phase 2 et utilisez les fenêtres de soin (~12 min)."
            AppLanguage.TR -> "1. Aşamada Crystal Prisons ve Energy Hubs alın, 2. Aşamada Central Ironprison’a yüklenin ve iyileşme sürelerini kullanın."
            AppLanguage.RU -> "В 1-й фазе берите Crystal Prisons и Energy Hubs, во 2-й — Central Ironprison, используя окна лечения."
            AppLanguage.ZH -> "第1阶段夺取Crystal Prisons与Energy Hubs，第2阶段强攻Central Ironprison并利用治疗窗口。"
            AppLanguage.DE -> "Phase 1 Crystal Prisons & Energy Hubs sichern, Phase 2 Central Ironprison angreifen, Heilzyklen nutzen."
            AppLanguage.EN -> "Secure Crystal Prisons, Ironholds & Energy Hubs in Phase 1, push Central Ironprison in Phase 2, use 12-min healing cycles."
            AppLanguage.IT -> "Conquista Crystal Prisons, Ironholds ed Energy Hubs nella fase 1; attacca Central Ironprison nella fase 2 e sfrutta i cicli di cura di circa 12 minuti."
        }
        SANDSEA_CONQUEST -> when (lang) {
            AppLanguage.FR -> "Économisez le Carburant (Fuel) sur les routes, protégez les Vessel Stations, capturez les Phares et poussez vers S100 avec le Tacticien."
            AppLanguage.TR -> "Yakıtı (Fuel) koruyun, Gemi İstasyonlarını savunun, Deniz Fenerlerini alın ve Taktikçi ile S100'e ilerleyin."
            AppLanguage.RU -> "Экономьте топливо, защищайте станции судов, захватывайте маяки и атакуйте S100 с тактиком."
            AppLanguage.ZH -> "节省队伍燃料，保护船坞站点，占领灯塔，配合战术官推进至S100。"
            AppLanguage.DE -> "Treibstoff sparen, Vessel Stations schützen, Leuchttürme einnehmen und koordiniert auf S100 vorrücken."
            AppLanguage.EN -> "Save squad Fuel on sand roads, protect Vessel Stations, capture Lighthouses and coordinate pushes toward S100."
            AppLanguage.IT -> "Risparmia carburante durante gli spostamenti sulla sabbia, proteggi le Vessel Stations, conquista i fari e coordina l’avanzata verso S100."
        }
        SVS -> when (lang) {
            AppLanguage.FR -> "Optimisez la puissance avant le matchmaking, gardez accélérations & éclats pour la Préparation, puis défendez/attaquez en Phase Guerre."
            AppLanguage.TR -> "Eşleşme öncesi gücü düzenleyin, hızlandırma ve parçaları Hazırlık Aşamasına saklayın, Savaşta saldırın/savunun."
            AppLanguage.RU -> "Оптимизируйте силу до подбора, копите ускорения и осколки к фазе подготовки, затем воюйте в фазе войны."
            AppLanguage.ZH -> "匹配前优化战力，加速道具和碎片留给准备阶段，战争阶段协同攻防。"
            AppLanguage.DE -> "Macht vor Matchmaking prüfen, Beschleuniger/Splitter für Vorbereitung sparen, dann Kriegsphase."
            AppLanguage.EN -> "Manage matchmaking power, save speedups & hero shards for Preparation Phase, then fight in War Phase."
            AppLanguage.IT -> "Gestisci la potenza prima del matchmaking, conserva accelerazioni e frammenti eroe per la fase di preparazione, poi combatti nella fase di guerra."
        }
        TURMOIL_SURGE -> when (lang) {
            AppLanguage.FR -> "20 vagues (~40 min) : les vagues 10 & 20 visent les Tourelles (Sentry Turrets), et les vagues 7, 14 & 17 ciblent les joueurs en ligne. Échangez vos garnisons."
            AppLanguage.TR -> "20 dalga: 10. ve 20. dalgalar Kulelere, 7/14/17. dalgalar aktif oyunculara saldırır. Müttefiklerle garnizon takası yapın."
            AppLanguage.RU -> "20 волн: 10 и 20 бьют по турелям, 7/14/17 — по игрокам онлайн. Обменивайтесь подкреплениями с союзниками."
            AppLanguage.ZH -> "20波进攻：第10、20波攻击炮塔，第7、14、17波攻击在线玩家，与盟友互换驻防。"
            AppLanguage.DE -> "20 Wellen: 10 & 20 treffen Sentry Turrets; 7, 14 & 17 aktive Spieler. Garnisonen mit Verbündeten tauschen."
            AppLanguage.EN -> "20 waves (~40 min): waves 10 & 20 hit Sentry Turrets; Frenzy waves 7, 14 & 17 hit online players. Swap ally garrisons."
            AppLanguage.IT -> "20 ondate (circa 40 min): le ondate 10 e 20 colpiscono le torrette Sentry; le ondate Frenzy 7, 14 e 17 attaccano i giocatori online. Scambia le guarnigioni con gli alleati."
        }
        GOLD_HUNTER -> when (lang) {
            AppLanguage.FR -> "Réservez éclats, livres de compétences, matériaux d'Arsenal et gros bâtiments exclusivement pour les paliers d'événement."
            AppLanguage.TR -> "Parçaları, beceri kitaplarını, ekipman malzemelerini ve büyük yükseltmeleri sadece etkinlik günlerinde harcayın."
            AppLanguage.RU -> "Храните осколки, книги навыков, материалы арсенала и крупные стройки для этапов события."
            AppLanguage.ZH -> "将英雄碎片、技能书、军械材料与大型建筑升级留到活动窗口再集中使用。"
            AppLanguage.DE -> "Splitter, Skillbücher, Ausrüstungsmaterialien und große Ausbauten für Event-Tage aufsparen."
            AppLanguage.EN -> "Save hero shards, skill books, Arsenal materials and long upgrades exclusively for event milestone days."
            AppLanguage.IT -> "Conserva frammenti eroe, libri abilità, materiali dell’Arsenale e miglioramenti lunghi per i giorni delle tappe evento."
        }
        GORILLA_LEGION -> when (lang) {
            AppLanguage.FR -> "Vérifie le boss et le bonus affichés en jeu : le bonus du jour est amplifié de +1000 %. Seuls les compétences de héros, l'équipement exclusif et ce bonus comptent. Ratio communauté de départ : 0 % Shieldbearers / 10 % Bombers / 90 % Shooters ; compare plusieurs rapports et ajuste selon le boss."
            AppLanguage.TR -> "Oyunda gösterilen gorili ve günlük bonusu kontrol edin: günün bonusu +%1000 artırılır. Yalnızca kahraman becerileri, özel ekipman ve günlük bonus etkilidir. Topluluk başlangıç oranı: %0 Shieldbearer / %10 Bomber / %90 Shooter; savaş raporlarını karşılaştırıp gorile göre ayarlayın."
            AppLanguage.RU -> "Проверьте босса и дневной бонус в игре: бонус дня усилен на +1000 %. Учитываются только навыки героев, эксклюзивное снаряжение и бонус дня. Стартовая доля сообщества: 0 % Shieldbearers / 10 % Bombers / 90 % Shooters; сравнивайте несколько боевых отчётов и меняйте состав под босса."
            AppLanguage.ZH -> "先确认游戏内显示的首领和每日加成：当日加成额外提高 1000%。只有英雄技能、专属装备和当日加成生效。社区起始比例：Shieldbearers 0% / Bombers 10% / Shooters 90%；比较多次战报，再按首领调整。"
            AppLanguage.DE -> "Prüfe den angezeigten Gorilla und Tagesbonus im Spiel: Der Tagesbonus wird um +1000 % verstärkt. Es zählen nur Heldenfähigkeiten, exklusive Ausrüstung und der Tagesbonus. Community-Startverhältnis: 0 % Shieldbearers / 10 % Bombers / 90 % Shooters; mehrere Kampfberichte vergleichen und je Boss anpassen."
            AppLanguage.EN -> "Check the in-game boss and daily bonus: that day's bonus is amplified by +1000%. Only hero skills, exclusive equipment and the daily bonus count. Community starting ratio: 0% Shieldbearers / 10% Bombers / 90% Shooters; compare several battle reports and adjust for the boss."
            AppLanguage.IT -> "Controlla il boss e il bonus giornaliero mostrati nel gioco: il bonus del giorno è amplificato del +1000%. Contano solo le abilità degli eroi, l’equipaggiamento esclusivo e il bonus giornaliero. Rapporto iniziale della community: 0% Shieldbearer / 10% Bomber / 90% Shooter; confronta più rapporti di battaglia e adatta la formazione al boss."
        }
    }
}

@Serializable
enum class AllianceRank(val labelFr: String) {
    R5("R5 · Chef"),
    R4("R4 · Officier"),
    R3("R3"),
    R2("R2"),
    R1("R1");

    fun labelFor(language: AppLanguage): String = when (this) {
        R5 -> when (language) {
            AppLanguage.FR -> labelFr
            AppLanguage.EN -> "R5 · Leader"
            AppLanguage.IT -> "R5 · Capo"
            AppLanguage.DE -> "R5 · Anführer"
            AppLanguage.TR -> "R5 · Lider"
            AppLanguage.RU -> "R5 · Лидер"
            AppLanguage.ZH -> "R5 · 领袖"
        }
        R4 -> when (language) {
            AppLanguage.FR -> labelFr
            AppLanguage.EN -> "R4 · Officer"
            AppLanguage.IT -> "R4 · Ufficiale"
            AppLanguage.DE -> "R4 · Offizier"
            AppLanguage.TR -> "R4 · Subay"
            AppLanguage.RU -> "R4 · Офицер"
            AppLanguage.ZH -> "R4 · 官员"
        }
        R3 -> "R3"
        R2 -> "R2"
        R1 -> "R1"
    }
}

@Serializable
data class AlliancePlayerProfile(
    val id: String = "",
    val playerName: String = "",
    val server: String = "",
    val allianceName: String = "",
    val rank: AllianceRank = AllianceRank.R1,
    val powerText: String = "",
    val heroSkillSummary: String = "",
    val robotSummary: String = "",
    val updatedAtIso: String = ""
)

@Serializable
data class AllianceWorkspace(
    val server: String = "",
    val allianceName: String = "",
    val ownProfile: AlliancePlayerProfile = AlliancePlayerProfile(),
    val members: List<AlliancePlayerProfile> = emptyList()
)

@Serializable
data class LocalGameAccountProfile(
    val id: String,
    val name: String,
    val stateJson: String,
    val updatedAtIso: String = ""
)

@Serializable
data class HeroSkill(
    val nom: String,
    val niveau: String,
    val description: String? = null,
    val type: String = "inconnu",
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
    val champsIncertains: List<String> = emptyList(),
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
data class ResourceRecord(
    val nom: String,
    val quantiteTexte: String,
    val quantiteNumerique: Double? = null,
    val incertain: Boolean = false,
    val raisonIncertitude: String? = null
)

@Serializable
data class RobotRecord(
    val id: String = "",
    val nom: String,
    val niveau: String = "",
    val rarete: String = "",
    val bonus: String = "",
    val competences: String = "",
    val puissance: String = "",
    val champsIncertains: List<String> = emptyList(),
    val incertain: Boolean = false,
    val raisonIncertitude: String? = null,
    val source: String = "saisie_manuel",
    val updatedAtIso: String = ""
)

@Serializable
data class EquipmentRecord(
    val id: String = "",
    val piece: String,
    val niveau: String = "",
    val qualite: String = "",
    val materiaux: String = "",
    val experience: String = "",
    val reforme: String = "",
    val texteObserve: String = "",
    val incertain: Boolean = true,
    val raisonIncertitude: String? = null
)

@Serializable
data class EventProgressRecord(
    val id: String = "",
    val eventId: String,
    val eventName: String,
    val date: String = "",
    val score: String = "",
    val notes: String = "",
    val updatedAtIso: String = ""
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
    val equipements: List<EquipmentRecord> = emptyList(),
    val champsIncertains: List<UncertainItem> = emptyList()
)

@Serializable
data class LocalGameProfileSnapshot(
    val schemaVersion: Int? = null,
    val allianceWorkspace: AllianceWorkspace = AllianceWorkspace(),
    val analyses: List<ScreenshotAnalysis> = emptyList(),
    val heroes: List<HeroRecord> = emptyList(),
    val resources: List<ResourceRecord> = emptyList(),
    val robots: List<RobotRecord> = emptyList(),
    val eventRecords: List<EventProgressRecord> = emptyList(),
    val currentPower: PowerMetric? = null,
    val adviceMode: String = "TRIAL_CAGE",
    val adviceResult: TacticalAdviceResult? = null,
    val playerRole: String = "POLYVALENT_F2P",
    val routineState: Map<String, String> = emptyMap()
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
data class TroopRatioProfile(
    val id: String = "",
    val role: String = "",
    val roleLabel: String = "",
    val label: String = "",
    val shieldbearer: Int = 0,
    val bomber: Int = 0,
    val shooter: Int = 0,
    val note: String = ""
)

@Serializable
data class TacticalAdviceResult(
    val mode: String = "",
    val language: String? = null,
    val generatedAt: String = "",
    val generatedAtIso: String = "",
    val synthese: String = "",
    val compositionRecommandee: String = "",
    val recommandations: List<AdviceRecommendation> = emptyList(),
    val heroRecommendations: List<AdviceRecommendation> = emptyList(),
    val robotRecommendations: List<AdviceRecommendation> = emptyList(),
    val troopRatioTitle: String? = null,
    val troopRatioProfiles: List<TroopRatioProfile> = emptyList(),
    val informationsIncertaines: List<String> = emptyList(),
    val sources: List<GroundedWebSource> = emptyList(),
    val geminiWarning: String? = null
)

/**
 * Catalogue de référence communautaire. Les métadonnées de classe/saison/rareté
 * sont issues du catalogue LOJ Strats (50 héros, vérifié le 06/11/2026).
 * Les spécialités War indiquées comme observées proviennent de HEROpedia LOJ XLab
 * (captures en jeu, saisons 1 à 7, vérifiées en novembre 2026).
 * Ce catalogue n'est PAS le profil du joueur : niveaux, étoiles et compétences
 * personnelles doivent être lus sur ses propres captures.
 */
data class HeroCatalogEntry(
    val nom: String,
    val rarete: String,
    val saison: String,
    val typeTroupe: String,
    val usageWarObserve: String? = null,
    val sourceNiveau: String = "Catalogue communautaire",
    val sourceUrl: String = "https://lojstrats.com/hero",
    val portraitUrl: String? = null
)

object HeroCatalog {
    val entries: List<HeroCatalogEntry> = listOf(
        HeroCatalogEntry("Tyronn", "SSR", "S1", "Shieldbearer", "Rally · ATK +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/3a0d37fd19b18166.webp"),
        HeroCatalogEntry("Phoenix", "SSR", "S1", "Shieldbearer", "Solo · HP +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/de3dc1adeadc04a4.webp"),
        HeroCatalogEntry("Devilian", "SSR", "S1", "Shooter", "Rally · DEF +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/7611cea4c086779b.webp"),
        HeroCatalogEntry("Inata", "SSR", "S1", "Shooter", "Défense · HP +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/e33012e9864d4259.webp"),
        HeroCatalogEntry("Tormund", "SSR", "S1", "Bomber", "Défense · DEF +16% au niveau de compétence 5", "Observé en jeu", sourceUrl = "https://lojwiki.com/fr/heroes/tormund/", portraitUrl = "https://img.lojwiki.com/half/e8e153e31561afad.webp"),
        HeroCatalogEntry("Vivian", "SSR", "S1", "Bomber", "Rally · Létalité +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/0694be4993a40daa.webp"),
        HeroCatalogEntry("Mia", "SSR / SR", "S1", "Shooter", "Expéditions · pas d'équipement exclusif", "Observé en jeu", sourceUrl = "https://lojwiki.com/fr/heroes/mia/", portraitUrl = "https://img.lojwiki.com/heroes/13d80d99fed98c96.webp"),
        HeroCatalogEntry("Aiksen", "R", "S1", "Shooter", sourceUrl = "https://lojwiki.com/fr/heroes/aiksen/", portraitUrl = "https://img.lojwiki.com/heroes/095b25f9b8130678.webp"),
        HeroCatalogEntry("Beary", "SSR", "S1", "Shieldbearer", portraitUrl = "https://img.lojwiki.com/half/3f4acb38ddf39b58.webp"),
        HeroCatalogEntry("Durga", "R", "S1", "Shieldbearer", sourceUrl = "https://lojwiki.com/fr/heroes/durga/", portraitUrl = "https://img.lojwiki.com/heroes/811b3f2841f82aad.webp"),
        HeroCatalogEntry("Ekko", "SR", "S1", "Bomber", sourceUrl = "https://lojwiki.com/fr/heroes/ekko/", portraitUrl = "https://img.lojwiki.com/heroes/4361a8e2b7b436b4.webp"),
        HeroCatalogEntry("Flora", "SR", "S1", "Shooter", sourceUrl = "https://lojwiki.com/fr/heroes/flora/", portraitUrl = "https://img.lojwiki.com/heroes/199dea62ff8f8a72.webp"),
        HeroCatalogEntry("Gerd", "SR", "S1", "Shieldbearer", sourceUrl = "https://lojwiki.com/fr/heroes/gerd/", portraitUrl = "https://img.lojwiki.com/heroes/1079cda528942ea8.webp"),
        HeroCatalogEntry("Gimes", "SR", "S1", "Bomber", sourceUrl = "https://lojwiki.com/fr/heroes/gimes/", portraitUrl = "https://img.lojwiki.com/heroes/41dc76aabb2c5aab.webp"),
        HeroCatalogEntry("Harton", "R", "S1", "Shieldbearer", sourceUrl = "https://lojwiki.com/fr/heroes/harton/", portraitUrl = "https://img.lojwiki.com/heroes/e82a25ef340e3ef8.webp"),
        HeroCatalogEntry("Iwakado", "SR", "S1", "Shieldbearer", sourceUrl = "https://lojwiki.com/fr/heroes/iwakado/", portraitUrl = "https://img.lojwiki.com/heroes/22f6243d4b345987.webp"),
        HeroCatalogEntry("Lofili", "SR", "S1", "Shooter", sourceUrl = "https://lojwiki.com/fr/heroes/lofili/", portraitUrl = "https://img.lojwiki.com/heroes/f0b15a07e08bce8d.webp"),
        HeroCatalogEntry("Lunarl", "SR", "S1", "Bomber", sourceUrl = "https://lojwiki.com/fr/heroes/lunarl/", portraitUrl = "https://img.lojwiki.com/half/852918d66fdf283d.webp"),
        HeroCatalogEntry("Pasino", "R", "S1", "Bomber", sourceUrl = "https://lojwiki.com/fr/heroes/pasino/", portraitUrl = "https://img.lojwiki.com/heroes/bacf86bdac1b2b3c.webp"),
        HeroCatalogEntry("Platos", "SR", "S1", "Shooter", sourceUrl = "https://lojwiki.com/fr/heroes/platos/", portraitUrl = "https://img.lojwiki.com/heroes/b662bb38e020836a.webp"),
        HeroCatalogEntry("Samir", "SR", "S1", "Bomber", sourceUrl = "https://lojwiki.com/fr/heroes/samir/", portraitUrl = "https://img.lojwiki.com/heroes/06a325063e5fbaad.webp"),
        HeroCatalogEntry("Vesaryon", "SR", "S1", "Shieldbearer", sourceUrl = "https://lojwiki.com/fr/heroes/vesaryon/", portraitUrl = "https://img.lojwiki.com/heroes/56b130e1fd4a2b69.webp"),
        HeroCatalogEntry("Alph", "SSR", "S2", "Bomber", "Solo · ATK +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/4436d16fe9ecbf0d.webp"),
        HeroCatalogEntry("Caesar", "SSR", "S2", "Shieldbearer", "Défense · ATK +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/978ac245a8850469.webp"),
        HeroCatalogEntry("Lanchester", "SSR", "S2", "Bomber", "Défense · DEF +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/f62c5b98a5028535.webp"),
        HeroCatalogEntry("Mireya", "SSR", "S2", "Shooter", "Rally · Létalité +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/020f1610ceead6ab.webp"),
        HeroCatalogEntry("Sawyer", "SSR", "S3", "Shooter", "Rally · HP +16% au niveau de compétence 5", "Observé en jeu", sourceUrl = "https://lojwiki.com/fr/heroes/sawyer/", portraitUrl = "https://img.lojwiki.com/half/37fbe6e7af9872d3.webp"),
        HeroCatalogEntry("Veronica", "SSR", "S3", "Shooter", "Solo · Létalité +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/9e495457e33e2b84.webp"),
        HeroCatalogEntry("Whisper", "SSR", "S3", "Bomber", "Rally · Létalité +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/bc2af2e96fd49d4c.webp"),
        HeroCatalogEntry("Xuanming", "SSR", "S3", "Shieldbearer", "Défense · ATK +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/c5d977f83654eb21.webp"),
        HeroCatalogEntry("Drake", "SSR", "S4", "Shooter", "Défense · Létalité +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/64f5749fd05ff727.webp"),
        HeroCatalogEntry("Koschevoi", "SSR", "S4", "Bomber", "Solo · DEF +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/d5311ea855d0a805.webp"),
        HeroCatalogEntry("Ryuichi Ito", "SSR", "S4", "Bomber", "Rally · ATK +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/c76c0c2c577d46e9.webp"),
        HeroCatalogEntry("Zoltan", "SSR", "S4", "Shieldbearer", "Défense · DEF +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/de662a4812c0e5c3.webp"),
        HeroCatalogEntry("Ada", "SSR", "S5", "Shooter", "Rally · ATK +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/2febb81d552fc993.webp"),
        HeroCatalogEntry("Edwin", "SSR", "S5", "Shooter", "Solo · HP +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/0468e9d249df4c2f.webp"),
        HeroCatalogEntry("Lee", "SSR", "S5", "Bomber", "Défense · ATK +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/c0ecb10733584426.webp"),
        HeroCatalogEntry("Marcus", "SSR", "S5", "Shieldbearer", "Défense · HP +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/70cb38e59f20e3de.webp"),
        HeroCatalogEntry("Kate", "SSR", "S6", "Shooter", "Défense · Létalité +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/5d3f6502abaec63c.webp"),
        HeroCatalogEntry("Otto", "SSR", "S6", "Shieldbearer", "Défense · DEF +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/e15fde4c8e34d2c3.webp"),
        HeroCatalogEntry("Worrell", "SSR", "S6", "Bomber", "Solo · ATK +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/53c7fc8236643682.webp"),
        HeroCatalogEntry("Wukong", "SSR", "S6", "Bomber", "Rally · Létalité +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/7a9085421bb7bc95.webp"),
        HeroCatalogEntry("Boogie", "SSR", "S7", "Bomber", "Défense · ATK +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/c01591f22aa4f540.webp"),
        HeroCatalogEntry("Fran & Pike", "SSR", "S7", "Shieldbearer", "Défense · HP +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/7d74a030adb8f3e8.webp"),
        HeroCatalogEntry("Rex", "SSR", "S7", "Shooter", "Solo · Létalité +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/8fd765d6b7b4acd1.webp"),
        HeroCatalogEntry("Rin", "SSR", "S7", "Shooter", "Rally · ATK +16% au niveau de compétence 5", "Observé en jeu", portraitUrl = "https://img.lojwiki.com/half/68aecf36998f0d92.webp"),
        HeroCatalogEntry("Mai Shiranui", "SSR", "KOF Legend", "Shooter", sourceUrl = "https://lojwiki.com/fr/heroes/mai-shiranui/", portraitUrl = "https://img.lojwiki.com/heroes/3630ed863cf6cd96.webp"),
        HeroCatalogEntry("Omega Rugal", "SSR", "KOF Legend", "Shieldbearer", sourceUrl = "https://lojwiki.com/fr/heroes/omega-rugal/", portraitUrl = "https://img.lojwiki.com/heroes/86217089c37ab207.webp"),
        HeroCatalogEntry("Terry Bogard", "SSR", "KOF Legend", "Bomber", sourceUrl = "https://lojwiki.com/fr/heroes/terry-bogard/", portraitUrl = "https://img.lojwiki.com/heroes/6168d436e6bde331.webp"),
        HeroCatalogEntry("Minyeon & Taehun", "SR", "S1", "Bomber", sourceUrl = "https://lojwiki.com/fr/heroes/minyeon-and-taehun/", portraitUrl = "https://img.lojwiki.com/heroes/9015182e0dcbf5a9.webp")
    )
}

/**
 * Pure selection rules for the Forticlad calculator. Keeping the bounds here
 * makes the level picker deterministic and directly testable.
 */
/**
 * Matches OCR resource names across supported game/app languages.
 * Labels passed here are stable resource keys, not translated display strings.
 */
object ResourceNameMatcher {
    // Aliases are kept language-neutral so the stock/cost comparison works
    // even when the game language differs from the app language.
    private val aliases = mapOf(
        "food" to listOf("nourriture", "food", "vivres", "nahrung", "yiyecek", "еда", "食物", "cibo"),
        "wood" to listOf("bois", "wood", "lumber", "holz", "odun", "дерево", "木材", "legno"),
        "steel" to listOf("acier", "steel", "fer", "iron", "stahl", "çelik", "сталь", "钢材", "acciaio"),
        "gasoline" to listOf("essence", "gasoline", "fuel", "pétrole", "petrole", "benzin", "бензин", "汽油", "benzina", "carburante")
    )

    fun matchesWardenCost(resourceName: String, resourceKey: String): Boolean {
        val normalizedName = java.text.Normalizer.normalize(
            resourceName.lowercase().trim(),
            java.text.Normalizer.Form.NFD
        ).replace(Regex("\\p{Mn}+"), "")
        val candidates = aliases[resourceKey.lowercase()] ?: return false
        return normalizedName.isNotBlank() && candidates.any { alias ->
            val normalizedAlias = java.text.Normalizer.normalize(alias.lowercase(), java.text.Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
            normalizedName.contains(normalizedAlias)
        }
    }
}

object ForticladSelection {
    fun currentLevels(): IntRange = 1..9
    fun completedSteps(): IntRange = 0..4
    fun targetLevels(currentLevel: Int): IntRange =
        (currentLevel.coerceIn(1, 9) + 1)..10

    fun targetAfterCurrentChange(currentLevel: Int, targetLevel: Int): Int {
        val current = currentLevel.coerceIn(1, 9)
        return if (targetLevel <= current) (current + 1).coerceAtMost(10)
        else targetLevel.coerceIn(current + 1, 10)
    }
}
