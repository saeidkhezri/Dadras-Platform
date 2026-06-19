package com.example.ui.screens

import kotlinx.coroutines.launch

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CaseEntity
import com.example.ui.components.FrostedGlassBackground
import com.example.ui.components.glassy3D
import com.example.ui.theme.*
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.CitizenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenDashboardScreen(
    authViewModel: AuthViewModel,
    citizenViewModel: CitizenViewModel,
    adminViewModel: com.example.viewmodel.AdminViewModel,
    initialTab: String = "dashboard",
    onTriggerSystemMenu: () -> Unit = {},
    onNavigateToWizard: () -> Unit,
    onNavigateToCase: (Int) -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val session by authViewModel.session.collectAsState()
    val cases by citizenViewModel.cases.collectAsState()
    val notifications by citizenViewModel.notifications.collectAsState()
    val isDark by authViewModel.isDarkTheme.collectAsState()
    val isDynamicBg by authViewModel.isDynamicBackground.collectAsState()

    var activeTab by remember(initialTab) { mutableStateOf(initialTab) } // dashboard, cases, research, timeline, notifications, profile, settings
    var searchQuery by remember { mutableStateOf("") }

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

    var geminiFieldsToShow by remember(curGeminiKeys) {
        mutableStateOf(
            if (curGeminiKeys.getOrNull(2)?.isNotBlank() == true) 3
            else if (curGeminiKeys.getOrNull(1)?.isNotBlank() == true) 2
            else 1
        )
    }
    var openRouterFieldsToShow by remember(curOpenRouterKeys) {
        mutableStateOf(
            if (curOpenRouterKeys.getOrNull(2)?.isNotBlank() == true) 3
            else if (curOpenRouterKeys.getOrNull(1)?.isNotBlank() == true) 2
            else 1
        )
    }
    var openAiFieldsToShow by remember(curOpenAiKeys) {
        mutableStateOf(
            if (curOpenAiKeys.getOrNull(2)?.isNotBlank() == true) 3
            else if (curOpenAiKeys.getOrNull(1)?.isNotBlank() == true) 2
            else 1
        )
    }
    var groqFieldsToShow by remember(curGroqKeys) {
        mutableStateOf(
            if (curGroqKeys.getOrNull(2)?.isNotBlank() == true) 3
            else if (curGroqKeys.getOrNull(1)?.isNotBlank() == true) 2
            else 1
        )
    }
    var cohereFieldsToShow by remember(curCohereKeys) {
        mutableStateOf(
            if (curCohereKeys.getOrNull(2)?.isNotBlank() == true) 3
            else if (curCohereKeys.getOrNull(1)?.isNotBlank() == true) 2
            else 1
        )
    }
    var huggingFaceFieldsToShow by remember(curHuggingFaceKeys) {
        mutableStateOf(
            if (curHuggingFaceKeys.getOrNull(2)?.isNotBlank() == true) 3
            else if (curHuggingFaceKeys.getOrNull(1)?.isNotBlank() == true) 2
            else 1
        )
    }

    val revealedKeys = remember { mutableStateMapOf<String, Boolean>() }
    var showGuideForProvider by remember { mutableStateOf<String?>(null) }
    var apiKeysSavedSuccess by remember { mutableStateOf(false) }
    var prevTabBeforeApis by remember { mutableStateOf("settings") }
    
    // Theme-specific glass styles
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val glassBorderColor = if (isDark) GlassBorderDark else Color(0x33000000)

    val filteredCases = cases.filter {
        it.title.contains(searchQuery) || it.type.contains(searchQuery) || it.legalPosition.contains(searchQuery)
    }

    FrostedGlassBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth > 850.dp

            Row(modifier = Modifier.fillMaxSize()) {
                // DESKTOP LEFT SIDEBAR
                if (isWideScreen) {
                    Column(
                        modifier = Modifier
                            .width(260.dp)
                            .fillMaxHeight()
                            .background(surfaceColor)
                            .border(1.dp, glassBorderColor)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Header / Identity
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "میز خدمت کاربران عادی",
                                style = Typography.titleMedium,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Star, contentDescription = null, tint = AccentGold)
                        }

                        Divider(color = glassBorderColor, modifier = Modifier.padding(bottom = 8.dp))

                        val sidebarTabs = listOf(
                            Triple("dashboard", "صفحه نخست", Icons.Default.Home),
                            Triple("cases", "لیست پرونده‌ها", Icons.Default.Menu),
                            Triple("research", "جستجوی مراجع قانونی", Icons.Default.Search),
                            Triple("timeline", "مراحل رسیدگی", Icons.Default.Build),
                            Triple("notifications", "صندوق پیام‌ها", Icons.Default.Notifications),
                            Triple("profile", "پروفایل کاربری", Icons.Default.Person),
                            Triple("settings", "تنظیمات برنامه", Icons.Default.Settings)
                        )

                        sidebarTabs.forEach { (tabId, label, icon) ->
                            val isSelected = activeTab == tabId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) primaryColor.copy(alpha = 0.15f) else Color.Transparent)
                                    .clickable { activeTab = tabId }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = Typography.bodyMedium,
                                    color = if (isSelected) primaryColor else onBgColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) primaryColor else onSurfaceColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Custom FAB inside desktop sidebar
                        Button(
                            onClick = onNavigateToWizard,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("دادخواست جدید", style = Typography.bodyMedium, color = Color.Black, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        }
                    }
                }

                // MAIN CONTENT SECTION
                Scaffold(
                    modifier = Modifier.weight(1f),
                    containerColor = Color.Transparent,
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = "میز هوشمند دادرس ملی",
                                    style = Typography.titleLarge,
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = { authViewModel.logout() }) {
                                    Icon(Icons.Default.Close, contentDescription = "logout", tint = SoftCrimson)
                                }
                            },
                            actions = {
                                Box(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .background(surfaceColor, CircleShape)
                                        .border(1.dp, glassBorderColor, CircleShape)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = session?.username ?: "کاربر عادی",
                                        style = Typography.labelMedium,
                                        color = AccentGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                        )
                    },
                    bottomBar = {
                        if (!isWideScreen) {
                            NavigationBar(
                                containerColor = Color(0xD9050B18),
                                tonalElevation = 0.dp
                            ) {
                                NavigationBarItem(
                                    selected = activeTab == "profile",
                                    onClick = { activeTab = "profile" },
                                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    label = { Text("پروفایل", style = Typography.labelSmall) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = SlateNavyMedium, unselectedIconColor = onSurfaceColor, unselectedTextColor = onSurfaceColor)
                                )
                                NavigationBarItem(
                                    selected = activeTab == "research",
                                    onClick = { activeTab = "research" },
                                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                                    label = { Text("تحقیق RAG", style = Typography.labelSmall) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = SlateNavyMedium, unselectedIconColor = onSurfaceColor, unselectedTextColor = onSurfaceColor)
                                )
                                NavigationBarItem(
                                    selected = activeTab == "cases",
                                    onClick = { activeTab = "cases" },
                                    icon = { Icon(Icons.Default.Menu, contentDescription = null) },
                                    label = { Text("کارتابل", style = Typography.labelSmall) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = SlateNavyMedium, unselectedIconColor = onSurfaceColor, unselectedTextColor = onSurfaceColor)
                                )
                                NavigationBarItem(
                                    selected = activeTab == "dashboard",
                                    onClick = { activeTab = "dashboard" },
                                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                    label = { Text("داشبورد", style = Typography.labelSmall) },
                                    colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = SlateNavyMedium, unselectedIconColor = onSurfaceColor, unselectedTextColor = onSurfaceColor)
                                )
                            }
                        }
                    },
                    floatingActionButton = {},
                    floatingActionButtonPosition = FabPosition.Center
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        when (activeTab) {
                            "dashboard" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "شاخص عملکرد و آمار مستندات پرونده",
                                        style = Typography.headlineMedium,
                                        color = onBgColor,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (!isWideScreen) {
                                        CollapsibleNewRequestTab(
                                            isDark = isDark,
                                            surfaceColor = surfaceColor,
                                            glassBorderColor = glassBorderColor,
                                            onBgColor = onBgColor,
                                            onSurfaceColor = onSurfaceColor,
                                            onNavigateToWizard = onNavigateToWizard
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier.weight(1f).glassy3D(cornerRadius = 16.dp, glowColor = primaryColor.copy(alpha = 0.08f)),
                                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Menu, contentDescription = null, tint = primaryColor, modifier = Modifier.size(28.dp))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(cases.size.toString(), style = Typography.displayMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                Text("پرونده ثبتی", style = Typography.labelMedium, color = onSurfaceColor)
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.weight(1f).glassy3D(cornerRadius = 16.dp, glowColor = AccentGold.copy(alpha = 0.08f)),
                                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = AccentGold, modifier = Modifier.size(28.dp))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("%۹۴", style = Typography.displayMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                Text("ضریب اطمینان RAG", style = Typography.labelMedium, color = onSurfaceColor)
                                            }
                                        }
                                    }

                                    Card(
                                        modifier = Modifier.fillMaxWidth().glassy3D(cornerRadius = 16.dp, glowColor = AccentGold.copy(alpha = 0.08f)),
                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("راهنمای سامانه دادرس هوشمند", style = Typography.titleMedium, color = AccentGold, fontWeight = FontWeight.Bold)
                                            Divider(color = glassBorderColor)
                                            Text(
                                                text = "این پلتفرم برای کمک به کاربران عادی و تنظیم دادخواست‌های منطبق بر موازین قضایی جمهوری اسلامی ایران به وسیله هوش مصنوعی طراحی شده است. از میان‌برهای زیر استفاده کنید غوطه ور در جلوه شیشه‌ای Liquid Glass.",
                                                style = Typography.bodyMedium,
                                                color = onSurfaceColor,
                                                textAlign = TextAlign.Right
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Button(
                                                onClick = onNavigateToLibrary,
                                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                                modifier = Modifier.align(Alignment.Start)
                                            ) {
                                                Text("ورود به کتابخانه قوانین ملی", color = Color.White)
                                            }
                                        }
                                    }

                                    // Quick Shortcuts
                                    Text("بخش‌های دسترسی سریع", style = Typography.titleMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        listOf(
                                            Pair("شروع به تولید مستقیم شکواییه قضایی", "new_wizard"),
                                            Pair("بررسی قوانین مالیات و حقوق تجارت", "library"),
                                            Pair("تاریخچه دادرسی و تقویم پرونده", "timeline")
                                        ).forEach { (label, act) ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .glassy3D(cornerRadius = 12.dp, glowColor = primaryColor.copy(alpha = 0.04f))
                                                    .clickable {
                                                        if (act == "new_wizard") onNavigateToWizard()
                                                        else if (act == "library") onNavigateToLibrary()
                                                        else activeTab = "timeline"
                                                    }
                                                    .padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = null, tint = primaryColor)
                                                Text(label, style = Typography.bodyMedium, color = onBgColor, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }

                            "cases" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "کارتابل پرونده‌های کاربر عادی",
                                        style = Typography.headlineMedium,
                                        color = onBgColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        textAlign = TextAlign.Right
                                    )

                                    if (!isWideScreen) {
                                        CollapsibleNewRequestTab(
                                            isDark = isDark,
                                            surfaceColor = surfaceColor,
                                            glassBorderColor = glassBorderColor,
                                            onBgColor = onBgColor,
                                            onSurfaceColor = onSurfaceColor,
                                            onNavigateToWizard = onNavigateToWizard
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                    }

                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("جستجو در بین پرونده‌ها...", color = onSurfaceColor) },
                                        singleLine = true,
                                        trailingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentGold) },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = onBgColor,
                                            unfocusedTextColor = onBgColor,
                                            focusedBorderColor = AccentGold,
                                            unfocusedBorderColor = glassBorderColor,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )

                                    if (filteredCases.isEmpty()) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(Icons.Default.Info, contentDescription = null, tint = onSurfaceColor, modifier = Modifier.size(56.dp))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("پرونده‌ای یافت نشد.", style = Typography.titleMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        LazyColumn(
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(filteredCases) { case ->
                                                CaseCardItem(
                                                    case = case,
                                                    onClick = { onNavigateToCase(case.id) },
                                                    onDelete = { citizenViewModel.deleteCase(case.id, session?.username ?: "کاربر عادی") }
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            "research" -> {
                                var researchQuery by remember { mutableStateOf("") }
                                var isSearchingRAG by remember { mutableStateOf(false) }
                                var searchResults by remember { mutableStateOf<List<String>>(emptyList()) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "بخش جستجو و تحقیق قوانین ملی",
                                        style = Typography.headlineMedium,
                                        color = onBgColor,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right
                                    )

                                    if (!isWideScreen) {
                                        CollapsibleNewRequestTab(
                                            isDark = isDark,
                                            surfaceColor = surfaceColor,
                                            glassBorderColor = glassBorderColor,
                                            onBgColor = onBgColor,
                                            onSurfaceColor = onSurfaceColor,
                                            onNavigateToWizard = onNavigateToWizard
                                        )
                                    }
                                    Text(
                                        text = "جستجوی واژه‌محور و برداری (Hybrid Search) در آرشیو آرای وحدت رویه و قوانین ملی:",
                                        style = Typography.bodyMedium,
                                        color = onSurfaceColor,
                                        textAlign = TextAlign.Right
                                    )

                                    OutlinedTextField(
                                        value = researchQuery,
                                        onValueChange = { researchQuery = it },
                                        placeholder = { Text("مثال: ماده ۱۹۰ قانون مدنی تعهدات قراردادها", color = onSurfaceColor) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = onBgColor,
                                            unfocusedTextColor = onBgColor,
                                            focusedBorderColor = primaryColor,
                                            unfocusedBorderColor = glassBorderColor,
                                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                        )
                                    )

                                    Button(
                                        onClick = {
                                            if (researchQuery.isNotBlank()) {
                                                isSearchingRAG = true
                                                searchResults = emptyList()
                                                // شبیه‌سازی فنی برگرداندن پاسخ RAG
                                                searchResults = listOf(
                                                    "قانون مدنی ماده ۱۹۰: برای صحت هر معامله شرایط ذیل اساسی است: ۱) قصد طرفین و رضای آن‌ها ۲) اهلیت طرفین ۳) موضوع معین که مورد معامله باشد ۴) مشروعیت جهت معامله. (تطابق برداری: %۹۸)",
                                                    "رای وحدت رویه شماره ۷۹۰ دیوان عالی کشور: در خصوص عدم نفوذ عقود عاری از اهلیت متعاقدین و نحوه مطالبه خسارات مادی و معنوی در محاکم عمومی حقوقی. (تطابق برداری: %۹۱)",
                                                    "نظریه مشورتی قوه قضاییه ۱۲۳۴/۷: چنانچه جهت معامله تصریح نشده باشد، اصل بر صحت است مگر شواهد قطعی بر خلاف مشروعیت جهت وجود داشته باشد. (تطابق برداری: %۸۵)"
                                                )
                                                isSearchingRAG = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                        modifier = Modifier.fillMaxWidth().height(48.dp)
                                    ) {
                                        Text("جستجوی هیبریدی دانش برداری RAG", color = Color.White)
                                    }

                                    if (searchResults.isNotEmpty()) {
                                        Text("پایگاه مراجع منطبق (بر اساس رتبه برداری):", style = Typography.titleMedium, color = AccentGold, fontWeight = FontWeight.Bold)
                                        searchResults.forEach { res ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth().border(1.dp, glassBorderColor, RoundedCornerShape(12.dp)),
                                                colors = CardDefaults.cardColors(containerColor = surfaceColor)
                                            ) {
                                                Text(
                                                    text = res,
                                                    style = Typography.bodyMedium,
                                                    color = onBgColor,
                                                    modifier = Modifier.padding(12.dp),
                                                    textAlign = TextAlign.Right
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            "timeline" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "خط زمانی و مراحل دادرسی",
                                        style = Typography.headlineMedium,
                                        color = onBgColor,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right
                                    )

                                    Text(
                                        "مراحل پیش‌بینی شده در فرآیند دادخواست قضایی شما:",
                                        style = Typography.bodyMedium,
                                        color = onSurfaceColor
                                    )

                                    val timelineEvents = listOf(
                                        Triple("گام اول: تنظیم مدارک", "ثبت شرح واقعه و ارایه شواهد اولیه", "۱۶ خرداد ۱۴۰۵"),
                                        Triple("گام دوم: تحلیل و امضای اسناد", "سنجش درصد موفقیت و تایید هوشمند لایحه", "۱۷ خرداد ۱۴۰۵"),
                                        Triple("گام سوم: ابلاغیه الکترونیک", "ارسال خودکار دادخواست به سامانه رسمی ابلاغ", "۱۹ خرداد ۱۴۰۵"),
                                        Triple("گام چهارم: شورای حل اختلاف", "تلاش برای صلح موضوع بر اساس کدهای تسبیب", "به زودی")
                                    )

                                    timelineEvents.forEachIndexed { index, ev ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(surfaceColor, RoundedCornerShape(12.dp))
                                                .border(1.dp, glassBorderColor, RoundedCornerShape(12.dp))
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(ev.third, style = Typography.labelSmall, color = AccentGold)
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(ev.first, style = Typography.bodyLarge, color = onBgColor, fontWeight = FontWeight.Bold)
                                                Text(ev.second, style = Typography.labelMedium, color = onSurfaceColor)
                                            }
                                        }
                                    }
                                }
                            }

                            "notifications" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "صندوق پستی و اعلانات ورودی",
                                        style = Typography.headlineMedium,
                                        color = onBgColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                        textAlign = TextAlign.Right
                                    )

                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(notifications) { notif ->
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(surfaceColor, RoundedCornerShape(16.dp))
                                                    .border(1.dp, glassBorderColor, RoundedCornerShape(16.dp))
                                                    .padding(16.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = notif,
                                                        style = Typography.bodyMedium,
                                                        color = onBgColor,
                                                        modifier = Modifier.weight(1f),
                                                        textAlign = TextAlign.Right
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = primaryColor)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            "profile" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState()),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "مشخصات و امضای الکترونیکی قضایی",
                                        style = Typography.headlineMedium,
                                        color = onBgColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Right
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .background(surfaceColor, CircleShape)
                                            .border(2.dp, AccentGold, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = AccentGold, modifier = Modifier.size(40.dp))
                                    }

                                    Text(session?.username ?: "کاربر گرامی", style = Typography.titleLarge, color = onBgColor, fontWeight = FontWeight.Bold)
                                    Text("شناسه کاربری: ۲۸۱۸۲۷۱۲۳۸ | وضعیت تایید: نقره‌ای", style = Typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)

                                    Card(
                                        modifier = Modifier.fillMaxWidth().glassy3D(cornerRadius = 16.dp, glowColor = primaryColor.copy(alpha = 0.08f)),
                                        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            ProfileRow("محدوده آدرس ملی الکترونیکی", "سامانه دادرسی رسمی")
                                            ProfileRow("اعتبار گواهی تا تاریخ", "۱۴۰۶/۱۲/۲۹")
                                            ProfileRow("سهمیه امبدینگ RAG فضا", "باقیمانده ۹۸ درخواست")
                                            ProfileRow("کارت هوشمند امضاء الکترونیک", "فعال و ثبت شده")
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { 
                                                        prevTabBeforeApis = "profile"
                                                        activeTab = "apis"
                                                    }
                                                    .glassy3D(cornerRadius = 16.dp, glowColor = AccentGold.copy(alpha = 0.15f)),
                                                colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = AccentGold)
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Column(horizontalAlignment = Alignment.End) {
                                                            Text("کلیدهای امنیتی هوش مصنوعی (APIs)", style = Typography.bodyMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                            Text("تنظیم کلیدهای دلخواه و رایگان گوگل و سایر مدل‌ها", style = Typography.labelSmall, color = onSurfaceColor)
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Icon(Icons.Default.Lock, contentDescription = null, tint = AccentGold)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = { authViewModel.logout() },
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftCrimson),
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("خروج امن از حساب کاربری", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            "settings" -> {
                                var isOcrEnabled by remember { mutableStateOf(true) }
                                var isVoicePromptEnabled by remember { mutableStateOf(false) }

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text(
                                        text = "تنظیمات دستگاه دادرس",
                                        style = Typography.headlineMedium,
                                        color = onBgColor,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right
                                    )

                                    Text("مجموعه تنظیمات محلی و دسترسی‌ها:", style = Typography.bodyMedium, color = onSurfaceColor)

                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Switch(checked = isOcrEnabled, onCheckedChange = { isOcrEnabled = it })
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("استخراج خودکار عکس اسناد (OCR)", style = Typography.bodyMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                            Text("تبدیل تصاویر شواهد به متون مستند", style = Typography.labelSmall, color = onSurfaceColor)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().background(surfaceColor, RoundedCornerShape(12.dp)).padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Switch(checked = isVoicePromptEnabled, onCheckedChange = { isVoicePromptEnabled = it })
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("دستیار صوتی و فرمان صادرکننده", style = Typography.bodyMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                            Text("خواندن بلایح تولید شده به فارسی", style = Typography.labelSmall, color = onSurfaceColor)
                                        }
                                    }

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { 
                                                prevTabBeforeApis = "settings"
                                                activeTab = "apis"
                                            }
                                            .glassy3D(cornerRadius = 12.dp, glowColor = AccentGold.copy(alpha = 0.1f)),
                                        colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.5f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = AccentGold)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("تنظیم کلیدهای API هوش مصنوعی (APIs)", style = Typography.bodyMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                    Text("مدیریت کلیدهای اختصاصی گوگل جمینای، کلود، گراک و غیره", style = Typography.labelSmall, color = onSurfaceColor)
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Icon(Icons.Default.Lock, contentDescription = null, tint = AccentGold)
                                            }
                                        }
                                    }
                                }
                            }

                            "apis" -> {
                                val context = androidx.compose.ui.platform.LocalContext.current
                                val testScope = androidx.compose.runtime.rememberCoroutineScope()
                                var testingProvider by remember { mutableStateOf<String?>(null) }
                                val testConnectionResults = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }
                                val testConnectionSuccessStatus = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }

                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    // Header with Back Option
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { activeTab = prevTabBeforeApis }) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = AccentGold)
                                                Text("بازگشت", style = Typography.bodyMedium, color = AccentGold, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Text(
                                            text = "مدیریت کلیدهای امنیتی (APIs)",
                                            style = Typography.headlineSmall,
                                            color = onBgColor,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Right
                                        )
                                    }

                                    Text(
                                        text = "جهت پایداری کامل و استفاده نامحدود، می‌توانید ۱ تا ۳ کلید اختصاصی برای هر سرویس هوش مصنوعی ثبت کنید. کلیدها تنها روی دستگاه شما ذخیره شده و امنیت آن‌ها کاملاً تضمین شده است.",
                                        style = Typography.labelSmall,
                                        color = onSurfaceColor,
                                        textAlign = TextAlign.Right,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Divider(color = glassBorderColor)

                                    // Helper function to render a providers' section
                                    @Composable
                                    fun ProviderApiSection(
                                        providerId: String,
                                        title: String,
                                        consoleUrl: String,
                                        consoleLabel: String,
                                        hintPrefix: String,
                                        keysList: List<String>,
                                        fieldsCount: Int,
                                        onIncrementFields: () -> Unit,
                                        onKeyChange: (Int, String) -> Unit
                                    ) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(surfaceColor, RoundedCornerShape(12.dp))
                                                .border(1.dp, glassBorderColor, RoundedCornerShape(12.dp))
                                                .padding(16.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val hasActiveKey = keysList.any { it.isNotBlank() }
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (hasActiveKey) SoftEmerald.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(8.dp)
                                                            .clip(CircleShape)
                                                            .background(if (hasActiveKey) SoftEmerald else Color.Gray)
                                                    )
                                                    Text(
                                                        text = if (hasActiveKey) "فعال" else "غیرفعال",
                                                        color = if (hasActiveKey) SoftEmerald else Color.Gray,
                                                        style = Typography.labelSmall,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Text(
                                                    text = title, 
                                                    style = Typography.titleMedium, 
                                                    color = AccentGold, 
                                                    fontWeight = FontWeight.Bold, 
                                                    textAlign = TextAlign.Right
                                                )
                                            }
                                            
                                            // Navigation & Help Guide icons
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // 1. Help Guide Option
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(AccentGold.copy(alpha = 0.1f))
                                                        .clickable { showGuideForProvider = providerId }
                                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Info, contentDescription = "راهنما", tint = AccentGold, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "راهنمای دریافت رایگان کلید",
                                                        style = Typography.labelSmall,
                                                        color = AccentGold,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                // 2. Link to Console
                                                Row(
                                                    modifier = Modifier.clickable {
                                                        try {
                                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(consoleUrl))
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    },
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = consoleLabel,
                                                        style = Typography.labelSmall,
                                                        color = AccentGold,
                                                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }

                                            // Inputs up to fieldsCount
                                            for (i in 0 until fieldsCount) {
                                                val v = keysList.getOrNull(i) ?: ""
                                                val keyId = "${providerId}_$i"
                                                val isRevealed = revealedKeys[keyId] == true

                                                OutlinedTextField(
                                                    value = v,
                                                    onValueChange = { onKeyChange(i, it) },
                                                    placeholder = { Text("کلید شماره ${i+1} $hintPrefix...", color = onSurfaceColor.copy(alpha = 0.5f), fontSize = 12.sp) },
                                                    singleLine = true,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 2.dp)
                                                        .testTag("api_key_${providerId}_$i"),
                                                    shape = RoundedCornerShape(8.dp),
                                                    visualTransformation = if (isRevealed) VisualTransformation.None else PasswordVisualTransformation(),
                                                    trailingIcon = {
                                                        IconButton(onClick = { revealedKeys[keyId] = !isRevealed }) {
                                                            Icon(
                                                                imageVector = if (isRevealed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                                contentDescription = if (isRevealed) "پنهان‌سازی" else "نمایش",
                                                                tint = onSurfaceColor.copy(alpha = 0.7f),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    },
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedTextColor = onBgColor,
                                                        unfocusedTextColor = onBgColor,
                                                        focusedBorderColor = AccentGold,
                                                        unfocusedBorderColor = glassBorderColor,
                                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                                    )
                                                )
                                            }

                                            // Add backup key option
                                            if (fieldsCount < 3) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable { onIncrementFields() }
                                                        .padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "افزودن کلید پشتیبان (زاپاس) ${fieldsCount + 1}",
                                                        style = Typography.labelSmall,
                                                        color = AccentGold,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.Add,
                                                        contentDescription = "افزودن",
                                                        tint = AccentGold,
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .border(1.dp, AccentGold, CircleShape)
                                                            .padding(2.dp)
                                                    )
                                                }
                                            }

                                            // Diagnostic test and Deletion Row
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Clear Keys Button
                                                TextButton(
                                                    onClick = {
                                                        for (idx in 0..2) {
                                                            onKeyChange(idx, "")
                                                        }
                                                        testConnectionResults.remove(providerId)
                                                        testConnectionSuccessStatus.remove(providerId)
                                                    },
                                                    colors = ButtonDefaults.textButtonColors(contentColor = SoftCrimson)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("پاکسازی کلیدها", style = Typography.labelSmall, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                // Test connection Button
                                                val isTesting = testingProvider == providerId
                                                Button(
                                                    onClick = {
                                                        val activeKey = keysList.firstOrNull { it.isNotBlank() } ?: ""
                                                        if (activeKey.isBlank()) {
                                                            testConnectionResults[providerId] = "لطفاً ابتدا یک کلید معتبر وارد نمایید."
                                                            testConnectionSuccessStatus[providerId] = false
                                                            return@Button
                                                        }
                                                        testScope.launch {
                                                            testingProvider = providerId
                                                            testConnectionResults[providerId] = "در حال اتصال به سرور جهت سنجش اعتبار..."
                                                            try {
                                                                val res = com.example.network.AiOrchestrator.testProviderConnection(providerId, activeKey)
                                                                testConnectionResults[providerId] = res
                                                                testConnectionSuccessStatus[providerId] = true
                                                            } catch (e: Exception) {
                                                                testConnectionResults[providerId] = "تست ناموفق: ${e.localizedMessage ?: "زمان درخواست به پایان رسید"}"
                                                                testConnectionSuccessStatus[providerId] = false
                                                            } finally {
                                                                testingProvider = null
                                                            }
                                                        }
                                                    },
                                                    enabled = !isTesting,
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (testConnectionSuccessStatus[providerId] == true) SoftEmerald else AccentGold,
                                                        disabledContainerColor = AccentGold.copy(alpha = 0.5f)
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(32.dp),
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                                ) {
                                                    if (isTesting) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier.size(14.dp),
                                                            color = Color.White,
                                                            strokeWidth = 2.dp
                                                        )
                                                    } else {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("تست اتصال نهایی", style = Typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                                                        }
                                                    }
                                                }
                                            }

                                            // Connection status text message
                                            val connMsg = testConnectionResults[providerId]
                                            if (!connMsg.isNullOrBlank()) {
                                                val scoreSucceed = testConnectionSuccessStatus[providerId] == true
                                                Text(
                                                    text = connMsg,
                                                    style = Typography.labelSmall,
                                                    color = if (scoreSucceed) SoftEmerald else SoftCrimson,
                                                    textAlign = TextAlign.Right,
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Render 6 Services
                                    ProviderApiSection(
                                        providerId = "gemini",
                                        title = "کلیدهای اختصاصی Google Gemini API (رایگان)",
                                        consoleUrl = "https://aistudio.google.com/app/apikey",
                                        consoleLabel = "دریافت کلید رسمی گوگل جمینای",
                                        hintPrefix = "جمینای (مثال: AIzaSy...)",
                                        keysList = tempGeminiKeys,
                                        fieldsCount = geminiFieldsToShow,
                                        onIncrementFields = { geminiFieldsToShow = minOf(3, geminiFieldsToShow + 1) },
                                        onKeyChange = { idx, valStr ->
                                            val updated = tempGeminiKeys.toMutableList()
                                            while (updated.size <= idx) updated.add("")
                                            updated[idx] = valStr
                                            tempGeminiKeys = updated
                                        }
                                    )

                                    ProviderApiSection(
                                        providerId = "openrouter",
                                        title = "کلیدهای اختصاصی OpenRouter API",
                                        consoleUrl = "https://openrouter.ai/keys",
                                        consoleLabel = "کسب کلید اپن‌روتر",
                                        hintPrefix = "اپن‌روتر (مثال: sk-or-...)",
                                        keysList = tempOpenRouterKeys,
                                        fieldsCount = openRouterFieldsToShow,
                                        onIncrementFields = { openRouterFieldsToShow = minOf(3, openRouterFieldsToShow + 1) },
                                        onKeyChange = { idx, valStr ->
                                            val updated = tempOpenRouterKeys.toMutableList()
                                            while (updated.size <= idx) updated.add("")
                                            updated[idx] = valStr
                                            tempOpenRouterKeys = updated
                                        }
                                    )

                                    ProviderApiSection(
                                        providerId = "openai",
                                        title = "کلیدهای اختصاصی OpenAI API (ChatGPT)",
                                        consoleUrl = "https://platform.openai.com/api-keys",
                                        consoleLabel = "مدیریت و صدور کلیدهای OpenAI",
                                        hintPrefix = "اپن‌ای‌آی (مثال: sk-proj-...)",
                                        keysList = tempOpenAiKeys,
                                        fieldsCount = openAiFieldsToShow,
                                        onIncrementFields = { openAiFieldsToShow = minOf(3, openAiFieldsToShow + 1) },
                                        onKeyChange = { idx, valStr ->
                                            val updated = tempOpenAiKeys.toMutableList()
                                            while (updated.size <= idx) updated.add("")
                                            updated[idx] = valStr
                                            tempOpenAiKeys = updated
                                        }
                                    )

                                    ProviderApiSection(
                                        providerId = "groq",
                                        title = "کلیدهای اختصاصی Groq API (رایگان پرسرعت)",
                                        consoleUrl = "https://console.groq.com/keys",
                                        consoleLabel = "کسب کلید رایگان گراک",
                                        hintPrefix = "گراک (مثال: gsk-...)",
                                        keysList = tempGroqKeys,
                                        fieldsCount = groqFieldsToShow,
                                        onIncrementFields = { groqFieldsToShow = minOf(3, groqFieldsToShow + 1) },
                                        onKeyChange = { idx, valStr ->
                                            val updated = tempGroqKeys.toMutableList()
                                            while (updated.size <= idx) updated.add("")
                                            updated[idx] = valStr
                                            tempGroqKeys = updated
                                        }
                                    )

                                    ProviderApiSection(
                                        providerId = "cohere",
                                        title = "کلیدهای اختصاصی Cohere API (رایگان چندزبانه)",
                                        consoleUrl = "https://dashboard.cohere.com/api-keys",
                                        consoleLabel = "کسب کلید توسعه‌دهنده کوهر",
                                        hintPrefix = "کوهر (مثال: co_...)",
                                        keysList = tempCohereKeys,
                                        fieldsCount = cohereFieldsToShow,
                                        onIncrementFields = { cohereFieldsToShow = minOf(3, cohereFieldsToShow + 1) },
                                        onKeyChange = { idx, valStr ->
                                            val updated = tempCohereKeys.toMutableList()
                                            while (updated.size <= idx) updated.add("")
                                            updated[idx] = valStr
                                            tempCohereKeys = updated
                                        }
                                    )

                                    ProviderApiSection(
                                        providerId = "hf",
                                        title = "کلیدهای اختصاصی Hugging Face API (رایگان لاما)",
                                        consoleUrl = "https://huggingface.co/settings/tokens",
                                        consoleLabel = "صدور توکن امنیتی هاگینگ‌فیس",
                                        hintPrefix = "هاگینگ‌فیس (مثال: hf_...)",
                                        keysList = tempHuggingFaceKeys,
                                        fieldsCount = huggingFaceFieldsToShow,
                                        onIncrementFields = { huggingFaceFieldsToShow = minOf(3, huggingFaceFieldsToShow + 1) },
                                        onKeyChange = { idx, valStr ->
                                            val updated = tempHuggingFaceKeys.toMutableList()
                                            while (updated.size <= idx) updated.add("")
                                            updated[idx] = valStr
                                            tempHuggingFaceKeys = updated
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = {
                                            adminViewModel.saveMultiApiKeys(
                                                gemini = tempGeminiKeys,
                                                openRouter = tempOpenRouterKeys,
                                                openAi = tempOpenAiKeys,
                                                groq = tempGroqKeys,
                                                cohere = tempCohereKeys,
                                                huggingFace = tempHuggingFaceKeys,
                                                proxyUrl = adminViewModel.geminiProxyUrl.value
                                            )
                                            apiKeysSavedSuccess = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald),
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "بروزرسانی و ثبت کلیدهای اختصاصی کاربری",
                                            style = Typography.bodyMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (apiKeysSavedSuccess) {
                                        Text(
                                            text = "تغییرات با موفقیت در این تلفن ذخیره و فعال شد!",
                                            color = SoftEmerald,
                                            style = Typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    // Dynamic Step-by-Step guides as a dialog
                                    if (showGuideForProvider != null) {
                                        val provider = showGuideForProvider!!
                                        var guideTab by remember(showGuideForProvider) { mutableStateOf(0) }
                                        
                                        val titleText = when (provider) {
                                            "gemini" -> "مرکز راهنمایی Google Gemini"
                                            "openrouter" -> "مرکز راهنمایی OpenRouter"
                                            "openai" -> "مرکز راهنمایی OpenAI Platform"
                                            "groq" -> "مرکز راهنمایی Groq LPU"
                                            "cohere" -> "مرکز راهنمایی Cohere"
                                            "hf" -> "مرکز راهنمایی Hugging Face"
                                            else -> "راهنمای اتصال هوش مستقل"
                                        }

                                        val tabHeaders = listOf(
                                            "کلی (Overview)",
                                            "ثبت‌نام (Register)",
                                            "دریافت کلید (API Key)",
                                            "سهمیه رایگان (Free Tier)",
                                            "پیکربندی (Config)"
                                        )

                                        val contentText = when (provider) {
                                            "gemini" -> when (guideTab) {
                                                0 -> "• ارائه‌دهنده: شرکت بزرگ گوگل (Google AI Studio)\n" +
                                                     "• برترین مدل‌ها: Gemini 1.5 Pro & Gemini 1.5 Flash\n" +
                                                     "• مزیت رقابتی: پنجره بافت فوق‌عریض ۲ میلیون توکنی جهت تحلیل عمیق و هوشمند پرونده‌های قضایی پرحجم، خوانش بی‌نظیر مدارک تصویری و نگارش لوایح منقح به زبان محلی شیرین فارسی در کسری از ثانیه."
                                                1 -> "۱) یک نرم‌افزار عبور از تحریم (قندشکن/پراکسی) با سرور پایدار غیر ایرانی متصل کنید.\n" +
                                                     "۲) مرورگر دستگاه خود را باز کنید و وارد پرتال رسمی توسعه‌دهندگان گوگل به نشانی aistudio.google.com شوید.\n" +
                                                     "۳) با حساب جیمیل (Gmail) استاندارد تاییدشده خود لاگین کنید و توافقات اولیه گوگل را تایید نمایید."
                                                2 -> "۱) در ستون سمت چپ یا منوی ناوبری اصلی کنسول روی دکمه آبی‌رنگ 'Get API Key' کلیک کنید.\n" +
                                                     "۲) بلافاصله دکمه 'Create API Key' را بفشارید.\n" +
                                                     "۳) یک پروژه پیش‌فرض حقوقی تعیین کنید و پس از لود کادر رمز، شناسه توکن را کپی کرده و جهت امنیت دائم، در فیلد ورودی دادرس قرار دهید."
                                                3 -> "گوگل امکاناتی غنی را رایگان عرضه می‌کند:\n" +
                                                     "• ۱۵ درخواست در هر دقیقه (15 RPM)\n" +
                                                     "• ۱,۵۰۰ درخواست در روز (1500 RPD)\n" +
                                                     "• سهمیه ماهانه گسترده بدون نیاز به کارت اعتباری جهت استفاده مستقل و مقرون‌به‌صرفه عامه مردم."
                                                4 -> "توکن صادر شده از سیستم گوگل همواره باید از کلیپ‌بورد در کادر 'کلید Google Gemini' در این صفحه وارد شود. این کلیدها با عبارت الگویی پیش‌فرض 'AIzaSy' شروع خواهند شد. اطمینان یابید هیچ فضای خالی (فاصله) اضافه در کادر نباشد."
                                                else -> ""
                                            }
                                            "openrouter" -> when (guideTab) {
                                                0 -> "• ارائه‌دهنده: پلتفرم باز تجمیع کننده خدمات ابری (OpenRouter.ai)\n" +
                                                     "• برترین مدل‌ها: Claude 3.5 Sonnet، GPT-4o، DeepSeek V3 و بیش از ۱۵۰ مدل هوش برتر بر لیدربرد جهانی.\n" +
                                                     "• مزیت رقابتی: بهترین انتخاب برای دور زدن مطمئن تحریم‌ها و اتصال به چندین مغز استدلالی حقوقی پیشرفته در یک فریم‌ورک یکپارچه."
                                                1 -> "۱) آدرس وب‌سایت اصلی openrouter.ai را در مرورگر تلفن باز فرمایید.\n" +
                                                     "۲) در بالا راست دکمه 'Sign In' را لمس کنید.\n" +
                                                     "۳) خیلی ساده می‌توانید با اکانت امن گوگل (جیمیل) خود بدون دردسر ساخت احراز هویت اولیه عضو شوید."
                                                2 -> "۱) از دایره منوی پروفایل کاربری خود در بالا سمت راست، گزینه 'Settings' و سپس 'Keys' را کلیک کنید.\n" +
                                                     "۲) دکمه بزرگ 'Create Key' را انتخاب کنید.\n" +
                                                     "۳) یک نام دلخواه (مثلاً DadrasApp) بنویسید و کلید هوشمند نهایی را فقط برای یک بار نمایش داده شده کپی و نگهداری کنید."
                                                3 -> "اپن‌روتر دسترسی دائمی به ده‌ها مدل لیدربرد جهانی معروف بالا بالا را ۱۰۰٪ رایگان یا با هزینه‌ای ناچیز (کمتر از چند هزارم دلار) فراهم می‌سازد. مدل‌های پرقدرتی مثل Qwen 2.5-72B و Llama-3 در گیت‌وی اپن‌روتر رایگان و نامحدود هستند."
                                                4 -> "کلید صادر شده را در باکس ورودی 'کلید OpenRouter' درج کنید. این کلید همواره با عبارت ثابت الگوی 'sk-or-...' آغاز می‌شود که فعال‌ساز تمام ربات‌های هوشمند سیستم خواهد بود."
                                                else -> ""
                                            }
                                            "openai" -> when (guideTab) {
                                                0 -> "• ارائه‌دهنده: موسسه هوش مصنوعی اوپن ای‌آی (OpenAI platform)\n" +
                                                     "• برترین مدل‌ها: GPT-4o (سازمانی)، GPT-4-mini\n" +
                                                     "• مزیت رقابتی: بهینه‌ترین و شیواترین نثر نگارش آرا، لوايح، شکواییه‌ها بر پایه رعایت ادبیات رسمی حقوقی بومی جمهوری اسلامی ایران."
                                                1 -> "۱) وارد آدرس platform.openai.com مجرای دولوپرهای محترم شوید.\n" +
                                                     "۲) دکمه 'Sign Up' را بزنید و مراحل ایجاد اکانت را از طریق ایمیل تاییدیه خود پشت سر بگذارید."
                                                2 -> "۱) در سایدبار سمت راست منوی اکانت هوشمند، وارد زبانه مدیریت اصلی تحت عنوان 'API Keys' شوید.\n" +
                                                     "۲) دکمه '+ Create new secret key' را انتخاب و یک نام اختیاری بگذارید.\n" +
                                                     "۳) بلافاصله کپی برداری کلید نهایی را انجام دهید و در جای امن الصاق کنید."
                                                3 -> "اکانت‌های دولوپر ورودی به این ارائه‌دهنده، در ابتدا معادل ۵ دلار شارژ هدیه رایگان (Trial Credit) دریافت می‌کنند که جهت ساخت، ویرایش و تنظیم صدها صفحه لایحه تخصص قضایی کارآموزان کافی خواهد بود."
                                                4 -> "کلید اختصاصی برداشته شده را در کارتابل OpenAI ذخیره کنید. این کلیدها از منظر الگویی حتماً با پیشوند استاندارد 'sk-proj-...' شروع خواهند گردید."
                                                else -> ""
                                            }
                                            "groq" -> when (guideTab) {
                                                0 -> "• ارائه‌دهنده: پایگاه سخت‌افزارهای پردازشی فوق‌سریع لایتنینگ (Groq LPU)\n" +
                                                     "• برترین مدل‌ها: LLaMA 3.1 70B (توسط متا)، Mixtral 8x7B\n" +
                                                     "• مزیت رقابتی: پادشاه بلامنازع سرعت استنتاج زبانی دنیا. اگر نیاز دارید در کم‌ترین زمان و با سرعت خیره‌کننده زیر ۱ ثانیه‌ای لوایح اولیه مد نظر به روز رسانی شوند، گراک بی‌همتا است."
                                                1 -> "۱) به فضا و داشبورد مدیریتی فوق پیشرفته به آدرس console.groq.com مراجعه کنید.\n" +
                                                     "۲) با زدن مستقیم دکمه جیمیل گوگل، وارد سیستم شوید."
                                                2 -> "۱) از نوار منوها یا فهرست اصلی پنل، روی بخش 'API Keys' کلیلک داشته باشید.\n" +
                                                     "۲) روی گزینه 'Create API Key' ضربه بزنید.\n" +
                                                     "۳) کلید صادر شده را با زدن دکمه کپی به حافظه موقت انتقال دهید."
                                                3 -> "شرکت معتبر گراک در حال حاضر ۱۰۰٪ آزاد و کاملاً رایگان سهمیه روزانه و ساعتی عظیمی را در اختیار برنامه‌نویسان قرار می‌دهد تا متون حقوقی با غنای عالی و بدون کوچک‌ترین محدودیت شارژ مالی نگارش گردند."
                                                4 -> "کلید دریافتی دارای فرمت الگوی کلی 'gsk-...' می‌باشد. آن را با دقت برداشته و در کارتابل اختصاصی گراک قرار دهید تا شتاب استنباط آن بیدار شود."
                                                else -> ""
                                            }
                                            "cohere" -> when (guideTab) {
                                                0 -> "• ارائه‌دهنده: پلتفرم نامی تحقیقاتی کوهر کانادا (Cohere)\n" +
                                                     "• برترین مدل‌ها: Command R+ & Command R\n" +
                                                     "• مزیت رقابتی: مدل Command R عمیقاً برای تحلیل موازی زبان‌های بین‌المللی و بومی از جمله زبان فارسی آموزش دیده و در انطباق مواد قانونی فقهی و حقوقی کشور عملکرد موثری دارد."
                                                1 -> "۱) به پرتال داشبورد و تبلت توسعه‌دهندگان پایگاه به آدرس dashboard.cohere.com مراجعه نمایید.\n" +
                                                     "۲) با ثبت نام ایمیلی رایگان یک اکانت معتبر اختصاص دهید."
                                                2 -> "۱) از منوی داشبورد بالا، زبانه 'API Keys' را لمس نموده تا وارد شوید.\n" +
                                                     "۲) شرکت کوهر در آغاز یک کلید تست تحت عنوان Trial Key پیش فرض در اختیارتان می‌گذارد که بلافاصله کپی برداری آن برای مصارف مستقل در این سیستم آماده می‌باشد."
                                                3 -> "سهمیه رایگان و آزمایشی این توکن دسترسی برای راستی‌آزمایی و بررسی‌های مستمر نگارش در برنامه دادرس به ارزش ده‌ها درخواست پردازشی در روز ۱۰۰٪ بدون پرداخت وجه پابرجا خواهد بود."
                                                4 -> "با استفاده از توکن کپی شده به عنوان کلید آزمایشی اختصاصی کوهر، آن را داخل فیلد ثبت کلید 'Cohere' در داشبورد ثبت فرمایید."
                                                else -> ""
                                            }
                                            "hf" -> when (guideTab) {
                                                0 -> "• ارائه‌دهنده: پایگاه مرجع هوش باز دنیا هاگینگ فیس (Hugging Face Hub)\n" +
                                                     "• برترین مدل‌ها: LLaMA 3 (متا)، Qwen 2.5، Mistral\n" +
                                                     "• مزیت رقابتی: دریچه دسترسی برخط به هزاران مدل متن‌باز آزاد محلی و تحقیقاتی معتبر سراسر دنیا بدون هیچ هزینه اضافی."
                                                1 -> "۱) با مرورگر به هاب مرجع رسمی هوش باز دنیا به نشانی huggingface.co مراجعه فرمایید.\n" +
                                                     "۲) گزینه 'Sign Up' را انتخاب نموده و اکانت رایگان بسازید."
                                                2 -> "۱) با زدن دایره عکس پروفایل کاربری خود در بالا راست، وارد بخش اصلی تنظیمات یا 'Settings' شوید.\n" +
                                                     "۲) از منوی کناری زبانه مستقل اکتیو 'Access Tokens' (huggingface.co/settings/tokens) را باز کنید.\n" +
                                                     "۳) دکمه 'New Token' را زده، نامی بنویسید و فیلد دسترسی را حتماً روی حالت 'Read' تعریف کنید."
                                                3 -> "هاگینگ‌فیس به شما اجازه می‌دهد به‌صورت نامحدود از اندپوینت اینفرنس سورس‌باز تستی استفاده بکنید که همواره ۱۰۰٪ رایگان و ماناست."
                                                4 -> "کلید کپی شده را که نمونه الگویی آن با پیشوند اختصاصی 'hf_...' آغاز می‌شود را دریافت کرده و در باکس همنام Hugging Face الصاق نمایید."
                                                else -> ""
                                            }
                                            else -> ""
                                        }

                                        AlertDialog(
                                            onDismissRequest = { showGuideForProvider = null },
                                            confirmButton = {
                                                TextButton(
                                                    onClick = { showGuideForProvider = null },
                                                    modifier = Modifier.testTag("guide_dialog_close")
                                                ) {
                                                    Text("متوجه شدم و بستن راهنما", style = Typography.bodyMedium, color = AccentGold, fontWeight = FontWeight.Bold)
                                                }
                                            },
                                            title = {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Info,
                                                        contentDescription = null,
                                                        tint = AccentGold,
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                    Text(
                                                        text = titleText,
                                                        style = Typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = AccentGold,
                                                        textAlign = TextAlign.Right
                                                    )
                                                }
                                            },
                                            text = {
                                                Column(
                                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                                    horizontalAlignment = Alignment.End
                                                ) {
                                                    // Responsive Pill Selector for 5 Help Sections (RTL Scrollable Row)
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .horizontalScroll(rememberScrollState()),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        tabHeaders.asReversed().forEachIndexed { indexReversed, headerTitle ->
                                                            val originalIndex = tabHeaders.size - 1 - indexReversed
                                                            val isSelected = guideTab == originalIndex
                                                            
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(20.dp))
                                                                    .background(if (isSelected) AccentGold else Color.White.copy(alpha = 0.05f))
                                                                    .border(
                                                                        width = 1.dp,
                                                                        color = if (isSelected) AccentGold else Color.White.copy(alpha = 0.15f),
                                                                        shape = RoundedCornerShape(20.dp)
                                                                    )
                                                                    .clickable { guideTab = originalIndex }
                                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                                            ) {
                                                                Text(
                                                                    text = headerTitle,
                                                                    color = if (isSelected) Color.Black else Color.White.copy(alpha = 0.8f),
                                                                    style = Typography.labelSmall,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(4.dp))

                                                    // Main tab content scrollable panel
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .heightIn(max = 240.dp),
                                                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f))
                                                    ) {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .verticalScroll(rememberScrollState())
                                                                .padding(12.dp),
                                                            verticalArrangement = Arrangement.Top,
                                                            horizontalAlignment = Alignment.End
                                                        ) {
                                                            Text(
                                                                text = contentText,
                                                                style = Typography.bodyMedium,
                                                                color = Color.White.copy(alpha = 0.95f),
                                                                textAlign = TextAlign.Right,
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            containerColor = Color(0xFF101B34),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier.border(1.dp, glassBorderColor, RoundedCornerShape(16.dp))
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

// Case Card Item Component
@Composable
fun CaseCardItem(
    case: CaseEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onBg = MaterialTheme.colorScheme.onBackground
    val onSurf = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = surfaceColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف مورد", tint = SoftCrimson)
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = case.title, style = Typography.bodyLarge, color = onBg, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = case.legalPosition, style = Typography.labelSmall, color = primaryColor)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "رده: ${case.type}", style = Typography.labelMedium, color = onSurf)
                }
            }
        }
    }
}

// Profile Row Component
@Composable
fun ProfileRow(title: String, valText: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = valText, style = Typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        Text(text = title, style = Typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun CollapsibleNewRequestTab(
    isDark: Boolean,
    surfaceColor: Color,
    glassBorderColor: Color,
    onBgColor: Color,
    onSurfaceColor: Color,
    onNavigateToWizard: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .border(1.dp, glassBorderColor, RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(containerColor = if (isExpanded) surfaceColor else surfaceColor.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AccentGold,
                    modifier = Modifier.size(24.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isExpanded) "بستن پنل دسترسی لایحه" else "درخواست حقوقی جدید +",
                        style = Typography.titleMedium,
                        color = AccentGold,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Divider(color = glassBorderColor.copy(alpha = 0.5f))
                    Text(
                        text = "دستیار صادرکننده لایحه دادرس ملی آماده هدایت پرونده، دادخواست، شکواییه یا اظهارنامه قضایی به صورت تمام خودکار و هوشمند است.",
                        style = Typography.bodySmall,
                        color = onSurfaceColor,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = onNavigateToWizard,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "شروع تنظیم و نگارش سند جدید در دستیار",
                            style = Typography.bodyMedium,
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                    }
                }
            }
        }
    }
}

