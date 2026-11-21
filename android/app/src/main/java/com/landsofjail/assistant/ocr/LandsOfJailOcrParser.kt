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
    val power: PowerMetric?,
    val resources: List<ResourceRecord>,
    val heroes: List<HeroRecord>,
    val uncertainItems: List<UncertainItem>
)

object LandsOfJailOcrParser {

    suspend fun recognizeBitmap(bitmap: Bitmap): String = suspendCancellableCoroutine { cont ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                cont.resume(result.text)
            }
            .addOnFailureListener { err ->
                cont.resumeWithException(err)
            }
    }

    /**
     * Analyse heuristique locale du texte OCR ML Kit en attendant l'enrichissement structuré.
     * Marque systématiquement les champs ambigus comme incertains.
     */
    fun parseLocalOcrText(rawText: String): LocalOcrExtraction {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val uncertain = mutableListOf<UncertainItem>()
        val resources = mutableListOf<ResourceRecord>()
        val heroes = mutableListOf<HeroRecord>()
        var power: PowerMetric? = null

        val powerRegex = Regex("""(?:puissance|power|cp)\s*[:\-]?\s*([0-9][0-9\s.,kmbKMB]+)""", RegexOption.IGNORE_CASE)
        val levelRegex = Regex("""(?:niv\.?|lv\.?|level)\s*([0-9]+)""", RegexOption.IGNORE_CASE)

        for (line in lines) {
            val powerMatch = powerRegex.find(line)
            if (powerMatch != null && power == null) {
                val valueText = powerMatch.groupValues[1].trim()
                val numeric = valueText.replace(Regex("""[^\d]"""), "").toLongOrNull()
                val isAmbiguous = valueText.contains("k", true) || valueText.contains("m", true) || numeric == null
                power = PowerMetric(
                    puissanceTotaleTexte = valueText,
                    puissanceTotaleNumerique = numeric,
                    incertain = isAmbiguous,
                    raisonIncertitude = if (isAmbiguous) "Abréviation ou séparateur OCR à confirmer" else null
                )
                if (isAmbiguous) {
                    uncertain.add(
                        UncertainItem(
                            champ = "Puissance totale",
                            valeurObservee = valueText,
                            raison = "Format abrégé ou lecture OCR partielle"
                        )
                    )
                }
            }

            val lvlMatch = levelRegex.find(line)
            if (lvlMatch != null) {
                val candidateName = line.substring(0, lvlMatch.range.first).trim().ifEmpty { "Héros détecté" }
                val lvl = lvlMatch.groupValues[1].toIntOrNull()
                heroes.add(
                    HeroRecord(
                        nom = candidateName,
                        niveau = lvl,
                        niveauTexte = "Niv. ${lvl ?: "?"}",
                        incertain = true,
                        raisonIncertitude = "Nom et compétences extraits par OCR brut ; vérification conseillée"
                    )
                )
            }
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

        return LocalOcrExtraction(
            rawText = rawText,
            power = power,
            resources = resources,
            heroes = heroes,
            uncertainItems = uncertain
        )
    }
}
