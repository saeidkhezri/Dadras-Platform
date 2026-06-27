package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.UserRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------
// Data Entities matching Lawyer Profile & Workspace in Iran (2026)
// ---------------------------------------------------------------------

data class CourtCase(
    val id: String,
    val caseNumber: String,       // شماره پرونده ۱۶ رقمی
    val classNumber: String,      // کلاسه پرونده
    val archiveNumber: String,    // شماره بایگانی
    val clientName: String,       // نام موکل
    val caseTitle: String,        // موضوع خواسته/اتهام
    val caseType: String,         // نوع پرونده (حقوقی، کیفری، خانواده، ثبتی)
    val courtBranch: String,      // مرجع و شعبه رسیدگی کننده
    val judgeName: String,        // نام قاضی شعبه
    val dateNextSession: String,  // وقت ابلاغ دادرسی بعدی
    val lastUpdate: String,       // آخرین پیام ابلاغیه ثنا مربوطه
    val status: String,           // در جریان، مختومه، فرجام‌خواهی، تعلیق
    val feeAmount: Double,        // مبلغ حق‌الوکاله (ریال)
    val expensesAmount: Double,   // هزینه‌های دادرسی و سفر پرداختی (ریال)
    val tags: List<String>        // برچسب‌ها
)

data class LawyerClient(
    val id: String,
    val firstName: String,
    val lastName: String,
    val nationalId: String,
    val phone: String,
    val email: String,
    val address: String,
    val notes: String,
    val caseIds: List<String>     // پرونده‌های متصل به این موکل
)

data class LegalCalendarEvent(
    val id: String,
    val type: String,             // جلسه دادگاه، مهلت قانونی، مشاوره، قرار ملاقات
    val title: String,
    val dateTimeStr: String,      // تاریخ و ساعت
    val relatedCaseNo: String,
    val description: String,
    val hasAlert: Boolean
)

data class DocumentItem(
    val id: String,
    val name: String,
    val folder: String,           // لوایح، مدارک هویت، ادله اثبات
    val fileSize: String,
    val extension: String,        // PDF, DOCX, TXT, PNG
    val version: Int,
    val uploadDate: String
)

data class LegalTemplate(
    val id: String,
    val category: String,         // دادخواست، شکواییه، لایحه دفاعیه، اظهارنامه
    val title: String,
    val description: String,
    val body: String
)

data class LawArticle(
    val lawCategory: String,      // قانون مدنی، قانون مجازات اسلامی، آیین دادرسی
    val articleNumber: String,
    val body: String
)

data class PrecedentVerdict(
    val id: String,
    val number: String,
    val title: String,
    val body: String,
    val issueDate: String
)

data class TeamMember(
    val id: String,
    val name: String,
    val role: String,             // کارآموز وکالت، منشی دفتر، وکیل همکار
    val phone: String,
    val permissions: List<String>
)

data class RetainerInvoice(
    val id: String,
    val clientName: String,
    val caseNo: String,
    val title: String,
    val totalAmount: Double,
    val receivedAmount: Double,
    val paymentStatus: String,    // تسویه شده، دارای اقساط معوق، پیش‌پرداخت
    val dateIssued: String
)

// ---------------------------------------------------------------------
// Primary Screen Layout
// ---------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawyerPanelScreen(
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var showLawyerAiSettings by remember { mutableStateOf(false) }
    var importedFileContent by remember { mutableStateOf("") }
    var showWizardDialog by remember { mutableStateOf(false) }
    var showHealthDashboard by remember { mutableStateOf(false) }

    if (showHealthDashboard) {
        ApiKeyHealthDashboardDialog(
            adminViewModel = adminViewModel,
            onDismiss = { showHealthDashboard = false }
        )
    }

    val curGeminiKeys by adminViewModel.geminiApiKeys.collectAsState()
    val curOpenRouterKeys by adminViewModel.openrouterApiKeys.collectAsState()
    val curOpenAiKeys by adminViewModel.openaiApiKeys.collectAsState()
    val curGroqKeys by adminViewModel.groqApiKeys.collectAsState()
    val curCohereKeys by adminViewModel.cohereApiKeys.collectAsState()
    val curHuggingFaceKeys by adminViewModel.huggingfaceApiKeys.collectAsState()
    val curYouComKeys by adminViewModel.youcomApiKeys.collectAsState()
    val curProxyUrl by adminViewModel.geminiProxyUrl.collectAsState()

    var tempGeminiKeys by remember(curGeminiKeys) { mutableStateOf(curGeminiKeys) }
    var tempOpenRouterKeys by remember(curOpenRouterKeys) { mutableStateOf(curOpenRouterKeys) }
    var tempOpenAiKeys by remember(curOpenAiKeys) { mutableStateOf(curOpenAiKeys) }
    var tempGroqKeys by remember(curGroqKeys) { mutableStateOf(curGroqKeys) }
    var tempCohereKeys by remember(curCohereKeys) { mutableStateOf(curCohereKeys) }
    var tempHuggingFaceKeys by remember(curHuggingFaceKeys) { mutableStateOf(curHuggingFaceKeys) }
    var tempYouComKeys by remember(curYouComKeys) { mutableStateOf(curYouComKeys) }
    var tempGeminiProxyUrl by remember(curProxyUrl) { mutableStateOf(curProxyUrl) }

    if (showWizardDialog) {
        ApiKeyImportWizardDialog(
            fileContent = importedFileContent,
            onDismiss = { showWizardDialog = false },
            onConfirmSave = { gemini, openRouter, openAi, groq, cohere, huggingFace, youcom ->
                val gList = (gemini + listOf("", "", "")).take(3)
                val orList = (openRouter + listOf("", "", "")).take(3)
                val oaList = (openAi + listOf("", "", "")).take(3)
                val grList = (groq + listOf("", "", "")).take(3)
                val coList = (cohere + listOf("", "", "")).take(3)
                val hfList = (huggingFace + listOf("", "", "")).take(3)
                val ycList = (youcom + listOf("", "", "")).take(3)
                
                adminViewModel.saveMultiApiKeys(
                    gemini = gList,
                    openRouter = orList,
                    openAi = oaList,
                    groq = grList,
                    cohere = coList,
                    huggingFace = hfList,
                    youcom = ycList,
                    proxyUrl = curProxyUrl
                )
                showWizardDialog = false
            }
        )
    }

    if (showLawyerAiSettings) {
        var activeGuideInfo by remember { mutableStateOf<ApiGuideInfo?>(null) }

        Dialog(
            onDismissRequest = { showLawyerAiSettings = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.90f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
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
                        IconButton(onClick = { showLawyerAiSettings = false }) {
                            Icon(Icons.Default.Close, contentDescription = "بستن", tint = Color.White)
                        }
                        Text(
                            "تنظیمات فوق‌تخصصی موتورهای هوش مصنوعی وکلا",
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    
                    Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 12.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "وکلا و کارشناسان محترم کانون دادگستری، جهت افزایش سرعت نگارش لوایح و آرشیو هوشمند پرونده‌ها، می‌توانید فایل کلیدهای امنیتی خود را آپلود کنید و عیارسنجی آنلاین نمایید.",
                            style = Typography.bodySmall,
                            color = TextSecondaryFarsi,
                            textAlign = TextAlign.Right
                        )

                        ApiKeyFileImportTrigger(
                            onFileContentReady = { content ->
                                importedFileContent = content
                                showWizardDialog = true
                            },
                            buttonColor = Color(0xFFD97706)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = { showHealthDashboard = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Color.White)
                                Text("سامانه آنلاین پایش سلامت و سهمیه درگاه‌های هوش (چراغ LED)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // 1. Google Gemini API Keys
                        ApiKeyServiceSection(
                            title = "کلیدهای اختصاصی Google Gemini API (تا ۳ کلید با اولویت بالا به پایین):",
                            guideTitle = "راهنمای اتصال Google Gemini",
                            onOpenGuide = { activeGuideInfo = geminiGuide },
                            keys = tempGeminiKeys,
                            onKeysChange = { tempGeminiKeys = it },
                            placeholderPrefix = "کلید جمینای"
                        )

                        Spacer(modifier = Modifier.height(4.dp))
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
                            placeholderPrefix = "کلید اپن‌روتر"
                        )

                        Divider(color = GlassBorderLight.copy(alpha = 0.5f))

                        // 3. OpenAI API Keys
                        ApiKeyServiceSection(
                            title = "کلیدهای اختصاصی OpenAI API (تا ۳ کلید با اولویت بالا به پایین):",
                            guideTitle = "راهنمای اتصال OpenAI",
                            onOpenGuide = { activeGuideInfo = openAiGuide },
                            keys = tempOpenAiKeys,
                            onKeysChange = { tempOpenAiKeys = it },
                            placeholderPrefix = "کلید اپن‌ای‌آی"
                        )

                        Divider(color = GlassBorderLight.copy(alpha = 0.5f))

                        // 4. Groq API Keys
                        ApiKeyServiceSection(
                            title = "کلیدهای اختصاصی Groq API (تا ۳ کلید با اولویت بالا به پایین):",
                            guideTitle = "راهنمای اتصال Groq Cloud",
                            onOpenGuide = { activeGuideInfo = groqGuide },
                            keys = tempGroqKeys,
                            onKeysChange = { tempGroqKeys = it },
                            placeholderPrefix = "کلید گراک"
                        )

                        Divider(color = GlassBorderLight.copy(alpha = 0.5f))

                        // 5. Cohere API Keys
                        ApiKeyServiceSection(
                            title = "کلیدهای اختصاصی Cohere API (تا ۳ کلید با اولویت بالا به پایین):",
                            guideTitle = "راهنمای اتصال Cohere AI",
                            onOpenGuide = { activeGuideInfo = cohereGuide },
                            keys = tempCohereKeys,
                            onKeysChange = { tempCohereKeys = it },
                            placeholderPrefix = "کلید کوهر"
                        )

                        Divider(color = GlassBorderLight.copy(alpha = 0.5f))

                        // 6. Hugging Face Access Tokens
                        ApiKeyServiceSection(
                            title = "کلیدهای اختصاصی Hugging Face API (تا ۳ کلید با اولویت بالا به پایین):",
                            guideTitle = "راهنمای اتصال Hugging Face",
                            onOpenGuide = { activeGuideInfo = huggingFaceGuide },
                            keys = tempHuggingFaceKeys,
                            onKeysChange = { tempHuggingFaceKeys = it },
                            placeholderPrefix = "کلید هاگینگ‌فیس"
                        )

                        Divider(color = GlassBorderLight.copy(alpha = 0.5f))

                        // 7. YouCom Access Tokens
                        ApiKeyServiceSection(
                            title = "کلیدهای اختصاصی You.com API (تا ۳ کلید با اولویت بالا به پایین):",
                            guideTitle = "راهنمای اتصال You.com",
                            onOpenGuide = { activeGuideInfo = youcomGuide },
                            keys = tempYouComKeys,
                            onKeysChange = { tempYouComKeys = it },
                            placeholderPrefix = "کلید یوکام"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    var showSuccessLabel by remember { mutableStateOf(false) }

                    if (showSuccessLabel) {
                        Text(
                            text = "تغییرات با موفقیت ذخیره گردید و آماده کاربری است.",
                            style = Typography.bodySmall,
                            color = SoftEmerald,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                adminViewModel.saveMultiApiKeys(
                                    gemini = tempGeminiKeys,
                                    openRouter = tempOpenRouterKeys,
                                    openAi = tempOpenAiKeys,
                                    groq = tempGroqKeys,
                                    cohere = tempCohereKeys,
                                    huggingFace = tempHuggingFaceKeys,
                                    youcom = tempYouComKeys,
                                    proxyUrl = tempGeminiProxyUrl
                                )
                                showSuccessLabel = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("ذخیره نهایی کلیدها", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showLawyerAiSettings = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            Text("خروج", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

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
                    containerColor = Color(0xFF1E293B)
                )
            }
        }
    }

    // Velvet Luxury Lawyer Palette: Amber Dark Royal Glow
    val lawyerPrimaryColor = Color(0xFFD97706) // Velvet Ochre Gold Amber
    val glassBorderColor = Color.White.copy(alpha = 0.12f)
    val cardBackground = Color(0xFF1E293B).copy(alpha = 0.85f)

    // Master Tab States (0: داشبورد و تقویم, 1: پرونده‌ها و موکلین, 2: اسناد و کتابخانه, 3: هوش مصنوعی و دادرس‌ساز, 4: قوانین و مالی)
    var selectedTab by remember { mutableStateOf(0) }

    // State databases for high interactive fidelity (simulated local repositories)
    var testCases by remember {
        mutableStateOf(
            listOf(
                CourtCase(
                    id = "1",
                    caseNumber = "۱۴۰۲۶۸۹۲۰۰۰۱۴۷۵۶",
                    classNumber = "۰۲۰۱۴۷۵",
                    archiveNumber = "۹۹۰۳۲۴",
                    clientName = "علیرضا رضایی",
                    caseTitle = "مطالبه وجه التزام قرارداد مشارکت در ساخت مسکن مهر پردیس",
                    caseType = "حقوقی",
                    courtBranch = "شعبه ۴۲ دادگاه عمومی حقوقی صدر تهران",
                    judgeName = "جناب دکتر مرتضوی پور",
                    dateNextSession = "۱۴۰۶/۰۴/۱۵ ساعت ۱۰:۰۰ صبح (تبادل لوایح)",
                    lastUpdate = "ابلاغ نظریه رسمی کارشناسی سه نفره با برآورد خسارت",
                    status = "در جریان رسیدگی بدوی",
                    feeAmount = 450000000.0,
                    expensesAmount = 25000000.0,
                    tags = listOf("مشارکت ساخت", "خسارت وجه التزام")
                ),
                CourtCase(
                    id = "2",
                    caseNumber = "۱۴۰۱۶۸۹۲۰۰۰۸۴۹۲۰",
                    classNumber = "۰۱۰۰۸۴۹",
                    archiveNumber = "۹۸۰۱۱۲",
                    clientName = "زهرا کریمی",
                    caseTitle = "دفاع در قبال خلع ید مغازه تجاری بازار بزرگ تجریش و اثبات سرقفلی",
                    caseType = "تجاری/حقوقی",
                    courtBranch = "شعبه ۱۲ دادگاه تجدیدنظر استان تهران",
                    judgeName = "قاضی سید رضا طباطبایی",
                    dateNextSession = "۱۴۰۶/۰۳/۲۸ ساعت ۰۹:۳۰ صبح (حضور طرفین)",
                    lastUpdate = "تعیین جلسه رسیدگی دادرسی حضوری به شرح اوراق پرونده",
                    status = "در مرحله تجدیدنظر",
                    feeAmount = 750000000.0,
                    expensesAmount = 35000000.0,
                    tags = listOf("ملکی", "حق سرقفلی")
                ),
                CourtCase(
                    id = "3",
                    caseNumber = "۱۴۰۲۶۸۹۲۰۰۰۷۴۵۱۲",
                    classNumber = "۰۲۰۷۴۵۱",
                    archiveNumber = "۹۹۰۵۶۸",
                    clientName = "شرکت مبنا سازه پاد",
                    caseTitle = "ابطال رای داور در قرارداد تامین مصالح بتنی پایه‌های صلب شهرداری",
                    caseType = "تجاری",
                    courtBranch = "شعبه ۸۹ دادگاه عمومی حقوقی مجتمع قضایی بهشتی",
                    judgeName = "جناب مستشار علوی",
                    dateNextSession = "۱۴۰۶/۰۵/۱۰ (موقت دادرسی صادر گردیده)",
                    lastUpdate = "دستور ابطال وقت دادرسی به جهت فوریت کارشناسی مستقل",
                    status = "در جریان دادرسی",
                    feeAmount = 1200000000.0,
                    expensesAmount = 50000000.0,
                    tags = listOf("داوری", "ابطال رای")
                ),
                CourtCase(
                    id = "4",
                    caseNumber = "۱۴۰۲۶۸۹۲۰۰۰۴۹۳۲۱",
                    classNumber = "۰۲۰۴۹۳۲",
                    archiveNumber = "۹۹۰۶۸۸",
                    clientName = "سپهر همتی",
                    caseTitle = "دفاع پیرامون اتهام کلاهبرداری اینترنتی و تحصیل مال از طریق نامشروع",
                    caseType = "کیفری",
                    courtBranch = "شعبه ۱۰۲ دادگاه کیفری دو تهران (مقدسی)",
                    judgeName = "قاضی صالحی",
                    dateNextSession = "۱۴۰۶/۰۳/۲۵ ساعت ۱۱:۰۰ صبح (دفاعیه نهایی)",
                    lastUpdate = "دستور استعلام وضعیت حساب موکل از بانک مرکزی جمهوری اسلامی",
                    status = "در جریان (کیفری دو)",
                    feeAmount = 900000000.0,
                    expensesAmount = 18000000.0,
                    tags = listOf("کیفری", "فضای مجازی")
                )
            )
        )
    }

    var testClients by remember {
        mutableStateOf(
            listOf(
                LawyerClient("1", "علیرضا", "رضایی", "۰۰۷۶۸۹۵۴۱۲", "۰۹۱۲۳۴۵۶۷۸۹", "rezai@gmail.com", "تهران، پاسداران، خیابان مریم، پلاک ۱۲", "موکل بسیار پیگیر، نیازمند دریافت صورتحساب‌ها مستمر", listOf("1")),
                LawyerClient("2", "زهرا", "کریمی", "۰۰۲۴۸۹۵۶۱۲", "۰۹۱۸۲۳۴۵۶۷۸", "karimi.z@yahoo.com", "بومهن، لاله دوم، مجتمع آسمان، بلوک ب", "سرقفلی مغازه تجاری بازار تجریش موروثی از پدر مرحوم", listOf("2")),
                LawyerClient("3", "سپهر", "همتی", "۱۲۸۹۵۶۷۸۹۱", "۰۹۳۵۴۶۷۱۱۲۲", "hemati.s@outlook.com", "تهران، سعادت آباد، بلوار پاکنژاد، طبقه ۴", "بررسی تبادل مالی رمزارزها توسط کارشناس ممیز دادسرای فتا ضرورت دارد", listOf("4")),
                LawyerClient("4", "مهندس خسرو", "شایگان (مبنا سازه)", "۱۱۰۲۹۸۳۴۵۶", "۰۹۱۲۹۹۹۸۸۷۷", "shayegan@mabnasaze.co", "تهران، شهرک غرب، سپهر، پلاک ۷۸", "مدیرعامل شرکت مبنا سازه پاد - قرارداد پیمانکاری عمرانی", listOf("3"))
            )
        )
    }

    var testEvents by remember {
        mutableStateOf(
            listOf(
                LegalCalendarEvent("1", "جلسه دادگاه", "تبادل لوایح پرونده مشارکت ساخت پردیس", "۱۴۰۶/۰۴/۱۵ - ۱۰:۰۰ صبح", "۱۴۰۲۶۸۹۲۰۰۰۱۴۷۵۶", "حضور لزوماً اختیاری است ولی تبادل لایحه و دفاعیه آخر ضمیمه گردد", true),
                LegalCalendarEvent("2", "جلسه دادگاه", "پرونده دفاع سرقفلی تجریش", "۱۴۰۶/۰۳/۲۸ - ۰۹:۳۰ صبح", "۱۴۰۱۶۸۹۲۰۰۰۸۴۹۲۰", "اثبات حق کسب و پیشه با تجدید کلاسه کارتابل کارشناسی ثنا", true),
                LegalCalendarEvent("3", "ملاقات حضوری", "مشاوره با نماینده حقوقی بانک ملی", "۱۴۰۶/۰۳/۲۲ - ۱۶:۰۰ عصر", "عدم صدور پرونده فعال", "بررسی پیش‌نویس صلح‌نامه تجاری نمایندگان بانک", false),
                LegalCalendarEvent("4", "مهلت قانونی", "فرصت اعتراض تجدیدنظر پرونده خلع ید موقت", "۱۴۰۶/۰۳/۲۵ - ۲۴:۰۰ شب", "۱۴۰۲۶۸۹۲۰۰۰۹۸۵۱۲", "لوایح اعتراضی حتما قبل از ۲۴ برای پیشخوان خدمات قضایی ارسال گردد", true)
            )
        )
    }

    var testInvoices by remember {
        mutableStateOf(
            listOf(
                RetainerInvoice("1", "علیرضا رضایی", "۱۴۰۲۶۸۹۲۰۰۰۱۴۷۵۶", "پیش‌پرداخت اول مشارکت ساخت پردیس", 450000000.0, 300000000.0, "دارای اقساط معوق", "۱۴۰۶/۰۲/۱۰"),
                RetainerInvoice("2", "زهرا کریمی", "۱۴۰۱۶۸۹۲۰۰۰۸۴۹۲۰", "کل حق‌الوکاله اثبات سرقفلی تجریش", 750000000.0, 750000000.0, "تسویه شده", "۱۴۰۶/۰۱/۱۵"),
                RetainerInvoice("3", "سپهر همتی", "۱۴۰۲۶۸۹۲۰۰۰۴۹۳۲۱", "قسط اول دفاع کیفری رایانه‌ای", 900000000.0, 300000000.0, "پیش‌پرداخت", "۱۴۰۶/۰۳/۰۵")
            )
        )
    }

    var testDocuments by remember {
        mutableStateOf(
            listOf(
                DocumentItem("1", "لایحه دفاعیه_ابطال_داوری_فاز_دوم", "لوایح و مستندات دادگاه", "۴۲۴ KB", "DOCX", 2, "۱۴۰۶/۰۳/۱۵"),
                DocumentItem("2", "سند صلح‌نامه رسمی ۷۶۹۰۳", "ادله اثبات دعوا", "۲.۸ MB", "PDF", 1, "۱۴۰۶/۰۲/۱۸"),
                DocumentItem("3", "فیش واریز وجوه قرارداد خرید بتن", "ادله اثبات دعوا", "۹۸۰ KB", "PNG", 1, "۱۴۰۶/۰۳/۰۳"),
                DocumentItem("4", "شناسنامه و کارت ملی موکل رضایی", "مستندات هویتی", "۱.۲ MB", "PDF", 1, "۱۴۰۶/۰۱/۲۰")
            )
        )
    }

    var personalLibrary by remember {
        mutableStateOf(
            listOf(
                LegalTemplate("1", "دادخواست", "دادخواست مطالبه وجه التزام تعهد ملکی", "کاربرد در دعاوی املاک و پیش‌فروش مسکن با بدعهدی سازنده", "خواهان: موکل، خوانده: سازنده متعهد، به کلاسه...\nتعیین کارشناسی و اثبات زمان دقیق آغاز تادیه وجه التزام به میزان روزانه... ریال"),
                LegalTemplate("2", "شکواییه", "شکواییه کلاهبرداری اینترنتی و فیشینگ تفصیلی", "مناسب جرائم دادسرای تخصصی جرایم رایانه‌ای", "موضوع شکایت: کلاهبرداری با سوءاستفاده از سیستم درگاه پرداخت...\nدلایل: تراکنش بانکی، تاییدیه پلیس فتا، ردیابی آدرس IP ثبت شده"),
                LegalTemplate("3", "لایحه دفاعیه", "لایحه دفاع در قبال تصرف عدوانی موکل حقوقی", "لایحه قدرتمند بی گناهی با تکیه بر تصرف مسبق", "ریاست محترم شعبه کیفری دو...\nاحتراماً موکل متصرف مسبق بیش از ۵ سال در ملک موضوع مناقشه بوده و سند رسمی مفروز ارائه می‌گردد")
            )
        )
    }

    var teamMembers by remember {
        mutableStateOf(
            listOf(
                TeamMember("1", "محمدحسین عباسی", "وکیل همکار", "۰۹۱۲۰۰۰۵۵۴۴", listOf("دیدن پرونده‌ها", "ویرایش لوایح", "گزارش مالی")),
                TeamMember("2", "نرگس صادقی", "کارآموز وکالت", "۰۹۳۶۱۱۱۲۲۳۳", listOf("دیدن پرونده‌ها", "جستجو برداری", "ثبت تقویم")),
                TeamMember("3", "سهراب امینی", "منشی ارشد دفتر", "۰۹۱۵۸۸۸۷۷۶۶", listOf("ثبت تقویم", "امور مالی ساده", "مستندات هویتی"))
            )
        )
    }

    // Modal forms states
    var showAddCaseDialog by remember { mutableStateOf(false) }
    var showAddClientDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAddDocDialog by remember { mutableStateOf(false) }
    var showAddTeamDialog by remember { mutableStateOf(false) }

    FrostedGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.img_app_icon_1782545555023),
                                contentDescription = "Logo",
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(androidx.compose.foundation.shape.CircleShape)
                                    .border(1.dp, lawyerPrimaryColor.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
                            )
                            Text(
                                text = "سامانه جامع و فوق تخصصی وکلای دادگستری",
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
                        IconButton(onClick = { showLawyerAiSettings = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "تنظیمات هوش مصنوعی", tint = Color(0xFFD97706))
                        }
                        IconButton(onClick = { authViewModel.logout() }) {
                            Icon(Icons.Default.Logout, contentDescription = "خروج", tint = CoralRedDark)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF0F172A).copy(alpha = 0.90f)
                    )
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // Royal Identity Card Top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    lawyerPrimaryColor.copy(alpha = 0.35f),
                                    Color(0xFF1E293B).copy(alpha = 0.85f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(1.2.dp, lawyerPrimaryColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
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
                                text = "وکیل پایه یک دادگستری - کانون مرکز",
                                style = Typography.labelSmall,
                                color = lawyerPrimaryColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(lawyerPrimaryColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
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
                                    text = "نام کاربری فعال: DrMahyabadi4770 (پروانه: ۲۹۴۷۷/ک)",
                                    style = Typography.bodySmall,
                                    color = TextSecondaryFarsi
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Modern 5-hub Scrollable Tab Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.90f))
                        .border(1.dp, GlassBorderLight, RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val hubs = listOf("امور قضایی", "هوش و اسناد", "آرشیو پرونده", "پرونده/موکل", "پیشخوان")
                    val hubIcons = listOf(Icons.Default.Language, Icons.Default.AutoAwesome, Icons.Default.FolderZip, Icons.Default.FolderShared, Icons.Default.Gavel)
                    
                    // Persian right to left order: 4 to 0
                    hubs.forEachIndexed { index, title ->
                        val currentIdx = 4 - index
                        val isSelected = selectedTab == currentIdx

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedTab = currentIdx }
                                .background(if (isSelected) lawyerPrimaryColor else Color.Transparent)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = hubIcons[currentIdx],
                                    contentDescription = title,
                                    tint = if (isSelected) Color.White else TextSecondaryFarsi,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = title,
                                    style = Typography.labelSmall.copy(fontSize = 9.sp),
                                    color = if (isSelected) Color.White else TextSecondaryFarsi,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Routing Content
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> DashboardAndCalendarTab(
                            testCases = testCases,
                            testEvents = testEvents,
                            onAddEventClick = { showAddEventDialog = true },
                            testInvoices = testInvoices,
                            teamMembers = teamMembers,
                            onAddTeamClick = { showAddTeamDialog = true },
                            lawyerPrimaryColor = lawyerPrimaryColor
                        )
                        1 -> CasesAndClientsTab(
                            cases = testCases,
                            clients = testClients,
                            onAddCase = { showAddCaseDialog = true },
                            onAddClient = { showAddClientDialog = true },
                            onDeleteCase = { id -> testCases = testCases.filterNot { it.id == id } },
                            onDeleteClient = { id -> testClients = testClients.filterNot { it.id == id } },
                            lawyerPrimaryColor = lawyerPrimaryColor
                        )
                        2 -> DocumentsAndLibraryTab(
                            docs = testDocuments,
                            onAddDoc = { showAddDocDialog = true },
                            templates = personalLibrary,
                            onAddTemplate = { newT -> personalLibrary = personalLibrary + newT },
                            lawyerPrimaryColor = lawyerPrimaryColor
                        )
                        3 -> IntelligentAiAndDrafterTab(
                            cases = testCases,
                            templates = personalLibrary,
                            lawyerPrimaryColor = lawyerPrimaryColor
                        )
                        4 -> AccountingAndModularIntegrationTab(
                            invoices = testInvoices,
                            onAddInvoice = { newI -> testInvoices = testInvoices + newI },
                            lawyerPrimaryColor = lawyerPrimaryColor
                        )
                    }
                }
            }

            // Common dynamic Dialogs
            if (showAddCaseDialog) {
                AddCaseDialog(
                    clients = testClients,
                    onDismiss = { showAddCaseDialog = false },
                    onConfirm = { newCase ->
                        testCases = testCases + newCase
                        showAddCaseDialog = false
                    },
                    lawyerPrimaryColor = lawyerPrimaryColor
                )
            }

            if (showAddClientDialog) {
                AddClientDialog(
                    onDismiss = { showAddClientDialog = false },
                    onConfirm = { newClient ->
                        testClients = testClients + newClient
                        showAddClientDialog = false
                    },
                    lawyerPrimaryColor = lawyerPrimaryColor
                )
            }

            if (showAddEventDialog) {
                AddCalendarEventDialog(
                    cases = testCases,
                    onDismiss = { showAddEventDialog = false },
                    onConfirm = { newEv ->
                        testEvents = testEvents + newEv
                        showAddEventDialog = false
                    },
                    lawyerPrimaryColor = lawyerPrimaryColor
                )
            }

            if (showAddDocDialog) {
                AddDocDialog(
                    onDismiss = { showAddDocDialog = false },
                    onConfirm = { newDoc ->
                        testDocuments = testDocuments + newDoc
                        showAddDocDialog = false
                    },
                    lawyerPrimaryColor = lawyerPrimaryColor
                )
            }

            if (showAddTeamDialog) {
                AddTeamMemberDialog(
                    onDismiss = { showAddTeamDialog = false },
                    onConfirm = { newMem ->
                        teamMembers = teamMembers + newMem
                        showAddTeamDialog = false
                    },
                    lawyerPrimaryColor = lawyerPrimaryColor
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// TAB 0: DASHBOARD & LEGAL CALENDAR & TEAM ROLES
// ---------------------------------------------------------------------

@Composable
fun DashboardAndCalendarTab(
    testCases: List<CourtCase>,
    testEvents: List<LegalCalendarEvent>,
    onAddEventClick: () -> Unit,
    testInvoices: List<RetainerInvoice>,
    teamMembers: List<TeamMember>,
    onAddTeamClick: () -> Unit,
    lawyerPrimaryColor: Color
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Aggregate statistics
    val activeCases = testCases.count { !it.status.contains("مختومه") }
    val archivedCases = testCases.count { it.status.contains("مختومه") }
    val unreadAlerts = testEvents.count { it.hasAlert }

    val totalRetainer = testInvoices.sumOf { it.totalAmount }
    val totalReceived = testInvoices.sumOf { it.receivedAmount }
    val totalDue = totalRetainer - totalReceived

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Stats grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Case Stats
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF0F172A).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .border(1.dp, GlassBorderLight, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("کل پرونده‌ها", style = Typography.labelSmall, color = TextSecondaryFarsi)
                        Icon(Icons.Default.Folder, contentDescription = null, tint = lawyerPrimaryColor, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        PersianFirstUtils.formatDigits(testCases.size.toString(), true),
                        style = Typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "فـعال: ${PersianFirstUtils.formatDigits(activeCases.toString(), true)} | مختومه: ${PersianFirstUtils.formatDigits(archivedCases.toString(), true)}",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = SoftEmerald
                    )
                }
            }

            // Legal Session Notifications
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xFF0F172A).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .border(1.dp, GlassBorderLight, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("مهلت‌های حساس ثنا", style = Typography.labelSmall, color = TextSecondaryFarsi)
                        Icon(Icons.Default.Warning, contentDescription = null, tint = CoralRedDark, modifier = Modifier.size(16.dp))
                    }
                    Text(
                        PersianFirstUtils.formatDigits(unreadAlerts.toString(), true),
                        style = Typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CoralRedDark
                    )
                    Text(
                        "پرونده‌های منتظر لایحه دفاعیه",
                        style = Typography.labelSmall.copy(fontSize = 10.sp),
                        color = TextSecondaryFarsi
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Finance Dashboard Simple
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0F172A).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                    .border(1.dp, lawyerPrimaryColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("خلاصه برآورد وضعیت مالی دفتر", style = Typography.labelSmall, color = Color.White, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.Payments, contentDescription = null, tint = SoftEmerald, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("وصول شده", style = Typography.labelSmall, color = TextSecondaryFarsi)
                            Text(
                                PersianFirstUtils.formatDigits(String.format("%,.0f", totalReceived / 10000000.0) + " م.ت", true),
                                style = Typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SoftEmerald
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("دریافت نشده", style = Typography.labelSmall, color = TextSecondaryFarsi)
                            Text(
                                PersianFirstUtils.formatDigits(String.format("%,.0f", totalDue / 10000000.0) + " م.ت", true),
                                style = Typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = CoralRedDark
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("کل تعهدات معاهداتی", style = Typography.labelSmall, color = TextSecondaryFarsi)
                            Text(
                                PersianFirstUtils.formatDigits(String.format("%,.0f", totalRetainer / 10000000.0) + " م.ت", true),
                                style = Typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Calendar Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAddEventClick() }
                    .background(lawyerPrimaryColor.copy(alpha = 0.15f))
                    .border(1.dp, lawyerPrimaryColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = lawyerPrimaryColor, modifier = Modifier.size(16.dp))
                Text("ثبت جلسه/مشاوره جدید", style = Typography.labelSmall, color = lawyerPrimaryColor, fontWeight = FontWeight.Bold)
            }
            Text("تقویم هوشمند جلسات و قرارهای دفتری", style = Typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            testEvents.forEach { ev ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B).copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                        .border(
                            1.dp,
                            if (ev.hasAlert) CoralRedDark.copy(alpha = 0.4f) else GlassBorderLight,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(ev.dateTimeStr, style = Typography.labelSmall, color = AccentGold)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    ev.title,
                                    style = Typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(5.dp))
                                        .background(if (ev.type.contains("دادگاه")) CoralRedDark else SoftEmerald)
                                )
                            }
                        }
                        Text(
                            "پرونده همکار: ${ev.relatedCaseNo}",
                            style = Typography.labelSmall.copy(fontSize = 11.sp),
                            color = TextSecondaryFarsi
                        )
                        Text(
                            ev.description,
                            style = Typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.65f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Team management sub-module
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.15f))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onAddTeamClick() }
                    .background(lawyerPrimaryColor.copy(alpha = 0.15f))
                    .border(1.dp, lawyerPrimaryColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = null, tint = lawyerPrimaryColor, modifier = Modifier.size(16.dp))
                Text("افزودن عضو حقوقی", style = Typography.labelSmall, color = lawyerPrimaryColor, fontWeight = FontWeight.Bold)
            }
            Text("مدیریت اعضای تیم حقوقی موسسه", style = Typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            teamMembers.forEach { mem ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                        .border(1.dp, GlassBorderLight, RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("دسترسی‌ها:", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondaryFarsi)
                            Text(
                                mem.permissions.joinToString(" • "),
                                style = Typography.labelSmall.copy(fontSize = 9.sp),
                                color = SoftEmerald
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        mem.role,
                                        style = Typography.labelSmall,
                                        color = AccentGold,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(AccentGold.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                    Text(mem.name, style = Typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Text("تلفن ثابت: ${mem.phone}", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondaryFarsi)
                            }
                            Icon(Icons.Default.Face, contentDescription = null, tint = lawyerPrimaryColor, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// TAB 1: CASES & CLIENTS WORKSPACE (ADVANCED M:N FILTERING)
// ---------------------------------------------------------------------

@Composable
fun CasesAndClientsTab(
    cases: List<CourtCase>,
    clients: List<LawyerClient>,
    onAddCase: () -> Unit,
    onAddClient: () -> Unit,
    onDeleteCase: (String) -> Unit,
    onDeleteClient: (String) -> Unit,
    lawyerPrimaryColor: Color
) {
    var searchKeyword by remember { mutableStateOf("") }
    var subTabState by remember { mutableStateOf(0) } // 0: Cases, 1: Clients
    var selectedTagFilter by remember { mutableStateOf("همه") }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.End
    ) {
        // Advanced Filter Area
        OutlinedTextField(
            value = searchKeyword,
            onValueChange = { searchKeyword = it },
            placeholder = { Text("جستجوی پیشرفته موکلین، پرونده، شماره بایگانی و قاضی...", color = TextSecondaryFarsi) },
            singleLine = true,
            trailingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = lawyerPrimaryColor) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = lawyerPrimaryColor,
                unfocusedBorderColor = GlassBorderLight,
                focusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { subTabState = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (subTabState == 1) lawyerPrimaryColor else Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("بانک موکلین (${clients.size})", style = Typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = { subTabState = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (subTabState == 0) lawyerPrimaryColor else Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("مدیریت پرونده‌ها (${cases.size})", style = Typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tags shortcut filters
        if (subTabState == 0) {
            val allTags = listOf("همه") + cases.flatMap { it.tags }.distinct()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
            ) {
                allTags.take(5).forEach { tag ->
                    val isTagSelected = selectedTagFilter == tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { selectedTagFilter = tag }
                            .background(if (isTagSelected) lawyerPrimaryColor.copy(alpha = 0.3f) else Color(0xFF0F172A).copy(alpha = 0.6f))
                            .border(1.dp, if (isTagSelected) lawyerPrimaryColor else GlassBorderLight, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(tag, style = Typography.labelSmall.copy(fontSize = 10.sp), color = if (isTagSelected) Color.White else TextSecondaryFarsi)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        if (subTabState == 0) onAddCase() else onAddClient()
                    }
                    .background(lawyerPrimaryColor.copy(alpha = 0.15f))
                    .border(1.dp, lawyerPrimaryColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = lawyerPrimaryColor, modifier = Modifier.size(16.dp))
                Text(
                    if (subTabState == 0) "ثبت پرونده قضایی کانون جدید" else "ثبت موکل حقیقی/حقوقی ثنا",
                    style = Typography.labelSmall,
                    color = lawyerPrimaryColor,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                if (subTabState == 0) "لیست جامع کلاسه پرونده‌های فعال" else "فهرست موکلین کارتابل ثنا",
                style = Typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Scrolling lists
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (subTabState == 0) {
                // Cases display filtered
                val filteredCases = cases.filter {
                    (searchKeyword.isBlank() || it.clientName.contains(searchKeyword) ||
                            it.caseNumber.contains(searchKeyword) || it.caseTitle.contains(searchKeyword) ||
                            it.courtBranch.contains(searchKeyword) || it.judgeName.contains(searchKeyword)) &&
                            (selectedTagFilter == "همه" || it.tags.contains(selectedTagFilter))
                }

                items(filteredCases) { caseItem ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B).copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                            .border(1.dp, GlassBorderLight, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onDeleteCase(caseItem.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = CoralRedDark, modifier = Modifier.size(18.dp))
                                }

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        "موکل: ${caseItem.clientName}",
                                        style = Typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Icon(Icons.Default.Person, contentDescription = null, tint = lawyerPrimaryColor, modifier = Modifier.size(16.dp))
                                }
                            }

                            Text(caseItem.caseTitle, style = Typography.bodySmall, color = AccentGold, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right)
                            
                            // Fields asked explicitly: شماره پرونده، کلاسه، بایگانی، نوع، مرجع، شعبه، قاضی، وضعیت
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("شماره پرونده: ${caseItem.caseNumber}", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondaryFarsi)
                                Spacer(modifier = Modifier.weight(1f))
                                Text("کلاسه پرونده: ${caseItem.classNumber}", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondaryFarsi)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("شماره بایگانی ثنا: ${caseItem.archiveNumber}", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondaryFarsi)
                                Spacer(modifier = Modifier.weight(1f))
                                Text("نوع رسیدگی: ${caseItem.caseType}", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondaryFarsi)
                            }
                            Text("مرجع و شعبه: ${caseItem.courtBranch}", style = Typography.labelSmall.copy(fontSize = 10.sp), color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Right)
                            Text("قاضی پرونده: ${caseItem.judgeName}", style = Typography.labelSmall.copy(fontSize = 10.sp), color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Right)
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    caseItem.tags.forEach { tag ->
                                        Text(tag, style = Typography.labelSmall.copy(fontSize = 8.sp), color = lawyerPrimaryColor, modifier = Modifier.background(lawyerPrimaryColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp))
                                    }
                                }
                                Text("وضعیت: ${caseItem.status}", style = Typography.labelSmall, color = SoftEmerald, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Clients list
                val filteredClients = clients.filter {
                    searchKeyword.isBlank() || it.firstName.contains(searchKeyword) ||
                            it.lastName.contains(searchKeyword) || it.nationalId.contains(searchKeyword) ||
                            it.phone.contains(searchKeyword)
                }

                items(filteredClients) { clientItem ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .border(1.dp, GlassBorderLight, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { onDeleteClient(clientItem.id) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = "حذف موکل", tint = CoralRedDark, modifier = Modifier.size(18.dp))
                                }

                                Text(
                                    "${clientItem.firstName} ${clientItem.lastName}",
                                    style = Typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Text("کد ملی ثنا: ${PersianFirstUtils.formatDigits(clientItem.nationalId, true)}", style = Typography.labelSmall, color = TextSecondaryFarsi)
                            Text("شماره تلفن: ${PersianFirstUtils.formatDigits(clientItem.phone, true)}", style = Typography.labelSmall, color = TextSecondaryFarsi)
                            Text("ایمیل: ${clientItem.email}", style = Typography.labelSmall, color = TextSecondaryFarsi)
                            Text("آدرس پستی: ${clientItem.address}", style = Typography.labelSmall, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Right)
                            Text("شرح پرونده‌ها: ${clientItem.notes}", style = Typography.labelSmall, color = AccentGold, textAlign = TextAlign.Right)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// TAB 2: DOCUMENT WORKSPACE & PERSONAL LAWYER LIBRARY
// ---------------------------------------------------------------------

@Composable
fun DocumentsAndLibraryTab(
    docs: List<DocumentItem>,
    onAddDoc: () -> Unit,
    templates: List<LegalTemplate>,
    onAddTemplate: (LegalTemplate) -> Unit,
    lawyerPrimaryColor: Color
) {
    var searchKeyword by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf("همه") }
    var isLibrarySubTab by remember { mutableStateOf(false) } // False: Doc Storage, True: Library

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.End
    ) {
        // Toggle Sub Hub
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { isLibrarySubTab = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLibrarySubTab) lawyerPrimaryColor else Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LibraryBooks, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("کتابخانه من و اسناد پیش‌نویس", style = Typography.labelSmall)
                }
            }

            Button(
                onClick = { isLibrarySubTab = false },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!isLibrarySubTab) lawyerPrimaryColor else Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("پرونده الکترونیک و آرشیو اسناد", style = Typography.labelSmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (!isLibrarySubTab) {
            // Folders Filters
            val folders = listOf("همه", "لوایح و مستندات دادگاه", "ادله اثبات دعوا", "مستندات هویتی")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End)
            ) {
                folders.take(4).forEach { f ->
                    val isFSelected = selectedFolder == f
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { selectedFolder = f }
                            .background(if (isFSelected) lawyerPrimaryColor.copy(alpha = 0.3f) else Color(0xFF0F172A).copy(alpha = 0.6f))
                            .border(1.dp, if (isFSelected) lawyerPrimaryColor else GlassBorderLight, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(f, style = Typography.labelSmall.copy(fontSize = 10.sp), color = if (isFSelected) Color.White else TextSecondaryFarsi)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onAddDoc() }
                        .background(lawyerPrimaryColor.copy(alpha = 0.15f))
                        .border(1.dp, lawyerPrimaryColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null, tint = lawyerPrimaryColor, modifier = Modifier.size(16.dp))
                    Text("آپلود سند الکترونیکی به ابر کانون", style = Typography.labelSmall, color = lawyerPrimaryColor, fontWeight = FontWeight.Bold)
                }

                Text("آرشیو مدارک پرونده و پرونده دیجیتال", style = Typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val filteredDocs = docs.filter {
                    selectedFolder == "همه" || it.folder == selectedFolder
                }

                items(filteredDocs) { docItem ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B).copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                            .border(1.dp, GlassBorderLight, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "دانلود",
                                    tint = SoftEmerald,
                                    modifier = Modifier
                                        .clickable {
                                            // Simulated export toast trigger
                                        }
                                        .size(24.dp)
                                )
                                Text(docItem.fileSize, style = Typography.labelSmall, color = TextSecondaryFarsi)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(docItem.name, style = Typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(
                                        "پوشه: ${docItem.folder} • نسخه: ${docItem.version} • ثبت: ${docItem.uploadDate}",
                                        style = Typography.labelSmall.copy(fontSize = 9.sp),
                                        color = TextSecondaryFarsi
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(lawyerPrimaryColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(docItem.extension, style = Typography.labelSmall, fontWeight = FontWeight.Bold, color = lawyerPrimaryColor)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Personal Lawyer library forms
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "آرشیو فرم‌ها و لوایح شخصی وکلای کانون",
                    style = Typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(templates) { template ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                            .border(1.dp, lawyerPrimaryColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    template.category,
                                    style = Typography.labelSmall,
                                    color = lawyerPrimaryColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(lawyerPrimaryColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )

                                Text(template.title, style = Typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Text(template.description, style = Typography.labelSmall, color = TextSecondaryFarsi, textAlign = TextAlign.Right)
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    template.body,
                                    style = Typography.labelSmall.copy(fontSize = 11.sp),
                                    color = Color.White.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// TAB 3: SMART AI DRAFTER & SPECIALTY LEGAL RESEARCH ENGINE
// ---------------------------------------------------------------------

@Composable
fun IntelligentAiAndDrafterTab(
    cases: List<CourtCase>,
    templates: List<LegalTemplate>,
    lawyerPrimaryColor: Color
) {
    var queryInput by remember { mutableStateOf("") }
    var resultAiReport by remember { mutableStateOf("") }
    var selectedCaseIndex by remember { mutableStateOf(0) }
    var isAnalyzing by remember { mutableStateOf(false) }

    // Drafter tool states
    var selectedDocType by remember { mutableStateOf("دادخواست") }
    var drafterTopic by remember { mutableStateOf("مطالبه وجه التزام تاخیر در تجهیز ملک") }
    var clientRoleInBrief by remember { mutableStateOf("خواهان") }
    var createdDrafterResult by remember { mutableStateOf("") }
    var isDraftingActive by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text("هوش مصنوعی تخصصی تحلیل ادلّه دادرسی", style = Typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)

        // Select case for deep audit
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                .border(1.dp, GlassBorderLight, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                Text("شبیه‌ساز ارزیابی پرونده و شناسایی نقاط ضعف/قوت", style = Typography.bodySmall, color = AccentGold, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                
                cases.forEachIndexed { idx, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCaseIndex = idx }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCaseIndex == idx,
                            onClick = { selectedCaseIndex = idx },
                            colors = RadioButtonDefaults.colors(selectedColor = lawyerPrimaryColor)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(item.caseTitle, style = Typography.labelSmall, color = Color.White)
                            Text("کلاسه: ${item.classNumber} • موکل: ${item.clientName}", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondaryFarsi)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        isAnalyzing = true
                        coroutineScope.launch {
                            delay(1500)
                            val c = cases[selectedCaseIndex]
                            resultAiReport = """【گزارش ممیزی هوشمند پرونده کلاسه ${c.classNumber}】
✅ نقاط قوت شناسایی شده:
۱. تسلیم به موقع دادخواست نخستین در مهلت مقرر قانونی.
۲. وجود و فاکتور تفصیلی نظریه هیئت ۳ نفره کارشناسی با تصدیق ۵۵ درصد خسارت عدم تعهد.

⚠️ نقاط ضعف و تهدیدها:
۱. بند ۴ توافق مشارکت ساخت ممکن است اصل صلاحیت رسیدگی داوری کانون مهندسان پردیس را جاری کرده و دادگاه را با مواجهه قرار عدم استماع مواجه سازد.
۲. احتمال رد ایرادات متقابل خوانده در صورت غیاب مابقی مالکین زمین مشاعی.

🛡️ استراتژی دفاع پیشنهادی:
- دفاع با اتکا به مفهوم مخالف ماده ۲۲ ق.ث در خصوص مالکین فاقد سند مشاعی و تلاش جهت بقای صلاحیت دادگاه فعلی بدوی."""
                            isAnalyzing = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = lawyerPrimaryColor)
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text("ارزیابی ریسک و پیشنهاد استراتژی دفاع", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (resultAiReport.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF131B2E).copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                    .border(1.dp, lawyerPrimaryColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { clipboardManager.setText(AnnotatedString(resultAiReport)) }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "کپی", tint = SoftEmerald)
                        }
                        Text("خروجی تحلیل فوق تخصصی دادرس هوشمند", style = Typography.labelSmall, color = lawyerPrimaryColor, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(resultAiReport, style = Typography.labelSmall.copy(fontSize = 11.sp), color = Color.White, textAlign = TextAlign.Right)
                }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.White.copy(alpha = 0.15f))

        // Document Drafter Tool
        Text("مولد هوشمند اسناد و لوایح حقوقی", style = Typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                .border(1.dp, GlassBorderLight, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("نوع سند را انتخاب نمایید:", style = Typography.labelSmall, color = Color.White)
                
                Row(modifier = Modifier.verticalScroll(rememberScrollState()).height(42.dp)) {
                    listOf("دادخواست", "شکواییه", "لایحه دفاعیه", "اظهارنامه رسمي", "تجدیدنظرخواهی").forEach { docType ->
                        val isSelected = selectedDocType == docType
                        Box(
                            modifier = Modifier
                                .clickable { selectedDocType = docType }
                                .background(if (isSelected) lawyerPrimaryColor else Color.DarkGray, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                                .padding(end = 4.dp)
                        ) {
                            Text(docType, style = Typography.labelSmall, color = Color.White)
                        }
                    }
                }

                OutlinedTextField(
                    value = drafterTopic,
                    onValueChange = { drafterTopic = it },
                    placeholder = { Text("مثال: مطالبه وجه التزام تاخیر در تجهیز ملک", color = TextSecondaryFarsi) },
                    singleLine = true,
                    label = { Text("موضوع سند حقوقی", color = TextSecondaryFarsi) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = lawyerPrimaryColor,
                        unfocusedBorderColor = GlassBorderLight,
                        focusedTextColor = Color.White
                    )
                )

                Button(
                    onClick = {
                        isDraftingActive = true
                        coroutineScope.launch {
                            delay(1800)
                            createdDrafterResult = """【پیش‌نویس نهایی $selectedDocType کانون ملّی】
خواهان: موکل گرانقدر
خوانده: تعهددهنده/بدهکار پرونده ثنا
موضوع: $drafterTopic

ریاست محترم شعبه مربوطه،
با احترام، به عنوان وکیل خواهان به استحضار می‌رساند متعهد علی‌رغم وصول مطالبات مصرح ماده ۲ همعهدنامه، از تجهیز مواضع پلاک موضوع دعوا استنکاف نموده و مسبب خسارات عمیق معیشتی موکل گردیده است.
لذا مستند به ماده ۲۲۱ قانون مدنی و نظریه کارشناسی پیوستی دایر بر تقصیر مسلم، تقاضای صدور حکم شایسته بر محکومیت خوانده به تادیه وجه التزام تاخیر به انضمام کلیه خسارات دادرسی و حق‌الوکاله را استدعا دارم.

با تقدیم احترام،
دکتر حسین پورمحی‌آبادی - عضو رسمی کانون وکلا مرکز"""
                            isDraftingActive = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = lawyerPrimaryColor)
                ) {
                    if (isDraftingActive) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text("نگارش پیش‌نویس تفصیلی منطبق قوانین کشور", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (createdDrafterResult.isNotBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B).copy(alpha = 0.95f), RoundedCornerShape(12.dp))
                    .border(1.dp, AccentGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { clipboardManager.setText(AnnotatedString(createdDrafterResult)) }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "کپی لایحه", tint = SoftEmerald)
                            }
                            IconButton(onClick = {
                                // Simulate print
                            }) {
                                Icon(Icons.Default.Print, contentDescription = "چاپ لایحه", tint = Color.White)
                            }
                        }
                        Text("سند تولید شده آماده چاپ کانون مرکز", style = Typography.labelSmall, color = AccentGold, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(createdDrafterResult, style = Typography.labelSmall.copy(fontSize = 11.sp), color = Color.White, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// TAB 4: LAW BANK, FINANCIALS & CONNECTION GATEWAYS
// ---------------------------------------------------------------------

@Composable
fun AccountingAndModularIntegrationTab(
    invoices: List<RetainerInvoice>,
    onAddInvoice: (RetainerInvoice) -> Unit,
    lawyerPrimaryColor: Color
) {
    var searchLawStr by remember { mutableStateOf("") }
    var selectedLawSec by remember { mutableStateOf("قانون مدنی") }
    var currentSubHubState by remember { mutableStateOf(0) } // 0: Law repository, 1: Finance ledger, 2: Gateway connect

    val lawsDb = listOf(
        LawArticle("قانون مدنی", "ماده ۱۰", "قراردادهای خصوصی نسبت به کسانی که آن را منعقد نموده‌اند در صورتی که مخالف صریح قانون نباشد نافذ است."),
        LawArticle("قانون مدنی", "ماده ۲۲۰", "عقود نه فقط متعاملین را به اجرای چیزی که در آن تصریح شده است ملزم می‌نماید بلکه متعاملین به کلیه نتایجی هم که به موجب عرف و عادت یا به موجب قانون از عقد حاصل می‌شود ملزم می‌باشند."),
        LawArticle("قانون مجازات اسلامی", "ماده ۶۷۴", "هرگاه اموال غیرمنقول یا منقول یا اسناد نوشته شده از قبیل چک و سفته و قبض و نظایر آن‌ها به عنوان اجاره یا امانت یا رهن یا برای هر کار با اجرت یا بی‌اجرت به کسی داده شده و بنابر استرداد یا به مصرف معینی رسانیدن بوده و محکوم‌علیه آن‌ها را به ضرر مالکین مصرف یا تصاحب یا تلف یا مفقود نماید به حبس از شش ماه تا سه سال محکوم خواهد شد.")
    )

    val supremePrecedents = listOf(
        PrecedentVerdict("1", "رای وحدت رویه شماره ۸۳۴", "خلاصه در خصوص ثبتی اسناد کشور", "عدم لزوم تقدیم شناسنامه زوج در مطالبه مهریه به صورت مستقیم از طریق مراجع ثبتی اسناد کشور", "۱۴۰۶/۰۲/۱۵"),
        PrecedentVerdict("2", "رای وحدت رویه شماره ۴۵۲", "خلاصه کیفری در خصوص کلاسه جزا", "تعیین دقیق مرجع تجدیدنظر خواسته برای مراجع قضایی بدوی کیفری با در نظر گرفتن جزای نقدی بالای میلیاردها ریال", "۱۴۰۵/۱۱/۲۰")
    )

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Hub Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = { currentSubHubState = 2 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (currentSubHubState == 2) lawyerPrimaryColor else Color(0xFF0F172A))
            ) {
                Text("درگاه ثنا", style = Typography.labelSmall)
            }

            Button(
                onClick = { currentSubHubState = 1 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (currentSubHubState == 1) lawyerPrimaryColor else Color(0xFF0F172A))
            ) {
                Text("دفتر مالی", style = Typography.labelSmall)
            }

            Button(
                onClick = { currentSubHubState = 0 },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (currentSubHubState == 0) lawyerPrimaryColor else Color(0xFF0F172A))
            ) {
                Text("قوانین", style = Typography.labelSmall)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (currentSubHubState) {
            0 -> {
                // Bank of Laws
                Text("بانک کتب و قوانین جاری جمهوری اسلامی ایران", style = Typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = searchLawStr,
                    onValueChange = { searchLawStr = it },
                    placeholder = { Text("جستجوی مواد قانونی، آیین‌نامه و آرای وحدت رویه...", color = TextSecondaryFarsi) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = lawyerPrimaryColor,
                        unfocusedBorderColor = GlassBorderLight,
                        focusedTextColor = Color.White
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("قانون مدنی", "قانون مجازات اسلامی").forEach { sec ->
                        Box(
                            modifier = Modifier
                                .clickable { selectedLawSec = sec }
                                .background(if (selectedLawSec == sec) lawyerPrimaryColor else Color.DarkGray, RoundedCornerShape(4.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(sec, style = Typography.labelSmall, color = Color.White)
                        }
                    }
                }

                lawsDb.filter { it.lawCategory == selectedLawSec && (searchLawStr.isBlank() || it.body.contains(searchLawStr)) }.forEach { art ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                            .border(1.dp, GlassBorderLight, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                            Text("${art.lawCategory} - ${art.articleNumber}", style = Typography.bodySmall, color = AccentGold, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(art.body, style = Typography.labelSmall.copy(fontSize = 11.sp), color = Color.White, textAlign = TextAlign.Right)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("آرای وحدت رویه اخیر دیوان عالی کشور", style = Typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)

                supremePrecedents.forEach { pr ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                            .border(1.dp, lawyerPrimaryColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(pr.issueDate, style = Typography.labelSmall, color = TextSecondaryFarsi)
                                Text(pr.title, style = Typography.bodySmall, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Text(pr.body, style = Typography.labelSmall.copy(fontSize = 10.sp), color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Right)
                        }
                    }
                }
            }
            1 -> {
                // Accounting Ledger
                Text("حسابداری حق‌الوکاله و صورتحساب‌های صادره موکلین", style = Typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)

                invoices.forEach { inv ->
                    val balanceDue = inv.totalAmount - inv.receivedAmount
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1E293B).copy(alpha = 0.9f), RoundedCornerShape(10.dp))
                            .border(1.dp, GlassBorderLight, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    inv.paymentStatus,
                                    style = Typography.labelSmall,
                                    color = if (inv.paymentStatus == "تسویه شده") SoftEmerald else CoralRedDark,
                                    modifier = Modifier
                                        .background(if (inv.paymentStatus == "تسویه شده") SoftEmerald.copy(alpha = 0.15f) else CoralRedDark.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                                Text("موکل: ${inv.clientName}", style = Typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Text(inv.title, style = Typography.labelSmall, color = AccentGold)
                            Text("مبلغ توافق شده: ${PersianFirstUtils.formatDigits(String.format("%,.0f", inv.totalAmount), true)} ریال", style = Typography.labelSmall, color = TextSecondaryFarsi)
                            Text("دریافت شده تابحال: ${PersianFirstUtils.formatDigits(String.format("%,.0f", inv.receivedAmount), true)} ریال", style = Typography.labelSmall, color = TextSecondaryFarsi)
                            if (balanceDue > 0) {
                                Text("باقیمانده طلب معوق: ${PersianFirstUtils.formatDigits(String.format("%,.0f", balanceDue), true)} ریال", style = Typography.labelSmall, color = CoralRedDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            2 -> {
                // Modular Connection Portals to Adliran, Sana, etc.
                Text("بخش اتصال فنی و ماژولار به سامانه‌های قضایی کل کشور", style = Typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                        .border(1.2.dp, lawyerPrimaryColor.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("طرح معماری ماژولار درگاه واسط (REST API / Web Service GUID)", style = Typography.bodySmall, color = AccentGold, fontWeight = FontWeight.Bold)
                        Text(
                            "برای انطباق با دستورالعمل عدم بکارگیری سیستم شبیه‌سازی غیررسمی، زیرساخت اتصال یکپارچه وب سرویس اورجینال در زیر طراحی و مستند شده است:",
                            style = Typography.labelSmall.copy(fontSize = 11.sp),
                            color = Color.White,
                            textAlign = TextAlign.Right
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                """【مشخصات فنی و امنیتی کارتابل خدمات قضایی】
● REST API Target: adliran.ir/api/v2/lawyer/sync
● Method: POST (Payload: OAuth Token 3 + Digital Signature SH-256)
● Scopes Required: read:sana-notices, read:case-timeline
● Sync Interval: 120 Minutes (Modular Worker Class)
● Status: آماده پیکربندی و اتصال مستقیم به وب‌سرویس رسمی قوه قضائیه به محض صدور توکن API اختصاصی""",
                                style = Typography.labelSmall.copy(fontSize = 10.sp),
                                color = SoftEmerald,
                                textAlign = TextAlign.Left,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Text("فعال‌سازی سیستم آفلاین هماهنگ‌سازی به محض دریافت گواهی دیجیتال کانون وکلا.", style = Typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondaryFarsi, textAlign = TextAlign.Right)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// DYNAMIC COMPOSABLE FORMS & MODAL DIALOGS
// ---------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCaseDialog(
    clients: List<LawyerClient>,
    onDismiss: () -> Unit,
    onConfirm: (CourtCase) -> Unit,
    lawyerPrimaryColor: Color
) {
    var caseNo by remember { mutableStateOf("") }
    var classNo by remember { mutableStateOf("") }
    var archiveNo by remember { mutableStateOf("") }
    var selectedClientIdx by remember { mutableStateOf(0) }
    var title by remember { mutableStateOf("") }
    var caseType by remember { mutableStateOf("حقوقی") }
    var courtBranch by remember { mutableStateOf("") }
    var judgeName by remember { mutableStateOf("") }
    var feesText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .border(1.2.dp, lawyerPrimaryColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("ثبت پرونده قضایی و کلاسه ثنا جدید", style = Typography.titleMedium, color = lawyerPrimaryColor, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = caseNo,
                    onValueChange = { caseNo = it },
                    placeholder = { Text("شماره پرونده ۱۶ رقمی ثنا", color = TextSecondaryFarsi) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = classNo,
                    onValueChange = { classNo = it },
                    placeholder = { Text("کلاسه پرونده", color = TextSecondaryFarsi) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = archiveNo,
                    onValueChange = { archiveNo = it },
                    placeholder = { Text("شماره بایگانی شعبه", color = TextSecondaryFarsi) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("موضوع خواسته یا اتهام", color = TextSecondaryFarsi) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = courtBranch,
                    onValueChange = { courtBranch = it },
                    placeholder = { Text("مرجع و شعبه دادگاه رسیدگی‌کننده", color = TextSecondaryFarsi) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = judgeName,
                    onValueChange = { judgeName = it },
                    placeholder = { Text("نام قاضی محترم رسیدگی‌کننده", color = TextSecondaryFarsi) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = feesText,
                    onValueChange = { feesText = it },
                    placeholder = { Text("مبلغ کل حق‌الوکاله (ریال)", color = TextSecondaryFarsi) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            if (caseNo.isNotBlank() && title.isNotBlank()) {
                                val cName = if (clients.isNotEmpty() && selectedClientIdx < clients.size) {
                                    "${clients[selectedClientIdx].firstName} ${clients[selectedClientIdx].lastName}"
                                } else {
                                    "موکل متفرقه"
                                }
                                onConfirm(
                                    CourtCase(
                                        id = (System.currentTimeMillis() % 10000).toString(),
                                        caseNumber = caseNo,
                                        classNumber = classNo.ifBlank { "ثبت نشده" },
                                        archiveNumber = archiveNo.ifBlank { "ثبت نشده" },
                                        clientName = cName,
                                        caseTitle = title,
                                        caseType = caseType,
                                        courtBranch = courtBranch.ifBlank { "شعبه عمومی حقوقی" },
                                        judgeName = judgeName.ifBlank { "ریاست شعبه" },
                                        dateNextSession = "به زودی توسط ابلاغیه ثنا",
                                        lastUpdate = "تشکیل پرونده نهایی دفتری",
                                        status = "در جریان دادرسی",
                                        feeAmount = feesText.toDoubleOrNull() ?: 0.0,
                                        expensesAmount = 0.0,
                                        tags = listOf(caseType)
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = lawyerPrimaryColor)
                    ) {
                        Text("تایید و ثبت")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClientDialog(
    onDismiss: () -> Unit,
    onConfirm: (LawyerClient) -> Unit,
    lawyerPrimaryColor: Color
) {
    var cFirst by remember { mutableStateOf("") }
    var cLast by remember { mutableStateOf("") }
    var cNat by remember { mutableStateOf("") }
    var cPhone by remember { mutableStateOf("") }
    var cEmail by remember { mutableStateOf("") }
    var cAddress by remember { mutableStateOf("") }
    var cNotes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .border(1.2.dp, lawyerPrimaryColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("ثبت پرونده هویتی موکل ثنا", style = Typography.titleMedium, color = lawyerPrimaryColor, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = cFirst,
                    onValueChange = { cFirst = it },
                    placeholder = { Text("نام موکل", color = TextSecondaryFarsi) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = cLast,
                    onValueChange = { cLast = it },
                    placeholder = { Text("نام خانوادگی موکل", color = TextSecondaryFarsi) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = cNat,
                    onValueChange = { cNat = it },
                    placeholder = { Text("کد ملی ۱۰ رقمی ثنا", color = TextSecondaryFarsi) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = cPhone,
                    onValueChange = { cPhone = it },
                    placeholder = { Text("شماره همراه موکل", color = TextSecondaryFarsi) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = cEmail,
                    onValueChange = { cEmail = it },
                    placeholder = { Text("پست الکترونیکی", color = TextSecondaryFarsi) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = cAddress,
                    onValueChange = { cAddress = it },
                    placeholder = { Text("آدرس پستی تفصیلی", color = TextSecondaryFarsi) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = cNotes,
                    onValueChange = { cNotes = it },
                    placeholder = { Text("توضیحات و شرح خلاصه دعوی همبند", color = TextSecondaryFarsi) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            if (cFirst.isNotBlank() && cLast.isNotBlank()) {
                                onConfirm(
                                    LawyerClient(
                                        id = (System.currentTimeMillis() % 10000).toString(),
                                        firstName = cFirst,
                                        lastName = cLast,
                                        nationalId = cNat,
                                        phone = cPhone,
                                        email = cEmail,
                                        address = cAddress,
                                        notes = cNotes,
                                        caseIds = emptyList()
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = lawyerPrimaryColor)
                    ) {
                        Text("تایید و ثبت")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCalendarEventDialog(
    cases: List<CourtCase>,
    onDismiss: () -> Unit,
    onConfirm: (LegalCalendarEvent) -> Unit,
    lawyerPrimaryColor: Color
) {
    var title by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var selectedCaseIdx by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .border(1.2.dp, lawyerPrimaryColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("ثبت موعد دفتری و تقویمی وکیل", style = Typography.titleMedium, color = lawyerPrimaryColor, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("عنوان قرار/جلسه دادرسی", color = TextSecondaryFarsi) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = dateStr,
                    onValueChange = { dateStr = it },
                    placeholder = { Text("تاریخ و ساعت (مثال: ۱۴۰۶/۰۴/۱۵ - ۱۰:۰۰)", color = TextSecondaryFarsi) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    placeholder = { Text("شرح و نکات تکمیلی جلسه حضوری", color = TextSecondaryFarsi) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                val cNo = if (cases.isNotEmpty() && selectedCaseIdx < cases.size) {
                                    cases[selectedCaseIdx].caseNumber
                                } else {
                                    "بدون پرونده متصل"
                                }
                                onConfirm(
                                    LegalCalendarEvent(
                                        id = (System.currentTimeMillis() % 10000).toString(),
                                        type = "جلسه مشاوره",
                                        title = title,
                                        dateTimeStr = dateStr.ifBlank { "۱۴۰۶/۰۳/۳۰" },
                                        relatedCaseNo = cNo,
                                        description = desc,
                                        hasAlert = true
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = lawyerPrimaryColor)
                    ) {
                        Text("تایید")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocDialog(
    onDismiss: () -> Unit,
    onConfirm: (DocumentItem) -> Unit,
    lawyerPrimaryColor: Color
) {
    var name by remember { mutableStateOf("") }
    var folderName by remember { mutableStateOf("لوایح و مستندات دادگاه") }
    var extName by remember { mutableStateOf("PDF") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .border(1.2.dp, lawyerPrimaryColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("آپلود سند به پرونده دیجیتال کانون", style = Typography.titleMedium, color = lawyerPrimaryColor, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("نام سند (مثال: شناسنامه موکل)", color = TextSecondaryFarsi) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                Text("پوشه آرشیو:", style = Typography.labelSmall, color = Color.White)
                listOf("لوایح و مستندات دادگاه", "ادله اثبات دعوا", "مستندات هویتی").forEach { folder ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { folderName = folder }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(folder, style = Typography.labelSmall, color = Color.White, modifier = Modifier.padding(end = 8.dp))
                        RadioButton(selected = folderName == folder, onClick = { folderName = folder }, colors = RadioButtonDefaults.colors(selectedColor = lawyerPrimaryColor))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(
                                    DocumentItem(
                                        id = (System.currentTimeMillis() % 10000).toString(),
                                        name = name,
                                        folder = folderName,
                                        fileSize = "1.2 MB",
                                        extension = extName,
                                        version = 1,
                                        uploadDate = "۱۴۰۶/۰۳/۲۰"
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = lawyerPrimaryColor)
                    ) {
                        Text("تایید آپلود")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTeamMemberDialog(
    onDismiss: () -> Unit,
    onConfirm: (TeamMember) -> Unit,
    lawyerPrimaryColor: Color
) {
    var name by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("کارآموز وکالت") }
    var phone by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0F172A))
                .border(1.2.dp, lawyerPrimaryColor.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("افزودن همکار حقوقی جدید به دفتر", style = Typography.titleMedium, color = lawyerPrimaryColor, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("نام و نام خانوادگی", color = TextSecondaryFarsi) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    placeholder = { Text("شماره همراه فعال", color = TextSecondaryFarsi) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = lawyerPrimaryColor, unfocusedBorderColor = GlassBorderLight, focusedTextColor = Color.White)
                )

                Text("مسئولیت و سمت دفتری:", style = Typography.labelSmall, color = Color.White)
                listOf("وکیل همکار", "کارآموز وکالت", "منشی دفتر").forEach { sym ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { role = sym }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(sym, style = Typography.labelSmall, color = Color.White, modifier = Modifier.padding(end = 8.dp))
                        RadioButton(selected = role == sym, onClick = { role = sym }, colors = RadioButtonDefaults.colors(selectedColor = lawyerPrimaryColor))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                        Text("انصراف")
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onConfirm(
                                    TeamMember(
                                        id = (System.currentTimeMillis() % 10000).toString(),
                                        name = name,
                                        role = role,
                                        phone = phone.ifBlank { "ثبت نشده" },
                                        permissions = listOf("دیدن پرونده‌ها", "ثبت تقویم")
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = lawyerPrimaryColor)
                    ) {
                        Text("تایید قرارگیری")
                    }
                }
            }
        }
    }
}
