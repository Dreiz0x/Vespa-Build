package dev.vskelk.cdf.core.database.dao

import androidx.room.*
import dev.vskelk.cdf.core.database.entity.UserGapLogEntity
import dev.vskelk.cdf.core.database.entity.UserTopicMasteryEntity
import kotlinx.coroutines.flow.Flow

data class ErrorTypeCount(
    val errorType: String,
    val count: Int
)

@Dao
interface UserMasteryDao {
    @Query("SELECT * FROM user_topic_mastery WHERE subtemaId = :subtemaId")
    suspend fun getMasteryBySubtema(subtemaId: String): UserTopicMasteryEntity?

    @Query("SELECT * FROM user_topic_mastery")
    fun getAllMastery(): Flow<List<UserTopicMasteryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMastery(mastery: UserTopicMasteryEntity)

    @Query("SELECT COUNT(*) FROM user_gap_log WHERE topicId = :topicId")
    fun getErrorCountByTopic(topicId: String): Flow<Int>

    @Query("SELECT errorType, COUNT(*) as count FROM user_gap_log GROUP BY errorType")
    fun getErrorTypeStats(): Flow<List<ErrorTypeCount>>
}
