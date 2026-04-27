package dev.vskelk.cdf.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ontology_nodes")
data class OntologyNodeEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nodeType: String = "",
    val parentId: String? = null,
    val cargoId: String? = null,
    val isActive: Boolean = true,
    val description: String = "",
    val subtemaId: String = ""
)

@Entity(tableName = "ontology_relations")
data class OntologyRelationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceNodeId: String,
    val targetNodeId: String,
    val relationType: String
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
