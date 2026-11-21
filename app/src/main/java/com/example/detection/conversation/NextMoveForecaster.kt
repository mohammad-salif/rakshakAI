package com.example.detection.conversation

import com.example.detection.rules.StageRule
import com.example.domain.models.NextMove
import com.example.domain.models.NextMoveLikelihood

object NextMoveForecaster {

    fun forecast(
        currentStage: Int,
        stages: List<StageRule>,
        isHindiOutput: Boolean
    ): List<NextMove> {
        if (currentStage <= 0) return emptyList()
        val rule = stages.firstOrNull { it.stage == currentStage } ?: return emptyList()
        return rule.nextMoves.take(2).map { move ->
            val likelihood = when (move.likelihood.uppercase()) {
                "LIKELY" -> NextMoveLikelihood.LIKELY
                "POSSIBLE" -> NextMoveLikelihood.POSSIBLE
                else -> NextMoveLikelihood.LESS_LIKELY
            }
            NextMove(
                stage = move.stage,
                title = if (isHindiOutput) move.titleHi else move.titleEn,
                likelihood = likelihood,
                advice = if (isHindiOutput) move.adviceHi else move.adviceEn
            )
        }
    }

    fun stageName(stage: Int, stages: List<StageRule>, isHindiOutput: Boolean): String {
        val rule = stages.firstOrNull { it.stage == stage }
        return if (rule != null) {
            if (isHindiOutput) rule.nameHi else rule.nameEn
        } else {
            if (isHindiOutput) "कोई चरण नहीं" else "None"
        }
    }
}
