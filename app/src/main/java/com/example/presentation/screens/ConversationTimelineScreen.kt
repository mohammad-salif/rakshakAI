package com.example.presentation.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.models.ChatSender
import com.example.presentation.RakshakUiState
import com.example.presentation.RakshakViewModel
import com.example.presentation.components.KillChainStepperBar
import com.example.presentation.components.RiskRingGauge
import com.example.presentation.components.SectionCard
import com.example.presentation.components.riskColor
import com.example.presentation.theme.RakshakColors

@Composable
fun ConversationTimelineScreen(
    uiState: RakshakUiState,
    viewModel: RakshakViewModel,
    onOpenIsolationBreaker: () -> Unit,
    onOpenPreActionCheck: () -> Unit,
    onOpenFullResult: () -> Unit,
    onBack: () -> Unit
) {
    val conv = uiState.latestConversationResult
    if (conv == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("No conversation analyzed yet.")
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onBack) { Text("Back to Conversation Scanner") }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            SectionCard(borderColor = riskColor(conv.riskLevel, conv.uncertain)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RiskRingGauge(
                        score = conv.cumulativeScore,
                        level = conv.riskLevel,
                        uncertain = conv.uncertain
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cumulative Conversation Risk: ${conv.riskLevel.name}",
                            style = MaterialTheme.typography.titleMedium,
                            color = riskColor(conv.riskLevel, conv.uncertain)
                        )
                        Text(
                            text = "${conv.turns.size} messages · ${conv.stagesReached.size} escalation stages reached",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        if (conv.isolationTriggered) {
                            Surface(
                                color = RakshakColors.DangerSoft,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Isolation / High-pressure threshold reached",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = RakshakColors.CriticalDarkRed,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        if (conv.isolationTriggered) {
            item {
                Button(
                    onClick = onOpenIsolationBreaker,
                    colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DangerRed),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("timeline_isolation_cta")
                ) {
                    Text("Open Isolation Breaker & Alert Family")
                }
            }
        }

        item {
            SectionCard(title = "Kill-Chain Progression & Possible Next Move") {
                KillChainStepperBar(
                    stagesReached = conv.stagesReached,
                    currentStage = conv.currentStage,
                    onStageSelected = {}
                )
                Spacer(modifier = Modifier.height(10.dp))
                conv.nextMoves.forEach { move ->
                    Surface(
                        color = RakshakColors.CautionSoft,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "${move.likelihood.name}: ${move.title}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Text(text = move.advice, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Chronological Turn Timeline
        item {
            Text("Chronological Turn Timeline", style = MaterialTheme.typography.titleMedium)
        }

        itemsIndexed(conv.turns) { idx, turn ->
            val isThem = turn.message.sender == ChatSender.THEM
            SectionCard(
                containerColor = if (isThem) MaterialTheme.colorScheme.surface else RakshakColors.TealSoft,
                borderColor = if (turn.scanResult.riskScore >= 60) RakshakColors.DangerRed else MaterialTheme.colorScheme.outline
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${idx + 1} ${turn.message.senderLabel}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Surface(
                        color = riskColor(turn.scanResult.riskLevel, turn.scanResult.uncertain),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (turn.primaryStage > 0) "S${turn.primaryStage}: ${turn.stageName}" else "Turn ${turn.scanResult.riskScore}/100",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = turn.message.text, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Coercion & Pressure Check-in (C11)
        item {
            val coercion = uiState.coercionAssessment ?: conv.coercionAssessment
            val ans = uiState.coercionAnswers
            SectionCard(title = "Coercion & Pressure Estimate (${coercion.pressureScore}/100)") {
                Text(
                    text = coercion.disclaimer,
                    style = MaterialTheme.typography.labelSmall,
                    color = RakshakColors.InkSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = coercion.levelLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (coercion.pressureScore >= 65) RakshakColors.DangerRed else RakshakColors.DeepNavy
                )
                Spacer(modifier = Modifier.height(8.dp))
                listOf(
                    Triple(ans.toldToStayOnCall, "I've been told to stay on the call") { v: Boolean ->
                        viewModel.updateCoercionAnswers(ans.copy(toldToStayOnCall = v))
                    },
                    Triple(ans.toldNotToTellAnyone, "I was told not to tell anyone") { v: Boolean ->
                        viewModel.updateCoercionAnswers(ans.copy(toldNotToTellAnyone = v))
                    },
                    Triple(ans.givenDeadline, "I was given an urgent deadline") { v: Boolean ->
                        viewModel.updateCoercionAnswers(ans.copy(givenDeadline = v))
                    },
                    Triple(ans.threatenedWithArrestOrBlock, "I was threatened with arrest / blocking") { v: Boolean ->
                        viewModel.updateCoercionAnswers(ans.copy(threatenedWithArrestOrBlock = v))
                    }
                ).forEach { (checked, label, setter) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checked, onCheckedChange = setter)
                        Text(label, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onOpenFullResult,
                    modifier = Modifier.weight(1f).heightIn(min = 50.dp)
                ) {
                    Text("Full Result Card")
                }
                Button(
                    onClick = onOpenPreActionCheck,
                    colors = ButtonDefaults.buttonColors(containerColor = RakshakColors.DeepNavy),
                    modifier = Modifier.weight(1f).heightIn(min = 50.dp)
                ) {
                    Text("Pre-Action Check")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}
