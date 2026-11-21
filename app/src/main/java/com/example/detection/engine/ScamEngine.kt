package com.example.detection.engine

import com.example.detection.category.ScamCategoryClassifier
import com.example.detection.conversation.ConversationSplitter
import com.example.detection.conversation.KillChainTracker
import com.example.detection.conversation.NextMoveForecaster
import com.example.detection.explain.ExplanationEngine
import com.example.detection.explain.RecommendationEngine
import com.example.detection.intent.DetectedIntent
import com.example.detection.intent.IntentDetector
import com.example.detection.manipulation.ManipulationDetector
import com.example.detection.risk.CoercionPressureEstimator
import com.example.detection.risk.RiskFusionConfig
import com.example.detection.risk.RiskFusionEngine
import com.example.detection.rules.RuleBundle
import com.example.detection.rules.RuleLoadResult
import com.example.detection.rules.RuleLoader
import com.example.detection.rules.RuleSource
import com.example.detection.upi.UpiQrAnalyzer
import com.example.detection.url.BrandImpersonationDetector
import com.example.detection.url.UrlAnalyzer
import com.example.detection.url.UrlExtractor
import com.example.domain.models.AmountRange
import com.example.domain.models.ChatSender
import com.example.domain.models.CoercionAnswers
import com.example.domain.models.Confidence
import com.example.domain.models.ConversationAnalysisResult
import com.example.domain.models.ConversationMessage
import com.example.domain.models.ConversationTurnAnalysis
import com.example.domain.models.Evidence
import com.example.domain.models.PreActionDecision
import com.example.domain.models.PreActionInput
import com.example.domain.models.PreActionType
import com.example.domain.models.PreActionVerdict
import com.example.domain.models.RiskLevel
import com.example.domain.models.ScanInput
import com.example.domain.models.ScanResult
import com.example.domain.models.ScoreComponents
import com.example.domain.models.Source
import com.example.domain.models.UpiAnalysis
import java.util.UUID

/**
 * Pure Kotlin detector abstraction so alternative/ML detectors could plug in later.
 * RakshakAI ships strictly with RuleBasedScamEngine (zero ML models, zero internet).
 */
interface ScamDetector {
    fun isReady(): Boolean
    fun analyze(input: ScanInput, outputLanguage: String = "en"): ScanResult
    fun analyzeUrl(rawUrl: String, claimedBrandId: String? = null, outputLanguage: String = "en"): ScanResult
    fun analyzeQrPayload(payload: String, claimedBrandId: String? = null, outputLanguage: String = "en"): ScanResult
    fun analyzeConversation(
        messages: List<ConversationMessage>,
        coercionAnswers: CoercionAnswers = CoercionAnswers(),
        outputLanguage: String = "en"
    ): ConversationAnalysisResult

    fun splitAndAnalyzeConversation(
        rawTranscript: String,
        coercionAnswers: CoercionAnswers = CoercionAnswers(),
        outputLanguage: String = "en"
    ): ConversationAnalysisResult

    fun evaluatePreAction(input: PreActionInput, isHindi: Boolean = false): PreActionDecision
}

class RuleBasedScamEngine(
    ruleSource: RuleSource,
    private val config: RiskFusionConfig = RiskFusionConfig()
) : ScamDetector {

    private val loadResult: RuleLoadResult = RuleLoader(ruleSource).load()

    override fun isReady(): Boolean = loadResult is RuleLoadResult.Loaded

    fun getRuleBundleOrNull(): RuleBundle? = (loadResult as? RuleLoadResult.Loaded)?.bundle

    override fun analyze(input: ScanInput, outputLanguage: String): ScanResult {
        val startNs = System.nanoTime()
        val isHindiOut = outputLanguage.startsWith("hi", ignoreCase = true)

        val bundle = when (loadResult) {
            is RuleLoadResult.Loaded -> loadResult.bundle
            is RuleLoadResult.Unavailable -> {
                return unavailableResult(input, loadResult.reason, startNs)
            }
        }

        val preprocessed = TextPreprocessor.preprocess(input.text)
        if (preprocessed.originalDisplayText.isBlank()) {
            return emptyOrUnreadableResult(
                input = input,
                preprocessed = preprocessed,
                reason = if (isHindiOut) "जांचने के लिए कृपया कोई संदेश या लिंक दर्ज करें।" else "Please enter or paste readable text to inspect.",
                startNs = startNs
            )
        }

        if (!preprocessed.isReadable) {
            return emptyOrUnreadableResult(
                input = input,
                preprocessed = preprocessed,
                reason = if (isHindiOut) "यह टेक्स्ट पढ़ने योग्य नहीं है। कृपया साफ़ पाठ पेस्ट करें।" else "Input appears unreadable or symbol-only. Please provide readable content.",
                startNs = startNs
            )
        }

        val lang = LanguageDetector.detect(
            normalizedText = preprocessed.normalizedLower,
            hinglishMarkers = bundle.lexiconHinglish.hinglishMarkers,
            manualHint = input.languageHint
        )

        val officialDomains = bundle.brands.brands.flatMap { it.officialDomains }.map { it.lowercase() }.toSet()
        val extracted = UrlExtractor.extract(
            text = preprocessed.originalDisplayText,
            suspiciousTlds = bundle.suspiciousTlds.suspiciousTlds.toSet(),
            shorteners = bundle.suspiciousTlds.shorteners.toSet(),
            officialDomains = officialDomains
        )

        val brandDetector = BrandImpersonationDetector(bundle.brands.brands)
        val claimedBrands = buildList {
            addAll(brandDetector.detectClaimedBrands(preprocessed.normalizedLower))
            if (!input.claimedBrandHint.isNullOrBlank()) {
                bundle.brands.brands.firstOrNull { it.id == input.claimedBrandHint }?.let { add(it) }
            }
        }.distinctBy { it.id }

        val urlAnalyzer = UrlAnalyzer(
            brands = bundle.brands.brands,
            suspiciousTlds = bundle.suspiciousTlds.suspiciousTlds.toSet(),
            shorteners = bundle.suspiciousTlds.shorteners.toSet(),
            suspiciousPathKeywords = bundle.suspiciousTlds.suspiciousPathKeywords.toSet(),
            twoLevelSuffixes = bundle.publicSuffixes.twoLevelSuffixes.toSet()
        )
        val urlResults = extracted.urls.map { url ->
            urlAnalyzer.analyze(url, claimedBrands)
        }

        var upiAnalysis: UpiAnalysis? = null
        if (extracted.upiUris.isNotEmpty()) {
            upiAnalysis = UpiQrAnalyzer.analyze(
                rawPayload = extracted.upiUris.first(),
                claimedBrandName = claimedBrands.firstOrNull()?.name
            )
        }

        val lexicons = listOf(bundle.lexiconEn, bundle.lexiconHi, bundle.lexiconHinglish)
        val intentResult = IntentDetector.detect(
            preprocessed = preprocessed,
            lexicons = lexicons,
            hasUrls = urlResults.isNotEmpty(),
            hasUpiUri = upiAnalysis != null,
            isHindiOutput = isHindiOut
        )

        val manipResult = ManipulationDetector.detect(
            preprocessed = preprocessed,
            lexicons = lexicons,
            isHindiOutput = isHindiOut
        )

        val maxUrlScore = maxOf(urlResults.maxOfOrNull { it.score } ?: 0, upiAnalysis?.riskScore ?: 0)
        val categoryResult = ScamCategoryClassifier.classify(
            preprocessed = preprocessed,
            rules = bundle.categories.categories,
            intentHitsCount = intentResult.intents.size,
            tacticHitsCount = manipResult.tactics.size,
            maxUrlScore = maxUrlScore,
            isHindiOutput = isHindiOut
        )

        val postPaymentSignals = listOf("extra fee", "additional fee", "clearance fee", "additional tax", "अतिरिक्त शुल्क")
            .any { preprocessed.normalizedLower.contains(it) }

        val killChain = KillChainTracker.mapStages(
            categoryId = categoryResult.categoryId,
            tactics = manipResult.tactics,
            intents = intentResult.intents,
            hasUrls = urlResults.any { !it.isOfficialMatch },
            hasUrgencyOrFear = "urgency" in manipResult.tactics || "fear" in manipResult.tactics,
            postPaymentKeywordsPresent = postPaymentSignals
        )

        val combinedLegitMarkers = lexicons.flatMap { it.legitMarkers }
        val fusion = RiskFusionEngine.fuse(
            normalizedLower = preprocessed.normalizedLower,
            textLength = preprocessed.normalizedLower.length,
            categoryId = categoryResult.categoryId,
            categoryScoreM = categoryResult.categoryScore,
            manipulationScoreS = manipResult.manipulationScore,
            tactics = manipResult.tactics,
            intents = intentResult.intents,
            sensitiveInfoScoreP = intentResult.sensitiveInfoScore,
            urlResults = urlResults,
            upiRiskScore = upiAnalysis?.riskScore ?: 0,
            stagesReached = killChain.stagesReached,
            unexpectedSenderHint = input.unexpectedSenderHint,
            unsupportedScript = lang.unsupportedScriptWarning,
            legitMarkers = combinedLegitMarkers,
            config = config
        )

        val nextMoves = NextMoveForecaster.forecast(
            currentStage = killChain.currentStage,
            stages = bundle.stageTransitions.stages,
            isHindiOutput = isHindiOut
        )

        val evidence = buildList {
            addAll(manipResult.evidence)
            addAll(intentResult.evidence)
            for (u in urlResults) {
                if (u.score >= 25 || u.isOfficialMatch) {
                    val startIdx = preprocessed.originalDisplayText.indexOf(u.rawUrl.removePrefix("http://"), ignoreCase = true)
                        .takeIf { it >= 0 }
                    val endIdx = startIdx?.let { (it + u.rawUrl.length).coerceAtMost(preprocessed.originalDisplayText.length) }
                    add(
                        Evidence(
                            type = "URL",
                            phrase = u.rawUrl,
                            start = startIdx,
                            end = endIdx,
                            description = u.summary + (if (u.flags.isNotEmpty()) ": ${u.flags.first()}" else ""),
                            observed = true,
                            weight = (u.score / 15).coerceAtLeast(1)
                        )
                    )
                }
            }
            if (upiAnalysis != null) {
                add(
                    Evidence(
                        type = "UPI_QR",
                        phrase = upiAnalysis.payeeAddress.ifBlank { upiAnalysis.rawUri },
                        start = null,
                        end = null,
                        description = upiAnalysis.warnings.firstOrNull() ?: "UPI QR payment request detected",
                        observed = true,
                        weight = 5
                    )
                )
            }
            addAll(categoryResult.evidence)
        }.distinctBy { "${it.type}|${it.phrase}" }

        val templates = if (isHindiOut) bundle.explanationsHi else bundle.explanationsEn
        val explanation = ExplanationEngine.build(
            level = fusion.level,
            uncertain = fusion.uncertain,
            categoryId = categoryResult.categoryId,
            intents = intentResult.intents,
            tactics = manipResult.tactics,
            urlResults = urlResults,
            upiAnalysis = upiAnalysis,
            templates = templates,
            isHindiOutput = isHindiOut
        )

        val recommendations = RecommendationEngine.build(
            level = fusion.level,
            uncertain = fusion.uncertain,
            categoryId = categoryResult.categoryId,
            intents = intentResult.intents,
            tactics = manipResult.tactics,
            urlResults = urlResults,
            upiAnalysis = upiAnalysis,
            isHindiOutput = isHindiOut
        )

        val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)

        return ScanResult(
            scanId = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            sourceText = preprocessed.originalDisplayText,
            source = input.source,
            riskScore = fusion.score,
            riskLevel = fusion.level,
            uncertain = fusion.uncertain,
            confidence = fusion.confidence,
            language = lang.displayName,
            scamCategory = categoryResult.categoryDisplayName,
            scamCategoryId = categoryResult.categoryId,
            intents = intentResult.intents.map { it.code },
            requestedAction = intentResult.requestedAction,
            manipulation = manipResult.tactics,
            stagesReached = killChain.stagesReached,
            currentStage = killChain.currentStage,
            nextMoves = nextMoves,
            urlResults = urlResults,
            upiAnalysis = upiAnalysis,
            evidence = evidence,
            components = fusion.components,
            explanation = explanation.detailedWhy,
            elderlyExplanation = explanation.elderlyVerdict,
            whatCouldHappen = explanation.whatCouldHappen,
            recommendations = recommendations,
            wasTruncated = preprocessed.wasTruncated,
            unsupportedLanguageWarning = lang.unsupportedScriptWarning,
            detectorAvailable = true,
            detectorError = null,
            processingTimeMs = elapsedMs,
            processedOffline = true
        )
    }

    override fun analyzeUrl(
        rawUrl: String,
        claimedBrandId: String?,
        outputLanguage: String
    ): ScanResult {
        return analyze(
            ScanInput(
                text = rawUrl.trim(),
                source = Source.MANUAL,
                claimedBrandHint = claimedBrandId
            ),
            outputLanguage = outputLanguage
        )
    }

    override fun analyzeQrPayload(
        payload: String,
        claimedBrandId: String?,
        outputLanguage: String
    ): ScanResult {
        val trimmed = payload.trim()
        val isHindiOut = outputLanguage.startsWith("hi", ignoreCase = true)
        val bundle = (loadResult as? RuleLoadResult.Loaded)?.bundle
            ?: return unavailableResult(
                ScanInput(trimmed, Source.QR),
                (loadResult as? RuleLoadResult.Unavailable)?.reason ?: "Rules unavailable",
                System.nanoTime()
            )

        if (trimmed.startsWith("upi://", ignoreCase = true)) {
            val startNs = System.nanoTime()
            val claimedBrandName = claimedBrandId?.let { id ->
                bundle.brands.brands.firstOrNull { it.id == id }?.name
            }
            val upi = UpiQrAnalyzer.analyze(trimmed, claimedBrandName)
            val level = when {
                upi.riskScore >= 80 -> RiskLevel.CRITICAL
                upi.riskScore >= 60 -> RiskLevel.HIGH
                upi.riskScore >= 30 -> RiskLevel.MEDIUM
                else -> RiskLevel.LOW
            }
            val templates = if (isHindiOut) bundle.explanationsHi else bundle.explanationsEn
            val exp = ExplanationEngine.build(
                level = level,
                uncertain = false,
                categoryId = if (upi.riskScore >= 60) "payment_refund" else null,
                intents = listOf(DetectedIntent.MONEY),
                tactics = emptyList(),
                urlResults = emptyList(),
                upiAnalysis = upi,
                templates = templates,
                isHindiOutput = isHindiOut
            )
            val recs = RecommendationEngine.build(
                level = level,
                uncertain = false,
                categoryId = "payment_refund",
                intents = listOf(DetectedIntent.MONEY),
                tactics = emptyList(),
                urlResults = emptyList(),
                upiAnalysis = upi,
                isHindiOutput = isHindiOut
            )
            val evidence = upi.warnings.mapIndexed { idx, warn ->
                Evidence(
                    type = "UPI_QR",
                    phrase = if (idx == 0) upi.payeeAddress.ifBlank { upi.payeeName } else null,
                    description = warn,
                    observed = true,
                    weight = if (idx == 0) 4 else 3
                )
            }
            val elapsed = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
            return ScanResult(
                scanId = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                sourceText = trimmed,
                source = Source.QR,
                riskScore = upi.riskScore,
                riskLevel = level,
                uncertain = false,
                confidence = if (upi.riskScore >= 60) Confidence.HIGH else Confidence.MEDIUM,
                language = "UPI Payment Payload",
                scamCategory = if (upi.riskScore >= 60) {
                    if (isHindiOut) "यूपीआई / क्यूआर भुगतान धोखाधड़ी" else "UPI / QR Payment Trap"
                } else {
                    if (isHindiOut) "यूपीआई भुगतान क्यूआर" else "UPI Payment QR"
                },
                scamCategoryId = "payment_refund",
                intents = listOf(DetectedIntent.MONEY.code),
                requestedAction = if (isHindiOut) "यूपीआई भुगतान स्वीकृत करना" else "Approve outgoing UPI payment",
                manipulation = if (upi.riskScore >= 60) listOf("greed") else emptyList(),
                stagesReached = listOf(5, 6),
                currentStage = 6,
                nextMoves = NextMoveForecaster.forecast(6, bundle.stageTransitions.stages, isHindiOut),
                upiAnalysis = upi,
                evidence = evidence,
                components = ScoreComponents(
                    messageCategoryScore = upi.riskScore,
                    urlRiskScore = upi.riskScore,
                    sensitiveInfoScore = 60,
                    weightedRawScore = upi.riskScore
                ),
                explanation = exp.detailedWhy,
                elderlyExplanation = exp.elderlyVerdict,
                whatCouldHappen = exp.whatCouldHappen,
                recommendations = recs,
                processingTimeMs = elapsed,
                processedOffline = true
            )
        }

        // Otherwise inspect QR payload via standard message + URL scanner
        return analyze(
            ScanInput(text = trimmed, source = Source.QR, claimedBrandHint = claimedBrandId),
            outputLanguage = outputLanguage
        )
    }

    override fun splitAndAnalyzeConversation(
        rawTranscript: String,
        coercionAnswers: CoercionAnswers,
        outputLanguage: String
    ): ConversationAnalysisResult {
        val messages = ConversationSplitter.split(rawTranscript)
        return analyzeConversation(messages, coercionAnswers, outputLanguage)
    }

    override fun analyzeConversation(
        messages: List<ConversationMessage>,
        coercionAnswers: CoercionAnswers,
        outputLanguage: String
    ): ConversationAnalysisResult {
        val isHindiOut = outputLanguage.startsWith("hi", ignoreCase = true)
        val bundle = (loadResult as? RuleLoadResult.Loaded)?.bundle
        val stagesList = bundle?.stageTransitions?.stages.orEmpty()

        val turns = messages.map { msg ->
            val res = analyze(
                ScanInput(text = msg.text, source = Source.MANUAL),
                outputLanguage = outputLanguage
            )
            val stage = res.currentStage
            ConversationTurnAnalysis(
                message = msg,
                scanResult = res,
                primaryStage = stage,
                stageName = NextMoveForecaster.stageName(stage, stagesList, isHindiOut)
            )
        }

        val themTurns = turns.filter { it.message.sender == ChatSender.THEM }.ifEmpty { turns }
        val allStages = themTurns.flatMap { it.scanResult.stagesReached }.distinct().sorted()
        val highestStage = allStages.maxOrNull() ?: 0
        val allTactics = themTurns.flatMap { it.scanResult.manipulation }.distinct()
        val allIntents = themTurns.flatMap { it.scanResult.intents }.distinct()
        val maxSingleScore = themTurns.maxOfOrNull { it.scanResult.riskScore } ?: 0

        // Cumulative conversation risk can exceed any single message when stages escalate
        val escalationBonus = when {
            allStages.size >= 4 -> 22
            allStages.size == 3 -> 14
            allStages.size == 2 -> 8
            else -> 0
        }
        val isolationBonus = if ("isolation" in allTactics) 12 else 0
        val cumulativeScore = (maxSingleScore + escalationBonus + isolationBonus).coerceIn(0, 100)

        val level = when {
            cumulativeScore >= config.criticalThreshold -> RiskLevel.CRITICAL
            cumulativeScore >= config.highThreshold -> RiskLevel.HIGH
            cumulativeScore >= config.mediumThreshold -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val coercion = CoercionPressureEstimator.estimate(
            stagesReached = allStages,
            tactics = allTactics,
            answers = coercionAnswers,
            isHindiOutput = isHindiOut
        )

        val nextMoves = NextMoveForecaster.forecast(highestStage, stagesList, isHindiOut)
        val fullTranscript = messages.joinToString("\n") { "${it.senderLabel}: ${it.text}" }
        val aggregateScan = analyze(
            ScanInput(text = fullTranscript, source = Source.MANUAL),
            outputLanguage = outputLanguage
        ).copy(
            riskScore = maxOf(cumulativeScore, maxSingleScore),
            riskLevel = level,
            stagesReached = allStages,
            currentStage = highestStage,
            nextMoves = nextMoves
        )

        val isolationTriggered = "isolation" in allTactics ||
            level == RiskLevel.HIGH ||
            level == RiskLevel.CRITICAL ||
            coercion.pressureScore >= 65

        return ConversationAnalysisResult(
            conversationId = UUID.randomUUID().toString(),
            turns = turns,
            cumulativeScore = aggregateScan.riskScore,
            riskLevel = aggregateScan.riskLevel,
            uncertain = aggregateScan.uncertain && allStages.size <= 1,
            confidence = if (allStages.size >= 3) Confidence.HIGH else aggregateScan.confidence,
            stagesReached = allStages,
            currentStage = highestStage,
            nextMoves = nextMoves,
            manipulationSummary = allTactics,
            intentSummary = allIntents,
            isolationTriggered = isolationTriggered,
            coercionAssessment = coercion,
            combinedResult = aggregateScan
        )
    }

    override fun evaluatePreAction(input: PreActionInput, isHindi: Boolean): PreActionDecision {
        var score = (input.latestScanScore * 0.55).toInt()
        val reasons = mutableListOf<String>()

        when (input.actionType) {
            PreActionType.SHARE_OTP_PIN -> {
                score += 45
                reasons.add(if (isHindi) "ओटीपी या यूपीआई पिन साझा करना सबसे अधिक जोखिम भरा कदम है।" else "Sharing an OTP or PIN enables immediate account drain.")
            }
            PreActionType.INSTALL_APP -> {
                score += 40
                reasons.add(if (isHindi) "अनजान ऐप या स्क्रीन-शेयरिंग टूल से आपका फोन दूसरे के नियंत्रण में जा सकता है।" else "Installing unverified apps or remote tools exposes OTPs and banking screens.")
            }
            PreActionType.PAY_UPI, PreActionType.TRANSFER_MONEY -> {
                score += 25
                reasons.add(if (isHindi) "भेजे गए यूपीआई या बैंक पैसे वापस लाना बहुत कठिन होता है।" else "Money sent via UPI or bank transfer is hard to reverse once authorized.")
            }
            PreActionType.SHARE_ID_DOC -> {
                score += 25
                reasons.add(if (isHindi) "आधार/पैन साझा करने से पहचान की चोरी हो सकती है।" else "ID documents can be misused for identity theft or SIM fraud.")
            }
            PreActionType.OPEN_LINK -> {
                score += 18
                reasons.add(if (isHindi) "संदिग्ध लिंक नकली लॉगिन पेज पर ले जा सकता है।" else "Unverified links can lead to credential-stealing pages.")
            }
        }

        if (!input.knowsPayeePersonally) {
            score += 20
            reasons.add(if (isHindi) "आप सामने वाले व्यक्ति को व्यक्तिगत रूप से नहीं जानते।" else "You do not personally know the recipient or caller.")
        }
        if (input.toldToHurry) {
            score += 20
            reasons.add(if (isHindi) "आप पर जल्दबाजी करने का दबाव डाला गया है।" else "You were pushed to act quickly or before a deadline.")
        }
        if (input.toldNotToTellAnyone) {
            score += 30
            reasons.add(if (isHindi) "आपको किसी को न बताने या बात गुप्त रखने के लिए कहा गया।" else "Secrecy pressure ('don't tell anyone') is a hallmark of high-pressure scams.")
        }
        if (input.startedUnexpectedly) {
            score += 15
            reasons.add(if (isHindi) "यह बातचीत एक अनपेक्षित कॉल या मैसेज से शुरू हुई।" else "This started from an unexpected call, SMS, or WhatsApp message.")
        }
        when (input.amountRange) {
            AmountRange.FROM_1000_TO_10000 -> score += 10
            AmountRange.ABOVE_10000 -> {
                score += 20
                reasons.add(if (isHindi) "राशि ₹10,000 से अधिक है — सत्यापन बहुत जरूरी है।" else "High amount (> ₹10,000) warrants independent verification.")
            }
            else -> Unit
        }

        val combined = score.coerceIn(0, 100)
        val verdict = when {
            combined >= 70 || input.actionType == PreActionType.SHARE_OTP_PIN -> PreActionVerdict.STOP_DO_NOT_PROCEED
            combined >= 40 -> PreActionVerdict.PAUSE_AND_VERIFY
            else -> PreActionVerdict.PROCEED_CAREFULLY
        }

        val requiresPause = verdict != PreActionVerdict.PROCEED_CAREFULLY
        val headline = when (verdict) {
            PreActionVerdict.STOP_DO_NOT_PROCEED -> if (isHindi) "रुकें — अभी आगे न बढ़ें" else "Stop — Do Not Proceed Yet"
            PreActionVerdict.PAUSE_AND_VERIFY -> if (isHindi) "रुकें और पहले जांच करें" else "Pause & Verify Before Acting"
            PreActionVerdict.PROCEED_CAREFULLY -> if (isHindi) "सावधानी से आगे बढ़ें" else "Proceed Carefully & Double-Check"
        }

        val steps = if (isHindi) {
            listOf(
                "अपने बैंक के कार्ड के पीछे लिखे आधिकारिक नंबर पर खुद कॉल करके जांच करें।",
                "कोई भी कदम उठाने से पहले परिवार के किसी सदस्य या भरोसेमंद मित्र से सलाह लें।",
                "कोई भी संदेश, लिंक या भुगतान अनुरोध आधिकारिक ऐप खुद खोलकर सत्यापित करें।"
            )
        } else {
            listOf(
                "Call your bank using the official number printed on the back of your card.",
                "Ask a trusted family member or friend to review this before proceeding.",
                "Open your official bank/service app directly instead of tapping any link."
            )
        }

        return PreActionDecision(
            verdict = verdict,
            combinedRiskScore = combined,
            requiresForcedPause = requiresPause,
            pauseSeconds = if (requiresPause) 10 else 0,
            headline = headline,
            reasons = reasons.distinct(),
            verificationSteps = steps
        )
    }

    private fun unavailableResult(input: ScanInput, reason: String, startNs: Long): ScanResult {
        return ScanResult(
            scanId = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            sourceText = input.text.take(TextPreprocessor.MAX_INPUT_CHARS),
            source = input.source,
            riskScore = 0,
            riskLevel = RiskLevel.MEDIUM,
            uncertain = true,
            confidence = Confidence.LOW,
            language = "Unknown",
            explanation = reason,
            elderlyExplanation = reason,
            whatCouldHappen = "Analysis could not be completed because local detection rules could not be loaded.",
            recommendations = listOf("Reinstall or verify app files before trusting any message."),
            detectorAvailable = false,
            detectorError = reason,
            processingTimeMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L),
            processedOffline = true
        )
    }

    private fun emptyOrUnreadableResult(
        input: ScanInput,
        preprocessed: PreprocessedText,
        reason: String,
        startNs: Long
    ): ScanResult {
        return ScanResult(
            scanId = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            sourceText = preprocessed.originalDisplayText,
            source = input.source,
            riskScore = 0,
            riskLevel = RiskLevel.LOW,
            uncertain = true,
            confidence = Confidence.LOW,
            language = "Unknown",
            explanation = reason,
            elderlyExplanation = reason,
            whatCouldHappen = "Without readable text or a valid URL, RakshakAI cannot evaluate risk.",
            recommendations = listOf("Paste readable SMS, WhatsApp text, or a URL to scan."),
            wasTruncated = preprocessed.wasTruncated,
            detectorAvailable = true,
            processingTimeMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L),
            processedOffline = true
        )
    }
}
