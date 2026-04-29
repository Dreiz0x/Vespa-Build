package dev.vskelk.cdf.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val modulo: String = "",
    val examArea: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val correctos: Int = 0,
    val totalReactivos: Int = 0,
    val tiempoPromedioSeg: Float = 0f,
    val weakSubtemas: String? = null,
    val dominantErrors: String? = null
)

@Entity(tableName = "user_topic_mastery")
data class UserTopicMasteryEntity(
    @PrimaryKey val subtemaId: Long,
    val estadoDominio: String = "NO_VISTO",
    val precision: Float = 0f,
    val totalIntentos: Int = 0,
    val velocidadPromedio: Float = 0f,
    val lastReviewed: Long = 0
)

@Entity(tableName = "user_gap_logs")
data class UserGapLogEntity(
    @PrimaryKey(autoGenerate = true) val gapId: Long = 0,
    val subtemaId: Long,
    val errorType: String = "",
    val reactivoId: Long = 0,
    val sessionId: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)
