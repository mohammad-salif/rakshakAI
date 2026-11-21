package com.example

import com.example.detection.DemoSamples
import com.example.detection.conversation.ConversationSplitter
import com.example.detection.engine.RuleBasedScamEngine
import com.example.detection.risk.CoercionPressureEstimator
import com.example.detection.risk.CrossSignalEngine
import com.example.domain.models.AmountRange
import com.example.domain.models.ChatSender
import com.example.domain.models.CoercionAnswers
import com.example.domain.models.CrossSignalInput
import com.example.domain.models.PreActionInput
import com.example.domain.models.PreActionType
import com.example.domain.models.PreActionVerdict
import com.example.domain.models.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationAndPredictiveTest {

    private val engine = RuleBasedScamEngine(FileSystemRuleSource())

    @Test fun `splits bracketed WhatsApp timestamps accurately`() {
        val raw = """
            [10:30] Unknown Caller: Hello sir I am from CBI
            [10:31] Me: Why are you calling?
            [10:32] Unknown Caller: Do not tell anyone. Transfer ₹50,000 now.
        """.trimIndent()
        val msgs = ConversationSplitter.split(raw)
        assertEquals(3, msgs.size)
        assertEquals("10:30", msgs[0].timestampLabel)
        assertEquals(ChatSender.ME, msgs[1].sender)
        assertEquals(ChatSender.THEM, msgs[2].sender)
    }

    @Test fun `splits blank line separated message blocks`() {
        val raw = "First message hello\n\nSecond message urgent KYC\n\nThird message pay fee"
        val msgs = ConversationSplitter.split(raw)
        assertEquals(3, msgs.size)
    }

    @Test fun `digital arrest conversation detects stages and triggers isolation breaker`() {
        val res = engine.splitAndAnalyzeConversation(DemoSamples.digitalArrestConversationText)
        assertTrue(res.stagesReached.containsAll(listOf(2, 3, 4, 6)))
        assertTrue(res.isolationTriggered)
        assertTrue(res.cumulativeScore >= 80)
        assertEquals(RiskLevel.CRITICAL, res.riskLevel)
        assertTrue(res.nextMoves.any { it.stage == 7 })
    }

    @Test fun `cumulative conversation risk exceeds mild single turn`() {
        val raw = """
            Them: Hello sir, calling from bank KYC team.
            Them: Please keep this confidential.
            Them: Share your OTP immediately to avoid block.
        """.trimIndent()
        val res = engine.splitAndAnalyzeConversation(raw)
        val firstTurnScore = res.turns.first().scanResult.riskScore
        assertTrue(res.cumulativeScore > firstTurnScore)
    }

    @Test fun `coercion estimator respects user check-in answers and includes disclaimer`() {
        val assessment = CoercionPressureEstimator.estimate(
            stagesReached = listOf(1, 2, 3),
            tactics = listOf("authority", "fear"),
            answers = CoercionAnswers(
                toldToStayOnCall = true,
                toldNotToTellAnyone = true,
                givenDeadline = true,
                threatenedWithArrestOrBlock = true
            )
        )
        assertTrue(assessment.pressureScore >= 75)
        assertTrue(assessment.disclaimer.contains("not a psychological diagnosis", true))
    }

    @Test fun `cross signal engine escalates when installed remote app meets high scan risk`() {
        val report = CrossSignalEngine.evaluate(
            CrossSignalInput(
                messageRisk = 65,
                installedRemoteApps = listOf("AnyDesk")
            )
        )
        assertTrue(report.overallScore >= 80)
        assertEquals(RiskLevel.CRITICAL, report.riskLevel)
        assertTrue(report.remoteAccessWarning?.contains("AnyDesk") == true)
        assertTrue(report.shouldTriggerIsolationBreaker)
    }

    @Test fun `cross signal engine handles simulated toggles cleanly`() {
        val report = CrossSignalEngine.evaluate(
            CrossSignalInput(
                messageRisk = 50,
                simulatedScreenShareOn = true
            )
        )
        assertTrue(report.contributions.any { it.isSimulated })
        assertTrue(report.overallScore >= 80)
    }

    @Test fun `pre action check forces 10s pause on high risk UPI transfer`() {
        val decision = engine.evaluatePreAction(
            PreActionInput(
                actionType = PreActionType.PAY_UPI,
                knowsPayeePersonally = false,
                toldToHurry = true,
                toldNotToTellAnyone = true,
                startedUnexpectedly = true,
                amountRange = AmountRange.ABOVE_10000,
                latestScanScore = 75
            )
        )
        assertEquals(PreActionVerdict.STOP_DO_NOT_PROCEED, decision.verdict)
        assertTrue(decision.requiresForcedPause)
        assertEquals(10, decision.pauseSeconds)
    }

    @Test fun `pre action check allows low risk known payee`() {
        val decision = engine.evaluatePreAction(
            PreActionInput(
                actionType = PreActionType.PAY_UPI,
                knowsPayeePersonally = true,
                toldToHurry = false,
                toldNotToTellAnyone = false,
                startedUnexpectedly = false,
                amountRange = AmountRange.UNDER_1000,
                latestScanScore = 5
            )
        )
        assertEquals(PreActionVerdict.PROCEED_CAREFULLY, decision.verdict)
    }
}
