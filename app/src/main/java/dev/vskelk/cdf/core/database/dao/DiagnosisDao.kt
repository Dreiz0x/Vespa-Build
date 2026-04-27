package dev.vskelk.cdf.core.database.dao

import androidx.room.*
import dev.vskelk.cdf.core.database.entity.DiagnosisEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosisDao {
    @Query("SELECT * FROM diagnosis_table ORDER BY timestamp DESC")
    fun getAllDiagnosis(): Flow<List<DiagnosisEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiagnosis(item: DiagnosisEntity)
}
