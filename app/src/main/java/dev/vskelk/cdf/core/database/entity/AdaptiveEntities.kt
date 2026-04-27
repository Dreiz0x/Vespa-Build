package dev.vskelk.cdf.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val topicId: String,
    val subtemaId: String = "",
    val startTime: Long,
    val endTime: Long? = null,
    val score: Float = 0f
)

@Entity(tableName = "user_topic_mastery")
data class UserTopicMasteryEntity(
    @PrimaryKey val subtemaId: String,
    val topicId: String = "",
    val masteryLevel: Float = 0f,
    val precision: Float = 0f,
    val estadoDominio: Int = 0,
    val lastReviewed: Long = 0
)

@Entity(tableName = "user_gap_log")
data class UserGapLogEntity(
    @PrimaryKey(autoGenerate = true) val gapId: Long = 0,
    val topicId: String,
    val subtemaId: String = "",
    val errorType: String = "",
    val errorCount: Int = 0,
    val timestamp: Long
)
