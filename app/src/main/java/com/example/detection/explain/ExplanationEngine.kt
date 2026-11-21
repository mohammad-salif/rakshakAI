package com.example.detection.explain

import com.example.detection.intent.DetectedIntent
import com.example.detection.rules.ExplanationsFile
import com.example.domain.models.RiskLevel
import com.example.domain.models.UpiAnalysis
import com.example.domain.models.UrlResult

data class BuiltExplanation(
    val standardVerdict: String,
    val elderlyVerdict: String,
    val whatCouldHappen: String,
    val detailedWhy: String
)

object ExplanationEngine {

    fun build(
        level: RiskLevel,
        uncertain: Boolean,
        categoryId: String?,
        intents: List<DetectedIntent>,
        tactics: List<String>,
        urlResults: List<UrlResult>,
        upiAnalysis: UpiAnalysis?,
        templates: ExplanationsFile,
        isHindiOutput: Boolean
    ): BuiltExplanation {
        val verdictKey = if (uncertain && level != RiskLevel.CRITICAL && level != RiskLevel.HIGH) {
            "UNCERTAIN"
        } else {
            level.name
        }

        val standardVerdict = templates.verdicts[verdictKey]
            ?: templates.verdicts["MEDIUM"]
            ?: "Verify independently before acting."

        val elderlyVerdict = templates.elderlyVerdicts[verdictKey]
            ?: templates.elderlyVerdicts["MEDIUM"]
            ?: standardVerdict

        val parts = mutableListOf<String>()
        if (upiAnalysis != null && upiAnalysis.warnings.isNotEmpty()) {
            parts.add(upiAnalysis.warnings.first())
        }
        val riskyUrl = urlResults.firstOrNull { it.score >= 30 }
        if (riskyUrl != null) {
            parts.add(riskyUrl.summary)
        }
        if (tactics.isNotEmpty()) {
            val tacticNotes = tactics.take(3).mapNotNull { templates.manipulationDescriptions[it] }
            parts.addAll(tacticNotes)
        }
        if (intents.isNotEmpty()) {
            val intentNotes = intents.take(2).mapNotNull { templates.intentLabels[it.code] }
            if (intentNotes.isNotEmpty()) {
                val prefix = if (isHindiOutput) "संदेश में यह मांग की गई है: " else "The message pushes you to: "
                parts.add(prefix + intentNotes.joinToString("; ") + ".")
            }
        }

        val detailedWhy = if (parts.isEmpty()) {
            standardVerdict
        } else {
            parts.distinct().joinToString(" ")
        }

        val consequence = (categoryId?.let { templates.categoryConsequences[it] }
            ?: when {
                upiAnalysis != null && upiAnalysis.riskScore >= 50 -> templates.categoryConsequences["payment_refund"]
                riskyUrl != null -> templates.categoryConsequences["kyc_fraud"]
                else -> templates.categoryConsequences["other_suspicious"]
            })
            ?: standardVerdict

        return BuiltExplanation(
            standardVerdict = standardVerdict,
            elderlyVerdict = elderlyVerdict,
            whatCouldHappen = consequence,
            detailedWhy = detailedWhy
        )
    }
}
