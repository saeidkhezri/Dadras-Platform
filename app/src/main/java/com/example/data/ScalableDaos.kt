package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScalableDao {
    // Users
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    // Subscriptions
    @Query("SELECT * FROM subscriptions WHERE user_id = :userId LIMIT 1")
    suspend fun getSubscriptionByUserId(userId: Int): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(sub: SubscriptionEntity)

    // Case Documents & Timelines
    @Query("SELECT * FROM case_documents WHERE case_id = :caseId")
    fun getDocumentsForCase(caseId: Int): Flow<List<CaseDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaseDocument(doc: CaseDocumentEntity)

    @Query("SELECT * FROM case_timelines WHERE case_id = :caseId ORDER BY event_date ASC")
    fun getTimelineForCase(caseId: Int): Flow<List<CaseTimelineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCaseTimeline(timeline: CaseTimelineEntity)

    // Laws & Articles
    @Query("SELECT * FROM laws")
    fun getAllLaws(): Flow<List<LawEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLaw(law: LawEntity): Long

    @Query("SELECT * FROM law_articles WHERE law_id = :lawId")
    fun getArticlesForLaw(lawId: Int): Flow<List<LawArticleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLawArticle(article: LawArticleEntity)

    // Unification Decisions, Advisory Opinions, Judicial Decisions
    @Query("SELECT * FROM unification_decisions")
    fun getAllUnificationDecisions(): Flow<List<UnificationDecisionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUnificationDecision(decision: UnificationDecisionEntity)

    @Query("SELECT * FROM advisory_opinions")
    fun getAllAdvisoryOpinions(): Flow<List<AdvisoryOpinionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdvisoryOpinion(opinion: AdvisoryOpinionEntity)

    @Query("SELECT * FROM judicial_decisions")
    fun getAllJudicialDecisions(): Flow<List<JudicialDecisionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJudicialDecision(decision: JudicialDecisionEntity)

    // Document Imports
    @Query("SELECT * FROM document_imports ORDER BY import_date DESC")
    fun getAllImports(): Flow<List<DocumentImportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocumentImport(documentImport: DocumentImportEntity): Long

    // Registry
    @Query("SELECT * FROM official_sources_registry")
    fun getAllRegisteredSources(): Flow<List<OfficialSourcesRegistryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOfficialSource(source: OfficialSourcesRegistryEntity)

    // Vector Store (RAG Search)
    @Query("SELECT * FROM vector_documents")
    suspend fun getVectorDocuments(): List<VectorDocumentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVectorDocument(vecDoc: VectorDocumentEntity)

    // AI Outputs
    @Query("SELECT * FROM ai_outputs WHERE case_id = :caseId")
    fun getOutputsForCase(caseId: Int): Flow<List<AiOutputEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiOutput(output: AiOutputEntity): Long

    // Evaluations
    @Query("SELECT * FROM legal_evaluations WHERE output_id = :outputId LIMIT 1")
    suspend fun getEvaluationForOutput(outputId: Int): LegalEvaluationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvaluation(eval: LegalEvaluationEntity)

    // User Feedback
    @Query("SELECT * FROM user_feedback WHERE output_id = :outputId")
    fun getFeedbackForOutput(outputId: Int): Flow<List<UserFeedbackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: UserFeedbackEntity)

    // Audit Logs
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)
}
