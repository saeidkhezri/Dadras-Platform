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
            .background(if (isLight) Color(0xFFF9FBFD) else Color(0xFF020204)) // Base colors
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    if (isLight) {
                        // 1. LITE THEME: 3D Luxury Pearl & Alabaster Gold-Ochre Gradient
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF6F8FC), // Alabaster Ice top
                                    Color(0xFFFDF7EE), // Pearl Cream Warm silk middle
                                    Color(0xFFE9EDF5)  // Polished Alabaster bottom
                                )
                            )
                        )

                        // 2. Translucent Drifting Soft Glass Orbs (Tactile 3D Depth)
                        val orbPhase = if (isDynamic) (animPhase * 0.01f) else 0f
                        
                        // Pearl-Gold Luxury Orb
                        val orb1X = size.width * (0.8f + 0.10f * sin(orbPhase.toDouble()).toFloat())
                        val orb1Y = size.height * (0.2f + 0.08f * cos(orbPhase.toDouble()).toFloat())
                        val orb1Radius = size.minDimension * 0.7f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFF5DDA7).copy(alpha = 0.28f), // Soft amber-gold
                                    Color(0xFFFCECD5).copy(alpha = 0.12f),
                                    Color.Transparent
                                ),
                                center = Offset(orb1X, orb1Y),
                                radius = orb1Radius
                            ),
                            radius = orb1Radius,
                            center = Offset(orb1X, orb1Y)
                        )

                        // Apple Lapis-Mist Soft Blue Orb
                        val orb2X = size.width * (0.25f + 0.12f * cos(orbPhase.toDouble() + 2.0).toFloat())
                        val orb2Y = size.height * (0.75f + 0.10f * sin(orbPhase.toDouble() + 2.0).toFloat())
                        val orb2Radius = size.minDimension * 0.75f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF8EB1FF).copy(alpha = 0.22f), // Royal lapis sky blue
                                    Color(0xFFE3ECFF).copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                center = Offset(orb2X, orb2Y),
                                radius = orb2Radius
                            ),
                            radius = orb2Radius,
                            center = Offset(orb2X, orb2Y)
                        )

                        // 3. Thin Concentric Vector Lines (Embodies premium certificates / legal seals)
                        val lineBrush = Brush.linearGradient(
                            colors = listOf(Color(0xFFD4AF37).copy(alpha = 0.18f), Color.Transparent)
                        )
                        drawCircle(
                            brush = lineBrush,
                            radius = size.minDimension * 0.4f,
                            center = Offset(size.width * 0.9f, size.height * 0.1f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                        drawCircle(
                            brush = lineBrush,
                            radius = size.minDimension * 0.42f,
                            center = Offset(size.width * 0.9f, size.height * 0.1f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8.dp.toPx())
                        )

                    } else {
                        // 1. DARK THEME: SORA2-inspired deep slate-void obsidian black
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF030205), // Deep void top
                                    Color(0xFF08070D), // Midnight slate middle
                                    Color(0xFF010102)  // Absolute black bottom
                                )
                            )
                        )

                        // 2. Redesigned Fluid Shifting Neon Aurora Waves (Sora2 Premium style)
                        val shiftPhase = if (isDynamic) (animPhase * 0.012f) else 0f
                        
                        // Wave 1: SORA Sunset Crimson/Rose fluid wave (hot, creative AI energy)
                        val s1X = size.width * (0.4f + 0.15f * sin(shiftPhase.toDouble()).toFloat())
                        val s1Y = size.height * (0.3f + 0.10f * cos(shiftPhase.toDouble()).toFloat())
                        val s1Radius = size.minDimension * 0.95f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFC1B65).copy(alpha = 0.14f), // Intense Rose-Neon
                                    Color(0xFF5C102C).copy(alpha = 0.04f),
                                    Color.Transparent
                                ),
                                center = Offset(s1X, s1Y),
                                radius = s1Radius
                            ),
                            radius = s1Radius,
                            center = Offset(s1X, s1Y)
                        )

                        // Wave 2: SORA Deep Cobalt/Blue flow (focused reasoning & law precision)
                        val s2X = size.width * (0.15f + 0.10f * cos(shiftPhase.toDouble() + 1.2).toFloat())
                        val s2Y = size.height * (0.7f + 0.14f * sin(shiftPhase.toDouble() + 1.2).toFloat())
                        val s2Radius = size.minDimension * 1.1f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1648FF).copy(alpha = 0.12f), // Royal Purple-Blue Glow
                                    Color(0xFF05124D).copy(alpha = 0.03f),
                                    Color.Transparent
                                ),
                                center = Offset(s2X, s2Y),
                                radius = s2Radius
                            ),
                            radius = s2Radius,
                            center = Offset(s2X, s2Y)
                        )

                        // Wave 3: SORA Sovereign Golden Sunrise (equity, justice, majesty representation)
                        val s3X = size.width * (0.85f + 0.12f * sin(shiftPhase.toDouble() + 2.5).toFloat())
                        val s3Y = size.height * (0.8f + 0.08f * cos(shiftPhase.toDouble() + 2.5).toFloat())
                        val s3Radius = size.minDimension * 0.8f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF9E1B).copy(alpha = 0.10f), // Premium Saffron-Orange/Amber
                                    Color.Transparent
                                ),
                                center = Offset(s3X, s3Y),
                                radius = s3Radius
                            ),
                            radius = s3Radius,
                            center = Offset(s3X, s3Y)
                        )

                        // 3. Elegant 3D Parallax Twinkling Stars
                        stars.forEach { star ->
                            val driftX = if (isDynamic) (animPhase * 0.16f * star.depth) else 0f
                            val driftY = if (isDynamic) (animPhase * 0.05f * star.depth) else 0f

                            var px = (star.xRatio * size.width + driftX) % size.width
                            if (px < 0) px += size.width
                            var py = (star.yRatio * size.height + driftY) % size.height
                            if (py < 0) py += size.height

                            val starOpacity = if (isDynamic) {
                                val twinkle = sin((animPhase * 0.045f * star.twinkleSpeed) + star.twinklePhase)
                                val normalizedTwinkle = (twinkle + 1f) / 2f
                                star.baseOpacity * (0.3f + 0.7f * normalizedTwinkle)
                            } else {
                                star.baseOpacity * 0.8f
                            }

                            drawCircle(
                                color = Color.White.copy(alpha = starOpacity),
                                radius = star.size,
                                center = Offset(px, py)
                            )
                        }
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
