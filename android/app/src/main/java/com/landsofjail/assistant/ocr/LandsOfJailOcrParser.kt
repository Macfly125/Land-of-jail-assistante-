package com.landsofjail.assistant.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.landsofjail.assistant.model.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrTextBox(val text: String, val left: Int, val top: Int, val right: Int, val bottom: Int)

data class OcrRecognition(
    val text: String,
    val boxes: List<OcrTextBox>,
    val imageWidth: Int,
    val imageHeight: Int
)

data class LocalOcrExtraction(
    val rawText: String,
    val detectedCategory: String,
    val ocrSnippet: String,
    val power: PowerMetric?,
    val resources: List<ResourceRecord>,
    val heroes: List<HeroRecord>,
    val robots: List<RobotRecord> = emptyList(),
    val uncertainItems: List<UncertainItem>,
    val equipment: List<EquipmentRecord> = emptyList()
)

object LandsOfJailOcrParser {

    // Le scanner local doit suivre le catalogue complet : une liste codée en dur
    // de quelques héros faisait classer les autres captures comme « autre ».
    private val KNOWN_HEROES = HeroCatalog.entries
        .map { it.nom }
        .distinctBy { it.lowercase() }

    // Variantes de traduction et erreurs OCR fréquentes observées sur les captures FR/EN.
    private val HERO_ALIASES = mapOf(
        "Beary" to listOf("Berry"),
        "Durga" to listOf("Duraga"),
        "Iwakado" to listOf("Iwado", "Iwakad0"),
        "Caesar" to listOf("César", "Ces ar"),
        "Phoenix" to listOf("Phénix", "Phenix"),
        "Whisper" to listOf("Chuchotement"),
        "Veronica" to listOf("Véronique", "Veronique"),
        "Minyeon & Taehun" to listOf("Minyeon et Taehun", "Flameborne"),
        "Ryuichi Ito" to listOf("Ryuichi"),
        "Fran & Pike" to listOf("Fran et Pike"),
        "Mai Shiranui" to listOf("Mai"),
        "Omega Rugal" to listOf("Rugal"),
        "Terry Bogard" to listOf("Terry")
    )

    /**
     * Run all bundled script recognizers against one still image. The game UI can
     * use Latin, Chinese, Japanese or Korean independently of the app language.
     */
    suspend fun recognizeBitmapDetailed(bitmap: Bitmap): OcrRecognition {
        val boxes = mutableListOf<OcrTextBox>()
        val recognizerFactories: List<() -> TextRecognizer> = listOf(
            { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) },
            { TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build()) },
            { TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build()) },
            { TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build()) }
        )
        for (createRecognizer in recognizerFactories) {
            // A script-specific model failure must not discard successful Latin/CJK output.
            val recognized = runCatching { recognizeWith(createRecognizer(), bitmap) }.getOrDefault(emptyList())
            boxes += recognized
        }

        // Second pass for small/low-contrast UI text. The original image remains
        // authoritative; enhanced coordinates are mapped back before merging.
        val enhancedBitmap = runCatching {
            OcrImagePreprocessor.enhanceForOcr(bitmap)
        }.getOrNull()
        if (enhancedBitmap != null) {
            try {
                val enhancedBoxes = runCatching {
                    recognizeWith(
                        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
                        enhancedBitmap
                    )
                }.getOrDefault(emptyList())
                boxes += OcrImagePreprocessor.mapBoxesToOriginal(enhancedBoxes)
            } finally {
                if (!enhancedBitmap.isRecycled) enhancedBitmap.recycle()
            }
        }
        val unique = boxes
            .distinctBy { box -> box.text.trim().lowercase() to (box.left / 8 to box.top / 8) }
            .sortedWith(compareBy<OcrTextBox> { it.top }.thenBy { it.left })
        return OcrRecognition(
            text = unique.joinToString("\n") { it.text },
            boxes = unique,
            imageWidth = bitmap.width,
            imageHeight = bitmap.height
        )
    }

    suspend fun recognizeBitmap(bitmap: Bitmap): String =
        recognizeBitmapDetailed(bitmap).text

    private suspend fun recognizeWith(
        recognizer: TextRecognizer,
        bitmap: Bitmap
    ): List<OcrTextBox> = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromBitmap(bitmap, 0)
        cont.invokeOnCancellation { recognizer.close() }
        recognizer.process(image)
            .addOnSuccessListener { result ->
                recognizer.close()
                val boxes = result.textBlocks.flatMap { block ->
                    block.lines.mapNotNull { line ->
                        line.boundingBox?.let { rect ->
                            OcrTextBox(line.text, rect.left, rect.top, rect.right, rect.bottom)
                        }
                    }
                }
                if (cont.isActive) cont.resume(boxes)
            }
            .addOnFailureListener { error ->
                recognizer.close()
                if (cont.isActive) cont.resumeWithException(error)
            }
    }

    /**
     * Sélectionne uniquement la rangée horizontale la plus cohérente de nombres
     * située dans la zone supérieure. Cela évite de confondre les ressources
     * avec des niveaux, compteurs ou nombres provenant d'autres panneaux.
     */
    /**
     * A row with several upper-bar number boxes is likely to contain resource
     * counters. Even when the six-slot layout is incomplete or ambiguous, those
     * numbers must not be reinterpreted as the player's power.
     */
    private fun hasPotentialTopResourceNumberRow(
        boxes: List<OcrTextBox>,
        imageWidth: Int,
        imageHeight: Int
    ): Boolean {
        if (imageWidth <= 0 || imageHeight <= 0) return false
        val candidates = boxes
            .filter { it.top >= 0 && it.top <= (imageHeight * 0.20).toInt() }
            .filter { it.left >= 0 && it.right <= imageWidth }
            .filter { Regex("""^[0-9][0-9.,\s]*[kmbKMB]?$""").matches(it.text.trim()) }
        if (candidates.size < 4) return false
        val tolerance = maxOf(18, (imageHeight * 0.025).toInt())
        return candidates.any { anchor ->
            val anchorCenter = (anchor.top + anchor.bottom) / 2
            candidates.count { candidate ->
                kotlin.math.abs(((candidate.top + candidate.bottom) / 2) - anchorCenter) <= tolerance
            } >= 4
        }
    }

    /**
     * Player profile cards show total power beside a fist icon, above Eliminations.
     * This is a separate layout from the home-screen power badge and from the top
     * resource counters. Use the elimination row (or profile metadata below it)
     * as a positional anchor so the elimination amount itself is never selected.
     */
    private fun profilePowerBadgeFromCoordinates(
        boxes: List<OcrTextBox>,
        imageWidth: Int,
        imageHeight: Int
    ): String? {
        if (boxes.isEmpty() || imageWidth <= 0 || imageHeight <= 0) return null
        val compactNumber = Regex("""\b[0-9]+(?:[.,][0-9]+)?\s*[MB]\b""", RegexOption.IGNORE_CASE)
        // Require profile-specific anchors. A lone "Eliminations" label can occur
        // in other screens and must never turn a nearby quantity into player power.
        val hasPlayerId = boxes.any {
            Regex("""\bID\s*[:#]?\s*[0-9]{5,}""", RegexOption.IGNORE_CASE).containsMatchIn(it.text)
        }
        val hasProfileMetadata = boxes.any {
            Regex("""\b(?:alliance|serveur|server)\b""", RegexOption.IGNORE_CASE).containsMatchIn(it.text)
        }
        if (!hasPlayerId || !hasProfileMetadata) return null

        val eliminationBox = boxes.firstOrNull {
            Regex("""[ée]liminations?""", RegexOption.IGNORE_CASE).containsMatchIn(it.text)
        } ?: return null
        val anchorY = (eliminationBox.top + eliminationBox.bottom) / 2f
        val minY = anchorY - imageHeight * 0.10f
        return boxes.asSequence()
            .filter { box ->
                val centerY = (box.top + box.bottom) / 2f
                centerY in minY..anchorY &&
                    box.left >= imageWidth * 0.30f &&
                    box.left <= imageWidth * 0.90f &&
                    box.right <= imageWidth * 0.95f
            }
            .mapNotNull { box ->
                compactNumber.find(box.text)?.value?.trim()
            }
            .firstOrNull()
    }

    private fun topResourceNumberBoxes(
        boxes: List<OcrTextBox>,
        imageWidth: Int,
        imageHeight: Int
    ): List<OcrTextBox> {
        if (imageWidth <= 0 || imageHeight <= 0) return emptyList()
        val candidates = boxes
            .filter { it.top >= 0 && it.top <= (imageHeight * 0.20).toInt() }
            .filter { it.left >= 0 && it.right <= imageWidth }
            .filter { Regex("""^[0-9][0-9.,\s]*[kmbKMB]?$""").matches(it.text.trim()) }
        if (candidates.size < 4) return emptyList()

        val tolerance = maxOf(18, (imageHeight * 0.025).toInt())
        val bestRow = candidates
            .map { anchor ->
                candidates.filter { candidate ->
                    kotlin.math.abs(
                        ((candidate.top + candidate.bottom) / 2) -
                            ((anchor.top + anchor.bottom) / 2)
                    ) <= tolerance
                }
            }
            .maxByOrNull { it.size }
            .orEmpty()
        val alignedRow = bestRow.distinctBy { it.left to it.text }.sortedBy { it.left }
        // La barre attendue contient six emplacements. S'il en manque ou s'il y en a
        // davantage, l'OCR a probablement mélangé un compteur voisin : ne pas décaler
        // les associations ni enregistrer de faux stocks.
        return alignedRow.takeIf { it.size == 6 }.orEmpty()
    }

    /**
     * Analyse OCR locale complète (puissance, QG/Prison, héros, ressources, type d'écran)
     * permettant un affichage immédiat dans l'appli même hors-ligne ou sans backend.
     */
    fun parseLocalOcrText(
        rawText: String,
        textBoxes: List<OcrTextBox> = emptyList(),
        imageWidth: Int = 0,
        imageHeight: Int = 0,
        visualRobotMatches: List<RobotPortraitMatch> = emptyList()
    ): LocalOcrExtraction {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val lowerFull = rawText.lowercase()
        val uncertain = mutableListOf<UncertainItem>()
        val resources = mutableListOf<ResourceRecord>()
        val heroes = mutableListOf<HeroRecord>()
        val robots = mutableListOf<RobotRecord>()
        val equipment = mutableListOf<EquipmentRecord>()
        val seenEquipmentKeys = mutableSetOf<String>()
        val seenRobotKeys = mutableSetOf<String>()
        val knownRobotNames = (RobotCatalog.entries.map { it.name } + listOf("Chat Fantôme", "Hercules Alpha", "Valiant", "Scorpio")).distinct()
        // Un panneau de bâtiment est une frontière : ses niveaux ne décrivent jamais le robot précédent.
        val buildingPanelLabelRegex = Regex(
            """(?:^|\b)(?:forticlad\s+lab|laboratory|laboratoire|laboratorio|kitchen|cuisine|cucina|küche|mutfak|кухня|厨房|warehouse|entrep[oô]t|lagerhaus|depo|склад|仓库|barracks|caserne|kaserne|kışla|казармы|兵营|medical\s+station|infirmary|infirmerie|medizinische\s+station|tıbbi\s+istasyon|медпункт|医疗站|command\s+center|centre\s+de\s+commandement|kommandozentrum|komuta\s+merkezi|командный\s+центр|指挥中心|gate|porte|tor|kapı|ворота|大门|warden(?:['’]s)?\s+office|director(?:['’]s)?\s+office|bureau\s+du\s+directeur|robot\s+center|yard|cour|hof|avlu|двор|院子|cell|cellule|zelle|hücre|камера|牢房|comms|communication\s+center|kommunikationszentrum|iletişim\s+merkezi|центр\s+связи|通讯中心)(?:$|\b)""",
            RegexOption.IGNORE_CASE
        )
        val seenHeroKeys = mutableSetOf<String>()
        val seenResKeys = mutableSetOf<String>()
        var power: PowerMetric? = null
        var wardenLevel: String? = null

        val powerRegex = Regex("""(?:^|[^\p{L}\p{N}])(?:puissance|power|macht|güç|мощь|comb\.?\s*power|cp|战力)(?=\s|[:：-]|[0-9])\s*[:：-]?\s*([0-9][0-9\s.,kmbKMB]*)""", RegexOption.IGNORE_CASE)
        val levelRegex = Regex("""(?:niv\.?|lv\.?|level)\s*([0-9]+)""", RegexOption.IGNORE_CASE)
        // Le niveau du Bureau du directeur peut être affiché avant ou après son nom
        // selon la langue et la disposition de l'écran. Seul un couple libellé + niveau
        // explicite est accepté : un niveau de cuisine/caserne ne devient jamais celui du QG.
        val wardenBuildingNames =
            """(?:\bwarden(?:['’]s)?\s*office\b|\bdirector(?:['’]s)?\s*office\b|\bbureau\s*du\s*directeur\b|\bbüro\s+des\s+direktors\b|\bmüdür(?:ün)?\s+ofisi\b|\bофис\s+начальника\b|典狱长办公室|\bufficio\s+del\s+direttore\b|\bprison\b|\bqg\b|\bhq\b)"""
        val buildingLevelLabels =
            """(?:niv(?:eau)?|lv|lvl|level|livello|stufe|seviye|уровень|等级)"""
        val wardenRegex = Regex(
            """(?:(?:$wardenBuildingNames)\s*$buildingLevelLabels\.?\s*[:：#-]?\s*([0-9]{1,2})|\b([0-9]{1,2})\s+(?:$wardenBuildingNames)\b|\b$buildingLevelLabels\.?\s*[:：#-]?\s*([0-9]{1,2})\s+(?:$wardenBuildingNames)\b|\b(?:hq|qg)\s+([0-9]{1,2})\b)""",
            RegexOption.IGNORE_CASE
        )
        // Certains écrans affichent le nom du bâtiment et son niveau sur deux lignes.
        // N'accepter que le niveau explicitement étiqueté sur la ligne immédiatement suivante.
        val explicitBuildingLevelRegex = Regex(
            """$buildingLevelLabels\.?\s*[:：#-]?\s*([0-9]{1,2})""",
            RegexOption.IGNORE_CASE
        )
        val wardenBuildingLabelRegex = Regex(
            wardenBuildingNames,
            RegexOption.IGNORE_CASE
        )

        val resourcePatterns = listOf(
            "Nourriture (Food)" to Regex("""(?:nourriture|food|vivres|nahrung|yiyecek|еда|食物|cibo)(?=$|\s|[:：\-]|[0-9])\s*[:：\-]?\s*([0-9][0-9.,\s]*[kmbKMB]?)?""", RegexOption.IGNORE_CASE),
            "Bois (Wood)" to Regex("""(?:bois|wood|lumber|holz|odun|дерево|木材|legno)(?=$|\s|[:：\-]|[0-9])\s*[:：\-]?\s*([0-9][0-9.,\s]*[kmbKMB]?)?""", RegexOption.IGNORE_CASE),
            "Acier (Steel)" to Regex("""(?:acier|steel|fer|iron|stahl|çelik|сталь|钢材|acciaio)(?=$|\s|[:：\-]|[0-9])\s*[:：\-]?\s*([0-9][0-9.,\s]*[kmbKMB]?)?""", RegexOption.IGNORE_CASE),
            "Essence (Gasoline)" to Regex("""(?:essence|gasoline|fuel|p[ée]trole|benzina|carburante|benzin|бензин|汽油)(?=$|\s|[:：\-]|[0-9])\s*[:：\-]?\s*([0-9][0-9.,\s]*[kmbKMB]?)?""", RegexOption.IGNORE_CASE),
            "Diamants (Gems)" to Regex("""(?:diamanti|diamants?|diamonds?|gemmes?|gems?|diamanten|elmas|алмазы|钻石|宝石)(?=$|\s|[:：\-]|[0-9])\s*[:：\-]?\s*([0-9][0-9.,\s]*[kmbKMB]?)?""", RegexOption.IGNORE_CASE),
            "Cash / Or" to Regex("""(?:cash|gold|g[oö]ld|argent\s+liquide|altın|золото|金币|黄金|oro)(?=$|\s|[:：\-]|[0-9])\s*[:：\-]?\s*([0-9][0-9.,\s]*[kmbKMB]?)?|^or\s*[:：\-]\s*([0-9][0-9.,\s]*[kmbKMB]?)?""", RegexOption.IGNORE_CASE),
            "Carte en or (Gold Card)" to Regex("""(?:carte\s+en\s+or|gold\s+card|golden\s+card)(?=$|\s|[:：\-]|[0-9])\s*[:：\-]?\s*([0-9][0-9.,\s]*[kmbKMB]?)?""", RegexOption.IGNORE_CASE),
            "Endurance (Stamina)" to Regex("""(?:endurance|stamina|ausdauer|resistenza|выносливость|体力)(?=$|\s|[:：\-]|[0-9])\s*[:：\-]?\s*([0-9][0-9.,\s]*[kmbKMB]?)?""", RegexOption.IGNORE_CASE),
            "EXP d'équipement (Equipment EXP)" to Regex("""(?:exp\s+d['’]équipement|equipment\s+exp|equipment\s+experience|ausrüstungs-?\s*ep)(?=$|\s|[:：\-]|[0-9])\s*[:：\-]?\s*([0-9][0-9.,\s]*[kmbKMB]?)?""", RegexOption.IGNORE_CASE),
            "XP de héros (Hero XP)" to Regex("""(?:xp\s+de\s+h[ée]ros|hero\s+xp|hero\s+experience|helden-?\s*ep)(?=$|\s|[:：\-]|[0-9])\s*[:：\-]?\s*([0-9][0-9.,\s]*[kmbKMB]?)?""", RegexOption.IGNORE_CASE)
        )

        for ((index, line) in lines.withIndex()) {
            wardenRegex.find(line)?.let { match ->
                if (wardenLevel == null) {
                    wardenLevel = "Niv. ${match.groupValues.drop(1).firstOrNull { it.isNotBlank() }.orEmpty()}"
                }
            }
            if (wardenLevel == null && wardenBuildingLabelRegex.containsMatchIn(line)) {
                val followingLine = lines.getOrNull(index + 1).orEmpty()
                explicitBuildingLevelRegex.find(followingLine)?.groupValues?.get(1)?.let { level ->
                    wardenLevel = "Niv. $level"
                }
            }

            powerRegex.find(line)?.let { powerMatch ->
                if (power == null) {
                    val valueText = powerMatch.groupValues[1].trim()
                    val numeric = parseCompactNumeric(valueText)
                    // Ne pas créer de puissance si le nombre OCR est invalide :
                    // un objet PowerMetric avec une valeur nulle masquerait une
                    // lecture rejetée et ferait croire qu'une puissance existe.
                    if (numeric != null) {
                        val isAmbiguous = valueText.contains("k", true) || valueText.contains("m", true)
                        power = PowerMetric(
                            puissanceTotaleTexte = valueText,
                            puissanceTotaleNumerique = numeric.toLong(),
                            niveauPrisonOuQG = wardenLevel,
                            incertain = isAmbiguous,
                            raisonIncertitude = if (isAmbiguous) "Abréviation OCR à confirmer" else null
                        )
                    } else {
                        // Une valeur correctement groupée mais supérieure à Long.MAX_VALUE
                        // reste visible comme mesure incertaine, sans conversion numérique.
                        val isGroupedInteger = Regex("""^[0-9]{1,3}(?:[,.][0-9]{3}){2,}$""")
                            .matches(valueText)
                        val digits = valueText.filter(Char::isDigit)
                        val exceedsLongRange = isGroupedInteger &&
                            digits.length >= Long.MAX_VALUE.toString().length &&
                            (digits.length > Long.MAX_VALUE.toString().length ||
                                digits > Long.MAX_VALUE.toString())
                        if (exceedsLongRange) {
                            power = PowerMetric(
                                puissanceTotaleTexte = valueText,
                                puissanceTotaleNumerique = null,
                                niveauPrisonOuQG = wardenLevel,
                                incertain = true,
                                raisonIncertitude = "Valeur supérieure à la plage numérique prise en charge"
                            )
                        }
                    }
                }
            }

            // Ressources nommées
            for ((resLabel, regex) in resourcePatterns) {
                if (seenResKeys.contains(resLabel)) continue
                val match = regex.find(line)
                if (match != null) {
                    // « Fuel » peut désigner la ressource Essence ou le carburant
                    // d'un écran Sandsea. Seul, ce mot ne suffit pas à créer un stock.
                    val isFuelLabel = resLabel == "Essence (Gasoline)" &&
                        Regex("""^fuel\b""", RegexOption.IGNORE_CASE).containsMatchIn(line)
                    val hasSandseaContext = Regex(
                        """\b(sandsea|vessel\s+station|lighthouse|sandmeer|schiffsstation|leuchtturm|kum\s+denizi|gemi\s+istasyonu|deniz\s+feneri|песчаное\s+море|станция\s+судов|маяк)\b|沙海|船只站|灯塔""",
                        RegexOption.IGNORE_CASE
                    ).containsMatchIn(rawText)
                    val hasOtherResourceLabel = resourcePatterns
                        .filter { it.first != "Essence (Gasoline)" }
                        .any { (_, resourceRegex) -> resourceRegex.containsMatchIn(rawText) }
                    val isAmbiguousFuelOnly = isFuelLabel && (hasSandseaContext || !hasOtherResourceLabel)
                    if (isAmbiguousFuelOnly) continue
                    val inlineQuantity = match.groupValues.drop(1).firstOrNull { it.isNotBlank() }?.trim()
                    val nextLineQuantity = (index + 1..minOf(lines.lastIndex, index + 2))
                        .firstNotNullOfOrNull { candidateIndex ->
                            val candidate = lines[candidateIndex]
                            val skippedLinesAreOnlySymbols = (index + 1 until candidateIndex).all { skipped ->
                                lines[skipped].none { it.isLetterOrDigit() }
                            }
                            if (!skippedLinesAreOnlySymbols) null else {
                                Regex("""^\s*([0-9][0-9.,\s]*[kmbKMB]?)\s*$""", RegexOption.IGNORE_CASE)
                                    .find(candidate)?.groupValues?.get(1)?.trim()
                            }
                        }
                    val qtyText = inlineQuantity ?: nextLineQuantity ?: continue
                    val numericQuantity = parseCompactNumeric(qtyText) ?: continue
                    seenResKeys.add(resLabel)
                    val isAbbreviated = qtyText.contains(Regex("[kmb]$", RegexOption.IGNORE_CASE))
                    resources.add(
                        ResourceRecord(
                            nom = resLabel,
                            quantiteTexte = qtyText,
                            quantiteNumerique = numericQuantity,
                            incertain = isAbbreviated,
                            raisonIncertitude = if (isAbbreviated) "Valeur OCR locale abrégée" else null
                        )
                    )
                }
            }

            // Robots connus : ne créer une fiche que si le nom exact est lisible.
            for (robotName in knownRobotNames) {
                // Certains noms composés sont coupés sur deux lignes par l'OCR.
                // On accepte la ligne courante + la suivante, sans recherche floue.
                val robotText = lines.drop(index).take(2).joinToString(" ")
                val normalizedLine = " ${normalizeHeroText(robotText)} "
                val normalizedCurrentLine = " ${normalizeHeroText(line)} "
                val normalizedRobot = normalizeHeroText(robotName)
                val currentLineAlreadyNamesARobot = knownRobotNames.any { knownName ->
                    normalizedCurrentLine.contains(" ${normalizeHeroText(knownName)} ")
                }
                val robotNameMatchesCurrentOrSplitLine =
                    normalizedCurrentLine.contains(" $normalizedRobot ") ||
                        (!currentLineAlreadyNamesARobot && normalizedLine.contains(" $normalizedRobot "))
                if (robotNameMatchesCurrentOrSplitLine && seenRobotKeys.add(normalizedRobot)) {
                    // Ne pas emprunter le niveau ou les statistiques du robot suivant
                    // lorsque plusieurs fiches sont visibles dans le même écran.
                    val contextLimit = minOf(lines.size, index + 4)
                    val nextRobotIndex = (index + 1 until contextLimit).firstOrNull { candidateIndex ->
                        val candidateText = lines.drop(candidateIndex).take(2).joinToString(" ")
                        val normalizedCandidate = " ${normalizeHeroText(candidateText)} "
                        knownRobotNames.any { candidateName ->
                            normalizedCandidate.contains(" ${normalizeHeroText(candidateName)} ")
                        }
                    }
                    val nextBuildingPanelIndex = (index + 1 until contextLimit).firstOrNull { candidateIndex ->
                        buildingPanelLabelRegex.containsMatchIn(lines[candidateIndex])
                    }
                    // Ne jamais emprunter un niveau de bâtiment, même si aucune autre fiche robot ne suit.
                    val contextEnd = listOfNotNull(nextRobotIndex, nextBuildingPanelIndex).minOrNull() ?: contextLimit
                    val context = lines.subList(index, maxOf(index + 1, contextEnd)).joinToString(" ")
                    val level = Regex("""(?:lv\.?|level|niv\.?|niveau|seviye|уровень|等级)\s*[:：]?\s*([0-9]{1,3})""", RegexOption.IGNORE_CASE)
                        .find(context)?.groupValues?.get(1)
                    val powerText = Regex("""(?:power|puissance|atk|attack|güç|мощь|сила|战力)\s*[:：]?\s*([0-9]+(?:[.,][0-9]+)?\s*%?)""", RegexOption.IGNORE_CASE)
                        .find(context)?.groupValues?.get(1)?.trim().orEmpty()
                    val bonusText = Regex("""(?:bonus|buff|boost|bonus troop|troop bonus|бонус|加成|奖励)\s*[:：]?\s*([^,;。]{3,70})""", RegexOption.IGNORE_CASE)
                        .find(context)?.groupValues?.get(1)?.trim().orEmpty()
                    val skillText = Regex("""(?:skill|skills|comp[ée]tence|comp[ée]tences|core|beceri|beceriler|навык|навыки|技能)\s*[:：]?\s*([^,;。]{3,90})""", RegexOption.IGNORE_CASE)
                        .find(context)?.groupValues?.get(1)?.trim().orEmpty()
                    val hasDetails = level != null || powerText.isNotBlank() || bonusText.isNotBlank() || skillText.isNotBlank()
                    robots.add(RobotRecord(
                        id = normalizedRobot.replace(' ', '_'),
                        nom = robotName,
                        niveau = level.orEmpty(),
                        bonus = bonusText,
                        competences = skillText,
                        puissance = powerText,
                        incertain = !hasDetails || level == null,
                        raisonIncertitude = if (level == null) "Nom du robot reconnu, niveau non lu explicitement" else null,
                        source = "ocr_local"
                    ))
                }
            }

            // Équipements de héros : ne remplir que les valeurs réellement visibles dans l'OCR.
            val equipmentPartLabels = mapOf(
                "Arme" to listOf("weapon", "arme", "waffe", "silah", "оружие", "武器", "arma"),
                "Armure" to listOf("armor", "armour", "armure", "rüstung", "zırh", "броня", "护甲", "armatura"),
                "Casque" to listOf("helmet", "casque", "helm", "miğfer", "шлем", "头盔", "elmo"),
                "Gants" to listOf("glove", "gloves", "gant", "gants", "handschuh", "handschuhe", "eldiven", "перчатка", "перчатки", "手套", "guanto", "guanti"),
                "Bottes" to listOf("boot", "boots", "botte", "bottes", "stiefel", "bot", "сапог", "сапоги", "靴", "stivale", "stivali"),
                "Accessoire" to listOf("accessory", "accessoire", "zubehör", "aksesuar", "аксессуар", "配件", "accessorio")
            )
            // Normalisation Unicode : conserve cyrillique/chinois et ignore les accents OCR.
            val normalizedEquipmentLine = normalizeEquipmentText(line)
            for ((partName, aliases) in equipmentPartLabels) {
                if (aliases.none { alias ->
                        val normalizedAlias = normalizeEquipmentText(alias)
                        normalizedEquipmentLine == normalizedAlias ||
                            Regex(
                                "^" + Regex.escape(normalizedAlias) +
                                    "\\s+(?:\\d{1,3}|(?:lv\\.?|level|niv\\.?|niveau|stufe|seviye|уровень|等级|livello)\\s*[:+]?\\s*\\d{1,3})$",
                                RegexOption.IGNORE_CASE
                            ).matches(normalizedEquipmentLine)
                    }) continue
                val context = lines.subList(index, minOf(lines.size, index + 4)).joinToString(" ")
                val level = Regex("""(?:lv\.?|level|niv\.?|niveau|stufe|seviye|уровень|等级|livello)\s*[:+]?\s*([0-9]{1,3})""", RegexOption.IGNORE_CASE)
                    .find(context)?.groupValues?.get(1)
                    ?: Regex("""(?:^|\s)\+([0-9]{1,3})(?:\s|$)""").find(context)?.groupValues?.get(1).orEmpty()
                val quality = Regex("""\b(legendary|mythic|epic|rare|common|légendaire|épique|legendär|episch|selten|efsanevi|destansı|nadir|легендарный|эпический|редкий|обычный|传奇|神话|史诗|稀有|leggendario|mitico|epico|raro|comune)\b""", RegexOption.IGNORE_CASE)
                    .find(context)?.value.orEmpty()
                val materialAliases = listOf(
                    "magnet", "aimant", "potential coil", "precision gear", "equipment part",
                    "mıknatıs", "hassas dişli", "potansiyel bobin",
                    "магнит", "магнитная катушка", "прецизионная шестерня", "материал",
                    "磁铁", "精密齿轮", "潜能线圈",
                    "magnete", "ingranaggio di precisione", "bobina potenziale", "componente equipaggiamento"
                )
                val equipmentContextLines = lines.subList(index, minOf(lines.size, index + 4))
                // Les matériaux et leur quantité sont souvent affichés sur deux lignes.
                // Conserver uniquement les quantités explicitement adjacentes à un libellé connu.
                val materials = materialAliases.mapNotNull { alias ->
                    val materialIndex = equipmentContextLines.indexOfFirst {
                        it.contains(alias, ignoreCase = true)
                    }
                    if (materialIndex < 0) null else {
                        val materialLine = equipmentContextLines[materialIndex].trim()
                        val quantityLine = equipmentContextLines.getOrNull(materialIndex + 1)
                            ?.trim()
                            ?.takeIf {
                                Regex("""^(?:x\s*)?[0-9][0-9.,\s]*[kmbKMB]?$""", RegexOption.IGNORE_CASE)
                                    .matches(it)
                            }
                        if (quantityLine.isNullOrBlank()) materialLine.take(80)
                        else "$materialLine: $quantityLine".take(100)
                    }
                }.distinct().take(4).joinToString(" | ")
                val key = (partName + "-" + level.ifBlank { "?" }).lowercase()
                if (seenEquipmentKeys.add(key)) equipment.add(
                    EquipmentRecord(
                        id = key.replace(' ', '_'),
                        piece = partName,
                        niveau = level,
                        qualite = quality,
                        materiaux = materials,
                        texteObserve = context.take(220),
                        incertain = level.isBlank(),
                        raisonIncertitude = if (level.isBlank()) "Pièce repérée, niveau non lu explicitement" else null
                    )
                )
            }

            // Héros connus du jeu détectés dans la ligne
            for (heroName in KNOWN_HEROES) {
                if (matchesHeroName(heroName, line) && !seenHeroKeys.contains(heroName.lowercase())) {
                    // Ne jamais emprunter les étoiles, la rareté ou le niveau du héros suivant.
                    // Les captures OCR présentent souvent plusieurs cartes de héros à la suite.
                    val isAnotherHeroLine: (String) -> Boolean = { candidateLine ->
                        KNOWN_HEROES.any { otherName -> matchesHeroName(otherName, candidateLine) }
                    }
                    val contextLines = lines.subList(index, minOf(lines.size, index + 3))
                        .withIndex()
                        .takeWhile { indexed -> indexed.index == 0 || !isAnotherHeroLine(indexed.value) }
                        .map { it.value }
                    val contextText = contextLines.joinToString(" ")
                    val sameLineLvl = levelRegex.find(line)?.groupValues?.get(1)?.toIntOrNull()
                    val nextLine = lines.getOrNull(index + 1)
                    val nextLineIsAnotherHero = nextLine?.let(isAnotherHeroLine) == true
                    val nextLineLvl = if (nextLineIsAnotherHero) null else nextLine?.let {
                        levelRegex.find(it)?.groupValues?.get(1)?.toIntOrNull()
                    }
                    val lvl = sameLineLvl ?: nextLineLvl
                    // Ne renseigner les étoiles et la rareté que si un libellé/visuel explicite est lu.
                    val starMatch = Regex("""(?:([1-5])\s*(?:étoiles?|stars?)|([1-5])\s*[★☆]|([★☆]{1,5}))""", RegexOption.IGNORE_CASE)
                        .find(contextText)
                    val stars = when {
                        starMatch == null -> null
                        starMatch.groupValues[3].isNotBlank() -> starMatch.groupValues[3].count { it == '★' }.takeIf { it > 0 }?.toString()
                        else -> (starMatch.groupValues[1].ifBlank { starMatch.groupValues[2].ifBlank { starMatch.groupValues[0].filter { it.isDigit() } } })
                    }?.let { "$it/5" }
                    val rarity = Regex("""\b(legendary|mythic|epic|rare|common|légendaire|épique)\b""", RegexOption.IGNORE_CASE)
                        .find(contextText)?.value
                    seenHeroKeys.add(heroName.lowercase())
                    val catalogEntry = HeroCatalog.entries.firstOrNull {
                        it.nom.equals(heroName, ignoreCase = true)
                    }
                    heroes.add(
                        HeroRecord(
                            nom = heroName,
                            niveau = lvl,
                            niveauTexte = if (lvl != null) "Niv. $lvl" else "Niveau non lu",
                            rarete = rarity,
                            etoiles = stars,
                            roleHero = catalogEntry?.usageWarObserve,
                            typeTroupe = catalogEntry?.typeTroupe,
                            categorie = catalogEntry?.rarete,
                            saison = catalogEntry?.saison,
                            imageUrl = catalogEntry?.portraitUrl,
                            avatarUrl = catalogEntry?.portraitUrl,
                            wikiUrl = catalogEntry?.sourceUrl,
                            incertain = lvl == null || stars == null || rarity == null,
                            raisonIncertitude = listOfNotNull(
                                if (lvl == null) "Niveau non lu" else null,
                                if (stars == null) "Étoiles non lues explicitement" else null,
                                if (rarity == null) "Rareté non lue explicitement" else null
                            ).joinToString(" ; ")
                        )
                    )
                }
            }

            // Ne jamais créer un héros à partir d'un nom arbitraire suivi de « Lv ».
            // Les héros valides sont déjà ajoutés par la correspondance stricte au catalogue
            // ci-dessus ; un libellé de bâtiment (« Warehouse Lv 12 ») ne doit pas devenir héros.
        }

        // Les portraits confirmés sont fusionnés après l'OCR, même si aucune ligne de texte n'a été lue.
        // Aucun niveau, bonus ou compétence n'est déduit de l'image.
        for (portrait in visualRobotMatches.filter { it.isConfirmed }) {
            val robotId = portrait.robotId?.trim().orEmpty()
            val displayName = portrait.displayName?.trim().orEmpty()
            if (robotId.isBlank() || displayName.isBlank()) continue
            val key = normalizeHeroText(robotId)
            if (!seenRobotKeys.add(key)) continue
            robots.add(
                RobotRecord(
                    id = robotId,
                    nom = displayName,
                    incertain = true,
                    raisonIncertitude = "Identité proposée par comparaison du portrait ; détails à vérifier en jeu",
                    source = "portrait_local"
                )
            )
        }

        // Player profile-card badge: unlike the home screen, this value is in the
        // right-side profile panel, immediately above the Eliminations row.
        // Coordinates disambiguate it from the elimination count and top resource HUD.
        if (power == null && textBoxes.isNotEmpty()) {
            val profileBadge = profilePowerBadgeFromCoordinates(textBoxes, imageWidth, imageHeight)
            val numeric = profileBadge?.let(::parseCompactNumeric)
            if (profileBadge != null && numeric != null) {
                power = PowerMetric(
                    puissanceTotaleTexte = profileBadge,
                    puissanceTotaleNumerique = numeric.toLong(),
                    niveauPrisonOuQG = wardenLevel,
                    incertain = false,
                    raisonIncertitude = null
                )
            }
        }

        // Profil joueur : la puissance est affichée avec une icône de poing,
        // juste avant la ligne « Eliminations », sans libellé « Puissance ».
        // La ligne numérique située avant « Eliminations » peut aussi être une
        // quantité de ressource (notamment Food). N'inférer la puissance depuis
        // cette position que si aucune ressource nommée ni rangée de ressources
        // supérieure n'a été détectée. La sécurité prime sur une valeur supposée.
        val hasPlayerProfileText = Regex("""\bID\s*[:#]?\s*[0-9]{5,}""", RegexOption.IGNORE_CASE).containsMatchIn(rawText) &&
            Regex("""\b(?:alliance|serveur|server)\b""", RegexOption.IGNORE_CASE).containsMatchIn(rawText)
        if (power == null && resources.isEmpty() && hasPlayerProfileText &&
            !hasPotentialTopResourceNumberRow(textBoxes, imageWidth, imageHeight)
        ) {
            val eliminationLineIndex = lines.indexOfFirst {
                Regex("""[ée]liminations?""", RegexOption.IGNORE_CASE).containsMatchIn(it)
            }
            if (eliminationLineIndex >= 0) {
                val compactValueRegex = Regex("""\b\d+(?:[.,]\d+)?\s*[MB]\b""", RegexOption.IGNORE_CASE)
                val candidate = (eliminationLineIndex downTo maxOf(0, eliminationLineIndex - 2))
                    .firstNotNullOfOrNull { lineIndex ->
                        val line = lines[lineIndex]
                        val beforeEliminations = if (lineIndex == eliminationLineIndex) {
                            (Regex("""[ée]liminations?""", RegexOption.IGNORE_CASE).find(line)?.range?.first?.let { line.take(it) } ?: line)
                        } else {
                            line
                        }
                        compactValueRegex.findAll(beforeEliminations).lastOrNull()?.value?.trim()
                    }
                if (candidate != null) {
                    power = PowerMetric(
                        puissanceTotaleTexte = candidate,
                        puissanceTotaleNumerique = parseCompactNumeric(candidate)?.toLong(),
                        niveauPrisonOuQG = wardenLevel,
                        incertain = false,
                        raisonIncertitude = null
                    )
                }
            }
        }

        // Si aucune puissance explicite mais grand nombre de puissance top-bar (ex: "12.4M" ou "8,450,120")
        // Une grande quantité de ressource dans la barre supérieure ne doit jamais
        // être réinterprétée comme la puissance du joueur. Sans libellé explicite,
        // le fallback puissance n'est autorisé que si aucune ressource n'a été lue.
        // Dès que des boîtes OCR avec coordonnées existent, les nombres doivent
        // être interprétés à partir de leur position réelle (badge ou rangée HUD).
        // Le fallback textuel sans coordonnées pourrait prendre la nourriture
        // ou une autre ressource pour la puissance et empêcher le badge d'être lu.
        if (power == null && resources.isEmpty() && textBoxes.isEmpty() &&
            !powerRegex.containsMatchIn(rawText) &&
            !hasPotentialTopResourceNumberRow(textBoxes, imageWidth, imageHeight)
        ) {
            val topPowerLine = lines.take(6).firstOrNull { l ->
                Regex("""\b\d{1,3}(?:[.,\s]\d{3}){2,}\b|\b\d+(?:[.,]\d+)?\s*[MB]\b""").containsMatchIn(l)
            }
            if (topPowerLine != null) {
                val valueText = Regex("""\d{1,3}(?:[.,\s]\d{3}){2,}|\d+(?:[.,]\d+)?\s*[MB]""")
                    .find(topPowerLine)?.value?.trim() ?: topPowerLine
                power = PowerMetric(
                    puissanceTotaleTexte = valueText,
                    puissanceTotaleNumerique = parseCompactNumeric(valueText)?.toLong(),
                    niveauPrisonOuQG = wardenLevel,
                    incertain = true,
                    raisonIncertitude = "Nombre détecté en haut d'écran (probablement Puissance)"
                )
            }
        }

        if (power != null && power?.niveauPrisonOuQG == null && wardenLevel != null) {
            power = power?.copy(niveauPrisonOuQG = wardenLevel)
        } else if (power == null && wardenLevel != null) {
            // Le niveau du Bureau du directeur reste une donnée utile même si
            // la puissance n'est pas visible/lisible sur cette capture.
            // Le libellé explicite évite de présenter une puissance inventée.
            power = PowerMetric(
                puissanceTotaleTexte = "Non mesurée",
                niveauPrisonOuQG = wardenLevel,
                incertain = false,
                raisonIncertitude = null
            )
        }

        // Ne jamais mélanger des valeurs nommées avec une supposition par position.
        // Le mode positionnel n'est utilisé que si aucune ressource n'a été lue par son nom.
        val topNumbers = topResourceNumberBoxes(textBoxes, imageWidth, imageHeight)
        // Les compteurs HUD ne portent pas leur libellé OCR. Leur attribuer un type
        // par simple ordre horizontal est trop fragile : la barre varie selon l'écran.
        // On conserve les valeurs pour revue, mais elles ne deviennent jamais un stock.
        if (resources.isEmpty() && topNumbers.isNotEmpty()) {
            topNumbers.forEachIndexed { index, box ->
                uncertain.add(
                    UncertainItem(
                        champ = "Compteur HUD non identifié " + (index + 1),
                        valeurObservee = box.text.trim(),
                        raison = "Icône/libellé de ressource non reconnu ; attribution automatique interdite."
                    )
                )
            }
        }

        val screenRecognition = ScreenRecognitionEngine.analyze(rawText)
        // Conserver une trace exploitable même si l'OCR lit l'écran d'amélioration
        // mais ne parvient pas à distinguer la pièce. Aucun niveau n'est déduit.
        if (screenRecognition.category == "equipements" && equipment.isEmpty()) {
            val observedMaterials = lines.filter { line ->
                listOf(
                    "magnet", "aimant", "potential coil", "precision gear", "mıknatıs",
                    "магнит", "магнитная катушка", "磁铁", "精密齿轮",
                    "magnete", "ingranaggio di precisione", "bobina potenziale"
                ).any { line.contains(it, ignoreCase = true) }
            }.take(4).joinToString(" | ")
            val isExclusive = rawText.contains("exclusive", true) ||
                rawText.contains("exclusif", true) ||
                rawText.contains("exklusiv", true) ||
                rawText.contains("özel ekipman", true) ||
                rawText.contains("эксклюзив", true) ||
                rawText.contains("专属装备", true) ||
                rawText.contains("esclusivo", true)
            equipment.add(
                EquipmentRecord(
                    id = if (isExclusive) "exclusive_equipment_screen" else "hero_equipment_screen",
                    piece = if (isExclusive) "Équipement exclusif" else "Équipement de héros",
                    materiaux = observedMaterials,
                    texteObserve = lines.take(10).joinToString(" · ").take(220),
                    incertain = true,
                    raisonIncertitude = "Écran d'amélioration reconnu, mais pièce ou niveau non identifiés avec certitude"
                )
            )
        }
        // Home-screen power badge: the blue fist counter is positioned below
        // the top resource row, on the left. Only accept a grouped integer in
        // this narrow normalized region; never promote the resource row itself.
        if (power == null && textBoxes.isNotEmpty() && imageWidth > 0 && imageHeight > 0) {
            val powerBadge = textBoxes
                .filter { box ->
                    val centerY = (box.top + box.bottom) / 2f
                    centerY >= imageHeight * 0.095f &&
                        centerY <= imageHeight * 0.18f &&
                        box.left >= 0 &&
                        box.left <= imageWidth * 0.32f &&
                        box.right <= imageWidth * 0.36f
                }
                .mapNotNull { box ->
                    val valueText = box.text.trim()
                    val digits = valueText.filter(Char::isDigit)
                    if (digits.length !in 7..12 ||
                        !Regex("""^[0-9][0-9\s.,]*$""").matches(valueText)
                    ) null else valueText to parseCompactNumeric(valueText)
                }
                .firstOrNull { it.second != null }
            if (powerBadge != null) {
                power = PowerMetric(
                    puissanceTotaleTexte = powerBadge.first,
                    puissanceTotaleNumerique = powerBadge.second?.toLong(),
                    niveauPrisonOuQG = wardenLevel,
                    incertain = false,
                    raisonIncertitude = null
                )
            }
        }

        val detectedCategory = when {
            screenRecognition.category != "ocr_local" -> screenRecognition.category
            equipment.isNotEmpty() -> "equipements"
            heroes.isNotEmpty() -> "heros"
            robots.isNotEmpty() -> "robots"
            resources.isNotEmpty() -> "ressources"
            power != null -> "profil"
            else -> "ocr_local"
        }
        if (screenRecognition.needsVisualConfirmation && screenRecognition.category != "ocr_local") {
            uncertain.add(
                UncertainItem(
                    champ = "Type de capture",
                    valeurObservee = detectedCategory,
                    raison = "Classification OCR de confiance ${screenRecognition.confidence}; vérification visuelle recommandée."
                )
            )
        }

        if (screenRecognition.category == "equipements" && equipment.isEmpty()) {
            uncertain.add(UncertainItem(
                champ = "Équipement",
                valeurObservee = "Écran d'amélioration d'équipement détecté",
                raison = "Les pièces, niveaux et matériaux ne sont pas encore lisibles avec certitude dans ce texte OCR."
            ))
        }

        if (screenRecognition.category == "robots" && robots.isEmpty()) {
            uncertain.add(UncertainItem(
                champ = "Robot",
                valeurObservee = "Écran de robot détecté",
                raison = "L'écran est identifié, mais aucun modèle et niveau exacts n'ont été lus. Vérification visuelle nécessaire."
            ))
        }
        if (screenRecognition.category == "ressources" && resources.isEmpty()) {
            uncertain.add(UncertainItem(
                champ = "Ressources",
                valeurObservee = "Écran de ressources détecté",
                raison = "Les icônes et quantités ne sont pas encore associées avec certitude par l'OCR textuel."
            ))
        }

        if (lines.isEmpty()) {
            uncertain.add(
                UncertainItem(
                    champ = "Texte OCR",
                    valeurObservee = "Aucun texte reconnu",
                    raison = "Capture sombre, sans texte lisible ou en chargement"
                )
            )
        }

        val ocrSnippet = if (lines.isNotEmpty()) {
            lines.take(6).joinToString(" · ").take(240)
        } else {
            "Image capturée (aucun texte OCR lisible)"
        }

        return LocalOcrExtraction(
            rawText = rawText,
            detectedCategory = detectedCategory,
            ocrSnippet = ocrSnippet,
            power = power,
            resources = resources,
            heroes = heroes,
            robots = robots,
            uncertainItems = uncertain,
            equipment = equipment
        )
    }

    private fun normalizeEquipmentText(value: String): String =
        java.text.Normalizer.normalize(value.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .trim()

    private fun normalizeHeroText(value: String): String =
        java.text.Normalizer.normalize(value.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    private fun matchesHeroName(canonicalName: String, line: String): Boolean {
        val normalizedLine = " ${normalizeHeroText(line)} "
        val words = normalizedLine.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

        // Les alias/traductions doivent correspondre exactement : les comparer
        // de façon approximative crée des collisions (ex. « Berry » / « Terry »).
        val exactNames = listOf(canonicalName) + HERO_ALIASES[canonicalName].orEmpty()
        if (exactNames.any { candidate ->
                val normalized = normalizeHeroText(candidate)
                normalized.isNotBlank() && normalizedLine.contains(" $normalized ")
            }) return true

        // La tolérance OCR ne s'applique qu'au nom canonique, jamais aux alias.
        val target = normalizeHeroText(canonicalName)
        val targetWords = target.split(Regex("\\s+")).filter { it.isNotBlank() }
        return if (targetWords.size > 1) {
            targetWords.all { expected ->
                words.any { actual ->
                    expected.length >= 6 &&
                        kotlin.math.abs(actual.length - expected.length) <= 1 &&
                        editDistance(actual, expected) <= 1
                }
            }
        } else {
            val expected = targetWords.firstOrNull() ?: return false
            if (expected.length < 5) false
            else words.any { actual ->
                val maxDistance = if (expected.length >= 9) 2 else 1
                kotlin.math.abs(actual.length - expected.length) <= maxDistance &&
                    editDistance(actual, expected) <= maxDistance
            }
        }
    }

    private fun editDistance(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length
        var previous = IntArray(right.length + 1) { it }
        for (i in left.indices) {
            val current = IntArray(right.length + 1)
            current[0] = i + 1
            for (j in right.indices) {
                val cost = if (left[i] == right[j]) 0 else 1
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + cost
                )
            }
            previous = current
        }
        return previous[right.length]
    }

    private fun parseCompactNumeric(text: String): Double? {
        val original = text.trim().lowercase()
        if (original.isEmpty()) return null
        val suffix = original.lastOrNull()?.takeIf { it in listOf('k', 'm', 'b') }
        val multiplier = when (suffix) {
            'b' -> 1_000_000_000.0
            'm' -> 1_000_000.0
            'k' -> 1_000.0
            else -> 1.0
        }
        val spacedNumber = if (suffix != null) original.dropLast(1) else original
        if (spacedNumber.any(Char::isWhitespace)) {
            val groups = spacedNumber.trim().split(Regex("\\s+"))
            val validFirstGroup = groups.firstOrNull()?.matches(Regex("[0-9]{1,3}")) == true
            val followingGroups = groups.drop(1)
            val validFollowingGroups = followingGroups.isNotEmpty() &&
                followingGroups.dropLast(1).all { group -> Regex("[0-9]{3}").matches(group) } &&
                followingGroups.last().let { group ->
                    Regex("[0-9]{3}(?:[.,][0-9]+)?").matches(group)
                }
            if (!validFirstGroup || !validFollowingGroups) return null
        }
        val cleaned = original.replace(Regex("\\s+"), "")
        val rawNumber = if (suffix != null) cleaned.dropLast(1) else cleaned
        if (rawNumber.isEmpty() || !Regex("[0-9.,]+").matches(rawNumber)) return null

        val hasComma = rawNumber.contains(',')
        val hasDot = rawNumber.contains('.')
        val normalized = when {
            // En format mixte, le dernier séparateur est décimal ; les groupes
            // précédents doivent être des milliers de trois chiffres.
            hasComma && hasDot -> {
                val decimalIndex = rawNumber.lastIndexOfAny(charArrayOf(',', '.'))
                val integerPart = rawNumber.substring(0, decimalIndex)
                val decimalPart = rawNumber.substring(decimalIndex + 1)
                val groups = integerPart.split(',', '.')
                if (groups.isEmpty() || groups.first().length !in 1..3 ||
                    groups.drop(1).any { it.length != 3 } ||
                    decimalPart.isEmpty() || !decimalPart.all(Char::isDigit)
                ) return null
                groups.joinToString("") + "." + decimalPart
            }
            hasComma || hasDot -> {
                val separator = if (hasComma) ',' else '.'
                val groups = rawNumber.split(separator)
                if (groups.any { it.isEmpty() }) return null
                when {
                    groups.size == 1 -> groups[0]
                    groups.size == 2 && groups[1].length == 3 -> groups.joinToString("")
                    groups.size == 2 && groups[1].all(Char::isDigit) -> groups.joinToString(".")
                    groups.size > 2 && groups.first().length in 1..3 &&
                        groups.drop(1).all { it.length == 3 } -> groups.joinToString("")
                    else -> return null
                }
            }
            else -> rawNumber
        }
        val value = normalized.toDoubleOrNull()?.times(multiplier) ?: return null
        // Une valeur hors plage ne doit jamais devenir Long.MAX_VALUE lors de
        // la conversion de puissance, ni Infinity dans les stocks de ressources.
        return value.takeIf { it.isFinite() && it < Long.MAX_VALUE.toDouble() }
    }
}
