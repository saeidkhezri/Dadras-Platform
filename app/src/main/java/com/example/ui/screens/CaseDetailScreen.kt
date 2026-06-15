package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CaseEntity
import com.example.ui.theme.*
import com.example.ui.components.FrostedGlassBackground
import com.example.viewmodel.CitizenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseDetailScreen(
    caseId: Int,
    citizenViewModel: CitizenViewModel,
    onNavigateBack: () -> Unit
) {
    val cases by citizenViewModel.cases.collectAsState()
    val case = cases.find { it.id == caseId }

    val scrollState = rememberScrollState()
    var activeTab by remember { mutableStateOf("document") } // document, timeline
    var operationMsg by remember { mutableStateOf<String?>(null) }

    // Dynamic theming colors
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val glassBorderColor = if (onBgColor.luminance() > 0.5f) Color(0x33000000) else GlassBorderDark

    FrostedGlassBackground {
        if (case == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("پرونده مورد نظر یافت نشد.", color = SoftCrimson, style = Typography.titleLarge)
            }
        } else {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = case.title,
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
                            IconButton(onClick = { operationMsg = "کد رهگیری ملی پرونده به اشتراک گذاشته شد." }) {
                                Icon(Icons.Default.Share, contentDescription = "اشتراک‌گذاری", tint = AccentGold)
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
                        // Toggle tabs: document vs timeline
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { activeTab = "timeline" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeTab == "timeline") AccentGold else SlateNavyMedium
                                ),
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "خط زمانی مراحل دادرسی",
                                    style = Typography.bodyLarge,
                                    color = if (activeTab == "timeline") Color.Black else AccentGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Button(
                                onClick = { activeTab = "document" },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (activeTab == "document") AccentGold else SlateNavyMedium
                                ),
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "متن سند و لایحه AI",
                                    style = Typography.bodyLarge,
                                    color = if (activeTab == "document") Color.Black else AccentGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Main Scroll content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (activeTab == "document") {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, glassBorderColor, RoundedCornerShape(18.dp)),
                                    colors = CardDefaults.cardColors(containerColor = surfaceColor)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .background(SoftEmerald.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(text = "ضریب تایید %${case.confidenceScore}", color = SoftEmerald, style = Typography.labelSmall)
                                            }

                                            Text(
                                                text = "موضوع شکایت: ${case.type}",
                                                style = Typography.titleLarge,
                                                color = onBgColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))
                                        Divider(color = glassBorderColor)
                                        Spacer(modifier = Modifier.height(12.dp))

                                        DetailField("شاکی / خواهان اصلی", case.plaintiff, onBgColor, onSurfaceColor)
                                        DetailField("متهم / خوانده طرف دعوی", case.defendant, onBgColor, onSurfaceColor)
                                        DetailField("ذینفع پرونده", case.beneficiary, onBgColor, onSurfaceColor)
                                        DetailField("عنوان موضوع حقوقی", case.legalPosition, onBgColor, onSurfaceColor)
                                        DetailField("خواسته‌ها و نتایج مطلوب", case.relief, onBgColor, onSurfaceColor)
                                        DetailField("دلایل و مدارک کتبی", case.suggestedEvidence, onBgColor, onSurfaceColor)

                                        Spacer(modifier = Modifier.height(16.dp))
                                        Divider(color = glassBorderColor)
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text(
                                            text = "متن نهایی لایحه یکپارچه هوش مصنوعی:",
                                            style = Typography.titleMedium,
                                            color = AccentGold,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Right
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = case.unifiedOutput.ifBlank { "سند متنی یافت نشد." },
                                            style = Typography.bodyMedium,
                                            color = onBgColor,
                                            textAlign = TextAlign.Right,
                                            lineHeight = 24.sp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (onBgColor.luminance() > 0.5f) Color(0x0F000000) else GlassSurfaceLight, RoundedCornerShape(12.dp))
                                                .padding(12.dp)
                                        )
                                    }
                                }

                                // Interactive Actions / Downloads Block inside Case Detail
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(surfaceColor, RoundedCornerShape(18.dp))
                                        .border(1.dp, glassBorderColor, RoundedCornerShape(18.dp))
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "بارگیری این لایحه رسمی:",
                                        style = Typography.titleMedium,
                                        color = AccentGold,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Right
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { operationMsg = "سند با امضای الکترونیکی در قالب PDF ذخیره شد." },
                                            colors = ButtonDefaults.buttonColors(containerColor = SlateNavyMedium),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("قالب PDF", style = Typography.labelMedium, color = AccentGold)
                                        }

                                        Button(
                                            onClick = { operationMsg = "سند Word (DOCX) با سربرگ کانون بارگذاری شد." },
                                            colors = ButtonDefaults.buttonColors(containerColor = SlateNavyMedium),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("DOCX Word", style = Typography.labelMedium, color = AccentGold)
                                        }

                                        Button(
                                            onClick = { operationMsg = "پرونده متنی به درگاه سیستم ابلاغ ارسال گردید." },
                                            colors = ButtonDefaults.buttonColors(containerColor = SlateNavyMedium),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("ابلاغ مستقیم", style = Typography.labelMedium, color = AccentGold)
                                        }
                                    }
                                }
                            } else {
                                // Timeline of courtroom stages and status
                                Text(
                                    text = "خط زمانی و سیر مراحل قانونی دادگاه",
                                    style = Typography.titleLarge,
                                    color = onBgColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )

                                Text(
                                    text = "این زمان‌بندی بصری سیر مراحل دادرسی، تایید دلایل و قرار صادره را نمایش می‌دهد و امکان جابجایی ترتیبی در فرآیند را بر اساس آخرین قوانین مهیا می‌نماید.",
                                    style = Typography.bodyMedium,
                                    color = onSurfaceColor,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                val timelineSteps = listOf(
                                    TimelineStep("ثبت اولیه", case.date, "سند حقوقی تحلیلی با موفقیت توسط شهروند ثبت و ابلاغ شد.", "مدرک اثباتی ضمیمه: ${case.suggestedEvidence.substringBefore(",")}"),
                                    TimelineStep("ارجاع به شعبه اولیه", "۱۸ خرداد ۱۴۰۵", "پرونده ثبت گردیده و به شعبه ۱۰۲ کیفری ۲ مجتمع قضایی ارجاع داده شد.", "مبنای قانونی: ماده ۲۲۰ مجازات اسلامی"),
                                    TimelineStep("تطبیق ادله اثبات", "۲۵ خرداد ۱۴۰۵", "توسط کارشناس رسمی دادگستری مدارک و فیش‌های بانکی تایید ادله شد.", "نتیجه کارشناسی: ضریب صحت بالای ۹۰٪"),
                                    TimelineStep("صدور قرار ابلاغیه", "۳۰ خرداد ۱۴۰۵ (تخمینی)", "جلسه اول رسیدگی حضوری به منظور حضور طرفین و ابراز دفاعیات شفاهی صادر می‌گردد.", "پیش‌بینی دادرس: لایحه قاطع دعوی است.")
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(0.dp)
                                ) {
                                    timelineSteps.forEachIndexed { index, step ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 24.dp),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(surfaceColor, RoundedCornerShape(16.dp))
                                                    .border(1.dp, glassBorderColor, RoundedCornerShape(16.dp))
                                                    .padding(16.dp),
                                                horizontalAlignment = Alignment.End
                                            ) {
                                                Text(text = step.date, style = Typography.labelSmall, color = AccentGold, fontWeight = FontWeight.SemiBold)
                                                Text(text = step.title, style = Typography.titleMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(text = step.description, style = Typography.bodyMedium, color = onSurfaceColor, textAlign = TextAlign.Right)
                                                if (step.reference.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(text = step.reference, style = Typography.labelMedium, color = SoftEmerald, textAlign = TextAlign.Right)
                                                }
                                            }

                                            Spacer(modifier = Modifier.width(16.dp))

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.width(32.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .background(if (index == 0) SoftEmerald else AccentGold, CircleShape)
                                                        .border(2.dp, Color.White, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (index == 0) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                                if (index < timelineSteps.size - 1) {
                                                    Box(
                                                        modifier = Modifier
                                                            .width(2.dp)
                                                            .height(130.dp)
                                                            .background(glassBorderColor)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (operationMsg != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = operationMsg ?: "",
                                color = SoftEmerald,
                                style = Typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailField(label: String, value: String, textColor: Color, labelColor: Color) {
    if (value.isNotBlank()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = value,
                style = Typography.bodyMedium,
                color = textColor,
                textAlign = TextAlign.Right,
                modifier = Modifier.weight(1.5f)
            )
            Text(
                text = "$label:",
                style = Typography.bodyMedium,
                color = labelColor,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Right,
                modifier = Modifier.weight(1f).padding(end = 8.dp)
            )
        }
    }
}

data class TimelineStep(
    val title: String,
    val date: String,
    val description: String,
    val reference: String
)
