package com.landsofjail.assistant.ui

import android.content.Context
import java.text.NumberFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.landsofjail.assistant.model.AppLanguage
import com.landsofjail.assistant.model.PlayerResourceStock
import com.landsofjail.assistant.model.ProgressionCatalog
import com.landsofjail.assistant.model.ProgressionCatalogRecord
import com.landsofjail.assistant.model.CostConfidence
import com.landsofjail.assistant.model.ProgressionCoach
import com.landsofjail.assistant.model.canonicalProgressionResourceId
import com.landsofjail.assistant.model.ProgressionDomain
import com.landsofjail.assistant.model.ProgressionLevelRecord
import com.landsofjail.assistant.model.ProgressionObjective
import com.landsofjail.assistant.model.ProgressionPlayerProfile
import com.landsofjail.assistant.model.ProgressionProfileCodec
import com.landsofjail.assistant.model.ResourceRecord
import com.landsofjail.assistant.model.mergeProgressionStocks
import com.landsofjail.assistant.model.removeProgressionObjective
import com.landsofjail.assistant.model.upsertProgressionObjective

private val progressionDomains = listOf(
    ProgressionDomain.BUILDING,
    ProgressionDomain.HERO,
    ProgressionDomain.RESEARCH,
    ProgressionDomain.ROBOT,
    ProgressionDomain.ROBOT_CORE,
    ProgressionDomain.EQUIPMENT,
    ProgressionDomain.TROOPS
)

private fun progressionLabel(domain: ProgressionDomain, lang: AppLanguage): String = when (domain) {
    ProgressionDomain.BUILDING -> when (lang) {
        AppLanguage.FR -> "Bâtiment"; AppLanguage.EN -> "Building"; AppLanguage.DE -> "Gebäude"
        AppLanguage.TR -> "Bina"; AppLanguage.RU -> "Здание"; AppLanguage.ZH -> "建筑"; AppLanguage.IT -> "Edificio"
    }
    ProgressionDomain.HERO -> when (lang) {
        AppLanguage.FR -> "Héros"; AppLanguage.EN -> "Hero"; AppLanguage.DE -> "Held"
        AppLanguage.TR -> "Kahraman"; AppLanguage.RU -> "Герой"; AppLanguage.ZH -> "英雄"; AppLanguage.IT -> "Eroe"
    }
    ProgressionDomain.RESEARCH -> when (lang) {
        AppLanguage.FR -> "Recherche"; AppLanguage.EN -> "Research"; AppLanguage.DE -> "Forschung"
        AppLanguage.TR -> "Araştırma"; AppLanguage.RU -> "Исследование"; AppLanguage.ZH -> "研究"; AppLanguage.IT -> "Ricerca"
    }
    ProgressionDomain.ROBOT -> when (lang) {
        AppLanguage.FR -> "Robot"; AppLanguage.EN -> "Robot"; AppLanguage.DE -> "Roboter"
        AppLanguage.TR -> "Robot"; AppLanguage.RU -> "Робот"; AppLanguage.ZH -> "机器人"; AppLanguage.IT -> "Robot"
    }
    ProgressionDomain.EQUIPMENT -> when (lang) {
        AppLanguage.FR -> "Équipement"; AppLanguage.EN -> "Equipment"; AppLanguage.DE -> "Ausrüstung"
        AppLanguage.TR -> "Ekipman"; AppLanguage.RU -> "Снаряжение"; AppLanguage.ZH -> "装备"; AppLanguage.IT -> "Equipaggiamento"
    }
    ProgressionDomain.TROOPS -> when (lang) {
        AppLanguage.FR -> "Troupes"; AppLanguage.EN -> "Troops"; AppLanguage.DE -> "Truppen"
        AppLanguage.TR -> "Birlikler"; AppLanguage.RU -> "Войска"; AppLanguage.ZH -> "部队"; AppLanguage.IT -> "Truppe"
    }
    ProgressionDomain.ROBOT_CORE -> when (lang) {
        AppLanguage.FR -> "Noyaux robots"; AppLanguage.EN -> "Robot cores"; AppLanguage.DE -> "Roboterkerne"
        AppLanguage.TR -> "Robot çekirdekleri"; AppLanguage.RU -> "Ядра роботов"; AppLanguage.ZH -> "机器人核心"; AppLanguage.IT -> "Nuclei robot"
    }
}

@Composable
private fun ProgressionCatalogGoalCard(
    goal: ProgressionObjective,
    profile: ProgressionPlayerProfile,
    catalog: List<ProgressionCatalogRecord>,
    language: AppLanguage
) {
    fun text(fr: String, en: String, de: String, tr: String, ru: String, zh: String, it: String) =
        when (language) {
            AppLanguage.FR -> fr; AppLanguage.EN -> en; AppLanguage.DE -> de
            AppLanguage.TR -> tr; AppLanguage.RU -> ru; AppLanguage.ZH -> zh; AppLanguage.IT -> it
        }

    val current = profile.confirmedLevel(goal.targetId)
    val target = goal.targetLevel
    val maxLevel = catalog.maxOfOrNull { it.step.toLevel } ?: 0
    val partialCurrentBand = current != null && catalog.any {
        it.step.fromLevel < current && current < it.step.toLevel
    }
    val partialTargetBand = catalog.any {
        it.step.fromLevel < target && target < it.step.toLevel
    }
    val belowFirstPublishedLevel = current != null && current < (catalog.minOfOrNull { it.step.fromLevel } ?: 0)
    val rows = current?.let { level ->
        catalog.filter { it.step.fromLevel >= level && it.step.toLevel <= target }
    }.orEmpty()
    val stocks = profile.resourceStocks.map {
        it.copy(resourceId = canonicalProgressionResourceId(it.resourceId))
    }
    val plan = ProgressionCoach.calculate(rows.map { it.step }, stocks)

    fun materialLabel(resourceId: String): String = when (canonicalProgressionResourceId(resourceId)) {
        "prisoner armor data" -> text("Données d'armure prisonnier", "Prisoner Armor Data", "Gefangenen-Rüstungsdaten",
            "Mahkûm Zırh Verisi", "Данные брони заключённых", "囚犯护甲数据", "Dati armatura detenuto")
        "power modules" -> text("Modules de puissance", "Power Modules", "Energiemodule",
            "Güç Modülleri", "Модули энергии", "能量模块", "Moduli di potenza")
        "advanced power modules" -> text("Modules de puissance avancés", "Advanced Power Modules", "Erweiterte Energiemodule",
            "Gelişmiş Güç Modülleri", "Улучшенные модули энергии", "高级能量模块", "Moduli di potenza avanzati")
        "core chips" -> text("Puces de noyau", "Core Chips", "Kernchips",
            "Çekirdek Çipleri", "Чипы ядра", "核心芯片", "Chip nucleo")
        "equipment exp" -> text("EXP d'équipement", "Equipment EXP", "Ausrüstungs-EP",
            "Ekipman TP", "Опыт снаряжения", "装备经验", "EXP equipaggiamento")
        "magnets" -> text("Aimants", "Magnets", "Magnete", "Mıknatıslar", "Магниты", "磁铁", "Magneti")
        "potential coils" -> text("Bobines de potentiel", "Potential Coils", "Potenzialspulen",
            "Potansiyel Bobinleri", "Катушки потенциала", "潜能线圈", "Bobine di potenziale")
        "precision gears" -> text("Engrenages de précision", "Precision Gears", "Präzisionszahnräder",
            "Hassas Dişliler", "Прецизионные шестерни", "精密齿轮", "Ingranaggi di precisione")
        "hero shards" -> text("Fragments de héros", "Hero Shards", "Heldenfragmente",
            "Kahraman Parçaları", "Осколки героев", "英雄碎片", "Frammenti eroe")
        else -> resourceId
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text(fr = "Calculateur : ${progressionLabel(goal.domain, language)} · ${goal.targetId}",
                    en = "Calculator: ${progressionLabel(goal.domain, language)} · ${goal.targetId}",
                    de = "Rechner: ${progressionLabel(goal.domain, language)} · ${goal.targetId}",
                    tr = "Hesaplayıcı: ${progressionLabel(goal.domain, language)} · ${goal.targetId}",
                    ru = "Калькулятор: ${progressionLabel(goal.domain, language)} · ${goal.targetId}",
                    zh = "计算器：${progressionLabel(goal.domain, language)} · ${goal.targetId}",
                    it = "Calcolatore: ${progressionLabel(goal.domain, language)} · ${goal.targetId}"),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${catalog.first().sourceLabel} · ${catalog.first().checkedOn}",
                style = MaterialTheme.typography.bodySmall
            )
            when {
                current == null -> Text(
                    text(fr = "Niveau actuel non confirmé : renseigne-le dans l'objectif.",
                        en = "Current level is not confirmed: enter it in the goal.",
                        de = "Das aktuelle Level ist nicht bestätigt: Gib es im Ziel ein.",
                        tr = "Mevcut seviye doğrulanmadı: hedefe gir.",
                        ru = "Текущий уровень не подтверждён: укажи его в цели.",
                        zh = "当前等级尚未确认，请在目标中填写。",
                        it = "Il livello attuale non è confermato: inseriscilo nell'obiettivo."),
                    style = MaterialTheme.typography.bodySmall
                )
                current != null && target <= current -> Text(
                    text(fr = "Objectif déjà atteint.", en = "Target already reached.", de = "Ziel bereits erreicht.",
                        tr = "Hedefe ulaşıldı.", ru = "Цель уже достигнута.", zh = "目标已达成。",
                        it = "Obiettivo già raggiunto."),
                    style = MaterialTheme.typography.bodySmall
                )
                target > maxLevel -> Text(
                    text(fr = "Les données publiées couvrent cette catégorie jusqu'au niveau $maxLevel. Les coûts suivants restent inconnus.",
                        en = "Published data covers this category only through level $maxLevel. Later costs remain unknown.",
                        de = "Veröffentlichte Daten decken diese Kategorie nur bis Level $maxLevel ab. Weitere Kosten sind unbekannt.",
                        tr = "Yayımlanan veriler bu kategoriyi yalnızca $maxLevel seviyesine kadar kapsar. Sonraki maliyetler bilinmiyor.",
                        ru = "Опубликованные данные охватывают эту категорию только до уровня $maxLevel. Дальнейшие затраты неизвестны.",
                        zh = "已发布数据仅覆盖此类别至 $maxLevel 级，之后费用未知。",
                        it = "I dati pubblicati coprono questa categoria solo fino al livello $maxLevel. I costi successivi sono sconosciuti."),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (current != null && target > current) {
                if (partialCurrentBand || partialTargetBand || belowFirstPublishedLevel) {
                    Text(
                        text(fr = "La source publie des tranches complètes, pas un coût par niveau. Les tranches partielles ne sont pas proratisées et restent inconnues.",
                            en = "The source publishes complete bands, not per-level costs. Partial bands are not prorated and remain unknown.",
                            de = "Die Quelle veröffentlicht vollständige Bereiche, keine Kosten je Level. Teilbereiche werden nicht anteilig geschätzt.",
                            tr = "Kaynak seviye başına değil, tam aralıklar için maliyet yayımlar. Kısmi aralıklar oranlanmaz ve bilinmiyor kalır.",
                            ru = "Источник публикует стоимость полных диапазонов, а не отдельных уровней. Частичные диапазоны не пересчитываются.",
                            zh = "来源提供完整等级区间的费用，而非单级费用；部分区间不按比例估算。",
                            it = "La fonte pubblica costi per fasce complete, non per singolo livello. Le fasce parziali non vengono stimate proporzionalmente."),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (rows.isEmpty()) {
                    Text(
                        text(fr = "Aucune tranche complète disponible pour cette plage ; aucun coût n'est inventé.",
                            en = "No complete published band is available for this range; no cost is invented.",
                            de = "Für diesen Bereich ist kein vollständiger veröffentlichter Abschnitt verfügbar.",
                            tr = "Bu aralık için yayımlanmış tam bir kademe yok; maliyet uydurulmaz.",
                            ru = "Для этого диапазона нет полной опубликованной ступени; стоимость не выдумывается.",
                            zh = "此范围没有完整的已发布区间；不会虚构费用。",
                            it = "Non è disponibile una fascia pubblicata completa per questo intervallo; non vengono inventati costi."),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                plan.steps.forEach { step ->
                    val costs = step.costs.joinToString(" · ") { cost ->
                        val suffix = if (cost.confidence == CostConfidence.NEEDS_CONFIRMATION)
                            text(" (à confirmer)", " (confirm in-game)", " (im Spiel bestätigen)",
                                " (oyunda doğrula)", " (проверь в игре)", " (请在游戏中确认)", " (da confermare)")
                        else ""
                        "${materialLabel(cost.resourceId)} ${NumberFormat.getIntegerInstance().format(cost.quantity)}$suffix"
                    }
                    Text(
                        text = "${step.itemName} · ${step.fromLevel} → ${step.toLevel} : $costs",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (plan.unknownStepIds.isNotEmpty()) {
                    Text(
                        text(fr = "Certaines valeurs sont incertaines ou en conflit et sont exclues des totaux.",
                            en = "Some values are uncertain or conflicting and are excluded from totals.",
                            de = "Einige Werte sind unsicher oder widersprüchlich und werden nicht summiert.",
                            tr = "Bazı değerler belirsiz veya çelişkili; toplamlara dahil edilmez.",
                            ru = "Некоторые значения не подтверждены или противоречат друг другу и не входят в итоги.",
                            zh = "部分数值不确定或存在冲突，不计入总额。",
                            it = "Alcuni valori sono incerti o in conflitto e sono esclusi dai totali."),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                plan.requirements.forEach { requirement ->
                    val label = materialLabel(requirement.resourceId)
                    val required = NumberFormat.getIntegerInstance().format(requirement.required)
                    val stock = requirement.ownedConfirmed?.let { owned ->
                        val missing = NumberFormat.getIntegerInstance().format(requirement.missing ?: 0L)
                        "${text("posseduto", "owned", "vorhanden", "mevcut", "есть", "拥有", "disponibile")} ${NumberFormat.getIntegerInstance().format(owned)} · " +
                            "${text("mancante", "missing", "fehlen", "eksik", "не хватает", "缺少", "mancano")} $missing"
                    } ?: text("stock inconnu", "stock unknown", "Bestand unbekannt", "stok bilinmiyor",
                        "запас неизвестен", "库存未知", "scorta sconosciuta")
                    Text("$label : $required · $stock", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun ProgressionCoachScreen(
    profileId: String,
    language: AppLanguage,
    resources: List<ResourceRecord>
) {
    val context = LocalContext.current
    val prefs = remember(context, profileId) {
        context.getSharedPreferences("progression_profile_$profileId", Context.MODE_PRIVATE)
    }
    var profile by remember(profileId) {
        mutableStateOf(ProgressionProfileCodec.decodeOrDefault(prefs.getString("profile", null), profileId))
    }
    var selectedDomain by remember(profileId) { mutableStateOf(ProgressionDomain.BUILDING) }
    var targetId by remember(profileId) { mutableStateOf("warden_office") }
    var currentLevel by remember(profileId) { mutableStateOf("") }
    var targetLevel by remember(profileId) { mutableStateOf("") }
    var statusMessage by remember(profileId) { mutableStateOf("") }
    val supportedBuildingGoal = profile.objectives.sortedByDescending { it.isPrimary }.firstOrNull {
        it.domain == ProgressionDomain.BUILDING &&
            it.targetId.trim().lowercase() in setOf(
                "lab", "laboratory", "labo", "laboratoire",
                "warden_office", "warden's office", "bureau_du_directeur", "bureau du directeur", "hq",
                "gate", "porte", "kitchen", "cuisine", "cell", "cellule",
                "factory", "usine", "utility_room", "utility room", "salle utilitaire",
                "guard_room", "guard room", "salle de garde",
                "inventory", "warehouse", "entrepot", "entrepôt", "lager",
                "laundry", "buanderie", "wäsche", "playground", "terrain de jeu", "spielplatz", "shower_room", "shower room", "douche", "shower", "cafeteria", "cafétéria", "cantine", "kafeterya",
                "medical_station", "medical station", "medical", "infirmerie", "krankenstation",
                "communication_center", "communication center", "communications center", "centre des communications", "kommunikationszentrum",
                "command_center", "command center", "centre de commandement", "kommandozentrale",
                "shieldbearer_barracks", "shieldbearer barracks", "caserne bouclier", "caserne des boucliers",
                "shooter_barracks", "shooter barracks", "caserne tireurs", "caserne des tireurs",
                "bomber_barracks", "bomber barracks", "caserne bombardiers", "caserne des bombardiers"
            )
    }
    val isLabGoal = supportedBuildingGoal?.targetId?.trim()?.lowercase()?.let {
        it in setOf("lab", "laboratory", "labo", "laboratoire")
    } == true
    val buildingTargetKey = supportedBuildingGoal?.targetId?.trim()?.lowercase().orEmpty()
    val isGateGoal = buildingTargetKey in setOf("gate", "porte")
    val buildingCatalog = when (buildingTargetKey) {
        "lab", "laboratory", "labo", "laboratoire" -> ProgressionCatalog.labBuilding
        "gate", "porte" -> ProgressionCatalog.gateBuilding
        "kitchen", "cuisine" -> ProgressionCatalog.kitchenBuilding
        "cell", "cellule" -> ProgressionCatalog.cellBuilding
        "factory", "usine" -> ProgressionCatalog.factoryBuilding
        "utility_room", "utility room", "salle utilitaire" -> ProgressionCatalog.utilityRoomBuilding
        "guard_room", "guard room", "salle de garde" -> ProgressionCatalog.guardRoomBuilding
        "inventory", "warehouse", "entrepot", "entrepôt", "lager" -> ProgressionCatalog.inventoryBuilding
        "laundry", "buanderie", "wäsche" -> ProgressionCatalog.laundryBuilding
        "playground", "terrain de jeu", "spielplatz" -> ProgressionCatalog.playgroundBuilding
        "shower_room", "shower room", "douche", "shower" -> ProgressionCatalog.showerRoomBuilding
        "cafeteria", "cafétéria", "cantine", "kafeterya" -> ProgressionCatalog.cafeteriaBuilding
        "medical_station", "medical station", "medical", "infirmerie", "krankenstation" -> ProgressionCatalog.medicalStationBuilding
        "communication_center", "communication center", "communications center", "centre des communications", "kommunikationszentrum" -> ProgressionCatalog.communicationCenterBuilding
        "command_center", "command center", "centre de commandement", "kommandozentrale" -> ProgressionCatalog.commandCenterBuilding
        "shieldbearer_barracks", "shieldbearer barracks", "caserne bouclier", "caserne des boucliers" -> ProgressionCatalog.shieldbearerBarrackBuilding
        "shooter_barracks", "shooter barracks", "caserne tireurs", "caserne des tireurs" -> ProgressionCatalog.shooterBarrackBuilding
        "bomber_barracks", "bomber barracks", "caserne bombardiers", "caserne des bombardiers" -> ProgressionCatalog.bomberBarrackBuilding
        "warden_office", "warden's office", "bureau_du_directeur", "bureau du directeur", "hq" -> ProgressionCatalog.wardensOffice
        else -> emptyList()
    }
    val buildingMaxLevel = buildingCatalog.maxOfOrNull { it.step.toLevel } ?: 0
    val buildingCurrentLevel = supportedBuildingGoal?.let { profile.confirmedLevel(it.targetId) }
    val supportedNonBuildingGoals = profile.objectives
        .sortedByDescending { it.isPrimary }
        .filter { it.domain in setOf(
            ProgressionDomain.HERO,
            ProgressionDomain.ROBOT,
            ProgressionDomain.ROBOT_CORE,
            ProgressionDomain.EQUIPMENT
        ) }
    fun text(fr: String, en: String, de: String, tr: String, ru: String, zh: String, it: String) =
        when (language) {
            AppLanguage.FR -> fr; AppLanguage.EN -> en; AppLanguage.DE -> de
            AppLanguage.TR -> tr; AppLanguage.RU -> ru; AppLanguage.ZH -> zh; AppLanguage.IT -> it
        }

    val buildingDisplayName = when (buildingTargetKey) {
        "lab", "laboratory", "labo", "laboratoire" -> text("Laboratoire", "Laboratory", "Labor", "Laboratuvar", "Лаборатория", "实验室", "Laboratorio")
        "gate", "porte" -> text("Porte", "Gate", "Tor", "Kapı", "Ворота", "大门", "Cancello")
        "kitchen", "cuisine" -> text("Cuisine", "Kitchen", "Küche", "Mutfak", "Кухня", "厨房", "Cucina")
        "cell", "cellule" -> text("Cellule", "Cell", "Zelle", "Hücre", "Камера", "牢房", "Cella")
        "factory", "usine" -> text("Usine", "Factory", "Fabrik", "Fabrika", "Фабрика", "工厂", "Fabbrica")
        "utility_room", "utility room", "salle utilitaire" -> text("Salle utilitaire", "Utility Room", "Geräteraum", "Malzeme Odası", "Подсобное помещение", "杂物间", "Locale attrezzi")
        "guard_room", "guard room", "salle de garde" -> text("Salle de garde", "Guard Room", "Wachraum", "Nöbetçi Odası", "Караульная", "警卫室", "Sala di guardia")
        "inventory", "warehouse", "entrepot", "entrepôt", "lager" -> text("Entrepôt", "Inventory", "Lager", "Depo", "Склад", "仓库", "Magazzino")
        "laundry", "buanderie", "wäsche" -> text("Buanderie", "Laundry", "Wäscherei", "Çamaşırhane", "Прачечная", "洗衣房", "Lavanderia")
        "playground", "terrain de jeu", "spielplatz" -> text("Terrain de jeu", "Playground", "Spielplatz", "Oyun alanı", "Игровая площадка", "游乐场", "Parco giochi")
        "shower_room", "shower room", "douche", "shower" -> text("Salle de douche", "Shower Room", "Duschraum", "Duş Odası", "Душевая", "淋浴间", "Sala docce")
        "cafeteria", "cafétéria", "cantine", "kafeterya" -> text("Cafétéria", "Cafeteria", "Kantine", "Kafeterya", "Столовая", "食堂", "Caffetteria")
        "medical_station", "medical station", "medical", "infirmerie", "krankenstation" -> text("Infirmerie", "Medical Station", "Krankenstation", "Revir", "Медпункт", "医疗站", "Infermeria")
        "communication_center", "communication center", "communications center", "centre des communications", "kommunikationszentrum" -> text("Centre des communications", "Communication Center", "Kommunikationszentrum", "İletişim Merkezi", "Центр связи", "通讯中心", "Centro comunicazioni")
        "command_center", "command center", "centre de commandement", "kommandozentrale" -> text("Centre de commandement", "Command Center", "Kommandozentrale", "Komuta Merkezi", "Командный центр", "指挥中心", "Centro di comando")
        "shieldbearer_barracks", "shieldbearer barracks", "caserne bouclier", "caserne des boucliers" -> text("Caserne de boucliers", "Shieldbearer Barracks", "Schildträger-Kaserne", "Kalkanlı Kışlası", "Казарма щитоносцев", "盾卫兵营", "Caserma scudieri")
        "shooter_barracks", "shooter barracks", "caserne tireurs", "caserne des tireurs" -> text("Caserne de tireurs", "Shooter Barracks", "Schützenkaserne", "Nişancı Kışlası", "Казарма стрелков", "射手兵营", "Caserma tiratori")
        "bomber_barracks", "bomber barracks", "caserne bombardiers", "caserne des bombardiers" -> text("Caserne de bombardiers", "Bomber Barracks", "Bomber-Kaserne", "Bombacı Kışlası", "Казарма бомбардировщиков", "轰炸兵营", "Caserma bombardieri")
        else -> text("Bureau du directeur", "Warden's Office", "Büro des Direktors", "Müdür Ofisi", "Офис директора", "典狱长办公室", "Ufficio del Direttore")
    }

    // Keep the coach inventory synchronized with every new resource capture.
    // Empty or uncertain OCR observations cannot erase confirmed quantities.
    LaunchedEffect(profileId, resources) {
        val now = System.currentTimeMillis()
        val observedStocks = resources.mapNotNull { resource ->
            val quantity = resource.quantiteNumerique?.takeIf { it.isFinite() && it >= 0 }?.toLong()
            if (quantity == null) null else PlayerResourceStock(
                canonicalProgressionResourceId(resource.nom), quantity, !resource.incertain, now
            )
        }
        val mergedStocks = mergeProgressionStocks(profile.resourceStocks, observedStocks)
        if (mergedStocks != profile.resourceStocks) {
            profile = profile.copy(resourceStocks = mergedStocks, updatedAtEpochMillis = now)
            prefs.edit().putString("profile", ProgressionProfileCodec.encode(profile)).apply()
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().heightIn(max = 640.dp)) {
        item {
            Text(
                text(fr = "Coach de progression", en = "Progression coach", de = "Fortschrittscoach",
                    tr = "İlerleme koçu", ru = "Помощник прогресса", zh = "成长助手", it = "Coach di progressione"),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text(fr = "Profil local : les objectifs et valeurs confirmées restent sur cet appareil.",
                    en = "Local profile: goals and confirmed values stay on this device.",
                    de = "Lokales Profil: Ziele und bestätigte Werte bleiben auf diesem Gerät.",
                    tr = "Yerel profil: hedefler ve onaylanan değerler bu cihazda kalır.",
                    ru = "Локальный профиль: цели и подтверждённые значения хранятся на устройстве.",
                    zh = "本地档案：目标和已确认的数据保存在此设备上。",
                    it = "Profilo locale: obiettivi e valori confermati restano su questo dispositivo."),
                style = MaterialTheme.typography.bodySmall
            )
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text(fr = "Nouvel objectif", en = "New goal", de = "Neues Ziel", tr = "Yeni hedef", ru = "Новая цель", zh = "新目标", it = "Nuovo obiettivo"),
                        style = MaterialTheme.typography.titleMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(progressionDomains) { domain ->
                            FilterChip(
                                selected = selectedDomain == domain,
                                onClick = {
                                    if (selectedDomain != domain) {
                                        selectedDomain = domain
                                        targetId = ""
                                        currentLevel = ""
                                        targetLevel = ""
                                        statusMessage = ""
                                    }
                                },
                                label = { Text(progressionLabel(domain, language)) }
                            )
                        }
                    }
                    Text(
                        when (selectedDomain) {
                            ProgressionDomain.BUILDING -> text(
                                "Les coûts sourcés sont disponibles uniquement pour les bâtiments présents dans le catalogue. Les niveaux absents restent inconnus.",
                                "Sourced costs are available only for buildings present in the catalog. Missing levels remain unknown.",
                                "Belegte Kosten gibt es nur für Gebäude im Katalog. Fehlende Stufen bleiben unbekannt.",
                                "Kaynaklı maliyetler yalnızca katalogdaki binalar için vardır. Eksik seviyeler bilinmiyor.",
                                "Стоимость из источников доступна только для зданий в каталоге. Остальные уровни неизвестны.",
                                "仅目录中列出的建筑提供有来源的费用，缺失等级保持未知。",
                                "I costi documentati sono disponibili solo per gli edifici nel catalogo. I livelli mancanti restano sconosciuti."
                            )
                            ProgressionDomain.HERO -> text(
                                "Les fragments sont communs à tous les héros. Utilise l'étape 0–31 : 0 = non recruté, 1 = recruté, 31 = 5★.",
                                "Shard costs are shared by all heroes. Use stage 0–31: 0 = not recruited, 1 = recruited, 31 = 5★.",
                                "Splitterkosten sind für alle Helden gleich. Stufe 0–31: 0 = nicht rekrutiert, 1 = rekrutiert, 31 = 5★.",
                                "Parça maliyetleri tüm kahramanlarda aynıdır. 0–31 aşamasını kullan: 0 = alınmadı, 1 = alındı, 31 = 5★.",
                                "Стоимость осколков одинакова для всех героев. Этап 0–31: 0 — не нанят, 1 — нанят, 31 — 5★.",
                                "所有英雄的碎片费用相同。使用阶段 0–31：0=未招募，1=已招募，31=5★。",
                                "I costi dei frammenti sono uguali per tutti gli eroi. Usa la fase 0–31: 0 = non reclutato, 1 = reclutato, 31 = 5★."
                            )
                            ProgressionDomain.ROBOT -> text(
                                "Les coûts des robots sont publiés par tranches de 10 niveaux. Les tranches partielles ne sont pas estimées ; le coût des modules avancés 60–70 reste à confirmer.",
                                "Robot costs are published in 10-level bands. Partial bands are not estimated; the 60–70 Advanced Power Module value needs confirmation.",
                                "Roboterkosten sind in 10-Level-Bereichen veröffentlicht. Teilbereiche werden nicht geschätzt; der Wert für erweiterte Module 60–70 muss bestätigt werden.",
                                "Robot maliyetleri 10 seviyelik aralıklarla yayımlanır. Kısmi aralıklar tahmin edilmez; 60–70 gelişmiş modül değeri doğrulanmalıdır.",
                                "Стоимость роботов опубликована диапазонами по 10 уровней. Частичные диапазоны не оцениваются; значение улучшенных модулей 60–70 требует подтверждения.",
                                "机器人费用按 10 级区间发布，不估算部分区间；60–70 级高级能量模块费用待确认。",
                                "I costi dei robot sono pubblicati per fasce di 10 livelli. Le fasce parziali non vengono stimate; il valore dei moduli avanzati 60–70 va confermato."
                            )
                            ProgressionDomain.ROBOT_CORE -> text(
                                "Les coûts Core I–V sont par module : Solo, Barrière ou Formation.",
                                "Core I–V costs are per module: Solo, Barrier or Formation.",
                                "Core-I–V-Kosten gelten pro Modul: Solo, Barriere oder Formation.",
                                "Core I–V maliyetleri modül başınadır: Solo, Bariyer veya Formasyon.",
                                "Стоимость Core I–V указана за модуль: Solo, Barrier или Formation.",
                                "Core I–V 费用按单个模块计算：Solo、Barrier 或 Formation。",
                                "I costi Core I–V sono per modulo: Solo, Barriera o Formazione."
                            )
                            ProgressionDomain.EQUIPMENT -> text(
                                "Les promotions d'équipement sont publiées par tranches de 20 niveaux. Pour la réforme séparée, utilise l'identifiant « reform ».",
                                "Equipment promotions are published in 20-level bands. For the separate reform track, use ID “reform”.",
                                "Ausrüstungsaufwertungen sind in 20-Level-Bereichen veröffentlicht. Für die separate Reform verwende die ID „reform“.",
                                "Ekipman yükseltmeleri 20 seviyelik aralıklarla yayımlanır. Ayrı reform için “reform” kimliğini kullan.",
                                "Улучшение снаряжения опубликовано диапазонами по 20 уровней. Для отдельной реформы используй ID «reform».",
                                "装备晋升按 20 级区间发布。单独的改造路线请使用 ID“reform”。",
                                "Le promozioni dell'equipaggiamento sono pubblicate per fasce di 20 livelli. Per la riforma separata usa l'ID «reform»."
                            )
                            else -> text(
                                "Les objectifs de cette catégorie peuvent être enregistrés, mais son catalogue détaillé de coûts et prérequis reste à compléter.",
                                "Goals in this category can be saved, but its detailed cost and prerequisite catalog is still pending.",
                                "Ziele dieser Kategorie können gespeichert werden; der detaillierte Kosten- und Voraussetzungenkatalog fehlt noch.",
                                "Bu kategorideki hedefler kaydedilebilir; ayrıntılı maliyet ve ön koşul kataloğu henüz tamamlanmadı.",
                                "Цели этой категории можно сохранить, но подробный каталог затрат и требований еще не готов.",
                                "可以保存此类别的目标，但详细费用和前置条件目录仍待补充。",
                                "Gli obiettivi di questa categoria possono essere salvati, ma il catalogo dettagliato di costi e prerequisiti è ancora da completare."
                            )
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = targetId,
                        onValueChange = { targetId = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text(fr = "Identifiant / nom (ex. ADA, Solo, casque, reform)", en = "ID / name (e.g. ADA, Solo, helmet, reform)", de = "ID / Name (z. B. ADA, Solo, Helm, reform)", tr = "Kimlik / ad (örn. ADA, Solo, miğfer, reform)", ru = "ID / название (например ADA, Solo, шлем, reform)", zh = "标识/名称（如 ADA、Solo、头盔、reform）", it = "ID / nome (es. ADA, Solo, elmo, reform)")) },
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = currentLevel, onValueChange = { currentLevel = it.filter(Char::isDigit) },
                            modifier = Modifier.weight(1f), singleLine = true,
                            label = { Text(text(fr = "Niveau actuel", en = "Current level", de = "Aktuelles Level", tr = "Mevcut seviye", ru = "Текущий уровень", zh = "当前等级", it = "Livello attuale")) }
                        )
                        OutlinedTextField(
                            value = targetLevel, onValueChange = { targetLevel = it.filter(Char::isDigit) },
                            modifier = Modifier.weight(1f), singleLine = true,
                            label = { Text(text(fr = "Niveau cible", en = "Target level", de = "Ziel-Level", tr = "Hedef seviye", ru = "Целевой уровень", zh = "目标等级", it = "Livello obiettivo")) }
                        )
                    }
                    Button(
                        onClick = {
                            val target = targetLevel.toIntOrNull()
                            val current = currentLevel.toIntOrNull()
                            if (targetId.isNotBlank() && target != null && current != null && target > current) {
                                val id = "${selectedDomain.name.lowercase()}-${targetId.trim().lowercase()}-$target"
                                val now = System.currentTimeMillis()
                                val objectives = upsertProgressionObjective(
                                    profile.objectives,
                                    ProgressionObjective(
                                        id, selectedDomain, targetId.trim(), target,
                                        isPrimary = false, createdAtEpochMillis = now
                                    )
                                )
                                // Keep the confirmed level history; confirmedLevel() selects the newest
                                // confirmed record and ignores later uncertain observations.
                                val levels = profile.levels +
                                    ProgressionLevelRecord(targetId.trim(), current, true, now)
                                val observedStocks = resources.mapNotNull { resource ->
                                    val quantity = resource.quantiteNumerique?.takeIf { it.isFinite() && it >= 0 }?.toLong()
                                    if (quantity == null) null else PlayerResourceStock(
                                        canonicalProgressionResourceId(resource.nom), quantity, !resource.incertain, now
                                    )
                                }
                                val stocks = mergeProgressionStocks(profile.resourceStocks, observedStocks)
                                profile = profile.copy(objectives = objectives, levels = levels,
                                    resourceStocks = stocks, updatedAtEpochMillis = now)
                                prefs.edit().putString("profile", ProgressionProfileCodec.encode(profile)).apply()
                                statusMessage = text(fr = "Objectif enregistré.", en = "Goal saved.", de = "Ziel gespeichert.",
                                    tr = "Hedef kaydedildi.", ru = "Цель сохранена.", zh = "目标已保存。", it = "Obiettivo salvato.")
                                currentLevel = ""
                                targetLevel = ""
                            } else {
                                statusMessage = text(fr = "Indique un niveau cible supérieur au niveau actuel.",
                                    en = "Enter a target level higher than the current level.",
                                    de = "Das Ziel-Level muss höher als das aktuelle Level sein.",
                                    tr = "Hedef seviye mevcut seviyeden yüksek olmalı.",
                                    ru = "Целевой уровень должен быть выше текущего.",
                                    zh = "目标等级必须高于当前等级。",
                                    it = "Il livello obiettivo deve essere superiore a quello attuale.")
                            }
                        }
                    ) {
                        Text(text(fr = "Enregistrer l’objectif", en = "Save goal", de = "Ziel speichern",
                            tr = "Hedefi kaydet", ru = "Сохранить цель", zh = "保存目标", it = "Salva obiettivo"))
                    }
                    if (statusMessage.isNotBlank()) Text(statusMessage, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Text(text(fr = "Objectifs enregistrés", en = "Saved goals", de = "Gespeicherte Ziele",
                tr = "Kaydedilen hedefler", ru = "Сохранённые цели", zh = "已保存目标", it = "Obiettivi salvati"),
                style = MaterialTheme.typography.titleMedium)
        }
        if (profile.objectives.isEmpty()) {
            item {
                Text(text(fr = "Aucun objectif pour le moment.", en = "No goals yet.", de = "Noch keine Ziele.",
                    tr = "Henüz hedef yok.", ru = "Пока нет целей.", zh = "暂无目标。", it = "Nessun obiettivo."),
                    style = MaterialTheme.typography.bodySmall)
            }
        } else {
            items(profile.objectives.size) { index ->
                val goal = profile.objectives[index]
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("${progressionLabel(goal.domain, language)} · ${goal.targetId}",
                            style = MaterialTheme.typography.titleSmall)
                        val savedCurrentLevel = profile.confirmedLevel(goal.targetId)
                        Text(text(
                            fr = "Niveau actuel : ${savedCurrentLevel?.toString() ?: "inconnu"} · Cible : ${goal.targetLevel}",
                            en = "Current level: ${savedCurrentLevel?.toString() ?: "unknown"} · Target: ${goal.targetLevel}",
                            de = "Aktuelles Level: ${savedCurrentLevel?.toString() ?: "unbekannt"} · Ziel: ${goal.targetLevel}",
                            tr = "Mevcut seviye: ${savedCurrentLevel?.toString() ?: "bilinmiyor"} · Hedef: ${goal.targetLevel}",
                            ru = "Текущий уровень: ${savedCurrentLevel?.toString() ?: "неизвестно"} · Цель: ${goal.targetLevel}",
                            zh = "当前等级：${savedCurrentLevel?.toString() ?: "未知"} · 目标：${goal.targetLevel}",
                            it = "Livello attuale: ${savedCurrentLevel?.toString() ?: "sconosciuto"} · Obiettivo: ${goal.targetLevel}"
                        ))
                        if (goal.isPrimary) Text(text(fr = "Objectif principal", en = "Primary goal", de = "Hauptziel",
                            tr = "Ana hedef", ru = "Основная цель", zh = "主要目标", it = "Obiettivo principale"),
                            color = MaterialTheme.colorScheme.primary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                profile = profile.copy(objectives = profile.objectives.map {
                                    it.copy(isPrimary = it.id == goal.id)
                                }, updatedAtEpochMillis = System.currentTimeMillis())
                                prefs.edit().putString("profile", ProgressionProfileCodec.encode(profile)).apply()
                            }) {
                                Text(text(fr = "Principal", en = "Make primary", de = "Als Hauptziel", tr = "Ana yap",
                                    ru = "Сделать главной", zh = "设为主要目标", it = "Imposta principale"))
                            }
                            Button(onClick = {
                                profile = profile.copy(
                                    objectives = removeProgressionObjective(profile.objectives, goal.id),
                                    updatedAtEpochMillis = System.currentTimeMillis()
                                )
                                prefs.edit().putString("profile", ProgressionProfileCodec.encode(profile)).apply()
                            }) {
                                Text(text(fr = "Supprimer", en = "Delete", de = "Löschen", tr = "Sil", ru = "Удалить", zh = "删除", it = "Elimina"))
                            }
                        }
                    }
                }
            }
        }

        if (supportedBuildingGoal != null) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(text(fr = "Calculateur : $buildingDisplayName", en = "$buildingDisplayName calculator",
                            de = "$buildingDisplayName-Rechner", tr = "$buildingDisplayName hesaplayıcısı",
                            ru = "Калькулятор: $buildingDisplayName", zh = "$buildingDisplayName 计算器",
                            it = "Calcolatore: $buildingDisplayName"), style = MaterialTheme.typography.titleMedium)
                        Text(text(fr = "Référence communautaire LOJ Wiki · consultée le 25/11/2026 · temps de base sans bonus.",
                            en = "LOJ Wiki community reference · checked 25/11/2026 · base time before bonuses.",
                            de = "Community-Referenz LOJ Wiki · geprüft am 25.11.2026 · Grundzeit ohne Boni.",
                            tr = "LOJ Wiki topluluk kaynağı · 25.11.2026 tarihinde kontrol edildi · bonuslar hariç temel süre.",
                            ru = "Сообщество LOJ Wiki · проверено 25.11.2026 · базовое время без бонусов.",
                            zh = "LOJ Wiki 社区参考数据 · 2026/11/25 核对 · 不含加成的基础时间。",
                            it = "Riferimento community LOJ Wiki · verificato il 25/11/2026 · tempo base senza bonus."),
                            style = MaterialTheme.typography.bodySmall)
                        if (buildingCurrentLevel == null) {
                            Text(text(fr = "Niveau actuel non confirmé : renseigne-le dans l’objectif.",
                                en = "Current level is not confirmed: enter it in the goal.",
                                de = "Das aktuelle Level ist nicht bestätigt: Gib es im Ziel ein.",
                                tr = "Mevcut seviye doğrulanmadı: hedefe gir.",
                                ru = "Текущий уровень не подтверждён: укажи его в цели.",
                                zh = "当前等级尚未确认，请在目标中填写。",
                                it = "Il livello attuale non è confermato: inseriscilo nell'obiettivo."),
                                style = MaterialTheme.typography.bodySmall)
                        } else if (supportedBuildingGoal.targetLevel <= buildingCurrentLevel) {
                            Text(text(fr = "Objectif déjà atteint.", en = "Target already reached.", de = "Ziel bereits erreicht.",
                                tr = "Hedefe ulaşıldı.", ru = "Цель уже достигнута.", zh = "目标已达成。",
                                it = "Obiettivo già raggiunto."), style = MaterialTheme.typography.bodySmall)
                        } else if (supportedBuildingGoal.targetLevel > buildingMaxLevel) {
                            Text(text(fr = "Les données sourcées disponibles couvrent ce bâtiment jusqu’au niveau $buildingMaxLevel seulement. Les coûts suivants restent inconnus et ne sont pas estimés.",
                                en = "Sourced data currently covers this building only through level $buildingMaxLevel. Later costs remain unknown and are not estimated.",
                                de = "Belegte Daten decken dieses Gebäude nur bis Level $buildingMaxLevel ab. Weitere Kosten sind unbekannt.",
                                tr = "Kaynaklı veriler bu binayı yalnızca $buildingMaxLevel. seviyeye kadar kapsar. Sonraki maliyetler bilinmiyor.",
                                ru = "Источники содержат данные по этому зданию только до уровня $buildingMaxLevel. Дальнейшие затраты неизвестны.",
                                zh = "有来源的数据仅覆盖此建筑至 $buildingMaxLevel 级，之后费用未知，不作估算。",
                                it = "I dati documentati coprono questo edificio solo fino al livello $buildingMaxLevel. I costi successivi restano sconosciuti."),
                                style = MaterialTheme.typography.bodySmall)
                        } else {
                            val catalogRows = buildingCatalog
                                .filter { it.step.toLevel > buildingCurrentLevel && it.step.toLevel <= supportedBuildingGoal.targetLevel }
                            val steps = catalogRows.map { it.step }
                            val normalizedStocks = profile.resourceStocks.map {
                                it.copy(resourceId = canonicalProgressionResourceId(it.resourceId))
                            }
                            val plan = ProgressionCoach.calculate(steps, normalizedStocks)
                            val prerequisiteSummary = catalogRows.flatMap { it.prerequisiteNotes }.distinct()
                            if (prerequisiteSummary.isNotEmpty()) {
                                Text(text(fr = "Prérequis : ${prerequisiteSummary.joinToString()}",
                                    en = "Prerequisites: ${prerequisiteSummary.joinToString()}",
                                    de = "Voraussetzungen: ${prerequisiteSummary.joinToString()}",
                                    tr = "Ön koşullar: ${prerequisiteSummary.joinToString()}",
                                    ru = "Требования: ${prerequisiteSummary.joinToString()}",
                                    zh = "前置条件：${prerequisiteSummary.joinToString()}",
                                    it = "Prerequisiti: ${prerequisiteSummary.joinToString()}"),
                                    style = MaterialTheme.typography.bodySmall)
                            }
                            Text(text(fr = "$buildingDisplayName : $buildingCurrentLevel → ${supportedBuildingGoal.targetLevel}",
                                en = "$buildingDisplayName: $buildingCurrentLevel → ${supportedBuildingGoal.targetLevel}",
                                de = "$buildingDisplayName: $buildingCurrentLevel → ${supportedBuildingGoal.targetLevel}",
                                tr = "$buildingDisplayName: $buildingCurrentLevel → ${supportedBuildingGoal.targetLevel}",
                                ru = "$buildingDisplayName: $buildingCurrentLevel → ${supportedBuildingGoal.targetLevel}",
                                zh = "$buildingDisplayName：$buildingCurrentLevel → ${supportedBuildingGoal.targetLevel}",
                                it = "$buildingDisplayName: $buildingCurrentLevel → ${supportedBuildingGoal.targetLevel}"))
                            Text(text(fr = "Détail des étapes", en = "Step-by-step costs", de = "Kosten je Stufe",
                                tr = "Aşama maliyetleri", ru = "Стоимость каждого этапа", zh = "逐级费用", it = "Costi per livello"),
                                style = MaterialTheme.typography.titleSmall)
                            plan.steps.forEach { step ->
                                val stepCosts = step.costs.joinToString(" · ") { cost ->
                                    val resourceLabel = when (canonicalProgressionResourceId(cost.resourceId)) {
                                        "food" -> text("Nourriture", "Food", "Nahrung", "Yiyecek", "Еда", "食物", "Cibo")
                                        "wood" -> text("Bois", "Wood", "Holz", "Odun", "Дерево", "木材", "Legno")
                                        "steel" -> text("Acier", "Steel", "Stahl", "Çelik", "Сталь", "钢材", "Acciaio")
                                        "gasoline" -> text("Essence", "Gasoline", "Benzin", "Benzin", "Бензин", "汽油", "Benzina")
                                        "gold" -> text("Argent", "Cash", "Bargeld", "Nakit", "Наличные", "现金", "Contanti")
                                        "diamonds" -> text("Diamants", "Diamonds", "Diamanten", "Elmas", "Алмазы", "钻石", "Diamanti")
                                        else -> cost.resourceId
                                    }
                                    "$resourceLabel ${NumberFormat.getIntegerInstance().format(cost.quantity)}"
                                }
                                Text(text(fr = "Niv. ${step.fromLevel} → ${step.toLevel} : $stepCosts",
                                    en = "Lv. ${step.fromLevel} → ${step.toLevel}: $stepCosts",
                                    de = "Lvl. ${step.fromLevel} → ${step.toLevel}: $stepCosts",
                                    tr = "Sv. ${step.fromLevel} → ${step.toLevel}: $stepCosts",
                                    ru = "Ур. ${step.fromLevel} → ${step.toLevel}: $stepCosts",
                                    zh = "等级 ${step.fromLevel} → ${step.toLevel}：$stepCosts",
                                    it = "Liv. ${step.fromLevel} → ${step.toLevel}: $stepCosts"),
                                    style = MaterialTheme.typography.bodySmall)
                                catalogRows.firstOrNull { it.step.id == step.id }
                                    ?.prerequisiteNotes
                                    ?.takeIf { it.isNotEmpty() }
                                    ?.let { prerequisites ->
                                        Text(text(fr = "Prérequis : ${prerequisites.joinToString()}",
                                            en = "Prerequisites: ${prerequisites.joinToString()}",
                                            de = "Voraussetzungen: ${prerequisites.joinToString()}",
                                            tr = "Ön koşullar: ${prerequisites.joinToString()}",
                                            ru = "Требования: ${prerequisites.joinToString()}",
                                            zh = "前置条件：${prerequisites.joinToString()}",
                                            it = "Prerequisiti: ${prerequisites.joinToString()}"),
                                            style = MaterialTheme.typography.labelSmall)
                                    }
                            }
                            plan.requirements.forEach { requirement ->
                                val label = when (requirement.resourceId) {
                                    "food" -> text("Nourriture", "Food", "Nahrung", "Yiyecek", "Еда", "食物", "Cibo")
                                    "wood" -> text("Bois", "Wood", "Holz", "Odun", "Дерево", "木材", "Legno")
                                    "steel" -> text("Acier", "Steel", "Stahl", "Çelik", "Сталь", "钢材", "Acciaio")
                                    "gasoline" -> text("Essence", "Gasoline", "Benzin", "Benzin", "Бензин", "汽油", "Benzina")
                                    "gold" -> text("Argent liquide", "Cash", "Bargeld", "Nakit", "Наличные", "现金", "Contanti")
                                    "diamonds" -> text("Diamants", "Diamonds", "Diamanten", "Elmas", "Алмазы", "钻石", "Diamanti")
                                    else -> requirement.resourceId
                                }
                                val required = NumberFormat.getIntegerInstance().format(requirement.required)
                                val owned = requirement.ownedConfirmed?.let { NumberFormat.getIntegerInstance().format(it) }
                                val missing = requirement.missing?.let { NumberFormat.getIntegerInstance().format(it) }
                                Text(if (owned == null) "$label : $required · " +
                                    text("stock inconnu", "stock unknown", "Bestand unbekannt", "stok bilinmiyor", "запас неизвестен", "库存未知", "scorta sconosciuta")
                                    else "$label : $required · " +
                                    text("possédé", "owned", "vorhanden", "mevcut", "есть", "拥有", "disponibile") + " $owned · " +
                                    text("manque", "missing", "fehlen", "eksik", "не хватает", "缺少", "mancano") + " $missing")
                            }
                            plan.totalKnownDurationSeconds?.let { duration ->
                                Text(text(fr = "Temps de base : ${duration / 86400} j ${(duration % 86400) / 3600} h ${(duration % 3600) / 60} min",
                                    en = "Base time: ${duration / 86400} d ${(duration % 86400) / 3600} h ${(duration % 3600) / 60} min",
                                    de = "Grundzeit: ${duration / 86400} T ${(duration % 86400) / 3600} Std. ${(duration % 3600) / 60} Min.",
                                    tr = "Temel süre: ${duration / 86400} g ${(duration % 86400) / 3600} sa ${(duration % 3600) / 60} dk",
                                    ru = "Базовое время: ${duration / 86400} д ${(duration % 86400) / 3600} ч ${(duration % 3600) / 60} мин",
                                    zh = "基础时间：${duration / 86400} 天 ${(duration % 86400) / 3600} 小时 ${(duration % 3600) / 60} 分",
                                    it = "Tempo base: ${duration / 86400} g ${(duration % 86400) / 3600} h ${(duration % 3600) / 60} min"))
                            }
                            Text(text(fr = "Coûts issus d’une référence communautaire, non testés en direct dans le jeu. Vérifie les prérequis du Bureau du directeur avant l’amélioration.",
                                en = "Costs come from a community reference, not live in-game testing. Check Warden's Office prerequisites before upgrading.",
                                de = "Kosten aus Community-Referenz, nicht live im Spiel getestet. Voraussetzungen des Büros vor dem Upgrade prüfen.",
                                tr = "Maliyetler topluluk kaynağından alınmıştır, oyunda canlı test edilmemiştir. Yükseltmeden önce gereksinimleri kontrol et.",
                                ru = "Стоимость взята из сообщества, не проверена в игре. Перед улучшением проверь требования.",
                                zh = "费用来自社区参考，未经游戏内实时测试。升级前请确认典狱长办公室前置条件。",
                                it = "Costi da una fonte community, non testati in gioco. Verifica i prerequisiti prima di migliorare."),
                                style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        items(supportedNonBuildingGoals.size) { index ->
            val goal = supportedNonBuildingGoals[index]
            val catalog = when (goal.domain) {
                ProgressionDomain.HERO -> ProgressionCatalog.heroShardProgression
                ProgressionDomain.ROBOT -> ProgressionCatalog.robotLeveling
                ProgressionDomain.ROBOT_CORE -> ProgressionCatalog.robotCore
                ProgressionDomain.EQUIPMENT -> if (goal.targetId.trim().lowercase() in
                    setOf("reform", "equipment_reform", "reformation")
                ) ProgressionCatalog.heroEquipmentReform else ProgressionCatalog.heroEquipmentLeveling
                else -> emptyList()
            }
            if (catalog.isNotEmpty()) {
                ProgressionCatalogGoalCard(goal, profile, catalog, language)
            }
        }
        item {
            Spacer(Modifier.height(4.dp))
            Text(text(fr = "Ressources connues", en = "Known resources", de = "Bekannte Ressourcen",
                tr = "Bilinen kaynaklar", ru = "Известные ресурсы", zh = "已知资源", it = "Risorse note"),
                style = MaterialTheme.typography.titleMedium)
            val confirmed = profile.resourceStocks.filter { it.confirmed }
            val uncertain = profile.resourceStocks.filterNot { it.confirmed }
            if (confirmed.isEmpty() && uncertain.isEmpty()) {
                Text(text(fr = "Aucun stock confirmé. Les captures incertaines ne sont pas utilisées.",
                    en = "No confirmed stock. Uncertain captures are not used.",
                    de = "Kein bestätigter Bestand. Unsichere Erfassungen werden nicht verwendet.",
                    tr = "Onaylanmış stok yok. Belirsiz ekran okumaları kullanılmaz.",
                    ru = "Нет подтверждённых запасов. Неуверенные распознавания не учитываются.",
                    zh = "没有已确认库存。不会使用不确定的截图识别结果。",
                    it = "Nessuna scorta confermata. Le letture incerte non vengono utilizzate."),
                    style = MaterialTheme.typography.bodySmall)
            } else {
                confirmed.forEach { stock ->
                    val resourceLabel = when (canonicalProgressionResourceId(stock.resourceId)) {
                        "food" -> text("Nourriture", "Food", "Nahrung", "Yiyecek", "Еда", "食物", "Cibo")
                        "wood" -> text("Bois", "Wood", "Holz", "Odun", "Дерево", "木材", "Legno")
                        "steel" -> text("Acier", "Steel", "Stahl", "Çelik", "Сталь", "钢材", "Acciaio")
                        "gasoline" -> text("Essence", "Gasoline", "Benzin", "Benzin", "Бензин", "汽油", "Benzina")
                        "gold" -> text("Argent liquide", "Cash", "Bargeld", "Nakit", "Наличные", "现金", "Contanti")
                        "diamonds" -> text("Diamants", "Diamonds", "Diamanten", "Elmas", "Алмазы", "钻石", "Diamanti")
                        else -> stock.resourceId
                    }
                    Text("$resourceLabel: ${NumberFormat.getIntegerInstance().format(stock.quantity)}")
                }
                if (uncertain.isNotEmpty()) {
                    Text(text(fr = "${uncertain.size} valeur(s) incertaine(s) ignorée(s).",
                        en = "${uncertain.size} uncertain value(s) ignored.",
                        de = "${uncertain.size} unsichere Werte ignoriert.",
                        tr = "${uncertain.size} belirsiz değer yok sayıldı.",
                        ru = "Не учтено неопределённых значений: ${uncertain.size}.",
                        zh = "已忽略不确定数值：${uncertain.size} 项。",
                        it = "Valori incerti ignorati: ${uncertain.size}."),
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item {
            Text(text(fr = "Les coûts et prérequis détaillés seront affichés quand les données du jeu seront sourcées et vérifiées. Aucun coût inconnu n’est estimé.",
                en = "Detailed costs and prerequisites will appear when game data is sourced and verified. Unknown costs are never estimated.",
                de = "Detaillierte Kosten und Voraussetzungen erscheinen nach Quellenprüfung. Unbekannte Kosten werden nicht geschätzt.",
                tr = "Ayrıntılı maliyetler ve ön koşullar, oyun verileri doğrulandığında gösterilir. Bilinmeyen maliyetler tahmin edilmez.",
                ru = "Подробные затраты и требования появятся после проверки данных. Неизвестные затраты не оцениваются.",
                zh = "游戏数据经来源核实后才显示详细成本和前置条件；不会估算未知成本。",
                it = "Costi e prerequisiti dettagliati appariranno quando i dati saranno verificati. I costi sconosciuti non vengono stimati."),
                style = MaterialTheme.typography.bodySmall)
        }
    }
}
