package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {
    @Query("SELECT * FROM cases ORDER BY timestamp DESC")
    fun getAllCases(): Flow<List<CaseEntity>>

    @Query("SELECT * FROM cases WHERE id = :id LIMIT 1")
    suspend fun getCaseById(id: Int): CaseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCase(caseEntity: CaseEntity): Long

    @Update
    suspend fun updateCase(caseEntity: CaseEntity)

    @Delete
    suspend fun deleteCase(caseEntity: CaseEntity)

    @Query("DELETE FROM cases WHERE id = :id")
    suspend fun deleteCaseById(id: Int)
}

@Dao
interface ActivityLogDao {
    @Query("SELECT * FROM activity_logs ORDER BY timestamp DESC LIMIT 100")
    fun getAllLogs(): Flow<List<ActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ActivityLogEntity)

    @Query("DELETE FROM activity_logs")
    suspend fun clearLogs()
}

@Dao
interface LegalResourceDao {
    @Query("SELECT * FROM legal_resources ORDER BY id ASC")
    fun getAllResources(): Flow<List<LegalResourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResources(resources: List<LegalResourceEntity>)

    @Query("DELETE FROM legal_resources")
    suspend fun clearResources()
}
