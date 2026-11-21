package com.example.detection.intent

import com.example.detection.engine.PreprocessedText
import com.example.detection.rules.LexiconFile
import com.example.detection.rules.PhraseHit
import com.example.detection.rules.PhraseMatcher
import com.example.domain.models.Evidence

enum class DetectedIntent(val code: String) {
    OTP("OTP"),
    MONEY("MONEY"),
    PERSONAL_INFO("PERSONAL_INFO"),
    APP_INSTALL("APP_INSTALL"),
    LINK_CLICK("LINK_CLICK"),
    CALL_BACK("CALL_BACK")
}

data class IntentDetectionResult(
    val intents: List<DetectedIntent>,
    val requestedAction: String,
    val sensitiveInfoScore: Int,
    val evidence: List<Evidence>,
    val hits: List<PhraseHit>
)

object IntentDetector {

    private val intentGroups = setOf(
        "intent_otp",
        "intent_money",
        "intent_personal_info",
        "intent_app_install",
        "intent_link_click",
        "intent_callback"
    )

    fun detect(
        preprocessed: PreprocessedText,
        lexicons: List<LexiconFile>,
        hasUrls: Boolean,
        hasUpiUri: Boolean,
        isHindiOutput: Boolean
    ): IntentDetectionResult {
        val hits = PhraseMatcher.matchGroups(
            preprocessed = preprocessed,
            lexicons = lexicons,
            groupNames = intentGroups,
            enableNegationFilterForOtp = true
        )

        val intents = LinkedHashSet<DetectedIntent>()
        var otpWeight = 0
        var infoWeight = 0
        var moneyWeight = 0
        var appWeight = 0

        for (hit in hits) {
            when (hit.group) {
                "intent_otp" -> {
                    intents.add(DetectedIntent.OTP)
                    otpWeight += hit.weight
                }
                "intent_money" -> {
                    intents.add(DetectedIntent.MONEY)
                    moneyWeight += hit.weight
                }
                "intent_personal_info" -> {
                    intents.add(DetectedIntent.PERSONAL_INFO)
                    infoWeight += hit.weight
                }
                "intent_app_install" -> {
                    intents.add(DetectedIntent.APP_INSTALL)
                    appWeight += hit.weight
                }
                "intent_link_click" -> intents.add(DetectedIntent.LINK_CLICK)
                "intent_callback" -> intents.add(DetectedIntent.CALL_BACK)
            }
        }

        if (hasUpiUri) {
            intents.add(DetectedIntent.MONEY)
            moneyWeight += 5
        }
        if (hasUrls && intents.isNotEmpty()) {
            // Link accompanied by any demand implies link click intent
            intents.add(DetectedIntent.LINK_CLICK)
        }

        val sensitiveInfoScore = ((otpWeight * 10) + (infoWeight * 7) + (appWeight * 8) + (moneyWeight * 6))
            .coerceIn(0, 100)

        val evidence = hits.take(10).map { hit ->
            val desc = when (hit.group) {
                "intent_otp" -> if (isHindiOutput) "ओटीपी, पिन या पासवर्ड की मांग (${hit.phrase})" else "Requests sensitive OTP, PIN, or credential (${hit.phrase})"
                "intent_money" -> if (isHindiOutput) "पैसे भेजने या फीस भुगतान की मांग (${hit.phrase})" else "Requests payment, fee, or fund transfer (${hit.phrase})"
                "intent_personal_info" -> if (isHindiOutput) "केवाईसी या पहचान विवरण की मांग (${hit.phrase})" else "Requests KYC, identity, or account details (${hit.phrase})"
                "intent_app_install" -> if (isHindiOutput) "ऐप इंस्टॉल या स्क्रीन शेयरिंग की मांग (${hit.phrase})" else "Asks to install an app or share screen (${hit.phrase})"
                "intent_link_click" -> if (isHindiOutput) "लिंक खोलने या ग्रुप में जुड़ने की मांग (${hit.phrase})" else "Pushes user to open a link or join a group (${hit.phrase})"
                else -> if (isHindiOutput) "वापस कॉल करने की मांग (${hit.phrase})" else "Prompts user to call back or join a call (${hit.phrase})"
            }
            Evidence(
                type = "INTENT:${hit.group}",
                phrase = hit.matchedText,
                start = hit.start,
                end = hit.end,
                description = desc,
                observed = !hit.fuzzyMatched,
                weight = hit.weight
            )
        }

        val requestedAction = buildRequestedActionSummary(intents.toList(), isHindiOutput)

        return IntentDetectionResult(
            intents = intents.toList(),
            requestedAction = requestedAction,
            sensitiveInfoScore = sensitiveInfoScore,
            evidence = evidence,
            hits = hits
        )
    }

    private fun buildRequestedActionSummary(intents: List<DetectedIntent>, isHindi: Boolean): String {
        if (intents.isEmpty()) {
            return if (isHindi) "कोई स्पष्ट संवेदनशील मांग नहीं दिखी" else "No explicit sensitive action requested"
        }
        return intents.joinToString(" • ") { intent ->
            when (intent) {
                DetectedIntent.OTP -> if (isHindi) "ओटीपी / पिन साझा करें" else "Share OTP / PIN"
                DetectedIntent.MONEY -> if (isHindi) "पैसे / फीस भेजें" else "Send money / fee"
                DetectedIntent.PERSONAL_INFO -> if (isHindi) "केवाईसी / आईडी विवरण दें" else "Share KYC / ID details"
                DetectedIntent.APP_INSTALL -> if (isHindi) "ऐप डालें / स्क्रीन दिखाएं" else "Install app / share screen"
                DetectedIntent.LINK_CLICK -> if (isHindi) "लिंक खोलें" else "Open link"
                DetectedIntent.CALL_BACK -> if (isHindi) "कॉल बैक करें" else "Call back"
            }
        }
    }
}
