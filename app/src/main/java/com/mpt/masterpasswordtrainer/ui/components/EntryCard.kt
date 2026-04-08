package com.mpt.masterpasswordtrainer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SafetyCheck
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.mpt.masterpasswordtrainer.data.model.PasswordEntry
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/** Maps a service icon name to its Material Icons vector. */
fun getIconForName(name: String): ImageVector {
    return when (name) {
        "Shield" -> Icons.Filled.Shield
        "Key" -> Icons.Filled.Key
        "Lock" -> Icons.Filled.Lock
        "Fingerprint" -> Icons.Filled.Fingerprint
        "Safe" -> Icons.Filled.SafetyCheck
        "Cloud" -> Icons.Filled.Cloud
        "Globe" -> Icons.Filled.Public
        "Star" -> Icons.Filled.Star
        else -> Icons.Filled.Shield
    }
}

/** Masks an email for display: "johnsmith@gmail.com" → "j•••h@gm•••.com". */
fun maskEmail(email: String): String {
    val atIndex = email.indexOf('@')
    if (atIndex < 1) return email

    val local = email.substring(0, atIndex)
    val domain = email.substring(atIndex + 1)
    val dotIndex = domain.lastIndexOf('.')

    if (dotIndex < 2) return email

    val domainName = domain.substring(0, dotIndex)
    val tld = domain.substring(dotIndex) // includes the dot

    val maskedLocal = if (local.length <= 1) {
        "$local•••"
    } else {
        "${local.first()}•••${local.last()}"
    }

    val maskedDomain = if (domainName.length <= 2) {
        "$domainName•••"
    } else {
        "${domainName.take(2)}•••"
    }

    return "$maskedLocal@$maskedDomain$tld"
}

/** Returns the number of whole days since the entry was last verified. */
fun daysSinceLastVerified(entry: PasswordEntry): Int {
    val elapsed = System.currentTimeMillis() - entry.lastVerified
    return TimeUnit.MILLISECONDS.toDays(elapsed).toInt()
}

@Composable
fun EntryCard(
    entry: PasswordEntry,
    maskedEmail: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val daysElapsed = daysSinceLastVerified(entry)
    val status = calculateStatus(daysElapsed, entry.reminderDays)
    val accentColor = Color(entry.serviceColor)
    val icon = getIconForName(entry.serviceIcon)

    val swipeThreshold = with(LocalDensity.current) { 80.dp.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Swipe-behind layer
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp)),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Edit (swipe right reveals on left)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Color(0xFF1E88E5)),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit",
                    tint = Color.White,
                    modifier = Modifier.padding(start = 24.dp)
                )
            }

            // Delete (swipe left reveals on right)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Color(0xFFE53935)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 24.dp)
                )
            }
        }

        // Main card content
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                when {
                                    offsetX.value > swipeThreshold -> {
                                        offsetX.animateTo(0f, tween(200))
                                        onEdit()
                                    }
                                    offsetX.value < -swipeThreshold -> {
                                        offsetX.animateTo(0f, tween(200))
                                        onDelete()
                                    }
                                    else -> {
                                        offsetX.animateTo(0f, tween(200))
                                    }
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f, tween(200)) }
                        }
                    ) { _, dragAmount ->
                        scope.launch {
                            val newValue = (offsetX.value + dragAmount)
                                .coerceIn(-swipeThreshold * 1.5f, swipeThreshold * 1.5f)
                            offsetX.snapTo(newValue)
                        }
                    }
                },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Circular icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = entry.serviceName,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Center: Service name, masked email, status, streak
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = entry.serviceName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = maskedEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusBadge(status = status)
                        if (entry.streak > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = Color(0xFFFB8C00),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "${entry.streak}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFB8C00)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Right: ProgressRing
                ProgressRing(
                    daysElapsed = daysElapsed,
                    totalDays = entry.reminderDays
                )
            }
        }
    }
}
