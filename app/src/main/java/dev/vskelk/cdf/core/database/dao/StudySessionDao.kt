package dev.vskelk.cdf.core.database.dao

import androidx.room.*
import dev.vskelk.cdf.core.database.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {

    @Query("SELECT * FROM study_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<StudySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Query("SELECT * FROM study_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: Long): StudySessionEntity?

    @Query("SELECT * FROM study_sessions WHERE completedAt IS NOT NULL ORDER BY completedAt DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<StudySessionEntity>>

    @Query("SELECT COUNT(*) FROM study_sessions WHERE completedAt IS NOT NULL")
    suspend fun getCompletedSessionCount(): Int

    @Query("SELECT COUNT(*) FROM study_sessions WHERE completedAt IS NOT NULL")
    fun observeCompletedSessionCount(): Flow<Int>

    @Query("SELECT AVG(CAST(correctos AS FLOAT) / CAST(totalReactivos AS FLOAT)) FROM study_sessions WHERE completedAt IS NOT NULL AND totalReactivos > 0")
    suspend fun getOverallAccuracy(): Float?

    @Query("UPDATE study_sessions SET completedAt = :completedAt, correctos = :correctos, tiempoPromedioSeg = :tiempoPromedioSeg WHERE sessionId = :sessionId")
    suspend fun completeSession(
        sessionId: Long,
        correctos: Int,
        tiempoPromedioSeg: Float,
        completedAt: Long = System.currentTimeMillis()
    )
}
