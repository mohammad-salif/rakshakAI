package com.example.detection.risk

import com.example.domain.models.CoercionAnswers
import com.example.domain.models.CoercionAssessment

object CoercionPressureEstimator {

    fun estimate(
        stagesReached: List<Int>,
        tactics: List<String>,
        answers: CoercionAnswers = CoercionAnswers(),
        isHindiOutput: Boolean = false
    ): CoercionAssessment {
        var score = 0
        val factors = mutableListOf<String>()

        if (stagesReached.size >= 2) {
            val stagePts = (stagesReached.size * 9).coerceAtMost(35)
            score += stagePts
            factors.add(
                if (isHindiOutput) "बातचीत में ${stagesReached.size} जोखिम चरण देखे गए (+$stagePts)"
                else "Observed ${stagesReached.size} kill-chain escalation stages (+$stagePts)"
            )
        }

        val tacticPts = (tactics.size * 8).coerceAtMost(25)
        if (tacticPts > 0) {
            score += tacticPts
            factors.add(
                if (isHindiOutput) "दबाव की रणनीतियाँ मिलीं: ${tactics.joinToString(", ")} (+$tacticPts)"
                else "Pressure tactics detected: ${tactics.joinToString(", ")} (+$tacticPts)"
            )
        }

        if ("isolation" in tactics || answers.toldNotToTellAnyone) {
            score += 22
            factors.add(
                if (isHindiOutput) "गोपनीयता / परिवार को न बताने का दबाव (+22)"
                else "Secrecy or isolation pressure ('do not tell anyone') (+22)"
            )
        }

        if (answers.toldToStayOnCall) {
            score += 18
            factors.add(
                if (isHindiOutput) "कॉल पर बने रहने का दबाव (+18)"
                else "User reported being told to stay on the call (+18)"
            )
        }

        if (answers.givenDeadline) {
            score += 12
            factors.add(
                if (isHindiOutput) "कड़ी समय सीमा दी गई (+12)"
                else "User reported a strict urgent deadline (+12)"
            )
        }

        if (answers.threatenedWithArrestOrBlock) {
            score += 18
            factors.add(
                if (isHindiOutput) "गिरफ्तारी या खाता बंद करने की धमकी (+18)"
                else "User reported threats of arrest or account blocking (+18)"
            )
        }

        val finalScore = score.coerceIn(0, 100)
        val levelLabel = when {
            finalScore >= 75 -> if (isHindiOutput) "अत्यधिक दबाव जोखिम" else "High Pressure Risk"
            finalScore >= 45 -> if (isHindiOutput) "मध्यम दबाव जोखिम" else "Elevated Pressure Risk"
            finalScore >= 20 -> if (isHindiOutput) "हल्का दबाव संकेत" else "Mild Pressure Signals"
            else -> if (isHindiOutput) "कम दबाव संकेत" else "Low Pressure Signals"
        }

        val disclaimer = if (isHindiOutput) {
            "यह केवल दबाव-जोखिम अनुमान है — कोई मनोवैज्ञानिक निदान नहीं।"
        } else {
            "Pressure-risk estimate — not a psychological diagnosis."
        }

        return CoercionAssessment(
            pressureScore = finalScore,
            levelLabel = levelLabel,
            contributingFactors = factors,
            disclaimer = disclaimer
        )
    }
}
