package com.landsofjail.assistant.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
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

    suspend fun recognizeBitmapDetailed(bitmap: Bitmap): OcrRecognition = suspendCancellableCoroutine { cont ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
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
                if (cont.isActive) cont.resume(OcrRecognition(result.text, boxes, bitmap.width, bitmap.height))
            }
            .addOnFailureListener { err ->
                recognizer.close()
                if (cont.isActive) cont.resumeWithException(err)
            }
    }

    suspend fun recognizeBitmap(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        cont.invokeOnCancellation { recognizer.close() }
        recognizer.process(image)
            .addOnSuccessListener { result ->
                recognizer.close()
                if (cont.isActive) cont.resume(result.text)
            }
            .addOnFailureListener { err ->
                recognizer.close()
                if (cont.isActive) cont.resumeWithException(err)
            }
    }

    /**
     * Sélectionne uniquement la rangée horizontale la plus cohérente de nombres
     * située dans la zone supérieure. Cela évite de confondre les ressources
     * avec des niveaux, compteurs ou nombres provenant d'autres panneaux.
     */
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
        return if (bestRow.size >= 4) bestRow.distinctBy { it.left to it.text }.sortedBy { it.left } else emptyList()
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
        val knownRobotNames = listOf("Musashimaru", "Phantom Cat", "Ranger", "Infercore", "Hercules α", "Hercules Alpha", "Halo", "Light Cone", "Atlax", "Yokozuna", "Bastion", "Pluto", "Dauntless", "Valiant", "Scorpio")
        val seenHeroKeys = mutableSetOf<String>()
        val seenResKeys = mutableSetOf<String>()
        var power: PowerMetric? = null
        var wardenLevel: String? = null

        val powerRegex = Regex("""(?:puissance|power|comb\.?\s*power|cp)\s*[:\-]?\s*([0-9][0-9\s.,kmbKMB]+)""", RegexOption.IGNORE_CASE)
        val levelRegex = Regex("""(?:niv\.?|lv\.?|level)\s*([0-9]+)""", RegexOption.IGNORE_CASE)
        val wardenRegex = Regex("""(?:warden\s*office|bureau\s*du\s*directeur|prison|qg|hq)\s*(?:niv\.?|lv\.?|level)?\s*([0-9]+)""", RegexOption.IGNORE_CASE)

        val resourcePatterns = listOf(
            "Nourriture (Food)" to Regex("""(?:nourriture|food|vivres|nahrung|yiyecek|еда|食物|cibo)\s*[:：\-]?\s*([0-9]+(?:[.,][0-9]+)?\s*[kmbKMB]?)?""", RegexOption.IGNORE_CASE),
            "Bois (Wood)" to Regex("""(?:bois|wood|lumber|holz|odun|дерево|木材|legno)\s*[:：\-]?\s*([0-9]+(?:[.,][0-9]+)?\s*[kmbKMB]?)?""", RegexOption.IGNORE_CASE),
            "Acier (Steel)" to Regex("""(?:acier|steel|fer|iron|stahl|çelik|сталь|钢材|acciaio)\s*[:：\-]?\s*([0-9]+(?:[.,][0-9]+)?\s*[kmbKMB]?)?""", RegexOption.IGNORE_CASE),
            "Essence (Gasoline)" to Regex("""(?:essence|gasoline|fuel|p[ée]trole|benzina|carburante|benzin|бензин|汽油)\s*[:：\-]?\s*([0-9]+(?:[.,][0-9]+)?\s*[kmbKMB]?)?""", RegexOption.IGNORE_CASE),
            "Diamants (Gems)" to Regex("""(?:diamanti|diamants?|diamonds?|gemmes?|gems?|diamanten|elmas|алмазы|钻石|宝石)\s*[:：\-]?\s*([0-9]+(?:[.,][0-9]+)?\s*[kmbKMB]?)?""", RegexOption.IGNORE_CASE),
            "Cash / Or" to Regex("""(?:cash|gold|g[oö]ld|altın|золото|金币|黄金|oro)\s*[:：\-]?\s*([0-9]+(?:[.,][0-9]+)?\s*[kmbKMB]?)?|^or\s*[:：\-]\s*([0-9]+(?:[.,][0-9]+)?\s*[kmbKMB]?)?""", RegexOption.IGNORE_CASE)
        )

        for ((index, line) in lines.withIndex()) {
            wardenRegex.find(line)?.let { match ->
                if (wardenLevel == null) {
                    wardenLevel = "Niv. ${match.groupValues[1]}"
                }
            }

            powerRegex.find(line)?.let { powerMatch ->
                if (power == null) {
                    val valueText = powerMatch.groupValues[1].trim()
                    val numeric = parseCompactNumeric(valueText)
                    val isAmbiguous = valueText.contains("k", true) || valueText.contains("m", true) || numeric == null
                    power = PowerMetric(
                        puissanceTotaleTexte = valueText,
                        puissanceTotaleNumerique = numeric?.toLong(),
                        niveauPrisonOuQG = wardenLevel,
                        incertain = isAmbiguous,
                        raisonIncertitude = if (isAmbiguous) "Abréviation ou séparateur OCR à confirmer" else null
                    )
                }
            }

            // Ressources nommées
            for ((resLabel, regex) in resourcePatterns) {
                if (seenResKeys.contains(resLabel)) continue
                val match = regex.find(line)
                if (match != null) {
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
                    seenResKeys.add(resLabel)
                    resources.add(
                        ResourceRecord(
                            nom = resLabel,
                            quantiteTexte = qtyText,
                            quantiteNumerique = parseCompactNumeric(qtyText),
                            incertain = qtyText.contains(Regex("[kmb]$", RegexOption.IGNORE_CASE)) || parseCompactNumeric(qtyText) == null,
                            raisonIncertitude = if (qtyText.contains(Regex("[kmb]$", RegexOption.IGNORE_CASE))) "Valeur OCR locale abrégée" else if (parseCompactNumeric(qtyText) == null) "Valeur numérique non interprétable" else null
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
                val normalizedRobot = normalizeHeroText(robotName)
                if (normalizedLine.contains(" $normalizedRobot ") && seenRobotKeys.add(normalizedRobot)) {
                    val context = lines.subList(index, minOf(lines.size, index + 4)).joinToString(" ")
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
                                    "\\s+(?:\\+\\s*\\d{1,3}|(?:lv\\.?|level|niv\\.?|niveau|stufe|seviye|уровень|等级|livello)\\s*[:+]?\\s*\\d{1,3})$",
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
                val materials = materialAliases.firstOrNull { context.contains(it, ignoreCase = true) }
                    ?.let { alias ->
                        lines.subList(index, minOf(lines.size, index + 4))
                            .firstOrNull { it.contains(alias, ignoreCase = true) }?.trim()?.take(80) ?: alias
                    }.orEmpty()
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

            // Héros générique avec motif Lv/Niv
            val lvlMatch = levelRegex.find(line)
            if (lvlMatch != null) {
                val candidateName = line.substring(0, lvlMatch.range.first)
                    .replace(Regex("""[^A-Za-zÀ-ÿ0-9\s\-']"""), " ")
                    .trim()
                val lvl = lvlMatch.groupValues[1].toIntOrNull()
                if (candidateName.length in 3..24 &&
                    !seenHeroKeys.contains(candidateName.lowercase()) &&
                    !candidateName.contains("warden", true) &&
                    !candidateName.contains("prison", true) &&
                    !candidateName.contains("bureau", true)
                ) {
                    seenHeroKeys.add(candidateName.lowercase())
                    heroes.add(
                        HeroRecord(
                            nom = candidateName,
                            niveau = lvl,
                            niveauTexte = "Niv. ${lvl ?: "?"}",
                            incertain = true,
                            raisonIncertitude = "Extrait par OCR local ; vérifiez le nom en jeu"
                        )
                    )
                }
            }
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

        // Profil joueur : la puissance est affichée avec une icône de poing,
        // juste avant la ligne « Eliminations », sans libellé « Puissance ».
        if (power == null) {
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
        if (power == null) {
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
        }

        // Ne jamais mélanger des valeurs nommées avec une supposition par position.
        // Le mode positionnel n'est utilisé que si aucune ressource n'a été lue par son nom.
        val topNumbers = topResourceNumberBoxes(textBoxes, imageWidth, imageHeight)
        val resourceOrder = listOf(
            "Nourriture (Food)", "Bois (Wood)", "Acier (Steel)",
            "Essence (Gasoline)", "Diamants (Gems)", "Cash / Or"
        )
        if (resources.isEmpty() && topNumbers.size >= 4) {
            topNumbers.take(resourceOrder.size).forEachIndexed { index, box ->
                val name = resourceOrder[index]
                if (seenResKeys.add(name)) {
                    val quantity = box.text.trim()
                    resources.add(
                        ResourceRecord(
                            nom = name,
                            quantiteTexte = quantity,
                            quantiteNumerique = parseCompactNumeric(quantity),
                            incertain = true,
                            raisonIncertitude = "Valeur associée à la position de l'icône dans la barre supérieure ; confirmer visuellement."
                        )
                    )
                }
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
        val cleaned = text.trim().lowercase().replace(Regex("\\s+"), "")
        val suffix = cleaned.lastOrNull()?.takeIf { it in listOf('k', 'm', 'b') }
        val multiplier = when (suffix) {
            'b' -> 1_000_000_000.0
            'm' -> 1_000_000.0
            'k' -> 1_000.0
            else -> 1.0
        }
        val rawNumber = if (suffix != null) cleaned.dropLast(1) else cleaned
        // Sans suffixe, les groupes de trois chiffres séparés par . ou , sont des milliers
        // (ex. 8,450,120 ou 8.450.120), pas des décimales.
        val isGroupedInteger = suffix == null &&
            Regex("\\d{1,3}(?:[.,]\\d{3})+").matches(rawNumber)
        val normalized = if (isGroupedInteger) {
            rawNumber.replace(Regex("[.,]"), "")
        } else {
            rawNumber.replace(",", ".")
        }
        val numPart = normalized.replace(Regex("[^0-9.]"), "")
        return numPart.toDoubleOrNull()?.times(multiplier)
    }
}
