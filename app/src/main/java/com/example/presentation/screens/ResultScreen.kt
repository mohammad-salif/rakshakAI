package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.models.FeedbackOption
import com.example.domain.models.RiskLevel
import com.example.domain.models.ScanResult
import com.example.integrations.share.ShareIntentsHelper
import com.example.presentation.RakshakUiState
import com.example.presentation.RakshakViewModel
import com.example.presentation.components.HighlightedEvidenceText
import com.example.presentation.components.KillChainStepperBar
import com.example.presentation.components.RiskRingGauge
import com.example.presentation.components.SectionCard
import com.example.presentation.components.riskColor
import com.example.presentation.components.riskLabelText
import com.example.presentation.theme.LocalRakshakMetrics
import com.example.presentation.theme.RakshakColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultScreen(
    uiState: RakshakUiState,
    viewModel: RakshakViewModel,
    onOpenPreActionCheck: () -> Unit,
    onOpenIsolationBreaker: () -> Unit,
    onBack: () -> Unit
) {
    val result = uiState.latestScanResult
    if (result == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No scan result active.")
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onBack) { Text("Back to Scan") }
        }
        return
    }

    val context = LocalContext.current
    val metrics = LocalRakshakMetrics.current
    val isHindi = uiState.settings.appLanguage.startsWith("hi")

    var whyExpanded by remember { mutableStateOf(false) }
    var selectedStageInfo by remember { mutableStateOf<Int?>(null) }
    var showVaultDialog by remember { mutableStateOf(false) }
    var showBrowserConfirmUrl by remember { mutableStateOf<String?>(null) }
    var showQuarantineConfirm by remember { mutableStateOf(false) }
    var feedbackSaved by remember(result.scanId) { mutableStateOf<FeedbackOption?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Detector unavailable banner if JSON failed
        if (!result.detectorAvailable) {
            item {
                SectionCard(
                    containerColor = RakshakColors.DangerSoft,
                    borderColor = RakshakColors.DangerRed
                ) {
                    Text(
                        text = result.detectorError ?: "Detector unavailable.",
                        style = MaterialTheme.typography.titleSmall,
                        color = RakshakColors.CriticalDarkRed
                    )
                }
            }
        }

        // High / Critical obvious non-colour-only safety banner
        if (result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.CRITICAL) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RakshakColors.DangerRed),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (isHindi) "रुकें: लिंक न खोलें · कोड न बताएं · पैसे न भेजें"
                            else "STOP: Don't click · Don't share OTP/PIN · Don't pay",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 1. Risk Header Card
        item {
            SectionCard(borderColor = riskColor(result.riskLevel, result.uncertain)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RiskRingGauge(
                        score = result.riskScore,
                        level = result.riskLevel,
                        uncertain = result.uncertain
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = riskLabelText(result.riskLevel, result.uncertain, isHindi),
                            style = MaterialTheme.typography.titleMedium,
                            color = riskColor(result.riskLevel, result.uncertain),
                            fontWeight = FontWeight.Bold
                        )
                        result.scamCategory?.let { cat ->
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = RakshakColors.SurfaceMuted,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = cat,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (metrics.elderlyMode && result.elderlyExplanation.isNotBlank()) {
                                result.elderlyExplanation
                            } else {
                                result.explanation
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Confidence: ${result.confidence.name} · Language: ${result.language} · ${result.processingTimeMs} ms",
                            style = MaterialTheme.typography.labelSmall,
                            color = RakshakColors.InkSecondary
                        )
                        Text(
                            text = stringResource(R.string.result_processed_offline),
                            style = MaterialTheme.typography.labelSmall,
                            color = RakshakColors.TealPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (result.wasTruncated) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Notice: Long input was truncated to 5,000 characters for safe offline processing.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RakshakColors.CautionAmber
                    )
                }
                if (result.unsupportedLanguageWarning) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Limited analysis warning: Unsupported script detected. Only URLs and basic symbols were checked.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RakshakColors.CautionAmber
                    )
                }
            }
        }

        // Read Aloud quick button (especially prominent in Elderly Mode)
        item {
            OutlinedButton(
                onClick = { viewModel.speakResultAloud(result) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = metrics.minTouchTarget)
                    .testTag("result_read_aloud_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.result_read_aloud))
            }
        }

        // 2. What the sender wants you to do (intents)
        item {
            SectionCard(title = stringResource(R.string.result_wants_you_to)) {
                if (result.intents.isEmpty()) {
                    Text(
                        text = if (isHindi) "कोई स्पष्ट संवेदनशील कार्रवाई नहीं मांगी गई।" else "No explicit sensitive action request detected.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = result.requestedAction,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        result.intents.forEach { intent ->
                            AssistChip(onClick = {}, label = { Text(intent) })
                        }
                    }
                }
            }
        }

        // 3. Pressure tactics used
        item {
            SectionCard(title = stringResource(R.string.result_pressure_tactics)) {
                if (result.manipulation.isEmpty()) {
                    Text(
                        text = if (isHindi) "कोई स्पष्ट दबाव रणनीति नहीं मिली।" else "No strong social-engineering pressure phrases detected.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        result.manipulation.forEach { tactic ->
                            Surface(
                                color = RakshakColors.CautionSoft,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, RakshakColors.CautionAmber)
                            ) {
                                Text(
                                    text = tactic.replace('_', ' ').uppercase(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF92400E),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Why this is suspicious (Highlighted text + Observed vs Inferred list)
        item {
            SectionCard(title = stringResource(R.string.result_why_suspicious)) {
                if (result.sourceText.isNotBlank()) {
                    Surface(
                        color = RakshakColors.SurfaceMuted,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        HighlightedEvidenceText(
                            sourceText = result.sourceText,
                            evidence = result.evidence,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (result.evidence.isEmpty()) {
                    Text(
                        text = "No suspicious phrases or risky URL indicators matched.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    result.evidence.forEach { ev ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                color = if (ev.observed) RakshakColors.DeepNavy else RakshakColors.TealSoft,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = if (ev.observed) "Observed" else "Inferred",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (ev.observed) Color.White else RakshakColors.DeepNavy,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ev.description,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // URL breakdown cards if present
                result.urlResults.forEach { urlRes ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = if (urlRes.score >= 50) RakshakColors.DangerSoft else RakshakColors.SurfaceMuted,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Link: ${urlRes.rawUrl}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(
                                text = "Registered domain: ${urlRes.registeredDomain.ifBlank { "unknown" }} · URL score: ${urlRes.score}/100",
                                style = MaterialTheme.typography.bodySmall,
                                color = RakshakColors.InkSecondary
                            )
                            urlRes.flags.forEach { f ->
                                Text("• $f", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        ShareIntentsHelper.copyToClipboard(context, "Scanned URL", urlRes.rawUrl)
                                        viewModel.showToast("URL copied (not opened).")
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Text("Copy link")
                                }
                                OutlinedButton(
                                    onClick = { showBrowserConfirmUrl = urlRes.rawUrl },
                                    modifier = Modifier.heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open anyway")
                                }
                            }
                        }
                    }
                }

                // UPI QR breakdown card if present
                result.upiAnalysis?.let { upi ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = RakshakColors.CautionSoft,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("UPI QR Payload Inspection (Payment NOT launched)", style = MaterialTheme.typography.labelLarge)
                            Text("Payee Name (pn): ${upi.payeeName.ifBlank { "Not specified" }}", style = MaterialTheme.typography.bodySmall)
                            Text("Payee VPA (pa): ${upi.payeeAddress.ifBlank { "Not specified" }}", style = MaterialTheme.typography.bodySmall)
                            if (upi.amount.isNotBlank()) {
                                Text("Preset Amount (am): ₹${upi.amount} ${upi.currency}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            if (upi.note.isNotBlank()) {
                                Text("Note (tn): ${upi.note}", style = MaterialTheme.typography.bodySmall)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            upi.warnings.forEach { w ->
                                Text("• $w", style = MaterialTheme.typography.bodySmall, color = Color(0xFF78350F))
                            }
                        }
                    }
                }
            }
        }

        // 5. What could happen if you follow it
        item {
            SectionCard(title = stringResource(R.string.result_what_could_happen)) {
                Text(text = result.whatCouldHappen, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // 6. What to do now (recommendations)
        item {
            SectionCard(title = stringResource(R.string.result_what_to_do)) {
                result.recommendations.forEachIndexed { idx, rec ->
                    Text(
                        text = "${idx + 1}. $rec",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 3.dp)
                    )
                }
            }
        }

        // 7. Possible next move & Kill-chain bar
        if (result.stagesReached.isNotEmpty() || result.nextMoves.isNotEmpty()) {
            item {
                SectionCard(title = stringResource(R.string.result_possible_next_move)) {
                    KillChainStepperBar(
                        stagesReached = result.stagesReached,
                        currentStage = result.currentStage,
                        onStageSelected = { selectedStageInfo = it }
                    )
                    selectedStageInfo?.let { s ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Stage S$s selected — predictions are possibilities, not certainties.",
                            style = MaterialTheme.typography.bodySmall,
                            color = RakshakColors.InkSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    result.nextMoves.forEach { move ->
                        Surface(
                            color = RakshakColors.SurfaceMuted,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "${move.likelihood.name.replace('_', ' ')}: ${move.title}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = RakshakColors.DeepNavy
                                )
                                Text(text = move.advice, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }

        // 8. Expandable "Why this score?"
        item {
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.result_why_this_score),
                        style = MaterialTheme.typography.titleSmall
                    )
                    TextButton(onClick = { whyExpanded = !whyExpanded }) {
                        Text(if (whyExpanded) "Hide" else "Show weights")
                        Icon(
                            imageVector = if (whyExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null
                        )
                    }
                }
                if (whyExpanded) {
                    val c = result.components
                    Text(
                        text = "Formula: R = 0.35·M(${c.messageCategoryScore}) + 0.20·S(${c.manipulationScore}) + 0.20·U(${c.urlRiskScore}) + 0.15·P(${c.sensitiveInfoScore}) + 0.10·C(${c.contextScore}) = ${c.weightedRawScore}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    c.overridesApplied.forEach { o ->
                        Text("• Floor override: $o", style = MaterialTheme.typography.bodySmall, color = RakshakColors.DangerRed)
                    }
                    c.dampenersApplied.forEach { d ->
                        Text("• Legit dampener: $d", style = MaterialTheme.typography.bodySmall, color = RakshakColors.SafeGreen)
                    }
                }
            }
        }

        // 9. Actions Row
        item {
            SectionCard(title = "Actions") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenPreActionCheck,
                        colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DeepNavy),
                        modifier = Modifier.heightIn(min = metrics.minTouchTarget).testTag("result_pre_action_btn")
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pre-Action Check")
                    }
                    OutlinedButton(
                        onClick = { showVaultDialog = true },
                        modifier = Modifier.heightIn(min = metrics.minTouchTarget).testTag("result_save_vault_btn")
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to Vault")
                    }
                    OutlinedButton(
                        onClick = { showQuarantineConfirm = true },
                        modifier = Modifier.heightIn(min = metrics.minTouchTarget).testTag("result_quarantine_btn")
                    ) {
                        Text("Quarantine")
                    }
                    if (result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.CRITICAL || "isolation" in result.manipulation) {
                        Button(
                            onClick = onOpenIsolationBreaker,
                            colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DangerRed),
                            modifier = Modifier.heightIn(min = metrics.minTouchTarget)
                        ) {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Alert Trusted Contact")
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            ShareIntentsHelper.copyToClipboard(context, "RakshakAI Result", result.explanation)
                            viewModel.showToast("Explanation copied.")
                        },
                        modifier = Modifier.heightIn(min = metrics.minTouchTarget)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy")
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.dismissLatestResult()
                            onBack()
                        },
                        modifier = Modifier.heightIn(min = metrics.minTouchTarget)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Discard")
                    }
                }
            }
        }

        // 10. Feedback Row
        item {
            SectionCard(title = stringResource(R.string.result_feedback_prompt)) {
                Text(
                    text = "Stored locally only (does not auto-change rules or upload anything).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        FeedbackOption.CORRECT_SCAM to "Correct scam",
                        FeedbackOption.FALSE_ALARM to "False alarm (legit)",
                        FeedbackOption.MISSED_SOMETHING to "Missed something",
                        FeedbackOption.NOT_SURE to "Not sure"
                    ).forEach { (opt, label) ->
                        OutlinedButton(
                            onClick = {
                                val domain = result.urlResults.firstOrNull()?.registeredDomain
                                viewModel.submitFeedback(
                                    result.scanId,
                                    opt,
                                    if (opt == FeedbackOption.FALSE_ALARM) domain else null
                                )
                                feedbackSaved = opt
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(if (feedbackSaved == opt) "✓ $label" else label)
                        }
                    }
                }
            }
        }

        // 11. Footer disclaimer
        item {
            Text(
                text = stringResource(R.string.result_footer_disclaimer),
                style = MaterialTheme.typography.bodySmall,
                color = RakshakColors.InkSecondary,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }

    if (showVaultDialog) {
        var noteText by remember { mutableStateOf("") }
        var incidentTitle by remember { mutableStateOf(result.scamCategory ?: "Suspicious Incident") }
        AlertDialog(
            onDismissRequest = { showVaultDialog = false },
            title = { Text("Save to Encrypted Vault") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Stored locally with AES-256-GCM encryption (Android Keystore).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = incidentTitle,
                        onValueChange = { incidentTitle = it },
                        label = { Text("Incident title (for Replay Timeline)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Optional private note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveLatestToVault(noteText, incidentTitle, uiState.incidents.firstOrNull { it.title == incidentTitle }?.id)
                    showVaultDialog = false
                }) {
                    Text("Encrypt & Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVaultDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showQuarantineConfirm) {
        AlertDialog(
            onDismissRequest = { showQuarantineConfirm = false },
            title = { Text("Move Copy to Quarantine?") },
            text = {
                Text("This saves an encrypted copy inside RakshakAI Quarantine. Your original SMS or WhatsApp message is not touched — Android does not allow deleting messages in other apps.")
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.quarantineLatest()
                    showQuarantineConfirm = false
                }) { Text("Quarantine Copy") }
            },
            dismissButton = {
                TextButton(onClick = { showQuarantineConfirm = false }) { Text("Cancel") }
            }
        )
    }

    showBrowserConfirmUrl?.let { targetUrl ->
        AlertDialog(
            onDismissRequest = { showBrowserConfirmUrl = null },
            title = { Text("Warning: High Risk / Unverified Link") },
            text = {
                Text("RakshakAI strongly recommends running Pre-Action Safety Check first. Are you sure you want to open $targetUrl in an external browser?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        ShareIntentsHelper.openUrlInBrowserExplicitlyConfirmed(context, targetUrl)
                        showBrowserConfirmUrl = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DangerRed)
                ) { Text("Open Anyway") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBrowserConfirmUrl = null
                    onOpenPreActionCheck()
                }) { Text("Run Pre-Action Check") }
            }
        )
    }
}
