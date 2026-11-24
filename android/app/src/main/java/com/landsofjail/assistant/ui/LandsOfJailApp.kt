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
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.landsofjail.assistant.capture.CapturedScreenPayload
import com.landsofjail.assistant.capture.ScreenCaptureConsentActivity
import com.landsofjail.assistant.data.GeminiAssistantRepository
import com.landsofjail.assistant.data.GameIntelSnapshot
import com.landsofjail.assistant.data.SupabaseAuthRepository
import com.landsofjail.assistant.model.*
import com.landsofjail.assistant.ocr.LandsOfJailOcrParser
import com.landsofjail.assistant.service.GameForegroundMonitorService
import com.landsofjail.assistant.service.UsageStatsDetector
import kotlinx.coroutines.launch
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
    zh: String
): String = when (language) {
    AppLanguage.FR -> fr
    AppLanguage.EN -> en
    AppLanguage.DE -> de
    AppLanguage.TR -> tr
    AppLanguage.RU -> ru
    AppLanguage.ZH -> zh
}

private enum class AssistantTab {
    TABLEAU, HEROS, RESSOURCES, CONSEILS, ALLIANCE, HISTORIQUE, AMELIORATIONS;

    fun titleFor(lang: AppLanguage): String = when (this) {
        TABLEAU -> when (lang) {
            AppLanguage.FR -> "Tableau"
            AppLanguage.TR -> "Ana Sayfa"
            AppLanguage.RU -> "Главная"
            AppLanguage.ZH -> "主页"
            AppLanguage.DE -> "Start"
            AppLanguage.EN -> "Home"
        }
        HEROS -> when (lang) {
            AppLanguage.FR -> "Héros"
            AppLanguage.TR -> "Kahraman"
            AppLanguage.RU -> "Герои"
            AppLanguage.ZH -> "英雄"
            AppLanguage.DE -> "Helden"
            AppLanguage.EN -> "Heroes"
        }
        RESSOURCES -> when (lang) {
            AppLanguage.FR -> "Ressources"
            AppLanguage.TR -> "Kaynaklar"
            AppLanguage.RU -> "Ресурсы"
            AppLanguage.ZH -> "资源"
            AppLanguage.DE -> "Ressourc."
            AppLanguage.EN -> "Resources"
        }
        CONSEILS -> when (lang) {
            AppLanguage.FR -> "Événements"
            AppLanguage.TR -> "Etkinlik"
            AppLanguage.RU -> "События"
            AppLanguage.ZH -> "活动"
            AppLanguage.DE -> "Events"
            AppLanguage.EN -> "Events"
        }
        ALLIANCE -> when (lang) {
            AppLanguage.FR -> "Alliance"
            AppLanguage.TR -> "İttifak"
            AppLanguage.RU -> "Альянс"
            AppLanguage.ZH -> "联盟"
            AppLanguage.DE -> "Allianz"
            AppLanguage.EN -> "Alliance"
        }
        HISTORIQUE -> when (lang) {
            AppLanguage.FR -> "Historique"
            AppLanguage.TR -> "Geçmiş"
            AppLanguage.RU -> "История"
            AppLanguage.ZH -> "记录"
            AppLanguage.DE -> "Verlauf"
            AppLanguage.EN -> "History"
        }
        AMELIORATIONS -> when (lang) {
            AppLanguage.FR -> "Idées"
            AppLanguage.TR -> "Fikirler"
            AppLanguage.RU -> "Идеи"
            AppLanguage.ZH -> "建议"
            AppLanguage.DE -> "Ideen"
            AppLanguage.EN -> "Ideas"
        }
    }
}

private enum class ResourceSubTab {
    INVENTAIRE, ROBOTS, EVENEMENTS, BATIMENTS, HEROS_OBJETS, ROUTINE;

    fun labelFor(lang: AppLanguage): String = when (this) {
        INVENTAIRE -> when (lang) {
            AppLanguage.FR -> "Inventaire"
            AppLanguage.TR -> "Envanter"
            AppLanguage.RU -> "Инвентарь"
            AppLanguage.ZH -> "库存"
            AppLanguage.DE -> "Inventar"
            AppLanguage.EN -> "Inventory"
        }
        BATIMENTS -> when (lang) {
            AppLanguage.FR -> "Calcul Bâtiments"
            AppLanguage.TR -> "Bina Hesabı"
            AppLanguage.RU -> "Кальк. зданий"
            AppLanguage.ZH -> "建筑计算"
            AppLanguage.DE -> "Gebäude"
            AppLanguage.EN -> "Buildings Calc"
        }
        HEROS_OBJETS -> when (lang) {
            AppLanguage.FR -> "Calcul Héros"
            AppLanguage.TR -> "Kahraman Hesabı"
            AppLanguage.RU -> "Кальк. героев"
            AppLanguage.ZH -> "英雄计算"
            AppLanguage.DE -> "Helden-Rechner"
            AppLanguage.EN -> "Hero Calc"
        }
        ROUTINE -> when (lang) {
            AppLanguage.FR -> "Routine Hors-Jeu"
            AppLanguage.TR -> "Çevrimdışı Rutin"
            AppLanguage.RU -> "Авто-таймеры"
            AppLanguage.ZH -> "离线循环"
            AppLanguage.DE -> "Offline-Routine"
            AppLanguage.EN -> "Offline Routine"
        }
        ROBOTS -> when (lang) {
            AppLanguage.FR -> "Robots"
            AppLanguage.TR -> "Robotlar"
            AppLanguage.RU -> "Роботы"
            AppLanguage.ZH -> "机器人"
            AppLanguage.DE -> "Roboter"
            AppLanguage.EN -> "Robots"
        }
        EVENEMENTS -> when (lang) {
            AppLanguage.FR -> "Suivi événements"
            AppLanguage.TR -> "Etkinlik takibi"
            AppLanguage.RU -> "События"
            AppLanguage.ZH -> "活动记录"
            AppLanguage.DE -> "Eventverlauf"
            AppLanguage.EN -> "Event tracking"
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
                    scope.launch {
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

        val ocrRaw = if (localImage != null) {
            try {
                onProgress(20)
                LandsOfJailOcrParser.recognizeBitmap(localImage)
            } catch (_: Throwable) {
                ""
            } finally {
                localImage.recycle()
            }
        } else ""

        val localExtraction = LandsOfJailOcrParser.parseLocalOcrText(ocrRaw)
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
                champsIncertains = (remoteResult.champsIncertains + localExtraction.uncertainItems)
                    .distinctBy { normalizedKey(it.champ) + normalizedKey(it.valeurObservee) }
            )
            onProgress(100)
            return combined
        }

        // Si Internet ou Gemini est indisponible, ne pas perdre le résultat local.
        val hasLocalData = localExtraction.power != null ||
            localExtraction.heroes.isNotEmpty() ||
            localExtraction.resources.isNotEmpty()

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
                    }.joinToString(" · "),
                puissance = localExtraction.power,
                ressources = localExtraction.resources,
                heros = localExtraction.heroes,
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

        scope.launch {
            isQueueRunning = true
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
                                it.copy(
                                    status = "done",
                                    progress = 100,
                                    summary = analysis.resumeCapture
                                )
                            } else {
                                it
                            }
                        }
                    }.onFailure { err ->
                        analysisQueue = analysisQueue.map {
                            if (it.id == nextItem.id) {
                                it.copy(
                                    status = "error",
                                    progress = 0,
                                    summary = err.message ?: "Erreur"
                                )
                            } else {
                                it
                            }
                        }
                    }
                }
                val doneCount = analysisQueue.count { it.status == "done" }
                if (doneCount > 0) {
                    currentTab = AssistantTab.TABLEAU
                    snackbarHostState.showSnackbar("File terminée (100 %) : $doneCount capture(s) analysée(s)")
                }
            } finally {
                isQueueRunning = false
                isAnalyzing = false
            }
        }
    }

    LaunchedEffect(capturedScreenPayload) {
        if (capturedScreenPayload != null) {
            isAnalyzing = true
            singleAnalysisPercent = 20
            errorMessage = null
            try {
                val ocrRaw = runCatching {
                    LandsOfJailOcrParser.recognizeBitmap(capturedScreenPayload.bitmap)
                }.getOrDefault("")
                singleAnalysisPercent = 50
                val localExtraction = LandsOfJailOcrParser.parseLocalOcrText(ocrRaw)
                val localImageUri = capturedScreenPayload.imageFileUri

                val result = runCatching {
                    repository.analyzeBase64Screenshot(
                        imageBase64 = capturedScreenPayload.base64Jpeg,
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
                        champsIncertains = localExtraction.uncertainItems
                    )
                }

                singleAnalysisPercent = 100
                mergeNewAnalysis(result)
                AdviceMode.entries.firstOrNull { it.apiId == result.typeCapture }?.let {
                    selectedAdviceMode = it
                }

                val autoModeStillActive = GameForegroundMonitorService.isAutoCapturing.value
                if (!autoModeStillActive) {
                    currentTab = AssistantTab.TABLEAU
                    snackbarHostState.showSnackbar(
                        "Capture analysée 100 % (${result.heros.size} héros, ${result.robots.size} robot(s), ${result.ressources.size} ressource(s))"
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
            } catch (e: Throwable) {
                errorMessage = e.message ?: "Erreur pendant l'analyse OCR."
            } finally {
                onConsumeCapturedScreen()
                isAnalyzing = false
                singleAnalysisPercent = 0
            }
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
                                        }.onFailure { errorMessage = "Impossible d'ouvrir ce profil : ${it.message}" }
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
                    ) { Text("+ " + localizedUiText(currentLang, "Ajouter un profil", "Add a profile", "Profil hinzufügen", "Profil ekle", "Добавить профиль", "添加档案")) }
                    TextButton(
                        enabled = !isProfileSyncing && !isAnalyzing && !isAutoCapturing,
                        onClick = {
                            if (profileAuth.currentSession() == null) {
                                errorMessage = "Connecte-toi à ton compte dans l’onglet Compte avant la synchronisation."
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
                                        snackbarHostState.showSnackbar("Synchronisation terminée. Aucune sauvegarde distante n’a été supprimée.")
                                    } catch (error: Exception) {
                                        errorMessage = "Synchronisation interrompue : ${error.message ?: "erreur inconnue"}. La copie locale de secours est conservée."
                                    } finally {
                                        isProfileSyncing = false
                                    }
                                }
                            }
                        }
                    ) { Text(if (isProfileSyncing) "Synchronisation…" else "Synchroniser avec le cloud") }
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
                            }.onFailure { errorMessage = "Suppression impossible : ${it.message}" }
                        } else {
                            localProfiles = localProfiles.filterNot { it.id == target.id }
                        }
                    }
                    deletingProfileId = null
                }) { Text(localizedUiText(currentLang, "Supprimer", "Delete", "Löschen", "Sil", "Удалить", "删除")) }
            },
            dismissButton = { TextButton(onClick = { deletingProfileId = null }) { Text("Annuler") } }
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
                                "LANDS OF JAIL",
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.1.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "ASSISTANT TACTIQUE",
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
                                    "$pct %",
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
                                    Text("Fermer la file (100 %)")
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
                        onChangeLang = { currentLang = it },
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
                        onChangeLang = { currentLang = it },
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
                        workspace = allianceWorkspace,
                        heroes = heroes,
                        robots = robots,
                        currentPowerText = currentPower?.puissanceTotaleTexte.orEmpty(),
                        onSaveWorkspace = { allianceWorkspace = it }
                    )
                    AssistantTab.AMELIORATIONS -> ImprovementRequestsScreen(auth = profileAuth)
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
            title = { Text("Bienvenue · Inscription joueur") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Renseigne ton identité dans Land of Jail. Ces informations seront enregistrées sur cet appareil et pourront être transmises aux officiers de ton alliance après connexion.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = onboardingPlayerName,
                        onValueChange = { onboardingPlayerName = it.take(40) },
                        label = { Text("Pseudo dans le jeu") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = onboardingServer,
                        onValueChange = { onboardingServer = it.filter(Char::isDigit).take(6) },
                        label = { Text("Serveur (ex. 311)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = onboardingAllianceTag,
                        onValueChange = { onboardingAllianceTag = it.take(12) },
                        label = { Text("Initiales / tag de l'alliance") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Ton rang déclaré", style = MaterialTheme.typography.labelLarge)
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
                        "Si tu es le R5 qui crée l'espace cloud de l'alliance, tu deviens directement R5 sans validation. Pour une alliance déjà créée, rejoins-la d'abord comme membre puis demande au R5 actuel de te transférer le poste.",
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
                                        snackbarHostState.showSnackbar("Espace créé : tu es directement R5, sans validation.")
                                    } else {
                                        snackbarHostState.showSnackbar("Demande envoyée aux R4/R5 de l'alliance.")
                                    }
                                }.onFailure {
                                    snackbarHostState.showSnackbar(it.message ?: if (isInitialR5) {
                                        "Création de l'espace R5 impossible. Si l'alliance existe déjà, rejoins-la avec un code d'invitation."
                                    } else {
                                        "Demande non envoyée. Vérifie le compte et le tag."
                                    })
                                }
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (onboardingRank == AllianceRank.R5) {
                                        "Profil enregistré. Connecte-toi pour créer l'espace de l'alliance et devenir R5."
                                    } else {
                                        "Profil enregistré. Connecte-toi pour transmettre la demande aux officiers."
                                    }
                                )
                            }
                        }
                    },
                    enabled = onboardingPlayerName.isNotBlank() && onboardingServer.isNotBlank() && onboardingAllianceTag.isNotBlank()
                ) { Text("Enregistrer et continuer") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAllianceOnboarding = false
                        accountPrefs.edit().putBoolean("alliance_onboarding_completed_v1", true).apply()
                    }
                ) { Text("Plus tard") }
            }
        )
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
    onChangeLang: (AppLanguage) -> Unit,
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
        item {
            Spacer(Modifier.height(6.dp))
            LanguageSelectorRow(currentLang = currentLang, onChangeLang = onChangeLang)
        }

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
                                }
                            } else {
                                when (currentLang) {
                                    AppLanguage.FR -> "Configuration requise (1 fois)"
                                    AppLanguage.TR -> "İlk Kurulum Gerekli"
                                    AppLanguage.RU -> "Требуется настройка"
                                    AppLanguage.ZH -> "需要初始授权"
                                    AppLanguage.DE -> "Einmalige Einrichtung"
                                    AppLanguage.EN -> "Required Setup (once)"
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
                                    contentDescription = "Réglages capture"
                                )
                            }
                        }
                    }

                    // Afficher les boutons d'autorisation UNIQUEMENT s'il manque une permission ou si Settings est ouvert
                    if (!allPermissionsGranted || showCaptureSettings) {
                        if (!hasUsagePermission) {
                            Text(
                                "1. Statistiques d'utilisation : À autoriser",
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Medium
                            )
                            Button(onClick = onOpenUsageSettings) {
                                Text("Autoriser Statistiques d'utilisation")
                            }
                        } else {
                            Text(
                                "1. Statistiques d'utilisation : Autorisées",
                                color = Color(0xFF34D399),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (!hasOverlayPermission) {
                            Text(
                                "2. Superposition au-dessus du jeu : À autoriser",
                                color = Color(0xFFFBBF24),
                                fontWeight = FontWeight.Medium
                            )
                            Button(onClick = onOpenOverlaySettings) {
                                Text("Autoriser affichage par-dessus les applis")
                            }
                        } else {
                            Text(
                                "2. Superposition au-dessus du jeu : Autorisée",
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
                                "Garder le bouton flottant au-dessus du jeu",
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
                            label = { Text("Package (com.justgame.jails)") },
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
                                    }
                                )
                            }
                        }
                    }

                    if (isGameForeground) {
                        Text(
                            "Lands of Jail détecté au premier plan.",
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
                                    }
                                )
                            }
                        } else {
                            Box {
                                OutlinedButton(
                                    onClick = { showAutoIntervalMenu = true },
                                    enabled = isMonitoringRunning && hasUsagePermission && !isAnalyzing && !isProfileSyncing && !isAnalyzing && !isProfileSyncing
                                ) {
                                    Text("${autoCaptureIntervalSeconds}s")
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
                        "Puissance : ${currentPower?.puissanceTotaleTexte ?: "Non mesurée"}",
                        fontWeight = FontWeight.SemiBold
                    )
                    if (!currentPower?.niveauPrisonOuQG.isNullOrBlank()) {
                        Text(
                            "Prison / QG : ${currentPower?.niveauPrisonOuQG}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (currentPower?.incertain == true) {
                        Text(
                            "Incertain : ${currentPower.raisonIncertitude ?: "lecture partielle"}",
                            color = Color(0xFFFBBF24),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text("Héros suivis : $heroesCount · Ressources : $resourcesCount")
                }
            }
        }

        item {
            Text("Rôle du joueur", fontWeight = FontWeight.SemiBold)
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
                    "${HeroCatalog.entries.size} héros référencés · ${heroes.size} héros suivis sur ton compte",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(localizedUiText(language, "Progression de ton profil héros", "Hero profile progress", "Fortschritt deines Heldenprofils", "Kahraman profil ilerlemesi", "Прогресс профиля героев", "英雄档案进度"), fontWeight = FontWeight.SemiBold)
                        Text("$verifiedHeroes profil(s) suffisamment renseigné(s) · $heroesToComplete à compléter")
                        LinearProgressIndicator(
                            progress = { if (heroes.isEmpty()) 0f else verifiedHeroes.toFloat() / heroes.size.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Pour obtenir des conseils fiables, renseigne niveau, étoiles, puissance et compétences War / Arrest / Équipement. Les champs absents ne sont jamais devinés.",
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
                    "Les données du catalogue sont des références communautaires. Seules les valeurs détectées sur tes captures décrivent ton compte.",
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
                        "${entry.rarete} · ${entry.saison} · ${entry.typeTroupe}",
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
                                Text(if (expandedHeroName == entry.nom) "Masquer la fiche" else "Fiche complète")
                            }
                        }
                        if (!hero.rarete.isNullOrBlank() && !hero.rarete.equals(entry.rarete, true)) {
                            Text("Rareté lue sur capture : ${hero.rarete}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (hero.incertain) {
                            Text(
                                "À vérifier : ${hero.raisonIncertitude ?: "données de capture incomplètes"}",
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
                                "Champs précis à vérifier : ${fieldLabels.joinToString(" · ")}",
                                color = Color(0xFFFBBF24),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        hero.competences.forEach { skill ->
                            Text(
                                "• ${when (skill.type.lowercase()) { "war" -> "War"; "arrest" -> "Arrest"; "equipment" -> "Équipement"; else -> "Type à confirmer" }} — ${skill.nom} (${skill.niveau})${if (skill.incertain) " · Incertain" else ""}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (!skill.description.isNullOrBlank()) {
                                Text(skill.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        Text("Niveau, étoiles et compétences personnels : à scanner ou saisir depuis le jeu.", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { editingHero = onAddHero(entry) }) {
                            Text(localizedUiText(language, "Ajouter à mon profil", "Add to my profile", "Zu meinem Profil hinzufügen", "Profilime ekle", "Добавить в профиль", "添加到我的档案"))
                        }
                        TextButton(onClick = { expandedHeroName = if (expandedHeroName == entry.nom) null else entry.nom }) {
                            Text(if (expandedHeroName == entry.nom) "Masquer la fiche" else "Voir la fiche complète")
                        }
                    }
                    if (expandedHeroName == entry.nom) {
                        HorizontalDivider()
                        Text("Fiche de référence", fontWeight = FontWeight.SemiBold)
                        Text("Rareté : ${entry.rarete} · Saison : ${entry.saison} · Classe : ${entry.typeTroupe}")
                        Text("Source : ${entry.sourceNiveau}")
                        entry.usageWarObserve?.let { Text("Spécialité observée : $it") }
                        if (hero == null || hero.competences.isEmpty()) {
                            Text(
                                "Compétences personnelles non renseignées. Scanne les pages de compétences de ce héros pour obtenir ses effets et niveaux réels.",
                                color = Color(0xFFFBBF24),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        hero?.competences?.forEach { skill ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        "${skill.type.uppercase()} · ${skill.nom} · Niv. ${skill.niveau}",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(skill.description ?: "Description non disponible", style = MaterialTheme.typography.bodySmall)
                                    if (skill.incertain) Text("À vérifier", color = Color(0xFFFBBF24), style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        TextButton(onClick = { uriHandler.openUri(entry.sourceUrl) }) {
                            Text("Consulter la source du catalogue")
                        }
                    }
                    entry.usageWarObserve?.let {
                        Text("War / équipement observé : $it", style = MaterialTheme.typography.bodySmall)
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
            item { Text("Autres héros détectés", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            items(detectedOutsideCatalog, key = { "detected-" + it.nom }) { hero ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(hero.nom, fontWeight = FontWeight.SemiBold)
                        Text(listOfNotNull(hero.niveauTexte.takeIf { it.isNotBlank() }, hero.etoiles, hero.puissanceHero).joinToString(" · "))
                        TextButton(onClick = { editingHero = hero }) { Text("Modifier les données détectées") }
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
        title = { Text("Corriger le héros détecté") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nom du héros") }, singleLine = true)
                OutlinedTextField(level, { level = it }, label = { Text("Niveau") }, singleLine = true)
                OutlinedTextField(rarity, { rarity = it }, label = { Text("Rareté") }, singleLine = true)
                OutlinedTextField(stars, { stars = it }, label = { Text("Étoiles") }, singleLine = true)
                OutlinedTextField(power, { power = it }, label = { Text("Puissance") }, singleLine = true)
                OutlinedTextField(
                    skills,
                    { skills = it },
                    label = { Text("Compétences : type | nom | niveau | description (war / arrest / equipment / inconnu)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Les corrections manuelles remplacent les valeurs incertaines de la capture.", style = MaterialTheme.typography.bodySmall)
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
            ) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } }
    )
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
                        "Puissance actuelle : ${power?.puissanceTotaleTexte ?: "Non capturée"}",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (resources.isEmpty()) {
                    item {
                        Text(
                            "Aucune ressource capturée pour l’instant. Utilisez « Calcul Bâtiments » ou « Calcul Héros » ci-dessus pour calculer vos prochains paliers.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    items(resources) { res ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Text("${res.nom} : ${res.quantiteTexte}", fontWeight = FontWeight.Medium)
                                if (res.incertain) {
                                    Text(
                                        "Incertain : ${res.raisonIncertitude ?: "affichage tronqué"}",
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
                            Text("Inventaire des robots du profil", fontWeight = FontWeight.Bold)
                            Text("Ces données sont indépendantes des autres profils. Renseigne les bonus tels qu’ils apparaissent en jeu ; aucune statistique n’est devinée.", style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(robotName, { robotName = it }, label = { Text("Nom du robot") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(robotLevel, { robotLevel = it }, label = { Text("Niveau") }, modifier = Modifier.weight(1f), singleLine = true)
                                OutlinedTextField(robotRarity, { robotRarity = it }, label = { Text("Rareté / étoiles") }, modifier = Modifier.weight(1f), singleLine = true)
                            }
                            OutlinedTextField(robotPower, { robotPower = it }, label = { Text("Puissance (facultatif)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                            OutlinedTextField(robotBonus, { robotBonus = it }, label = { Text("Bonus actifs / statistiques") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                            OutlinedTextField(robotSkills, { robotSkills = it }, label = { Text("Compétences / effets (facultatif)") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
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
                            ) { Text("Ajouter / mettre à jour le robot") }
                        }
                    }
                }
                if (robots.isEmpty()) {
                    item { Text("Aucun robot enregistré dans ce profil.", style = MaterialTheme.typography.bodySmall) }
                } else {
                    items(robots, key = { it.id }) { robot ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(robot.nom, fontWeight = FontWeight.SemiBold)
                                val meta = listOf(robot.niveau.takeIf { it.isNotBlank() }?.let { "Niv. $it" }, robot.rarete, robot.puissance.takeIf { it.isNotBlank() }?.let { "Puissance $it" }).filterNotNull()
                                if (meta.isNotEmpty()) Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                                if (robot.bonus.isNotBlank()) Text("Bonus : ${robot.bonus}", style = MaterialTheme.typography.bodySmall)
                                if (robot.competences.isNotBlank()) Text("Compétences : ${robot.competences}", style = MaterialTheme.typography.bodySmall)
                                if (robot.incertain) {
                                    Text(
                                        robot.raisonIncertitude?.takeIf { it.isNotBlank() } ?: "Données détectées à vérifier sur l’écran du jeu.",
                                        color = Color(0xFFFBBF24),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (robot.champsIncertains.isNotEmpty()) {
                                    val robotFieldLabels = robot.champsIncertains.map { field ->
                                        when (field.lowercase()) {
                                            "niveau" -> "niveau"
                                            "rarete" -> "rareté"
                                            "bonus" -> "bonus"
                                            "competences" -> "compétences"
                                            "puissance" -> "puissance"
                                            else -> field
                                        }
                                    }
                                    Text(
                                        "Champs à vérifier : ${robotFieldLabels.joinToString(" · ")}",
                                        color = Color(0xFFFBBF24),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                TextButton(onClick = { onSaveRobots(robots.filterNot { it.id == robot.id }) }) { Text("Retirer") }
                            }
                        }
                    }
                }
            }

            ResourceSubTab.EVENEMENTS -> {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Journal des événements du profil", fontWeight = FontWeight.Bold)
                            Text("Conserve tes résultats, scores et observations pour comparer tes propres performances entre les sessions.", style = MaterialTheme.typography.bodySmall)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(AdviceMode.entries) { event ->
                                    FilterChip(
                                        selected = selectedTrackedEvent == event,
                                        onClick = { selectedTrackedEvent = event },
                                        label = { Text(event.titleFor(currentLang), maxLines = 1) }
                                    )
                                }
                            }
                            OutlinedTextField(eventDateInput, { eventDateInput = it }, label = { Text("Date (ex. 23/11/2026)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(eventScoreInput, { eventScoreInput = it }, label = { Text("Score / dégâts / classement") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(eventNotesInput, { eventNotesInput = it }, label = { Text("Composition, ratio, remarques") }, minLines = 3, modifier = Modifier.fillMaxWidth())
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
                            ) { Text("Enregistrer le résultat") }
                        }
                    }
                }
                if (eventRecords.isEmpty()) {
                    item { Text("Aucun résultat d’événement enregistré pour ce profil.", style = MaterialTheme.typography.bodySmall) }
                } else {
                    items(eventRecords.sortedByDescending { it.updatedAtIso }, key = { it.id }) { record ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(record.eventName, fontWeight = FontWeight.SemiBold)
                                Text(listOf(record.date, record.score).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                                if (record.notes.isNotBlank()) Text(record.notes, style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { onSaveEventRecords(eventRecords.filterNot { it.id == record.id }) }) { Text("Supprimer l’entrée") }
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
                                "Bureau du directeur (Warden Office) → Niv. $targetBuildingLvl",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Règle d'or dès Niv. 9 : gardez le Centre de communications à Niv. ${targetBuildingLvl - 1}.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFBBF24)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { if (targetBuildingLvl > 2) targetBuildingLvl-- },
                                    enabled = targetBuildingLvl > 2
                                ) { Text("-1 Niveau") }
                                Button(
                                    onClick = { if (targetBuildingLvl < 30) targetBuildingLvl++ },
                                    enabled = targetBuildingLvl < 30
                                ) { Text("+1 Niveau") }
                            }
                            Text("Prérequis : ${req.prereqs}", fontWeight = FontWeight.Medium)
                            Text("Nourriture : ${formatCompactFr(req.food)}")
                            Text("Bois : ${formatCompactFr(req.wood)}")
                            Text("Acier : ${formatCompactFr(req.steel)}")
                            Text("Essence : ${formatCompactFr(req.gasoline)}")
                            Text(
                                if (req.verified) "Source : Wiki / lojxlab vérifié"
                                else "Source : Estimation (à confirmer en jeu)",
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
                            Text("Éclats de héros ($starFrom★ → $starTo★)", fontWeight = FontWeight.SemiBold)
                            Text("Total 1★→5★ : 1 075 éclats (+10 recrutement). Seuil 4★ recommandé : 475 éclats.")
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    starFrom = (starFrom + 1) % 5
                                    if (starTo <= starFrom) starTo = starFrom + 1
                                }) { Text("Départ : $starFrom★") }
                                Button(onClick = {
                                    starTo = if (starTo >= 5) starFrom + 1 else starTo + 1
                                }) { Text("Cible : $starTo★") }
                            }
                            OutlinedTextField(
                                value = ownedShardsText,
                                onValueChange = { ownedShardsText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("Éclats en stock") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text("Requis : $totalShards éclats · Manquants : $missing", fontWeight = FontWeight.Medium)
                        }
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Tomes de troupes (Niv. $tomeFrom → $tomeTo)", fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    tomeFrom = (tomeFrom + 1) % 6
                                    if (tomeTo <= tomeFrom) tomeTo = tomeFrom + 1
                                }) { Text("De : $tomeFrom") }
                                Button(onClick = {
                                    tomeTo = if (tomeTo >= 6) tomeFrom + 1 else tomeTo + 1
                                }) { Text("À : $tomeTo") }
                            }
                            Text("Seal of Wisdom : $totalWisdom")
                            Text("Seal of Knowledge : $totalKnowledge")
                            Text(
                                "Note : Mystery Tomes requis à partir du palier 12→13. Pour les compétences héros : Arrest & War Skill Books selon rareté.",
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
                                label = { Text("Durée personnalisée (minutes)") },
                                supportingText = { Text("De 1 à 10 080 minutes (7 jours)") },
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
                                    Text("Démarrer")
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
    onChangeLang: (AppLanguage) -> Unit,
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
            LanguageSelectorRow(currentLang = currentLang, onChangeLang = onChangeLang)
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
                    Text("Contrôle des données avant recommandation", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Profil : ${heroes.size} héros · ${heroesWithWarSkills} avec compétence War identifiée · ${robots.size} robots · ${robotsWithDetails} avec bonus ou compétence renseigné.",
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
                            "Robots incomplets : renseigne leurs bonus et compétences avant de les comparer.",
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
                        }
                    } else {
                        when (currentLang) {
                            AppLanguage.FR -> "Obtenir les conseils & photos Internet"
                            AppLanguage.TR -> "İnternet Rehberi ve Fotoğrafları Al"
                            AppLanguage.RU -> "Получить гайды и фото"
                            AppLanguage.ZH -> "获取网络攻略与图片"
                            AppLanguage.DE -> "Internet-Guides & Fotos abrufen"
                            AppLanguage.EN -> "Get Internet Guides & Photos"
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
                        Text("Synthèse (${adviceDate.take(10)})", fontWeight = FontWeight.SemiBold)
                        Text(res.synthese)
                        Text("Composition : ${res.compositionRecommandee}")
                    }
                }
            }

            if (res.troopRatioProfiles.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text(res.troopRatioTitle ?: "Ratios de troupes", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Ratios de référence : vérifie toujours les consignes du chef et les règles affichées dans l’événement.",
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
                                        "${ratio.shieldbearer}% Shieldbearers · ${ratio.bomber}% Bombers · ${ratio.shooter}% Shooters",
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
                            Text("Héros du profil — compétences correspondantes", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Sélection fondée sur les compétences reconnues dans ce profil. Ce n’est pas un classement de puissance ni une simulation de combat.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            res.heroRecommendations.forEach { hero ->
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(hero.titre, fontWeight = FontWeight.Medium)
                                    Text(hero.detail, style = MaterialTheme.typography.bodySmall)
                                    hero.raisonIncertitude?.let {
                                        Text("À vérifier : $it", color = Color(0xFFFBBF24), style = MaterialTheme.typography.bodySmall)
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
                            Text("Robots du profil — correspondances à vérifier", fontWeight = FontWeight.SemiBold)
                            Text(
                                "Sélection basée sur les bonus et compétences que tu as saisis. Ce n’est pas une simulation ni un classement garanti.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            res.robotRecommendations.forEach { robot ->
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(robot.titre, fontWeight = FontWeight.Medium)
                                    Text(robot.detail, style = MaterialTheme.typography.bodySmall)
                                    robot.raisonIncertitude?.let {
                                        Text("À vérifier : $it", color = Color(0xFFFBBF24), style = MaterialTheme.typography.bodySmall)
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
                                    "Incertain : ${rec.raisonIncertitude}",
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
                            Text("AVERTISSEMENT — CONSEILS NON VÉRIFIÉS", fontWeight = FontWeight.Bold, color = Color(0xFFFF5252))
                            res.informationsIncertaines.forEach { info ->
                                Text("• $info", color = Color(0xFFFFCDD2))
                            }
                        }
                    }
                }
            }

            item {
                Text("Sources Internet, Photos & Dates", fontWeight = FontWeight.SemiBold)
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
                                "Date : ${src.detectedDate ?: adviceDate.take(10)}",
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
    var showClearConfirmation by remember { mutableStateOf(false) }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Effacer l’historique ?") },
            text = { Text("Toutes les analyses enregistrées dans l’historique seront supprimées. Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearHistory()
                    showClearConfirmation = false
                }) { Text("Effacer") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text("Annuler") }
            }
        )
    }

    if (analyses.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Historique vide. Appuyez sur « Analyser l’écran » pour enregistrer votre première capture.")
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
                Text("Effacer l’historique")
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
                        Text(item.typeCapture, fontWeight = FontWeight.SemiBold)
                        Text(
                            item.analyzedAtIso.take(19).replace('T', ' '),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8)
                        )
                        Text(item.resumeCapture, style = MaterialTheme.typography.bodySmall)
                        if (item.champsIncertains.isNotEmpty()) {
                            Text(
                                "${item.champsIncertains.size} champ(s) incertain(s)",
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
private fun SupabaseAccountCard() {
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
            Text("Compte & sauvegarde cloud", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Connecte ton compte pour synchroniser ton profil de joueur et tes héros dans l'espace cloud sécurisé.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB8C3D1)
            )
            if (session != null) {
                Text("Session enregistrée : ${session?.email.orEmpty()}", color = Color(0xFF86EFAC), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Les jetons sont chiffrés avec Android Keystore. La synchronisation du profil se lance depuis le formulaire joueur ci-dessous.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFBBF24)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                busy = true
                                auth.refreshSession()
                                    .onSuccess { session = it; message = "Session actualisée."; isError = false }
                                    .onFailure { message = it.message ?: "Actualisation impossible."; isError = true }
                                busy = false
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) { Text("Actualiser") }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                busy = true
                                auth.signOut()
                                    .onSuccess { session = null; message = "Déconnexion effectuée."; isError = false }
                                    .onFailure { message = it.message ?: "Déconnexion impossible."; isError = true }
                                busy = false
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) { Text("Déconnexion") }
                }
            } else {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = { Text("Adresse e-mail") },
                    singleLine = true,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mot de passe (8 caractères minimum)") },
                    singleLine = true,
                    enabled = !busy,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (createAccount) {
                    HorizontalDivider(color = Color(0xFF334155))
                    Text("Profil joueur et alliance", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Ces informations sont enregistrées pendant la création du compte. Tu n'auras pas à refaire une inscription dans l'espace Alliance.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8C3D1))
                    OutlinedTextField(
                        value = signupPlayerName,
                        onValueChange = { signupPlayerName = it.take(40) },
                        label = { Text("Pseudo dans Land of Jail") },
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = signupServer,
                        onValueChange = { signupServer = it.filter(Char::isDigit).take(6) },
                        label = { Text("Serveur (ex. 311)") },
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = signupAllianceTag,
                        onValueChange = { signupAllianceTag = it.take(12) },
                        label = { Text("Tag de l'alliance") },
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Ton rang actuel", style = MaterialTheme.typography.labelLarge)
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
                    Text("Choisis R5 uniquement si tu crées un nouvel espace d'alliance. Pour rejoindre une alliance existante, indique ton rang actuel : ta demande sera transmise aux responsables.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFBBF24))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { createAccount = !createAccount; message = null }, enabled = !busy) {
                        Text(if (createAccount) "J'ai déjà un compte" else "Créer un compte")
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
                                                message = "Compte connecté. Espace créé : tu es directement R5."
                                            } else {
                                                message = "Compte connecté. Demande envoyée aux R4/R5 de ${savedAlliance.allianceName}."
                                            }
                                        }.onFailure {
                                            message = "Compte connecté. Profil conservé ; inscription d'alliance impossible : ${it.message ?: "vérifie le serveur et le tag"}"
                                            isError = true
                                        }
                                    }
                                }.onFailure {
                                    message = it.message ?: "Une erreur est survenue."
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
                            Text(if (createAccount) "S'inscrire" else "Se connecter")
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
                        Text("Renvoyer l'e-mail de confirmation")
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
        item { SupabaseAccountCard() }
        item { AllianceCloudControls(server = server, allianceName = allianceName) }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Demandes d'adhésion · R4 / R5", fontWeight = FontWeight.Bold)
                    Text(
                        "Les demandes des joueurs de ton alliance apparaissent ici. Seuls les droits confirmés par un officier sont appliqués.",
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
                        Text(if (joinRequestsBusy) "Actualisation…" else "Voir les nouvelles demandes")
                    }
                    joinRequestsMessage?.let {
                        Text(it, color = if (joinRequestsError) Color(0xFFFCA5A5) else Color(0xFF86EFAC), style = MaterialTheme.typography.bodySmall)
                    }
                    joinRequests.forEach { request ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202B3A))) {
                            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(request.playerName, fontWeight = FontWeight.Bold)
                                Text("Rang demandé : ${request.requestedRank} · ${request.createdAt.take(10)}", style = MaterialTheme.typography.bodySmall)
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
                                    ) { Text("Accepter") }
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
                                    ) { Text("Refuser") }
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
                        Text("${workspace.members.size + if (workspace.ownProfile.playerName.isNotBlank()) 1 else 0} joueur(s)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        "Serveur ${workspace.server.ifBlank { "—" }} · Alliance ${workspace.allianceName.ifBlank { "—" }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (workspace.ownProfile.playerName.isNotBlank()) {
                        Text(
                            "Toi : ${workspace.ownProfile.playerName} · ${workspace.ownProfile.rank.labelFor(language)} · ${heroes.size} héros analysés",
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
                        Text(if (cloudRosterBusy) "Chargement du roster…" else localizedUiText(language, "Actualiser depuis le cloud", "Refresh from cloud", "Aus der Cloud aktualisieren", "Buluttan yenile", "Обновить из облака", "从云端刷新"))
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
                    Text("Puissance : ${member.powerText.ifBlank { "non renseignée" }}", style = MaterialTheme.typography.bodySmall)
                    if (member.heroSkillSummary.isNotBlank()) Text("Héros / compétences : ${member.heroSkillSummary}", style = MaterialTheme.typography.bodySmall)
                    if (member.robotSummary.isNotBlank()) Text("Robot : ${member.robotSummary}", style = MaterialTheme.typography.bodySmall)
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
                                    Text(if (rankChangeBusyFor == member.id) "Mise à jour…" else "Changer le rang · ${member.rank.name}")
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
                                Text("Le R5 se remplace en nommant un R4.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    if (isOfficer) {
                        TextButton(onClick = {
                            onSaveWorkspace(workspace.copy(members = workspace.members.filterNot { it.id == member.id }))
                            simulation = emptyList()
                        }) { Text("Retirer du roster") }
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
                        Text("Lancer la simulation (R4 / R5)")
                    }
                    simulation.forEach { line ->
                        Text(line, style = MaterialTheme.typography.bodySmall, color = if (line.startsWith("À confirmer")) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
        item {
            Text(
                "Les profils synchronisés sont chargés depuis le cloud selon les droits de visibilité du rang dans l'alliance. Les ajouts manuels effectués ici restent enregistrés localement sur cet appareil.",
                color = Color(0xFFFBBF24),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showAddMember) {
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            title = { Text("Ajouter un membre") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(memberName, { memberName = it }, label = { Text("Pseudo du joueur") }, singleLine = true)
                    OutlinedTextField(memberPower, { memberPower = it }, label = { Text("Puissance (facultatif)") }, singleLine = true)
                    Text("Rang dans l'alliance", style = MaterialTheme.typography.labelMedium)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        items(AllianceRank.entries) { item ->
                            FilterChip(selected = memberRank == item, onClick = { memberRank = item }, label = { Text(item.name) })
                        }
                    }
                    OutlinedTextField(
                        memberSkills, { memberSkills = it },
                        label = { Text("Héros et compétences War (copier les infos)") },
                        minLines = 3, modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        memberRobots, { memberRobots = it },
                        label = { Text("Robot / bonus (facultatif)") },
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
                ) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(onClick = { showAddMember = false }) { Text("Annuler") }
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
            Text("Alliance partagée en ligne", fontWeight = FontWeight.Bold)
            Text(
                "Crée l'espace cloud de ton alliance ou rejoins celui d'un autre joueur avec un code d'invitation. Le rang R5 du créateur est attribué côté serveur.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!signedIn) {
                Text("Connecte-toi d'abord dans « Compte & sauvegarde cloud » pour utiliser ces fonctions.",
                    color = Color(0xFFFBBF24), style = MaterialTheme.typography.bodySmall)
            } else {
                if (allianceId.isBlank()) {
                    OutlinedButton(
                        onClick = {
                            allianceId = prefs.getString("cloud_alliance_id", "").orEmpty()
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Actualiser le lien cloud après connexion") }
                    Button(
                        onClick = {
                            scope.launch {
                                busy = true
                                message = null
                                auth.createAlliance(server, allianceName)
                                    .onSuccess { id ->
                                        allianceId = id
                                        prefs.edit().putString("cloud_alliance_id", id).apply()
                                        message = "Alliance créée en ligne. Tu es enregistré comme R5."
                                        isError = false
                                    }
                                    .onFailure {
                                        message = it.message ?: "Création impossible."
                                        isError = true
                                    }
                                busy = false
                            }
                        },
                        enabled = !busy && server.isNotBlank() && allianceName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (busy) "Création…" else "Créer cette alliance dans le cloud") }
                } else {
                    Text("Alliance cloud liée : $allianceId", style = MaterialTheme.typography.bodySmall)
                    if (myCloudRank.isNotBlank()) {
                        Text("Ton rang confirmé dans le cloud : $myCloudRank", color = Color(0xFF86EFAC), style = MaterialTheme.typography.bodySmall)
                    }
                    if (myCloudRank == "R5" || (isAppDeveloper && myCloudRank == "R4")) {
                        Divider()
                        Text(
                            if (isAppDeveloper && myCloudRank == "R4") {
                                "Privilèges développeur · Gestion R5"
                            } else {
                                "Transférer le poste de R5"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (isAppDeveloper && myCloudRank == "R4") {
                                "Ton rang d'alliance reste R4. En tant que développeur unique de l'application, tu peux aussi gérer les fonctions R5. Choisis un membre R4 pour lui transférer le poste R5."
                            } else {
                                "Choisis un membre déjà R4. Après confirmation, il deviendra R5 et ton compte passera R4."
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
                                        transferMessage = "Liste des R4 actualisée."
                                        transferError = false
                                    }.onFailure {
                                        transferMessage = it.message ?: "Chargement des R4 impossible."
                                        transferError = true
                                    }
                                    transferBusy = false
                                }
                            },
                            enabled = !transferBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Actualiser la liste des R4") }
                        if (r4Members.isEmpty()) {
                            Text("Aucun membre R4 disponible dans la liste cloud.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            r4Members.forEach { member ->
                                OutlinedButton(
                                    onClick = { selectedR4 = member },
                                    enabled = !transferBusy,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("${member.playerName} · R4")
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
                                        message = "Code créé. Transmets-le uniquement aux membres de ton alliance."
                                        isError = false
                                    }
                                    .onFailure {
                                        message = it.message ?: "Création du code impossible."
                                        isError = true
                                    }
                                busy = false
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (busy) "Génération…" else "Générer un code d'invitation (105 membres max.)") }
                }
                if (inviteCode.isNotBlank()) {
                    OutlinedTextField(
                        value = inviteCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Code d'invitation") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = inviteInput,
                    onValueChange = { inviteInput = it.trim().take(256) },
                    label = { Text("Code reçu pour rejoindre une alliance") },
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
                                    message = "Invitation acceptée. Ton compte a rejoint l'alliance."
                                    isError = false
                                }
                                .onFailure {
                                    message = it.message ?: "Invitation refusée."
                                    isError = true
                                }
                            busy = false
                        }
                    },
                    enabled = !busy && inviteInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (busy) "Vérification…" else "Rejoindre avec ce code") }
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
            title = { Text("Confirmer le transfert du R5") },
            text = {
                Text("Confirmer que ${candidate.playerName} devient R5 ? Ton propre rang passera automatiquement à R4.")
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
                                    transferMessage = "Transfert effectué. ${candidate.playerName} est le nouveau R5."
                                    transferError = false
                                    selectedR4 = null
                                }
                                .onFailure {
                                    transferMessage = it.message ?: "Transfert impossible."
                                    transferError = true
                                }
                            transferBusy = false
                        }
                    },
                    enabled = !transferBusy
                ) { Text(if (transferBusy) "Transfert…" else "Confirmer le transfert") }
            },
            dismissButton = {
                TextButton(onClick = { selectedR4 = null }, enabled = !transferBusy) { Text("Annuler") }
            }
        )
    }
}

@Composable
private fun ImprovementRequestsScreen(auth: SupabaseAuthRepository) {
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
            }.onFailure { message = it.message ?: "Chargement des propositions impossible."; isError = true }
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
        "bug" to "Bug / problème",
        "idea" to "Nouvelle idée",
        "game_data" to "Données du jeu",
        "interface" to "Interface",
        "performance" to "Performance",
        "other" to "Autre"
    )
    val statusLabels = mapOf(
        "pending" to "En attente",
        "accepted" to "Acceptée",
        "rejected" to "Refusée",
        "needs_info" to "Précisions demandées",
        "in_progress" to "En développement",
        "implemented" to "Installée dans l'application"
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
                    Text("Proposer une amélioration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "Partage une idée, signale un bug ou propose une amélioration. Tu pourras suivre la réponse du développeur ici.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFB8C3D1)
                    )
                    if (session == null) {
                        Text("Connecte-toi à ton compte pour envoyer une proposition.", color = Color(0xFFFBBF24))
                    } else {
                        OutlinedTextField(
                            value = title, onValueChange = { title = it.take(120) },
                            label = { Text("Titre de la proposition") },
                            supportingText = { Text("${title.length}/120") },
                            singleLine = true, modifier = Modifier.fillMaxWidth()
                        )
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                                Text(categoryLabels[category] ?: "Catégorie")
                            }
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                categoryLabels.forEach { (key, label) ->
                                    DropdownMenuItem(text = { Text(label) }, onClick = { category = key; expanded = false })
                                }
                            }
                        }
                        OutlinedTextField(
                            value = description, onValueChange = { description = it.take(4000) },
                            label = { Text("Décris ton idée ou le problème") },
                            supportingText = { Text("${description.length}/4000 — ajoute des exemples si possible.") },
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
                                        message = "Proposition envoyée. Tu recevras la réponse dans cette rubrique."
                                        isError = false
                                        refresh()
                                    }.onFailure {
                                        message = it.message ?: "Envoi impossible."
                                        isError = true
                                    }
                                    sending = false
                                }
                            },
                            enabled = !sending && title.trim().length >= 5 && description.trim().length >= 15,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (sending) CircularProgressIndicator(modifier = Modifier.size(17.dp), strokeWidth = 2.dp)
                            else Text("Envoyer ma proposition")
                        }
                    }
                    message?.let { Text(it, color = if (isError) Color(0xFFFCA5A5) else Color(0xFF86EFAC), style = MaterialTheme.typography.bodySmall) }
                }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isDeveloper) "Toutes les propositions · Développeur" else "Mes propositions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = { refresh() }, enabled = !loading) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Actualiser")
                }
            }
        }

        if (session != null && requests.isEmpty() && !loading) {
            item { Text("Aucune proposition pour le moment.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
                        Text("Compte joueur : ${item.userId.take(8)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (item.aiSummary.isNotBlank()) {
                            Text("Pré-analyse IA : ${item.aiSummary}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (item.developerReply.isNotBlank()) {
                        Text("Réponse du développeur", fontWeight = FontWeight.SemiBold, color = Color(0xFFFFC18A))
                        Text(item.developerReply, style = MaterialTheme.typography.bodySmall)
                    }
                    if (isDeveloper) {
                        OutlinedButton(
                            onClick = { selected = item; reply = item.developerReply },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Traiter cette proposition") }
                    }
                }
            }
        }
    }

    selected?.let { item ->
        var status by remember(item.id) { mutableStateOf(item.status.takeIf { it != "pending" } ?: "accepted") }
        AlertDialog(
            onDismissRequest = { if (!reviewBusy) selected = null },
            title = { Text("Répondre à la proposition") },
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
                        label = { Text("Ta réponse au joueur") },
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
                                    message = "Réponse enregistrée et visible par le joueur."
                                    isError = false
                                    selected = null
                                    refresh()
                                }
                                .onFailure {
                                    message = it.message ?: "Enregistrement impossible."
                                    isError = true
                                }
                            reviewBusy = false
                        }
                    },
                    enabled = !reviewBusy
                ) { Text(if (reviewBusy) "Enregistrement…" else "Envoyer la réponse") }
            },
            dismissButton = {
                TextButton(onClick = { selected = null }, enabled = !reviewBusy) { Text("Annuler") }
            }
        )
    }
}
