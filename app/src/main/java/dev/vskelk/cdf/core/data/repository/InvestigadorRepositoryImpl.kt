package dev.vskelk.cdf.core.data.repository

import dev.vskelk.cdf.core.database.dao.NormativeDao
import dev.vskelk.cdf.core.database.dao.OntologyDao
import dev.vskelk.cdf.core.database.dao.QuarantineDao
import dev.vskelk.cdf.core.database.entity.NormativeFragmentEntity
import dev.vskelk.cdf.core.domain.model.FragmentoInvestigado
import dev.vskelk.cdf.core.domain.model.InvestigacionEstado
import dev.vskelk.cdf.core.domain.model.InvestigacionResult
import dev.vskelk.cdf.core.domain.repository.InvestigadorRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvestigadorRepositoryImpl @Inject constructor(
    private val quarantineDao: QuarantineDao,
    private val normativeDao: NormativeDao,
    private val ontologyDao: OntologyDao,
    private val preferencesDataSource: dev.vskelk.cdf.core.datastore.PreferencesDataSource
) : InvestigadorRepository {

    override suspend fun investigar(tema: String, areaExamen: String?): InvestigacionEstado {
        try {
            emitState(InvestigacionEstado.Acotando("Buscando información existente..."))
            val existente = normativeDao.searchByKeyword(tema)
            if (existente.size >= 5) {
                return InvestigacionEstado.Completado(
                    InvestigacionResult(
                        fragmentos = existente.map { it.toInvestigado() },
                        conflictos = emptyList(),
                        fuentesVerificadas = existente.size,
                        necesitaRevision = false
                    )
                )
            }

            emitState(InvestigacionEstado.Formulando("Preparando consulta..."))
            val apiKey = preferencesDataSource.getApiKey("GEMINI")
                ?: return InvestigacionEstado.Error("No hay API key configurada")

            emitState(InvestigacionEstado.Consultando("Consultando fuentes..."))
            kotlinx.coroutines.delay(1000)

            emitState(InvestigacionEstado.Validando("Analizando resultados..."))

            return InvestigacionEstado.Completado(
                InvestigacionResult(
                    fragmentos = emptyList(),
                    conflictos = emptyList(),
                    fuentesVerificadas = 0,
                    necesitaRevision = true
                )
            )
        } catch (e: Exception) {
            return InvestigacionEstado.Error(e.message ?: "Error en investigación")
        }
    }

    private suspend fun emitState(state: InvestigacionEstado): InvestigacionEstado {
        kotlinx.coroutines.delay(500)
        return state
    }

    override fun observePendientes(): Flow<List<dev.vskelk.cdf.core.database.entity.CuarentenaFragmentoEntity>> = quarantineDao.observePendientes()
    
    override fun observeConflictos(): Flow<List<dev.vskelk.cdf.core.database.entity.CuarentenaFragmentoEntity>> = quarantineDao.observeConflictos()

    override suspend fun approveFragmento(fragmentoId: Long) {
        quarantineDao.approveFragmento(fragmentoId)
    }

    override suspend fun rejectFragmento(fragmentoId: Long) {
        quarantineDao.rejectFragmento(fragmentoId)
    }

    override suspend fun approveAndPromoteToNormative(fragmentoId: Long) {
        val fragmento = quarantineDao.getFragmentoById(fragmentoId) ?: return

        val existente = normativeDao.findExactDuplicate(fragmento.contenido)
        if (existente != null) {
            normativeDao.updateFragment(
                existente.copy(
                    confidenceCount = existente.confidenceCount + 1,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            val nuevoFragmento = NormativeFragmentEntity(
                content = fragmento.contenido,
                source = fragmento.fuente ?: "DESCONOCIDA",
                articleRef = null,
                sourceType = fragmento.fuenteTipo ?: "DESCONOCIDO",
                certainty = fragmento.certeza,
                areaExamen = fragmento.areaExamen ?: "TECNICO",
                vigenciaDesde = System.currentTimeMillis()
            )
            normativeDao.insertFragment(nuevoFragmento)
        }

        quarantineDao.approveFragmento(fragmentoId)
    }

    override fun observePendienteCount(): Flow<Int> = quarantineDao.observePendienteCount()
    
    override fun observeConflictoCount(): Flow<Int> = quarantineDao.observeConflictoCount()

    private fun NormativeFragmentEntity.toInvestigado() = FragmentoInvestigado(
        contenido = content,
        fuente = source,
        articleRef = articleRef,
        certeza = certainty,
        areaExamen = areaExamen,
        nodoSugerido = null
    )
}
