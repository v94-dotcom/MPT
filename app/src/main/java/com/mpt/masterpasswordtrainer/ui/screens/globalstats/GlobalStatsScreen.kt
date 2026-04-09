package com.mpt.masterpasswordtrainer.ui.screens.globalstats

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.mpt.masterpasswordtrainer.ui.components.getIconForName
import com.mpt.masterpasswordtrainer.ui.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalStatsScreen(
    navController: NavHostController,
    viewModel: GlobalStatsViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadStats()
    }

    val stats by viewModel.stats.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            stats == null || stats!!.totalEntries == 0 -> {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No stats yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Complete your first challenge\nto start tracking stats",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                val s = stats!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // === KEY METRICS ===
                    KeyMetricsSection(s)

                    Spacer(modifier = Modifier.height(28.dp))

                    // === WEEKLY ACTIVITY ===
                    Text(
                        text = "Weekly Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    WeeklyActivityChart(weeklyActivity = s.weeklyActivity)

                    Spacer(modifier = Modifier.height(28.dp))

                    // === MONTHLY TREND ===
                    Text(
                        text = "Monthly Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    MonthlyTrendChart(monthlyTrend = s.monthlyTrend)

                    Spacer(modifier = Modifier.height(28.dp))

                    // === PER-ENTRY COMPARISON ===
                    if (s.entryComparisons.isNotEmpty()) {
                        Text(
                            text = "Per-Entry Comparison",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        s.entryComparisons.forEach { comparison ->
                            EntryComparisonRow(
                                comparison = comparison,
                                onClick = {
                                    navController.navigate(
                                        Routes.entryDetail(comparison.entry.id)
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // === FUN STATS ===
                    if (s.totalChecks > 0) {
                        FunStatsSection(funStats = s.funStats)
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun KeyMetricsSection(stats: GlobalStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KeyMetricCard(
            value = "${stats.totalChecks}",
            label = "Total Checks",
            modifier = Modifier.weight(1f)
        )
        KeyMetricCard(
            value = "${(stats.globalSuccessRate * 100).toInt()}%",
            label = "Success Rate",
            modifier = Modifier.weight(1f),
            showProgress = true,
            progress = stats.globalSuccessRate
        )
        KeyMetricCard(
            value = "${stats.bestActiveStreak}",
            label = if (stats.bestActiveStreakService.isNotEmpty())
                stats.bestActiveStreakService else "Active Streak",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun KeyMetricCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false,
    progress: Float = 0f
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progress"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showProgress) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(44.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeWidth = 4.dp
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun WeeklyActivityChart(weeklyActivity: List<DayActivity>) {
    val successColor = Color(0xFF43A047)
    val failureColor = Color(0xFFE53935)
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    val maxTotal = weeklyActivity.maxOfOrNull { it.successes + it.failures } ?: 1
    val chartMax = maxOf(maxTotal, 1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp)
    ) {
        val barCount = weeklyActivity.size
        val barWidth = size.width / (barCount * 2f)
        val chartHeight = size.height - 24.dp.toPx() // leave space for labels
        val labelY = size.height - 4.dp.toPx()

        weeklyActivity.forEachIndexed { index, day ->
            val centerX = (index * 2 + 1) * barWidth
            val total = day.successes + day.failures

            if (total == 0) {
                // Gray stub for empty days
                val stubHeight = 4.dp.toPx()
                drawRoundRect(
                    color = emptyColor,
                    topLeft = Offset(centerX - barWidth / 2, chartHeight - stubHeight),
                    size = Size(barWidth, stubHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            } else {
                val totalHeight = (total.toFloat() / chartMax) * chartHeight
                val successHeight = (day.successes.toFloat() / chartMax) * chartHeight
                val failureHeight = totalHeight - successHeight

                // Failure portion (top of stacked bar)
                if (day.failures > 0) {
                    drawRoundRect(
                        color = failureColor,
                        topLeft = Offset(centerX - barWidth / 2, chartHeight - totalHeight),
                        size = Size(barWidth, failureHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                }

                // Success portion (bottom of stacked bar)
                if (day.successes > 0) {
                    drawRoundRect(
                        color = successColor,
                        topLeft = Offset(
                            centerX - barWidth / 2,
                            chartHeight - successHeight
                        ),
                        size = Size(barWidth, successHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                    )
                }
            }

            // Day label
            val labelStyle = TextStyle(
                fontSize = 10.sp,
                color = labelColor,
                textAlign = TextAlign.Center
            )
            val measured = textMeasurer.measure(day.label, labelStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    centerX - measured.size.width / 2f,
                    labelY - measured.size.height / 2f
                )
            )
        }
    }
}

@Composable
private fun MonthlyTrendChart(monthlyTrend: List<MonthActivity>) {
    val barColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val textMeasurer = rememberTextMeasurer()

    val maxTotal = monthlyTrend.maxOfOrNull { it.total } ?: 1
    val chartMax = maxOf(maxTotal, 1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp)
    ) {
        val barCount = monthlyTrend.size
        val barWidth = size.width / (barCount * 2f)
        val chartHeight = size.height - 24.dp.toPx()
        val labelY = size.height - 4.dp.toPx()

        monthlyTrend.forEachIndexed { index, month ->
            val centerX = (index * 2 + 1) * barWidth

            if (month.total == 0) {
                val stubHeight = 4.dp.toPx()
                drawRoundRect(
                    color = emptyColor,
                    topLeft = Offset(centerX - barWidth / 2, chartHeight - stubHeight),
                    size = Size(barWidth, stubHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            } else {
                val barHeight = (month.total.toFloat() / chartMax) * chartHeight
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(centerX - barWidth / 2, chartHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                )
            }

            // Month label
            val labelStyle = TextStyle(
                fontSize = 10.sp,
                color = labelColor,
                textAlign = TextAlign.Center
            )
            val measured = textMeasurer.measure(month.label, labelStyle)
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(
                    centerX - measured.size.width / 2f,
                    labelY - measured.size.height / 2f
                )
            )
        }
    }
}

@Composable
private fun EntryComparisonRow(
    comparison: EntryComparison,
    onClick: () -> Unit
) {
    val accentColor = Color(comparison.entry.serviceColor)
    val icon = getIconForName(comparison.entry.serviceIcon)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Service icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = comparison.entry.serviceName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { comparison.successRate },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = accentColor,
                trackColor = accentColor.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${(comparison.successRate * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = "${comparison.entry.streak} streak",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}

@Composable
private fun FunStatsSection(funStats: FunStats) {
    Text(
        text = "Highlights",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(12.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (funStats.mostPracticedName.isNotEmpty()) {
            FunStatRow(
                icon = Icons.Filled.Star,
                iconColor = Color(0xFFFB8C00),
                label = "Most practiced",
                value = "${funStats.mostPracticedName} (${funStats.mostPracticedCount})"
            )
        }
        if (funStats.mostReliableName.isNotEmpty()) {
            FunStatRow(
                icon = Icons.Filled.EmojiEvents,
                iconColor = Color(0xFF43A047),
                label = "Most reliable",
                value = "${funStats.mostReliableName} (${(funStats.mostReliableRate * 100).toInt()}%)"
            )
        }
        if (funStats.needsWorkName.isNotEmpty()) {
            FunStatRow(
                icon = Icons.AutoMirrored.Filled.TrendingDown,
                iconColor = Color(0xFFE53935),
                label = "Needs work",
                value = "${funStats.needsWorkName} (${(funStats.needsWorkRate * 100).toInt()}%)"
            )
        }
        if (funStats.totalDaysTrained > 0) {
            FunStatRow(
                icon = Icons.Filled.CalendarMonth,
                iconColor = MaterialTheme.colorScheme.primary,
                label = "Total days trained",
                value = "${funStats.totalDaysTrained}"
            )
        }
        if (funStats.longestGapDays > 0 && funStats.longestGapService.isNotEmpty()) {
            FunStatRow(
                icon = Icons.Filled.Timer,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                label = "Longest gap",
                value = "${funStats.longestGapDays} days (${funStats.longestGapService})"
            )
        }
    }
}

@Composable
private fun FunStatRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
