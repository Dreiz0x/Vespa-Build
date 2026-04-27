package dev.vskelk.cdf.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val lastMessageTimestamp: Long // ESTA FALTABA
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val messageId: Long = 0,
    val conversationId: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long
)

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val syncId: Long = 0,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val timestamp: Long
)
