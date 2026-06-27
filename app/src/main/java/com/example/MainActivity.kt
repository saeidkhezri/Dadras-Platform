package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.components.CopilotOverlay
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.testTag
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.CitizenViewModel
import com.example.viewmodel.CopilotViewModel
import com.example.viewmodel.UserRole
import androidx.core.view.WindowCompat
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.layout.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.utils.NetworkMonitor

class MainActivity : ComponentActivity() {
    override fun onPause() {
        super.onPause()
        try {
            val authViewModel = androidx.lifecycle.ViewModelProvider(this)[com.example.viewmodel.AuthViewModel::class.java]
            authViewModel.markActivityTime()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.network.AiOrchestrator.appContext = applicationContext
        com.example.network.AiOrchestrator.loadKeysFromPrefs(applicationContext)
        com.example.network.FirebaseService.initialize(applicationContext)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val adminViewModel: AdminViewModel = viewModel()
            val citizenViewModel: CitizenViewModel = viewModel()
            val copilotViewModel: CopilotViewModel = viewModel()

            val isDarkTheme by authViewModel.isDarkTheme.collectAsState()
            val isDynamicBg by authViewModel.isDynamicBackground.collectAsState()

            val context = LocalContext.current
            
            val requiredPermissions = remember {
                val list = mutableListOf(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.RECORD_AUDIO
                )
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    list.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                    list.add(android.Manifest.permission.READ_MEDIA_AUDIO)
                    list.add(android.Manifest.permission.READ_MEDIA_VIDEO)
                    list.add(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    list.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q) {
                        list.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }
                }
                list.toTypedArray()
            }

            var showPermissionDialog by remember {
                val allGranted = requiredPermissions.all {
                    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                }
                mutableStateOf(!allGranted)
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { result ->
                showPermissionDialog = false
            }

            CompositionLocalProvider(com.example.ui.components.LocalDynamicBackground provides isDynamicBg) {
                MyApplicationTheme(darkTheme = isDarkTheme) {
                    val session by authViewModel.session.collectAsState()
                val navController = rememberNavController()

                // ردیابی نام صفحه جاری برای راهنمایی هوشمند دستیار کپیلوت
                var currentScreenName by remember { mutableStateOf("صفحه ورود") }
                var isUniversalMenuOpen by remember { mutableStateOf(false) }

                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                        NavHost(
                            navController = navController,
                            startDestination = "login",
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // ۱. صفحه ورود کاربران
                            composable("login") {
                                currentScreenName = "صفحه ورود"
                                LoginScreen(
                                    authViewModel = authViewModel,
                                    onLoginSuccess = {
                                        val activeRole = authViewModel.session.value?.role
                                        if (activeRole == UserRole.ADMIN) {
                                            navController.navigate("admin_panel") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        } else if (activeRole == UserRole.LAWYER) {
                                            navController.navigate("lawyer_panel") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        } else {
                                            navController.navigate("dashboard") {
                                                popUpTo("login") { inclusive = true }
                                            }
                                        }
                                    }
                                )
                            }

                            // ۲. پیشخوان کاربر عادی
                            composable(
                                route = "dashboard?tab={tab}",
                                arguments = listOf(navArgument("tab") { defaultValue = "dashboard"; type = NavType.StringType })
                            ) { backStackEntry ->
                                val tab = backStackEntry.arguments?.getString("tab") ?: "dashboard"
                                currentScreenName = "میز خدمت کاربران عادی"
                                CitizenDashboardScreen(
                                    authViewModel = authViewModel,
                                    citizenViewModel = citizenViewModel,
                                    adminViewModel = adminViewModel,
                                    initialTab = tab,
                                    onTriggerSystemMenu = { isUniversalMenuOpen = true },
                                    onNavigateToWizard = { navController.navigate("wizard") },
                                    onNavigateToCase = { id -> navController.navigate("case_detail/$id") },
                                    onNavigateToLibrary = { navController.navigate("library") },
                                    onNavigateToAdmin = { navController.navigate("admin_panel") }
                                )
                            }

                            // ۳. دستیار هوشمند تنظیم اسناد دادرس
                            composable("wizard") {
                                currentScreenName = "دستیار هوشمند تنظیم اسناد دادرس"
                                RequestWizardScreen(
                                    authViewModel = authViewModel,
                                    citizenViewModel = citizenViewModel,
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }

                            // ۴. کتابخانه قوانین
                            composable("library") {
                                currentScreenName = "کتابخانه قوانین فارسی"
                                LibraryScreen(
                                    citizenViewModel = citizenViewModel,
                                    authViewModel = authViewModel,
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }

                            // ۵. جزئیات پرونده و خط دادرسی بصری
                            composable(
                                route = "case_detail/{caseId}",
                                arguments = listOf(navArgument("caseId") { type = NavType.IntType })
                            ) { backStackEntry ->
                                val caseId = backStackEntry.arguments?.getInt("caseId") ?: 0
                                currentScreenName = "خط زمانی و اسناد پرونده"
                                CaseDetailScreen(
                                    caseId = caseId,
                                    citizenViewModel = citizenViewModel,
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }

                            // ۶. پیشخوان مدیر سیستم (ادمین پنل)
                            composable("admin_panel") {
                                currentScreenName = "پنل مدیریت سیستم"
                                AdminPanelScreen(
                                    authViewModel = authViewModel,
                                    adminViewModel = adminViewModel,
                                    onNavigateBack = { authViewModel.logout() }
                                )
                            }

                            // ۷. پیشخوان اختصاصی وکلای دادگستری
                            composable("lawyer_panel") {
                                currentScreenName = "پیشخوان فوق تخصصی وکلای دادگستری"
                                LawyerPanelScreen(
                                    authViewModel = authViewModel,
                                    adminViewModel = adminViewModel,
                                    onNavigateBack = { authViewModel.logout() }
                                )
                            }
                        }

                        // مانیتورینگ خروج از اکانت و بازگشت به صفحه لاگین
                        LaunchedEffect(session) {
                            if (session == null) {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }

                        // همیار هوشمند شناور (تراز با تمام صفحات)
                        if (session != null) {
                            CopilotOverlay(
                                copilotViewModel = copilotViewModel,
                                currentScreenName = currentScreenName
                            )
                        }

                        // ۱. مانیتورینگ اتصال دائم به اینترنت همراه با هشدارهای ۳ ثانیه‌ای
                        val networkMonitor = remember { NetworkMonitor(context.applicationContext) }
                        val isConnected by networkMonitor.isConnected.collectAsState()
                        var showConnectionPopup by remember { mutableStateOf(false) }
                        var popupMessage by remember { mutableStateOf("") }
                        var popupColor by remember { mutableStateOf(Color.Red) }
                        var popupIcon by remember { mutableStateOf(Icons.Default.CloudOff) }
                        var hasInitializedNetwork by remember { mutableStateOf(false) }

                        LaunchedEffect(isConnected) {
                            if (!hasInitializedNetwork) {
                                hasInitializedNetwork = true
                                if (!isConnected) {
                                    popupMessage = "اتصال به اینترنت برقرار نیست! لطفا شبکه یا فیلترشکن خود را بررسی کنید."
                                    popupColor = Color(0xFFEF4444)
                                    popupIcon = Icons.Default.CloudOff
                                    showConnectionPopup = true
                                    kotlinx.coroutines.delay(3000)
                                    showConnectionPopup = false
                                }
                                return@LaunchedEffect
                            }

                            if (isConnected) {
                                popupMessage = "اتصال به اینترنت مجدداً برقرار شد."
                                popupColor = Color(0xFF10B981)
                                popupIcon = Icons.Default.Cloud
                                showConnectionPopup = true
                            } else {
                                popupMessage = "اتصال به اینترنت قطع شد! لطفا شبکه یا فیلترشکن خود را بررسی کنید."
                                popupColor = Color(0xFFEF4444)
                                popupIcon = Icons.Default.CloudOff
                                showConnectionPopup = true
                            }

                            kotlinx.coroutines.delay(3000)
                            showConnectionPopup = false
                        }

                        if (showConnectionPopup) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 80.dp, start = 16.dp, end = 16.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = popupColor.copy(alpha = 0.95f)),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth(0.9f)
                                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = popupIcon,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = popupMessage,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // ۲. دیالوگ لوکس و هوشمند مجوزهای سیستمی (Onboarding Permissions Dialog)
                        if (showPermissionDialog) {
                            Dialog(
                                onDismissRequest = { /* Don't dismiss without choice */ },
                                properties = DialogProperties(usePlatformDefaultWidth = false)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.85f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier
                                            .fillMaxWidth(0.9f)
                                            .border(1.dp, Color(0xFFB18F54).copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                                            .padding(2.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            Text(
                                                text = "درخواست مجوزهای دسترسی",
                                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFFB18F54),
                                                textAlign = TextAlign.Center
                                            )
                                            
                                            Text(
                                                text = "جهت عملکرد کامل، دقیق و بدون اختلال خدمات هوشمند سامانه دادرس، لطفاً مجوزهای زیر را اعطا بفرمایید:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White.copy(alpha = 0.85f),
                                                textAlign = TextAlign.Right,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            // Permission Items
                                            PermissionItemRow(
                                                icon = Icons.Default.Camera,
                                                title = "دوربین و گالری تصاویر",
                                                desc = "جهت تصویربرداری و اسکن لوایح، مدارک و استخراج متن اسناد حقوقی با موتور OCR"
                                            )
                                            
                                            PermissionItemRow(
                                                icon = Icons.Default.Mic,
                                                title = "میکروفون و ضبط صدا",
                                                desc = "جهت ضبط مستقیم شواهد کلامی شما و پیاده‌سازی خودکار گفتار به نوشتار رسمی"
                                            )
                                            
                                            PermissionItemRow(
                                                icon = Icons.Default.Folder,
                                                title = "حافظه و مدیریت فایل‌ها",
                                                desc = "جهت بارگذاری مستندات لوایح از روی حافظه محلی و ذخیره فایل‌های خروجی صادر شده"
                                            )
                                            
                                            PermissionItemRow(
                                                icon = Icons.Default.Notifications,
                                                title = "ارسال اطلاعیه‌ها (نوتیفیکیشن)",
                                                desc = "جهت دریافت لحظه‌ای هشدارهای دادرسی و پیامدهای ثبت لوایح در پس‌زمینه"
                                            )
                                            
                                            Spacer(modifier = Modifier.height(16.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        permissionLauncher.launch(requiredPermissions)
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB18F54)),
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("اعطای مجوزهای مورد نیاز", color = Color.Black, fontWeight = FontWeight.Bold)
                                                }
                                                
                                                TextButton(
                                                    onClick = { showPermissionDialog = false },
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text("بعداً", color = Color.White.copy(alpha = 0.6f))
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
}
}

@Composable
fun PermissionItemRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(Color(0xFF1E293B), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFFB18F54), modifier = Modifier.size(20.dp))
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
            Text(text = desc, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
        }
    }
}
