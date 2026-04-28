package dev.vskelk.cdf.core.database.dao

import androidx.room.*
import dev.vskelk.cdf.core.database.entity.OntologyNodeEntity
import dev.vskelk.cdf.core.database.entity.OntologyRelationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OntologyDao {

    @Transaction
    @Query("SELECT * FROM ontology_nodes WHERE id = :nodeId")
    suspend fun getNodeById(nodeId: String): OntologyNodeEntity?

    @Transaction
    @Query("SELECT * FROM ontology_nodes WHERE level = :level")
    suspend fun getNodesByLevel(level: Int): List<OntologyNodeEntity>

    @Transaction
    @Query("SELECT * FROM ontology_nodes WHERE type = :type")
    suspend fun getNodesByType(type: String): List<OntologyNodeEntity>

    @Transaction
    @Query("SELECT n.* FROM ontology_nodes n JOIN ontology_relations r ON n.id = r.targetId WHERE r.sourceId = :nodeId")
    suspend fun getChildrenNodes(nodeId: String): List<OntologyNodeEntity>

    @Transaction
    @Query("SELECT n.* FROM ontology_nodes n JOIN ontology_relations r ON n.id = r.sourceId WHERE r.targetId = :nodeId")
    suspend fun getParentNodes(nodeId: String): List<OntologyNodeEntity>

    @Transaction
    @Query("SELECT n.* FROM ontology_nodes n JOIN ontology_relations r ON n.id = r.targetId WHERE r.sourceId = :nodeId AND r.type = :relationType")
    suspend fun getChildrenNodesByType(nodeId: String, relationType: String): List<OntologyNodeEntity>

    @Transaction
    @Query("SELECT n.* FROM ontology_nodes n JOIN ontology_relations r ON n.id = r.sourceId WHERE r.targetId = :nodeId AND r.type = :relationType")
    suspend fun getParentNodesByType(nodeId: String, relationType: String): List<OntologyNodeEntity>

    @Query("SELECT COUNT(*) FROM ontology_nodes")
    suspend fun getNodeCount(): Int

    @Query("SELECT COUNT(*) FROM ontology_relations")
    suspend fun getRelationCount(): Int

    // --- Subtopic queries ---
    // This query seems to be problematic based on the error: "no such column: m.subtemaId"
    // It's likely that the 'subtemaId' is not directly in the ontology_nodes table or related tables as expected.
    // Assuming 'subtemaId' should be 'id' from ontology_nodes table when joining.
    // Also, assuming 'estadoDominio', 'precision', 'totalIntentos', 'velocidadPromedio' should be joined from user_topic_mastery table.
    @Transaction
    @Query("""
        SELECT n.*, m.estadoDominio, m.precision, m.totalIntentos, m.velocidadPromedio
        FROM ontology_nodes n
        LEFT JOIN user_topic_mastery m ON n.id = m.topicId
        WHERE n.level = :level AND n.type = :type
    """)
    fun getSubtopicsWithDomainStatus(level: Int, type: String): Flow<List<OntologyNodeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: OntologyNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelation(relation: OntologyRelationEntity)

    @Query("DELETE FROM ontology_nodes")
    suspend fun clearNodes()

    @Query("DELETE FROM ontology_relations")
    suspend fun clearRelations()
}
