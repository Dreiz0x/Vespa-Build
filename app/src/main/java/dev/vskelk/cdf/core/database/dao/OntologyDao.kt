package dev.vskelk.cdf.core.database.dao

import androidx.room.*
import dev.vskelk.cdf.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OntologyDao {

    @Query("SELECT * FROM ontology_nodes WHERE id = :nodeId")
    suspend fun getNodeById(nodeId: Long): OntologyNodeEntity?

    @Query("SELECT * FROM ontology_nodes WHERE nodeType = :type")
    fun getNodesByType(type: String): Flow<List<OntologyNodeEntity>>

    @Query("SELECT * FROM ontology_nodes WHERE parentId = :parentId")
    fun getChildNodes(parentId: Long): Flow<List<OntologyNodeEntity>>

    @Query("SELECT * FROM ontology_nodes WHERE nodeType = 'SUBTEMA'")
    fun getSubtemas(): Flow<List<OntologyNodeEntity>>

    @Query("SELECT * FROM ontology_nodes WHERE name LIKE '%' || :query || '%'")
    suspend fun searchNodes(query: String): List<OntologyNodeEntity>

    @Query("SELECT n.* FROM ontology_nodes n INNER JOIN ontology_relations r ON n.id = r.targetNodeId WHERE r.sourceNodeId = :nodeId AND r.relationType = :relationType")
    fun getRelatedNodes(nodeId: Long, relationType: String): Flow<List<OntologyNodeEntity>>

    @Query("SELECT COUNT(*) FROM ontology_nodes WHERE isActive = 1")
    suspend fun getActiveNodeCount(): Int

    @Query("SELECT * FROM ontology_nodes WHERE isActive = 1")
    fun observeAllNodes(): Flow<List<OntologyNodeEntity>>

    @Transaction
    @Query("SELECT n.*, m.estadoDominio, m.precision, m.totalIntentos, m.velocidadPromedio FROM ontology_nodes n LEFT JOIN user_topic_mastery m ON n.id = m.subtemaId WHERE n.nodeType = :type")
    fun getSubtopicsWithDomainStatus(type: String): Flow<List<OntologyNodeWithMastery>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: OntologyNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCargo(cargo: CargoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelation(relation: OntologyRelationEntity)

    @Query("DELETE FROM ontology_nodes")
    suspend fun clearNodes()

    @Query("DELETE FROM ontology_relations")
    suspend fun clearRelations()
}

data class OntologyNodeWithMastery(
    @Embedded val node: OntologyNodeEntity,
    @ColumnInfo(name = "estadoDominio") val estadoDominio: String?,
    @ColumnInfo(name = "precision") val precision: Float?,
    @ColumnInfo(name = "totalIntentos") val totalIntentos: Int?,
    @ColumnInfo(name = "velocidadPromedio") val velocidadPromedio: Float?
)
