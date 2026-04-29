package dev.vskelk.cdf.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ontology_nodes")
data class OntologyNodeEntity(
    @PrimaryKey val id: Long,
    val nodeType: String,
    val name: String,
    val description: String? = null,
    val parentId: Long? = null,
    val weight: Float = 1.0f,
    val displayOrder: Int = 0,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
