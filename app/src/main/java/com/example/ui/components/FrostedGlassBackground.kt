package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * A highly optimized, luxury 3D animated background wrapper.
 * Draws continuous morphing mesh-blurs, glowing lights, and a premium starry desert sky.
 * Perfect for high-end judiciary/civilian system apps inspired by iOS translucent designs.
 */
val LocalDynamicBackground = staticCompositionLocalOf { true }

data class DesertStar(
    val xRatio: Float,
    val yRatio: Float,
    val size: Float,
    val baseOpacity: Float,
    val twinkleSpeed: Float,
    val twinklePhase: Float,
    val depth: Float
)

/**
 * A highly optimized, luxury 3D animated background wrapper.
 * Draws a mesmerizing starry sky of the Iranian Desert with deep Lapis Lazuli color harmony.
 * Stars twinkle and drift slowly in 3D parallax space when dynamic background is active.
 */
@Composable
fun FrostedGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val isDynamic = LocalDynamicBackground.current

    // Infinite float transition for twinkling and drift
    val infiniteTransition = rememberInfiniteTransition(label = "Desert Sky Drift")
    val animPhase by if (isDynamic) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 2000f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "Drift Phase"
        )
    } else {
        remember { mutableStateOf(0f) }
    }

    val stars = remember {
        val random = java.util.Random(1337)
        List(140) {
            val isProminent = random.nextFloat() < 0.12f
            val size = if (isProminent) {
                random.nextFloat() * 2.8f + 2.0f // brighter and larger stars (2.0dp - 4.8dp)
            } else {
                random.nextFloat() * 1.2f + 0.4f // smaller, fainter stars (0.4dp - 1.6dp)
            }
            DesertStar(
                xRatio = random.nextFloat(),
                yRatio = random.nextFloat(),
                size = size,
                baseOpacity = if (isProminent) random.nextFloat() * 0.4f + 0.6f else random.nextFloat() * 0.5f + 0.2f,
                twinkleSpeed = random.nextFloat() * 2.5f + 0.8f,
                twinklePhase = random.nextFloat() * 2f * Math.PI.toFloat(),
                depth = random.nextFloat() * 0.8f + 0.2f
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030814)) // Base space dark color
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    // 1. Draw Persian Desert Lapis Lazuli Luxury Gradient
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF020713), // Deep space top
                                Color(0xFF0A183D), // Noble Persian Lapis Lazuli
                                Color(0xFF040A1A)  // Dark horizon bottom
                            )
                        )
                    )

                    // 2. Cosmic Nebulae Orbs (3D ambient clouds)
                    val nebulaPhase = if (isDynamic) (animPhase * 0.015f) else 0f
                    
                    // Lapis Lazuli Soft Light
                    val n1X = size.width * (0.3f + 0.12f * sin(nebulaPhase.toDouble()).toFloat())
                    val n1Y = size.height * (0.25f + 0.08f * cos(nebulaPhase.toDouble()).toFloat())
                    val n1Radius = size.minDimension * 0.9f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF2E5BFF).copy(alpha = 0.15f),
                                Color(0xFF102875).copy(alpha = 0.08f),
                                Color.Transparent
                            ),
                            center = Offset(n1X, n1Y),
                            radius = n1Radius
                        ),
                        radius = n1Radius,
                        center = Offset(n1X, n1Y)
                    )

                    // Desert Warm Ochre Glow (represents soft starlight glow on Persian deserts)
                    val n2X = size.width * (0.7f + 0.10f * cos(nebulaPhase.toDouble() + 1.5).toFloat())
                    val n2Y = size.height * (0.7f + 0.12f * sin(nebulaPhase.toDouble() + 1.5).toFloat())
                    val n2Radius = size.minDimension * 0.8f
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFC5A880).copy(alpha = 0.08f), // Luxury Soft Amber
                                Color.Transparent
                            ),
                            center = Offset(n2X, n2Y),
                            radius = n2Radius
                        ),
                        radius = n2Radius,
                        center = Offset(n2X, n2Y)
                    )

                    // 3. Render 3D Parallax Twinkling Stars
                    stars.forEach { star ->
                        // Slow 3D Parallax Drift
                        val driftX = if (isDynamic) (animPhase * 0.15f * star.depth) else 0f
                        val driftY = if (isDynamic) (animPhase * 0.06f * star.depth) else 0f

                        var px = (star.xRatio * size.width + driftX) % size.width
                        if (px < 0) px += size.width
                        var py = (star.yRatio * size.height + driftY) % size.height
                        if (py < 0) py += size.height

                        // Elegant Eye-Pleasing Twinkling
                        val starOpacity = if (isDynamic) {
                            val twinkle = sin((animPhase * 0.05f * star.twinkleSpeed) + star.twinklePhase)
                            val normalizedTwinkle = (twinkle + 1f) / 2f // [0f .. 1f]
                            star.baseOpacity * (0.35f + 0.65f * normalizedTwinkle)
                        } else {
                            star.baseOpacity
                        }

                        drawCircle(
                            color = Color.White.copy(alpha = starOpacity),
                            radius = star.size,
                            center = Offset(px, py)
                        )
                    }

                    // 4. If Light Theme is active, overlay a soft satin-white sheen so starlight is seen through silk mist
                    if (isLight) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xE6EBEFF5), // Silk white with soft lapis reflection
                                    Color(0xCCF1F4FA)
                                )
                            )
                        )
                    }
                }
        )
        content()
    }
}

/**
 * Custom 3D Glassmorphic Modifier extension.
 * Creates an outstanding 3D bevel effect, glass shine, dual borders, and a soft outer neon/pastel drop shadow.
 * Fully inspired by premium iOS high-fidelity widgets.
 */
fun Modifier.glassy3D(
    cornerRadius: Dp = 16.dp,
    borderAlpha: Float = 0.22f,
    elevation: Dp = 8.dp,
    glowColor: Color = Color.Transparent,
    borderColor: Color? = null
): Modifier = composed {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f

    // Configure beautiful matte semi-translucent card surfaces for elegant glassmorphism and absolute star occlusion
    val surfaceColor = if (isLight) {
        Color(0xF5FFFFFF) // Gorgeous 96% translucent milk-white glass
    } else {
        Color(0xF20F172A) // Gorgeous 95% translucent Lapis lazuli/navy dark glass
    }

    val specColor = if (isLight) Color.White.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.25f)
    val baseBorderColor = borderColor ?: (if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155))

    this
        // 1. Beautiful 3D shadow system (with optional tinted neon diffuse glow)
        .shadow(
            elevation = elevation,
            shape = RoundedCornerShape(cornerRadius),
            clip = false,
            ambientColor = if (glowColor != Color.Transparent) glowColor else Color.Black.copy(alpha = 0.1f),
            spotColor = if (glowColor != Color.Transparent) glowColor else Color.Black.copy(alpha = 0.2f)
        )
        // 2. Translucent backdrop blend
        .background(
            color = surfaceColor,
            shape = RoundedCornerShape(cornerRadius)
        )
        // 3. 3D Bevel Highlight: Inner high-contrast top-down glare border
        .border(
            width = 1.2.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    specColor,
                    baseBorderColor.copy(alpha = borderAlpha)
                )
            ),
            shape = RoundedCornerShape(cornerRadius)
        )
}
