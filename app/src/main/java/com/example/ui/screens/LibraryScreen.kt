package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.example.ui.theme.*
import com.example.ui.components.FrostedGlassBackground
import com.example.ui.components.PersianFirstUtils
import com.example.viewmodel.CitizenViewModel
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// متد نرمال‌سازی قلم و کاراکترهای عربی/فارسی جهت رفع نقص موتور استعلام حقوقی
fun normalizePersianString(input: String): String {
    return input
        .replace('ي', 'ی')
        .replace('ك', 'ک')
        .replace('إ', 'ا')
        .replace('أ', 'ا')
        .replace('آ', 'ا')
        .replace('ة', 'ه')
        .replace("‌", "") // حذف نیم‌فاصله جهت سهولت تطبیق
        .replace("  ", " ")
        .trim()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    citizenViewModel: CitizenViewModel,
    authViewModel: com.example.viewmodel.AuthViewModel, // دریافت وضعیت تنظیمات اعداد فارسی
    onNavigateBack: () -> Unit
) {
    val resources by citizenViewModel.legalResources.collectAsState()
    val isPersianDigits by authViewModel.isPersianNumbersEnabled.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("همه") }

    val categories = listOf("همه", "قانون مدنی", "قانون مجازات", "حقوق خانواده", "قوانین تجاری")

    // وضعیت دستیار صوتی
    var isVoiceOverlayVisible by remember { mutableStateOf(false) }
    var voiceStatusText by remember { mutableStateOf("در حال شنود دادخواهی شما...") }
    var isTranscribingVoice by remember { mutableStateOf(false) }

    // وضعیت اسکن نوری OCR
    var isOcrOverlayVisible by remember { mutableStateOf(false) }
    var selectedOcrTemplateIndex by remember { mutableStateOf(-1) }
    var isOcrProcessing by remember { mutableStateOf(false) }

    // نرمال‌سازی کلمات کلیدی برای تطبیق ۱۰۰ درصد دقیق با بانک داده
    val normalizedSearchQuery = normalizePersianString(searchQuery)
    val filteredResources = resources.filter {
        (selectedCategory == "همه" || it.category == selectedCategory) &&
        (normalizePersianString(it.title).contains(normalizedSearchQuery) ||
         normalizePersianString(it.content).contains(normalizedSearchQuery) ||
         normalizePersianString(it.description).contains(normalizedSearchQuery) ||
         it.articleNo.contains(searchQuery))
    }

    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // قالب‌بندی شیشه‌ای هماهنگ با سیستم عامل ادمین و شهروند
    val surfaceColor = Color(0xDD0C1322)
    val onBgColor = Color.White
    val onSurfaceColor = Color(0xFFBAC5D9)
    val glassBorderColor = Color(0x33B18F54)

    FrostedGlassBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "بایگانی متمرکز و مراجع قوانین مدون",
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
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // نوار جستجوی صوتی، متنی و OCR پیشرفته
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("مثال: ماده ۱۰، تسبیب، یا بارگیری تصویر سند جهت OCR...", color = onSurfaceColor, fontSize = 12.sp) },
                        singleLine = true,
                        leadingIcon = {
                            Row(
                                modifier = Modifier.padding(start = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // دکمه صوتی
                                IconButton(
                                    onClick = {
                                        isVoiceOverlayVisible = true
                                        isTranscribingVoice = false
                                        voiceStatusText = "در حال شنود دادخواهی شما..."
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(AccentGold.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "جستجوی صوتی",
                                        tint = AccentGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // دکمه OCR نوری اسناد
                                IconButton(
                                    onClick = {
                                        isOcrOverlayVisible = true
                                        selectedOcrTemplateIndex = -1
                                        isOcrProcessing = false
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(AccentGold.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "اسکن صوتی اسناد",
                                        tint = AccentGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        trailingIcon = {
                            IconButton(onClick = {}, modifier = Modifier.testTag("submit_search_btn")) {
                                Icon(Icons.Default.Search, contentDescription = "جستجو", tint = AccentGold)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("library_search_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = onBgColor,
                            unfocusedTextColor = onBgColor,
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = glassBorderColor
                        )
                    )

                    // دکمه‌های گزینش دسته‌بندی مکتوب قوانین با قلم بومی
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = selectedCategory == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AccentGold.copy(alpha = 0.25f) else surfaceColor)
                                    .border(1.dp, if (isSelected) AccentGold else glassBorderColor, RoundedCornerShape(8.dp))
                                    .clickable { selectedCategory = cat }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat,
                                    style = Typography.labelSmall,
                                    color = if (isSelected) AccentGold else onBgColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }

                    // ردیف کلمات کلیدی مفید و نتایج
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Text("پاکسازی فیلتر", color = SoftCrimson, style = Typography.labelSmall)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        val countText = "یافت شده: ${filteredResources.size} بند قانونی موضوعی"
                        Text(
                            text = PersianFirstUtils.formatDigits(countText, isPersianDigits),
                            style = Typography.bodySmall,
                            color = AccentGold
                        )
                    }

                    // لیست تبلور اسناد مستخرجه RAG به صورت فارسی RTL
                    if (filteredResources.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = onSurfaceColor, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("هیچ بند قانونی منطبق با اصطلاحات نورمالایز شده یافت نشد.", color = onBgColor, style = Typography.bodyMedium, textAlign = TextAlign.Center)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(filteredResources) { resource ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, glassBorderColor, RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = surfaceColor)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        // هدر کارت
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(AccentGold.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(text = resource.category, color = AccentGold, style = Typography.labelSmall)
                                            }

                                            val articleLabel = "ماده ${resource.articleNo}"
                                            val formattedArticle = PersianFirstUtils.formatDigits(articleLabel, isPersianDigits)
                                            Text(
                                                text = "${resource.title} ($formattedArticle)",
                                                style = Typography.titleMedium,
                                                color = onBgColor,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Right
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        // شرح کوتاه تفکیکی
                                        Text(
                                            text = resource.description,
                                            style = Typography.labelMedium,
                                            color = AccentGold,
                                            fontWeight = FontWeight.Medium,
                                            textAlign = TextAlign.Right
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Divider(color = glassBorderColor)
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // متن کامل قانون موضوعه
                                        val formattedContentDigits = PersianFirstUtils.formatDigits(resource.content, isPersianDigits)
                                        Text(
                                            text = formattedContentDigits,
                                            style = Typography.bodyMedium,
                                            color = onBgColor,
                                            textAlign = TextAlign.Right,
                                            lineHeight = 24.sp
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Divider(color = glassBorderColor)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        // ابزارهای صادرات و انتشار RTL در قالب رسمی
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            listOf("PDF", "DOCX", "TXT").forEach { format ->
                                                Button(
                                                    onClick = {
                                                        PersianFirstUtils.simulateExportFile(
                                                            context = context,
                                                            resourceTitle = "${resource.title} - تبصره ${resource.articleNo}",
                                                            content = resource.content,
                                                            format = format
                                                        )
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold.copy(alpha = 0.1f)),
                                                    modifier = Modifier
                                                        .height(34.dp)
                                                        .border(0.5.dp, AccentGold, RoundedCornerShape(6.dp))
                                                ) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = AccentGold, modifier = Modifier.size(12.dp))
                                                        Text(text = "صادرات $format", style = Typography.labelSmall, color = AccentGold, fontSize = 9.sp)
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

                // ۱. پنل شبیه‌ساز صوتی دستیار
                if (isVoiceOverlayVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xEE050B18))
                            .clickable { isVoiceOverlayVisible = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .border(1.dp, AccentGold, RoundedCornerShape(16.dp))
                                .clickable(enabled = false) {},
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1322))
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(text = "دستیار صوتی و ضبط ادله دادگستری", style = Typography.titleLarge, color = AccentGold, fontWeight = FontWeight.Bold)
                                Divider(color = glassBorderColor)

                                Text(text = voiceStatusText, style = Typography.bodyMedium, color = Color.White, textAlign = TextAlign.Center)

                                if (!isTranscribingVoice) {
                                    // نمایش دایره متحرک وولوم صدا
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(AccentGold.copy(alpha = 0.25f))
                                            .clickable {
                                                isTranscribingVoice = true
                                                voiceStatusText = "در حال شبیه‌سازی تبدیل موج صوتی به متن..."
                                                coroutineScope.launch {
                                                    delay(1500)
                                                    val randomQuery = PersianFirstUtils.mockVoiceQueries.random()
                                                    searchQuery = randomQuery
                                                    isVoiceOverlayVisible = false
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = AccentGold, modifier = Modifier.size(32.dp))
                                    }
                                    Text(text = "برای شبیه‌سازی شروع صحبت روی دکمه فوق ضربه بزنید", style = Typography.labelSmall, color = onSurfaceColor)
                                } else {
                                    CircularProgressIndicator(color = AccentGold)
                                }

                                TextButton(onClick = { isVoiceOverlayVisible = false }) {
                                    Text("بستن دستیار صوتی", color = SoftCrimson, style = Typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                // ۲. پنل شبیه‌ساز OCR نوری اسناد
                if (isOcrOverlayVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xEE050B18))
                            .clickable { isOcrOverlayVisible = false },
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .heightIn(max = 500.dp)
                                .border(1.dp, AccentGold, RoundedCornerShape(16.dp))
                                .clickable(enabled = false) {},
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1322))
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(18.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "پردازشگر اسناد و OCR بومی مستقل",
                                    style = Typography.titleLarge,
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                                Text(
                                    text = "تصویر یا متن دست‌نویس پرونده قضایی را جهت تحلیل و استخراج کلمات در این بخش اسکن کنید:",
                                    style = Typography.bodySmall,
                                    color = onSurfaceColor,
                                    textAlign = TextAlign.Right
                                )

                                Divider(color = glassBorderColor)

                                Text(text = "انتخاب فایل اسکن شده نمونه:", style = Typography.labelMedium, color = AccentGold)
                                
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    PersianFirstUtils.ocrDocTemplates.forEachIndexed { index, template ->
                                        val isSelected = selectedOcrTemplateIndex == index
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isSelected) AccentGold.copy(alpha = 0.2f) else surfaceColor, RoundedCornerShape(10.dp))
                                                .border(1.dp, if (isSelected) AccentGold else glassBorderColor, RoundedCornerShape(10.dp))
                                                .clickable { selectedOcrTemplateIndex = index; isOcrProcessing = false }
                                                .padding(12.dp)
                                        ) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Info,
                                                    contentDescription = null,
                                                    tint = if (isSelected) SoftEmerald else AccentGold
                                                )
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(text = template.docName, style = Typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                                                    Text(text = template.extractedTitle, style = Typography.labelSmall, color = AccentGold)
                                                }
                                            }
                                        }
                                    }
                                }

                                if (selectedOcrTemplateIndex != -1) {
                                    val template = PersianFirstUtils.ocrDocTemplates[selectedOcrTemplateIndex]
                                    
                                    Button(
                                        onClick = {
                                            isOcrProcessing = true
                                            coroutineScope.launch {
                                                delay(1200)
                                                searchQuery = template.extractedContent
                                                isOcrOverlayVisible = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(42.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftEmerald),
                                        enabled = !isOcrProcessing
                                    ) {
                                        Text(text = if (isOcrProcessing) "در حال پردازش OCR نوری..." else "شروع استخراج الگوهای خطی متن اسکن و فیلتر", style = Typography.titleSmall, color = Color.White)
                                    }
                                }

                                TextButton(onClick = { isOcrOverlayVisible = false }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                                    Text("بستن اسکنر OCR", color = SoftCrimson)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
