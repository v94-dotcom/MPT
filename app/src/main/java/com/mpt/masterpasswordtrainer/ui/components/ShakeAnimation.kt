package com.mpt.masterpasswordtrainer.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Applies a horizontal shake animation when [trigger] becomes true.
 * Pattern: 0 → -10dp → 10dp → -6dp → 6dp → 0, 400ms total.
 */
@Composable
fun Modifier.shakeAnimation(
    trigger: Boolean,
    onComplete: () -> Unit = {}
): Modifier {
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger) {
            offsetX.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -30f at 80
                    30f at 160
                    -18f at 240
                    18f at 320
                    0f at 400
                }
            )
            onComplete()
        }
    }

    return this.graphicsLayer {
        translationX = offsetX.value
    }
}
