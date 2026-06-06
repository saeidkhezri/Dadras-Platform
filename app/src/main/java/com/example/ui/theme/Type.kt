package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp

// Define Vazirmatn and IRANSansX as font families mapped to high-quality native SansSerif fallback
val Vazirmatn = FontFamily.SansSerif
val IRANSansX = FontFamily.SansSerif

// Clean Persian typography supporting native RTL layout globally with designated fonts
val Typography = Typography(
    displayMedium = TextStyle(
        fontFamily = Vazirmatn,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        textAlign = TextAlign.Right
    ),
    headlineMedium = TextStyle(
        fontFamily = Vazirmatn,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        textAlign = TextAlign.Right
    ),
    titleLarge = TextStyle(
        fontFamily = Vazirmatn,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        textAlign = TextAlign.Right
    ),
    titleMedium = TextStyle(
        fontFamily = Vazirmatn,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl,
        textAlign = TextAlign.Right
    ),
    bodyLarge = TextStyle(
        fontFamily = Vazirmatn,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp,
        textDirection = TextDirection.Rtl,
        textAlign = TextAlign.Right
    ),
    bodyMedium = TextStyle(
        fontFamily = Vazirmatn,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp,
        textDirection = TextDirection.Rtl,
        textAlign = TextAlign.Right
    ),
    labelMedium = TextStyle(
        fontFamily = Vazirmatn,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        textDirection = TextDirection.Rtl,
        textAlign = TextAlign.Right
    ),
    labelSmall = TextStyle(
        fontFamily = Vazirmatn,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        textDirection = TextDirection.Rtl,
        textAlign = TextAlign.Right
    )
)
