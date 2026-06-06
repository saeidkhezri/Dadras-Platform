package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.components.FrostedGlassBackground
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.CitizenViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestWizardScreen(
    authViewModel: AuthViewModel,
    citizenViewModel: CitizenViewModel,
    onNavigateBack: () -> Unit
) {
    val session by authViewModel.session.collectAsState()
    val currentStep by citizenViewModel.currentStep.collectAsState()
    val requestType by citizenViewModel.requestType.collectAsState()
    val caseDescription by citizenViewModel.caseDescription.collectAsState()

    val plaintiffName by citizenViewModel.plaintiffName.collectAsState()
    val defendantName by citizenViewModel.defendantName.collectAsState()
    val beneficiaryName by citizenViewModel.beneficiaryName.collectAsState()
    val legalPosition by citizenViewModel.legalPosition.collectAsState()
    val confidenceScore by citizenViewModel.confidenceScore.collectAsState()
    val suggestedEvidence by citizenViewModel.suggestedEvidence.collectAsState()
    val requestedRelief by citizenViewModel.requestedRelief.collectAsState()
    val uploadedFiles by citizenViewModel.uploadedFiles.collectAsState()

    val gptOutput by citizenViewModel.gptOutput.collectAsState()
    val claudeOutput by citizenViewModel.claudeOutput.collectAsState()
    val geminiOutput by citizenViewModel.geminiOutput.collectAsState()
    val deepSeekOutput by citizenViewModel.deepSeekOutput.collectAsState()
    val qwenOutput by citizenViewModel.qwenOutput.collectAsState()
    val unifiedOutput by citizenViewModel.unifiedOutput.collectAsState()

    val isAnalyzing by citizenViewModel.isAnalyzing.collectAsState()
    val isGenerating by citizenViewModel.isGenerating.collectAsState()

    var activeOutputTab by remember { mutableStateOf("unified") } // unified, gemini, gpt, claude, deepseek, qwen
    var finalCaseTitle by remember { mutableStateOf("") }
    var operationResultMsg by remember { mutableStateOf<String?>(null) }

    var isPlainExplanation by remember { mutableStateOf(false) }
    var showDebateMode by remember { mutableStateOf(false) }
    var showLitigationSimulator by remember { mutableStateOf(false) }
    var isDebating by remember { mutableStateOf(false) }
    var debateLog by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val isDark by authViewModel.isDarkTheme.collectAsState()

    // Dynamic theming colors
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val glassBorderColor = if (isDark) GlassBorderDark else Color(0x33000000)

    FrostedGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "جادوگر تنظیم اسناد قضایی (۸ گام)",
                            style = Typography.titleLarge,
                            color = AccentGold,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت", tint = AccentGold)
                        }
                    },
                    actions = {
                        IconButton(onClick = { citizenViewModel.resetWizard() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "ریست", tint = AccentGold)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Progress Step Indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        (1..8).forEach { step ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                step == currentStep -> AccentGold
                                                step < currentStep -> SoftEmerald
                                                else -> if (isDark) SlateNavyMedium else Color(0x1F000000)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (step < currentStep) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    } else {
                                        Text(
                                            text = step.toString(),
                                            style = Typography.labelMedium,
                                            color = if (step == currentStep) Color.Black else onSurfaceColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (step) {
                                        1 -> "نوع سند"
                                        2 -> "شرح واقعه"
                                        3 -> "تحلیل هویتی"
                                        4 -> "شواهد پیشنهادی"
                                        5 -> "خواسته نهایی"
                                        6 -> "پردازش AI"
                                        7 -> "لایحه قضایی"
                                        8 -> "خروجی رسمی"
                                        else -> ""
                                    },
                                    style = Typography.labelSmall,
                                    color = if (step == currentStep) AccentGold else onSurfaceColor,
                                    fontSize = 8.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Main Step content container with scrolling
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                            .padding(bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        when (currentStep) {
                            1 -> {
                                Text(
                                    text = "گام اول: نوع سند حقوقی خود را انتخاب کنید",
                                    style = Typography.titleLarge,
                                    color = onBgColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )

                                val types = listOf(
                                    "شکایت" to "برای اعلام جرم کیفری به دادسرا",
                                    "دادخواست" to "به منظور مطالبه حقوقی و مالی از دادگاه مدنی",
                                    "لایحه دفاعیه" to "جهت ابراز دفاعیات در جلسات حضوری دادگاه",
                                    "اظهارنامه" to "مطالب رسمی قبل از طرح پرونده قضایی جهت تذکر تعهدات",
                                    "تجدیدنظرخواهی" to "اعتراض قانونی به احکام صادره بدوی",
                                    "فرجام‌خواهی" to "اعتراض حقوقی به دیوان عالی کشور",
                                    "مشاوره حقوقی" to "پرسش و پاسخ تفصیلی قوانین",
                                    "سایر" to "اسناد عمومی، وصیت‌نامه، صلح‌نامه‌های اختصاصی"
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    types.forEach { pair ->
                                        val isSelected = requestType == pair.first
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) AccentGold.copy(alpha = 0.12f) else surfaceColor)
                                                .border(
                                                    width = 1.dp,
                                                    color = if (isSelected) AccentGold else glassBorderColor,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable { citizenViewModel.updateRequestType(pair.first) }
                                                .padding(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = { citizenViewModel.updateRequestType(pair.first) },
                                                    colors = RadioButtonDefaults.colors(selectedColor = AccentGold)
                                                )
                                                Column(
                                                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                                                    horizontalAlignment = Alignment.End
                                                ) {
                                                    Text(text = pair.first, style = Typography.titleMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                    Text(text = pair.second, style = Typography.labelMedium, color = onSurfaceColor)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            2 -> {
                                Text(
                                    text = "گام دوم: شرح تفصیلی واقعه را بنویسید یا ضبط کنید",
                                    style = Typography.titleLarge,
                                    color = onBgColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )

                                OutlinedTextField(
                                    value = caseDescription,
                                    onValueChange = { citizenViewModel.updateCaseDescription(it) },
                                    placeholder = {
                                        Text(
                                            text = "مثال: اینجانب در تاریخ ۱۴۰۵/۰۲/۰۱ مبلغ پنجاه میلیون تومان برای خرید کامپیوتر به حساب آقای علی حسینی واریز کردم ولی ایشان کالا را تحویل نداده...",
                                            color = onSurfaceColor,
                                            fontSize = 13.sp,
                                            textAlign = TextAlign.Right
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .testTag("case_desc_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = onBgColor,
                                        unfocusedTextColor = onBgColor,
                                        focusedBorderColor = AccentGold,
                                        unfocusedBorderColor = glassBorderColor
                                    )
                                )

                                // Dynamic voice action row
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(surfaceColor, RoundedCornerShape(16.dp))
                                        .border(1.dp, glassBorderColor, RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "مکانیزم ورودی صوتی و آپلود مدارک",
                                        style = Typography.titleMedium,
                                        color = AccentGold,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Right
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                citizenViewModel.updateCaseDescription(
                                                    "اینجانب با واریز بیست میلیون ریال جهت رهن مغازه به حساب مشتکی‌عنه قرارداد داشتم ولیکن وجه دریافتی مسترد نگردیده است."
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SlateNavyMedium),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("شبیه‌سازی گفتار به متن", style = Typography.labelMedium, color = AccentGold)
                                        }

                                        Button(
                                            onClick = {
                                                citizenViewModel.uploadSimulatedFile("فیش_بانکی_تراکنش_۹۸۲۳.pdf")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = SlateNavyMedium),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("افزودن مدرک فیزیکی", style = Typography.labelMedium, color = AccentGold)
                                        }
                                    }

                                    if (uploadedFiles.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "فایل‌های ضمیمه شده:",
                                            style = Typography.labelSmall,
                                            color = onSurfaceColor,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Right
                                        )
                                        uploadedFiles.forEach { file ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(onClick = { citizenViewModel.removeSimulatedFile(file) }) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, tint = SoftCrimson)
                                                }
                                                Text(text = file, color = SoftEmerald, style = Typography.labelMedium)
                                            }
                                        }
                                    }
                                }
                            }

                            3 -> {
                                Text(
                                    text = "گام سوم: تحلیل کلمات کلیدی و طرفین دعوی با Gemini",
                                    style = Typography.titleLarge,
                                    color = onBgColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )

                                Text(
                                    text = "هوش مصنوعی دادرس در این گام به شکل خودکار واقعه شما را بررسی کرده و عناوین حقوقی، شاکی، متهم و درصد اطمینان حقوقی پرونده را استخراج می‌کند.",
                                    style = Typography.bodyMedium,
                                    color = onSurfaceColor,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                if (isAnalyzing) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(color = AccentGold)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("اتصال به هسته قضایی Gemini و تحلیل متن...", color = AccentGold, style = Typography.bodyMedium)
                                    }
                                } else {
                                    Button(
                                        onClick = { citizenViewModel.runAutomaticLegalAnalysis() },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("تحلیل خودکار با هوش مصنوعی (Gemini)", style = Typography.titleMedium, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            4 -> {
                                Text(
                                    text = "گام چهارم: طرفین شناسایی شده و مستندات اثباتی پیشنهادی",
                                    style = Typography.titleLarge,
                                    color = onBgColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, glassBorderColor, RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = surfaceColor)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("تحلیل هویتی و حقوقی پرونده", style = Typography.titleMedium, color = AccentGold)
                                        Divider(color = glassBorderColor)

                                        IdentityRow("خواهان / شاکی اصلی", plaintiffName) { citizenViewModel.updatePlaintiff(it) }
                                        IdentityRow("خوانده / متکی‌عنه (طرف مقابل)", defendantName) { citizenViewModel.updateDefendant(it) }
                                        IdentityRow("ذینفع پرونده استخراج شده", beneficiaryName) { citizenViewModel.updateBeneficiary(it) }
                                        IdentityRow("عنوان جرم / اتهام حقوقی", legalPosition) { citizenViewModel.updateLegalPosition(it) }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = "%$confidenceScore از ۱۰۰", color = SoftEmerald, style = Typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text(text = "ضریب اطمینان قوت حقوقی پرونده:", color = onSurfaceColor)
                                        }
                                        LinearProgressIndicator(
                                            progress = confidenceScore / 100f,
                                            color = SoftEmerald,
                                            trackColor = if (isDark) SlateNavyMedium else Color(0x1F000000),
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(surfaceColor, RoundedCornerShape(16.dp))
                                        .border(1.dp, glassBorderColor, RoundedCornerShape(16.dp))
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "دلایل و شواهد پیشنهادی برای برنده شدن در دادکاه:",
                                        style = Typography.titleMedium,
                                        color = AccentGold,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Right
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (suggestedEvidence.isEmpty()) {
                                        Text("موردی بارگذاری نشده است.", color = onSurfaceColor, style = Typography.labelMedium)
                                    } else {
                                        suggestedEvidence.forEachIndexed { i, ev ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = ev, style = Typography.bodyMedium, color = onBgColor, textAlign = TextAlign.Right, modifier = Modifier.weight(1f))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Box(modifier = Modifier.size(8.dp).background(AccentGold, CircleShape))
                                            }
                                        }
                                    }
                                }
                            }

                            5 -> {
                                Text(
                                    text = "گام پنجم: خواسته یا نتیجه مورد نظر خود را بنویسید (relief)",
                                    style = Typography.titleLarge,
                                    color = onBgColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )

                                Text(
                                    text = "خواسته‌ها یا نتایجی را که مایلید قاضی بر اساس آن طرف مقابل را محکوم کند وارد نمایید. مثلاً: بازپس‌گیری اصل پول به انضمام کلیه خسارت‌های قانونی تادیه، هزینه واخواست و دادرسی.",
                                    style = Typography.bodyMedium,
                                    color = onSurfaceColor,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = requestedRelief,
                                    onValueChange = { citizenViewModel.updateRequestedRelief(it) },
                                    placeholder = {
                                        Text(
                                            text = "خواسته‌های مادی یا معنوی خود را در اینجا بنویسید...",
                                            color = onSurfaceColor,
                                            fontSize = 14.sp
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp)
                                        .testTag("requested_relief_input"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = onBgColor,
                                        unfocusedTextColor = onBgColor,
                                        focusedBorderColor = AccentGold,
                                        unfocusedBorderColor = glassBorderColor
                                    )
                                )
                            }

                            6 -> {
                                Text(
                                    text = "گام ششم: پردازش AI و تولید لایحه به روش همزمان با ۵ مدل",
                                    style = Typography.titleLarge,
                                    color = onBgColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )

                                Text(
                                    text = "در این گام، سیستم دادرس متن درخواست را بر اساس ۵ نوع موتور مشهور هوش مصنوعی (GPT - Claude - Gemini - DeepSeek - Qwen) پردازش می‌کند و نهایتاً بهترین لایحه یکپارچه متوازن را ارائه می‌دهد. این عمل حدوداً ۶ تا ۱۰ ثانیه زمان می‌برد.",
                                    style = Typography.bodyMedium,
                                    color = onSurfaceColor,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                if (isGenerating) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        CircularProgressIndicator(color = AccentGold)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("ارتباط با وب‌سرویس‌های هوش مصنوعی و نگارش اسناد...", color = AccentGold, style = Typography.bodyMedium)
                                    }
                                } else {
                                    Button(
                                        onClick = { citizenViewModel.generateAIOutputs(session?.username ?: "شهروند") },
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                        modifier = Modifier.fillMaxWidth().height(52.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("تولید تمام اسناد با هوش مصنوعی ترکیبی", style = Typography.titleMedium, color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            7 -> {
                                Text(
                                    text = "گام هفتم: مقایسه و بررسی تخصصی لایحه‌های تولیدی",
                                    style = Typography.titleLarge,
                                    color = onBgColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )

                                // Horizontal output tabs including Qwen
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(
                                        Triple("deepseek", "DeepSeek", SoftCrimson),
                                        Triple("qwen", "Qwen", AccentGold),
                                        Triple("gpt", "GPT-4o", Color.Cyan),
                                        Triple("claude", "Claude 3.5", AccentGold),
                                        Triple("gemini", "Gemini 3.5", Color.Magenta),
                                        Triple("unified", "یکپارچه", SoftEmerald)
                                    ).forEach { tabInfo ->
                                        val isSelected = activeOutputTab == tabInfo.first
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (isSelected) tabInfo.third.copy(alpha = 0.20f) else surfaceColor, RoundedCornerShape(8.dp))
                                                .border(1.dp, if (isSelected) tabInfo.third else glassBorderColor, RoundedCornerShape(8.dp))
                                                .clickable { activeOutputTab = tabInfo.first }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = tabInfo.second, style = Typography.labelSmall, color = if (isSelected) tabInfo.third else onBgColor, fontWeight = FontWeight.Bold, fontSize = 7.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val activeText = when (activeOutputTab) {
                                    "gpt" -> gptOutput
                                    "claude" -> claudeOutput
                                    "gemini" -> geminiOutput
                                    "deepseek" -> deepSeekOutput
                                    "qwen" -> qwenOutput
                                    else -> unifiedOutput
                                }

                                OutlinedTextField(
                                    value = activeText,
                                    onValueChange = { citizenViewModel.updateUnifiedOutput(it) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = onBgColor,
                                        unfocusedTextColor = onBgColor,
                                        focusedBorderColor = AccentGold,
                                        unfocusedBorderColor = glassBorderColor
                                    ),
                                    textStyle = TextStyle(fontSize = 13.sp, lineHeight = 20.sp, textDirection = androidx.compose.ui.text.style.TextDirection.Rtl)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Switches and Interactive simulators
                                Card(
                                    modifier = Modifier.fillMaxWidth().border(1.dp, glassBorderColor, RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = surfaceColor)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Switch(
                                                checked = isPlainExplanation,
                                                onCheckedChange = { isPlainExplanation = it },
                                                colors = SwitchDefaults.colors(checkedThumbColor = AccentGold)
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "تبدیل لایحه به زبان خیلی ساده (نسخه شهروندی)",
                                                    style = Typography.titleMedium,
                                                    color = AccentGold,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Icon(Icons.Default.Info, contentDescription = null, tint = AccentGold)
                                            }
                                        }

                                        if (isPlainExplanation) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(if (isDark) Color(0xE6050B18) else Color(0x0F000000), RoundedCornerShape(12.dp))
                                                    .padding(12.dp),
                                                horizontalAlignment = Alignment.End,
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text(
                                                    text = "بررسی به زبان ساده جهت آگاهی از حقوق قانونی شما:",
                                                    style = Typography.titleSmall,
                                                    color = SoftEmerald,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Right
                                                )
                                                Divider(color = glassBorderColor)
                                                Text(
                                                    text = "۱. خواسته اصلی شما: مطالبه خسارت و تعهدات نقدی بر اساس شرح ارسالی دعوی بر مبنای تراکنش‌های پیوستی.",
                                                    style = Typography.bodyMedium,
                                                    color = onBgColor,
                                                    textAlign = TextAlign.Right
                                                )
                                                Text(
                                                    text = "۲. مبنای قدرت پرونده: قانون مدنی ایران (ماده ۱۰ و ۲۱۹) صریحاً توافقات شفاهی و پیامکی شما را معتبر و الزام‌آور می‌شناسد، حتی اگر سند چاپی محضری در میان نباشد.",
                                                    style = Typography.bodyMedium,
                                                    color = onBgColor,
                                                    textAlign = TextAlign.Right
                                                )
                                                Text(
                                                    text = "۳. قدمهای حضوری دادگاه: پرونده ابتدا به شورای حل اختلاف یا دادسرا ارجاع شده و قاضی برای خوانده صادر ابلاغیه می‌کند. در صورت عدم حضور، دادگاه غیابی تشکیل می‌شود.",
                                                    style = Typography.bodyMedium,
                                                    color = onBgColor,
                                                    textAlign = TextAlign.Right
                                                )
                                                Text(
                                                    text = "۴. میزان احتمال موفقیت شما در دادسرا: بالا (حدود ${confidenceScore}٪) به شرط اینکه فیش‌های تراکنش بانکی را حتماً ممهور به مهر شعبه بانک کرده و ضمیمه نمایید.",
                                                    style = Typography.bodySmall,
                                                    color = AccentGold,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Right
                                                )
                                            }
                                        } else {
                                            Text(
                                                text = "اکنون در حال مشاهده لایحه تفصیلی و رسمی قوه قضاییه (مخصوص ارایه به قاضی) هستید. جهت درک عمومی، سوئیچ بالا را فعال کنید.",
                                                style = Typography.bodySmall,
                                                color = onSurfaceColor,
                                                textAlign = TextAlign.Right
                                            )
                                        }
                                    }
                                }

                                // Interactive debate mode nested inside step 7
                                Card(
                                    modifier = Modifier.fillMaxWidth().border(1.dp, glassBorderColor, RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = surfaceColor)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { showDebateMode = !showDebateMode }) {
                                                Icon(
                                                    imageVector = if (showDebateMode) Icons.Default.Close else Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = AccentGold
                                                )
                                            }
                                            Text(
                                                text = "شبیه‌سازی مناظره حقوقی سه جانبه (Debate Mode)",
                                                style = Typography.titleMedium,
                                                color = AccentGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (showDebateMode) {
                                            Text(
                                                text = "یک شبیه‌سازی واقعی از جلسه دفاع حضوری دادسرا بین وکلای طرفین و قاضی ناظر:",
                                                style = Typography.bodySmall,
                                                color = onSurfaceColor,
                                                textAlign = TextAlign.Right
                                            )

                                            Button(
                                                onClick = {
                                                    isDebating = true
                                                    debateLog = emptyList()
                                                    coroutineScope.launch {
                                                        kotlinx.coroutines.delay(1000)
                                                        debateLog = debateLog + ("وکیل شاکی (خواهان)" to "ریاست محترم دادگاه، موکل بنده تمام اسناد بانکی ممهور را ارایه داده. پرداخت فیش‌ها دال بر قرارداد لازم‌الاجراست.")
                                                        kotlinx.coroutines.delay(1800)
                                                        debateLog = debateLog + ("وکیل مشتکی‌عنه (خوانده)" to "اعتراض دارم! ارایه تراکنش نقدی به تنهایی اثبات‌کننده تعهد کتبی قرارداد نیست و ممکن است موکل واریزی بابت بدهی دیگری انجام داده باشد.")
                                                        kotlinx.coroutines.delay(1800)
                                                        debateLog = debateLog + ("تحلیل مستند قاضی ناظر" to "صدور رای: مستند به اصل عدم تبرع در حقوق ایران و عدم ارایه مدرکی از سوی خوانده بر طلب پیشین، پرداخت وجه گویای تعهد فی‌مابین است. اعتراض خوانده رد می‌گردد.")
                                                        isDebating = false
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = SlateNavyMedium),
                                                modifier = Modifier.fillMaxWidth(),
                                                enabled = !isDebating
                                            ) {
                                                Text(
                                                    text = if (isDebating) "در حال مباحثه وکلا..." else "اجرا و آغاز مناظره صوتی و حقوقی",
                                                    color = AccentGold
                                                )
                                            }

                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(if (isDark) Color(0xE6050B18) else Color(0x0F000000), RoundedCornerShape(12.dp))
                                                    .padding(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (debateLog.isEmpty()) {
                                                    Text(
                                                        text = "منتظر فرمان دادرسی... دکمه آغاز را فشار دهید تا استدلال‌های وکیل شاکی، وکیل متهم و قاضی را زنده بازخوانی کنید.",
                                                        color = onSurfaceColor,
                                                        style = Typography.bodySmall,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                                                    )
                                                } else {
                                                    debateLog.forEach { (role, statement) ->
                                                        val color = when {
                                                            role.contains("شاکی") -> SoftEmerald
                                                            role.contains("مشتکی") -> SoftCrimson
                                                            else -> AccentGold
                                                        }
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(color.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                                .padding(8.dp),
                                                            horizontalAlignment = Alignment.End
                                                        ) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(text = role, style = Typography.labelMedium, color = color, fontWeight = FontWeight.Bold)
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
                                                            }
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(text = statement, style = Typography.bodySmall, color = onBgColor, textAlign = TextAlign.Right)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Litigation Simulator Pathway inside Step 7
                                Card(
                                    modifier = Modifier.fillMaxWidth().border(1.dp, glassBorderColor, RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = surfaceColor)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(onClick = { showLitigationSimulator = !showLitigationSimulator }) {
                                                Icon(
                                                    imageVector = if (showLitigationSimulator) Icons.Default.Close else Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = AccentGold
                                                )
                                            }
                                            Text(
                                                text = "پیش‌بینی مسیر دادرسی پرونده (Litigation Simulator)",
                                                style = Typography.titleMedium,
                                                color = AccentGold,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        if (showLitigationSimulator) {
                                            Text(
                                                text = "گاه‌شمار پیش‌بینی شده مراحل شکایت بر مبنای فرآیند واقعی قضایی ایران:",
                                                style = Typography.bodySmall,
                                                color = onSurfaceColor,
                                                textAlign = TextAlign.Right
                                            )

                                            Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                val timelineSteps = listOf(
                                                    Triple("۱. ثبت دادخواست حقوقی", "ثبت در دفاتر خدمات قضایی الکترونیک و پرداخت هزینه دادرسی پرونده.", "ریسک: رد دفتر اسناد در صورت اشتباه ثبتی"),
                                                    Triple("۲. ابلاغ الکترونیک هوشمند", "ارسال پیامک و احضارنامه رسمی به حساب کاربری خوانده جهت رویت.", "ریسک: عدم رویت ابلاغ الکترونیک و لزوم نشر آگهی (افزایش زمان)"),
                                                    Triple("۳. جلسه شورای حل اختلاف", "تلاش مراجع جهت جلب سازش و ارجاع پرونده به مراجع تخصصی حقوقی.", "ریسک: عدم توافق طرفین و ارجاع به محکمه"),
                                                    Triple("۴. جلسه دادرسی بدوی", "برگزاری حضور فیزیکی طرفین در شعبه محکمه عمومی و شنیدن پاسخ‌ها.", "ریسک: ادعای جعل سند یا درخواست مهلت خوانده")
                                                )

                                                timelineSteps.forEach { (title, desc, risk) ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .background(if (isDark) Color(0xE6050B18) else Color(0x0F000000), RoundedCornerShape(10.dp))
                                                            .padding(10.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                                                            horizontalAlignment = Alignment.End
                                                        ) {
                                                            Text(text = title, style = Typography.bodyMedium, color = AccentGold, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right)
                                                            Text(text = desc, style = Typography.bodySmall, color = onBgColor, textAlign = TextAlign.Right)
                                                            Text(text = risk, style = Typography.labelSmall, color = SoftCrimson, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right)
                                                        }
                                                        Box(
                                                            modifier = Modifier
                                                                .size(24.dp)
                                                                .background(AccentGold.copy(alpha = 0.2f), CircleShape)
                                                                .border(1.dp, AccentGold, CircleShape),
                                                            contentAlignment = Alignment.Center
                                                        ) {
                                                            Box(modifier = Modifier.size(8.dp).background(AccentGold, CircleShape))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            8 -> {
                                // GĀM 8: Final Official Document Export + Download Setup + Local Database Save
                                Text(
                                    text = "گام هشتم: بارگیری رسمی و ثبت نهایی در مراجع دادرسی مستقل",
                                    style = Typography.titleLarge,
                                    color = onBgColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )

                                Text(
                                    text = "پرونده لایحه قضایی شما آماده نهایی‌سازی است. پس از نام‌گذاری دلخواه، فرمت مدنظر خود را برای استفاده آفلاین دانلود نموده و پرونده را در کارتابل ملی ثبت کنید تا همواره به آن دسترسی داشته باشید.",
                                    style = Typography.bodyMedium,
                                    color = onSurfaceColor,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(surfaceColor, RoundedCornerShape(16.dp))
                                        .border(1.dp, glassBorderColor, RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "بارگیری رسمی سند حقوقی فوق (فرمت‌های استاندارد دادگاه):",
                                        style = Typography.titleMedium,
                                        color = AccentGold,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Right
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { operationResultMsg = "بارگیری سند با فرمت PDF نهایی با موفقیت در دستگاه انجام شد." },
                                            colors = ButtonDefaults.buttonColors(containerColor = SlateNavyMedium),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("دانلود PDF", style = Typography.labelMedium, color = AccentGold)
                                        }

                                        Button(
                                            onClick = { operationResultMsg = "سند متنی Word (DOCX) با موفقیت در پوشه مراجع شما ذخیره شد." },
                                            colors = ButtonDefaults.buttonColors(containerColor = SlateNavyMedium),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("DOCX Word", style = Typography.labelMedium, color = AccentGold)
                                        }

                                        Button(
                                            onClick = { operationResultMsg = "سند TXT ساده ثبت شد و آماده ارسال پیامکی گردید." },
                                            colors = ButtonDefaults.buttonColors(containerColor = SlateNavyMedium),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("خروجی TXT", style = Typography.labelMedium, color = AccentGold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Case Naming field
                                OutlinedTextField(
                                    value = finalCaseTitle,
                                    onValueChange = { finalCaseTitle = it },
                                    placeholder = { Text("مثال: لایحه شکایت تصرف عدوانی ملکی نهایی", color = onSurfaceColor) },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("نام مستعار برای ذخیره پرونده در کارتابل ملی", color = AccentGold) },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = onBgColor,
                                        unfocusedTextColor = onBgColor,
                                        focusedBorderColor = AccentGold,
                                        unfocusedBorderColor = glassBorderColor
                                    )
                                )

                                if (operationResultMsg != null) {
                                    Text(
                                        text = operationResultMsg ?: "",
                                        color = SoftEmerald,
                                        style = Typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        textAlign = TextAlign.Right
                                    )
                                }

                                Button(
                                    onClick = {
                                        val finalTitleToSave = if (finalCaseTitle.isNotBlank()) finalCaseTitle else "لایحه تولیدی_${requestType}"
                                        citizenViewModel.saveGeneratedCaseToDb(
                                            title = finalTitleToSave,
                                            userName = session?.username ?: "شهروند",
                                            onFinished = onNavigateBack
                                        )
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("save_case_db_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("ثبت نهایی و انتقال به کارتابل ملی من", style = Typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Navigation Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep < 8) {
                            Button(
                                onClick = { citizenViewModel.changeStep(currentStep + 1) },
                                enabled = when (currentStep) {
                                    1 -> requestType.isNotBlank()
                                    2 -> caseDescription.isNotBlank()
                                    5 -> requestedRelief.isNotBlank()
                                    else -> true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                modifier = Modifier.weight(1.2f).height(48.dp)
                            ) {
                                Text(
                                    text = if (currentStep == 3) "تحلیل و ادامه" else "گام بعدی",
                                    style = Typography.bodyLarge,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1.2f))
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        if (currentStep > 1) {
                            Button(
                                onClick = { citizenViewModel.changeStep(currentStep - 1) },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateNavyMedium),
                                modifier = Modifier.weight(0.8f).height(48.dp)
                            ) {
                                Text("گام قبلی", style = Typography.bodyLarge, color = AccentGold)
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(0.8f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IdentityRow(label: String, value: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.height(50.dp).weight(1.5f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                focusedBorderColor = AccentGold,
                unfocusedBorderColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0x33000000) else GlassBorderDark
            ),
            textStyle = TextStyle(fontSize = 12.sp, textDirection = androidx.compose.ui.text.style.TextDirection.Rtl)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$label:", style = Typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1.2f), textAlign = TextAlign.Right)
    }
}
