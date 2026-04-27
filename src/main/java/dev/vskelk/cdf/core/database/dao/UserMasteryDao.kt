package dev.vskelk.cdf.core.database.dao

import androidx.room.*
import dev.vskelk.cdf.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMasteryDao {

    @Query("SELECT * FROM user_topic_mastery WHERE topicId = :topicId")
    suspend fun getTopicMasteryByTopicId(topicId: String): UserTopicMasteryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopicMastery(userTopicMastery: UserTopicMasteryEntity)

    @Update
    suspend fun updateTopicMastery(userTopicMastery: UserTopicMasteryEntity)

    @Query("SELECT * FROM user_topic_mastery WHERE userId = :userId")
    fun getUserTopicMasteries(userId: String): Flow<List<UserTopicMasteryEntity>>

    @Query("SELECT * FROM user_topic_mastery WHERE topicId = :topicId")
    fun getUserTopicMasteryByTopicIdFlow(topicId: String): Flow<UserTopicMasteryEntity?>

    @Query("SELECT COUNT(*) FROM user_topic_mastery WHERE estadoDominio = :estadoDominio")
    fun getCountByEstadoDominio(estadoDominio: String): Flow<Int>

    @Query("SELECT AVG(precision) FROM user_topic_mastery WHERE topicId = :topicId")
    fun getAveragePrecision(topicId: String): Flow<Double?>

    @Query("SELECT SUM(totalIntentos) FROM user_topic_mastery WHERE topicId = :topicId")
    fun getTotalIntentos(topicId: String): Flow<Int?>

    @Query("SELECT AVG(velocidadPromedio) FROM user_topic_mastery WHERE topicId = :topicId")
    fun getAverageVelocidadPromedio(topicId: String): Flow<Double?>


    // UserGapLogEntity methods
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGapLog(userGapLogEntity: UserGapLogEntity)

    @Query("SELECT * FROM user_gap_logs")
    suspend fun getAllUserGapLogs(): List<UserGapLogEntity>

    @Query("SELECT * FROM user_gap_logs WHERE topicId = :topicId")
    fun getUserGapLogsByTopicIdFlow(topicId: String): Flow<List<UserGapLogEntity>>

    @Query("SELECT errorType, COUNT(*) as count FROM user_gap_logs GROUP BY errorType")
    fun getErrorTypeCount(): Flow<List<ErrorTypeCount>>

    @Query("SELECT COUNT(*) FROM user_gap_logs")
    fun getStudySessionCount(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT topicId) FROM user_gap_logs")
    fun getFragmentCount(): Flow<Int>

    @Query("SELECT * FROM user_gap_logs WHERE topicId = :topicId AND errorType = :errorType")
    suspend fun getUserGapLogByTopicAndError(topicId: String, errorType: String): UserGapLogEntity?
}

data class ErrorTypeCount(
    @ColumnInfo(name = "errorType") val errorType: String,
    @ColumnInfo(name = "count") val count: Int
)
