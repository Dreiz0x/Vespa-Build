package dev.vskelk.cdf.core.data.repository

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
