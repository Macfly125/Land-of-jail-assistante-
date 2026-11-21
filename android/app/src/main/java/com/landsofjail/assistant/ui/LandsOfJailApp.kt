package com.landsofjail.assistant.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import java.time.Instant
import java.util.UUID

private enum class AssistantTab(val titleFr: String) {
    TABLEAU("Tableau"),
    HEROS("Héros"),
    RESSOURCES("Ressources"),
    CONSEILS("Conseils"),
    HISTORIQUE("Historique")
}

private enum class ResourceSubTab(val titleFr: String) {
    INVENTAIRE("Inventaire"),
    BATIMENTS("Calcul Bâtiments"),
    HEROS_OBJETS("Calcul Héros")
}

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
    var resources by remember { mutableStateOf<List<ResourceRecord>>(emptyList()) }
    var currentPower by remember { mutableStateOf<PowerMetric?>(null) }

    var isAnalyzing by remember { mutableStateOf(false) }
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

    fun mergeNewAnalysis(result: ScreenshotAnalysis) {
        analyses = listOf(result) + analyses
        result.puissance?.let { currentPower = it }
        if (result.heros.isNotEmpty()) {
            val merged = (heroes + result.heros).associateBy { it.nom.lowercase() }
            heroes = merged.values.toList()
        }
        if (result.ressources.isNotEmpty()) {
            val merged = (resources + result.ressources).associateBy { it.nom.lowercase() }
            resources = merged.values.toList()
        }
    }

    fun launchSharedUriAnalysis(uri: Uri) {
        scope.launch {
            isAnalyzing = true
            errorMessage = null
            repository.analyzeSharedScreenshot(context, uri, playerRole, null)
                .onSuccess { mergeNewAnalysis(it) }
                .onFailure { errorMessage = it.message ?: "Échec de l’analyse." }
            isAnalyzing = false
        }
    }

    LaunchedEffect(capturedScreenPayload) {
        if (capturedScreenPayload != null) {
            isAnalyzing = true
            errorMessage = null
            try {
                val ocrRaw = LandsOfJailOcrParser.recognizeBitmap(capturedScreenPayload.bitmap)
                val localExtraction = LandsOfJailOcrParser.parseLocalOcrText(ocrRaw)

                val result = runCatching {
                    repository.analyzeBase64Screenshot(
                        imageBase64 = capturedScreenPayload.base64Jpeg,
                        mimeType = "image/jpeg",
                        imageLabel = "capture-ecran-autorisee.jpg",
                        playerRole = playerRole,
                        note = "OCR local : ${ocrRaw.take(400)}"
                    )
                }.getOrElse {
                    ScreenshotAnalysis(
                        id = UUID.randomUUID().toString(),
                        analyzedAtIso = Instant.now().toString(),
                        imageUriString = "",
                        typeCapture = "ocr_local",
                        resumeCapture = "Extraction OCR locale effectuée (vérification recommandée).",
                        puissance = localExtraction.power,
                        ressources = localExtraction.resources,
                        heros = localExtraction.heroes,
                        champsIncertains = localExtraction.uncertainItems
                    )
                }

                mergeNewAnalysis(result)
                if (result.typeCapture == "crystal_clash") {
                    selectedAdviceMode = AdviceMode.CRYSTAL_CLASH
                } else if (result.typeCapture == "trial_cage") {
                    selectedAdviceMode = AdviceMode.TRIAL_CAGE
                }

                currentTab = AssistantTab.TABLEAU
                isFetchingAdvice = true
                repository.fetchTacticalAdvice(
                    mode = selectedAdviceMode,
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
            }
        }
    }

    LaunchedEffect(sharedScreenshotUri) {
        if (sharedScreenshotUri != null) {
            launchSharedUriAnalysis(sharedScreenshotUri)
            onConsumeSharedUri()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) launchSharedUriAnalysis(uri)
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
            topBar = {
                TopAppBar(
                    title = { Text("Lands of Jail Assistant", fontWeight = FontWeight.SemiBold) },
                    actions = {
                        TextButton(onClick = {
                            context.startActivity(
                                Intent(context, ScreenCaptureConsentActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            )
                        }) {
                            Text("Analyser l’écran")
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
                            icon = { Icon(icon, contentDescription = tab.titleFr) },
                            label = { Text(tab.titleFr) }
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
                if (isAnalyzing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("Analyse OCR de l’écran autorisé en cours…")
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
                        onPickImage = { imagePicker.launch("image/*") }
                    )
                    AssistantTab.HEROS -> HeroesScreen(heroes)
                    AssistantTab.RESSOURCES -> ResourcesScreen(currentPower, resources)
                    AssistantTab.CONSEILS -> AdviceScreen(
                        selectedMode = selectedAdviceMode,
                        onSelectMode = { selectedAdviceMode = it },
                        playerRole = playerRole,
                        advice = adviceResult,
                        isLoading = isFetchingAdvice,
                        onGenerateAdvice = {
                            scope.launch {
                                isFetchingAdvice = true
                                errorMessage = null
                                repository.fetchTacticalAdvice(
                                    mode = selectedAdviceMode,
                                    playerRole = playerRole,
                                    heroes = heroes,
                                    powerText = currentPower?.puissanceTotaleTexte
                                ).onSuccess { adviceResult = it }
                                    .onFailure { errorMessage = it.message }
                                isFetchingAdvice = false
                            }
                        }
                    )
                    AssistantTab.HISTORIQUE -> HistoryScreen(analyses)
                }
            }
        }
    }
}

@Composable
private fun DashboardScreen(
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
    onPickImage: () -> Unit
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Spacer(Modifier.height(8.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Bouton flottant au-dessus du jeu & Détection",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "1) Autorisez les statistiques d’utilisation. 2) Autorisez l’affichage par-dessus les autres applis pour que le bouton déplaçable « Analyser l’écran » reste visible dans Lands of Jail.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        if (hasUsagePermission) "1. Statistiques d'utilisation : Autorisées"
                        else "1. Statistiques d'utilisation : À autoriser",
                        color = if (hasUsagePermission) Color(0xFF34D399) else Color(0xFFFBBF24),
                        fontWeight = FontWeight.Medium
                    )
                    if (!hasUsagePermission) {
                        Button(onClick = onOpenUsageSettings) {
                            Text("Autoriser Statistiques d'utilisation")
                        }
                    }

                    Text(
                        if (hasOverlayPermission) "2. Superposition au-dessus du jeu : Autorisée"
                        else "2. Superposition au-dessus du jeu : À autoriser (obligatoire pour le bouton permanent)",
                        color = if (hasOverlayPermission) Color(0xFF34D399) else Color(0xFFFBBF24),
                        fontWeight = FontWeight.Medium
                    )
                    if (!hasOverlayPermission) {
                        Button(onClick = onOpenOverlaySettings) {
                            Text("Autoriser affichage par-dessus les applis")
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Garder le bouton flottant toujours visible",
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
                        label = { Text("Package du jeu (défaut : com.justgame.jails)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onStartMonitoring,
                            enabled = (hasUsagePermission || hasOverlayPermission) && !isMonitoringRunning
                        ) {
                            Text(if (isMonitoringRunning) "Détection active" else "Démarrer & afficher le bouton")
                        }
                        if (isMonitoringRunning) {
                            OutlinedButton(onClick = onStopMonitoring) {
                                Text("Arrêter la détection")
                            }
                        }
                    }

                    if (isGameForeground) {
                        Text(
                            "Lands of Jail détecté au premier plan · Appuyez sur le bouton flottant pour capturer.",
                            color = Color(0xFF34D399),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = onAnalyzeScreenNow) {
                            Text("Analyser l’écran")
                        }
                        TextButton(onClick = onPickImage) {
                            Text("Importer image")
                        }
                    }
                }
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Puissance : ${currentPower?.puissanceTotaleTexte ?: "Non mesurée"}")
                    if (currentPower?.incertain == true) {
                        Text(
                            "Incertain : ${currentPower.raisonIncertitude ?: "lecture partielle"}",
                            color = Color(0xFFFBBF24)
                        )
                    }
                    Text("Héros suivis : $heroesCount · Ressources : $resourcesCount · Analyses : $analysesCount")
                }
            }
        }
        item {
            Text("Rôle du joueur", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            PlayerRole.entries.forEach { role ->
                Row(verticalAlignment = Alignment.CenterVertically) {
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
private fun ResourcesScreen(power: PowerMetric?, resources: List<ResourceRecord>) {
    var subTab by remember { mutableStateOf(ResourceSubTab.INVENTAIRE) }
    var targetBuildingLvl by remember { mutableIntStateOf(16) }
    var starFrom by remember { mutableIntStateOf(0) }
    var starTo by remember { mutableIntStateOf(4) }
    var ownedShardsText by remember { mutableStateOf("0") }
    var tomeFrom by remember { mutableIntStateOf(0) }
    var tomeTo by remember { mutableIntStateOf(5) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ResourceSubTab.entries.forEach { t ->
                    FilterChip(
                        selected = subTab == t,
                        onClick = { subTab = t },
                        label = { Text(t.titleFr) }
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                AdviceMode.entries.forEach { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = { onSelectMode(mode) },
                        label = { Text(mode.labelFr) }
                    )
                }
            }
            Text("Rôle actif : ${playerRole.labelFr}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Button(onClick = onGenerateAdvice, enabled = !isLoading) {
                Text(if (isLoading) "Recherche en cours…" else "Obtenir les conseils Internet")
            }
        }
        advice?.let { res ->
            val adviceDate = res.generatedAt.ifBlank { res.generatedAtIso }
            res.geminiWarning?.let { warn ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF451A1A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(warn, modifier = Modifier.padding(12.dp), color = Color(0xFFFCA5A5))
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Synthèse ($adviceDate)", fontWeight = FontWeight.SemiBold)
                        Text(res.synthese)
                        Text("Composition : ${res.compositionRecommandee}")
                    }
                }
            }
            items(res.recommandations) { rec ->
                Card(modifier = Modifier.fillMaxWidth()) {
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
                Text("Sources Internet & Dates", fontWeight = FontWeight.SemiBold)
            }
            items(res.sources) { src ->
                TextButton(onClick = { uriHandler.openUri(src.url) }) {
                    Text("${src.title} · Date : ${src.detectedDate ?: "Consulté le ${adviceDate.take(10)}"}")
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(analyses: List<ScreenshotAnalysis>) {
    if (analyses.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Historique vide.")
        }
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(analyses) { item ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AsyncImage(
                        model = item.imageUriString,
                        contentDescription = item.resumeCapture,
                        modifier = Modifier.size(64.dp)
                    )
                    Column {
                        Text(item.typeCapture, fontWeight = FontWeight.SemiBold)
                        Text(item.analyzedAtIso, style = MaterialTheme.typography.bodySmall)
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
