package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Composable
fun FrostedGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val baseBgColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBgColor)
    ) {
        // Drawing ambient radial gradients for the liquid glass mesh blur effect
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    if (isLight) {
                        // Light Mode: Pastel, soft sky-blue glow with very low opacity
                        val topRadius = size.minDimension * 1.1f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF3B82F6).copy(alpha = 0.10f),
                                    Color.Transparent
                                ),
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.15f),
                                radius = topRadius
                            ),
                            radius = topRadius,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.15f)
                        )

                        // Pastel, soft peach/gold glow at the bottom-right
                        val bottomRadius = size.minDimension * 1.1f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFF59E0B).copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.85f),
                                radius = bottomRadius
                            ),
                            radius = bottomRadius,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.85f)
                        )
                    } else {
                        // Dark Mode: Vibrant electric blue and purple glow
                        val topRadius = size.minDimension * 0.9f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF2F80ED).copy(alpha = 0.22f), // Electric Blue
                                    Color.Transparent
                                ),
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.15f),
                                radius = topRadius
                            ),
                            radius = topRadius,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.15f)
                        )

                        val bottomRadius = size.minDimension * 0.9f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF7E22CE).copy(alpha = 0.20f), // Deep Purple
                                    Color.Transparent
                                ),
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.85f),
                                radius = bottomRadius
                            ),
                            radius = bottomRadius,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.85f)
                        )

                        // Center amber subtle accent highlight
                        val accentRadius = size.minDimension * 0.5f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFD4AF37).copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.5f),
                                radius = accentRadius
                            ),
                            radius = accentRadius,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.5f)
                        )

                        // طرّاحی ستاره‌های بسیار ریز و شفاف آسمان کویر (Subtle Desert Starfield for luxury dark navy aesthetic)
                        val starPoints = listOf(
                            Pair(0.12f, 0.08f), Pair(0.28f, 0.15f), Pair(0.08f, 0.25f), Pair(0.42f, 0.20f),
                            Pair(0.65f, 0.11f), Pair(0.85f, 0.07f), Pair(0.92f, 0.22f), Pair(0.72f, 0.28f),
                            Pair(0.18f, 0.38f), Pair(0.35f, 0.45f), Pair(0.55f, 0.35f), Pair(0.82f, 0.41f),
                            Pair(0.05f, 0.52f), Pair(0.22f, 0.60f), Pair(0.48f, 0.58f), Pair(0.68f, 0.50f),
                            Pair(0.90f, 0.55f), Pair(0.15f, 0.72f), Pair(0.30f, 0.78f), Pair(0.62f, 0.70f),
                            Pair(0.78f, 0.76f), Pair(0.08f, 0.88f), Pair(0.25f, 0.92f), Pair(0.45f, 0.85f),
                            Pair(0.58f, 0.94f), Pair(0.88f, 0.90f), Pair(0.70f, 0.88f), Pair(0.50f, 0.10f),
                            Pair(0.94f, 0.38f), Pair(0.38f, 0.82f)
                        )
                        starPoints.forEachIndexed { index, star ->
                            val x = star.first * size.width
                            val y = star.second * size.height
                            val starSize = if (index % 3 == 0) 2.5f else if (index % 3 == 1) 1.5f else 1.0f
                            val opacity = if (index % 4 == 0) 0.35f else if (index % 4 == 1) 0.22f else 0.12f
                            drawCircle(
                                color = Color.White.copy(alpha = opacity),
                                radius = starSize,
                                center = androidx.compose.ui.geometry.Offset(x, y)
                            )
                        }
                    }
                }
        )
        content()
    }
}
