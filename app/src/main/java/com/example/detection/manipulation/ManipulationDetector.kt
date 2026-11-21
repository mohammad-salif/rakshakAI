package com.example.detection.manipulation

import com.example.detection.engine.PreprocessedText
import com.example.detection.rules.LexiconFile
import com.example.detection.rules.PhraseHit
import com.example.detection.rules.PhraseMatcher
import com.example.domain.models.Evidence

data class ManipulationDetectionResult(
    val tactics: List<String>,
    val manipulationScore: Int,
    val hits: List<PhraseHit>,
    val evidence: List<Evidence>
)

object ManipulationDetector {

    private val manipulationGroups = setOf(
        "fear",
        "urgency",
        "authority",
        "isolation",
        "greed",
        "false_trust"
    )

    fun detect(
        preprocessed: PreprocessedText,
        lexicons: List<LexiconFile>,
        isHindiOutput: Boolean
    ): ManipulationDetectionResult {
        val hits = PhraseMatcher.matchGroups(
            preprocessed = preprocessed,
            lexicons = lexicons,
            groupNames = manipulationGroups
        )

        val byGroup = hits.groupBy { it.group }
        val orderedTactics = listOf("isolation", "fear", "authority", "urgency", "greed", "false_trust")
            .filter { byGroup.containsKey(it) }

        val totalWeight = hits.sumOf { it.weight }
        val diversityBonus = if (orderedTactics.size >= 2) orderedTactics.size * 8 else 0
        val isolationBonus = if ("isolation" in orderedTactics) 15 else 0
        val score = ((totalWeight * 5) + diversityBonus + isolationBonus).coerceIn(0, 100)

        val evidence = hits.take(10).map { hit ->
            val desc = when (hit.group) {
                "fear" -> if (isHindiOutput) "डर या खाते/गिरफ्तारी की धमकी ('${hit.matchedText}')" else "Fear or threat trigger ('${hit.matchedText}')"
                "urgency" -> if (isHindiOutput) "तुरंत कार्रवाई का दबाव ('${hit.matchedText}')" else "Urgency / artificial deadline ('${hit.matchedText}')"
                "authority" -> if (isHindiOutput) "बैंक या सरकारी अधिकारी होने का दावा ('${hit.matchedText}')" else "Authority or official claim ('${hit.matchedText}')"
                "isolation" -> if (isHindiOutput) "बात गुप्त रखने या कॉल न काटने का दबाव ('${hit.matchedText}')" else "Secrecy or isolation pressure ('${hit.matchedText}')"
                "greed" -> if (isHindiOutput) "इनाम या बड़े मुनाफे का लालच ('${hit.matchedText}')" else "Reward, prize, or guaranteed-profit lure ('${hit.matchedText}')"
                else -> if (isHindiOutput) "झूठा भरोसा दिलाने वाला वाक्य ('${hit.matchedText}')" else "False-trust reassurance ('${hit.matchedText}')"
            }
            Evidence(
                type = "TACTIC:${hit.group}",
                phrase = hit.matchedText,
                start = hit.start,
                end = hit.end,
                description = desc,
                observed = !hit.fuzzyMatched,
                weight = hit.weight
            )
        }

        return ManipulationDetectionResult(
            tactics = orderedTactics,
            manipulationScore = score,
            hits = hits,
            evidence = evidence
        )
    }
}
