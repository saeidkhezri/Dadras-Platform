package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CaseEntity
import com.example.ui.components.FrostedGlassBackground
import com.example.ui.theme.*
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.CitizenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenDashboardScreen(
    authViewModel: AuthViewModel,
    citizenViewModel: CitizenViewModel,
    onNavigateToWizard: () -> Unit,
    onNavigateToCase: (Int) -> Unit,
    onNavigateToLibrary: () -> Unit
) {
    val session by authViewModel.session.collectAsState()
    val cases by citizenViewModel.cases.collectAsState()
    val notifications by citizenViewModel.notifications.collectAsState()
    val isDark by authViewModel.isDarkTheme.collectAsState()

    var activeTab by remember { mutableStateOf("dashboard") } // dashboard, cases, research, timeline, notifications, profile, settings
    var searchQuery by remember { mutableStateOf("") }
    
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
                                text = "پیشخوان کاربری مستقل",
                                style = Typography.titleMedium,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Star, contentDescription = null, tint = AccentGold)
                        }

                        Divider(color = glassBorderColor, modifier = Modifier.padding(bottom = 8.dp))

                        val sidebarTabs = listOf(
                            Triple("dashboard", "میز کاربری", Icons.Default.Home),
                            Triple("cases", "کارتابل عریض پرونده", Icons.Default.Menu),
                            Triple("research", "تحقیق و جستجوی RAG", Icons.Default.Search),
                            Triple("timeline", "خط زمانی قضایی", Icons.Default.Build),
                            Triple("notifications", "اعلان‌های امنیتی", Icons.Default.Notifications),
                            Triple("profile", "امضاء و پروفایل", Icons.Default.Person),
                            Triple("settings", "دستگاه و تنظیمات", Icons.Default.Settings)
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
                                // Theme Toggle Button
                                IconButton(
                                    onClick = { authViewModel.toggleTheme() },
                                    modifier = Modifier.testTag("theme_toggle_dashboard")
                                ) {
                                    Icon(
                                        imageVector = if (isDark) Icons.Default.Star else Icons.Default.Refresh,
                                        contentDescription = "Theme Toggle",
                                        tint = AccentGold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .background(surfaceColor, CircleShape)
                                        .border(1.dp, glassBorderColor, CircleShape)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = session?.username ?: "شهروند",
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
                    floatingActionButton = {
                        if (!isWideScreen) {
                            ExtendedFloatingActionButton(
                                onClick = onNavigateToWizard,
                                containerColor = AccentGold,
                                contentColor = Color.Black,
                                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                                text = { Text("درخواست حقوقی جدید", style = Typography.titleMedium, fontWeight = FontWeight.Bold) },
                                modifier = Modifier
                                    .testTag("new_request_fab")
                                    .padding(bottom = 16.dp)
                            )
                        }
                    },
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

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Card(
                                            modifier = Modifier.weight(1f).border(1.dp, glassBorderColor, RoundedCornerShape(16.dp)),
                                            colors = CardDefaults.cardColors(containerColor = surfaceColor)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(Icons.Default.Menu, contentDescription = null, tint = primaryColor, modifier = Modifier.size(28.dp))
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(cases.size.toString(), style = Typography.displayMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                Text("پرونده ثبتی", style = Typography.labelMedium, color = onSurfaceColor)
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.weight(1f).border(1.dp, glassBorderColor, RoundedCornerShape(16.dp)),
                                            colors = CardDefaults.cardColors(containerColor = surfaceColor)
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
                                        modifier = Modifier.fillMaxWidth().border(1.dp, glassBorderColor, RoundedCornerShape(16.dp)),
                                        colors = CardDefaults.cardColors(containerColor = surfaceColor)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("راهنمای سامانه مستقل دادرس هوشمند", style = Typography.titleMedium, color = AccentGold, fontWeight = FontWeight.Bold)
                                            Divider(color = glassBorderColor)
                                            Text(
                                                text = "این پلتفرم برای کمک به شهروندان و تنظیم دادخواست‌های منطبق بر موازین قضایی جمهوری اسلامی ایران به وسیله هوش مصنوعی طراحی شده است. از میان‌برهای زیر استفاده کنید غوطه ور در جلوه شیشه‌ای Liquid Glass.",
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
                                                    .background(surfaceColor, RoundedCornerShape(12.dp))
                                                    .border(1.dp, glassBorderColor, RoundedCornerShape(12.dp))
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
                                        text = "کارتابل پرونده‌های شهروند",
                                        style = Typography.headlineMedium,
                                        color = onBgColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        textAlign = TextAlign.Right
                                    )

                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = { Text("جستجو در بین پرونده‌ها...", color = onSurfaceColor) },
                                        singleLine = true,
                                        trailingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentGold) },
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = onBgColor, unfocusedTextColor = onBgColor, focusedBorderColor = AccentGold, unfocusedBorderColor = glassBorderColor)
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
                                                    onDelete = { citizenViewModel.deleteCase(case.id, session?.username ?: "شهروند") }
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
                                        text = "تحقیق حقوقی چندمدلی با RAG مستقل",
                                        style = Typography.headlineMedium,
                                        color = onBgColor,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right
                                    )
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
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = onBgColor, unfocusedTextColor = onBgColor, focusedBorderColor = primaryColor, unfocusedBorderColor = glassBorderColor)
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
                                        Triple("گام دوم: تحلیل و امضای RAG", "سنجش درصد موفقیت و ثبت مستقل هوشمند", "۱۷ خرداد ۱۴۰۵"),
                                        Triple("گام سوم: ابلاغیه الکترونیک", "ارسال خودکار دادخواست به سامانه ابلاغ مستقل", "۱۹ خرداد ۱۴۰۵"),
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
                                        text = "صندوق پستی و اعلانات امنیتی",
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

                                    Text(session?.username ?: "شهروند گرامی", style = Typography.titleLarge, color = onBgColor, fontWeight = FontWeight.Bold)
                                    Text("شناسه مستقل: ۲۸۱۸۲۷۱۲۳۸ | وضعیت تایید: نقره‌ای", style = Typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)

                                    Card(
                                        modifier = Modifier.fillMaxWidth().border(1.dp, glassBorderColor, RoundedCornerShape(16.dp)),
                                        colors = CardDefaults.cardColors(containerColor = surfaceColor)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            ProfileRow("محدوده آدرس ملی الکترونیکی", "سامانه مستقل دادرسی")
                                            ProfileRow("اعتبار گواهی تا تاریخ", "۱۴۰۶/۱۲/۲۹")
                                            ProfileRow("سهمیه امبدینگ RAG فضا", "باقیمانده ۹۸ درخواست")
                                            ProfileRow("کارت هوشمند امضاء الکترونیک", "فعال و ثبت شده")
                                        }
                                    }

                                    Button(
                                        onClick = { authViewModel.logout() },
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftCrimson),
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("خروج امن از سیستم ملی مستقل", color = Color.White, fontWeight = FontWeight.Bold)
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
