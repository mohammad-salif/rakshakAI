package com.example.detection.risk

import com.example.detection.intent.DetectedIntent
import com.example.detection.rules.PhraseMatcher
import com.example.domain.models.Confidence
import com.example.domain.models.RiskLevel
import com.example.domain.models.ScoreComponents
import com.example.domain.models.UrlResult
import kotlin.math.roundToInt

data class RiskFusionConfig(
    val weightM: Double = 0.35,
    val weightS: Double = 0.20,
    val weightU: Double = 0.20,
    val weightP: Double = 0.15,
    val weightC: Double = 0.10,
    val mediumThreshold: Int = 30,
    val highThreshold: Int = 60,
    val criticalThreshold: Int = 80
)

data class RiskFusionOutput(
    val score: Int,
    val level: RiskLevel,
    val uncertain: Boolean,
    val confidence: Confidence,
    val components: ScoreComponents
)

object RiskFusionEngine {

    fun fuse(
        normalizedLower: String,
        textLength: Int,
        categoryId: String?,
        categoryScoreM: Int,
        manipulationScoreS: Int,
        tactics: List<String>,
        intents: List<DetectedIntent>,
        sensitiveInfoScoreP: Int,
        urlResults: List<UrlResult>,
        upiRiskScore: Int,
        stagesReached: List<Int>,
        unexpectedSenderHint: Boolean,
        unsupportedScript: Boolean,
        legitMarkers: List<String>,
        config: RiskFusionConfig = RiskFusionConfig()
    ): RiskFusionOutput {
        val maxUrlU = maxOf(urlResults.maxOfOrNull { it.score } ?: 0, upiRiskScore).coerceIn(0, 100)

        // Context component C (distinct intents, escalation stages, short text, unexpected sender)
        val contextRaw = (
            intents.size * 22 +
                stagesReached.size * 12 +
                (if (unexpectedSenderHint) 20 else 0) +
                (if (textLength in 15..55 && intents.isNotEmpty()) 15 else 0)
            ).coerceIn(0, 100)

        val weightedRaw = (
            config.weightM * categoryScoreM +
                config.weightS * manipulationScoreS +
                config.weightU * maxUrlU +
                config.weightP * sensitiveInfoScoreP +
                config.weightC * contextRaw
            ).roundToInt().coerceIn(0, 100)

        var score = weightedRaw
        val overrides = mutableListOf<String>()
        val dampeners = mutableListOf<String>()

        val hasUrgencyOrFear = "urgency" in tactics || "fear" in tactics
        val hasAuthority = "authority" in tactics
        val hasIsolation = "isolation" in tactics
        val hasGreed = "greed" in tactics

        // Floor Overrides (combination rules from §5.5)
        if (DetectedIntent.OTP in intents && (hasUrgencyOrFear || categoryId == "otp_theft" || intents.size >= 1 && normalizedLower.contains("verify"))) {
            val floor = if (hasUrgencyOrFear) 74 else 66
            if (score < floor) score = floor
            overrides.add("OTP/PIN share request combined with threat/urgency or verification demand (floor >= $floor)")
        }

        val hasBrandOrLookalikeMismatch = urlResults.any { it.isBrandMismatch || it.score >= 60 }
        val hasLoginOrPaymentKeywords = DetectedIntent.MONEY in intents ||
            DetectedIntent.PERSONAL_INFO in intents ||
            DetectedIntent.LINK_CLICK in intents ||
            urlResults.any { it.flags.any { f -> f.contains("lure keywords", true) } }
        if (hasBrandOrLookalikeMismatch && hasLoginOrPaymentKeywords) {
            if (score < 68) score = 68
            overrides.add("Brand/lookalike URL signal combined with login, KYC, or payment lure (floor >= 68)")
        }

        if (DetectedIntent.APP_INSTALL in intents &&
            (categoryId == "remote_access" || normalizedLower.contains("code") || hasGreed || hasUrgencyOrFear)
        ) {
            if (score < 82) score = 82
            overrides.add("Remote-access / app-install request with support, refund, or code lure (floor >= 82)")
        }

        if (hasAuthority && hasIsolation && (DetectedIntent.MONEY in intents || normalizedLower.contains("safe account"))) {
            if (score < 88) score = 88
            overrides.add("Authority claim + isolation secrecy + payment/safe-account demand (floor >= 88)")
        }

        if ((hasGreed || categoryId == "lottery_prize") &&
            (DetectedIntent.MONEY in intents || normalizedLower.contains("fee") || normalizedLower.contains("deposit") || normalizedLower.contains("फीस"))
        ) {
            val floor = if (categoryId == "lottery_prize") 72 else 66
            if (score < floor) score = floor
            overrides.add("Prize/reward/high-return lure with upfront fee or deposit request (floor >= $floor)")
        }

        // Additional category + tactic combination overrides for unseen paraphrases & shortener scams
        if (categoryId == "utility_disconnection" && hasUrgencyOrFear &&
            (DetectedIntent.MONEY in intents || DetectedIntent.LINK_CLICK in intents)
        ) {
            if (score < 68) score = 68
            overrides.add("Utility disconnection threat paired with payment/link demand (floor >= 68)")
        }

        if (categoryId == "job_fraud" && DetectedIntent.MONEY in intents) {
            if (score < 66) score = 66
            overrides.add("Job offer paired with upfront registration/UPI payment (floor >= 66)")
        }

        if (categoryId == "investment_fraud" && hasGreed && DetectedIntent.MONEY in intents) {
            if (score < 62) score = 62
            overrides.add("Guaranteed/multiplied returns paired with deposit demand (floor >= 62)")
        }

        if (categoryId == "delivery_fraud" && DetectedIntent.MONEY in intents && maxUrlU >= 30) {
            if (score < 66) score = 66
            overrides.add("Parcel release fee paired with suspicious URL (floor >= 66)")
        }

        if (urlResults.any { it.isShortener } && hasUrgencyOrFear &&
            (DetectedIntent.PERSONAL_INFO in intents || DetectedIntent.LINK_CLICK in intents || DetectedIntent.MONEY in intents)
        ) {
            if (score < 68) score = 68
            overrides.add("Shortened URL paired with urgency/fear and action demand (floor >= 68)")
        }

        if (upiRiskScore >= 60) {
            if (score < upiRiskScore) score = upiRiskScore
            overrides.add("High-risk UPI QR payment trap indicators (floor >= $upiRiskScore)")
        }

        // Legit-message dampeners (§5.5) - only apply when no malicious URL/remote-app/isolation/OTP-share demand exists
        val hasDangerousUrl = urlResults.any { !it.isOfficialMatch && it.score >= 30 }
        val hasHardRedFlag = hasIsolation ||
            DetectedIntent.APP_INSTALL in intents ||
            (DetectedIntent.OTP in intents) ||
            hasDangerousUrl ||
            upiRiskScore >= 60

        val matchedLegitMarker = legitMarkers.any { marker ->
            val m = marker.lowercase()
            PhraseMatcher.findBoundaryIndex(normalizedLower, m) >= 0 || normalizedLower.contains(m)
        }

        val hasShareOtpNegation = normalizedLower.contains("do not share") ||
            normalizedLower.contains("never asks for otp") ||
            normalizedLower.contains("साझा न करें") ||
            normalizedLower.contains("कभी ओटीपी नहीं")

        if (!hasHardRedFlag) {
            // 1. Standard bank OTP template ("... is your OTP ... Do not share with anyone")
            if (hasShareOtpNegation && urlResults.isEmpty()) {
                if (score > 15) score = 12
                dampeners.add("Standard bank 'do not share OTP' safety template with no link (capped <= 15)")
            } else if (matchedLegitMarker && urlResults.isEmpty()) {
                // 2. Purely informational / official app reminder
                if (score > 20) score = 15
                dampeners.add("Informational / official-channel reminder with no link or credential ask (capped <= 20)")
            } else if (urlResults.isNotEmpty() && urlResults.all { it.isOfficialMatch } &&
                intents.none { it in setOf(DetectedIntent.OTP, DetectedIntent.APP_INSTALL) } &&
                !hasUrgencyOrFear
            ) {
                // 3. Exact official domain with no request for secrets
                if (score > 25) score = 18
                dampeners.add("Contains only known official domain with no secret request (capped <= 25)")
            } else if (tactics == listOf("urgency") && intents.isEmpty() && urlResults.isEmpty()) {
                // 4. Urgency alone is never treated as proof
                if (score > 25) score = 20
                dampeners.add("Urgency phrasing alone without threat, link, or sensitive request (capped <= 25)")
            }
        }

        val finalScore = score.coerceIn(0, 100)
        val level = when {
            finalScore >= config.criticalThreshold -> RiskLevel.CRITICAL
            finalScore >= config.highThreshold -> RiskLevel.HIGH
            finalScore >= config.mediumThreshold -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        // Count independent indicator families
        val indicatorFamilies = listOf(
            categoryId != null,
            tactics.isNotEmpty(),
            intents.isNotEmpty(),
            urlResults.any { !it.isOfficialMatch && it.score >= 25 } || upiRiskScore >= 40
        ).count { it }

        val onlyShortenerUncertain = urlResults.isNotEmpty() &&
            urlResults.all { it.isShortener || it.isUncertain } &&
            tactics.isEmpty() &&
            intents.none { it != DetectedIntent.LINK_CLICK }

        val uncertain = unsupportedScript ||
            (textLength < 25 && overrides.isEmpty() && indicatorFamilies <= 1) ||
            onlyShortenerUncertain ||
            (finalScore in config.mediumThreshold until config.highThreshold && indicatorFamilies <= 1)

        val confidence = when {
            indicatorFamilies >= 3 || (overrides.isNotEmpty() && indicatorFamilies >= 2) -> Confidence.HIGH
            indicatorFamilies == 2 || dampeners.isNotEmpty() -> Confidence.MEDIUM
            else -> Confidence.LOW
        }

        return RiskFusionOutput(
            score = finalScore,
            level = level,
            uncertain = uncertain,
            confidence = confidence,
            components = ScoreComponents(
                messageCategoryScore = categoryScoreM,
                manipulationScore = manipulationScoreS,
                urlRiskScore = maxUrlU,
                sensitiveInfoScore = sensitiveInfoScoreP,
                contextScore = contextRaw,
                weightedRawScore = weightedRaw,
                overridesApplied = overrides,
                dampenersApplied = dampeners
            )
        )
    }
}
