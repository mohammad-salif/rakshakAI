package com.example.detection.engine

data class LanguageDetectionResult(
    val code: String,          // "en", "hi", "hinglish", "mixed", "unsupported"
    val displayName: String,
    val unsupportedScriptWarning: Boolean
)

object LanguageDetector {

    fun detect(
        normalizedText: String,
        hinglishMarkers: List<String>,
        manualHint: String = "auto"
    ): LanguageDetectionResult {
        when (manualHint.lowercase()) {
            "en" -> return LanguageDetectionResult("en", "English", false)
            "hi" -> return LanguageDetectionResult("hi", "Hindi (हिन्दी)", false)
            "hinglish" -> return LanguageDetectionResult("hinglish", "Hinglish", false)
        }

        var devanagariCount = 0
        var latinCount = 0
        var otherScriptCount = 0

        for (ch in normalizedText) {
            if (!ch.isLetter()) continue
            when {
                ch in '\u0900'..'\u097F' -> devanagariCount++
                ch in 'a'..'z' || ch in 'A'..'Z' -> latinCount++
                else -> otherScriptCount++
            }
        }

        val totalLetters = devanagariCount + latinCount + otherScriptCount
        if (totalLetters == 0) {
            return LanguageDetectionResult("en", "English / Symbols", false)
        }

        val otherRatio = otherScriptCount.toFloat() / totalLetters
        if (otherRatio > 0.35f) {
            return LanguageDetectionResult(
                code = "unsupported",
                displayName = "Unsupported / Other Script",
                unsupportedScriptWarning = true
            )
        }

        val devRatio = devanagariCount.toFloat() / totalLetters
        val latinRatio = latinCount.toFloat() / totalLetters

        if (devRatio >= 0.45f) {
            return if (latinCount >= 12 && latinRatio >= 0.20f) {
                LanguageDetectionResult("mixed", "Hindi-English Mixed", false)
            } else {
                LanguageDetectionResult("hi", "Hindi (हिन्दी)", false)
            }
        }

        if (devanagariCount >= 6 && latinCount >= 10) {
            return LanguageDetectionResult("mixed", "Hindi-English Mixed", false)
        }

        val tokens = normalizedText.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }.toSet()
        val markerSet = hinglishMarkers.map { it.lowercase() }.toSet()
        val hinglishHits = tokens.count { it in markerSet }

        return if (hinglishHits >= 2) {
            LanguageDetectionResult("hinglish", "Hinglish", false)
        } else {
            LanguageDetectionResult("en", "English", false)
        }
    }
}
