package com.mpt.masterpasswordtrainer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

/**
 * Page 1 — Password text with characters randomly replaced by "?" marks, then reforming. Loops.
 */
@Composable
fun PasswordFadeAnimation(modifier: Modifier = Modifier) {
    val basePassword = "M@ster_P4ss!"
    var displayText by remember { mutableStateOf(basePassword) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    // Animate through phases: reveal → decay → reveal
    var phase by remember { mutableIntStateOf(0) } // 0=visible, 1-12=decaying, 13=fully hidden, 14-26=reforming
    val maxPhase = 26

    LaunchedEffect(Unit) {
        while (true) {
            phase = (phase + 1) % (maxPhase + 1)
            val chars = basePassword.toCharArray()
            val totalChars = chars.size

            when {
                phase == 0 -> {
                    displayText = basePassword
                    delay(1200) // Hold fully visible
                }
                phase <= 12 -> {
                    // Progressively replace chars with "?"
                    val charsToReplace = (totalChars * phase / 12).coerceAtMost(totalChars)
                    val indices = (0 until totalChars).shuffled().take(charsToReplace)
                    indices.forEach { chars[it] = '?' }
                    displayText = String(chars)
                    delay(150)
                }
                phase == 13 -> {
                    displayText = "?".repeat(totalChars)
                    delay(800) // Hold fully hidden
                }
                else -> {
                    // Progressively reform
                    val charsReformed = (totalChars * (phase - 13) / 13).coerceAtMost(totalChars)
                    val allQuestion = "?".repeat(totalChars).toCharArray()
                    val indices = (0 until totalChars).shuffled().take(charsReformed)
                    indices.forEach { allQuestion[it] = basePassword[it] }
                    displayText = String(allQuestion)
                    delay(150)
                }
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "passwordPulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Glow background
        Canvas(modifier = Modifier.size(200.dp)) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = glowAlpha * 0.3f),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension / 2
            )
        }

        Text(
            text = displayText,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = if (displayText.contains('?')) {
                Color(
                    red = primaryColor.red * (1 - glowAlpha) + errorColor.red * glowAlpha,
                    green = primaryColor.green * (1 - glowAlpha) + errorColor.green * glowAlpha,
                    blue = primaryColor.blue * (1 - glowAlpha) + errorColor.blue * glowAlpha,
                    alpha = 1f
                )
            } else {
                primaryColor
            },
            textAlign = TextAlign.Center,
            letterSpacing = 2.sp
        )
    }
}

/**
 * Page 2 — 4 icons in circular arrangement with connecting arrows drawing between them.
 * Calendar → Bell → Keyboard → Checkmark cycle.
 */
@Composable
fun CycleAnimation(modifier: Modifier = Modifier) {
    val icons = listOf(
        Icons.Filled.CalendarMonth,
        Icons.Filled.Notifications,
        Icons.Filled.Keyboard,
        Icons.Filled.CheckCircle
    )
    val labels = listOf("Schedule", "Remind", "Practice", "Verify")
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val infiniteTransition = rememberInfiniteTransition(label = "cycle")

    // Which icon is currently highlighted (cycles 0→1→2→3)
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "cycleProgress"
    )

    val activeIndex = animProgress.toInt() % 4
    val segmentProgress = animProgress - animProgress.toInt() // 0..1 within current segment

    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        val radius = 80.dp

        // Draw connecting arcs
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val r = radius.toPx()

            for (i in 0 until 4) {
                val startAngle = -90.0 + i * 90.0
                val fromX = centerX + r * cos(Math.toRadians(startAngle)).toFloat()
                val fromY = centerY + r * sin(Math.toRadians(startAngle)).toFloat()
                val endAngle = -90.0 + (i + 1) * 90.0
                val toX = centerX + r * cos(Math.toRadians(endAngle)).toFloat()
                val toY = centerY + r * sin(Math.toRadians(endAngle)).toFloat()

                val alpha = when {
                    i < activeIndex -> 0.6f
                    i == activeIndex -> 0.3f + 0.5f * segmentProgress
                    else -> 0.15f
                }

                drawLine(
                    color = primaryColor.copy(alpha = alpha),
                    start = Offset(fromX, fromY),
                    end = Offset(toX, toY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw icons at 4 positions around the circle
        for (i in 0 until 4) {
            val angle = -90.0 + i * 90.0
            val isActive = i == activeIndex
            val isPast = i < activeIndex

            val scale by animateFloatAsState(
                targetValue = if (isActive) 1.2f else 1.0f,
                animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
                label = "iconScale$i"
            )

            val iconAlpha = when {
                isActive -> 1f
                isPast -> 0.7f
                else -> 0.35f
            }

            Box(
                modifier = Modifier
                    .offset(
                        x = (radius.value * cos(Math.toRadians(angle))).dp,
                        y = (radius.value * sin(Math.toRadians(angle))).dp
                    )
                    .scale(scale)
                    .alpha(iconAlpha),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = icons[i],
                        contentDescription = labels[i],
                        modifier = Modifier.size(32.dp),
                        tint = if (isActive) tertiaryColor else primaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = labels[i],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) tertiaryColor else primaryColor.copy(alpha = iconAlpha)
                    )
                }
            }
        }
    }
}

/**
 * Page 3 — Central shield with cloud/wifi/server icons floating toward it and bouncing away.
 */
@Composable
fun ShieldBounceAnimation(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    data class ThreatIcon(
        val icon: ImageVector,
        val startAngle: Double,
        val label: String
    )

    val threats = remember {
        listOf(
            ThreatIcon(Icons.Filled.Cloud, 200.0, "Cloud"),
            ThreatIcon(Icons.Filled.Wifi, 340.0, "Network"),
            ThreatIcon(Icons.Filled.Dns, 70.0, "Server"),
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "shield")

    // Shield pulse
    val shieldScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shieldPulse"
    )

    // Threat approach cycle: 0..1 approach, 1..1.3 bounce back, 1.3..2 fade out
    val threatProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "threatProgress"
    )

    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Shield glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tertiaryColor.copy(alpha = 0.15f * shieldScale),
                        Color.Transparent
                    )
                ),
                radius = size.minDimension / 2.5f
            )
        }

        // Central shield
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = "Shield",
            modifier = Modifier
                .size(64.dp)
                .scale(shieldScale),
            tint = tertiaryColor
        )

        // Threat icons
        for (threat in threats) {
            val maxRadius = 100f
            val minRadius = 35f

            val distance: Float
            val alpha: Float
            val showX: Boolean

            when {
                threatProgress < 1f -> {
                    // Approaching
                    distance = maxRadius - (maxRadius - minRadius) * threatProgress
                    alpha = 0.3f + 0.7f * threatProgress
                    showX = false
                }
                threatProgress < 1.4f -> {
                    // Bounce back with X flash
                    val bounceT = (threatProgress - 1f) / 0.4f
                    distance = minRadius + (maxRadius * 0.4f) * bounceT
                    alpha = 1f - bounceT * 0.5f
                    showX = bounceT < 0.6f
                }
                else -> {
                    // Fade out
                    val fadeT = (threatProgress - 1.4f) / 0.6f
                    distance = minRadius + maxRadius * 0.4f * (1f - fadeT) + maxRadius * 0.6f * fadeT
                    alpha = 0.5f * (1f - fadeT)
                    showX = false
                }
            }

            val x = (distance * cos(Math.toRadians(threat.startAngle))).dp
            val y = (distance * sin(Math.toRadians(threat.startAngle))).dp

            Box(
                modifier = Modifier
                    .offset(x = x, y = y)
                    .alpha(alpha),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = threat.icon,
                    contentDescription = threat.label,
                    modifier = Modifier.size(28.dp),
                    tint = if (showX) errorColor else primaryColor.copy(alpha = 0.6f)
                )
                if (showX) {
                    Text(
                        text = "✕",
                        color = errorColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.offset(x = 14.dp, y = (-10).dp)
                    )
                }
            }
        }
    }
}

/**
 * Page 4 — Shield logo with gentle pulse and radial glow.
 */
@Composable
fun LogoPulseAnimation(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val infiniteTransition = rememberInfiniteTransition(label = "logoPulse")

    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )

    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowRadius"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        // Radial glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = glowAlpha),
                        tertiaryColor.copy(alpha = glowAlpha * 0.3f),
                        Color.Transparent
                    ),
                    radius = size.minDimension * glowRadius
                ),
                radius = size.minDimension / 2
            )
        }

        // Outer ring
        Canvas(modifier = Modifier.size(120.dp)) {
            drawCircle(
                color = primaryColor.copy(alpha = 0.15f),
                radius = size.minDimension / 2,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Shield icon
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = "MPT",
            modifier = Modifier
                .size(72.dp)
                .scale(scale),
            tint = primaryColor
        )

        // Lock overlay on shield
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            modifier = Modifier
                .size(28.dp)
                .scale(scale)
                .offset(y = 2.dp),
            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
        )
    }
}
