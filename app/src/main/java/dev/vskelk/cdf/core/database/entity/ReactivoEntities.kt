package dev.vskelk.cdf.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * ReactivoEntity - Preguntas del examen con trazabilidad normativa
 *
 * Cada reactivo almacena fundamento normativo verificado y trazable.
 *
 * Per spec: Es el motor de reactivos trazables.
 * Un reactivo sin fundamento = NO es válido en Vespa.
 */
@Entity(
    tableName = "reactivos",
    indices = [
        Index(value = ["modulo"]),
        Index(value = ["examArea"]),
        Index(value = ["temaId"]),
        Index(value = ["subtemaId"]),
        Index(value = ["status"]),
        Index(value = ["nivelCognitivo"]),
        Index(value = ["vigenciaHasta"])
    ]
)
data class ReactivoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Texto de la pregunta */
    val enunciado: String,

    /** Módulo al que pertenece */
    val modulo: String, // SIMULADOR, DIAGNOSTICO

    /** Área de examen */
    val examArea: String, // TECNICO, SISTEMA, GENERAL

    /** FK al tema ontológico */
    val temaId: Long,

    /** FK al subtema ontológico (mayor granularidad) */
    val subtemaId: Long,

    /** FK al nodo ontológico principal */
    val ontologyNodeId: Long? = null,

    /** Tipo de reactivo */
    val tipoReactivo: String, // OPCION_MULTIPLE, ORDENAMIENTO, CASO, ABIERTA

    /** Nivel cognitivoBloom */
    val nivelCognitivo: String, // CONOCIMIENTO, COMPRENSION, APLICACION, ANALISIS

    /** Dificultad calculada (0.0 - 1.0) */
    val dificultad: Float = 0.5f,

    /** FK al patrón de error asociado */
    val patronErrorId: Long? = null,

    /** Cita textual del fundamento (si aplica) */
    val citaTextual: String? = null,

    /** Texto del caso (si es tipo CASO) */
    val casoTexto: String? = null,

    /** Instrucciones especiales (si aplica) */
    val instrucciones: String? = null,

    /** Orden para ordenamiento (si aplica) */
    val ordenItems: String? = null, // JSON array del orden correcto

    /** Vigencia desde (timestamp) */
    val vigenciaDesde: Long,

    /** Vigencia hasta (null = vigente) */
    val vigenciaHasta: Long? = null,

    /** Origen del reactivo */
    val origen: String, // MANUAL, SEMI_GENERADO, GENERADO_VALIDADO

    /** Estado del reactivo */
    val status: String = "ACTIVE", // ACTIVE, INVALIDATED

    /** Razón de invalidación (si fue invalidado) */
    val invalidationReason: String? = null,

    /** Timestamp de creación */
    val createdAt: Long = System.currentTimeMillis(),

    /** Timestamp de última actualización */
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * ReactivoOptionEntity - Opciones de respuesta
 */
@Entity(
    tableName = "reactivo_opciones",
    foreignKeys = [
        ForeignKey(
            entity = ReactivoEntity::class,
            parentColumns = ["id"],
            childColumns = ["reactivoId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["reactivoId"])]
)
data class ReactivoOptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** FK al reactivo padre */
    val reactivoId: Long,

    /** Texto de la opción */
    val texto: String,

    /** Si es la respuesta correcta */
    val isCorrect: Boolean,

    /** Explicación de por qué es correcta/incorrecta */
    val explicacion: String? = null,

    /** Tipo de distractor (para clasificación de errores) */
    val distractorTipo: String? = null, // SIMILAR_ORGANO, PLAZO_INCORRECTO, EXCEPCION, etc.

    /** Orden de visualización (0 = primero) */
    val displayOrder: Int = 0
)

/**
 * ReactivoIntentoEntity - Registro de intentos del usuario
 */
@Entity(
    tableName = "reactivo_intentos",
    indices = [
        Index(value = ["reactivoId"]),
        Index(value = ["sessionId"]),
        Index(value = ["createdAt"])
    ]
)
data class ReactivoIntentoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** FK a la sesión de estudio */
    val sessionId: Long,

    /** FK al reactivo */
    val reactivoId: Long,

    /** FK a la opción seleccionada */
    val selectedOptionId: Long,

    /** Si fue correcta */
    val isCorrect: Boolean,

    /** Tiempo de respuesta en milisegundos */
    val tiempoRespuestaMs: Long,

    /** Tipo de error (si incorrecto) */
    val errorType: String? = null,

    /** Timestamp del intento */
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Constantes de módulo
 */
object ReactivoModulo {
    const val SIMULADOR = "SIMULADOR"
    const val DIAGNOSTICO = "DIAGNOSTICO"
}

/**
 * Constantes de tipo de reactivo
 */
object ReactivoTipo {
    const val OPCION_MULTIPLE = "OPCION_MULTIPLE"
    const val ORDENAMIENTO = "ORDENAMIENTO"
    const val CASO = "CASO"
    const val ABIERTA = "ABIARTA"
}

/**
 * Constantes de nivel cognitivo (Bloom)
 */
object NivelCognitivo {
    const val CONOCIMIENTO = "CONOCIMIENTO"   // Recuerda dato, artículo, plazo
    const val COMPRENSION = "COMPRENSION"     // Explica con sus palabras
    const val APLICACION = "APLICACION"       // Aplica la norma a un caso
    const val ANALISIS = "ANALISIS"           // Distingue entre casos similares

    /**
     * Los reactivos del SPE se concentran en APLICACION y ANALISIS
     */
    @androidx.room.Ignore val nivelesAvanzados = setOf(APLICACION, ANALISIS)
}

/**
 * Constantes de origen
 */
object ReactivoOrigen {
    const val MANUAL = "MANUAL"
    const val SEMI_GENERADO = "SEMI_GENERADO"
    const val GENERADO_VALIDADO = "GENERADO_VALIDADO"
}

/**
 * Constantes de estado
 */
object ReactivoStatus {
    const val ACTIVE = "ACTIVE"
    const val INVALIDATED = "INVALIDATED"
}

/**
 * Constantes de tipo de distractor
 */
object DistractorTipo {
    const val SIMILAR_ORGANO = "SIMILAR_ORGANO"
    const val PLAZO_INCORRECTO = "PLAZO_INCORRECTO"
    const val EXCEPCION = "EXCEPCION"
    const val SEMANTICO = "SEMANTICO"
    const val NUMERICO = "NUMERICO"
    const val INVERSION = "INVERSION"
}
