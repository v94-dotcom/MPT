package com.mpt.masterpasswordtrainer.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class EntryStatus {
    GREEN, AMBER, RED
}

fun calculateStatus(daysElapsed: Int, reminderDays: Int): EntryStatus {
    return when {
        daysElapsed >= reminderDays -> EntryStatus.RED
        daysElapsed >= reminderDays - 1 -> EntryStatus.AMBER
        else -> EntryStatus.GREEN
    }
}

@Composable
fun StatusBadge(
    status: EntryStatus,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, label) = when (status) {
        EntryStatus.GREEN -> Color(0xFF43A047).copy(alpha = 0.15f) to "On track"
        EntryStatus.AMBER -> Color(0xFFFB8C00).copy(alpha = 0.15f) to "Due soon"
        EntryStatus.RED -> Color(0xFFE53935).copy(alpha = 0.15f) to "Overdue"
    }

    val dotColor = when (status) {
        EntryStatus.GREEN -> Color(0xFF43A047)
        EntryStatus.AMBER -> Color(0xFFFB8C00)
        EntryStatus.RED -> Color(0xFFE53935)
    }

    val textColor = when (status) {
        EntryStatus.GREEN -> Color(0xFF43A047)
        EntryStatus.AMBER -> Color(0xFFFB8C00)
        EntryStatus.RED -> Color(0xFFE53935)
    }

    // Pulse animation for overdue badges
    val pulseAlpha = if (status == EntryStatus.RED) {
        val infiniteTransition = rememberInfiniteTransition(label = "overdue_pulse")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
        alpha
    } else {
        1f
    }

    Row(
        modifier = modifier
            .alpha(pulseAlpha)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
