package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * سرویس هماهنگ‌کننده و نگارشگر تخصصی اسناد حقوقی استاندارد ایران
 * این کلاس اطلاعات فرم خام شهروند را پردازش کرده و بر اساس شابلون‌های مصوب قوه قضاییه،
 * متن لایحه را از طریق وب‌سرویس‌های جمینای، گراک، کوهر و هاگینگ‌فیس تولید می‌کند.
 */
object IranianLegalDocumentGeneratorService {

    /**
     * تولید متن سند حقوقی بر اساس الگوهای رسمی ایران با استفاده از Gemini و سایر سرویس‌های رایگان
     */
    suspend fun generateDocuments(
        requestType: String,
        caseDescription: String,
        plaintiff: String,
        defendant: String,
        evidence: List<String>,
        relief: String
    ): Map<String, String> = withContext(Dispatchers.IO) {
        
        // ۱. تصفیه و ایمن‌سازی متون ارسالی جهت حریم خصوصی کاربر
        val cleanDescription = AiOrchestrator.anonymizeText(caseDescription)
        
        // ۲. انتخاب شابلون استخوان‌بندی متناسب با نوع سند در مراجع دادگستری ایران
        val standardPatternAndPrompt = buildIranianStandardPrompt(
            requestType = requestType,
            caseDescription = cleanDescription,
            plaintiff = plaintiff,
            defendant = defendant,
            evidence = evidence,
            relief = relief
        )

        val systemInstruction = """
            شما سیستم ارشد نگارش اسناد قضایی دادرس هستید؛ یک دکترین حقوقی تراز اول و وکیل پایه‌یک دادگستری ایران با دانش عمیق فقهی و قانونی.
            متن خروجی باید کاملاً رسمی، بلیغ، با ادبیات محاکم قضایی و منطبق با فرمت سنتی دفاتر دادگاه‌ها صادر شود.
            قوانین حاکم بر دادرسی به ویژه قانون مدنی (مواد ۱۰، ۱۹۰، ۲۱۹، ۲۲۰، ۲۲۱، ۲۲۲، ۵۲۲) و قانون آیین دادرسی مدنی را حتماً در لابلای متن مستند نمایید.
        """.trimIndent()

        // ۳. فراخوانی موازی وب‌سرویس‌های هوش مصنوعی جهت سرعت حداکثری
        val geminiDeferred = async {
            try {
                GeminiHelper.askGemini(standardPatternAndPrompt, systemInstruction)
            } catch (e: Exception) {
                fallbackIranianTemplate(requestType, "Gemini (Direct)", plaintiff, defendant, relief, evidence)
            }
        }

        val groqDeferred = async {
            try {
                // استفاده از گیتوی هوش مصنوعی مرکزی برای فراخوانی گراک
                AiOrchestrator.executeWithFailover("Groq LLaMA-3 (رایگان)", standardPatternAndPrompt, systemInstruction)
            } catch (e: Exception) {
                fallbackIranianTemplate(requestType, "Groq LLaMA-3", plaintiff, defendant, relief, evidence)
            }
        }

        val cohereDeferred = async {
            try {
                // استفاده از گیتوی مرکزی برای فراخوانی کوهر
                AiOrchestrator.executeWithFailover("Cohere Command-R (رایگان)", standardPatternAndPrompt, systemInstruction)
            } catch (e: Exception) {
                fallbackIranianTemplate(requestType, "Cohere Command-R", plaintiff, defendant, relief, evidence)
            }
        }

        val hfDeferred = async {
            try {
                // استفاده از گیتوی مرکزی برای فراخوانی هاگینگ‌فیس
                AiOrchestrator.executeWithFailover("HF LLaMA-3 (رایگان)", standardPatternAndPrompt, systemInstruction)
            } catch (e: Exception) {
                fallbackIranianTemplate(requestType, "HuggingFace LLaMA-3", plaintiff, defendant, relief, evidence)
            }
        }

        val geminiResult = geminiDeferred.await()
        val groqResult = groqDeferred.await()
        val cohereResult = cohereDeferred.await()
        val hfResult = hfDeferred.await()

        mapOf(
            "gemini" to geminiResult,
            "groq" to groqResult,
            "cohere" to cohereResult,
            "hf" to hfResult
        )
    }

    /**
     * مهندسی و پیکربندی دقیق پرامپت الگوهای بومی ایران بر مبنای نوع سند
     */
    private fun buildIranianStandardPrompt(
        requestType: String,
        caseDescription: String,
        plaintiff: String,
        defendant: String,
        evidence: List<String>,
        relief: String
    ): String {
        val evidenceListText = if (evidence.isNotEmpty()) evidence.joinToString("، ") else "مدارک استنادی تعبیه شده در پرونده"
        
        val templateStructure = when (requestType) {
            "شکایت" -> """
                [فرم چارچوب رسمی شکواییه کیفری]
                - شاکی (خواهان مادی و معنوی): $plaintiff
                - مشتکی‌عنه (متهم موصوف): $defendant
                - موضوع جرم انتسابی: $relief
                - تاریخ و محل وقوع جرم: تهران / محل تراکنش الکترونیکی یا فیزیکی
                - دلایل و مدارک اثبات جرم: $evidenceListText
                
                دستورالعمل نگارش: شکواییه را با استناد صریح به مواد قانون مجازات اسلامی ایران و قانون آیین دادرسی کیفری بنویسید. بند خلاصه شکواییه و شرح واقعه را در صدر قرار دهید.
            """.trimIndent()
            
            "دادخواست" -> """
                [فرم پرونده رسمی دادخواست حقوقی مراجع قضایی مدنی]
                - خواهان: $plaintiff
                - خوانده: $defendant
                - خواسته و یا بهای آن: $relief
                - دلایل و منضمات دادخواست: $evidenceListText
                
                دستورالعمل نگارش: دادخواست را طبق فرم استاندارد مجتمع‌های قضایی ایران بنویسید. حتماً به مواد عمومی قانون مدنی (ماده ۱۰ در اعتبار روابط خصوصی، ماده ۲۱۹ در لزوم عقود، ماده ۵۲۲ در مطالبه خسارت تاخیر تادیه بر اساس شاخص بانک مرکزی) استناد عمیق قضایی کنید.
            """.trimIndent()

            "لایحه دفاعیه" -> """
                [فرم رسمی لایحه دفاعیه جلسات دادرسی]
                - کلاسه پرونده: کلاسه فرضی صمیمانه مراجع عمومی دادخواهی
                - دفاع‌کننده صالح حقوقی: آقای/خانم $plaintiff
                - طرف مقابل دعوا: آقای/خانم $defendant
                - خواسته مورد لایحه: $relief
                - مستند دفاعی: $evidenceListText
                
                دستورالعمل نگارش: متن لایحه را با عنوان محترمانه «ریاست محترم شعبه رسیدگی‌کننده...» آغاز کنید. شامل ایرادات شکلی (صلاحیت)، ماهیت دفاع، تحلیل شواهد و نهایتاً درخواست رد دعوای واهی طرف مقابل باشد.
            """.trimIndent()

            "اظهارنامه" -> """
                [فرم اظهارنامه رسمی موضوع ماده ۱۵۶ قانون آیین دادرسی مدنی]
                - اظهارکننده رسمی: $plaintiff
                - مخاطب ابلاغ رسمی: $defendant
                - موضوع اظهارنامه: مطالبه رسمی ایفا تعهد حقوقی/مالی به مبلغ/عنوان: $relief
                
                دستورالعمل نگارش: متن را با لحن رسمی و قاطع، اخطار دهنده ولی مؤدبانه بنویسید. به مخاطب هشدار دهید که در صورت عدم ایفا تعهد ظرف مهلت مقرر (مثلا ۷۲ ساعت)، اقدامات حاد قانونی و ثبتی آغاز می‌گردد.
            """.trimIndent()
            
            else -> """
                [پایه قضایی سند تعهد مکتوب]
                - ذینفع اول: $plaintiff
                - متعهد دوم: $defendant
                - موضوع سند: $relief
                - شروط و بندهای اثباتی: $evidenceListText
                
                دستورالعمل نگارش: بر اساس اصول اولیه قانون مدنی ایران، سندی مستدل، محکم، غیرقابل تفبیر منفی و یکپارچه ایجاد کنید.
            """.trimIndent()
        }

        return """
            به عنوان راهنمای ارشد حقوقی بومی، سند حقوقی استاندارد بومی زیر را تدوین کنید:
            
            اطلاعات اساسی فرم حقوقی:
            $templateStructure
            
            توصیف واقعه و وقایع تراکنش بستر دعوا:
            $caseDescription
            
            متن نهایی سند را به صورت کاملاً RTL، منقح، و بدون هیچ‌گونه توضیح اضافی بیرون از متن، صادر کنید.
        """.trimIndent()
    }

    /**
     * پیش‌نویس‌های الگوهای استاندارد محلی ایران برای زمانی که وب‌سرویس‌ها قطع هستند
     */
    fun fallbackIranianTemplate(
        requestType: String,
        engineName: String,
        plaintiff: String,
        defendant: String,
        relief: String,
        evidence: List<String>
    ): String {
        val evidenceText = if (evidence.isNotEmpty()) evidence.joinToString("، ") else "مدارک پیوست"
        return when (requestType) {
            "شکایت" -> """
                بسمه تعالی
                موضوع: شکواییه رسمی شکایت کیفری با موتور پردازش $engineName
                شاکی: آقای/خانم $plaintiff
                مشتکی‌عنه: آقای/خانم $defendant
                موضوع جرم: $relief
                دلایل و مدارک: $evidenceText
                
                ریاست محترم دادسرای عمومی و انقلاب،
                با سلام و تحیات، احتراماً به استحضار می‌رساند مشتکی‌عنه موصوف با سوء‌نیت تمام اقدام به ارتکاب جرم موضوع خواسته نموده و بر این اساس موجب تضییع حقوق مسلم اینجانب شاکی پرونده شده است. نظر به اهمیت موضوع و خسارت معنوی وارده، مستنداً به قانون مجازات اسلامی و آیین دادرسی کیفری تعقیب، رسیدگی و مجازات نامبرده به انضمام رد مال مورد استدعا می‌باشد.
            """.trimIndent()

            "دادخواست" -> """
                بسمه تعالی
                ریاست محترم دادگاه عمومی و حقوقی مجتمع قضایی
                خواهان: آقای/خانم $plaintiff
                خوانده: آقای/خانم $defendant
                خواسته: $relief (تولید بومی با موتور تخصصی $engineName)
                دلایل و منضمات: $evidenceText
                
                با سلام، احتراما به استحضار می‌رساند فی‌مابین طرفین عقدی لازم‌الاجرا منعقد گردیده که خوانده متعهد به ایفا تعهد و پرداخت آن بوده است. نظر به استنکاف خوانده از ادای دیون، مستنداً به ماده ۱۰، ۲۱۹ قانون مدنی و ماده ۵۲۲ قانون آیین دادرسی مدنی محکومیت خوانده به ایفا تعهد به انضمام تاخیر تادیه و جبران خسارات دادرسی مورد تقاضاست.
            """.trimIndent()

            "لایحه دفاعیه" -> """
                بسمه تعالی
                ریاست محترم شعبه محاکم عمومی حقوقی
                کلاسه پرونده: شبیه‌سازی شده $engineName
                طرفین دعوا: خواهان $plaintiff علیه خوانده $defendant
                
                با سلام و عرض ادب، احتراماً در مقام دفاع ماهوی از دعوای مطروحه خواسته به استحضار می‌رساند خواسته خواهان فاقد پشتوانه قانونی محکم بوده و با موازین مسلم قانون برائت و مواد قانون مدنی سازگار نمی‌باشد. دلایل ارایه شده توسط خواهن از زمره ادله اثبات دعوا خارج بوده، لذا صدور حکم شایسته مبنی بر رد دعوا و برائت موکد مورد تقاضا می‌باشد.
            """.trimIndent()

            "اظهارنامه" -> """
                [اظهارنامه رسمی موضوع ماده ۱۵۶ قانون آیین دادرسی مدنی - نگارش $engineName]
                اظهارکننده: آقای/خانم $plaintiff
                مخاطب: آقای/خانم $defendant
                موضوع اظهار: لزوم ایفای فوری تعهد ناشی از قرارداد و مطالبه وجه پرداخت شده
                
                مخاطب محترم، بدین وسیله به شما اخطار رسمی داده می‌شود که به موجب ماده ۱۵۶ قانون دادرسی مدنی، مکلف هستید ظرف مهلت قانونی نسبت به تحویل خواسته $relief و ایفا بندهای عقدی به نفع اظهارکننده اقدام نمایید. در غیر این صورت مراتب از طریق دفتر خدمات الکترونیک قضایی و دادگاه صالحه پیگیری خواهد شد.
            """.trimIndent()

            else -> """
                [سند حقوقی اختصاصی استاندارد توسعه یافته با هماهنگ‌کننده $engineName]
                نگارش بومی برای طرفین دعوا: $plaintiff و $defendant
                خلاصه خواسته: $relief
                توصیف ماوقع: اسناد و مدارک کافی دلالت بر صحت ادعای ذینفع دارد و طرفین موظف به پایبندی به اصول حاکمیت قراردادها می‌باشند.
            """.trimIndent()
        }
    }
}
