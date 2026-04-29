package dev.vskelk.cdf.core.database.dao

import androidx.room.*
import dev.vskelk.cdf.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMasteryDao {

    @Query("SELECT * FROM user_topic_mastery WHERE subtemaId = :subtemaId")
    suspend fun getMasteryBySubtemaId(subtemaId: Long): UserTopicMasteryEntity?

    @Query("SELECT * FROM user_topic_mastery WHERE estadoDominio = :estado")
    fun getMasteryByState(estado: String): Flow<List<UserTopicMasteryEntity>>

    @Query("SELECT * FROM user_topic_mastery WHERE estadoDominio IN (:estados)")
    fun getMasteryByStates(estados: List<String>): Flow<List<UserTopicMasteryEntity>>

    @Query("SELECT * FROM user_topic_mastery")
    fun observeAllMastery(): Flow<List<UserTopicMasteryEntity>>

    @Query("SELECT COUNT(*) FROM user_topic_mastery")
    suspend fun getMasteryCount(): Int

    @Query("SELECT * FROM user_topic_mastery WHERE precision < :threshold ORDER BY precision ASC LIMIT :limit")
    suspend fun getWeakSubtemas(threshold: Float, limit: Int): List<UserTopicMasteryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMastery(mastery: UserTopicMasteryEntity)

    @Update
    suspend fun updateMastery(mastery: UserTopicMasteryEntity)

    @Query("UPDATE user_topic_mastery SET precision = :precision, totalIntentos = totalIntentos + 1, velocidadPromedio = :velocidadPromedio, estadoDominio = :estadoDominio, lastReviewed = :timestamp WHERE subtemaId = :subtemaId")
    suspend fun recordAttemptAndUpdateMastery(
        subtemaId: Long,
        precision: Float,
        velocidadPromedio: Float,
        estadoDominio: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGapLog(gapLog: UserGapLogEntity)

    @Query("SELECT * FROM user_gap_logs WHERE subtemaId = :subtemaId")
    fun getGapLogsBySubtema(subtemaId: Long): Flow<List<UserGapLogEntity>>

    @Query("SELECT errorType, COUNT(*) as count FROM user_gap_logs GROUP BY errorType ORDER BY count DESC LIMIT :limit")
    suspend fun getGlobalErrorTypeCounts(limit: Int = 10): List<ErrorTypeCount>

    @Query("SELECT COUNT(DISTINCT subtemaId) FROM user_gap_logs")
    fun observeAffectedSubtemaCount(): Flow<Int>

    @Query("SELECT * FROM user_gap_logs")
    suspend fun getAllUserGapLogs(): List<UserGapLogEntity>
}

data class ErrorTypeCount(
    @ColumnInfo(name = "errorType") val errorType: String,
    @ColumnInfo(name = "count") val count: Int
)
