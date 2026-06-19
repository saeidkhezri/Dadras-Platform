package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import kotlin.math.roundToInt
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.CopilotViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CopilotOverlay(
    copilotViewModel: CopilotViewModel,
    currentScreenName: String
) {
    val isCopilotOpen by copilotViewModel.isCopilotOpen.collectAsState()
    val isThinking by copilotViewModel.isCopilotThinking.collectAsState()
    val responseText by copilotViewModel.copilotResponse.collectAsState()

    var userQuestion by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // انیمیشن‌های پیشرفته همیار مستقل رنگارنگ جمینایی/کوپایلاتی
    val infiniteTransition = rememberInfiniteTransition(label = "copilot_glow")

    // ۱. چرخش مداوم گرادیان رنگی (Gemini/Copilot-style gradient flow)
    val colorShiftPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "color_shift"
    )

    // ۲. نوسان افقی بسیار ریز برای جلب توجه چشمگیر (attention-grabbing shifts)
    val localShiftX by infiniteTransition.animateFloat(
        initialValue = -2.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "local_shift_x"
    )

    // ۳. نوسان عمودی بسیار ریز (attention-grabbing shifts)
    val localShiftY by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "local_shift_y"
    )

    // ۴. تپش اندازه دکمه (attention-grabbing pulse scale)
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1750, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // موقعیت دکمه کشیدنی در صفحه
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var isInitialized by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }

        // مقداردهی اولیه موقعیت دکمه (کناره راست صفحه به صورت نیم‌دایره چسبیده)
        if (!isInitialized && maxWidthPx > 0f) {
            // دکمه با قطر ۵۰ پیکسل، ۲۵ پیکسل خارج صفحه قرار می‌گیرد تا نیم‌دایره شود
            offsetX = maxWidthPx - with(density) { 25.dp.toPx() }
            offsetY = maxHeightPx * 0.7f // در ارتفاع مناسب بالای نوار هدایت
            isInitialized = true
        }

        // متغیر رهگیری کل میزان درگ برای تفکیک تپ با جابه‌جایی
        var dragAccumulation by remember { mutableStateOf(0f) }

        // ۱. دکمه شناور همیشه جلو دید (اگر همیار بسته باشد)
        if (!isCopilotOpen) {
            Box(
                modifier = Modifier
                    .offset {
                        androidx.compose.ui.unit.IntOffset(
                            (offsetX + with(density) { localShiftX.dp.toPx() }).roundToInt(),
                            (offsetY + with(density) { localShiftY.dp.toPx() }).roundToInt()
                        )
                    }
                    .size(50.dp)
                    .pointerInput(Unit) {
                        try {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                var dragAccum = 0f
                                var change: PointerInputChange? = null
                                do {
                                    val event = awaitPointerEvent()
                                    change = event.changes.firstOrNull()
                                    if (change != null && change.pressed) {
                                        val dragAmount = change.positionChange()
                                        if (dragAmount.getDistance() > 0f) {
                                            offsetX += dragAmount.x
                                            offsetY += dragAmount.y
                                            dragAccum += dragAmount.getDistance()
                                            change.consume()
                                        }
                                    }
                                } while (change != null && change.pressed)

                                if (dragAccum < 15f) {
                                    copilotViewModel.toggleCopilot()
                                } else {
                                    // چسبیدن خودکار به نزدیک‌ترین لبه عمودی چپ یا راست به صورت نیم‌دایره
                                    val leftEdgeX = -with(density) { 25.dp.toPx() }
                                    val rightEdgeX = maxWidthPx - with(density) { 25.dp.toPx() }
                                    val middleScreen = maxWidthPx / 2f
                                    offsetX = if (offsetX + with(density) { 25.dp.toPx() } < middleScreen) leftEdgeX else rightEdgeX
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    .testTag("floating_copilot_button"),
                contentAlignment = Alignment.Center
            ) {
                // المان چرخان رنگارنگ پس‌زمینه (رنگین‌کمانی جمینای)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            rotationZ = colorShiftPhase,
                            scaleX = pulseScale,
                            scaleY = pulseScale
                        )
                        .clip(CircleShape)
                        .background(
                            androidx.compose.ui.graphics.Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFF9E00FF), // بنفش متالیک جمینای
                                    Color(0xFF00E0FF), // سایان درخشان
                                    Color(0xFFFF2E93), // صورتی نئونی کوپایلوت
                                    Color(0xFFFFD600), // زرد فسفری
                                    Color(0xFF2563EB), // آبی عمیق
                                    Color(0xFF9E00FF)  // فرود بر نقطه اول جهت انسجام حلقه
                                )
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                )

                // آیکون هوشمند فوق‌لوکس، متحرک و نوین همیار دادرس
                LuxuryAnimatedCopilotIcon(colorShiftPhase, pulseScale)
            }
        }

        // ۲. برگ شیشه‌ای بازشونده همیار (Glass Copilot Bottom Drawer)
        AnimatedVisibility(
            visible = isCopilotOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .border(1.dp, GlassBorderDark, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xEA050B18)),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // هدر همیار
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { copilotViewModel.closeCopilot() },
                            colors = ButtonDefaults.buttonColors(containerColor = SlateNavyMedium)
                        ) {
                            Text("بستن همیار", color = AccentGold, style = Typography.labelMedium)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "دستیار هوشمند حقوقی",
                                style = Typography.titleMedium,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(Icons.Default.Star, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // میانبرهای راهنمای پویای صفحه جاری
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(GlassSurfaceDark, RoundedCornerShape(8.dp))
                                .border(1.dp, GlassBorderLight, RoundedCornerShape(8.dp))
                                .clickable { copilotViewModel.explainPage(currentScreenName) }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("راهنمای این صفحه", style = Typography.labelSmall, color = TextPrimaryFarsi, fontSize = 9.sp)
                        }

                        Box(
                            modifier = Modifier
                                        .weight(1.2f)
                                        .background(GlassSurfaceDark, RoundedCornerShape(8.dp))
                                        .border(1.dp, GlassBorderLight, RoundedCornerShape(8.dp))
                                        .clickable { copilotViewModel.explainTerm("مستند تاخیر تادیه") }
                                        .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("تفسیر تاخیر تادیه", style = Typography.labelSmall, color = TextPrimaryFarsi, fontSize = 9.sp)
                        }

                        Box(
                            modifier = Modifier
                                        .weight(1.2f)
                                        .background(GlassSurfaceDark, RoundedCornerShape(8.dp))
                                        .border(1.dp, GlassBorderLight, RoundedCornerShape(8.dp))
                                        .clickable { copilotViewModel.explainTerm("قاعده تسبیب") }
                                        .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("قاعده فقهی تسبیب", style = Typography.labelSmall, color = TextPrimaryFarsi, fontSize = 9.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // محیط نمایش پاسخ هوش مصنوعی
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(GlassSurfaceDark, RoundedCornerShape(16.dp))
                            .border(1.dp, GlassBorderLight, RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.Top,
                            horizontalAlignment = Alignment.End
                        ) {
                            if (isThinking) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = AccentGold, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("گروه مشاوران هوشمند دادرسی در حال تنظیم پاسخ...", color = TextSecondaryFarsi, style = Typography.labelMedium)
                                    }
                                }
                            } else if (responseText.isNotBlank()) {
                                ParsedMarkdownText(
                                    text = responseText,
                                    textColor = TextPrimaryFarsi
                                )
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = TextSecondaryFarsi, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "سلام! در هر بخشی از این اپلیکیشن هستید کافیست سوال خود را بنویسید یا برای تشریح اهداف این صفحه، دکمه 'راهنمای این صفحه' را بفشارید.",
                                        style = Typography.labelMedium,
                                        color = TextSecondaryFarsi,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // کادر نوشتن سوال اختصاصی و شلیک به Gemini
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (userQuestion.isNotBlank()) {
                                    copilotViewModel.askCopilot(userQuestion, currentScreenName)
                                    userQuestion = ""
                                }
                            },
                            modifier = Modifier
                                .background(AccentGold, CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "ارسال", tint = Color.Black)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedTextField(
                            value = userQuestion,
                            onValueChange = { userQuestion = it },
                            placeholder = { Text("سوال حقوقی خود را بپرسید...", color = TextSecondaryFarsi, fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("copilot_chat_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimaryFarsi,
                                unfocusedTextColor = TextPrimaryFarsi,
                                focusedBorderColor = AccentGold,
                                unfocusedBorderColor = GlassBorderDark
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LuxuryAnimatedCopilotIcon(colorShiftPhase: Float, pulseScale: Float) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(36.dp)) {
        val width = size.width
        val height = size.height
        val center = androidx.compose.ui.geometry.Offset(width / 2f, height / 2f)
        
        // ۱. رسم هاله نورانی طلایی لوکس در پس‌زمینه با شفافیت پویا
        val pulseIntensity = (pulseScale - 0.95f) / 0.1f // بین ۰ تا ۱
        val glowRadius = (width / 2f) * (0.8f + pulseIntensity * 0.2f)
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFD700).copy(alpha = 0.4f * (1.2f - pulseScale)),
                    Color(0xFFD4AF37).copy(alpha = 0.1f * (1.2f - pulseScale)),
                    Color.Transparent
                ),
                center = center,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = center
        )

        // ۲. رینگ چرخان الماس‌گونه بیرونی (اوربیت دور هسته هوش مصنوعی)
        val ringRadius = (width / 2.3f)
        val phaseRad = Math.toRadians(colorShiftPhase.toDouble()).toFloat()
        
        // رسم خطوط فرضی مدار بصورت گرادیان طلایی
        drawCircle(
            color = Color(0xFFD4AF37).copy(alpha = 0.3f),
            radius = ringRadius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 1.5.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 15f), phaseRad * 20f)
            )
        )

        // ۳. رسم ستون قضاوت و تعادل قانون (فرشته عدالت با ترکیب رباتیک هوشمند)
        // یک المان نگینی درخشان لوکس در مرکز (ستاره یا الماس ۴ پر با رنگ طلایی پرمیوم)
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(center.x, center.y - (height * 0.35f) * pulseScale)
            quadraticTo(center.x, center.y, center.x + (width * 0.35f) * pulseScale, center.y)
            quadraticTo(center.x, center.y, center.x, center.y + (height * 0.35f) * pulseScale)
            quadraticTo(center.x, center.y, center.x - (width * 0.35f) * pulseScale, center.y)
            quadraticTo(center.x, center.y, center.x, center.y - (height * 0.35f) * pulseScale)
            close()
        }
        
        drawPath(
            path = path,
            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFFFFF), // هسته سفید درخشان
                    Color(0xFFFFD700), // طلایی درخشان ۲۴ عیار
                    Color(0xFFB8860B)  // طلایی تیره مجلل
                )
            )
        )

        // ۴. نگین‌های درخشان ماهواره‌ای چرخان دور مرکز هوشمند
        val satX = center.x + ringRadius * kotlin.math.cos(phaseRad)
        val satY = center.y + ringRadius * kotlin.math.sin(phaseRad)
        drawCircle(
            color = Color(0xFFFFFFFF),
            radius = 3.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(satX, satY)
        )
        // افکت درخشش دور قمر چرخان
        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = 0.4f),
            radius = 6.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(satX, satY)
        )
    }
}

@Composable
fun ParsedMarkdownText(
    text: String,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val lines = text.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                continue
            }
            when {
                trimmed.startsWith("### ") || trimmed.startsWith("###") -> {
                    val rawText = if (trimmed.startsWith("### ")) trimmed.removePrefix("### ") else trimmed.removePrefix("###")
                    Text(
                        text = rawText,
                        style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = AccentGold),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                trimmed.startsWith("## ") || trimmed.startsWith("##") -> {
                    val rawText = if (trimmed.startsWith("## ")) trimmed.removePrefix("## ") else trimmed.removePrefix("##")
                    Text(
                        text = rawText,
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold, color = AccentGold),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                trimmed.startsWith("# ") || trimmed.startsWith("#") -> {
                    val rawText = if (trimmed.startsWith("# ")) trimmed.removePrefix("# ") else trimmed.removePrefix("#")
                    Text(
                        text = rawText,
                        style = Typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AccentGold),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                trimmed.startsWith("* ") || trimmed.startsWith("*") || trimmed.startsWith("- ") || trimmed.startsWith("-") -> {
                    val rawText = when {
                        trimmed.startsWith("* ") -> trimmed.removePrefix("* ")
                        trimmed.startsWith("*") -> trimmed.removePrefix("*")
                        trimmed.startsWith("- ") -> trimmed.removePrefix("- ")
                        else -> trimmed.removePrefix("-")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = parseBoldMarkdown(rawText),
                            style = Typography.bodyMedium.copy(color = textColor),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "•",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = AccentGold)
                        )
                    }
                }
                else -> {
                    Text(
                        text = parseBoldMarkdown(trimmed),
                        style = Typography.bodyMedium.copy(color = textColor),
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun parseBoldMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return androidx.compose.ui.text.buildAnnotatedString {
        val parts = text.split("**")
        var isBold = false
        for (part in parts) {
            if (isBold) {
                pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = AccentGold))
                append(part)
                pop()
            } else {
                append(part)
            }
            isBold = !isBold
        }
    }
}
