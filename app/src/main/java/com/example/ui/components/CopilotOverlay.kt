package com.example.ui.components

import androidx.compose.animation.*
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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // ۱. دکمه شناور همیشه جلوی دید (Floating Circle Button)
        if (!isCopilotOpen) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = 90.dp) // نوار مسیریابی پایین را قطع نکند
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = listOf(Color(0xFF2563EB), Color(0xFF4F46E5))
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    .clickable { copilotViewModel.toggleCopilot() }
                    .testTag("floating_copilot_button"),
                contentAlignment = Alignment.Center
            ) {
                // آیکون ربات همیار حقوقی دادس
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = "همیار حقوقی دادرس",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
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
                                text = "همیار حقوقی مستقل (هوش پیشرفته)",
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
                                Text(
                                    text = responseText,
                                    style = Typography.bodyMedium,
                                    color = TextPrimaryFarsi,
                                    textAlign = TextAlign.Right,
                                    lineHeight = 22.sp
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
