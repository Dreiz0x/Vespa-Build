package dev.vskelk.cdf.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quarantine_table")
data class CuarentenaFragmentoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawContent: String,
    val confidence: Float,
    val spenArea: String,
    val status: String,
    val timestamp: Long
)
