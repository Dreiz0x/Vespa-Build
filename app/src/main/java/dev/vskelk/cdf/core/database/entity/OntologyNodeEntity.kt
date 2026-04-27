@Entity(tableName = "ontology_nodes")
data class OntologyNodeEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val tipo: String,
    val displayOrder: Int = 0
)
