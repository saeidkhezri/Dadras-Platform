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
                    }
                }
        )
        content()
    }
}
