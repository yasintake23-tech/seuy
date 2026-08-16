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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CoralLight
import com.example.ui.theme.RoseDark
import com.example.ui.theme.RoseLight
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.WineTertiary
import kotlin.math.sin

@Composable
fun RomanticPulsatingHeart(
    code: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartPulse")
    
    // Smooth breathing / pulsing animation
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heartScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Floating particles
    val particleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particleOffset"
    )

    Box(
        modifier = modifier
            .size(290.dp)
            .testTag("romantic_heart_container"),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing aura
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(scale * 1.15f)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        RoseLight.copy(alpha = glowAlpha),
                        CoralLight.copy(alpha = glowAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = size.width * 0.48f
                ),
                radius = size.width * 0.48f,
                center = center
            )
        }

        // Floating sparkles / micro heart dots
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val rad = Math.toRadians(particleOffset.toDouble())
            val x1 = center.x + (size.width * 0.42f) * sin(rad).toFloat()
            val y1 = center.y + (size.height * 0.42f) * kotlin.math.cos(rad).toFloat()
            drawCircle(
                color = CoralLight.copy(alpha = 0.7f),
                radius = 5.dp.toPx(),
                center = Offset(x1, y1)
            )

            val rad2 = Math.toRadians((particleOffset + 180).toDouble())
            val x2 = center.x + (size.width * 0.38f) * sin(rad2).toFloat()
            val y2 = center.y + (size.height * 0.38f) * kotlin.math.cos(rad2).toFloat()
            drawCircle(
                color = RoseLight.copy(alpha = 0.8f),
                radius = 4.dp.toPx(),
                center = Offset(x2, y2)
            )
        }

        // Main Heart Vector Canvas
        Canvas(
            modifier = Modifier
                .size(240.dp)
                .scale(scale)
        ) {
            val width = size.width
            val height = size.height
            val path = Path().apply {
                moveTo(width / 2f, height * 0.88f)
                // Left curve
                cubicTo(
                    width * 0.05f, height * 0.55f,
                    width * 0.05f, height * 0.15f,
                    width * 0.38f, height * 0.12f
                )
                cubicTo(
                    width * 0.5f, height * 0.12f,
                    width * 0.5f, height * 0.28f,
                    width * 0.5f, height * 0.28f
                )
                // Right curve
                cubicTo(
                    width * 0.5f, height * 0.28f,
                    width * 0.5f, height * 0.12f,
                    width * 0.62f, height * 0.12f
                )
                cubicTo(
                    width * 0.95f, height * 0.15f,
                    width * 0.95f, height * 0.55f,
                    width / 2f, height * 0.88f
                )
                close()
            }

            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        RoseLight,
                        RosePrimary,
                        RoseDark
                    )
                ),
                style = Fill
            )
        }

        // Content inside the heart
        Column(
            modifier = Modifier
                .offset(y = (-6).dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✨",
                fontSize = 18.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Senin Eşleşme Kodun",
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            // Code with letter spacing
            val spacedCode = code.chunked(1).joinToString(" ")
            Text(
                text = spacedCode,
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("heart_pairing_code_text")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "💖",
                fontSize = 16.sp
            )
        }
    }
}
