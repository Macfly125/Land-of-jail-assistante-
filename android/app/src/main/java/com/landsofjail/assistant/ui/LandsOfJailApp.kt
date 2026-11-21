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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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

    // Actualise l'état des permissions quand l'utilisateur revient des Paramètres Android
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

    // Déclenché UNIQUEMENT après autorisation visible MediaProjection et appui sur « Analyser l’écran »
    LaunchedEffect(capturedScreenPayload) {
        if (capturedScreenPayload != null) {
            isAnalyzing = true
            errorMessage = null
            try {
                // 1. OCR local ML Kit immédiat sur l'appareil
                val ocrRaw = LandsOfJailOcrParser.recognizeBitmap(capturedScreenPayload.bitmap)
                val localExtraction = LandsOfJailOcrParser.parseLocalOcrText(ocrRaw)

                // 2. Analyse structurée complète avec fallback local OCR si hors-ligne
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

                // Affiche le tableau de bord et charge les conseils Trial Cage / Crystal Clash
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
                        isMonitoringRunning = isMonitoringRunning,
                        isGameForeground = isGameForeground,
                        packageFilter = packageFilter,
                        onPackageFilterChange = { packageFilter = it },
                        onOpenUsageSettings = { UsageStatsDetector.openUsageAccessSettings(context) },
                        onOpenOverlaySettings = { UsageStatsDetector.openOverlaySettings(context) },
                        onStartMonitoring = {
                            GameForegroundMonitorService.start(context, packageFilter)
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
                        "Détection de lancement (UsageStatsManager)",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Détecte uniquement le passage au premier plan de Lands of Jail pour afficher un bouton « Analyser l’écran ». Aucune action dans le jeu et aucune capture sans votre autorisation système visible.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        if (hasUsagePermission) "Statistiques d'utilisation : Autorisées"
                        else "Statistiques d'utilisation : Non autorisées",
                        color = if (hasUsagePermission) Color(0xFF34D399) else Color(0xFFFBBF24),
                        fontWeight = FontWeight.Medium
                    )

                    if (!hasUsagePermission) {
                        Button(onClick = onOpenUsageSettings) {
                            Text("Autoriser dans les paramètres Android")
                        }
                    }

                    OutlinedTextField(
                        value = packageFilter,
                        onValueChange = onPackageFilterChange,
                        label = { Text("Package ou mot-clé du jeu") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onStartMonitoring,
                            enabled = hasUsagePermission && !isMonitoringRunning
                        ) {
                            Text(if (isMonitoringRunning) "Détection active" else "Démarrer la détection")
                        }
                        if (isMonitoringRunning) {
                            OutlinedButton(onClick = onStopMonitoring) {
                                Text("Arrêter la détection")
                            }
                        }
                    }

                    if (!hasOverlayPermission) {
                        TextButton(onClick = onOpenOverlaySettings) {
                            Text("Optionnel : autoriser le bouton flottant au-dessus du jeu")
                        }
                    }

                    if (isGameForeground) {
                        Text(
                            "Lands of Jail détecté au premier plan · Appuyez pour capturer avec autorisation.",
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
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(
                "Puissance actuelle : ${power?.puissanceTotaleTexte ?: "Non capturée"}",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
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
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Synthèse (${res.generatedAtIso})", fontWeight = FontWeight.SemiBold)
                        Text(res.synthese)
                        Text("Composition : ${res.compositionRecommandee}")
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
                    Text("${src.title} · Date : ${src.detectedDate ?: "Consulté le ${res.generatedAtIso.take(10)}"}")
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
