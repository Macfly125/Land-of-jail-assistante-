package com.landsofjail.assistant.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.landsofjail.assistant.model.*
import com.landsofjail.assistant.ocr.LandsOfJailOcrParser
import com.landsofjail.assistant.service.GameForegroundMonitorService
import com.landsofjail.assistant.service.UsageStatsDetector
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.coroutines.CompletableDeferred
import java.time.Instant
import java.util.UUID

private enum class AssistantTab {
    TABLEAU, HEROS, RESSOURCES, CONSEILS, HISTORIQUE;

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
    val pausedRemainingMs: Long? = null
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

    var currentLang by remember { mutableStateOf(AppLanguage.FR) }
    var currentTab by remember { mutableStateOf(AssistantTab.TABLEAU) }
    var playerRole by remember { mutableStateOf(PlayerRole.POLYVALENT_F2P) }
    var packageFilter by remember { mutableStateOf(UsageStatsDetector.DEFAULT_GAME_PACKAGE_KEYWORD) }
    var alwaysKeepOverlay by remember { mutableStateOf(true) }

    var hasUsagePermission by remember { mutableStateOf(UsageStatsDetector.hasUsageStatsPermission(context)) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    val isMonitoringRunning by GameForegroundMonitorService.isRunning.collectAsState()
    val isGameForeground by GameForegroundMonitorService.detectedGameForeground.collectAsState()

    var analyses by remember { mutableStateOf<List<ScreenshotAnalysis>>(emptyList()) }
    var heroes by remember { mutableStateOf<List<HeroRecord>>(emptyList()) }
    var robots by remember { mutableStateOf<List<RobotRecord>>(emptyList()) }
    var reviewRobots by remember { mutableStateOf<List<RobotRecord>>(emptyList()) }
    var pendingReview by remember { mutableStateOf<ScreenshotAnalysis?>(null) }
    var reviewHeroes by remember { mutableStateOf<List<HeroRecord>>(emptyList()) }
    var pendingReviewCompletion by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }
    var resources by remember { mutableStateOf<List<ResourceRecord>>(emptyList()) }
    var currentPower by remember { mutableStateOf<PowerMetric?>(null) }

    var isAnalyzing by remember { mutableStateOf(false) }
    var singleAnalysisPercent by remember { mutableIntStateOf(0) }
    var analysisQueue by remember { mutableStateOf<List<AndroidQueuedUri>>(emptyList()) }
    var isQueueRunning by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedAdviceMode by remember { mutableStateOf(AdviceMode.TRIAL_CAGE) }
    var selectedEventPosition by remember { mutableStateOf("joiner") }
    var selectedGorilla by remember { mutableStateOf("") }
    var adviceResult by remember { mutableStateOf<TacticalAdviceResult?>(null) }
    var isFetchingAdvice by remember { mutableStateOf(false) }
    val gorillaJson = remember { Json { ignoreUnknownKeys = true } }
    var gorillaRuns by remember {
        mutableStateOf(
            runCatching {
                val saved = context.getSharedPreferences("loj_assistant", android.content.Context.MODE_PRIVATE)
                    .getString("gorilla_damage_log_v1", "[]") ?: "[]"
                gorillaJson.decodeFromString<List<GorillaDamageRecord>>(saved)
            }.getOrDefault(emptyList())
        )
    }
    LaunchedEffect(gorillaRuns) {
        runCatching {
            context.getSharedPreferences("loj_assistant", android.content.Context.MODE_PRIVATE)
                .edit()
                .putString("gorilla_damage_log_v1", gorillaJson.encodeToString(gorillaRuns))
                .apply()
        }
    }

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

    fun mergeNewAnalysis(result: ScreenshotAnalysis) {
        analyses = listOf(result) + analyses
        result.puissance?.let { currentPower = it }
        if (result.heros.isNotEmpty()) {
            val merged = (heroes + result.heros).associateBy { it.nom.lowercase() }
            heroes = merged.values.toList()
        }
        if (result.robots.isNotEmpty()) {
            robots = (robots + result.robots).associateBy { it.nom.trim().lowercase() }.values.toList()
        }
        if (result.ressources.isNotEmpty()) {
            val merged = (resources + result.ressources).associateBy { it.nom.lowercase() }
            resources = merged.values.toList()
        }
    }

    suspend fun analyzeSingleUriInternal(uri: Uri, onProgress: (Int) -> Unit): ScreenshotAnalysis {
        onProgress(15)
        val remote = repository.analyzeSharedScreenshot(context, uri, playerRole, null)
        onProgress(55)
        return remote.getOrElse {
            val bitmap = runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
            onProgress(65)
            val ocrRaw = if (bitmap != null) {
                runCatching { LandsOfJailOcrParser.recognizeBitmap(bitmap) }.getOrDefault("")
            } else {
                ""
            }
            onProgress(85)
            val localExtraction = LandsOfJailOcrParser.parseLocalOcrText(ocrRaw)
            val foundParts = buildList {
                localExtraction.power?.let { add("Puissance : ${it.puissanceTotaleTexte}") }
                if (localExtraction.heroes.isNotEmpty()) {
                    add("${localExtraction.heroes.size} héros")
                }
                if (localExtraction.resources.isNotEmpty()) {
                    add("${localExtraction.resources.size} ressource(s)")
                }
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
                        // Chaque capture importée passe par la même vérification que l'analyse d'écran.
                        val reviewCompletion = CompletableDeferred<Boolean>()
                        pendingReviewCompletion = reviewCompletion
                        pendingReview = analysis
                        reviewHeroes = analysis.heros
                        reviewRobots = analysis.robots
                        currentTab = AssistantTab.TABLEAU
                        val approved = reviewCompletion.await()
                        pendingReviewCompletion = null
                        analysisQueue = analysisQueue.map {
                            if (it.id == nextItem.id) {
                                it.copy(
                                    status = if (approved) "done" else "skipped",
                                    progress = 100,
                                    summary = if (approved) analysis.resumeCapture else "Capture ignorée par l'utilisateur"
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
                    )
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
                pendingReview = result
                reviewHeroes = result.heros
                reviewRobots = result.robots
                AdviceMode.entries.firstOrNull { it.apiId == result.typeCapture }?.let {
                    selectedAdviceMode = it
                }
                currentTab = AssistantTab.TABLEAU
                snackbarHostState.showSnackbar(
                    "Analyse terminée : vérifie les héros avant l'enregistrement."
                )
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
            primary = Color(0xFFF59E0B),
            background = Color(0xFF0B0F17),
            surface = Color(0xFF121826),
            onBackground = Color(0xFFF1F5F9),
            onSurface = Color(0xFFF1F5F9)
        )
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Lands of Jail",
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        TextButton(onClick = {
                            context.startActivity(
                                Intent(context, ScreenCaptureConsentActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }) {
                            Text(
                                when (currentLang) {
                                    AppLanguage.FR -> "Analyser l’écran"
                                    AppLanguage.TR -> "Ekranı Tara"
                                    AppLanguage.RU -> "Сканировать"
                                    AppLanguage.ZH -> "扫描屏幕"
                                    AppLanguage.DE -> "Scannen"
                                    AppLanguage.EN -> "Scan screen"
                                },
                                maxLines = 1
                            )
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    AssistantTab.entries.forEach { tab ->
                        val icon = when (tab) {
                            AssistantTab.TABLEAU -> Icons.Outlined.Dashboard
                            AssistantTab.HEROS -> Icons.Outlined.Groups
                            AssistantTab.RESSOURCES -> Icons.Outlined.Inventory2
                            AssistantTab.CONSEILS -> Icons.Outlined.MilitaryTech
                            AssistantTab.HISTORIQUE -> Icons.Outlined.History
                        }
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(icon, contentDescription = tab.titleFor(currentLang)) },
                            label = {
                                Text(
                                    text = tab.titleFor(currentLang),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 10.sp
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
                        analysesCount = analyses.size,
                        latestAnalysis = analyses.firstOrNull(),
                        onOpenHistory = { currentTab = AssistantTab.HISTORIQUE },
                        onPickImage = { imagePicker.launch("image/*") }
                    )
                    AssistantTab.HEROS -> HeroesScreen(heroes)
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
                        eventPosition = selectedEventPosition,
                        onEventPositionChange = { selectedEventPosition = it; adviceResult = null },
                        selectedGorilla = selectedGorilla,
                        onGorillaChange = { selectedGorilla = it; adviceResult = null },
                        gorillaRuns = gorillaRuns,
                        onAddGorillaRun = { run -> gorillaRuns = (listOf(run) + gorillaRuns).take(100) },
                        onClearGorillaRuns = { gorillaRuns = emptyList() },
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
                                    robots = robots,
                                    powerText = currentPower?.puissanceTotaleTexte,
                                    eventPosition = selectedEventPosition,
                                    gorillaName = selectedGorilla.takeIf { selectedAdviceMode == AdviceMode.GORILLA_LEGION }
                                ).onSuccess { adviceResult = it }
                                    .onFailure { errorMessage = it.message }
                                isFetchingAdvice = false
                            }
                        }
                    )
                    AssistantTab.HISTORIQUE -> HistoryScreen(analyses)
                }

                pendingReview?.let { review ->
                    AlertDialog(
                        onDismissRequest = {
                            pendingReview = null
                            reviewHeroes = emptyList()
                            reviewRobots = emptyList()
                            pendingReviewCompletion?.complete(false)
                        },
                        title = { Text("Vérifier les héros détectés") },
                        text = {
                            Column(
                                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Corrige les informations à partir de la capture originale. Rien ne sera enregistré avant confirmation.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFBBF24)
                                )
                                if (reviewHeroes.isEmpty()) {
                                    Text("Aucun héros détecté. Tu peux confirmer pour enregistrer les autres données.")
                                }
                                Text("Robots / compagnons", fontWeight = FontWeight.SemiBold)
                                if (reviewRobots.isEmpty()) {
                                    Text("Aucun robot détecté automatiquement. Tu peux l'ajouter et compléter ses informations depuis ta capture.", style = MaterialTheme.typography.bodySmall)
                                }
                                reviewRobots.forEachIndexed { index, robot ->
                                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF172033)), modifier = Modifier.fillMaxWidth()) {
                                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                            Text("Robot ${index + 1}", fontWeight = FontWeight.SemiBold)
                                            OutlinedTextField(value = robot.nom, onValueChange = { value -> reviewRobots = reviewRobots.toMutableList().also { it[index] = robot.copy(nom = value) } }, label = { Text("Nom du robot") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = robot.niveau.orEmpty(), onValueChange = { value -> reviewRobots = reviewRobots.toMutableList().also { it[index] = robot.copy(niveau = value) } }, label = { Text("Niveau") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = robot.puissance.orEmpty(), onValueChange = { value -> reviewRobots = reviewRobots.toMutableList().also { it[index] = robot.copy(puissance = value) } }, label = { Text("Puissance") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                            OutlinedTextField(value = robot.rarete.orEmpty(), onValueChange = { value -> reviewRobots = reviewRobots.toMutableList().also { it[index] = robot.copy(rarete = value) } }, label = { Text("Rareté") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                            robot.competences.forEachIndexed { skillIndex, skill ->
                                                OutlinedTextField(value = skill.niveau, onValueChange = { value -> reviewRobots = reviewRobots.toMutableList().also { list -> val current = list[index]; list[index] = current.copy(competences = current.competences.toMutableList().also { skills -> skills[skillIndex] = skill.copy(niveau = value) }) } }, label = { Text(skill.nom) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                                            }
                                            if (robot.incertain) Text(robot.raisonIncertitude ?: "Informations à vérifier", color = Color(0xFFFBBF24), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                TextButton(onClick = { reviewRobots = reviewRobots + RobotRecord(nom = "") }) { Text("＋ Ajouter un robot") }
                                reviewHeroes.forEachIndexed { index, hero ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF172033)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                            Text("Héros ${index + 1}", fontWeight = FontWeight.SemiBold)
                                            OutlinedTextField(
                                                value = hero.nom,
                                                onValueChange = { value ->
                                                    reviewHeroes = reviewHeroes.toMutableList().also { it[index] = hero.copy(nom = value) }
                                                },
                                                label = { Text("Nom") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            OutlinedTextField(
                                                value = hero.niveau?.toString() ?: "",
                                                onValueChange = { value ->
                                                    val level = value.toIntOrNull()
                                                    reviewHeroes = reviewHeroes.toMutableList().also {
                                                        it[index] = hero.copy(niveau = level, niveauTexte = level?.let { "Niv. $it" } ?: "Inconnu")
                                                    }
                                                },
                                                label = { Text("Niveau") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            OutlinedTextField(
                                                value = hero.etoiles.orEmpty(),
                                                onValueChange = { value -> reviewHeroes = reviewHeroes.toMutableList().also { it[index] = hero.copy(etoiles = value) } },
                                                label = { Text("Étoiles") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            OutlinedTextField(
                                                value = hero.puissanceHero.orEmpty(),
                                                onValueChange = { value -> reviewHeroes = reviewHeroes.toMutableList().also { it[index] = hero.copy(puissanceHero = value) } },
                                                label = { Text("Puissance") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            hero.competences.forEachIndexed { skillIndex, skill ->
                                                OutlinedTextField(
                                                    value = skill.niveau,
                                                    onValueChange = { value ->
                                                        reviewHeroes = reviewHeroes.toMutableList().also { list ->
                                                            val current = list[index]
                                                            list[index] = current.copy(
                                                                competences = current.competences.toMutableList().also { skills ->
                                                                    skills[skillIndex] = skill.copy(niveau = value)
                                                                }
                                                            )
                                                        }
                                                    },
                                                    label = { Text(skill.nom) },
                                                    singleLine = true,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            if (hero.incertain) {
                                                Text(hero.raisonIncertitude ?: "Lecture incertaine : vérifie ce héros.", color = Color(0xFFFBBF24), style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                enabled = reviewHeroes.all { it.nom.isNotBlank() && (it.niveau == null || it.niveau in 1..999) } && reviewRobots.all { it.nom.isNotBlank() },
                                onClick = {
                                    val confirmed = review.copy(robots = reviewRobots.map { robot -> robot.copy(nom = robot.nom.trim(), incertain = robot.niveau.isNullOrBlank() || robot.competences.any { it.niveau.isBlank() || it.niveau.equals("Inconnu", true) }) }, heros = reviewHeroes.map { hero ->
                                        hero.copy(
                                            nom = hero.nom.trim(),
                                            incertain = false,
                                            raisonIncertitude = null,
                                            competences = hero.competences.map { skill ->
                                                skill.copy(incertain = skill.niveau.isBlank() || skill.niveau.equals("Inconnu", true))
                                            }
                                        )
                                    })
                                    mergeNewAnalysis(confirmed)
                                    pendingReview = null
                                    reviewHeroes = emptyList()
                                    reviewRobots = emptyList()
                                    pendingReviewCompletion?.complete(true)
                                    scope.launch {
                                        isFetchingAdvice = true
                                        repository.fetchTacticalAdvice(
                                            mode = selectedAdviceMode,
                                            language = currentLang,
                                            playerRole = playerRole,
                                            heroes = (heroes + confirmed.heros).associateBy { it.nom.lowercase() }.values.toList(),
                                            robots = (robots + confirmed.robots).associateBy { it.nom.lowercase() }.values.toList(),
                                            powerText = confirmed.puissance?.puissanceTotaleTexte ?: currentPower?.puissanceTotaleTexte
                                        ).onSuccess { adviceResult = it }
                                            .onFailure { errorMessage = it.message }
                                        isFetchingAdvice = false
                                        snackbarHostState.showSnackbar("Capture vérifiée et enregistrée.")
                                    }
                                }
                            ) { Text("Confirmer et enregistrer") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                    pendingReview = null
                                    reviewHeroes = emptyList()
                                    reviewRobots = emptyList()
                                    pendingReviewCompletion?.complete(false)
                                }) {
                                Text("Ignorer")
                            }
                        }
                    )
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
    analysesCount: Int,
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
                    Text("Héros suivis : $heroesCount · Ressources : $resourcesCount · Captures : $analysesCount")
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
private fun HeroesScreen(heroes: List<HeroRecord>) {
    if (heroes.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Aucun héros extrait. Appuyez sur « Analyser l’écran » sur votre fiche héros.")
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(heroes) { hero ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${hero.nom} · ${hero.niveauTexte}", fontWeight = FontWeight.SemiBold)
                    if (hero.incertain) {
                        Text(
                            "Information incertaine : ${hero.raisonIncertitude ?: "vérification conseillée"}",
                            color = Color(0xFFFBBF24),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    hero.competences.forEach { skill ->
                        Text("• ${skill.nom} (${skill.niveau})${if (skill.incertain) " [Incertain]" else ""}")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResourcesScreen(
    currentLang: AppLanguage,
    power: PowerMetric?,
    resources: List<ResourceRecord>
) {
    var subTab by remember { mutableStateOf(ResourceSubTab.INVENTAIRE) }
    var targetBuildingLvl by remember { mutableIntStateOf(16) }
    var starFrom by remember { mutableIntStateOf(0) }
    var starTo by remember { mutableIntStateOf(4) }
    var ownedShardsText by remember { mutableStateOf("0") }
    var tomeFrom by remember { mutableIntStateOf(0) }
    var tomeTo by remember { mutableIntStateOf(5) }

    var routineItems by remember {
        mutableStateOf(
            listOf(
                AndroidRoutineItem("g1", "Marche Récolte #1 (Vivres/Bois/Acier/Essence)", "Lancer avant de quitter le jeu", 120, true, null),
                AndroidRoutineItem("g2", "Marche Récolte #2 / Mine d'Alliance", "Garder les 2 slots occupés", 240, true, null),
                AndroidRoutineItem("tr", "Entraînement Casernes", "Collecter et relancer selon le stock", 180, true, null),
                AndroidRoutineItem("aj", "Aide Alliance, Dons & Auto-Join Ralliement", "Renouveler avant expiration", 480, true, null),
                AndroidRoutineItem("vip", "VIP, Courrier, Arrestation & Distributrice", "Collecte quotidienne", 1440, true, null),
                AndroidRoutineItem("sh", "Alerte Bouclier (8h)", "Rappel avant fin de protection", 480, true, null)
            )
        )
    }
    var customRoutineMinutes by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
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
                                        endsAtMs = base + it.minutes * 60_000L,
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
                            val customText = customRoutineMinutes[task.id] ?: task.minutes.toString()
                            val parsedCustomMinutes = customText.toIntOrNull()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = customText,
                                    onValueChange = { value ->
                                        customRoutineMinutes = customRoutineMinutes + (task.id to value.filter { it.isDigit() }.take(5))
                                    },
                                    modifier = Modifier.weight(1f),
                                    label = { Text("Durée personnalisée (minutes)") },
                                    singleLine = true
                                )
                                Button(
                                    enabled = parsedCustomMinutes != null && parsedCustomMinutes in 1..10080,
                                    onClick = {
                                        val minutes = parsedCustomMinutes ?: return@Button
                                        val base = System.currentTimeMillis()
                                        routineItems = routineItems.map {
                                            if (it.id == task.id) {
                                                it.copy(
                                                    minutes = minutes,
                                                    enabled = true,
                                                    endsAtMs = base + minutes * 60_000L,
                                                    pausedRemainingMs = null
                                                )
                                            } else it
                                        }
                                    }
                                ) {
                                    Text("Lancer")
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
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
                                    else -> "${task.minutes / 60}h (${task.minutes} min)"
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
                                Button(onClick = {
                                    val base = System.currentTimeMillis()
                                    routineItems = routineItems.map {
                                        if (it.id == task.id) {
                                            it.copy(
                                                enabled = true,
                                                endsAtMs = base + it.minutes * 60_000L,
                                                pausedRemainingMs = null
                                            )
                                        } else {
                                            it
                                        }
                                    }
                                }) {
                                    Text("Lancer ${task.minutes / 60}h")
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
    eventPosition: String,
    onEventPositionChange: (String) -> Unit,
    selectedGorilla: String,
    onGorillaChange: (String) -> Unit,
    gorillaRuns: List<GorillaDamageRecord>,
    onAddGorillaRun: (GorillaDamageRecord) -> Unit,
    onClearGorillaRuns: () -> Unit,
    advice: TacticalAdviceResult?,
    isLoading: Boolean,
    onGenerateAdvice: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    var gorillaDamageInput by remember { mutableStateOf("") }
    var gorillaShieldInput by remember { mutableStateOf("0") }
    var gorillaBomberInput by remember { mutableStateOf("15") }
    var gorillaShooterInput by remember { mutableStateOf("85") }
    var gorillaHeroesInput by remember { mutableStateOf("") }
    var gorillaRobotInput by remember { mutableStateOf("") }
    var gorillaNotesInput by remember { mutableStateOf("") }
    var gorillaBonusInput by remember { mutableStateOf("") }
    val gorillaRatioTotal = (gorillaShieldInput.toIntOrNull() ?: 0) +
        (gorillaBomberInput.toIntOrNull() ?: 0) + (gorillaShooterInput.toIntOrNull() ?: 0)
    val numberFormat = remember { java.text.NumberFormat.getIntegerInstance(java.util.Locale.FRANCE) }
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
            if (selectedMode == AdviceMode.GORILLA_LEGION) {
                Text("Gorille affiché dans le jeu", style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8))
                val gorillaOptions = listOf(
                    "Space Gorilla", "Wise Gorilla", "Cyber Gorilla", "Primal Gorilla",
                    "Armed Gorilla", "Treasure Gorilla", "Gorilla Warlord / High Chieftain"
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(selected = selectedGorilla.isBlank(), onClick = { onGorillaChange("") }, label = { Text("À confirmer") })
                        gorillaOptions.take(3).forEach { gorilla ->
                            FilterChip(selected = selectedGorilla == gorilla, onClick = { onGorillaChange(gorilla) }, label = { Text(gorilla) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        gorillaOptions.drop(3).forEach { gorilla ->
                            FilterChip(selected = selectedGorilla == gorilla, onClick = { onGorillaChange(gorilla) }, label = { Text(gorilla) })
                        }
                    }
                }
                Text("Choisis le nom exact visible : ne te fie pas uniquement au jour de la semaine.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161E2E))
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Journal des dégâts Gorilla", fontWeight = FontWeight.SemiBold, color = Color(0xFFF59E0B))
                        Text("Enregistre tes essais pour comparer les compositions sur ce même gorille.", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(value = gorillaDamageInput, onValueChange = { gorillaDamageInput = it.filter(Char::isDigit) }, label = { Text("Dégâts infligés") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Text("Ratio Boucliers / Bombardiers / Tireurs (%)", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = gorillaShieldInput, onValueChange = { gorillaShieldInput = it.filter(Char::isDigit).take(3) }, label = { Text("Boucliers") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = gorillaBomberInput, onValueChange = { gorillaBomberInput = it.filter(Char::isDigit).take(3) }, label = { Text("Bombardiers") }, modifier = Modifier.weight(1f), singleLine = true)
                            OutlinedTextField(value = gorillaShooterInput, onValueChange = { gorillaShooterInput = it.filter(Char::isDigit).take(3) }, label = { Text("Tireurs") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                        Text("Total : $gorillaRatioTotal %", color = if (gorillaRatioTotal == 100) Color(0xFF34D399) else Color(0xFFFBBF24), style = MaterialTheme.typography.labelSmall)
                        OutlinedTextField(value = gorillaHeroesInput, onValueChange = { gorillaHeroesInput = it }, label = { Text("Héros utilisés") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = gorillaRobotInput, onValueChange = { gorillaRobotInput = it }, label = { Text("Robot utilisé") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = gorillaBonusInput, onValueChange = { gorillaBonusInput = it }, label = { Text("Bonus actif (identique pour comparer)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = gorillaNotesInput, onValueChange = { gorillaNotesInput = it }, label = { Text("Équipement / remarques") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
                        Button(
                            onClick = {
                                val parsedDamage = gorillaDamageInput.toLongOrNull()
                                val shield = gorillaShieldInput.toIntOrNull()
                                val bomber = gorillaBomberInput.toIntOrNull()
                                val shooter = gorillaShooterInput.toIntOrNull()
                                if (selectedGorilla.isNotBlank() && parsedDamage != null && parsedDamage > 0 &&
                                    shield != null && bomber != null && shooter != null &&
                                    listOf(shield, bomber, shooter).all { it in 0..100 } && gorillaRatioTotal == 100
                                ) {
                                    onAddGorillaRun(GorillaDamageRecord(
                                        id = UUID.randomUUID().toString(),
                                        date = java.time.LocalDate.now().toString(),
                                        gorilla = selectedGorilla,
                                        damage = parsedDamage,
                                        shield = shield,
                                        bomber = bomber,
                                        shooter = shooter,
                                        heroes = gorillaHeroesInput.trim(),
                                        robot = gorillaRobotInput.trim(),
                                        notes = gorillaNotesInput.trim(),
                                        bonus = gorillaBonusInput.trim()
                                    ))
                                    gorillaDamageInput = ""
                                    gorillaNotesInput = ""
                                }
                            },
                            enabled = selectedGorilla.isNotBlank() && gorillaDamageInput.toLongOrNull()?.let { it > 0 } == true && gorillaRatioTotal == 100
                        ) { Text("Enregistrer l’essai") }
                        val bestRun = gorillaRuns.filter { it.gorilla == selectedGorilla }.maxByOrNull { it.damage }
                        Text("Meilleur résultat — $selectedGorilla", fontWeight = FontWeight.Medium)
                        if (bestRun == null) {
                            Text("Aucun essai enregistré pour ce gorille.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        } else {
                            Text("${numberFormat.format(bestRun.damage)} dégâts · ${bestRun.shield}/${bestRun.bomber}/${bestRun.shooter} · ${bestRun.date}", color = Color(0xFFFBBF24), style = MaterialTheme.typography.bodySmall)
                        }
                        val comparableRuns = gorillaRuns
                            .filter { it.bonus.isNotBlank() }
                            .groupBy { Triple(it.gorilla, it.bonus.trim().lowercase(), "${it.shield}/${it.bomber}/${it.shooter}") }
                            .filterValues { it.size >= 2 }
                        Text("Comparaison des ratios (même gorille et même bonus)", fontWeight = FontWeight.Medium)
                        Text("Moyenne affichée après au moins deux essais avec le même bonus déclaré et le même ratio.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        if (comparableRuns.isEmpty()) {
                            Text("Ajoute deux essais comparables en renseignant le même bonus pour voir les moyennes.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                        } else {
                            comparableRuns.forEach { (key, group) ->
                                val average = group.sumOf { it.damage } / group.size
                                val best = group.maxOf { it.damage }
                                Text("${key.first} · ${group.first().bonus} · Ratio ${key.third}", style = MaterialTheme.typography.bodySmall)
                                Text("Moyenne : ${numberFormat.format(average)} · Meilleur : ${numberFormat.format(best)} · ${group.size} essais", color = Color(0xFFFBBF24), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Text("Historique : ${gorillaRuns.size} essai(s)", fontWeight = FontWeight.Medium)
                        gorillaRuns.take(5).forEach { run ->
                            Text("${run.gorilla} · ${numberFormat.format(run.damage)} · ${run.shield}/${run.bomber}/${run.shooter}", style = MaterialTheme.typography.bodySmall)
                        }
                        if (gorillaRuns.isNotEmpty()) TextButton(onClick = onClearGorillaRuns) { Text("Effacer le journal") }
                    }
                }
            }
            if (selectedMode != AdviceMode.GORILLA_LEGION) {
                Text("Position dans l'événement", style = MaterialTheme.typography.labelMedium, color = Color(0xFF94A3B8))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = eventPosition == "leader", onClick = { onEventPositionChange("leader") }, label = { Text("Leader") })
                    FilterChip(selected = eventPosition == "joiner", onClick = { onEventPositionChange("joiner") }, label = { Text("Participant") })
                    FilterChip(selected = eventPosition == "solo", onClick = { onEventPositionChange("solo") }, label = { Text("Solo") })
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
                                    color = Color(0xFFFBBF24),
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
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF422006)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("Informations incertaines", fontWeight = FontWeight.SemiBold, color = Color(0xFFFCD34D))
                            res.informationsIncertaines.forEach { info ->
                                Text("• $info", color = Color(0xFFFEF3C7))
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
private fun HistoryScreen(analyses: List<ScreenshotAnalysis>) {
    if (analyses.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Historique vide. Appuyez sur « Analyser l’écran » pour enregistrer votre première capture.")
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
