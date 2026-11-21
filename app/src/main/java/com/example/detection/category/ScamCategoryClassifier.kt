package com.example.detection.category

import com.example.detection.engine.PreprocessedText
import com.example.detection.rules.CategoryRule
import com.example.detection.rules.PhraseMatcher
import com.example.domain.models.Evidence

data class CategoryClassificationResult(
    val categoryId: String?,
    val categoryDisplayName: String?,
    val categoryScore: Int,
    val matchedGroupsCount: Int,
    val evidence: List<Evidence>
)

object ScamCategoryClassifier {

    fun classify(
        preprocessed: PreprocessedText,
        rules: List<CategoryRule>,
        intentHitsCount: Int,
        tacticHitsCount: Int,
        maxUrlScore: Int,
        isHindiOutput: Boolean
    ): CategoryClassificationResult {
        val textNorm = preprocessed.normalizedLower
        val textMatch = preprocessed.matchingText
        if (textNorm.isBlank()) {
            return CategoryClassificationResult(null, null, 0, 0, emptyList())
        }

        var bestRule: CategoryRule? = null
        var bestScore = 0
        var bestMatchedGroups = 0
        var bestEvidencePhrases = emptyList<String>()

        for (rule in rules) {
            var groupsMatched = 0
            val matchedPhrases = mutableListOf<String>()

            for (group in rule.requiredAnyGroups) {
                val hit = findBestPhrase(textNorm, textMatch, group)
                if (hit != null) {
                    groupsMatched++
                    matchedPhrases.add(hit)
                }
            }

            if (groupsMatched < rule.requiredAnyGroups.size) {
                continue
            }

            var boosterScore = 0
            for (booster in rule.boosters) {
                val b = booster.lowercase()
                if (PhraseMatcher.findBoundaryIndex(textNorm, b) >= 0 ||
                    PhraseMatcher.findBoundaryIndex(textMatch, b) >= 0 ||
                    (!b.all { it in 'a'..'z' } && textNorm.contains(b))
                ) {
                    boosterScore += 8
                    matchedPhrases.add(booster)
                }
            }

            // Combination boost when category pattern + intents + manipulation align
            val comboBoost = (intentHitsCount.coerceAtMost(4) * 6) + (tacticHitsCount.coerceAtMost(4) * 5)
            val urlBoost = if (maxUrlScore >= 40) 15 else 0
            val totalScore = (rule.baseWeight + boosterScore + comboBoost + urlBoost).coerceIn(0, 100)

            // Prefer more specific rules over generic other_suspicious
            val adjustedScore = if (rule.id == "other_suspicious") (totalScore - 15).coerceAtLeast(0) else totalScore

            if (adjustedScore > bestScore) {
                bestScore = adjustedScore
                bestRule = rule
                bestMatchedGroups = groupsMatched
                bestEvidencePhrases = matchedPhrases.distinct()
            }
        }

        if (bestRule == null) {
            // Partial fallback score when tactics + intent exist without a named category match
            val fallbackScore = if (intentHitsCount > 0 && tacticHitsCount > 0) {
                (25 + intentHitsCount * 8 + tacticHitsCount * 6).coerceIn(0, 75)
            } else if (maxUrlScore >= 60) {
                55
            } else {
                0
            }
            return CategoryClassificationResult(
                categoryId = if (fallbackScore >= 30) "other_suspicious" else null,
                categoryDisplayName = if (fallbackScore >= 30) {
                    if (isHindiOutput) "अन्य संदिग्ध गतिविधि" else "Other Suspicious Activity"
                } else {
                    null
                },
                categoryScore = fallbackScore,
                matchedGroupsCount = 0,
                evidence = emptyList()
            )
        }

        val displayName = if (isHindiOutput) bestRule.displayNameHi else bestRule.displayNameEn
        val catEvidence = if (bestEvidencePhrases.isNotEmpty()) {
            listOf(
                Evidence(
                    type = "CATEGORY:${bestRule.id}",
                    phrase = bestEvidencePhrases.take(3).joinToString(", "),
                    start = null,
                    end = null,
                    description = if (isHindiOutput) {
                        "संदेश का पैटर्न '$displayName' से मेल खाता है (${bestEvidencePhrases.take(4).joinToString(", ")})"
                    } else {
                        "Message pattern aligns with $displayName (${bestEvidencePhrases.take(4).joinToString(", ")})"
                    },
                    observed = false,
                    weight = bestRule.baseWeight
                )
            )
        } else {
            emptyList()
        }

        return CategoryClassificationResult(
            categoryId = bestRule.id,
            categoryDisplayName = displayName,
            categoryScore = bestScore,
            matchedGroupsCount = bestMatchedGroups,
            evidence = catEvidence
        )
    }

    private fun findBestPhrase(textNorm: String, textMatch: String, candidates: List<String>): String? {
        for (phrase in candidates) {
            val p = phrase.lowercase()
            if (PhraseMatcher.findBoundaryIndex(textNorm, p) >= 0 ||
                PhraseMatcher.findBoundaryIndex(textMatch, p) >= 0
            ) {
                return phrase
            }
        }
        return null
    }
}
