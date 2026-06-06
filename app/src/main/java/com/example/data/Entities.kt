package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cases")
data class CaseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String,               // مثلاً: شکایت، دادخواست، لایحه دفاعیه
    val title: String,
    val description: String,
    val plaintiff: String = "",       // شاکی / خواهان
    val defendant: String = "",       // متهم / خوانده
    val beneficiary: String = "",     // ذینفع
    val legalPosition: String = "",   // عنوان اتهام / عنوان حقوقی
    val suggestedEvidence: String = "", // دلایل پیشنهادی (JSON یا متنی جدا شده با ویرگول)
    val relief: String = "",          // خواسته‌ها
    val confidenceScore: Int = 85,    // درصد اطمینان 0-100
    val status: String = "جاری",       // جاری، نهایی‌شده، پیش‌نویس
    val gptOutput: String = "",
    val claudeOutput: String = "",
    val geminiOutput: String = "",
    val deepSeekOutput: String = "",
    val unifiedOutput: String = "",
    val date: String = "",            // مثلاً: ۱۶ خرداد ۱۴۰۵
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val action: String,               // شرح فعالیت انجام شده
    val date: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "legal_resources")
data class LegalResourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,             // جزایی، حقوقی، خانواده، تجاری
    val description: String,
    val content: String,
    val articleNo: String = ""        // شماره ماده قانونی
)
