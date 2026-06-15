package com.example.ui.components

import android.content.Context
import android.widget.Toast
import java.util.Calendar

object PersianFirstUtils {

    // تبدیل اعداد انگلیسی به فارسی به صورت پویا بر اساس تنظیمات ادمین
    fun formatDigits(text: String, usePersian: Boolean = true): String {
        if (!usePersian) return text
        return text.map { char ->
            when (char) {
                '0' -> '۰'
                '1' -> '۱'
                '2' -> '۲'
                '3' -> '۳'
                '4' -> '۴'
                '5' -> '۵'
                '6' -> '۶'
                '7' -> '۷'
                '8' -> '۸'
                '9' -> '۹'
                else -> char
            }
        }.joinToString("")
    }

    // تبدیل تاریخ میلادی به هجری شمسی دقیق بر اساس الگوریتم ریاضی تقویم جلالی
    fun getSolarHijriDate(year: Int, month: Int, day: Int): String {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        var gy = year - 1600
        var gm = month - 1
        var gd = day - 1

        var gDayNo = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
        for (i in 0 until gm) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var i = 0
        while (i < 12 && jDayNo >= jDaysInMonth[i]) {
            jDayNo -= jDaysInMonth[i]
            i++
        }
        val jm = i + 1
        val jd = jDayNo + 1

        val shamsiMonths = listOf(
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        )

        return "$jd ${shamsiMonths[jm - 1]} $jy"
    }

    // گرفتن تاریخ شمسی جاری سیستم
    fun getCurrentShamsiDate(): String {
        val cal = Calendar.getInstance()
        return getSolarHijriDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    // شبیه‌سازی دقیقOCR اسناد حقوقی و مراجع قضایی
    data class OcrTemplate(
        val docName: String,
        val extractedTitle: String,
        val extractedContent: String
    )

    val ocrDocTemplates = listOf(
        OcrTemplate(
            docName = "تصویر دادنامه مهریه زوجین.jpg",
            extractedTitle = "رأی محکومیت پرداخت مهریه شعبه ۱۰۲ خانواده",
            extractedContent = "محکوم علیه آقا/خانم متعهد به تادیه تعداد ۱۱۰ عدد سکه تمام بهار آزادی بابت صداق زوجه می‌باشد. مستنداً به ماده ۱۰۸۲ قانون مدنی زوجه به مجرد عقد مالک مهر می‌شود."
        ),
        OcrTemplate(
            docName = "اسکن صلحنامه تجاری اراضی.pdf",
            extractedTitle = "صلح‌نامه انتقال حقوق تجاری مشاع",
            extractedContent = "توافق گردید کلیه حقوق فرضیه و متصوره مصالح در محدوده ملک تجاری واقع در پلاک ثبتی ۱۲/۳۴۵ به متصالح واگذار شود و اسناد تعهدآور بر همین اساس در دفترخانه تنظیم گردد."
        ),
        OcrTemplate(
            docName = "شرح خواست دادخواست خسارت تادیه.png",
            extractedTitle = "دادخواست مطالبه خسارت تاخیر تادیه دین",
            extractedContent = "موضوع خواسته: مطالبه مبلغ دویست میلیون ریال خسارت تاخیر تادیه مستند به شاخص تورم سالانه بانک مرکزی موضوع ماده ۵۲۲ آیین دادرسی مدنی به همراه هزینه دادرسی."
        )
    )

    // شبیه‌سازی سیستم صوتی ضبط صدا و تبدیل آن به متن حقوقی فارسی
    val mockVoiceQueries = listOf(
        "ماده ۱۰ قانون مدنی در مورد چه چیزی صحبت می‌کند؟",
        "آیا خسارت تاخیر تادیه بر اساس شاخص بانک مرکزی محاسبه می‌شود؟",
        "مدارک لازم برای اثبات صلح‌نامه ثبت نشده چیست؟",
        "آخرین رأی وحدت رویه دیوان عالی کشور درباره مهریه",
        "شرایط فسخ معامله در قانون تجارت به علت ورشکستگی"
    )

    // خروجی اسناد متنی فارسی به شیوه‌نامه معتبر ایرانی (RTL) همراه با نشان آب و امضا
    fun simulateExportFile(context: Context, resourceTitle: String, content: String, format: String) {
        val shamsi = getCurrentShamsiDate()
        val formattedContent = """
        ==================================================
        [جمهوری اسلامی ایران - قوه قضاییه]
        [سامانه رسمی دادرس هوشمند - نسخه مدیر سیستم]
        ==================================================
        تاریخ صدور خروجی: $shamsi
        منبع استناد: $resourceTitle
        
        متن تبیین شده:
        $content
        
        --------------------------------------------------
        نشان آب پایگاه: "دارای گواهی اصالت الکترونیکی معتبر"
        این سند به صورت خودکار از بایگانی RAG با الگوریتم‌های
        هوش مصنوعی منطبق بر قوانین موضوعه ایران استخراج گردید.
        ==================================================
        """.trimIndent()
        
        Toast.makeText(context, "فایل $resourceTitle با موفقیت به فرمت $format صادر و در پوشه دانلودها ذخیره شد (RTL)", Toast.LENGTH_LONG).show()
    }
}
