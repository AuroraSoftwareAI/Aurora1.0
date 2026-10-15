package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.GemBlue
import com.example.ui.theme.GemCyan
import com.example.ui.theme.GemLivePulse
import com.example.ui.theme.GemViolet
import kotlin.math.cos
import kotlin.math.sin

enum class VoiceOrbState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

@Composable
fun VoiceVisualizerOrb(
    state: VoiceOrbState,
    amplitude: Float, // 0.0 to 1.0
    modifier: Modifier = Modifier,
    size: Dp = 220.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb_rotation")

    // Slow continuous rotation
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_angle"
    )

    // Breathing pulse for idle
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idle_pulse"
    )

    // Fast ripple for speaking/listening
    val fastRipple by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fast_ripple"
    )

    // Thinking rotation speedup
    val thinkingSpin by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "thinking_spin"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = this.size.minDimension / 2.8f

            val effectiveScale = when (state) {
                VoiceOrbState.IDLE -> idlePulse
                VoiceOrbState.LISTENING -> (1f + amplitude * 0.45f).coerceIn(1f, 1.5f)
                VoiceOrbState.SPEAKING -> (1f + amplitude * 0.35f + (fastRipple - 1f) * 0.2f).coerceIn(0.95f, 1.45f)
                VoiceOrbState.THINKING -> idlePulse
            }

            val currentRadius = baseRadius * effectiveScale

            val coreColors = when (state) {
                VoiceOrbState.IDLE -> listOf(GemCyan.copy(alpha = 0.7f), GemViolet.copy(alpha = 0.8f), GemBlue.copy(alpha = 0.9f))
                VoiceOrbState.LISTENING -> listOf(GemCyan, GemLivePulse, GemViolet)
                VoiceOrbState.THINKING -> listOf(GemViolet, GemCyan, Color(0xFFFFB300))
                VoiceOrbState.SPEAKING -> listOf(GemCyan, GemViolet, Color(0xFFFF4081))
            }

            // Outer ethereal aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        coreColors[0].copy(alpha = 0.35f),
                        coreColors[1].copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = currentRadius * 1.5f
                ),
                radius = currentRadius * 1.5f,
                center = center
            )

            // Dynamic ripples if active
            if (state == VoiceOrbState.LISTENING || state == VoiceOrbState.SPEAKING) {
                drawCircle(
                    color = coreColors[0].copy(alpha = (1.4f - fastRipple).coerceIn(0f, 0.5f)),
                    radius = baseRadius * fastRipple * (1f + amplitude * 0.3f),
                    center = center,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Rotating gradient core
            val rad = Math.toRadians((if (state == VoiceOrbState.THINKING) thinkingSpin else angle).toDouble())
            val offsetVector = Offset(
                x = (cos(rad) * currentRadius * 0.45).toFloat(),
                y = (sin(rad) * currentRadius * 0.45).toFloat()
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = coreColors,
                    center = center + offsetVector,
                    radius = currentRadius
                ),
                radius = currentRadius,
                center = center
            )

            // Inner glowing accent highlight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.85f),
                        Color.White.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = center - offsetVector * 0.4f,
                    radius = currentRadius * 0.55f
                ),
                radius = currentRadius * 0.55f,
                center = center - offsetVector * 0.4f
            )

            // Orbiting satellite dots for thinking state
            if (state == VoiceOrbState.THINKING) {
                for (i in 0..2) {
                    val dotRad = Math.toRadians((thinkingSpin + i * 120).toDouble())
                    val dotPos = Offset(
                        x = center.x + (cos(dotRad) * currentRadius * 1.25).toFloat(),
                        y = center.y + (sin(dotRad) * currentRadius * 1.25).toFloat()
                    )
                    drawCircle(
                        color = GemCyan,
                        radius = 4.dp.toPx(),
                        center = dotPos
                    )
                }
            }
        }
    }
}
