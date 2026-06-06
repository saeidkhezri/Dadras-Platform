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
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.CitizenViewModel
import com.example.viewmodel.CopilotViewModel
import com.example.viewmodel.UserRole

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            val adminViewModel: AdminViewModel = viewModel()
            val citizenViewModel: CitizenViewModel = viewModel()
            val copilotViewModel: CopilotViewModel = viewModel()

            val isDarkTheme by authViewModel.isDarkTheme.collectAsState()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                val session by authViewModel.session.collectAsState()
                val navController = rememberNavController()

                // ردیابی نام صفحه جاری برای راهنمایی هوشمند دستیار کپیلوت
                var currentScreenName by remember { mutableStateOf("صفحه ورود") }

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = "login",
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // ۱. صفحه ورود کاربران
                            composable("login") {
                                currentScreenName = "صفحه ورود مستقل"
                                LoginScreen(
                                    authViewModel = authViewModel,
                                    onLoginSuccess = {
                                        val activeRole = authViewModel.session.value?.role
                                        if (activeRole == UserRole.ADMIN) {
                                            navController.navigate("admin_panel") {
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

                            // ۲. پیشخوان شهروند معمولی
                            composable("dashboard") {
                                currentScreenName = "میز خدمت شهروندی"
                                CitizenDashboardScreen(
                                    authViewModel = authViewModel,
                                    citizenViewModel = citizenViewModel,
                                    onNavigateToWizard = { navController.navigate("wizard") },
                                    onNavigateToCase = { id -> navController.navigate("case_detail/$id") },
                                    onNavigateToLibrary = { navController.navigate("library") }
                                )
                            }

                            // ۳. جادوگر مولی استپ ۷ مرحله‌ای
                            composable("wizard") {
                                currentScreenName = "جادوگر تنظیم اسناد قضایی"
                                RequestWizardScreen(
                                    authViewModel = authViewModel,
                                    citizenViewModel = citizenViewModel,
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }

                            // ۴. کتابخانه قوانین
                            composable("library") {
                                currentScreenName = "کتابخانه قوانین ملی"
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
                                currentScreenName = "پنل فوق امنیتی مدیریت"
                                AdminPanelScreen(
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
                        CopilotOverlay(
                            copilotViewModel = copilotViewModel,
                            currentScreenName = currentScreenName
                        )
                    }
                }
            }
        }
    }
}
