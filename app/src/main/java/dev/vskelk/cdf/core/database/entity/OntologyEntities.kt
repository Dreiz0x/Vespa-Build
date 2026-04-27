package dev.vskelk.cdf.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// No change needed

@Entity(tableName = "ontology_relations")
data class OntologyRelationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceNodeId: String,
    val targetNodeId: String,
    val relationType: String,
    val displayOrder: Int = 0
)

@Entity(tableName = "cargos")
data class CargoEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val descripcion: String,
    val isDefault: Boolean = false
)

@Entity(tableName = "organos")
data class OrganoEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val tipo: String
)
