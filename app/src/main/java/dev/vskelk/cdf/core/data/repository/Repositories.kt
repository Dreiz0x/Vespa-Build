package dev.vskelk.cdf.core.data.repository

import dev.vskelk.cdf.core.database.dao.*
import dev.vskelk.cdf.core.database.entity.*
import dev.vskelk.cdf.core.domain.model.*
import dev.vskelk.cdf.core.domain.repository.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

object DomainState {
    const val NO_VISTO = "NO_VISTO"
    const val DOMINADO = "DOMINADO"
    const val EN_CONSOLIDACION = "EN_CONSOLIDACION"
    const val INESTABLE = "INESTABLE"
    
    val estadosDebiles = setOf(NO_VISTO, INESTABLE)
    
    object Thresholds {
        const val PRECISION_DOMINADO = 0.7f
    }
}

@Singleton
class OntologyRepositoryImpl @Inject constructor(
    private val ontologyDao: OntologyDao,
    private val userMasteryDao: UserMasteryDao
) : OntologyRepository {

    override fun observeAllNodes(): Flow<List<OntologyNode>> {
        return ontologyDao.observeAllNodes().map { nodes -> nodes.map { it.toModel() } }
    }

    override fun observeNodesByType(nodeType: String): Flow<List<OntologyNode>> {
        return ontologyDao.getNodesByType(nodeType).map { nodes -> nodes.map { it.toModel() } }
    }

    override fun observeChildNodes(parentId: Long): Flow<List<OntologyNode>> {
        return ontologyDao.getChildNodes(parentId).map { nodes -> nodes.map { it.toModel() } }
    }

    override suspend fun getNodeById(nodeId: Long): OntologyNode? {
        return ontologyDao.getNodeById(nodeId)?.toModel()
    }

    override suspend fun searchNodes(query: String): List<OntologyNode> {
        return ontologyDao.searchNodes(query).map { it.toModel() }
    }

    override fun observeSubtemasConDominio(): Flow<List<SubtemaConDominio>> {
        return combine(ontologyDao.getSubtemas(), userMasteryDao.observeAllMastery()) { subtemas, masteryList ->
            subtemas.mapNotNull { subtema ->
                val mastery = masteryList.find { it.subtemaId == subtema.id }
                if (mastery != null) mastery.toSubtemaConDominio(subtema.toModel())
                else SubtemaConDominio(subtema = subtema.toModel(), estadoDominio = DomainState.NO_VISTO, precision = 0f, totalIntentos = 0, velocidadPromedio = 0f)
            }
        }
    }

    override suspend fun getWeakSubtemas(limit: Int): List<SubtemaConDominio> {
        val weakMasteries = userMasteryDao.getWeakSubtemas(threshold = DomainState.Thresholds.PRECISION_DOMINADO, limit = limit)
        return weakMasteries.mapNotNull { mastery ->
            val node = ontologyDao.getNodeById(mastery.subtemaId)
            node?.let { mastery.toSubtemaConDominio(it.toModel()) }
        }
    }

    override suspend fun getDiagnostico(): DiagnosticoResult {
        val allNodes = ontologyDao.getSubtemas().first()
        val allMastery = userMasteryDao.observeAllMastery().first()
        val errorCounts = userMasteryDao.getGlobalErrorTypeCounts()
        val subtemasConDominio = allNodes.map { node ->
            val mastery = allMastery.find { it.subtemaId == node.id }
            mastery?.toSubtemaConDominio(node.toModel()) ?: SubtemaConDominio(subtema = node.toModel(), estadoDominio = DomainState.NO_VISTO, precision = 0f, totalIntentos = 0, velocidadPromedio = 0f)
        }
        val subtemasDebiles = subtemasConDominio.filter { it.estadoDominio in DomainState.estadosDebiles }.sortedBy { it.precision }
        val precisionGeneral = if (allMastery.isNotEmpty()) allMastery.map { it.precision }.average().toFloat() else 0f
        val recomendaciones = subtemasDebiles.take(5).map { sdc ->
            Recomendacion(
                tipo = when {
                    sdc.estadoDominio == DomainState.NO_VISTO -> RecomendacionTipo.INVESTIGAR
                    sdc.precision < 0.4f -> RecomendacionTipo.REPASAR_FUNDAMENTO
                    else -> RecomendacionTipo.PRACTICAR_MAS
                },
                subtemaId = sdc.subtema.id, subtemaNombre = sdc.subtema.name, fundamentoRef = null,
                descripcion = "Precision actual: ${(sdc.precision * 100).toInt()}%"
            )
        }
        return DiagnosticoResult(
            totalSubtemas = subtemasConDominio.size, subtemasDebiles = subtemasDebiles,
            erroresFrecuentes = errorCounts.associate { it.errorType to it.count },
            precisionGeneral = precisionGeneral, recomendaciones = recomendaciones
        )
    }

    override fun observeRelatedNodes(nodeId: Long, relationType: String): Flow<List<OntologyNode>> {
        return ontologyDao.getRelatedNodes(nodeId, relationType).map { nodes -> nodes.map { it.toModel() } }
    }
}
