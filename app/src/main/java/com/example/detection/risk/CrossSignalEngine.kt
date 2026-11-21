package com.example.detection.risk

import com.example.domain.models.CrossSignalContribution
import com.example.domain.models.CrossSignalInput
import com.example.domain.models.CrossSignalReport
import com.example.domain.models.RiskLevel

object CrossSignalEngine {

    fun evaluate(input: CrossSignalInput): CrossSignalReport {
        val contributions = mutableListOf<CrossSignalContribution>()

        val contentPeak = maxOf(input.messageRisk, input.conversationRisk, input.callTranscriptRisk)
        if (input.messageRisk > 0) {
            contributions.add(
                CrossSignalContribution(
                    signalName = "Message Scan",
                    score = input.messageRisk,
                    weightLabel = "35%",
                    detail = "Latest scanned message risk: ${input.messageRisk}/100"
                )
            )
        }
        if (input.conversationRisk > 0) {
            contributions.add(
                CrossSignalContribution(
                    signalName = "Conversation Timeline",
                    score = input.conversationRisk,
                    weightLabel = "35%",
                    detail = "Multi-turn cumulative risk: ${input.conversationRisk}/100"
                )
            )
        }
        if (input.callTranscriptRisk > 0) {
            contributions.add(
                CrossSignalContribution(
                    signalName = "Call Transcript",
                    score = input.callTranscriptRisk,
                    weightLabel = "35%",
                    detail = "Transcript analysis risk: ${input.callTranscriptRisk}/100"
                )
            )
        }
        if (input.linkRisk > 0) {
            contributions.add(
                CrossSignalContribution(
                    signalName = "Link / QR Analysis",
                    score = input.linkRisk,
                    weightLabel = "25%",
                    detail = "URL / QR indicator score: ${input.linkRisk}/100"
                )
            )
        }
        if (input.preActionRisk > 0) {
            contributions.add(
                CrossSignalContribution(
                    signalName = "Pre-Action Context",
                    score = input.preActionRisk,
                    weightLabel = "20%",
                    detail = "Payment/action answers score: ${input.preActionRisk}/100"
                )
            )
        }

        val hasInstalledRemoteApp = input.installedRemoteApps.isNotEmpty()
        var remoteAppScore = 0
        if (hasInstalledRemoteApp) {
            remoteAppScore = if (contentPeak >= 45 || input.preActionRisk >= 45) 85 else 25
            contributions.add(
                CrossSignalContribution(
                    signalName = "Installed Remote-Access App",
                    score = remoteAppScore,
                    weightLabel = "20%",
                    detail = "Detected on device: ${input.installedRemoteApps.joinToString(", ")}"
                )
            )
        }
        if (input.simulatedRemoteAppPrompt) {
            remoteAppScore = maxOf(remoteAppScore, 80)
            contributions.add(
                CrossSignalContribution(
                    signalName = "Caller Asked to Install App (SIMULATED)",
                    score = 80,
                    weightLabel = "20%",
                    detail = "Simulated signal: caller requested remote support app install",
                    isSimulated = true
                )
            )
        }
        if (input.simulatedScreenShareOn) {
            remoteAppScore = maxOf(remoteAppScore, 90)
            contributions.add(
                CrossSignalContribution(
                    signalName = "Screen Sharing Active (SIMULATED)",
                    score = 90,
                    weightLabel = "25%",
                    detail = "Simulated signal: active screen share during financial context",
                    isSimulated = true
                )
            )
        }

        // Weighted fusion + escalation when multiple distinct signals fire together
        val activeSignals = contributions.count { it.score >= 45 }
        val baseWeighted = (
            contentPeak * 0.40 +
                input.linkRisk * 0.20 +
                input.preActionRisk * 0.20 +
                remoteAppScore * 0.20
            ).toInt()

        val multiSignalEscalation = when {
            activeSignals >= 3 -> 22
            activeSignals == 2 -> 12
            else -> 0
        }

        // Critical combination: remote-access tool present + elevated financial/scam context
        val remoteWarning = if ((hasInstalledRemoteApp || input.simulatedRemoteAppPrompt || input.simulatedScreenShareOn) &&
            (contentPeak >= 45 || input.preActionRisk >= 45)
        ) {
            val appNames = input.installedRemoteApps.ifEmpty { listOf("a remote-access app") }.joinToString(", ")
            "Remote-access app detected ($appNames) alongside high-risk financial/pressure signals. Do NOT share any 9-digit code, PIN, or banking screen."
        } else null

        var finalScore = (maxOf(baseWeighted, (contentPeak * 0.65).toInt()) + multiSignalEscalation)
            .coerceIn(0, 100)
        if (remoteWarning != null) {
            finalScore = maxOf(finalScore, 82)
        }

        val level = when {
            finalScore >= 80 -> RiskLevel.CRITICAL
            finalScore >= 60 -> RiskLevel.HIGH
            finalScore >= 30 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val headline = when {
            remoteWarning != null -> "Critical Cross-Signal Alert: Remote-Access + Financial Risk"
            finalScore >= 80 -> "Multiple high-risk signals active together"
            finalScore >= 60 -> "Elevated cross-signal risk — pause and verify"
            finalScore >= 30 -> "Moderate signals detected across checks"
            else -> "No combined threat escalation detected"
        }

        return CrossSignalReport(
            overallScore = finalScore,
            riskLevel = level,
            headline = headline,
            contributions = contributions,
            shouldTriggerIsolationBreaker = finalScore >= 65,
            remoteAccessWarning = remoteWarning
        )
    }
}
