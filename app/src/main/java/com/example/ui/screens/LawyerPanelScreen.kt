package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.components.FrostedGlassBackground
import com.example.ui.components.PersianFirstUtils
import com.example.ui.theme.*
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.UserRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Model classes for Lawyer Panel simulation
data class LawyerCase(
    val id: String,
    val clientName: String,
    val caseTitle: String,
    val courtBranch: String,
    val caseNumber: String,
    val dateNextSession: String,
    val lastUpdate: String,
    val status: String
)

data class LawyerNotification(
    val id: String,
    val title: String,
    val body: String,
    val date: String,
    val isRead: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawyerPanelScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Velvet Amber theme shades specifically matching the lawyer profile
    val lawyerPrimaryColor = Color(0xFFD97706) // Velvet Ochre Gold Amber
    val glassBorderColor = Color.White.copy(alpha = 0.12f)

    // Tab state
    var selectedTab by remember { mutableStateOf(0) } // 0: Cases, 1: CMS Sync, 2: Calculator, 3: Bill generator
    
    // DB simulated cases
    var casesList by remember {
        mutableStateOf(
            listOf(
                LawyerCase(
                    "۱",
                    "علیرضا رضایی",
                    "دعوای مطالبه وجه التزام قرارداد مشارکت در ساخت",
                    "شعبه ۴۲ دادگاه عمومی حقوقی مجتمع قضایی صدر تهران",
                    "۱۴۰۲۶۸۹۲۰۰۰۱۴۷۵۶",
                    "۱۴۰۶/۰۴/۱۵ ساعت ۱۰:۰۰صبح(تبادل لوایح)",
                    "ابلاغ نظریه کارشناسی رسمی دادگستری",
                    "در جریان رسیدگی"
                ),
                LawyerCase(
                    "۲",
                    "زهرا کریمی",
                    "دفاع در قبال خلع ید مغازه تجاری بازار بزرگ",
                    "شعبه ۱۲ دادگاه تجدیدنظر استان تهران",
                    "۱۴۰۱۶۸۹۲۰۰۰۸۴۹۲۰",
                    "۱۴۰۶/۰۳/۲۸ ساعت ۰۹:۳۰صبح(حضور اصحاب دعوا)",
                    "تعیین وقت جلسه دادرسی تجدیدنظر",
                    "در جریان تجدیدنظر"
                ),
                LawyerCase(
                    "۳",
                    "شرکت مبنا سازه پاد",
                    "ابطال رای داور در قرارداد خرید مصالح ساختمانی",
                    "شعبه ۸۹ دادگاه عمومی حقوقی مجتمع قضایی شهید بهشتی",
                    "۱۴۰۲۶۸۹۲۰۰۰۷۴۵۱۲",
                    "مواجه با دستور ابطال وقت دادرسی",
                    "صدور نظریه تکمیلی هیات ۳ نفره کارشناسان",
                    "رای صادره - در مرحله تجدیدنظر"
                )
            )
        )
    }

    // CMS notifications simulated state
    var cmsNotifications by remember {
        mutableStateOf(
            listOf(
                LawyerNotification(
                    "۱",
                    "ابلاغیه الکترونیکی جدید (ثنا)",
                    "ابلاغ نظریه هیئت ۳ نفره کارشناسی در خصوص کلاسه پرونده ۱۴۰۲۶۸۹۲۰۰۰۱۴۷۵۶",
                    "۱۴۰۶/۰۳/۱۸",
                    false
                ),
                LawyerNotification(
                    "۲",
                    "اوقات دادرسی ابلاغ شده",
                    "تعیین وقت حضور در جلسه دادرسی شعبه ۱۲ دادگاه تجدیدنظر جهت تاریخ ۱۴۰۶/۰۳/۲۸",
                    "۱۴۰۶/۰۳/۱۶",
                    false
                ),
                LawyerNotification(
                    "۳",
                    "صدور اجرائیه نهایی",
                    "صدور برگ اجرائیه کلاسه بایگانی ۹۹۰۰۳۲۴ علیه محکوم‌علیه مسعود مرادی",
                    "۱۴۰۶/۰۳/۱۰",
                    true
                )
            )
        )
    }

    // Modal adding case state
    var showAddCaseDialog by remember { mutableStateOf(false) }
    var newClientName by remember { mutableStateOf("") }
    var newCaseTitle by remember { mutableStateOf("") }
    var newCourtBranch by remember { mutableStateOf("") }
    var newCaseNumber by remember { mutableStateOf("") }
    var newSessionDate by remember { mutableStateOf("") }

    // Calculator states
    var disputeAmountText by remember { mutableStateOf("") }
    var isFirstInstance by remember { mutableStateOf(true) } // Court of first instance or appeal
    var disputeResultCourtFee by remember { mutableStateOf(0.0) }
    var disputeResultTariffFee by remember { mutableStateOf(0.0) }

    // Damages calculations parameters
    var baseAmountText by remember { mutableStateOf("") }
    var yearFromText by remember { mutableStateOf("1401") }
    var yearToText by remember { mutableStateOf("1405") }
    var lateDamagesResult by remember { mutableStateOf(0.0) }

    // Bill drafter states
    var selectedTemplate by remember { mutableStateOf("تجدیدنظرخواهی") }
    var billSubject by remember { mutableStateOf("لایحه تجدیدنظرخواهی در خصوص کلاسه پرونده") }
    var clientRoleInCase by remember { mutableStateOf("تجدیدنظرخواه (محکوم‌علیه)") }
    var draftContent by remember { mutableStateOf("") }
    var isGeneratingAiBill by remember { mutableStateOf(false) }

    // Update draft template when changed
    LaunchedEffect(selectedTemplate, billSubject, clientRoleInCase) {
        draftContent = when (selectedTemplate) {
            "تجدیدنظرخواهی" -> """ریاست محترم دادگاه تجدیدنظر استان
موضوع: لایحه تجدیدنظرخواهی نسبت به دادنامه شماره صادره از شعبه دادگاه عمومی حقوقی

با سلام و احترام،
اینجانب دکتر حسین پورمحی‌آبادی به وکالت از تجدیدنظرخواه، در فرجه قانونی مقرر نسبت به دادنامه فوق‌الاشاره اعتراض نموده و دلایل نقض آن را به شرح ذیل معروق می‌دارم:

۱. عدم توجه به ادله ابرازی و مستندات پرداخت وجه:
دادگاه نخستین متاسفانه نسبت به فیش‌های واریزی پیوست پرونده که مؤید ایفای کامل تعهدات موکل است تسامح ورزیده است.

۲. مخالفت رای صادره با اصول حاکم بر تفسیر قراردادها:
بر اساس ماده ۲۲۳ قانون مدنی، اصل بر صحت قراردادها بوده و شروط مندرج به نفع موکل تفهیم شده است که در رای بدوی مغفول مانده.

بنا به مراتب فوق، مستنداً به ماده ۳۴۸ قانون آیین دادرسی مدنی، تقاضای نقض دادنامه بدوی و صدور حکم شایسته بر برائت ذمه موکل را استدعا دارم."""
            "تعدیل اقساط" -> """ریاست محترم شعبه دادگاه عمومی حقوقی کلاسه پرونده: ۹۹۰۳۴۵
موضوع: دادخواست تعدیل اقساط محکوم‌به به دلیل اعسار ثانوی و حرج شدید معیشتی

با سلام و تسلیم تحیات،
احتراماً به استحضار می‌رساند موکل اینجانب بر اساس دادنامه شماره به پرداخت مهریه/بدهی به صورت اقساطی محکوم گردیده است. ولیکن مستند به مدارک پیوست، به دلیل نوسانات شدید بازار مسکن و تورم بالای سالیانه بانک مرکزی، توان پرداختی موکل به شدت کاهش یافته به طوری که دخل و خرج وی فاقد توازن حداقلی است.
لذا مستنداً به ماده ۱۱ قانون نحوه اجرای محکومیت‌های مالی، صدور حکم بر تعدیل اقساط مذکور مورد استدعاست."""
            "مشارکت در ساخت" -> """قرارداد مشارکت در ساخت مصالح و آپارتمان مسکونی
ماده ۱: طرفین قرارداد
سازنده: شرکت فنی مهندسی ... به نمایندگی ...
مالک زمین: آقای/خانم ... به شماره ملی ...

ماده ۲: موضوع قرارداد
مشارکت، طراحی، اخذ پروانه ساختمانی و احداث بنای مسکونی ۶ طبقه به روی پلاک ثبتی شماره فرعی از اصلی... بخش ثبتی با متریال عالی درجه یک مطابق صورت ریز پیوست شماره یک.

ماده ۳: سهم الشرکه طرفین
سهم مالک زمین میزان ۵۵٪ از کل اعیانی احداثی و سهم سازنده میزان ۴۵٪ تعیین گردید."""
            else -> ""
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = lawyerPrimaryColor, modifier = Modifier.size(24.dp))
                        Text(
                            text = "پیشخوان فوق تخصصی وکلای دادگستری",
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // Fast Logout simulator
                        authViewModel.logout()
                    }) {
                        Icon(Icons.Default.Logout, contentDescription = "خروج", tint = CoralRedDark)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0F172A).copy(alpha = 0.90f)
                )
            )
        }
    ) { innerPadding ->
        FrostedGlassBackground {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Header Welcome with Lawyer Name & Elegant Info Shield
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    lawyerPrimaryColor.copy(alpha = 0.25f),
                                    Color(0xFF1E293B).copy(alpha = 0.85f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(1.2.dp, lawyerPrimaryColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "عضو رسمی کانون وکلای دادگستری مرکز",
                                style = Typography.labelSmall,
                                color = lawyerPrimaryColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(lawyerPrimaryColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "جناب آقای دکتر حسین پورمحی‌آبادی",
                                    style = Typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "شماره پروانه وکالت: ۲۹۴۷۷/ک",
                                    style = Typography.bodySmall,
                                    color = TextSecondaryFarsi
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = 0.15f))

                        // Brief stats row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${casesList.size} پرونده", style = Typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                Text(text = "کل پرونده‌های فعال", style = Typography.labelSmall, color = TextSecondaryFarsi)
                            }
                            VerticalDivider(modifier = Modifier.height(24.dp), color = Color.White.copy(alpha = 0.15f))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "${cmsNotifications.filter { !it.isRead }.size} ابلاغیه", style = Typography.bodyMedium, color = CoralRedDark, fontWeight = FontWeight.Bold)
                                Text(text = "خوانده نشده جدید", style = Typography.labelSmall, color = TextSecondaryFarsi)
                            }
                            VerticalDivider(modifier = Modifier.height(24.dp), color = Color.White.copy(alpha = 0.15f))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "۲ جلسه", style = Typography.bodyMedium, color = SoftEmerald, fontWeight = FontWeight.Bold)
                                Text(text = "جلسات هفته آینده", style = Typography.labelSmall, color = TextSecondaryFarsi)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Beautiful Persian Modern Scrollable Tab Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.90f))
                        .border(1.dp, GlassBorderLight, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf("تنظیم لایحه", "محاسبات مالی", "اتصال عدل ایران", "پرونده‌ها و موکلین")
                    val tabIcons = listOf(Icons.Default.Create, Icons.Default.Calculate, Icons.Default.Sync, Icons.Default.FolderSpecial)
                    
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTab == 3 - index // reverse for Persian RTL layout
                        val currentIdx = 3 - index

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = currentIdx }
                                .background(if (isSelected) lawyerPrimaryColor else Color.Transparent)
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = tabIcons[currentIdx],
                                    contentDescription = title,
                                    tint = if (isSelected) Color.White else TextSecondaryFarsi,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = title,
                                    style = Typography.labelSmall,
                                    color = if (isSelected) Color.White else TextSecondaryFarsi,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Render respective tab layout dynamically with slide animation
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> CasesManagementTab(
                            casesList = casesList,
                            onAddCaseClick = { showAddCaseDialog = true },
                            lawyerPrimaryColor = lawyerPrimaryColor
                        )
                        1 -> CmsSyncTab(
                            notifications = cmsNotifications,
                            onSyncClick = {
                                // Simulate CMS sync loading and update notify
                                coroutineScope.launch {
                                    cmsNotifications = listOf(
                                        LawyerNotification(
                                            "۱",
                                            "ابلاغیه الکترونیکی جدید (ثنا)",
                                            "ابلاغیه صادر شده در خصوص دعوت به پرداخت هزینه کارشناسی در پرونده ابطال سند کلاسه ۱۴۰۲۶۸۹۲۰۰۰۱۴۷۵۶",
                                            "۱۴۰۶/۰۳/۱۸",
                                            false
                                        ),
                                        LawyerNotification(
                                            "۴",
                                            "ابلاغ رای نهایی دیوان عالی",
                                            "تایید رای ابرام اعتراض فرجامی صادره در خصوص کلاسه بایگانی ۹۸۰۰۲۳۴ قانون مجازات اسلامی",
                                            "۱۴۰۶/۰۳/۱۹",
                                            false
                                        )
                                    ) + cmsNotifications
                                    // Simulated toast
                                }
                            },
                            lawyerPrimaryColor = lawyerPrimaryColor,
                            uriHandler = uriHandler
                        )
                        2 -> OnlineCalculatorsTab(
                            amountText = disputeAmountText,
                            onAmountChange = { disputeAmountText = it },
                            isFirstInstance = isFirstInstance,
                            onInstanceChange = { isFirstInstance = it },
                            courtFeeResult = disputeResultCourtFee,
                            tariffFeeResult = disputeResultTariffFee,
                            onCalculate = {
                                val amtNum = disputeAmountText.toDoubleOrNull() ?: 0.0
                                if (isFirstInstance) {
                                    // First Instance Court Fee: 2.5% for <= 20M, 3.5% for > 20M Tomans
                                    disputeResultCourtFee = if (amtNum <= 20000000.0) {
                                        amtNum * 0.025
                                    } else {
                                        amtNum * 0.035
                                    }
                                    // Tariff fee simulator: typical standard scale (approx 4% to 8%)
                                    disputeResultTariffFee = amtNum * 0.06
                                } else {
                                    // Appeal court fee: 4.5% of dispute amount
                                    disputeResultCourtFee = amtNum * 0.045
                                    disputeResultTariffFee = amtNum * 0.04
                                }
                            },
                            baseLateText = baseAmountText,
                            onBaseLateChange = { baseAmountText = it },
                            yearFrom = yearFromText,
                            onYearFromChange = { yearFromText = it },
                            yearTo = yearToText,
                            onYearToChange = { yearToText = it },
                            lateDamagesResult = lateDamagesResult,
                            onCalculateLate = {
                                val base = baseAmountText.toDoubleOrNull() ?: 0.0
                                val from = yearFromText.toIntOrNull() ?: 1401
                                val to = yearToText.toIntOrNull() ?: 1405
                                val diffYears = (to - from).coerceAtLeast(1)
                                // Standard simulated inflation compounding damage rate over time
                                lateDamagesResult = base * (diffYears * 0.38)
                            },
                            lawyerPrimaryColor = lawyerPrimaryColor
                        )
                        3 -> SmartBillGeneratorTab(
                            selectedTemplate = selectedTemplate,
                            onTemplateSelected = { selectedTemplate = it },
                            subject = billSubject,
                            onSubjectChange = { billSubject = it },
                            role = clientRoleInCase,
                            onRoleChange = { clientRoleInCase = it },
                            content = draftContent,
                            onContentChange = { draftContent = it },
                            isLoadingAI = isGeneratingAiBill,
                            onGenerateAi = {
                                isGeneratingAiBill = true
                                coroutineScope.launch {
                                    delay(2000)
                                    draftContent = "/* اصلاح لایحه مجهز به دستیار عریضه‌نویسی کانون دادرس */\n\nبه وکالت از موکل، معروض می‌دارد رای صادره به جهت تضاد آشکار مابین مدلل دادنامه و بند ۴ قرارداد مخل صحت تلقی شده و شایسته اصرار نمی‌باشد..."
                                    isGeneratingAiBill = false
                                }
                            },
                            onCopyInput = {
                                clipboardManager.setText(AnnotatedString(draftContent))
                            },
                            lawyerPrimaryColor = lawyerPrimaryColor
                        )
                    }
                }
            }

            // Simple Dialog for adding cases
            if (showAddCaseDialog) {
                Dialog(onDismissRequest = { showAddCaseDialog = false }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF0F172A))
                            .border(1.2.dp, lawyerPrimaryColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "افزودن پرونده و موکل حقوقی جدید",
                                style = Typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = lawyerPrimaryColor,
                                textAlign = TextAlign.Right
                            )

                            OutlinedTextField(
                                value = newClientName,
                                onValueChange = { newClientName = it },
                                placeholder = { Text("نام و نام خانوادگی موکل", color = TextSecondaryFarsi) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = lawyerPrimaryColor,
                                    unfocusedBorderColor = GlassBorderLight,
                                    focusedTextColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = newCaseTitle,
                                onValueChange = { newCaseTitle = it },
                                placeholder = { Text("موضوع و خواسته پرونده قضایی", color = TextSecondaryFarsi) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = lawyerPrimaryColor,
                                    unfocusedBorderColor = GlassBorderLight,
                                    focusedTextColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = newCourtBranch,
                                onValueChange = { newCourtBranch = it },
                                placeholder = { Text("شعبه دادگاه و مرجع قضایی", color = TextSecondaryFarsi) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = lawyerPrimaryColor,
                                    unfocusedBorderColor = GlassBorderLight,
                                    focusedTextColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = newCaseNumber,
                                onValueChange = { newCaseNumber = it },
                                placeholder = { Text("شماره پرونده ۱۶ رقمی ثنا", color = TextSecondaryFarsi) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = lawyerPrimaryColor,
                                    unfocusedBorderColor = GlassBorderLight,
                                    focusedTextColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = newSessionDate,
                                onValueChange = { newSessionDate = it },
                                placeholder = { Text("ابلاغیه وقت دادرسی حضور بعدی", color = TextSecondaryFarsi) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = lawyerPrimaryColor,
                                    unfocusedBorderColor = GlassBorderLight,
                                    focusedTextColor = Color.White
                                )
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { showAddCaseDialog = false },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                                ) {
                                    Text("انصراف", color = Color.White)
                                }

                                Button(
                                    onClick = {
                                        if (newClientName.isNotBlank() && newCaseTitle.isNotBlank()) {
                                            casesList = casesList + LawyerCase(
                                                (casesList.size + 1).toString(),
                                                newClientName,
                                                newCaseTitle,
                                                if (newCourtBranch.isBlank()) "شعبه شعبه عمومی حقوقی صدر" else newCourtBranch,
                                                if (newCaseNumber.isBlank()) "۱۴۰۲۶۸۹۲۰۰۰۱۴۷۵۶" else newCaseNumber,
                                                if (newSessionDate.isBlank()) "ثبت نشده" else newSessionDate,
                                                "پرونده فیزیکی جدید ثبت شده توسط وکیل",
                                                "در جریان اول"
                                            )
                                            // Reset inputs
                                            newClientName = ""
                                            newCaseTitle = ""
                                            newCourtBranch = ""
                                            newCaseNumber = ""
                                            newSessionDate = ""
                                            showAddCaseDialog = false
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = lawyerPrimaryColor)
                                ) {
                                    Text("ثبت پرونده", color = Color.White, fontWeight = FontWeight.Bold)
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
fun CasesManagementTab(
    casesList: List<LawyerCase>,
    onAddCaseClick: () -> Unit,
    lawyerPrimaryColor: Color
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.End
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // New Case FAB as beautiful button
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAddCaseClick() }
                    .background(lawyerPrimaryColor.copy(alpha = 0.20f))
                    .border(1.dp, lawyerPrimaryColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "افزودن پرونده", tint = lawyerPrimaryColor, modifier = Modifier.size(16.dp))
                Text("افزودن پرونده و موکل رسمی", style = Typography.labelMedium, color = lawyerPrimaryColor, fontWeight = FontWeight.Bold)
            }

            Text(
                text = "پرونده‌های در جریان موکلین شما",
                style = Typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(casesList) { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                        .border(1.dp, GlassBorderLight, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.status,
                                style = Typography.labelSmall,
                                color = if (item.status.contains("رای")) CoralRedDark else SoftEmerald,
                                modifier = Modifier
                                    .background(if (item.status.contains("رای")) CoralRedDark.copy(alpha = 0.15f) else SoftEmerald.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "موکل: ${item.clientName}",
                                    style = Typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(Icons.Default.Person, contentDescription = null, tint = lawyerPrimaryColor, modifier = Modifier.size(18.dp))
                            }
                        }

                        Text(
                            text = item.caseTitle,
                            style = Typography.bodySmall,
                            color = AccentGold,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "کلاسه ثنا: ${item.caseNumber}",
                            style = Typography.bodySmall,
                            color = TextSecondaryFarsi,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "مرجع قضایی: ${item.courtBranch}",
                            style = Typography.bodySmall,
                            color = TextSecondaryFarsi,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.15f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, tint = SoftEmerald, modifier = Modifier.size(16.dp))
                                Text(
                                    text = item.dateNextSession,
                                    style = Typography.labelSmall,
                                    color = SoftEmerald,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "ابلاغیه آخر: ${item.lastUpdate}",
                                style = Typography.labelSmall,
                                color = TextSecondaryFarsi
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CmsSyncTab(
    notifications: List<LawyerNotification>,
    onSyncClick: () -> Unit,
    lawyerPrimaryColor: Color,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.End
    ) {
        // Sana CMS Connected Status Panel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                .border(1.dp, lawyerPrimaryColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSyncClick() }
                            .background(SoftEmerald.copy(alpha = 0.20f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "استعلام زنده", tint = SoftEmerald, modifier = Modifier.size(16.dp))
                        Text("همگام‌سازی و استعلام زنده عدلیران", style = Typography.labelSmall, color = SoftEmerald, fontWeight = FontWeight.Bold)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("وضعیت اتصال به درگاه عدل ایران (مخصوص وکلا)", style = Typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
                        Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(SoftEmerald))
                    }
                }

                Text(
                    text = "سامانه CMS یکپارچه داخلی در حال حاضر با توکن هوشمند شما متصل است. در زیر لیست پیام‌ها و ابلاغیه‌های اخیر واکشی شده از کارتابل آورده شده است.",
                    style = Typography.labelSmall,
                    color = TextSecondaryFarsi,
                    textAlign = TextAlign.Right
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Direct Browser Links for Lawyers
        Text(text = "درگاه‌های مستقیم و خارجی خدمات قضایی", style = Typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("سامانه کانون (ایکاف)", "https://portals.icbar.org", Icons.Default.Language),
                Triple("مرکز وکلا قوه قضائیه", "https://www.23055.ir", Icons.Default.Public),
                Triple("ثنا عدل ایران مخصوص وکلا", "https://sana.adliran.ir", Icons.Default.OpenInNew)
            ).forEach { item ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { uriHandler.openUri(item.second) }
                        .background(Color(0xFF1E293B))
                        .border(1.dp, GlassBorderLight, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(item.third, contentDescription = null, tint = lawyerPrimaryColor, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(item.first, style = Typography.labelSmall, color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "ابلاغیه‌های دریافتی عدل ایران", style = Typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(notifications) { msg ->
                val isUnread = !msg.isRead
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(10.dp))
                        .background(if (isUnread) Color(0xFF131B2E) else Color(0xFF1E293B))
                        .border(1.dp, if (isUnread) lawyerPrimaryColor.copy(alpha = 0.5f) else GlassBorderLight, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(msg.date, style = Typography.labelSmall, color = TextSecondaryFarsi)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(msg.title, style = Typography.bodySmall, color = if (isUnread) lawyerPrimaryColor else Color.White, fontWeight = FontWeight.Bold)
                                if (isUnread) {
                                    Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(CoralRedDark))
                                }
                            }
                        }
                        Text(msg.body, style = Typography.labelSmall, color = if (isUnread) Color.White else TextSecondaryFarsi, textAlign = TextAlign.Right)
                    }
                }
            }
        }
    }
}

@Composable
fun OnlineCalculatorsTab(
    amountText: String,
    onAmountChange: (String) -> Unit,
    isFirstInstance: Boolean,
    onInstanceChange: (Boolean) -> Unit,
    courtFeeResult: Double,
    tariffFeeResult: Double,
    onCalculate: () -> Unit,
    baseLateText: String,
    onBaseLateChange: (String) -> Unit,
    yearFrom: String,
    onYearFromChange: (String) -> Unit,
    yearTo: String,
    onYearToChange: (String) -> Unit,
    lateDamagesResult: Double,
    onCalculateLate: () -> Unit,
    lawyerPrimaryColor: Color
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.End
    ) {
        item {
            Text(
                "ماشین‌حساب هزینه دادرسی و تعرفه کانون وکلا",
                style = Typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                    .border(1.dp, GlassBorderLight, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = onAmountChange,
                        placeholder = { Text("مبلغ خواسته دعوی (به ریال)", color = TextSecondaryFarsi) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = lawyerPrimaryColor,
                            unfocusedBorderColor = GlassBorderLight
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مرحله تجدیدنظرخواهی (۴.۵٪)", style = Typography.labelMedium, color = Color.White)
                        RadioButton(selected = !isFirstInstance, onClick = { onInstanceChange(false) }, colors = RadioButtonDefaults.colors(selectedColor = lawyerPrimaryColor))
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Text("مرحه نخستین (بدوی ۲.۵٪ و ۳.۵٪)", style = Typography.labelMedium, color = Color.White)
                        RadioButton(selected = isFirstInstance, onClick = { onInstanceChange(true) }, colors = RadioButtonDefaults.colors(selectedColor = lawyerPrimaryColor))
                    }

                    Button(
                        onClick = onCalculate,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = lawyerPrimaryColor)
                    ) {
                        Text("محاسبه سریع هزینه دادرسی قانونی", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (courtFeeResult > 0.0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                "هزینه تمبر دادخواست: ${PersianFirstUtils.formatDigits(String.format("%,.0f", courtFeeResult), true)} ریال",
                                style = Typography.bodyMedium,
                                color = SoftEmerald,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "حق‌الوکاله پیشنهادی آیین‌نامه تعرفه: ${PersianFirstUtils.formatDigits(String.format("%,.0f", tariffFeeResult), true)} ریال",
                                style = Typography.bodySmall,
                                color = AccentGold
                            )
                        }
                    }
                }
            }
        }

        // Damages Calculator
        item {
            Text(
                "ماشین‌حساب محاسبه خسارت تاخیر تادیه (نرخ تورم بانک مرکزی)",
                style = Typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                    .border(1.dp, GlassBorderLight, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = baseLateText,
                        onValueChange = onBaseLateChange,
                        placeholder = { Text("اصل مبلغ بدهی (به ریال)", color = TextSecondaryFarsi) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = lawyerPrimaryColor,
                            unfocusedBorderColor = GlassBorderLight
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = yearTo,
                            onValueChange = onYearToChange,
                            placeholder = { Text("سال تادیه (سررسید)", color = TextSecondaryFarsi) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                focusedBorderColor = lawyerPrimaryColor,
                                unfocusedBorderColor = GlassBorderLight
                            )
                        )

                        OutlinedTextField(
                            value = yearFrom,
                            onValueChange = onYearFromChange,
                            placeholder = { Text("سال مبدا (سررسید بدهی)", color = TextSecondaryFarsi) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                focusedBorderColor = lawyerPrimaryColor,
                                unfocusedBorderColor = GlassBorderLight
                            )
                        )
                    }

                    Button(
                        onClick = onCalculateLate,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("محاسبه خسارت قانونی تاخیر", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    if (lateDamagesResult > 0.0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                "مبلغ خسارت تادیه: ${PersianFirstUtils.formatDigits(String.format("%,.0f", lateDamagesResult), true)} ریال",
                                style = Typography.bodyMedium,
                                color = SoftEmerald,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SmartBillGeneratorTab(
    selectedTemplate: String,
    onTemplateSelected: (String) -> Unit,
    subject: String,
    onSubjectChange: (String) -> Unit,
    role: String,
    onRoleChange: (String) -> Unit,
    content: String,
    onContentChange: (String) -> Unit,
    isLoadingAI: Boolean,
    onGenerateAi: () -> Unit,
    onCopyInput: () -> Unit,
    lawyerPrimaryColor: Color
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.End
    ) {
        item {
            Text(
                "دستیار تنظیم خودکار لایحه دفاعیه و عرا‌یض قانونی",
                style = Typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        // Templates Row Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("تعدیل اقساط", "مشارکت در ساخت", "تجدیدنظرخواهی").forEach { t ->
                    val isS = selectedTemplate == t
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onTemplateSelected(t) }
                            .background(if (isS) lawyerPrimaryColor.copy(alpha = 0.25f) else Color.Transparent)
                            .border(1.2.dp, if (isS) lawyerPrimaryColor else GlassBorderLight, RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(t, style = Typography.labelSmall, color = if (isS) lawyerPrimaryColor else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = subject,
                onValueChange = onSubjectChange,
                label = { Text("موضوع لایحه", color = TextSecondaryFarsi) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = lawyerPrimaryColor,
                    unfocusedBorderColor = GlassBorderLight
                )
            )
        }

        item {
            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                label = { Text("متن پیش‌نویس لایحه قانونی", color = TextSecondaryFarsi) },
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right, fontSize = 13.sp),
                minLines = 10,
                maxLines = 14,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = lawyerPrimaryColor,
                    unfocusedBorderColor = GlassBorderLight
                )
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCopyInput,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "کپی", tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("کپی متن لایحه", color = Color.White)
                    }
                }

                Button(
                    onClick = onGenerateAi,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = lawyerPrimaryColor)
                ) {
                    if (isLoadingAI) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "هوشمند", tint = Color.White, modifier = Modifier.size(16.dp))
                            Text("بهینه‌سازی هوشمند لایحه", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
