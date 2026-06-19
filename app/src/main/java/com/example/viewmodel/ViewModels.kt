package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject

// تعریف نقش‌های کاربری
enum class UserRole(val label: String) {
    ADMIN("مدیر سیستم"),
    CITIZEN("کاربر عادی"),
    LAWYER("وکیل (به‌زودی)"),
    JUDGE("مقام قضایی (به‌زودی)")
}

data class UserSession(
    val username: String,
    val role: UserRole,
    val token: String
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val logDao = db.logDao()
    private val scalableDao = db.scalableDao()

    private val _session = MutableStateFlow<UserSession?>(null)
    val session: StateFlow<UserSession?> = _session.asStateFlow()

    private val _isPasswordChangeRequired = MutableStateFlow(false)
    val isPasswordChangeRequired: StateFlow<Boolean> = _isPasswordChangeRequired.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _isDynamicBackground = MutableStateFlow(true)
    val isDynamicBackground: StateFlow<Boolean> = _isDynamicBackground.asStateFlow()

    private val _isPersianNumbersEnabled = MutableStateFlow(true)
    val isPersianNumbersEnabled: StateFlow<Boolean> = _isPersianNumbersEnabled.asStateFlow()

    private var registeredUsers = listOf<UserEntity>()
    private var adminCustomPassword = ""

    init {
        viewModelScope.launch {
            scalableDao.getAllUsers().collect {
                registeredUsers = it
            }
        }
        restoreSessionIfValid()
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun toggleDynamicBackground() {
        _isDynamicBackground.value = !_isDynamicBackground.value
    }

    fun toggleNumberFormat() {
        _isPersianNumbersEnabled.value = !_isPersianNumbersEnabled.value
    }

    fun isPasswordSecure(password: String): Boolean {
        if (password.length < 12) return false
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        return hasUpper && hasLower && hasDigit && hasSpecial
    }

    fun login(username: String, password: String): Boolean {
        _loginError.value = null
        
        // 1. ادمین با پسورد سفارشی تغییر داده شده
        if (username == "Administrator" && adminCustomPassword.isNotEmpty() && password == adminCustomPassword) {
            _session.value = UserSession("Administrator", UserRole.ADMIN, "admin-auth-token")
            _isPasswordChangeRequired.value = false
            viewModelScope.launch {
                logDao.insertLog(ActivityLogEntity(username = "Administrator", action = "ورود موفق مدیر سیستم با رمز عبور سفارشی فوق امن", date = getPersianDateNow()))
                scalableDao.insertAuditLog(AuditLogEntity(user_id = 1, action_type = "ADMIN_LOGIN_CUSTOM", description = "ورود موفق مدیر ارشد با رمز سفارشی"))
            }
            return true
        }

        // 2. ورود ادمین با رمز فرضی اولیه (نیاز به تغییر فوری رمز)
        if (username == "Administrator" && password == "Administrator") {
            _session.value = UserSession("Administrator", UserRole.ADMIN, "admin-temp-token")
            _isPasswordChangeRequired.value = true
            viewModelScope.launch {
                logDao.insertLog(ActivityLogEntity(username = "Administrator", action = "ورود اولیه مدیر سیستم با رمز فرضی", date = getPersianDateNow()))
                scalableDao.insertAuditLog(AuditLogEntity(user_id = 1, action_type = "ADMIN_LOGIN_INIT", description = "ورود مدیر سیستم با پسورد پیش‌فرض و اعلام نیاز به تغییر رمز"))
            }
            return true
        }

        // 3. کاربر شهروند تعیین شده: محمدسعید خضریپور / SaeidKh3916
        if (username == "محمدسعید خضریپور" && password == "SaeidKh3916") {
            _session.value = UserSession("محمدسعید خضریپور", UserRole.CITIZEN, "citizen-saeid-token")
            _isPasswordChangeRequired.value = false
            viewModelScope.launch {
                logDao.insertLog(ActivityLogEntity(username = "محمدسعید خضریپور", action = "ورود شهروند تعیین شده به سیستم", date = getPersianDateNow()))
                scalableDao.insertAuditLog(AuditLogEntity(user_id = 99, action_type = "CITIZEN_LOGIN", description = "ورود موفق شهروند پیش‌ساخته: محمدسعید خضریپور"))
            }
            return true
        }

        // 4. کاربران ثبت‌شده بصورت دینامیک در دیتابیس
        val dbUser = registeredUsers.find { it.username == username && it.password_hash == password }
        if (dbUser != null) {
            val role = if (dbUser.role_id == 1) UserRole.ADMIN else UserRole.CITIZEN
            _session.value = UserSession(dbUser.username, role, "token-${dbUser.id}")
            _isPasswordChangeRequired.value = false
            viewModelScope.launch {
                logDao.insertLog(ActivityLogEntity(username = dbUser.username, action = "ورود موفق کاربر پایگاه داده", date = getPersianDateNow()))
                scalableDao.insertAuditLog(AuditLogEntity(user_id = dbUser.id, action_type = "USER_LOGIN", description = "ورود موفق کاربر دیتابیس: ${dbUser.username}"))
            }
            return true
        }

        _loginError.value = "نام کاربری یا رمز عبور اشتباه است."
        return false
    }

    fun completePasswordChange(newPassword: String) {
        if (isPasswordSecure(newPassword)) {
            adminCustomPassword = newPassword
            _isPasswordChangeRequired.value = false
            _session.value = UserSession("Administrator", UserRole.ADMIN, "admin-auth-token")
            viewModelScope.launch {
                logDao.insertLog(ActivityLogEntity(username = "Administrator", action = "تغییر موفقیت‌آمیز رمز عبور حساب ادمین به رمز فوق امنیتی", date = getPersianDateNow()))
                scalableDao.insertAuditLog(AuditLogEntity(user_id = 1, action_type = "ADMIN_PASSWORD_CHANGE", description = "رمز امنیتی ادمین تغییر داده شد."))
            }
        } else {
            _loginError.value = "رمز عبور جدید ضعیف است! طبق قوانین امنیتی، باید حداقل ۱۲ کاراکتر، شامل حروف بزرگ، حروف کوچک، عدد و کاراکتر خاص باشد."
        }
    }

    fun markActivityTime() {
        val sess = _session.value
        if (sess != null) {
            val prefs = getApplication<Application>().getSharedPreferences("user_session_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("username", sess.username)
                .putString("role", sess.role.name)
                .putString("token", sess.token)
                .putLong("last_active_time", System.currentTimeMillis())
                .apply()
        }
    }

    fun restoreSessionIfValid() {
        val prefs = getApplication<Application>().getSharedPreferences("user_session_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("username", null)
        val roleStr = prefs.getString("role", null)
        val token = prefs.getString("token", null)
        val lastActive = prefs.getLong("last_active_time", 0)

        if (username != null && roleStr != null && token != null && lastActive > 0) {
            val diff = System.currentTimeMillis() - lastActive
            // 5 minutes is 300,000 milliseconds
            if (diff <= 300000) {
                try {
                    val role = UserRole.valueOf(roleStr)
                    _session.value = UserSession(username, role, token)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                prefs.edit().clear().apply()
            }
        }
    }

    fun logout() {
        val currentUsername = _session.value?.username ?: "نامشخص"
        _session.value = null
        _isPasswordChangeRequired.value = false
        val prefs = getApplication<Application>().getSharedPreferences("user_session_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        viewModelScope.launch {
            logDao.insertLog(ActivityLogEntity(username = currentUsername, action = "خروج از سامانه مستقل", date = getPersianDateNow()))
        }
    }

    private fun getPersianDateNow(): String = "۱۶ خرداد ۱۴۰۵"
}

// کلاس‌ها و قالب‌های ارتباطی پایگاه مدیریت منابع قانونی
data class OfficialSourceDetails(
    val id: Int,
    val title: String,
    val category: String, // قانون ملی, آیین‌نامه اجرایی, نظریه مشورتی, آرای وحدت رویه, آرای محاکم, سند علمی دانشگاهی
    val issueDate: String,
    val effectiveDate: String,
    val organization: String,
    val version: String,
    val status: String, // فعال, غیرفعال
    val officialUrl: String,
    val priority: String, // بسیار بالا, بالا, متوسط, پایین
    val trustScore: Int, // ۱ تا ۱۰۰
    val lastVerification: String,
    val lastSync: String,
    val availabilityStatus: String, // در دسترس, خطا در برقراری, منقطع
    val validationStatus: String // معتبر, نامعتبر, تایید نشده
)

data class SourceVersion(
    val id: Int,
    val sourceId: Int,
    val sourceTitle: String,
    val version: String,
    val date: String,
    val modifications: String,
    val content: String,
    val isCurrent: Boolean
)

// ویومدل پنل مدیریت
class AdminViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val logDao = db.logDao()
    private val caseDao = db.caseDao()
    private val resourceDao = db.resourceDao()
    private val scalableDao = db.scalableDao()

    val activityLogs: StateFlow<List<ActivityLogEntity>> = logDao.getAllLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ذخیره کلیدهای هوش بر اساس اولویت پنل و سه کلید پشتیبان فعال
    private val _geminiApiKey = MutableStateFlow("")
    val geminiApiKey = _geminiApiKey.asStateFlow()

    private val _openaiApiKey = MutableStateFlow("")
    val openaiApiKey = _openaiApiKey.asStateFlow()

    private val _openrouterApiKey = MutableStateFlow("")
    val openrouterApiKey = _openrouterApiKey.asStateFlow()

    private val _geminiApiKeys = MutableStateFlow<List<String>>(emptyList())
    val geminiApiKeys = _geminiApiKeys.asStateFlow()

    private val _openaiApiKeys = MutableStateFlow<List<String>>(emptyList())
    val openaiApiKeys = _openaiApiKeys.asStateFlow()

    private val _openrouterApiKeys = MutableStateFlow<List<String>>(emptyList())
    val openrouterApiKeys = _openrouterApiKeys.asStateFlow()

    private val _geminiProxyUrl = MutableStateFlow("https://generativelanguage.googleapis.com/")
    val geminiProxyUrl = _geminiProxyUrl.asStateFlow()

    // ۳ وب‌سرویس جدید اضافه شده
    private val _groqApiKeys = MutableStateFlow<List<String>>(emptyList())
    val groqApiKeys = _groqApiKeys.asStateFlow()

    private val _cohereApiKeys = MutableStateFlow<List<String>>(emptyList())
    val cohereApiKeys = _cohereApiKeys.asStateFlow()

    private val _huggingfaceApiKeys = MutableStateFlow<List<String>>(emptyList())
    val huggingfaceApiKeys = _huggingfaceApiKeys.asStateFlow()

    init {
        val prefs = application.getSharedPreferences("admin_ai_keys", Context.MODE_PRIVATE)
        
        // در اولین راه‌اندازی، هیچ کلید سخت‌کدی در برنامه وجود ندارد و کاربر خودش مقادیر را تنظیم می‌کند
        if (!prefs.contains("keys_initialized") || !prefs.contains("openai_proxy_url")) {
            val defaultGeminiKey = try { com.example.BuildConfig.GEMINI_API_KEY } catch (e: Exception) { "" }
            val editor = prefs.edit()
            
            if (!prefs.contains("keys_initialized")) {
                editor.putString("gemini_key_1", defaultGeminiKey)
                editor.putString("gemini_key_2", "")
                editor.putString("gemini_key_3", "")
                editor.putString("openrouter_key_1", "")
                editor.putString("openrouter_key_2", "")
                editor.putString("openrouter_key_3", "")
                editor.putString("openai_key_1", "")
                editor.putString("openai_key_2", "")
                editor.putString("openai_key_3", "")
                editor.putString("groq_key_1", "")
                editor.putString("groq_key_2", "")
                editor.putString("groq_key_3", "")
                editor.putString("cohere_key_1", "")
                editor.putString("cohere_key_2", "")
                editor.putString("cohere_key_3", "")
                editor.putString("huggingface_key_1", "")
                editor.putString("huggingface_key_2", "")
                editor.putString("huggingface_key_3", "")
                editor.putBoolean("keys_initialized", true)
            }
            if (!prefs.contains("gemini_proxy_url")) {
                editor.putString("gemini_proxy_url", "https://generativelanguage.googleapis.com/")
            }
            editor.putString("openrouter_proxy_url", prefs.getString("openrouter_proxy_url", "https://openrouter.ai/"))
            editor.putString("openai_proxy_url", prefs.getString("openai_proxy_url", "https://api.openai.com/"))
            editor.putString("groq_proxy_url", prefs.getString("groq_proxy_url", "https://api.groq.com/"))
            editor.putString("cohere_proxy_url", prefs.getString("cohere_proxy_url", "https://api.cohere.com/"))
            editor.putString("huggingface_proxy_url", prefs.getString("huggingface_proxy_url", "https://api-inference.huggingface.co/"))
            editor.apply()
        }

        val g1 = prefs.getString("gemini_key_1", "") ?: ""
        val g2 = prefs.getString("gemini_key_2", "") ?: ""
        val g3 = prefs.getString("gemini_key_3", "") ?: ""

        val o1 = prefs.getString("openai_key_1", "") ?: ""
        val o2 = prefs.getString("openai_key_2", "") ?: ""
        val o3 = prefs.getString("openai_key_3", "") ?: ""

        val or1 = prefs.getString("openrouter_key_1", "") ?: ""
        val or2 = prefs.getString("openrouter_key_2", "") ?: ""
        val or3 = prefs.getString("openrouter_key_3", "") ?: ""

        val gr1 = prefs.getString("groq_key_1", "") ?: ""
        val gr2 = prefs.getString("groq_key_2", "") ?: ""
        val gr3 = prefs.getString("groq_key_3", "") ?: ""

        val co1 = prefs.getString("cohere_key_1", "") ?: ""
        val co2 = prefs.getString("cohere_key_2", "") ?: ""
        val co3 = prefs.getString("cohere_key_3", "") ?: ""

        val hf1 = prefs.getString("huggingface_key_1", "") ?: ""
        val hf2 = prefs.getString("huggingface_key_2", "") ?: ""
        val hf3 = prefs.getString("huggingface_key_3", "") ?: ""
        val proxyUrl = prefs.getString("gemini_proxy_url", "https://generativelanguage.googleapis.com/") ?: "https://generativelanguage.googleapis.com/"

        _geminiApiKeys.value = listOf(g1, g2, g3)
        _openaiApiKeys.value = listOf(o1, o2, o3)
        _openrouterApiKeys.value = listOf(or1, or2, or3)
        _groqApiKeys.value = listOf(gr1, gr2, gr3)
        _cohereApiKeys.value = listOf(co1, co2, co3)
        _huggingfaceApiKeys.value = listOf(hf1, hf2, hf3)
        _geminiProxyUrl.value = proxyUrl

        _geminiApiKey.value = g1.ifBlank { g2.ifBlank { g3 } }
        _openaiApiKey.value = o1.ifBlank { o2.ifBlank { o3 } }
        _openrouterApiKey.value = or1.ifBlank { or2.ifBlank { or3 } }

        com.example.network.RetrofitClient.customBaseUrl = proxyUrl
        syncOrchestratorKeys()
    }

    private fun syncOrchestratorKeys() {
        val gemini = _geminiApiKeys.value.firstOrNull { it.isNotBlank() } ?: ""
        val openai = _openaiApiKeys.value.firstOrNull { it.isNotBlank() } ?: ""
        val openrouter = _openrouterApiKeys.value.firstOrNull { it.isNotBlank() } ?: ""

        com.example.network.AiOrchestrator.adminGeminiKey = gemini
        com.example.network.AiOrchestrator.adminOpenRouterKey = openrouter
        com.example.network.AiOrchestrator.adminOpenAiKey = openai
        
        com.example.network.AiOrchestrator.geminiKeysList = _geminiApiKeys.value.filter { it.isNotBlank() }
        com.example.network.AiOrchestrator.openrouterKeysList = _openrouterApiKeys.value.filter { it.isNotBlank() }
        com.example.network.AiOrchestrator.openaiKeysList = _openaiApiKeys.value.filter { it.isNotBlank() }

        // وب‌سرویس‌های جدید
        com.example.network.AiOrchestrator.groqKeysList = _groqApiKeys.value.filter { it.isNotBlank() }
        com.example.network.AiOrchestrator.cohereKeysList = _cohereApiKeys.value.filter { it.isNotBlank() }
        com.example.network.AiOrchestrator.huggingfaceKeysList = _huggingfaceApiKeys.value.filter { it.isNotBlank() }

        // همگام‌سازی آدرس‌های پروکسی ثبت شده
        val prefs = getApplication<Application>().getSharedPreferences("admin_ai_keys", Context.MODE_PRIVATE)
        com.example.network.AiOrchestrator.openRouterBaseUrl = prefs.getString("openrouter_proxy_url", "https://openrouter.ai/") ?: "https://openrouter.ai/"
        com.example.network.AiOrchestrator.openAiBaseUrl = prefs.getString("openai_proxy_url", "https://api.openai.com/") ?: "https://api.openai.com/"
        com.example.network.AiOrchestrator.groqBaseUrl = prefs.getString("groq_proxy_url", "https://api.groq.com/") ?: "https://api.groq.com/"
        com.example.network.AiOrchestrator.cohereBaseUrl = prefs.getString("cohere_proxy_url", "https://api.cohere.com/") ?: "https://api.cohere.com/"
        com.example.network.AiOrchestrator.huggingFaceBaseUrl = prefs.getString("huggingface_proxy_url", "https://api-inference.huggingface.co/") ?: "https://api-inference.huggingface.co/"
    }

    fun saveApiKeys(gemini: String, openRouter: String, openAi: String) {
        val geminiList = listOf(gemini, "", "")
        val openRouterList = listOf(openRouter, "", "")
        val openAiList = listOf(openAi, "", "")
        saveMultiApiKeys(geminiList, openRouterList, openAiList, listOf("", "", ""), listOf("", "", ""), listOf("", "", ""), _geminiProxyUrl.value)
    }

    fun saveMultiApiKeys(
        gemini: List<String>,
        openRouter: List<String>,
        openAi: List<String>,
        groq: List<String>,
        cohere: List<String>,
        huggingFace: List<String>,
        proxyUrl: String = "https://generativelanguage.googleapis.com/"
    ) {
        val prefs = getApplication<Application>().getSharedPreferences("admin_ai_keys", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        
        val gList = (gemini.map { it.trim() } + listOf("", "", "")).take(3)
        val orList = (openRouter.map { it.trim() } + listOf("", "", "")).take(3)
        val oaList = (openAi.map { it.trim() } + listOf("", "", "")).take(3)
        val grList = (groq.map { it.trim() } + listOf("", "", "")).take(3)
        val coList = (cohere.map { it.trim() } + listOf("", "", "")).take(3)
        val hfList = (huggingFace.map { it.trim() } + listOf("", "", "")).take(3)

        for (i in 0..2) {
            editor.putString("gemini_key_${i+1}", gList[i])
            editor.putString("openrouter_key_${i+1}", orList[i])
            editor.putString("openai_key_${i+1}", oaList[i])
            editor.putString("groq_key_${i+1}", grList[i])
            editor.putString("cohere_key_${i+1}", coList[i])
            editor.putString("huggingface_key_${i+1}", hfList[i])
        }
        editor.putString("gemini_proxy_url", proxyUrl)
        editor.putBoolean("keys_initialized", true)
        editor.apply()

        _geminiApiKeys.value = gList
        _openrouterApiKeys.value = orList
        _openaiApiKeys.value = oaList
        _groqApiKeys.value = grList
        _cohereApiKeys.value = coList
        _huggingfaceApiKeys.value = hfList
        _geminiProxyUrl.value = proxyUrl

        _geminiApiKey.value = gList.firstOrNull { it.isNotBlank() } ?: ""
        _openrouterApiKey.value = orList.firstOrNull { it.isNotBlank() } ?: ""
        _openaiApiKey.value = oaList.firstOrNull { it.isNotBlank() } ?: ""

        com.example.network.RetrofitClient.customBaseUrl = proxyUrl
        syncOrchestratorKeys()

        viewModelScope.launch {
            logDao.insertLog(
                ActivityLogEntity(
                    username = "مدیر سیستم",
                    action = "بروزرسانی موفق لیست کلیدهای اصلی و زاپاس API ارائه‌دهندگان هوش مصنوعی در اندروید",
                    date = getPersianDateNow()
                )
            )
        }
    }

    private fun getPersianDateNow(): String = "۱۶ خرداد ۱۴۰۵"

    val totalCasesCount = caseDao.getAllCases()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalResourcesCount = resourceDao.getAllResources()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val systemUsers: StateFlow<List<UserEntity>> = scalableDao.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // تنظیمات سامانه
    private val _aiProvider = MutableStateFlow("GPT-4o Enterprise")
    val aiProvider = _aiProvider.asStateFlow()

    private val _isRateLimitingEnabled = MutableStateFlow(true)
    val isRateLimitingEnabled = _isRateLimitingEnabled.asStateFlow()

    private val _totalUsersCount = MutableStateFlow(125)
    val totalUsersCount = _totalUsersCount.asStateFlow()

    private val _totalSubscriptionsActive = MutableStateFlow(49)
    val totalSubscriptionsActive = _totalSubscriptionsActive.asStateFlow()

    // --- مدیریت فراداده منابع و رجیستری رسمی ---
    private val _registeredSources = MutableStateFlow<List<OfficialSourceDetails>>(
        listOf(
            OfficialSourceDetails(
                id = 1,
                title = "قانون مدنی جمهوری اسلامی ایران",
                category = "قانون ملی",
                issueDate = "۱۳۰۷/۰۲/۱۸",
                effectiveDate = "۱۳۰۷/۰۳/۰۱",
                organization = "قوه مقننه مجلس شورای اسلامی",
                version = "۳.۲",
                status = "فعال",
                officialUrl = "https://rooznamehrasmi.ir/laws/civil_code",
                priority = "بسیار بالا",
                trustScore = 100,
                lastVerification = "۱۶ خرداد ۱۴۰۵",
                lastSync = "۱۶ خرداد ۱۴۰۵",
                availabilityStatus = "در دسترس",
                validationStatus = "معتبر"
            ),
            OfficialSourceDetails(
                id = 2,
                title = "قانون مجازات اسلامی تعزیرات",
                category = "قانون ملی",
                issueDate = "۱۳۹۲/۰۲/۰۱",
                effectiveDate = "۱۳۹۲/۰۳/۱۵",
                organization = "مجلس شورای اسلامی",
                version = "۲.۴",
                status = "فعال",
                officialUrl = "https://rooznamehrasmi.ir/laws/penal_code",
                priority = "بسیار بالا",
                trustScore = 99,
                lastVerification = "۱۶ خرداد ۱۴۰۵",
                lastSync = "۱۶ خرداد ۱۴۰۵",
                availabilityStatus = "در دسترس",
                validationStatus = "معتبر"
            ),
            OfficialSourceDetails(
                id = 3,
                title = "آیین‌نامه اجرایی حمایت از خانواده و طلاق",
                category = "آیین‌نامه اجرایی",
                issueDate = "۱۳۹۳/۱۲/۲۷",
                effectiveDate = "۱۳۹۴/۰۱/۱۵",
                organization = "ریاست معزز قوه قضاییه",
                version = "۱.۷",
                status = "فعال",
                officialUrl = "https://eadl.ir/directives/family_protection",
                priority = "بالا",
                trustScore = 95,
                lastVerification = "۱۵ خرداد ۱۴۰۵",
                lastSync = "۱۶ خرداد ۱۴۰۵",
                availabilityStatus = "در دسترس",
                validationStatus = "معتبر"
            ),
            OfficialSourceDetails(
                id = 4,
                title = "نظریه مشورتی تسبیب مراجع تجاری الکترونیک",
                category = "نظریه مشورتی",
                issueDate = "۱۴۰۱/۰۷/۱۰",
                effectiveDate = "۱۴۰۱/۰۷/۱۰",
                organization = "اداره کل حقوقی قوه قضاییه",
                version = "۱.۰",
                status = "فعال",
                officialUrl = "https://eadl.ir/opinions/causation",
                priority = "متوسط",
                trustScore = 92,
                lastVerification = "۱۴ خرداد ۱۴۰۵",
                lastSync = "۱۶ خرداد ۱۴۰۵",
                availabilityStatus = "در دسترس",
                validationStatus = "معتبر"
            ),
            OfficialSourceDetails(
                id = 5,
                title = "رأی وحدت رویه شماره ۸۲۸ دیوان عالی کشور",
                category = "آرای وحدت رویه",
                issueDate = "۱۴۰۲/۱۲/۰۸",
                effectiveDate = "۱۴۰۲/۱۲/۰۸",
                organization = "هیئت عمومی دیوان عالی کشور",
                version = "۱.۰",
                status = "فعال",
                officialUrl = "https://divanali.ir/precedents/828",
                priority = "بالا",
                trustScore = 98,
                lastVerification = "۱۵ خرداد ۱۴۰۵",
                lastSync = "۱۶ خرداد ۱۴۰۵",
                availabilityStatus = "در دسترس",
                validationStatus = "معتبر"
            )
        )
    )
    val registeredSources = _registeredSources.asStateFlow()

    // --- مدیریت زنجیره نسخه‌گذاری منابع حقوقی ---
    private val _sourceVersions = MutableStateFlow<List<SourceVersion>>(
        listOf(
            SourceVersion(
                id = 1,
                sourceId = 1,
                sourceTitle = "قانون مدنی جمهوری اسلامی ایران",
                version = "۳.۲",
                date = "۱۴۰۲/۰۶/۱۵",
                modifications = "اصلاحات ماده ۱۱۶۹ و واگذاری حضانت فرزند تا ۷ سالگی کاملاً به مادر",
                content = "قراردادهای خصوصی نسبت به کسانی که آن را منعقد نموده‌اند نافذ است... حضانت طفل تا ۷ سالگی به عهده مادر و پس از آن به عهده پدر است مگر در صورت اختلاف که دادگاه تصمیم می‌گیرد.",
                isCurrent = true
            ),
            SourceVersion(
                id = 2,
                sourceId = 1,
                sourceTitle = "قانون مدنی جمهوری اسلامی ایران",
                version = "۳.۱",
                date = "۱۳۸۲/۰۹/۰۸",
                modifications = "اصلاحات ماده ۱۱۶۹ در خصوص طفل دختر و پسر تفکیکی",
                content = "قراردادهای خصوصی نسبت به کسانی که آن را منعقد نموده‌اند نافذ است... حضانت پسر تا ۲ سال و دختر تا ۷ سال بر عهده مادر است.",
                isCurrent = false
            ),
            SourceVersion(
                id = 3,
                sourceId = 3,
                sourceTitle = "آیین‌نامه اجرایی حمایت از خانواده و طلاق",
                version = "۱.۷",
                date = "۱۳۹۸/۰۲/۱۰",
                modifications = "تخصیص الزامی داوران و ارجاع پرونده‌ها به مصلحین خانواده",
                content = "ماده ۱: محاکم بدوی موظفند کلیه طلاق‌های توافق را پیش از صدور حکم به مراکز غربالگری تخصصی و داوری خانواده واگذار نمایند.",
                isCurrent = true
            ),
            SourceVersion(
                id = 4,
                sourceId = 3,
                sourceTitle = "آیین‌نامه اجرایی حمایت از خانواده و طلاق",
                version = "۱.۶",
                date = "۱۳۹۴/۰۱/۱۵",
                modifications = "نسخه مصوب اولیه حمایت از خانواده بدون سامانه تصمیم",
                content = "ماده ۱: زوجین متقاضی طلاق راساً به مراجع دادگستری مراجعه و گواهی عدم امکان سازش اخذ می‌نمایند.",
                isCurrent = false
            )
        )
    )
    val sourceVersions = _sourceVersions.asStateFlow()

    // تنظیمات همگام‌سازی زمان‌بندی‌شده
    private val _selectedSyncSchedule = MutableStateFlow("خاموش (دستی)")
    val selectedSyncSchedule = _selectedSyncSchedule.asStateFlow()

    private val _syncingProgress = MutableStateFlow<Int?>(null) // null means not syncing, contains 0-100 when syncing
    val syncingProgress = _syncingProgress.asStateFlow()

    fun updateAiProvider(provider: String) {
        _aiProvider.value = provider
        viewModelScope.launch {
            logDao.insertLog(ActivityLogEntity(username = "Administrator", action = "تغییر ارائه‌دهنده پیش‌فرض هوش مصنوعی به: $provider", date = "۱۶ خرداد ۱۴۰۵"))
        }
    }

    fun toggleRateLimiting() {
        _isRateLimitingEnabled.value = !_isRateLimitingEnabled.value
    }

    fun addManualResource(title: String, category: String, description: String, content: String, articleNo: String) {
        viewModelScope.launch {
            resourceDao.insertResources(
                listOf(
                    LegalResourceEntity(
                        title = title,
                        category = category,
                        description = description,
                        content = content,
                        articleNo = articleNo
                    )
                )
            )
            
            // خودکار ثبت در جدول اسناد رسمی
            val nextId = (_registeredSources.value.maxByOrNull { it.id }?.id ?: 0) + 1
            val source = OfficialSourceDetails(
                id = nextId,
                title = title,
                category = "قانون ملی",
                issueDate = "۱۶ خرداد ۱۴۰۵",
                effectiveDate = "۱۶ خرداد ۱۴۰۵",
                organization = "مجلس شورای اسلامی",
                version = "۱.۰",
                status = "فعال",
                officialUrl = "https://rooznamehrasmi.ir/laws/manual_$nextId",
                priority = "متوسط",
                trustScore = 90,
                lastVerification = "۱۶ خرداد ۱۴۰۵",
                lastSync = "۱۶ خرداد ۱۴۰۵",
                availabilityStatus = "در دسترس",
                validationStatus = "معتبر"
            )
            _registeredSources.value = _registeredSources.value + source
            
            scalableDao.insertOfficialSource(
                OfficialSourcesRegistryEntity(
                    title = title,
                    source_url = "https://rooznamehrasmi.ir/laws/manual_$nextId",
                    source_type = category,
                    validation_date = "۱۶ خرداد ۱۴۰۵",
                    status = "active"
                )
            )

            scalableDao.insertAuditLog(
                AuditLogEntity(
                    user_id = 1,
                    action_type = "MANUAL_SOURCE_UPLOAD",
                    description = "ثبت منبع مکتوب جدید $title در ردیف منابع معتبر داخلی"
                )
            )

            logDao.insertLog(ActivityLogEntity(username = "Administrator", action = "افزودن منبع قانونی جدید در پایگاه مدون: $title", date = "۱۶ خرداد ۱۴۰۵"))
        }
    }

    // پایش مداوم و همگام‌سازی دستی/اتوماتیک منابع
    fun triggerManualSync(sourceId: Int) {
        viewModelScope.launch {
            _syncingProgress.value = 0
            val sourceName = _registeredSources.value.find { it.id == sourceId }?.title ?: "منبع نامشخص"
            
            logDao.insertLog(ActivityLogEntity(username = "Administrator", action = "آغاز همگام‌سازی و خزش رسمی وب برای سند: $sourceName", date = "۱۶ خرداد ۱۴۰۵"))
            
            // تولید تدریجی نوار پیشرفت خزش
            for (progress in listOf(20, 50, 80, 100)) {
                kotlinx.coroutines.delay(400)
                _syncingProgress.value = progress
            }
            
            // شبیه‌سازی تغییر مدل و ایجاد نسخه قدیمی بر اساس کشف تغییرات در مراجع
            val sourceIndex = _registeredSources.value.indexOfFirst { it.id == sourceId }
            if (sourceIndex != -1) {
                val current = _registeredSources.value[sourceIndex]
                val versionSplits = current.version.split(".")
                val major = versionSplits.getOrNull(0)?.toIntOrNull() ?: 1
                val minor = versionSplits.getOrNull(1)?.toIntOrNull() ?: 0
                val nextVersion = "$major.${minor + 1}"
                
                // بروزرسانی فراداده منبع
                val updated = current.copy(
                    version = nextVersion,
                    lastSync = "۱۶ خرداد ۱۴۰۵",
                    lastVerification = "۱۶ خرداد ۱۴۰۵",
                    availabilityStatus = "در دسترس",
                    validationStatus = "معتبر"
                )
                
                val currentList = _registeredSources.value.toMutableList()
                currentList[sourceIndex] = updated
                _registeredSources.value = currentList
                
                // افزودن زنجیره نسخه جدید به تاریخچه و تغییر نسخه قبلی به غیرجاری
                val newVersionEntity = SourceVersion(
                    id = (_sourceVersions.value.maxByOrNull { it.id }?.id ?: 0) + 1,
                    sourceId = sourceId,
                    sourceTitle = current.title,
                    version = nextVersion,
                    date = "۱۶ خرداد ۱۴۰۵",
                    modifications = "همگام‌سازی خودکار و پایش مراجع: ارتقا نگارش RAG به $nextVersion به علت تغییرات جزیی در تبصره الحاقی",
                    content = "توافقات جدید حاصله با ابلاغیه عالی قضایی... " + current.title,
                    isCurrent = true
                )
                
                // تغییر وضعیت نسخه‌های قدیمی این منبع
                val updatedVersions = _sourceVersions.value.map {
                    if (it.sourceId == sourceId) it.copy(isCurrent = false) else it
                } + newVersionEntity
                _sourceVersions.value = updatedVersions
                
                logDao.insertLog(ActivityLogEntity(username = "Administrator", action = "پایان همگام‌سازی $sourceName. کشف تغییرات فعال! ثبت نسخه تاریخی $nextVersion به صورت اتوماتیک.", date = "۱۶ خرداد ۱۴۰۵"))
                
                scalableDao.insertAuditLog(
                    AuditLogEntity(
                        user_id = 1,
                        action_type = "ONLINE_SYNC_COMPLETED",
                        description = "پایان خزش وب و کشف تغییر در $sourceName. بروزرسانی اتوماتیک به نسخه $nextVersion انجام شد."
                    )
                )
            }
            _syncingProgress.value = null
        }
    }

    // ذخیره تنظیمات زمان‌بندی همگام‌سازی مراجع
    fun updateSyncSchedule(schedule: String) {
        _selectedSyncSchedule.value = schedule
        viewModelScope.launch {
            logDao.insertLog(ActivityLogEntity(username = "Administrator", action = "تنظیم زمان‌بندی رصد خودکار قوانین به: $schedule", date = "۱۶ خرداد ۱۴۰۵"))
            scalableDao.insertAuditLog(
                AuditLogEntity(
                    user_id = 1,
                    action_type = "SYNC_SCHEDULE_UPDATE",
                    description = "تغییر دوره زمانی پایش وب به $schedule"
                )
            )
        }
    }

    // اضافه کردن منبع معتبر جدید به لیست رسمی
    fun addOfficialSource(title: String, category: String, sourceUrl: String, org: String, priority: String, trustScore: Int) {
        val nextId = (_registeredSources.value.maxByOrNull { it.id }?.id ?: 0) + 1
        val source = OfficialSourceDetails(
            id = nextId,
            title = title,
            category = category,
            issueDate = "۱۶ خرداد ۱۴۰۵",
            effectiveDate = "۱۶ خرداد ۱۴۰۵",
            organization = org,
            version = "۱.۰",
            status = "فعال",
            officialUrl = sourceUrl,
            priority = priority,
            trustScore = trustScore,
            lastVerification = "۱۶ خرداد ۱۴۰۵",
            lastSync = "۱۶ خرداد ۱۴۰۵",
            availabilityStatus = "در دسترس",
            validationStatus = "معتبر"
        )
        _registeredSources.value = _registeredSources.value + source
        
        viewModelScope.launch {
            scalableDao.insertOfficialSource(
                OfficialSourcesRegistryEntity(
                    title = title,
                    source_url = sourceUrl,
                    source_type = category,
                    validation_date = "۱۶ خرداد ۱۴۰۵",
                    status = "active"
                )
            )
            logDao.insertLog(ActivityLogEntity(username = "Administrator", action = "تعریف مرجع قانونی جدید: $title با امتیاز اعتماد $trustScore", date = "۱۶ خرداد ۱۴۰۵"))
            
            scalableDao.insertAuditLog(
                AuditLogEntity(
                    user_id = 1,
                    action_type = "OFFICIAL_SOURCE_CREATE",
                    description = "ایجاد مرجع رسمی پایگاه داده: $title در دسته $category"
                )
            )
        }
    }

    // فعال/غیرفعال کردن منبع
    fun toggleOfficialSourceStatus(sourceId: Int) {
        val index = _registeredSources.value.indexOfFirst { it.id == sourceId }
        if (index != -1) {
            val source = _registeredSources.value[index]
            val nextStatus = if (source.status == "فعال") "غیرفعال" else "فعال"
            val updated = source.copy(status = nextStatus)
            
            val currentList = _registeredSources.value.toMutableList()
            currentList[index] = updated
            _registeredSources.value = currentList
            
            viewModelScope.launch {
                logDao.insertLog(ActivityLogEntity(username = "Administrator", action = "تغییر وضعیت مرجع قانونی $source.title به $nextStatus", date = "۱۶ خرداد ۱۴۰۵"))
                scalableDao.insertAuditLog(
                    AuditLogEntity(
                        user_id = 1,
                        action_type = "SOURCE_STATUS_TOGGLE",
                        description = "تغییر دادن وضعیت مرجع پیوند $source.title به $nextStatus"
                    )
                )
            }
        }
    }

    // به روز رسانی رتبه اهمیت و سطح اعتماد
    fun updateTrustRanking(sourceId: Int, score: Int, priority: String) {
        val index = _registeredSources.value.indexOfFirst { it.id == sourceId }
        if (index != -1) {
            val current = _registeredSources.value[index]
            val updated = current.copy(trustScore = score, priority = priority)
            
            val currentList = _registeredSources.value.toMutableList()
            currentList[index] = updated
            _registeredSources.value = currentList
            
            viewModelScope.launch {
                logDao.insertLog(ActivityLogEntity(username = "Administrator", action = "بهبود پارامترهای بازیابی $current.title: سطح اهمیت $priority، ضریب اعتماد $score", date = "۱۶ خرداد ۱۴۰۵"))
                scalableDao.insertAuditLog(
                    AuditLogEntity(
                        user_id = 1,
                        action_type = "TRUST_RANK_UPDATE",
                        description = "تغییر اعتبار مرجع $current.title به امتیاز $score و اولویت $priority"
                    )
                )
            }
        }
    }

    fun createNewUser(username: String, email: String, mobile: String, rawPassword: String, planName: String, roleLabel: String) {
        viewModelScope.launch {
            val roleId = if (roleLabel == "مدیر") 1 else 2
            val user = UserEntity(
                username = username,
                email = email,
                mobile = mobile,
                password_hash = rawPassword,
                role_id = roleId,
                status = "active",
                created_at = "۱۶ خرداد ۱۴۰۵",
                updated_at = "۱۶ خرداد ۱۴۰۵",
                last_login = "تاکنون وارد نشده"
            )
            val userId = scalableDao.insertUser(user)
            
            scalableDao.insertSubscription(
                SubscriptionEntity(
                    user_id = userId.toInt(),
                    plan_name = planName,
                    start_date = "۱۶ خرداد ۱۴۰۵",
                    end_date = "۱۶ خرداد ۱۴۰۶",
                    status = "active"
                )
            )

            scalableDao.insertAuditLog(
                AuditLogEntity(
                    user_id = 1,
                    action_type = "USER_CREATE",
                    description = "ایجاد حساب کاربر هوشمند مستقل با نام: $username در پلن: $planName"
                )
            )

            logDao.insertLog(
                ActivityLogEntity(
                    username = "Administrator",
                    action = "ایجاد کاربر جدید $username با سطح دسترسی $roleLabel در پلن $planName",
                    date = "۱۶ خرداد ۱۴۰۵"
                )
            )
            _totalUsersCount.value += 1
            _totalSubscriptionsActive.value += 1
        }
    }

    fun clearAllSystemLogs() {
        viewModelScope.launch {
            logDao.clearLogs()
        }
    }
}

// ویومدل شهروند و جادوگر درخواست حقوقی
class CitizenViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val caseDao = db.caseDao()
    private val logDao = db.logDao()

    val legalResources: StateFlow<List<LegalResourceEntity>> = db.resourceDao().getAllResources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cases: StateFlow<List<CaseEntity>> = caseDao.getAllCases()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // اطلاعات جاری پرونده در جادوگر درخواست Wizard State
    private val _currentStep = MutableStateFlow(1)
    val currentStep = _currentStep.asStateFlow()

    private val _requestType = MutableStateFlow("شکایت")
    val requestType = _requestType.asStateFlow()

    private val _caseDescription = MutableStateFlow("")
    val caseDescription = _caseDescription.asStateFlow()

    private val _plaintiffName = MutableStateFlow("")
    val plaintiffName = _plaintiffName.asStateFlow()

    private val _defendantName = MutableStateFlow("")
    val defendantName = _defendantName.asStateFlow()

    private val _beneficiaryName = MutableStateFlow("")
    val beneficiaryName = _beneficiaryName.asStateFlow()

    private val _legalPosition = MutableStateFlow("")
    val legalPosition = _legalPosition.asStateFlow()

    private val _confidenceScore = MutableStateFlow(85)
    val confidenceScore = _confidenceScore.asStateFlow()

    private val _suggestedEvidence = MutableStateFlow<List<String>>(emptyList())
    val suggestedEvidence = _suggestedEvidence.asStateFlow()

    private val _requestedRelief = MutableStateFlow("")
    val requestedRelief = _requestedRelief.asStateFlow()

    // آپلود فایل شبیه‌سازی فنی
    private val _uploadedFiles = MutableStateFlow<List<String>>(emptyList())
    val uploadedFiles = _uploadedFiles.asStateFlow()

    // نتایج ساخته شده توسط هوش‌های مصنوعی
    private val _gptOutput = MutableStateFlow("")
    val gptOutput = _gptOutput.asStateFlow()

    private val _claudeOutput = MutableStateFlow("")
    val claudeOutput = _claudeOutput.asStateFlow()

    private val _geminiOutput = MutableStateFlow("")
    val geminiOutput = _geminiOutput.asStateFlow()

    private val _deepSeekOutput = MutableStateFlow("")
    val deepSeekOutput = _deepSeekOutput.asStateFlow()

    private val _qwenOutput = MutableStateFlow("")
    val qwenOutput = _qwenOutput.asStateFlow()

    private val _unifiedOutput = MutableStateFlow("")
    val unifiedOutput = _unifiedOutput.asStateFlow()

    private val _groqOutput = MutableStateFlow("")
    val groqOutput = _groqOutput.asStateFlow()

    private val _cohereOutput = MutableStateFlow("")
    val cohereOutput = _cohereOutput.asStateFlow()

    private val _hfOutput = MutableStateFlow("")
    val hfOutput = _hfOutput.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating = _isGenerating.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    // اعلان‌ها
    private val _notifications = MutableStateFlow(
        listOf(
            "ورود به سامانه با موفقیت انجام شد.",
            "پرونده جدیدی در سیستم ایجاد نشده است، از دستیار هوشمند استفاده کنید.",
            "مجموعه قوانین و کتابخانه حقوقی با آخرین بخشنامه‌ها به‌روزرسانی شد."
        )
    )
    val notifications = _notifications.asStateFlow()

    fun dismissNotification(index: Int) {
        _notifications.value = _notifications.value.filterIndexed { idx, _ -> idx != index }
    }

    fun changeStep(step: Int) {
        if (step in 1..8) {
            _currentStep.value = step
        }
    }

    fun updateRequestType(type: String) {
        _requestType.value = type
    }

    fun updateCaseDescription(desc: String) {
        _caseDescription.value = desc
    }

    fun updatePlaintiff(name: String) {
        _plaintiffName.value = name
    }

    fun updateDefendant(name: String) {
        _defendantName.value = name
    }

    fun updateBeneficiary(name: String) {
        _beneficiaryName.value = name
    }

    fun updateLegalPosition(position: String) {
        _legalPosition.value = position
    }

    fun updateRequestedRelief(relief: String) {
        _requestedRelief.value = relief
    }

    fun updateUnifiedOutput(output: String) {
        _unifiedOutput.value = output
    }

    fun updateGptOutput(output: String) { _gptOutput.value = output }
    fun updateClaudeOutput(output: String) { _claudeOutput.value = output }
    fun updateGeminiOutput(output: String) { _geminiOutput.value = output }
    fun updateDeepSeekOutput(output: String) { _deepSeekOutput.value = output }
    fun updateQwenOutput(output: String) { _qwenOutput.value = output }
    fun updateGroqOutput(output: String) { _groqOutput.value = output }
    fun updateCohereOutput(output: String) { _cohereOutput.value = output }
    fun updateHfOutput(output: String) { _hfOutput.value = output }

    fun uploadSimulatedFile(filename: String) {
        _uploadedFiles.value = _uploadedFiles.value + filename
    }

    fun removeSimulatedFile(filename: String) {
        _uploadedFiles.value = _uploadedFiles.value.filter { it != filename }
    }

    // تحلیل خودکار توصیف پرونده به وسیله موتور ارشد اورکستراتور مستقل
    fun runAutomaticLegalAnalysis() {
        _isAnalyzing.value = true
        viewModelScope.launch {
            // Anonymize description before sending to AI (Enterprise Privacy Architecture)
            val anonymizedDesc = com.example.network.AiOrchestrator.anonymizeText(_caseDescription.value)

            val prompt = """
            بر اساس شرح این درخواست حقوقی به زبان فارسی، تحلیل کن و یک اطلاعات ساختاریافته به فرمت JSON برگردان.
            متن درخواست: "$anonymizedDesc"
            فرمت خروجی صرفاً به این صوت باشد:
            {
              "plaintiff": "نام شاکی یا نام خواهان فرضی یافت شده",
              "defendant": "نام متهم یا خوانده تجاری فرضی یافت شده",
              "beneficiary": "نام ذینفع مستقیم",
              "legalPosition": "عنوان مجرمانه یا موضوع حقوقی پرونده",
              "score": عدد بین 30 تا 100 متناسب با دقت شواهد
            }
            چیزی غیر از JSON برنگردان.
            """.trimIndent()

            val response = com.example.network.AiOrchestrator.executeWithFailover(
                modelName = "GPT-4o Enterprise",
                prompt = prompt,
                systemInstruction = "دستیار قضایی و کارشناس خبره تحلیل متون حقوقی مستقل."
            )

            try {
                // پارس کردن ساده خروجی JSON
                val cleanJson = response.substringAfter("{").substringBeforeLast("}")
                val parsed = JSONObject("{$cleanJson}")
                _plaintiffName.value = parsed.optString("plaintiff", "نامشخص (خواهان)")
                _defendantName.value = parsed.optString("defendant", "نامشخص (خوانده)")
                _beneficiaryName.value = parsed.optString("beneficiary", "خواهان")
                _legalPosition.value = parsed.optString("legalPosition", "تعهد قراردادی مستقل")
                _confidenceScore.value = parsed.optInt("score", 88)
            } catch (e: Exception) {
                // در صورت خطا داده‌های پیش‌فرض
                _plaintiffName.value = "خواهان پرونده"
                _defendantName.value = "خوانده پرونده"
                _beneficiaryName.value = "شاکی خصوصی"
                _legalPosition.value = "تعهد مال‌باخته / تاخیر تادیه حقوقی"
                _confidenceScore.value = 85
            }

            // تولید شواهد تجربی مناسب موضوع
            val evidencePrompt = "لیستی از دلایل و مدارک یا شواهد اثبات دعوی مورد نیاز برای پرونده ای با عنوان حقوقی '${_legalPosition.value}' به فارسی بنویس. فقط ۳ مدرک اصلی به صورت خطوطی جداگانه بدون شماره بنویس."
            val evidenceResponse = com.example.network.AiOrchestrator.executeWithFailover("Claude 3.5 Sonnet", evidencePrompt, null)
            val rawLines = evidenceResponse.lines().filter { it.isNotBlank() }.map { it.replace("- ", "").replace("* ", "").trim() }
            val linesAreValid = rawLines.isNotEmpty() && rawLines.all { it.length < 150 } && !evidenceResponse.contains("دادخواست") && !evidenceResponse.contains("احتراماً") && !evidenceResponse.contains("ریاست")
            if (linesAreValid) {
                _suggestedEvidence.value = rawLines
            } else {
                _suggestedEvidence.value = listOf(
                    "۱. سند یا قرارداد مکتوب فیزیکی یا تایید ارتباطات الکترونیک",
                    "۲. استشهادیه شهود مطلع متعهد به واقعیت امر و تراکنش‌ها",
                    "۳. گواهی عدم انجام تعهد صادره از مراجع یا اسناد بانکی"
                )
            }

            _isAnalyzing.value = false
            _currentStep.value = 4 // انتقال خودکار به گام بعد بعد از محاسبات موفق
        }
    }

    // تولید مستند حقوقی نهایی به وسیله جریان کار چندمدلی اورکستراتور مستقل
    fun generateAIOutputs(userName: String) {
        _isGenerating.value = true
        viewModelScope.launch {
            // ۱. فراخوانی موازی جریان چندمدلی کارشناسی اصلی و موتور تخصصی اسناد استاندارد با وب‌سرویس‌های رایگان
            val workflowMapDeferred = async(Dispatchers.IO) {
                com.example.network.AiOrchestrator.executeStrategicMultiModelWorkflow(
                    requestType = _requestType.value,
                    caseDescription = _caseDescription.value,
                    plaintiff = _plaintiffName.value,
                    defendant = _defendantName.value,
                    evidence = _suggestedEvidence.value,
                    relief = _requestedRelief.value
                )
            }

            val freeServicesMapDeferred = async(Dispatchers.IO) {
                com.example.network.IranianLegalDocumentGeneratorService.generateDocuments(
                    requestType = _requestType.value,
                    caseDescription = _caseDescription.value,
                    plaintiff = _plaintiffName.value,
                    defendant = _defendantName.value,
                    evidence = _suggestedEvidence.value,
                    relief = _requestedRelief.value
                )
            }

            val workflowMap = workflowMapDeferred.await()
            val freeServicesMap = freeServicesMapDeferred.await()

            _gptOutput.value = workflowMap["gpt"] ?: ""
            _claudeOutput.value = workflowMap["claude"] ?: ""
            _deepSeekOutput.value = workflowMap["deepseek"] ?: ""
            _qwenOutput.value = workflowMap["qwen"] ?: ""
            _unifiedOutput.value = workflowMap["unified"] ?: ""

            // اعمال اسناد تولیدی بر اساس پرامپت اختصاصی و الگوهای رسمی ایران
            _geminiOutput.value = freeServicesMap["gemini"] ?: ""
            _groqOutput.value = freeServicesMap["groq"] ?: ""
            _cohereOutput.value = freeServicesMap["cohere"] ?: ""
            _hfOutput.value = freeServicesMap["hf"] ?: ""
            
            val parsedScore = workflowMap["confidence"]?.toIntOrNull() ?: 89
            _confidenceScore.value = parsedScore

            _isGenerating.value = false
            _currentStep.value = 7 // برو به مرور نهایی (گام ۷)
            
            // ثبت در رویدادها
            logDao.insertLog(ActivityLogEntity(username = userName, action = "تولید همزمان چندمدلی لایحه حقوقی ${_requestType.value} با موتور اورکستراتور مستقل و وب‌سرویس جمینای بومی", date = "۱۶ خرداد ۱۴۰۵"))
        }
    }

    // ذخیره فیزیکی پرونده در دیتابیس
    fun saveGeneratedCaseToDb(title: String, userName: String, onFinished: () -> Unit) {
        viewModelScope.launch {
            caseDao.insertCase(
                CaseEntity(
                    type = _requestType.value,
                    title = title.ifBlank { "پرونده ${_requestType.value} - " + getPersianDateNow() },
                    description = _caseDescription.value,
                    plaintiff = _plaintiffName.value,
                    defendant = _defendantName.value,
                    beneficiary = _beneficiaryName.value,
                    legalPosition = _legalPosition.value,
                    suggestedEvidence = _suggestedEvidence.value.joinToString(", "),
                    relief = _requestedRelief.value,
                    confidenceScore = _confidenceScore.value,
                    status = "آماده ابلاغ",
                    gptOutput = _gptOutput.value,
                    claudeOutput = _claudeOutput.value,
                    geminiOutput = _geminiOutput.value,
                    deepSeekOutput = _deepSeekOutput.value,
                    unifiedOutput = _unifiedOutput.value,
                    date = getPersianDateNow()
                )
            )
            logDao.insertLog(ActivityLogEntity(username = userName, action = "ذخیره‌سازی پرونده حقوقی نهایی با نام $title در کارتابل ملی", date = getPersianDateNow()))
            
            // ریست کردن فرم
            resetWizard()
            onFinished()
        }
    }

    fun deleteCase(id: Int, userName: String) {
        viewModelScope.launch {
            caseDao.deleteCaseById(id)
            logDao.insertLog(ActivityLogEntity(username = userName, action = "حذف فیزیکی شماره پرونده #$id از کارتابل شهروند", date = getPersianDateNow()))
        }
    }

    fun resetWizard() {
        _currentStep.value = 1
        _caseDescription.value = ""
        _plaintiffName.value = ""
        _defendantName.value = ""
        _beneficiaryName.value = ""
        _legalPosition.value = ""
        _confidenceScore.value = 85
        _suggestedEvidence.value = emptyList()
        _requestedRelief.value = ""
        _uploadedFiles.value = emptyList()
        _gptOutput.value = ""
        _claudeOutput.value = ""
        _geminiOutput.value = ""
        _deepSeekOutput.value = ""
        _qwenOutput.value = ""
        _unifiedOutput.value = ""
    }

    private fun getPersianDateNow(): String = "۱۶ خرداد ۱۴۰۵"
}

// ویومدل دستیار شناور همیار حقوقی (Legal Copilot)
class CopilotViewModel : androidx.lifecycle.ViewModel() {
    private val _copilotResponse = MutableStateFlow<String>("")
    val copilotResponse = _copilotResponse.asStateFlow()

    private val _isCopilotThinking = MutableStateFlow(false)
    val isCopilotThinking = _isCopilotThinking.asStateFlow()

    private val _isCopilotOpen = MutableStateFlow(false)
    val isCopilotOpen = _isCopilotOpen.asStateFlow()

    fun toggleCopilot() {
        _isCopilotOpen.value = !_isCopilotOpen.value
    }

    fun closeCopilot() {
        _isCopilotOpen.value = false
    }

    fun askCopilot(question: String, contextScreen: String) {
        _isCopilotThinking.value = true
        _copilotResponse.value = ""
        viewModelScope.launch {
            val systemInstruction = "شما همیار و دستیار شناور حقوقی پلتفرم مستقل دادرس هستید. وظیفه شما راهنمایی گام به گام کاربر در این صفحه ('$contextScreen')، توضیح تخصصی اصطلاحات حقوقی، و افزایش سطح دانش حقوقی اوست."
            val response = com.example.network.AiOrchestrator.executeWithFailover("Claude 3.5 Sonnet", question, systemInstruction)
            _copilotResponse.value = response
            _isCopilotThinking.value = false
        }
    }

    fun explainPage(pageName: String) {
        askCopilot("درباره این صفحه به زبان ساده توضیح بده و بگو چطور می‌توانم بهترین بهره را از آن ببرم.", pageName)
    }

    fun explainTerm(term: String) {
        askCopilot("اصطلاح حقوقی '$term' را خیلی خلاصه و ساده در دو خط به من توضیح بده.", "واژه‌نامه شناور")
    }
}
