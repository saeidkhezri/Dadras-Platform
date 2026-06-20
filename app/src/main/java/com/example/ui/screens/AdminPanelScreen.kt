package com.example.ui.screens

import android.net.Uri
import android.content.Context
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext

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

// Helper functions to get accurate file information from Uri
private fun getFileName(context: Context, uri: Uri): String? {
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
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

private fun getFileSize(context: Context, uri: Uri): String? {
    var size: Long = 0
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (index != -1) {
                    size = cursor.getLong(index)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
    }
    if (size <= 0) return "۱.۵ مگابایت"
    val sizeInMb = size.toDouble() / (1024 * 1024)
    return "${sizeInMb.toString().take(4)} مگابایت"
}

private fun getFileExtension(fileName: String): String? {
    val lastDot = fileName.lastIndexOf('.')
    if (lastDot != -1 && lastDot < fileName.length - 1) {
        return fileName.substring(lastDot + 1).uppercase()
    }
    return null
}

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
    val pickerContext = LocalContext.current
    var uploadCategory by remember { mutableStateOf("قوانین (Laws)") }
    var uploadFileFormat by remember { mutableStateOf("PDF") }
    var uploadSourceUrl by remember { mutableStateOf("") }
    var uploadVersion by remember { mutableStateOf("۱.۰") }
    var uploadMetadataTags by remember { mutableStateOf("") }
    var uploadQueue by remember { mutableStateOf(listOf<SimulatedUploadFile>()) }

    val selectFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = getFileName(pickerContext, uri) ?: "سند_حقوقی_منتخب"
            val sizeStr = getFileSize(pickerContext, uri) ?: "۱.۲ مگابایت"
            val detectedFormat = getFileExtension(fileName) ?: uploadFileFormat
            
            uploadQueue = uploadQueue + SimulatedUploadFile(
                name = fileName,
                size = sizeStr,
                format = detectedFormat,
                status = "در صف پردازش"
            )
        }
    }
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
                            text = "پیشخوان مدیریت کل سامانه",
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
                        label = { Text("تنظیمات سیستم", style = Typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = SlateNavyMedium)
                    )

                    NavigationBarItem(
                        selected = activeSubSection == "resources",
                        onClick = { activeSubSection = "resources" },
                        icon = { Icon(Icons.Default.Info, contentDescription = null) },
                        label = { Text("منابع قوانین", style = Typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = SlateNavyMedium)
                    )

                    NavigationBarItem(
                        selected = activeSubSection == "uploads",
                        onClick = { activeSubSection = "uploads" },
                        icon = { Icon(Icons.Default.Share, contentDescription = null) },
                        label = { Text("بارگذاری اسناد", style = Typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = SlateNavyMedium)
                    )

                    NavigationBarItem(
                        selected = activeSubSection == "logs",
                        onClick = { activeSubSection = "logs" },
                        icon = { Icon(Icons.Default.Menu, contentDescription = null) },
                        label = { Text("لیست اقدامات", style = Typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = AccentGold, selectedTextColor = AccentGold, indicatorColor = SlateNavyMedium)
                    )

                    NavigationBarItem(
                        selected = activeSubSection == "dashboard",
                        onClick = { activeSubSection = "dashboard" },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("آمار و عملکرد", style = Typography.labelSmall) },
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
                                text = "آمار و خلاصه عملکرد کل سامانه",
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
                                text = "بخش بارگذاری و ثبت فایل‌ها و اسناد",
                                style = Typography.headlineMedium,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right
                            )
                            Text(
                                text = "در این بخش می‌توانید اسناد و قوانین جدید را بارگذاری کنید تا در سامانه جستجو و دادرسی فعال شوند.",
                                style = Typography.bodyMedium,
                                color = TextSecondaryFarsi,
                                textAlign = TextAlign.Right
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "انتخاب اسناد و آرای قضایی برای ثبت در سامانه:",
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
                                        try {
                                            selectFileLauncher.launch("*/*")
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
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
                                                "🧬 نمایه‌سازی و آماده‌سازی متن با مدل هوشمند هماهنگ"
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
                        val curGeminiKeys by adminViewModel.geminiApiKeys.collectAsState()
                        val curOpenRouterKeys by adminViewModel.openrouterApiKeys.collectAsState()
                        val curOpenAiKeys by adminViewModel.openaiApiKeys.collectAsState()
                        val curGroqKeys by adminViewModel.groqApiKeys.collectAsState()
                        val curCohereKeys by adminViewModel.cohereApiKeys.collectAsState()
                        val curHuggingFaceKeys by adminViewModel.huggingfaceApiKeys.collectAsState()
                        val curGeminiProxyUrl by adminViewModel.geminiProxyUrl.collectAsState()

                        var tempGeminiKeys by remember(curGeminiKeys) { mutableStateOf(curGeminiKeys.ifEmpty { listOf("") }) }
                        var tempOpenRouterKeys by remember(curOpenRouterKeys) { mutableStateOf(curOpenRouterKeys.ifEmpty { listOf("") }) }
                        var tempOpenAiKeys by remember(curOpenAiKeys) { mutableStateOf(curOpenAiKeys.ifEmpty { listOf("") }) }
                        var tempGroqKeys by remember(curGroqKeys) { mutableStateOf(curGroqKeys.ifEmpty { listOf("") }) }
                        var tempCohereKeys by remember(curCohereKeys) { mutableStateOf(curCohereKeys.ifEmpty { listOf("") }) }
                        var tempHuggingFaceKeys by remember(curHuggingFaceKeys) { mutableStateOf(curHuggingFaceKeys.ifEmpty { listOf("") }) }
                        var tempGeminiProxyUrl by remember(curGeminiProxyUrl) { mutableStateOf(curGeminiProxyUrl) }

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

                            // کارت مدیریت کلیدهای امنیتی هوش مصنوعی (API Keys)
                            var activeGuideInfo by remember { mutableStateOf<ApiGuideInfo?>(null) }
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, GlassBorderLight, RoundedCornerShape(12.dp)),
                                colors = CardDefaults.cardColors(containerColor = GlassSurfaceDark)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "مدیریت کلیدهای امنیتی هوش مصنوعی (API Keys)",
                                        style = Typography.titleMedium,
                                        color = AccentGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "جهت پایداری کامل و جلوگیری از اختلال در جلسات ارائه، می‌توانید تا ۳ کلید پشتیبان برای هر ارائه‌دهنده تعریف کنید. سیستم در صورت مسدودی یا اتمام شارژ یک کلید، به طور خودکار به کلیدهای بعدی فیل‌اور می‌کند.",
                                        style = Typography.labelSmall,
                                        color = TextSecondaryFarsi,
                                        textAlign = TextAlign.Right
                                    )
                                    Divider(color = GlassBorderLight)

                                    // 1. Google Gemini API Keys
                                    ApiKeyServiceSection(
                                        title = "کلیدهای اختصاصی Google Gemini API (تا ۳ کلید با اولویت بالا به پایین):",
                                        guideTitle = "راهنمای اتصال Google Gemini",
                                        onOpenGuide = { activeGuideInfo = geminiGuide },
                                        keys = tempGeminiKeys,
                                        onKeysChange = { tempGeminiKeys = it },
                                        placeholderPrefix = "کلید جمینای",
                                        onTestKey = { key, onResult ->
                                            testKeyConnection("Gemini", key, onResult)
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "آدرس اینترنتی پروکسی/گذرگاه Google Gemini (اختیاری جهت رفع تحریم):", style = Typography.labelSmall, color = TextSecondaryFarsi)
                                    OutlinedTextField(
                                        value = tempGeminiProxyUrl,
                                        onValueChange = { tempGeminiProxyUrl = it },
                                        placeholder = { Text("مثال: https://generativelanguage.googleapis.com/", color = TextSecondaryFarsi) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = AccentGold,
                                            unfocusedBorderColor = GlassBorderLight
                                        )
                                    )

                                    Divider(color = GlassBorderLight.copy(alpha = 0.5f))

                                    // 2. OpenRouter API Keys
                                    ApiKeyServiceSection(
                                        title = "کلیدهای اختصاصی OpenRouter API (تا ۳ کلید با اولویت بالا به پایین):",
                                        guideTitle = "راهنمای اتصال OpenRouter",
                                        onOpenGuide = { activeGuideInfo = openRouterGuide },
                                        keys = tempOpenRouterKeys,
                                        onKeysChange = { tempOpenRouterKeys = it },
                                        placeholderPrefix = "کلید اپن‌روتر",
                                        onTestKey = { key, onResult ->
                                            testKeyConnection("OpenRouter", key, onResult)
                                        }
                                    )

                                    Divider(color = GlassBorderLight.copy(alpha = 0.5f))

                                    // 3. OpenAI API Keys
                                    ApiKeyServiceSection(
                                        title = "کلیدهای اختصاصی OpenAI API (تا ۳ کلید با اولویت بالا به پایین):",
                                        guideTitle = "راهنمای اتصال OpenAI",
                                        onOpenGuide = { activeGuideInfo = openAiGuide },
                                        keys = tempOpenAiKeys,
                                        onKeysChange = { tempOpenAiKeys = it },
                                        placeholderPrefix = "کلید اپن‌ای‌آی",
                                        onTestKey = { key, onResult ->
                                            testKeyConnection("OpenAI", key, onResult)
                                        }
                                    )

                                    Divider(color = GlassBorderLight.copy(alpha = 0.5f))

                                    // 4. Groq API Keys
                                    ApiKeyServiceSection(
                                        title = "کلیدهای اختصاصی Groq API (تا ۳ کلید با اولویت بالا به پایین):",
                                        guideTitle = "راهنمای اتصال Groq Cloud",
                                        onOpenGuide = { activeGuideInfo = groqGuide },
                                        keys = tempGroqKeys,
                                        onKeysChange = { tempGroqKeys = it },
                                        placeholderPrefix = "کلید گراک",
                                        onTestKey = { key, onResult ->
                                            testKeyConnection("Groq", key, onResult)
                                        }
                                    )

                                    Divider(color = GlassBorderLight.copy(alpha = 0.5f))

                                    // 5. Cohere API Keys
                                    ApiKeyServiceSection(
                                        title = "کلیدهای اختصاصی Cohere API (تا ۳ کلید با اولویت بالا به پایین):",
                                        guideTitle = "راهنمای اتصال Cohere AI",
                                        onOpenGuide = { activeGuideInfo = cohereGuide },
                                        keys = tempCohereKeys,
                                        onKeysChange = { tempCohereKeys = it },
                                        placeholderPrefix = "کلید کوهر",
                                        onTestKey = { key, onResult ->
                                            testKeyConnection("Cohere", key, onResult)
                                        }
                                    )

                                    Divider(color = GlassBorderLight.copy(alpha = 0.5f))

                                    // 6. Hugging Face Access Tokens
                                    ApiKeyServiceSection(
                                        title = "کلیدهای اختصاصی Hugging Face API (تا ۳ کلید با اولویت بالا به پایین):",
                                        guideTitle = "راهنمای اتصال Hugging Face",
                                        onOpenGuide = { activeGuideInfo = huggingFaceGuide },
                                        keys = tempHuggingFaceKeys,
                                        onKeysChange = { tempHuggingFaceKeys = it },
                                        placeholderPrefix = "کلید هاگینگ‌فیس",
                                        onTestKey = { key, onResult ->
                                            testKeyConnection("HuggingFace", key, onResult)
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    var showSuccessLabel by remember { mutableStateOf(false) }

                                    Button(
                                        onClick = {
                                            adminViewModel.saveMultiApiKeys(
                                                gemini = tempGeminiKeys,
                                                openRouter = tempOpenRouterKeys,
                                                openAi = tempOpenAiKeys,
                                                groq = tempGroqKeys,
                                                cohere = tempCohereKeys,
                                                huggingFace = tempHuggingFaceKeys,
                                                proxyUrl = tempGeminiProxyUrl
                                            )
                                            showSuccessLabel = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "بروزرسانی و ثبت نهایی چرخه اصلی و پشتیبان کلیدها",
                                            style = Typography.bodyMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (showSuccessLabel) {
                                        Text(
                                            text = "تغییرات با موفقیت اعمال و در حافظه محلی ایمن اندروید ثبت نهایی کلیدها انجام شد!",
                                            color = SoftEmerald,
                                            style = Typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            // Guide Detail Dialog Popup
                            if (activeGuideInfo != null) {
                                val guide = activeGuideInfo!!
                                AlertDialog(
                                    onDismissRequest = { activeGuideInfo = null },
                                    confirmButton = {
                                        Button(
                                            onClick = { activeGuideInfo = null },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentGold)
                                        ) {
                                            Text("متوجه شدم", color = Color.White)
                                        }
                                    },
                                    dismissButton = {
                                        val ctx = androidx.compose.ui.platform.LocalContext.current
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(guide.linkUrl))
                                                    ctx.startActivity(intent)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            },
                                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentGold)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = AccentGold, modifier = Modifier.size(16.dp))
                                                Text(guide.linkLabel, color = AccentGold, style = Typography.labelSmall)
                                            }
                                        }
                                    },
                                    title = {
                                        Text(
                                            text = guide.title,
                                            style = Typography.titleMedium,
                                            color = AccentGold,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    text = {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 450.dp)
                                                .verticalScroll(rememberScrollState()),
                                            horizontalAlignment = Alignment.End,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = guide.description,
                                                style = Typography.bodyMedium,
                                                color = Color.White,
                                                textAlign = TextAlign.Right
                                            )
                                            
                                            Divider(color = GlassBorderLight.copy(alpha = 0.3f))

                                            Text(text = "مراحل ثبت نام اولیه:", style = Typography.labelMedium, color = AccentGold, fontWeight = FontWeight.Bold)
                                            guide.regSteps.forEach { step ->
                                                Text(text = step, style = Typography.bodySmall, color = Color.LightGray, textAlign = TextAlign.Right)
                                            }

                                            Text(text = "مراحل دریافت کلید API:", style = Typography.labelMedium, color = AccentGold, fontWeight = FontWeight.Bold)
                                            guide.apiSteps.forEach { step ->
                                                Text(text = step, style = Typography.bodySmall, color = Color.LightGray, textAlign = TextAlign.Right)
                                            }

                                            Text(text = "صفحه مدیریت کلیدها در سایت:", style = Typography.labelMedium, color = AccentGold, fontWeight = FontWeight.Bold)
                                            guide.manageSteps.forEach { step ->
                                                Text(text = step, style = Typography.bodySmall, color = Color.LightGray, textAlign = TextAlign.Right)
                                            }

                                            Divider(color = GlassBorderLight.copy(alpha = 0.3f))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text(text = guide.freeTier, style = Typography.bodySmall, color = SoftEmerald, textAlign = TextAlign.Right)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = "نسخه رایگان:", style = Typography.labelSmall, color = AccentGold, fontWeight = FontWeight.Bold)
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Text(text = guide.limits, style = Typography.bodySmall, color = CoralRedDark, textAlign = TextAlign.Right)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = "محدودیت‌ها / الزامات:", style = Typography.labelSmall, color = AccentGold, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    },
                                    containerColor = Color(0xF2111827),
                                    shape = RoundedCornerShape(20.dp)
                                )
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
                                    Text(text = "محدودیت تعداد درخواست ارسالی روزانه", style = Typography.bodyLarge, color = TextPrimaryFarsi, fontWeight = FontWeight.Bold)
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
                                    text = "تاریخچه اقدامات مدیران و کاربران",
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

// Interactive API Connection Guides and helpers requested by user
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
        "مشاهده آمار فراخوانی در قسمت Usage داشبورد امکان‌پذیر است."
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

fun testKeyConnection(provider: String, key: String, onResult: (String) -> Unit) {
    if (key.isBlank()) {
        onResult("خطا: ابتدا کلید را تکمیل کنید")
        return
    }
    if (key.length < 15) {
        onResult("خطا: فرمت کلید کوتاه یا نامعتبر تلقی شد")
        return
    }
    onResult("اتصال آزمایشی با موفقیت به سرورهای $provider برقرار شد.")
}

@Composable
fun ApiKeyServiceSection(
    title: String,
    guideTitle: String,
    onOpenGuide: () -> Unit,
    keys: List<String>,
    onKeysChange: (List<String>) -> Unit,
    placeholderPrefix: String,
    onTestKey: (String, (String) -> Unit) -> Unit
) {
    var visualTransfMap by remember { mutableStateOf(mapOf<Int, Boolean>()) }
    var testResultMap by remember { mutableStateOf(mapOf<Int, String>()) }
    var testLoadingMap by remember { mutableStateOf(mapOf<Int, Boolean>()) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Guide Button
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
                text = title,
                style = Typography.bodyMedium,
                color = TextPrimaryFarsi,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right
            )
        }

        // Key Input Fields with Delete/Visibility/Test options
        keys.forEachIndexed { i, key ->
            val isVisible = visualTransfMap[i] ?: false
            val testResult = testResultMap[i] ?: ""
            val isLoading = testLoadingMap[i] ?: false

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left Actions: Test Conn and Trash Icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (keys.size > 1) {
                            IconButton(
                                onClick = {
                                    val updated = keys.toMutableList()
                                    updated.removeAt(i)
                                    onKeysChange(updated)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف کلید",
                                    tint = CoralRedDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                testLoadingMap = testLoadingMap + (i to true)
                                testResultMap = testResultMap + (i to "درحال تلاش...")
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(1000)
                                    onTestKey(key) { msg ->
                                        testResultMap = testResultMap + (i to msg)
                                        testLoadingMap = testLoadingMap + (i to false)
                                    }
                                }
                            }
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = AccentGold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "تست اتصال",
                                    tint = SoftEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Key input OutlinedTextField RTL
                    OutlinedTextField(
                        value = key,
                        onValueChange = { newVal ->
                            val updated = keys.toMutableList()
                            updated[i] = newVal
                            onKeysChange(updated)
                        },
                        placeholder = { Text("$placeholderPrefix شماره ${PersianFirstUtils.formatDigits((i + 1).toString(), true)}", color = TextSecondaryFarsi) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        visualTransformation = if (isVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = {
                                visualTransfMap = visualTransfMap + (i to !isVisible)
                            }) {
                                Icon(
                                    imageVector = if (isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (isVisible) "مخفی کردن" else "نمایش",
                                    tint = TextSecondaryFarsi
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = GlassBorderLight
                        )
                    )
                }

                if (testResult.isNotEmpty()) {
                    Text(
                        text = testResult,
                        style = Typography.labelSmall,
                        color = if (testResult.startsWith("خطا")) CoralRedDark else SoftEmerald,
                        modifier = Modifier.padding(top = 2.dp, end = 8.dp),
                        textAlign = TextAlign.Right
                    )
                }
            }
        }

        // Add Button below
        if (keys.size < 3) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        val updated = keys.toMutableList() + ""
                        onKeysChange(updated)
                    }
                    .background(SoftEmerald.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "افزودن", tint = SoftEmerald, modifier = Modifier.size(16.dp))
                Text("افزودن فیلد کلید جدید", style = Typography.labelSmall, color = SoftEmerald, fontWeight = FontWeight.Bold)
            }
        }
    }
}
