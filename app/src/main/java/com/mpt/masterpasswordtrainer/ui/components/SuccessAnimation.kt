package com.mpt.masterpasswordtrainer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun SuccessAnimation(
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFF43A047)
) {
    val checkProgress = remember { Animatable(0f) }
    val circleProgress = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0f) }

    // Confetti particles
    val particleCount = 24
    val particleProgress = remember { List(particleCount) { Animatable(0f) } }
    val particleAngles = remember { List(particleCount) { Random.nextFloat() * 360f } }
    val particleDistances = remember { List(particleCount) { 0.6f + Random.nextFloat() * 0.4f } }
    val particleColors = remember {
        List(particleCount) {
            listOf(
                Color(0xFF43A047),
                Color(0xFF66BB6A),
                Color(0xFF81C784),
                Color(0xFFA5D6A7),
                Color(0xFF4CAF50),
                Color(0xFFFFD54F),
                Color(0xFF4FC3F7)
            ).random()
        }
    }

    LaunchedEffect(Unit) {
        // Circle draws first
        launch {
            circleProgress.animateTo(1f, tween(400, easing = LinearEasing))
        }
        // Then checkmark
        delay(300)
        launch {
            checkProgress.animateTo(1f, tween(350, easing = LinearEasing))
        }
        // Scale bounce
        launch {
            scaleAnim.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium))
        }
        // Confetti burst after checkmark
        delay(200)
        particleProgress.forEach { anim ->
            launch {
                anim.animateTo(
                    1f,
                    tween(600 + Random.nextInt(400), easing = LinearEasing)
                )
            }
        }
    }

    Box(modifier = modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2
            val cy = size.height / 2
            val radius = size.minDimension / 2 * 0.8f
            val scale = scaleAnim.value

            // Circle outline
            if (circleProgress.value > 0f) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = 360f * circleProgress.value,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(cx - radius * scale, cy - radius * scale),
                    size = androidx.compose.ui.geometry.Size(radius * 2 * scale, radius * 2 * scale)
                )
            }

            // Checkmark path
            if (checkProgress.value > 0f) {
                val checkPath = Path()
                // Checkmark points relative to center
                val startX = cx - radius * 0.35f * scale
                val startY = cy + radius * 0.05f * scale
                val midX = cx - radius * 0.05f * scale
                val midY = cy + radius * 0.35f * scale
                val endX = cx + radius * 0.4f * scale
                val endY = cy - radius * 0.25f * scale

                val progress = checkProgress.value
                if (progress <= 0.5f) {
                    // First segment: start to mid
                    val t = progress / 0.5f
                    checkPath.moveTo(startX, startY)
                    checkPath.lineTo(
                        startX + (midX - startX) * t,
                        startY + (midY - startY) * t
                    )
                } else {
                    // Full first segment + partial second
                    val t = (progress - 0.5f) / 0.5f
                    checkPath.moveTo(startX, startY)
                    checkPath.lineTo(midX, midY)
                    checkPath.lineTo(
                        midX + (endX - midX) * t,
                        midY + (endY - midY) * t
                    )
                }

                drawPath(
                    path = checkPath,
                    color = accentColor,
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Confetti particles
            for (i in 0 until particleCount) {
                val p = particleProgress[i].value
                if (p > 0f) {
                    val angle = particleAngles[i] * PI.toFloat() / 180f
                    val maxDist = radius * 1.8f * particleDistances[i]
                    val dist = maxDist * p
                    val px = cx + cos(angle) * dist
                    val py = cy + sin(angle) * dist
                    val alpha = (1f - p).coerceIn(0f, 1f)
                    val particleSize = (3.dp.toPx() * (1f - p * 0.5f))

                    rotate(particleAngles[i] * p * 2f, Offset(px, py)) {
                        drawCircle(
                            color = particleColors[i].copy(alpha = alpha),
                            radius = particleSize,
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }
    }
}
