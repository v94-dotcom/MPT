package com.mpt.masterpasswordtrainer.ui.screens.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingPage(
    visual: @Composable () -> Unit,
    headline: String,
    body: String? = null,
    extraContent: (@Composable () -> Unit)? = null,
    isVisible: Boolean = true
) {
    // Staggered entrance animations
    var hasAppeared by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        hasAppeared = isVisible
    }

    val visualAlpha by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0f,
        animationSpec = tween(500, easing = EaseOutCubic),
        label = "visualAlpha"
    )
    val visualScale by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "visualScale"
    )

    val headlineAlpha by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0f,
        animationSpec = tween(500, delayMillis = 200, easing = EaseOutCubic),
        label = "headlineAlpha"
    )
    val headlineOffset by animateFloatAsState(
        targetValue = if (hasAppeared) 0f else 24f,
        animationSpec = tween(500, delayMillis = 200, easing = EaseOutCubic),
        label = "headlineOffset"
    )

    val bodyAlpha by animateFloatAsState(
        targetValue = if (hasAppeared) 1f else 0f,
        animationSpec = tween(500, delayMillis = 350, easing = EaseOutCubic),
        label = "bodyAlpha"
    )
    val bodyOffset by animateFloatAsState(
        targetValue = if (hasAppeared) 0f else 24f,
        animationSpec = tween(500, delayMillis = 350, easing = EaseOutCubic),
        label = "bodyOffset"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Visual area — top 45%
        Box(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = visualAlpha
                    scaleX = visualScale
                    scaleY = visualScale
                },
            contentAlignment = Alignment.Center
        ) {
            visual()
        }

        // Text area — bottom 55%
        Column(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = headline,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .alpha(headlineAlpha)
                    .graphicsLayer { translationY = headlineOffset }
                    .widthIn(max = 320.dp)
            )

            if (body != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier
                        .alpha(bodyAlpha)
                        .graphicsLayer { translationY = bodyOffset }
                        .widthIn(max = 280.dp)
                )
            }

            if (extraContent != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .alpha(bodyAlpha)
                        .graphicsLayer { translationY = bodyOffset }
                ) {
                    extraContent()
                }
            }
        }
    }
}
