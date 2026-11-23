package com.landsofjail.assistant.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.landsofjail.assistant.model.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class LocalOcrExtraction(
    val rawText: String,
    val detectedCategory: String,
    val ocrSnippet: String,
    val power: PowerMetric?,
    val resources: List<ResourceRecord>,
    val heroes: List<HeroRecord>,
    val uncertainItems: List<UncertainItem>
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
     * Analyse OCR locale complète (puissance, QG/Prison, héros, ressources, type d'écran)
     * permettant un affichage immédiat dans l'appli même hors-ligne ou sans backend.
     */
    fun parseLocalOcrText(rawText: String): LocalOcrExtraction {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val lowerFull = rawText.lowercase()
        val uncertain = mutableListOf<UncertainItem>()
        val resources = mutableListOf<ResourceRecord>()
        val heroes = mutableListOf<HeroRecord>()
        val seenHeroKeys = mutableSetOf<String>()
        val seenResKeys = mutableSetOf<String>()
        var power: PowerMetric? = null
        var wardenLevel: String? = null

        val powerRegex = Regex("""(?:puissance|power|comb\.?\s*power|cp)\s*[:\-]?\s*([0-9][0-9\s.,kmbKMB]+)""", RegexOption.IGNORE_CASE)
        val levelRegex = Regex("""(?:niv\.?|lv\.?|level)\s*([0-9]+)""", RegexOption.IGNORE_CASE)
        val wardenRegex = Regex("""(?:warden\s*office|bureau\s*du\s*directeur|prison|qg|hq)\s*(?:niv\.?|lv\.?|level)?\s*([0-9]+)""", RegexOption.IGNORE_CASE)

        val resourcePatterns = listOf(
            "Nourriture (Food)" to Regex("""(?:nourriture|food|vivres)\s*[:\-]?\s*([0-9]+(?:[.,][0-9]+)?\s*[kmbKMB]?)""", RegexOption.IGNORE_CASE),
            "Bois (Wood)" to Regex("""(?:bois|wood|lumber)\s*[:\-]?\s*([0-9]+(?:[.,][0-9]+)?\s*[kmbKMB]?)""", RegexOption.IGNORE_CASE),
            "Acier (Steel)" to Regex("""(?:acier|steel|fer|iron)\s*[:\-]?\s*([0-9]+(?:[.,][0-9]+)?\s*[kmbKMB]?)""", RegexOption.IGNORE_CASE),
            "Essence (Gasoline)" to Regex("""(?:essence|gasoline|fuel|p[ée]trole)\s*[:\-]?\s*([0-9]+(?:[.,][0-9]+)?\s*[kmbKMB]?)""", RegexOption.IGNORE_CASE),
            "Diamants (Gems)" to Regex("""(?:diamants?|diamonds?|gemmes?|gems?)\s*[:\-]?\s*([0-9]+(?:[.,][0-9]+)?\s*[kmbKMB]?)""", RegexOption.IGNORE_CASE),
            "Cash / Or" to Regex("""(?:cash|gold|or)\s*[:\-]?\s*([0-9]+(?:[.,][0-9]+)?\s*[kmbKMB]?)""", RegexOption.IGNORE_CASE)
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
                    val qtyText = match.groupValues[1].trim()
                    seenResKeys.add(resLabel)
                    resources.add(
                        ResourceRecord(
                            nom = resLabel,
                            quantiteTexte = qtyText,
                            quantiteNumerique = parseCompactNumeric(qtyText),
                            incertain = qtyText.contains("k", true) || qtyText.contains("m", true),
                            raisonIncertitude = "Valeur OCR locale abrégée"
                        )
                    )
                }
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

        val screenRecognition = ScreenRecognitionEngine.analyze(rawText)
        val detectedCategory = when {
            screenRecognition.category != "ocr_local" -> screenRecognition.category
            heroes.isNotEmpty() -> "heros"
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
            uncertainItems = uncertain
        )
    }

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
