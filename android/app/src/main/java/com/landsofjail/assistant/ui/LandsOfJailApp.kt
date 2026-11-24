package com.landsofjail.assistant.ui

import android.content.Context
import android.content.pm.PackageManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.content.Intent
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.landsofjail.assistant.capture.CapturedScreenPayload
import com.landsofjail.assistant.capture.ScreenCaptureConsentActivity
import com.landsofjail.assistant.data.GeminiAssistantRepository
import com.landsofjail.assistant.data.GameIntelSnapshot
import com.landsofjail.assistant.data.GiftCodeRepository
import com.landsofjail.assistant.data.GiftCodeSnapshot
import com.landsofjail.assistant.data.GiftCodeEntry
import android.content.ClipboardManager
import android.content.ClipData
import com.landsofjail.assistant.data.SupabaseAuthRepository
import com.landsofjail.assistant.model.*
import com.landsofjail.assistant.ocr.LandsOfJailOcrParser
import com.landsofjail.assistant.service.GameForegroundMonitorService
import com.landsofjail.assistant.service.UsageStatsDetector
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

private fun localizedUiText(
    language: AppLanguage,
    fr: String,
    en: String,
    de: String,
    tr: String,
    ru: String,
    zh: String,
    it: String = en
): String = when (language) {
    AppLanguage.FR -> fr
    AppLanguage.EN -> en
    AppLanguage.DE -> de
    AppLanguage.TR -> tr
    AppLanguage.RU -> ru
    AppLanguage.ZH -> zh
    AppLanguage.IT -> it
}




internal fun localizedCaptureCategory(language: AppLanguage, raw: String): String {
    val key = raw.lowercase()
    if (language == AppLanguage.IT) return when (key) {
        "heros", "heroes" -> "Eroi"
        "robots" -> "Robot"
        "power_armor" -> "Hangar armature potenziate"
        "forticlad_lab" -> "Laboratorio Forticlad"
        "lab" -> "Laboratorio"
        "cosmos_hub" -> "Centro cosmico"
        "dungeon" -> "Prigione"
        "equipements", "equipment", "gear" -> "Miglioramento equipaggiamento"
        "ressources", "resources" -> "Risorse"
        "batiments", "buildings" -> "Edifici"
        "troupes", "troops" -> "Truppe"
        "profil", "profile" -> "Profilo"
        "gorilla_legion" -> "Legione dei Gorilla"
        "trial_cage" -> "Gabbia di prova"
        "crystal_clash" -> "Scontro cristallino"
        "sandsea_conquest" -> "Conquista del Mare di Sabbia"
        "svs" -> "SvS"
        "turmoil_surge" -> "Ondata di caos"
        "gold_hunter" -> "Cacciatore d'oro"
        else -> "Altra schermata"
    }
    return when (key) {
        "heros", "heroes" -> localizedUiText(language, "Héros", "Heroes", "Helden", "Kahramanlar", "Герои", "英雄")
        "robots" -> localizedUiText(language, "Robots", "Robots", "Roboter", "Robotlar", "Роботы", "机器人")
        "power_armor" -> localizedUiText(language, "Hangar d’armure de puissance", "Power Armor Hangar", "Power-Armor-Hangar", "Güç Zırhı Hangarı", "Ангар силовой брони", "动力装甲机库")
        "forticlad_lab" -> localizedUiText(language, "Laboratoire Forticlad", "Forticlad Lab", "Forticlad-Labor", "Forticlad Laboratuvarı", "Лаборатория Forticlad", "Forticlad 实验室")
        "lab" -> localizedUiText(language, "Laboratoire", "Lab", "Labor", "Laboratuvar", "Лаборатория", "实验室")
        "cosmos_hub" -> localizedUiText(language, "Centre cosmique", "Cosmos Hub", "Kosmos-Zentrum", "Kozmos Merkezi", "Космический центр", "宇宙中心")
        "dungeon" -> localizedUiText(language, "Donjon", "Dungeon", "Verlies", "Zindan", "Подземелье", "地牢")
        "equipements", "equipment", "gear" -> localizedUiText(language, "Amélioration d'équipements", "Equipment upgrades", "Ausrüstungsverbesserung", "Ekipman geliştirme", "Улучшение снаряжения", "装备升级")
        "ressources", "resources" -> localizedUiText(language, "Ressources", "Resources", "Ressourcen", "Kaynaklar", "Ресурсы", "资源")
        "batiments", "buildings" -> localizedUiText(language, "Bâtiments", "Buildings", "Gebäude", "Binalar", "Здания", "建筑")
        "troupes", "troops" -> localizedUiText(language, "Troupes", "Troops", "Truppen", "Birlikler", "Войска", "部队")
        "profil", "profile" -> localizedUiText(language, "Profil", "Profile", "Profil", "Profil", "Профиль", "个人资料")
        "gorilla_legion" -> localizedUiText(language, "Légion de Gorilla", "Gorilla Legion", "Gorilla-Legion", "Goril Lejyonu", "Легион горилл", "大猩猩军团")
        "trial_cage" -> localizedUiText(language, "Cage d’essai", "Trial Cage", "Prüfungskäfig", "Deneme Kafesi", "Испытательная клетка", "试炼笼")
        "crystal_clash" -> localizedUiText(language, "Conflit cristallin", "Crystal Clash", "Kristallkonflikt", "Kristal Çatışması", "Кристальная битва", "水晶冲突")
        "sandsea_conquest" -> localizedUiText(language, "Conquête de sable", "Sandsea Conquest", "Sandmeer-Eroberung", "Kum Denizi Fethi", "Покорение песчаного моря", "沙海征服")
        "svs" -> "SvS"
        "turmoil_surge" -> localizedUiText(language, "Vague de chaos", "Turmoil Surge", "Chaoswelle", "Kaos Dalgası", "Волна хаоса", "混乱浪潮")
        "gold_hunter" -> localizedUiText(language, "Chasseur d’or", "Gold Hunter", "Goldjäger", "Altın Avcısı", "Золотой охотник", "黄金猎人")
        else -> localizedUiText(language, "Autre capture", "Other capture", "Andere Aufnahme", "Diğer yakalama", "Другая запись", "其他截图")
    }
}

internal fun localizedResourceName(language: AppLanguage, raw: String): String {
    val key = raw.lowercase()
    if (language == AppLanguage.IT) return when {
        key.contains("nourriture") || key.contains("food") -> "Cibo"
        key.contains("bois") || key.contains("wood") -> "Legno"
        key.contains("acier") || key.contains("steel") -> "Acciaio"
        key.contains("essence") || key.contains("gasoline") -> "Benzina"
        key.contains("diamant") || key.contains("gem") -> "Diamanti"
        key.contains("cash") || key == "or" || key.contains("gold") || key.contains("altın") || key.contains("золото") || key.contains("金币") || key.contains("黄金") -> "Oro"
        else -> raw
    }
    return when {
        key.contains("nourriture") || key.contains("food") -> localizedUiText(language, "Nourriture", "Food", "Nahrung", "Yiyecek", "Еда", "食物")
        key.contains("bois") || key.contains("wood") -> localizedUiText(language, "Bois", "Wood", "Holz", "Odun", "Дерево", "木材")
        key.contains("acier") || key.contains("steel") -> localizedUiText(language, "Acier", "Steel", "Stahl", "Çelik", "Сталь", "钢材")
        key.contains("essence") || key.contains("gasoline") -> localizedUiText(language, "Essence", "Gasoline", "Benzin", "Benzin", "Бензин", "汽油")
        key.contains("diamant") || key.contains("gem") -> localizedUiText(language, "Diamants", "Gems", "Diamanten", "Elmas", "Алмазы", "钻石")
        key.contains("cash") || key == "or" || key.contains("gold") || key.contains("altın") || key.contains("золото") || key.contains("金币") || key.contains("黄金") -> localizedUiText(language, "Or", "Gold", "Gold", "Altın", "Золото", "金币")
        else -> raw
    }
}

internal fun localizedEquipmentPiece(language: AppLanguage, raw: String): String {
    val key = raw.lowercase().trim()
    if (language == AppLanguage.IT) return when {
        key.contains("arme") || key.contains("weapon") -> "Arma"
        key.contains("armure") || key.contains("armor") -> "Armatura"
        key.contains("casque") || key.contains("helmet") || key.contains("helm") -> "Elmo"
        key.contains("gant") || key.contains("glove") -> "Guanti"
        key.contains("botte") || key.contains("boot") || key == "bot" -> "Stivali"
        key.contains("accessoire") || key.contains("accessory") -> "Accessorio"
        else -> raw
    }
    return when {
        key.contains("arme") || key.contains("weapon") || key.contains("waffe") || key.contains("silah") || key.contains("оруж") || key.contains("武器") ->
            localizedUiText(language, "Arme", "Weapon", "Waffe", "Silah", "Оружие", "武器")
        key.contains("armure") || key.contains("armor") || key.contains("rüstung") || key.contains("zırh") || key.contains("брон") || key.contains("护甲") ->
            localizedUiText(language, "Armure", "Armor", "Rüstung", "Zırh", "Броня", "护甲")
        key.contains("casque") || key.contains("helmet") || key.contains("helm") || key.contains("miğfer") || key.contains("шлем") || key.contains("头盔") ->
            localizedUiText(language, "Casque", "Helmet", "Helm", "Miğfer", "Шлем", "头盔")
        key.contains("gant") || key.contains("glove") || key.contains("handschuh") || key.contains("eldiven") || key.contains("перчат") || key.contains("手套") ->
            localizedUiText(language, "Gants", "Gloves", "Handschuhe", "Eldiven", "Перчатки", "手套")
        key.contains("botte") || key.contains("boot") || key.contains("stiefel") || key == "bot" || key.contains("сапог") || key.contains("靴") ->
            localizedUiText(language, "Bottes", "Boots", "Stiefel", "Bot", "Сапоги", "靴子")
        key.contains("accessoire") || key.contains("accessory") || key.contains("zubehör") || key.contains("aksesuar") || key.contains("аксессуар") || key.contains("配件") ->
            localizedUiText(language, "Accessoire", "Accessory", "Zubehör", "Aksesuar", "Аксессуар", "配件")
        else -> raw
    }
}

private fun localizedCaptureSummary(item: ScreenshotAnalysis, language: AppLanguage): String {
    val parts = buildList {
        item.puissance?.puissanceTotaleTexte?.takeIf { it.isNotBlank() }?.let {
            add(if (language == AppLanguage.IT) "Potenza: $it" else localizedUiText(language, "Puissance : $it", "Power: $it", "Macht: $it", "Güç: $it", "Мощь: $it", "战力：$it"))
        }
        if (item.ressources.isNotEmpty()) {
            val values = item.ressources.joinToString(" · ") {
                "${localizedResourceName(language, it.nom)}: ${it.quantiteTexte}"
            }
            add(if (language == AppLanguage.IT) "Risorse: $values" else localizedUiText(language, "Ressources : $values", "Resources: $values", "Ressourcen: $values", "Kaynaklar: $values", "Ресурсы: $values", "资源：$values"))
        }
        if (item.heros.isNotEmpty()) {
            val names = item.heros.take(4).joinToString(", ") { it.nom }
            add(if (language == AppLanguage.IT) "Eroi: $names" else localizedUiText(language, "Héros : $names", "Heroes: $names", "Helden: $names", "Kahramanlar: $names", "Герои: $names", "英雄：$names"))
        }
        if (item.robots.isNotEmpty()) {
            val names = item.robots.take(3).joinToString(", ") { it.nom }
            add(if (language == AppLanguage.IT) "Robot: $names" else localizedUiText(language, "Robots : $names", "Robots: $names", "Roboter: $names", "Robotlar: $names", "Роботы: $names", "机器人：$names"))
        }
        if (item.equipements.isNotEmpty()) {
            val details = item.equipements.take(4).joinToString(", ") {
                if (it.niveau.isNotBlank()) "${localizedEquipmentPiece(language, it.piece)} ${localizedUiText(language, "Niv.", "Lv.", "Stufe", "Sv.", "Ур.", "等级")} ${it.niveau}" else localizedEquipmentPiece(language, it.piece)
            }
            add(if (language == AppLanguage.IT) "Equipaggiamento: $details" else localizedUiText(language, "Équipements : $details", "Equipment: $details", "Ausrüstung: $details", "Ekipman: $details", "Снаряжение: $details", "装备：$details"))
        }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
        ?: if (language == AppLanguage.IT) "Nessun valore letto con certezza; verifica lo screenshot."
        else localizedUiText(language, "Aucune valeur lue avec certitude ; vérification de la capture nécessaire.", "No values read with confidence; screenshot review is needed.", "Keine Werte sicher gelesen; Screenshot muss geprüft werden.", "Hiçbir değer güvenle okunamadı; ekran görüntüsü kontrol edilmeli.", "Не удалось уверенно прочитать значения; снимок нужно проверить.", "未能可靠读取数值。")
}


private val LocalAssistantLanguage = staticCompositionLocalOf { AppLanguage.FR }

/** Central UI fallback for screens whose labels were previously hard-coded in French. */
@Composable
private fun localizedAppText(source: String): String {
    val language = LocalAssistantLanguage.current
    if (language == AppLanguage.FR) return source
    val index = when (language) {
        AppLanguage.FR -> 0
        AppLanguage.EN -> 1
        AppLanguage.DE -> 2
        AppLanguage.TR -> 3
        AppLanguage.RU -> 4
        AppLanguage.ZH -> 5
        AppLanguage.IT -> 6
    }
    val dictionary = mapOf(
        "Annuler" to listOf("Annuler", "Cancel", "Abbrechen", "İptal", "Отмена", "取消"),
        "Synchronisation terminée. Aucune sauvegarde distante n’a été supprimée." to listOf("Synchronisation terminée. Aucune sauvegarde distante n’a été supprimée.", "Sync complete. No remote backup was deleted.", "Synchronisierung abgeschlossen. Kein Remote-Backup wurde gelöscht.", "Senkronizasyon tamamlandı. Hiçbir uzak yedek silinmedi.", "Синхронизация завершена. Ни одна удалённая резервная копия не удалена.", "同步完成。没有删除任何远程备份。"),
        "Réglages capture" to listOf("Réglages capture", "Capture settings", "Aufnahmeeinstellungen", "Yakalama ayarları", "Настройки захвата", "捕获设置"),
        "Synchronisation…" to listOf("Synchronisation…", "Syncing…", "Synchronisierung…", "Eşitleniyor…", "Синхронизация…", "正在同步…"),
        "Synchroniser avec le cloud" to listOf("Synchroniser avec le cloud", "Sync with cloud", "Mit der Cloud synchronisieren", "Bulutla eşitle", "Синхронизировать с облаком", "与云端同步"),
        "Masquer la fiche" to listOf("Masquer la fiche", "Hide profile", "Profil ausblenden", "Profili gizle", "Скрыть профиль", "隐藏档案"),
        "Fiche complète" to listOf("Fiche complète", "Full profile", "Vollständiges Profil", "Tam profil", "Полный профиль", "完整档案"),
        "Voir la fiche complète" to listOf("Voir la fiche complète", "View full profile", "Vollständiges Profil anzeigen", "Tam profili görüntüle", "Посмотреть полный профиль", "查看完整档案"),
        "Description non disponible" to listOf("Description non disponible", "Description unavailable", "Beschreibung nicht verfügbar", "Açıklama mevcut değil", "Описание недоступно", "暂无描述"),
        "Les corrections manuelles remplacent les valeurs incertaines de la capture." to listOf("Les corrections manuelles remplacent les valeurs incertaines de la capture.", "Manual corrections replace uncertain values from the capture.", "Manuelle Korrekturen ersetzen unsichere Werte aus der Aufnahme.", "Elle yapılan düzeltmeler, ekran görüntüsündeki belirsiz değerlerin yerine geçer.", "Ручные исправления заменяют неточные значения со снимка.", "手动更正会替换截图中不确定的数值。"),
        "Conserve tes résultats, scores et observations pour comparer tes propres performances entre les sessions." to listOf("Conserve tes résultats, scores et observations pour comparer tes propres performances entre les sessions.", "Keep your results, scores and notes to compare your performance between sessions.", "Bewahre Ergebnisse, Punktzahlen und Notizen auf, um deine Leistung zwischen Sitzungen zu vergleichen.", "Oturumlar arasında performansını karşılaştırmak için sonuçlarını, puanlarını ve notlarını sakla.", "Сохраняйте результаты, очки и заметки, чтобы сравнивать свои показатели между сессиями.", "保存结果、分数和备注，以便比较不同场次的表现。"),
        "Date (ex. 23/11/2026)" to listOf("Date (ex. 23/11/2026)", "Date (e.g. 23/11/2026)", "Datum (z. B. 23.11.2026)", "Tarih (örn. 23/11/2026)", "Дата (например, 23.11.2026)", "日期（例如 2026/11/23）"),
        "Score / dégâts / classement" to listOf("Score / dégâts / classement", "Score / damage / ranking", "Punktzahl / Schaden / Rang", "Puan / hasar / sıralama", "Очки / урон / рейтинг", "分数 / 伤害 / 排名"),
        "Composition, ratio, remarques" to listOf("Composition, ratio, remarques", "Composition, ratio, notes", "Zusammensetzung, Verhältnis, Notizen", "Dizilim, oran, notlar", "Состав, соотношение, заметки", "阵容、比例、备注"),
        "Voir les nouvelles demandes" to listOf("Voir les nouvelles demandes", "View new requests", "Neue Anfragen anzeigen", "Yeni istekleri görüntüle", "Посмотреть новые запросы", "查看新申请"),
        "Renvoyer l’e-mail de confirmation" to listOf("Renvoyer l’e-mail de confirmation", "Resend confirmation email", "Bestätigungs-E-Mail erneut senden", "Onay e-postasını yeniden gönder", "Повторно отправить письмо подтверждения", "重新发送确认邮件"),
        "S’inscrire" to listOf("S’inscrire", "Sign up", "Registrieren", "Kaydol", "Зарегистрироваться", "注册"),
        "Se connecter" to listOf("Se connecter", "Sign in", "Anmelden", "Giriş yap", "Войти", "登录"),
        "Fermer la file (100 %)" to listOf("Fermer la file (100 %)", "Close queue (100%)", "Warteschlange schließen (100 %)", "Kuyruğu kapat (%100)", "Закрыть очередь (100 %)", "关闭队列（100%）"),
        "Bienvenue · Inscription joueur" to listOf("Bienvenue · Inscription joueur", "Welcome · Player registration", "Willkommen · Spielerregistrierung", "Hoş geldiniz · Oyuncu kaydı", "Добро пожаловать · Регистрация игрока", "欢迎 · 玩家注册"),
        "Renseigne ton identité dans Land of Jail. Ces informations seront enregistrées sur cet appareil et pourront être transmises aux officiers de ton alliance après connexion." to listOf("Renseigne ton identité dans Land of Jail. Ces informations seront enregistrées sur cet appareil et pourront être transmises aux officiers de ton alliance après connexion.", "Enter your Land of Jail identity. This information is saved on this device and can be shared with your alliance officers after you sign in.", "Gib deine Land-of-Jail-Spielerdaten ein. Diese Informationen werden auf diesem Gerät gespeichert und können nach der Anmeldung an deine Allianz-Offiziere weitergegeben werden.", "Land of Jail oyuncu bilgilerini gir. Bu bilgiler cihazda saklanır ve giriş yaptıktan sonra ittifak yetkilileriyle paylaşılabilir.", "Укажите данные игрока Land of Jail. Они сохранятся на устройстве и после входа могут быть переданы офицерам альянса.", "填写你的 Land of Jail 玩家信息。这些信息会保存在此设备上，登录后可分享给联盟官员。"),
        "Pseudo dans le jeu" to listOf("Pseudo dans le jeu", "In-game name", "Spielername im Spiel", "Oyun içi ad", "Игровое имя", "游戏昵称"),
        "Serveur (ex. 311)" to listOf("Serveur (ex. 311)", "Server (e.g. 311)", "Server (z. B. 311)", "Sunucu (örn. 311)", "Сервер (например, 311)", "服务器（例如 311）"),
        "Initiales / tag de l'alliance" to listOf("Initiales / tag de l'alliance", "Alliance initials / tag", "Allianz-Kürzel / Tag", "İttifak baş harfleri / etiketi", "Аббревиатура / тег альянса", "联盟缩写 / 标签"),
        "Ton rang déclaré" to listOf("Ton rang déclaré", "Your declared rank", "Dein angegebener Rang", "Belirttiğin rütbe", "Указанный ранг", "你填写的职位"),
        "Enregistrer et continuer" to listOf("Enregistrer et continuer", "Save and continue", "Speichern und fortfahren", "Kaydet ve devam et", "Сохранить и продолжить", "保存并继续"),
        "Plus tard" to listOf("Plus tard", "Later", "Später", "Daha sonra", "Позже", "稍后"),
        "Autoriser Statistiques d'utilisation" to listOf("Autoriser Statistiques d'utilisation", "Allow usage access", "Nutzungszugriff erlauben", "Kullanım erişimine izin ver", "Разрешить доступ к статистике использования", "允许使用情况访问权限"),
        "Autoriser affichage par-dessus les applis" to listOf("Autoriser affichage par-dessus les applis", "Allow display over other apps", "Anzeige über anderen Apps erlauben", "Diğer uygulamaların üzerinde gösterime izin ver", "Разрешить отображение поверх других приложений", "允许显示在其他应用上层"),
        "Héros suivis" to listOf("Héros suivis", "Tracked heroes", "Verfolgte Helden", "Takip edilen kahramanlar", "Отслеживаемые герои", "已跟踪英雄"),
        "Rôle du joueur" to listOf("Rôle du joueur", "Player role", "Spielerrolle", "Oyuncu rolü", "Роль игрока", "玩家定位"),
        "Rareté lue sur capture" to listOf("Rareté lue sur capture", "Rarity read from screenshot", "Seltenheit aus Screenshot erkannt", "Ekran görüntüsünden okunan nadirlik", "Редкость, распознанная на снимке", "截图识别的稀有度"),
        "Niveau, étoiles et compétences personnels : à scanner ou saisir depuis le jeu." to listOf("Niveau, étoiles et compétences personnels : à scanner ou saisir depuis le jeu.", "Your level, stars and skills: scan or enter them from the game.", "Dein Level, Sterne und Fähigkeiten: im Spiel scannen oder eingeben.", "Kişisel seviye, yıldız ve beceriler: oyundan tara veya gir.", "Личный уровень, звёзды и навыки: отсканируйте или введите из игры.", "个人等级、星级和技能：请从游戏中扫描或填写。"),
        "Fiche de référence" to listOf("Fiche de référence", "Reference profile", "Referenzprofil", "Referans profili", "Справочная карточка", "参考资料"),
        "À vérifier" to listOf("À vérifier", "Needs verification", "Zu überprüfen", "Kontrol edilmeli", "Требует проверки", "待核实"),
        "Consulter la source du catalogue" to listOf("Consulter la source du catalogue", "View catalog source", "Katalogquelle anzeigen", "Katalog kaynağını görüntüle", "Открыть источник каталога", "查看目录来源"),
        "Autres héros détectés" to listOf("Autres héros détectés", "Other detected heroes", "Weitere erkannte Helden", "Diğer algılanan kahramanlar", "Другие обнаруженные герои", "其他已识别英雄"),
        "Modifier les données détectées" to listOf("Modifier les données détectées", "Edit detected data", "Erkannte Daten bearbeiten", "Algılanan verileri düzenle", "Изменить распознанные данные", "编辑识别的数据"),
        "Corriger le héros détecté" to listOf("Corriger le héros détecté", "Correct detected hero", "Erkannten Helden korrigieren", "Algılanan kahramanı düzelt", "Исправить распознанного героя", "修正识别的英雄"),
        "Nom du héros" to listOf("Nom du héros", "Hero name", "Heldenname", "Kahraman adı", "Имя героя", "英雄名称"),
        "Niveau" to listOf("Niveau", "Level", "Stufe", "Seviye", "Уровень", "等级"),
        "Rareté" to listOf("Rareté", "Rarity", "Seltenheit", "Nadirlik", "Редкость", "稀有度"),
        "Étoiles" to listOf("Étoiles", "Stars", "Sterne", "Yıldızlar", "Звёзды", "星级"),
        "Puissance" to listOf("Puissance", "Power", "Macht", "Güç", "Мощь", "战力"),
        "Enregistrer" to listOf("Enregistrer", "Save", "Speichern", "Kaydet", "Сохранить", "保存"),
        "Bonus actifs / statistiques" to listOf("Bonus actifs / statistiques", "Active bonuses / stats", "Aktive Boni / Werte", "Aktif bonuslar / istatistikler", "Активные бонусы / характеристики", "生效加成 / 属性"),
        "Compétences / effets (facultatif)" to listOf("Compétences / effets (facultatif)", "Skills / effects (optional)", "Fähigkeiten / Effekte (optional)", "Beceriler / etkiler (isteğe bağlı)", "Навыки / эффекты (необязательно)", "技能 / 效果（可选）"),
        "Journal des événements du profil" to listOf("Journal des événements du profil", "Profile event log", "Ereignisprotokoll des Profils", "Profil etkinlik günlüğü", "Журнал событий профиля", "个人资料活动记录"),
        "Enregistrer le résultat" to listOf("Enregistrer le résultat", "Save result", "Ergebnis speichern", "Sonucu kaydet", "Сохранить результат", "保存结果"),
        "Supprimer l’entrée" to listOf("Supprimer l’entrée", "Delete entry", "Eintrag löschen", "Kaydı sil", "Удалить запись", "删除条目"),
        "Éclats en stock" to listOf("Éclats en stock", "Shards in stock", "Splitter auf Lager", "Stoktaki parçalar", "Осколки в запасе", "库存碎片"),
        "Contrôle des données avant recommandation" to listOf("Contrôle des données avant recommandation", "Data check before recommendation", "Datenprüfung vor Empfehlung", "Öneri öncesi veri kontrolü", "Проверка данных перед рекомендацией", "推荐前数据检查"),
        "Synthèse" to listOf("Synthèse", "Summary", "Zusammenfassung", "Özet", "Сводка", "摘要"),
        "AVERTISSEMENT — CONSEILS NON VÉRIFIÉS" to listOf("AVERTISSEMENT — CONSEILS NON VÉRIFIÉS", "WARNING — UNVERIFIED ADVICE", "WARNUNG — NICHT ÜBERPRÜFTE TIPPS", "UYARI — DOĞRULANMAMIŞ ÖNERİLER", "ВНИМАНИЕ — НЕПРОВЕРЕННЫЕ СОВЕТЫ", "警告 — 未经验证的建议"),
        "Sources Internet, Photos & Dates" to listOf("Sources Internet, Photos & Dates", "Internet sources, photos & dates", "Internetquellen, Fotos & Daten", "İnternet kaynakları, fotoğraflar ve tarihler", "Интернет-источники, фото и даты", "网络来源、图片和日期"),
        "Effacer l’historique ?" to listOf("Effacer l’historique ?", "Clear history?", "Verlauf löschen?", "Geçmiş temizlensin mi?", "Очистить историю?", "清除历史记录？"),
        "Effacer" to listOf("Effacer", "Clear", "Löschen", "Temizle", "Очистить", "清除"),
        "Effacer l’historique" to listOf("Effacer l’historique", "Clear history", "Verlauf löschen", "Geçmişi temizle", "Очистить историю", "清除历史记录"),
        "Compte & sauvegarde cloud" to listOf("Compte & sauvegarde cloud", "Account & cloud backup", "Konto & Cloud-Sicherung", "Hesap ve bulut yedekleme", "Аккаунт и облачная копия", "账户与云备份"),
        "Adresse e-mail" to listOf("Adresse e-mail", "Email address", "E-Mail-Adresse", "E-posta adresi", "Адрес электронной почты", "电子邮箱"),
        "Actualiser" to listOf("Actualiser", "Refresh", "Aktualisieren", "Yenile", "Обновить", "刷新"),
        "Déconnexion" to listOf("Déconnexion", "Sign out", "Abmelden", "Çıkış yap", "Выйти", "退出登录"),
        "Profil joueur et alliance" to listOf("Profil joueur et alliance", "Player and alliance profile", "Spieler- und Allianzprofil", "Oyuncu ve ittifak profili", "Профиль игрока и альянса", "玩家与联盟资料"),
        "Ton rang actuel" to listOf("Ton rang actuel", "Your current rank", "Dein aktueller Rang", "Mevcut rütben", "Ваш текущий ранг", "你当前的职位"),
        "Demandes d'adhésion · R4 / R5" to listOf("Demandes d'adhésion · R4 / R5", "Membership requests · R4 / R5", "Beitrittsanfragen · R4 / R5", "Üyelik istekleri · R4 / R5", "Заявки на вступление · R4 / R5", "加入申请 · R4 / R5"),
        "Accepter" to listOf("Accepter", "Accept", "Annehmen", "Kabul et", "Принять", "接受"),
        "Refuser" to listOf("Refuser", "Decline", "Ablehnen", "Reddet", "Отклонить", "拒绝"),
        "Ajouter un membre" to listOf("Ajouter un membre", "Add member", "Mitglied hinzufügen", "Üye ekle", "Добавить участника", "添加成员"),
        "Pseudo du joueur" to listOf("Pseudo du joueur", "Player name", "Spielername", "Oyuncu adı", "Имя игрока", "玩家昵称"),
        "Rang dans l'alliance" to listOf("Rang dans l'alliance", "Alliance rank", "Allianzrang", "İttifak rütbesi", "Ранг в альянсе", "联盟职位"),
        "Code d'invitation" to listOf("Code d'invitation", "Invitation code", "Einladungscode", "Davet kodu", "Код приглашения", "邀请码"),
        "Confirmer le transfert du R5" to listOf("Confirmer le transfert du R5", "Confirm R5 transfer", "R5-Übertragung bestätigen", "R5 devrini onayla", "Подтвердить передачу R5", "确认转让 R5"),
        "Proposer une amélioration" to listOf("Proposer une amélioration", "Suggest an improvement", "Verbesserung vorschlagen", "İyileştirme öner", "Предложить улучшение", "提出改进建议"),
        "Titre de la proposition" to listOf("Titre de la proposition", "Suggestion title", "Titel des Vorschlags", "Öneri başlığı", "Название предложения", "建议标题"),
        "Décris ton idée ou le problème" to listOf("Décris ton idée ou le problème", "Describe your idea or the problem", "Beschreibe deine Idee oder das Problem", "Fikrini veya sorunu açıkla", "Опишите идею или проблему", "描述你的想法或问题"),
        "Envoyer ma proposition" to listOf("Envoyer ma proposition", "Send my suggestion", "Vorschlag senden", "Önerimi gönder", "Отправить предложение", "发送建议"),
        "Réponse du développeur" to listOf("Réponse du développeur", "Developer response", "Antwort des Entwicklers", "Geliştirici yanıtı", "Ответ разработчика", "开发者回复"),
        "Traiter cette proposition" to listOf("Traiter cette proposition", "Review this suggestion", "Diesen Vorschlag bearbeiten", "Bu öneriyi incele", "Рассмотреть предложение", "处理此建议"),
        "Garder le bouton flottant au-dessus du jeu" to listOf("Garder le bouton flottant au-dessus du jeu", "Keep the floating button above the game", "Schwebende Schaltfläche über dem Spiel behalten", "Yüzen düğmeyi oyunun üzerinde tut", "Оставлять плавающую кнопку поверх игры", "在游戏上方保留悬浮按钮"),
        "Pour obtenir des conseils fiables, renseigne niveau, étoiles, puissance et compétences War / Arrest / Équipement. Les champs absents ne sont jamais devinés." to listOf("Pour obtenir des conseils fiables, renseigne niveau, étoiles, puissance et compétences War / Arrest / Équipement. Les champs absents ne sont jamais devinés.", "For reliable advice, enter level, stars, power and War / Arrest / Equipment skills. Missing fields are never guessed.", "Für zuverlässige Empfehlungen gib Level, Sterne, Macht und War-/Arrest-/Ausrüstungsfähigkeiten an. Fehlende Felder werden niemals geraten.", "Güvenilir öneriler için seviye, yıldız, güç ve War / Arrest / Ekipman becerilerini gir. Eksik alanlar asla tahmin edilmez.", "Для точных рекомендаций укажите уровень, звёзды, мощь и навыки War / Arrest / Equipment. Отсутствующие данные не угадываются.", "如需可靠建议，请填写等级、星级、战力以及 War / Arrest / Equipment 技能。不会猜测缺失字段。"),
        "Les données du catalogue sont des références communautaires. Seules les valeurs détectées sur tes captures décrivent ton compte." to listOf("Les données du catalogue sont des références communautaires. Seules les valeurs détectées sur tes captures décrivent ton compte.", "Catalog data is community reference information. Only values detected in your screenshots describe your account.", "Katalogdaten sind Community-Referenzwerte. Nur auf deinen Screenshots erkannte Werte beschreiben dein Konto.", "Katalog verileri topluluk referanslarıdır. Hesabını yalnızca ekran görüntülerinde algılanan değerler tanımlar.", "Данные каталога — справочная информация сообщества. Ваш аккаунт описывают только значения, распознанные на снимках.", "目录数据仅供社区参考。只有从你的截图中识别出的数值才代表你的账号。"),
        "Compétences personnelles non renseignées. Scanne les pages de compétences de ce héros pour obtenir ses effets et niveaux réels." to listOf("Compétences personnelles non renseignées. Scanne les pages de compétences de ce héros pour obtenir ses effets et niveaux réels.", "Personal skills are not available. Scan this hero's skill pages to capture their actual effects and levels.", "Persönliche Fähigkeiten sind nicht angegeben. Scanne die Fähigkeitsseiten dieses Helden, um echte Effekte und Stufen zu erfassen.", "Kişisel beceriler girilmedi. Gerçek etkileri ve seviyeleri almak için bu kahramanın beceri sayfalarını tara.", "Личные навыки не указаны. Отсканируйте страницы навыков героя, чтобы получить реальные эффекты и уровни.", "尚未填写个人技能。请扫描该英雄的技能页面，以获取实际效果和等级。"),
        "Compétences : type | nom | niveau | description (war / arrest / equipment / inconnu)" to listOf("Compétences : type | nom | niveau | description (war / arrest / equipment / inconnu)", "Skills: type | name | level | description (war / arrest / equipment / unknown)", "Fähigkeiten: Typ | Name | Stufe | Beschreibung (War / Arrest / Ausrüstung / unbekannt)", "Beceriler: tür | ad | seviye | açıklama (war / arrest / equipment / bilinmiyor)", "Навыки: тип | название | уровень | описание (war / arrest / equipment / неизвестно)", "技能：类型 | 名称 | 等级 | 描述（war / arrest / equipment / 未知）"),
        "Aucune ressource capturée pour l’instant. Utilisez « Calcul Bâtiments » ou « Calcul Héros » ci-dessus pour calculer vos prochains paliers." to listOf("Aucune ressource capturée pour l’instant. Utilisez « Calcul Bâtiments » ou « Calcul Héros » ci-dessus pour calculer vos prochains paliers.", "No resources have been captured yet. Use “Building Calculator” or “Hero Calculator” above to calculate your next upgrade levels.", "Bisher wurden keine Ressourcen erfasst. Verwende oben den Gebäude- oder Heldenrechner, um deine nächsten Ausbaustufen zu berechnen.", "Henüz kaynak yakalanmadı. Sonraki yükseltme aşamalarını hesaplamak için yukarıdaki Bina Hesaplayıcı veya Kahraman Hesaplayıcı'yı kullan.", "Ресурсы пока не распознаны. Используйте калькулятор зданий или героев выше, чтобы рассчитать следующие уровни улучшения.", "尚未捕获资源。请使用上方的“建筑计算器”或“英雄计算器”计算下一阶段升级需求。"),
        "Ces données sont indépendantes des autres profils. Renseigne les bonus tels qu’ils apparaissent en jeu ; aucune statistique n’est devinée." to listOf("Ces données sont indépendantes des autres profils. Renseigne les bonus tels qu’ils apparaissent en jeu ; aucune statistique n’est devinée.", "This data is independent of other profiles. Enter bonuses exactly as shown in the game; no stats are guessed.", "Diese Daten sind unabhängig von anderen Profilen. Gib Boni genau wie im Spiel angezeigt ein; Werte werden nicht geraten.", "Bu veriler diğer profillerden bağımsızdır. Bonusları oyunda göründüğü gibi gir; hiçbir istatistik tahmin edilmez.", "Эти данные не зависят от других профилей. Указывайте бонусы так, как они показаны в игре; характеристики не угадываются.", "这些数据独立于其他档案。请按游戏中的显示填写加成；不会猜测任何属性。"),
        "Note : Mystery Tomes requis à partir du palier 12→13. Pour les compétences héros : Arrest & War Skill Books selon rareté." to listOf("Note : Mystery Tomes requis à partir du palier 12→13. Pour les compétences héros : Arrest & War Skill Books selon rareté.", "Note: Mystery Tomes are required from level 12→13. Hero skills require Arrest & War Skill Books depending on rarity.", "Hinweis: Ab Stufe 12→13 werden Mystery Tomes benötigt. Für Heldenfähigkeiten sind je nach Seltenheit Arrest- und War-Skill-Bücher erforderlich.", "Not: 12→13 aşamasından itibaren Mystery Tomes gerekir. Kahraman becerileri için nadirliğe göre Arrest ve War Skill Books gerekir.", "Примечание: начиная с уровня 12→13 требуются Mystery Tomes. Для навыков героев нужны книги Arrest и War в зависимости от редкости.", "注意：从 12→13 阶段开始需要 Mystery Tomes。英雄技能所需的 Arrest 和 War 技能书取决于稀有度。"),
        "Robots incomplets : renseigne leurs bonus et compétences avant de les comparer." to listOf("Robots incomplets : renseigne leurs bonus et compétences avant de les comparer.", "Incomplete robots: enter their bonuses and skills before comparing them.", "Unvollständige Roboter: Gib Boni und Fähigkeiten ein, bevor du sie vergleichst.", "Eksik robotlar: karşılaştırmadan önce bonuslarını ve becerilerini gir.", "Неполные данные роботов: укажите бонусы и навыки перед сравнением.", "机器人信息不完整：请先填写加成和技能，再进行比较。"),
        "Ratios de référence : vérifie toujours les consignes du chef et les règles affichées dans l’événement." to listOf("Ratios de référence : vérifie toujours les consignes du chef et les règles affichées dans l’événement.", "Reference ratios: always check the leader's instructions and the rules shown in the event.", "Referenzverhältnisse: Beachte immer die Anweisungen des Anführers und die im Event angezeigten Regeln.", "Referans oranlar: liderin talimatlarını ve etkinlikte gösterilen kuralları daima kontrol et.", "Рекомендуемые соотношения: всегда проверяйте указания лидера и правила события.", "参考比例：请始终确认盟主指示以及活动中显示的规则。"),
        "Sélection fondée sur les compétences reconnues dans ce profil. Ce n’est pas un classement de puissance ni une simulation de combat." to listOf("Sélection fondée sur les compétences reconnues dans ce profil. Ce n’est pas un classement de puissance ni une simulation de combat.", "Selection is based on skills recognized in this profile. It is not a power ranking or combat simulation.", "Die Auswahl basiert auf den in diesem Profil erkannten Fähigkeiten. Sie ist weder eine Macht-Rangliste noch eine Kampfsimulation.", "Seçim, bu profilde tanınan becerilere dayanır. Güç sıralaması veya savaş simülasyonu değildir.", "Выбор основан на навыках, распознанных в этом профиле. Это не рейтинг мощи и не симуляция боя.", "选择依据是此档案中识别的技能，并非战力排名或战斗模拟。"),
        "Sélection basée sur les bonus et compétences que tu as saisis. Ce n’est pas une simulation ni un classement garanti." to listOf("Sélection basée sur les bonus et compétences que tu as saisis. Ce n’est pas une simulation ni un classement garanti.", "Selection is based on the bonuses and skills you entered. It is not a simulation or a guaranteed ranking.", "Die Auswahl basiert auf deinen eingegebenen Boni und Fähigkeiten. Sie ist keine Simulation und keine garantierte Rangliste.", "Seçim, girdiğin bonus ve becerilere dayanır. Simülasyon veya garantili sıralama değildir.", "Выбор основан на введённых вами бонусах и навыках. Это не симуляция и не гарантированный рейтинг.", "选择依据是你填写的加成和技能，并非模拟结果或保证排名。"),
        "Toutes les analyses enregistrées dans l’historique seront supprimées. Cette action est irréversible." to listOf("Toutes les analyses enregistrées dans l’historique seront supprimées. Cette action est irréversible.", "All analyses saved in history will be deleted. This action cannot be undone.", "Alle im Verlauf gespeicherten Analysen werden gelöscht. Dieser Vorgang kann nicht rückgängig gemacht werden.", "Geçmişte kayıtlı tüm analizler silinecek. Bu işlem geri alınamaz.", "Все анализы, сохранённые в истории, будут удалены. Это действие нельзя отменить.", "历史记录中的所有分析都将被删除。此操作无法撤销。"),
        "Historique vide. Appuyez sur « Analyser l’écran » pour enregistrer votre première capture." to listOf("Historique vide. Appuyez sur « Analyser l’écran » pour enregistrer votre première capture.", "History is empty. Tap “Analyze screen” to save your first capture.", "Der Verlauf ist leer. Tippe auf „Bildschirm analysieren“, um deinen ersten Screenshot zu speichern.", "Geçmiş boş. İlk ekran görüntünü kaydetmek için “Ekranı analiz et” seçeneğine dokun.", "История пуста. Нажмите «Анализировать экран», чтобы сохранить первый снимок.", "历史记录为空。点击“分析屏幕”以保存第一张截图。"),
        "Connecte ton compte pour synchroniser ton profil de joueur et tes héros dans l'espace cloud sécurisé." to listOf("Connecte ton compte pour synchroniser ton profil de joueur et tes héros dans l'espace cloud sécurisé.", "Sign in to sync your player profile and heroes to secure cloud storage.", "Melde dich an, um dein Spielerprofil und deine Helden sicher mit der Cloud zu synchronisieren.", "Oyuncu profilini ve kahramanlarını güvenli bulut alanıyla eşitlemek için hesabına giriş yap.", "Войдите, чтобы синхронизировать профиль игрока и героев с защищённым облачным хранилищем.", "登录账号，将玩家档案和英雄同步到安全云端。"),
        "Les jetons sont chiffrés avec Android Keystore. La synchronisation du profil se lance depuis le formulaire joueur ci-dessous." to listOf("Les jetons sont chiffrés avec Android Keystore. La synchronisation du profil se lance depuis le formulaire joueur ci-dessous.", "Tokens are encrypted with Android Keystore. Profile sync starts from the player form below.", "Tokens werden mit Android Keystore verschlüsselt. Die Profilsynchronisierung wird über das Spielerformular unten gestartet.", "Jetonlar Android Keystore ile şifrelenir. Profil eşitlemesi aşağıdaki oyuncu formundan başlatılır.", "Токены шифруются с помощью Android Keystore. Синхронизация профиля запускается из формы игрока ниже.", "令牌使用 Android Keystore 加密。请通过下方玩家表单启动档案同步。"),
        "Créer un compte" to listOf("Créer un compte", "Create an account", "Konto erstellen", "Hesap oluştur", "Создать аккаунт", "创建账号"),
        "J'ai déjà un compte" to listOf("J'ai déjà un compte", "I already have an account", "Ich habe bereits ein Konto", "Zaten hesabım var", "У меня уже есть аккаунт", "我已有账号"),
        "Ces informations sont enregistrées pendant la création du compte. Tu n'auras pas à refaire une inscription dans l'espace Alliance." to listOf("Ces informations sont enregistrées pendant la création du compte. Tu n'auras pas à refaire une inscription dans l'espace Alliance.", "This information is saved when your account is created. You won't need to register again in the Alliance area.", "Diese Angaben werden bei der Kontoerstellung gespeichert. Du musst dich im Allianzbereich nicht erneut registrieren.", "Bu bilgiler hesap oluşturulurken kaydedilir. İttifak alanına yeniden kaydolman gerekmez.", "Эти данные сохраняются при создании аккаунта. Повторная регистрация в разделе альянса не потребуется.", "这些信息会在创建账号时保存。你无需在联盟空间再次注册。"),
        "Pseudo dans Land of Jail" to listOf("Pseudo dans Land of Jail", "Land of Jail username", "Land-of-Jail-Spielername", "Land of Jail oyuncu adı", "Имя игрока в Land of Jail", "Land of Jail 游戏昵称"),
        "Tag de l'alliance" to listOf("Tag de l'alliance", "Alliance tag", "Allianz-Tag", "İttifak etiketi", "Тег альянса", "联盟标签"),
        "Choisis R5 uniquement si tu crées un nouvel espace d'alliance. Pour rejoindre une alliance existante, indique ton rang actuel : ta demande sera transmise aux responsables." to listOf("Choisis R5 uniquement si tu crées un nouvel espace d'alliance. Pour rejoindre une alliance existante, indique ton rang actuel : ta demande sera transmise aux responsables.", "Choose R5 only if you are creating a new alliance space. To join an existing alliance, enter your current rank; your request will be sent to its officers.", "Wähle R5 nur, wenn du einen neuen Allianzbereich erstellst. Um einer bestehenden Allianz beizutreten, gib deinen aktuellen Rang an; deine Anfrage wird an die Offiziere gesendet.", "Yalnızca yeni bir ittifak alanı oluşturuyorsan R5'i seç. Mevcut bir ittifaka katılmak için rütbeni belirt; isteğin yetkililere iletilir.", "Выбирайте R5, только если создаёте новое пространство альянса. Для вступления в существующий альянс укажите текущий ранг — запрос будет отправлен офицерам.", "仅在创建新联盟空间时选择 R5。若要加入现有联盟，请填写当前职位，申请将发送给管理人员。"),
        "Actualisation…" to listOf("Actualisation…", "Refreshing…", "Aktualisierung…", "Yenileniyor…", "Обновление…", "正在刷新…"),
        "Le R5 se remplace en nommant un R4." to listOf("Le R5 se remplace en nommant un R4.", "The R5 role is transferred by appointing an R4.", "Die R5-Rolle wird übertragen, indem ein R4 ernannt wird.", "R5 görevi bir R4 atanarak devredilir.", "Передача роли R5 выполняется назначением R4.", "任命一名 R4 即可转交 R5 职位。"),
        "Retirer du roster" to listOf("Retirer du roster", "Remove from roster", "Aus Aufstellung entfernen", "Kad rodan çıkar", "Убрать из состава", "从名单中移除"),
        "Lancer la simulation (R4 / R5)" to listOf("Lancer la simulation (R4 / R5)", "Run simulation (R4 / R5)", "Simulation starten (R4 / R5)", "Simülasyonu başlat (R4 / R5)", "Запустить симуляцию (R4 / R5)", "运行模拟（R4 / R5）"),
        "Alliance partagée en ligne" to listOf("Alliance partagée en ligne", "Shared alliance online", "Allianz online geteilt", "Çevrimiçi paylaşılan ittifak", "Общий альянс онлайн", "在线共享联盟"),
        "Crée l'espace cloud de ton alliance ou rejoins celui d'un autre joueur avec un code d'invitation. Le rang R5 du créateur est attribué côté serveur." to listOf("Crée l'espace cloud de ton alliance ou rejoins celui d'un autre joueur avec un code d'invitation. Le rang R5 du créateur est attribué côté serveur.", "Create your alliance's cloud space or join another player's using an invitation code. The creator's R5 rank is assigned by the server.", "Erstelle den Cloud-Bereich deiner Allianz oder tritt einem anderen Spieler per Einladungscode bei. Der R5-Rang des Erstellers wird serverseitig vergeben.", "İttifakının bulut alanını oluştur veya davet koduyla başka bir oyuncunun alanına katıl. Oluşturanın R5 rütbesi sunucu tarafından atanır.", "Создайте облачное пространство альянса или вступите в пространство другого игрока по коду приглашения. Ранг R5 создателя назначается сервером.", "创建联盟云空间，或使用邀请码加入其他玩家的空间。创建者的 R5 职位由服务器分配。"),
        "Connecte-toi d'abord dans « Compte & sauvegarde cloud » pour utiliser ces fonctions." to listOf("Connecte-toi d'abord dans « Compte & sauvegarde cloud » pour utiliser ces fonctions.", "Sign in under “Account & cloud backup” first to use these features.", "Melde dich zuerst unter „Konto & Cloud-Sicherung“ an, um diese Funktionen zu nutzen.", "Bu özellikleri kullanmak için önce “Hesap ve bulut yedekleme” bölümünden giriş yap.", "Сначала войдите в разделе «Аккаунт и облачная копия», чтобы использовать эти функции.", "请先在“账号与云备份”中登录，再使用这些功能。"),
        "Créer cette alliance dans le cloud" to listOf("Créer cette alliance dans le cloud", "Create this alliance in the cloud", "Diese Allianz in der Cloud erstellen", "Bu ittifakı bulutta oluştur", "Создать этот альянс в облаке", "在云端创建此联盟"),
        "Actualiser la liste des R4" to listOf("Actualiser la liste des R4", "Refresh R4 list", "R4-Liste aktualisieren", "R4 listesini yenile", "Обновить список R4", "刷新 R4 列表"),
        "Aucun membre R4 disponible dans la liste cloud." to listOf("Aucun membre R4 disponible dans la liste cloud.", "No R4 members are available in the cloud list.", "In der Cloud-Liste sind keine R4-Mitglieder verfügbar.", "Bulut listesinde R4 üyesi bulunmuyor.", "В облачном списке нет участников R4.", "云端列表中没有可用的 R4 成员。"),
        "Générer un code d'invitation (105 membres max.)" to listOf("Générer un code d'invitation (105 membres max.)", "Generate invitation code (max. 105 members)", "Einladungscode erstellen (max. 105 Mitglieder)", "Davet kodu oluştur (en fazla 105 üye)", "Создать код приглашения (макс. 105 участников)", "生成邀请码（最多 105 名成员）"),
        "Code reçu pour rejoindre une alliance" to listOf("Code reçu pour rejoindre une alliance", "Invitation code to join an alliance", "Erhaltener Code zum Beitritt zu einer Allianz", "İttifaka katılmak için alınan kod", "Полученный код для вступления в альянс", "加入联盟的邀请码"),
        "Rejoindre avec ce code" to listOf("Rejoindre avec ce code", "Join with this code", "Mit diesem Code beitreten", "Bu kodla katıl", "Вступить по этому коду", "使用此代码加入"),
        "Confirmer le transfert" to listOf("Confirmer le transfert", "Confirm transfer", "Übertragung bestätigen", "Devri onayla", "Подтвердить передачу", "确认转让"),
        "Partage une idée, signale un bug ou propose une amélioration. Tu pourras suivre la réponse du développeur ici." to listOf("Partage une idée, signale un bug ou propose une amélioration. Tu pourras suivre la réponse du développeur ici.", "Share an idea, report a bug or suggest an improvement. You can follow the developer's response here.", "Teile eine Idee, melde einen Fehler oder schlage eine Verbesserung vor. Hier kannst du die Antwort des Entwicklers verfolgen.", "Fikrini paylaş, hata bildir veya iyileştirme öner. Geliştiricinin yanıtını buradan takip edebilirsin.", "Поделитесь идеей, сообщите об ошибке или предложите улучшение. Здесь можно отслеживать ответ разработчика.", "分享想法、报告错误或提出改进建议。你可以在这里查看开发者的回复。"),
        "Connecte-toi à ton compte pour envoyer une proposition." to listOf("Connecte-toi à ton compte pour envoyer une proposition.", "Sign in to your account to send a suggestion.", "Melde dich an, um einen Vorschlag zu senden.", "Öneri göndermek için hesabına giriş yap.", "Войдите в аккаунт, чтобы отправить предложение.", "登录账号以发送建议。"),
        "Toutes les propositions · Développeur" to listOf("Toutes les propositions · Développeur", "All suggestions · Developer", "Alle Vorschläge · Entwickler", "Tüm öneriler · Geliştirici", "Все предложения · Разработчик", "所有建议 · 开发者"),
        "Mes propositions" to listOf("Mes propositions", "My suggestions", "Meine Vorschläge", "Önerilerim", "Мои предложения", "我的建议"),
        "Aucune proposition pour le moment." to listOf("Aucune proposition pour le moment.", "No suggestions yet.", "Noch keine Vorschläge vorhanden.", "Henüz öneri yok.", "Пока нет предложений.", "目前还没有建议。"),
        "Répondre à la proposition" to listOf("Répondre à la proposition", "Reply to suggestion", "Auf Vorschlag antworten", "Öneriyi yanıtla", "Ответить на предложение", "回复建议"),
        "Ta réponse au joueur" to listOf("Ta réponse au joueur", "Your reply to the player", "Deine Antwort an den Spieler", "Oyuncuya yanıtın", "Ваш ответ игроку", "给玩家的回复"),
        "Envoyer la réponse" to listOf("Envoyer la réponse", "Send reply", "Antwort senden", "Yanıtı gönder", "Отправить ответ", "发送回复"),
        "Enregistrement…" to listOf("Enregistrement…", "Saving…", "Speichern…", "Kaydediliyor…", "Сохранение…", "正在保存…"),
        "Package (com.justgame.jails)" to listOf("Package (com.justgame.jails)", "Package (com.justgame.jails)", "Paket (com.justgame.jails)", "Paket (com.justgame.jails)", "Пакет (com.justgame.jails)", "应用包名 (com.justgame.jails)"),
        "S'inscrire" to listOf("S'inscrire", "Sign up", "Registrieren", "Kaydol", "Зарегистрироваться", "注册"),
        "1. Statistiques d'utilisation : À autoriser" to listOf("1. Statistiques d'utilisation : À autoriser", "1. Usage access: Not allowed yet", "1. Nutzungszugriff: Noch nicht erlaubt", "1. Kullanım erişimi: Henüz izin verilmedi", "1. Доступ к статистике: ещё не разрешён", "1. 使用情况访问权限：尚未允许"),
        "1. Statistiques d'utilisation : Autorisées" to listOf("1. Statistiques d'utilisation : Autorisées", "1. Usage access: Allowed", "1. Nutzungszugriff: Erlaubt", "1. Kullanım erişimi: İzin verildi", "1. Доступ к статистике: разрешён", "1. 使用情况访问权限：已允许"),
        "2. Superposition au-dessus du jeu : À autoriser" to listOf("2. Superposition au-dessus du jeu : À autoriser", "2. Display over the game: Not allowed yet", "2. Anzeige über dem Spiel: Noch nicht erlaubt", "2. Oyunun üzerinde gösterim: Henüz izin verilmedi", "2. Отображение поверх игры: ещё не разрешено", "2. 游戏上层显示：尚未允许"),
        "2. Superposition au-dessus du jeu : Autorisée" to listOf("2. Superposition au-dessus du jeu : Autorisée", "2. Display over the game: Allowed", "2. Anzeige über dem Spiel: Erlaubt", "2. Oyunun üzerinde gösterim: İzin verildi", "2. Отображение поверх игры: разрешено", "2. 游戏上层显示：已允许"),
        "Si tu es le R5 qui crée l'espace cloud de l'alliance, tu deviens directement R5 sans validation. Pour une alliance déjà créée, rejoins-la d'abord comme membre puis demande au R5 actuel de te transférer le poste." to listOf("Si tu es le R5 qui crée l'espace cloud de l'alliance, tu deviens directement R5 sans validation. Pour une alliance déjà créée, rejoins-la d'abord comme membre puis demande au R5 actuel de te transférer le poste.", "If you are the R5 creating the alliance cloud space, you become R5 immediately without approval. For an existing alliance, join as a member first, then ask the current R5 to transfer the role to you.", "Wenn du als R5 den Cloud-Bereich der Allianz erstellst, wirst du ohne Bestätigung direkt R5. Bei einer bestehenden Allianz tritt zuerst als Mitglied bei und bitte dann den aktuellen R5 um die Übertragung.", "İttifakın bulut alanını oluşturan R5 sensen onay gerekmeden doğrudan R5 olursun. Mevcut bir ittifakta önce üye olarak katıl, ardından mevcut R5'ten görevi devretmesini iste.", "Если вы создаёте облачное пространство альянса как R5, вы сразу становитесь R5 без подтверждения. Для существующего альянса сначала вступите как участник, затем попросите текущего R5 передать вам должность.", "如果你是创建联盟云空间的 R5，则无需审核即可直接成为 R5。若联盟已存在，请先以成员身份加入，再请当前 R5 转让职位。"),
        "Les demandes des joueurs de ton alliance apparaissent ici. Seuls les droits confirmés par un officier sont appliqués." to listOf("Les demandes des joueurs de ton alliance apparaissent ici. Seuls les droits confirmés par un officier sont appliqués.", "Requests from players in your alliance appear here. Only permissions confirmed by an officer are applied.", "Anfragen von Spielern deiner Allianz erscheinen hier. Nur von einem Offizier bestätigte Rechte werden übernommen.", "İttifakındaki oyuncuların istekleri burada görünür. Yalnızca bir yetkilinin onayladığı izinler uygulanır.", "Здесь отображаются запросы игроков вашего альянса. Применяются только права, подтверждённые офицером.", "你联盟玩家的申请会显示在这里。只有经官员确认的权限才会生效。"),

        "Lands of Jail détecté au premier plan." to listOf("Lands of Jail détecté au premier plan.", "Land of Jail detected in the foreground.", "Land of Jail im Vordergrund erkannt.", "Land of Jail ön planda algılandı.", "Land of Jail обнаружена на переднем плане.", "已检测到 Land of Jail 正在前台运行。"),
        "Total 1★→5★ : 1 075 éclats (+10 recrutement). Seuil 4★ recommandé : 475 éclats." to listOf("Total 1★→5★ : 1 075 éclats (+10 recrutement). Seuil 4★ recommandé : 475 éclats.", "Total 1★→5★: 1,075 shards (+10 recruitment). Recommended 4★ threshold: 475 shards.", "Gesamt 1★→5★: 1.075 Splitter (+10 Rekrutierung). Empfohlener Schwellenwert für 4★: 475 Splitter.", "Toplam 1★→5★: 1.075 parça (+10 işe alım). Önerilen 4★ eşiği: 475 parça.", "Всего 1★→5★: 1 075 осколков (+10 за набор). Рекомендуемый порог 4★: 475 осколков.", "1★→5★总计：1,075 碎片（招募另加10）。建议达到4★的门槛：475碎片。"),
        "Contrôle des données avant recommandation" to listOf("Contrôle des données avant recommandation", "Data check before recommendation", "Datenprüfung vor Empfehlungen", "Öneriden önce veri kontrolü", "Проверка данных перед рекомендацией", "推荐前的数据检查"),
        "Robots incomplets : renseigne leurs bonus et compétences avant de les comparer." to listOf("Robots incomplets : renseigne leurs bonus et compétences avant de les comparer.", "Incomplete robots: enter their bonuses and skills before comparing them.", "Unvollständige Roboter: Trage Boni und Fähigkeiten ein, bevor du sie vergleichst.", "Eksik robotlar: Karşılaştırmadan önce bonuslarını ve becerilerini gir.", "Неполные данные роботов: укажи бонусы и навыки перед сравнением.", "机器人信息不完整：比较前请填写其加成和技能。"),
        "Ratios de référence : vérifie toujours les consignes du chef et les règles affichées dans l’événement." to listOf("Ratios de référence : vérifie toujours les consignes du chef et les règles affichées dans l’événement.", "Reference ratios: always check the leader's instructions and the rules shown in the event.", "Referenzverhältnisse: Beachte immer die Anweisungen des Anführers und die angezeigten Ereignisregeln.", "Referans oranlar: Liderin talimatlarını ve etkinlikte gösterilen kuralları daima kontrol et.", "Эталонные соотношения: всегда проверяй указания лидера и правила события.", "参考比例：务必查看队长指示和活动中显示的规则。"),
        "Héros du profil — compétences correspondantes" to listOf("Héros du profil — compétences correspondantes", "Profile heroes — matching skills", "Profilhelden — passende Fähigkeiten", "Profil kahramanları — eşleşen beceriler", "Герои профиля — подходящие навыки", "资料中的英雄 — 对应技能"),
        "Robots du profil — correspondances à vérifier" to listOf("Robots du profil — correspondances à vérifier", "Profile robots — matches to verify", "Profilroboter — Übereinstimmungen prüfen", "Profil robotları — doğrulanacak eşleşmeler", "Роботы профиля — совпадения для проверки", "资料中的机器人 — 待核对匹配项"),
        "Mot de passe (8 caractères minimum)" to listOf("Mot de passe (8 caractères minimum)", "Password (at least 8 characters)", "Passwort (mindestens 8 Zeichen)", "Şifre (en az 8 karakter)", "Пароль (не менее 8 символов)", "密码（至少8个字符）"),
        "Mise à jour…" to listOf("Mise à jour…", "Updating…", "Aktualisierung…", "Güncelleniyor…", "Обновление…", "正在更新…"),
        "Les profils synchronisés sont chargés depuis le cloud selon les droits de visibilité du rang dans l'alliance. Les ajouts manuels effectués ici restent enregistrés localement sur cet appareil." to listOf("Les profils synchronisés sont chargés depuis le cloud selon les droits de visibilité du rang dans l'alliance. Les ajouts manuels effectués ici restent enregistrés localement sur cet appareil.", "Synced profiles are loaded from the cloud according to alliance-rank visibility permissions. Manual additions here remain stored locally on this device.", "Synchronisierte Profile werden gemäß den Sichtbarkeitsrechten des Allianzrangs aus der Cloud geladen. Manuelle Ergänzungen bleiben lokal auf diesem Gerät gespeichert.", "Senkronize profiller, ittifak rütbesinin görünürlük izinlerine göre buluttan yüklenir. Buraya elle eklenenler bu cihazda yerel olarak saklanır.", "Синхронизированные профили загружаются из облака в соответствии с правами видимости ранга в альянсе. Добавления вручную сохраняются локально на этом устройстве.", "同步资料会根据联盟职位的可见权限从云端加载。此处手动添加的内容仍保存在本设备本地。"),
        "Héros et compétences War (copier les infos)" to listOf("Héros et compétences War (copier les infos)", "War heroes and skills (copy details)", "Kriegshelden und Fähigkeiten (Infos kopieren)", "Savaş kahramanları ve becerileri (bilgileri kopyala)", "Боевые герои и навыки (скопировать данные)", "战争英雄与技能（复制信息）"),
        "Actualiser le lien cloud après connexion" to listOf("Actualiser le lien cloud après connexion", "Refresh cloud link after signing in", "Cloud-Verknüpfung nach der Anmeldung aktualisieren", "Giriş yaptıktan sonra bulut bağlantısını yenile", "Обновить связь с облаком после входа", "登录后刷新云端关联"),
    )
    var translated = dictionary[source]?.getOrNull(index)
        ?: (if (language == AppLanguage.IT) dictionary[source]?.getOrNull(1) else null)
        ?: source
    val fragments = when (language) {
        AppLanguage.FR -> emptyMap()
        AppLanguage.EN -> mapOf(
            "Héros suivis :" to "Heroes tracked:", "Ressources :" to "Resources:", "Puissance :" to "Power:",
            "Compétences :" to "Skills:", "Robot :" to "Robot:", "Rang demandé :" to "Requested rank:",
            "À compléter" to "to complete", "profil(s) suffisamment renseigné(s)" to "profile(s) sufficiently completed",
            "à compléter" to "to complete", "Puissance (facultatif)" to "Power (optional)", "Rareté / étoiles" to "Rarity / stars",
            "Bonus :" to "Bonuses:", "Éclats de héros" to "Hero shards", "Départ :" to "From:",
            "Cible :" to "Target:", "Requis :" to "Required:", "Manquants :" to "Missing:",
            "Tomes de troupes" to "Troop tomes", "De :" to "From:", "À :" to "To:",
            "Seal of Wisdom :" to "Seal of Wisdom:", "Seal of Knowledge :" to "Seal of Knowledge:",
            "Synthèse (" to "Summary (", "Composition :" to "Composition:", "Ratios de troupes" to "Troop ratios",
            "À vérifier :" to "To verify:", "Session enregistrée :" to "Session saved:",
            "Rareté lue sur capture :" to "Rarity read from screenshot:", "Spécialité observée :" to "Observed specialty:",
            "Source :" to "Source:", "War / équipement observé :" to "Observed war / equipment:",
            "Alliance cloud liée :" to "Linked cloud alliance:", "Ton rang confirmé dans le cloud :" to "Your confirmed cloud rank:",
            "Compte joueur :" to "Player account:", "Pré-analyse IA :" to "AI pre-analysis:",
            "Héros / compétences :" to "Heroes / skills:", "non renseignée" to "not provided",
            "Catégorie" to "Category", " joueur(s)" to " player(s)", "Description non disponible" to "Description unavailable",
            "Confirmer que " to "Confirm that ", " devient R5 ? Ton propre rang passera automatiquement à R4." to " becomes R5? Your own rank will automatically change to R4.",
            "Code créé. Transmets-le uniquement aux membres de ton alliance." to "Code created. Share it only with your alliance members.",
            "Création du code impossible." to "Could not create the code.", "Invitation acceptée. Ton compte a rejoint l'alliance." to "Invitation accepted. Your account has joined the alliance.",
            "Invitation refusée." to "Invitation declined.", "Vérification…" to "Checking…", "Rejoindre avec ce code" to "Join with this code",
            "Partage une idée, signale un bug ou propose une amélioration. Tu pourras suivre la réponse du développeur ici." to "Share an idea, report a bug or suggest an improvement. You can follow the developer's response here.",
            "Toutes les propositions · Développeur" to "All suggestions · Developer", "Mes propositions" to "My suggestions",
            "Aucune proposition pour le moment." to "No suggestions yet.", "Chargement des propositions impossible." to "Could not load suggestions.",
            "Proposition envoyée. Tu recevras la réponse dans cette rubrique." to "Suggestion sent. You will receive the response in this section.",
            "Envoi impossible." to "Could not send.", "Répondre à la proposition" to "Reply to suggestion",
            "Ta réponse au joueur" to "Your reply to the player", "Réponse enregistrée et visible par le joueur." to "Reply saved and visible to the player.",
            "Enregistrement impossible." to "Could not save.", "Enregistrement…" to "Saving…", "Envoyer la réponse" to "Send reply"
        )
        AppLanguage.DE -> mapOf(
            "Héros suivis :" to "Verfolgte Helden:", "Ressources :" to "Ressourcen:", "Puissance :" to "Macht:",
            "Compétences :" to "Fähigkeiten:", "Robot :" to "Roboter:", "Rang demandé :" to "Angeforderter Rang:",
            "À compléter" to "noch zu vervollständigen", "profil(s) suffisamment renseigné(s)" to "ausreichend ausgefüllte(s) Profil(e)",
            "à compléter" to "zu vervollständigen", "Puissance (facultatif)" to "Macht (optional)", "Rareté / étoiles" to "Seltenheit / Sterne",
            "Bonus :" to "Boni:", "Éclats de héros" to "Heldensplitter", "Départ :" to "Von:",
            "Cible :" to "Ziel:", "Requis :" to "Benötigt:", "Manquants :" to "Fehlend:",
            "Tomes de troupes" to "Truppenbände", "De :" to "Von:", "À :" to "Bis:",
            "Seal of Wisdom :" to "Seal of Wisdom:", "Seal of Knowledge :" to "Seal of Knowledge:",
            "Synthèse (" to "Zusammenfassung (", "Composition :" to "Zusammensetzung:", "Ratios de troupes" to "Truppenverhältnisse",
            "À vérifier :" to "Zu prüfen:", "Session enregistrée :" to "Sitzung gespeichert:",
            "Rareté lue sur capture :" to "Seltenheit aus Screenshot:", "Spécialité observée :" to "Beobachteter Schwerpunkt:",
            "Source :" to "Quelle:", "War / équipement observé :" to "Beobachteter Krieg / Ausrüstung:",
            "Alliance cloud liée :" to "Verknüpfte Cloud-Allianz:", "Ton rang confirmé dans le cloud :" to "Dein bestätigter Cloud-Rang:",
            "Compte joueur :" to "Spielerkonto:", "Pré-analyse IA :" to "KI-Voranalyse:",
            "Héros / compétences :" to "Helden / Fähigkeiten:", "non renseignée" to "nicht angegeben",
            "Catégorie" to "Kategorie", " joueur(s)" to " Spieler", "Description non disponible" to "Beschreibung nicht verfügbar",
            "Confirmer que " to "Bestätigen, dass ", " devient R5 ? Ton propre rang passera automatiquement à R4." to " R5 wird? Dein eigener Rang wechselt automatisch zu R4.",
            "Code créé. Transmets-le uniquement aux membres de ton alliance." to "Code erstellt. Teile ihn nur mit Mitgliedern deiner Allianz.",
            "Création du code impossible." to "Code konnte nicht erstellt werden.", "Invitation acceptée. Ton compte a rejoint l'alliance." to "Einladung angenommen. Dein Konto ist der Allianz beigetreten.",
            "Invitation refusée." to "Einladung abgelehnt.", "Vérification…" to "Wird überprüft…", "Rejoindre avec ce code" to "Mit diesem Code beitreten",
            "Partage une idée, signale un bug ou propose une amélioration. Tu pourras suivre la réponse du développeur ici." to "Teile eine Idee, melde einen Fehler oder schlage eine Verbesserung vor. Die Antwort des Entwicklers kannst du hier verfolgen.",
            "Toutes les propositions · Développeur" to "Alle Vorschläge · Entwickler", "Mes propositions" to "Meine Vorschläge",
            "Aucune proposition pour le moment." to "Noch keine Vorschläge.", "Chargement des propositions impossible." to "Vorschläge konnten nicht geladen werden.",
            "Proposition envoyée. Tu recevras la réponse dans cette rubrique." to "Vorschlag gesendet. Du erhältst die Antwort in diesem Bereich.",
            "Envoi impossible." to "Senden fehlgeschlagen.", "Répondre à la proposition" to "Auf Vorschlag antworten",
            "Ta réponse au joueur" to "Deine Antwort an den Spieler", "Réponse enregistrée et visible par le joueur." to "Antwort gespeichert und für den Spieler sichtbar.",
            "Enregistrement impossible." to "Speichern fehlgeschlagen.", "Enregistrement…" to "Wird gespeichert…", "Envoyer la réponse" to "Antwort senden"
        )
        AppLanguage.TR -> mapOf(
            "Héros suivis :" to "Takip edilen kahramanlar:", "Ressources :" to "Kaynaklar:", "Puissance :" to "Güç:",
            "Compétences :" to "Beceriler:", "Robot :" to "Robot:", "Rang demandé :" to "İstenen rütbe:",
            "À compléter" to "tamamlanacak", "profil(s) suffisamment renseigné(s)" to "yeterince tamamlanmış profil",
            "à compléter" to "tamamlanacak", "Puissance (facultatif)" to "Güç (isteğe bağlı)", "Rareté / étoiles" to "Nadirlik / yıldız",
            "Bonus :" to "Bonuslar:", "Éclats de héros" to "Kahraman parçaları", "Départ :" to "Başlangıç:",
            "Cible :" to "Hedef:", "Requis :" to "Gerekli:", "Manquants :" to "Eksik:",
            "Tomes de troupes" to "Birlik ciltleri", "De :" to "Şuradan:", "À :" to "Şuraya:",
            "Seal of Wisdom :" to "Bilgelik Mührü:", "Seal of Knowledge :" to "Bilgi Mührü:",
            "Synthèse (" to "Özet (", "Composition :" to "Dizilim:", "Ratios de troupes" to "Birlik oranları",
            "À vérifier :" to "Kontrol edilecek:", "Session enregistrée :" to "Oturum kaydedildi:",
            "Rareté lue sur capture :" to "Ekran görüntüsünden okunan nadirlik:", "Spécialité observée :" to "Gözlemlenen uzmanlık:",
            "Source :" to "Kaynak:", "War / équipement observé :" to "Gözlemlenen savaş / ekipman:",
            "Alliance cloud liée :" to "Bağlı bulut ittifakı:", "Ton rang confirmé dans le cloud :" to "Bulutta doğrulanan rütben:",
            "Compte joueur :" to "Oyuncu hesabı:", "Pré-analyse IA :" to "Yapay zekâ ön analizi:",
            "Héros / compétences :" to "Kahramanlar / beceriler:", "non renseignée" to "belirtilmemiş",
            "Catégorie" to "Kategori", " joueur(s)" to " oyuncu", "Description non disponible" to "Açıklama mevcut değil",
            "Confirmer que " to "Şunu onayla: ", " devient R5 ? Ton propre rang passera automatiquement à R4." to " R5 olacak mı? Kendi rütben otomatik olarak R4'e düşer.",
            "Code créé. Transmets-le uniquement aux membres de ton alliance." to "Kod oluşturuldu. Yalnızca ittifak üyelerine ilet.",
            "Création du code impossible." to "Kod oluşturulamadı.", "Invitation acceptée. Ton compte a rejoint l'alliance." to "Davet kabul edildi. Hesabın ittifaka katıldı.",
            "Invitation refusée." to "Davet reddedildi.", "Vérification…" to "Kontrol ediliyor…", "Rejoindre avec ce code" to "Bu kodla katıl",
            "Partage une idée, signale un bug ou propose une amélioration. Tu pourras suivre la réponse du développeur ici." to "Fikrini paylaş, hata bildir veya iyileştirme öner. Geliştiricinin yanıtını buradan takip edebilirsin.",
            "Toutes les propositions · Développeur" to "Tüm öneriler · Geliştirici", "Mes propositions" to "Önerilerim",
            "Aucune proposition pour le moment." to "Henüz öneri yok.", "Chargement des propositions impossible." to "Öneriler yüklenemedi.",
            "Proposition envoyée. Tu recevras la réponse dans cette rubrique." to "Öneri gönderildi. Yanıtı bu bölümde alacaksın.",
            "Envoi impossible." to "Gönderilemedi.", "Répondre à la proposition" to "Öneriyi yanıtla",
            "Ta réponse au joueur" to "Oyuncuya yanıtın", "Réponse enregistrée et visible par le joueur." to "Yanıt kaydedildi ve oyuncu tarafından görülebilir.",
            "Enregistrement impossible." to "Kaydedilemedi.", "Enregistrement…" to "Kaydediliyor…", "Envoyer la réponse" to "Yanıtı gönder"
        )
        AppLanguage.RU -> mapOf(
            "Héros suivis :" to "Отслеживаемые герои:", "Ressources :" to "Ресурсы:", "Puissance :" to "Мощь:",
            "Compétences :" to "Навыки:", "Robot :" to "Робот:", "Rang demandé :" to "Запрошенный ранг:",
            "À compléter" to "нужно заполнить", "profil(s) suffisamment renseigné(s)" to "достаточно заполненных профилей",
            "à compléter" to "нужно заполнить", "Puissance (facultatif)" to "Мощь (необязательно)", "Rareté / étoiles" to "Редкость / звёзды",
            "Bonus :" to "Бонусы:", "Éclats de héros" to "Осколки героев", "Départ :" to "От:",
            "Cible :" to "Цель:", "Requis :" to "Требуется:", "Manquants :" to "Не хватает:",
            "Tomes de troupes" to "Тома войск", "De :" to "От:", "À :" to "До:",
            "Seal of Wisdom :" to "Печать мудрости:", "Seal of Knowledge :" to "Печать знаний:",
            "Synthèse (" to "Сводка (", "Composition :" to "Состав:", "Ratios de troupes" to "Соотношение войск",
            "À vérifier :" to "Проверить:", "Session enregistrée :" to "Сеанс сохранён:",
            "Rareté lue sur capture :" to "Редкость со снимка:", "Spécialité observée :" to "Замеченная специализация:",
            "Source :" to "Источник:", "War / équipement observé :" to "Замеченная война / экипировка:",
            "Alliance cloud liée :" to "Связанный облачный альянс:", "Ton rang confirmé dans le cloud :" to "Ваш ранг, подтверждённый в облаке:",
            "Compte joueur :" to "Аккаунт игрока:", "Pré-analyse IA :" to "Предварительный анализ ИИ:",
            "Héros / compétences :" to "Герои / навыки:", "non renseignée" to "не указано",
            "Catégorie" to "Категория", " joueur(s)" to " игрок(ов)", "Description non disponible" to "Описание недоступно",
            "Confirmer que " to "Подтвердить, что ", " devient R5 ? Ton propre rang passera automatiquement à R4." to " станет R5? Ваш собственный ранг автоматически изменится на R4.",
            "Code créé. Transmets-le uniquement aux membres de ton alliance." to "Код создан. Передавайте его только участникам своего альянса.",
            "Création du code impossible." to "Не удалось создать код.", "Invitation acceptée. Ton compte a rejoint l'alliance." to "Приглашение принято. Ваш аккаунт присоединился к альянсу.",
            "Invitation refusée." to "Приглашение отклонено.", "Vérification…" to "Проверка…", "Rejoindre avec ce code" to "Вступить по этому коду",
            "Partage une idée, signale un bug ou propose une amélioration. Tu pourras suivre la réponse du développeur ici." to "Поделитесь идеей, сообщите об ошибке или предложите улучшение. Ответ разработчика можно будет увидеть здесь.",
            "Toutes les propositions · Développeur" to "Все предложения · Разработчик", "Mes propositions" to "Мои предложения",
            "Aucune proposition pour le moment." to "Пока нет предложений.", "Chargement des propositions impossible." to "Не удалось загрузить предложения.",
            "Proposition envoyée. Tu recevras la réponse dans cette rubrique." to "Предложение отправлено. Ответ появится в этом разделе.",
            "Envoi impossible." to "Не удалось отправить.", "Répondre à la proposition" to "Ответить на предложение",
            "Ta réponse au joueur" to "Ваш ответ игроку", "Réponse enregistrée et visible par le joueur." to "Ответ сохранён и виден игроку.",
            "Enregistrement impossible." to "Не удалось сохранить.", "Enregistrement…" to "Сохранение…", "Envoyer la réponse" to "Отправить ответ"
        )
        AppLanguage.ZH -> mapOf(
            "Héros suivis :" to "已跟踪英雄：", "Ressources :" to "资源：", "Puissance :" to "战力：",
            "Compétences :" to "技能：", "Robot :" to "机器人：", "Rang demandé :" to "申请职位：",
            "À compléter" to "待完善", "profil(s) suffisamment renseigné(s)" to "已完善的资料",
            "à compléter" to "待完善", "Puissance (facultatif)" to "战力（可选）", "Rareté / étoiles" to "稀有度 / 星级",
            "Bonus :" to "加成：", "Éclats de héros" to "英雄碎片", "Départ :" to "起始：",
            "Cible :" to "目标：", "Requis :" to "需要：", "Manquants :" to "缺少：",
            "Tomes de troupes" to "部队手册", "De :" to "从：", "À :" to "至：",
            "Seal of Wisdom :" to "智慧印章：", "Seal of Knowledge :" to "知识印章：",
            "Synthèse (" to "摘要（", "Composition :" to "阵容：", "Ratios de troupes" to "部队比例",
            "À vérifier :" to "待核实：", "Session enregistrée :" to "会话已保存：",
            "Rareté lue sur capture :" to "截图识别的稀有度：", "Spécialité observée :" to "观察到的专长：",
            "Source :" to "来源：", "War / équipement observé :" to "观察到的战争 / 装备：",
            "Alliance cloud liée :" to "已关联的云端联盟：", "Ton rang confirmé dans le cloud :" to "云端确认的职位：",
            "Compte joueur :" to "玩家账户：", "Pré-analyse IA :" to "AI 初步分析：",
            "Héros / compétences :" to "英雄 / 技能：", "non renseignée" to "未填写",
            "Catégorie" to "类别", " joueur(s)" to " 名玩家", "Description non disponible" to "暂无描述",
            "Confirmer que " to "确认 ", " devient R5 ? Ton propre rang passera automatiquement à R4." to " 成为 R5？你的职位将自动变为 R4。",
            "Code créé. Transmets-le uniquement aux membres de ton alliance." to "邀请码已创建。请仅分享给联盟成员。",
            "Création du code impossible." to "无法创建邀请码。", "Invitation acceptée. Ton compte a rejoint l'alliance." to "邀请已接受，你的账户已加入联盟。",
            "Invitation refusée." to "邀请已拒绝。", "Vérification…" to "正在验证…", "Rejoindre avec ce code" to "使用此邀请码加入",
            "Partage une idée, signale un bug ou propose une amélioration. Tu pourras suivre la réponse du développeur ici." to "分享想法、报告错误或提出改进建议。你可以在这里查看开发者的回复。",
            "Toutes les propositions · Développeur" to "全部建议 · 开发者", "Mes propositions" to "我的建议",
            "Aucune proposition pour le moment." to "目前没有建议。", "Chargement des propositions impossible." to "无法加载建议。",
            "Proposition envoyée. Tu recevras la réponse dans cette rubrique." to "建议已发送，你会在此栏目收到回复。",
            "Envoi impossible." to "发送失败。", "Répondre à la proposition" to "回复建议",
            "Ta réponse au joueur" to "给玩家的回复", "Réponse enregistrée et visible par le joueur." to "回复已保存，玩家可以查看。",
            "Enregistrement impossible." to "保存失败。", "Enregistrement…" to "正在保存…", "Envoyer la réponse" to "发送回复"
        )
        AppLanguage.IT -> mapOf(
            "Héros suivis :" to "Eroi seguiti:", "Ressources :" to "Risorse:", "Puissance :" to "Potenza:",
            "Compétences :" to "Abilità:", "Robot :" to "Robot:", "Rang demandé :" to "Ruolo richiesto:",
            "À compléter" to "Da completare", "profil(s) suffisamment renseigné(s)" to "profilo/i compilato/i a sufficienza",
            "à compléter" to "da completare", "Puissance (facultatif)" to "Potenza (facoltativa)", "Rareté / étoiles" to "Rarità / stelle",
            "Bonus :" to "Bonus:", "Éclats de héros" to "Frammenti eroe", "Départ :" to "Partenza:",
            "Cible :" to "Obiettivo:", "Requis :" to "Richiesto:", "Manquants :" to "Mancanti:",
            "Tomes de troupes" to "Manuali delle truppe", "De :" to "Da:", "À :" to "A:",
            "Composition :" to "Composizione:", "Ratios de troupes" to "Rapporti delle truppe",
            "À vérifier :" to "Da verificare:", "Session enregistrée :" to "Sessione salvata:",
            "Source :" to "Fonte:", "Compte joueur :" to "Account giocatore:",
            "Catégorie" to "Categoria", "Description non disponible" to "Descrizione non disponibile",
            "Vérification…" to "Verifica…", "Rejoindre avec ce code" to "Unisciti con questo codice"
        )
    }
    val extraFragments = when (language) {
        AppLanguage.FR -> emptyMap()
        AppLanguage.EN -> mapOf(
            "Lands of Jail détecté au premier plan." to "Lands of Jail detected in the foreground.",
            "héros référencés" to "heroes listed", " héros suivis sur ton compte" to " heroes tracked on your account",
            "Champs précis à vérifier :" to "Specific fields to verify:", "Champs à vérifier :" to "Fields to verify:",
            "Équipement" to "Equipment", "Type à confirmer" to "Type to confirm", "Incertain" to "Uncertain",
            "Rareté :" to "Rarity:", "Saison :" to "Season:", "Classe :" to "Class:",
            "Puissance actuelle :" to "Current power:", "Non capturée" to "Not captured",
            "affichage tronqué" to "truncated display",
            "Règle d'or dès Niv. 9 : gardez le Centre de communications à Niv." to "Golden rule from Lv. 9: keep the Communications Center at Lv.",
            "Total 1★→5★ : 1 075 éclats (+10 recrutement). Seuil 4★ recommandé : 475 éclats." to "Total 1★→5★: 1,075 shards (+10 recruitment). Recommended 4★ threshold: 475 shards.",
            "Héros du profil — compétences correspondantes" to "Profile heroes — matching skills",
            "Robots du profil — correspondances à vérifier" to "Profile robots — matches to verify",
            "Mot de passe (8 caractères minimum)" to "Password (at least 8 characters)",
            "Profil :" to "Profile:", " avec compétence War identifiée · " to " with identified War skill · ",
            " héros · " to " heroes · ", " robots · " to " robots · ",
            " avec bonus ou compétence renseigné." to " with a bonus or skill recorded.",
            "Toi :" to "You:", " héros analysés" to " heroes analyzed",
            "Mise à jour…" to "Updating…",
            "Les profils synchronisés sont chargés depuis le cloud selon les droits de visibilité du rang dans l'alliance. Les ajouts manuels effectués ici restent enregistrés localement sur cet appareil." to "Synced profiles are loaded from the cloud according to alliance-rank visibility permissions. Manual additions made here remain stored locally on this device.",
            "Héros et compétences War (copier les infos)" to "Heroes and War skills (paste information)",
            "Actualiser le lien cloud après connexion" to "Refresh the cloud link after signing in",
            "Création…" to "Creating…", "Génération…" to "Generating…"
        )
        AppLanguage.DE -> mapOf(
            "Lands of Jail détecté au premier plan." to "Lands of Jail im Vordergrund erkannt.",
            "héros référencés" to "Helden im Katalog", " héros suivis sur ton compte" to " Helden in deinem Konto verfolgt",
            "Champs précis à vérifier :" to "Zu prüfende Felder:", "Champs à vérifier :" to "Zu prüfende Felder:",
            "Équipement" to "Ausrüstung", "Type à confirmer" to "Typ bestätigen", "Incertain" to "Unsicher",
            "Rareté :" to "Seltenheit:", "Saison :" to "Saison:", "Classe :" to "Klasse:",
            "Puissance actuelle :" to "Aktuelle Macht:", "Non capturée" to "Nicht erfasst",
            "affichage tronqué" to "abgeschnittene Anzeige",
            "Règle d'or dès Niv. 9 : gardez le Centre de communications à Niv." to "Goldene Regel ab Stufe 9: Kommunikationszentrum auf Stufe halten",
            "Total 1★→5★ : 1 075 éclats (+10 recrutement). Seuil 4★ recommandé : 475 éclats." to "Gesamt 1★→5★: 1.075 Splitter (+10 Rekrutierungen). Empfohlene 4★-Schwelle: 475 Splitter.",
            "Héros du profil — compétences correspondantes" to "Profilhelden — passende Fähigkeiten",
            "Robots du profil — correspondances à vérifier" to "Profil-Roboter — Übereinstimmungen prüfen",
            "Mot de passe (8 caractères minimum)" to "Passwort (mindestens 8 Zeichen)",
            "Profil :" to "Profil:", " avec compétence War identifiée · " to " mit erkannter War-Fähigkeit · ",
            " héros · " to " Helden · ", " robots · " to " Roboter · ",
            " avec bonus ou compétence renseigné." to " mit eingetragenem Bonus oder Fähigkeit.",
            "Toi :" to "Du:", " héros analysés" to " analysierte Helden",
            "Mise à jour…" to "Wird aktualisiert…",
            "Les profils synchronisés sont chargés depuis le cloud selon les droits de visibilité du rang dans l'alliance. Les ajouts manuels effectués ici restent enregistrés localement sur cet appareil." to "Synchronisierte Profile werden gemäß den Sichtbarkeitsrechten des Allianzrangs aus der Cloud geladen. Manuelle Ergänzungen bleiben lokal auf diesem Gerät gespeichert.",
            "Héros et compétences War (copier les infos)" to "Helden und War-Fähigkeiten (Informationen einfügen)",
            "Actualiser le lien cloud après connexion" to "Cloud-Verknüpfung nach der Anmeldung aktualisieren",
            "Création…" to "Wird erstellt…", "Génération…" to "Wird generiert…"
        )
        AppLanguage.TR -> mapOf(
            "Lands of Jail détecté au premier plan." to "Lands of Jail ön planda algılandı.",
            "héros référencés" to "kayıtlı kahraman", " héros suivis sur ton compte" to " hesabında takip edilen kahraman",
            "Champs précis à vérifier :" to "Kontrol edilecek alanlar:", "Champs à vérifier :" to "Kontrol edilecek alanlar:",
            "Équipement" to "Ekipman", "Type à confirmer" to "Doğrulanacak tür", "Incertain" to "Belirsiz",
            "Rareté :" to "Nadirlik:", "Saison :" to "Sezon:", "Classe :" to "Sınıf:",
            "Puissance actuelle :" to "Mevcut güç:", "Non capturée" to "Yakalanmadı",
            "affichage tronqué" to "kesilmiş görüntü",
            "Règle d'or dès Niv. 9 : gardez le Centre de communications à Niv." to "9. seviyeden itibaren altın kural: İletişim Merkezi seviyesini şu düzeyde tutun:",
            "Total 1★→5★ : 1 075 éclats (+10 recrutement). Seuil 4★ recommandé : 475 éclats." to "Toplam 1★→5★: 1.075 parça (+10 işe alım). Önerilen 4★ eşiği: 475 parça.",
            "Héros du profil — compétences correspondantes" to "Profil kahramanları — eşleşen beceriler",
            "Robots du profil — correspondances à vérifier" to "Profil robotları — kontrol edilecek eşleşmeler",
            "Mot de passe (8 caractères minimum)" to "Şifre (en az 8 karakter)",
            "Profil :" to "Profil:", " avec compétence War identifiée · " to " tanımlanan War becerisiyle · ",
            " héros · " to " kahraman · ", " robots · " to " robot · ",
            " avec bonus ou compétence renseigné." to " bonusu veya becerisi girilmiş.",
            "Toi :" to "Sen:", " héros analysés" to " analiz edilen kahraman",
            "Mise à jour…" to "Güncelleniyor…",
            "Les profils synchronisés sont chargés depuis le cloud selon les droits de visibilité du rang dans l'alliance. Les ajouts manuels effectués ici restent enregistrés localement sur cet appareil." to "Senkronize profiller, ittifak rütbesinin görünürlük izinlerine göre buluttan yüklenir. Burada manuel eklenenler bu cihazda yerel olarak saklanır.",
            "Héros et compétences War (copier les infos)" to "Kahramanlar ve War becerileri (bilgileri yapıştır)",
            "Actualiser le lien cloud après connexion" to "Giriş yaptıktan sonra bulut bağlantısını yenile",
            "Création…" to "Oluşturuluyor…", "Génération…" to "Oluşturuluyor…"
        )
        AppLanguage.RU -> mapOf(
            "Lands of Jail détecté au premier plan." to "Игра Lands of Jail обнаружена на переднем плане.",
            "héros référencés" to "героев в каталоге", " héros suivis sur ton compte" to " героев отслеживается в вашем аккаунте",
            "Champs précis à vérifier :" to "Поля для проверки:", "Champs à vérifier :" to "Поля для проверки:",
            "Équipement" to "Снаряжение", "Type à confirmer" to "Тип требует подтверждения", "Incertain" to "Не подтверждено",
            "Rareté :" to "Редкость:", "Saison :" to "Сезон:", "Classe :" to "Класс:",
            "Puissance actuelle :" to "Текущая мощь:", "Non capturée" to "Не захвачено",
            "affichage tronqué" to "текст обрезан",
            "Règle d'or dès Niv. 9 : gardez le Centre de communications à Niv." to "Главное правило с 9-го уровня: держите Центр связи на уровне",
            "Total 1★→5★ : 1 075 éclats (+10 recrutement). Seuil 4★ recommandé : 475 éclats." to "Всего 1★→5★: 1 075 осколков (+10 наймов). Рекомендуемый порог 4★: 475 осколков.",
            "Héros du profil — compétences correspondantes" to "Герои профиля — подходящие навыки",
            "Robots du profil — correspondances à vérifier" to "Роботы профиля — совпадения для проверки",
            "Mot de passe (8 caractères minimum)" to "Пароль (не менее 8 символов)",
            "Profil :" to "Профиль:", " avec compétence War identifiée · " to " с определённым навыком War · ",
            " héros · " to " героев · ", " robots · " to " роботов · ",
            " avec bonus ou compétence renseigné." to " с указанным бонусом или навыком.",
            "Toi :" to "Вы:", " héros analysés" to " проанализированных героев",
            "Mise à jour…" to "Обновление…",
            "Les profils synchronisés sont chargés depuis le cloud selon les droits de visibilité du rang dans l'alliance. Les ajouts manuels effectués ici restent enregistrés localement sur cet appareil." to "Синхронизированные профили загружаются из облака с учётом прав видимости ранга в альянсе. Ручные добавления остаются на этом устройстве.",
            "Héros et compétences War (copier les infos)" to "Герои и навыки War (вставьте данные)",
            "Actualiser le lien cloud après connexion" to "Обновить связь с облаком после входа",
            "Création…" to "Создание…", "Génération…" to "Генерация…"
        )
        AppLanguage.ZH -> mapOf(
            "Lands of Jail détecté au premier plan." to "已检测到 Lands of Jail 正在前台运行。",
            "héros référencés" to "名英雄已收录", " héros suivis sur ton compte" to " 名英雄已在你的账号中追踪",
            "Champs précis à vérifier :" to "需要核对的字段：", "Champs à vérifier :" to "需要核对的字段：",
            "Équipement" to "装备", "Type à confirmer" to "待确认类型", "Incertain" to "不确定",
            "Rareté :" to "稀有度：", "Saison :" to "赛季：", "Classe :" to "类别：",
            "Puissance actuelle :" to "当前战力：", "Non capturée" to "未截图",
            "affichage tronqué" to "显示内容被截断",
            "Règle d'or dès Niv. 9 : gardez le Centre de communications à Niv." to "9级起的重要规则：通讯中心保持在以下等级：",
            "Total 1★→5★ : 1 075 éclats (+10 recrutement). Seuil 4★ recommandé : 475 éclats." to "1★升至5★总计：1,075碎片（另加10次招募）。推荐4★门槛：475碎片。",
            "Héros du profil — compétences correspondantes" to "档案英雄 — 对应技能",
            "Robots du profil — correspondances à vérifier" to "档案机器人 — 待核对匹配项",
            "Mot de passe (8 caractères minimum)" to "密码（至少8个字符）",
            "Profil :" to "档案：", " avec compétence War identifiée · " to " 已识别 War 技能 · ",
            " héros · " to " 名英雄 · ", " robots · " to " 个机器人 · ",
            " avec bonus ou compétence renseigné." to " 已填写加成或技能。",
            "Toi :" to "你：", " héros analysés" to " 名已分析英雄",
            "Mise à jour…" to "正在更新…",
            "Les profils synchronisés sont chargés depuis le cloud selon les droits de visibilité du rang dans l'alliance. Les ajouts manuels effectués ici restent enregistrés localement sur cet appareil." to "同步的档案会根据联盟职位的可见权限从云端加载。此处手动添加的内容仍保存在本设备上。",
            "Héros et compétences War (copier les infos)" to "英雄和 War 技能（粘贴信息）",
            "Actualiser le lien cloud après connexion" to "登录后刷新云端连接",
            "Création…" to "正在创建…", "Génération…" to "正在生成…"
        )
        AppLanguage.IT -> mapOf(
            "Compte & sauvegarde cloud" to "Account e backup cloud",
            "Connecte ton compte pour synchroniser ton profil de joueur et tes héros dans l'espace cloud sécurisé." to "Accedi al tuo account per sincronizzare il profilo giocatore e gli eroi nello spazio cloud sicuro.",
            "Langue de l’application" to "Lingua dell’applicazione",
            "Adresse e-mail" to "Indirizzo e-mail",
            "Mot de passe (8 caractères minimum)" to "Password (almeno 8 caratteri)",
            "Profil joueur et alliance" to "Profilo giocatore e alleanza",
            "Ces informations sont enregistrées pendant la création du compte. Tu n'auras pas à refaire une inscription dans l'espace Alliance." to "Queste informazioni vengono salvate durante la creazione dell’account. Non dovrai registrarti di nuovo nell’area Alleanza.",
            "Pseudo dans Land of Jail" to "Nome giocatore su Land of Jail",
            "Serveur (ex. 311)" to "Server (es. 311)",
            "Tag de l'alliance" to "Tag dell’alleanza",
            "Ton rang actuel" to "Il tuo grado attuale",
            "Choisis R5 uniquement si tu crées un nouvel espace d'alliance. Pour rejoindre une alliance existante, indique ton rang actuel : ta demande sera transmise aux responsables." to "Scegli R5 solo se stai creando un nuovo spazio alleanza. Per unirti a un’alleanza esistente, indica il tuo grado attuale: la richiesta sarà inviata ai responsabili.",
            "J'ai déjà un compte" to "Ho già un account",
            "Créer un compte" to "Crea un account",
            "S'inscrire" to "Registrati",
            "Se connecter" to "Accedi",
            "Renvoyer l'e-mail de confirmation" to "Invia di nuovo l’e-mail di conferma",
            "Actualiser" to "Aggiorna",
            "Déconnexion" to "Esci",
            "Session enregistrée :" to "Sessione salvata:",
            "Les jetons sont chiffrés avec Android Keystore. La synchronisation du profil se lance depuis le formulaire joueur ci-dessous." to "I token sono crittografati con Android Keystore. La sincronizzazione del profilo si avvia dal modulo giocatore qui sotto.",
            "Compte connecté." to "Account connesso.",
            "Une erreur est survenue." to "Si è verificato un errore."
        )
    }
    extraFragments.forEach { (from, to) -> translated = translated.replace(from, to) }
    fragments.forEach { (from, to) -> translated = translated.replace(from, to) }
    return translated
}

private fun resourceUiText(language: AppLanguage, key: String): String {
    val translations = mapOf(
        "power" to listOf("Puissance actuelle", "Current power", "Aktuelle Macht", "Mevcut güç", "Текущая мощь", "当前战力"),
        "not_captured" to listOf("Non capturée", "Not captured", "Nicht erfasst", "Yakalanmadı", "Не зафиксировано", "未采集"),
        "no_resources" to listOf("Aucune ressource capturée pour l’instant.", "No resources captured yet.", "Noch keine Ressourcen erfasst.", "Henüz kaynak kaydedilmedi.", "Ресурсы пока не зафиксированы.", "尚未记录资源。"),
        "robot_inventory" to listOf("Inventaire des robots du profil", "Profile robot inventory", "Roboterinventar des Profils", "Profil robot envanteri", "Инвентарь роботов профиля", "个人资料机器人库存"),
        "robot_name" to listOf("Nom du robot", "Robot name", "Robotername", "Robot adı", "Название робота", "机器人名称"),
        "level" to listOf("Niveau", "Level", "Stufe", "Seviye", "Уровень", "等级"),
        "rarity_stars" to listOf("Rareté / étoiles", "Rarity / stars", "Seltenheit / Sterne", "Nadirlik / yıldız", "Редкость / звёзды", "稀有度 / 星级"),
        "power_optional" to listOf("Puissance (facultatif)", "Power (optional)", "Macht (optional)", "Güç (isteğe bağlı)", "Мощь (необязательно)", "战力（可选）"),
        "active_bonuses" to listOf("Bonus actifs / statistiques", "Active bonuses / stats", "Aktive Boni / Werte", "Aktif bonuslar / istatistikler", "Активные бонусы / характеристики", "生效加成 / 属性"),
        "skills_effects" to listOf("Compétences / effets (facultatif)", "Skills / effects (optional)", "Fähigkeiten / Effekte (optional)", "Beceriler / etkiler (isteğe bağlı)", "Навыки / эффекты (необязательно)", "技能 / 效果（可选）"),
        "save_robot" to listOf("Ajouter / mettre à jour le robot", "Add / update robot", "Roboter hinzufügen / aktualisieren", "Robot ekle / güncelle", "Добавить / обновить робота", "添加 / 更新机器人"),
        "no_robot" to listOf("Aucun robot enregistré dans ce profil.", "No robot saved in this profile.", "Keine Roboter in diesem Profil gespeichert.", "Bu profilde kayıtlı robot yok.", "В этом профиле нет сохранённых роботов.", "此档案尚未保存机器人。"),
        "remove" to listOf("Retirer", "Remove", "Entfernen", "Kaldır", "Удалить", "移除"),
        "event_journal" to listOf("Journal des événements du profil", "Profile event log", "Ereignisprotokoll des Profils", "Profil etkinlik günlüğü", "Журнал событий профиля", "个人资料活动记录"),
        "save_result" to listOf("Enregistrer le résultat", "Save result", "Ergebnis speichern", "Sonucu kaydet", "Сохранить результат", "保存结果"),
        "no_event" to listOf("Aucun résultat d’événement enregistré pour ce profil.", "No event results saved for this profile.", "Keine Ereignisergebnisse für dieses Profil gespeichert.", "Bu profil için etkinlik sonucu kaydedilmedi.", "Для этого профиля нет сохранённых результатов событий.", "此档案尚无活动结果记录。"),
        "prerequisites" to listOf("Prérequis", "Prerequisites", "Voraussetzungen", "Ön koşullar", "Требования", "前置条件"),
        "food" to listOf("Nourriture", "Food", "Nahrung", "Yiyecek", "Еда", "食物"),
        "wood" to listOf("Bois", "Wood", "Holz", "Odun", "Дерево", "木材"),
        "steel" to listOf("Acier", "Steel", "Stahl", "Çelik", "Сталь", "钢材"),
        "gasoline" to listOf("Essence", "Gasoline", "Benzin", "Benzin", "Бензин", "汽油"),
        "verified_source" to listOf("Source : Wiki / lojxlab vérifié", "Source: verified Wiki / lojxlab", "Quelle: geprüftes Wiki / lojxlab", "Kaynak: doğrulanmış Wiki / lojxlab", "Источник: проверенная Wiki / lojxlab", "来源：已核实 Wiki / lojxlab"),
        "estimated_source" to listOf("Source : Estimation (à confirmer en jeu)", "Source: estimate (confirm in game)", "Quelle: Schätzung (im Spiel prüfen)", "Kaynak: tahmin (oyunda doğrulayın)", "Источник: оценка (проверьте в игре)", "来源：估算（请在游戏中确认）"),
        "start" to listOf("Démarrer", "Start", "Starten", "Başlat", "Запустить", "开始"),
        "start_cycles" to listOf("Démarrer tous les cycles actifs", "Start all active cycles", "Alle aktiven Zyklen starten", "Tüm etkin döngüleri başlat", "Запустить все активные циклы", "启动所有启用的计时器"),
        "custom_duration" to listOf("Durée personnalisée (minutes)", "Custom duration (minutes)", "Benutzerdefinierte Dauer (Minuten)", "Özel süre (dakika)", "Своя длительность (минуты)", "自定义时长（分钟）"),
        "duration_range" to listOf("De 1 à 10 080 minutes (7 jours)", "From 1 to 10,080 minutes (7 days)", "Von 1 bis 10.080 Minuten (7 Tage)", "1–10.080 dakika (7 gün)", "От 1 до 10 080 минут (7 дней)", "1 至 10,080 分钟（7 天）")
    )
    val index = when (language) {
        AppLanguage.FR -> 0
        AppLanguage.EN -> 1
        AppLanguage.DE -> 2
        AppLanguage.TR -> 3
        AppLanguage.RU -> 4
        AppLanguage.ZH -> 5
        AppLanguage.IT -> 6
    }
    val italian = if (language == AppLanguage.IT) mapOf(
        "power" to "Potenza attuale", "not_captured" to "Non acquisita",
        "no_resources" to "Nessuna risorsa acquisita per ora.", "robot_inventory" to "Inventario robot del profilo",
        "robot_name" to "Nome robot", "level" to "Livello", "rarity_stars" to "Rarità / stelle",
        "power_optional" to "Potenza (facoltativa)", "active_bonuses" to "Bonus attivi / statistiche",
        "skills_effects" to "Abilità / effetti (facoltativo)", "save_robot" to "Aggiungi / aggiorna robot",
        "no_robot" to "Nessun robot salvato in questo profilo.", "remove" to "Rimuovi",
        "event_journal" to "Registro eventi del profilo", "save_result" to "Salva risultato",
        "no_event" to "Nessun risultato evento salvato per questo profilo.", "prerequisites" to "Prerequisiti",
        "food" to "Cibo", "wood" to "Legno", "steel" to "Acciaio", "gasoline" to "Benzina",
        "verified_source" to "Fonte: Wiki / lojxlab verificata", "estimated_source" to "Fonte: stima (da confermare nel gioco)",
        "start" to "Avvia", "start_cycles" to "Avvia tutti i cicli attivi",
        "custom_duration" to "Durata personalizzata (minuti)", "duration_range" to "Da 1 a 10.080 minuti (7 giorni)"
    )[key] else null
    return italian ?: translations[key]?.getOrNull(index)
        ?: (if (language == AppLanguage.IT) translations[key]?.getOrNull(1) else null)
        ?: key
}

private enum class AssistantTab {
    TABLEAU, HEROS, RESSOURCES, CONSEILS, ALLIANCE, HISTORIQUE, AMELIORATIONS, CODES;

    fun titleFor(lang: AppLanguage): String = when (this) {
        TABLEAU -> when (lang) {
            AppLanguage.FR -> "Tableau"
            AppLanguage.TR -> "Ana Sayfa"
            AppLanguage.RU -> "Главная"
            AppLanguage.ZH -> "主页"
            AppLanguage.DE -> "Start"
            AppLanguage.EN -> "Home"
            AppLanguage.IT -> "Home"
        }
        HEROS -> when (lang) {
            AppLanguage.FR -> "Héros"
            AppLanguage.TR -> "Kahraman"
            AppLanguage.RU -> "Герои"
            AppLanguage.ZH -> "英雄"
            AppLanguage.DE -> "Helden"
            AppLanguage.EN -> "Heroes"
            AppLanguage.IT -> "Eroi"
        }
        RESSOURCES -> when (lang) {
            AppLanguage.FR -> "Ressources"
            AppLanguage.TR -> "Kaynaklar"
            AppLanguage.RU -> "Ресурсы"
            AppLanguage.ZH -> "资源"
            AppLanguage.DE -> "Ressourc."
            AppLanguage.EN -> "Resources"
            AppLanguage.IT -> "Risorse"
        }
        CONSEILS -> when (lang) {
            AppLanguage.FR -> "Événements"
            AppLanguage.TR -> "Etkinlik"
            AppLanguage.RU -> "События"
            AppLanguage.ZH -> "活动"
            AppLanguage.DE -> "Events"
            AppLanguage.EN -> "Events"
            AppLanguage.IT -> "Eventi"
        }
        ALLIANCE -> when (lang) {
            AppLanguage.FR -> "Alliance"
            AppLanguage.TR -> "İttifak"
            AppLanguage.RU -> "Альянс"
            AppLanguage.ZH -> "联盟"
            AppLanguage.DE -> "Allianz"
            AppLanguage.EN -> "Alliance"
            AppLanguage.IT -> "Alleanza"
        }
        HISTORIQUE -> when (lang) {
            AppLanguage.FR -> "Historique"
            AppLanguage.TR -> "Geçmiş"
            AppLanguage.RU -> "История"
            AppLanguage.ZH -> "记录"
            AppLanguage.DE -> "Verlauf"
            AppLanguage.EN -> "History"
            AppLanguage.IT -> "Cronologia"
        }
        CODES -> when (lang) {
            AppLanguage.FR -> "Codes"
            AppLanguage.EN -> "Codes"
            AppLanguage.DE -> "Codes"
            AppLanguage.TR -> "Kodlar"
            AppLanguage.RU -> "Коды"
            AppLanguage.ZH -> "兑换码"
            AppLanguage.IT -> "Codici"
        }
        AMELIORATIONS -> when (lang) {
            AppLanguage.FR -> "Idées"
            AppLanguage.TR -> "Fikirler"
            AppLanguage.RU -> "Идеи"
            AppLanguage.ZH -> "建议"
            AppLanguage.DE -> "Ideen"
            AppLanguage.EN -> "Ideas"
            AppLanguage.IT -> "Idee"
        }
    }
}

private enum class ResourceSubTab {
    INVENTAIRE, ROBOTS, EVENEMENTS, BATIMENTS, HEROS_OBJETS, DEVELOPPEMENTS, ROUTINE;

    fun labelFor(lang: AppLanguage): String = when (this) {
        INVENTAIRE -> when (lang) {
            AppLanguage.FR -> "Inventaire"
            AppLanguage.TR -> "Envanter"
            AppLanguage.RU -> "Инвентарь"
            AppLanguage.ZH -> "库存"
            AppLanguage.DE -> "Inventar"
            AppLanguage.EN -> "Inventory"
            AppLanguage.IT -> "Inventario"
        }
        BATIMENTS -> when (lang) {
            AppLanguage.FR -> "Calcul Bâtiments"
            AppLanguage.TR -> "Bina Hesabı"
            AppLanguage.RU -> "Кальк. зданий"
            AppLanguage.ZH -> "建筑计算"
            AppLanguage.DE -> "Gebäude"
            AppLanguage.EN -> "Buildings Calc"
            AppLanguage.IT -> "Calcolo edifici"
        }
        HEROS_OBJETS -> when (lang) {
            AppLanguage.FR -> "Calcul Héros"
            AppLanguage.TR -> "Kahraman Hesabı"
            AppLanguage.RU -> "Кальк. героев"
            AppLanguage.ZH -> "英雄计算"
            AppLanguage.DE -> "Helden-Rechner"
            AppLanguage.EN -> "Hero Calc"
            AppLanguage.IT -> "Calcolo eroi"
        }
        DEVELOPPEMENTS -> when (lang) {
            AppLanguage.FR -> "Développements"
            AppLanguage.TR -> "Gelişim"
            AppLanguage.RU -> "Развитие"
            AppLanguage.ZH -> "发展"
            AppLanguage.DE -> "Entwicklung"
            AppLanguage.EN -> "Development"
            AppLanguage.IT -> "Sviluppo"
        }
        ROUTINE -> when (lang) {
            AppLanguage.FR -> "Routine Hors-Jeu"
            AppLanguage.TR -> "Çevrimdışı Rutin"
            AppLanguage.RU -> "Авто-таймеры"
            AppLanguage.ZH -> "离线循环"
            AppLanguage.DE -> "Offline-Routine"
            AppLanguage.EN -> "Offline Routine"
            AppLanguage.IT -> "Routine offline"
        }
        ROBOTS -> when (lang) {
            AppLanguage.FR -> "Robots"
            AppLanguage.TR -> "Robotlar"
            AppLanguage.RU -> "Роботы"
            AppLanguage.ZH -> "机器人"
            AppLanguage.DE -> "Roboter"
            AppLanguage.EN -> "Robots"
            AppLanguage.IT -> "Robot"
        }
        EVENEMENTS -> when (lang) {
            AppLanguage.FR -> "Suivi événements"
            AppLanguage.TR -> "Etkinlik takibi"
            AppLanguage.RU -> "События"
            AppLanguage.ZH -> "活动记录"
            AppLanguage.DE -> "Eventverlauf"
            AppLanguage.EN -> "Event tracking"
            AppLanguage.IT -> "Monitoraggio eventi"
        }
    }
}

private data class AndroidRoutineItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val minutes: Int,
    val enabled: Boolean,
    val endsAtMs: Long?,
    val pausedRemainingMs: Long? = null,
    val durationInput: String = minutes.toString()
)

private data class AndroidQueuedUri(
    val id: String,
    val uri: Uri,
    val label: String,
    val progress: Int = 0,
    val status: String = "waiting",
    val summary: String? = null
)

private data class WardenLevelReq(
    val level: Int,
    val prereqs: String,
    val food: Long,
    val wood: Long,
    val steel: Long,
    val gasoline: Long,
    val verified: Boolean
)

private val WARDEN_LEVELS_TABLE = listOf(
    WardenLevelReq(2, "Cuisine Niv. 1", 0, 350, 0, 0, true),
    WardenLevelReq(3, "Cellule 1 Niv. 2", 0, 1_600, 0, 0, true),
    WardenLevelReq(4, "Usine Niv. 3", 0, 3_600, 700, 0, true),
    WardenLevelReq(5, "Cellule 3 Niv. 3", 0, 15_000, 3_000, 0, true),
    WardenLevelReq(6, "Approvisionnement 1 · Douches 5", 0, 40_000, 8_000, 2_000, true),
    WardenLevelReq(7, "Cour Niv. 5", 0, 140_000, 28_000, 2_000, true),
    WardenLevelReq(8, "Caserne Shieldbearers Niv. 7", 0, 240_000, 48_000, 12_000, true),
    WardenLevelReq(9, "Comms Niv. 8 · Infirmerie 1", 0, 500_000, 100_000, 25_000, true),
    WardenLevelReq(10, "Caserne Shooters 9 · Labo 1", 0, 900_000, 180_000, 45_000, true),
    WardenLevelReq(11, "Comms 10 · Caserne Bombers 10", 2_500_000, 2_500_000, 500_000, 125_000, true),
    WardenLevelReq(12, "Comms 11 · Commandement 1", 3_000_000, 3_000_000, 600_000, 150_000, true),
    WardenLevelReq(13, "Comms 12 · Caserne Shieldbearers 12", 4_500_000, 4_500_000, 900_000, 225_000, true),
    WardenLevelReq(14, "Comms 13 · Caserne Shooters 13", 6_000_000, 6_000_000, 1_200_000, 300_000, true),
    WardenLevelReq(15, "Comms 14 · Caserne Bombers 14", 9_000_000, 9_000_000, 1_800_000, 450_000, true),
    WardenLevelReq(16, "Comms 15 · Shieldbearers 15 (Crystal Clash)", 18_000_000, 18_000_000, 3_600_000, 900_000, true),
    WardenLevelReq(17, "Comms 16 · Shooters 16", 25_000_000, 25_000_000, 5_000_000, 1_200_000, true),
    WardenLevelReq(18, "Comms 17 · Bombers 17", 30_000_000, 30_000_000, 6_000_000, 1_500_000, true),
    WardenLevelReq(19, "Comms 18 · Caserne Niv. 18 (Estimé)", 35_000_000, 35_000_000, 7_000_000, 1_750_000, false),
    WardenLevelReq(20, "Comms 19 · Laboratoire 19", 40_000_000, 40_000_000, 8_000_000, 2_000_000, true),
    WardenLevelReq(21, "Comms 20 · Caserne 20 (Estimé)", 55_000_000, 55_000_000, 11_000_000, 2_750_000, false),
    WardenLevelReq(22, "Comms 21 · Caserne 21 (Estimé)", 75_000_000, 75_000_000, 15_000_000, 3_750_000, false),
    WardenLevelReq(23, "Comms 22 · Caserne 22 (Estimé)", 100_000_000, 100_000_000, 20_000_000, 5_000_000, false),
    WardenLevelReq(24, "Comms 23 · Labo/Caserne 23 (Estimé)", 130_000_000, 130_000_000, 26_000_000, 6_500_000, false),
    WardenLevelReq(25, "Comms 24 · Bâtiment Niv. 24", 160_000_000, 160_000_000, 32_000_000, 8_000_000, true),
    WardenLevelReq(26, "Comms 25 · Caserne 25 (Estimé)", 210_000_000, 210_000_000, 42_000_000, 10_500_000, false),
    WardenLevelReq(27, "Comms 26 · Caserne 26 (Estimé)", 270_000_000, 270_000_000, 54_000_000, 13_500_000, false),
    WardenLevelReq(28, "Comms 27 · Labo 27 (Estimé)", 340_000_000, 340_000_000, 68_000_000, 17_000_000, false),
    WardenLevelReq(29, "Comms 28 · Caserne 28 (Estimé)", 430_000_000, 430_000_000, 86_000_000, 21_500_000, false),
    WardenLevelReq(30, "Comms 29 · Bâtiment 29 → Débloque Forticlad", 550_000_000, 550_000_000, 110_000_000, 27_500_000, false)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandsOfJailAssistantApp(
    repository: GeminiAssistantRepository,
    sharedScreenshotUri: Uri?,
    onConsumeSharedUri: () -> Unit,
    capturedScreenPayload: CapturedScreenPayload?,
    onConsumeCapturedScreen: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var currentLang by remember(context) {
        mutableStateOf(
            runCatching {
                val savedCode = context.getSharedPreferences(
                    "lands_of_jail_account_v1",
                    Context.MODE_PRIVATE
                ).getString("app_language_code", AppLanguage.FR.code)
                AppLanguage.entries.firstOrNull { it.code == savedCode } ?: AppLanguage.FR
            }.getOrDefault(AppLanguage.FR)
        )
    }
    LaunchedEffect(currentLang) {
        context.getSharedPreferences("lands_of_jail_account_v1", Context.MODE_PRIVATE)
            .edit()
            .putString("app_language_code", currentLang.code)
            .apply()
    }
    var currentTab by remember { mutableStateOf(AssistantTab.TABLEAU) }
    val giftCodeJson = remember { Json { ignoreUnknownKeys = true } }
    val giftCodePrefs = remember(context) { context.getSharedPreferences("gift_codes_cache_v1", Context.MODE_PRIVATE) }
    var giftCodeSnapshot by remember {
        mutableStateOf(runCatching {
            giftCodeJson.decodeFromString<GiftCodeSnapshot>(giftCodePrefs.getString("snapshot", "") ?: "")
        }.getOrElse { GiftCodeRepository.initialSnapshot() })
    }
    var giftCodesRefreshing by remember { mutableStateOf(false) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && !giftCodesRefreshing) {
                scope.launch {
                    giftCodesRefreshing = true
                    try {
                        val refreshed = withContext(Dispatchers.IO) { GiftCodeRepository.refresh(giftCodeSnapshot) }
                        giftCodeSnapshot = refreshed
                        giftCodePrefs.edit().putString("snapshot", giftCodeJson.encodeToString(refreshed)).apply()
                    } finally {
                        giftCodesRefreshing = false
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var playerRole by remember { mutableStateOf(PlayerRole.POLYVALENT_F2P) }
    var packageFilter by remember { mutableStateOf(UsageStatsDetector.DEFAULT_GAME_PACKAGE_KEYWORD) }
    var alwaysKeepOverlay by remember { mutableStateOf(true) }

    var hasUsagePermission by remember { mutableStateOf(UsageStatsDetector.hasUsageStatsPermission(context)) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val isMonitoringRunning by GameForegroundMonitorService.isRunning.collectAsState()
    val isGameForeground by GameForegroundMonitorService.detectedGameForeground.collectAsState()
    val isAutoCapturing by GameForegroundMonitorService.isAutoCapturing.collectAsState()
    var autoCaptureIntervalSeconds by remember { mutableIntStateOf(10) }

    val accountPrefs = remember(context) {
        context.getSharedPreferences("lands_of_jail_account_v1", Context.MODE_PRIVATE)
    }
    val accountJson = remember { Json { ignoreUnknownKeys = true } }
    val profileAuth = remember(context) { SupabaseAuthRepository(context) }
    var accountDataLoaded by remember { mutableStateOf(false) }
    var allianceWorkspace by remember {
        mutableStateOf(runCatching {
            accountJson.decodeFromString<AllianceWorkspace>(
                accountPrefs.getString("alliance_workspace", "{}") ?: "{}"
            )
        }.getOrDefault(AllianceWorkspace()))
    }

    // Player identity and alliance details are now collected directly in account signup.
    var showAllianceOnboarding by remember { mutableStateOf(false) }
    var onboardingPlayerName by remember { mutableStateOf(allianceWorkspace.ownProfile.playerName) }
    var onboardingServer by remember { mutableStateOf(allianceWorkspace.server) }
    var onboardingAllianceTag by remember { mutableStateOf(allianceWorkspace.allianceName) }
    var onboardingRank by remember { mutableStateOf(allianceWorkspace.ownProfile.rank) }

    var analyses by remember {
        mutableStateOf(runCatching {
            accountJson.decodeFromString<List<ScreenshotAnalysis>>(
                accountPrefs.getString("analyses", "[]") ?: "[]"
            )
        }.getOrDefault(emptyList()))
    }
    var heroes by remember {
        mutableStateOf(runCatching {
            accountJson.decodeFromString<List<HeroRecord>>(
                accountPrefs.getString("heroes", "[]") ?: "[]"
            )
        }.getOrDefault(emptyList()))
    }
    var resources by remember {
        mutableStateOf(runCatching {
            accountJson.decodeFromString<List<ResourceRecord>>(
                accountPrefs.getString("resources", "[]") ?: "[]"
            )
        }.getOrDefault(emptyList()))
    }
    var robots by remember { mutableStateOf(emptyList<RobotRecord>()) }
    var eventRecords by remember { mutableStateOf(emptyList<EventProgressRecord>()) }
    var currentPower by remember {
        mutableStateOf(runCatching {
            accountJson.decodeFromString<PowerMetric?>(
                accountPrefs.getString("current_power", "null") ?: "null"
            )
        }.getOrNull())
    }

    var localProfiles by remember {
        mutableStateOf(runCatching {
            accountJson.decodeFromString<List<LocalGameAccountProfile>>(
                accountPrefs.getString("local_game_profiles_v1", "[]") ?: "[]"
            )
        }.getOrDefault(emptyList()))
    }
    var activeProfileId by remember {
        mutableStateOf(accountPrefs.getString("active_game_profile_v1", null).orEmpty())
    }
    var showProfileDialog by remember { mutableStateOf(false) }
    var editingProfileId by remember { mutableStateOf<String?>(null) }
    var profileNameInput by remember { mutableStateOf("") }
    var deletingProfileId by remember { mutableStateOf<String?>(null) }
    var isProfileSyncing by remember { mutableStateOf(false) }
    var selectedAdviceMode by remember { mutableStateOf(AdviceMode.TRIAL_CAGE) }
    var adviceResult by remember { mutableStateOf<TacticalAdviceResult?>(null) }
    val gameIntelPrefs = remember(context) {
        context.getSharedPreferences("game_intel_cache_v1", Context.MODE_PRIVATE)
    }
    var gameIntel by remember {
        mutableStateOf(runCatching {
            gameIntelPrefs.getString("snapshot", null)?.let {
                accountJson.decodeFromString<GameIntelSnapshot>(it)
            }
        }.getOrNull())
    }
    var gameIntelLoading by remember { mutableStateOf(false) }
    var gameIntelError by remember { mutableStateOf<String?>(null) }
    var routineRevision by remember { mutableIntStateOf(0) }

    // Une seule actualisation à l'ouverture, uniquement si un backend est configuré.
    // En mode local-first (backend vide), aucune requête réseau inutile n'est lancée :
    // le dernier instantané sauvegardé reste disponible hors ligne.
    LaunchedEffect(Unit) {
        if (repository.isRemoteBackendEnabled) {
            gameIntelLoading = true
            repository.fetchGameIntel(forceRefresh = false)
                .onSuccess { snapshot ->
                    gameIntel = snapshot
                    gameIntelPrefs.edit()
                        .putString("snapshot", accountJson.encodeToString(snapshot))
                        .putLong("saved_at", System.currentTimeMillis())
                        .apply()
                }
                .onFailure { gameIntelError = it.message ?: "Veille indisponible." }
            gameIntelLoading = false
        } else {
            gameIntelLoading = false
            gameIntelError = null
        }
    }

    fun readRoutineState(profileId: String): Map<String, String> {
        val prefs = context.getSharedPreferences("offline_routine_" + profileId, Context.MODE_PRIVATE)
        return prefs.all.mapNotNull { (key, value) ->
            val encoded = when (value) {
                is String -> "S|" + value
                is Int -> "I|" + value
                is Long -> "L|" + value
                is Boolean -> "B|" + value
                is Float -> "F|" + value
                is Set<*> -> "SS|" + value.filterIsInstance<String>().joinToString("\u001F")
                else -> null
            }
            encoded?.let { key to it }
        }.toMap()
    }

    fun restoreRoutineState(profileId: String, state: Map<String, String>, clearWhenEmpty: Boolean = false) {
        // Older snapshots did not contain routine data: preserve local timers for those.
        // Versioned snapshots may intentionally contain no timers and must clear stale state.
        if (state.isEmpty() && !clearWhenEmpty) return
        val prefs = context.getSharedPreferences("offline_routine_" + profileId, Context.MODE_PRIVATE)
        val editor = prefs.edit().clear()
        state.forEach { (key, encoded) ->
            val separator = encoded.indexOf('|')
            if (separator > 0) {
                val type = encoded.substring(0, separator)
                val value = encoded.substring(separator + 1)
                when (type) {
                    "S" -> editor.putString(key, value)
                    "I" -> value.toIntOrNull()?.let { editor.putInt(key, it) }
                    "L" -> value.toLongOrNull()?.let { editor.putLong(key, it) }
                    "B" -> editor.putBoolean(key, value.toBoolean())
                    "F" -> value.toFloatOrNull()?.let { editor.putFloat(key, it) }
                    "SS" -> editor.putStringSet(key, value.split("\u001F").filter { it.isNotEmpty() }.toSet())
                }
            }
        }
        editor.commit()
    }

    fun currentProfileSnapshot() = LocalGameProfileSnapshot(
        allianceWorkspace = allianceWorkspace,
        analyses = analyses.take(50),
        heroes = heroes,
        resources = resources,
        robots = robots,
        eventRecords = eventRecords,
        currentPower = currentPower,
        adviceMode = selectedAdviceMode.name,
        adviceResult = adviceResult,
        playerRole = playerRole.name,
        routineState = if (activeProfileId.isNotBlank()) readRoutineState(activeProfileId) else emptyMap(),
        schemaVersion = 2
    )

    fun saveActiveProfile() {
        if (activeProfileId.isBlank()) return
        val snapshotJson = accountJson.encodeToString(currentProfileSnapshot())
        localProfiles = localProfiles.map { profile ->
            if (profile.id == activeProfileId) profile.copy(
                stateJson = snapshotJson,
                updatedAtIso = Instant.now().toString()
            ) else profile
        }
    }

    LaunchedEffect(Unit) {
        if (localProfiles.isEmpty()) {
            // One-time, additive migration: copy the existing account data into profile 1.
            // Legacy keys remain untouched as a recovery copy.
            val id = UUID.randomUUID().toString()
            val first = LocalGameAccountProfile(
                id = id,
                name = "Profil 1",
                stateJson = accountJson.encodeToString(currentProfileSnapshot()),
                updatedAtIso = Instant.now().toString()
            )
            localProfiles = listOf(first)
            activeProfileId = id
            accountPrefs.edit()
                .putString("local_game_profiles_v1", accountJson.encodeToString(listOf(first)))
                .putString("active_game_profile_v1", id)
                .putBoolean("local_game_profiles_migrated_v1", true)
                .commit()
        } else {
            val selected = localProfiles.firstOrNull { it.id == activeProfileId }
                ?: localProfiles.first()
            activeProfileId = selected.id
            runCatching {
                val snapshot = accountJson.decodeFromString<LocalGameProfileSnapshot>(selected.stateJson)
                allianceWorkspace = snapshot.allianceWorkspace
                analyses = snapshot.analyses
                heroes = snapshot.heroes
                resources = snapshot.resources
                                             robots = snapshot.robots
                                             eventRecords = snapshot.eventRecords
                currentPower = snapshot.currentPower
                selectedAdviceMode = runCatching { AdviceMode.valueOf(snapshot.adviceMode) }.getOrDefault(AdviceMode.TRIAL_CAGE)
                adviceResult = snapshot.adviceResult
                playerRole = runCatching { PlayerRole.valueOf(snapshot.playerRole) }.getOrDefault(PlayerRole.POLYVALENT_F2P)
                restoreRoutineState(selected.id, snapshot.routineState, clearWhenEmpty = snapshot.schemaVersion != null && snapshot.schemaVersion >= 2)
                routineRevision += 1
            }
            accountPrefs.edit().putString("active_game_profile_v1", selected.id).commit()
        }
        accountDataLoaded = true
    }

    LaunchedEffect(allianceWorkspace, analyses, heroes, resources, robots, eventRecords, currentPower, selectedAdviceMode, adviceResult, playerRole, activeProfileId, accountDataLoaded) {
        if (accountDataLoaded && activeProfileId.isNotBlank()) {
            val snapshotJson = accountJson.encodeToString(currentProfileSnapshot())
            localProfiles = localProfiles.map { profile ->
                if (profile.id == activeProfileId && profile.stateJson != snapshotJson) profile.copy(
                    stateJson = snapshotJson,
                    updatedAtIso = Instant.now().toString()
                ) else profile
            }
            accountPrefs.edit()
                .putString("local_game_profiles_v1", accountJson.encodeToString(localProfiles))
                .putString("active_game_profile_v1", activeProfileId)
                .commit()
        }
    }

    LaunchedEffect(localProfiles, accountDataLoaded) {
        if (accountDataLoaded) {
            accountPrefs.edit()
                .putString("local_game_profiles_v1", accountJson.encodeToString(localProfiles))
                .commit()
        }
    }

    LaunchedEffect(allianceWorkspace) {
        accountPrefs.edit()
            .putString("alliance_workspace", accountJson.encodeToString(allianceWorkspace))
            .apply()
    }

    LaunchedEffect(analyses, heroes, resources, currentPower) {
        accountPrefs.edit()
            .putString("analyses", accountJson.encodeToString(analyses.take(50)))
            .putString("heroes", accountJson.encodeToString(heroes))
            .putString("resources", accountJson.encodeToString(resources))
            .putString("current_power", accountJson.encodeToString(currentPower))
            .apply()
    }

    var isAnalyzing by remember { mutableStateOf(false) }
    var singleAnalysisPercent by remember { mutableIntStateOf(0) }
    var analysisQueue by remember { mutableStateOf<List<AndroidQueuedUri>>(emptyList()) }
    var isQueueRunning by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isFetchingAdvice by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsagePermission = UsageStatsDetector.hasUsageStatsPermission(context)
                hasOverlayPermission = Settings.canDrawOverlays(context)

                // Check pending alliance requests when the app starts or returns to foreground.
                val linkedAllianceId = accountPrefs.getString("cloud_alliance_id", "").orEmpty()
                if (profileAuth.currentSession() != null && linkedAllianceId.isNotBlank()) {
                    // This observer outlives individual recompositions; use the Activity lifecycle scope.
                    lifecycleOwner.lifecycleScope.launch {
                        profileAuth.fetchPendingAllianceJoinRequests(linkedAllianceId).onSuccess { pending ->
                            val previousIds = accountPrefs.getStringSet("notified_join_request_ids", emptySet()).orEmpty().toSet()
                            val currentIds = pending.map { it.id }.toSet()
                            val newCount = (currentIds - previousIds).size
                            if (newCount > 0) notifyAllianceJoinRequests(context, newCount)
                            accountPrefs.edit().putStringSet("notified_join_request_ids", previousIds + currentIds).apply()
                        }
                    }
                }

            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun normalizedKey(value: String): String =
        value.lowercase().trim().replace(Regex("\\s+"), " ")

    fun mergeHeroRecords(existing: List<HeroRecord>, incoming: List<HeroRecord>): List<HeroRecord> {
        val merged = existing.associateBy { normalizedKey(it.nom) }.toMutableMap()
        incoming.forEach { fresh ->
            val key = normalizedKey(fresh.nom)
            if (key.isBlank()) return@forEach
            val previous = merged[key]
            if (previous == null) {
                val initialUncertainFields = if (fresh.incertain && fresh.champsIncertains.isEmpty()) {
                    listOf("niveau", "rarete", "etoiles", "roleHero", "puissanceHero")
                } else fresh.champsIncertains
                merged[key] = fresh.copy(
                    champsIncertains = initialUncertainFields,
                    incertain = fresh.incertain || initialUncertainFields.isNotEmpty()
                )
            } else {
                val personalFieldKeys = setOf("niveau", "rarete", "etoiles", "rolehero", "puissancehero")
                val freshUncertainFields = if (fresh.incertain && fresh.champsIncertains.isEmpty()) {
                    personalFieldKeys
                } else fresh.champsIncertains.map { it.trim().lowercase() }.toSet()
                val previousUncertainFields = if (previous.incertain && previous.champsIncertains.isEmpty()) {
                    personalFieldKeys
                } else previous.champsIncertains.map { it.trim().lowercase() }.toSet()
                fun isFieldUncertain(hero: HeroRecord, uncertainFields: Set<String>, field: String): Boolean =
                    field in uncertainFields || (hero.incertain && uncertainFields.isEmpty())
                fun isFieldAvailable(hero: HeroRecord, field: String): Boolean = when (field) {
                    "niveau" -> hero.niveau != null || hero.niveauTexte.isNotBlank()
                    "rarete" -> !hero.rarete.isNullOrBlank()
                    "etoiles" -> !hero.etoiles.isNullOrBlank()
                    "rolehero" -> !hero.roleHero.isNullOrBlank()
                    "puissancehero" -> !hero.puissanceHero.isNullOrBlank()
                    else -> false
                }
                fun freshFieldReliable(field: String): Boolean =
                    !isFieldUncertain(fresh, freshUncertainFields, field)
                fun previousFieldReliable(field: String): Boolean =
                    !isFieldUncertain(previous, previousUncertainFields, field)
                fun chooseTextField(field: String, oldValue: String?, newValue: String?): String? = when {
                    freshFieldReliable(field) && !newValue.isNullOrBlank() -> newValue
                    !oldValue.isNullOrBlank() -> oldValue
                    else -> newValue
                }
                val unresolvedFields = (previous.champsIncertains + fresh.champsIncertains)
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .filter { field ->
                        val normalizedField = field.lowercase()
                        !(isFieldAvailable(previous, normalizedField) && previousFieldReliable(normalizedField)) &&
                            !(isFieldAvailable(fresh, normalizedField) && freshFieldReliable(normalizedField))
                    }
                val skills = (previous.competences + fresh.competences)
                    .filter { it.nom.isNotBlank() }
                    .groupBy { normalizedKey(it.nom) }
                    .values
                    .map { entries ->
                        val trusted = entries.lastOrNull { !it.incertain }
                        val selected = trusted ?: entries.last()
                        val knownType = entries.lastOrNull {
                            it.type.lowercase() in setOf("war", "arrest", "equipment")
                        }?.type
                        selected.copy(
                            niveau = selected.niveau.ifBlank {
                                entries.lastOrNull { it.niveau.isNotBlank() }?.niveau.orEmpty()
                            },
                            description = selected.description?.takeIf { it.isNotBlank() }
                                ?: entries.mapNotNull { entry -> entry.description?.takeIf { description -> description.isNotBlank() } }.lastOrNull(),
                            type = selected.type.takeIf {
                                it.lowercase() in setOf("war", "arrest", "equipment")
                            } ?: knownType ?: "inconnu",
                            iconUrl = selected.iconUrl ?: entries.mapNotNull { entry -> entry.iconUrl }.lastOrNull(),
                            incertain = trusted == null,
                            raisonIncertitude = if (trusted == null) selected.raisonIncertitude else null
                        )
                    }
                merged[key] = previous.copy(
                    niveau = if (freshFieldReliable("niveau")) fresh.niveau ?: previous.niveau else previous.niveau ?: fresh.niveau,
                    niveauTexte = if (freshFieldReliable("niveau")) {
                        fresh.niveauTexte.takeIf {
                            it.isNotBlank() && !it.contains("non lu", true) && !it.contains("inconnu", true)
                        } ?: previous.niveauTexte
                    } else previous.niveauTexte.ifBlank { fresh.niveauTexte },
                    rarete = chooseTextField("rarete", previous.rarete, fresh.rarete),
                    etoiles = chooseTextField("etoiles", previous.etoiles, fresh.etoiles),
                    roleHero = chooseTextField("rolehero", previous.roleHero, fresh.roleHero),
                    puissanceHero = chooseTextField("puissancehero", previous.puissanceHero, fresh.puissanceHero),
                    imageUrl = fresh.imageUrl ?: previous.imageUrl,
                    avatarUrl = fresh.avatarUrl ?: previous.avatarUrl,
                    titre = fresh.titre ?: previous.titre,
                    typeTroupe = fresh.typeTroupe ?: previous.typeTroupe,
                    categorie = fresh.categorie ?: previous.categorie,
                    saison = fresh.saison ?: previous.saison,
                    puissanceMax5Etoiles = fresh.puissanceMax5Etoiles ?: previous.puissanceMax5Etoiles,
                    resumeBio = fresh.resumeBio ?: previous.resumeBio,
                    idealPour = fresh.idealPour ?: previous.idealPour,
                    wikiUrl = fresh.wikiUrl ?: previous.wikiUrl,
                    champsIncertains = unresolvedFields,
                    competences = skills,
                    incertain = unresolvedFields.isNotEmpty(),
                    raisonIncertitude = if (unresolvedFields.isNotEmpty()) {
                        fresh.raisonIncertitude ?: previous.raisonIncertitude
                    } else null
                )
            }
        }
        return merged.values.toList()
    }

    fun mergeResourceRecords(existing: List<ResourceRecord>, incoming: List<ResourceRecord>): List<ResourceRecord> {
        val merged = existing.associateBy { normalizedKey(it.nom) }.toMutableMap()
        incoming.forEach { fresh ->
            val key = normalizedKey(fresh.nom)
            if (key.isNotBlank()) {
                val previous = merged[key]
                merged[key] = if (previous == null) fresh else fresh.copy(
                    quantiteNumerique = fresh.quantiteNumerique ?: previous.quantiteNumerique,
                    quantiteTexte = fresh.quantiteTexte.ifBlank { previous.quantiteTexte },
                    raisonIncertitude = fresh.raisonIncertitude ?: previous.raisonIncertitude
                )
            }
        }
        return merged.values.toList()
    }

    fun mergeRobotRecords(existing: List<RobotRecord>, incoming: List<RobotRecord>): List<RobotRecord> {
        val merged = existing.toMutableList()
        val robotFields = setOf("niveau", "rarete", "bonus", "competences", "puissance")
        fun valueFor(robot: RobotRecord, field: String): String = when (field) {
            "niveau" -> robot.niveau
            "rarete" -> robot.rarete
            "bonus" -> robot.bonus
            "competences" -> robot.competences
            "puissance" -> robot.puissance
            else -> ""
        }
        incoming.forEach { raw ->
            val robotName = raw.nom.trim()
            if (robotName.isBlank()) return@forEach
            val existingIndex = merged.indexOfFirst { it.nom.equals(robotName, ignoreCase = true) }
            val candidateFields = if (raw.incertain && raw.champsIncertains.isEmpty()) {
                robotFields
            } else raw.champsIncertains.map { it.trim().lowercase() }.toSet()
            val candidate = raw.copy(
                id = raw.id.ifBlank { UUID.randomUUID().toString() },
                champsIncertains = candidateFields.toList(),
                incertain = raw.incertain || candidateFields.isNotEmpty(),
                source = if (raw.source == "saisie_manuel") "capture_ia" else raw.source,
                updatedAtIso = raw.updatedAtIso.ifBlank { Instant.now().toString() }
            )
            if (existingIndex < 0) {
                merged.add(candidate)
            } else {
                val previous = merged[existingIndex]
                val previousFields = if (previous.incertain && previous.champsIncertains.isEmpty()) {
                    robotFields
                } else previous.champsIncertains.map { it.trim().lowercase() }.toSet()
                val keepManual = previous.source == "saisie_manuel"
                fun choose(field: String, oldValue: String, newValue: String): String = when {
                    keepManual && field !in previousFields && oldValue.isNotBlank() -> oldValue
                    field in candidateFields && oldValue.isNotBlank() -> oldValue
                    newValue.isNotBlank() -> newValue
                    else -> oldValue
                }
                val allUncertainFields = (previousFields + candidateFields).intersect(robotFields)
                val unresolvedFields = allUncertainFields.filter { field ->
                    val oldReliable = valueFor(previous, field).isNotBlank() && field !in previousFields
                    val newReliable = valueFor(candidate, field).isNotBlank() && field !in candidateFields
                    !oldReliable && !newReliable
                }
                merged[existingIndex] = previous.copy(
                    niveau = choose("niveau", previous.niveau, candidate.niveau),
                    rarete = choose("rarete", previous.rarete, candidate.rarete),
                    bonus = choose("bonus", previous.bonus, candidate.bonus),
                    competences = choose("competences", previous.competences, candidate.competences),
                    puissance = choose("puissance", previous.puissance, candidate.puissance),
                    champsIncertains = unresolvedFields.toList(),
                    incertain = unresolvedFields.isNotEmpty(),
                    raisonIncertitude = if (unresolvedFields.isNotEmpty()) {
                        candidate.raisonIncertitude ?: previous.raisonIncertitude
                    } else null,
                    source = if (keepManual) previous.source else candidate.source,
                    updatedAtIso = Instant.now().toString()
                )
            }
        }
        return merged.sortedBy { it.nom.lowercase() }
    }

    fun mergeNewAnalysis(result: ScreenshotAnalysis) {
        analyses = (listOf(result) + analyses).take(50)
        result.puissance?.takeIf { it.puissanceTotaleTexte.isNotBlank() }?.let {
            currentPower = it
        }
        heroes = mergeHeroRecords(heroes, result.heros)
        resources = mergeResourceRecords(resources, result.ressources)
        robots = mergeRobotRecords(robots, result.robots)
    }

    /**
     * Les images choisies dans la galerie sont traitées uniquement sur l'appareil.
     * Une image n'est conservée dans filesDir/import_review que si le décodage/OCR
     * échoue ou si aucune donnée exploitable n'a pu être extraite.
     */
    fun keepImportedImageForReview(uri: Uri): String? = runCatching {
        val reviewDir = File(context.filesDir, "import_review").apply { mkdirs() }
        val reviewFile = File(reviewDir, "capture-${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(reviewFile).use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException("Impossible de lire le fichier source")
        Uri.fromFile(reviewFile).toString()
    }.getOrNull()

    suspend fun analyzeSingleUriInternal(uri: Uri, onProgress: (Int) -> Unit): ScreenshotAnalysis {
        onProgress(5)
        val timestamp = Instant.now().toString()

        // Deux lectures complémentaires : OCR local ML Kit et vision Gemini en ligne.
        // L'analyse distante est tentée même si le décodage local échoue.
        val localImage = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()

        val ocrResult = if (localImage != null) {
            try {
                onProgress(20)
                LandsOfJailOcrParser.recognizeBitmapDetailed(localImage)
            } catch (_: Throwable) {
                null
            } finally {
                localImage.recycle()
            }
        } else null
        val ocrRaw = ocrResult?.text.orEmpty()

        val localExtraction = withContext(Dispatchers.Default) {
            LandsOfJailOcrParser.parseLocalOcrText(
                ocrRaw,
                ocrResult?.boxes.orEmpty(),
                ocrResult?.imageWidth ?: 0,
                ocrResult?.imageHeight ?: 0
            )
        }
        onProgress(45)

        val remoteResult = runCatching {
            repository.analyzeSharedScreenshot(
                context = context,
                imageUri = uri,
                playerRole = playerRole,
                note = buildString {
                    append("Analyse d'une capture partagée depuis le jeu. ")
                    append("OCR local complémentaire : ")
                    append(ocrRaw.take(1200).ifBlank { "indisponible" })
                    append(". Vérifie visuellement les données et distingue les faits lisibles des incertitudes.")
                }
            ).getOrThrow()
        }.getOrNull()

        onProgress(85)
        if (remoteResult != null) {
            // La vision en ligne est prioritaire pour les valeurs lues sur l'image.
            // L'OCR local complète les éléments que le modèle distant aurait manqués.
            val combined = remoteResult.copy(
                id = UUID.randomUUID().toString(),
                analyzedAtIso = timestamp,
                imageUriString = "",
                puissance = remoteResult.puissance?.takeIf { it.puissanceTotaleTexte.isNotBlank() }
                    ?: localExtraction.power,
                ressources = mergeResourceRecords(localExtraction.resources, remoteResult.ressources),
                heros = mergeHeroRecords(localExtraction.heroes, remoteResult.heros),
                robots = mergeRobotRecords(localExtraction.robots, remoteResult.robots),
                equipements = (remoteResult.equipements + localExtraction.equipment).distinctBy {
                    (it.piece + it.niveau).lowercase()
                },
                champsIncertains = (remoteResult.champsIncertains + localExtraction.uncertainItems)
                    .distinctBy { normalizedKey(it.champ) + normalizedKey(it.valeurObservee) }
            )
            onProgress(100)
            return combined
        }

        // Si Internet ou Gemini est indisponible, ne pas perdre le résultat local.
        val hasLocalData = localExtraction.power != null ||
            localExtraction.heroes.isNotEmpty() ||
            localExtraction.resources.isNotEmpty() ||
            localExtraction.robots.isNotEmpty() ||
            localExtraction.equipment.isNotEmpty() ||
            localExtraction.detectedCategory == "robots" ||
            localExtraction.detectedCategory == "ressources" ||
            localExtraction.detectedCategory == "equipements"

        if (hasLocalData) {
            onProgress(100)
            return ScreenshotAnalysis(
                id = UUID.randomUUID().toString(),
                analyzedAtIso = timestamp,
                imageUriString = "",
                typeCapture = localExtraction.detectedCategory,
                resumeCapture = "Analyse locale de secours (serveur en ligne indisponible) · " +
                    buildList {
                        localExtraction.power?.let { add("Puissance : ${it.puissanceTotaleTexte}") }
                        if (localExtraction.heroes.isNotEmpty()) add("${localExtraction.heroes.size} héros")
                        if (localExtraction.resources.isNotEmpty()) add("${localExtraction.resources.size} ressource(s)")
                        if (localExtraction.robots.isNotEmpty()) add("${localExtraction.robots.size} robot(s)")
                        if (localExtraction.equipment.isNotEmpty()) add("${localExtraction.equipment.size} pièce(s) d'équipement")
                    }.joinToString(" · "),
                puissance = localExtraction.power,
                ressources = localExtraction.resources,
                heros = localExtraction.heroes,
                robots = localExtraction.robots,
                equipements = localExtraction.equipment,
                champsIncertains = localExtraction.uncertainItems + UncertainItem(
                    "Analyse en ligne",
                    "Indisponible",
                    "La capture a été lue localement, mais l'analyse visuelle en ligne n'a pas répondu. Certaines données peuvent manquer."
                )
            )
        }

        // Aucun résultat exploitable : conserver une copie privée pour permettre une vérification.
        val retainedUri = keepImportedImageForReview(uri).orEmpty()
        val reason = when {
            localImage == null -> "Le décodage local a échoué"
            ocrRaw.isBlank() -> "Aucun texte lisible détecté"
            else -> "Aucune donnée structurée exploitable détectée"
        }
        return ScreenshotAnalysis(
            id = UUID.randomUUID().toString(),
            analyzedAtIso = timestamp,
            imageUriString = retainedUri,
            typeCapture = "lecture_a_verifier",
            resumeCapture = "$reason. L'analyse en ligne n'a pas abouti. " +
                if (retainedUri.isNotBlank()) "Image conservée localement pour vérification." else "La copie locale n'a pas pu être conservée.",
            champsIncertains = localExtraction.uncertainItems + UncertainItem(
                "Analyse de capture",
                "Lecture incomplète",
                "$reason ; le service en ligne est également indisponible."
            )
        ).also { onProgress(100) }
    }

    fun enqueueUrisSequentially(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val baseIndex = analysisQueue.size
        val added = uris.mapIndexed { idx, uri ->
            AndroidQueuedUri(
                id = "${System.currentTimeMillis()}-${baseIndex + idx}",
                uri = uri,
                label = uri.lastPathSegment ?: "Capture #${baseIndex + idx + 1}",
                progress = 0,
                status = "waiting"
            )
        }
        analysisQueue = analysisQueue + added
        if (isQueueRunning) return
        isQueueRunning = true
    }

    LaunchedEffect(isQueueRunning) {
        if (!isQueueRunning) return@LaunchedEffect
        isAnalyzing = true
        errorMessage = null
        try {
            while (true) {
                val nextItem = analysisQueue.firstOrNull { it.status == "waiting" } ?: break
                analysisQueue = analysisQueue.map {
                    if (it.id == nextItem.id) it.copy(status = "analyzing", progress = 15) else it
                }
                val result = runCatching {
                    analyzeSingleUriInternal(nextItem.uri) { pct ->
                        analysisQueue = analysisQueue.map {
                            if (it.id == nextItem.id) it.copy(progress = pct) else it
                        }
                    }
                }
                result.onSuccess { analysis ->
                    mergeNewAnalysis(analysis)
                    analysisQueue = analysisQueue.map {
                        if (it.id == nextItem.id) {
                            it.copy(status = "done", progress = 100, summary = analysis.resumeCapture)
                        } else it
                    }
                }.onFailure { err ->
                    analysisQueue = analysisQueue.map {
                        if (it.id == nextItem.id) {
                            it.copy(status = "error", progress = 0, summary = err.message ?: "Erreur")
                        } else it
                    }
                }
            }
            val doneCount = analysisQueue.count { it.status == "done" }
            if (doneCount > 0) {
                currentTab = AssistantTab.TABLEAU
                snackbarHostState.showSnackbar(localizedUiText(currentLang, "File terminée (100 %) : $doneCount capture(s) analysée(s)", "Queue complete (100%): $doneCount capture(s) analyzed", "Warteschlange abgeschlossen (100 %): $doneCount Aufnahme(n) analysiert", "Kuyruk tamamlandı (%100): $doneCount ekran görüntüsü analiz edildi", "Очередь завершена (100 %): проанализировано снимков: $doneCount", "队列已完成（100%）：已分析 $doneCount 张截图"))
            }
        } finally {
            isQueueRunning = false
            isAnalyzing = false
        }
    }

    DisposableEffect(capturedScreenPayload, lifecycleOwner) {
        val payload = capturedScreenPayload
        if (payload != null) {
            // L'analyse appartient au cycle de vie de l'activité, pas à la composition Compose.
            lifecycleOwner.lifecycleScope.launch {
            // Ne supprimer la capture persistée qu'après l'enregistrement du résultat.
            // Une annulation liée à la composition doit laisser la capture récupérable.
            var analysisCommitted = false
            isAnalyzing = true
            singleAnalysisPercent = 20
            errorMessage = null
            try {
                val ocrResult = runCatching {
                    LandsOfJailOcrParser.recognizeBitmapDetailed(payload.bitmap)
                }.getOrNull()
                val ocrRaw = ocrResult?.text.orEmpty()
                singleAnalysisPercent = 50
                val localExtraction = withContext(Dispatchers.Default) {
                    LandsOfJailOcrParser.parseLocalOcrText(
                        ocrRaw,
                        ocrResult?.boxes.orEmpty(),
                        ocrResult?.imageWidth ?: 0,
                        ocrResult?.imageHeight ?: 0
                    )
                }
                val localImageUri = payload.imageFileUri

                val result = runCatching {
                    repository.analyzeBase64Screenshot(
                        imageBase64 = payload.base64Jpeg,
                        mimeType = "image/jpeg",
                        imageLabel = localImageUri.ifBlank { "capture-ecran-autorisee.jpg" },
                        playerRole = playerRole,
                        note = "OCR local : ${ocrRaw.take(400)}"
                    ).let { remote ->
                        remote.copy(
                            puissance = remote.puissance?.takeIf { it.puissanceTotaleTexte.isNotBlank() }
                                ?: localExtraction.power,
                            ressources = mergeResourceRecords(remote.ressources, localExtraction.resources),
                            heros = mergeHeroRecords(remote.heros, localExtraction.heroes),
                            robots = mergeRobotRecords(remote.robots, localExtraction.robots),
                            equipements = (remote.equipements + localExtraction.equipment).distinctBy {
                                (it.piece + it.niveau).lowercase()
                            },
                            champsIncertains = (remote.champsIncertains + localExtraction.uncertainItems)
                                .distinctBy { normalizedKey(it.champ) + normalizedKey(it.valeurObservee) }
                        )
                    }
                }.getOrElse {
                    val foundParts = buildList {
                        localExtraction.power?.let { add("Puissance : ${it.puissanceTotaleTexte}") }
                        if (localExtraction.heroes.isNotEmpty()) {
                            add("${localExtraction.heroes.size} héros (${localExtraction.heroes.take(3).joinToString { it.nom }})")
                        }
                        if (localExtraction.resources.isNotEmpty()) {
                            add("${localExtraction.resources.size} ressource(s)")
                        }
                        if (localExtraction.robots.isNotEmpty()) {
                            add("${localExtraction.robots.size} robot(s)")
                        }
                        if (localExtraction.equipment.isNotEmpty()) {
                            add("${localExtraction.equipment.size} pièce(s) d'équipement")
                        }
                    }
                    val summaryLine = if (foundParts.isNotEmpty()) {
                        "${foundParts.joinToString(" · ")} — ${localExtraction.ocrSnippet}"
                    } else {
                        "Capture enregistrée · OCR : ${localExtraction.ocrSnippet}"
                    }
                    ScreenshotAnalysis(
                        id = UUID.randomUUID().toString(),
                        analyzedAtIso = Instant.now().toString(),
                        imageUriString = localImageUri,
                        typeCapture = localExtraction.detectedCategory,
                        resumeCapture = summaryLine,
                        puissance = localExtraction.power,
                        ressources = localExtraction.resources,
                        heros = localExtraction.heroes,
                        robots = localExtraction.robots,
                        equipements = localExtraction.equipment,
                        champsIncertains = localExtraction.uncertainItems
                    )
                }

                singleAnalysisPercent = 100
                mergeNewAnalysis(result)
                analysisCommitted = true
                AdviceMode.entries.firstOrNull { it.apiId == result.typeCapture }?.let {
                    selectedAdviceMode = it
                }

                val autoModeStillActive = GameForegroundMonitorService.isAutoCapturing.value
                if (!autoModeStillActive) {
                    currentTab = AssistantTab.TABLEAU
                    snackbarHostState.showSnackbar(
                        localizedUiText(currentLang, "Capture analysée 100 % (${result.heros.size} héros, ${result.robots.size} robot(s), ${result.ressources.size} ressource(s))", "Capture analyzed 100% (${result.heros.size} heroes, ${result.robots.size} robot(s), ${result.ressources.size} resource(s))", "Aufnahme zu 100 % analysiert (${result.heros.size} Helden, ${result.robots.size} Roboter, ${result.ressources.size} Ressourcen)", "Ekran görüntüsü %100 analiz edildi (${result.heros.size} kahraman, ${result.robots.size} robot, ${result.ressources.size} kaynak)", "Снимок проанализирован на 100 % (${result.heros.size} героев, ${result.robots.size} роботов, ${result.ressources.size} ресурсов)", "截图已完成 100% 分析（${result.heros.size} 位英雄、${result.robots.size} 个机器人、${result.ressources.size} 项资源）")
                    )
                }

                isFetchingAdvice = true
                repository.fetchTacticalAdvice(
                    mode = selectedAdviceMode,
                    language = currentLang,
                    playerRole = playerRole,
                    heroes = heroes,
                    powerText = currentPower?.puissanceTotaleTexte,
                    robots = robots
                ).onSuccess { adviceResult = it }
                isFetchingAdvice = false
            } catch (e: CancellationException) {
                // La capture reste dans le stockage local : elle sera restaurée à la reprise.
                throw e
            } catch (e: Throwable) {
                errorMessage = e.message ?: localizedUiText(currentLang, "Erreur pendant l'analyse OCR.", "Error during OCR analysis.", "Fehler bei der OCR-Analyse.", "OCR analizi sırasında hata oluştu.", "Ошибка при распознавании текста.", "OCR 分析时出错。")
            } finally {
                if (analysisCommitted) onConsumeCapturedScreen()
                isAnalyzing = false
                singleAnalysisPercent = 0
            }
            }
        }
        onDispose {
            // Ne pas annuler l'analyse lorsque l'interface se recompose ou passe en arrière-plan.
        }
    }

    LaunchedEffect(sharedScreenshotUri) {
        if (sharedScreenshotUri != null) {
            enqueueUrisSequentially(listOf(sharedScreenshotUri))
            onConsumeSharedUri()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) enqueueUrisSequentially(uris)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFF26B32),
            onPrimary = Color(0xFF030405),
            secondary = Color(0xFFFF9A55),
            background = Color(0xFF080A0F),
            surface = Color(0xFF10141C),
            surfaceVariant = Color(0xFF171D28),
            onBackground = Color(0xFFF4F0E8),
            onSurface = Color(0xFFF4F0E8),
            onSurfaceVariant = Color(0xFFA9ADB5),
            outline = Color(0xFF2D3745)
        ),
        shapes = Shapes(
            small = RoundedCornerShape(4.dp),
            medium = RoundedCornerShape(4.dp),
            large = RoundedCornerShape(4.dp)
        )
    ) {
        CompositionLocalProvider(LocalAssistantLanguage provides currentLang) {
        if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text(localizedUiText(currentLang, "Profils de jeu", "Game profiles", "Spielprofile", "Oyun profilleri", "Игровые профили", "游戏档案")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(localizedUiText(currentLang, "Chaque profil conserve séparément ses héros, robots, ressources, puissance, événements, minuteries et historique. Renomme-les selon le personnage ou le compte pour les retrouver facilement.", "Each profile keeps its heroes, robots, resources, power, events, timers and history separate. Rename profiles by character or account to find them easily.", "Jedes Profil speichert Helden, Roboter, Ressourcen, Stärke, Ereignisse, Timer und Verlauf getrennt. Benenne Profile nach Charakter oder Konto, damit du sie leichter findest.", "Her profil kahramanları, robotları, kaynakları, gücü, etkinlikleri, zamanlayıcıları ve geçmişi ayrı tutar. Kolay bulmak için profilleri karaktere veya hesaba göre yeniden adlandır.", "В каждом профиле отдельно хранятся герои, роботы, ресурсы, сила, события, таймеры и история. Переименуйте профиль по персонажу или аккаунту, чтобы его было легче найти.", "每个档案分别保存英雄、机器人、资源、战力、活动、计时器和历史记录。可按角色或账号重命名，方便查找。"))
                    localProfiles.forEach { profile ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                profile.name,
                                fontWeight = if (profile.id == activeProfileId) FontWeight.Bold else FontWeight.Normal
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(enabled = !isAnalyzing && !isAutoCapturing, onClick = {
                                    if (profile.id != activeProfileId) {
                                        saveActiveProfile()
                                        runCatching {
                                            val snapshot = accountJson.decodeFromString<LocalGameProfileSnapshot>(profile.stateJson)
                                            allianceWorkspace = snapshot.allianceWorkspace
                                            analyses = snapshot.analyses
                                            heroes = snapshot.heroes
                                            resources = snapshot.resources
                                             robots = snapshot.robots
                                             eventRecords = snapshot.eventRecords
                                            currentPower = snapshot.currentPower
                                            selectedAdviceMode = runCatching { AdviceMode.valueOf(snapshot.adviceMode) }.getOrDefault(AdviceMode.TRIAL_CAGE)
                                            adviceResult = snapshot.adviceResult
                                            playerRole = runCatching { PlayerRole.valueOf(snapshot.playerRole) }.getOrDefault(PlayerRole.POLYVALENT_F2P)
                                            restoreRoutineState(profile.id, snapshot.routineState, clearWhenEmpty = snapshot.schemaVersion != null && snapshot.schemaVersion >= 2)
                                            routineRevision += 1
                                            activeProfileId = profile.id
                                            accountPrefs.edit().putString("active_game_profile_v1", profile.id).commit()
                                        }.onFailure { errorMessage = localizedUiText(currentLang, "Impossible d'ouvrir ce profil : ${it.message}", "Could not open this profile: ${it.message}", "Dieses Profil konnte nicht geöffnet werden: ${it.message}", "Bu profil açılamadı: ${it.message}", "Не удалось открыть этот профиль: ${it.message}", "无法打开此档案：${it.message}") }
                                    }
                                    showProfileDialog = false
                                }) { Text(if (profile.id == activeProfileId) localizedUiText(currentLang, "Actif", "Active", "Aktiv", "Etkin", "Активен", "当前") else localizedUiText(currentLang, "Ouvrir", "Open", "Öffnen", "Aç", "Открыть", "打开")) }
                                TextButton(enabled = !isAnalyzing && !isAutoCapturing, onClick = {
                                    editingProfileId = profile.id
                                    profileNameInput = profile.name
                                }) { Text(localizedUiText(currentLang, "Renommer", "Rename", "Umbenennen", "Yeniden adlandır", "Переименовать", "重命名")) }
                                TextButton(
                                    enabled = localProfiles.size > 1 && !isAnalyzing && !isAutoCapturing,
                                    onClick = { deletingProfileId = profile.id }
                                ) { Text(localizedUiText(currentLang, "Supprimer", "Delete", "Löschen", "Sil", "Удалить", "删除")) }
                            }
                        }
                    }
                    TextButton(
                        enabled = !isAnalyzing && !isAutoCapturing,
                        onClick = {
                            saveActiveProfile()
                            val id = UUID.randomUUID().toString()
                            val number = generateSequence(1) { it + 1 }.first { candidate ->
                                localProfiles.none { profile -> profile.name.equals("Profil $candidate", ignoreCase = true) }
                            }
                            val fresh = LocalGameAccountProfile(
                                id = id,
                                name = "Profil $number",
                                stateJson = accountJson.encodeToString(LocalGameProfileSnapshot(schemaVersion = 2)),
                                updatedAtIso = Instant.now().toString()
                            )
                            localProfiles = localProfiles + fresh
                            activeProfileId = id
                            allianceWorkspace = AllianceWorkspace()
                            analyses = emptyList()
                            heroes = emptyList()
                            resources = emptyList()
                            robots = emptyList()
                            eventRecords = emptyList()
                            currentPower = null
                            selectedAdviceMode = AdviceMode.TRIAL_CAGE
                            adviceResult = null
                            playerRole = PlayerRole.POLYVALENT_F2P
                            accountPrefs.edit().putString("active_game_profile_v1", id).commit()
                            showProfileDialog = false
                        }
                    ) { Text(localizedAppText(localizedAppText("+ ")) + localizedUiText(currentLang, "Ajouter un profil", "Add a profile", "Profil hinzufügen", "Profil ekle", "Добавить профиль", "添加档案")) }
                    TextButton(
                        enabled = !isProfileSyncing && !isAnalyzing && !isAutoCapturing,
                        onClick = {
                            if (profileAuth.currentSession() == null) {
                                errorMessage = localizedUiText(currentLang, "Connecte-toi à ton compte dans l’onglet Compte avant la synchronisation.", "Sign in to your account in the Account tab before syncing.", "Melde dich vor der Synchronisierung im Konto-Tab an.", "Eşitlemeden önce Hesap sekmesinden hesabına giriş yap.", "Перед синхронизацией войдите в аккаунт на вкладке «Аккаунт».", "同步前，请先在“账户”选项卡登录。")
                            } else {
                                scope.launch {
                                    isProfileSyncing = true
                                    try {
                                        val localWithRoutine = localProfiles.map { profile ->
                                            val snapshot = if (profile.id == activeProfileId) {
                                                currentProfileSnapshot()
                                            } else {
                                                runCatching {
                                                    accountJson.decodeFromString<LocalGameProfileSnapshot>(profile.stateJson)
                                                }.getOrDefault(LocalGameProfileSnapshot())
                                            }
                                            val routineState = readRoutineState(profile.id)
                                            val profileTime = runCatching { Instant.parse(profile.updatedAtIso) }.getOrNull()
                                            val routineTime = routineState["updated_at"]?.substringAfter("|")
                                                ?.let { runCatching { Instant.parse(it) }.getOrNull() }
                                            profile.copy(
                                                stateJson = accountJson.encodeToString(
                                                    snapshot.copy(routineState = routineState)
                                                ),
                                                updatedAtIso = if (profile.id == activeProfileId) Instant.now().toString()
                                                    else listOfNotNull(profileTime, routineTime).maxOrNull()?.toString()
                                                        ?: profile.updatedAtIso
                                            )
                                        }
                                        // Keep a recoverable local copy, including each profile's current routine timers.
                                        accountPrefs.edit()
                                            .putString("local_game_profiles_backup_v1", accountJson.encodeToString(localWithRoutine))
                                            .commit()
                                        val remote = profileAuth.fetchGameAccountProfiles().getOrThrow()
                                        val remoteById = remote.associateBy { it.profileId }
                                        val merged = localWithRoutine.toMutableList()
                                        remote.forEach { cloud ->
                                            val index = merged.indexOfFirst { it.id == cloud.profileId }
                                            if (index < 0) {
                                                merged.add(
                                                    LocalGameAccountProfile(
                                                        id = cloud.profileId,
                                                        name = cloud.profileName.ifBlank { "Profil synchronisé" },
                                                        stateJson = cloud.stateJson,
                                                        updatedAtIso = cloud.updatedAt
                                                    )
                                                )
                                            } else {
                                                val local = merged[index]
                                                val localTime = runCatching { Instant.parse(local.updatedAtIso) }.getOrNull()
                                                val cloudTime = runCatching { Instant.parse(cloud.updatedAt) }.getOrNull()
                                                if (cloudTime != null && (localTime == null || cloudTime.isAfter(localTime))) {
                                                    merged[index] = local.copy(
                                                        name = cloud.profileName.ifBlank { local.name },
                                                        stateJson = cloud.stateJson,
                                                        updatedAtIso = cloud.updatedAt
                                                    )
                                                }
                                            }
                                        }
                                        merged.forEach { local ->
                                            val cloud = remoteById[local.id]
                                            val localTime = runCatching { Instant.parse(local.updatedAtIso) }.getOrNull()
                                            val cloudTime = cloud?.let { runCatching { Instant.parse(it.updatedAt) }.getOrNull() }
                                            if (cloud == null || (localTime != null && (cloudTime == null || !cloudTime.isAfter(localTime)))) {
                                                profileAuth.syncGameAccountProfile(local.id, local.name, local.stateJson).getOrThrow()
                                            }
                                        }
                                        localProfiles = merged
                                        accountPrefs.edit()
                                            .putString("local_game_profiles_v1", accountJson.encodeToString(merged))
                                            .commit()
                                        merged.firstOrNull { it.id == activeProfileId }?.let { active ->
                                            val snapshot = accountJson.decodeFromString<LocalGameProfileSnapshot>(active.stateJson)
                                            allianceWorkspace = snapshot.allianceWorkspace
                                            analyses = snapshot.analyses
                                            heroes = snapshot.heroes
                                            resources = snapshot.resources
                                             robots = snapshot.robots
                                             eventRecords = snapshot.eventRecords
                                            currentPower = snapshot.currentPower
                                            selectedAdviceMode = runCatching { AdviceMode.valueOf(snapshot.adviceMode) }.getOrDefault(AdviceMode.TRIAL_CAGE)
                                            adviceResult = snapshot.adviceResult
                                            playerRole = runCatching { PlayerRole.valueOf(snapshot.playerRole) }.getOrDefault(PlayerRole.POLYVALENT_F2P)
                                            restoreRoutineState(active.id, snapshot.routineState, clearWhenEmpty = snapshot.schemaVersion != null && snapshot.schemaVersion >= 2)
                                            routineRevision += 1
                                        }
                                        snackbarHostState.showSnackbar(localizedUiText(currentLang, "Synchronisation terminée. Aucune sauvegarde distante n’a été supprimée.", "Sync complete. No remote backup was deleted.", "Synchronisierung abgeschlossen. Kein Remote-Backup wurde gelöscht.", "Senkronizasyon tamamlandı. Hiçbir uzak yedek silinmedi.", "Синхронизация завершена. Ни одна удалённая резервная копия не удалена.", "同步完成。没有删除任何远程备份。"))
                                    } catch (error: Exception) {
                                        errorMessage = localizedUiText(currentLang, "Synchronisation interrompue : ${error.message ?: "erreur inconnue"}. La copie locale de secours est conservée.", "Sync interrupted: ${error.message ?: "unknown error"}. The local backup is preserved.", "Synchronisierung unterbrochen: ${error.message ?: "unbekannter Fehler"}. Die lokale Sicherung bleibt erhalten.", "Eşitleme kesildi: ${error.message ?: "bilinmeyen hata"}. Yerel yedek korundu.", "Синхронизация прервана: ${error.message ?: "неизвестная ошибка"}. Локальная резервная копия сохранена.", "同步中断：${error.message ?: "未知错误"}。本地备份已保留。")
                                    } finally {
                                        isProfileSyncing = false
                                    }
                                }
                            }
                        }
                    ) { Text(if (isProfileSyncing) localizedAppText(localizedAppText("Synchronisation…")) else localizedAppText(localizedAppText("Synchroniser avec le cloud"))) }
                }
            },
            confirmButton = { TextButton(onClick = { showProfileDialog = false }) { Text(localizedUiText(currentLang, "Fermer", "Close", "Schließen", "Kapat", "Закрыть", "关闭")) } }
        )
    }
    if (editingProfileId != null) {
        AlertDialog(
            onDismissRequest = { editingProfileId = null },
            title = { Text(localizedUiText(currentLang, "Renommer le profil", "Rename profile", "Profil umbenennen", "Profili yeniden adlandır", "Переименовать профиль", "重命名档案")) },
            text = {
                OutlinedTextField(
                    value = profileNameInput,
                    onValueChange = { profileNameInput = it.take(32) },
                    label = { Text(localizedUiText(currentLang, "Nom du profil", "Profile name", "Profilname", "Profil adı", "Название профиля", "档案名称")) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = profileNameInput.trim().take(32)
                    if (name.isNotEmpty()) {
                        localProfiles = localProfiles.map { profile ->
                            if (profile.id == editingProfileId) profile.copy(name = name, updatedAtIso = Instant.now().toString()) else profile
                        }
                    }
                    editingProfileId = null
                }, enabled = profileNameInput.trim().isNotEmpty()) { Text(localizedUiText(currentLang, "Enregistrer", "Save", "Speichern", "Kaydet", "Сохранить", "保存")) }
            },
            dismissButton = { TextButton(onClick = { editingProfileId = null }) { Text(localizedUiText(currentLang, "Annuler", "Cancel", "Abbrechen", "İptal", "Отмена", "取消")) } }
        )
    }
    if (deletingProfileId != null) {
        val target = localProfiles.firstOrNull { it.id == deletingProfileId }
        AlertDialog(
            onDismissRequest = { deletingProfileId = null },
            title = { Text(localizedUiText(currentLang, "Supprimer ce profil ?", "Delete this profile?", "Dieses Profil löschen?", "Bu profil silinsin mi?", "Удалить этот профиль?", "删除此档案？")) },
            text = { Text(localizedUiText(currentLang, "Les données locales de « ${target?.name.orEmpty()} » seront supprimées de cet appareil. Cette action ne supprime aucune sauvegarde distante.", "The local data for “${target?.name.orEmpty()}” will be deleted from this device. This does not delete any cloud backup.", "Die lokalen Daten von „${target?.name.orEmpty()}“ werden von diesem Gerät gelöscht. Cloud-Sicherungen bleiben erhalten.", "“${target?.name.orEmpty()}” profilinin yerel verileri bu cihazdan silinecek. Bulut yedekleri silinmez.", "Локальные данные профиля «${target?.name.orEmpty()}» будут удалены с этого устройства. Облачные копии не удаляются.", "“${target?.name.orEmpty()}”的本地数据将从此设备删除。此操作不会删除云端备份。")) },
            confirmButton = {
                TextButton(onClick = {
                    if (target != null && localProfiles.size > 1) {
                        if (target.id == activeProfileId) {
                            val next = localProfiles.first { it.id != target.id }
                            runCatching {
                                val snapshot = accountJson.decodeFromString<LocalGameProfileSnapshot>(next.stateJson)
                                allianceWorkspace = snapshot.allianceWorkspace
                                analyses = snapshot.analyses
                                heroes = snapshot.heroes
                                resources = snapshot.resources
                                             robots = snapshot.robots
                                             eventRecords = snapshot.eventRecords
                                currentPower = snapshot.currentPower
                                selectedAdviceMode = runCatching { AdviceMode.valueOf(snapshot.adviceMode) }.getOrDefault(AdviceMode.TRIAL_CAGE)
                                adviceResult = snapshot.adviceResult
                                playerRole = runCatching { PlayerRole.valueOf(snapshot.playerRole) }.getOrDefault(PlayerRole.POLYVALENT_F2P)
                                restoreRoutineState(next.id, snapshot.routineState, clearWhenEmpty = snapshot.schemaVersion != null && snapshot.schemaVersion >= 2)
                                routineRevision += 1
                                activeProfileId = next.id
                                accountPrefs.edit().putString("active_game_profile_v1", next.id).commit()
                                localProfiles = localProfiles.filterNot { it.id == target.id }
                            }.onFailure { errorMessage = localizedUiText(currentLang, "Suppression impossible : ${it.message}", "Could not delete: ${it.message}", "Löschen nicht möglich: ${it.message}", "Silinemedi: ${it.message}", "Не удалось удалить: ${it.message}", "无法删除：${it.message}") }
                        } else {
                            localProfiles = localProfiles.filterNot { it.id == target.id }
                        }
                    }
                    deletingProfileId = null
                }) { Text(localizedUiText(currentLang, "Supprimer", "Delete", "Löschen", "Sil", "Удалить", "删除")) }
            },
            dismissButton = { TextButton(onClick = { deletingProfileId = null }) { Text(localizedAppText(localizedAppText("Annuler"))) } }
        )
    }

    Scaffold(
            containerColor = Color(0xFF080A0F),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0B0D13),
                        titleContentColor = Color(0xFFF4F0E8),
                        actionIconContentColor = Color(0xFFFF9A55)
                    ),
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                localizedAppText("LANDS OF JAIL"),
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.1.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                localizedAppText("ASSISTANT TACTIQUE"),
                                color = Color(0xFFFF9A55),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            )
                        }
                    },
                    actions = {
                        TextButton(enabled = !isAnalyzing && !isAutoCapturing, onClick = { showProfileDialog = true }) {
                            Text(
                                localProfiles.firstOrNull { it.id == activeProfileId }?.name ?: "Profil",
                                maxLines = 1,
                                fontSize = 11.sp,
                                color = Color(0xFFFFC18A)
                            )
                        }
                        FilledTonalButton(
                            onClick = {
                                context.startActivity(
                                    Intent(context, ScreenCaptureConsentActivity::class.java).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                )
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color(0xFF5B2D1D),
                                contentColor = Color(0xFFFF9A55)
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(Icons.Outlined.CenterFocusStrong, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                when (currentLang) {
                                    AppLanguage.FR -> "Analyser"
                                    AppLanguage.TR -> "Tara"
                                    AppLanguage.RU -> "Сканировать"
                                    AppLanguage.ZH -> "扫描"
                                    AppLanguage.DE -> "Scannen"
                                    AppLanguage.EN -> "Scan"
                                    AppLanguage.IT -> "Scan"
                                },
                                maxLines = 1,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF0B0D13),
                    contentColor = Color(0xFF9AA8BA),
                    tonalElevation = 0.dp
                ) {
                    AssistantTab.entries.forEach { tab ->
                        val icon = when (tab) {
                            AssistantTab.TABLEAU -> Icons.Outlined.Dashboard
                            AssistantTab.HEROS -> Icons.Outlined.Groups
                            AssistantTab.RESSOURCES -> Icons.Outlined.Inventory2
                            AssistantTab.CONSEILS -> Icons.Outlined.MilitaryTech
                            AssistantTab.ALLIANCE -> Icons.Outlined.Groups
                            AssistantTab.HISTORIQUE -> Icons.Outlined.History
                            AssistantTab.AMELIORATIONS -> Icons.Outlined.Lightbulb
                            AssistantTab.CODES -> Icons.Outlined.CardGiftcard
                        }
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFFFF9A55),
                                selectedTextColor = Color(0xFFFF9A55),
                                indicatorColor = Color(0xFF5B2D1D),
                                unselectedIconColor = Color(0xFF6E7785),
                                unselectedTextColor = Color(0xFF6E7785)
                            ),
                            icon = { Icon(icon, contentDescription = tab.titleFor(currentLang)) },
                            label = {
                                Text(
                                    text = tab.titleFor(currentLang),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 10.sp,
                                    fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (analysisQueue.isNotEmpty()) {
                    val total = analysisQueue.size
                    val done = analysisQueue.count { it.status == "done" }
                    val waiting = analysisQueue.count { it.status == "waiting" }
                    val pct = (analysisQueue.sumOf { if (it.status == "done") 100 else it.progress } / total).coerceIn(0, 100)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2E)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (isQueueRunning) "File : $done/$total ($waiting en attente)"
                                    else "File terminée : $done/$total",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (pct == 100) Color(0xFF34D399) else Color(0xFFF59E0B)
                                )
                                Text(
                                    localizedAppText("$pct %"),
                                    fontWeight = FontWeight.Bold,
                                    color = if (pct == 100) Color(0xFF34D399) else Color(0xFFF59E0B)
                                )
                            }
                            LinearProgressIndicator(
                                progress = { pct / 100f },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (!isQueueRunning) {
                                TextButton(onClick = { analysisQueue = emptyList() }) {
                                    Text(localizedAppText(localizedAppText("Fermer la file (100 %)")))
                                }
                            }
                        }
                    }
                } else if (isAnalyzing) {
                    val pct = singleAnalysisPercent.coerceIn(0, 100)
                    LinearProgressIndicator(
                        progress = { pct / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when (currentLang) {
                            AppLanguage.FR -> "Analyse OCR en cours : $pct %"
                            AppLanguage.TR -> "OCR ekran analizi: %$pct"
                            AppLanguage.RU -> "OCR-анализ: $pct %"
                            AppLanguage.ZH -> "OCR 截图分析进度：$pct%"
                            AppLanguage.DE -> "OCR-Analyse läuft: $pct %"
                            AppLanguage.EN -> "OCR screen analysis: $pct%"
                            AppLanguage.IT -> "OCR screen analysis: $pct%"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                errorMessage?.let { msg ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF451A1A)),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text(msg, modifier = Modifier.padding(12.dp), color = Color(0xFFFCA5A5))
                    }
                }

                when (currentTab) {
                    AssistantTab.TABLEAU -> DashboardScreen(
                        currentLang = currentLang,
                        hasUsagePermission = hasUsagePermission,
                        hasOverlayPermission = hasOverlayPermission,
                        alwaysKeepOverlay = alwaysKeepOverlay,
                        onToggleAlwaysKeepOverlay = { alwaysKeepOverlay = it },
                        isMonitoringRunning = isMonitoringRunning,
                        isGameForeground = isGameForeground,
                        isAutoCapturing = isAutoCapturing,
                        isAnalyzing = isAnalyzing,
                        isProfileSyncing = isProfileSyncing,
                        autoCaptureIntervalSeconds = autoCaptureIntervalSeconds,
                        onChangeAutoCaptureInterval = { autoCaptureIntervalSeconds = it },
                        onStartAutoCapture = {
                            context.startActivity(
                                Intent(context, ScreenCaptureConsentActivity::class.java).apply {
                                    putExtra(ScreenCaptureConsentActivity.EXTRA_AUTO_CAPTURE, true)
                                    putExtra(ScreenCaptureConsentActivity.EXTRA_INTERVAL_MILLIS, autoCaptureIntervalSeconds * 1000L)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        },
                        onStopAutoCapture = { GameForegroundMonitorService.stopAutoCapture(context) },
                        packageFilter = packageFilter,
                        onPackageFilterChange = { packageFilter = it },
                        onOpenUsageSettings = { UsageStatsDetector.openUsageAccessSettings(context) },
                        onOpenOverlaySettings = { UsageStatsDetector.openOverlaySettings(context) },
                        onStartMonitoring = {
                            GameForegroundMonitorService.start(
                                context = context,
                                packageFilter = packageFilter,
                                alwaysShowFloating = alwaysKeepOverlay
                            )
                        },
                        onStopMonitoring = {
                            GameForegroundMonitorService.stop(context)
                        },
                        onAnalyzeScreenNow = {
                            context.startActivity(
                                Intent(context, ScreenCaptureConsentActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        },
                        playerRole = playerRole,
                        onSelectRole = { playerRole = it },
                        currentPower = currentPower,
                        heroesCount = heroes.size,
                        resourcesCount = resources.size,
                        heroes = heroes,
                        resources = resources,
                        robots = robots,
                        gameIntel = gameIntel,
                        gameIntelLoading = gameIntelLoading,
                        gameIntelError = gameIntelError,
                        onPickImage = { imagePicker.launch("image/*") }
                    )
                    AssistantTab.HEROS -> HeroesScreen(
                        language = currentLang,
                        heroes = heroes,
                        onAddHero = { entry ->
                            val newHero = HeroRecord(
                                nom = entry.nom,
                                niveauTexte = "",
                                rarete = entry.rarete,
                                typeTroupe = entry.typeTroupe,
                                saison = entry.saison,
                                champsIncertains = listOf("niveau", "etoiles", "puissanceHero"),
                                incertain = true,
                                raisonIncertitude = "Niveau, étoiles et puissance à renseigner depuis le jeu."
                            )
                            heroes = heroes + newHero
                            newHero
                        },
                        onUpdateHero = { original, updated ->
                            heroes = heroes.map { if (it.nom == original.nom) updated else it }
                        }
                    )
                    AssistantTab.RESSOURCES -> ResourcesScreen(
                        currentLang, currentPower, resources, activeProfileId,
                        robots = robots,
                        eventRecords = eventRecords,
                        onSaveRobots = { robots = it },
                        onSaveEventRecords = { eventRecords = it },
                        migrateLegacyRoutine = localProfiles.firstOrNull()?.id == activeProfileId,
                        routineRevision = routineRevision
                    )
                    AssistantTab.CONSEILS -> AdviceScreen(
                        heroes = heroes,
                        robots = robots,
                        currentLang = currentLang,
                        selectedMode = selectedAdviceMode,
                        onSelectMode = {
                            selectedAdviceMode = it
                            adviceResult = null
                        },
                        playerRole = playerRole,
                        advice = adviceResult,
                        isLoading = isFetchingAdvice,
                        onGenerateAdvice = {
                            scope.launch {
                                isFetchingAdvice = true
                                errorMessage = null
                                repository.fetchTacticalAdvice(
                                    mode = selectedAdviceMode,
                                    language = currentLang,
                                    playerRole = playerRole,
                                    heroes = heroes,
                                    powerText = currentPower?.puissanceTotaleTexte,
                                    robots = robots
                                ).onSuccess { adviceResult = it }
                                    .onFailure { errorMessage = it.message }
                                isFetchingAdvice = false
                            }
                        }
                    )
                    AssistantTab.ALLIANCE -> AllianceScreen(
                        language = currentLang,
                        onChangeLanguage = { currentLang = it },
                        workspace = allianceWorkspace,
                        heroes = heroes,
                        robots = robots,
                        currentPowerText = currentPower?.puissanceTotaleTexte.orEmpty(),
                        onSaveWorkspace = { allianceWorkspace = it }
                    )
                    AssistantTab.AMELIORATIONS -> ImprovementRequestsScreen(auth = profileAuth)
                    AssistantTab.CODES -> GiftCodesScreen(
                        language = currentLang,
                        snapshot = giftCodeSnapshot,
                        refreshing = giftCodesRefreshing,
                        onRefresh = {
                            scope.launch {
                                if (!giftCodesRefreshing) {
                                    giftCodesRefreshing = true
                                    val refreshed = withContext(Dispatchers.IO) { GiftCodeRepository.refresh(giftCodeSnapshot) }
                                    giftCodeSnapshot = refreshed
                                    giftCodePrefs.edit().putString("snapshot", giftCodeJson.encodeToString(refreshed)).apply()
                                    giftCodesRefreshing = false
                                }
                            }
                        }
                    )
                    AssistantTab.HISTORIQUE -> HistoryScreen(analyses, onClearHistory = {
                            analyses.forEach { analysis ->
                                if (analysis.imageUriString.startsWith("file:")) {
                                    runCatching { File(Uri.parse(analysis.imageUriString).path.orEmpty()).delete() }
                                }
                            }
                            File(context.filesDir, "import_review").listFiles()?.forEach { it.delete() }
                            analyses = emptyList()
                        })
                }
            }

        }
    }

    if (showAllianceOnboarding) {
        AlertDialog(
            onDismissRequest = {
                showAllianceOnboarding = false
                accountPrefs.edit().putBoolean("alliance_onboarding_completed_v1", true).apply()
            },
            title = { Text(localizedAppText(localizedAppText("Bienvenue · Inscription joueur"))) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        localizedAppText("Renseigne ton identité dans Land of Jail. Ces informations seront enregistrées sur cet appareil et pourront être transmises aux officiers de ton alliance après connexion."),
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = onboardingPlayerName,
                        onValueChange = { onboardingPlayerName = it.take(40) },
                        label = { Text(localizedAppText(localizedAppText("Pseudo dans le jeu"))) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = onboardingServer,
                        onValueChange = { onboardingServer = it.filter(Char::isDigit).take(6) },
                        label = { Text(localizedAppText(localizedAppText("Serveur (ex. 311)"))) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = onboardingAllianceTag,
                        onValueChange = { onboardingAllianceTag = it.take(12) },
                        label = { Text(localizedAppText(localizedAppText("Initiales / tag de l'alliance"))) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(localizedAppText(localizedAppText("Ton rang déclaré")), style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        items(AllianceRank.entries) { item ->
                            FilterChip(
                                selected = onboardingRank == item,
                                onClick = { onboardingRank = item },
                                label = { Text(item.name) }
                            )
                        }
                    }
                    Text(
                        localizedAppText("Si tu es le R5 qui crée l'espace cloud de l'alliance, tu deviens directement R5 sans validation. Pour une alliance déjà créée, rejoins-la d'abord comme membre puis demande au R5 actuel de te transférer le poste."),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFBBF24)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val player = AlliancePlayerProfile(
                            id = allianceWorkspace.ownProfile.id.ifBlank { UUID.randomUUID().toString() },
                            playerName = onboardingPlayerName.trim(),
                            server = onboardingServer.trim(),
                            allianceName = onboardingAllianceTag.trim(),
                            rank = onboardingRank,
                            powerText = allianceWorkspace.ownProfile.powerText,
                            heroSkillSummary = allianceWorkspace.ownProfile.heroSkillSummary,
                            robotSummary = allianceWorkspace.ownProfile.robotSummary,
                            updatedAtIso = Instant.now().toString()
                        )
                        allianceWorkspace = allianceWorkspace.copy(
                            server = onboardingServer.trim(),
                            allianceName = onboardingAllianceTag.trim(),
                            ownProfile = player
                        )
                        accountPrefs.edit().putBoolean("alliance_onboarding_completed_v1", true).apply()
                        showAllianceOnboarding = false
                        currentTab = AssistantTab.ALLIANCE
                        if (profileAuth.currentSession() != null &&
                            onboardingPlayerName.isNotBlank() &&
                            onboardingServer.isNotBlank() &&
                            onboardingAllianceTag.isNotBlank() &&
                            accountPrefs.getString("cloud_alliance_id", "").isNullOrBlank()
                        ) {
                            scope.launch {
                                val isInitialR5 = onboardingRank == AllianceRank.R5
                                val result = if (isInitialR5) {
                                    profileAuth.createAlliance(onboardingServer, onboardingAllianceTag)
                                } else {
                                    profileAuth.submitAllianceJoinRequest(
                                        onboardingServer, onboardingAllianceTag, onboardingPlayerName, onboardingRank.name
                                    )
                                }
                                result.onSuccess { id ->
                                    if (isInitialR5) {
                                        accountPrefs.edit().putString("cloud_alliance_id", id).apply()
                                        snackbarHostState.showSnackbar(localizedUiText(currentLang, "Espace créé : tu es directement R5, sans validation.", "Workspace created: you are R5 immediately, without approval.", "Bereich erstellt: Du bist sofort R5, ohne Bestätigung.", "Alan oluşturuldu: onay gerekmeden doğrudan R5 oldun.", "Пространство создано: вы сразу становитесь R5 без подтверждения.", "空间已创建：你直接成为 R5，无需审核。"))
                                    } else {
                                        snackbarHostState.showSnackbar(localizedUiText(currentLang, "Demande envoyée aux R4/R5 de l'alliance.", "Request sent to the alliance R4/R5 officers.", "Anfrage an die R4/R5 der Allianz gesendet.", "İstek ittifakın R4/R5 yetkililerine gönderildi.", "Запрос отправлен офицерам R4/R5 альянса.", "申请已发送给联盟 R4/R5 管理员。"))
                                    }
                                }.onFailure {
                                    snackbarHostState.showSnackbar(it.message ?: if (isInitialR5) {
                                        localizedUiText(currentLang, "Création de l'espace R5 impossible. Si l'alliance existe déjà, rejoins-la avec un code d'invitation.", "Could not create the R5 workspace. If the alliance already exists, join it with an invitation code.", "Der R5-Bereich konnte nicht erstellt werden. Wenn die Allianz bereits besteht, tritt ihr mit einem Einladungscode bei.", "R5 alanı oluşturulamadı. İttifak zaten varsa davet koduyla katıl.", "Не удалось создать пространство R5. Если альянс уже существует, вступите в него по коду приглашения.", "无法创建 R5 空间。如果联盟已存在，请使用邀请码加入。")
                                    } else {
                                        localizedUiText(currentLang, "Demande non envoyée. Vérifie le compte et le tag.", "Request not sent. Check the account and alliance tag.", "Anfrage nicht gesendet. Überprüfe das Konto und das Allianz-Tag.", "İstek gönderilmedi. Hesabı ve ittifak etiketini kontrol et.", "Запрос не отправлен. Проверьте аккаунт и тег альянса.", "申请未发送。请检查账号和联盟标签。")
                                    })
                                }
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (onboardingRank == AllianceRank.R5) {
                                        localizedUiText(currentLang, "Profil enregistré. Connecte-toi pour créer l'espace de l'alliance et devenir R5.", "Profile saved. Sign in to create the alliance workspace and become R5.", "Profil gespeichert. Melde dich an, um den Allianzbereich zu erstellen und R5 zu werden.", "Profil kaydedildi. İttifak alanını oluşturup R5 olmak için giriş yap.", "Профиль сохранён. Войдите, чтобы создать пространство альянса и стать R5.", "档案已保存。请登录以创建联盟空间并成为 R5。")
                                    } else {
                                        localizedUiText(currentLang, "Profil enregistré. Connecte-toi pour transmettre la demande aux officiers.", "Profile saved. Sign in to send the request to the officers.", "Profil gespeichert. Melde dich an, um die Anfrage an die Offiziere zu senden.", "Profil kaydedildi. İsteği yetkililere iletmek için giriş yap.", "Профиль сохранён. Войдите, чтобы отправить запрос офицерам.", "档案已保存。请登录以向管理人员提交申请。")
                                    }
                                )
                            }
                        }
                    },
                    enabled = onboardingPlayerName.isNotBlank() && onboardingServer.isNotBlank() && onboardingAllianceTag.isNotBlank()
                ) { Text(localizedAppText(localizedAppText("Enregistrer et continuer"))) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAllianceOnboarding = false
                        accountPrefs.edit().putBoolean("alliance_onboarding_completed_v1", true).apply()
                    }
                ) { Text(localizedAppText(localizedAppText("Plus tard"))) }
            }
        )
    }
        }
}

@Composable
private fun LanguageSelectorRow(
    currentLang: AppLanguage,
    onChangeLang: (AppLanguage) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(AppLanguage.entries) { lang ->
            FilterChip(
                selected = currentLang == lang,
                onClick = { onChangeLang(lang) },
                label = { Text(lang.label, fontSize = 11.sp, maxLines = 1) }
            )
        }
    }
}

@Composable
private fun DashboardScreen(
    currentLang: AppLanguage,
    hasUsagePermission: Boolean,
    hasOverlayPermission: Boolean,
    alwaysKeepOverlay: Boolean,
    onToggleAlwaysKeepOverlay: (Boolean) -> Unit,
    isMonitoringRunning: Boolean,
    isGameForeground: Boolean,
    isAutoCapturing: Boolean,
    isAnalyzing: Boolean,
    isProfileSyncing: Boolean,
    autoCaptureIntervalSeconds: Int,
    onChangeAutoCaptureInterval: (Int) -> Unit,
    onStartAutoCapture: () -> Unit,
    onStopAutoCapture: () -> Unit,
    packageFilter: String,
    onPackageFilterChange: (String) -> Unit,
    onOpenUsageSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onStartMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onAnalyzeScreenNow: () -> Unit,
    playerRole: PlayerRole,
    onSelectRole: (PlayerRole) -> Unit,
    currentPower: PowerMetric?,
    heroesCount: Int,
    resourcesCount: Int,
    heroes: List<HeroRecord>,
    resources: List<ResourceRecord>,
    robots: List<RobotRecord>,
    gameIntel: GameIntelSnapshot?,
    gameIntelLoading: Boolean,
    gameIntelError: String?,
    onPickImage: () -> Unit
) {
    var showCaptureSettings by remember { mutableStateOf(false) }
    var showAutoIntervalMenu by remember { mutableStateOf(false) }
    val allPermissionsGranted = hasUsagePermission && hasOverlayPermission

    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Carte capture & autorisations (les autorisations disparaissent dès qu'elles sont accordées)
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (allPermissionsGranted) {
                                when (currentLang) {
                                    AppLanguage.FR -> "Capture & Détection prêtes"
                                    AppLanguage.TR -> "Yakalama ve Algılama Hazır"
                                    AppLanguage.RU -> "Захват и поиск готовы"
                                    AppLanguage.ZH -> "截图与检测已就绪"
                                    AppLanguage.DE -> "Erfassung & Erkennung bereit"
                                    AppLanguage.EN -> "Capture & Detection Ready"
                                    AppLanguage.IT -> "Capture & Detection Ready"
                                }
                            } else {
                                when (currentLang) {
                                    AppLanguage.FR -> "Configuration requise (1 fois)"
                                    AppLanguage.TR -> "İlk Kurulum Gerekli"
                                    AppLanguage.RU -> "Требуется настройка"
                                    AppLanguage.ZH -> "需要初始授权"
                                    AppLanguage.DE -> "Einmalige Einrichtung"
                                    AppLanguage.EN -> "Required Setup (once)"
                                    AppLanguage.IT -> "Required Setup (once)"
                                }
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        if (allPermissionsGranted) {
                            IconButton(
                                onClick = { showCaptureSettings = !showCaptureSettings },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Settings,
                                    contentDescription = localizedAppText(localizedAppText("Réglages capture"))
                                )
                            }
                        }
                    }

                    // Afficher les boutons d'autorisation UNIQUEMENT s'il manque une permission ou si Settings est ouvert
                    if (!allPermissionsGranted || showCaptureSettings) {
                        if (!hasUsagePermission) {
                            Text(
                                localizedAppText("1. Statistiques d'utilisation : À autoriser"),
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Medium
                            )
                            Button(onClick = onOpenUsageSettings) {
                                Text(localizedAppText(localizedAppText("Autoriser Statistiques d'utilisation")))
                            }
                        } else {
                            Text(
                                localizedAppText("1. Statistiques d'utilisation : Autorisées"),
                                color = Color(0xFF34D399),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (!hasOverlayPermission) {
                            Text(
                                localizedAppText("2. Superposition au-dessus du jeu : À autoriser"),
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Medium
                            )
                            Button(onClick = onOpenOverlaySettings) {
                                Text(localizedAppText(localizedAppText("Autoriser affichage par-dessus les applis")))
                            }
                        } else {
                            Text(
                                localizedAppText("2. Superposition au-dessus du jeu : Autorisée"),
                                color = Color(0xFF34D399),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                localizedAppText("Garder le bouton flottant au-dessus du jeu"),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = alwaysKeepOverlay,
                                onCheckedChange = onToggleAlwaysKeepOverlay
                            )
                        }

                        OutlinedTextField(
                            value = packageFilter,
                            onValueChange = onPackageFilterChange,
                            label = { Text(localizedAppText(localizedAppText("Package (com.justgame.jails)"))) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!isMonitoringRunning) {
                            Button(
                                onClick = onStartMonitoring,
                                enabled = hasUsagePermission || hasOverlayPermission
                            ) {
                                Text(
                                    when (currentLang) {
                                        AppLanguage.FR -> "Démarrer sur le jeu"
                                        AppLanguage.TR -> "Oyunda Başlat"
                                        AppLanguage.RU -> "Запуск в игре"
                                        AppLanguage.ZH -> "游戏内启动"
                                        AppLanguage.DE -> "Im Spiel starten"
                                        AppLanguage.EN -> "Start on Game"
                                        AppLanguage.IT -> "Start on Game"
                                    }
                                )
                            }
                        } else {
                            OutlinedButton(onClick = onStopMonitoring) {
                                Text(
                                    when (currentLang) {
                                        AppLanguage.FR -> "Arrêter la détection"
                                        AppLanguage.TR -> "Algılamayı Durdur"
                                        AppLanguage.RU -> "Остановить"
                                        AppLanguage.ZH -> "停止检测"
                                        AppLanguage.DE -> "Stoppen"
                                        AppLanguage.EN -> "Stop detection"
                                        AppLanguage.IT -> "Stop detection"
                                    }
                                )
                            }
                        }
                    }

                    if (isGameForeground) {
                        Text(
                            localizedAppText("Lands of Jail détecté au premier plan."),
                            color = Color(0xFF34D399),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = onAnalyzeScreenNow, enabled = !isAnalyzing && !isAutoCapturing) {
                            Text(
                                when (currentLang) {
                                    AppLanguage.FR -> "Analyser l’écran"
                                    AppLanguage.TR -> "Ekranı Tara"
                                    AppLanguage.RU -> "Сканировать"
                                    AppLanguage.ZH -> "扫描屏幕"
                                    AppLanguage.DE -> "Bildschirm scannen"
                                    AppLanguage.EN -> "Scan screen"
                                    AppLanguage.IT -> "Scan screen"
                                }
                            )
                        }
                        TextButton(enabled = !isAnalyzing && !isAutoCapturing, onClick = onPickImage) {
                            Text(
                                when (currentLang) {
                                    AppLanguage.FR -> "Importer image"
                                    AppLanguage.TR -> "Resim Seç"
                                    AppLanguage.RU -> "Загрузить фото"
                                    AppLanguage.ZH -> "导入图片"
                                    AppLanguage.DE -> "Bild wählen"
                                    AppLanguage.EN -> "Import image"
                                    AppLanguage.IT -> "Import image"
                                }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                when (currentLang) {
                                    AppLanguage.FR -> "Analyse automatique"
                                    AppLanguage.TR -> "Otomatik analiz"
                                    AppLanguage.RU -> "Автоанализ"
                                    AppLanguage.ZH -> "自动分析"
                                    AppLanguage.DE -> "Automatische Analyse"
                                    AppLanguage.EN -> "Automatic analysis"
                                    AppLanguage.IT -> "Automatic analysis"
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                when (currentLang) {
                                    AppLanguage.FR -> "Capture et analyse uniquement quand le jeu est au premier plan."
                                    AppLanguage.TR -> "Yalnızca oyun öndeyken yakala ve analiz et."
                                    AppLanguage.RU -> "Снимок и анализ только когда игра открыта."
                                    AppLanguage.ZH -> "仅在游戏位于前台时截图和分析。"
                                    AppLanguage.DE -> "Aufnahme und Analyse nur bei geöffnetem Spiel."
                                    AppLanguage.EN -> "Capture and analyze only while the game is in the foreground."
                                    AppLanguage.IT -> "Capture and analyze only while the game is in the foreground."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isAutoCapturing) {
                            FilledTonalButton(onClick = onStopAutoCapture) {
                                Text(
                                    when (currentLang) {
                                        AppLanguage.FR -> "Arrêter"
                                        AppLanguage.TR -> "Durdur"
                                        AppLanguage.RU -> "Стоп"
                                        AppLanguage.ZH -> "停止"
                                        AppLanguage.DE -> "Stoppen"
                                        AppLanguage.EN -> "Stop"
                                        AppLanguage.IT -> "Stop"
                                    }
                                )
                            }
                        } else {
                            Box {
                                OutlinedButton(
                                    onClick = { showAutoIntervalMenu = true },
                                    enabled = isMonitoringRunning && hasUsagePermission && !isAnalyzing && !isProfileSyncing && !isAnalyzing && !isProfileSyncing
                                ) {
                                    Text(localizedAppText(localizedAppText("${autoCaptureIntervalSeconds}s")))
                                }
                                DropdownMenu(
                                    expanded = showAutoIntervalMenu,
                                    onDismissRequest = { showAutoIntervalMenu = false }
                                ) {
                                    listOf(10, 20, 30, 60).forEach { seconds ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    when (currentLang) {
                                                        AppLanguage.FR -> "${seconds} secondes"
                                                        AppLanguage.TR -> "${seconds} sn"
                                                        AppLanguage.RU -> "${seconds} с"
                                                        AppLanguage.ZH -> "${seconds} 秒"
                                                        AppLanguage.DE -> "${seconds} Sekunden"
                                                        AppLanguage.EN -> "${seconds} seconds"
                                                        AppLanguage.IT -> "${seconds} seconds"
                                                    }
                                                )
                                            },
                                            onClick = {
                                                onChangeAutoCaptureInterval(seconds)
                                                showAutoIntervalMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                            FilledTonalButton(
                                onClick = onStartAutoCapture,
                                enabled = isMonitoringRunning && hasUsagePermission
                            ) {
                                Text(
                                    when (currentLang) {
                                        AppLanguage.FR -> "Démarrer"
                                        AppLanguage.TR -> "Başlat"
                                        AppLanguage.RU -> "Старт"
                                        AppLanguage.ZH -> "开始"
                                        AppLanguage.DE -> "Start"
                                        AppLanguage.EN -> "Start"
                                        AppLanguage.IT -> "Start"
                                    }
                                )
                            }
                        }
                    }
                    Text(
                        when (currentLang) {
                            AppLanguage.FR -> if (!isMonitoringRunning || !hasUsagePermission) "Active d’abord la détection du jeu et l’accès aux statistiques d’utilisation." else if (isAutoCapturing) "Actif : une analyse à la fois; les écrans quasi identiques sont ignorés. Le profil reste verrouillé pour éviter tout mélange. Arrête quand tu as terminé." else "Intervalle cible : 10 à 60 s. Une seule analyse à la fois; si elle dure plus longtemps, la suivante attend sa fin. Chaque analyse utilise le réseau et le quota IA."
                            AppLanguage.TR -> if (!isMonitoringRunning || !hasUsagePermission) "Önce oyun algılamayı ve kullanım erişimini etkinleştir." else if (isAutoCapturing) "Etkin: aynı anda tek analiz; benzer ekranlar atlanır. Veri karışmasını önlemek için profil kilitli." else "Hedef aralık: 10–60 sn. Analizler çakışmaz; her analiz internet ve yapay zekâ kotası kullanır."
                            AppLanguage.RU -> if (!isMonitoringRunning || !hasUsagePermission) "Сначала включите обнаружение игры и доступ к статистике использования." else if (isAutoCapturing) "Активно: по одному анализу; похожие экраны пропускаются. Профиль заблокирован, чтобы не смешивать данные." else "Целевой интервал: 10–60 с. Анализы не перекрываются и расходуют лимит ИИ."
                            AppLanguage.ZH -> if (!isMonitoringRunning || !hasUsagePermission) "请先启用游戏检测和使用情况访问权限。" else if (isAutoCapturing) "已启用：一次只运行一个分析；相似画面会跳过。为避免数据混合，当前配置已锁定。" else "目标间隔：10–60 秒。分析不会重叠，每次都会使用 AI 配额。"
                            AppLanguage.DE -> if (!isMonitoringRunning || !hasUsagePermission) "Aktiviere zuerst die Spielerkennung und den Nutzungszugriff." else if (isAutoCapturing) "Aktiv: immer nur eine Analyse; nahezu identische Bildschirme werden übersprungen. Das Profil bleibt gesperrt, damit keine Daten vermischt werden." else "Zielintervall: 10–60 s. Analysen überlappen sich nicht und nutzen das KI-Kontingent."
                            AppLanguage.EN -> if (!isMonitoringRunning || !hasUsagePermission) "Enable game detection and usage access first." else if (isAutoCapturing) "Active: one analysis at a time; near-identical screens are skipped. The profile stays locked to prevent data mixing." else "Target interval: 10–60 s. Analyses never overlap; each uses network and AI quota."
                            AppLanguage.IT -> if (!isMonitoringRunning || !hasUsagePermission) "Enable game detection and usage access first." else if (isAutoCapturing) "Active: one analysis at a time; near-identical screens are skipped. The profile stays locked to prevent data mixing." else "Target interval: 10–60 s. Analyses never overlap; each uses network and AI quota."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Objectifs calculés à partir des données réellement enregistrées pour ce profil.
        val heroesToComplete = heroes.filter { hero -> hero.niveauTexte.isBlank() || hero.etoiles.isNullOrBlank() || hero.puissanceHero.isNullOrBlank() || hero.competences.isEmpty() }
        val currentWardenLevel = currentPower?.niveauPrisonOuQG?.filter(Char::isDigit)?.toIntOrNull()
        val nextWardenRequirement = currentWardenLevel?.let { current -> WARDEN_LEVELS_TABLE.firstOrNull { requirement -> requirement.level == current + 1 } }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        localizedUiText(currentLang, "Objectifs à atteindre", "Goals to reach", "Zu erreichende Ziele", "Ulaşılacak hedefler", "Цели для достижения", "待完成目标"),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        localizedUiText(
                            currentLang,
                            "Priorités calculées depuis les données de ce profil. Les valeurs absentes ou incertaines ne sont jamais considérées comme acquises.",
                            "Priorities are calculated from this profile. Missing or uncertain values are never treated as confirmed.",
                            "Prioritäten basieren auf diesem Profil. Fehlende oder unsichere Werte gelten nicht als bestätigt.",
                            "Öncelikler bu profile göre hesaplanır. Eksik veya belirsiz değerler doğrulanmış sayılmaz.",
                            "Приоритеты рассчитаны по этому профилю. Отсутствующие и сомнительные значения не считаются подтверждёнными.",
                            "根据此档案计算优先事项。缺失或不确定的数据不会被视为已确认。"
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (currentWardenLevel == null) {
                        Text(
                            localizedUiText(currentLang, "• Niveau du QG / prison à mesurer", "• Measure HQ / Warden Office level", "• HQ-/Gefängnisstufe erfassen", "• Karargâh / Müdürlük seviyesini ölç", "• Определить уровень штаба / тюрьмы", "• 先识别总部 / 典狱长办公室等级"),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            localizedUiText(
                                currentLang,
                                "À faire : importer une capture du bâtiment principal pour identifier le niveau et calculer la prochaine amélioration.",
                                "Next: import a screenshot of the main building to identify its level and calculate the next upgrade.",
                                "Nächster Schritt: Screenshot des Hauptgebäudes importieren, um die Stufe und das nächste Upgrade zu ermitteln.",
                                "Sonraki adım: seviyeyi belirlemek ve sonraki yükseltmeyi hesaplamak için ana bina ekran görüntüsünü içe aktar.",
                                "Далее: импортируйте снимок главного здания, чтобы определить уровень и рассчитать улучшение.",
                                "下一步：导入主建筑截图，以识别等级并计算下一次升级。"
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else if (nextWardenRequirement != null) {
                        Text(
                            localizedUiText(currentLang, "• Prochain objectif bâtiment : niveau ${nextWardenRequirement.level}", "• Next building goal: level ${nextWardenRequirement.level}", "• Nächstes Gebäudeziel: Stufe ${nextWardenRequirement.level}", "• Sonraki bina hedefi: seviye ${nextWardenRequirement.level}", "• Следующая цель здания: уровень ${nextWardenRequirement.level}", "• 下一个建筑目标：等级 ${nextWardenRequirement.level}"),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            localizedUiText(currentLang, "Prérequis : ${nextWardenRequirement.prereqs}", "Requirements: ${nextWardenRequirement.prereqs}", "Voraussetzungen: ${nextWardenRequirement.prereqs}", "Ön koşullar: ${nextWardenRequirement.prereqs}", "Требования: ${nextWardenRequirement.prereqs}", "前置条件：${nextWardenRequirement.prereqs}"),
                            style = MaterialTheme.typography.bodySmall
                        )
                        val requiredResources = listOf(
                            Triple("Nourriture", nextWardenRequirement.food, listOf("nourriture", "food", "vivres")),
                            Triple("Bois", nextWardenRequirement.wood, listOf("bois", "wood", "lumber")),
                            Triple("Acier", nextWardenRequirement.steel, listOf("acier", "steel", "fer", "iron")),
                            Triple("Essence", nextWardenRequirement.gasoline, listOf("essence", "gasoline", "fuel", "pétrole", "petrole"))
                        ).filter { it.second > 0L }
                        requiredResources.forEach { (label, required, aliases) ->
                            val stock = resources.firstOrNull { resource -> aliases.any { alias -> resource.nom.contains(alias, ignoreCase = true) } }
                            val available = stock?.quantiteNumerique
                            val shortage = available?.let { (required.toDouble() - it).coerceAtLeast(0.0).toLong() }
                            Text(
                                when (currentLang) {
                                    AppLanguage.FR -> "$label : requis ${required} · disponible ${available?.toLong()?.toString() ?: "à confirmer"} · manque ${shortage?.toString() ?: "inconnu"}${if (stock?.incertain == true) " (lecture à confirmer)" else ""}"
                                    AppLanguage.EN -> "$label: required ${required} · available ${available?.toLong()?.toString() ?: "unconfirmed"} · shortfall ${shortage?.toString() ?: "unknown"}${if (stock?.incertain == true) " (verify reading)" else ""}"
                                    AppLanguage.IT -> "$label: required ${required} · available ${available?.toLong()?.toString() ?: "unconfirmed"} · shortfall ${shortage?.toString() ?: "unknown"}${if (stock?.incertain == true) " (verify reading)" else ""}"
                                    AppLanguage.DE -> "$label: benötigt ${required} · vorhanden ${available?.toLong()?.toString() ?: "unbestätigt"} · fehlend ${shortage?.toString() ?: "unbekannt"}"
                                    AppLanguage.TR -> "$label: gereken ${required} · mevcut ${available?.toLong()?.toString() ?: "doğrulanmadı"} · eksik ${shortage?.toString() ?: "bilinmiyor"}"
                                    AppLanguage.RU -> "$label: нужно ${required} · есть ${available?.toLong()?.toString() ?: "не подтверждено"} · не хватает ${shortage?.toString() ?: "неизвестно"}"
                                    AppLanguage.ZH -> "$label：需要 ${required} · 当前 ${available?.toLong()?.toString() ?: "未确认"} · 缺少 ${shortage?.toString() ?: "未知"}"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (!nextWardenRequirement.verified) {
                            Text(
                                localizedUiText(currentLang, "Coûts / prérequis estimés : à confirmer dans le jeu.", "Estimated costs / requirements: confirm in-game.", "Geschätzte Kosten / Voraussetzungen: im Spiel prüfen.", "Tahmini maliyetler / ön koşullar: oyunda doğrulayın.", "Расчётные затраты / требования: проверьте в игре.", "估算费用 / 前置条件：请在游戏内确认。"),
                                color = Color(0xFFFBBF24),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    } else if (currentWardenLevel >= 30) {
                        Text(localizedUiText(currentLang, "• Objectif bâtiment : niveau maximum actuellement référencé (30).", "• Building goal: currently documented maximum level (30).", "• Gebäudeziel: derzeit dokumentierte Höchststufe (30).", "• Bina hedefi: şu anda belgelenen en yüksek seviye (30).", "• Цель здания: максимальный уровень в текущих данных (30).", "• 建筑目标：当前资料记录的最高等级（30）。"), style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(localizedUiText(currentLang, "• Données de prérequis manquantes pour le niveau ${(currentWardenLevel ?: 0) + 1}.", "• Requirements are not documented for level ${(currentWardenLevel ?: 0) + 1}.", "• Voraussetzungen für Stufe ${(currentWardenLevel ?: 0) + 1} fehlen.", "• ${(currentWardenLevel ?: 0) + 1}. seviye için ön koşul verisi yok.", "• Нет данных о требованиях для уровня ${(currentWardenLevel ?: 0) + 1}.", "• 缺少等级 ${(currentWardenLevel ?: 0) + 1} 的前置条件数据。"), style = MaterialTheme.typography.bodySmall)
                    }
                    if (heroesToComplete.isNotEmpty()) {
                        Text(
                            localizedUiText(currentLang, "• Héros à compléter : ${heroesToComplete.size}", "• Heroes to complete: ${heroesToComplete.size}", "• Zu ergänzende Helden: ${heroesToComplete.size}", "• Tamamlanacak kahraman: ${heroesToComplete.size}", "• Героев для заполнения: ${heroesToComplete.size}", "• 待补全英雄：${heroesToComplete.size}"),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            heroesToComplete.take(4).joinToString(" · ") { hero ->
                                val missing = buildList {
                                    if (hero.niveauTexte.isBlank()) add("niveau")
                                    if (hero.etoiles.isNullOrBlank()) add("étoiles")
                                    if (hero.puissanceHero.isNullOrBlank()) add("puissance")
                                    if (hero.competences.isEmpty()) add("compétences")
                                }
                                "${hero.nom} (${missing.joinToString(", ")})"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            localizedUiText(currentLang, "À faire : ouvrir Héros et compléter les champs manquants avec les captures du jeu.", "Next: open Heroes and complete missing fields using game screenshots.", "Nächster Schritt: Helden öffnen und fehlende Felder anhand von Spiel-Screenshots ergänzen.", "Sonraki adım: Kahramanlar bölümünü açıp eksik alanları oyun ekran görüntüleriyle tamamlayın.", "Далее: откройте героев и заполните пропуски по игровым снимкам.", "下一步：打开英雄页面，使用游戏截图补全缺失字段。"),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (resources.isEmpty()) {
                        Text(localizedUiText(currentLang, "• Inventaire à renseigner", "• Resource inventory missing", "• Ressourceninventar fehlt", "• Kaynak envanteri eksik", "• Не заполнен запас ресурсов", "• 尚未记录资源库存"), fontWeight = FontWeight.SemiBold)
                        Text(localizedUiText(currentLang, "À faire : importer une capture de l'inventaire pour comparer tes stocks aux prochains coûts.", "Next: import an inventory screenshot to compare your stock with upcoming costs.", "Nächster Schritt: Screenshot des Inventars importieren und Bestände mit künftigen Kosten vergleichen.", "Sonraki adım: stokları sonraki maliyetlerle karşılaştırmak için envanter ekranını içe aktarın.", "Далее: импортируйте снимок запасов, чтобы сравнить их с будущими затратами.", "下一步：导入库存截图，将现有资源与后续升级费用比较。"), style = MaterialTheme.typography.bodySmall)
                    }
                    if (robots.isEmpty()) {
                        Text(localizedUiText(currentLang, "• Profil robot à renseigner", "• Robot profile missing", "• Roboterprofil fehlt", "• Robot profili eksik", "• Не заполнен профиль робота", "• 尚未记录机器人资料"), fontWeight = FontWeight.SemiBold)
                        Text(localizedUiText(currentLang, "À faire : importer les écrans du robot, de ses bonus et de ses compétences.", "Next: import the robot screens showing its bonuses and skills.", "Nächster Schritt: Screenshots des Roboters mit Boni und Fähigkeiten importieren.", "Sonraki adım: robotun bonuslarını ve becerilerini gösteren ekranları içe aktarın.", "Далее: импортируйте экраны робота, его бонусов и навыков.", "下一步：导入机器人、加成和技能页面。"), style = MaterialTheme.typography.bodySmall)
                    }
                    if (heroesToComplete.isEmpty() && resources.isNotEmpty() && robots.isNotEmpty() && currentWardenLevel != null) {
                        Text(localizedUiText(currentLang, "Les principales données du profil sont renseignées. Consulte Événements et Conseils pour définir tes prochains objectifs tactiques.", "The main profile data is complete. Check Events and Advice to set your next tactical goals.", "Die wichtigsten Profildaten sind erfasst. Prüfe Ereignisse und Tipps für deine nächsten taktischen Ziele.", "Profilin temel verileri tamamlandı. Sonraki taktik hedefler için Etkinlikler ve Tavsiyeler'e bakın.", "Основные данные профиля заполнены. Откройте события и советы, чтобы определить следующие тактические цели.", "主要档案数据已填写。请查看活动和建议，制定下一步战术目标。"), color = Color(0xFF34D399), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        // Résumé du compte
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        localizedAppText("Puissance : ${currentPower?.puissanceTotaleTexte ?: "Non mesurée"}"),
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!currentPower?.niveauPrisonOuQG.isNullOrBlank()) {
                        Text(
                            localizedAppText("Prison / QG : ${currentPower?.niveauPrisonOuQG}"),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (currentPower?.incertain == true) {
                        Text(
                            localizedAppText("Incertain : ${currentPower.raisonIncertitude ?: "lecture partielle"}"),
                            color = Color(0xFFFBBF24),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(localizedAppText(localizedAppText("Héros suivis : $heroesCount · Ressources : $resourcesCount")))
                }
            }
        }

        item {
            Text(localizedAppText(localizedAppText("Rôle du joueur")), fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            PlayerRole.entries.forEach { role ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { onSelectRole(role) }
                ) {
                    RadioButton(selected = role == playerRole, onClick = { onSelectRole(role) })
                    Column {
                        Text(role.labelFor(currentLang), fontWeight = FontWeight.Medium)
                        Text(role.descriptionFor(currentLang), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}


@Composable
private fun HeroPortraitFallback(typeTroupe: String?) {
    val classIcon = when (typeTroupe) {
        "Shieldbearer" -> "🛡️"
        "Shooter" -> "🎯"
        "Bomber" -> "💥"
        else -> "⭐"
    }
    Box(
        modifier = Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(classIcon, fontSize = 28.sp)
    }
}

@Composable
private fun HeroesScreen(
    language: AppLanguage,
    heroes: List<HeroRecord>,
    onAddHero: (HeroCatalogEntry) -> HeroRecord,
    onUpdateHero: (HeroRecord, HeroRecord) -> Unit
) {
    var editingHero by remember { mutableStateOf<HeroRecord?>(null) }
    var search by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("Tous") }
    var expandedHeroName by remember { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current
    val profileByName = remember(heroes) { heroes.associateBy { it.nom.trim().lowercase() } }
    val verifiedHeroes = heroes.count { hero ->
        !hero.niveauTexte.isBlank() && !hero.etoiles.isNullOrBlank() &&
            !hero.puissanceHero.isNullOrBlank() && hero.competences.isNotEmpty()
    }
    val heroesToComplete = heroes.count { hero ->
        hero.niveauTexte.isBlank() || hero.etoiles.isNullOrBlank() ||
            hero.puissanceHero.isNullOrBlank() || hero.competences.isEmpty()
    }
    val visibleHeroes = remember(search, selectedClass, heroes) {
        HeroCatalog.entries.filter { entry ->
            (search.isBlank() || entry.nom.contains(search.trim(), ignoreCase = true)) &&
                (selectedClass == "Tous" || entry.typeTroupe == selectedClass)
        }.sortedByDescending { entry -> profileByName.containsKey(entry.nom.trim().lowercase()) }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(localizedUiText(language, "Catalogue des héros", "Hero catalogue", "Heldenkatalog", "Kahraman kataloğu", "Каталог героев", "英雄目录"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    localizedAppText("${HeroCatalog.entries.size} héros référencés · ${heroes.size} héros suivis sur ton compte"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(localizedUiText(language, "Progression de ton profil héros", "Hero profile progress", "Fortschritt deines Heldenprofils", "Kahraman profil ilerlemesi", "Прогресс профиля героев", "英雄档案进度"), fontWeight = FontWeight.SemiBold)
                        Text(localizedAppText(localizedAppText("$verifiedHeroes profil(s) suffisamment renseigné(s) · $heroesToComplete à compléter")))
                        LinearProgressIndicator(
                            progress = { if (heroes.isEmpty()) 0f else verifiedHeroes.toFloat() / heroes.size.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            localizedAppText("Pour obtenir des conseils fiables, renseigne niveau, étoiles, puissance et compétences War / Arrest / Équipement. Les champs absents ne sont jamais devinés."),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text(localizedUiText(language, "Rechercher un héros", "Search heroes", "Helden suchen", "Kahraman ara", "Поиск героев", "搜索英雄")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("Tous", "Shieldbearer", "Shooter", "Bomber")) { troopClass ->
                        FilterChip(
                            selected = selectedClass == troopClass,
                            onClick = { selectedClass = troopClass },
                            label = { Text(troopClass) }
                        )
                    }
                }
                Text(
                    localizedAppText("Les données du catalogue sont des références communautaires. Seules les valeurs détectées sur tes captures décrivent ton compte."),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFBBF24)
                )
            }
        }
        item {
            Text(
                localizedUiText(language, "Profils détectés en priorité · Catalogue complet ensuite", "Detected profiles first · Full catalogue below", "Erkannte Profile zuerst · Vollständiger Katalog darunter", "Önce algılanan profiller · Ardından tam katalog", "Сначала обнаруженные профили · Затем полный каталог", "优先显示已识别档案 · 下方为完整目录"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        items(visibleHeroes, key = { it.nom }) { entry ->
            val hero = profileByName[entry.nom.trim().lowercase()]
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!entry.portraitUrl.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = entry.portraitUrl,
                                contentDescription = "Portrait de ${entry.nom}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)),
                                loading = { HeroPortraitFallback(entry.typeTroupe) },
                                error = { HeroPortraitFallback(entry.typeTroupe) },
                                success = { SubcomposeAsyncImageContent() }
                            )
                        } else {
                            HeroPortraitFallback(entry.typeTroupe)
                        }
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(entry.nom, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (hero != null) localizedUiText(language, "Profil détecté", "Profile detected", "Profil erkannt", "Profil algılandı", "Профиль обнаружен", "已识别档案") else localizedUiText(language, "Catalogue", "Catalogue", "Katalog", "Katalog", "Каталог", "目录"),
                                color = if (hero != null) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    Text(
                        localizedAppText("${entry.rarete} · ${entry.saison} · ${entry.typeTroupe}"),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (hero != null) {
                        Text(
                            listOfNotNull(
                                hero.niveauTexte.takeIf { it.isNotBlank() },
                                hero.etoiles,
                                hero.puissanceHero?.let { "Puissance $it" }
                            ).joinToString(" · "),
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { editingHero = hero }) {
                                Text(localizedUiText(language, "Modifier mon héros", "Edit my hero", "Meinen Helden bearbeiten", "Kahramanımı düzenle", "Изменить героя", "编辑我的英雄"))
                            }
                            TextButton(onClick = { expandedHeroName = if (expandedHeroName == entry.nom) null else entry.nom }) {
                                Text(if (expandedHeroName == entry.nom) localizedAppText(localizedAppText("Masquer la fiche")) else localizedAppText(localizedAppText("Fiche complète")))
                            }
                        }
                        if (!hero.rarete.isNullOrBlank() && !hero.rarete.equals(entry.rarete, true)) {
                            Text(localizedAppText(localizedAppText("Rareté lue sur capture : ${hero.rarete}")), style = MaterialTheme.typography.bodySmall)
                        }
                        if (hero.incertain) {
                            Text(
                                localizedAppText("À vérifier : ${hero.raisonIncertitude ?: "données de capture incomplètes"}"),
                                color = Color(0xFFFBBF24),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        if (hero.champsIncertains.isNotEmpty()) {
                            val fieldLabels = hero.champsIncertains.map { field ->
                                when (field.lowercase()) {
                                    "niveau" -> "niveau"
                                    "rarete" -> "rareté"
                                    "etoiles" -> "étoiles"
                                    "rolehero" -> "rôle"
                                    "puissancehero" -> "puissance"
                                    else -> field
                                }
                            }
                            Text(
                                localizedAppText("Champs précis à vérifier : ${fieldLabels.joinToString(" · ")}"),
                                color = Color(0xFFFBBF24),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        hero.competences.forEach { skill ->
                            Text(
                                localizedAppText("• ${when (skill.type.lowercase()) { "war" -> "War"; "arrest" -> "Arrest"; "equipment" -> "Équipement"; else -> "Type à confirmer" }} — ${skill.nom} (${skill.niveau})${if (skill.incertain) " · Incertain" else ""}"),
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (!skill.description.isNullOrBlank()) {
                                Text(skill.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        Text(localizedAppText(localizedAppText("Niveau, étoiles et compétences personnels : à scanner ou saisir depuis le jeu.")), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { editingHero = onAddHero(entry) }) {
                            Text(localizedUiText(language, "Ajouter à mon profil", "Add to my profile", "Zu meinem Profil hinzufügen", "Profilime ekle", "Добавить в профиль", "添加到我的档案"))
                        }
                        TextButton(onClick = { expandedHeroName = if (expandedHeroName == entry.nom) null else entry.nom }) {
                            Text(if (expandedHeroName == entry.nom) localizedAppText(localizedAppText("Masquer la fiche")) else localizedAppText(localizedAppText("Voir la fiche complète")))
                        }
                    }
                    if (expandedHeroName == entry.nom) {
                        HorizontalDivider()
                        Text(localizedAppText(localizedAppText("Fiche de référence")), fontWeight = FontWeight.SemiBold)
                        Text(localizedAppText(localizedAppText("Rareté : ${entry.rarete} · Saison : ${entry.saison} · Classe : ${entry.typeTroupe}")))
                        Text(localizedAppText(localizedAppText("Source : ${entry.sourceNiveau}")))
                        entry.usageWarObserve?.let { Text(localizedAppText(localizedAppText("Spécialité observée : $it"))) }
                        if (hero == null || hero.competences.isEmpty()) {
                            Text(
                                localizedAppText("Compétences personnelles non renseignées. Scanne les pages de compétences de ce héros pour obtenir ses effets et niveaux réels."),
                                color = Color(0xFFFBBF24),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        hero?.competences?.forEach { skill ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        localizedAppText("${skill.type.uppercase()} · ${skill.nom} · Niv. ${skill.niveau}"),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(skill.description ?: localizedAppText(localizedAppText("Description non disponible")), style = MaterialTheme.typography.bodySmall)
                                    if (skill.incertain) Text(localizedAppText(localizedAppText("À vérifier")), color = Color(0xFFFBBF24), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        TextButton(onClick = { uriHandler.openUri(entry.sourceUrl) }) {
                            Text(localizedAppText(localizedAppText("Consulter la source du catalogue")))
                        }
                    }
                    entry.usageWarObserve?.let {
                        Text(localizedAppText(localizedAppText("War / équipement observé : $it")), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        if (entry.sourceNiveau == "Observé en jeu") "Référence : LOJ XLab · données observées en jeu"
                        else "Référence : LOJ Strats / Wiki · à confirmer dans ton jeu",
                        color = if (entry.sourceNiveau == "Observé en jeu") Color(0xFF34D399) else Color(0xFFFBBF24),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
        if (visibleHeroes.isEmpty()) {
            item {
                Text(localizedUiText(language, "Aucun héros ne correspond à cette recherche.", "No heroes match this search.", "Keine Helden entsprechen dieser Suche.", "Bu aramayla eşleşen kahraman yok.", "Герои не найдены.", "没有符合此搜索的英雄。"))
            }
        }
        val detectedOutsideCatalog = heroes.filter { detected ->
            HeroCatalog.entries.none { it.nom.equals(detected.nom, ignoreCase = true) }
        }
        if (detectedOutsideCatalog.isNotEmpty()) {
            item { Text(localizedAppText(localizedAppText("Autres héros détectés")), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(detectedOutsideCatalog, key = { "detected-" + it.nom }) { hero ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(hero.nom, fontWeight = FontWeight.SemiBold)
                        Text(listOfNotNull(hero.niveauTexte.takeIf { it.isNotBlank() }, hero.etoiles, hero.puissanceHero).joinToString(" · "))
                        TextButton(onClick = { editingHero = hero }) { Text(localizedAppText(localizedAppText("Modifier les données détectées"))) }
                    }
                }
            }
        }
    }
    editingHero?.let { original ->
        HeroEditDialog(
            hero = original,
            onDismiss = { editingHero = null },
            onSave = { updated ->
                onUpdateHero(original, updated)
                editingHero = null
            }
        )
    }
}

@Composable
private fun HeroEditDialog(
    hero: HeroRecord,
    onDismiss: () -> Unit,
    onSave: (HeroRecord) -> Unit
) {
    var name by remember(hero) { mutableStateOf(hero.nom) }
    var level by remember(hero) { mutableStateOf(hero.niveauTexte.ifBlank { hero.niveau?.toString().orEmpty() }) }
    var rarity by remember(hero) { mutableStateOf(hero.rarete.orEmpty()) }
    var stars by remember(hero) { mutableStateOf(hero.etoiles.orEmpty()) }
    var power by remember(hero) { mutableStateOf(hero.puissanceHero.orEmpty()) }
    var skills by remember(hero) {
        mutableStateOf(hero.competences.joinToString("\n") { it.type + " | " + it.nom + " | " + it.niveau + " | " + it.description.orEmpty() })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(localizedAppText(localizedAppText("Corriger le héros détecté"))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(localizedAppText(localizedAppText("Nom du héros"))) }, singleLine = true)
                OutlinedTextField(level, { level = it }, label = { Text(localizedAppText(localizedAppText("Niveau"))) }, singleLine = true)
                OutlinedTextField(rarity, { rarity = it }, label = { Text(localizedAppText(localizedAppText("Rareté"))) }, singleLine = true)
                OutlinedTextField(stars, { stars = it }, label = { Text(localizedAppText(localizedAppText("Étoiles"))) }, singleLine = true)
                OutlinedTextField(power, { power = it }, label = { Text(localizedAppText(localizedAppText("Puissance"))) }, singleLine = true)
                OutlinedTextField(
                    skills,
                    { skills = it },
                    label = { Text(localizedAppText(localizedAppText("Compétences : type | nom | niveau | description (war / arrest / equipment / inconnu)"))) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(localizedAppText(localizedAppText("Les corrections manuelles remplacent les valeurs incertaines de la capture.")), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val oldSkills = hero.competences.associateBy { it.nom.trim().lowercase() }
                    val correctedSkills = skills.lines().mapNotNull { line ->
                        val parts = line.split("|", limit = 4).map { it.trim() }
                        val explicitType = parts.getOrNull(0)?.lowercase()?.takeIf {
                            it in setOf("war", "arrest", "equipment", "inconnu")
                        }
                        val skillName = if (explicitType != null) parts.getOrNull(1).orEmpty() else parts.getOrNull(0).orEmpty()
                        val levelIndex = if (explicitType != null) 2 else 1
                        val descriptionIndex = if (explicitType != null) 3 else 2
                        if (skillName.isBlank()) null else {
                            val previous = oldSkills[skillName.lowercase()]
                            HeroSkill(
                                nom = skillName,
                                niveau = parts.getOrNull(levelIndex)?.takeIf { it.isNotBlank() } ?: previous?.niveau.orEmpty(),
                                description = parts.getOrNull(descriptionIndex)?.takeIf { it.isNotBlank() } ?: previous?.description,
                                type = explicitType ?: previous?.type ?: "inconnu",
                                incertain = false
                            )
                        }
                    }
                    val fieldsToCheck = if (hero.incertain && hero.champsIncertains.isEmpty()) {
                        listOf("niveau", "rarete", "etoiles", "roleHero", "puissanceHero")
                    } else hero.champsIncertains
                    val remainingUncertainFields = fieldsToCheck.filter { field ->
                        when (field.lowercase()) {
                            "niveau" -> level.isBlank()
                            "rarete" -> rarity.isBlank()
                            "etoiles" -> stars.isBlank()
                            "puissancehero" -> power.isBlank()
                            else -> true
                        }
                    }
                    onSave(
                        hero.copy(
                            nom = name.trim(),
                            niveau = level.filter { it.isDigit() }.toIntOrNull(),
                            niveauTexte = level.trim(),
                            rarete = rarity.trim().ifBlank { null },
                            etoiles = stars.trim().ifBlank { null },
                            puissanceHero = power.trim().ifBlank { null },
                            competences = correctedSkills,
                            champsIncertains = remainingUncertainFields,
                            incertain = remainingUncertainFields.isNotEmpty(),
                            raisonIncertitude = hero.raisonIncertitude.takeIf { remainingUncertainFields.isNotEmpty() }
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) { Text(localizedAppText(localizedAppText("Enregistrer"))) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(localizedAppText(localizedAppText("Annuler"))) } }
    )
}

@Composable
private fun GiftCodesScreen(
    language: AppLanguage,
    snapshot: GiftCodeSnapshot,
    refreshing: Boolean,
    onRefresh: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var filter by remember { mutableStateOf("ALL") }
    val labels = when (language) {
        AppLanguage.FR -> listOf("Tous", "Liste officielle", "À vérifier", "Expirés", "Actualiser", "Dernière vérification", "Date de publication", "Expiration", "Source", "Copier", "Copié", "Ouvrir Discord", "Utiliser les codes", "Aucun code dans cette catégorie.", "Certaines sources sont indisponibles. Les données enregistrées restent visibles.", "Non testé dans le jeu", "Pas de date d’expiration annoncée", "Actualisation de l’application")
        AppLanguage.EN -> listOf("All", "Official list", "Needs checking", "Expired", "Refresh", "Last checked", "Published", "Expires", "Source", "Copy", "Copied", "Open Discord", "Redeem codes", "No codes in this category.", "Some sources are unavailable. Previously saved data remains visible.", "Not tested in game", "No expiry date announced", "App refresh")
        AppLanguage.DE -> listOf("Alle", "Offizielle Liste", "Zu prüfen", "Abgelaufen", "Aktualisieren", "Zuletzt geprüft", "Veröffentlicht", "Läuft ab", "Quelle", "Kopieren", "Kopiert", "Discord öffnen", "Codes einlösen", "Keine Codes in dieser Kategorie.", "Einige Quellen sind nicht erreichbar. Gespeicherte Daten bleiben sichtbar.", "Nicht im Spiel getestet", "Kein Ablaufdatum angegeben", "App-Aktualisierung")
        AppLanguage.TR -> listOf("Tümü", "Resmî liste", "Kontrol edilmeli", "Süresi doldu", "Yenile", "Son kontrol", "Yayın tarihi", "Son kullanma", "Kaynak", "Kopyala", "Kopyalandı", "Discord’u aç", "Kodları kullan", "Bu kategoride kod yok.", "Bazı kaynaklara erişilemiyor. Kaydedilen veriler görünür kalır.", "Oyunda test edilmedi", "Son kullanma tarihi açıklanmadı", "Uygulama yenilemesi")
        AppLanguage.RU -> listOf("Все", "Официальный список", "Проверить", "Истекли", "Обновить", "Последняя проверка", "Опубликован", "Истекает", "Источник", "Копировать", "Скопировано", "Открыть Discord", "Активировать коды", "В этой категории нет кодов.", "Некоторые источники недоступны. Сохранённые данные остаются видимыми.", "Не проверено в игре", "Срок действия не указан", "Обновление приложения")
        AppLanguage.ZH -> listOf("全部", "官方列表", "待核实", "已过期", "刷新", "最后检查", "发布日期", "过期日期", "来源", "复制", "已复制", "打开 Discord", "兑换代码", "此类别暂无代码。", "部分来源不可用，仍显示已保存的数据。", "未在游戏中测试", "未公布过期日期", "应用刷新时间")
        AppLanguage.IT -> listOf("Tutti", "Elenco ufficiale", "Da verificare", "Scaduti", "Aggiorna", "Ultimo controllo", "Pubblicato", "Scade", "Fonte", "Copia", "Copiato", "Apri Discord", "Riscatta codici", "Nessun codice in questa categoria.", "Alcune fonti non sono disponibili. I dati salvati restano visibili.", "Non testato nel gioco", "Nessuna scadenza annunciata", "Aggiornamento dell’app")
    }
    val updatedLabel = runCatching {
        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(java.time.ZoneId.systemDefault()).format(Instant.parse(snapshot.updatedAt))
    }.getOrDefault("—")
    val filtered = snapshot.entries.filter {
        when (filter) {
            "OFFICIAL" -> it.status == "OFFICIAL_LISTED"
            "CHECK" -> it.status == "UNVERIFIED" || it.status == "NEEDS_REVIEW"
            "EXPIRED" -> it.status == "EXPIRED"
            else -> true
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF151D2B))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        localizedUiText(language, "Codes cadeaux Land of Jail", "Land of Jail gift codes", "Land of Jail Geschenkcodes", "Land of Jail hediye kodları", "Подарочные коды Land of Jail", "Land of Jail 礼包码", "Codici regalo Land of Jail"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(labels[17] + " : " + updatedLabel, style = MaterialTheme.typography.bodySmall)
                    Text(
                        localizedUiText(language,
                            "Les codes listés comme officiels sont rapportés par la communauté, mais leur validité doit toujours être confirmée dans le jeu. « Déjà utilisé » ne signifie pas expiré.",
                            "Codes marked official are reported by the community; validity must still be confirmed in game. “Already used” does not mean expired.",
                            "Als offiziell markierte Codes stammen aus Community-Berichten; die Gültigkeit muss im Spiel geprüft werden.",
                            "Resmî olarak işaretlenen kodlar topluluk kaynaklıdır; geçerlilik oyunda doğrulanmalıdır.",
                            "Коды из официального списка сообщества всё равно нужно проверять в игре.",
                            "标记为官方的代码来自社区来源，仍需在游戏中确认有效性。",
                            "I codici indicati come ufficiali provengono dalla community; la validità va comunque verificata nel gioco."
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onRefresh, enabled = !refreshing) {
                            if (refreshing) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                            else Icon(Icons.Outlined.Refresh, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(labels[4])
                        }
                        OutlinedButton(onClick = { uriHandler.openUri(GiftCodeRepository.DISCORD_URL) }) {
                            Icon(Icons.Outlined.Forum, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(labels[11])
                        }
                    }
                    OutlinedButton(onClick = { uriHandler.openUri(GiftCodeRepository.REDEEM_URL) }, modifier = Modifier.fillMaxWidth()) {
                        Text(labels[12])
                    }
                    TextButton(
                        onClick = { uriHandler.openUri(GiftCodeRepository.GUIDE_URL) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(localizedUiText(language, "Consulter les codes sur GuideLOJ", "View codes on GuideLOJ", "Codes auf GuideLOJ ansehen", "Kodları GuideLOJ'de görüntüle", "Посмотреть коды на GuideLOJ", "在 GuideLOJ 查看兑换码", "Vedi i codici su GuideLOJ"))
                    }
                    if (snapshot.sourceErrors.isNotEmpty()) {
                        Text(labels[14], color = Color(0xFFFFC18A), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                items(listOf("ALL" to labels[0], "OFFICIAL" to labels[1], "CHECK" to labels[2], "EXPIRED" to labels[3])) { (key, label) ->
                    FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(label, maxLines = 1) })
                }
            }
        }
        if (filtered.isEmpty()) {
            item { Text(labels[13], color = Color(0xFFCBD5E1), modifier = Modifier.padding(12.dp)) }
        }
        items(filtered, key = { it.code.lowercase() }) { entry ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(entry.code, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        val statusText = when (entry.status) {
                            "OFFICIAL_LISTED" -> labels[1]
                            "EXPIRED" -> labels[3]
                            else -> labels[2]
                        }
                        Text(statusText, color = Color(0xFFFFC18A), style = MaterialTheme.typography.labelSmall)
                    }
                    if (entry.firstSeen.isNotBlank()) Text(labels[6] + " : " + entry.firstSeen, style = MaterialTheme.typography.bodySmall)
                    if (entry.lastChecked.isNotBlank()) Text(labels[5] + " : " + entry.lastChecked, style = MaterialTheme.typography.bodySmall)
                    Text(labels[7] + " : " + entry.expiresAt.ifBlank { labels[16] }, style = MaterialTheme.typography.bodySmall)
                    Text(labels[8] + " : " + entry.source, style = MaterialTheme.typography.bodySmall)
                    if (entry.reward.isNotBlank()) Text(localizedUiText(language, "Récompenses : ", "Rewards: ", "Belohnungen: ", "Ödüller: ", "Награды: ", "奖励：", "Ricompense: ") + entry.reward, style = MaterialTheme.typography.bodySmall)
                    if (entry.note.isNotBlank()) Text(entry.note, style = MaterialTheme.typography.bodySmall, color = Color(0xFFCBD5E1))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Land of Jail code", entry.code))
                        }) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(labels[9])
                        }
                    }
                }
            }
        }
        item {
            Text(
                localizedUiText(language, "Les codes « à vérifier » ne sont jamais présentés comme actifs. Après saisie dans le jeu, vérifie le message : succès, déjà utilisé ou expiré.",
                    "Unverified codes are never presented as active. After trying one in game, check the response: successful, already used, or expired.",
                    "Nicht bestätigte Codes werden nicht als aktiv angezeigt. Prüfe nach dem Einlösen die Spielmeldung.",
                    "Doğrulanmamış kodlar aktif olarak gösterilmez. Oyundaki yanıtı kontrol et.",
                    "Непроверенные коды не помечаются активными. Проверяйте ответ игры.",
                    "未核实代码不会显示为有效。请以游戏兑换结果为准。",
                    "I codici non verificati non vengono indicati come attivi. Controlla la risposta del gioco."),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun ResourcesScreen(
    currentLang: AppLanguage,
    power: PowerMetric?,
    resources: List<ResourceRecord>,
    profileId: String,
    robots: List<RobotRecord>,
    eventRecords: List<EventProgressRecord>,
    onSaveRobots: (List<RobotRecord>) -> Unit,
    onSaveEventRecords: (List<EventProgressRecord>) -> Unit,
    migrateLegacyRoutine: Boolean,
    routineRevision: Int
) {
    val context = LocalContext.current
    val routinePrefs = remember(context, profileId) {
        context.getSharedPreferences("offline_routine_" + profileId, Context.MODE_PRIVATE)
    }
    LaunchedEffect(profileId, migrateLegacyRoutine) {
        val migrationPrefs = context.getSharedPreferences("offline_routine_profile_migration_v1", Context.MODE_PRIVATE)
        if (migrateLegacyRoutine && !migrationPrefs.getBoolean("done", false)) {
            val legacy = context.getSharedPreferences("offline_routine", Context.MODE_PRIVATE)
            val editor = routinePrefs.edit()
            legacy.all.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString(key, value)
                    is Int -> editor.putInt(key, value)
                    is Long -> editor.putLong(key, value)
                    is Boolean -> editor.putBoolean(key, value)
                    is Float -> editor.putFloat(key, value)
                    is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
                }
            }
            editor.commit()
            migrationPrefs.edit().putBoolean("done", true).commit()
        }
    }
    var subTab by remember(profileId) { mutableStateOf(ResourceSubTab.INVENTAIRE) }
    var targetBuildingLvl by remember(profileId) { mutableIntStateOf(16) }
    var starFrom by remember(profileId) { mutableIntStateOf(0) }
    var starTo by remember(profileId) { mutableIntStateOf(4) }
    var ownedShardsText by remember(profileId) { mutableStateOf("0") }
    var tomeFrom by remember(profileId) { mutableIntStateOf(0) }
    var tomeTo by remember(profileId) { mutableIntStateOf(5) }
    var robotName by remember(profileId) { mutableStateOf("") }
    var robotLevel by remember(profileId) { mutableStateOf("") }
    var robotRarity by remember(profileId) { mutableStateOf("") }
    var robotPower by remember(profileId) { mutableStateOf("") }
    var robotBonus by remember(profileId) { mutableStateOf("") }
    var robotSkills by remember(profileId) { mutableStateOf("") }
    var selectedTrackedEvent by remember(profileId) { mutableStateOf(AdviceMode.TRIAL_CAGE) }
    var eventDateInput by remember(profileId) { mutableStateOf("") }
    var eventScoreInput by remember(profileId) { mutableStateOf("") }
    var eventNotesInput by remember(profileId) { mutableStateOf("") }
    val developmentPrefs = remember(context, profileId) {
        context.getSharedPreferences("development_tracker_" + profileId, Context.MODE_PRIVATE)
    }
    var fcBuildingKey by remember(profileId) { mutableStateOf(developmentPrefs.getString("fc_building", "warden_office") ?: "warden_office") }
    var fcCurrentLevel by remember(profileId) { mutableIntStateOf(developmentPrefs.getInt("fc_current_level", 1).coerceIn(1, 9)) }
    var fcCurrentProgress by remember(profileId) { mutableIntStateOf(developmentPrefs.getInt("fc_current_progress", 0).coerceIn(0, 4)) }
    var fcTargetLevel by remember(profileId) { mutableIntStateOf(developmentPrefs.getInt("fc_target_level", 2).coerceIn(2, 10)) }
    var fcCoresOwned by remember(profileId) { mutableStateOf(developmentPrefs.getString("fc_cores_owned", "0") ?: "0") }
    var fcAdvancedOwned by remember(profileId) { mutableStateOf(developmentPrefs.getString("fc_advanced_owned", "0") ?: "0") }
    var gearFromTier by remember(profileId) { mutableIntStateOf(developmentPrefs.getInt("gear_from", 0).coerceIn(0, 9)) }
    var gearToTier by remember(profileId) { mutableIntStateOf(developmentPrefs.getInt("gear_to", 1).coerceIn(1, 10)) }
    var gearExpOwned by remember(profileId) { mutableStateOf(developmentPrefs.getString("gear_exp", "0") ?: "0") }
    var gearMagnetsOwned by remember(profileId) { mutableStateOf(developmentPrefs.getString("gear_magnets", "0") ?: "0") }
    var gearCoilsOwned by remember(profileId) { mutableStateOf(developmentPrefs.getString("gear_coils", "0") ?: "0") }
    var gearPrecisionOwned by remember(profileId) { mutableStateOf(developmentPrefs.getString("gear_precision", "0") ?: "0") }
    var armorCoreTier by remember(profileId) { mutableIntStateOf(developmentPrefs.getInt("armor_core_tier", 1).coerceIn(1, 5)) }
    var armorCoreModule by remember(profileId) { mutableStateOf(developmentPrefs.getString("armor_core_module", "Solo") ?: "Solo") }
    var armorCoreModulesCount by remember(profileId) { mutableIntStateOf(developmentPrefs.getInt("armor_core_modules_count", 1).coerceIn(1, 3)) }
    var armorCoreChipsOwned by remember(profileId) { mutableStateOf(developmentPrefs.getString("armor_core_chips", "0") ?: "0") }
    var hyperalloyOwned by remember(profileId) { mutableStateOf(developmentPrefs.getString("hyperalloy_owned", "0") ?: "0") }
    var hyperalloyPlan by remember(profileId) { mutableStateOf(developmentPrefs.getString("hyperalloy_plan", "bastion") ?: "bastion") }
    var satelliteRarity by remember(profileId) { mutableStateOf(developmentPrefs.getString("satellite_rarity", "SSR") ?: "SSR") }
    var satelliteCurrentMilestone by remember(profileId) { mutableIntStateOf(developmentPrefs.getInt("satellite_current", 0).coerceIn(0, 80)) }
    var satelliteTargetMilestone by remember(profileId) { mutableIntStateOf(developmentPrefs.getInt("satellite_target", 10).coerceIn(10, 90)) }
    var satelliteDisksOwned by remember(profileId) { mutableStateOf(developmentPrefs.getString("satellite_disks", "0") ?: "0") }
    var satelliteCoinsOwned by remember(profileId) { mutableStateOf(developmentPrefs.getString("satellite_coins", "0") ?: "0") }
    LaunchedEffect(fcBuildingKey, fcCurrentLevel, fcCurrentProgress, fcTargetLevel, fcCoresOwned, fcAdvancedOwned, gearFromTier, gearToTier, gearExpOwned, gearMagnetsOwned, gearCoilsOwned, gearPrecisionOwned, armorCoreTier, armorCoreModule, armorCoreModulesCount, armorCoreChipsOwned, hyperalloyOwned, hyperalloyPlan, satelliteRarity, satelliteCurrentMilestone, satelliteTargetMilestone, satelliteDisksOwned, satelliteCoinsOwned) {
        developmentPrefs.edit()
            .putString("fc_building", fcBuildingKey)
            .putInt("fc_current_level", fcCurrentLevel)
            .putInt("fc_current_progress", fcCurrentProgress)
            .putInt("fc_target_level", fcTargetLevel)
            .putString("fc_cores_owned", fcCoresOwned)
            .putString("fc_advanced_owned", fcAdvancedOwned)
            .putInt("gear_from", gearFromTier)
            .putInt("gear_to", gearToTier)
            .putString("gear_exp", gearExpOwned)
            .putString("gear_magnets", gearMagnetsOwned)
            .putString("gear_coils", gearCoilsOwned)
            .putString("gear_precision", gearPrecisionOwned)
            .putInt("armor_core_tier", armorCoreTier)
            .putString("armor_core_module", armorCoreModule)
            .putInt("armor_core_modules_count", armorCoreModulesCount)
            .putString("armor_core_chips", armorCoreChipsOwned)
            .putString("hyperalloy_owned", hyperalloyOwned)
            .putString("hyperalloy_plan", hyperalloyPlan)
            .putString("satellite_rarity", satelliteRarity)
            .putInt("satellite_current", satelliteCurrentMilestone)
            .putInt("satellite_target", satelliteTargetMilestone)
            .putString("satellite_disks", satelliteDisksOwned)
            .putString("satellite_coins", satelliteCoinsOwned)
            .apply()
    }

    var routineItems by remember(routinePrefs, routineRevision) {
        mutableStateOf(
            listOf(
                AndroidRoutineItem("g1", "Marche Récolte #1 (Vivres/Bois/Acier/Essence)", "Lancer avant de quitter le jeu", 120, true, null),
                AndroidRoutineItem("g2", "Marche Récolte #2 / Mine d'Alliance", "Garder les 2 slots occupés", 240, true, null),
                AndroidRoutineItem("tr", "Entraînement Casernes", "Collecter et relancer selon le stock", 180, true, null),
                AndroidRoutineItem("aj", "Aide Alliance, Dons & Auto-Join Ralliement", "Renouveler avant expiration", 480, true, null),
                AndroidRoutineItem("vip", "VIP, Courrier, Arrestation & Distributrice", "Collecte quotidienne", 1440, true, null),
                AndroidRoutineItem("sh", "Alerte Bouclier (8h)", "Rappel avant fin de protection", 480, true, null)
            ).map { item ->
                val savedEnd = routinePrefs.getLong("end_${item.id}", -1L).takeIf { it >= 0L }
                val savedPaused = routinePrefs.getLong("paused_${item.id}", -1L).takeIf { it >= 0L }
                val savedMinutes = routinePrefs.getInt("minutes_${item.id}", item.minutes).coerceIn(1, 10080)
                item.copy(
                    minutes = savedMinutes,
                    enabled = routinePrefs.getBoolean("enabled_${item.id}", item.enabled),
                    endsAtMs = savedEnd,
                    pausedRemainingMs = savedPaused,
                    durationInput = routinePrefs.getString("duration_${item.id}", savedMinutes.toString()) ?: savedMinutes.toString()
                )
            }
        )
    }
    LaunchedEffect(routineItems) {
        val editor = routinePrefs.edit()
            .putString("updated_at", Instant.now().toString())
        routineItems.forEach { item ->
            editor.putInt("minutes_${item.id}", item.minutes)
            editor.putString("duration_${item.id}", item.durationInput)
            editor.putBoolean("enabled_${item.id}", item.enabled)
            if (item.endsAtMs != null) editor.putLong("end_${item.id}", item.endsAtMs) else editor.remove("end_${item.id}")
            if (item.pausedRemainingMs != null) editor.putLong("paused_${item.id}", item.pausedRemainingMs) else editor.remove("paused_${item.id}")
        }
        editor.apply()
    }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(subTab) {
        while (true) {
            nowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ResourceSubTab.entries) { t ->
                    FilterChip(
                        selected = subTab == t,
                        onClick = { subTab = t },
                        label = { Text(t.labelFor(currentLang), maxLines = 1) }
                    )
                }
            }
        }

        when (subTab) {
            ResourceSubTab.INVENTAIRE -> {
                item {
                    Text(
                        localizedAppText("Puissance actuelle : ${power?.puissanceTotaleTexte ?: "Non capturée"}"),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (resources.isEmpty()) {
                    item {
                        Text(
                            localizedAppText("Aucune ressource capturée pour l’instant. Utilisez « Calcul Bâtiments » ou « Calcul Héros » ci-dessus pour calculer vos prochains paliers."),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    items(resources) { res ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(resourceIcon(res.nom), fontSize = 26.sp)
                                    Column(Modifier.weight(1f)) {
                                        Text(localizedResourceName(currentLang, res.nom), fontWeight = FontWeight.Medium)
                                        Text(res.quantiteTexte, style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                                if (res.incertain) {
                                    Text(
                                        localizedUiText(currentLang, "Incertain : ${res.raisonIncertitude ?: "affichage tronqué"}", "Uncertain: ${res.raisonIncertitude ?: "truncated display"}", "Unsicher: ${res.raisonIncertitude ?: "Anzeige abgeschnitten"}", "Belirsiz: ${res.raisonIncertitude ?: "görüntü kesildi"}", "Неточно: ${res.raisonIncertitude ?: "данные обрезаны"}", "不确定：${res.raisonIncertitude ?: "显示内容被截断"}"),
                                        color = Color(0xFFFBBF24),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ResourceSubTab.ROBOTS -> {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(resourceUiText(currentLang, "robot_inventory"), fontWeight = FontWeight.Bold)
                            Text(localizedAppText(localizedAppText("Ces données sont indépendantes des autres profils. Renseigne les bonus tels qu’ils apparaissent en jeu ; aucune statistique n’est devinée.")), style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(robotName, { robotName = it }, label = { Text(resourceUiText(currentLang, "robot_name")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(robotLevel, { robotLevel = it }, label = { Text(resourceUiText(currentLang, "level")) }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(robotRarity, { robotRarity = it }, label = { Text(resourceUiText(currentLang, "rarity_stars")) }, modifier = Modifier.weight(1f), singleLine = true)
                            }
                            OutlinedTextField(robotPower, { robotPower = it }, label = { Text(resourceUiText(currentLang, "power_optional")) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(robotBonus, { robotBonus = it }, label = { Text(resourceUiText(currentLang, "active_bonuses")) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            OutlinedTextField(robotSkills, { robotSkills = it }, label = { Text(resourceUiText(currentLang, "skills_effects")) }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            Button(
                                enabled = robotName.isNotBlank(),
                                onClick = {
                                    val now = Instant.now().toString()
                                    val normalizedName = robotName.trim()
                                    val previous = robots.firstOrNull { it.nom.equals(normalizedName, ignoreCase = true) }
                                    val fieldsToCheck = when {
                                        previous == null -> setOf("niveau", "rarete", "bonus", "competences", "puissance")
                                        previous.incertain && previous.champsIncertains.isEmpty() ->
                                            setOf("niveau", "rarete", "bonus", "competences", "puissance")
                                        else -> previous.champsIncertains.map { it.lowercase() }.toSet()
                                    }
                                    val remainingRobotFields = fieldsToCheck.filter { field ->
                                        when (field) {
                                            "niveau" -> robotLevel.isBlank()
                                            "rarete" -> robotRarity.isBlank()
                                            "bonus" -> robotBonus.isBlank()
                                            "competences" -> robotSkills.isBlank()
                                            "puissance" -> robotPower.isBlank()
                                            else -> true
                                        }
                                    }
                                    val updatedRobot = RobotRecord(
                                        id = previous?.id ?: UUID.randomUUID().toString(),
                                        nom = normalizedName,
                                        niveau = robotLevel.trim().ifBlank { previous?.niveau.orEmpty() },
                                        rarete = robotRarity.trim().ifBlank { previous?.rarete.orEmpty() },
                                        bonus = robotBonus.trim().ifBlank { previous?.bonus.orEmpty() },
                                        competences = robotSkills.trim().ifBlank { previous?.competences.orEmpty() },
                                        puissance = robotPower.trim().ifBlank { previous?.puissance.orEmpty() },
                                        champsIncertains = remainingRobotFields,
                                        incertain = remainingRobotFields.isNotEmpty(),
                                        raisonIncertitude = if (remainingRobotFields.isNotEmpty()) {
                                            "Certains champs du robot restent à vérifier."
                                        } else null,
                                        source = "saisie_manuel",
                                        updatedAtIso = now
                                    )
                                    onSaveRobots(robots.filterNot { it.nom.equals(normalizedName, ignoreCase = true) } + updatedRobot)
                                    robotName = ""; robotLevel = ""; robotRarity = ""
                                    robotPower = ""; robotBonus = ""; robotSkills = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(resourceUiText(currentLang, "save_robot")) }
                        }
                    }
                }
                if (robots.isEmpty()) {
                    item { Text(resourceUiText(currentLang, "no_robot"), style = MaterialTheme.typography.bodySmall) }
                } else {
                    items(robots, key = { it.id }) { robot ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("🤖", fontSize = 28.sp)
                                    Column(Modifier.weight(1f)) {
                                        Text(robot.nom, fontWeight = FontWeight.SemiBold)
                                        if (robot.niveau.isNotBlank()) {
                                            Text(localizedUiText(currentLang, "Niveau ${robot.niveau}", "Level ${robot.niveau}", "Stufe ${robot.niveau}", "Seviye ${robot.niveau}", "Уровень ${robot.niveau}", "等级 ${robot.niveau}"), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                                val meta = listOf(robot.niveau.takeIf { it.isNotBlank() }?.let { localizedUiText(currentLang, "Niv. $it", "Lv. $it", "Stufe $it", "Sv. $it", "Ур. $it", "等级 $it") }, robot.rarete, robot.puissance.takeIf { it.isNotBlank() }?.let { localizedUiText(currentLang, "Puissance $it", "Power $it", "Macht $it", "Güç $it", "Мощь $it", "战力 $it") }).filterNotNull()
                                if (meta.isNotEmpty()) Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                                if (robot.bonus.isNotBlank()) Text(localizedUiText(currentLang, "Bonus : ${robot.bonus}", "Bonus: ${robot.bonus}", "Bonus: ${robot.bonus}", "Bonus: ${robot.bonus}", "Бонус: ${robot.bonus}", "加成：${robot.bonus}"), style = MaterialTheme.typography.bodySmall)
                                if (robot.competences.isNotBlank()) Text(localizedUiText(currentLang, "Compétences : ${robot.competences}", "Skills: ${robot.competences}", "Fähigkeiten: ${robot.competences}", "Beceriler: ${robot.competences}", "Навыки: ${robot.competences}", "技能：${robot.competences}"), style = MaterialTheme.typography.bodySmall)
                                if (robot.incertain) {
                                    Text(
                                        robot.raisonIncertitude?.takeIf { it.isNotBlank() }?.let { localizedUiText(currentLang, "Nom du robot reconnu, niveau non lu explicitement", "Robot name recognized; level not read explicitly", "Robotername erkannt; Stufe nicht eindeutig gelesen", "Robot adı tanındı; seviye açıkça okunamadı", "Название робота распознано, уровень не прочитан", "已识别机器人名称，但未明确读取等级") } ?: localizedUiText(currentLang, "Données détectées à vérifier sur l’écran du jeu.", "Detected data must be checked against the game screen.", "Erkannte Daten müssen im Spiel überprüft werden.", "Algılanan veriler oyun ekranında doğrulanmalı.", "Распознанные данные нужно проверить по экрану игры.", "识别的数据需要与游戏画面核对。"),
                                        color = Color(0xFFFBBF24),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (robot.champsIncertains.isNotEmpty()) {
                                    val robotFieldLabels = robot.champsIncertains.map { field ->
                                        when (field.lowercase()) {
                                            "niveau" -> resourceUiText(currentLang, "level")
                                            "rarete" -> resourceUiText(currentLang, "rarity_stars")
                                            "bonus" -> resourceUiText(currentLang, "active_bonuses")
                                            "competences" -> resourceUiText(currentLang, "skills_effects")
                                            "puissance" -> resourceUiText(currentLang, "power_optional")
                                            else -> field
                                        }
                                    }
                                    Text(
                                        localizedAppText("Champs à vérifier : ${robotFieldLabels.joinToString(" · ")}"),
                                        color = Color(0xFFFBBF24),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                TextButton(onClick = { onSaveRobots(robots.filterNot { it.id == robot.id }) }) { Text(resourceUiText(currentLang, "remove")) }
                            }
                        }
                    }
                }
            }

            ResourceSubTab.DEVELOPPEMENTS -> {
                val fcCosts = mapOf(
                    "warden_office" to (listOf(250, 320, 490, 720, 360, 400, 460, 510, 630) to listOf(0, 0, 0, 0, 24, 36, 48, 72, 168)),
                    "shield_barracks" to (listOf(130, 150, 230, 300, 160, 180, 210, 230, 290) to listOf(0, 0, 0, 0, 10, 15, 22, 32, 75)),
                    "bomber_barracks" to (listOf(130, 150, 230, 300, 160, 180, 210, 230, 290) to listOf(0, 0, 0, 0, 10, 15, 22, 32, 75)),
                    "shooter_barracks" to (listOf(130, 150, 230, 300, 160, 180, 210, 230, 290) to listOf(0, 0, 0, 0, 10, 15, 22, 32, 75)),
                    "comms_center" to (listOf(50, 60, 80, 130, 90, 100, 110, 130, 160) to listOf(0, 0, 0, 0, 5, 8, 12, 17, 41)),
                    "command_center" to (listOf(50, 60, 80, 130, 70, 80, 90, 100, 130) to listOf(0, 0, 0, 0, 5, 8, 12, 14, 34)),
                    "medical_station" to (listOf(50, 60, 80, 130, 70, 80, 90, 100, 130) to listOf(0, 0, 0, 0, 5, 8, 12, 14, 34)),
                    "forticlad_lab" to (listOf(150, 200, 250, 300, 160, 190, 220, 270, 270) to listOf(0, 0, 0, 0, 10, 15, 22, 30, 72))
                )
                val buildingLabels = listOf(
                    "warden_office" to "🏛️ " + localizedUiText(currentLang, "Bureau du directeur", "Warden Office", "Direktorbüro", "Müdür Ofisi", "Кабинет надзирателя", "典狱长办公室"),
                    "shield_barracks" to "🛡️ " + localizedUiText(currentLang, "Caserne Shieldbearer", "Shieldbearer Barracks", "Schildträger-Kaserne", "Kalkanlı Kışlası", "Казарма щитоносцев", "盾卫兵营"),
                    "bomber_barracks" to "💥 " + localizedUiText(currentLang, "Caserne Bomber", "Bomber Barracks", "Bomber-Kaserne", "Bombacı Kışlası", "Казарма бомбардировщиков", "轰炸兵营"),
                    "shooter_barracks" to "🎯 " + localizedUiText(currentLang, "Caserne Shooter", "Shooter Barracks", "Schützen-Kaserne", "Nişancı Kışlası", "Казарма стрелков", "射手兵营"),
                    "comms_center" to "📡 " + localizedUiText(currentLang, "Centre de communications", "Communication Center", "Kommunikationszentrum", "İletişim Merkezi", "Центр связи", "通讯中心"),
                    "command_center" to "🚩 " + localizedUiText(currentLang, "Centre de commandement", "Command Center", "Kommandozentrale", "Komuta Merkezi", "Командный центр", "指挥中心"),
                    "medical_station" to "➕ " + localizedUiText(currentLang, "Poste médical", "Medical Station", "Sanitätsstation", "Sağlık İstasyonu", "Медицинский пункт", "医疗站"),
                    "forticlad_lab" to "🧪 " + localizedUiText(currentLang, "Laboratoire Forticlad", "Forticlad Lab", "Forticlad-Labor", "Forticlad Laboratuvarı", "Лаборатория Forticlad", "Forticlad 实验室")
                )
                val selectedCosts = fcCosts[fcBuildingKey] ?: fcCosts.getValue("warden_office")
                val startStep = (fcCurrentLevel - 1) * 5 + fcCurrentProgress
                val targetStep = (fcTargetLevel - 1) * 5
                var requiredCores = 0
                var requiredAdvanced = 0
                for (step in startStep until targetStep) {
                    val levelIndex = (step / 5).coerceIn(0, 8)
                    requiredCores += selectedCosts.first[levelIndex]
                    requiredAdvanced += selectedCosts.second[levelIndex]
                }
                val ownedCores = fcCoresOwned.toIntOrNull() ?: 0
                val ownedAdvanced = fcAdvancedOwned.toIntOrNull() ?: 0
                val missingCores = (requiredCores - ownedCores).coerceAtLeast(0)
                val missingAdvanced = (requiredAdvanced - ownedAdvanced).coerceAtLeast(0)
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("🔷 " + localizedUiText(currentLang, "Calculateur Forticlad · 8 bâtiments", "Forticlad calculator · 8 buildings", "Forticlad-Rechner · 8 Gebäude", "Forticlad hesaplayıcı · 8 bina", "Калькулятор Forticlad · 8 зданий", "Forticlad 计算器 · 8 栋建筑"), fontWeight = FontWeight.Bold)
                            Text(
                                localizedUiText(currentLang, "Chaque niveau comporte 5 étapes. Les coûts par étape proviennent des tableaux GuideLOJ/LOJ Wiki ; le coût du Poste médical sur le dernier palier est estimé à partir du Centre de commande. Vérifie les valeurs en jeu avant de dépenser.", "Each level has 5 steps. Per-step costs come from GuideLOJ/LOJ Wiki tables; the Medical Station cost at the final tier is estimated from the Command Center. Verify in-game before spending.", "Jede Stufe hat 5 Schritte. Die Kosten stammen aus GuideLOJ/LOJ-Wiki-Tabellen; die Kosten der Sanitätsstation auf der letzten Stufe sind vom Kommandozentrum abgeleitet. Vor dem Ausgeben im Spiel prüfen.", "Her seviyede 5 aşama vardır. Aşama maliyetleri GuideLOJ/LOJ Wiki tablolarından alınmıştır; son kademedeki Sağlık İstasyonu maliyeti Komuta Merkezi'nden tahmin edilmiştir. Harcamadan önce oyunda doğrula.", "Каждый уровень состоит из 5 этапов. Стоимость взята из таблиц GuideLOJ/LOJ Wiki; стоимость медпункта на последнем этапе оценена по командному центру. Перед тратой проверьте в игре.", "每级有 5 个阶段。每阶段成本来自 GuideLOJ/LOJ Wiki；最后阶段的医疗站成本按指挥中心估算。消耗前请在游戏中核对。"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(buildingLabels) { (key, label) ->
                                    FilterChip(
                                        selected = fcBuildingKey == key,
                                        onClick = { fcBuildingKey = key },
                                        label = { Text(label, maxLines = 1) }
                                    )
                                }
                            }
                            var currentLevelMenu by remember { mutableStateOf(false) }
                            var currentProgressMenu by remember { mutableStateOf(false) }
                            var targetLevelMenu by remember { mutableStateOf(false) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(resourceUiText(currentLang, "level"), style = MaterialTheme.typography.labelSmall)
                                    Box {
                                        OutlinedButton(onClick = { currentLevelMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                            Text(fcCurrentLevel.toString())
                                        }
                                        DropdownMenu(expanded = currentLevelMenu, onDismissRequest = { currentLevelMenu = false }) {
                                            ForticladSelection.currentLevels().forEach { level ->
                                                DropdownMenuItem(
                                                    text = { Text(level.toString()) },
                                                    onClick = {
                                                        fcCurrentLevel = level
                                                        fcTargetLevel = ForticladSelection.targetAfterCurrentChange(level, fcTargetLevel)
                                                        currentLevelMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(localizedUiText(currentLang, "Étapes terminées (0–4)", "Completed steps (0–4)", "Abgeschlossene Schritte (0–4)", "Tamamlanan aşama (0–4)", "Завершённые этапы (0–4)", "已完成阶段（0–4）"), style = MaterialTheme.typography.labelSmall)
                                    Box {
                                        OutlinedButton(onClick = { currentProgressMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                            Text(fcCurrentProgress.toString())
                                        }
                                        DropdownMenu(expanded = currentProgressMenu, onDismissRequest = { currentProgressMenu = false }) {
                                            ForticladSelection.completedSteps().forEach { progress ->
                                                DropdownMenuItem(
                                                    text = { Text(progress.toString()) },
                                                    onClick = { fcCurrentProgress = progress; currentProgressMenu = false }
                                                )
                                            }
                                        }
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(localizedUiText(currentLang, "Niveau cible", "Target level", "Zielstufe", "Hedef seviye", "Целевой уровень", "目标等级"), style = MaterialTheme.typography.labelSmall)
                                    Box {
                                        OutlinedButton(onClick = { targetLevelMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                            Text(fcTargetLevel.toString())
                                        }
                                        DropdownMenu(expanded = targetLevelMenu, onDismissRequest = { targetLevelMenu = false }) {
                                            ForticladSelection.targetLevels(fcCurrentLevel).forEach { level ->
                                                DropdownMenuItem(
                                                    text = { Text(level.toString()) },
                                                    onClick = { fcTargetLevel = level; targetLevelMenu = false }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = fcCoresOwned,
                                onValueChange = { fcCoresOwned = it.filter(Char::isDigit).take(9) },
                                label = { Text("🔷 " + localizedUiText(currentLang, "Noyaux Forticlad possédés", "Forticlad Cores owned", "Forticlad-Kerne vorhanden", "Mevcut Forticlad Çekirdekleri", "Ядра Forticlad в наличии", "现有 Forticlad 核心")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = fcAdvancedOwned,
                                onValueChange = { fcAdvancedOwned = it.filter(Char::isDigit).take(9) },
                                label = { Text("💠 " + localizedUiText(currentLang, "Noyaux Forticlad avancés possédés", "Advanced Forticlad Cores owned", "Erweiterte Forticlad-Kerne vorhanden", "Mevcut Gelişmiş Forticlad Çekirdekleri", "Улучшенные ядра Forticlad в наличии", "现有高级 Forticlad 核心")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Divider()
                            Text(buildingLabels.firstOrNull { it.first == fcBuildingKey }?.second ?: "", fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF172333))) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("🔷 " + localizedUiText(currentLang, "Noyaux requis", "Cores required", "Benötigte Kerne", "Gerekli çekirdek", "Требуется ядер", "所需核心"), style = MaterialTheme.typography.labelSmall)
                                        Text(formatCompactFr(requiredCores.toLong()), style = MaterialTheme.typography.titleLarge)
                                        Text(localizedUiText(currentLang, "Manquants : ${formatCompactFr(missingCores.toLong())}", "Missing: ${formatCompactFr(missingCores.toLong())}", "Fehlend: ${formatCompactFr(missingCores.toLong())}", "Eksik: ${formatCompactFr(missingCores.toLong())}", "Не хватает: ${formatCompactFr(missingCores.toLong())}", "缺少：${formatCompactFr(missingCores.toLong())}"), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFF172333))) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("💠 " + localizedUiText(currentLang, "Avancés requis", "Advanced required", "Erweiterte benötigt", "Gelişmiş gerekli", "Улучшенных требуется", "所需高级核心"), style = MaterialTheme.typography.labelSmall)
                                        Text(formatCompactFr(requiredAdvanced.toLong()), style = MaterialTheme.typography.titleLarge)
                                        Text(localizedUiText(currentLang, "Manquants : ${formatCompactFr(missingAdvanced.toLong())}", "Missing: ${formatCompactFr(missingAdvanced.toLong())}", "Fehlend: ${formatCompactFr(missingAdvanced.toLong())}", "Eksik: ${formatCompactFr(missingAdvanced.toLong())}", "Не хватает: ${formatCompactFr(missingAdvanced.toLong())}", "缺少：${formatCompactFr(missingAdvanced.toLong())}"), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    val gearSteps = listOf(
                        Triple(2300, 20, 0), Triple(6980, 40, 0), Triple(19220, 120, 0),
                        Triple(41140, 200, 0), Triple(77000, 400, 0), Triple(117000, 600, 20),
                        Triple(157000, 1000, 40), Triple(197000, 1000, 60),
                        Triple(258000, 2000, 80), Triple(338200, 2000, 100)
                    )
                    val gearStart = gearFromTier.coerceIn(0, 9)
                    val gearEnd = gearToTier.coerceIn(gearStart + 1, 10)
                    val gearRange = gearSteps.subList(gearStart, gearEnd)
                    val requiredExp = gearRange.sumOf { it.first }
                    val requiredMagnets = gearRange.sumOf { it.second }
                    val requiredCoils = gearRange.sumOf { it.third }
                    val ownedExp = gearExpOwned.toIntOrNull() ?: 0
                    val ownedMagnets = gearMagnetsOwned.toIntOrNull() ?: 0
                    val ownedCoils = gearCoilsOwned.toIntOrNull() ?: 0
                    val ownedPrecision = gearPrecisionOwned.toIntOrNull() ?: 0
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🛡️ " + localizedUiText(currentLang, "Arsenal · Équipements de héros", "Arsenal · Hero equipment", "Arsenal · Heldenausrüstung", "Cephanelik · Kahraman ekipmanı", "Арсенал · Снаряжение героев", "军械库 · 英雄装备"), fontWeight = FontWeight.Bold)
                            Text(localizedUiText(currentLang, "Calcule l’EXP, les aimants et les bobines potentielles pour faire évoluer une pièce de niveau 0 à 200. Le niveau de réforme est suivi séparément.", "Calculate EXP, Magnets and Potential Coils to upgrade one piece from level 0 to 200. Reform level is tracked separately.", "Berechne EXP, Magnete und Potenzialspulen für ein Ausrüstungsteil von Stufe 0 bis 200. Reformstufen werden separat verfolgt.", "Bir ekipmanı 0'dan 200'e yükseltmek için EXP, Mıknatıs ve Potansiyel Bobinleri hesapla. Reform seviyesi ayrı izlenir.", "Рассчитайте опыт, магниты и потенциальные катушки для улучшения предмета с 0 до 200. Уровень перековки учитывается отдельно.", "计算单件装备从 0 升至 200 所需经验、磁铁和潜能线圈。重铸等级单独跟踪。"), style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(localizedUiText(currentLang, "Palier de départ (0–9)", "Starting tier (0–9)", "Startstufe (0–9)", "Başlangıç kademesi (0–9)", "Начальный этап (0–9)", "起始阶段（0–9）"), style = MaterialTheme.typography.labelSmall)
                                    OutlinedTextField(gearFromTier.toString(), { gearFromTier = (it.toIntOrNull() ?: 0).coerceIn(0, 9); if (gearToTier <= gearFromTier) gearToTier = gearFromTier + 1 }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(localizedUiText(currentLang, "Palier cible (1–10)", "Target tier (1–10)", "Zielstufe (1–10)", "Hedef kademe (1–10)", "Целевой этап (1–10)", "目标阶段（1–10）"), style = MaterialTheme.typography.labelSmall)
                                    OutlinedTextField(gearToTier.toString(), { gearToTier = (it.toIntOrNull() ?: (gearFromTier + 1)).coerceIn(gearFromTier + 1, 10) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                }
                            }
                            OutlinedTextField(gearExpOwned, { gearExpOwned = it.filter(Char::isDigit).take(10) }, label = { Text("📘 " + localizedUiText(currentLang, "EXP équipement possédée", "Equipment EXP owned", "Ausrüstungs-EXP vorhanden", "Mevcut Ekipman EXP", "Опыт снаряжения в наличии", "现有装备经验")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(gearMagnetsOwned, { gearMagnetsOwned = it.filter(Char::isDigit).take(9) }, label = { Text("🧲 " + localizedUiText(currentLang, "Aimants", "Magnets", "Magnete", "Mıknatıs", "Магниты", "磁铁")) }, singleLine = true, modifier = Modifier.weight(1f))
                                OutlinedTextField(gearCoilsOwned, { gearCoilsOwned = it.filter(Char::isDigit).take(9) }, label = { Text("🌀 " + localizedUiText(currentLang, "Bobines potentielles", "Potential Coils", "Potenzialspulen", "Potansiyel Bobin", "Потенциальные катушки", "潜能线圈")) }, singleLine = true, modifier = Modifier.weight(1f))
                            }
                            OutlinedTextField(gearPrecisionOwned, { gearPrecisionOwned = it.filter(Char::isDigit).take(9) }, label = { Text("⚙️ " + localizedUiText(currentLang, "Engrenages de précision (réforme)", "Precision Gears (reform)", "Präzisionszahnräder (Reform)", "Hassas Dişliler (reform)", "Точные шестерни (перековка)", "精密齿轮（重铸）")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Divider()
                            Text(localizedUiText(currentLang, "Nécessaire pour cette plage", "Required for this range", "Für diesen Bereich benötigt", "Bu aralık için gerekli", "Требуется для этого диапазона", "此阶段所需"), fontWeight = FontWeight.SemiBold)
                            Text("📘 EXP : ${formatCompactFr(requiredExp.toLong())} · ${localizedUiText(currentLang, "manquant", "missing", "fehlend", "eksik", "не хватает", "缺少")} ${formatCompactFr((requiredExp - ownedExp).coerceAtLeast(0).toLong())}")
                            Text("🧲 Aimants : ${formatCompactFr(requiredMagnets.toLong())} · ${localizedUiText(currentLang, "manquants", "missing", "fehlend", "eksik", "не хватает", "缺少")} ${formatCompactFr((requiredMagnets - ownedMagnets).coerceAtLeast(0).toLong())}")
                            Text("🌀 Bobines : ${formatCompactFr(requiredCoils.toLong())} · ${localizedUiText(currentLang, "manquantes", "missing", "fehlend", "eksik", "не хватает", "缺少")} ${formatCompactFr((requiredCoils - ownedCoils).coerceAtLeast(0).toLong())}")
                            Text(localizedUiText(currentLang, "Réforme complète 0→20 : 11 000 aimants + 4 200 engrenages de précision. Ton stock : ${formatCompactFr(ownedMagnets.toLong())} aimants et ${formatCompactFr(ownedPrecision.toLong())} engrenages.", "Full reform 0→20: 11,000 Magnets + 4,200 Precision Gears. Your stock: ${formatCompactFr(ownedMagnets.toLong())} Magnets and ${formatCompactFr(ownedPrecision.toLong())} Gears.", "Vollständige Reform 0→20: 11.000 Magnete + 4.200 Präzisionszahnräder. Bestand: ${formatCompactFr(ownedMagnets.toLong())} Magnete und ${formatCompactFr(ownedPrecision.toLong())} Zahnräder.", "Tam reform 0→20: 11.000 Mıknatıs + 4.200 Hassas Dişli. Stok: ${formatCompactFr(ownedMagnets.toLong())} Mıknatıs ve ${formatCompactFr(ownedPrecision.toLong())} Dişli.", "Полная перековка 0→20: 11 000 магнитов + 4 200 точных шестерён. В наличии: ${formatCompactFr(ownedMagnets.toLong())} магнитов и ${formatCompactFr(ownedPrecision.toLong())} шестерён.", "完整重铸 0→20：11,000 磁铁 + 4,200 精密齿轮。库存：${formatCompactFr(ownedMagnets.toLong())} 磁铁、${formatCompactFr(ownedPrecision.toLong())} 齿轮。"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    val chipsPerTier = listOf(3640, 7180, 19320, 36360, 49960)
                    val requiredChips = chipsPerTier[armorCoreTier - 1] * armorCoreModulesCount
                    val ownedChips = armorCoreChipsOwned.toIntOrNull() ?: 0
                    val missingChips = (requiredChips - ownedChips).coerceAtLeast(0)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🤖 " + localizedUiText(currentLang, "Robots · Noyaux I–V", "Robots · Cores I–V", "Roboter · Kerne I–V", "Robotlar · Çekirdekler I–V", "Роботы · Ядра I–V", "机器人 · 核心 I–V"), fontWeight = FontWeight.Bold)
                            Text(localizedUiText(currentLang, "Planifie les puces de noyau des robots pour les modules Solo, Formation ou Barrière. Le coût affiché est celui d’un module complet du palier choisi.", "Plan robot Core Chips for Solo, Formation or Barrier modules. The displayed cost is for one complete module at the selected tier.", "Plane Roboter-Kernchips für Solo-, Formations- oder Barriere-Module. Die Kosten gelten für ein vollständiges Modul der gewählten Stufe.", "Solo, Formasyon veya Bariyer robot modülleri için Çekirdek Çiplerini planla. Maliyet seçilen kademedeki bir tam modül içindir.", "Планируйте чипы ядер роботов для модулей «Соло», «Формирование» или «Барьер». Указана стоимость одного полного модуля выбранного уровня.", "规划机器人单人、编队或屏障模块的核心芯片。显示的是所选等级单个完整模块的成本。"), style = MaterialTheme.typography.bodySmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("Solo", "Formation", "Barrier")) { module ->
                                    FilterChip(selected = armorCoreModule == module, onClick = { armorCoreModule = module }, label = {
                                        Text(when (module) {
                                            "Solo" -> localizedUiText(currentLang, "Solo", "Solo", "Solo", "Solo", "Соло", "单人")
                                            "Formation" -> localizedUiText(currentLang, "Formation", "Formation", "Formation", "Formasyon", "Формирование", "编队")
                                            else -> localizedUiText(currentLang, "Barrière", "Barrier", "Barriere", "Bariyer", "Барьер", "屏障")
                                        })
                                    })
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(localizedUiText(currentLang, "Noyau I–V", "Core I–V", "Kern I–V", "Çekirdek I–V", "Ядро I–V", "核心 I–V"), style = MaterialTheme.typography.labelSmall)
                                    OutlinedTextField(armorCoreTier.toString(), { armorCoreTier = (it.toIntOrNull() ?: 1).coerceIn(1, 5) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(localizedUiText(currentLang, "Nombre de modules", "Number of modules", "Anzahl Module", "Modül sayısı", "Количество модулей", "模块数量"), style = MaterialTheme.typography.labelSmall)
                                    OutlinedTextField(armorCoreModulesCount.toString(), { armorCoreModulesCount = (it.toIntOrNull() ?: 1).coerceIn(1, 3) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                }
                            }
                            OutlinedTextField(armorCoreChipsOwned, { armorCoreChipsOwned = it.filter(Char::isDigit).take(10) }, label = { Text("🔹 " + localizedUiText(currentLang, "Puces de noyau possédées", "Core Chips owned", "Kernchips vorhanden", "Mevcut Çekirdek Çipleri", "Чипы ядра в наличии", "现有核心芯片")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Divider()
                            Text(localizedUiText(currentLang, "Coût : ${formatCompactFr(requiredChips.toLong())} puces · Manquantes : ${formatCompactFr(missingChips.toLong())}", "Cost: ${formatCompactFr(requiredChips.toLong())} chips · Missing: ${formatCompactFr(missingChips.toLong())}", "Kosten: ${formatCompactFr(requiredChips.toLong())} Chips · Fehlend: ${formatCompactFr(missingChips.toLong())}", "Maliyet: ${formatCompactFr(requiredChips.toLong())} çip · Eksik: ${formatCompactFr(missingChips.toLong())}", "Стоимость: ${formatCompactFr(requiredChips.toLong())} чипов · Не хватает: ${formatCompactFr(missingChips.toLong())}", "成本：${formatCompactFr(requiredChips.toLong())} 芯片 · 缺少：${formatCompactFr(missingChips.toLong())}"), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item {
                    val hyperalloyRequired = when (hyperalloyPlan) {
                        "one_type" -> 48150
                        "three_types" -> 144450
                        else -> 25730
                    }
                    val hyperalloyMissing = (hyperalloyRequired - (hyperalloyOwned.toIntOrNull() ?: 0)).coerceAtLeast(0)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🔬 " + localizedUiText(currentLang, "Laboratoire Forticlad · Hyperalliage", "Forticlad Lab · Hyperalloy", "Forticlad-Labor · Hyperlegierung", "Forticlad Laboratuvarı · Hiperalaşım", "Лаборатория Forticlad · Гиперсплав", "Forticlad 实验室 · 超合金"), fontWeight = FontWeight.Bold)
                            Text(localizedUiText(currentLang, "Objectifs documentés : débloquer les troupes niveau 11 Bastion, terminer un type de troupe ou les trois types.", "Documented goals: unlock Bastion level 11 troops, complete one troop type, or all three types.", "Dokumentierte Ziele: Bastion-Truppen Stufe 11 freischalten, einen Truppentyp oder alle drei abschließen.", "Belgelenen hedefler: 11. seviye Bastion birliklerini açmak, bir birlik türünü veya üçünü tamamlamak.", "Цели: открыть войска Bastion 11-го уровня, завершить один или все три типа войск.", "已记录目标：解锁 11 级 Bastion 部队、完成一种或全部三种部队科技。"), style = MaterialTheme.typography.bodySmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("bastion", "one_type", "three_types")) { plan ->
                                    FilterChip(
                                        selected = hyperalloyPlan == plan,
                                        onClick = { hyperalloyPlan = plan },
                                        label = {
                                            Text(when (plan) {
                                                "bastion" -> localizedUiText(currentLang, "Débloquer Bastion Niv. 11", "Unlock Bastion Lv. 11", "Bastion Stufe 11 freischalten", "Bastion Sv. 11 aç", "Открыть Bastion ур. 11", "解锁 Bastion 11 级")
                                                "one_type" -> localizedUiText(currentLang, "1 type de troupe", "1 troop type", "1 Truppentyp", "1 birlik türü", "1 тип войск", "1 种部队")
                                                else -> localizedUiText(currentLang, "3 types de troupe", "3 troop types", "3 Truppentypen", "3 birlik türü", "3 типа войск", "3 种部队")
                                            })
                                        }
                                    )
                                }
                            }
                            OutlinedTextField(hyperalloyOwned, { hyperalloyOwned = it.filter(Char::isDigit).take(9) }, label = { Text("🧩 " + localizedUiText(currentLang, "Hyperalliage possédé", "Hyperalloy owned", "Hyperlegierung vorhanden", "Mevcut Hiperalaşım", "Гиперсплав в наличии", "现有超合金")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Divider()
                            Text(localizedUiText(currentLang, "Requis : ${formatCompactFr(hyperalloyRequired.toLong())} · Manquant : ${formatCompactFr(hyperalloyMissing.toLong())}", "Required: ${formatCompactFr(hyperalloyRequired.toLong())} · Missing: ${formatCompactFr(hyperalloyMissing.toLong())}", "Benötigt: ${formatCompactFr(hyperalloyRequired.toLong())} · Fehlend: ${formatCompactFr(hyperalloyMissing.toLong())}", "Gerekli: ${formatCompactFr(hyperalloyRequired.toLong())} · Eksik: ${formatCompactFr(hyperalloyMissing.toLong())}", "Требуется: ${formatCompactFr(hyperalloyRequired.toLong())} · Не хватает: ${formatCompactFr(hyperalloyMissing.toLong())}", "需要：${formatCompactFr(hyperalloyRequired.toLong())} · 缺少：${formatCompactFr(hyperalloyMissing.toLong())}"), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item {
                    val satelliteBands = when (satelliteRarity) {
                        "R" -> listOf(1565 to 10, 2840 to 20, 4730 to 40, 7480 to 60)
                        "SR" -> listOf(1740 to 20, 3125 to 40, 5205 to 60, 8225 to 100, 12350 to 150, 17565 to 200)
                        else -> listOf(1785 to 30, 3180 to 60, 5310 to 100, 8380 to 150, 12555 to 210, 17820 to 280, 24130 to 360, 29025 to 450, 32855 to 500)
                    }
                    val satelliteMax = satelliteBands.size * 10
                    val satelliteStart = satelliteCurrentMilestone.coerceIn(0, satelliteMax - 10) / 10
                    val satelliteEnd = satelliteTargetMilestone.coerceIn((satelliteStart + 1) * 10, satelliteMax) / 10
                    val satelliteRange = satelliteBands.subList(satelliteStart, satelliteEnd)
                    val requiredDisks = satelliteRange.sumOf { it.first }
                    val requiredPlanetCoins = satelliteRange.sumOf { it.second }
                    val missingDisks = (requiredDisks - (satelliteDisksOwned.toIntOrNull() ?: 0)).coerceAtLeast(0)
                    val missingCoins = (requiredPlanetCoins - (satelliteCoinsOwned.toIntOrNull() ?: 0)).coerceAtLeast(0)
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("🛰️ " + localizedUiText(currentLang, "Cosmos Hub · Calculateur de satellites", "Cosmos Hub · Satellite calculator", "Kosmos-Zentrum · Satellitenrechner", "Kozmos Merkezi · Uydu hesaplayıcı", "Космический центр · Калькулятор спутников", "宇宙中心 · 卫星计算器"), fontWeight = FontWeight.Bold)
                            Text(localizedUiText(currentLang, "Calcule les disques de données et pièces planétaires pour les paliers de 10 niveaux. Les coûts R, SR et SSR sont issus des tableaux communautaires XLab ; les niveaux intermédiaires ne sont pas calculés.", "Calculate Data Disks and Planet Coins for 10-level milestones. R, SR and SSR costs come from community XLab tables; intermediate levels are not calculated.", "Berechne Datendisks und Planet-Münzen für 10-Level-Meilensteine. R-, SR- und SSR-Kosten stammen aus Community-XLab-Tabellen; Zwischenstufen werden nicht berechnet.", "10 seviyelik aşamalar için Veri Diski ve Gezegen Parası hesapla. R, SR ve SSR maliyetleri topluluk XLab tablolarından alınmıştır; ara seviyeler hesaplanmaz.", "Рассчитайте диски данных и планетные монеты для этапов по 10 уровней. Стоимость R, SR и SSR взята из таблиц сообщества XLab; промежуточные уровни не рассчитываются.", "计算每 10 级阶段所需的数据盘和行星币。R、SR、SSR 成本来自社区 XLab 表格；不计算中间等级。"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("R", "SR", "SSR")) { rarity ->
                                    FilterChip(selected = satelliteRarity == rarity, onClick = {
                                        satelliteRarity = rarity
                                        val maxLevel = when (rarity) { "R" -> 40; "SR" -> 60; else -> 90 }
                                        satelliteCurrentMilestone = satelliteCurrentMilestone.coerceAtMost(maxLevel - 10)
                                        satelliteTargetMilestone = satelliteTargetMilestone.coerceIn(satelliteCurrentMilestone + 10, maxLevel)
                                    }, label = { Text(rarity) })
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(localizedUiText(currentLang, "Palier actuel", "Current milestone", "Aktueller Meilenstein", "Mevcut aşama", "Текущий этап", "当前阶段"), style = MaterialTheme.typography.labelSmall)
                                    OutlinedTextField(value = satelliteCurrentMilestone.toString(), onValueChange = {
                                        val maxLevel = when (satelliteRarity) { "R" -> 40; "SR" -> 60; else -> 90 }
                                        satelliteCurrentMilestone = ((it.toIntOrNull() ?: 0).coerceIn(0, maxLevel - 10) / 10) * 10
                                        if (satelliteTargetMilestone <= satelliteCurrentMilestone) satelliteTargetMilestone = satelliteCurrentMilestone + 10
                                    }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(localizedUiText(currentLang, "Palier cible", "Target milestone", "Ziel-Meilenstein", "Hedef aşama", "Целевой этап", "目标阶段"), style = MaterialTheme.typography.labelSmall)
                                    OutlinedTextField(value = satelliteTargetMilestone.toString(), onValueChange = {
                                        val maxLevel = when (satelliteRarity) { "R" -> 40; "SR" -> 60; else -> 90 }
                                        satelliteTargetMilestone = ((it.toIntOrNull() ?: satelliteCurrentMilestone + 10).coerceIn(satelliteCurrentMilestone + 10, maxLevel) / 10) * 10
                                    }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                }
                            }
                            OutlinedTextField(satelliteDisksOwned, { satelliteDisksOwned = it.filter(Char::isDigit).take(10) }, label = { Text("💿 " + localizedUiText(currentLang, "Disques de données possédés", "Data Disks owned", "Datendisks vorhanden", "Mevcut Veri Diskleri", "Диски данных в наличии", "现有数据盘")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(satelliteCoinsOwned, { satelliteCoinsOwned = it.filter(Char::isDigit).take(10) }, label = { Text("🪙 " + localizedUiText(currentLang, "Pièces planétaires possédées", "Planet Coins owned", "Planet-Münzen vorhanden", "Mevcut Gezegen Paraları", "Планетные монеты в наличии", "现有行星币")) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Divider()
                            Text(localizedUiText(currentLang, "À réunir pour cette plage", "Required for this range", "Für diesen Bereich benötigt", "Bu aralık için gereken", "Требуется для этого диапазона", "此范围所需"), fontWeight = FontWeight.SemiBold)
                            Text("💿 " + localizedUiText(currentLang, "Disques : ", "Disks: ", "Disks: ", "Disk: ", "Диски: ", "数据盘：") + formatCompactFr(requiredDisks.toLong()) + " · " + localizedUiText(currentLang, "manquants", "missing", "fehlend", "eksik", "не хватает", "缺少") + " " + formatCompactFr(missingDisks.toLong()))
                            Text("🪙 " + localizedUiText(currentLang, "Pièces planétaires : ", "Planet Coins: ", "Planet-Münzen: ", "Gezegen Parası: ", "Планетные монеты: ", "行星币：") + formatCompactFr(requiredPlanetCoins.toLong()) + " · " + localizedUiText(currentLang, "manquantes", "missing", "fehlend", "eksik", "не хватает", "缺少") + " " + formatCompactFr(missingCoins.toLong()))
                            Text(localizedUiText(currentLang, "Repère documenté : un satellite SSR du niveau 0 au niveau 90 demande 135 040 disques et 2 140 pièces planétaires selon la somme des paliers publiés.", "Documented reference: one SSR satellite from level 0 to 90 requires 135,040 disks and 2,140 Planet Coins when summing the published milestones.", "Dokumentierter Richtwert: Ein SSR-Satellit von Stufe 0 bis 90 benötigt laut Summe der veröffentlichten Meilensteine 135.040 Disks und 2.140 Planet-Münzen.", "Belgelenen referans: yayımlanan aşamaların toplamına göre 0–90 SSR uydu için 135.040 disk ve 2.140 Gezegen Parası gerekir.", "Справочный показатель: для SSR-спутника с 0 до 90 требуется 135 040 дисков и 2 140 планетных монет по сумме опубликованных этапов.", "参考数据：根据已发布阶段汇总，SSR 卫星从 0 升至 90 需要 135,040 数据盘和 2,140 行星币。"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    Text(localizedUiText(currentLang, "Autres systèmes à suivre", "Other systems to track", "Weitere Systeme", "Diğer sistemler", "Другие системы", "其他系统"), fontWeight = FontWeight.Bold)
                }
                item {
                    val systems = listOf(
                        Triple("🛡️", localizedUiText(currentLang, "Arsenal", "Arsenal", "Arsenal", "Cephanelik", "Арсенал", "军械库"), localizedUiText(currentLang, "Niv. 15 · Équipement héros : EXP, aimants, bobines potentielles et engrenages de précision", "Lv. 15 · Hero equipment: EXP, Magnets, Potential Coils and Precision Gears", "Stufe 15 · Heldenausrüstung: EXP, Magnete, Potenzialspulen und Präzisionszahnräder", "Sv. 15 · Kahraman ekipmanı: EXP, Mıknatıs, Potansiyel Bobin ve Hassas Dişli", "Ур. 15 · Снаряжение героев: опыт, магниты, потенциальные катушки и точные шестерни", "15级 · 英雄装备：经验、磁铁、潜能线圈和精密齿轮")),
                        Triple("🧪", localizedUiText(currentLang, "Laboratoire", "Lab", "Labor", "Laboratuvar", "Лаборатория", "实验室"), localizedUiText(currentLang, "Niv. 9 · Recherches Logistique, Économie et Militaire", "Lv. 9 · Logistics, Economy and Military research", "Stufe 9 · Forschung in Logistik, Wirtschaft und Militär", "Sv. 9 · Lojistik, Ekonomi ve Askerî araştırmalar", "Ур. 9 · Исследования логистики, экономики и военного дела", "9级 · 后勤、经济和军事研究")),
                        Triple("🤖", localizedUiText(currentLang, "Hangar d’armure de puissance", "Power Armor Hangar", "Power-Armor-Hangar", "Güç Zırhı Hangarı", "Ангар силовой брони", "动力装甲机库"), localizedUiText(currentLang, "Niv. 25 · Armure du prisonnier, données d’armure, modules et noyaux I–V", "Lv. 25 · Prisoner Armor, Armor Data, modules and Core I–V", "Stufe 25 · Gefangenenrüstung, Rüstungsdaten, Module und Kerne I–V", "Sv. 25 · Mahkûm Zırhı, zırh verisi, modüller ve Çekirdek I–V", "Ур. 25 · Броня заключённого, данные брони, модули и ядра I–V", "25级 · 囚犯动力装甲、装甲数据、模块和核心 I–V")),
                        Triple("🛰️", localizedUiText(currentLang, "Centre cosmique (Cosmos Hub)", "Cosmos Hub", "Kosmos-Zentrum", "Kozmos Merkezi", "Космический центр", "宇宙中心"), localizedUiText(currentLang, "Niv. 20 · Satellites, disques de données et puces", "Lv. 20 · Satellites, Data Disks and chips", "Stufe 20 · Satelliten, Datendisks und Chips", "Sv. 20 · Uydular, Veri Diskleri ve çipler", "Ур. 20 · Спутники, диски данных и чипы", "20级 · 卫星、数据盘和芯片")),
                        Triple("🏰", localizedUiText(currentLang, "Donjon · Bureau et surveillance", "Dungeon · Office and Surveillance", "Verlies · Büro und Überwachung", "Zindan · Ofis ve Gözetim", "Подземелье · офис и наблюдение", "地牢 · 办公室与监控"), localizedUiText(currentLang, "Suivi des prisonniers fugitifs et criminels, avec les salles du donjon", "Track fugitive and criminal prisoners and dungeon rooms", "Flüchtige und kriminelle Gefangene sowie Verliesräume verfolgen", "Kaçak ve suçlu mahkûmları ve zindan odalarını takip et", "Учёт беглых и преступных заключённых и помещений подземелья", "追踪逃犯、罪犯囚犯及地牢房间")),
                        Triple("⚡", localizedUiText(currentLang, "Station d’énergie Forticlad", "Forticlad Energy Station", "Forticlad-Energiestation", "Forticlad Enerji İstasyonu", "Энергостанция Forticlad", "Forticlad 能源站"), localizedUiText(currentLang, "Niv. 30 · Production quotidienne de noyaux Forticlad", "Lv. 30 · Daily Forticlad Core supply", "Stufe 30 · Tägliche Versorgung mit Forticlad-Kernen", "Sv. 30 · Günlük Forticlad Çekirdeği üretimi", "Ур. 30 · Ежедневная выдача ядер Forticlad", "30级 · 每日供应 Forticlad 核心")),
                        Triple("🔬", localizedUiText(currentLang, "Laboratoire Forticlad", "Forticlad Lab", "Forticlad-Labor", "Forticlad Laboratuvarı", "Лаборатория Forticlad", "Forticlad 实验室"), localizedUiText(currentLang, "Niv. 30 · Technologie Forticlad et Hyperalliage pour les troupes", "Lv. 30 · Forticlad Technology and Hyperalloy for troops", "Stufe 30 · Forticlad-Technologie und Hyperlegierung für Truppen", "Sv. 30 · Birlikler için Forticlad Teknolojisi ve Hiperalaşım", "Ур. 30 · Технология Forticlad и гиперсплав для войск", "30级 · 部队 Forticlad 科技和超合金"))
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        systems.forEach { (icon, name, description) ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(icon, fontSize = 24.sp)
                                    Column(Modifier.weight(1f)) {
                                        Text(name, fontWeight = FontWeight.SemiBold)
                                        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ResourceSubTab.EVENEMENTS -> {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(resourceUiText(currentLang, "event_journal"), fontWeight = FontWeight.Bold)
                            Text(localizedAppText(localizedAppText("Conserve tes résultats, scores et observations pour comparer tes propres performances entre les sessions.")), style = MaterialTheme.typography.bodySmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(AdviceMode.entries) { event ->
                                    FilterChip(
                                        selected = selectedTrackedEvent == event,
                                        onClick = { selectedTrackedEvent = event },
                                        label = { Text(event.titleFor(currentLang), maxLines = 1) }
                                    )
                                }
                            }
                            OutlinedTextField(eventDateInput, { eventDateInput = it }, label = { Text(localizedAppText(localizedAppText("Date (ex. 23/11/2026)"))) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(eventScoreInput, { eventScoreInput = it }, label = { Text(localizedAppText(localizedAppText("Score / dégâts / classement"))) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(eventNotesInput, { eventNotesInput = it }, label = { Text(localizedAppText(localizedAppText("Composition, ratio, remarques"))) }, minLines = 3, modifier = Modifier.fillMaxWidth())
                            Button(
                                enabled = eventDateInput.isNotBlank() || eventScoreInput.isNotBlank() || eventNotesInput.isNotBlank(),
                                onClick = {
                                    val now = Instant.now().toString()
                                    val eventId = selectedTrackedEvent.name
                                    val existing = eventRecords.firstOrNull { it.eventId == eventId && it.date == eventDateInput.trim() }
                                    val record = EventProgressRecord(
                                        id = existing?.id ?: UUID.randomUUID().toString(),
                                        eventId = eventId,
                                        eventName = selectedTrackedEvent.titleFor(currentLang),
                                        date = eventDateInput.trim(),
                                        score = eventScoreInput.trim(),
                                        notes = eventNotesInput.trim(),
                                        updatedAtIso = now
                                    )
                                    onSaveEventRecords((eventRecords.filterNot { it.id == record.id } + record).sortedByDescending { it.updatedAtIso })
                                    eventDateInput = ""; eventScoreInput = ""; eventNotesInput = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(resourceUiText(currentLang, "save_result")) }
                        }
                    }
                }
                if (eventRecords.isEmpty()) {
                    item { Text(resourceUiText(currentLang, "no_event"), style = MaterialTheme.typography.bodySmall) }
                } else {
                    items(eventRecords.sortedByDescending { it.updatedAtIso }, key = { it.id }) { record ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(record.eventName, fontWeight = FontWeight.SemiBold)
                                Text(listOf(record.date, record.score).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                                if (record.notes.isNotBlank()) Text(record.notes, style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { onSaveEventRecords(eventRecords.filterNot { it.id == record.id }) }) { Text(localizedAppText(localizedAppText("Supprimer l’entrée"))) }
                            }
                        }
                    }
                }
            }

            ResourceSubTab.BATIMENTS -> {
                val req = WARDEN_LEVELS_TABLE.firstOrNull { it.level == targetBuildingLvl }
                    ?: WARDEN_LEVELS_TABLE.first()
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                localizedAppText("Bureau du directeur (Warden Office) → Niv. $targetBuildingLvl"),
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                localizedAppText("Règle d'or dès Niv. 9 : gardez le Centre de communications à Niv. ${targetBuildingLvl - 1}."),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFBBF24)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { if (targetBuildingLvl > 2) targetBuildingLvl-- },
                                    enabled = targetBuildingLvl > 2
                                ) { Text(localizedAppText(localizedAppText("-1 Niveau"))) }
                                Button(
                                    onClick = { if (targetBuildingLvl < 30) targetBuildingLvl++ },
                                    enabled = targetBuildingLvl < 30
                                ) { Text(localizedAppText(localizedAppText("+1 Niveau"))) }
                            }
                            Text(localizedAppText("${resourceUiText(currentLang, "prerequisites")} : ${req.prereqs}"), fontWeight = FontWeight.Medium)
                            Text(localizedAppText("${resourceUiText(currentLang, "food")} : ${formatCompactFr(req.food)}"))
                            Text(localizedAppText("${resourceUiText(currentLang, "wood")} : ${formatCompactFr(req.wood)}"))
                            Text(localizedAppText("${resourceUiText(currentLang, "steel")} : ${formatCompactFr(req.steel)}"))
                            Text(localizedAppText("${resourceUiText(currentLang, "gasoline")} : ${formatCompactFr(req.gasoline)}"))
                            Text(
                                if (req.verified) resourceUiText(currentLang, "verified_source")
                                else resourceUiText(currentLang, "estimated_source"),
                                color = if (req.verified) Color(0xFF34D399) else Color(0xFFFBBF24),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            ResourceSubTab.HEROS_OBJETS -> {
                val shardCosts = mapOf(1 to 20, 2 to 40, 3 to 115, 4 to 300, 5 to 600)
                val unlockCost = if (starFrom == 0) 10 else 0
                val totalShards = unlockCost + ((starFrom + 1)..starTo).sumOf { shardCosts[it] ?: 0 }
                val owned = ownedShardsText.toIntOrNull() ?: 0
                val missing = (totalShards - owned).coerceAtLeast(0)

                val wisdomMap = mapOf(1 to 10, 2 to 50, 3 to 90, 4 to 160, 5 to 270, 6 to 380)
                val knowledgeMap = mapOf(1 to 10, 2 to 60, 3 to 110, 4 to 200, 5 to 330, 6 to 460)
                val totalWisdom = ((tomeFrom + 1)..tomeTo).sumOf { wisdomMap[it] ?: 0 }
                val totalKnowledge = ((tomeFrom + 1)..tomeTo).sumOf { knowledgeMap[it] ?: 0 }

                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(localizedAppText(localizedAppText("Éclats de héros ($starFrom★ → $starTo★)")), fontWeight = FontWeight.SemiBold)
                            Text(localizedAppText(localizedAppText("Total 1★→5★ : 1 075 éclats (+10 recrutement). Seuil 4★ recommandé : 475 éclats.")))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    starFrom = (starFrom + 1) % 5
                                    if (starTo <= starFrom) starTo = starFrom + 1
                                }) { Text(localizedAppText(localizedAppText("Départ : $starFrom★"))) }
                                Button(onClick = {
                                    starTo = if (starTo >= 5) starFrom + 1 else starTo + 1
                                }) { Text(localizedAppText(localizedAppText("Cible : $starTo★"))) }
                            }
                            OutlinedTextField(
                                value = ownedShardsText,
                                onValueChange = { ownedShardsText = it.filter { ch -> ch.isDigit() } },
                                label = { Text(localizedAppText(localizedAppText("Éclats en stock"))) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(localizedAppText(localizedAppText("Requis : $totalShards éclats · Manquants : $missing")), fontWeight = FontWeight.Medium)
                        }
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(localizedAppText(localizedAppText("Tomes de troupes (Niv. $tomeFrom → $tomeTo)")), fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    tomeFrom = (tomeFrom + 1) % 6
                                    if (tomeTo <= tomeFrom) tomeTo = tomeFrom + 1
                                }) { Text(localizedAppText(localizedAppText("De : $tomeFrom"))) }
                                Button(onClick = {
                                    tomeTo = if (tomeTo >= 6) tomeFrom + 1 else tomeTo + 1
                                }) { Text(localizedAppText(localizedAppText("À : $tomeTo"))) }
                            }
                            Text(localizedAppText(localizedAppText("Seal of Wisdom : $totalWisdom")))
                            Text(localizedAppText(localizedAppText("Seal of Knowledge : $totalKnowledge")))
                            Text(
                                localizedAppText("Note : Mystery Tomes requis à partir du palier 12→13. Pour les compétences héros : Arrest & War Skill Books selon rareté."),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            ResourceSubTab.ROUTINE -> {
                item {
                    Button(
                        onClick = {
                            val base = System.currentTimeMillis()
                            routineItems = routineItems.map {
                                if (it.enabled) {
                                    it.copy(
                                        minutes = (it.durationInput.toIntOrNull() ?: it.minutes).coerceIn(1, 10080),
                                        endsAtMs = base + (it.durationInput.toIntOrNull() ?: it.minutes).coerceIn(1, 10080) * 60_000L,
                                        pausedRemainingMs = null
                                    )
                                } else {
                                    it
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            when (currentLang) {
                                AppLanguage.FR -> "Démarrer tous les cycles actifs"
                                AppLanguage.TR -> "Aktif Tüm Döngüleri Başlat"
                                AppLanguage.RU -> "Запустить все активные циклы"
                                AppLanguage.ZH -> "启动所有已启用计时器"
                                AppLanguage.DE -> "Alle aktiven Zyklen starten"
                                AppLanguage.EN -> "Start All Active Cycles"
                                AppLanguage.IT -> "Start All Active Cycles"
                            }
                        )
                    }
                }
                items(routineItems) { task ->
                    val isPaused = !task.enabled
                    val rem = if (task.enabled) {
                        task.endsAtMs?.minus(nowMs)
                    } else {
                        task.pausedRemainingMs
                    }
                    val isDue = task.enabled && rem != null && rem <= 0L
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(task.title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                Switch(
                                    checked = task.enabled,
                                    onCheckedChange = { checked ->
                                        val now = System.currentTimeMillis()
                                        routineItems = routineItems.map { item ->
                                            if (item.id != task.id) return@map item
                                            if (!checked) {
                                                val frozen = item.endsAtMs?.minus(now)?.coerceAtLeast(0L)
                                                    ?: item.pausedRemainingMs
                                                item.copy(
                                                    enabled = false,
                                                    endsAtMs = null,
                                                    pausedRemainingMs = frozen
                                                )
                                            } else {
                                                val resumedEndsAt = item.pausedRemainingMs?.let { now + it }
                                                item.copy(
                                                    enabled = true,
                                                    endsAtMs = resumedEndsAt,
                                                    pausedRemainingMs = null
                                                )
                                            }
                                        }
                                    }
                                )
                            }
                            Text(task.subtitle, style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(
                                value = task.durationInput,
                                onValueChange = { value ->
                                    routineItems = routineItems.map { item ->
                                        if (item.id == task.id) item.copy(durationInput = value.filter { ch -> ch.isDigit() }.take(5)) else item
                                    }
                                },
                                label = { Text(resourceUiText(currentLang, "custom_duration")) },
                                supportingText = { Text(resourceUiText(currentLang, "duration_range")) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val configuredMinutes = task.durationInput.toIntOrNull()?.coerceIn(1, 10080) ?: task.minutes
                                val labelTime = when {
                                    isDue -> "PRÊT / À COLLECTER"
                                    isPaused && rem != null -> {
                                        val sec = (rem / 1000L).coerceAtLeast(0L)
                                        String.format("%02d:%02d:%02d · PAUSE", sec / 3600, (sec % 3600) / 60, sec % 60)
                                    }
                                    rem != null -> {
                                        val sec = (rem / 1000L).coerceAtLeast(0L)
                                        String.format("%02d:%02d:%02d", sec / 3600, (sec % 3600) / 60, sec % 60)
                                    }
                                    else -> "${configuredMinutes / 60}h ($configuredMinutes min)"
                                }
                                Text(
                                    labelTime,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isDue -> Color(0xFF34D399)
                                        isPaused -> Color(0xFF94A3B8)
                                        else -> Color(0xFFF59E0B)
                                    }
                                )
                                Button(
                                    enabled = task.durationInput.toIntOrNull() in 1..10080,
                                    onClick = {
                                    val base = System.currentTimeMillis()
                                    routineItems = routineItems.map {
                                        if (it.id == task.id) {
                                            it.copy(
                                                enabled = true,
                                                minutes = (it.durationInput.toIntOrNull() ?: it.minutes).coerceIn(1, 10080),
                                                endsAtMs = base + (it.durationInput.toIntOrNull() ?: it.minutes).coerceIn(1, 10080) * 60_000L,
                                                pausedRemainingMs = null
                                            )
                                        } else {
                                            it
                                        }
                                    }
                                }) {
                                    Text(resourceUiText(currentLang, "start"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatCompactFr(value: Long): String = when {
    value >= 1_000_000_000L -> String.format("%.2f B", value / 1_000_000_000.0)
    value >= 1_000_000L -> String.format("%.1f M", value / 1_000_000.0)
    value >= 1_000L -> String.format("%.0f K", value / 1_000.0)
    else -> value.toString()
}

@Composable
private fun AdviceScreen(
    heroes: List<HeroRecord>,
    robots: List<RobotRecord>,
    currentLang: AppLanguage,
    selectedMode: AdviceMode,
    onSelectMode: (AdviceMode) -> Unit,
    playerRole: PlayerRole,
    advice: TacticalAdviceResult?,
    isLoading: Boolean,
    onGenerateAdvice: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(Modifier.height(6.dp))
             Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AdviceMode.entries) { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { onSelectMode(mode) },
                        label = { Text(mode.titleFor(currentLang), maxLines = 1) }
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2E))
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        selectedMode.titleFor(currentLang),
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF59E0B)
                    )
                    Text(
                        selectedMode.briefFor(currentLang),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(localizedUiText(currentLang, "Rôle : ${playerRole.labelFor(currentLang)}", "Role: ${playerRole.labelFor(currentLang)}", "Rolle: ${playerRole.labelFor(currentLang)}", "Rol: ${playerRole.labelFor(currentLang)}", "Роль: ${playerRole.labelFor(currentLang)}", "角色：${playerRole.labelFor(currentLang)}"), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            val heroesWithWarSkills = heroes.count { hero ->
                hero.competences.any { skill ->
                    skill.type.contains("war", ignoreCase = true) ||
                        skill.type.contains("guerre", ignoreCase = true)
                }
            }
            val heroesWithUncertainSkills = heroes.count { hero ->
                hero.competences.isEmpty() || hero.competences.any { it.incertain }
            }
            val robotsWithDetails = robots.count { robot ->
                robot.bonus.isNotBlank() || robot.competences.isNotBlank()
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(localizedAppText(localizedAppText("Contrôle des données avant recommandation")), fontWeight = FontWeight.SemiBold)
                    Text(
                        localizedAppText("Profil : ${heroes.size} héros · ${heroesWithWarSkills} avec compétence War identifiée · ${robots.size} robots · ${robotsWithDetails} avec bonus ou compétence renseigné."),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        when {
                            heroes.isEmpty() -> "Données insuffisantes : scanne tes héros pour obtenir des propositions personnalisées. Les conseils affichés resteront génériques."
                            heroesWithWarSkills == 0 -> "Compétences War non identifiées : aucune composition de héros ne sera présentée comme optimale."
                            heroesWithUncertainSkills > 0 -> "Certaines compétences sont absentes ou incertaines : les correspondances seront à confirmer dans le jeu."
                            else -> "Compétences War détectées. Cela permet des correspondances, mais pas de prouver un classement optimal sans tests de combat."
                        },
                        color = Color(0xFFFBBF24),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (robots.isNotEmpty() && robotsWithDetails < robots.size) {
                        Text(
                            localizedAppText("Robots incomplets : renseigne leurs bonus et compétences avant de les comparer."),
                            color = Color(0xFFFBBF24),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Button(onClick = onGenerateAdvice, enabled = !isLoading) {
                Text(
                    if (isLoading) {
                        when (currentLang) {
                            AppLanguage.FR -> "Recherche Internet & photos…"
                            AppLanguage.TR -> "İnternet rehberleri aranıyor…"
                            AppLanguage.RU -> "Поиск гайдов и фото…"
                            AppLanguage.ZH -> "正在搜索攻略与图片…"
                            AppLanguage.DE -> "Suche Guides & Fotos…"
                            AppLanguage.EN -> "Searching Internet & photos…"
                            AppLanguage.IT -> "Searching Internet & photos…"
                        }
                    } else {
                        when (currentLang) {
                            AppLanguage.FR -> "Obtenir les conseils & photos Internet"
                            AppLanguage.TR -> "İnternet Rehberi ve Fotoğrafları Al"
                            AppLanguage.RU -> "Получить гайды и фото"
                            AppLanguage.ZH -> "获取网络攻略与图片"
                            AppLanguage.DE -> "Internet-Guides & Fotos abrufen"
                            AppLanguage.EN -> "Get Internet Guides & Photos"
                            AppLanguage.IT -> "Get Internet Guides & Photos"
                        }
                    }
                )
            }
        }

        advice?.let { res ->
            val adviceDate = res.generatedAt.ifBlank { res.generatedAtIso }
            val photoSources = res.sources.filter { !it.imageUrl.isNullOrBlank() }

            // Galerie horizontale de photos des guides trouvés sur Internet
            if (photoSources.isNotEmpty()) {
                item {
                    Text(
                        when (currentLang) {
                            AppLanguage.FR -> "Photos des guides Internet"
                            AppLanguage.TR -> "İnternet Rehber Fotoğrafları"
                            AppLanguage.RU -> "Фотографии из гайдов"
                            AppLanguage.ZH -> "攻略图片"
                            AppLanguage.DE -> "Fotos aus Guides"
                            AppLanguage.EN -> "Photos from Internet Guides"
                            AppLanguage.IT -> "Photos from Internet Guides"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(photoSources) { src ->
                            Card(
                                modifier = Modifier
                                    .width(230.dp)
                                    .clickable { uriHandler.openUri(src.url) }
                            ) {
                                Column {
                                    AsyncImage(
                                        model = src.imageUrl,
                                        contentDescription = src.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(124.dp)
                                    )
                                    Column(Modifier.padding(10.dp)) {
                                        Text(
                                            src.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        src.detectedDate?.let { dt ->
                                            Text(
                                                dt,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF94A3B8)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(localizedAppText(localizedAppText("Synthèse (${adviceDate.take(10)})")), fontWeight = FontWeight.SemiBold)
                        Text(res.synthese)
                        Text(localizedAppText(localizedAppText("Composition : ${res.compositionRecommandee}")))
                    }
                }
            }

            if (res.troopRatioProfiles.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text(res.troopRatioTitle ?: localizedAppText(localizedAppText("Ratios de troupes")), fontWeight = FontWeight.SemiBold)
                            Text(
                                localizedAppText("Ratios de référence : vérifie toujours les consignes du chef et les règles affichées dans l’événement."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            res.troopRatioProfiles.forEach { ratio ->
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(ratio.label, fontWeight = FontWeight.Medium)
                                    val ratioTotal = ratio.shieldbearer + ratio.bomber + ratio.shooter
                                    Text(
                                        if (ratioTotal == 100) "Total : 100 % · ratio de référence"
                                        else "Total calculé : ${ratioTotal} % · ratio incomplet à vérifier",
                                        color = if (ratioTotal == 100) Color(0xFF86EFAC) else Color(0xFFFBBF24),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        localizedAppText("${ratio.shieldbearer}% Shieldbearers · ${ratio.bomber}% Bombers · ${ratio.shooter}% Shooters"),
                                        color = Color(0xFFFBBF24),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (ratio.roleLabel.isNotBlank()) {
                                        Text(ratio.roleLabel, style = MaterialTheme.typography.labelSmall)
                                    }
                                    if (ratio.note.isNotBlank()) {
                                        Text(ratio.note, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (res.heroRecommendations.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(localizedAppText(localizedAppText("Héros du profil — compétences correspondantes")), fontWeight = FontWeight.SemiBold)
                            Text(
                                localizedAppText("Sélection fondée sur les compétences reconnues dans ce profil. Ce n’est pas un classement de puissance ni une simulation de combat."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            res.heroRecommendations.forEach { hero ->
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(hero.titre, fontWeight = FontWeight.Medium)
                                    Text(hero.detail, style = MaterialTheme.typography.bodySmall)
                                    hero.raisonIncertitude?.let {
                                        Text(localizedAppText(localizedAppText("À vérifier : $it")), color = Color(0xFFFBBF24), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (res.robotRecommendations.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(localizedAppText(localizedAppText("Robots du profil — correspondances à vérifier")), fontWeight = FontWeight.SemiBold)
                            Text(
                                localizedAppText("Sélection basée sur les bonus et compétences que tu as saisis. Ce n’est pas une simulation ni un classement garanti."),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            res.robotRecommendations.forEach { robot ->
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(robot.titre, fontWeight = FontWeight.Medium)
                                    Text(robot.detail, style = MaterialTheme.typography.bodySmall)
                                    robot.raisonIncertitude?.let {
                                        Text(localizedAppText(localizedAppText("À vérifier : $it")), color = Color(0xFFFBBF24), style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            items(res.recommandations) { rec ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        if (!rec.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = rec.imageUrl,
                                contentDescription = rec.titre,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(145.dp)
                            )
                        }
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(rec.titre, fontWeight = FontWeight.SemiBold)
                            Text(rec.detail, style = MaterialTheme.typography.bodySmall)
                            if (rec.incertain && !rec.raisonIncertitude.isNullOrBlank()) {
                                Text(
                                    localizedAppText("Incertain : ${rec.raisonIncertitude}"),
                                    color = Color(0xFFEF4444),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            if (res.informationsIncertaines.isNotEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(localizedAppText(localizedAppText("AVERTISSEMENT — CONSEILS NON VÉRIFIÉS")), fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                            res.informationsIncertaines.forEach { info ->
                                Text(localizedAppText(localizedAppText("• $info")), color = Color(0xFFFFCDD2))
                            }
                        }
                    }
                }
            }

            item {
                Text(localizedAppText(localizedAppText("Sources Internet, Photos & Dates")), fontWeight = FontWeight.SemiBold)
            }
            items(res.sources) { src ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri(src.url) }
                ) {
                    Row(
                        Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!src.imageUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = src.imageUrl,
                                contentDescription = src.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                src.title,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFBBF24),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                localizedAppText("Date : ${src.detectedDate ?: adviceDate.take(10)}"),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    analyses: List<ScreenshotAnalysis>,
    onClearHistory: () -> Unit
) {
    val currentLang = LocalAssistantLanguage.current
    var showClearConfirmation by remember { mutableStateOf(false) }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(localizedAppText(localizedAppText("Effacer l’historique ?"))) },
            text = { Text(localizedAppText(localizedAppText("Toutes les analyses enregistrées dans l’historique seront supprimées. Cette action est irréversible."))) },
            confirmButton = {
                TextButton(onClick = {
                    onClearHistory()
                    showClearConfirmation = false
                }) { Text(localizedAppText(localizedAppText("Effacer"))) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text(localizedAppText(localizedAppText("Annuler"))) }
            }
        )
    }

    if (analyses.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(localizedAppText(localizedAppText("Historique vide. Appuyez sur « Analyser l’écran » pour enregistrer votre première capture.")))
        }
        return
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { showClearConfirmation = true }) {
                Icon(Icons.Outlined.DeleteSweep, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(localizedAppText(localizedAppText("Effacer l’historique")))
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(analyses) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (item.imageUriString.isNotBlank()) {
                        AsyncImage(
                            model = item.imageUriString,
                            contentDescription = item.resumeCapture,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                    } else {
                        Icon(
                            Icons.Outlined.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(localizedCaptureCategory(currentLang, item.typeCapture), fontWeight = FontWeight.SemiBold)
                        Text(
                            item.analyzedAtIso.take(19).replace('T', ' '),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(localizedCaptureSummary(item, currentLang), style = MaterialTheme.typography.bodySmall)
                        if (item.champsIncertains.isNotEmpty()) {
                            Text(
                                localizedAppText("${item.champsIncertains.size} champ(s) incertain(s)"),
                                color = Color(0xFFFBBF24),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
}


@Composable
private fun SupabaseAccountCard(onChangeLanguage: (AppLanguage) -> Unit) {
    val currentLang = LocalAssistantLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember(context) { SupabaseAuthRepository(context) }
    val profilePrefs = remember(context) { context.getSharedPreferences("lands_of_jail_account_v1", Context.MODE_PRIVATE) }
    val profileJson = remember { Json { ignoreUnknownKeys = true } }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var session by remember { mutableStateOf(auth.currentSession()) }
    var createAccount by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    val savedAllianceAtStart = remember {
        runCatching {
            profileJson.decodeFromString<AllianceWorkspace>(
                profilePrefs.getString("alliance_workspace", "{}") ?: "{}"
            )
        }.getOrDefault(AllianceWorkspace())
    }
    var signupPlayerName by remember { mutableStateOf(savedAllianceAtStart.ownProfile.playerName) }
    var signupServer by remember { mutableStateOf(savedAllianceAtStart.server) }
    var signupAllianceTag by remember { mutableStateOf(savedAllianceAtStart.allianceName) }
    var signupRank by remember { mutableStateOf(savedAllianceAtStart.ownProfile.rank) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151C27)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(localizedAppText(localizedAppText("Compte & sauvegarde cloud")), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                localizedAppText("Connecte ton compte pour synchroniser ton profil de joueur et tes héros dans l'espace cloud sécurisé."),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB8C3D1)
            )
            if (session != null) {
                Text(localizedAppText(localizedAppText("Session enregistrée : ${session?.email.orEmpty()}")), color = Color(0xFF86EFAC), style = MaterialTheme.typography.bodyMedium)
                Text(
                    localizedAppText("Les jetons sont chiffrés avec Android Keystore. La synchronisation du profil se lance depuis le formulaire joueur ci-dessous."),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFBBF24)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                busy = true
                                auth.refreshSession()
                                    .onSuccess { session = it; message = localizedUiText(currentLang, "Session actualisée.", "Session refreshed.", "Sitzung aktualisiert.", "Oturum yenilendi.", "Сеанс обновлён.", "会话已刷新。"); isError = false }
                                    .onFailure { message = it.message ?: localizedUiText(currentLang, "Actualisation impossible.", "Refresh failed.", "Aktualisierung fehlgeschlagen.", "Yenileme başarısız.", "Не удалось обновить.", "刷新失败。"); isError = true }
                                busy = false
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) { Text(localizedAppText(localizedAppText("Actualiser"))) }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                busy = true
                                auth.signOut()
                                    .onSuccess { session = null; message = localizedUiText(currentLang, "Déconnexion effectuée.", "Signed out.", "Abgemeldet.", "Oturum kapatıldı.", "Вы вышли из аккаунта.", "已退出登录。"); isError = false }
                                    .onFailure { message = it.message ?: localizedUiText(currentLang, "Déconnexion impossible.", "Could not sign out.", "Abmelden nicht möglich.", "Oturum kapatılamadı.", "Не удалось выйти из аккаунта.", "无法退出登录。"); isError = true }
                                busy = false
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) { Text(localizedAppText(localizedAppText("Déconnexion"))) }
                }
            } else {
                Text(localizedUiText(currentLang, "Langue de l’application", "App language", "App-Sprache", "Uygulama dili", "Язык приложения", "应用语言", "Lingua dell’app"), style = MaterialTheme.typography.labelLarge)
                LanguageSelectorRow(currentLang = currentLang, onChangeLang = onChangeLanguage)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = { Text(localizedAppText(localizedAppText("Adresse e-mail"))) },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(localizedAppText(localizedAppText("Mot de passe (8 caractères minimum)"))) },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (createAccount) {
                    HorizontalDivider(color = Color(0xFF334155))
                    Text(localizedAppText(localizedAppText("Profil joueur et alliance")), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(localizedAppText(localizedAppText("Ces informations sont enregistrées pendant la création du compte. Tu n'auras pas à refaire une inscription dans l'espace Alliance.")), style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8C3D1))
                    OutlinedTextField(
                        value = signupPlayerName,
                        onValueChange = { signupPlayerName = it.take(40) },
                        label = { Text(localizedAppText(localizedAppText("Pseudo dans Land of Jail"))) },
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = signupServer,
                        onValueChange = { signupServer = it.filter(Char::isDigit).take(6) },
                        label = { Text(localizedAppText(localizedAppText("Serveur (ex. 311)"))) },
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = signupAllianceTag,
                        onValueChange = { signupAllianceTag = it.take(12) },
                        label = { Text(localizedAppText(localizedAppText("Tag de l'alliance"))) },
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(localizedAppText(localizedAppText("Ton rang actuel")), style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        items(AllianceRank.entries) { rank ->
                            FilterChip(
                                selected = signupRank == rank,
                                onClick = { signupRank = rank },
                                enabled = !busy,
                                label = { Text(rank.name) }
                            )
                        }
                    }
                    Text(localizedAppText(localizedAppText("Choisis R5 uniquement si tu crées un nouvel espace d'alliance. Pour rejoindre une alliance existante, indique ton rang actuel : ta demande sera transmise aux responsables.")), style = MaterialTheme.typography.bodySmall, color = Color(0xFFFBBF24))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { createAccount = !createAccount; message = null }, enabled = !busy) {
                        Text(if (createAccount) localizedAppText(localizedAppText("J'ai déjà un compte")) else localizedAppText(localizedAppText("Créer un compte")))
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            scope.launch {
                                busy = true
                                message = null
                                if (createAccount) {
                                    val player = savedAllianceAtStart.ownProfile.copy(
                                        id = savedAllianceAtStart.ownProfile.id.ifBlank { UUID.randomUUID().toString() },
                                        playerName = signupPlayerName.trim(),
                                        server = signupServer.trim(),
                                        allianceName = signupAllianceTag.trim(),
                                        rank = signupRank,
                                        updatedAtIso = Instant.now().toString()
                                    )
                                    val signupWorkspace = savedAllianceAtStart.copy(
                                        server = signupServer.trim(),
                                        allianceName = signupAllianceTag.trim(),
                                        ownProfile = player
                                    )
                                    profilePrefs.edit()
                                        .putString("alliance_workspace", profileJson.encodeToString(signupWorkspace))
                                        .apply()
                                }
                                val result = if (createAccount) auth.signUp(email, password) else auth.signIn(email, password)
                                result.onSuccess { outcome ->
                                    session = outcome.session ?: auth.currentSession()
                                    message = outcome.message
                                    isError = false
                                    val activeSession = session
                                    val savedAlliance = runCatching {
                                        profileJson.decodeFromString<AllianceWorkspace>(
                                            profilePrefs.getString("alliance_workspace", "{}") ?: "{}"
                                        )
                                    }.getOrDefault(AllianceWorkspace())
                                    if (activeSession != null &&
                                        profilePrefs.getString("cloud_alliance_id", "").isNullOrBlank() &&
                                        savedAlliance.ownProfile.playerName.isNotBlank() &&
                                        savedAlliance.server.isNotBlank() &&
                                        savedAlliance.allianceName.isNotBlank()
                                    ) {
                                        val isInitialR5 = savedAlliance.ownProfile.rank == AllianceRank.R5
                                        val registration = if (isInitialR5) {
                                            auth.createAlliance(savedAlliance.server, savedAlliance.allianceName)
                                        } else {
                                            auth.submitAllianceJoinRequest(
                                                savedAlliance.server,
                                                savedAlliance.allianceName,
                                                savedAlliance.ownProfile.playerName,
                                                savedAlliance.ownProfile.rank.name
                                            )
                                        }
                                        registration.onSuccess { id ->
                                            if (isInitialR5) {
                                                profilePrefs.edit().putString("cloud_alliance_id", id).apply()
                                                message = localizedUiText(currentLang, "Compte connecté. Espace créé : tu es directement R5.", "Account connected. Workspace created: you are now R5.", "Konto verbunden. Bereich erstellt: Du bist jetzt R5.", "Hesap bağlandı. Alan oluşturuldu: artık R5'sin.", "Аккаунт подключён. Пространство создано: теперь вы R5.", "账户已连接。空间已创建：你现在是 R5。")
                                            } else {
                                                message = localizedUiText(currentLang, "Compte connecté. Demande envoyée aux R4/R5 de ${savedAlliance.allianceName}.", "Account connected. Request sent to the R4/R5 officers of ${savedAlliance.allianceName}.", "Konto verbunden. Anfrage an die R4/R5-Offiziere von ${savedAlliance.allianceName} gesendet.", "Hesap bağlandı. ${savedAlliance.allianceName} ittifakının R4/R5 yetkililerine istek gönderildi.", "Аккаунт подключён. Запрос отправлен офицерам R4/R5 альянса ${savedAlliance.allianceName}.", "账户已连接。申请已发送给 ${savedAlliance.allianceName} 联盟的 R4/R5 管理员。")
                                            }
                                        }.onFailure {
                                            message = localizedUiText(currentLang, "Compte connecté. Profil conservé ; inscription d'alliance impossible : ${it.message ?: "vérifie le serveur et le tag"}", "Account connected. Profile saved; alliance registration failed: ${it.message ?: "check the server and tag"}", "Konto verbunden. Profil gespeichert; Allianzregistrierung fehlgeschlagen: ${it.message ?: "Server und Tag prüfen"}", "Hesap bağlandı. Profil korundu; ittifak kaydı başarısız: ${it.message ?: "sunucuyu ve etiketi kontrol et"}", "Аккаунт подключён. Профиль сохранён; не удалось зарегистрировать альянс: ${it.message ?: "проверьте сервер и тег"}", "账户已连接。档案已保留；联盟注册失败：${it.message ?: "请检查服务器和标签"}")
                                            isError = true
                                        }
                                    }
                                }.onFailure {
                                    message = it.message ?: localizedUiText(currentLang, "Une erreur est survenue.", "An error occurred.", "Ein Fehler ist aufgetreten.", "Bir hata oluştu.", "Произошла ошибка.", "发生错误。")
                                    isError = true
                                }
                                busy = false
                            }
                        },
                        enabled = !busy && email.isNotBlank() && password.length >= 8 &&
                            (!createAccount || (signupPlayerName.isNotBlank() && signupServer.isNotBlank() && signupAllianceTag.isNotBlank()))
                    ) {
                        if (busy) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (createAccount) localizedAppText(localizedAppText("S'inscrire")) else localizedAppText(localizedAppText("Se connecter")))
                        }
                    }
                }
                if (email.contains("@")) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                busy = true
                                message = null
                                auth.resendSignupConfirmation(email)
                                    .onSuccess { message = it; isError = false }
                                    .onFailure { message = it.message ?: "Renvoi impossible."; isError = true }
                                busy = false
                            }
                        },
                        enabled = !busy
                    ) {
                        Text(localizedAppText(localizedAppText("Renvoyer l'e-mail de confirmation")))
                    }
                }
            }
            message?.let {
                Text(
                    it,
                    color = if (isError) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun AllianceScreen(
    language: AppLanguage,
    onChangeLanguage: (AppLanguage) -> Unit,
    workspace: AllianceWorkspace,
    heroes: List<HeroRecord>,
    robots: List<RobotRecord>,
    currentPowerText: String,
    onSaveWorkspace: (AllianceWorkspace) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cloudAuth = remember(context) { SupabaseAuthRepository(context) }
    val accountPrefs = remember(context) {
        context.getSharedPreferences("lands_of_jail_account_v1", Context.MODE_PRIVATE)
    }
    var cloudRosterBusy by remember { mutableStateOf(false) }
    var cloudRosterMessage by remember { mutableStateOf<String?>(null) }
    var cloudRosterError by remember { mutableStateOf(false) }
    var cloudCanManageRanks by remember { mutableStateOf(false) }
    var cloudMemberIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var expandedRankMenuFor by remember { mutableStateOf<String?>(null) }
    var rankChangeBusyFor by remember { mutableStateOf<String?>(null) }
    var rankChangeMessage by remember { mutableStateOf<String?>(null) }
    var rankChangeError by remember { mutableStateOf(false) }
    var cloudSyncBusy by remember { mutableStateOf(false) }
    var cloudSyncMessage by remember { mutableStateOf<String?>(null) }
    var cloudSyncError by remember { mutableStateOf(false) }
    var joinRequests by remember { mutableStateOf<List<SupabaseAuthRepository.AllianceJoinRequest>>(emptyList()) }
    var joinRequestsBusy by remember { mutableStateOf(false) }
    var joinRequestsMessage by remember { mutableStateOf<String?>(null) }
    var joinRequestBusy by remember { mutableStateOf(false) }
    var joinRequestError by remember { mutableStateOf(false) }
    var joinRequestMessage by remember { mutableStateOf<String?>(null) }
    var joinRequestsError by remember { mutableStateOf(false) }
    var playerName by remember(workspace.ownProfile.playerName) { mutableStateOf(workspace.ownProfile.playerName) }
    var server by remember(workspace.server) { mutableStateOf(workspace.server) }
    var allianceName by remember(workspace.allianceName) { mutableStateOf(workspace.allianceName) }
    var rank by remember(workspace.ownProfile.rank) { mutableStateOf(workspace.ownProfile.rank) }
    var ownRobotSummary by remember(workspace.ownProfile.robotSummary) { mutableStateOf(workspace.ownProfile.robotSummary) }
    var showAddMember by remember { mutableStateOf(false) }
    var memberName by remember { mutableStateOf("") }
    var memberPower by remember { mutableStateOf("") }
    var memberRank by remember { mutableStateOf(AllianceRank.R1) }
    var memberSkills by remember { mutableStateOf("") }
    var memberRobots by remember { mutableStateOf("") }
    var selectedEvent by remember { mutableStateOf(AdviceMode.TRIAL_CAGE) }
    var simulation by remember { mutableStateOf<List<String>>(emptyList()) }
    val isOfficer = rank == AllianceRank.R5 || rank == AllianceRank.R4
    val ownHeroSummary = remember(heroes) {
        heroes.sortedByDescending { it.competences.size }.joinToString("\n") { hero ->
            val skills = hero.competences.joinToString("; ") { skill ->
                listOf(skill.nom, skill.niveau, skill.description.orEmpty()).filter { it.isNotBlank() }.joinToString(" ")
            }
            "${hero.nom}: ${skills.ifBlank { "compétences non renseignées" }}"
        }
    }

    LaunchedEffect(isOfficer) {
        val linkedAllianceId = accountPrefs.getString("cloud_alliance_id", "").orEmpty()
        if (isOfficer && cloudAuth.currentSession() != null && linkedAllianceId.isNotBlank()) {
            cloudAuth.fetchPendingAllianceJoinRequests(linkedAllianceId).onSuccess { pending ->
                joinRequests = pending
                val previousIds = accountPrefs.getStringSet("notified_join_request_ids", emptySet()).orEmpty().toSet()
                val currentIds = pending.map { it.id }.toSet()
                val newCount = (currentIds - previousIds).size
                if (newCount > 0) notifyAllianceJoinRequests(context, newCount)
                accountPrefs.edit().putStringSet("notified_join_request_ids", previousIds + currentIds).apply()
            }
        }
    }

    LaunchedEffect(cloudAuth.currentSession() != null, accountPrefs.getString("cloud_alliance_id", "")) {
        val linkedAllianceId = accountPrefs.getString("cloud_alliance_id", "").orEmpty()
        if (cloudAuth.currentSession() != null && linkedAllianceId.isNotBlank()) {
            val developer = cloudAuth.isAppDeveloper().getOrDefault(false)
            val cloudRank = cloudAuth.fetchMyAllianceRank(linkedAllianceId).getOrNull()
            val canManage = cloudRank == "R5" || (developer && cloudRank == "R4")
            cloudCanManageRanks = canManage
            if (canManage) {
                cloudAuth.fetchAllianceRoster(linkedAllianceId).onSuccess { cloudMembers ->
                    cloudMemberIds = cloudMembers.map { it.id }.toSet()
                    val cloudNames = cloudMembers.map { it.playerName.lowercase() }.toSet()
                    val mergedMembers = cloudMembers.map { cloudMember ->
                        val local = workspace.members.firstOrNull {
                            it.playerName.equals(cloudMember.playerName, ignoreCase = true)
                        }
                        val resolved = if (cloudMember.playerName == "Profil non synchronisé" && local != null) {
                            cloudMember.copy(
                                playerName = local.playerName,
                                powerText = local.powerText,
                                heroSkillSummary = local.heroSkillSummary,
                                robotSummary = local.robotSummary
                            )
                        } else cloudMember
                        resolved.copy(server = server, allianceName = allianceName)
                    } + workspace.members.filterNot { it.playerName.lowercase() in cloudNames }
                    onSaveWorkspace(workspace.copy(members = mergedMembers))
                }
            }
        } else {
            cloudCanManageRanks = false
            cloudMemberIds = emptySet()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item { SupabaseAccountCard(onChangeLanguage = onChangeLanguage) }
        item { AllianceCloudControls(server = server, allianceName = allianceName) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(localizedAppText(localizedAppText("Demandes d'adhésion · R4 / R5")), fontWeight = FontWeight.Bold)
                    Text(
                        localizedAppText("Les demandes des joueurs de ton alliance apparaissent ici. Seuls les droits confirmés par un officier sont appliqués."),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            val allianceId = accountPrefs.getString("cloud_alliance_id", "").orEmpty()
                            if (!isOfficer) {
                                joinRequestsMessage = "La consultation est réservée aux R4/R5 validés par le serveur."
                                joinRequestsError = true
                            } else if (allianceId.isBlank()) {
                                joinRequestsMessage = "L'alliance cloud n'est pas encore liée. Rejoins-la ou crée son espace cloud."
                                joinRequestsError = true
                            } else {
                                scope.launch {
                                    joinRequestsBusy = true
                                    joinRequestsMessage = null
                                    cloudAuth.fetchPendingAllianceJoinRequests(allianceId)
                                        .onSuccess {
                                            joinRequests = it
                                            joinRequestsMessage = if (it.isEmpty()) "Aucune nouvelle demande." else "${it.size} demande(s) en attente."
                                            joinRequestsError = false
                                        }
                                        .onFailure {
                                            joinRequestsMessage = it.message ?: "Lecture des demandes impossible."
                                            joinRequestsError = true
                                        }
                                    joinRequestsBusy = false
                                }
                            }
                        },
                        enabled = !joinRequestsBusy,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (joinRequestsBusy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Outlined.NotificationsActive, contentDescription = null)
                        Spacer(Modifier.width(7.dp))
                        Text(if (joinRequestsBusy) localizedAppText(localizedAppText("Actualisation…")) else localizedAppText(localizedAppText("Voir les nouvelles demandes")))
                    }
                    joinRequestsMessage?.let {
                        Text(it, color = if (joinRequestsError) Color(0xFFFCA5A5) else Color(0xFF86EFAC), style = MaterialTheme.typography.bodySmall)
                    }
                    joinRequests.forEach { request ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202B3A))) {
                            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(request.playerName, fontWeight = FontWeight.Bold)
                                Text(localizedAppText(localizedAppText("Rang demandé : ${request.requestedRank} · ${request.createdAt.take(10)}")), style = MaterialTheme.typography.bodySmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                joinRequestsBusy = true
                                                val rankToGrant = if (request.requestedRank == "R5") "R4" else request.requestedRank
                                                cloudAuth.reviewAllianceJoinRequest(request.id, true, rankToGrant)
                                                    .onSuccess {
                                                        joinRequests = joinRequests.filterNot { it.id == request.id }
                                                        joinRequestsMessage = if (request.requestedRank == "R5") {
                                                            "Demande acceptée comme R4. Le R5 actuel peut ensuite lui transférer le poste."
                                                        } else {
                                                            "Demande acceptée. Rang $rankToGrant appliqué."
                                                        }
                                                        joinRequestsError = false
                                                    }
                                                    .onFailure {
                                                        joinRequestsMessage = it.message ?: "Validation impossible."
                                                        joinRequestsError = true
                                                    }
                                                joinRequestsBusy = false
                                            }
                                        },
                                        enabled = !joinRequestsBusy,
                                        modifier = Modifier.weight(1f)
                                    ) { Text(localizedAppText(localizedAppText("Accepter"))) }
                                    OutlinedButton(
                                        onClick = {
                                            scope.launch {
                                                joinRequestsBusy = true
                                                cloudAuth.reviewAllianceJoinRequest(request.id, false)
                                                    .onSuccess {
                                                        joinRequests = joinRequests.filterNot { it.id == request.id }
                                                        joinRequestsMessage = "Demande refusée."
                                                        joinRequestsError = false
                                                    }
                                                    .onFailure {
                                                        joinRequestsMessage = it.message ?: "Refus impossible."
                                                        joinRequestsError = true
                                                    }
                                                joinRequestsBusy = false
                                            }
                                        },
                                        enabled = !joinRequestsBusy,
                                        modifier = Modifier.weight(1f)
                                    ) { Text(localizedAppText(localizedAppText("Refuser"))) }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(localizedUiText(language, "Espace Alliance", "Alliance Space", "Allianzbereich", "İttifak Alanı", "Пространство альянса", "联盟空间"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    localizedUiText(language, "Ton profil vient de ton compte. Consulte le roster et prépare les événements avec les capacités de tes membres.", "Your profile comes from your account. Check the roster and prepare events using your members’ capabilities.", "Dein Profil stammt aus deinem Konto. Sieh dir die Mitgliederliste an und bereite Events anhand der Fähigkeiten deiner Mitglieder vor.", "Profilin hesabından gelir. Kadroyu incele ve üyelerinin yetenekleriyle etkinliklere hazırlan.", "Ваш профиль связан с аккаунтом. Просматривайте состав и готовьтесь к событиям с учётом возможностей участников.", "你的个人资料来自账号。查看成员名单，并根据成员能力准备活动。"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(localizedUiText(language, "Roster de l'alliance", "Alliance roster", "Allianzmitglieder", "İttifak kadrosu", "Состав альянса", "联盟成员名单"), fontWeight = FontWeight.Bold)
                        Text(localizedAppText(localizedAppText("${workspace.members.size + if (workspace.ownProfile.playerName.isNotBlank()) 1 else 0} joueur(s)")),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        localizedAppText("Serveur ${workspace.server.ifBlank { "—" }} · Alliance ${workspace.allianceName.ifBlank { "—" }}"),
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (workspace.ownProfile.playerName.isNotBlank()) {
                        Text(
                            localizedAppText("Toi : ${workspace.ownProfile.playerName} · ${workspace.ownProfile.rank.labelFor(language)} · ${heroes.size} héros analysés"),
                            color = Color(0xFF34D399),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (isOfficer) {
                        Button(onClick = { showAddMember = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.PersonAdd, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(localizedUiText(language, "Ajouter un membre au roster", "Add a member to the roster", "Mitglied zur Liste hinzufügen", "Kadorya üye ekle", "Добавить участника в состав", "添加联盟成员"))
                        }
                    } else {
                        Text(localizedUiText(language, "Demande à un R4/R5 d'ajouter les profils des membres.", "Ask an R4/R5 to add member profiles.", "Bitte einen R4/R5, die Mitgliederprofile hinzuzufügen.", "Üye profillerini eklemesi için bir R4/R5'ten yardım iste.", "Попросите R4/R5 добавить профили участников.", "请联系 R4/R5 添加成员资料。"), style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = {
                            val linkedAllianceId = accountPrefs.getString("cloud_alliance_id", "").orEmpty()
                            if (linkedAllianceId.isBlank()) {
                                cloudRosterMessage = "Crée ou rejoins d'abord une alliance cloud avec le code d'invitation."
                                cloudRosterError = true
                            } else {
                                scope.launch {
                                    cloudRosterBusy = true
                                    cloudRosterMessage = null
                                    cloudAuth.fetchAllianceRoster(linkedAllianceId)
                                        .onSuccess { cloudMembers ->
                                            cloudMemberIds = cloudMembers.map { it.id }.toSet()
                                            val cloudNames = cloudMembers.map { it.playerName.lowercase() }.toSet()
                                            val mergedMembers = cloudMembers.map { cloudMember ->
                                                val local = workspace.members.firstOrNull {
                                                    it.playerName.equals(cloudMember.playerName, ignoreCase = true)
                                                }
                                                val resolved = if (cloudMember.playerName == "Profil non synchronisé" && local != null) {
                                                    cloudMember.copy(
                                                        playerName = local.playerName,
                                                        powerText = local.powerText,
                                                        heroSkillSummary = local.heroSkillSummary,
                                                        robotSummary = local.robotSummary
                                                    )
                                                } else cloudMember
                                                resolved.copy(server = workspace.server, allianceName = workspace.allianceName)
                                            } + workspace.members.filterNot { local ->
                                                cloudNames.contains(local.playerName.lowercase())
                                            }
                                            onSaveWorkspace(workspace.copy(members = mergedMembers))
                                            cloudRosterMessage = "Roster cloud actualisé : ${cloudMembers.size} membre(s) récupéré(s). Les profils masqués par les droits de rang restent non détaillés."
                                            cloudRosterError = false
                                            simulation = emptyList()
                                        }
                                        .onFailure {
                                            cloudRosterMessage = it.message ?: "Chargement du roster impossible."
                                            cloudRosterError = true
                                        }
                                    cloudRosterBusy = false
                                }
                            }
                        },
                        enabled = !cloudRosterBusy && cloudAuth.currentSession() != null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (cloudRosterBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (cloudRosterBusy) localizedUiText(language, "Chargement de la liste…", "Loading roster…", "Liste wird geladen…", "Liste yükleniyor…", "Загрузка списка…", "正在加载成员列表…") else localizedUiText(language, "Actualiser depuis le cloud", "Refresh from cloud", "Aus der Cloud aktualisieren", "Buluttan yenile", "Обновить из облака", "从云端刷新"))
                    }
                    cloudRosterMessage?.let {
                        Text(
                            it,
                            color = if (cloudRosterError) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    rankChangeMessage?.let {
                        Text(
                            it,
                            color = if (rankChangeError) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
        items(workspace.members, key = { it.id }) { member ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(member.playerName, fontWeight = FontWeight.SemiBold)
                        Text(member.rank.labelFor(language), color = Color(0xFFFFB36B), style = MaterialTheme.typography.labelSmall)
                    }
                    val memberPowerLabel = localizedAppText(localizedAppText("Puissance :")) + " " + member.powerText.ifBlank { localizedAppText(localizedAppText("non renseignée")) }
                    Text(memberPowerLabel, style = MaterialTheme.typography.bodySmall)
                    if (member.heroSkillSummary.isNotBlank()) Text(localizedAppText(localizedAppText("Héros / compétences : ${member.heroSkillSummary}")), style = MaterialTheme.typography.bodySmall)
                    if (member.robotSummary.isNotBlank()) Text(localizedAppText(localizedAppText("Robot : ${member.robotSummary}")), style = MaterialTheme.typography.bodySmall)
                    if (cloudCanManageRanks && member.id in cloudMemberIds) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                OutlinedButton(
                                    onClick = { expandedRankMenuFor = member.id },
                                    enabled = rankChangeBusyFor == null,
                                    modifier = Modifier.heightIn(min = 36.dp)
                                ) {
                                    Text(if (rankChangeBusyFor == member.id) localizedAppText(localizedAppText("Mise à jour…")) else localizedAppText(localizedAppText("Changer le rang · ${member.rank.name}")))
                                }
                                DropdownMenu(
                                    expanded = expandedRankMenuFor == member.id,
                                    onDismissRequest = { expandedRankMenuFor = null }
                                ) {
                                    AllianceRank.entries
                                        .filter { it != member.rank && (it != AllianceRank.R5 || member.rank == AllianceRank.R4) }
                                        .forEach { newRank ->
                                            DropdownMenuItem(
                                                text = { Text(newRank.name) },
                                                onClick = {
                                                    expandedRankMenuFor = null
                                                    val linkedAllianceId = accountPrefs.getString("cloud_alliance_id", "").orEmpty()
                                                    scope.launch {
                                                        rankChangeBusyFor = member.id
                                                        rankChangeMessage = null
                                                        cloudAuth.setAllianceMemberRank(linkedAllianceId, member.id, newRank.name)
                                                            .onSuccess {
                                                                val developer = cloudAuth.isAppDeveloper().getOrDefault(false)
                                                                val myCloudRank = cloudAuth.fetchMyAllianceRank(linkedAllianceId).getOrNull()
                                                                cloudCanManageRanks = myCloudRank == "R5" || (developer && myCloudRank == "R4")
                                                                cloudAuth.fetchAllianceRoster(linkedAllianceId).onSuccess { latest ->
                                                                    cloudMemberIds = latest.map { it.id }.toSet()
                                                                    val latestById = latest.associateBy { it.id }
                                                                    onSaveWorkspace(workspace.copy(
                                                                        members = workspace.members.map { existing ->
                                                                            latestById[existing.id]?.let { existing.copy(rank = it.rank) } ?: existing
                                                                        }
                                                                    ))
                                                                }
                                                                rankChangeMessage = "${member.playerName} est maintenant ${newRank.name}."
                                                                rankChangeError = false
                                                            }
                                                            .onFailure {
                                                                rankChangeMessage = it.message ?: "Modification du rang impossible."
                                                                rankChangeError = true
                                                            }
                                                        rankChangeBusyFor = null
                                                    }
                                                }
                                            )
                                        }
                                }
                            }
                            if (member.rank == AllianceRank.R5) {
                                Text(localizedAppText(localizedAppText("Le R5 se remplace en nommant un R4.")), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (isOfficer) {
                        TextButton(onClick = {
                            onSaveWorkspace(workspace.copy(members = workspace.members.filterNot { it.id == member.id }))
                            simulation = emptyList()
                        }) { Text(localizedAppText(localizedAppText("Retirer du roster"))) }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(localizedUiText(language, "Simulateur de performance", "Performance simulator", "Leistungssimulator", "Performans simülatörü", "Симулятор эффективности", "战力模拟器"), fontWeight = FontWeight.Bold)
                    Text(
                        localizedUiText(language, "Compare les profils disponibles et propose une répartition des rôles. Les données manquantes ne sont pas inventées.", "Compares available profiles and suggests role assignments. Missing data is never invented.", "Vergleicht verfügbare Profile und schlägt Rollenverteilungen vor. Fehlende Daten werden nicht erfunden.", "Mevcut profilleri karşılaştırır ve rol dağılımı önerir. Eksik veriler uydurulmaz.", "Сравнивает доступные профили и предлагает распределение ролей. Отсутствующие данные не выдумываются.", "比较现有档案并建议角色分配，不会编造缺失数据。"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(AdviceMode.entries) { event ->
                            FilterChip(
                                selected = selectedEvent == event,
                                onClick = { selectedEvent = event; simulation = emptyList() },
                                label = { Text(event.titleFor(language), maxLines = 1) }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val roster = buildList {
                                if (workspace.ownProfile.playerName.isNotBlank()) {
                                    val robotInventorySummary = robots.joinToString("; ") { robot ->
                                        listOf(
                                            robot.nom,
                                            robot.niveau.takeIf { it.isNotBlank() }?.let { "niv. $it" },
                                            robot.bonus,
                                            robot.competences
                                        ).filterNotNull().filter { it.isNotBlank() }.joinToString(" ")
                                    }
                                    add(workspace.ownProfile.copy(
                                        server = workspace.server,
                                        allianceName = workspace.allianceName,
                                        powerText = currentPowerText.ifBlank { workspace.ownProfile.powerText },
                                        heroSkillSummary = ownHeroSummary,
                                        robotSummary = robotInventorySummary.ifBlank { workspace.ownProfile.robotSummary }
                                    ))
                                }
                                addAll(workspace.members)
                            }
                            if (roster.isEmpty()) {
                                simulation = listOf("Aucun joueur enregistré. Inscris ton profil et ajoute les membres.")
                            } else {
                                val power = { p: AlliancePlayerProfile ->
                                    p.powerText.filter(Char::isDigit).toLongOrNull() ?: 0L
                                }
                                val lower = { p: AlliancePlayerProfile ->
                                    (p.heroSkillSummary + " " + p.robotSummary).lowercase()
                                }
                                val cageTags = listOf(
                                    "def reduction" to listOf("réduction de def", "baisse de def", "def ennemie", "defense reduction", "def reduction"),
                                    "dégâts" to listOf("dégât", "damage", "dmg"),
                                    "attaque" to listOf("attaque", "atk", "attack"),
                                    "létalité" to listOf("létalité", "lethality", "letalite"),
                                    "critique" to listOf("critique", "critical", "crit")
                                )
                                val turmoilTags = listOf(
                                    "défense" to listOf("défense", "defense", "def"),
                                    "points de vie" to listOf("pv", "hp", "santé", "health"),
                                    "renfort" to listOf("renfort", "reinforce", "garnison", "garrison"),
                                    "contrôle" to listOf("contrôle", "control", "étourdissement", "stun"),
                                    "attaque" to listOf("attaque", "atk", "attack", "dégât", "damage")
                                )
                                val tags: List<Pair<String, List<String>>> = when (selectedEvent) {
                                    AdviceMode.TRIAL_CAGE -> cageTags
                                    AdviceMode.TURMOIL_SURGE -> turmoilTags
                                    else -> listOf(
                                        "attaque / dégâts" to listOf("attaque", "atk", "attack", "dégât", "damage"),
                                        "défense / résistance" to listOf("défense", "defense", "résistance", "resistance", "hp", "pv"),
                                        "contrôle / soutien" to listOf("contrôle", "control", "soin", "heal", "stun", "étourdissement"),
                                        "mobilité / tactique" to listOf("mobilité", "movement", "vitesse", "speed", "tactique", "tactical")
                                    )
                                }
                                val leader: AlliancePlayerProfile? = roster.maxByOrNull { candidate: AlliancePlayerProfile -> power(candidate) }
                                val joiners: List<AlliancePlayerProfile> = roster.filter { candidate: AlliancePlayerProfile -> candidate.id != leader?.id }
                                    .sortedByDescending { member: AlliancePlayerProfile ->
                                        val memberText: String = lower(member)
                                        val matchedTags: Int = tags.fold(0) { total: Int, tag: Pair<String, List<String>> ->
                                            total + if (tag.second.any { word: String -> memberText.contains(word) }) 10 else 0
                                        }
                                        matchedTags + (power(member).coerceAtMost(9_999_999_999L) / 1_000_000_000L).toInt()
                                    }
                                val output = mutableListOf<String>()
                                output += "Événement : ${selectedEvent.titleFor(language)}"
                                output += "Effectif analysé : ${roster.size} membre(s)"
                                leader?.let {
                                    output += "LEADER proposé : ${it.playerName} (${it.rank.labelFor(language)}) — puissance ${it.powerText.ifBlank { "non renseignée" }}"
                                    if (it.heroSkillSummary.isNotBlank()) output += "Capacités du leader : ${it.heroSkillSummary.take(420)}"
                                    if (it.robotSummary.isNotBlank()) output += "Robot du leader : ${it.robotSummary}"
                                }
                                val used = mutableSetOf<String>()
                                joiners.forEach { member ->
                                    val text = lower(member)
                                    val tag = tags.firstOrNull { (tagName, words) ->
                                        words.any(text::contains) && itTagNotUsed(used, tagName)
                                    }?.first
                                    if (tag != null && used.add(tag)) {
                                        output += "Soutien ${used.size} — ${member.playerName} : rôle ${tag}; ${member.heroSkillSummary.take(220)}"
                                    }
                                    if (used.size >= 4) return@forEach
                                }
                                if (used.isEmpty()) {
                                    output += "Aucun bonus distinct identifiable : complète les compétences War et les données du robot pour obtenir une répartition fiable."
                                }
                                output += "À confirmer : simulation basée sur les informations saisies/scannées, pas sur des tests de combat de cette alliance."
                                simulation = output
                            }
                        },
                        enabled = isOfficer && (workspace.ownProfile.playerName.isNotBlank() || workspace.members.isNotEmpty()),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(localizedAppText(localizedAppText("Lancer la simulation (R4 / R5)")))
                    }
                    simulation.forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodySmall, color = if (line.startsWith("À confirmer")) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        item {
            Text(
                localizedAppText("Les profils synchronisés sont chargés depuis le cloud selon les droits de visibilité du rang dans l'alliance. Les ajouts manuels effectués ici restent enregistrés localement sur cet appareil."),
                color = Color(0xFFFBBF24),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showAddMember) {
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            title = { Text(localizedAppText(localizedAppText("Ajouter un membre"))) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(memberName, { memberName = it }, label = { Text(localizedAppText(localizedAppText("Pseudo du joueur"))) }, singleLine = true)
                    OutlinedTextField(memberPower, { memberPower = it }, label = { Text(localizedAppText(localizedAppText("Puissance (facultatif)"))) }, singleLine = true)
                    Text(localizedAppText(localizedAppText("Rang dans l'alliance")), style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        items(AllianceRank.entries) { item ->
                            FilterChip(selected = memberRank == item, onClick = { memberRank = item }, label = { Text(item.name) })
                        }
                    }
                    OutlinedTextField(
                        memberSkills, { memberSkills = it },
                        label = { Text(localizedAppText(localizedAppText("Héros et compétences War (copier les infos)"))) },
                        minLines = 3, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        memberRobots, { memberRobots = it },
                        label = { Text(localizedAppText(localizedAppText("Robot / bonus (facultatif)"))) },
                        minLines = 2, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newMember = AlliancePlayerProfile(
                            id = UUID.randomUUID().toString(),
                            playerName = memberName.trim(),
                            server = workspace.server,
                            allianceName = workspace.allianceName,
                            rank = memberRank,
                            powerText = memberPower.trim(),
                            heroSkillSummary = memberSkills.trim(),
                            robotSummary = memberRobots.trim(),
                            updatedAtIso = Instant.now().toString()
                        )
                        onSaveWorkspace(workspace.copy(members = workspace.members.filterNot { it.playerName.equals(newMember.playerName, true) } + newMember))
                        memberName = ""; memberPower = ""; memberSkills = ""; memberRobots = ""
                        memberRank = AllianceRank.R1
                        showAddMember = false
                        simulation = emptyList()
                    },
                    enabled = memberName.isNotBlank() && isOfficer
                ) { Text(localizedAppText(localizedAppText("Enregistrer"))) }
            },
            dismissButton = {
                TextButton(onClick = { showAddMember = false }) { Text(localizedAppText(localizedAppText("Annuler"))) }
            }
        )
    }
}

private fun itTagNotUsed(used: Set<String>, tag: String): Boolean = tag !in used

private fun notifyImprovementUpdate(context: Context, title: String, status: String) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "app_improvement_updates"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Réponses aux propositions", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
    val statusText = when (status) {
        "accepted" -> "Ta proposition a été acceptée."
        "rejected" -> "Ta proposition a été refusée."
        "needs_info" -> "Le développeur demande des précisions."
        "in_progress" -> "Ta proposition est en cours de développement."
        "implemented" -> "L'amélioration est maintenant intégrée."
        else -> "Le statut de ta proposition a changé."
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Réponse à : $title")
        .setContentText(statusText)
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
    manager.notify((title + status).hashCode(), notification)
}

private fun notifyAllianceJoinRequests(context: Context, count: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    ) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channelId = "alliance_join_requests"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        manager.createNotificationChannel(
            NotificationChannel(channelId, "Demandes de l'alliance", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Nouvelle demande dans ton alliance")
        .setContentText(if (count == 1) "1 joueur attend la validation d'un R4/R5." else "$count joueurs attendent la validation d'un R4/R5.")
        .setAutoCancel(true)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
    manager.notify(7301, notification)
}



@Composable
private fun AllianceCloudControls(server: String, allianceName: String) {
    val currentLang = LocalAssistantLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember(context) { SupabaseAuthRepository(context) }
    val prefs = remember(context) {
        context.getSharedPreferences("lands_of_jail_account_v1", Context.MODE_PRIVATE)
    }
    var allianceId by remember {
        mutableStateOf(prefs.getString("cloud_alliance_id", "").orEmpty())
    }
    var inviteInput by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var myCloudRank by remember { mutableStateOf("") }
    var r4Members by remember { mutableStateOf(emptyList<AlliancePlayerProfile>()) }
    var selectedR4 by remember { mutableStateOf<AlliancePlayerProfile?>(null) }
    var transferBusy by remember { mutableStateOf(false) }
    var transferMessage by remember { mutableStateOf<String?>(null) }
    var transferError by remember { mutableStateOf(false) }
    var isAppDeveloper by remember { mutableStateOf(false) }
    val signedIn = auth.currentSession() != null

    LaunchedEffect(allianceId, signedIn) {
        val developer = if (signedIn) {
            auth.isAppDeveloper().getOrDefault(false)
        } else {
            false
        }
        isAppDeveloper = developer
        if (signedIn && allianceId.isNotBlank()) {
            auth.fetchMyAllianceRank(allianceId).onSuccess { currentRank ->
                myCloudRank = currentRank.orEmpty()
                if (currentRank == "R5" || (developer && currentRank == "R4")) {
                    auth.fetchAllianceRoster(allianceId).onSuccess { roster ->
                        r4Members = roster.filter { it.rank == AllianceRank.R4 }
                    }
                } else {
                    r4Members = emptyList()
                }
            }
        } else {
            myCloudRank = ""
            r4Members = emptyList()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF172333))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(localizedAppText(localizedAppText("Alliance partagée en ligne")), fontWeight = FontWeight.Bold)
            Text(
                localizedAppText("Crée l'espace cloud de ton alliance ou rejoins celui d'un autre joueur avec un code d'invitation. Le rang R5 du créateur est attribué côté serveur."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!signedIn) {
                Text(localizedAppText(localizedAppText("Connecte-toi d'abord dans « Compte & sauvegarde cloud » pour utiliser ces fonctions.")),
                    color = Color(0xFFFBBF24), style = MaterialTheme.typography.bodySmall)
            } else {
                if (allianceId.isBlank()) {
                    OutlinedButton(
                        onClick = {
                            allianceId = prefs.getString("cloud_alliance_id", "").orEmpty()
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(localizedAppText(localizedAppText("Actualiser le lien cloud après connexion"))) }
                    Button(
                        onClick = {
                            scope.launch {
                                busy = true
                                message = null
                                auth.createAlliance(server, allianceName)
                                    .onSuccess { id ->
                                        allianceId = id
                                        prefs.edit().putString("cloud_alliance_id", id).apply()
                                        message = localizedUiText(currentLang, "Alliance créée en ligne. Tu es enregistré comme R5.", "Alliance created online. You are registered as R5.", "Allianz online erstellt. Du bist als R5 registriert.", "İttifak çevrimiçi oluşturuldu. R5 olarak kaydedildin.", "Альянс создан онлайн. Вы зарегистрированы как R5.", "联盟已在线创建。你已登记为 R5。")
                                        isError = false
                                    }
                                    .onFailure {
                                        message = it.message ?: localizedUiText(currentLang, "Création impossible.", "Could not create.", "Erstellen nicht möglich.", "Oluşturulamadı.", "Не удалось создать.", "无法创建。")
                                        isError = true
                                    }
                                busy = false
                            }
                        },
                        enabled = !busy && server.isNotBlank() && allianceName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (busy) localizedAppText(localizedAppText("Création…")) else localizedAppText(localizedAppText("Créer cette alliance dans le cloud"))) }
                } else {
                    Text(localizedAppText(localizedAppText("Alliance cloud liée : $allianceId")), style = MaterialTheme.typography.bodySmall)
                    if (myCloudRank.isNotBlank()) {
                        Text(localizedAppText(localizedAppText("Ton rang confirmé dans le cloud : $myCloudRank")), color = Color(0xFF86EFAC), style = MaterialTheme.typography.bodySmall)
                    }
                    if (myCloudRank == "R5" || (isAppDeveloper && myCloudRank == "R4")) {
                        Divider()
                        Text(
                            if (isAppDeveloper && myCloudRank == "R4") {
                                localizedUiText(currentLang, "Privilèges développeur · Gestion R5", "Developer privileges · R5 management", "Entwicklerrechte · R5-Verwaltung", "Geliştirici ayrıcalıkları · R5 yönetimi", "Права разработчика · управление R5", "开发者权限 · R5 管理")
                            } else {
                                localizedUiText(currentLang, "Transférer le poste de R5", "Transfer R5 leadership", "R5-Führung übertragen", "R5 liderliğini devret", "Передать руководство R5", "转让 R5 职位")
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (isAppDeveloper && myCloudRank == "R4") {
                                localizedUiText(currentLang, "Ton rang d'alliance reste R4. En tant que développeur unique de l'application, tu peux aussi gérer les fonctions R5. Choisis un membre R4 pour lui transférer le poste R5.", "Your alliance rank remains R4. As the app's sole developer, you can also manage R5 functions. Choose an R4 member to transfer R5 leadership to them.", "Dein Allianzrang bleibt R4. Als alleiniger App-Entwickler kannst du auch R5-Funktionen verwalten. Wähle ein R4-Mitglied, dem du die R5-Führung überträgst.", "İttifak rütben R4 olarak kalır. Uygulamanın tek geliştiricisi olarak R5 işlevlerini de yönetebilirsin. R5 liderliğini devretmek için bir R4 üyesi seç.", "Ваш ранг в альянсе останется R4. Как единственный разработчик приложения, вы также можете управлять функциями R5. Выберите участника R4 для передачи руководства.", "你的联盟职位仍为 R4。作为应用唯一开发者，你也可以管理 R5 功能。选择一名 R4 成员，将 R5 职位转交给他。")
                            } else {
                                localizedUiText(currentLang, "Choisis un membre déjà R4. Après confirmation, il deviendra R5 et ton compte passera R4.", "Choose an existing R4 member. After confirmation, they become R5 and your account moves to R4.", "Wähle ein bestehendes R4-Mitglied. Nach der Bestätigung wird es R5 und dein Konto wechselt zu R4.", "Mevcut bir R4 üyesi seç. Onaydan sonra R5 olur ve hesabın R4'e geçer.", "Выберите действующего участника R4. После подтверждения он станет R5, а ваш аккаунт перейдёт на R4.", "选择一名现有 R4 成员。确认后，该成员将成为 R5，你的账号将变为 R4。")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    transferBusy = true
                                    auth.fetchAllianceRoster(allianceId).onSuccess { roster ->
                                        r4Members = roster.filter { it.rank == AllianceRank.R4 }
                                        transferMessage = localizedUiText(currentLang, "Liste des R4 actualisée.", "R4 list refreshed.", "R4-Liste aktualisiert.", "R4 listesi yenilendi.", "Список R4 обновлён.", "R4 列表已刷新。")
                                        transferError = false
                                    }.onFailure {
                                        transferMessage = it.message ?: localizedUiText(currentLang, "Chargement des R4 impossible.", "Could not load R4 members.", "R4-Mitglieder konnten nicht geladen werden.", "R4 üyeleri yüklenemedi.", "Не удалось загрузить участников R4.", "无法加载 R4 成员。")
                                        transferError = true
                                    }
                                    transferBusy = false
                                }
                            },
                            enabled = !transferBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(localizedAppText(localizedAppText("Actualiser la liste des R4"))) }
                        if (r4Members.isEmpty()) {
                            Text(localizedAppText(localizedAppText("Aucun membre R4 disponible dans la liste cloud.")), style = MaterialTheme.typography.bodySmall)
                        } else {
                            r4Members.forEach { member ->
                                OutlinedButton(
                                    onClick = { selectedR4 = member },
                                    enabled = !transferBusy,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(localizedAppText(localizedAppText("${member.playerName} · R4")))
                                }
                            }
                        }
                        transferMessage?.let {
                            Text(it, color = if (transferError) Color(0xFFFCA5A5) else Color(0xFF86EFAC), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                busy = true
                                message = null
                                auth.createAllianceInvite(allianceId, 105)
                                    .onSuccess {
                                        inviteCode = it
                                        message = localizedUiText(currentLang, "Code créé. Transmets-le uniquement aux membres de ton alliance.", "Code created. Share it only with members of your alliance.", "Code erstellt. Teile ihn nur mit Mitgliedern deiner Allianz.", "Kod oluşturuldu. Yalnızca ittifak üyelerinle paylaş.", "Код создан. Передавайте его только участникам своего альянса.", "邀请码已创建。请仅与联盟成员分享。")
                                        isError = false
                                    }
                                    .onFailure {
                                        message = it.message ?: localizedUiText(currentLang, "Création du code impossible.", "Could not create the code.", "Code konnte nicht erstellt werden.", "Kod oluşturulamadı.", "Не удалось создать код.", "无法创建邀请码。")
                                        isError = true
                                    }
                                busy = false
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (busy) localizedAppText(localizedAppText("Génération…")) else localizedAppText(localizedAppText("Générer un code d'invitation (105 membres max.)"))) }
                }
                if (inviteCode.isNotBlank()) {
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(localizedAppText(localizedAppText("Code d'invitation"))) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = inviteInput,
                    onValueChange = { inviteInput = it.trim().take(256) },
                    label = { Text(localizedAppText(localizedAppText("Code reçu pour rejoindre une alliance"))) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            message = null
                            auth.acceptAllianceInvite(inviteInput)
                                .onSuccess { id ->
                                    allianceId = id
                                    prefs.edit().putString("cloud_alliance_id", id).apply()
                                    inviteCode = ""
                                    message = localizedUiText(currentLang, "Invitation acceptée. Ton compte a rejoint l'alliance.", "Invitation accepted. Your account has joined the alliance.", "Einladung angenommen. Dein Konto ist der Allianz beigetreten.", "Davet kabul edildi. Hesabın ittifaka katıldı.", "Приглашение принято. Ваш аккаунт вступил в альянс.", "邀请已接受。你的账户已加入联盟。")
                                    isError = false
                                }
                                .onFailure {
                                    message = it.message ?: localizedUiText(currentLang, "Invitation refusée.", "Invitation declined.", "Einladung abgelehnt.", "Davet reddedildi.", "Приглашение отклонено.", "邀请已拒绝。")
                                    isError = true
                                }
                            busy = false
                        }
                    },
                    enabled = !busy && inviteInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (busy) localizedAppText(localizedAppText("Vérification…")) else localizedAppText(localizedAppText("Rejoindre avec ce code"))) }
            }
            message?.let {
                Text(it, color = if (isError) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    selectedR4?.let { candidate ->
        AlertDialog(
            onDismissRequest = { if (!transferBusy) selectedR4 = null },
            title = { Text(localizedUiText(currentLang, "Confirmer le transfert du R5", "Confirm R5 leadership transfer", "Übertragung der R5-Führung bestätigen", "R5 liderlik devrini onayla", "Подтвердить передачу руководства R5", "确认转让 R5 职位")) },
            text = {
                Text(localizedUiText(currentLang, "Confirmer que ${candidate.playerName} devient R5 ? Ton propre rang passera automatiquement à R4.", "Confirm that ${candidate.playerName} becomes R5? Your rank will automatically move to R4.", "Bestätigen, dass ${candidate.playerName} R5 wird? Dein Rang wechselt automatisch zu R4.", " ${candidate.playerName} R5 olsun mu? Senin rütben otomatik olarak R4'e düşecek.", "Подтвердить, что ${candidate.playerName} станет R5? Ваш ранг автоматически изменится на R4.", "确认让 ${candidate.playerName} 成为 R5？你的职位将自动变为 R4。"))
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            transferBusy = true
                            transferMessage = null
                            auth.transferAllianceR5(allianceId, candidate.id)
                                .onSuccess {
                                    myCloudRank = "R4"
                                    r4Members = emptyList()
                                    transferMessage = localizedUiText(currentLang, "Transfert effectué. ${candidate.playerName} est le nouveau R5.", "Transfer complete. ${candidate.playerName} is the new R5.", "Übertragung abgeschlossen. ${candidate.playerName} ist der neue R5.", "Devir tamamlandı. Yeni R5: ${candidate.playerName}.", "Передача завершена. ${candidate.playerName} теперь R5.", "转让完成。${candidate.playerName} 现在是新的 R5。")
                                    transferError = false
                                    selectedR4 = null
                                }
                                .onFailure {
                                    transferMessage = it.message ?: localizedUiText(currentLang, "Transfert impossible.", "Transfer failed.", "Übertragung fehlgeschlagen.", "Devir başarısız.", "Не удалось передать руководство.", "转让失败。")
                                    transferError = true
                                }
                            transferBusy = false
                        }
                    },
                    enabled = !transferBusy
                ) { Text(if (transferBusy) localizedAppText(localizedAppText("Transfert…")) else localizedAppText(localizedAppText("Confirmer le transfert"))) }
            },
            dismissButton = {
                TextButton(onClick = { selectedR4 = null }, enabled = !transferBusy) { Text(localizedAppText(localizedAppText("Annuler"))) }
            }
        )
    }
}

@Composable
private fun ImprovementRequestsScreen(auth: SupabaseAuthRepository) {
    val currentLang = LocalAssistantLanguage.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember(context) { context.getSharedPreferences("lands_of_jail_account_v1", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val session = auth.currentSession()
    var isDeveloper by remember { mutableStateOf(false) }
    var requests by remember { mutableStateOf<List<SupabaseAuthRepository.ImprovementRequest>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("idea") }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<SupabaseAuthRepository.ImprovementRequest?>(null) }
    var reply by remember { mutableStateOf("") }
    var reviewBusy by remember { mutableStateOf(false) }

    fun refresh() {
        scope.launch {
            loading = true
            auth.isAppDeveloper().onSuccess { isDeveloper = it }
            auth.fetchAppImprovementRequests().onSuccess { updated ->
                if (!isDeveloper) {
                    updated.forEach { request ->
                        val key = "improvement_status_${request.id}"
                        val previous = prefs.getString(key, null)
                        if (previous != null && previous != request.status) {
                            notifyImprovementUpdate(context, request.title, request.status)
                        }
                        prefs.edit().putString(key, request.status).apply()
                    }
                }
                requests = updated
            }.onFailure { message = it.message ?: localizedUiText(currentLang, "Chargement des propositions impossible.", "Could not load suggestions.", "Vorschläge konnten nicht geladen werden.", "Öneriler yüklenemedi.", "Не удалось загрузить предложения.", "无法加载建议。"); isError = true }
            loading = false
        }
    }

    DisposableEffect(session?.user_id, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && session != null) refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(session?.user_id) {
        if (session != null) refresh()
    }

    val categoryLabels = mapOf(
        "bug" to localizedUiText(currentLang, "Bug / problème", "Bug / issue", "Fehler / Problem", "Hata / sorun", "Ошибка / проблема", "错误 / 问题"),
        "idea" to localizedUiText(currentLang, "Nouvelle idée", "New idea", "Neue Idee", "Yeni fikir", "Новая идея", "新想法"),
        "game_data" to localizedUiText(currentLang, "Données du jeu", "Game data", "Spieldaten", "Oyun verileri", "Данные игры", "游戏数据"),
        "interface" to localizedUiText(currentLang, "Interface", "Interface", "Oberfläche", "Arayüz", "Интерфейс", "界面"),
        "performance" to localizedUiText(currentLang, "Performance", "Performance", "Leistung", "Performans", "Производительность", "性能"),
        "other" to localizedUiText(currentLang, "Autre", "Other", "Sonstiges", "Diğer", "Другое", "其他")
    )
    val statusLabels = mapOf(
        "pending" to localizedUiText(currentLang, "En attente", "Pending", "Ausstehend", "Beklemede", "Ожидает", "待处理"),
        "accepted" to localizedUiText(currentLang, "Acceptée", "Accepted", "Angenommen", "Kabul edildi", "Принято", "已接受"),
        "rejected" to localizedUiText(currentLang, "Refusée", "Rejected", "Abgelehnt", "Reddedildi", "Отклонено", "已拒绝"),
        "needs_info" to localizedUiText(currentLang, "Précisions demandées", "More information needed", "Weitere Informationen erforderlich", "Ek bilgi gerekli", "Требуется уточнение", "需要补充信息"),
        "in_progress" to localizedUiText(currentLang, "En développement", "In progress", "In Entwicklung", "Geliştiriliyor", "В разработке", "开发中"),
        "implemented" to localizedUiText(currentLang, "Installée dans l'application", "Implemented in the app", "In der App umgesetzt", "Uygulamaya eklendi", "Добавлено в приложение", "已在应用中实现")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151C27)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(localizedAppText(localizedAppText("Proposer une amélioration")), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        localizedAppText("Partage une idée, signale un bug ou propose une amélioration. Tu pourras suivre la réponse du développeur ici."),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB8C3D1)
                    )
                    if (session == null) {
                        Text(localizedAppText(localizedAppText("Connecte-toi à ton compte pour envoyer une proposition.")), color = Color(0xFFFBBF24))
                    } else {
                        OutlinedTextField(
                            value = title, onValueChange = { title = it.take(120) },
                            label = { Text(localizedAppText(localizedAppText("Titre de la proposition"))) },
                            supportingText = { Text(localizedAppText(localizedAppText("${title.length}/120"))) },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(categoryLabels[category] ?: localizedAppText(localizedAppText("Catégorie")))
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                categoryLabels.forEach { (key, label) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = { category = key; expanded = false })
                                }
                            }
                        }
                        OutlinedTextField(
                            value = description, onValueChange = { description = it.take(4000) },
                            label = { Text(localizedAppText(localizedAppText("Décris ton idée ou le problème"))) },
                            supportingText = { Text(localizedAppText(localizedAppText("${description.length}/4000 — ajoute des exemples si possible."))) },
                            minLines = 4, modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    sending = true
                                    message = null
                                    auth.submitAppImprovement(
                                        title, description, category,
                                        aiSummary = "",
                                        aiCategory = category,
                                        aiQuestions = emptyList()
                                    ).onSuccess {
                                        title = ""
                                        description = ""
                                        message = localizedUiText(currentLang, "Proposition envoyée. Tu recevras la réponse dans cette rubrique.", "Suggestion sent. You will receive the response in this section.", "Vorschlag gesendet. Du erhältst die Antwort in diesem Bereich.", "Öneri gönderildi. Yanıtı bu bölümde alacaksın.", "Предложение отправлено. Ответ появится в этом разделе.", "建议已发送。你将在此栏目收到回复。")
                                        isError = false
                                        refresh()
                                    }.onFailure {
                                        message = it.message ?: localizedUiText(currentLang, "Envoi impossible.", "Could not send.", "Senden nicht möglich.", "Gönderilemedi.", "Не удалось отправить.", "无法发送。")
                                        isError = true
                                    }
                                    sending = false
                                }
                            },
                            enabled = !sending && title.trim().length >= 5 && description.trim().length >= 15,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (sending) CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
                            else Text(localizedAppText(localizedAppText("Envoyer ma proposition")))
                        }
                    }
                    message?.let { Text(it, color = if (isError) Color(0xFFFCA5A5) else Color(0xFF86EFAC), style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isDeveloper) localizedAppText(localizedAppText("Toutes les propositions · Développeur")) else localizedAppText(localizedAppText("Mes propositions")), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { refresh() }, enabled = !loading) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text(localizedAppText(localizedAppText("Actualiser")))
                }
            }
        }

        if (session != null && requests.isEmpty() && !loading) {
            item { Text(localizedAppText(localizedAppText("Aucune proposition pour le moment.")), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }

        items(requests, key = { it.id }) { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151C27)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(statusLabels[item.status] ?: item.status, color = when (item.status) {
                            "accepted", "implemented" -> Color(0xFF86EFAC)
                            "rejected" -> Color(0xFFFCA5A5)
                            "needs_info" -> Color(0xFFFBBF24)
                            else -> Color(0xFFB8C3D1)
                        }, style = MaterialTheme.typography.labelSmall)
                    }
                    Text(categoryLabels[item.category] ?: item.category, style = MaterialTheme.typography.labelSmall, color = Color(0xFFFFC18A))
                    Text(item.description, style = MaterialTheme.typography.bodySmall)
                    if (isDeveloper) {
                        Text(localizedAppText(localizedAppText("Compte joueur : ${item.userId.take(8)}")), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.aiSummary.isNotBlank()) {
                            Text(localizedAppText(localizedAppText("Pré-analyse IA : ${item.aiSummary}")), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (item.developerReply.isNotBlank()) {
                        Text(localizedAppText(localizedAppText("Réponse du développeur")), fontWeight = FontWeight.SemiBold, color = Color(0xFFFFC18A))
                        Text(item.developerReply, style = MaterialTheme.typography.bodySmall)
                    }
                    if (isDeveloper) {
                        OutlinedButton(
                            onClick = { selected = item; reply = item.developerReply },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(localizedAppText(localizedAppText("Traiter cette proposition"))) }
                    }
                }
            }
        }
    }

    selected?.let { item ->
        var status by remember(item.id) { mutableStateOf(item.status.takeIf { it != "pending" } ?: "accepted") }
        AlertDialog(
            onDismissRequest = { if (!reviewBusy) selected = null },
            title = { Text(localizedAppText(localizedAppText("Répondre à la proposition"))) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.title, fontWeight = FontWeight.Bold)
                    Text(item.description, style = MaterialTheme.typography.bodySmall)
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(statusLabels[status] ?: status)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            listOf("accepted", "rejected", "needs_info", "in_progress", "implemented").forEach { key ->
                                DropdownMenuItem(text = { Text(statusLabels[key] ?: key) }, onClick = { status = key; expanded = false })
                            }
                        }
                    }
                    OutlinedTextField(
                        value = reply, onValueChange = { reply = it.take(3000) },
                        label = { Text(localizedAppText(localizedAppText("Ta réponse au joueur"))) },
                        minLines = 3, modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            reviewBusy = true
                            auth.reviewAppImprovement(item.id, status, reply)
                                .onSuccess {
                                    message = localizedUiText(currentLang, "Réponse enregistrée et visible par le joueur.", "Reply saved and visible to the player.", "Antwort gespeichert und für den Spieler sichtbar.", "Yanıt kaydedildi ve oyuncu tarafından görülebilir.", "Ответ сохранён и виден игроку.", "回复已保存，玩家可以查看。")
                                    isError = false
                                    selected = null
                                    refresh()
                                }
                                .onFailure {
                                    message = it.message ?: localizedUiText(currentLang, "Enregistrement impossible.", "Could not save.", "Speichern nicht möglich.", "Kaydedilemedi.", "Не удалось сохранить.", "无法保存。")
                                    isError = true
                                }
                            reviewBusy = false
                        }
                    },
                    enabled = !reviewBusy
                ) { Text(if (reviewBusy) localizedAppText(localizedAppText("Enregistrement…")) else localizedAppText(localizedAppText("Envoyer la réponse"))) }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }, enabled = !reviewBusy) { Text(localizedAppText(localizedAppText("Annuler"))) }
            }
        )
    }
}
