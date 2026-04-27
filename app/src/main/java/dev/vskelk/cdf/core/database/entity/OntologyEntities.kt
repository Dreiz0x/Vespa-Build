package dev.vskelk.cdf.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * OntologyNodeEntity - Nodos del grafo ontológico
 *
 * Representa cada concepto en el dominio del cargo VOE.
 * La ontología es el EJE que conecta Simulador, Diagnóstico y Entrevista.
 *
 * Per spec: Sin la ontología, los tres módulos son islas.
 * Con ella, un error en el Simulador genera recomendación en Diagnóstico
 * y pregunta de seguimiento en Entrevista.
 */
@Entity(
    tableName = "ontology_nodes",
    indices = [
        Index(value = ["nodeType"]),
        Index(value = ["parentId"]),
        Index(value = ["cargoId"])
    ]
)
data class OntologyNodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Tipo de nodo - define el rol semántico */
    val nodeType: String,

    /** Nombre visible del nodo */
    val name: String,

    /** Descripción detallada */
    val description: String? = null,

    /** FK al nodo padre (null para nodos raíz) */
    val parentId: Long? = null,

    /** FK al cargo al que pertenece (null si es transversal) */
    val cargoId: Long? = null,

    /** Referencia normativa principal (LEGIPE Art. X, etc.) */
    val primaryFundamentoRef: String? = null,

    /** Peso del nodo para cálculo de dominio (default 1.0) */
    val weight: Float = 1.0f,

    /** Orden de visualización dentro del mismo nivel */
    val displayOrder: Int = 0,

    /** Si está activo para estudio */
    val isActive: Boolean = true,

    /** Timestamp de última actualización */
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Relaciones entre nodos ontológicos
 * Permite relaciones muchos a muchos más allá de la jerarquía padre-hijo
 */
@Entity(
    tableName = "ontology_relations",
    primaryKeys = ["sourceNodeId", "targetNodeId", "relationType"],
    indices = [
        Index(value = ["sourceNodeId"]),
        Index(value = ["targetNodeId"])
    ]
)
data class OntologyRelationEntity(
    val sourceNodeId: Long,
    val targetNodeId: Long,
    val relationType: String, // ejercе, participa_en, se_funda_en, contiene, relacionado_con, etc.
    val description: String? = null
)

/**
 * CargoEntity - Cargos evaluados en el SPE
 *
 * Per spec: Usuario es Técnico de Organización Electoral, Distrito 06, Chihuahua
 */
@Entity(tableName = "cargos")
data class CargoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val descripcion: String? = null,
    val areaExamen: String, // TECNICO, SISTEMA, GENERAL
    val nivel: String? = null, // Vocalía, Coordinación, etc.
    val isDefault: Boolean = false
)

/**
 * ÓrganoEntity - Órganos del INE con atribuciones
 */
@Entity(tableName = "organos")
data class OrganoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val nombre: String,
    val tipo: String, // CONSEJO, JUNTA, COMITÉ, etc.
    val nivel: String, // FEDERAL, ESTATAL, DISTRITAL
    val ambito: String? = null // INE, OPLE, etc.
)

/**
 * Constantes de tipos de nodo ontológico
 * Usar estos valores exactos en nodeType
 */
object OntologyNodeTypes {
    const val ORGANO = "ORGANO"
    const val CARGO = "CARGO"
    const val PROCEDIMIENTO = "PROCEDIMIENTO"
    const val ATRIBUCION = "ATRIBUCION"
    const val FUNDAMENTO = "FUNDAMENTO"
    const val PLAZO = "PLAZO"
    const val TEMA = "TEMA"
    const val SUBTEMA = "SUBTEMA"
    const val COMPETENCIA = "COMPETENCIA"
    const val PATRON_ERROR = "PATRON_ERROR"
    const val EXCEPCION = "EXCEPCION"
    const val CONDICION = "CONDICION"
    
    // Áreas del Bozal Ontológico
    const val AREA_ESTRUCTURA = "ESTRUCTURA"
    const val AREA_PROCESO = "PROCESO"
    const val AREA_ORGANIZACION = "ORGANIZACION"
    const val AREA_JUSTICIA = "JUSTICIA"
    const val AREA_RESULTADOS = "RESULTADOS"
    const val AREA_RAZONAMIENTO = "RAZONAMIENTO_LOGICO_MATEMATICO"
    const val AREA_COMPRENSION = "COMPRENSION_LECTORA"
}

/**
 * Constantes de tipos de relación ontológica
 */
object OntologyRelationTypes {
    const val EJERCE = "ejerce"
    const val PARTICIPA_EN = "participa_en"
    const val SE_FUNDA_EN = "se_funda_en"
    const val PROVIENE_DE = "proviene_de"
    const val CONTIENE = "contiene"
    const val EVALUA = "evalua"
    const val CITA = "cita"
    const val DOMINA = "domina"
    const val FALLA_EN = "falla_en"
    const val SE_OBSERVA_EN = "se_observa_en"
    const val RELACIONADO_CON = "relacionado_con"
}
