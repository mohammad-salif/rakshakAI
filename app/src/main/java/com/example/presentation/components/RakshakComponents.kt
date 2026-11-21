package com.example.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.models.Evidence
import com.example.domain.models.RiskLevel
import com.example.presentation.theme.LocalRakshakMetrics
import com.example.presentation.theme.RakshakColors

enum class MainBottomTab {
    HOME, SCAN, PROTECT, VAULT, MORE
}

fun riskColor(level: RiskLevel, uncertain: Boolean = false): Color {
    return when {
        uncertain && level != RiskLevel.CRITICAL && level != RiskLevel.HIGH -> RakshakColors.CautionAmber
        level == RiskLevel.CRITICAL -> RakshakColors.DangerRed
        level == RiskLevel.HIGH -> RakshakColors.DangerRed
        level == RiskLevel.MEDIUM -> RakshakColors.CautionAmber
        else -> RakshakColors.SafeGreen
    }
}

fun riskSoftContainer(level: RiskLevel, uncertain: Boolean = false): Color {
    return when {
        uncertain && level != RiskLevel.CRITICAL && level != RiskLevel.HIGH -> RakshakColors.CautionSoft
        level == RiskLevel.CRITICAL || level == RiskLevel.HIGH -> RakshakColors.DangerSoft
        level == RiskLevel.MEDIUM -> RakshakColors.CautionSoft
        else -> RakshakColors.SafeSoft
    }
}

fun riskIcon(level: RiskLevel, uncertain: Boolean = false): ImageVector {
    return when {
        uncertain && level != RiskLevel.CRITICAL && level != RiskLevel.HIGH -> Icons.Filled.Warning
        level == RiskLevel.CRITICAL || level == RiskLevel.HIGH -> Icons.Filled.Error
        level == RiskLevel.MEDIUM -> Icons.Filled.Warning
        else -> Icons.Filled.CheckCircle
    }
}

fun riskLabelText(level: RiskLevel, uncertain: Boolean, isHindi: Boolean): String {
    if (uncertain && level != RiskLevel.CRITICAL && level != RiskLevel.HIGH) {
        return if (isHindi) "अनिश्चित / पर्याप्त सबूत नहीं" else "Uncertain / Insufficient Evidence"
    }
    return when (level) {
        RiskLevel.CRITICAL -> if (isHindi) "अत्यधिक जोखिम (CRITICAL)" else "Critical Risk"
        RiskLevel.HIGH -> if (isHindi) "उच्च जोखिम (HIGH)" else "High Risk"
        RiskLevel.MEDIUM -> if (isHindi) "मध्यम जोखिम (MEDIUM)" else "Medium Risk"
        RiskLevel.LOW -> if (isHindi) "कम जोखिम (गारंटी नहीं)" else "Low Risk (Not a guarantee)"
    }
}

@Composable
fun RakshakTopBar(
    subtitle: String,
    elderlyMode: Boolean,
    onToggleLanguage: () -> Unit,
    currentLanguage: String
) {
    Surface(
        color = RakshakColors.DeepNavy,
        contentColor = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_kavach_logo),
                contentDescription = "RakshakAI Shield Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RakshakAI",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFB6C6DD),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (elderlyMode) {
                Surface(
                    color = RakshakColors.TealPrimary,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = "ELDERLY",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Surface(
                color = Color.White.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onToggleLanguage)
                    .testTag("topbar_language_toggle")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (currentLanguage.startsWith("hi")) "EN" else "हिन्दी",
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun RakshakSubTopBar(
    title: String,
    onBackClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    Surface(
        color = RakshakColors.DeepNavy,
        contentColor = Color.White,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("subbar_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = Color.White
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onHomeClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("subbar_home_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Go to Home",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun RakshakBottomNavBar(
    selectedTab: MainBottomTab,
    onSelectTab: (MainBottomTab) -> Unit
) {
    val navItems = listOf(
        Triple(MainBottomTab.HOME, stringResource(R.string.tab_home), Icons.Filled.Home),
        Triple(MainBottomTab.SCAN, stringResource(R.string.tab_scan), Icons.Filled.Search),
        Triple(MainBottomTab.PROTECT, stringResource(R.string.tab_protect), Icons.Filled.Shield),
        Triple(MainBottomTab.VAULT, stringResource(R.string.tab_vault), Icons.Filled.Lock),
        Triple(MainBottomTab.MORE, stringResource(R.string.tab_more), Icons.Filled.Menu)
    )
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        navItems.forEach { (tab, label, icon) ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = RakshakColors.TealPrimary,
                    selectedTextColor = RakshakColors.DeepNavy,
                    indicatorColor = RakshakColors.TealSoft,
                    unselectedIconColor = RakshakColors.InkSecondary,
                    unselectedTextColor = RakshakColors.InkSecondary
                ),
                modifier = Modifier.testTag("bottom_tab_${tab.name.lowercase()}")
            )
        }
    }
}

@Composable
fun SimulatedDemoBadge(
    text: String = "SIMULATED / DEMO",
    modifier: Modifier = Modifier
) {
    Surface(
        color = RakshakColors.CautionSoft,
        border = BorderStroke(1.dp, RakshakColors.CautionAmber),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            color = Color(0xFF92400E),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun RiskRingGauge(
    score: Int,
    level: RiskLevel,
    uncertain: Boolean,
    sizeDp: Int = 112
) {
    val metrics = LocalRakshakMetrics.current
    val targetFraction = (score.coerceIn(0, 100) / 100f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = if (metrics.reducedMotion) 0 else 750),
        label = "risk_gauge_sweep"
    )
    val ringColor = riskColor(level, uncertain)
    val trackColor = ringColor.copy(alpha = 0.16f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(sizeDp.dp)
            .semantics {
                contentDescription = "Risk score $score out of 100, level ${level.name}"
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * animatedFraction,
                useCenter = false,
                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = riskIcon(level, uncertain),
                contentDescription = level.name,
                tint = ringColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "$score",
                style = MaterialTheme.typography.headlineSmall,
                color = ringColor
            )
            Text(
                text = "/100",
                style = MaterialTheme.typography.labelSmall,
                color = RakshakColors.InkSecondary
            )
        }
    }
}

@Composable
fun HighlightedEvidenceText(
    sourceText: String,
    evidence: List<Evidence>,
    modifier: Modifier = Modifier
) {
    val spans = evidence.mapNotNull { ev ->
        val s = ev.start
        val e = ev.end
        if (s != null && e != null && s >= 0 && e <= sourceText.length && s < e) {
            s to e
        } else null
    }.sortedBy { it.first }

    val annotated = buildAnnotatedString {
        var cursor = 0
        for ((start, end) in spans) {
            if (start > cursor) {
                append(sourceText.substring(cursor, start))
            }
            if (end > cursor) {
                val realStart = maxOf(cursor, start)
                withStyle(
                    SpanStyle(
                        background = RakshakColors.DangerSoft,
                        color = RakshakColors.CriticalDarkRed,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append(sourceText.substring(realStart, end))
                }
                cursor = end
            }
        }
        if (cursor < sourceText.length) {
            append(sourceText.substring(cursor))
        }
    }

    Text(
        text = annotated,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    )
}

@Composable
fun KillChainStepperBar(
    stagesReached: List<Int>,
    currentStage: Int,
    onStageSelected: (Int) -> Unit
) {
    val stageLabels = listOf(
        1 to "S1 Hook",
        2 to "S2 Authority",
        3 to "S3 Fear",
        4 to "S4 Isolation",
        5 to "S5 Action",
        6 to "S6 Pay",
        7 to "S7 Escalate"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stageLabels.forEach { (stageNum, label) ->
            val reached = stageNum in stagesReached
            val isCurrent = stageNum == currentStage
            val isNextLikely = currentStage in 1..6 && stageNum == currentStage + 1

            val bg = when {
                isCurrent -> RakshakColors.DangerRed
                reached -> RakshakColors.DeepNavy
                isNextLikely -> RakshakColors.CautionSoft
                else -> RakshakColors.SurfaceMuted
            }
            val fg = when {
                isCurrent || reached -> Color.White
                isNextLikely -> Color(0xFF92400E)
                else -> RakshakColors.InkSecondary
            }

            Surface(
                color = bg,
                shape = RoundedCornerShape(14.dp),
                border = if (isNextLikely) BorderStroke(1.dp, RakshakColors.CautionAmber) else null,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable { onStageSelected(stageNum) }
                    .testTag("killchain_stage_$stageNum")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = fg,
                        fontWeight = if (isCurrent || reached) FontWeight.Bold else FontWeight.Medium
                    )
                    if (isNextLikely) {
                        Text(
                            text = "Likely next",
                            style = MaterialTheme.typography.labelSmall,
                            color = fg
                        )
                    } else if (isCurrent) {
                        Text(
                            text = "Current",
                            style = MaterialTheme.typography.labelSmall,
                            color = fg
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionCard(
    title: String? = null,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            content()
        }
    }
}
