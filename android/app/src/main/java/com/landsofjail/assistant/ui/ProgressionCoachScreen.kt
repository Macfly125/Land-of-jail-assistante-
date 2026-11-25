package com.landsofjail.assistant.ui

import android.content.Context
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.landsofjail.assistant.model.AppLanguage
import com.landsofjail.assistant.model.PlayerResourceStock
import com.landsofjail.assistant.model.ProgressionDomain
import com.landsofjail.assistant.model.ProgressionLevelRecord
import com.landsofjail.assistant.model.ProgressionObjective
import com.landsofjail.assistant.model.ProgressionPlayerProfile
import com.landsofjail.assistant.model.ProgressionProfileCodec
import com.landsofjail.assistant.model.ResourceRecord

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

    fun text(fr: String, en: String, de: String, tr: String, ru: String, zh: String, it: String) =
        when (language) {
            AppLanguage.FR -> fr; AppLanguage.EN -> en; AppLanguage.DE -> de
            AppLanguage.TR -> tr; AppLanguage.RU -> ru; AppLanguage.ZH -> zh; AppLanguage.IT -> it
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
                                        resource.nom.lowercase(), quantity, !resource.incertain, now
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
