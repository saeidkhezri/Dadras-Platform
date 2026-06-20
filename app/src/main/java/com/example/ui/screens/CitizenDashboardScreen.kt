package com.example.ui.screens

import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import android.widget.Toast
import android.provider.OpenableColumns
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.PasswordVisualTransformation

import com.example.data.CaseEntity
import com.example.data.LegalResourceEntity
import com.example.data.AppDatabase
import com.example.ui.components.FrostedGlassBackground
import com.example.ui.components.glassy3D
import com.example.ui.components.PersianFirstUtils
import com.example.ui.theme.*
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.CitizenViewModel
import com.example.viewmodel.AdminViewModel
import com.example.utils.DocumentProcessor
import com.example.network.AiOrchestrator
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitizenDashboardScreen(
    authViewModel: AuthViewModel,
    citizenViewModel: CitizenViewModel,
    adminViewModel: AdminViewModel,
    initialTab: String = "dashboard",
    onTriggerSystemMenu: () -> Unit = {},
    onNavigateToWizard: () -> Unit,
    onNavigateToCase: (Int) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToAdmin: () -> Unit = {}
) {
    val session by authViewModel.session.collectAsState()
    val cases by citizenViewModel.cases.collectAsState()
    val notifications by citizenViewModel.notifications.collectAsState()
    val legalResources by citizenViewModel.legalResources.collectAsState()
    val isDark by authViewModel.isDarkTheme.collectAsState()
    val isDynamicBg by authViewModel.isDynamicBackground.collectAsState()
    val isPersianNumbersEnabled by authViewModel.isPersianNumbersEnabled.collectAsState()

    // Primary 5 Navigation Tabs
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) } // "dashboard", "repository", "analysis", "cases", "settings"
    var searchQuery by remember { mutableStateOf("") }

    // API Key states
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

    val curProxyUrl by adminViewModel.geminiProxyUrl.collectAsState()
    var tempProxyUrl by remember(curProxyUrl) { mutableStateOf(curProxyUrl) }

    val revealedKeys = remember { mutableStateMapOf<String, Boolean>() }
    var showGuideForProvider by remember { mutableStateOf<String?>(null) }
    var apiKeysSavedSuccess by remember { mutableStateOf(false) }

    // Analysis Tab states
    var selectedAnalysisDocs by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var isAnalysisWorking by remember { mutableStateOf(false) }
    var analysisProgressText by remember { mutableStateOf("") }
    var generatedReportText by remember { mutableStateOf<String?>(null) }
    var generatedReportTitle by remember { mutableStateOf("") }
    var enableRagGrounding by remember { mutableStateOf(true) }
    var selectedAnalysisProvider by remember { mutableStateOf("Gemini 1.5 Pro") }

    // Repository Tab states
    var repositorySearchQuery by remember { mutableStateOf("") }
    var repositorySelectedCategory by remember { mutableStateOf("همه") }
    var repositoryManualTitle by remember { mutableStateOf("") }
    var repositoryManualCategory by remember { mutableStateOf("قانون مدنی") }
    var repositoryManualContent by remember { mutableStateOf("") }
    var showAddManualResourceDialog by remember { mutableStateOf(false) }
    var isRepositoryActionLoading by remember { mutableStateOf(false) }

    // Cases Tab sorting and filtering states
    var caseTypeFilter by remember { mutableStateOf("همه") }
    var caseSortOrder by remember { mutableStateOf("جدیدترین") } // "جدیدترین", "قدیمی‌ترین", "الفبایی الف-ی", "بالاترین امتیاز"

    // Dialog state for viewing full previous document history details
    var activeViewingCase by remember { mutableStateOf<CaseEntity?>(null) }

    val scope = rememberCoroutineScope()
    val localContext = LocalContext.current

    // Transcribing features
    val recorderHelper = remember { com.example.utils.AudioRecorderHelper(localContext) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingFile by remember { mutableStateOf<java.io.File?>(null) }
    var transcribedText by remember { mutableStateOf("") }
    var isTranscribingWorking by remember { mutableStateOf(false) }
    var hasMicPermission by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            Toast.makeText(localContext, "مجوز دسترسی به میکروفون صادر شد.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(localContext, "دسترسی به میکروفون جهت ضبط شواهد کلامی الزامی است.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        val currentPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            localContext,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        hasMicPermission = currentPermission
    }
    val db: com.example.data.AppDatabase = remember { com.example.data.AppDatabase.getDatabase(localContext) }

    // Theme-specific glass styles
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val glassBorderColor = if (isDark) GlassBorderDark else Color(0x33000000)

    // File Pickers
    val analysisFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isAnalysisWorking = true
                analysisProgressText = "در حال تحلیل و استخراج متون اسناد مستقل..."
                val currentDocs = selectedAnalysisDocs.toMutableList()
                for (uri in uris) {
                    val name = getFileName(localContext, uri)
                    val text = DocumentProcessor.extractTextFromUri(localContext, uri, name)
                    currentDocs.add(Pair(name, text))
                }
                selectedAnalysisDocs = currentDocs
                isAnalysisWorking = false
                Toast.makeText(localContext, "تعداد ${uris.size} سند با موفقیت بارگذاری شد.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val repositoryFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isRepositoryActionLoading = true
                val name = getFileName(localContext, uri)
                val text = DocumentProcessor.extractTextFromUri(localContext, uri, name)
                val lang = DocumentProcessor.detectLanguage(text)
                
                val category = when {
                    name.contains("کیفری") || name.contains("مجازات") || name.contains("جزایی") -> "قانون مجازات"
                    name.contains("خانواده") || name.contains("ازدواج") || name.contains("طلاق") -> "حقوق خانواده"
                    name.contains("تجاری") || name.contains("شرکت") || name.contains("چک") -> "قوانین تجاری"
                    else -> "قانون مدنی"
                }

                val entity = LegalResourceEntity(
                    title = name.substringBeforeLast("."),
                    category = category,
                    description = "استخراج ساخت یافته ($lang)",
                    content = text,
                    articleNo = "سند پیوستی"
                )

                withContext(Dispatchers.IO) {
                    db.resourceDao().insertResources(listOf(entity))
                }
                isRepositoryActionLoading = false
                Toast.makeText(localContext, "سند حقوقی محلی آپلود و به مخزن اضافه شد.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        FrostedGlassBackground {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWideScreen = maxWidth > 850.dp

                Row(modifier = Modifier.fillMaxSize()) {
                    // DESKTOP LEFT SIDEBAR (Renders on Widescreens)
                    if (isWideScreen) {
                        Column(
                            modifier = Modifier
                                .width(260.dp)
                                .fillMaxHeight()
                                .background(surfaceColor)
                                .border(1.dp, glassBorderColor)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            // Header Title
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = AccentGold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "میز خدمت هوشمند دادرس",
                                    style = Typography.titleMedium,
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Divider(color = glassBorderColor, modifier = Modifier.padding(bottom = 8.dp))

                            // Sidebar Navigation Tabs
                            val sidebarTabs = listOf(
                                Triple("dashboard", "داشبورد اصلی", Icons.Default.Home),
                                Triple("repository", "مخزن دانش حقوقی", Icons.Default.Book),
                                Triple("analysis", "تحلیل خودکار اسناد", Icons.Default.Upload),
                                Triple("transcribe", "پیاده‌سازی و رونویسی صوتی", Icons.Default.Mic),
                                Triple("cases", "پیشینه و کارتابل لوایح", Icons.Default.FolderOpen),
                                Triple("settings", "تنظیمات سیستمی", Icons.Default.Settings)
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
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) primaryColor else onSurfaceColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = label,
                                        style = Typography.bodyMedium,
                                        color = if (isSelected) primaryColor else onBgColor,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            // Compact Wizard Trigger
                            Button(
                                onClick = onNavigateToWizard,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تنظیم لایحه رسمی جدید", style = Typography.bodyMedium, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // MAIN SCREEN WRAPPER
                    Scaffold(
                        modifier = Modifier.weight(1f),
                        containerColor = Color.Transparent,
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Text(
                                        text = "پایانه دادگستری هوشمند دادرس",
                                        style = Typography.titleLarge,
                                        color = AccentGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                navigationIcon = {
                                    IconButton(
                                        onClick = { authViewModel.logout() },
                                        modifier = Modifier.testTag("logout_button")
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "logout", tint = SoftCrimson)
                                    }
                                },
                                actions = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(end = 12.dp)
                                    ) {
                                        Text(
                                            text = session?.username ?: "کاربر عادی",
                                            style = Typography.labelMedium,
                                            color = AccentGold,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(surfaceColor, CircleShape)
                                                .border(1.dp, glassBorderColor, CircleShape)
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
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
                                    tonalElevation = 0.dp,
                                    modifier = Modifier.height(72.dp)
                                ) {
                                    NavigationBarItem(
                                        selected = activeTab == "dashboard",
                                        onClick = { activeTab = "dashboard" },
                                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                                        label = { Text("پیشخوان", style = Typography.labelSmall) },
                                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = SlateNavyMedium, unselectedIconColor = onSurfaceColor, unselectedTextColor = onSurfaceColor)
                                    )
                                    NavigationBarItem(
                                        selected = activeTab == "transcribe",
                                        onClick = { activeTab = "transcribe" },
                                        icon = { Icon(Icons.Default.Mic, contentDescription = null) },
                                        label = { Text("رونویسی صوتی", style = Typography.labelSmall) },
                                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = SlateNavyMedium, unselectedIconColor = onSurfaceColor, unselectedTextColor = onSurfaceColor)
                                    )
                                    
                                    // Highlighted Central Navigation FAB for Document Analysis
                                    NavigationBarItem(
                                        selected = activeTab == "analysis",
                                        onClick = { activeTab = "analysis" },
                                        icon = { 
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .background(
                                                        brush = Brush.radialGradient(
                                                            colors = listOf(AccentGold, AccentGold.copy(alpha = 0.6f))
                                                        ),
                                                        shape = CircleShape
                                                    )
                                                    .border(1.5.dp, Color.White, CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
                                            }
                                        },
                                        label = { Text("تحلیل اسناد", style = Typography.labelSmall, fontWeight = FontWeight.Bold, color = AccentGold) },
                                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = Color.Transparent, unselectedIconColor = onSurfaceColor, unselectedTextColor = onSurfaceColor)
                                    )

                                    NavigationBarItem(
                                        selected = activeTab == "cases",
                                        onClick = { activeTab = "cases" },
                                        icon = { Icon(Icons.Default.FolderOpen, contentDescription = null) },
                                        label = { Text("کارتابل", style = Typography.labelSmall) },
                                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = SlateNavyMedium, unselectedIconColor = onSurfaceColor, unselectedTextColor = onSurfaceColor)
                                    )
                                    NavigationBarItem(
                                        selected = activeTab == "settings",
                                        onClick = { activeTab = "settings" },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                        label = { Text("تنظیمات", style = Typography.labelSmall) },
                                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = SlateNavyMedium, unselectedIconColor = onSurfaceColor, unselectedTextColor = onSurfaceColor)
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (activeTab) {
                                // ====================================
                                //   1. TAB: HOME DASHBOARD
                                // ====================================
                                "dashboard" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "گزارش کلی عملکرد و مستندات سیستمی",
                                            style = Typography.headlineMedium,
                                            color = onBgColor,
                                            fontWeight = FontWeight.Bold,
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

                                        // Stats Cards Grid
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .glassy3D(cornerRadius = 16.dp, glowColor = primaryColor.copy(alpha = 0.08f)),
                                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Default.Menu, contentDescription = null, tint = primaryColor, modifier = Modifier.size(28.dp))
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(cases.size.toString(), style = Typography.displayMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                    Text("کل اسناد و لوایح پرونده", style = Typography.labelMedium, color = onSurfaceColor)
                                                }
                                            }

                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .glassy3D(cornerRadius = 16.dp, glowColor = AccentGold.copy(alpha = 0.08f)),
                                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Default.Book, contentDescription = null, tint = AccentGold, modifier = Modifier.size(28.dp))
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Text(legalResources.size.toString(), style = Typography.displayMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                    Text("مفاد قوانین فقهی مخزن", style = Typography.labelMedium, color = onSurfaceColor)
                                                }
                                            }
                                        }

                                        // Important Announcements
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .glassy3D(cornerRadius = 16.dp, glowColor = AccentGold.copy(alpha = 0.04f)),
                                            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Notifications, contentDescription = null, tint = AccentGold)
                                                    Text("اطلاعیه‌های مراجع دادرسی و دادگاه‌ها", style = Typography.titleMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                }
                                                Divider(color = glassBorderColor)
                                                if (notifications.isEmpty()) {
                                                    Text("صندوق اطلاعیه‌های شما در حال حاضر خالی است.", style = Typography.bodyMedium, color = onSurfaceColor, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                                                } else {
                                                    notifications.forEachIndexed { index, notif ->
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(surfaceColor, RoundedCornerShape(8.dp))
                                                                .padding(12.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            IconButton(onClick = { citizenViewModel.dismissNotification(index) }) {
                                                                Icon(Icons.Default.Check, contentDescription = "خوانده شد", tint = Color.Green)
                                                            }
                                                            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                                                                Text(text = notif, style = Typography.bodyMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Quick Launch Card
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .glassy3D(cornerRadius = 16.dp, glowColor = primaryColor.copy(alpha = 0.06f)),
                                            colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.6f))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.Start) {
                                                Text("دستیار خودکار نگارش حقوقی دادرس", style = Typography.titleMedium, color = AccentGold, fontWeight = FontWeight.Bold)
                                                Text("آیا قصد طرح شکایت یا تهیه لایحه دفاعیه دارید؟ دادرس با فرآیند ۳ گامه‌ای هوشمند تمام خودکار، دادخواست نهایی منطبق بر منابع فقهی را برای شما صادر می‌کند.", style = Typography.bodyMedium, color = onSurfaceColor)
                                                Button(
                                                    onClick = onNavigateToWizard,
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("ورود به دستیار هوشمند لایحه", color = Color.Black, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.Black)
                                                }
                                            }
                                        }
                                    }
                                }

                                // ====================================
                                //   2. TAB: LEGAL KNOWLEDGE REPOSITORY
                                // ====================================
                                "repository" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "مخزن جامع قوانین و فتاوای فقهی",
                                            style = Typography.headlineMedium,
                                            color = onBgColor,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = { showAddManualResourceDialog = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("افزودن ماده حقوقی دستی", color = Color.White, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = { repositoryFilePicker.launch("*/*") },
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.Black)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("آپلود کتاب قانون (PDF/DOCX/TXT)", color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Export Aggregated Dataset Card
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.5f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .padding(12.dp)
                                                    .fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Button(
                                                    onClick = {
                                                        if (legalResources.isEmpty()) {
                                                            Toast.makeText(localContext, "هیچ منبعی در پایگاه داده وجود ندارد.", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            val aggregated = StringBuilder()
                                                            aggregated.append("=== پایگاه یکپارچه کلان‌داده فقهی دادرس ===\n")
                                                            aggregated.append("شامل کل قوانین محلی معتبر و فتاوا استخراج شده\n\n")
                                                            legalResources.forEachIndexed { i, res ->
                                                                aggregated.append("بند ${i+1}: ${res.title}\n")
                                                                aggregated.append("دسته بندی: ${res.category}\n")
                                                                aggregated.append("ماده قانونی پیوست: ${res.articleNo}\n")
                                                                aggregated.append("محتوای اصلی:\n${res.content}\n")
                                                                aggregated.append("-------------------------------------------\n")
                                                            }
                                                            copyToClipboard(localContext, "Aggregated Dataset", aggregated.toString())
                                                            Toast.makeText(localContext, "کلان‌داده یکپارچه کپی شد و آماده استفاده حقوقی است.", Toast.LENGTH_LONG).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("صادرات کلان‌داده یکپارچه مخزن", color = Color.Black)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("تولید کیت آموزشی مخزن قوانین", style = Typography.titleSmall, color = onBgColor, fontWeight = FontWeight.Bold)
                                                    Text("ادغام، حذف تکرارها و دانلود یکپارچه کلیه قوانین", style = Typography.bodySmall, color = onSurfaceColor)
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = repositorySearchQuery,
                                            onValueChange = { repositorySearchQuery = it },
                                            placeholder = { Text("جستجو در بین قوانین، مقررات و دکترین محلی...", color = onSurfaceColor) },
                                            singleLine = true,
                                            trailingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentGold) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = onBgColor,
                                                unfocusedTextColor = onBgColor,
                                                focusedBorderColor = AccentGold,
                                                unfocusedBorderColor = glassBorderColor
                                            )
                                        )

                                        // Category filters
                                        val categories = listOf("همه", "قانون مدنی", "قانون مجازات", "حقوق خانواده", "قوانین تجاری")
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            categories.forEach { cat ->
                                                val isSelected = repositorySelectedCategory == cat
                                                Box(
                                                    modifier = Modifier
                                                        .background(if (isSelected) AccentGold else surfaceColor, RoundedCornerShape(20.dp))
                                                        .border(1.dp, if (isSelected) AccentGold else glassBorderColor, RoundedCornerShape(20.dp))
                                                        .clickable { repositorySelectedCategory = cat }
                                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                                ) {
                                                    Text(text = cat, color = if (isSelected) Color.Black else onBgColor, fontWeight = FontWeight.SemiBold, style = Typography.labelMedium)
                                                }
                                            }
                                        }

                                        // Resources List
                                        val filteredResources = legalResources.filter { res ->
                                            (repositorySelectedCategory == "همه" || res.category == repositorySelectedCategory) &&
                                            (res.title.contains(repositorySearchQuery) || res.content.contains(repositorySearchQuery) || res.description.contains(repositorySearchQuery))
                                        }

                                        if (filteredResources.isEmpty()) {
                                            Column(
                                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = null, tint = onSurfaceColor, modifier = Modifier.size(56.dp))
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text("هیچ منبع یا قاعده فقهی یافت نشد.", style = Typography.titleMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            LazyColumn(
                                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f).fillMaxWidth()
                                            ) {
                                                items(filteredResources) { res ->
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = CardDefaults.cardColors(containerColor = surfaceColor)
                                                    ) {
                                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                ) {
                                                                    Text(text = res.category, style = Typography.labelSmall, color = primaryColor)
                                                                }
                                                                Text(text = "${res.title} (ماده ${res.articleNo})", style = Typography.bodyLarge, color = onBgColor, fontWeight = FontWeight.Bold)
                                                            }
                                                            Text(text = res.description, style = Typography.bodySmall, color = onSurfaceColor)
                                                            Divider(color = glassBorderColor.copy(alpha = 0.4f))
                                                            Text(text = res.content, style = Typography.bodyMedium, color = onBgColor, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // ====================================
                                //   3. TAB: UNIVERSAL AI PROCESSOR (Analysis)
                                // ====================================
                                "analysis" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "کیت هوشمند دادرسی و تحلیل خودکار اسناد",
                                            style = Typography.headlineMedium,
                                            color = onBgColor,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // Drag & Drop / Selection UI
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(2.dp, Brush.linearGradient(listOf(AccentGold, primaryColor)), RoundedCornerShape(16.dp))
                                                .clickable { analysisFilePicker.launch("*/*") },
                                            colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.4f))
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(Icons.Default.CloudUpload, contentDescription = null, tint = AccentGold, modifier = Modifier.size(56.dp))
                                                Text("بارگذاری اسناد شواهد و مدارک پرونده", style = Typography.titleMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                Text("فرمت‌های مجاز: PDF, DOCX, TXT, HTML, JSON, CSV, XML", style = Typography.bodySmall, color = onSurfaceColor)
                                                Text("سیستم به صورت پویا ساختار، نوع زبان و قواعد متنی را به صورت محلی استخراج خواهد کرد.", style = Typography.labelSmall, color = AccentGold, textAlign = TextAlign.Center)
                                            }
                                        }

                                        // RAG toggle & Model Selectors
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = surfaceColor)
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Switch(checked = enableRagGrounding, onCheckedChange = { enableRagGrounding = it })
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text("هوش دانش‌بنیان (همراه با RAG قوانین)", style = Typography.bodyMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                        Text("استنباط بر پایه‌ فتاوا و قواعد موجود در مخزن محلی", style = Typography.labelSmall, color = onSurfaceColor)
                                                    }
                                                }

                                                Divider(color = glassBorderColor.copy(alpha = 0.5f))

                                                Text("انتخاب مدل‌ برتر دادرسی سیستم:", style = Typography.titleSmall, color = AccentGold, fontWeight = FontWeight.Bold)
                                                val engines = listOf("Gemini 1.5 Pro", "Claude 3.5 Sonnet", "GPT-4o Enterprise", "DeepSeek Llama-3")
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .horizontalScroll(rememberScrollState()),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    engines.forEach { model ->
                                                        val isSelected = selectedAnalysisProvider == model
                                                        Box(
                                                            modifier = Modifier
                                                                .background(if (isSelected) primaryColor else surfaceColor, RoundedCornerShape(12.dp))
                                                                .border(1.dp, if (isSelected) primaryColor else glassBorderColor, RoundedCornerShape(12.dp))
                                                                .clickable { selectedAnalysisProvider = model }
                                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                                        ) {
                                                            Text(text = model, color = Color.White, style = Typography.labelMedium)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // List of Imported Files
                                        if (selectedAnalysisDocs.isNotEmpty()) {
                                            Text("اسناد بارگذاری شده جاری جهت پردازش:", style = Typography.titleMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                            selectedAnalysisDocs.forEachIndexed { index, doc ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0x330B1220))
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .padding(12.dp)
                                                            .fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        IconButton(onClick = {
                                                            val updated = selectedAnalysisDocs.toMutableList()
                                                            updated.removeAt(index)
                                                            selectedAnalysisDocs = updated
                                                        }) {
                                                            Icon(Icons.Default.Delete, contentDescription = "حذف مورد", tint = SoftCrimson)
                                                        }

                                                        Column(horizontalAlignment = Alignment.End) {
                                                            Text(text = doc.first, style = Typography.bodyMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                Box(modifier = Modifier.background(Color.DarkGray, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                                    Text(DocumentProcessor.detectLanguage(doc.second), style = Typography.labelSmall, color = Color.LightGray)
                                                                }
                                                                Text("طول متن: ${doc.second.length} کاراکتر", style = Typography.labelSmall, color = onSurfaceColor)
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Synthesis trigger
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        isAnalysisWorking = true
                                                        analysisProgressText = "در حال ایجاد یکپارچگی قضایی با مدل $selectedAnalysisProvider..."
                                                        
                                                        // Grounding logic
                                                        val groundingContext = StringBuilder()
                                                        if (enableRagGrounding && legalResources.isNotEmpty()) {
                                                            groundingContext.append("\n\nپایگاه دانش حقوقی به عنوان منابع استنادی دادرسی:\n")
                                                            legalResources.take(5).forEach { res ->
                                                                groundingContext.append("- ${res.title} (${res.category}): ${res.content}\n")
                                                            }
                                                        }

                                                        val completeParsedContent = selectedAnalysisDocs.joinToString("\n\n") { "اسم فایل: ${it.first}\nمحتوای متنی اسنادی:\n${it.second}" }
                                                        val builtPrompt = """
                                                            نقش: شما وکیل ارشد حقوقی تراز اول و تحلیل‌گر دادرسی سامانه مستقل دادرس هستید.
                                                            با بررسی همه‌جانبه‌ی متون اسنادی زیر و اعمال قواعد قانونی فقهی نسبت به آن‌ها، یک گزارش استنتاجی یکپارچه، بدون نقص و عاری از توهم به زبان فارسی برای کاربر تدوین کنید.
                                                            
                                                            اسناد جهت تفحص:
                                                            $completeParsedContent
                                                            $groundingContext
                                                            
                                                            خواسته: گزارش تحلیل و صحت‌سنجی فتاوا را دقیقاً ذیل بخش‌های شماره‌گذاری شده بنویسید:
                                                            بخش ۱: خلاصه‌سازی کلیدی و تشریح موضوع
                                                            بخش ۲: تطبیق حقوقی و مستندات قانونی و قواعد فقهی
                                                            بخش ۳: اصحاب دعوی، اشخاص ثالث، مراجع و ارگان‌های ذیمدخل
                                                            بخش ۴: اقدامات حمایتی، توصیه‌های راهبردی و مسیر دادرسی کاربر
                                                        """.trimIndent()

                                                        try {
                                                            val response = AiOrchestrator.executeWithFailover(selectedAnalysisProvider, builtPrompt, "شما دستیار تحلیل هوشمند متنی دادگاه هستید.")
                                                            generatedReportText = response
                                                            generatedReportTitle = if (selectedAnalysisDocs.size == 1) "تحلیل منفرد - ${selectedAnalysisDocs.first().first}" else "تحلیل یکپارچه چندسندی (${selectedAnalysisDocs.size} سند)"
                                                            
                                                            // Save directly to Case SQLite Room History Database!
                                                            val savedCase = CaseEntity(
                                                                type = "تحلیل سند هوشمند",
                                                                title = generatedReportTitle,
                                                                description = completeParsedContent.take(150),
                                                                plaintiff = "کاربر دوسیه",
                                                                defendant = "پیوست پرونده",
                                                                beneficiary = "متقاضی محلی",
                                                                legalPosition = generatedReportTitle,
                                                                suggestedEvidence = selectedAnalysisDocs.map { it.first }.joinToString("، "),
                                                                relief = "صحت سنجی استناد شواهد",
                                                                confidenceScore = 90,
                                                                status = "نهایی‌شده",
                                                                unifiedOutput = response,
                                                                geminiOutput = response,
                                                                gptOutput = response,
                                                                date = "۱۶ خرداد ۱۴۰۵"
                                                            )

                                                            withContext(Dispatchers.IO) {
                                                                db.caseDao().insertCase(savedCase)
                                                            }

                                                            Toast.makeText(localContext, "تحلیل تکمیل شد و در آرشیو تاریخچه ثبت گردید.", Toast.LENGTH_LONG).show()
                                                        } catch (e: Exception) {
                                                            Toast.makeText(localContext, "خطا در تماس با مدل: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                                                        } finally {
                                                            isAnalysisWorking = false
                                                        }
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.Black)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (selectedAnalysisDocs.size > 1) "اجرای تحلیل ترکیبی مجموع اسناد" else "اجرای تحلیل اختصاصی این سند",
                                                    color = Color.Black,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Analysis Output display
                                        generatedReportText?.let { rep ->
                                            Text("گزارش تحلیلی نهایی صادر شده:", style = Typography.titleMedium, color = AccentGold, fontWeight = FontWeight.Bold)
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = surfaceColor)
                                            ) {
                                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        IconButton(onClick = {
                                                            copyToClipboard(localContext, "AI Analysis Report", rep)
                                                            Toast.makeText(localContext, "گزارش در حافظه موقت کپی شد.", Toast.LENGTH_SHORT).show()
                                                        }) {
                                                            Icon(Icons.Default.ContentCopy, contentDescription = "کپی لایحه", tint = AccentGold)
                                                        }
                                                        Text(text = generatedReportTitle, style = Typography.bodyLarge, color = onBgColor, fontWeight = FontWeight.Bold)
                                                    }
                                                    Divider(color = glassBorderColor.copy(alpha = 0.5f))
                                                    Text(text = rep, style = Typography.bodyMedium, color = onBgColor, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                                }
                                            }
                                        }
                                    }
                                }

                                "transcribe" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "پیاده‌سازی و رونویسی از کلام دادرس",
                                            style = Typography.headlineMedium,
                                            color = onBgColor,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Text(
                                            text = "شما می‌توانید دفاعیات، اظهارات یا فرکانس‌های کلامی پرونده را از طریق میکروفون به زبان روان فارسی ضبط کنید. هوش مصنوعی مستقل دادرس (Gemini 3.5 Flash) صدا را رونویسی، ساختاردهی و تایید می‌کند.",
                                            style = Typography.bodyMedium,
                                            color = onSurfaceColor,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // Pulse Microphone Card
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(24.dp),
                                            border = BorderStroke(2.dp, if (isRecording) AccentGold else glassBorderColor)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                // Animated pulse micro icon
                                                Box(
                                                    modifier = Modifier
                                                        .size(80.dp)
                                                        .background(
                                                            color = if (isRecording) SoftCrimson.copy(alpha = 0.2f) else primaryColor.copy(alpha = 0.1f),
                                                            shape = CircleShape
                                                        )
                                                        .border(
                                                            width = 2.dp,
                                                            color = if (isRecording) SoftCrimson else AccentGold.copy(alpha = 0.3f),
                                                            shape = CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                                        contentDescription = null,
                                                        tint = if (isRecording) SoftCrimson else AccentGold,
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                }

                                                Text(
                                                    text = when {
                                                        isRecording -> "در حال ضبط شواهد کلامی شما..."
                                                        isTranscribingWorking -> "در حال رونویسی صوتی با مدل Gemini 3.5..."
                                                        else -> "آماده برای آغاز ضبط گفتار حقوقی"
                                                    },
                                                    style = Typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isRecording) SoftCrimson else onBgColor
                                                )

                                                if (isRecording) {
                                                    Text(
                                                        text = "سیستم در حال نمونه‌برداری صوتی با وضوح بالا می‌باشد...",
                                                        style = Typography.labelSmall,
                                                        color = onSurfaceColor
                                                    )
                                                }

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    if (!isRecording) {
                                                        Button(
                                                            onClick = {
                                                                if (!hasMicPermission) {
                                                                    micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                                                } else {
                                                                    recordingFile = recorderHelper.startRecording()
                                                                    if (recordingFile != null) {
                                                                        isRecording = true
                                                                        Toast.makeText(localContext, "ضبط صدا آغاز شد. صحبت کنید...", Toast.LENGTH_SHORT).show()
                                                                    } else {
                                                                        Toast.makeText(localContext, "خطا در آغاز ضبط صدا.", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                                            shape = RoundedCornerShape(12.dp)
                                                        ) {
                                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("شروع ضبط صدا", color = Color.Black, fontWeight = FontWeight.Bold)
                                                        }
                                                    } else {
                                                        Button(
                                                            onClick = {
                                                                recorderHelper.stopRecording()
                                                                isRecording = false
                                                                val file = recordingFile
                                                                if (file != null && file.exists()) {
                                                                    scope.launch {
                                                                        isTranscribingWorking = true
                                                                        try {
                                                                            val bytes = file.readBytes()
                                                                            val result = com.example.network.GeminiHelper.transcribeAudio(bytes, "audio/m4a")
                                                                            transcribedText = result
                                                                        } catch (e: Exception) {
                                                                            transcribedText = "خطا در پیاده‌سازی صدا: ${e.localizedMessage}"
                                                                        } finally {
                                                                            isTranscribingWorking = false
                                                                        }
                                                                    }
                                                                } else {
                                                                    Toast.makeText(localContext, "فایل صوتی یافت نشد.", Toast.LENGTH_SHORT).show()
                                                                }
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = SoftCrimson),
                                                            shape = RoundedCornerShape(12.dp)
                                                        ) {
                                                            Icon(Icons.Default.Stop, contentDescription = null, tint = Color.White)
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("توقف ضبط و پردازش", color = Color.White, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // Loading Progress
                                        if (isTranscribingWorking) {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.5f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(16.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    CircularProgressIndicator(color = AccentGold, modifier = Modifier.size(24.dp))
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text("هوش مصنوعی دادرس در حال رونویسی شواهد کلامی شما است...", style = Typography.bodySmall, color = AccentGold)
                                                }
                                            }
                                        }

                                        // Transcribed Result
                                        if (transcribedText.isNotBlank()) {
                                            Text("متن رونویسی شده فارسی گفتار:", style = Typography.titleMedium, color = AccentGold, fontWeight = FontWeight.Bold)
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                                                shape = RoundedCornerShape(16.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                                ) {
                                                    Text(
                                                        text = transcribedText,
                                                        style = Typography.bodyMedium,
                                                        color = onBgColor,
                                                        textAlign = TextAlign.Right,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )

                                                    HorizontalDivider(color = glassBorderColor.copy(alpha = 0.5f))

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Left Actions
                                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            IconButton(onClick = {
                                                                transcribedText = ""
                                                            }) {
                                                                Icon(Icons.Default.Delete, contentDescription = "پاک کردن", tint = SoftCrimson)
                                                            }
                                                            IconButton(onClick = {
                                                                copyToClipboard(localContext, "Transcribed Audio Text", transcribedText)
                                                                Toast.makeText(localContext, "متن در حافظه موقت کپی شد.", Toast.LENGTH_SHORT).show()
                                                            }) {
                                                                Icon(Icons.Default.ContentCopy, contentDescription = "کپی", tint = AccentGold)
                                                            }
                                                        }

                                                        // Add to Analysis List Action
                                                        Button(
                                                            onClick = {
                                                                val currentDocs = selectedAnalysisDocs.toMutableList()
                                                                currentDocs.add(Pair("صوت_رونویسی_شده_${System.currentTimeMillis() / 1000}.txt", transcribedText))
                                                                selectedAnalysisDocs = currentDocs
                                                                activeTab = "analysis"
                                                                Toast.makeText(localContext, "متن صوت با موفقیت به لیست اسناد دادرسی اضافه شد.", Toast.LENGTH_LONG).show()
                                                            },
                                                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                                            shape = RoundedCornerShape(8.dp)
                                                        ) {
                                                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text("افزودن به تحلیل اسناد به عنوان لایحه خام", style = Typography.labelSmall, color = Color.White)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // ====================================
                                //   4. TAB: CASES PORTFOLIO (History Archive)
                                // ====================================
                                "cases" -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(
                                            text = "آرشیو پرونده‌ها و مستندات دادرسی",
                                            style = Typography.headlineMedium,
                                            color = onBgColor,
                                            fontWeight = FontWeight.Bold,
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

                                        // Search Bar
                                        OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            placeholder = { Text("جستجو در پرونده‌ها یا لوایح دفاعیه...", color = onSurfaceColor) },
                                            singleLine = true,
                                            trailingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = AccentGold) },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = onBgColor,
                                                unfocusedTextColor = onBgColor,
                                                focusedBorderColor = AccentGold,
                                                unfocusedBorderColor = glassBorderColor
                                            )
                                        )

                                        // Filters Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // Categorical Filter
                                            var showFilterDropdown by remember { mutableStateOf(false) }
                                            Box(modifier = Modifier.weight(1f)) {
                                                Button(
                                                    onClick = { showFilterDropdown = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = surfaceColor),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    border = BorderStroke(1.dp, glassBorderColor),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("دسته: $caseTypeFilter", color = onBgColor, style = Typography.labelMedium)
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentGold)
                                                }
                                                DropdownMenu(expanded = showFilterDropdown, onDismissRequest = { showFilterDropdown = false }) {
                                                    listOf("همه", "شکایت", "دادخواست", "لایحه دفاعیه", "تحلیل سند هوشمند").forEach { type ->
                                                        DropdownMenuItem(text = { Text(type) }, onClick = {
                                                            caseTypeFilter = type
                                                            showFilterDropdown = false
                                                        })
                                                    }
                                                }
                                            }

                                            // Sort Selector
                                            var showSortDropdown by remember { mutableStateOf(false) }
                                            Box(modifier = Modifier.weight(1f)) {
                                                Button(
                                                    onClick = { showSortDropdown = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = surfaceColor),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    border = BorderStroke(1.dp, glassBorderColor),
                                                    shape = RoundedCornerShape(10.dp)
                                                ) {
                                                    Text("ترتیب: $caseSortOrder", color = onBgColor, style = Typography.labelMedium)
                                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentGold)
                                                }
                                                DropdownMenu(expanded = showSortDropdown, onDismissRequest = { showSortDropdown = false }) {
                                                    listOf("جدیدترین", "قدیمی‌ترین", "الفبایی الف-ی", "بالاترین امتیاز").forEach { order ->
                                                        DropdownMenuItem(text = { Text(order) }, onClick = {
                                                            caseSortOrder = order
                                                            showSortDropdown = false
                                                        })
                                                    }
                                                }
                                            }
                                        }

                                        // Filtering & Sorting calculations
                                        val initialFiltered = cases.filter {
                                            (caseTypeFilter == "همه" || it.type == caseTypeFilter) &&
                                            (it.title.contains(searchQuery) || it.type.contains(searchQuery) || it.legalPosition.contains(searchQuery))
                                        }

                                        val sortedCases = when (caseSortOrder) {
                                            "جدیدترین" -> initialFiltered.sortedByDescending { it.timestamp }
                                            "قدیمی‌ترین" -> initialFiltered.sortedBy { it.timestamp }
                                            "الفبایی الف-ی" -> initialFiltered.sortedBy { it.title }
                                            "بالاترین امتیاز" -> initialFiltered.sortedByDescending { it.confidenceScore }
                                            else -> initialFiltered
                                        }

                                        if (sortedCases.isEmpty()) {
                                            Column(
                                                modifier = Modifier.weight(1f).fillMaxWidth(),
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
                                                modifier = Modifier.weight(1f).fillMaxWidth()
                                            ) {
                                                items(sortedCases) { case ->
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = CardDefaults.cardColors(containerColor = surfaceColor)
                                                    ) {
                                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .background(primaryColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                                ) {
                                                                    Text(text = case.type, style = Typography.labelSmall, color = primaryColor)
                                                                }
                                                                Text(text = case.title, style = Typography.bodyLarge, color = onBgColor, fontWeight = FontWeight.Bold)
                                                            }

                                                            Text(text = case.description, style = Typography.bodySmall, color = onSurfaceColor, maxLines = 2)

                                                            Divider(color = glassBorderColor.copy(alpha = 0.4f))

                                                            // Custom persistent interactions: Reopen, Duplicate, Export, Delete
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                    // Delete Action with safety
                                                                    IconButton(onClick = {
                                                                        citizenViewModel.deleteCase(case.id, session?.username ?: "کاربر عادی")
                                                                        Toast.makeText(localContext, "پرونده حذف گردید.", Toast.LENGTH_SHORT).show()
                                                                    }) {
                                                                        Icon(Icons.Default.Delete, contentDescription = "حذف مجرا", tint = SoftCrimson, modifier = Modifier.size(20.dp))
                                                                    }

                                                                    // Duplicate Action (cross-sessions persistent)
                                                                    IconButton(onClick = {
                                                                        scope.launch {
                                                                            val duplicatedCase = case.copy(
                                                                                id = 0,
                                                                                title = "رونوشت - ${case.title}",
                                                                                timestamp = System.currentTimeMillis()
                                                                            )
                                                                            withContext(Dispatchers.IO) {
                                                                                db.caseDao().insertCase(duplicatedCase)
                                                                            }
                                                                            Toast.makeText(localContext, "رونوشت با موفقیت ایجاد شد.", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    }) {
                                                                        Icon(Icons.Default.CopyAll, contentDescription = "تکثیر سند", tint = AccentGold, modifier = Modifier.size(20.dp))
                                                                    }

                                                                    // Export Action (shares or copies content)
                                                                    IconButton(onClick = {
                                                                        copyToClipboard(localContext, "Document Output", case.unifiedOutput)
                                                                        Toast.makeText(localContext, "متن سند قضایی صادر و در کلیپ‌برد ذخیره شد.", Toast.LENGTH_SHORT).show()
                                                                    }) {
                                                                        Icon(Icons.Default.ContentCopy, contentDescription = "کپی لایحه", tint = primaryColor, modifier = Modifier.size(20.dp))
                                                                    }
                                                                }

                                                                // Reopen Action (view details securely)
                                                                Button(
                                                                    onClick = { activeViewingCase = case },
                                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                                                    shape = RoundedCornerShape(8.dp)
                                                                ) {
                                                                    Text("مشاهده جزئیات سند", color = Color.Black, style = Typography.labelMedium, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // ====================================
                                //   5. TAB: AUTHENTICATED APPEARANCE SETTINGS
                                // ====================================
                                "settings" -> {
                                    var isOcrEnabled by remember { mutableStateOf(true) }
                                    var isVoicePromptEnabled by remember { mutableStateOf(false) }
                                    var isContactsExpanded by remember { mutableStateOf(false) }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(16.dp),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = "تنظیمات پایانه و هویت کاربری",
                                            style = Typography.headlineMedium,
                                            color = onBgColor,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // Profile Details Card (formerly in standalone profile tab)
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = surfaceColor)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Person, contentDescription = null, tint = AccentGold)
                                                    Text("مشخصات کاربری صاحب لایحه", style = Typography.titleMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                }
                                                Divider(color = glassBorderColor)
                                                ProfileRow(title = "شناسه ورود", valText = session?.username ?: "نامشخص")
                                                ProfileRow(title = "سطح دسترسی سیستم", valText = session?.role?.name ?: "کاربر عادی دادرسی")
                                                ProfileRow(title = "کد ارزیابی دادخواه", valText = "MSRK-2966-IR")
                                            }
                                        }

                                        // Appearance Customization Controls (moved from Login Screen)
                                        Text("شخصی‌سازی رابط گرافیکی پایانه دادرس:", style = Typography.titleMedium, color = AccentGold, fontWeight = FontWeight.Bold)
                                        
                                        // Dark theme toggle
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(surfaceColor, RoundedCornerShape(12.dp))
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Switch(checked = !isDark, onCheckedChange = { authViewModel.toggleTheme() })
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("تم روشن / تم تاریک سیستمی", style = Typography.bodyMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                Text("انتخاب میان ساختار بصری شب و روز", style = Typography.labelSmall, color = onSurfaceColor)
                                            }
                                        }

                                        // Dynamic movement space background motion toggle
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(surfaceColor, RoundedCornerShape(12.dp))
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Switch(checked = isDynamicBg, onCheckedChange = { authViewModel.toggleDynamicBackground() })
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("پس‌زمینه متحرک معلق", style = Typography.bodyMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                Text("حالت گرافیکی شیشه‌ای همراه با حرکت فضا", style = Typography.labelSmall, color = onSurfaceColor)
                                            }
                                        }

                                        // Persian numerical numbers formatting toggle
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(surfaceColor, RoundedCornerShape(12.dp))
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Switch(checked = isPersianNumbersEnabled, onCheckedChange = { authViewModel.toggleNumberFormat() })
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("اعداد فارسی محلی", style = Typography.bodyMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                Text("نمایش تمامی مبالغ و آمارها به الفبای عددی فارسی", style = Typography.labelSmall, color = onSurfaceColor)
                                            }
                                        }

                                        // System Features Toggles
                                        Text("مدیریت قابلیت‌های بومی دادرسی:", style = Typography.titleMedium, color = AccentGold, fontWeight = FontWeight.Bold)

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(surfaceColor, RoundedCornerShape(12.dp))
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Switch(checked = isOcrEnabled, onCheckedChange = { isOcrEnabled = it })
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("استخراج خودکار عکس اسناد (OCR)", style = Typography.bodyMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                Text("تبدیل هوشمند شواهد تصویری به لوایح متنی", style = Typography.labelSmall, color = onSurfaceColor)
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(surfaceColor, RoundedCornerShape(12.dp))
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Switch(checked = isVoicePromptEnabled, onCheckedChange = { isVoicePromptEnabled = it })
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("فرمان‌دهی صوتی مستقل", style = Typography.bodyMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                Text("کنترل فرآیند سه گامه‌ی لایحه با صدای فارسی کاربر", style = Typography.labelSmall, color = onSurfaceColor)
                                            }
                                        }

                                        // Embedded API Key Config Panel (formerly in standalone APIs tab)
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, glassBorderColor, RoundedCornerShape(16.dp)),
                                            colors = CardDefaults.cardColors(containerColor = surfaceColor)
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Default.Lock, contentDescription = null, tint = AccentGold)
                                                    Text("تنظیمات درگاه‌های امن سرور هوش مصنوعی", style = Typography.titleMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                }
                                                Divider(color = glassBorderColor)
                                                
                                                // Custom Proxy URL config field
                                                Column(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                    horizontalAlignment = Alignment.End
                                                ) {
                                                    Text("آدرس سرور پروکسی اختصاصی (اختیاری جهت رفع فیلترینگ):", style = Typography.labelMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    OutlinedTextField(
                                                        value = tempProxyUrl,
                                                        onValueChange = { tempProxyUrl = it },
                                                        placeholder = { Text("https://generativelanguage.googleapis.com/... (درگاه Gemini)", color = onSurfaceColor.copy(alpha = 0.5f), fontSize = 11.sp) },
                                                        textStyle = Typography.bodySmall.copy(textAlign = TextAlign.Left),
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = true,
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedTextColor = onBgColor,
                                                            unfocusedTextColor = onBgColor,
                                                            focusedBorderColor = AccentGold,
                                                            unfocusedBorderColor = glassBorderColor
                                                        )
                                                    )
                                                }

                                                Divider(color = glassBorderColor.copy(alpha = 0.3f))

                                                if (apiKeysSavedSuccess) {
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = Color(0x334CAF50)),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Text("کلیدهای امنیتی با موفقیت بروزرسانی و ذخیره شد.", color = Color.Green, style = Typography.bodyMedium, modifier = Modifier.padding(8.dp).fillMaxWidth(), textAlign = TextAlign.Center)
                                                    }
                                                }

                                                // Google Gemini key field style filling
                                                ProviderApiSection(
                                                    providerName = "Google AI Gemini (کنترل هوشمند)",
                                                    apiKeys = tempGeminiKeys,
                                                    fieldsToShow = 1,
                                                    revealedKeys = revealedKeys,
                                                    onKeyChange = { index, value ->
                                                        val newList = tempGeminiKeys.toMutableList()
                                                        while (newList.size <= index) newList.add("")
                                                        newList[index] = value
                                                        tempGeminiKeys = newList
                                                    },
                                                    onShowGuide = { showGuideForProvider = "gemini" }
                                                )

                                                // OpenAI credentials
                                                ProviderApiSection(
                                                    providerName = "OpenAI GPT-4",
                                                    apiKeys = tempOpenAiKeys,
                                                    fieldsToShow = 1,
                                                    revealedKeys = revealedKeys,
                                                    onKeyChange = { index, value ->
                                                        val newList = tempOpenAiKeys.toMutableList()
                                                        while (newList.size <= index) newList.add("")
                                                        newList[index] = value
                                                        tempOpenAiKeys = newList
                                                    },
                                                    onShowGuide = { showGuideForProvider = "openai" }
                                                 )

                                                 // OpenRouter API Section
                                                 ProviderApiSection(
                                                     providerName = "دروازه جامع OpenRouter",
                                                     apiKeys = tempOpenRouterKeys,
                                                     fieldsToShow = 1,
                                                     revealedKeys = revealedKeys,
                                                     onKeyChange = { index, value ->
                                                         val newList = tempOpenRouterKeys.toMutableList()
                                                         while (newList.size <= index) newList.add("")
                                                         newList[index] = value
                                                         tempOpenRouterKeys = newList
                                                     },
                                                     onShowGuide = { showGuideForProvider = "openrouter" }
                                                 )

                                                 // Groq API Section
                                                 ProviderApiSection(
                                                     providerName = "دروازه پرسرعت Groq (رایگان)",
                                                     apiKeys = tempGroqKeys,
                                                     fieldsToShow = 1,
                                                     revealedKeys = revealedKeys,
                                                     onKeyChange = { index, value ->
                                                         val newList = tempGroqKeys.toMutableList()
                                                         while (newList.size <= index) newList.add("")
                                                         newList[index] = value
                                                         tempGroqKeys = newList
                                                     },
                                                     onShowGuide = { showGuideForProvider = "groq" }
                                                 )

                                                 // Cohere API Section
                                                 ProviderApiSection(
                                                     providerName = "دروازه چندزبانه Cohere",
                                                     apiKeys = tempCohereKeys,
                                                     fieldsToShow = 1,
                                                     revealedKeys = revealedKeys,
                                                     onKeyChange = { index, value ->
                                                         val newList = tempCohereKeys.toMutableList()
                                                         while (newList.size <= index) newList.add("")
                                                         newList[index] = value
                                                         tempCohereKeys = newList
                                                     },
                                                     onShowGuide = { showGuideForProvider = "cohere" }
                                                 )

                                                 // HuggingFace API Section
                                                 ProviderApiSection(
                                                     providerName = "توکن محلی Hugging Face",
                                                     apiKeys = tempHuggingFaceKeys,
                                                     fieldsToShow = 1,
                                                     revealedKeys = revealedKeys,
                                                     onKeyChange = { index, value ->
                                                         val newList = tempHuggingFaceKeys.toMutableList()
                                                         while (newList.size <= index) newList.add("")
                                                         newList[index] = value
                                                         tempHuggingFaceKeys = newList
                                                     },
                                                     onShowGuide = { showGuideForProvider = "huggingface" }
                                                )

                                                Button(
                                                    onClick = {
                                                        adminViewModel.saveMultiApiKeys(
                                                            gemini = tempGeminiKeys,
                                                            openRouter = tempOpenRouterKeys,
                                                            openAi = tempOpenAiKeys,
                                                            groq = tempGroqKeys,
                                                            cohere = tempCohereKeys,
                                                            huggingFace = tempHuggingFaceKeys, proxyUrl = tempProxyUrl
                                                        )
                                                        apiKeysSavedSuccess = true
                                                        scope.launch {
                                                            kotlinx.coroutines.delay(3000)
                                                            apiKeysSavedSuccess = false
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("ذخیره و همگام‌سازی توکن‌ها با پایگاه", color = Color.Black, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Collapsible Contact Us (تماس با ما) Section
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { isContactsExpanded = !isContactsExpanded },
                                            colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.4f)),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    val arrowRotation by androidx.compose.animation.core.animateFloatAsState(
                                                        targetValue = if (isContactsExpanded) 180f else 0f,
                                                        label = "arrowRotation"
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.KeyboardArrowDown,
                                                        contentDescription = null,
                                                        tint = AccentGold,
                                                        modifier = Modifier
                                                            .size(24.dp)
                                                            .graphicsLayer { rotationZ = arrowRotation }
                                                    )
                                                    Text(
                                                        text = "تماس با ما",
                                                        color = AccentGold,
                                                        style = Typography.titleMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                AnimatedVisibility(
                                                    visible = isContactsExpanded,
                                                    enter = expandVertically() + fadeIn(),
                                                    exit = shrinkVertically() + fadeOut()
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        horizontalAlignment = Alignment.End,
                                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                                    ) {
                                                        Divider(color = glassBorderColor.copy(alpha = 0.5f), thickness = 1.dp)

                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(Color(0xE6050B18), RoundedCornerShape(12.dp))
                                                                .border(1.dp, glassBorderColor, RoundedCornerShape(12.dp))
                                                                .padding(14.dp),
                                                            horizontalAlignment = Alignment.End,
                                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                                        ) {
                                                            // Profile 1
                                                            Column(
                                                                horizontalAlignment = Alignment.End,
                                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Text(text = "محمدسعید خضری‌پور", style = Typography.bodyMedium, color = AccentGold, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right)
                                                                Text(text = "نقش: بنیان‌گذار مشترک، مدیر بخش فنی و برنامه‌نویسی مستقل", style = Typography.labelSmall, color = onSurfaceColor, textAlign = TextAlign.Right)
                                                                Text(text = "عهده‌دار برنامه‌نویسی کلیدی لوپ‌های دادرسی، لایحه دفاعیه، زیرساخت و پایگاه RAG محلی دادرس.", style = Typography.bodySmall, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Right)
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.End,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text(text = "۰۹۱۳۳۴۰۳۹۱۶", style = Typography.bodySmall, color = AccentGold)
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Icon(Icons.Default.Phone, contentDescription = null, tint = AccentGold, modifier = Modifier.size(14.dp))
                                                                }
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.End,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text(text = "saeidkhezri91@gmail.com", style = Typography.bodySmall, color = Color.White)
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Icon(Icons.Default.Email, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                                                }
                                                            }

                                                            Divider(color = glassBorderColor.copy(alpha = 0.3f))

                                                            // Profile 2
                                                            Column(
                                                                horizontalAlignment = Alignment.End,
                                                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                                                modifier = Modifier.fillMaxWidth()
                                                            ) {
                                                                Text(text = "دکتر حسین پورمحی‌آبادی", style = Typography.bodyMedium, color = AccentGold, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right)
                                                                Text(text = "نقش: بنیان‌گذار مشترک، مدیر علمی، انطباق فقهی و صحت‌سنجی مدلی", style = Typography.labelSmall, color = onSurfaceColor, textAlign = TextAlign.Right)
                                                                Text(text = "عهده‌دار تدوین قواعد فقهی استنتاج، انطباق موضوعی بر ادله اثباتی و تایید علمی و شرعی خروجی‌های قضایی صادر شده.", style = Typography.bodySmall, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Right)
                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.End,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text(text = "۰۹۱۳۱۹۷۴۷۷۰", style = Typography.bodySmall, color = AccentGold)
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Icon(Icons.Default.Phone, contentDescription = null, tint = AccentGold, modifier = Modifier.size(14.dp))
                                                                }
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.End,
                                                                    verticalAlignment = Alignment.CenterVertically
                                                                ) {
                                                                    Text(text = "dr-mahyabadi@vru.ac.ir", style = Typography.bodySmall, color = Color.White)
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Icon(Icons.Default.Email, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        if (session?.role == com.example.viewmodel.UserRole.ADMIN) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text("مدیریت سیستم دادرس (مخصوص مدیران ارشد):", style = Typography.titleMedium, color = AccentGold, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Button(
                                                onClick = { onNavigateToAdmin() },
                                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Text("ورود مستقیم به پنل مدیریت کل کشور", color = Color.Black, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        
                                        // Dummy wrapper card to neatly align with the existing file's nesting closing brackets

                                    }
                                }
                            }
                        }
                    }
                }

                // ====================================
                //   GLOBAL MODALS & DIALOGS
                // ====================================

                // 1. LOADING PROCESS OVERLAY
                if (isAnalysisWorking) {
                    Dialog(onDismissRequest = {}) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF070D19)),
                            border = BorderStroke(1.5.dp, AccentGold),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(color = AccentGold)
                                Text(text = analysisProgressText, color = Color.White, style = Typography.bodyMedium, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                // 2. DIALOG FOR VIEWING DETAILED PREVIOUS DOCUMENT (REOPEN LOGIC)
                activeViewingCase?.let { c ->
                    Dialog(onDismissRequest = { activeViewingCase = null }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 600.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1324)),
                            border = BorderStroke(1.5.dp, AccentGold),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { activeViewingCase = null }) {
                                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = SoftCrimson)
                                    }
                                    Text(text = c.title, style = Typography.titleMedium, color = AccentGold, fontWeight = FontWeight.Bold)
                                }

                                Divider(color = glassBorderColor)

                                // Main scrollable layout presenting standard RAG unified results and credentials
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    ProfileRow(title = "طبقه‌بندی لایحه", valText = c.type)
                                    ProfileRow(title = "عنوان اتهام / موضوع سند", valText = c.legalPosition)
                                    ProfileRow(title = "سطح اطمینان انطباق مدل", valText = "${c.confidenceScore}%")
                                    ProfileRow(title = "آخرین تاریخ بازبینی", valText = c.date)

                                    Divider(color = glassBorderColor.copy(alpha = 0.5f))

                                    Text("شرح خواسته‌ها و پرونده استخراج شده:", style = Typography.titleSmall, color = AccentGold, fontWeight = FontWeight.Bold)
                                    Text(text = c.description, style = Typography.bodyMedium, color = Color.White, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())

                                    Divider(color = glassBorderColor.copy(alpha = 0.5f))

                                    c.suggestedEvidence.let { ev ->
                                        if (ev.isNotBlank()) {
                                            Text("ادله اثباتی و منضمات صادر شده:", style = Typography.titleSmall, color = AccentGold, fontWeight = FontWeight.Bold)
                                            Text(text = ev, style = Typography.bodyMedium, color = Color.White, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                            Divider(color = glassBorderColor.copy(alpha = 0.5f))
                                        }
                                    }

                                    Text("متن نهایی لایحه اصلاحیه صادر شده (خروجی یکپارچه):", style = Typography.titleSmall, color = AccentGold, fontWeight = FontWeight.Bold)
                                    Text(text = c.unifiedOutput, style = Typography.bodyMedium, color = Color.White, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                                }

                                Button(
                                    onClick = {
                                        copyToClipboard(localContext, "Unified Output", c.unifiedOutput)
                                        Toast.makeText(localContext, "متن سند قضایی صادر و در حافظه کپی شد.", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("کپی متن لایحه یکپارچه به کلیپ‌برد", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // 3. DIALOG FOR ADDING MATERIAL LAWS MANUALLY TO THE LOCAL REPOSITORY
                if (showAddManualResourceDialog) {
                    Dialog(onDismissRequest = { showAddManualResourceDialog = false }) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1324)),
                            border = BorderStroke(1.5.dp, primaryColor),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("ثبت دستی قانون جدید در مخزن محلی", style = Typography.titleMedium, color = AccentGold, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Right)
                                
                                OutlinedTextField(
                                    value = repositoryManualTitle,
                                    onValueChange = { repositoryManualTitle = it },
                                    label = { Text("عنوان قانون (مثلا: ماده ۱۰ مدنی)", color = onSurfaceColor) },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = repositoryManualContent,
                                    onValueChange = { repositoryManualContent = it },
                                    label = { Text("شرح متن و دامنه نفوذ فقهی قانون", color = onSurfaceColor) },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3
                                )

                                // Category Dropdown
                                var isCategoryMenuOpen by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { isCategoryMenuOpen = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = surfaceColor),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("دسته بندی: $repositoryManualCategory", color = onBgColor)
                                    }
                                    DropdownMenu(expanded = isCategoryMenuOpen, onDismissRequest = { isCategoryMenuOpen = false }) {
                                        listOf("قانون مدنی", "قانون مجازات", "حقوق خانواده", "قوانین تجاری").forEach { type ->
                                            DropdownMenuItem(text = { Text(type) }, onClick = {
                                                repositoryManualCategory = type
                                                isCategoryMenuOpen = false
                                            })
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = { showAddManualResourceDialog = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("انصراف", color = Color.White)
                                    }

                                    Button(
                                        onClick = {
                                            if (repositoryManualTitle.isBlank() || repositoryManualContent.isBlank()) {
                                                Toast.makeText(localContext, "لطفا تمام موارد را پر کنید", Toast.LENGTH_SHORT).show()
                                            } else {
                                                scope.launch {
                                                    val entity = LegalResourceEntity(
                                                        title = repositoryManualTitle,
                                                        category = repositoryManualCategory,
                                                        description = "ثبت دستی توسط کاربر",
                                                        content = repositoryManualContent,
                                                        articleNo = "ماده پیوست"
                                                    )
                                                    withContext(Dispatchers.IO) {
                                                        db.resourceDao().insertResources(listOf(entity))
                                                    }
                                                    Toast.makeText(localContext, "قانون ثبت دستی شد و به پایگاه افزوده گردید.", Toast.LENGTH_SHORT).show()
                                                    repositoryManualTitle = ""
                                                    repositoryManualContent = ""
                                                    showAddManualResourceDialog = false
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("ثبت سند", color = Color.Black, fontWeight = FontWeight.Bold)
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
fun ProviderApiSection(
    providerName: String,
    apiKeys: List<String>,
    fieldsToShow: Int,
    revealedKeys: MutableMap<String, Boolean>,
    onKeyChange: (Int, String) -> Unit,
    onShowGuide: () -> Unit
) {
    val keyLabel = providerName.replace(" ", "_").lowercase()
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val glassBorderColor = Color(0x33B18F54)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onShowGuide,
                colors = ButtonDefaults.buttonColors(containerColor = AccentGold.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("راهنمای استعلام", color = AccentGold, style = Typography.labelSmall)
            }
            Text(text = providerName, style = Typography.bodyMedium, color = onBgColor, fontWeight = FontWeight.Bold)
        }

        for (i in 0 until fieldsToShow) {
            val keyString = apiKeys.getOrNull(i) ?: ""
            val isRevealed = revealedKeys["$keyLabel-$i"] ?: false
            
            OutlinedTextField(
                value = keyString,
                onValueChange = { onKeyChange(i, it) },
                label = { Text("توکن اختصاصی درگاه شماره ${i + 1}", color = onSurfaceColor) },
                singleLine = true,
                visualTransformation = if (isRevealed) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { revealedKeys["$keyLabel-$i"] = !isRevealed }) {
                        Icon(
                            imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "مشاهده یا پنهان سازی",
                            tint = AccentGold
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = onBgColor,
                    unfocusedTextColor = onBgColor,
                    focusedBorderColor = AccentGold,
                    unfocusedBorderColor = glassBorderColor
                )
            )
        }
    }
}

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
                        text = "دستیار صادرکننده لایحه دادرس فارسی آماده هدایت پرونده، دادخواست، شکواییه یا اظهارنامه قضایی به صورت تمام خودکار و هوشمند است.",
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

// Global context file parsing helper
private fun getFileName(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "document.txt"
}

// Private helper to copy text to clipboard natively in Android
private fun copyToClipboard(context: android.content.Context, label: String, text: String) {
    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clipData = android.content.ClipData.newPlainText(label, text)
    clipboardManager.setPrimaryClip(clipData)
}
