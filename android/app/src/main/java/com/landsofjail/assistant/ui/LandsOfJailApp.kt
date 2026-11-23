package com.landsofjail.assistant.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
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
import com.landsofjail.assistant.capture.CapturedScreenPayload
import com.landsofjail.assistant.capture.ScreenCaptureConsentActivity
import com.landsofjail.assistant.data.GeminiAssistantRepository
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

private enum class AssistantTab {
    TABLEAU, HEROS, RESSOURCES, CONSEILS, ALLIANCE, HISTORIQUE;

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
    }
}

private enum class ResourceSubTab {
    INVENTAIRE, BATIMENTS, HEROS_OBJETS, ROUTINE;

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

private val FORTICLAD_BUILDING_OPTIONS = listOf(
    "warden_office" to "Bureau du directeur",
    "shield_barracks" to "Caserne Shieldbearers",
    "bomber_barracks" to "Caserne Bombers",
    "shooter_barracks" to "Caserne Shooters",
    "comms_center" to "Centre de communications",
    "command_center" to "Centre de commandement",
    "medical_station" to "Poste médical",
    "forticlad_lab" to "Laboratoire Forticlad"
)

// Coût par palier (1/5 d'un niveau FC), d'après les tableaux communautaires LOJ XLab.
// Les coûts du poste médical ne sont pas publiés pour FC9/FC10 ; le Laboratoire Forticlad
// est éligible mais son barème détaillé n'est pas disponible dans la source consultée.
private val FORTICLAD_STEP_COSTS: Map<String, Map<Int, Pair<Long, Long>>> = mapOf(
    "warden_office" to mapOf(
        1 to (250L to 0L), 2 to (320L to 0L), 3 to (490L to 0L), 4 to (600L to 0L), 5 to (720L to 0L),
        6 to (360L to 24L), 7 to (400L to 36L), 8 to (460L to 48L), 9 to (510L to 72L), 10 to (630L to 168L)
    ),
    "shield_barracks" to mapOf(
        1 to (130L to 0L), 2 to (150L to 0L), 3 to (230L to 0L), 4 to (250L to 0L), 5 to (300L to 0L),
        6 to (160L to 10L), 7 to (180L to 15L), 8 to (210L to 22L), 9 to (230L to 32L), 10 to (290L to 75L)
    ),
    "bomber_barracks" to mapOf(
        1 to (130L to 0L), 2 to (150L to 0L), 3 to (230L to 0L), 4 to (250L to 0L), 5 to (300L to 0L),
        6 to (160L to 10L), 7 to (180L to 15L), 8 to (210L to 22L), 9 to (230L to 32L), 10 to (290L to 75L)
    ),
    "shooter_barracks" to mapOf(
        1 to (130L to 0L), 2 to (150L to 0L), 3 to (230L to 0L), 4 to (250L to 0L), 5 to (300L to 0L),
        6 to (160L to 10L), 7 to (180L to 15L), 8 to (210L to 22L), 9 to (230L to 32L), 10 to (290L to 75L)
    ),
    "comms_center" to mapOf(
        1 to (50L to 0L), 2 to (60L to 0L), 3 to (80L to 0L), 4 to (110L to 0L), 5 to (130L to 0L),
        6 to (90L to 5L), 7 to (100L to 8L), 8 to (110L to 12L), 9 to (130L to 17L), 10 to (160L to 41L)
    ),
    "command_center" to mapOf(
        1 to (50L to 0L), 2 to (60L to 0L), 3 to (80L to 0L), 4 to (110L to 0L), 5 to (130L to 0L),
        6 to (70L to 5L), 7 to (80L to 8L), 8 to (90L to 12L), 9 to (100L to 14L), 10 to (130L to 34L)
    ),
    "medical_station" to mapOf(
        1 to (50L to 0L), 2 to (60L to 0L), 3 to (80L to 0L), 4 to (110L to 0L), 5 to (130L to 0L),
        6 to (70L to 5L), 7 to (80L to 8L), 8 to (90L to 12L)
    )
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
    val authRepository = remember(context) { SupabaseAuthRepository(context) }
    var authSession by remember { mutableStateOf(authRepository.currentSession()) }

    var currentLang by remember { mutableStateOf(AppLanguage.FR) }
    var currentTab by remember { mutableStateOf(AssistantTab.TABLEAU) }
    var playerRole by remember { mutableStateOf(PlayerRole.POLYVALENT_F2P) }
    var packageFilter by remember { mutableStateOf(UsageStatsDetector.DEFAULT_GAME_PACKAGE_KEYWORD) }
    var alwaysKeepOverlay by remember { mutableStateOf(true) }

    var hasUsagePermission by remember { mutableStateOf(UsageStatsDetector.hasUsageStatsPermission(context)) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val isMonitoringRunning by GameForegroundMonitorService.isRunning.collectAsState()
    val isGameForeground by GameForegroundMonitorService.detectedGameForeground.collectAsState()

    val accountPrefs = remember(context) {
        context.getSharedPreferences("lands_of_jail_account_v1", Context.MODE_PRIVATE)
    }
    val accountJson = remember { Json { ignoreUnknownKeys = true } }
    var accountDataLoaded by remember { mutableStateOf(false) }
    var allianceWorkspace by remember {
        mutableStateOf(runCatching {
            accountJson.decodeFromString<AllianceWorkspace>(
                accountPrefs.getString("alliance_workspace", "{}") ?: "{}"
            )
        }.getOrDefault(AllianceWorkspace()))
    }

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
    var currentPower by remember {
        mutableStateOf(runCatching {
            accountJson.decodeFromString<PowerMetric?>(
                accountPrefs.getString("current_power", "null") ?: "null"
            )
        }.getOrNull())
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
    var selectedAdviceMode by remember { mutableStateOf(AdviceMode.TRIAL_CAGE) }
    var adviceResult by remember { mutableStateOf<TacticalAdviceResult?>(null) }
    var isFetchingAdvice by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsagePermission = UsageStatsDetector.hasUsageStatsPermission(context)
                hasOverlayPermission = Settings.canDrawOverlays(context)
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
                merged[key] = fresh
            } else {
                val skills = (previous.competences + fresh.competences)
                    .associateBy { normalizedKey(it.nom) }
                    .values.toList()
                merged[key] = previous.copy(
                    niveau = fresh.niveau ?: previous.niveau,
                    niveauTexte = fresh.niveauTexte.takeIf {
                        it.isNotBlank() && !it.contains("non lu", true) && !it.contains("inconnu", true)
                    } ?: previous.niveauTexte,
                    rarete = fresh.rarete ?: previous.rarete,
                    etoiles = fresh.etoiles ?: previous.etoiles,
                    roleHero = fresh.roleHero ?: previous.roleHero,
                    puissanceHero = fresh.puissanceHero ?: previous.puissanceHero,
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
                    competences = skills,
                    incertain = fresh.incertain && previous.incertain,
                    raisonIncertitude = fresh.raisonIncertitude ?: previous.raisonIncertitude
                )
            }
        }
        return merged.values.toList()
    }

    fun mergeResourceRecords(existing: List<ResourceRecord>, incoming: List<ResourceRecord>): List<ResourceRecord> {
        fun resourceKey(name: String): String {
            val key = normalizedKey(name.replace(Regex("""\\s*\\([^)]*\\)"""), ""))
            return when (key) {
                "food", "vivres" -> "nourriture"
                "wood", "lumber" -> "bois"
                "steel", "iron", "fer" -> "acier"
                "gasoline", "fuel", "petrole", "pétrole" -> "essence"
                "cash" -> "argent liquide"
                "gold card" -> "carte en or"
                "stamina" -> "endurance"
                "experience" -> "exp"
                else -> key
            }
        }
        val merged = existing.associateBy { resourceKey(it.nom) }.toMutableMap()
        incoming.forEach { fresh ->
            val key = resourceKey(fresh.nom)
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

    fun mergeNewAnalysis(result: ScreenshotAnalysis) {
        analyses = (listOf(result) + analyses).take(50)
        result.puissance?.takeIf { it.puissanceTotaleTexte.isNotBlank() }?.let {
            currentPower = it
        }
        heroes = mergeHeroRecords(heroes, result.heros)
        resources = mergeResourceRecords(resources, result.ressources)
    }

    suspend fun analyzeSingleUriInternal(uri: Uri, onProgress: (Int) -> Unit): ScreenshotAnalysis {
        onProgress(15)
        val bitmap = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()

        onProgress(35)
        val ocrRaw = if (bitmap != null) {
            runCatching { LandsOfJailOcrParser.recognizeBitmap(bitmap) }.getOrDefault("")
        } else {
            ""
        }
        val localExtraction = LandsOfJailOcrParser.parseLocalOcrText(ocrRaw)

        onProgress(55)
        val remote = repository.analyzeSharedScreenshot(
            context,
            uri,
            playerRole,
            "Capture importée : inspecte aussi les icônes et les compteurs de ressources visibles. " + ocrRaw.take(400)
        )
        onProgress(85)

        return remote.fold(
            onSuccess = { remoteAnalysis ->
                // L'OCR local complète toujours l'analyse distante : le profil joueur
                // affiche la puissance près de l'icône du poing, sans libellé textuel.
                val mergedPower = remoteAnalysis.puissance ?: localExtraction.power
                val mergedHeroes = if (remoteAnalysis.heros.isEmpty()) {
                    localExtraction.heroes
                } else {
                    remoteAnalysis.heros
                }
                // Les valeurs locales du tableau « Ressources totales » priment
                // sur les valeurs distantes si les deux lectures trouvent la même ressource.
                val mergedResources = mergeResourceRecords(
                    remoteAnalysis.ressources,
                    localExtraction.resources
                )
                val localPowerSummary = if (
                    remoteAnalysis.puissance == null &&
                    localExtraction.power?.puissanceTotaleTexte?.isNotBlank() == true
                ) {
                    "Puissance détectée localement : ${localExtraction.power.puissanceTotaleTexte}"
                } else {
                    null
                }
                remoteAnalysis.copy(
                    typeCapture = if (
                        remoteAnalysis.typeCapture.equals("autre", true) &&
                        localExtraction.detectedCategory != "ocr_local"
                    ) localExtraction.detectedCategory else remoteAnalysis.typeCapture,
                    resumeCapture = listOf(remoteAnalysis.resumeCapture, localPowerSummary)
                        .filterNot { it.isNullOrBlank() }
                        .distinct()
                        .joinToString(" · "),
                    puissance = mergedPower,
                    heros = mergedHeroes,
                    ressources = mergedResources,
                    champsIncertains = remoteAnalysis.champsIncertains + localExtraction.uncertainItems
                )
            },
            onFailure = {
                val foundParts = buildList {
                    localExtraction.power?.let { add("Puissance : ${it.puissanceTotaleTexte}") }
                    if (localExtraction.heroes.isNotEmpty()) add("${localExtraction.heroes.size} héros")
                    if (localExtraction.resources.isNotEmpty()) add("${localExtraction.resources.size} ressource(s)")
                }
                val summaryLine = if (foundParts.isNotEmpty()) {
                    "${foundParts.joinToString(" · ")} — ${localExtraction.ocrSnippet}"
                } else if (ocrRaw.isNotBlank()) {
                    "OCR local : ${localExtraction.ocrSnippet}"
                } else {
                    "Image importée (aucun texte OCR lisible)"
                }
                ScreenshotAnalysis(
                    id = UUID.randomUUID().toString(),
                    analyzedAtIso = Instant.now().toString(),
                    imageUriString = uri.toString(),
                    typeCapture = localExtraction.detectedCategory,
                    resumeCapture = summaryLine,
                    puissance = localExtraction.power,
                    ressources = localExtraction.resources,
                    heros = localExtraction.heroes,
                    champsIncertains = localExtraction.uncertainItems
                )
            }
        )
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

                currentTab = AssistantTab.TABLEAU
                snackbarHostState.showSnackbar(
                    "Capture analysée 100 % (${result.heros.size} héros, ${result.ressources.size} ressource(s))"
                )

                isFetchingAdvice = true
                repository.fetchTacticalAdvice(
                    mode = selectedAdviceMode,
                    language = currentLang,
                    playerRole = playerRole,
                    heroes = heroes,
                    powerText = currentPower?.puissanceTotaleTexte
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
        if (authSession == null) {
            AuthGateScreen(onSessionChanged = { authSession = it })
        } else {
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
                        Spacer(Modifier.width(4.dp))
                        TextButton(
                            onClick = {
                                scope.launch {
                                    authRepository.signOut()
                                        .onSuccess { authSession = null }
                                        .onFailure { snackbarHostState.showSnackbar(it.message ?: "Déconnexion impossible.") }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Text("Quitter", color = Color(0xFFB8C3D1), fontSize = 11.sp)
                        }
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
                        latestAnalysis = analyses.firstOrNull(),
                        onOpenHistory = { currentTab = AssistantTab.HISTORIQUE },
                        onPickImage = { imagePicker.launch("image/*") }
                    )
                    AssistantTab.HEROS -> HeroesScreen(
                        heroes = heroes,
                        onUpdateHero = { original, updated ->
                            heroes = heroes.map { if (it.nom == original.nom) updated else it }
                        }
                    )
                    AssistantTab.RESSOURCES -> ResourcesScreen(currentLang, currentPower, resources)
                    AssistantTab.CONSEILS -> AdviceScreen(
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
                                    powerText = currentPower?.puissanceTotaleTexte
                                ).onSuccess { adviceResult = it }
                                    .onFailure { errorMessage = it.message }
                                isFetchingAdvice = false
                            }
                        }
                    )
                    AssistantTab.ALLIANCE -> AllianceScreen(
                        workspace = allianceWorkspace,
                        heroes = heroes,
                        currentPowerText = currentPower?.puissanceTotaleTexte.orEmpty(),
                        onSaveWorkspace = { allianceWorkspace = it }
                    )
                    AssistantTab.HISTORIQUE -> HistoryScreen(analyses, onClearHistory = { analyses = emptyList() })
                }
            }
        }
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
    onChangeLang: (AppLanguage) -> Unit,
    hasUsagePermission: Boolean,
    hasOverlayPermission: Boolean,
    alwaysKeepOverlay: Boolean,
    onToggleAlwaysKeepOverlay: (Boolean) -> Unit,
    isMonitoringRunning: Boolean,
    isGameForeground: Boolean,
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
    latestAnalysis: ScreenshotAnalysis?,
    onOpenHistory: () -> Unit,
    onPickImage: () -> Unit
) {
    var showCaptureSettings by remember { mutableStateOf(false) }
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
                        FilledTonalButton(onClick = onAnalyzeScreenNow) {
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
                        TextButton(onClick = onPickImage) {
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
                }
            }
        }

        // Dernière capture analysée (aperçu visuel immédiat + OCR)
        if (latestAnalysis != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2E))
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Dernière capture (${latestAnalysis.typeCapture})",
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF59E0B)
                            )
                            TextButton(onClick = onOpenHistory) {
                                Text("Historique")
                            }
                        }
                        if (latestAnalysis.imageUriString.isNotBlank()) {
                            AsyncImage(
                                model = latestAnalysis.imageUriString,
                                contentDescription = latestAnalysis.resumeCapture,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        }
                        Text(
                            latestAnalysis.resumeCapture,
                            style = MaterialTheme.typography.bodySmall
                        )
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
                        Text(role.labelFr, fontWeight = FontWeight.Medium)
                        Text(role.descriptionFr, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroesScreen(
    heroes: List<HeroRecord>,
    onUpdateHero: (HeroRecord, HeroRecord) -> Unit
) {
    var editingHero by remember { mutableStateOf<HeroRecord?>(null) }
    var search by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf("Tous") }
    val profileByName = remember(heroes) { heroes.associateBy { it.nom.trim().lowercase() } }
    val visibleHeroes = remember(search, selectedClass, heroes) {
        HeroCatalog.entries.filter { entry ->
            (search.isBlank() || entry.nom.contains(search.trim(), ignoreCase = true)) &&
                (selectedClass == "Tous" || entry.typeTroupe == selectedClass)
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Catalogue des héros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "${HeroCatalog.entries.size} héros référencés · ${heroes.size} héros détectés sur ton compte",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Rechercher un héros") },
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
        items(visibleHeroes, key = { it.nom }) { entry ->
            val hero = profileByName[entry.nom.lowercase()]
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(entry.nom, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (hero != null) "Profil détecté" else "Catalogue",
                            color = if (hero != null) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
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
                        TextButton(onClick = { editingHero = hero }) {
                            Text("Modifier les données détectées")
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
                        hero.competences.forEach { skill ->
                            Text(
                                "• ${skill.nom} (${skill.niveau})${if (skill.incertain) " · Incertain" else ""}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (!skill.description.isNullOrBlank()) {
                                Text(skill.description, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        Text("Niveau, étoiles et compétences personnels : à scanner depuis le jeu.", style = MaterialTheme.typography.bodySmall)
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
            item { Text("Aucun héros ne correspond à cette recherche.") }
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
        mutableStateOf(hero.competences.joinToString("\n") { it.nom + " | " + it.niveau + " | " + it.description.orEmpty() })
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
                    label = { Text("Compétences : Nom | niveau | description (une ligne par compétence)") },
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
                        val parts = line.split("|", limit = 3)
                        val skillName = parts.getOrNull(0)?.trim().orEmpty()
                        if (skillName.isBlank()) null else {
                            val previous = oldSkills[skillName.lowercase()]
                            HeroSkill(
                                nom = skillName,
                                niveau = parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() } ?: previous?.niveau.orEmpty(),
                                description = parts.getOrNull(2)?.trim()?.takeIf { it.isNotBlank() } ?: previous?.description,
                                incertain = false
                            )
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
                            incertain = false,
                            raisonIncertitude = null
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
    resources: List<ResourceRecord>
) {
    val context = LocalContext.current
    val routinePrefs = remember(context) { context.getSharedPreferences("offline_routine", Context.MODE_PRIVATE) }
    var subTab by remember { mutableStateOf(ResourceSubTab.INVENTAIRE) }
    var targetBuildingLvl by remember { mutableIntStateOf(16) }
    var selectedBuildingId by remember { mutableStateOf("warden_office") }
    var fcCurrentLevel by remember { mutableIntStateOf(0) }
    var fcFilledSteps by remember { mutableIntStateOf(0) }
    var fcTargetLevel by remember { mutableIntStateOf(1) }
    var fcBuildingIds by remember { mutableStateOf(setOf("warden_office")) }
    var upgradeGoal by remember { mutableStateOf("QG / progression") }
    val buildingOptions = listOf(
        "warden_office" to "Bureau du directeur",
        "comms" to "Centre de communications",
        "shield" to "Caserne Shieldbearers",
        "shooters" to "Caserne Shooters",
        "bombers" to "Caserne Bombers",
        "lab" to "Laboratoire",
        "warehouse" to "Entrepôt"
    )
    val buildingFactor = when (selectedBuildingId) {
        "comms" -> 0.55
        "shield", "shooters", "bombers" -> 0.45
        "lab" -> 0.50
        "warehouse" -> 0.35
        else -> 1.0
    }
    val selectedBuildingLabel = buildingOptions.firstOrNull { it.first == selectedBuildingId }?.second ?: "Bureau du directeur"
    val fcBuildingBreakdown = fcBuildingIds.map { buildingId ->
        var cores = 0L
        var advanced = 0L
        var missing = false
        val table = FORTICLAD_STEP_COSTS[buildingId]
        for (level in (fcCurrentLevel + 1)..fcTargetLevel) {
            val steps = if (level == fcCurrentLevel + 1) (5 - fcFilledSteps).coerceIn(1, 5) else 5
            val stepCost = table?.get(level)
            if (stepCost == null) {
                missing = true
            } else {
                cores += stepCost.first * steps
                advanced += stepCost.second * steps
            }
        }
        Triple(buildingId, Pair(cores, advanced), missing)
    }
    val totalFcCores = fcBuildingBreakdown.sumOf { it.second.first }
    val totalAdvancedFcCores = fcBuildingBreakdown.sumOf { it.second.second }
    val hasMissingFcCosts = fcBuildingBreakdown.any { it.third }
    var starFrom by remember { mutableIntStateOf(0) }
    var starTo by remember { mutableIntStateOf(4) }
    var ownedShardsText by remember { mutableStateOf("0") }
    var tomeFrom by remember { mutableIntStateOf(0) }
    var tomeTo by remember { mutableIntStateOf(5) }

    var routineItems by remember(routinePrefs) {
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

            ResourceSubTab.BATIMENTS -> {
                val req = WARDEN_LEVELS_TABLE.firstOrNull { it.level == targetBuildingLvl }
                    ?: WARDEN_LEVELS_TABLE.first()
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Calculateur de bâtiments · niveaux 1 à 30", fontWeight = FontWeight.SemiBold)
                            Text("Choisis le bâtiment à estimer. Le barème détaillé disponible concerne le Bureau du directeur ; les autres coûts sont des estimations proportionnelles à confirmer en jeu.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFBBF24))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(buildingOptions, key = { it.first }) { option ->
                                    FilterChip(
                                        selected = selectedBuildingId == option.first,
                                        onClick = { selectedBuildingId = option.first },
                                        label = { Text(option.second, maxLines = 1) }
                                    )
                                }
                            }
                            Text("$selectedBuildingLabel → Niv. $targetBuildingLvl", fontWeight = FontWeight.SemiBold)
                            if (selectedBuildingId == "warden_office" && targetBuildingLvl >= 9) {
                                Text("Règle d'or : gardez le Centre de communications au niveau ${targetBuildingLvl - 1}.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFBBF24))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { if (targetBuildingLvl > 2) targetBuildingLvl-- }, enabled = targetBuildingLvl > 2) { Text("-1 Niveau") }
                                Button(onClick = { if (targetBuildingLvl < 30) targetBuildingLvl++ }, enabled = targetBuildingLvl < 30) { Text("+1 Niveau") }
                            }
                            Text(
                                "Prérequis : " + if (selectedBuildingId == "warden_office") req.prereqs else "À vérifier dans l'écran du bâtiment en jeu.",
                                fontWeight = FontWeight.Medium
                            )
                            Text("Nourriture estimée : ${formatCompactFr((req.food * buildingFactor).toLong())}")
                            Text("Bois estimé : ${formatCompactFr((req.wood * buildingFactor).toLong())}")
                            Text("Acier estimé : ${formatCompactFr((req.steel * buildingFactor).toLong())}")
                            Text("Essence estimée : ${formatCompactFr((req.gasoline * buildingFactor).toLong())}")
                            Text(
                                if (selectedBuildingId == "warden_office" && req.verified) "Source : barème Warden vérifié"
                                else "Source : estimation · confirmer les coûts et prérequis dans le jeu",
                                color = Color(0xFFFBBF24),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text("Calculateur Forticlad · FC1 à FC5", fontWeight = FontWeight.Bold)
                            Text("Sélectionne précisément les bâtiments qui doivent passer au Forticlad. Le calcul additionne les coûts pour tous les bâtiments cochés.", style = MaterialTheme.typography.bodySmall)
                            Text("Bâtiments à inclure", fontWeight = FontWeight.SemiBold)
                            FORTICLAD_BUILDING_OPTIONS.forEach { option ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = fcBuildingIds.contains(option.first),
                                        onCheckedChange = { checked ->
                                            fcBuildingIds = if (checked) fcBuildingIds + option.first else fcBuildingIds - option.first
                                        }
                                    )
                                    Text(option.second, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(onClick = {
                                    fcCurrentLevel = if (fcCurrentLevel >= 9) 0 else fcCurrentLevel + 1
                                    fcFilledSteps = 0
                                    fcTargetLevel = (fcCurrentLevel + 1).coerceAtMost(10)
                                }) { Text(if (fcCurrentLevel == 0) "Actuel : Niv. 30" else "Actuel : FC$fcCurrentLevel") }
                                OutlinedButton(onClick = { fcFilledSteps = (fcFilledSteps + 1) % 5 }) {
                                    Text("Paliers faits : $fcFilledSteps/5")
                                }
                                Button(onClick = {
                                    fcTargetLevel = if (fcTargetLevel >= 10) (fcCurrentLevel + 1).coerceAtMost(10) else fcTargetLevel + 1
                                }) { Text("Cible : FC$fcTargetLevel") }
                            }
                            Text("Bâtiments sélectionnés : ${fcBuildingIds.size}", style = MaterialTheme.typography.labelMedium)
                            Text("Forticlad Cores nécessaires : ${formatCompactFr(totalFcCores)}", fontWeight = FontWeight.SemiBold, color = Color(0xFFFFB36B))
                            Text("Advanced Forticlad Cores : ${formatCompactFr(totalAdvancedFcCores)}", fontWeight = FontWeight.SemiBold)
                            fcBuildingBreakdown.forEach { row ->
                                val label = FORTICLAD_BUILDING_OPTIONS.firstOrNull { it.first == row.first }?.second ?: row.first
                                Text("$label : ${formatCompactFr(row.second.first)} FC · ${formatCompactFr(row.second.second)} AFC" + if (row.third) " · données manquantes" else "", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("Calcul : FC$fcCurrentLevel + $fcFilledSteps/5 paliers → FC$fcTargetLevel. Chaque niveau comprend cinq paliers.", style = MaterialTheme.typography.bodySmall)
                            Text(
                                if (hasMissingFcCosts) "Calcul partiel : le barème du Laboratoire Forticlad ou certains coûts du Poste médical FC9/FC10 ne sont pas publiés dans la source. Le total affiché exclut ces valeurs manquantes."
                                else "Source : coûts par bâtiment et par palier LOJ XLab ; confirmez les valeurs dans le jeu après chaque mise à jour.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFBBF24)
                            )
                        }
                    }
                }
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                            Text("Priorité d'évolution", fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf("QG / progression", "Trial Cage", "PvP / Crystal Clash / SvS", "Économie / F2P")) { goal ->
                                    FilterChip(selected = upgradeGoal == goal, onClick = { upgradeGoal = goal }, label = { Text(goal, maxLines = 1) })
                                }
                            }
                            val priorityText = when (upgradeGoal) {
                                "Trial Cage" -> "1. Caserne Shooters et capacité de marche. 2. Recherches offensives du Laboratoire. 3. Héros apportant dégâts directs, bonus d'attaque ou réduction de DEF ennemie. 4. Autres casernes selon le ratio utilisé. Les bonus purement défensifs ne sont pas prioritaires pour le score de dégâts."
                                "PvP / Crystal Clash / SvS" -> "1. Caserne Shieldbearers pour la première ligne. 2. Caserne Shooters pour le DPS. 3. Laboratoire : attaque, défense, PV et capacité de marche. 4. Héros principaux et compétences adaptées à l'événement. 5. Progression QG/Comms selon les prérequis."
                                "Économie / F2P" -> "1. Centre de communications et Bureau du directeur selon les prérequis. 2. Laboratoire : recherches permanentes de construction et collecte. 3. Une caserne principale liée à votre meilleure équipe. 4. Quelques héros clés. 5. Bâtiments secondaires uniquement si nécessaires."
                                else -> "1. Centre de communications au niveau requis (N-1). 2. Bureau du directeur. 3. Bâtiment secondaire demandé par le prochain palier. 4. Laboratoire pour les recherches utiles. 5. Entrepôt et autres bâtiments selon les besoins."
                            }
                            Text(priorityText, style = MaterialTheme.typography.bodySmall)
                            Text("Cet ordre est une recommandation générale. Vérifie toujours les prérequis et les coûts exacts dans le jeu avant de lancer une amélioration.", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFBBF24))
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
            Text("Rôle : ${playerRole.labelFr}", style = MaterialTheme.typography.bodySmall)
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
private fun AuthGateScreen(onSessionChanged: (SupabaseAuthRepository.Session?) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(22.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("LANDS OF JAIL", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
        Text("ASSISTANT TACTIQUE", color = Color(0xFFFF9A55), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(12.dp))
        Text(
            "Connecte-toi ou crée ton compte pour retrouver ton profil joueur, ta puissance, tes ressources et tes objectifs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(14.dp))
        SupabaseAccountCard(onSessionChanged = onSessionChanged)
        Text("Tes données de jeu locales seront conservées sur cet appareil.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SupabaseAccountCard(onSessionChanged: (SupabaseAuthRepository.Session?) -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember(context) { SupabaseAuthRepository(context) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var session by remember { mutableStateOf(auth.currentSession()) }
    var createAccount by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

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
                                    .onSuccess { session = null; onSessionChanged(null); message = "Déconnexion effectuée."; isError = false }
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
                                val result = if (createAccount) auth.signUp(email, password) else auth.signIn(email, password)
                                result.onSuccess { outcome ->
                                    session = outcome.session ?: auth.currentSession()
                                    onSessionChanged(session)
                                    message = outcome.message
                                    isError = false
                                }.onFailure {
                                    message = it.message ?: "Une erreur est survenue."
                                    isError = true
                                }
                                busy = false
                            }
                        },
                        enabled = !busy && email.isNotBlank() && password.length >= 8
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
    workspace: AllianceWorkspace,
    heroes: List<HeroRecord>,
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
    var cloudSyncBusy by remember { mutableStateOf(false) }
    var cloudSyncMessage by remember { mutableStateOf<String?>(null) }
    var cloudSyncError by remember { mutableStateOf(false) }
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
    var selectedMemberIds by remember { mutableStateOf((listOf(workspace.ownProfile.id.ifBlank { "own-profile" }) + workspace.members.map { it.id }).filter { it.isNotBlank() }.toSet()) }
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 20.dp)
    ) {
        item { SupabaseAccountCard() }
        item { AllianceCloudControls(server = server, allianceName = allianceName) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Espace Alliance", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Inscris ton profil de jeu et rassemble les capacités de tes membres pour préparer les événements.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Inscription du joueur", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = playerName,
                        onValueChange = { playerName = it },
                        label = { Text("Pseudo dans le jeu") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = server,
                        onValueChange = { server = it.filter(Char::isDigit).take(6) },
                        label = { Text("Serveur (ex. 311)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = allianceName,
                        onValueChange = { allianceName = it },
                        label = { Text("Nom / tag de l'alliance") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ownRobotSummary,
                        onValueChange = { ownRobotSummary = it },
                        label = { Text("Ton robot / bonus actif (facultatif)") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Ton rang dans le jeu", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(AllianceRank.entries) { item ->
                            FilterChip(
                                selected = rank == item,
                                onClick = { rank = item },
                                label = { Text(item.labelFr) }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val profile = AlliancePlayerProfile(
                                id = workspace.ownProfile.id.ifBlank { UUID.randomUUID().toString() },
                                playerName = playerName.trim(),
                                server = server.trim(),
                                allianceName = allianceName.trim(),
                                rank = rank,
                                powerText = currentPowerText.ifBlank { workspace.ownProfile.powerText },
                                heroSkillSummary = ownHeroSummary,
                                robotSummary = ownRobotSummary.trim(),
                                updatedAtIso = Instant.now().toString()
                            )
                            onSaveWorkspace(workspace.copy(
                                server = server.trim(),
                                allianceName = allianceName.trim(),
                                ownProfile = profile
                            ))
                        },
                        enabled = playerName.isNotBlank() && server.isNotBlank() && allianceName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Enregistrer mon inscription") }
                    OutlinedButton(
                        onClick = {
                            if (cloudAuth.currentSession() == null) {
                                cloudSyncMessage = "Connecte-toi à ton compte cloud avant de synchroniser."
                                cloudSyncError = true
                            } else {
                                val profile = AlliancePlayerProfile(
                                    id = workspace.ownProfile.id.ifBlank { UUID.randomUUID().toString() },
                                    playerName = playerName.trim(),
                                    server = server.trim(),
                                    allianceName = allianceName.trim(),
                                    rank = rank,
                                    powerText = currentPowerText.ifBlank { workspace.ownProfile.powerText },
                                    heroSkillSummary = ownHeroSummary,
                                    robotSummary = ownRobotSummary.trim(),
                                    updatedAtIso = Instant.now().toString()
                                )
                                onSaveWorkspace(workspace.copy(
                                    server = server.trim(),
                                    allianceName = allianceName.trim(),
                                    ownProfile = profile
                                ))
                                scope.launch {
                                    cloudSyncBusy = true
                                    cloudSyncMessage = null
                                    cloudAuth.syncPlayerProfile(profile, heroes)
                                        .onSuccess {
                                            cloudSyncMessage = "Profil et ${heroes.size} héros synchronisés avec ton compte."
                                            cloudSyncError = false
                                        }
                                        .onFailure {
                                            cloudSyncMessage = it.message ?: "Synchronisation impossible."
                                            cloudSyncError = true
                                        }
                                    cloudSyncBusy = false
                                }
                            }
                        },
                        enabled = !cloudSyncBusy && playerName.isNotBlank() && server.isNotBlank() && allianceName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (cloudSyncBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (cloudSyncBusy) "Synchronisation…" else "Synchroniser mon profil et mes héros")
                    }
                    cloudSyncMessage?.let {
                        Text(
                            it,
                            color = if (cloudSyncError) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        if (isOfficer) "Rang déclaré R4/R5 : gestion du roster et simulations activées sur cet appareil. Vérification du rang requise pour l’espace cloud partagé."
                        else "Rang déclaré R1/R2/R3 : tu peux enregistrer ton profil. La gestion du roster et les simulations sont réservées aux R4/R5.",
                        color = if (isOfficer) Color(0xFF34D399) else Color(0xFFFBBF24),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
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
                        Text("Roster de l'alliance", fontWeight = FontWeight.Bold)
                        Text("${workspace.members.size + if (workspace.ownProfile.playerName.isNotBlank()) 1 else 0} joueur(s)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall)
                    }
                    Text(
                        "Serveur ${workspace.server.ifBlank { "—" }} · Alliance ${workspace.allianceName.ifBlank { "—" }}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (workspace.ownProfile.playerName.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = selectedMemberIds.contains(workspace.ownProfile.id.ifBlank { "own-profile" }),
                                onCheckedChange = { checked ->
                                    val id = workspace.ownProfile.id.ifBlank { "own-profile" }
                                    selectedMemberIds = if (checked) selectedMemberIds + id else selectedMemberIds - id
                                    simulation = emptyList()
                                }
                            )
                            Text(
                                "Inclure mon profil : ${workspace.ownProfile.playerName} · ${workspace.ownProfile.rank.labelFr} · ${heroes.size} héros",
                                color = Color(0xFF34D399),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (isOfficer) {
                        Button(onClick = { showAddMember = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.PersonAdd, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Ajouter un membre au roster")
                        }
                    } else {
                        Text("Demande à un R4/R5 d'ajouter les profils des membres.", style = MaterialTheme.typography.bodySmall)
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
                        Text(if (cloudRosterBusy) "Chargement du roster…" else "Actualiser depuis le cloud")
                    }
                    cloudRosterMessage?.let {
                        Text(
                            it,
                            color = if (cloudRosterError) Color(0xFFFCA5A5) else Color(0xFF86EFAC),
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
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = selectedMemberIds.contains(member.id),
                                onCheckedChange = { checked ->
                                    selectedMemberIds = if (checked) selectedMemberIds + member.id else selectedMemberIds - member.id
                                    simulation = emptyList()
                                }
                            )
                            Text(member.playerName, fontWeight = FontWeight.SemiBold)
                        }
                        Text(member.rank.labelFr, color = Color(0xFFFFB36B), style = MaterialTheme.typography.labelSmall)
                    }
                    Text("Puissance : ${member.powerText.ifBlank { "non renseignée" }}", style = MaterialTheme.typography.bodySmall)
                    if (member.heroSkillSummary.isNotBlank()) Text("Héros / compétences : ${member.heroSkillSummary}", style = MaterialTheme.typography.bodySmall)
                    if (member.robotSummary.isNotBlank()) Text("Robot : ${member.robotSummary}", style = MaterialTheme.typography.bodySmall)
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
                    Text("Simulateur de performance", fontWeight = FontWeight.Bold)
                    Text(
                        "Sélectionne plusieurs joueurs dans le roster ci-dessus : seuls les profils cochés seront utilisés pour cette simulation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedMemberIds = (listOf(workspace.ownProfile.id.ifBlank { "own-profile" }) + workspace.members.map { it.id }).filter { id ->
                                    id == "own-profile" && workspace.ownProfile.playerName.isNotBlank() || workspace.members.any { it.id == id }
                                }.toSet()
                                simulation = emptyList()
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Tout sélectionner") }
                        OutlinedButton(
                            onClick = { selectedMemberIds = emptySet(); simulation = emptyList() },
                            modifier = Modifier.weight(1f)
                        ) { Text("Tout désélectionner") }
                    }
                    Text("Sélection : ${selectedMemberIds.size} membre(s)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(AdviceMode.entries) { event ->
                            FilterChip(
                                selected = selectedEvent == event,
                                onClick = { selectedEvent = event; simulation = emptyList() },
                                label = { Text(event.titleFor(AppLanguage.FR), maxLines = 1) }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val allRoster = buildList {
                                if (workspace.ownProfile.playerName.isNotBlank()) {
                                    add(workspace.ownProfile.copy(
                                        id = workspace.ownProfile.id.ifBlank { "own-profile" },
                                        server = workspace.server,
                                        allianceName = workspace.allianceName,
                                        powerText = currentPowerText.ifBlank { workspace.ownProfile.powerText },
                                        heroSkillSummary = ownHeroSummary
                                    ))
                                }
                                addAll(workspace.members)
                            }
                            val roster = allRoster.filter { selectedMemberIds.contains(it.id) }
                            if (roster.isEmpty()) {
                                simulation = listOf(if (allRoster.isEmpty()) "Aucun joueur enregistré. Inscris ton profil et ajoute les membres." else "Aucun membre sélectionné. Coche au moins un joueur dans le roster.")
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
                                output += "Événement : ${selectedEvent.titleFor(AppLanguage.FR)}"
                                output += "Effectif analysé : ${roster.size} membre(s)"
                                leader?.let {
                                    output += "LEADER proposé : ${it.playerName} (${it.rank.labelFr}) — puissance ${it.powerText.ifBlank { "non renseignée" }}"
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
    val signedIn = auth.currentSession() != null

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
}
