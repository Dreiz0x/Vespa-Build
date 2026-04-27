package dev.vskelk.cdf.core.database.dao

import androidx.room.*
import dev.vskelk.cdf.core.database.entity.QuarantineEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuarantineDao {
    @Query("SELECT * FROM quarantine_table ORDER BY timestamp DESC")
    fun getAllQuarantine(): Flow<List<QuarantineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuarantine(item: QuarantineEntity)

    @Query("DELETE FROM quarantine_table WHERE id = :itemId")
    suspend fun deleteById(itemId: Long)
}
