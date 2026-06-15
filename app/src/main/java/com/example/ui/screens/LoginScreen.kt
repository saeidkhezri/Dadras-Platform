package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.components.FrostedGlassBackground
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.UserRole

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val session by authViewModel.session.collectAsState()
    val isPasswordChangeRequired by authViewModel.isPasswordChangeRequired.collectAsState()
    val loginError by authViewModel.loginError.collectAsState()
    val isDark by authViewModel.isDarkTheme.collectAsState()

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var showCreditsDialog by remember { mutableStateOf(false) }

    // Dynamic Glass Theme attributes
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onBgColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val primaryAccent = MaterialTheme.colorScheme.primary
    val glassBorderColor = if (isDark) GlassBorderDark else Color(0x33000000)

    // Redirect on success
    LaunchedEffect(session, isPasswordChangeRequired) {
        if (session != null && !isPasswordChangeRequired) {
            onLoginSuccess()
        }
    }

    FrostedGlassBackground {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Theme toggle at the very top of the login screen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { authViewModel.toggleTheme() },
                    modifier = Modifier
                        .testTag("theme_toggle_button")
                        .background(surfaceColor, RoundedCornerShape(12.dp))
                        .border(1.dp, glassBorderColor, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = if (isDark) Icons.Default.Star else Icons.Default.Refresh,
                        contentDescription = "Theme Toggle",
                        tint = AccentGold
                    )
                }

                // Simple placeholder to align
                Box(modifier = Modifier.size(24.dp))
            }

            Column(
                modifier = Modifier
                    .fillOuterPadding()
                    .padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Program Logo (Custom Scales of Justice representation)
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Security Shield",
                    tint = primaryAccent,
                    modifier = Modifier
                        .size(76.dp)
                        .background(surfaceColor, RoundedCornerShape(20.dp))
                        .border(1.dp, glassBorderColor, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "سامانه دادرس هوشمند",
                    style = Typography.displayMedium,
                    color = onBgColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "پلتفرم ملی تحلیل اسناد و دستیار حقوقی با هوش مصنوعی چندمدلی",
                    style = Typography.bodyMedium,
                    color = onSurfaceColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp).padding(horizontal = 12.dp)
                )

                // Glass Card Form
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(surfaceColor)
                        .border(1.dp, glassBorderColor, RoundedCornerShape(24.dp))
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (!isPasswordChangeRequired) {
                            // Standard Entry
                            Text(
                                text = "ورود به حساب کاربری",
                                style = Typography.titleLarge,
                                color = onBgColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.End)
                            )

                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                placeholder = { Text("نام کاربری یا کدملی", color = onSurfaceColor) },
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = primaryAccent) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("username_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = onBgColor,
                                    unfocusedTextColor = onBgColor,
                                    focusedBorderColor = primaryAccent,
                                    unfocusedBorderColor = glassBorderColor
                                )
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                placeholder = { Text("رمز عبور", color = onSurfaceColor) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryAccent) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("password_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = onBgColor,
                                    unfocusedTextColor = onBgColor,
                                    focusedBorderColor = primaryAccent,
                                    unfocusedBorderColor = glassBorderColor
                                )
                            )

                            if (loginError != null) {
                                Text(
                                    text = loginError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = Typography.labelMedium,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp)
                                )
                            }

                            Button(
                                onClick = {
                                    if (username.isNotBlank() && password.isNotBlank()) {
                                        authViewModel.login(username, password)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("login_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = primaryAccent),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "ورود به سامانه",
                                    style = Typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Dynamic Helper
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                                    .background(if (isDark) GlassSurfaceLight else Color(0x0F000000), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "راهنمای دسترسی کاربران:",
                                    color = AccentGold,
                                    style = Typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right
                                )
                                Text(
                                    text = "ورود کاربران در سه نقش مدیر سیستم، کاربر عادی ویژه و کارشناسان حقوقی انجام می‌پذیرد. مشخصات ورود پیش‌فرض کاربران در دفترچه راهنما ثبت شده است.",
                                    color = onSurfaceColor,
                                    style = Typography.labelSmall,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Right,
                                    lineHeight = 16.sp
                                )
                            }

                        } else {
                            // Enforce Password Change for Administrator
                            Text(
                                text = "تغییر رمز عبور اجباری مدیر",
                                style = Typography.titleLarge,
                                color = AccentGold,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.End)
                            )

                            Text(
                                text = "برای حفاظت از اطلاعات کاربری، رمز عبور پیش‌فرض مدیریت را تغییر دهید.",
                                style = Typography.bodyMedium,
                                color = onSurfaceColor,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                placeholder = { Text("رمز عبور جدید (حداقل ۶ کاراکتر)", color = onSurfaceColor) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = primaryAccent) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("new_password_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = onBgColor,
                                    unfocusedTextColor = onBgColor,
                                    focusedBorderColor = primaryAccent,
                                    unfocusedBorderColor = glassBorderColor
                                )
                            )

                            if (loginError != null) {
                                Text(
                                    text = loginError ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = Typography.labelMedium,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Button(
                                onClick = {
                                    authViewModel.completePasswordChange(newPassword)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("change_password_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "ثبت رمز کلیدگذاری و ورود نهایی",
                                    style = Typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Future Ready Architecture Roles View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair("مقام قضایی", "به‌زودی"),
                        Pair("وکیل", "به‌زودی"),
                        Pair("کاربر عادی", "فعال"),
                        Pair("مدیر سیستم", "فعال")
                    ).forEach { roleInfo ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(surfaceColor, RoundedCornerShape(12.dp))
                                .border(1.dp, glassBorderColor, RoundedCornerShape(12.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = roleInfo.first, style = Typography.labelMedium, color = onBgColor, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = roleInfo.second,
                                    style = Typography.labelSmall,
                                    color = if (roleInfo.second == "فعال") MaterialTheme.colorScheme.tertiary else onSurfaceColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Owners & Authors Credits Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCreditsDialog = true }
                        .border(1.dp, glassBorderColor, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = surfaceColor.copy(alpha = 0.85f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "مالکان و توسعه‌دهندگان سامانه",
                            color = AccentGold,
                            style = Typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Divider(color = glassBorderColor.copy(alpha = 0.5f), thickness = 1.dp)
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "محمدسعید خضریپور",
                                color = Color.White,
                                style = Typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "بخش فنی و پیاده‌سازی هوش مصنوعی:",
                                color = onSurfaceColor,
                                style = Typography.labelSmall
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "دکتر حسین پورمحی‌آبادی",
                                color = Color.White,
                                style = Typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "صحت‌سنجی فقهی و انطباق حقوقی:",
                                color = onSurfaceColor,
                                style = Typography.labelSmall
                            )
                        }
                    }
                }

                // Popup dialog explaining detailed roles and technical contributions
                if (showCreditsDialog) {
                    androidx.compose.ui.window.Dialog(
                        onDismissRequest = { showCreditsDialog = false }
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF0C1324), // Solid dark navy background to block background texts completely
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentGold)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                                    .also { println("") } // Empty print for syntactical boundary safety
                            ) {
                                Text(
                                    text = "مالکان و توسعه‌دهندگان سامانه مستقل دادرس",
                                    style = Typography.titleMedium,
                                    color = AccentGold,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Right,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Divider(color = AccentGold.copy(alpha = 0.3f))

                                // Person 1: Khezripour
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "محمدسعید خضری‌پور",
                                        style = Typography.bodyLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right
                                    )
                                    Text(
                                        text = "نقش: بنیان‌گذار مشترک، مدیر بخش فنی و برنامه‌نویسی هوشمند مستقل",
                                        style = Typography.labelSmall,
                                        color = AccentGold,
                                        textAlign = TextAlign.Right
                                    )
                                    Text(
                                        text = "عهده‌دار برنامه‌نویسی کلیدی اپلیکیشن، زیرساخت و معماری استدلال هوش مصنوعی و پیاده‌سازی همیار مستقل.",
                                        style = Typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Right
                                    )
                                }

                                Divider(color = glassBorderColor.copy(alpha = 0.3f))

                                // Person 2: Pourmohyabadi
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "دکتر حسین پورمحی‌آبادی",
                                        style = Typography.bodyLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right
                                    )
                                    Text(
                                        text = "نقش: بنیان‌گذار مشترک، مدیر علمی، انطباق فقهی و صحت‌سنجی فتاوا",
                                        style = Typography.labelSmall,
                                        color = AccentGold,
                                        textAlign = TextAlign.Right
                                    )
                                    Text(
                                        text = "عهده‌دار تدوین قواعد فقهی، انطباق احکام موضوعه بر ادله اثباتی و تایید علمی و شرعی لوایح صادر شده.",
                                        style = Typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Right
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { showCreditsDialog = false },
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "بستن اعتبارنامه",
                                        style = Typography.bodyMedium,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
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

// Help extension inside the file to clean up padding hierarchy
@Composable
private fun Modifier.fillOuterPadding() = Modifier
    .fillMaxWidth()
    .padding(24.dp)
