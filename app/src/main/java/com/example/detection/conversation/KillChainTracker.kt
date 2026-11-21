package com.example.detection.conversation

import com.example.detection.intent.DetectedIntent

data class KillChainMapping(
    val stagesReached: List<Int>,
    val currentStage: Int
)

object KillChainTracker {

    fun mapStages(
        categoryId: String?,
        tactics: List<String>,
        intents: List<DetectedIntent>,
        hasUrls: Boolean,
        hasUrgencyOrFear: Boolean,
        postPaymentKeywordsPresent: Boolean = false
    ): KillChainMapping {
        val stages = LinkedHashSet<Int>()

        val hasSignals = tactics.isNotEmpty() || intents.isNotEmpty() || categoryId != null || hasUrls
        if (!hasSignals) {
            return KillChainMapping(emptyList(), 0)
        }

        // S1: Hook / Contact
        if ("greed" in tactics || categoryId != null || tactics.isNotEmpty() || hasUrls) {
            stages.add(1)
        }

        // S2: Authority / Trust
        if ("authority" in tactics || "false_trust" in tactics) {
            stages.add(2)
        }

        // S3: Fear / Urgency
        if ("fear" in tactics || "urgency" in tactics || hasUrgencyOrFear) {
            stages.add(3)
        }

        // S4: Isolation
        if ("isolation" in tactics) {
            stages.add(4)
        }

        // S5: Action request (link / app / OTP / ID / call back)
        if (hasUrls || intents.any {
                it in setOf(
                    DetectedIntent.LINK_CLICK,
                    DetectedIntent.APP_INSTALL,
                    DetectedIntent.OTP,
                    DetectedIntent.PERSONAL_INFO,
                    DetectedIntent.CALL_BACK
                )
            }
        ) {
            stages.add(5)
        }

        // S6: Payment demand
        if (DetectedIntent.MONEY in intents) {
            stages.add(6)
        }

        // S7: Post-payment escalation
        if (postPaymentKeywordsPresent) {
            stages.add(7)
        }

        val sorted = stages.sorted()
        return KillChainMapping(
            stagesReached = sorted,
            currentStage = sorted.maxOrNull() ?: 0
        )
    }
}
