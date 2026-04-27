package dev.vskelk.cdf.core.database.dao

import androidx.room.*
import dev.vskelk.cdf.core.database.entity.CuarentenaFragmentoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface QuarantineDao {

    @Query("SELECT * FROM quarantine_table WHERE status = 'PENDIENTE' ORDER BY timestamp DESC")
    fun observePendientes(): Flow<List<CuarentenaFragmentoEntity>>

    @Query("SELECT * FROM quarantine_table WHERE status = 'CONFLICTO' ORDER BY timestamp DESC")
    fun observeConflictos(): Flow<List<CuarentenaFragmentoEntity>>

    @Query("SELECT COUNT(*) FROM quarantine_table WHERE status = 'PENDIENTE'")
    fun observePendienteCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM quarantine_table WHERE status = 'CONFLICTO'")
    fun observeConflictoCount(): Flow<Int>

    @Query("SELECT * FROM quarantine_table WHERE id = :fragmentoId LIMIT 1")
    suspend fun getFragmentoById(fragmentoId: Long): CuarentenaFragmentoEntity?

    @Query("UPDATE quarantine_table SET status = 'APROBADO' WHERE id = :fragmentoId")
    suspend fun approveFragmento(fragmentoId: Long)

    @Query("UPDATE quarantine_table SET status = 'RECHAZADO' WHERE id = :fragmentoId")
    suspend fun rejectFragmento(fragmentoId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFragmento(item: CuarentenaFragmentoEntity)
}
