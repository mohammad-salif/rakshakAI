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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.models.AmountRange
import com.example.domain.models.PreActionInput
import com.example.domain.models.PreActionType
import com.example.domain.models.PreActionVerdict
import com.example.integrations.share.ShareIntentsHelper
import com.example.presentation.RakshakUiState
import com.example.presentation.RakshakViewModel
import com.example.presentation.components.SectionCard
import com.example.presentation.components.SimulatedDemoBadge
import com.example.presentation.components.riskColor
import com.example.presentation.theme.LocalRakshakMetrics
import com.example.presentation.theme.RakshakColors
import kotlinx.coroutines.delay

@Composable
fun ProtectHubScreen(
    uiState: RakshakUiState,
    onOpenPreActionCheck: () -> Unit,
    onOpenEmergencyAssistant: () -> Unit,
    onOpenIsolationBreaker: () -> Unit,
    onOpenFamilyGuardian: () -> Unit,
    onOpenSimulationLab: () -> Unit
) {
    val cross = uiState.crossSignalReport
    val metrics = LocalRakshakMetrics.current

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Cross-signal intervention status
        item {
            SectionCard(
                title = "Cross-Signal Protection (${cross.overallScore}/100)",
                borderColor = riskColor(cross.riskLevel)
            ) {
                Text(
                    text = cross.headline,
                    style = MaterialTheme.typography.titleSmall,
                    color = riskColor(cross.riskLevel)
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (cross.contributions.isEmpty()) {
                    Text(
                        text = "No active multi-signal escalation. Installed remote-access app check: " +
                            if (uiState.installedRemoteApps.isEmpty()) "None found from monitored list."
                            else uiState.installedRemoteApps.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text("Which signals contributed:", style = MaterialTheme.typography.labelLarge)
                    cross.contributions.forEach { c ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("• ${c.signalName} (${c.score}/100): ${c.detail}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                            if (c.isSimulated) SimulatedDemoBadge(text = "SIMULATED")
                        }
                    }
                }
                cross.remoteAccessWarning?.let { warn ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(warn, style = MaterialTheme.typography.bodySmall, color = RakshakColors.DangerRed, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Feature cards
        val cards = listOf(
            Triple("Pre-Action Safety Check", "10-second pause & verification checklist before you click, install, or pay.", Icons.Filled.PauseCircle) to onOpenPreActionCheck,
            Triple("Scam Emergency Assistant", "Offline decision tree, 1930 helpline steps, evidence checklist & complaint draft.", Icons.Filled.Emergency) to onOpenEmergencyAssistant,
            Triple("Isolation Breaker", "Calm secrecy-breaker screen + one-tap metadata-only alert to trusted family.", Icons.Filled.Security) to onOpenIsolationBreaker,
            Triple("Family Guardian", "Encrypted trusted contacts + simulated second-device dashboard.", Icons.Filled.FamilyRestroom) to onOpenFamilyGuardian,
            Triple("Simulation Lab (Demo)", "Try all 15 test cases, Digital Arrest chat, QR/Link demos & simulated signals.", Icons.Filled.Science) to onOpenSimulationLab
        )

        items(cards.size) { index ->
            val (meta, action) = cards[index]
            Card(
                onClick = action,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().heightIn(min = maxOf(metrics.minTouchTarget, 76.dp))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = meta.third,
                        contentDescription = meta.first,
                        tint = if (index == 1) RakshakColors.DangerRed else RakshakColors.DeepNavy
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(meta.first, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = meta.second,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PreActionCheckScreen(
    uiState: RakshakUiState,
    viewModel: RakshakViewModel,
    onDone: () -> Unit
) {
    var actionType by remember { mutableStateOf(PreActionType.PAY_UPI) }
    var knowsPayee by remember { mutableStateOf(false) }
    var toldToHurry by remember { mutableStateOf(true) }
    var toldSecret by remember { mutableStateOf(false) }
    var unexpected by remember { mutableStateOf(true) }
    var amountRange by remember { mutableStateOf(AmountRange.FROM_1000_TO_10000) }
    var countdown by remember { mutableIntStateOf(0) }
    var evaluatedOnce by remember { mutableStateOf(false) }

    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000L)
            countdown--
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            Surface(
                color = RakshakColors.CautionSoft,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, RakshakColors.CautionAmber),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "This is a reminder and safety check. RakshakAI cannot lock or block payments made in other apps.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF78350F),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        item {
            SectionCard(title = "1. What are you about to do?") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        PreActionType.PAY_UPI to "Pay via UPI",
                        PreActionType.OPEN_LINK to "Open link",
                        PreActionType.INSTALL_APP to "Install app",
                        PreActionType.SHARE_OTP_PIN to "Share OTP/PIN",
                        PreActionType.SHARE_ID_DOC to "Share ID doc",
                        PreActionType.TRANSFER_MONEY to "Bank transfer"
                    ).forEach { (type, label) ->
                        FilterChip(
                            selected = actionType == type,
                            onClick = { actionType = type },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        item {
            SectionCard(title = "2. Quick Context Check") {
                listOf(
                    Triple(knowsPayee, "I personally know this recipient/caller") { v: Boolean -> knowsPayee = v },
                    Triple(toldToHurry, "I was told to hurry or act immediately") { v: Boolean -> toldToHurry = v },
                    Triple(toldSecret, "I was told not to tell anyone") { v: Boolean -> toldSecret = v },
                    Triple(unexpected, "This started from an unexpected call/message") { v: Boolean -> unexpected = v }
                ).forEach { (checked, label, setter) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checked, onCheckedChange = setter)
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Amount involved (if payment):", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        AmountRange.NONE to "None",
                        AmountRange.UNDER_1000 to "< ₹1,000",
                        AmountRange.FROM_1000_TO_10000 to "₹1k–₹10k",
                        AmountRange.ABOVE_10000 to "> ₹10,000"
                    ).forEach { (range, label) ->
                        FilterChip(
                            selected = amountRange == range,
                            onClick = { amountRange = range },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    val input = PreActionInput(
                        actionType = actionType,
                        knowsPayeePersonally = knowsPayee,
                        toldToHurry = toldToHurry,
                        toldNotToTellAnyone = toldSecret,
                        startedUnexpectedly = unexpected,
                        amountRange = amountRange,
                        latestScanScore = uiState.latestScanResult?.riskScore ?: 0
                    )
                    viewModel.evaluatePreAction(input)
                    evaluatedOnce = true
                    countdown = 10
                },
                colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DeepNavy),
                modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).testTag("preaction_evaluate_btn")
            ) {
                Text("Run Pre-Action Safety Check")
            }
        }

        if (evaluatedOnce) {
            uiState.preActionDecision?.let { decision ->
                item {
                    val boxColor = when (decision.verdict) {
                        PreActionVerdict.STOP_DO_NOT_PROCEED -> RakshakColors.DangerSoft
                        PreActionVerdict.PAUSE_AND_VERIFY -> RakshakColors.CautionSoft
                        PreActionVerdict.PROCEED_CAREFULLY -> RakshakColors.SafeSoft
                    }
                    val border = when (decision.verdict) {
                        PreActionVerdict.STOP_DO_NOT_PROCEED -> RakshakColors.DangerRed
                        PreActionVerdict.PAUSE_AND_VERIFY -> RakshakColors.CautionAmber
                        PreActionVerdict.PROCEED_CAREFULLY -> RakshakColors.SafeGreen
                    }
                    SectionCard(
                        title = "${decision.headline} (${decision.combinedRiskScore}/100)",
                        containerColor = boxColor,
                        borderColor = border
                    ) {
                        decision.reasons.forEach { r ->
                            Text("• $r", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Verification steps:", style = MaterialTheme.typography.labelLarge)
                        decision.verificationSteps.forEachIndexed { idx, s ->
                            Text("${idx + 1}. $s", style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        if (decision.requiresForcedPause && countdown > 0) {
                            OutlinedButton(
                                onClick = {},
                                enabled = false,
                                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("preaction_countdown_btn")
                            ) {
                                Text("Continue anyway (Pause ${countdown}s)")
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = onDone,
                                    colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.SafeGreen),
                                    modifier = Modifier.weight(1f).heightIn(min = 50.dp)
                                ) {
                                    Text("I'll Stop & Verify")
                                }
                                OutlinedButton(
                                    onClick = onDone,
                                    modifier = Modifier.weight(1f).heightIn(min = 50.dp)
                                ) {
                                    Text("Continue anyway")
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun IsolationBreakerScreen(
    uiState: RakshakUiState,
    viewModel: RakshakViewModel,
    onOpenPreActionCheck: () -> Unit,
    onAddContact: () -> Unit
) {
    val context = LocalContext.current
    val firstContact = uiState.trustedContacts.firstOrNull()?.let { viewModel.repository.decryptContact(it) }
    val riskLabel = uiState.latestScanResult?.riskLevel?.name ?: "HIGH"
    var showAlertConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = RakshakColors.DeepNavy),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Pause. Breathe.",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF7CE8E2)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Real banks, police, and government officers NEVER ask you to keep a call secret or stay isolated from your family.",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "You are allowed to hang up right now and talk to someone you trust.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFD6E2F0)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (firstContact != null && firstContact.phoneNumber.isNotBlank()) {
                    ShareIntentsHelper.dialTrustedContact(context, firstContact.phoneNumber)
                } else {
                    onAddContact()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.SafeGreen),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("isolation_call_trusted")
        ) {
            Icon(Icons.Filled.Call, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (firstContact != null) "Call ${firstContact.displayName}"
                else "Add & Call a Trusted Person"
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { showAlertConfirm = true },
            colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DangerRed),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("isolation_send_alert")
        ) {
            Icon(Icons.Filled.Sms, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Send Alert to Trusted Contact")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onOpenPreActionCheck,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
        ) {
            Text("Pause & Verify Steps (Reminder Only)")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Alerts open your own SMS/Share app with ONLY risk level + time + 'please call me' — message content is never included.",
            style = MaterialTheme.typography.bodySmall,
            color = RakshakColors.InkSecondary
        )
    }

    if (showAlertConfirm) {
        AlertDialog(
            onDismissRequest = { showAlertConfirm = false },
            title = { Text("Send Privacy-Safe Alert?") },
            text = {
                Text("This opens your SMS/Share app with: \"RakshakAI Safety Alert: $riskLabel risk situation detected — please call me.\" No message text is shared. You tap Send yourself.")
            },
            confirmButton = {
                Button(onClick = {
                    showAlertConfirm = false
                    if (firstContact != null && firstContact.phoneNumber.isNotBlank()) {
                        ShareIntentsHelper.sendTrustedContactSmsAlert(context, firstContact.phoneNumber, riskLabel)
                    } else {
                        ShareIntentsHelper.shareTrustedContactAlertSheet(context, riskLabel)
                    }
                }) { Text("Open Alert Draft") }
            },
            dismissButton = {
                TextButton(onClick = { showAlertConfirm = false }) { Text("Cancel") }
            }
        )
    }
}
