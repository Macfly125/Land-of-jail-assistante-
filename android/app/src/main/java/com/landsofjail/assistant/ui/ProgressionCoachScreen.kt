package com.landsofjail.assistant.ui

import android.content.Context
import java.text.NumberFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.landsofjail.assistant.model.ProgressionCoach
import com.landsofjail.assistant.model.canonicalProgressionResourceId
import com.landsofjail.assistant.model.ProgressionDomain
import com.landsofjail.assistant.model.ProgressionLevelRecord
import com.landsofjail.assistant.model.ProgressionObjective
import com.landsofjail.assistant.model.ProgressionPlayerProfile
import com.landsofjail.assistant.model.ProgressionProfileCodec
import com.landsofjail.assistant.model.ResourceRecord
import com.landsofjail.assistant.model.mergeProgressionStocks

private val progressionDomains = listOf(
    ProgressionDomain.BUILDING,
    ProgressionDomain.HERO,
    ProgressionDomain.RESEARCH,
    ProgressionDomain.ROBOT,
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
    val supportedBuildingGoal = profile.objectives.firstOrNull {
        it.domain == ProgressionDomain.BUILDING &&
            it.targetId.trim().lowercase() in setOf(
                "lab", "laboratory", "labo", "laboratoire",
                "warden_office", "warden's office", "bureau_du_directeur", "bureau du directeur", "hq"
            )
    }
    val isLabGoal = supportedBuildingGoal?.targetId?.trim()?.lowercase()?.let {
        it in setOf("lab", "laboratory", "labo", "laboratoire")
    } == true
    val buildingCurrentLevel = supportedBuildingGoal?.let { profile.confirmedLevel(it.targetId) }
    fun text(fr: String, en: String, de: String, tr: String, ru: String, zh: String, it: String) =
        when (language) {
            AppLanguage.FR -> fr; AppLanguage.EN -> en; AppLanguage.DE -> de
            AppLanguage.TR -> tr; AppLanguage.RU -> ru; AppLanguage.ZH -> zh; AppLanguage.IT -> it
        }

    val buildingDisplayName = if (isLabGoal)
        text("Laboratoire", "Laboratory", "Labor", "Laboratuvar", "Лаборатория", "实验室", "Laboratorio")
    else
        text("Bureau du directeur", "Warden's Office", "Büro des Direktors", "Müdür Ofisi", "Офис директора", "典狱长办公室", "Ufficio del Direttore")

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

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
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
                                onClick = { selectedDomain = domain },
                                label = { Text(progressionLabel(domain, language)) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = targetId,
                        onValueChange = { targetId = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text(fr = "Identifiant / nom", en = "ID / name", de = "ID / Name", tr = "Kimlik / ad", ru = "ID / название", zh = "标识 / 名称", it = "ID / nome")) },
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
                                val objectives = profile.objectives.filterNot { it.id == id } +
                                    ProgressionObjective(id, selectedDomain, targetId.trim(), target,
                                        isPrimary = profile.objectives.none { it.isPrimary }, createdAtEpochMillis = now)
                                val levels = profile.levels.filterNot { it.itemId == targetId.trim() } +
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
                                profile = profile.copy(objectives = profile.objectives.filterNot { it.id == goal.id }
                                    .mapIndexed { i, item -> if (i == 0) item.copy(isPrimary = true) else item },
                                    updatedAtEpochMillis = System.currentTimeMillis())
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
                        Text(if (isLabGoal) text(fr = "Calculateur du laboratoire", en = "Laboratory calculator", de = "Labor-Rechner",
                            tr = "Laboratuvar hesaplayıcısı", ru = "Калькулятор лаборатории", zh = "实验室计算器",
                            it = "Calcolatore del laboratorio")
                            else text(fr = "Calculateur du Bureau du directeur", en = "Warden's Office calculator", de = "Rechner Büro des Direktors",
                            tr = "Müdür Ofisi hesaplayıcısı", ru = "Калькулятор офиса директора", zh = "典狱长办公室计算器",
                            it = "Calcolatore Ufficio del Direttore"), style = MaterialTheme.typography.titleMedium)
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
                        } else if (supportedBuildingGoal.targetLevel > 30) {
                            Text(text(fr = "Le catalogue sourcé couvre ce bâtiment jusqu’au niveau 30. Les coûts au-delà restent inconnus et ne sont pas estimés.",
                                en = "The sourced catalog covers this building through level 30. Costs beyond that remain unknown and are not estimated.",
                                de = "Der belegte Katalog reicht für dieses Gebäude bis Level 30. Weitere Kosten sind unbekannt.",
                                tr = "Kaynaklı katalog bu binayı 30. seviyeye kadar kapsar. Sonraki maliyetler bilinmiyor.",
                                ru = "Каталог содержит данные по этому зданию до 30-го уровня. Дальнейшие затраты неизвестны.",
                                zh = "有来源的目录仅覆盖此建筑至 30 级，之后费用未知，不作估算。",
                                it = "Il catalogo documentato copre questo edificio fino al livello 30. I costi successivi restano sconosciuti."),
                                style = MaterialTheme.typography.bodySmall)
                        } else {
                            val catalog = if (isLabGoal) ProgressionCatalog.labBuilding else ProgressionCatalog.wardensOffice
                            val catalogRows = catalog
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
                    Text("${stock.resourceId}: ${stock.quantity}")
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
