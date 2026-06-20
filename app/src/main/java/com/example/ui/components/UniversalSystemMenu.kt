package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.theme.*
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.UserRole

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun UniversalSystemMenu(
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel,
    isOpen: Boolean,
    onClose: () -> Unit,
    onNavigateToCitizenTab: (String) -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val isDark by authViewModel.isDarkTheme.collectAsState()
    val isDynamicBg by authViewModel.isDynamicBackground.collectAsState()
    val session by authViewModel.session.collectAsState()

    val surfaceColor = MaterialTheme.colorScheme.surface
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val glassBorderColor = if (isDark) GlassBorderDark else Color(0x33000000)

    val curGeminiKeys by adminViewModel.geminiApiKeys.collectAsState()
    val curOpenRouterKeys by adminViewModel.openrouterApiKeys.collectAsState()
    val curOpenAiKeys by adminViewModel.openaiApiKeys.collectAsState()
    val curGroqKeys by adminViewModel.groqApiKeys.collectAsState()
    val curCohereKeys by adminViewModel.cohereApiKeys.collectAsState()
    val curHuggingFaceKeys by adminViewModel.huggingfaceApiKeys.collectAsState()

    var tempGeminiKeys by remember(curGeminiKeys) { mutableStateOf(curGeminiKeys) }
    var tempOpenRouterKeys by remember(curOpenRouterKeys) { mutableStateOf(curOpenRouterKeys) }
    var tempOpenAiKeys by remember(curOpenAiKeys) { mutableStateOf(curOpenAiKeys) }
    var tempGroqKeys by remember(curGroqKeys) { mutableStateOf(curGroqKeys) }
    var tempCohereKeys by remember(curCohereKeys) { mutableStateOf(curCohereKeys) }
    var tempHuggingFaceKeys by remember(curHuggingFaceKeys) { mutableStateOf(curHuggingFaceKeys) }

    var proxyUrl by remember { mutableStateOf(adminViewModel.geminiProxyUrl.value) }

    var activeMenuTab by remember { mutableStateOf("quick") } // quick, apis, theme, admin
    var apiKeysSavedSuccess by remember { mutableStateOf(false) }

    var geminiFieldsToShow by remember { mutableStateOf(1) }
    var openRouterFieldsToShow by remember { mutableStateOf(1) }
    var openAiFieldsToShow by remember { mutableStateOf(1) }
    var groqFieldsToShow by remember { mutableStateOf(1) }
    var cohereFieldsToShow by remember { mutableStateOf(1) }
    var huggingFaceFieldsToShow by remember { mutableStateOf(1) }

    val revealedKeys = remember { mutableStateMapOf<String, Boolean>() }

    // Backdrop blur representation
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { onClose() }
            .testTag("universal_system_menu_backdrop"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clickable(enabled = false) {}
                .glassy3D(cornerRadius = 24.dp, glowColor = AccentGold.copy(alpha = 0.1f)),
            colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_universal_menu_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Menu", tint = SoftCrimson)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "سامانه فرادست و پیشخوان هوشمند دادرس",
                            style = Typography.titleMedium,
                            color = AccentGold,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Menu, contentDescription = null, tint = AccentGold)
                    }
                }

                Divider(color = glassBorderColor, modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable Tabs bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (session?.role == UserRole.ADMIN) {
                        TabHeaderItem(
                            label = "کنترل ادمین",
                            icon = Icons.Default.AdminPanelSettings,
                            isSelected = activeMenuTab == "admin",
                            onClick = { activeMenuTab = "admin" }
                        )
                    }

                    TabHeaderItem(
                        label = "تنظیم تم و پوسته",
                        icon = Icons.Default.Palette,
                        isSelected = activeMenuTab == "theme",
                        onClick = { activeMenuTab = "theme" }
                    )

                    TabHeaderItem(
                        label = "تنظیم کلیدهای API",
                        icon = Icons.Default.VpnKey,
                        isSelected = activeMenuTab == "apis",
                        onClick = { activeMenuTab = "apis" }
                    )

                    TabHeaderItem(
                        label = "پیشخوان فرادست",
                        icon = Icons.Default.Widgets,
                        isSelected = activeMenuTab == "quick",
                        onClick = { activeMenuTab = "quick" }
                    )
                }

                // Main Tab Contents Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeMenuTab) {
                        "quick" -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                item {
                                    Text(
                                        text = "دسترسی فوری به پیشخوان‌ها و مراجع کارتابل",
                                        style = Typography.titleSmall,
                                        color = onBgColor,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                }

                                item {
                                    QuickGridItem(
                                        title = "صفحه نخست پیشخوان دادرس",
                                        desc = "کارپوشه اسناد، لیست پرونده‌های موکلین و آمار خدمات",
                                        icon = Icons.Default.Home,
                                        onClick = {
                                            onClose()
                                            onNavigateToCitizenTab("dashboard")
                                        }
                                    )
                                }

                                item {
                                    QuickGridItem(
                                        title = "تنظیم و هوشمندسازی عریضه‌نویسی دادرس",
                                        desc = "تنظیم خودکار شکواییه، داخواست و لوایح قضایی از مدارک",
                                        icon = Icons.Default.Build,
                                        onClick = {
                                            onClose()
                                            navController.navigate("wizard")
                                        }
                                    )
                                }

                                item {
                                    QuickGridItem(
                                        title = "کتابخانه قوانین و مقررات مدنی",
                                        desc = "مرجع دسترسی الکترونیک به قوانین مجازات اسلامی، آیین دادرسی و مدنی کشور",
                                        icon = Icons.Default.LibraryBooks,
                                        onClick = {
                                            onClose()
                                            navController.navigate("library")
                                        }
                                    )
                                }

                                item {
                                    QuickGridItem(
                                        title = "کارتابل عریضه‌ها و لوایح فعال",
                                        desc = "پیگیری پرونده‌ها، زمان‌بندی دادرسی بصری و اسناد تایید شده",
                                        icon = Icons.Default.FolderOpen,
                                        onClick = {
                                            onClose()
                                            onNavigateToCitizenTab("cases")
                                        }
                                    )
                                }

                                item {
                                    QuickGridItem(
                                        title = "پروفایل هویت فارسی کاربر",
                                        desc = "اطلاعات هویتی ثنا، صندوق پیام‌ها و سطح تایید اعتبار کاربری",
                                        icon = Icons.Default.Person,
                                        onClick = {
                                            onClose()
                                            onNavigateToCitizenTab("profile")
                                        }
                                    )
                                }

                                item {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = {
                                            onClose()
                                            authViewModel.logout()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftCrimson),
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("خروج ایمن از کاربری فعلی", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        "apis" -> {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "مدیریت و ثبت کلیدهای امنیتی شخصی (API Web SDK)",
                                    style = Typography.titleSmall,
                                    color = onBgColor,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right
                                )
                                Text(
                                    text = "جهت پایداری فوق‌العاده و دادرسی سریع، می‌توانید کلیدهای خود را به برنامه بدهید. این کلیدها با رمزگذاری چندوجهی تنها روی حافظه محلی تلفن همراه شما ثبت شده و امنیت آن‌ها کاملاً تضمین شده است.",
                                    style = Typography.labelSmall,
                                    color = onSurfaceColor,
                                    textAlign = TextAlign.Right,
                                    lineHeight = 16.sp
                                )

                                // Proxy URL config
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text("آدرس سرور پروکسی اختصاصی (اختیاری جهت رفع فیلترینگ):", style = Typography.labelMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    OutlinedTextField(
                                        value = proxyUrl,
                                        onValueChange = { proxyUrl = it },
                                        placeholder = { Text("https://your-proxy-domain.com/v1/ ...", color = onSurfaceColor.copy(alpha = 0.5f), fontSize = 12.sp) },
                                        textStyle = Typography.bodySmall.copy(textAlign = TextAlign.Left),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AccentGold,
                                            unfocusedBorderColor = glassBorderColor,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )
                                }

                                Divider(color = glassBorderColor.copy(alpha = 0.5f))

                                // Providers key inputs
                                ProviderKeyInputItem(
                                    providerId = "gemini",
                                    title = "دروازه Google Gemini API (رایگان)",
                                    tempKeys = tempGeminiKeys,
                                    onKeyChange = { idx, v ->
                                        val updated = tempGeminiKeys.toMutableList()
                                        while (updated.size <= idx) updated.add("")
                                        updated[idx] = v
                                        tempGeminiKeys = updated
                                    },
                                    fieldsToShow = geminiFieldsToShow,
                                    onAddBackup = { geminiFieldsToShow = minOf(3, geminiFieldsToShow + 1) },
                                    isRevealed = revealedKeys["gemini"] ?: false,
                                    onToggleReveal = { revealedKeys["gemini"] = !(revealedKeys["gemini"] ?: false) }
                                )

                                ProviderKeyInputItem(
                                    providerId = "openrouter",
                                    title = "دروازه جامع OpenRouter API",
                                    tempKeys = tempOpenRouterKeys,
                                    onKeyChange = { idx, v ->
                                        val updated = tempOpenRouterKeys.toMutableList()
                                        while (updated.size <= idx) updated.add("")
                                        updated[idx] = v
                                        tempOpenRouterKeys = updated
                                    },
                                    fieldsToShow = openRouterFieldsToShow,
                                    onAddBackup = { openRouterFieldsToShow = minOf(3, openRouterFieldsToShow + 1) },
                                    isRevealed = revealedKeys["openrouter"] ?: false,
                                    onToggleReveal = { revealedKeys["openrouter"] = !(revealedKeys["openrouter"] ?: false) }
                                )

                                ProviderKeyInputItem(
                                    providerId = "openai",
                                    title = "دروازه رسمی OpenAI API (ChatGPT)",
                                    tempKeys = tempOpenAiKeys,
                                    onKeyChange = { idx, v ->
                                        val updated = tempOpenAiKeys.toMutableList()
                                        while (updated.size <= idx) updated.add("")
                                        updated[idx] = v
                                        tempOpenAiKeys = updated
                                    },
                                    fieldsToShow = openAiFieldsToShow,
                                    onAddBackup = { openAiFieldsToShow = minOf(3, openAiFieldsToShow + 1) },
                                    isRevealed = revealedKeys["openai"] ?: false,
                                    onToggleReveal = { revealedKeys["openai"] = !(revealedKeys["openai"] ?: false) }
                                )

                                ProviderKeyInputItem(
                                    providerId = "groq",
                                    title = "دروازه رایگان Groq API (پرسرعت)",
                                    tempKeys = tempGroqKeys,
                                    onKeyChange = { idx, v ->
                                        val updated = tempGroqKeys.toMutableList()
                                        while (updated.size <= idx) updated.add("")
                                        updated[idx] = v
                                        tempGroqKeys = updated
                                    },
                                    fieldsToShow = groqFieldsToShow,
                                    onAddBackup = { groqFieldsToShow = minOf(3, groqFieldsToShow + 1) },
                                    isRevealed = revealedKeys["groq"] ?: false,
                                    onToggleReveal = { revealedKeys["groq"] = !(revealedKeys["groq"] ?: false) }
                                )

                                ProviderKeyInputItem(
                                    providerId = "cohere",
                                    title = "دروازه چندزبانه Cohere API",
                                    tempKeys = tempCohereKeys,
                                    onKeyChange = { idx, v ->
                                        val updated = tempCohereKeys.toMutableList()
                                        while (updated.size <= idx) updated.add("")
                                        updated[idx] = v
                                        tempCohereKeys = updated
                                    },
                                    fieldsToShow = cohereFieldsToShow,
                                    onAddBackup = { cohereFieldsToShow = minOf(3, cohereFieldsToShow + 1) },
                                    isRevealed = revealedKeys["cohere"] ?: false,
                                    onToggleReveal = { revealedKeys["cohere"] = !(revealedKeys["cohere"] ?: false) }
                                )

                                ProviderKeyInputItem(
                                    providerId = "huggingface",
                                    title = "مکانیزم محلی Hugging Face HF Token",
                                    tempKeys = tempHuggingFaceKeys,
                                    onKeyChange = { idx, v ->
                                        val updated = tempHuggingFaceKeys.toMutableList()
                                        while (updated.size <= idx) updated.add("")
                                        updated[idx] = v
                                        tempHuggingFaceKeys = updated
                                    },
                                    fieldsToShow = huggingFaceFieldsToShow,
                                    onAddBackup = { huggingFaceFieldsToShow = minOf(3, huggingFaceFieldsToShow + 1) },
                                    isRevealed = revealedKeys["huggingface"] ?: false,
                                    onToggleReveal = { revealedKeys["huggingface"] = !(revealedKeys["huggingface"] ?: false) }
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        adminViewModel.saveMultiApiKeys(
                                            gemini = tempGeminiKeys,
                                            openRouter = tempOpenRouterKeys,
                                            openAi = tempOpenAiKeys,
                                            groq = tempGroqKeys,
                                            cohere = tempCohereKeys,
                                            huggingFace = tempHuggingFaceKeys,
                                            proxyUrl = proxyUrl
                                        )
                                        apiKeysSavedSuccess = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald),
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "بروزرسانی و ثبت نهایی کلیدها",
                                        style = Typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (apiKeysSavedSuccess) {
                                    Text(
                                        text = "تغییرات با موفقیت ذخیره شدند و فعال گردیدند!",
                                        color = SoftEmerald,
                                        style = Typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        "theme" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "پیکربندی تزیینی و تم‌های بصری سیستم دادرسی",
                                    style = Typography.titleSmall,
                                    color = onBgColor,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right
                                )

                                // Night mode toggle row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(surfaceColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .border(1.dp, glassBorderColor, RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Switch(
                                        checked = isDark,
                                        onCheckedChange = { authViewModel.toggleTheme() },
                                        modifier = Modifier.testTag("universal_theme_dark_switch")
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("حالت تاریک سیستم (شب / روز)", style = Typography.bodyMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                        Text("سوییچ میان تم سورا و لایت مجستیک", style = Typography.labelSmall, color = onSurfaceColor)
                                    }
                                }

                                // Dynamic Background toggle row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(surfaceColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .border(1.dp, glassBorderColor, RoundedCornerShape(12.dp))
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Switch(
                                        checked = isDynamicBg,
                                        onCheckedChange = { authViewModel.toggleDynamicBackground() },
                                        modifier = Modifier.testTag("universal_theme_dynamics_switch")
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("پویانمایی هوشمند پس‌زمینه", style = Typography.bodyMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                        Text("طراحی سه بعدی هاله شفق فضایی در پس زمینه", style = Typography.labelSmall, color = onSurfaceColor)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.3f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, glassBorderColor)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "معرفی پوسته‌های دکوراتیو دستگاه قضایی دادرس",
                                            style = Typography.bodyMedium,
                                            color = AccentGold,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Right
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "☀️ تم لایت لوکس سه بعدی: گستره‌ای مینی‌مالیستی از آلاباستر عاجی، مروارید روشن و هاله‌های کرم طلایی با رقص معلق اورب‌های کریستالی که فضا را بسیار دلباز و سنگین می‌سازد.",
                                            style = Typography.labelSmall,
                                            color = onBgColor,
                                            textAlign = TextAlign.Right,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "🌙 تم تاریک سورا ۲: برگرفته از طراحی عمیق سینمایی شرکت اُپن‌ای‌آی با فریم‌های مشکی ذغالی ابسیدین مخملی، شفق‌های سیال کرم زرشکی کوبالت هوش مصنوعی و ستاره‌های تابان کوهستانی دشت لوت ایران.",
                                            style = Typography.labelSmall,
                                            color = onBgColor,
                                            textAlign = TextAlign.Right,
                                            lineHeight = 18.sp
                                        )
                                    }
                                }
                            }
                        }

                        "admin" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = "مدیریت ارشد سیستم دادرس (مخصوص مدیران)",
                                    style = Typography.titleSmall,
                                    color = onBgColor,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right
                                )

                                Button(
                                    onClick = {
                                        onClose()
                                        navController.navigate("admin_panel")
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("ورود مستقیم به پنل مدیریت کل کشور", color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.3f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, glassBorderColor)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = "گزارش وضعیت سیستمی کاربری ادمین",
                                            style = Typography.bodyMedium,
                                            color = AccentGold,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "۱. شناسه امنیتی سرور دایرکتوری: ۲۹۱۸۱۷۲۵۲",
                                            style = Typography.labelSmall,
                                            color = onBgColor
                                        )
                                        Text(
                                            text = "۲. مصرف جاری پهنای باند سرور دیتابیس: سالم",
                                            style = Typography.labelSmall,
                                            color = onBgColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabHeaderItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AccentGold.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isSelected) AccentGold else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = Typography.labelSmall,
                color = if (isSelected) AccentGold else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) AccentGold else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun QuickGridItem(
    title: String,
    desc: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .glassy3D(cornerRadius = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.End
            ) {
                Text(text = title, style = Typography.bodySmall, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = desc, style = Typography.labelSmall, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Right)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccentGold.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = AccentGold, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ProviderKeyInputItem(
    providerId: String,
    title: String,
    tempKeys: List<String>,
    onKeyChange: (Int, String) -> Unit,
    fieldsToShow: Int,
    onAddBackup: () -> Unit,
    isRevealed: Boolean,
    onToggleReveal: () -> Unit
) {
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val glassBorderColor = if (onSurfaceColor.red + onSurfaceColor.green + onSurfaceColor.blue < 1.5f) Color(0x33000000) else GlassBorderDark

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .glassy3D(cornerRadius = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleReveal) {
                    Icon(
                        imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Reveal",
                        tint = AccentGold
                    )
                }

                Text(
                    text = title,
                    style = Typography.bodySmall,
                    color = AccentGold,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            for (i in 0 until fieldsToShow) {
                val keyValue = tempKeys.getOrNull(i) ?: ""
                OutlinedTextField(
                    value = keyValue,
                    onValueChange = { onKeyChange(i, it) },
                    visualTransformation = if (isRevealed) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    placeholder = { Text("کلید شماره ${i + 1} ...", color = onSurfaceColor.copy(alpha = 0.4f), fontSize = 11.sp) },
                    textStyle = Typography.bodySmall.copy(textAlign = TextAlign.Left),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold,
                        unfocusedBorderColor = glassBorderColor,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            if (fieldsToShow < 3) {
                TextButton(
                    onClick = onAddBackup,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Text(text = "افزودن کلید پشتیبان (زاپاس) ${fieldsToShow + 1}", color = AccentGold, style = Typography.labelSmall)
                }
            }
        }
    }
}
