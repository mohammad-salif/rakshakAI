package com.example.domain.models

import kotlinx.serialization.Serializable

@Serializable
enum class Source {
    MANUAL,
    SHARED,
    SMS,
    SCREENSHOT,
    TRANSCRIPT,
    QR
}

@Serializable
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

@Serializable
enum class Confidence {
    LOW,
    MEDIUM,
    HIGH
}

@Serializable
enum class NextMoveLikelihood {
    LIKELY,
    POSSIBLE,
    LESS_LIKELY
}

@Serializable
data class ScanInput(
    val text: String,
    val source: Source = Source.MANUAL,
    val languageHint: String = "auto",
    val claimedBrandHint: String? = null,
    val unexpectedSenderHint: Boolean = false
)

@Serializable
data class Evidence(
    val type: String,
    val phrase: String? = null,
    val start: Int? = null,
    val end: Int? = null,
    val description: String,
    val observed: Boolean = true,
    val weight: Int = 1
)

@Serializable
data class NextMove(
    val stage: Int,
    val title: String,
    val likelihood: NextMoveLikelihood,
    val advice: String
)

@Serializable
data class UrlResult(
    val rawUrl: String,
    val normalizedHost: String,
    val registeredDomain: String,
    val score: Int,
    val isShortener: Boolean = false,
    val isOfficialMatch: Boolean = false,
    val matchedBrandId: String? = null,
    val matchedBrandName: String? = null,
    val isBrandMismatch: Boolean = false,
    val isUncertain: Boolean = false,
    val flags: List<String> = emptyList(),
    val summary: String = ""
)

@Serializable
data class UpiAnalysis(
    val rawUri: String,
    val payeeAddress: String = "",
    val payeeName: String = "",
    val amount: String = "",
    val note: String = "",
    val currency: String = "INR",
    val riskScore: Int = 0,
    val warnings: List<String> = emptyList()
)

@Serializable
data class ScoreComponents(
    val messageCategoryScore: Int = 0,
    val manipulationScore: Int = 0,
    val urlRiskScore: Int = 0,
    val sensitiveInfoScore: Int = 0,
    val contextScore: Int = 0,
    val weightedRawScore: Int = 0,
    val overridesApplied: List<String> = emptyList(),
    val dampenersApplied: List<String> = emptyList()
)

@Serializable
data class ScanResult(
    val scanId: String,
    val timestamp: Long,
    val sourceText: String = "",
    val source: Source = Source.MANUAL,
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val uncertain: Boolean,
    val confidence: Confidence,
    val language: String,
    val scamCategory: String? = null,
    val scamCategoryId: String? = null,
    val intents: List<String> = emptyList(),
    val requestedAction: String = "",
    val manipulation: List<String> = emptyList(),
    val stagesReached: List<Int> = emptyList(),
    val currentStage: Int = 0,
    val nextMoves: List<NextMove> = emptyList(),
    val urlResults: List<UrlResult> = emptyList(),
    val upiAnalysis: UpiAnalysis? = null,
    val evidence: List<Evidence> = emptyList(),
    val components: ScoreComponents = ScoreComponents(),
    val explanation: String,
    val elderlyExplanation: String = "",
    val whatCouldHappen: String,
    val recommendations: List<String> = emptyList(),
    val wasTruncated: Boolean = false,
    val unsupportedLanguageWarning: Boolean = false,
    val detectorAvailable: Boolean = true,
    val detectorError: String? = null,
    val processingTimeMs: Long = 0L,
    val processedOffline: Boolean = true
)

@Serializable
enum class ChatSender {
    THEM,
    ME
}

@Serializable
data class ConversationMessage(
    val id: String,
    val sender: ChatSender,
    val senderLabel: String,
    val text: String,
    val timestampLabel: String? = null
)

@Serializable
data class ConversationTurnAnalysis(
    val message: ConversationMessage,
    val scanResult: ScanResult,
    val primaryStage: Int,
    val stageName: String
)

@Serializable
data class ConversationAnalysisResult(
    val conversationId: String,
    val turns: List<ConversationTurnAnalysis>,
    val cumulativeScore: Int,
    val riskLevel: RiskLevel,
    val uncertain: Boolean,
    val confidence: Confidence,
    val stagesReached: List<Int>,
    val currentStage: Int,
    val nextMoves: List<NextMove>,
    val manipulationSummary: List<String>,
    val intentSummary: List<String>,
    val isolationTriggered: Boolean,
    val coercionAssessment: CoercionAssessment,
    val combinedResult: ScanResult
)

@Serializable
enum class PreActionType {
    OPEN_LINK,
    PAY_UPI,
    INSTALL_APP,
    SHARE_OTP_PIN,
    SHARE_ID_DOC,
    TRANSFER_MONEY
}

@Serializable
enum class AmountRange {
    NONE,
    UNDER_1000,
    FROM_1000_TO_10000,
    ABOVE_10000
}

@Serializable
data class PreActionInput(
    val actionType: PreActionType,
    val knowsPayeePersonally: Boolean,
    val toldToHurry: Boolean,
    val toldNotToTellAnyone: Boolean,
    val startedUnexpectedly: Boolean,
    val amountRange: AmountRange = AmountRange.NONE,
    val latestScanScore: Int = 0
)

@Serializable
enum class PreActionVerdict {
    PROCEED_CAREFULLY,
    PAUSE_AND_VERIFY,
    STOP_DO_NOT_PROCEED
}

@Serializable
data class PreActionDecision(
    val verdict: PreActionVerdict,
    val combinedRiskScore: Int,
    val requiresForcedPause: Boolean,
    val pauseSeconds: Int,
    val headline: String,
    val reasons: List<String>,
    val verificationSteps: List<String>
)

@Serializable
data class CoercionAnswers(
    val toldToStayOnCall: Boolean = false,
    val toldNotToTellAnyone: Boolean = false,
    val givenDeadline: Boolean = false,
    val threatenedWithArrestOrBlock: Boolean = false
)

@Serializable
data class CoercionAssessment(
    val pressureScore: Int,
    val levelLabel: String,
    val contributingFactors: List<String>,
    val disclaimer: String = "Pressure-risk estimate — not a psychological diagnosis."
)

@Serializable
data class CrossSignalInput(
    val messageRisk: Int = 0,
    val conversationRisk: Int = 0,
    val linkRisk: Int = 0,
    val callTranscriptRisk: Int = 0,
    val installedRemoteApps: List<String> = emptyList(),
    val simulatedRemoteAppPrompt: Boolean = false,
    val simulatedScreenShareOn: Boolean = false,
    val preActionRisk: Int = 0
)

@Serializable
data class CrossSignalContribution(
    val signalName: String,
    val score: Int,
    val weightLabel: String,
    val detail: String,
    val isSimulated: Boolean = false
)

@Serializable
data class CrossSignalReport(
    val overallScore: Int,
    val riskLevel: RiskLevel,
    val headline: String,
    val contributions: List<CrossSignalContribution>,
    val shouldTriggerIsolationBreaker: Boolean,
    val remoteAccessWarning: String? = null
)

@Serializable
enum class FeedbackOption {
    CORRECT_SCAM,
    FALSE_ALARM,
    MISSED_SOMETHING,
    NOT_SURE
}
