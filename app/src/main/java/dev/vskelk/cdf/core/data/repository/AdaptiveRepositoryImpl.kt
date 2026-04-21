package dev.vskelk.cdf.core.data.repository

import dev.vskelk.cdf.core.database.dao.*
import dev.vskelk.cdf.core.database.entity.*
import dev.vskelk.cdf.core.domain.model.*
import dev.vskelk.cdf.core.domain.repository.AdaptiveRepository
import dev.vskelk.cdf.core.domain.repository.OverallStats
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdaptiveRepositoryImpl @Inject constructor(
    private val reactivoDao: ReactivoDao,
    private val userMasteryDao: UserMasteryDao,
    private val studySessionDao: StudySessionDao,
    private val normativeDao: NormativeDao
) : AdaptiveRepository {

    override suspend fun getPrioritizedReactivos(limit: Int, modulo: String, examArea: String?): List<ReactivoUI> {
        val weakMasteries = userMasteryDao.getWeakSubtemas(threshold = DomainState.Thresholds.PRECISION_DOMINADO, limit = 20)
        val weakSubtemaIds = weakMasteries.map { it.subtemaId }
        val reactivos = if (weakSubtemaIds.isNotEmpty()) {
            reactivoDao.getReactivosBySubtemas(weakSubtemaIds, limit)
        } else {
            examArea?.let { reactivoDao.getReactivosByModuloAndArea(modulo, it, limit) } ?: reactivoDao.getRandomActiveReactivos(limit)
        }
        return reactivos.take(limit).mapNotNull { reactivo ->
            val opciones = reactivoDao.getOptionsForReactivo(reactivo.id)
            val fundamentos = normativeDao.getFragmentsForReactivo(reactivo.id).first()
            ReactivoUI(
                id = reactivo.id, enunciado = reactivo.enunciado, tipoReactivo = reactivo.tipoReactivo,
                nivelCognitivo = reactivo.nivelCognitivo, dificultad = reactivo.dificultad,
                opciones = opciones.map { op -> OpcionUI(id = op.id, texto = op.texto, isCorrect = op.isCorrect, explicacion = op.explicacion, distractorTipo = op.distractorTipo) },
                citaTextual = reactivo.citaTextual, casoTexto = reactivo.casoTexto,
                fundamentes = fundamentos.map { "${it.source} ${it.articleRef ?: ""}" }
            )
        }
    }

    override suspend fun getReactivoWithOptions(reactivoId: Long): ReactivoUI? {
        val reactivo = reactivoDao.getReactivoById(reactivoId) ?: return null
        val opciones = reactivoDao.getOptionsForReactivo(reactivoId)
        val fundamentos = normativeDao.getFragmentsForReactivo(reactivoId).first()
        return ReactivoUI(
            id = reactivo.id, enunciado = reactivo.enunciado, tipoReactivo = reactivo.tipoReactivo,
            nivelCognitivo = reactivo.nivelCognitivo, dificultad = reactivo.dificultad,
            opciones = opciones.map { op -> OpcionUI(id = op.id, texto = op.texto, isCorrect = op.isCorrect, explicacion = op.explicacion, distractorTipo = op.distractorTipo) },
            citaTextual = reactivo.citaTextual, casoTexto = reactivo.casoTexto,
            fundamentes = fundamentos.map { "${it.source} ${it.articleRef ?: ""}" }
        )
    }

    override suspend fun recordAnswer(sessionId: Long, reactivoId: Long, selectedOptionId: Long, isCorrect: Boolean, tiempoRespuestaMs: Long, errorType: String?) {
        val reactivo = reactivoDao.getReactivoById(reactivoId) ?: return
        val selectedOption = reactivoDao.getOptionById(selectedOptionId)
        val intento = ReactivoIntentoEntity(
            sessionId = sessionId, reactivoId = reactivoId, selectedOptionId = selectedOptionId,
            isCorrect = isCorrect, tiempoRespuestaMs = tiempoRespuestaMs,
            errorType = errorType ?: selectedOption?.distractorTipo?.let { ErrorType.fromDistractor(it) }
        )
        reactivoDao.insertIntento(intento)
        userMasteryDao.recordAttemptAndUpdateMastery(subtemaId = reactivo.subtemaId, isCorrect = isCorrect, tiempoRespuestaMs = tiempoRespuestaMs)
        if (!isCorrect && errorType != null) {
            val gapLog = UserGapLogEntity(subtemaId = reactivo.subtemaId, errorType = errorType, reactivoId = reactivoId, sessionId = sessionId)
            userMasteryDao.insertGapLog(gapLog)
        }
    }

    override suspend fun startSession(modulo: String, examArea: String?): Long {
        return studySessionDao.insertSession(StudySessionEntity(modulo = modulo, examArea = examArea, startedAt = System.currentTimeMillis()))
    }

    override suspend fun completeSession(sessionId: Long): SesionResultado {
        val session = studySessionDao.getSessionById(sessionId) ?: throw IllegalStateException("Sesión no encontrada")
        val intentos = reactivoDao.getIntentosForSession(sessionId)
        val correctos = intentos.count { it.isCorrect }
        val total = intentos.size
        val incorrectos = total - correctos
        val precision = if (total > 0) correctos.toFloat() / total else 0f
        val tiempoPromedio = reactivoDao.getAverageTimeForSession(sessionId) ?: 0f

        // ⚡ CORREGIDO: Casting explícito a List<Long>
        val weakSubtemas: List<Long> = intentos
            .filter { !it.isCorrect }
            .mapNotNull { attempt -> reactivoDao.getReactivoById(attempt.reactivoId)?.subtemaId }
            .distinct()
            
        val errorCounts = intentos.filter { !it.isCorrect && it.errorType != null }.groupBy { it.errorType!! }.mapValues { it.value.size }
        val dominantErrors = errorCounts.entries.sortedByDescending { it.value }.take(3).map { it.key }

        studySessionDao.completeSession(
            sessionId = sessionId, correctos = correctos, tiempoPromedioSeg = tiempoPromedio,
            weakSubtemas = weakSubtemas,
            dominantErrors = dominantErrors
        )

        return SesionResultado(
            sessionId = sessionId, totalReactivos = total, correctos = correctos, incorrectos = incorrectos,
            precision = precision, tiempoPromedioSeg = tiempoPromedio,
            subtemasDebiles = weakSubtemas.map { it.toString() },
            tiposErrorFrecuentes = dominantErrors,
            mensaje = if (precision >= 0.8f) "¡Excelente!" else "A repasar."
        )
    }

    override fun observeRecentSessions(limit: Int): Flow<List<StudySessionEntity>> = studySessionDao.getRecentSessions(limit)
    override fun observeCompletedSessionCount(): Flow<Int> = studySessionDao.observeCompletedSessionCount()

    override suspend fun observeWeakTopics(): Flow<List<SubtemaConDominio>> {
        return userMasteryDao.getMasteryByStates(DomainState.estadosDebiles.toList()).map { masteries ->
            masteries.map { mastery ->
                SubtemaConDominio(
                    subtema = OntologyNode(id = mastery.subtemaId, nodeType = "", name = "Subtema ${mastery.subtemaId}", description = null, parentId = null, weight = 1f, isActive = true),
                    estadoDominio = mastery.estadoDominio, precision = mastery.precision, totalIntentos = mastery.totalIntentos, velocidadPromedio = mastery.velocidadPromedio
                )
            }
        }
    }

    override suspend fun getFrequentErrorTypes(limit: Int): List<Pair<String, Int>> = userMasteryDao.getGlobalErrorTypeCounts(limit).map { it.errorType to it.count }

    override suspend fun getOverallStats(): OverallStats {
        val sessionCount = studySessionDao.getCompletedSessionCount()
        val overallAccuracy = studySessionDao.getOverallAccuracy() ?: 0f
        val dominadoCount = userMasteryDao.getMasteryByState(DomainState.DOMINADO).first().size
        val totalMastery = userMasteryDao.getMasteryCount()
        val brechaCount = userMasteryDao.observeAffectedSubtemaCount().first()

        return OverallStats(
            totalSesiones = sessionCount, precisionGeneral = overallAccuracy,
            subtemasDominados = dominadoCount, totalSubtemas = totalMastery, brechasActivas = brechaCount
        )
    }
}
