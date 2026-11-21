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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import com.example.detection.DemoSamples
import com.example.integrations.share.ShareIntentsHelper
import com.example.presentation.RakshakUiState
import com.example.presentation.RakshakViewModel
import com.example.presentation.components.SectionCard
import com.example.presentation.components.SimulatedDemoBadge
import com.example.presentation.theme.RakshakColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class EmergencyIncidentType(val label: String, val steps: List<String>) {
    SHARED_OTP(
        "Shared OTP / PIN",
        listOf(
            "Call your bank's official helpline (printed on the back of your debit/credit card) immediately to block your card and netbanking.",
            "Call National Cyber Crime Helpline 1930 (verify number on official site cybercrime.gov.in) right away.",
            "Report at cybercrime.gov.in and note your acknowledgement number.",
            "Change your banking/UPI passwords from a safe device."
        )
    ),
    SENT_MONEY(
        "Sent money",
        listOf(
            "Call 1930 immediately — the first 1–2 hours ('Golden Hour') improve chances of freezing funds.",
            "Call your bank or UPI app support through the official in-app help page to dispute the transaction.",
            "Note the UTR / Transaction Reference Number, amount, exact time, and payee UPI ID.",
            "File a formal cyber complaint at cybercrime.gov.in and your nearest cyber police station."
        )
    ),
    INSTALLED_APP(
        "Installed an app",
        listOf(
            "Turn ON Airplane Mode or disconnect Wi-Fi/Mobile Data immediately to stop remote access.",
            "Uninstall any recently installed remote-access app (AnyDesk, TeamViewer QuickSupport, RustDesk, or unknown APK).",
            "Do not open banking apps until the app is removed and passwords are changed from another phone.",
            "Call your bank and 1930 if any money moved or OTPs were visible."
        )
    ),
    CLICKED_LINK(
        "Clicked link / entered details",
        listOf(
            "Close the browser tab immediately and clear site data.",
            "If you entered bank login, card, or PIN details, immediately block the card and reset your netbanking password.",
            "Monitor your bank SMS alerts for any unauthorised debit and call 1930 if needed."
        )
    ),
    SHARED_ID(
        "Shared Aadhaar / PAN / ID",
        listOf(
            "Lock your Aadhaar biometrics via the official mAadhaar app or uidai.gov.in.",
            "Check your credit report (CIBIL/Experian) over the next few weeks for any unauthorised loan enquiries.",
            "Report identity misuse at cybercrime.gov.in if accompanied by fraud."
        )
    ),
    NOT_SURE(
        "Not sure what happened",
        listOf(
            "Pause all communication with the caller/sender immediately.",
            "Disconnect internet temporarily if you are unsure whether an app was installed.",
            "Call your bank's official number on your card to check recent activity, and call 1930 if any loss occurred."
        )
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmergencyAssistantScreen(
    viewModel: RakshakViewModel
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(EmergencyIncidentType.SENT_MONEY) }
    var amountEntered by remember { mutableStateOf("") }
    var payeeEntered by remember { mutableStateOf("") }
    var utrEntered by remember { mutableStateOf("") }
    val checklist = remember {
        mutableStateListOf(false, false, false, false, false)
    }
    val checklistLabels = listOf(
        "Screenshots of chat/call logs saved",
        "Transaction ID / UTR noted",
        "Scammer phone numbers / handles noted",
        "Suspicious URLs or QR payloads saved",
        "Exact date and time recorded"
    )
    val nowStr = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date()) }
    var draftText by remember(selectedType, amountEntered, payeeEntered, utrEntered) {
        mutableStateOf(
            """
            Cyber Fraud Complaint Draft (Offline Generated)
            Date & Time: $nowStr
            Incident Type: ${selectedType.label}
            Amount (if any): ${amountEntered.ifBlank { "₹[enter amount]" }}
            Payee / UPI ID / URL / Phone: ${payeeEntered.ifBlank { "[enter payee/number/URL]" }}
            Transaction ID / UTR: ${utrEntered.ifBlank { "[enter UTR if applicable]" }}

            Summary:
            I received a suspicious contact involving ${selectedType.label}. Please assist in freezing the transaction / investigating this incident.
            """.trimIndent()
        )
    }
    var showShareConfirm by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            Surface(
                color = RakshakColors.DangerSoft,
                border = BorderStroke(1.dp, RakshakColors.DangerRed),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Static Offline Emergency Decision Tree",
                        style = MaterialTheme.typography.titleSmall,
                        color = RakshakColors.CriticalDarkRed
                    )
                    Text(
                        text = "Works 100% offline with zero AI dependency. Verify all helpline numbers (e.g., 1930 / cybercrime.gov.in) on official government websites.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { ShareIntentsHelper.dialCyberHelpline1930(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DangerRed),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                    ) {
                        Icon(Icons.Filled.Call, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Dial National Cyber Helpline 1930")
                    }
                }
            }
        }

        item {
            SectionCard(title = "1. What happened?") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    EmergencyIncidentType.entries.forEach { t ->
                        FilterChip(
                            selected = selectedType == t,
                            onClick = { selectedType = t },
                            label = { Text(t.label) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text("Immediate numbered steps:", style = MaterialTheme.typography.labelLarge)
                selectedType.steps.forEachIndexed { idx, step ->
                    Text("${idx + 1}. $step", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 3.dp))
                }
            }
        }

        item {
            SectionCard(title = "2. Evidence Checklist") {
                checklistLabels.forEachIndexed { idx, label ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checklist[idx], onCheckedChange = { checklist[idx] = it })
                        Text(label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            SectionCard(title = "3. Editable Complaint Draft") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountEntered,
                        onValueChange = { amountEntered = it },
                        label = { Text("Amount (₹)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = utrEntered,
                        onValueChange = { utrEntered = it },
                        label = { Text("UTR / Ref ID") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = payeeEntered,
                    onValueChange = { payeeEntered = it },
                    label = { Text("Payee UPI / Phone / URL") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it },
                    label = { Text("Complaint Draft (Editable)") },
                    minLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            ShareIntentsHelper.copyToClipboard(context, "Cyber Complaint Draft", draftText)
                            viewModel.showToast("Complaint draft copied.")
                        },
                        modifier = Modifier.weight(1f).heightIn(min = 50.dp)
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy")
                    }
                    Button(
                        onClick = { showShareConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DeepNavy),
                        modifier = Modifier.weight(1f).heightIn(min = 50.dp)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Draft")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showShareConfirm) {
        AlertDialog(
            onDismissRequest = { showShareConfirm = false },
            title = { Text("Share Complaint Draft?") },
            text = { Text("Confirm opening your share sheet with this complaint summary.") },
            confirmButton = {
                Button(onClick = {
                    showShareConfirm = false
                    ShareIntentsHelper.sharePlainText(context, "Cyber Fraud Complaint Draft", draftText)
                }) { Text("Share") }
            },
            dismissButton = {
                TextButton(onClick = { showShareConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun FamilyGuardianScreen(
    uiState: RakshakUiState,
    viewModel: RakshakViewModel
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("Family") }
    var phone by remember { mutableStateOf("") }
    var confirmShareContact by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            SectionCard(title = "Trusted Family Contacts (Encrypted Locally)") {
                Text(
                    text = "Phone numbers are encrypted with Android Keystore AES-256-GCM. Alerts use your own SMS/Share action and NEVER include scanned message text.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (e.g. Son / Daughter)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = relation,
                        onValueChange = { relation = it },
                        label = { Text("Relation") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone number") },
                        modifier = Modifier.weight(1.4f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.addTrustedContact(name, relation, phone)
                        name = ""
                        phone = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.TealPrimary),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                ) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Trusted Contact")
                }
            }
        }

        items(uiState.trustedContacts, key = { it.id }) { entity ->
            val dec = viewModel.repository.decryptContact(entity)
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${dec.displayName} (${dec.relationship})", style = MaterialTheme.typography.titleSmall)
                        Text(dec.phoneNumber, style = MaterialTheme.typography.bodySmall, color = RakshakColors.InkSecondary)
                    }
                    OutlinedButton(
                        onClick = { confirmShareContact = dec.phoneNumber },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Alert")
                    }
                    IconButton(onClick = { viewModel.deleteTrustedContact(entity.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete contact")
                    }
                }
            }
        }

        // Simulated Second-Device Guardian Dashboard
        item {
            SectionCard(
                title = "Guardian Dashboard",
                containerColor = RakshakColors.SurfaceMuted
            ) {
                SimulatedDemoBadge(text = "SIMULATED / DEMO — Second Device Concept Preview")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Because RakshakAI has zero internet permission, live background syncing to another phone does not happen automatically. This preview shows the privacy-respecting design concept:",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                listOf(
                    "Mom's Phone · High risk alert at 3:12 PM · Message content NOT shared",
                    "Dad's Phone · Pre-Action Check paused a ₹5,000 QR scan at 11:40 AM · Content NOT shared"
                ).forEach { mock ->
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text(mock, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    confirmShareContact?.let { targetPhone ->
        val risk = uiState.latestScanResult?.riskLevel?.name ?: "HIGH"
        AlertDialog(
            onDismissRequest = { confirmShareContact = null },
            title = { Text("Send Consent-Based Alert?") },
            text = { Text("Opens your SMS app addressed to $targetPhone with only risk level ($risk) + time. Message content is never sent.") },
            confirmButton = {
                Button(onClick = {
                    ShareIntentsHelper.sendTrustedContactSmsAlert(context, targetPhone, risk)
                    confirmShareContact = null
                }) { Text("Open SMS App") }
            },
            dismissButton = {
                TextButton(onClick = { confirmShareContact = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SimulationLabScreen(
    uiState: RakshakUiState,
    viewModel: RakshakViewModel,
    onRunSampleToResult: () -> Unit,
    onRunConversationToTimeline: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            SectionCard {
                SimulatedDemoBadge(text = "SIMULATION LAB / DEMO DATA")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Test all 15 benchmark messages, Digital Arrest conversation, QR/Link demos, and simulated cross-signal triggers instantly offline.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Simulated Cross-Signal Toggles (C14)
        item {
            SectionCard(title = "Simulated Call / App Signals") {
                SimulatedDemoBadge(text = "SIMULATED / DEMO")
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Caller asked me to install remote app", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Switch(
                        checked = uiState.settings.simulatedRemotePrompt,
                        onCheckedChange = { viewModel.setSimulatedSignals(it, uiState.settings.simulatedScreenShare) }
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Screen sharing is active", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    Switch(
                        checked = uiState.settings.simulatedScreenShare,
                        onCheckedChange = { viewModel.setSimulatedSignals(uiState.settings.simulatedRemotePrompt, it) }
                    )
                }
            }
        }

        // Digital Arrest Conversation & QR Demo
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        viewModel.updateConversationRawInput(DemoSamples.digitalArrestConversationText)
                        viewModel.splitConversationFromRaw()
                        viewModel.analyzeConversation { onRunConversationToTimeline() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DangerRed),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag("sim_digital_arrest_btn")
                ) {
                    Text("Digital Arrest Chat")
                }
                Button(
                    onClick = {
                        viewModel.scanQrPayload(DemoSamples.qrUpiDemoPayload) { onRunSampleToResult() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DeepNavy),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag("sim_qr_demo_btn")
                ) {
                    Text("UPI Refund QR")
                }
            }
        }

        item {
            Text("Seed Message Benchmarks (§10)", style = MaterialTheme.typography.titleMedium)
        }

        items(DemoSamples.messageSamples, key = { it.id }) { sample ->
            SectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(sample.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Surface(color = RakshakColors.SurfaceMuted, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            sample.expectedBadge,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(sample.payload, style = MaterialTheme.typography.bodySmall, color = RakshakColors.InkSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.scanMessage(textOverride = sample.payload) {
                            onRunSampleToResult()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                ) {
                    Text("Run Offline Scan")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
