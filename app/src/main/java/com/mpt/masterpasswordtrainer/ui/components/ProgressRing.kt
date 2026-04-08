package com.mpt.masterpasswordtrainer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ProgressRing(
    daysElapsed: Int,
    totalDays: Int,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    strokeWidth: Dp = 4.dp
) {
    val progress = (daysElapsed.toFloat() / totalDays.coerceAtLeast(1)).coerceIn(0f, 1f)
    val daysRemaining = (totalDays - daysElapsed).coerceAtLeast(0)

    val ringColor = when {
        daysElapsed >= totalDays -> Color(0xFFE53935)      // Red — overdue
        daysElapsed >= totalDays - 1 -> Color(0xFFFB8C00)  // Amber — due tomorrow
        else -> Color(0xFF43A047)                           // Green — on track
    }

    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            targetValue = progress,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    val a11yDescription = if (daysElapsed >= totalDays) "Overdue" else "$daysRemaining days remaining"

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = a11yDescription },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)

            // Background track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )

            // Progress arc
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = animatedProgress.value * 360f,
                useCenter = false,
                style = stroke
            )
        }

        Text(
            text = if (daysElapsed >= totalDays) "!" else "$daysRemaining",
            style = if (size > 64.dp) MaterialTheme.typography.titleLarge
                    else MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = ringColor
        )
    }
}
