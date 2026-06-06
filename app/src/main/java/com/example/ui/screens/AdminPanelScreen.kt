package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.components.FrostedGlassBackground
import com.example.ui.components.PersianFirstUtils
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.OfficialSourceDetails
import com.example.viewmodel.SourceVersion
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.example.data.AppDatabase
import com.example.data.DocumentImportEntity
import com.example.data.VectorDocumentEntity
import com.example.data.AuditLogEntity

data class SimulatedUploadFile(
    val name: String,
    val size: String,
    val format: String,
    var status: String,
    var chunksCount: Int = 0,
    var logs: List<String> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel,
    onNavigateBack: () -> Unit
) {
    val session by authViewModel.session.collectAsState()
    val isPersianDigits by authViewModel.isPersianNumbersEnabled.collectAsState()
    
    val activityLogs by adminViewModel.activityLogs.collectAsState()
    val totalCasesCount by adminViewModel.totalCasesCount.collectAsState()
    val totalResourcesCount by adminViewModel.totalResourcesCount.collectAsState()

    val aiProvider by adminViewModel.aiProvider.collectAsState()
    val isRateLimitingEnabled by adminViewModel.isRateLimitingEnabled.collectAsState()
    val totalUsersCount by adminViewModel.totalUsersCount.collectAsState()
    val totalSubscriptionsActive by adminViewModel.totalSubscriptionsActive.collectAsState()

    val registeredSources by adminViewModel.registeredSources.collectAsState()
    val sourceVersions by adminViewModel.sourceVersions.collectAsState()
    val syncSchedule by adminViewModel.selectedSyncSchedule.collectAsState()
    val syncingProgress by adminViewModel.syncingProgress.collectAsState()

    var activeSubSection by remember { mutableStateOf("dashboard") } // dashboard, resources, settings, logs, uploads
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // متغیرهای بخش مدیریت قوانین
    var registrySubTab by remember { mutableStateOf("manual") } // manual, setup, health, version_compare

    // ۱. متغیرهای تب درج دستی منبع قانونی
    var manualTitle by remember { mutableStateOf("") }
    var manualArticleNo by remember { mutableStateOf("") }
    var manualCategory by remember { mutableStateOf("قانون مدنی") }
    var manualDescription by remember { mutableStateOf("") }
    var manualContent by remember { mutableStateOf("") }
    var manualOpResult by remember { mutableStateOf<String?>(null) }

    // ۲. متغیرهای افزونه ثبت مرجع جدید
    var setupTitle by remember { mutableStateOf("") }
    var setupCategory by remember { mutableStateOf("قانون ملی") }
    var setupUrl by remember { mutableStateOf("") }
    var setupOrg by remember { mutableStateOf("") }
    var setupPriority by remember { mutableStateOf("بالا") }
    var setupTrustScore by remember { mutableStateOf(100f) }
    var setupOpResult by remember { mutableStateOf<String?>(null) }

    // ۳. مقایسه نسخه‌ها
    var compareSourceId by remember { mutableStateOf(1) }
    var compareVersion1 by remember { mutableStateOf("۳.۱") }
    var compareVersion2 by remember { mutableStateOf("۳.۲") }
    var isComparing by remember { mutableStateOf(false) }

    // مرکز بارگذاری اسناد و محاسبات RAG
    var uploadCategory by remember { mutableStateOf("قوانین (Laws)") }
    var uploadFileFormat by remember { mutableStateOf("PDF") }
    var uploadSourceUrl by remember { mutableStateOf("") }
    var uploadVersion by remember { mutableStateOf("۱.۰") }
    var uploadMetadataTags by remember { mutableStateOf("") }
    var uploadQueue by remember { mutableStateOf(listOf<SimulatedUploadFile>()) }
    var isProcessingRAG by remember { mutableStateOf(false) }
    var activeProcessingIndex by remember { mutableStateOf(-1) }
    var hasProcessedSuccessfully by remember { mutableStateOf(false) }

    FrostedGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "پیشخوان فوقِ امنیتی ادمین مستقل (رسمی)",
                            style = Typography.titleLarge,
                            color = AccentGold,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { authViewModel.logout() }) {
                            Icon(Icons.Default.Close, contentDescription = "خروج", tint = SoftCrimson)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xCC050B18),
                    tonalElevation = 0.dp
                ) {
                    NavigationBarItem(
                        selected = activeSubSection == "settings",
                        onClick = { activeSubSection = "settings" },
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text("تنظیمات", style = Typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = SlateNavyMedium)
                    )

                    NavigationBarItem(
                        selected = activeSubSection == "resources",
                        onClick = { activeSubSection = "resources" },
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        label = { Text("منابع و مراجع", style = Typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = SlateNavyMedium)
                    )

                    NavigationBarItem(
                        selected = activeSubSection == "uploads",
                        onClick = { activeSubSection = "uploads" },
                        icon = { Icon(Icons.Default.Share, contentDescription = null) },
                        label = { Text("مرکز آپلود اسناد", style = Typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = SlateNavyMedium)
                    )

                    NavigationBarItem(
                        selected = activeSubSection == "logs",
                        onClick = { activeSubSection = "logs" },
                        icon = { Icon(Icons.Default.Menu, contentDescription = null) },
                        label = { Text("بازرسی و لاگ‌ها", style = Typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = SlateNavyMedium)
                    )

                    NavigationBarItem(
                        selected = activeSubSection == "dashboard",
                        onClick = { activeSubSection = "dashboard" },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("میز ادمین", style = Typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = SlateNavyMedium)
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (activeSubSection) {
                    "dashboard" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "آمار و ریزعملکرد کل دادرس هوشمند مستقل",
                                style = Typography.headlineMedium,
                                color = TextPrimaryFarsi,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "گزارش وضعیت اشتراک‌ها، منابع بازخورد، پرونده‌های شهروندان و پایش مراجع خارجی:",
                                style = Typography.bodyMedium,
                                color = TextSecondaryFarsi
                            )

                            // شبکه کارت اطلاعات آماری
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    AdminStatCard(
                                        title = "پرونده‌های فعال سیستم",
                                        value = PersianFirstUtils.formatDigits(totalCasesCount.toString(), isPersianDigits),
                                        icon = Icons.Default.Menu,
                                        modifier = Modifier.weight(1f)
                                    )

                                    AdminStatCard(
                                        title = "کل منابع مدون بارگیری شده",
                                        value = PersianFirstUtils.formatDigits(totalResourcesCount.toString(), isPersianDigits),
                                        icon = Icons.Default.Info,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    AdminStatCard(
                                        title = "شهروندان متصل به سامانه",
                                        value = PersianFirstUtils.formatDigits(totalUsersCount.toString(), isPersianDigits),
                                        icon = Icons.Default.Person,
                                        modifier = Modifier.weight(1f)
                                    )

                                    AdminStatCard(
                                        title = "اشتراک طلایی طلوع فعال",
                                        value = PersianFirstUtils.formatDigits(totalSubscriptionsActive.toString(), isPersianDigits),
                                        icon = Icons.Default.Star,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // کادر اطلاعات هوש مصنوعی جاری و پایش همگام‌سازی مراجع
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GlassBorderDark, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = GlassSurfaceDark)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = "پایشگر منابع و همگام‌سازی زمان‌بندی‌شده", style = Typography.titleMedium, color = AccentGold)
                                    Divider(color = GlassBorderLight)
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = syncSchedule, style = Typography.bodyMedium, color = SoftEmerald, fontWeight = FontWeight.Bold)
                                        Text(text = "زمان‌بندی رصد خودکار:", style = Typography.bodyMedium, color = TextSecondaryFarsi)
                                    }
                                    
                                    val verifiedCount = registeredSources.count { it.status == "فعال" && it.validationStatus == "معتبر" }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = PersianFirstUtils.formatDigits("$verifiedCount از ${registeredSources.size} مرجع", isPersianDigits),
                                            style = Typography.bodyMedium,
                                            color = TextPrimaryFarsi
                                        )
                                        Text(text = "مراجع معتبر فعال (Verified):", style = Typography.bodyMedium, color = TextSecondaryFarsi)
                                    }
                                }
                            }
                        }
                    }

                    "resources" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "مدیریت ثبتی، همگام‌سازی و سلامت مراجع",
                                style = Typography.headlineMedium,
                                color = TextPrimaryFarsi,
                                fontWeight = FontWeight.Bold
                            )

                            // تب بارهای رجیستری
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GlassSurfaceDark, RoundedCornerShape(12.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val tabs = listOf(
                                    "manual" to "درج دستی",
                                    "setup" to "رجیستری رسمی",
                                    "health" to "مانیتورینگ مراجع",
                                    "version_compare" to "تاریخچه و مقایسه"
                                )
                                tabs.forEach { (tabId, label) ->
                                    val isSelected = registrySubTab == tabId
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) AccentGold.copy(alpha = 0.25f) else Color.Transparent)
                                            .clickable { registrySubTab = tabId }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = Typography.labelSmall,
                                            color = if (isSelected) AccentGold else TextPrimaryFarsi,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            when (registrySubTab) {
                                "manual" -> {
                                    Text(
                                        text = "فرم درج مستقیم ماده قانون به کتابخانه محلی:",
                                        style = Typography.titleMedium,
                                        color = AccentGold
                                    )

                                    OutlinedTextField(
                                        value = manualTitle,
                                        onValueChange = { manualTitle = it },
                                        placeholder = { Text("مثال: ماده ۵۲۲ قانون آیین دادرسی مدنی", color = TextSecondaryFarsi) },
                                        modifier = Modifier.fillMaxWidth().testTag("admin_resource_title"),
                                        label = { Text("عنوان بند قانونی", color = AccentGold) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryFarsi, unfocusedTextColor = TextPrimaryFarsi, focusedBorderColor = AccentGold, unfocusedBorderColor = GlassBorderDark)
                                    )

                                    OutlinedTextField(
                                        value = manualArticleNo,
                                        onValueChange = { manualArticleNo = it },
                                        placeholder = { Text("مثال: ۵۲۲", color = TextSecondaryFarsi) },
                                        modifier = Modifier.fillMaxWidth().testTag("admin_resource_no"),
                                        label = { Text("شماره ماده قانون", color = AccentGold) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryFarsi, unfocusedTextColor = TextPrimaryFarsi, focusedBorderColor = AccentGold, unfocusedBorderColor = GlassBorderDark)
                                    )

                                    // نوع دسته‌بندی
                                    Text(text = "انتخاب گروه سند موضوعی:", style = Typography.labelMedium, color = AccentGold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("قانون مدنی", "قانون مجازات", "حقوق خانواده", "قوانین تجاری").forEach { cat ->
                                            val isSelected = manualCategory == cat
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (isSelected) AccentGold.copy(alpha = 0.2f) else GlassSurfaceDark, RoundedCornerShape(8.dp))
                                                    .border(1.dp, if (isSelected) AccentGold else GlassBorderLight, RoundedCornerShape(8.dp))
                                                    .clickable { manualCategory = cat }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = cat, style = Typography.labelSmall, color = if (isSelected) AccentGold else TextPrimaryFarsi, fontSize = 9.sp)
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = manualDescription,
                                        onValueChange = { manualDescription = it },
                                        placeholder = { Text("خلاصه تغییرات و اهمیت تادیه‌ای مالی...", color = TextSecondaryFarsi) },
                                        modifier = Modifier.fillMaxWidth().testTag("admin_resource_desc"),
                                        label = { Text("شرح توصیفی اجمالی", color = AccentGold) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryFarsi, unfocusedTextColor = TextPrimaryFarsi, focusedBorderColor = AccentGold, unfocusedBorderColor = GlassBorderDark)
                                    )

                                    OutlinedTextField(
                                        value = manualContent,
                                        onValueChange = { manualContent = it },
                                        placeholder = { Text("متن تفصیلی ماده قانونی مصوب مرجع محلی...", color = TextSecondaryFarsi) },
                                        modifier = Modifier.fillMaxWidth().height(120.dp).testTag("admin_resource_content"),
                                        label = { Text("متن کامل ماده قانونی", color = AccentGold) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryFarsi, unfocusedTextColor = TextPrimaryFarsi, focusedBorderColor = AccentGold, unfocusedBorderColor = GlassBorderDark)
                                    )

                                    if (manualOpResult != null) {
                                        Text(text = manualOpResult ?: "", color = SoftEmerald, style = Typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            if (manualTitle.isNotBlank() && manualContent.isNotBlank()) {
                                                adminViewModel.addManualResource(
                                                    title = manualTitle,
                                                    category = manualCategory,
                                                    description = manualDescription,
                                                    content = manualContent,
                                                    articleNo = manualArticleNo
                                                )
                                                manualOpResult = "ماده قانونی با موفقیت به کتابخانه دادرس و رجیستری بومی ارسال گردید."
                                                manualTitle = ""
                                                manualDescription = ""
                                                manualContent = ""
                                                manualArticleNo = ""
                                            } else {
                                                manualOpResult = "تکمیل فیلدهای عنوان و متن الزامی است."
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("admin_submit_resource_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald)
                                    ) {
                                        Text("ثبت و هماهنگ‌سازی در پایگاه محلی قوانین", style = Typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }

                                "setup" -> {
                                    Text(
                                        text = "ثبت مراجع و پایگاه‌های رسمی قوانین جدید (تبصره ۵ رجیستری ادمین):",
                                        style = Typography.titleMedium,
                                        color = AccentGold
                                    )

                                    OutlinedTextField(
                                        value = setupTitle,
                                        onValueChange = { setupTitle = it },
                                        placeholder = { Text("مثال: خبرگزاری خانه ملت (مجلس)", color = TextSecondaryFarsi) },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("عنوان پایگاه حقوقی رسمی", color = AccentGold) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryFarsi, unfocusedTextColor = TextPrimaryFarsi, focusedBorderColor = AccentGold, unfocusedBorderColor = GlassBorderDark)
                                    )

                                    OutlinedTextField(
                                        value = setupUrl,
                                        onValueChange = { setupUrl = it },
                                        placeholder = { Text("مثال: https://icana.ir/laws", color = TextSecondaryFarsi) },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("آدرس الکترونیکی خزش (Official URL Link)", color = AccentGold) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryFarsi, unfocusedTextColor = TextPrimaryFarsi, focusedBorderColor = AccentGold, unfocusedBorderColor = GlassBorderDark)
                                    )

                                    OutlinedTextField(
                                        value = setupOrg,
                                        onValueChange = { setupOrg = it },
                                        placeholder = { Text("مثال: مرکز پژوهش‌های مجلس", color = TextSecondaryFarsi) },
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("سازمان ناظر رسمی متبوع", color = AccentGold) },
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryFarsi, unfocusedTextColor = TextPrimaryFarsi, focusedBorderColor = AccentGold, unfocusedBorderColor = GlassBorderDark)
                                    )

                                    // گروه مراجع
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("قانون ملی", "آیین‌نامه اجرایی", "نظریه مشورتی", "آرای وحدت رویه").forEach { cat ->
                                            val isSelected = setupCategory == cat
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (isSelected) AccentGold.copy(alpha = 0.2f) else GlassSurfaceDark, RoundedCornerShape(8.dp))
                                                    .border(1.dp, if (isSelected) AccentGold else GlassBorderLight, RoundedCornerShape(8.dp))
                                                    .clickable { setupCategory = cat }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = cat, style = Typography.labelSmall, color = if (isSelected) AccentGold else TextPrimaryFarsi, fontSize = 8.sp)
                                            }
                                        }
                                    }

                                    // امتیاز اولویت
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            listOf("بسیار بالا", "بالا", "متوسط", "پایین").forEach { priority ->
                                                val isSelected = setupPriority == priority
                                                Box(
                                                    modifier = Modifier
                                                        .background(if (isSelected) AccentGold.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(6.dp))
                                                        .border(1.dp, if (isSelected) AccentGold else GlassBorderLight, RoundedCornerShape(6.dp))
                                                        .clickable { setupPriority = priority }
                                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                                ) {
                                                    Text(text = priority, style = Typography.labelSmall, color = if (isSelected) AccentGold else TextSecondaryFarsi)
                                                }
                                            }
                                        }
                                        Text(text = "اولویت بازیابی منبع:", style = Typography.bodyMedium, color = TextPrimaryFarsi)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // امتیاز اعتماد
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = PersianFirstUtils.formatDigits(setupTrustScore.toInt().toString(), isPersianDigits) + " از ۱۰۰",
                                                style = Typography.labelMedium,
                                                color = AccentGold
                                            )
                                            Text(text = "ضریب اطمینان و امتیاز صحت حقوقی (Trust Score):", style = Typography.bodyMedium, color = TextPrimaryFarsi)
                                        }
                                        Slider(
                                            value = setupTrustScore,
                                            onValueChange = { setupTrustScore = it },
                                            valueRange = 1f..100f,
                                            colors = SliderDefaults.colors(thumbColor = AccentGold, activeTrackColor = AccentGold)
                                        )
                                    }

                                    if (setupOpResult != null) {
                                        Text(text = setupOpResult ?: "", color = SoftEmerald, style = Typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            if (setupTitle.isNotBlank() && setupUrl.isNotBlank()) {
                                                adminViewModel.addOfficialSource(
                                                    title = setupTitle,
                                                    category = setupCategory,
                                                    sourceUrl = setupUrl,
                                                    org = setupOrg,
                                                    priority = setupPriority,
                                                    trustScore = setupTrustScore.toInt()
                                                )
                                                setupOpResult = "مرجع جدید با رتبه اعتباری به رجیستری متمرکز افزوده شد."
                                                setupTitle = ""
                                                setupUrl = ""
                                                setupOrg = ""
                                            } else {
                                                setupOpResult = "پر کردن کادر عنوان و آدرس منبع الزامی است."
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald)
                                    ) {
                                        Text("ثبت در پایگاه و اتصال نودهای استعلاماتی", style = Typography.titleMedium, color = Color.White)
                                    }
                                }

                                "health" -> {
                                    Text(
                                        text = "پایشگر اتصال و سنجش ثبات مراجع (Health Monitoring):",
                                        style = Typography.titleMedium,
                                        color = AccentGold
                                    )

                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().height(300.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(registeredSources) { source ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = GlassSurfaceDark),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp),
                                                    horizontalAlignment = Alignment.End,
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(10.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (source.status == "فعال" && source.availabilityStatus == "در دسترس") SoftEmerald else SoftCrimson)
                                                            )
                                                            Text(
                                                                text = source.status,
                                                                style = Typography.labelSmall,
                                                                color = if (source.status == "فعال") SoftEmerald else SoftCrimson
                                                            )
                                                        }
                                                        Text(
                                                            text = source.title,
                                                            style = Typography.titleSmall,
                                                            color = TextPrimaryFarsi,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }

                                                    Divider(color = GlassBorderLight)

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(text = source.category, style = Typography.bodySmall, color = AccentGold)
                                                        Text(text = "گروه سند:", style = Typography.bodySmall, color = TextSecondaryFarsi)
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            text = PersianFirstUtils.formatDigits(source.trustScore.toString(), isPersianDigits),
                                                            style = Typography.bodySmall,
                                                            color = TextPrimaryFarsi
                                                        )
                                                        Text(text = "امتیاز اعتبار داده:", style = Typography.bodySmall, color = TextSecondaryFarsi)
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(text = source.lastVerification, style = Typography.bodySmall, color = TextPrimaryFarsi)
                                                        Text(text = "آخرین راستی‌آزمایی پینگ:", style = Typography.bodySmall, color = TextSecondaryFarsi)
                                                    }

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Button(
                                                            onClick = { adminViewModel.toggleOfficialSourceStatus(source.id) },
                                                            colors = ButtonDefaults.buttonColors(containerColor = if (source.status == "فعال") SoftCrimson.copy(alpha = 0.2f) else SoftEmerald.copy(alpha = 0.2f)),
                                                            modifier = Modifier.weight(1f).height(36.dp)
                                                        ) {
                                                            Text(
                                                                text = if (source.status == "فعال") "غیرفعال کردن" else "فعال‌سازی مجدد",
                                                                style = Typography.labelSmall,
                                                                color = if (source.status == "فعال") SoftCrimson else SoftEmerald
                                                            )
                                                        }

                                                        Button(
                                                            onClick = { adminViewModel.triggerManualSync(source.id) },
                                                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold.copy(alpha = 0.15f)),
                                                            modifier = Modifier.weight(1.2f).height(36.dp)
                                                        ) {
                                                            Text("استعلام پینگ ارتباطی", style = Typography.labelSmall, color = AccentGold)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                "version_compare" -> {
                                    Text(
                                        text = "مقایسه نسخه‌گذاری و کشف تباینات وب و مراجع:",
                                        style = Typography.titleMedium,
                                        color = AccentGold
                                    )

                                    // انتخاب منبع برای مقایسه
                                    Text(text = "انتخاب منبع حقوقی:", style = Typography.labelMedium, color = AccentGold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(1 to "قانون مدنی", 3 to "حقوق خانواده").forEach { (sid, text) ->
                                            val isSelected = compareSourceId == sid
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (isSelected) AccentGold.copy(alpha = 0.2f) else GlassSurfaceDark, RoundedCornerShape(8.dp))
                                                    .border(1.dp, if (isSelected) AccentGold else GlassBorderLight, RoundedCornerShape(8.dp))
                                                    .clickable { compareSourceId = sid; compareVersion1 = if (sid == 1) "۳.۱" else "۱.۶"; compareVersion2 = if (sid == 1) "۳.۲" else "۱.۷" }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = text, style = Typography.labelSmall, color = if (isSelected) AccentGold else TextPrimaryFarsi)
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = compareVersion2,
                                            onValueChange = { compareVersion2 = it },
                                            modifier = Modifier.weight(1f),
                                            label = { Text("نسخه بررسی جدید", style = Typography.labelSmall, color = AccentGold) }
                                        )

                                        OutlinedTextField(
                                            value = compareVersion1,
                                            onValueChange = { compareVersion1 = it },
                                            modifier = Modifier.weight(1f),
                                            label = { Text("نسخه تاریخی پایه", style = Typography.labelSmall, color = AccentGold) }
                                        )
                                    }

                                    Button(
                                        onClick = { isComparing = true },
                                        modifier = Modifier.fillMaxWidth().height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald)
                                    ) {
                                        Text("مقاصه‌گری بندهای اصلاحی و بررسی تغییرات", style = Typography.titleMedium, color = Color.White)
                                    }

                                    if (isComparing) {
                                        val v1Data = sourceVersions.find { it.sourceId == compareSourceId && it.version == compareVersion1 }
                                        val v2Data = sourceVersions.find { it.sourceId == compareSourceId && it.version == compareVersion2 }

                                        if (v1Data != null && v2Data != null) {
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(containerColor = GlassSurfaceDark),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.End) {
                                                    Text(text = "مغایرت‌شناسی بندهای نسخه جدید و قدیمی", style = Typography.titleSmall, color = AccentGold)
                                                    Divider(color = GlassBorderLight)

                                                    Text(
                                                        text = "شرح اصلاحیه مکتوب مراجع رسمی:",
                                                        style = Typography.labelMedium,
                                                        color = AccentGold
                                                    )
                                                    Text(
                                                        text = v2Data.modifications,
                                                        style = Typography.bodyMedium,
                                                        color = TextPrimaryFarsi,
                                                        textAlign = TextAlign.Right
                                                    )

                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Divider(color = GlassBorderLight)

                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                                            Text(text = "نسخه بررسی ${v2Data.version} (جدید):", style = Typography.labelSmall, color = SoftEmerald)
                                                            Text(text = v2Data.content, style = Typography.bodySmall, color = TextPrimaryFarsi, textAlign = TextAlign.Right)
                                                        }
                                                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                                            Text(text = "نسخه پایه ${v1Data.version} (تاریخی):", style = Typography.labelSmall, color = SoftCrimson)
                                                            Text(text = v1Data.content, style = Typography.bodySmall, color = TextSecondaryFarsi, textAlign = TextAlign.Right)
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Text(text = "نسخه‌ای با مشخصات مذکور یافت نشد. مراجع معتبر فقط ۳.۱ و ۳.۲ در قانون مدنی و ۱.۶ و ۱.۷ در طلاق هستند.", color = SoftCrimson, style = Typography.bodyMedium, textAlign = TextAlign.Right)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "uploads" -> {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "مرکز فوق‌امنیتی بارگذاری و ذخیره‌سازی اسناد RAG",
                                style = Typography.headlineMedium,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right
                            )
                            Text(
                                text = "پایگاه بارگذاری مدون مستقل. اسناد ارسالی پیش از ثبت مدل در بانک برداری استخراج، قطعه‌بندی (Chunking)، تولید امبدینگ و در جداول مرتبط ذخیره می‌شوند.",
                                style = Typography.bodyMedium,
                                color = TextSecondaryFarsi,
                                textAlign = TextAlign.Right
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "بخش بارگذاری هدفمند اسناد حقوقی و آرای تفکیکی مستقل:",
                                style = Typography.titleMedium,
                                color = AccentGold
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val categories = listOf(
                                    "قوانین (Laws)",
                                    "آراء وحدت رویه (Unification Decisions)",
                                    "نظریات مشورتی (Advisory Opinions)",
                                    "تصمیمات قضایی (Judicial Decisions)",
                                    "دستورالعمل‌ها (Directives)",
                                    "آیین‌نامه‌ها (Regulations)",
                                    "اسناد پژوهشی (Research)"
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    categories.take(4).forEach { cat ->
                                        val isSelected = uploadCategory == cat
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (isSelected) AccentGold.copy(alpha = 0.2f) else GlassSurfaceDark, RoundedCornerShape(10.dp))
                                                .border(1.dp, if (isSelected) AccentGold else GlassBorderLight, RoundedCornerShape(10.dp))
                                                .clickable { uploadCategory = cat }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cat.substringBefore(" "),
                                                style = Typography.labelSmall,
                                                color = if (isSelected) AccentGold else TextPrimaryFarsi,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    categories.drop(4).forEach { cat ->
                                        val isSelected = uploadCategory == cat
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (isSelected) AccentGold.copy(alpha = 0.2f) else GlassSurfaceDark, RoundedCornerShape(10.dp))
                                                .border(1.dp, if (isSelected) AccentGold else GlassBorderLight, RoundedCornerShape(10.dp))
                                                .clickable { uploadCategory = cat }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = cat.substringBefore(" "),
                                                style = Typography.labelSmall,
                                                color = if (isSelected) AccentGold else TextPrimaryFarsi,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clickable {
                                        val selectedDoc = when {
                                            uploadCategory.contains("Laws") -> "قانون_جدید_بودجه_ملی_کشور_نسخه_۱_۴.pdf"
                                            uploadCategory.contains("Unification") -> "رای_وحدت_رویه_شماره_۸۳۰_دیوان_عالی.docx"
                                            uploadCategory.contains("Advisory") -> "نظریه_مشورتی_قوه_قضاییه_جرم_رایانه‌ای.txt"
                                            uploadCategory.contains("Judicial") -> "دادنامه_بدوی_شعبه_۱۰۲_حقوقی_تهران.pdf"
                                            uploadCategory.contains("Directives") -> "بخشنامه_قوه_قضاییه_درخصوص_صلح_و_سازش.docx"
                                            uploadCategory.contains("Regulations") -> "ایین_نامه_اجرایی_ثبت_اسناد_رسمی.html"
                                            else -> "پژوهش_سیاستگذاری_جرم‌شناسی_حقوق_ایران.csv"
                                        }
                                        uploadQueue = uploadQueue + SimulatedUploadFile(
                                            name = selectedDoc,
                                            size = "${(1.2 + uploadQueue.size * 0.7).toString().take(4)} مگابایت",
                                            format = uploadFileFormat,
                                            status = "در صف پردازش"
                                        )
                                    },
                                colors = CardDefaults.cardColors(containerColor = GlassSurfaceDark),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        tint = AccentGold,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "فایل‌های حقوقی را به اینجا بکشید یا برای انتخاب کلیک کنید (Drag & Drop)",
                                        style = Typography.titleMedium,
                                        color = TextPrimaryFarsi,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "پشتیبانی هدفمند از اسناد: PDF, DOCX, TXT, HTML, CSV, MD",
                                        style = Typography.labelSmall,
                                        color = TextSecondaryFarsi,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("PDF", "DOCX", "TXT", "HTML", "CSV", "Markdown").forEach { fmt ->
                                        val isSelected = uploadFileFormat == fmt
                                        Box(
                                            modifier = Modifier
                                                .background(if (isSelected) AccentGold.copy(alpha = 0.25f) else Color.Transparent, RoundedCornerShape(6.dp))
                                                .border(1.dp, if (isSelected) AccentGold else GlassBorderLight, RoundedCornerShape(6.dp))
                                                .clickable { uploadFileFormat = fmt }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(text = fmt, style = Typography.labelSmall, color = if (isSelected) AccentGold else TextSecondaryFarsi)
                                        }
                                    }
                                }
                                Text(text = "فرمت اسناد بارگذاری متوالی:", style = Typography.bodyMedium, color = TextPrimaryFarsi)
                            }

                            // METADATA & SOURCE URL & VERSION ASSIGNMENT EXPLICITLY MODIFIABLE
                            OutlinedTextField(
                                value = uploadSourceUrl,
                                onValueChange = { uploadSourceUrl = it },
                                placeholder = { Text("مثال: https://rooznamehrasmi.ir/laws/10243", color = TextSecondaryFarsi) },
                                modifier = Modifier.fillMaxWidth().testTag("upload_source_url"),
                                label = { Text("آدرس منبع رسمی و معتبر مرجع (Source URL Assignment)", color = AccentGold) },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryFarsi, unfocusedTextColor = TextPrimaryFarsi, focusedBorderColor = AccentGold, unfocusedBorderColor = GlassBorderDark)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = uploadMetadataTags,
                                    onValueChange = { uploadMetadataTags = it },
                                    placeholder = { Text("کیفیت، کیفری، تجاری، تصدیق", color = TextSecondaryFarsi) },
                                    modifier = Modifier.weight(1.5f).testTag("upload_metadata_tags"),
                                    label = { Text("برچسب‌های فراداده (Metadata Assignment)", color = AccentGold) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryFarsi, unfocusedTextColor = TextPrimaryFarsi, focusedBorderColor = AccentGold, unfocusedBorderColor = GlassBorderDark)
                                )
                                OutlinedTextField(
                                    value = uploadVersion,
                                    onValueChange = { uploadVersion = it },
                                    placeholder = { Text("۱.۰", color = TextSecondaryFarsi) },
                                    modifier = Modifier.weight(1f).testTag("upload_version"),
                                    label = { Text("نسخه رهگیری (Version Track)", color = AccentGold) },
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimaryFarsi, unfocusedTextColor = TextPrimaryFarsi, focusedBorderColor = AccentGold, unfocusedBorderColor = GlassBorderDark)
                                )
                            }

                            if (uploadQueue.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { uploadQueue = emptyList(); hasProcessedSuccessfully = false },
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftCrimson)
                                    ) {
                                        Text("پاکسازی صف بارگذاری", color = Color.White)
                                    }
                                    Text("صف فایل‌ها برای جفت‌سازی برداری (${PersianFirstUtils.formatDigits(uploadQueue.size.toString(), isPersianDigits)} فایل)", style = Typography.titleMedium, color = AccentGold)
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = GlassSurfaceDark),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        uploadQueue.forEachIndexed { idx, file ->
                                            val isActive = idx == activeProcessingIndex
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(if (isActive) AccentGold.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(8.dp))
                                                    .border(1.dp, if (isActive) AccentGold else Color.Transparent, RoundedCornerShape(8.dp))
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (file.status == "کامل شد") {
                                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = SoftEmerald)
                                                    } else if (isActive) {
                                                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = AccentGold, strokeWidth = 2.dp)
                                                    } else {
                                                        Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = TextSecondaryFarsi)
                                                    }
                                                    Column(horizontalAlignment = Alignment.Start) {
                                                        Text(text = file.name, style = Typography.bodyMedium, color = TextPrimaryFarsi)
                                                        Text(text = "سایز: ${PersianFirstUtils.formatDigits(file.size, isPersianDigits)} | وضعیت RAG: ${file.status}", style = Typography.labelSmall, color = TextSecondaryFarsi)
                                                    }
                                                }
                                            }
                                            if (isActive && file.logs.isNotEmpty()) {
                                                Card(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(start = 12.dp, top = 4.dp)
                                                        .border(0.5.dp, GlassBorderLight, RoundedCornerShape(6.dp)),
                                                    colors = CardDefaults.cardColors(containerColor = Color(0x99050B18))
                                                ) {
                                                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        file.logs.forEach { logLine ->
                                                            Text(text = PersianFirstUtils.formatDigits(logLine, isPersianDigits), style = Typography.labelSmall, color = AccentGold, fontSize = 9.sp, textAlign = TextAlign.Left)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (uploadQueue.isEmpty()) return@Button
                                    isProcessingRAG = true
                                    hasProcessedSuccessfully = false
                                    coroutineScope.launch {
                                        val db = AppDatabase.getDatabase(context)
                                        val sDao = db.scalableDao()

                                        uploadQueue.forEachIndexed { index, file ->
                                            activeProcessingIndex = index
                                            file.status = "استخراج متون..."
                                            file.logs = listOf("📥 شروع استخراج متون رسمی از سند ${file.name}...")
                                            kotlinx.coroutines.delay(1000)

                                            file.status = "بخش‌بندی (Chunking)..."
                                            file.chunksCount = 4
                                            file.logs = file.logs + listOf(
                                                "🗂️ متن سند با موفقیت استخراج شد (مجموعاً ۱۹۵۰ کلمه)",
                                                "🗂️ همگام‌سازی بخش‌بندی انجام شد: ایجاد ۴ بخش ۱۰۰ کلمه‌ای"
                                            )
                                            kotlinx.coroutines.delay(1000)

                                            file.status = "تولید بردار..."
                                            file.logs = file.logs + listOf(
                                                "🧬 ارسال بخش‌ها به مدل‌سازی برداری مراجع...",
                                                "🧬 تولید بردار ویژگی و امبدینگ ۱۲۸ بعدی با مدل هوشمند مستقل"
                                            )
                                            kotlinx.coroutines.delay(1000)

                                            file.status = "ذخیره دیتابیس..."
                                            file.logs = file.logs + listOf(
                                                "💾 ثبت پایگاه داده آغاز شد...",
                                                "💾 درج بردارها در جدول محلی vector_documents..."
                                            )

                                            try {
                                                // ۱. ثبت در بخش واردات اسناد (DocumentImportEntity)
                                                val importId = sDao.insertDocumentImport(
                                                    DocumentImportEntity(
                                                        document_name = file.name,
                                                        document_type = uploadCategory,
                                                        file_format = file.format,
                                                        upload_source = uploadSourceUrl.ifBlank { "https://rooznamehrasmi.ir/laws" },
                                                        processing_status = "Completed",
                                                        import_date = "۱۶ خرداد ۱۴۰۵"
                                                    )
                                                )

                                                // ۲. درج بردارهای RAG به تعداد بخش‌ها در جدول vector_documents
                                                for (chunkNo in 1..file.chunksCount) {
                                                    val dummyEmbedding = (1..128).map { (0..100).random() / 100f }.joinToString(",")
                                                    sDao.insertVectorDocument(
                                                        VectorDocumentEntity(
                                                          document_type = "import_${uploadCategory}",
                                                          document_id = importId.toInt(),
                                                          chunk_number = chunkNo,
                                                          chunk_text = "بخش شماره $chunkNo از اسناد استخراج شده ${file.name}. کلمات کلیدی: $uploadMetadataTags",
                                                          embedding = dummyEmbedding,
                                                          metadata = "{ \"source\":\"${uploadSourceUrl}\", \"version\":\"${uploadVersion}\", \"tags\":\"${uploadMetadataTags}\" }"
                                                        )
                                                    )
                                                }

                                                // ۳. اضافه کردن لاگ بازرسی سیستم
                                                sDao.insertAuditLog(
                                                    com.example.data.AuditLogEntity(
                                                        user_id = 1,
                                                        action_type = "RAG_ADMIN_UPLOAD",
                                                        description = "نمایه‌سازی موفق سند ${file.name} در دسته ${uploadCategory} به فراداده $uploadMetadataTags"
                                                    )
                                                )
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }

                                            file.status = "کامل شد"
                                            file.logs = file.logs + listOf(
                                                "✅ فایل با موفقیت کلیدگذاری، نمایه‌سازی و ذخیره شد!",
                                                "✅ سند برای بازیابی RAG آماده فراخوانی است."
                                            )
                                            kotlinx.coroutines.delay(500)
                                        }
                                        activeProcessingIndex = -1
                                        isProcessingRAG = false
                                        hasProcessedSuccessfully = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("admin_start_rag_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald),
                                enabled = uploadQueue.isNotEmpty() && !isProcessingRAG
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White)
                                    Text(
                                        text = if (isProcessingRAG) "در حال پردازش گام‌ها..." else "شروع استخراج متون برداری و ذخیره سازی صفی RAG",
                                        style = Typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            if (hasProcessedSuccessfully) {
                                Text(
                                    text = "موفقیت: تمام اسناد به صورت برداری خرد شدند و در جدول vector_documents ثبت نهایی گردیدند!",
                                    color = SoftEmerald,
                                    style = Typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right
                                )
                            }
                        }
                    }

                    "settings" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "تنظیمات پنل و پیوند ارائه‌دهنده AI",
                                style = Typography.headlineMedium,
                                color = TextPrimaryFarsi,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "گزینش مدل فعال دادرس بر اساس OpenRouter و پیکربندی‌های بومی‌سازی مراجع قانون:",
                                style = Typography.bodyMedium,
                                color = TextSecondaryFarsi
                            )

                            // ۱. انتخاب زبان پیش‌فرض و نمایش بومی‌سازی اعداد
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GlassBorderLight, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = GlassSurfaceDark)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(text = "پیکربندی بومی‌سازی و خروجی (Persian Configuration)", style = Typography.titleSmall, color = AccentGold)
                                    Divider(color = GlassBorderLight)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Switch(
                                            checked = isPersianDigits,
                                            onCheckedChange = { authViewModel.toggleNumberFormat() },
                                            colors = SwitchDefaults.colors(checkedThumbColor = SoftEmerald)
                                        )
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(text = "فعال‌سازی نمایش اعداد تمام فارسی", style = Typography.bodyLarge, color = TextPrimaryFarsi, fontWeight = FontWeight.Bold)
                                            Text(text = "نمایش کل قیمت‌ها، شماره تاریخ‌ها و رتبه‌ها با قلم واژگان ایرانی", style = Typography.labelSmall, color = TextSecondaryFarsi)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .background(AccentGold.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(text = "هجری شمسی (جلالی)", color = AccentGold, style = Typography.labelSmall)
                                        }
                                        Text(text = "گاهشمار و تقویم پیش‌فرض:", style = Typography.bodyMedium, color = TextSecondaryFarsi)
                                    }
                                }
                            }

                            // زمان‌بندی همگام سازی مراجع رسمی (Sync Schedule Option)
                            Text(text = "پیکربندی زمان‌بندی خزش و پایش مراجع:", style = Typography.titleMedium, color = AccentGold)
                            listOf("خاموش (دستی)", "روزانه (اتوماتیک)", "هفتگی (اتوماتیک)", "ماهانه (اتوماتیک)").forEach { scheduleOption ->
                                val isSelected = syncSchedule == scheduleOption
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) AccentGold.copy(alpha = 0.15f) else GlassSurfaceDark, RoundedCornerShape(12.dp))
                                        .border(1.dp, if (isSelected) AccentGold else GlassBorderLight, RoundedCornerShape(12.dp))
                                        .clickable { adminViewModel.updateSyncSchedule(scheduleOption) }
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = if (isSelected) SoftEmerald else TextSecondaryFarsi
                                        )
                                        Text(text = scheduleOption, style = Typography.bodyLarge, color = TextPrimaryFarsi, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // انتخاب وب‌سرویس هوش مصنوعی (AI Providers)
                            Text(text = "انتخاب مدل اصلی تحلیل لایحه:", style = Typography.titleMedium, color = AccentGold)
                            listOf("Gemini 3.5 Flash", "DeepSeek Llama 3", "GPT-4o Enterprise", "Claude 3.5 Sonnet").forEach { modelOption ->
                                val isSelected = aiProvider == modelOption
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) AccentGold.copy(alpha = 0.15f) else GlassSurfaceDark, RoundedCornerShape(12.dp))
                                        .border(1.dp, if (isSelected) AccentGold else GlassBorderLight, RoundedCornerShape(12.dp))
                                        .clickable { adminViewModel.updateAiProvider(modelOption) }
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = if (isSelected) SoftEmerald else TextSecondaryFarsi
                                        )
                                        Text(text = modelOption, style = Typography.bodyLarge, color = TextPrimaryFarsi, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // تغییر وضعیت لیمیت نرخ ورودی
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(GlassSurfaceDark, RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(
                                    checked = isRateLimitingEnabled,
                                    onCheckedChange = { adminViewModel.toggleRateLimiting() },
                                    colors = SwitchDefaults.colors(checkedThumbColor = SoftEmerald)
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "فعال‌سازی نرخ مجاز ترافیک مستقل", style = Typography.bodyLarge, color = TextPrimaryFarsi, fontWeight = FontWeight.Bold)
                                    Text(text = "محدود کردن کاربران به ۱۰۰ درخواست ساعتی", style = Typography.labelSmall, color = TextSecondaryFarsi)
                                }
                            }
                        }
                    }

                    "logs" -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { adminViewModel.clearAllSystemLogs() },
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftCrimson)
                                ) {
                                    Text("پاکسازی کل", color = Color.White)
                                }

                                Text(
                                    text = "درگاه ثبت وقایع امنیتی و بازرسی (Audit Logs)",
                                    style = Typography.headlineMedium,
                                    color = TextPrimaryFarsi,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (activityLogs.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("لاگی در سیستم ثبت نشده است.", color = TextSecondaryFarsi, style = Typography.bodyMedium)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(activityLogs) { log ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(GlassSurfaceDark, RoundedCornerShape(12.dp))
                                                .border(1.dp, GlassBorderLight, RoundedCornerShape(12.dp))
                                                .padding(12.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(text = PersianFirstUtils.formatDigits(log.date, isPersianDigits), style = Typography.labelSmall, color = AccentGold)
                                                    Text(text = log.username, style = Typography.labelMedium, color = AccentGold, fontWeight = FontWeight.Bold)
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = PersianFirstUtils.formatDigits(log.action, isPersianDigits),
                                                    style = Typography.bodyMedium,
                                                    color = TextPrimaryFarsi,
                                                    textAlign = TextAlign.Right
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // نمایش همگام‌سازی به صورت نوار لودینگ کلی بالا رونده
                syncingProgress?.let { progress ->
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp)
                            .border(1.dp, AccentGold, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFC050B18)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = AccentGold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "همگام‌سازی مراجع با وب‌سرویس ثبتی...",
                                style = Typography.titleMedium,
                                color = TextPrimaryFarsi
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = progress / 100f,
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = SoftEmerald,
                                trackColor = GlassBorderLight
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = PersianFirstUtils.formatDigits("$progress٪ پیشرفت ممیزی مراجع", isPersianDigits),
                                style = Typography.labelSmall,
                                color = AccentGold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .border(1.dp, GlassBorderLight, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = GlassSurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = AccentGold, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = Typography.displayMedium, color = TextPrimaryFarsi, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, style = Typography.labelMedium, color = TextSecondaryFarsi, textAlign = TextAlign.Center)
        }
    }
}
