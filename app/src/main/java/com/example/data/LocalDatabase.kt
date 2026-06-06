package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CaseEntity::class,
        ActivityLogEntity::class,
        LegalResourceEntity::class,
        UserEntity::class,
        RoleEntity::class,
        SubscriptionEntity::class,
        CaseDocumentEntity::class,
        CaseTimelineEntity::class,
        LegalCategoryEntity::class,
        LegalSourceEntity::class,
        LawEntity::class,
        LawArticleEntity::class,
        UnificationDecisionEntity::class,
        AdvisoryOpinionEntity::class,
        JudicialDecisionEntity::class,
        DocumentImportEntity::class,
        OfficialSourcesRegistryEntity::class,
        VectorDocumentEntity::class,
        AiOutputEntity::class,
        LegalEvaluationEntity::class,
        UserFeedbackEntity::class,
        AuditLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun caseDao(): CaseDao
    abstract fun logDao(): ActivityLogDao
    abstract fun resourceDao(): LegalResourceDao
    abstract fun scalableDao(): ScalableDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "iranian_legal_intel_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateDatabase(database)
                    }
                }
            }

            suspend fun populateDatabase(db: AppDatabase) {
                val dao = db.resourceDao()
                dao.clearResources()
                dao.insertResources(
                    listOf(
                        LegalResourceEntity(
                            title = "ماده ۱۰ قانون مدنی",
                            category = "قانون مدنی",
                            description = "اصل آزادی قراردادها و نفوذ توافق‌های خصوصی در حقوق ایران",
                            content = "قراردادهای خصوصی نسبت به کسانی که آن را منعقد نموده‌اند در صورتی که مخالف صریح قانون نباشد نافذ است. این ماده پایه و اساس آزادی اراده در مناسبات با شهروندان و تنظیم اسناد تجاری و تعهدات است.",
                            articleNo = "۱۰"
                        ),
                        LegalResourceEntity(
                            title = "ماده ۴۷ قانون ثبت اسناد",
                            category = "قانون مدنی",
                            description = "الزام به ثبت صلح‌نامه‌ها، هبه‌نامه‌ها و اجاره‌نامه‌های بلندمدت",
                            content = "در نقاطی که اداره ثبت اسناد و املاک و دفاتر اسناد رسمی موجود بوده و وزارت عدلیه مقتضی بداند، ثبت کلیه صلح‌نامه‌ها و هبه‌نامه‌ها و شرکت‌نامه‌ها اجباری است و عدم ثبت موجب عدم پذیرش در دادگاه‌ها خواهد بود.",
                            articleNo = "۴۷"
                        ),
                        LegalResourceEntity(
                            title = "ماده ۲۲۰ قانون مجازات اسلامی",
                            category = "قانون مجازات",
                            description = "تبیین اصول مسئولیت کیفری و ضمانات اجرای مجازات تعزیری",
                            content = "در جنایات غیرعمدی و مواردی که پرداخت دیه بر عهده عاقله یا بیت‌المال است، چنانچه مرتکب فوت کند، دیه حسب مورد از بیت‌المال یا عاقله مسترد می‌شود. تعیین میزان مسئولیت بر اساس تقصیر و ادله اثبات دعوی است.",
                            articleNo = "۲۲۰"
                        ),
                        LegalResourceEntity(
                            title = "ماده ۳۳۶ قانون مدنی",
                            category = "حقوق خانواده",
                            description = "شناسایی حق نحله و اجرت‌المثل کارهای زوجه در دوران زوجیت",
                            content = "هرگاه زوجه کارهایی را که شرعاً به عهده وی نبوده و عرفاً برای آن کار اجرت‌المثل باشد، به دستور زوج و با عدم قصد تبرع انجام داده باشد و برای دادگاه نیز ثابت شود، دادگاه اجرت‌المثل کارهای انجام شده را محاسبه و به پرداخت آن حکم می‌کند.",
                            articleNo = "۳۳۶"
                        ),
                        LegalResourceEntity(
                            title = "ماده ۵۲۲ قانون آیین دادرسی مدنی",
                            category = "قوانین تجاری",
                            description = "مطالبه خسارت تاخیر تادیه بر اساس شاخص تورم بانک مرکزی",
                            content = "در دعاوی که موضوع آن دین و از نوع وجه رایج کشور باشد، با مطالبه داین و تمکن مدیون، دادگاه با تغییر فاحش شاخص قیمت سالانه از زمان سررسید تا هنگام پرداخت، نرخ تورم اعلامی بانک مرکزی را لحاظ و متناسب با آن حکم صادر می‌نماید.",
                            articleNo = "۵۲۲"
                        )
                    )
                )

                // اضافه کردن یک لاگ اولیه در لاگ‌های سیستمی
                db.logDao().insertLog(
                    ActivityLogEntity(
                        username = "مدیر سیستم",
                        action = "راه‌اندازی اولیه پایگاه داده هوش حقوقی ایران و بارگذاری منابع قانونی پایه",
                        date = "۱۶ خرداد ۱۴۰۵"
                    )
                )
            }
        }
    }
}
