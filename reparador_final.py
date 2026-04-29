
import os

BASE = "/root/Spen-Vespa/app/src/main/java/dev/vskelk/cdf"

files = {
    # DomainState - falta import
    f"{BASE}/core/data/repository/Repositories.kt": '''package dev.vskelk.cdf.core.data.repository

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
''',

    # InvestigadorRepositoryImpl - certeza y areaExamen
    f"{BASE}/core/data/repository/InvestigadorRepositoryImpl.kt": '''package dev.vskelk.cdf.core.data.repository

import dev.vskelk.cdf.core.database.dao.NormativeDao
import dev.vskelk.cdf.core.database.dao.OntologyDao
import dev.vskelk.cdf.core.database.dao.QuarantineDao
import dev.vskelk.cdf.core.database.entity.CuarentenaFragmentoEntity
import dev.vskelk.cdf.core.database.entity.NormativeFragmentEntity
import dev.vskelk.cdf.core.domain.model.*
import dev.vskelk.cdf.core.domain.repository.InvestigadorRepository
import dev.vskelk.cdf.core.datastore.PreferencesDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvestigadorRepositoryImpl @Inject constructor(
    private val quarantineDao: QuarantineDao,
    private val normativeDao: NormativeDao,
    private val ontologyDao: OntologyDao,
    private val preferencesDataSource: PreferencesDataSource
) : InvestigadorRepository {

    override suspend fun investigar(tema: String, areaExamen: String?): InvestigacionEstado {
        try {
            val existente = normativeDao.searchByKeyword(tema)
            if (existente.size >= 5) {
                return InvestigacionEstado.Completado(
                    InvestigacionResult(
                        fragmentos = existente.map { it.toInvestigado() },
                        conflictos = emptyList(), fuentesVerificadas = existente.size, necesitaRevision = false
                    )
                )
            }
            val apiKey = preferencesDataSource.getApiKey("GEMINI") ?: return InvestigacionEstado.Error("No hay API key configurada")
            kotlinx.coroutines.delay(1000)
            return InvestigacionEstado.Completado(InvestigacionResult(fragmentos = emptyList(), conflictos = emptyList(), fuentesVerificadas = 0, necesitaRevision = true))
        } catch (e: Exception) {
            return InvestigacionEstado.Error(e.message ?: "Error en investigacion")
        }
    }

    override fun observePendientes(): Flow<List<CuarentenaFragmentoEntity>> = quarantineDao.observePendientes()
    override fun observeConflictos(): Flow<List<CuarentenaFragmentoEntity>> = quarantineDao.observeConflictos()
    override suspend fun approveFragmento(fragmentoId: Long) { quarantineDao.approveFragmento(fragmentoId) }
    override suspend fun rejectFragmento(fragmentoId: Long) { quarantineDao.rejectFragmento(fragmentoId) }

    override suspend fun approveAndPromoteToNormative(fragmentoId: Long) {
        val fragmento = quarantineDao.getFragmentoById(fragmentoId) ?: return
        val existente = normativeDao.findExactDuplicate(fragmento.rawContent)
        if (existente != null) {
            normativeDao.updateFragment(existente.copy(confidenceCount = existente.confidenceCount + 1, updatedAt = System.currentTimeMillis()))
        } else {
            val nuevoFragmento = NormativeFragmentEntity(
                content = fragmento.rawContent, source = "INVESTIGADOR", articleRef = null,
                sourceType = "DESCONOCIDO", certainty = "MEDIA", areaExamen = fragmento.spenArea,
                vigenciaDesde = System.currentTimeMillis()
            )
            normativeDao.insertFragment(nuevoFragmento)
        }
        quarantineDao.approveFragmento(fragmentoId)
    }

    override fun observePendienteCount(): Flow<Int> = quarantineDao.observePendienteCount()
    override fun observeConflictoCount(): Flow<Int> = quarantineDao.observeConflictoCount()

    private fun NormativeFragmentEntity.toInvestigado() = FragmentoInvestigado(
        contenido = content, fuente = source, articleRef = articleRef,
        certeza = certainty, areaExamen = areaExamen, nodoSugerido = null
    )
}
''',

    # InterviewViewModel
    f"{BASE}/ui/interview/InterviewViewModel.kt": '''package dev.vskelk.cdf.ui.interview

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class InterviewViewModel @Inject constructor() : ViewModel() {
    private val _estado = MutableStateFlow("Idle")
    val estado: StateFlow<String> = _estado
}
''',

    # CuarentenaViewModel
    f"{BASE}/ui/quarantine/CuarentenaViewModel.kt": '''package dev.vskelk.cdf.ui.quarantine

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class CuarentenaViewModel @Inject constructor() : ViewModel() {
    private val _estado = MutableStateFlow("Idle")
    val estado: StateFlow<String> = _estado
}
''',

    # MainViewModel - arreglar .id
    f"{BASE}/ui/main/MainViewModel.kt": '''package dev.vskelk.cdf.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vskelk.cdf.core.domain.model.BootstrapState
import dev.vskelk.cdf.core.domain.repository.AdaptiveRepository
import dev.vskelk.cdf.core.domain.repository.BootstrapRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionSummary(val sessionId: Long, val correctos: Int, val total: Int, val modulo: String)

data class MainUiState(
    val isLoading: Boolean = true, val progresoGeneral: Int = 0, val brechasDetectadas: Int = 0,
    val sesionesCompletadas: Int = 0, val subtemasDominados: Int = 0, val corpusVersion: String = "2.0.0",
    val pendientesInvestigador: Int = 0, val recientes: List<SessionSummary> = emptyList()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val adaptiveRepository: AdaptiveRepository,
    private val bootstrapRepository: BootstrapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            bootstrapRepository.bootstrapState.collect { state ->
                when (state) {
                    is BootstrapState.Seeding -> _uiState.update { it.copy(isLoading = true) }
                    is BootstrapState.Ready -> { _uiState.update { it.copy(isLoading = false) }; loadStats() }
                    is BootstrapState.Error -> _uiState.update { it.copy(isLoading = false) }
                    else -> Unit
                }
            }
        }
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val stats = adaptiveRepository.getOverallStats()
                _uiState.update { it.copy(progresoGeneral = (stats.precisionGeneral * 100).toInt(), brechasDetectadas = stats.brechasActivas, sesionesCompletadas = stats.totalSesiones, subtemasDominados = stats.subtemasDominados, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        viewModelScope.launch {
            adaptiveRepository.observeRecentSessions(5).collect { sessions ->
                _uiState.update { state -> state.copy(recientes = sessions.map { s -> SessionSummary(s.sessionId, s.correctos, s.totalReactivos, s.modulo) }) }
            }
        }
    }

    fun refresh() { _uiState.update { it.copy(isLoading = true) }; loadStats() }
}
'''
}

for path, content in files.items():
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content.strip() + '\n')
    print(f"OK: {path}")

print("\nTODO LISTO. Compila con: cd /root/Spen-Vespa && ./gradlew --stop && rm -rf build .gradle app/build app/.gradle && ./gradlew clean assembleDebug --no-daemon")

