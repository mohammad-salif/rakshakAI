package com.example.presentation.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.detection.DemoSamples
import com.example.domain.models.ChatSender
import com.example.domain.models.Source
import com.example.integrations.share.ShareIntentsHelper
import com.example.presentation.RakshakUiState
import com.example.presentation.RakshakViewModel
import com.example.presentation.components.SectionCard
import com.example.presentation.components.SimulatedDemoBadge
import com.example.presentation.theme.LocalRakshakMetrics
import com.example.presentation.theme.RakshakColors

enum class ScanModeTab(val label: String) {
    MESSAGE("Message"),
    CONVERSATION("Conversation"),
    LINK("Link"),
    SCREENSHOT("Screenshot"),
    CALL("Call Script")
}

@Composable
fun ScanHubScreen(
    initialTab: String,
    uiState: RakshakUiState,
    viewModel: RakshakViewModel,
    onNavigateToResult: () -> Unit,
    onNavigateToConversationTimeline: () -> Unit,
    onNavigateToSimulationLab: () -> Unit
) {
    var activeTab by remember {
        mutableStateOf(
            runCatching { ScanModeTab.valueOf(initialTab) }.getOrDefault(ScanModeTab.MESSAGE)
        )
    }
    LaunchedEffect(initialTab) {
        runCatching { ScanModeTab.valueOf(initialTab) }.getOrNull()?.let { activeTab = it }
    }

    val context = LocalContext.current
    val metrics = LocalRakshakMetrics.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Horizontal mode selector chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ScanModeTab.entries.forEach { tab ->
                FilterChip(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    label = { Text(tab.label) },
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("scan_tab_${tab.name.lowercase()}")
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (activeTab) {
                ScanModeTab.MESSAGE -> {
                    item {
                        SectionCard(title = "Message Investigator (English · Hindi · Hinglish)") {
                            Text(
                                text = "Paste any SMS, forwarded message, or WhatsApp text shared here. Analysis runs offline on your phone.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = uiState.messageInput,
                                onValueChange = viewModel::updateMessageInput,
                                label = { Text("Paste message to inspect") },
                                placeholder = { Text("e.g. Dear customer, your SBI KYC will be blocked...") },
                                minLines = 5,
                                maxLines = 10,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("message_input_field")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val clip = ShareIntentsHelper.readFromClipboard(context)
                                        if (clip.isNotBlank()) viewModel.updateMessageInput(clip)
                                        else viewModel.showToast("Clipboard is empty.")
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = metrics.minTouchTarget)
                                ) {
                                    Icon(Icons.Filled.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Paste")
                                }
                                OutlinedButton(
                                    onClick = onNavigateToSimulationLab,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = metrics.minTouchTarget)
                                ) {
                                    Text("Load Demo")
                                }
                                if (uiState.messageInput.isNotEmpty()) {
                                    OutlinedButton(
                                        onClick = { viewModel.updateMessageInput("") },
                                        modifier = Modifier.heightIn(min = metrics.minTouchTarget)
                                    ) {
                                        Text("Clear")
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            // Language Hint selector
                            Text("Language override:", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "auto" to "Auto Detect",
                                    "en" to "English",
                                    "hi" to "Hindi",
                                    "hinglish" to "Hinglish"
                                ).forEach { (code, label) ->
                                    FilterChip(
                                        selected = uiState.languageHint == code,
                                        onClick = { viewModel.updateLanguageHint(code) },
                                        label = { Text(label) }
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = uiState.unexpectedSenderHint,
                                    onCheckedChange = viewModel::updateUnexpectedSenderHint
                                )
                                Text(
                                    text = "This came from an unexpected/unknown sender",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    viewModel.scanMessage(source = Source.MANUAL) {
                                        onNavigateToResult()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DeepNavy),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = maxOf(metrics.minTouchTarget, 52.dp))
                                    .testTag("scan_message_submit_button")
                            ) {
                                Icon(Icons.Filled.Search, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyze Message Offline")
                            }
                        }
                    }
                }

                ScanModeTab.CONVERSATION -> {
                    item {
                        var addSender by remember { mutableStateOf(ChatSender.THEM) }
                        var addTurnText by remember { mutableStateOf("") }

                        SectionCard(title = "Conversation Trap Detector") {
                            Text(
                                text = "Paste a multi-turn chat ([10:32] Name: text or Them:/Me:), or add messages turn by turn to track kill-chain escalation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = uiState.conversationRawInput,
                                onValueChange = viewModel::updateConversationRawInput,
                                label = { Text("Paste multi-message transcript") },
                                minLines = 4,
                                maxLines = 8,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("conversation_raw_input")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = viewModel::splitConversationFromRaw,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text("Auto-Split")
                                }
                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateConversationRawInput(DemoSamples.digitalArrestConversationText)
                                        viewModel.splitConversationFromRaw()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                ) {
                                    Text("Digital Arrest Demo")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Or add a chat turn manually:", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = addSender == ChatSender.THEM,
                                    onClick = { addSender = ChatSender.THEM },
                                    label = { Text("Sender: Them") }
                                )
                                FilterChip(
                                    selected = addSender == ChatSender.ME,
                                    onClick = { addSender = ChatSender.ME },
                                    label = { Text("Sender: Me") }
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = addTurnText,
                                    onValueChange = { addTurnText = it },
                                    label = { Text("Next turn text") },
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        viewModel.addConversationTurn(addSender, addTurnText)
                                        addTurnText = ""
                                    },
                                    modifier = Modifier.heightIn(min = 52.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Add message turn")
                                }
                            }
                        }
                    }

                    if (uiState.conversationMessages.isNotEmpty()) {
                        items(uiState.conversationMessages, key = { it.id }) { msg ->
                            Surface(
                                color = if (msg.sender == ChatSender.THEM) RakshakColors.SurfaceMuted else RakshakColors.TealSoft,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = listOfNotNull(msg.senderLabel, msg.timestampLabel).joinToString(" · "),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(text = msg.text, style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { viewModel.deleteConversationTurn(msg.id) }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete turn")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = viewModel::clearConversation,
                                modifier = Modifier.heightIn(min = 50.dp)
                            ) {
                                Text("Clear")
                            }
                            Button(
                                onClick = {
                                    viewModel.analyzeConversation {
                                        onNavigateToConversationTimeline()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DeepNavy),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 52.dp)
                                    .testTag("analyze_conversation_button")
                            ) {
                                Text("Analyze Timeline & Next Move")
                            }
                        }
                    }
                }

                ScanModeTab.LINK -> {
                    item {
                        val brands = listOf(
                            null to "Auto / None",
                            "sbi" to "SBI",
                            "hdfc" to "HDFC",
                            "icici" to "ICICI",
                            "paytm" to "Paytm",
                            "amazon" to "Amazon",
                            "indiapost" to "India Post",
                            "uidai" to "UIDAI"
                        )
                        SectionCard(title = "Link & Brand Impersonation Investigator") {
                            Text(
                                text = "RakshakAI never opens links automatically. HTTPS is never treated as proof of safety.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = uiState.linkInput,
                                onValueChange = viewModel::updateLinkInput,
                                label = { Text("Paste URL to inspect") },
                                placeholder = { Text("https://sbi.co.in.secure-login.attacker.test/kyc") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("link_input_field")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Optional: Claimed brand in message", style = MaterialTheme.typography.labelLarge)
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                brands.forEach { (id, label) ->
                                    FilterChip(
                                        selected = uiState.linkClaimedBrandId == id,
                                        onClick = { viewModel.updateLinkClaimedBrand(id) },
                                        label = { Text(label) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = { viewModel.scanLink { onNavigateToResult() } },
                                colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DeepNavy),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .testTag("scan_link_button")
                            ) {
                                Text("Inspect Link Safely")
                            }
                        }
                    }
                    item {
                        SectionCard(title = "Try Link Demos") {
                            DemoSamples.linkDemos.forEach { sample ->
                                TextButton(
                                    onClick = { viewModel.updateLinkInput(sample.payload) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("${sample.title} [${sample.expectedBadge}]", modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                ScanModeTab.SCREENSHOT -> {
                    item {
                        val photoPicker = rememberLauncherForActivityResult(
                            ActivityResultContracts.PickVisualMedia()
                        ) { uri ->
                            if (uri != null) viewModel.inspectScreenshotUri(uri)
                        }

                        SectionCard(title = "Screenshot & Image Investigator (Bundled Offline OCR)") {
                            Text(
                                text = "Uses zero-permission Android Photo Picker and bundled Latin + Devanagari ML Kit OCR. Images are NOT saved unless you save them to the Encrypted Vault.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.TealPrimary),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                            ) {
                                Icon(Icons.Filled.Image, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Choose Screenshot (English / Hindi)")
                            }

                            if (uiState.isProcessingOcr) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Running offline OCR & QR detection...")
                                }
                            }

                            if (uiState.ocrEmptyWarning) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "OCR could not extract readable text from that image. You can type or paste the text below.",
                                    color = RakshakColors.CautionAmber,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            if (uiState.ocrQrPayloads.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("QR code found inside screenshot:", style = MaterialTheme.typography.labelLarge)
                                uiState.ocrQrPayloads.forEach { qr ->
                                    OutlinedButton(
                                        onClick = { viewModel.scanQrPayload(qr) { onNavigateToResult() } },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Inspect QR: ${qr.take(40)}")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = uiState.ocrExtractedText,
                                onValueChange = viewModel::updateOcrText,
                                label = { Text("Extracted text (editable)") },
                                minLines = 4,
                                maxLines = 8,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    viewModel.scanMessage(
                                        textOverride = uiState.ocrExtractedText,
                                        source = Source.SCREENSHOT
                                    ) { onNavigateToResult() }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DeepNavy),
                                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                            ) {
                                Text("Analyze Extracted Text")
                            }
                        }
                    }
                }

                ScanModeTab.CALL -> {
                    item {
                        SectionCard(title = "Live Call Scam Shield (Transcript / Scripted)") {
                            SimulatedDemoBadge(
                                text = "TRANSCRIPT / SIMULATED — RakshakAI does not record or listen to real calls."
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = uiState.callTranscriptInput,
                                onValueChange = viewModel::updateCallTranscriptInput,
                                label = { Text("Paste call transcript or speaker notes") },
                                minLines = 5,
                                maxLines = 10,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = viewModel::runScriptedDemoCall,
                                    enabled = !uiState.isCallDemoRunning,
                                    modifier = Modifier.weight(1f).heightIn(min = 50.dp)
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (uiState.isCallDemoRunning) "Playing..." else "Scripted Call Demo")
                                }
                                Button(
                                    onClick = { viewModel.analyzeCallTranscript { onNavigateToResult() } },
                                    colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DeepNavy),
                                    modifier = Modifier.weight(1f).heightIn(min = 50.dp)
                                ) {
                                    Text("Analyze Transcript")
                                }
                            }
                            if (uiState.callDemoRevealedLines.isNotEmpty() && uiState.latestConversationResult != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                val conv = uiState.latestConversationResult
                                Surface(
                                    color = RakshakColors.DangerSoft,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Live Demo Kill-Chain Risk: ${conv.cumulativeScore}/100 · Stage ${conv.currentStage}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = RakshakColors.CriticalDarkRed
                                        )
                                        Text(
                                            text = "Next likely move: ${conv.nextMoves.firstOrNull()?.title ?: "Escalating pressure"}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
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
}
