package dev.vskelk.cdf.core.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class SeedManifest(
    @SerialName("version") val version: String,
    @SerialName("minReactivos") val minReactivos: Int,
    @SerialName("minNormativa") val minNormativa: Int,
    @SerialName("minOntologia") val minOntologia: Int,
    @SerialName("descripcion") val descripcion: String,
    @SerialName("archivos") val archivos: Map<String, String>
)

@Serializable
data class SeedNormativaSource(
    val id: Long, val code: String, val name: String, val type: String, 
    val version: String, val active_since: Long, val status: String
)

@Serializable
data class SeedNormativaFragment(
    val id: Long, val source_id: Long, val version_id: Long, val article_ref: String,
    val citation_text: String?, val content: String, val vigencia_desde: Long,
    val vigencia_hasta: Long?, val status: String, val replaced_by_id: Long?
)

@Serializable
data class SeedOntologiaNode(
    val id: Long, @SerialName("node_type") val node_type: String, val label: String,
    val description: String, val confidence: Double, @SerialName("parent_id") val parent_id: Long?,
    @SerialName("is_active") val is_active: Boolean
)

@Serializable
data class SeedReactivo(
    val id: Long, val enunciado: String, val modulo: String, val exam_area: String,
    val tema_id: Long, val subtema_id: Long?, val tipo_reactivo: String,
    val nivel_cognitivo: String, val dificultad: Double, val patron_error_id: Long?,
    val fundamento_id: Long?, val cita_textual: String?, val vigencia_desde: Long,
    val vigencia_hasta: Long?, val origen: String, val status: String,
    val invalidation_reason: String?, val options: List<SeedReactivoOption>
)

@Serializable
data class SeedReactivoOption(
    val id: Long, val texto: String, val is_correct: Boolean, 
    val explicacion: String?, val distractor_tipo: String?
)
