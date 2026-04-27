package dev.vskelk.cdf.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * CuarentenaEntities - Sistema de validación de conocimiento
 */

@Entity(
    tableName = "cuarentena_fragmentos",
    indices = [
        Index(value = ["estado"]),
        Index(value = ["fuenteTipo"]),
        Index(value = ["areaExamen"]),
        Index(value = ["creadoEn"])
    ]
)
data class CuarentenaFragmentoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val contenido: String,
    val fuente: String? = null,
    val fuenteTipo: String? = null, 
    val certeza: String, 
    val areaExamen: String? = null, 
    val conflictoConId: Long? = null,
    val conflictoDescripcion: String? = null,
    val estado: String, 
    val promptOrigen: String? = null,
    val respuestaRaw: String? = null,
    val suggestedNodesJson: String? = null,
    val creadoEn: Long = System.currentTimeMillis(),
    val revisadoEn: Long? = null,
    val revisadoPor: String? = null
)

object CuarentenaEstado {
    const val PENDIENTE = "PENDIENTE"
    const val PENDING_VALIDATION = "PENDING_VALIDATION"
    const val APROBADO = "APROBADO"
    const val RECHAZADO = "RECHAZADO"
    const val CONFLICTO = "CONFLICTO"
}

object CuarentenaRules {
    fun debeDescartarse(fuente: String?, certeza: String): Boolean {
        if (fuente.isNullOrBlank()) return true
        return false
    }

    fun determinarEstadoInicial(
        fuente: String?,
        certeza: String,
        tieneConflicto: Boolean,
        confidenceScore: Float = 1.0f
    ): String {
        return when {
            fuente.isNullOrBlank() -> CuarentenaEstado.PENDIENTE
            tieneConflicto || confidenceScore < 0.85f -> CuarentenaEstado.CONFLICTO
            certeza == ExtractionCertainty.BAJA -> CuarentenaEstado.PENDING_VALIDATION
            else -> CuarentenaEstado.PENDIENTE
        }
    }

    fun debeForzarCertezaBaja(fuenteOficial: Boolean): Boolean {
        return !fuenteOficial
    }
}
