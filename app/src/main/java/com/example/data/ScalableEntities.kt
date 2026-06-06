package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// === 1. AUTHENTICATION TABLES ===

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val username: String,
    val email: String,
    val mobile: String,
    val password_hash: String,
    val role_id: Int,
    val status: String, // active, draft, blocked
    val created_at: String,
    val updated_at: String,
    val last_login: String
)

@Entity(tableName = "roles")
data class RoleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String // admin, citizen, lawyer, judge, supervisor
)

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val user_id: Int,
    val plan_name: String,
    val start_date: String,
    val end_date: String,
    val status: String // active, expired, canceled
)

// === 2. CASE MANAGEMENT (cases, case_documents, case_timelines) ===

@Entity(tableName = "case_documents")
data class CaseDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val case_id: Int,
    val file_name: String,
    val file_type: String, // pdf, docx, txt, etc.
    val storage_url: String,
    val extracted_text: String,
    val created_at: String
)

@Entity(tableName = "case_timelines")
data class CaseTimelineEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val case_id: Int,
    val event_date: String, // e.g. 1405/03/16
    val event_title: String,
    val event_description: String,
    val related_law_reference: String // e.g. ماده ۵۲۲ آیین دادرسی مدنی
)

// === 3. LEGAL KNOWLEDGE (legal_categories, legal_sources) ===

@Entity(tableName = "legal_categories")
data class LegalCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String // Criminal, Civil, Family, Commercial, Labor, Administrative, Tax, Property, Cyber Crime
)

@Entity(tableName = "legal_sources")
data class LegalSourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val source_name: String,
    val source_type: String, // Law, Regulation, Directive, Advisory Opinion, Court Decision, Precedent, Official Website
    val official_url: String,
    val reliability_score: Int, // 1 - 100
    val active_status: Boolean
)

// === 4. LAWS (laws, law_articles) ===

@Entity(tableName = "laws")
data class LawEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val law_title: String,
    val law_category: String, // e.g. Civil, criminal
    val official_source: String,
    val effective_date: String,
    val version: String,
    val status: String // active, legislative, repealed
)

@Entity(tableName = "law_articles")
data class LawArticleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val law_id: Int,
    val article_number: String,
    val article_title: String,
    val article_text: String,
    val keywords: String, // comma-separated strings
    val vector_embedding: String // comma-separated float representation of LLM embeddings (RAG)
)

// === 5. UNIFICATION DECISIONS ===

@Entity(tableName = "unification_decisions")
data class UnificationDecisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val decision_number: String, // e.g. رأی وحدت رویه شماره ۸۲۸
    val issue_date: String,
    val subject: String,
    val full_text: String,
    val related_articles: String, // comma separated references
    val keywords: String,
    val vector_embedding: String
)

// === 6. ADVISORY OPINIONS ===

@Entity(tableName = "advisory_opinions")
data class AdvisoryOpinionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val opinion_number: String, // e.g. نظریه مشورتی شماره ۷/۱۴۰۱/۱۲۳
    val issue_date: String,
    val subject: String,
    val full_text: String,
    val keywords: String,
    val vector_embedding: String
)

// === 7. JUDICIAL DECISIONS (Anonymized) ===

@Entity(tableName = "judicial_decisions")
data class JudicialDecisionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val court_name: String, // e.g. شعبه ۱۰۲ دادگاه کیفری دو تهران
    val subject: String,
    val decision_text: String,
    val anonymized_text: String, // Important: Always anonymized before storage
    val keywords: String,
    val vector_embedding: String
)

// === 8. DOCUMENT IMPORTS ===

@Entity(tableName = "document_imports")
data class DocumentImportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val document_name: String,
    val document_type: String, // Laws, Unification, Advisory, Judicial, Directive, Regulation, Research
    val file_format: String, // PDF, DOCX, TXT, HTML, CSV
    val upload_source: String, // URL or locally uploaded
    val processing_status: String, // Pending, Chunking, EmbeddingGenerated, Completed, Failed
    val import_date: String
)

// === 9. OFFICIAL SOURCES REGISTRY ===

@Entity(tableName = "official_sources_registry")
data class OfficialSourcesRegistryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String, // Official Gazette, Judiciary Sources, Parliament Sources, Legal Portals
    val source_url: String,
    val source_type: String,
    val validation_date: String,
    val status: String // active, offline
)

// === 10. VECTOR SEARCH (Chunk and Semantic RAG Store) ===

@Entity(tableName = "vector_documents")
data class VectorDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val document_type: String, // law_article, unification_decision, advisory_opinion, judicial_decision
    val document_id: Int,
    val chunk_number: Int,
    val chunk_text: String,
    val embedding: String, // comma-separated floats
    val metadata: String // serialized JSON meta data (source, version, etc.)
)

// === 11. AI OUTPUTS ===

@Entity(tableName = "ai_outputs")
data class AiOutputEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val case_id: Int,
    val model_name: String, // GPT, Claude, Gemini, DeepSeek, Qwen
    val output_text: String,
    val score: Int = 0, // LLM confidence score
    val created_at: String
)

// === 12. LEGAL EVALUATIONS ===

@Entity(tableName = "legal_evaluations")
data class LegalEvaluationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val output_id: Int, // references id in ai_outputs
    val accuracy_score: Int, // 0-25 (Legal Accuracy)
    val citation_score: Int, // 0-20 (Citation Quality)
    val logic_score: Int, // 0-20 (Reasoning Quality)
    val completeness_score: Int, // 0-15 (Completeness)
    val final_score: Int // sum of evaluations (0-100) including Persian writing, risk awareness
)

// === 13. FEEDBACK ===

@Entity(tableName = "user_feedback")
data class UserFeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val user_id: Int,
    val output_id: Int,
    val rating: Int, // 1 to 5
    val comments: String
)

// === 14. AUDIT LOGS ===

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val user_id: Int,
    val action_type: String, // Write, Update, Delete, Import, Search
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
