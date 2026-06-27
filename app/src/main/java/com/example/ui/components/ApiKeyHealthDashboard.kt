package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*
import com.example.viewmodel.AdminViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ---------------------------------------------------------------------
// Service Health Model
// ---------------------------------------------------------------------

data class ServiceHealthState(
    val providerId: String,
    val providerName: String,
    val keyUsed: String,
    val isConfigured: Boolean,
    val isChecking: Boolean = false,
    val checked: Boolean = false,
    val isValid: Boolean = false,
    val connectionOk: Boolean = false,
    val isRateLimited: Boolean = false,
    val message: String = "در انتظار ممیزی سلامت...",
    val usagePercent: Float = 0f,
    val quotaDetails: String = "",
    val rechargeInfo: String = "",
    val lastCheckedTime: String = "-"
)

// ---------------------------------------------------------------------
// Pulsing LED Indicator supporting Green, Red, Amber, Blue, Gray
// ---------------------------------------------------------------------

@Composable
fun PulsingHealthLed(
    isConfigured: Boolean,
    isChecking: Boolean,
    checked: Boolean,
    isValid: Boolean,
    connectionOk: Boolean,
    isRateLimited: Boolean
) {
    val baseColor = when {
        isChecking -> Color(0xFF3B82F6) // Electric Blue while checking
        !isConfigured -> Color.Gray // Gray if not configured
        !checked -> Color.White.copy(alpha = 0.4f) // Translucent if not checked yet
        isValid && connectionOk && !isRateLimited -> Color(0xFF10B981) // Emerald Green
        isValid && isRateLimited -> Color(0xFFF59E0B) // Amber Orange for Rate Limited
        isValid && !connectionOk -> Color(0xFFF59E0B) // Amber Orange for Network Issue
        else -> Color(0xFFEF4444) // Coral Red for Invalid
    }

    // Animation for pulsing glow if checking or active
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val currentAlpha = if (isChecking || (isValid && connectionOk)) alphaAnim else 1.0f
    val currentScale = if (isChecking) scaleAnim else 1.0f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(24.dp)
    ) {
        // Outer Glow Ring
        Box(
            modifier = Modifier
                .size(16.dp * currentScale)
                .shadow(
                    elevation = if (isConfigured) 8.dp else 0.dp,
                    shape = RoundedCornerShape(10.dp),
                    ambientColor = baseColor.copy(alpha = currentAlpha),
                    spotColor = baseColor.copy(alpha = currentAlpha)
                )
                .background(baseColor.copy(alpha = 0.25f * currentAlpha), RoundedCornerShape(10.dp))
        )
        // Inner Solid LED Core
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(baseColor, RoundedCornerShape(5.dp))
                .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(5.dp))
        )
    }
}

// ---------------------------------------------------------------------
// Full Featured API Health Dashboard Dialog
// ---------------------------------------------------------------------

@Composable
fun ApiKeyHealthDashboardDialog(
    adminViewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    
    // Collect keys from shared states
    val geminiKeys by adminViewModel.geminiApiKeys.collectAsState()
    val openAiKeys by adminViewModel.openaiApiKeys.collectAsState()
    val openRouterKeys by adminViewModel.openrouterApiKeys.collectAsState()
    val groqKeys by adminViewModel.groqApiKeys.collectAsState()
    val cohereKeys by adminViewModel.cohereApiKeys.collectAsState()
    val huggingFaceKeys by adminViewModel.huggingfaceApiKeys.collectAsState()
    val youcomKeys by adminViewModel.youcomApiKeys.collectAsState()
    
    // Resolve single active keys
    val geminiKey = remember(geminiKeys) { geminiKeys.firstOrNull { it.isNotBlank() } ?: "" }
    val openAiKey = remember(openAiKeys) { openAiKeys.firstOrNull { it.isNotBlank() } ?: "" }
    val openRouterKey = remember(openRouterKeys) { openRouterKeys.firstOrNull { it.isNotBlank() } ?: "" }
    val groqKey = remember(groqKeys) { groqKeys.firstOrNull { it.isNotBlank() } ?: "" }
    val cohereKey = remember(cohereKeys) { cohereKeys.firstOrNull { it.isNotBlank() } ?: "" }
    val huggingFaceKey = remember(huggingFaceKeys) { huggingFaceKeys.firstOrNull { it.isNotBlank() } ?: "" }
    val youcomKey = remember(youcomKeys) { youcomKeys.firstOrNull { it.isNotBlank() } ?: "" }

    // Health States Map
    var healthStates by remember {
        mutableStateOf<Map<String, ServiceHealthState>>(emptyMap())
    }

    // Initialize health configurations
    LaunchedEffect(geminiKey, openAiKey, openRouterKey, groqKey, cohereKey, huggingFaceKey, youcomKey) {
        val initialMap = linkedMapOf<String, ServiceHealthState>()
        
        listOf(
            Triple("Gemini", "Google Gemini AI", geminiKey),
            Triple("OpenAI", "OpenAI GPT-4 Engine", openAiKey),
            Triple("OpenRouter", "OpenRouter Gateways", openRouterKey),
            Triple("Groq", "Groq Fast Inference", groqKey),
            Triple("Cohere", "Cohere Language Model", cohereKey),
            Triple("HuggingFace", "Hugging Face Hub API", huggingFaceKey),
            Triple("YouCom", "You.com Intelligent Search", youcomKey)
        ).forEach { (id, name, key) ->
            // Keep existing checked results if keys haven't changed, otherwise create new
            val existing = healthStates[id]
            if (existing != null && existing.keyUsed == key) {
                initialMap[id] = existing
            } else {
                initialMap[id] = ServiceHealthState(
                    providerId = id,
                    providerName = name,
                    keyUsed = key,
                    isConfigured = key.isNotBlank()
                )
            }
        }
        healthStates = initialMap
    }

    // Verification worker
    val runCheckForService = remember {
        { id: String ->
            val currentState = healthStates[id]
            if (currentState != null && currentState.isConfigured && !currentState.isChecking) {
                coroutineScope.launch {
                    // Set checking status
                    healthStates = healthStates.toMutableMap().apply {
                        this[id] = currentState.copy(
                            isChecking = true,
                            message = "در حال برقراری ارتباط زنده با سرور..."
                        )
                    }

                    // Artificial small delay for premium feels
                    delay(600)

                    val res = ApiKeyVerifier.verifyKeyReal(id, currentState.keyUsed)
                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    val timeString = sdf.format(Date())

                    // Quota state updates
                    val rateLimited = res.usagePercent >= 1.0f

                    healthStates = healthStates.toMutableMap().apply {
                        this[id] = currentState.copy(
                            isChecking = false,
                            checked = true,
                            isValid = res.isValid,
                            connectionOk = res.connectionOk,
                            isRateLimited = rateLimited,
                            message = if (rateLimited) "محدودیت موقت اعمال شده (Rate Limited)" else res.message,
                            usagePercent = res.usagePercent,
                            quotaDetails = res.quotaDetails,
                            rechargeInfo = res.rechargeInfo,
                            lastCheckedTime = timeString
                        )
                    }
                }
            }
        }
    }

    val runCheckAll = remember {
        {
            healthStates.keys.forEach { id ->
                runCheckForService(id)
            }
        }
    }

    // Auto-verify all active keys when opened first time
    LaunchedEffect(healthStates.size) {
        if (healthStates.isNotEmpty() && healthStates.values.none { it.checked || it.isChecking }) {
            delay(400)
            runCheckAll()
        }
    }

    // Calculation of Overall metrics
    val configuredCount = healthStates.values.count { it.isConfigured }
    val activeCount = healthStates.values.count { it.checked && it.isValid && it.connectionOk && !it.isRateLimited }
    val issuesCount = healthStates.values.count { it.checked && (!it.isValid || !it.connectionOk || it.isRateLimited) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.96f))
                .border(1.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.End
            ) {
                // Header Panel
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = AccentGold, modifier = Modifier.size(24.dp))
                            Text(
                                "پیشخوان مانیتورینگ سلامت و ظرفیت درگاه‌های هوش",
                                style = Typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            "پایش زنده وضعیت اتصال (Active/Invalid/Rate Limited) و گراف اعتبارسنجی سهمیه‌ها",
                            style = Typography.labelSmall,
                            color = TextSecondaryFarsi
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Dashboard Bar
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Stat 3: Problematic
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("دارای اختلال", style = Typography.labelSmall, color = TextSecondaryFarsi)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$issuesCount درگاه",
                                style = Typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (issuesCount > 0) Color(0xFFEF4444) else Color.White.copy(alpha = 0.4f)
                            )
                        }

                        Box(modifier = Modifier.size(1.dp, 28.dp).background(Color.White.copy(alpha = 0.1f)))

                        // Stat 2: Active & Connected
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("فعال و پاسخگو", style = Typography.labelSmall, color = TextSecondaryFarsi)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$activeCount درگاه",
                                style = Typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (activeCount > 0) Color(0xFF10B981) else Color.White.copy(alpha = 0.4f)
                            )
                        }

                        Box(modifier = Modifier.size(1.dp, 28.dp).background(Color.White.copy(alpha = 0.1f)))

                        // Stat 1: Total Configured
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("کلیدهای تعریف شده", style = Typography.labelSmall, color = TextSecondaryFarsi)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$configuredCount از ۶",
                                style = Typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // LazyColumn for Key Services list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(healthStates.values.toList()) { state ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (state.isConfigured) Color(0xFF1E293B).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.02f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    1.dp,
                                    if (state.isChecking) Color(0xFF3B82F6).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f),
                                    RoundedCornerShape(16.dp)
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                // Top row: Icon/LED + Title + Key Mask
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left side: LED and Status Text
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        PulsingHealthLed(
                                            isConfigured = state.isConfigured,
                                            isChecking = state.isChecking,
                                            checked = state.checked,
                                            isValid = state.isValid,
                                            connectionOk = state.connectionOk,
                                            isRateLimited = state.isRateLimited
                                        )

                                        Text(
                                            text = when {
                                                state.isChecking -> "در حال آزمایش..."
                                                !state.isConfigured -> "پیکربندی نشده"
                                                !state.checked -> "در انتظار بررسی"
                                                state.isValid && state.connectionOk && !state.isRateLimited -> "اتصال فعال و پاسخگو"
                                                state.isValid && state.isRateLimited -> "محدودیت ظرفیت ترافیکی (Rate Limited)"
                                                state.isValid && !state.connectionOk -> "اختلال موقت شبکه / فیلترینگ"
                                                else -> "کلید نامعتبر یا منقضی"
                                            },
                                            style = Typography.labelSmall.copy(fontSize = 11.sp),
                                            color = when {
                                                state.isChecking -> Color(0xFF3B82F6)
                                                !state.isConfigured -> Color.White.copy(alpha = 0.3f)
                                                !state.checked -> Color.White.copy(alpha = 0.6f)
                                                state.isValid && state.connectionOk && !state.isRateLimited -> Color(0xFF10B981)
                                                state.isValid && state.isRateLimited -> Color(0xFFF59E0B)
                                                state.isValid && !state.connectionOk -> Color(0xFFF59E0B)
                                                else -> Color(0xFFEF4444)
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Right side: Service Name
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = state.providerName,
                                            style = Typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (state.isConfigured) Color.White else Color.White.copy(alpha = 0.4f)
                                        )
                                        
                                        // Simple descriptive Icon for the provider
                                        Icon(
                                            imageVector = when (state.providerId) {
                                                "Gemini" -> Icons.Default.AutoAwesome
                                                "OpenAI" -> Icons.Default.Layers
                                                "OpenRouter" -> Icons.Default.Hub
                                                "Groq" -> Icons.Default.Bolt
                                                "Cohere" -> Icons.Default.Language
                                                else -> Icons.Default.Cloud
                                            },
                                            contentDescription = null,
                                            tint = if (state.isConfigured) AccentGold else Color.White.copy(alpha = 0.2f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                // Key mask and action button row
                                if (state.isConfigured) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Manual Retest Button (Touch target 48dp)
                                        IconButton(
                                            onClick = { runCheckForService(state.providerId) },
                                            enabled = !state.isChecking,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = "تست مجدد کلید",
                                                tint = if (state.isChecking) Color.Gray else Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Key mask
                                        Column(horizontalAlignment = Alignment.End) {
                                            val masked = if (state.keyUsed.length > 15) {
                                                state.keyUsed.take(7) + "••••••••" + state.keyUsed.takeLast(6)
                                            } else {
                                                "••••••••••••"
                                            }
                                            Text(
                                                text = masked,
                                                style = Typography.bodySmall.copy(fontSize = 11.sp),
                                                color = Color.White.copy(alpha = 0.5f),
                                                textAlign = TextAlign.Left
                                            )
                                            Text(
                                                text = "کلید فعال کانون دادگستری",
                                                style = Typography.labelSmall.copy(fontSize = 9.sp),
                                                color = TextSecondaryFarsi
                                            )
                                        }
                                    }

                                    // Display detailed server report & progress bar
                                    if (state.checked) {
                                        Divider(color = Color.White.copy(alpha = 0.06f), modifier = Modifier.padding(vertical = 4.dp))

                                        // Message and response code report
                                        Text(
                                            text = state.message,
                                            style = Typography.labelSmall.copy(fontSize = 11.sp),
                                            color = if (state.isValid && state.connectionOk) Color(0xFF10B981) else Color.White.copy(alpha = 0.8f),
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        if (state.isValid) {
                                            // Progress bar container
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = String.format("%.0f%% از ظرفیت مصرف شده", state.usagePercent * 100),
                                                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = if (state.usagePercent > 0.8f) Color(0xFFEF4444) else AccentGold
                                                    )
                                                    Text(
                                                        text = "میزان مصرف کوئتا / تراکنش‌های دوره جاری",
                                                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = TextSecondaryFarsi
                                                    )
                                                }

                                                // Gorgeous Linear Progress Bar with touch safe dimensions
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(8.dp)
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(Color.White.copy(alpha = 0.1f))
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(state.usagePercent)
                                                            .fillMaxHeight()
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(
                                                                if (state.usagePercent > 0.8f) Color(0xFFEF4444)
                                                                else if (state.usagePercent > 0.5f) Color(0xFFF59E0B)
                                                                else Color(0xFF10B981)
                                                            )
                                                    )
                                                }

                                                // Detailed quota & recharge metadata
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                                                        Text(
                                                            text = state.rechargeInfo,
                                                            style = Typography.labelSmall.copy(fontSize = 9.sp),
                                                            color = Color(0xFF10B981)
                                                        )
                                                    }

                                                    Text(
                                                        text = state.quotaDetails,
                                                        style = Typography.labelSmall.copy(fontSize = 9.sp),
                                                        color = Color.White.copy(alpha = 0.6f)
                                                    )
                                                }

                                                // Last updated timestamp
                                                Text(
                                                    text = "آخرین عیارسنجی زنده: ساعت ${state.lastCheckedTime}",
                                                    style = Typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = Color.White.copy(alpha = 0.4f),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.Right
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Not Configured visual filler card
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                            .padding(12.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = "جهت فعال‌سازی این درگاه بومی و استفاده از سهمیه ارزیابی اختصاصی خود، کلید این سرویس را در بخش آپلود کلیدها پیکربندی فرمایید.",
                                            style = Typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 15.sp),
                                            color = Color.White.copy(alpha = 0.4f),
                                            textAlign = TextAlign.Right
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Section Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Close Button (Touch target 48dp)
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("بستن سیستم پایش", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Test All Keys (Touch target 48dp)
                    Button(
                        onClick = runCheckAll,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                        shape = RoundedCornerShape(12.dp),
                        enabled = healthStates.values.any { it.isConfigured && !it.isChecking },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White)
                            Text("بررسی سراسری تمام درگاه‌ها", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// Reusable API Connection Guides, Popups, and Service Expandable Section
// ---------------------------------------------------------------------

data class ApiGuideInfo(
    val title: String,
    val description: String,
    val regSteps: List<String>,
    val apiSteps: List<String>,
    val manageSteps: List<String>,
    val freeTier: String,
    val limits: String,
    val linkUrl: String,
    val linkLabel: String
)

val geminiGuide = ApiGuideInfo(
    title = "راهنمای اتصال گوگل جمینای (Google Gemini)",
    description = "سرویس هوش مصنوعی شرکت گوگل با مدل‌های سریع و قدرتمند جمینای که امکان دسترسی رایگان به کلیدهای API را نیز در اختیار برنامه‌نویسان قرار می‌دهد.",
    regSteps = listOf(
        "۱. ابتدا وارد سایت Google AI Studio شوید.",
        "۲. با حساب کاربری گوگل (GMAIL) خود ثبت‌نام کنید.",
        "۳. شرایط قوانین استفاده از هوش مصنوعی گوگل را تایید کنید."
    ),
    apiSteps = listOf(
        "۱. بر روی دکمه Get API Key کلیک کنید.",
        "۲. گزینه Create API Key را بفشارید.",
        "۳. یک پروژه موجود یا جدید را انتخاب کرده و کلید خود را کپی کنید."
    ),
    manageSteps = listOf(
        "شما همیشه می‌توانید کلیدهای خود را در پنل کاربری گوگل AI Studio مدیریت کنید."
    ),
    freeTier = "تا ۱۵ درخواست در دقیقه (RPM) به صورت کاملاً رایگان در مدل Gemini 1.5 Flash",
    limits = "نیازمند ابزار عبور از تحریم (پروکسی ممیزی شده) در صورت بسته بودن ارتباط مستقیم",
    linkUrl = "https://aistudio.google.com/app/apikey",
    linkLabel = "دریافت کلید رسمی Gemini"
)

val openRouterGuide = ApiGuideInfo(
    title = "راهنمای اتصال اپن‌روتر (OpenRouter)",
    description = "یک درگاه تجمیع‌کننده واسط پیشرفته که صدها مدل متنوع هوش مصنوعی (مانند DeepSeek, Llama 3, GPT, Claude) را با یک کلید واحد در اختیار شما می‌گذارد.",
    regSteps = listOf(
        "۱. وارد وب‌سایت openrouter.ai شوید.",
        "۲. با ایمیل یا کیف پول اکانت خود را ایجاد کنید."
    ),
    apiSteps = listOf(
        "۱. به بخش Keys در داشبورد خود بروید.",
        "۲. دکمه Create Key را بزنید.",
        "۳. نام کلید را مشخص کرده و کلید تولید شده را ذخیره نمایید."
    ),
    manageSteps = listOf(
        "تراکنش‌ها، شارژ حساب کاربری و لیست کلیدها کاملاً در بخش Settings/Keys مدیریت می‌شوند."
    ),
    freeTier = "دارای چندین مدل هوش مصنوعی کاملاً رایگان از جمله DeepSeek Llama 3 و Mistral",
    limits = "ندارد (بدون نیاز به تحریم شکن از سرورهای ما)",
    linkUrl = "https://openrouter.ai/keys",
    linkLabel = "دریافت کلید OpenRouter"
)

val openAiGuide = ApiGuideInfo(
    title = "راهنمای اتصال اپن‌ای‌آی (OpenAI)",
    description = "توسعه‌دهنده مدل‌های پیشگام هوش مصنوعی از جمله سری GPT-4o و GPT-4.",
    regSteps = listOf(
        "۱. وارد پلتفرم رسمی platform.openai.com شوید.",
        "۲. حساب کاربری جدید ایجاد کنید و ترجیحاً شماره تلفن معتبری ثبت کنید."
    ),
    apiSteps = listOf(
        "۱. وارد منوی API Keys از داشبورد چپ شوید.",
        "۲. دکمه Create new secret key را کلیک کنید.",
        "۳. نام و دسترسی‌ها را تایید کرده و کلید sk-proj را کپی کنید."
    ),
    manageSteps = listOf(
        "میزان مصرف و پایش کلیدها در منوی Usage و API Keys در پلتفرم قابل دسترسی است."
    ),
    freeTier = "مقداری اعتبار هدیه محدود برای حساب‌های جدید",
    limits = "نیاز به پرداخت ارزی مجزا برای استفاده طولانی مدت در مدل‌های حرفه‌ای",
    linkUrl = "https://platform.openai.com/api-keys",
    linkLabel = "دریافت کلید OpenAI"
)

val groqGuide = ApiGuideInfo(
    title = "راهنمای اتصال گراک (Groq Cloud)",
    description = "یکی از سریع‌ترین موتورهای استنتاج سخت‌افزاری دنیا با پشتیبانی عالی و رایگان از مدل‌های لاما به شدت پرسرعت.",
    regSteps = listOf(
        "۱. وارد پرتال Groq Console شوید.",
        "۲. با اکانت گوگل یا ایمیل خود ثبت‌نام کنید."
    ),
    apiSteps = listOf(
        "۱. در منوی سمت چپ به بخش API Keys بروید.",
        "۲. دکمه Create API Key را انتخاب کنید.",
        "۳. کلید gsk- را رونوشت نمایید."
    ),
    manageSteps = listOf(
        "مشاهدۀ آمار فراخوانی در قسمت Usage داشبورد امکان‌پذیر است."
    ),
    freeTier = "بسیار سخاوتمندانه و با سرعت باورنکردنی (رایگان با محدودیت نرخ معقول)",
    limits = "سرعت بالا اما صرفاً برای مدل‌های متن‌باز مثل لاما",
    linkUrl = "https://console.groq.com/keys",
    linkLabel = "دریافت کلید Groq"
)

val cohereGuide = ApiGuideInfo(
    title = "راهنمای اتصال کوهر (Cohere AI)",
    description = "سرویس ویژه متمرکز بر ساختار متون سازمانی و تولید اسناد حقوقی با مدل موفق Command-R.",
    regSteps = listOf(
        "۱. به پرتال توسعه‌دهندگان cohere.com بروید.",
        "۲. ثبت نام خود را نهایی کنید."
    ),
    apiSteps = listOf(
        "۱. از منوی اصلی داشبورد به بخش API Keys وارد شوید.",
        "۲. کلید تستی یا تجاری دریافت کنید."
    ),
    manageSteps = listOf(
        "مدیریت درگاه پرداخت و سقف بودجه از زبانه Billing."
    ),
    freeTier = "استفاده رایگان در حالت تستی و برنامه‌نویسی غیرتجاری",
    limits = "سرعت محدود در لایسنس رایگان",
    linkUrl = "https://dashboard.cohere.com/api-keys",
    linkLabel = "دریافت کلید Cohere"
)

val huggingFaceGuide = ApiGuideInfo(
    title = "راهنمای اتصال هاگینگ فیس (Hugging Face)",
    description = "بزرگترین مخزن مدل‌های هوش مصنوعی دنیا با امکان اجرای رایگان هزاران مدل متن باز.",
    regSteps = listOf(
        "۱. وارد huggingface.co شوید.",
        "۲. حساب کاربری بسازید."
    ),
    apiSteps = listOf(
        "۱. به بخش Settings و سپس Access Tokens کاربری خود بروید.",
        "۲. روی Create new token کلیک کنید.",
        "۳. نوع توکن را Read مشخص کرده و کلید hf_ را بردارید."
    ),
    manageSteps = listOf(
        "مدیریت کلیدها و دسترسی‌ها از طریق صفحه تنظیمات توکن‌ها."
    ),
    freeTier = "رایگان برای فراخوانی مدل‌های عمومی",
    limits = "محدودیت سرعت پردازش در سرورهای اشتراکی بدون هاست اختصاصی",
    linkUrl = "https://huggingface.co/settings/tokens",
    linkLabel = "دریافت توکن Hugging Face"
)

val youcomGuide = ApiGuideInfo(
    title = "راهنمای اتصال یوکام (You.com)",
    description = "سرویس هوش مصنوعی You.com برای جستجوی وب زنده و دسترسی به اطلاعات لایو وب جهانی.",
    regSteps = listOf(
        "۱. ابتدا وارد سایت api.you.com شوید.",
        "۲. یک حساب کاربری بسازید."
    ),
    apiSteps = listOf(
        "۱. به بخش API Keys در پنل کاربری بروید.",
        "۲. روی Create API Key کلیک کنید.",
        "۳. کلید جدید را کپی کرده و در این کادر قرار دهید."
    ),
    manageSteps = listOf(
        "مدیریت کلیدها از بخش داشبورد api.you.com انجام می‌شود."
    ),
    freeTier = "سهمیه آزمایشی برای توسعه‌دهندگان",
    limits = "۵,۰۰۰ درخواست در روز برای درگاه آزمایشی",
    linkUrl = "https://api.you.com",
    linkLabel = "دریافت کلید You.com"
)

@Composable
fun KeyStatusDetailPopup(
    providerName: String,
    keyMasked: String,
    statusState: ServiceHealthState,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .border(1.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(24.dp))
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                    }

                    Text(
                        text = "جزئیات سهمیه و سلامت کلید",
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.08f))

                // Key Mask Info
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    Text("درگاه ارائه‌دهنده:", style = Typography.labelSmall, color = TextSecondaryFarsi)
                    Text(providerName, style = Typography.bodyMedium, fontWeight = FontWeight.Bold, color = AccentGold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("مقدار کلید (پوشش امنیتی):", style = Typography.labelSmall, color = TextSecondaryFarsi)
                    Text(keyMasked, style = Typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                }

                // Status row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PulsingHealthLed(
                            isConfigured = statusState.isConfigured,
                            isChecking = statusState.isChecking,
                            checked = statusState.checked,
                            isValid = statusState.isValid,
                            connectionOk = statusState.connectionOk,
                            isRateLimited = statusState.isRateLimited
                        )
                        Text(
                            text = when {
                                statusState.isChecking -> "در حال ارزیابی..."
                                !statusState.isConfigured -> "پیکربندی نشده"
                                !statusState.checked -> "در انتظار عیارسنجی"
                                statusState.isValid && statusState.connectionOk && !statusState.isRateLimited -> "اتصال فعال و پاسخگو"
                                statusState.isValid && statusState.isRateLimited -> "محدودیت ترافیکی (Rate Limited)"
                                statusState.isValid && !statusState.connectionOk -> "اختلال موقت شبکه / فیلترینگ"
                                else -> "کلید نامعتبر یا منقضی"
                            },
                            style = Typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                statusState.isChecking -> Color(0xFF3B82F6)
                                !statusState.isConfigured -> Color.White.copy(alpha = 0.3f)
                                !statusState.checked -> Color.White.copy(alpha = 0.6f)
                                statusState.isValid && statusState.connectionOk && !statusState.isRateLimited -> Color(0xFF10B981)
                                statusState.isValid && statusState.isRateLimited -> Color(0xFFF59E0B)
                                statusState.isValid && !statusState.connectionOk -> Color(0xFFF59E0B)
                                else -> Color(0xFFEF4444)
                            }
                        )
                    }

                    Text("وضعیت نهایی:", style = Typography.labelSmall, color = TextSecondaryFarsi)
                }

                // Message
                if (statusState.checked && statusState.message.isNotEmpty()) {
                    Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                        Text("جزئیات پاسخ سرور یا خطا:", style = Typography.labelSmall, color = TextSecondaryFarsi)
                        Text(statusState.message, style = Typography.bodySmall, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Right)
                    }
                }

                // Quota bar & recharging
                if (statusState.checked && statusState.isValid) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format("%.0f%% مصرف شده", statusState.usagePercent * 100),
                                style = Typography.labelSmall,
                                color = if (statusState.usagePercent > 0.8f) Color(0xFFEF4444) else SoftEmerald
                            )
                            Text("وضعیت سهمیه مصرف:", style = Typography.labelSmall, color = TextSecondaryFarsi)
                        }

                        // Linear Progress
                        LinearProgressIndicator(
                            progress = { statusState.usagePercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (statusState.usagePercent > 0.8f) Color(0xFFEF4444) else SoftEmerald,
                            trackColor = Color.White.copy(alpha = 0.08f)
                        )

                        if (statusState.quotaDetails.isNotEmpty()) {
                            Text(
                                text = statusState.quotaDetails,
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Right,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        if (statusState.rechargeInfo.isNotEmpty()) {
                            Text(
                                text = statusState.rechargeInfo,
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = AccentGold.copy(alpha = 0.8f),
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Close Button (Touch target 48dp)
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("تایید و خروج", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun ApiKeyServiceSection(
    title: String,
    guideTitle: String,
    onOpenGuide: () -> Unit,
    keys: List<String>,
    onKeysChange: (List<String>) -> Unit,
    placeholderPrefix: String
) {
    val coroutineScope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(false) }
    var visualTransfMap by remember { mutableStateOf(mapOf<Int, Boolean>()) }
    
    val resolvedProviderId = remember(placeholderPrefix) {
        when {
            placeholderPrefix.contains("جمینای") || placeholderPrefix.contains("جمینی") -> "Gemini"
            placeholderPrefix.contains("اپن‌ای‌آی") || placeholderPrefix.contains("OpenAI") -> "OpenAI"
            placeholderPrefix.contains("اپن‌روتر") || placeholderPrefix.contains("OpenRouter") -> "OpenRouter"
            placeholderPrefix.contains("گراک") || placeholderPrefix.contains("Groq") -> "Groq"
            placeholderPrefix.contains("کوهر") || placeholderPrefix.contains("Cohere") -> "Cohere"
            placeholderPrefix.contains("هاگینگ") || placeholderPrefix.contains("Hugging") -> "HuggingFace"
            placeholderPrefix.contains("یوکام") || placeholderPrefix.contains("You") || placeholderPrefix.contains("YouCom") -> "YouCom"
            else -> "Gemini"
        }
    }

    var keyHealthStates by remember(keys) {
        mutableStateOf(
            keys.map { key ->
                ServiceHealthState(
                    providerId = resolvedProviderId,
                    providerName = resolvedProviderId,
                    keyUsed = key,
                    isConfigured = key.isNotBlank()
                )
            }
        )
    }

    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            keyHealthStates.forEachIndexed { i, state ->
                if (state.isConfigured && !state.checked && !state.isChecking) {
                    coroutineScope.launch {
                        keyHealthStates = keyHealthStates.toMutableList().apply {
                            this[i] = state.copy(isChecking = true, message = "در حال ارزیابی...")
                        }
                        val res = ApiKeyVerifier.verifyKeyReal(resolvedProviderId, state.keyUsed)
                        val rateLimited = res.usagePercent >= 1.0f
                        keyHealthStates = keyHealthStates.toMutableList().apply {
                            this[i] = state.copy(
                                isChecking = false,
                                checked = true,
                                isValid = res.isValid,
                                connectionOk = res.connectionOk,
                                isRateLimited = rateLimited,
                                message = res.message,
                                usagePercent = res.usagePercent,
                                quotaDetails = res.quotaDetails,
                                rechargeInfo = res.rechargeInfo,
                                lastCheckedTime = "الان"
                            )
                        }
                    }
                }
            }
        }
    }

    var activePopupState by remember { mutableStateOf<Pair<Int, ServiceHealthState>?>(null) }

    if (activePopupState != null) {
        val (index, state) = activePopupState!!
        val maskedKey = if (state.keyUsed.length > 15) {
            state.keyUsed.take(7) + "••••••••" + state.keyUsed.takeLast(6)
        } else {
            "••••••••••••"
        }
        KeyStatusDetailPopup(
            providerName = resolvedProviderId,
            keyMasked = maskedKey,
            statusState = state,
            onDismiss = { activePopupState = null }
        )
    }

    val configuredCount = keys.count { it.isNotBlank() }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E293B).copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = PersianFirstUtils.formatDigits("$configuredCount از ۳ کلید"),
                        style = Typography.labelSmall,
                        color = if (configuredCount > 0) SoftEmerald else Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(
                                if (configuredCount > 0) SoftEmerald.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )

                    Text(
                        text = title,
                        style = Typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Icon(
                        imageVector = when (resolvedProviderId) {
                            "Gemini" -> Icons.Default.AutoAwesome
                            "OpenAI" -> Icons.Default.Layers
                            "OpenRouter" -> Icons.Default.Hub
                            "Groq" -> Icons.Default.Bolt
                            "Cohere" -> Icons.Default.Language
                            "YouCom" -> Icons.Default.Cloud
                            else -> Icons.Default.Key
                        },
                        contentDescription = null,
                        tint = if (configuredCount > 0) AccentGold else Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (isExpanded) {
                Divider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = guideTitle,
                        style = Typography.labelMedium,
                        color = AccentGold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onOpenGuide() }
                            .background(AccentGold.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )

                    Text(
                        text = "ترتیب اولویت استفاده از بالا به پایین می‌باشد:",
                        style = Typography.labelSmall,
                        color = TextSecondaryFarsi
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    keys.forEachIndexed { i, key ->
                        val isVisible = visualTransfMap[i] ?: false
                        val healthState = keyHealthStates.getOrNull(i) ?: ServiceHealthState(
                            providerId = resolvedProviderId,
                            providerName = resolvedProviderId,
                            keyUsed = key,
                            isConfigured = key.isNotBlank()
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (keys.size > 1) {
                                        IconButton(
                                            onClick = {
                                                val updated = keys.toMutableList()
                                                updated.removeAt(i)
                                                onKeysChange(updated)
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "حذف",
                                                tint = CoralRedDark.copy(alpha = 0.85f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                keyHealthStates = keyHealthStates.toMutableList().apply {
                                                    this[i] = healthState.copy(isChecking = true, message = "در حال اتصال...")
                                                }
                                                val res = ApiKeyVerifier.verifyKeyReal(resolvedProviderId, key)
                                                val rateLimited = res.usagePercent >= 1.0f
                                                val finalState = healthState.copy(
                                                    isChecking = false,
                                                    checked = true,
                                                    isValid = res.isValid,
                                                    connectionOk = res.connectionOk,
                                                    isRateLimited = rateLimited,
                                                    message = res.message,
                                                    usagePercent = res.usagePercent,
                                                    quotaDetails = res.quotaDetails,
                                                    rechargeInfo = res.rechargeInfo,
                                                    lastCheckedTime = "الان"
                                                )
                                                keyHealthStates = keyHealthStates.toMutableList().apply {
                                                    this[i] = finalState
                                                }
                                                activePopupState = i to finalState
                                            }
                                        },
                                        modifier = Modifier
                                            .size(24.dp)
                                            .background(Color.White.copy(alpha = 0.04f), RoundedCornerShape(6.dp))
                                    ) {
                                        if (healthState.isChecking) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(10.dp),
                                                strokeWidth = 1.5.dp,
                                                color = AccentGold
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Assessment,
                                                contentDescription = "وضعیت",
                                                tint = if (healthState.checked && healthState.isValid) SoftEmerald else AccentGold,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = PersianFirstUtils.formatDigits("کلید ${i + 1}"),
                                        style = Typography.labelSmall.copy(fontSize = 11.sp),
                                        color = AccentGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    PulsingHealthLed(
                                        isConfigured = healthState.isConfigured,
                                        isChecking = healthState.isChecking,
                                        checked = healthState.checked,
                                        isValid = healthState.isValid,
                                        connectionOk = healthState.connectionOk,
                                        isRateLimited = healthState.isRateLimited
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = key,
                                onValueChange = { newVal ->
                                    val updated = keys.toMutableList()
                                    updated[i] = newVal
                                    onKeysChange(updated)
                                },
                                placeholder = {
                                    Text(
                                        text = "کلید...",
                                        color = TextSecondaryFarsi.copy(alpha = 0.4f),
                                        fontSize = 10.sp
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                textStyle = Typography.bodyMedium.copy(fontSize = 11.sp),
                                visualTransformation = if (isVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(
                                        onClick = { visualTransfMap = visualTransfMap + (i to !isVisible) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                            contentDescription = null,
                                            tint = TextSecondaryFarsi.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = AccentGold,
                                    unfocusedBorderColor = GlassBorderLight.copy(alpha = 0.4f),
                                    focusedContainerColor = Color.Black.copy(alpha = 0.15f),
                                    unfocusedContainerColor = Color.Black.copy(alpha = 0.15f)
                                )
                            )

                            if (healthState.checked) {
                                val statusText = if (healthState.isValid) {
                                    PersianFirstUtils.formatDigits("فعال (${String.format("%.0f%%", healthState.usagePercent * 100)})")
                                } else {
                                    "نامعتبر"
                                }
                                Text(
                                    text = statusText,
                                    style = Typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (healthState.isValid) SoftEmerald else Color(0xFFEF4444),
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                if (keys.size < 3) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val updated = keys.toMutableList() + ""
                                onKeysChange(updated)
                            }
                            .background(SoftEmerald.copy(alpha = 0.15f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .align(Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "افزودن کلید",
                            tint = SoftEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "افزودن کلید جدید",
                            style = Typography.labelSmall,
                            color = SoftEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
