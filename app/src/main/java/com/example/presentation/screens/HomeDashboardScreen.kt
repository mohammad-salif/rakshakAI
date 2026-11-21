package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.database.ScanHistoryEntity
import com.example.domain.models.RiskLevel
import com.example.presentation.RakshakUiState
import com.example.presentation.components.SectionCard
import com.example.presentation.components.riskColor
import com.example.presentation.components.riskIcon
import com.example.presentation.components.riskSoftContainer
import com.example.presentation.theme.LocalRakshakMetrics
import com.example.presentation.theme.RakshakColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    uiState: RakshakUiState,
    onOpenScanTab: (String) -> Unit,
    onOpenEmergency: () -> Unit,
    onOpenSafetyCentre: () -> Unit,
    onOpenPrivacyCentre: () -> Unit,
    onOpenPreActionCheck: () -> Unit,
    onOpenSimulationLab: () -> Unit,
    onOpenVaultHistory: () -> Unit
) {
    val metrics = LocalRakshakMetrics.current
    val isHindi = uiState.settings.appLanguage.startsWith("hi")
    val recentThree = uiState.history.take(3)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Status Hero Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RakshakColors.DeepNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPrivacyCentre)
                    .testTag("home_status_card")
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = RakshakColors.TealPrimary.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = "Protected",
                                tint = Color(0xFF7CE8E2),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_status_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_status_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC8D7EA)
                        )
                        if (uiState.installedRemoteApps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = RakshakColors.CautionAmber,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Remote tool on device: ${uiState.installedRemoteApps.joinToString(", ")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Black,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Cross-signal elevated banner when remote app + scam context combine
        uiState.crossSignalReport.remoteAccessWarning?.let { warning ->
            item {
                SectionCard(
                    containerColor = RakshakColors.DangerSoft,
                    borderColor = RakshakColors.DangerRed
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Warning",
                            tint = RakshakColors.DangerRed
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Cross-Signal Alert",
                                style = MaterialTheme.typography.titleSmall,
                                color = RakshakColors.CriticalDarkRed
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodyMedium,
                                color = RakshakColors.InkPrimary
                            )
                        }
                    }
                }
            }
        }

        // Red Emergency Button
        item {
            Button(
                onClick = onOpenEmergency,
                colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DangerRed),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxOf(metrics.minTouchTarget, 56.dp))
                    .testTag("home_emergency_button")
            ) {
                Icon(Icons.Filled.Emergency, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.home_emergency_cta),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Quick Scan 2x2 Grid
        item {
            Text(
                text = stringResource(R.string.home_quick_actions),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionTile(
                    title = stringResource(R.string.home_action_message),
                    subtitle = stringResource(R.string.home_action_message_desc),
                    icon = Icons.AutoMirrored.Filled.Chat,
                    accent = RakshakColors.DeepNavy,
                    onClick = { onOpenScanTab("MESSAGE") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_scan_message")
                )
                QuickActionTile(
                    title = stringResource(R.string.home_action_link),
                    subtitle = stringResource(R.string.home_action_link_desc),
                    icon = Icons.Filled.Link,
                    accent = RakshakColors.TealPrimary,
                    onClick = { onOpenScanTab("LINK") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_scan_link")
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionTile(
                    title = stringResource(R.string.home_action_screenshot),
                    subtitle = stringResource(R.string.home_action_screenshot_desc),
                    icon = Icons.Filled.ImageSearch,
                    accent = Color(0xFF3B82F6),
                    onClick = { onOpenScanTab("SCREENSHOT") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_scan_screenshot")
                )
            }
        }

        // Pre-Action Check + Simulation Lab Shortcuts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenPreActionCheck,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = metrics.minTouchTarget)
                        .testTag("home_pre_action_btn")
                ) {
                    Text(if (isHindi) "Pre-Action जांच" else "Before I Pay/Click")
                }
                OutlinedButton(
                    onClick = onOpenSimulationLab,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = metrics.minTouchTarget)
                        .testTag("home_sim_lab_btn")
                ) {
                    Text(if (isHindi) "डेमो प्रयोगशाला" else "Simulation Lab")
                }
            }
        }

        // Recent Scans (last 3 summary-only)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.home_recent_scans),
                    style = MaterialTheme.typography.titleMedium
                )
                if (uiState.history.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onOpenVaultHistory,
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(if (isHindi) "सभी देखें" else "View All")
                    }
                }
            }
        }

        if (recentThree.isEmpty()) {
            item {
                SectionCard {
                    Text(
                        text = if (isHindi) "अभी तक कोई स्कैन नहीं। ऊपर किसी टूल पर टैप करें या Simulation Lab से डेमो आज़माएं।"
                        else "No recent scans yet. Tap any quick scan tool above or try built-in samples in Simulation Lab.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(recentThree, key = { it.scanId }) { item ->
                RecentHistorySummaryCard(item = item)
            }
        }

        // Safety Centre Shortcut
        item {
            Card(
                onClick = onOpenSafetyCentre,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = RakshakColors.TealSoft),
                border = BorderStroke(1.dp, RakshakColors.TealPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .testTag("home_safety_centre_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.School,
                        contentDescription = null,
                        tint = RakshakColors.DeepNavy,
                        modifier = Modifier.size(30.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.home_safety_centre),
                            style = MaterialTheme.typography.titleSmall,
                            color = RakshakColors.DeepNavy
                        )
                        Text(
                            text = if (isHindi) "आम भारतीय ठगी के तरीके, सुनहरे नियम और क्विज़" else "Indian scam lessons, Golden Rules & Scam-or-Safe Quiz",
                            style = MaterialTheme.typography.bodySmall,
                            color = RakshakColors.InkSecondary
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun QuickActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = LocalRakshakMetrics.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.heightIn(min = if (metrics.elderlyMode) 128.dp else 112.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Surface(
                color = accent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = title, tint = accent)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RecentHistorySummaryCard(item: ScanHistoryEntity) {
    val level = runCatching { RiskLevel.valueOf(item.riskLevel) }.getOrDefault(RiskLevel.MEDIUM)
    val badgeColor = riskColor(level, item.uncertain)
    val badgeBg = riskSoftContainer(level, item.uncertain)
    val timeStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(item.timestamp))

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = badgeBg,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(end = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = riskIcon(level, item.uncertain),
                        contentDescription = level.name,
                        tint = badgeColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.riskScore}",
                        style = MaterialTheme.typography.labelLarge,
                        color = badgeColor
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${item.categoryName} · ${item.source}",
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.shortExplanation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = RakshakColors.InkSecondary
                )
            }
        }
    }
}
