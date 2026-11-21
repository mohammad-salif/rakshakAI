package com.example.presentation.screens

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.data.database.QuarantineEntity
import com.example.data.database.VaultItemEntity
import com.example.integrations.BiometricAuthHelper
import com.example.integrations.findFragmentActivity
import com.example.integrations.share.ShareIntentsHelper
import com.example.presentation.RakshakUiState
import com.example.presentation.RakshakViewModel
import com.example.presentation.components.SectionCard
import com.example.presentation.theme.RakshakColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class VaultSubTab(val label: String) {
    EVIDENCE("Evidence Vault"),
    QUARANTINE("Quarantine"),
    REPLAY("Replay Timeline"),
    HISTORY("Scan History")
}

@Composable
fun VaultTabScreen(
    uiState: RakshakUiState,
    viewModel: RakshakViewModel,
    onRestoredToScanner: () -> Unit
) {
    var activeTab by remember { mutableStateOf(VaultSubTab.EVIDENCE) }
    val context = LocalContext.current
    val activity = context.findFragmentActivity()
    var showExportConfirm by remember { mutableStateOf(false) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }
    var deleteVaultConfirm by remember { mutableStateOf<VaultItemEntity?>(null) }
    var deleteQuarantineConfirm by remember { mutableStateOf<QuarantineEntity?>(null) }

    val vaultLocked = uiState.settings.biometricVaultLock && !uiState.vaultUnlocked

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VaultSubTab.entries.forEach { tab ->
                FilterChip(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    label = { Text(tab.label) },
                    modifier = Modifier.heightIn(min = 48.dp).testTag("vault_tab_${tab.name.lowercase()}")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (activeTab) {
                VaultSubTab.EVIDENCE -> {
                    if (vaultLocked) {
                        item {
                            SectionCard(title = "Evidence Vault Locked") {
                                Text(
                                    text = "Biometric / device-credential protection is enabled for the Vault.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        if (activity != null) {
                                            BiometricAuthHelper.authenticate(
                                                activity = activity,
                                                title = "Unlock RakshakAI Vault",
                                                subtitle = "Verify to decrypt local evidence",
                                                onSuccess = viewModel::unlockVaultSession,
                                                onFailureOrCancel = { viewModel.showToast(it) }
                                            )
                                        } else {
                                            viewModel.unlockVaultSession()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DeepNavy),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                                ) {
                                    Icon(Icons.Filled.Fingerprint, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Unlock Vault")
                                }
                            }
                        }
                    } else {
                        item {
                            SectionCard {
                                Text(
                                    text = "Consent-only AES-256-GCM Encrypted Evidence Vault (Android Keystore). Items are stored locally with per-record random IVs.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (uiState.vaultItems.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { showExportConfirm = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.TealPrimary),
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp)
                                    ) {
                                        Icon(Icons.Filled.Download, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Export Decrypted Evidence ZIP")
                                    }
                                }
                            }
                        }
                        if (uiState.vaultItems.isEmpty()) {
                            item {
                                SectionCard {
                                    Text("No evidence saved yet. Save items from any Scan Result when you want to preserve proof.")
                                }
                            }
                        } else {
                            items(uiState.vaultItems, key = { it.id }) { item ->
                                val dec = remember(item.id) { viewModel.repository.decryptVaultItem(item) }
                                var bmp by remember(item.id) { mutableStateOf<Bitmap?>(null) }
                                LaunchedEffect(item.mediaFileName) {
                                    if (item.mediaFileName != null) {
                                        bmp = viewModel.repository.loadDecryptedScreenshot(item)
                                    }
                                }
                                val timeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.createdAt))
                                SectionCard {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${item.itemType} · ${item.categoryName} (${item.riskScore}/100)",
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { deleteVaultConfirm = item }) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete vault item")
                                        }
                                    }
                                    Text(timeStr, style = MaterialTheme.typography.labelSmall, color = RakshakColors.InkSecondary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(dec.decryptedText, style = MaterialTheme.typography.bodySmall)
                                    if (dec.decryptedNote.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Note: ${dec.decryptedNote}", style = MaterialTheme.typography.labelMedium, color = RakshakColors.TealPrimary)
                                    }
                                    bmp?.let { bitmap ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Decrypted evidence screenshot",
                                            contentScale = ContentScale.FillWidth,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(160.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                VaultSubTab.QUARANTINE -> {
                    item {
                        Surface(
                            color = RakshakColors.CautionSoft,
                            border = BorderStroke(1.dp, RakshakColors.CautionAmber),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Your original SMS/WhatsApp message is not touched. Android doesn't let this app delete messages in other apps. Quarantine stores an encrypted copy inside RakshakAI only.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF78350F),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                    if (uiState.quarantineItems.isEmpty()) {
                        item { SectionCard { Text("Quarantine is empty.") } }
                    } else {
                        items(uiState.quarantineItems, key = { it.id }) { q ->
                            val dec = remember(q.id) { viewModel.repository.decryptQuarantineItem(q) }
                            SectionCard {
                                Text("${q.categoryName} (${q.riskScore}/100)", style = MaterialTheme.typography.titleSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(dec.decryptedText, style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { viewModel.restoreFromQuarantine(q, onRestoredToScanner) },
                                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Filled.Restore, contentDescription = null)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Restore to Scan")
                                    }
                                    OutlinedButton(
                                        onClick = { deleteQuarantineConfirm = q },
                                        modifier = Modifier.heightIn(min = 48.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete quarantined item")
                                    }
                                }
                            }
                        }
                    }
                }

                VaultSubTab.REPLAY -> {
                    item {
                        SectionCard(title = "Scam Replay Timeline") {
                            Text(
                                text = "Evidence items saved with the same Incident title are grouped chronologically to show escalation across stages and risk growth.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (uiState.incidents.isEmpty()) {
                        item {
                            SectionCard {
                                Text("No grouped incidents yet. Save scan results to the Vault to build an Incident Replay Timeline.")
                            }
                        }
                    } else {
                        items(uiState.incidents, key = { it.id }) { incident ->
                            val linked = uiState.vaultItems.filter { it.incidentId == incident.id }.sortedBy { it.createdAt }
                            SectionCard(title = "${incident.title} · Peak ${incident.highestRiskScore}/100") {
                                if (linked.isEmpty()) {
                                    Text("No linked evidence items.", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    linked.forEachIndexed { idx, ev ->
                                        val time = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date(ev.createdAt))
                                        Surface(
                                            color = RakshakColors.SurfaceMuted,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(
                                                    text = "Step ${idx + 1} · $time · Risk ${ev.riskScore}/100",
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                                if (ev.stagesCsv.isNotBlank()) {
                                                    Text(
                                                        text = "Stages: ${ev.stagesCsv.split(',').joinToString(" → ") { "S$it" }}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = RakshakColors.DangerRed
                                                    )
                                                }
                                                Text(ev.categoryName, style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                VaultSubTab.HISTORY -> {
                    item {
                        SectionCard {
                            Text(
                                text = if (uiState.settings.historyEnabled) {
                                    "History is ON (summary-only: score, category, explanation, timestamp). Raw message text is never stored in history."
                                } else {
                                    "History recording is currently OFF in Settings."
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (uiState.history.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { showClearHistoryConfirm = true },
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("history_clear_all_btn")
                                ) {
                                    Text("Clear All History")
                                }
                            }
                        }
                    }
                    if (uiState.history.isEmpty()) {
                        item { SectionCard { Text("No scan summaries recorded.") } }
                    } else {
                        items(uiState.history, key = { it.scanId }) { h ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    RecentHistorySummaryCard(item = h)
                                }
                                IconButton(
                                    onClick = { viewModel.deleteHistoryItem(h.scanId) },
                                    modifier = Modifier.testTag("delete_history_${h.scanId}")
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete history item")
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showExportConfirm) {
        AlertDialog(
            onDismissRequest = { showExportConfirm = false },
            title = { Text("Export Decrypted Evidence?") },
            text = {
                Text("This creates an unencrypted ZIP containing your saved evidence text, notes, and screenshots so you can share it with police or your bank.")
            },
            confirmButton = {
                Button(onClick = {
                    showExportConfirm = false
                    val runExport: () -> Unit = {
                        viewModel.exportVaultZip { file ->
                            ShareIntentsHelper.shareExportFile(context, file)
                        }
                    }
                    if (activity != null && uiState.settings.biometricVaultLock) {
                        BiometricAuthHelper.authenticate(
                            activity = activity,
                            title = "Confirm Vault Export",
                            subtitle = "Authenticate to export decrypted evidence",
                            onSuccess = runExport,
                            onFailureOrCancel = { viewModel.showToast(it) }
                        )
                    } else {
                        runExport()
                    }
                }) { Text("Export ZIP") }
            },
            dismissButton = {
                TextButton(onClick = { showExportConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearHistoryConfirm) {
        AlertDialog(
            onDismissRequest = { showClearHistoryConfirm = false },
            title = { Text("Clear All Scan History?") },
            text = { Text("This deletes all summary scan history entries from this device.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearHistory()
                    showClearHistoryConfirm = false
                }) { Text("Clear All") }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryConfirm = false }) { Text("Cancel") }
            }
        )
    }

    deleteVaultConfirm?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteVaultConfirm = null },
            title = { Text("Delete Vault Item?") },
            text = { Text("This encrypted evidence item and any attached screenshot will be permanently removed.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteVaultItem(target)
                    deleteVaultConfirm = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteVaultConfirm = null }) { Text("Cancel") }
            }
        )
    }

    deleteQuarantineConfirm?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteQuarantineConfirm = null },
            title = { Text("Delete Quarantined Copy?") },
            text = { Text("Permanently remove this encrypted copy from RakshakAI Quarantine?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteQuarantineItem(target.id)
                    deleteQuarantineConfirm = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteQuarantineConfirm = null }) { Text("Cancel") }
            }
        )
    }
}
