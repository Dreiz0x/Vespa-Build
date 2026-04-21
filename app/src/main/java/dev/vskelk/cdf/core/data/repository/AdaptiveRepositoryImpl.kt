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
        
        val weakSubtemaIds = mutableListOf<Long>()
        for (mastery in weakMasteries) {
            weakSubtemaIds.add(mastery.subtemaId)
        }

        val reactivos = if (weakSubtemaIds.isNotEmpty()) {
            reactivoDao.getReactivosBySubtemas(weakSubtemaIds, limit)
        } else {
            if (examArea != null) {
                reactivoDao.getReactivosByModuloAndArea(modulo, examArea, limit)
            } else {
                reactivoDao.getRandomActiveReactivos(limit)
            }
        }

        val resultados = mutableListOf<ReactivoUI>()
        for (reactivo in reactivos) {
            val opciones = reactivoDao.getOptionsForReactivo(reactivo.id)
            val fundamentos = normativeDao.getFragmentsForReactivo(reactivo.id).first()
            
            val opcionesUI = mutableListOf<OpcionUI>()
            for (op in opciones) {
                opcionesUI.add(OpcionUI(id = op.id, texto = op.texto, isCorrect = op.isCorrect, explicacion = op.explicacion, distractorTipo = op.distractorTipo))
            }

            val fundamentosStr = mutableListOf<String>()
            for (f in fundamentos) {
                fundamentosStr.add("${f.source} ${f.articleRef ?: ""}")
            }

            resultados.add(
                ReactivoUI(
                    id = reactivo.id,
                    enunciado = reactivo.enunciado,
                    tipoReactivo = reactivo.tipoReactivo,
                    nivelCognitivo = reactivo.nivelCognitivo,
                    dificultad = reactivo.dificultad,
                    opciones = opcionesUI,
                    citaTextual = reactivo.citaTextual,
                    casoTexto = reactivo.casoTexto,
                    fundamentes = fundamentosStr
                )
            )
        }
        return resultados.take(limit)
    }

    override suspend fun getReactivoWithOptions(reactivoId: Long): ReactivoUI? {
        val reactivo = reactivoDao.getReactivoById(reactivoId) ?: return null
        val opciones = reactivoDao.getOptionsForReactivo(reactivoId)
        val fundamentos = normativeDao.getFragmentsForReactivo(reactivoId).first()
        
        val opcionesUI = mutableListOf<OpcionUI>()
        for (op in opciones) {
            opcionesUI.add(OpcionUI(id = op.id, texto = op.texto, isCorrect = op.isCorrect, explicacion = op.explicacion, distractorTipo = op.distractorTipo))
        }

        val fundamentosStr = mutableListOf<String>()
        for (f in fundamentos) {
            fundamentosStr.add("${f.source} ${f.articleRef ?: ""}")
        }

        return ReactivoUI(
            id = reactivo.id,
            enunciado = reactivo.enunciado,
            tipoReactivo = reactivo.tipoReactivo,
            nivelCognitivo = reactivo.nivelCognitivo,
            dificultad = reactivo.dificultad,
            opciones = opcionesUI,
            citaTextual = reactivo.citaTextual,
            casoTexto = reactivo.casoTexto,
            fundamentes = fundamentosStr
        )
    }

    override suspend fun recordAnswer(sessionId: Long, reactivoId: Long, selectedOptionId: Long, isCorrect: Boolean, tiempoRespuestaMs: Long, errorType: String?) {
        val reactivo = reactivoDao.getReactivoById(reactivoId) ?: return
        val selectedOption = reactivoDao.getOptionById(selectedOptionId)
        
        val resolvedErrorType = errorType ?: selectedOption?.distractorTipo?.let { ErrorType.fromDistractor(it) }
        
        val intento = ReactivoIntentoEntity(
            sessionId = sessionId,
            reactivoId = reactivoId,
            selectedOptionId = selectedOptionId,
            isCorrect = isCorrect,
            tiempoRespuestaMs = tiempoRespuestaMs,
            errorType = resolvedErrorType
        )
        reactivoDao.insertIntento(intento)
        userMasteryDao.recordAttemptAndUpdateMastery(subtemaId = reactivo.subtemaId, isCorrect = isCorrect, tiempoRespuestaMs = tiempoRespuestaMs)
        
        if (!isCorrect && resolvedErrorType != null) {
            val gapLog = UserGapLogEntity(subtemaId = reactivo.subtemaId, errorType = resolvedErrorType, reactivoId = reactivoId, sessionId = sessionId)
            userMasteryDao.insertGapLog(gapLog)
        }
    }

    override suspend fun startSession(modulo: String, examArea: String?): Long {
        return studySessionDao.insertSession(StudySessionEntity(modulo = modulo, examArea = examArea, startedAt = System.currentTimeMillis()))
    }

    override suspend fun completeSession(sessionId: Long): SesionResultado {
        val session = studySessionDao.getSessionById(sessionId) ?: throw IllegalStateException("Sesión no encontrada")
        val intentos = reactivoDao.getIntentosForSession(sessionId)
        
        var correctos = 0
        for (intento in intentos) {
            if (intento.isCorrect) correctos++
        }
        
        val total = intentos.size
        val incorrectos = total - correctos
        val precision = if (total > 0) correctos.toFloat() / total else 0f
        val tiempoPromedio = reactivoDao.getAverageTimeForSession(sessionId) ?: 0f

        val weakSubtemasRaw = mutableListOf<Long>()
        val errorCountsMap = mutableMapOf<String, Int>()

        for (intento in intentos) {
            if (!intento.isCorrect) {
                val reactivo = reactivoDao.getReactivoById(intento.reactivoId)
                if (reactivo != null) {
                    weakSubtemasRaw.add(reactivo.subtemaId)
                }

                val eType = intento.errorType
                if (eType != null) {
                    val currentCount = errorCountsMap[eType] ?: 0
                    errorCountsMap[eType] = currentCount + 1
                }
            }
        }

        // Tipo explícito forzado para la base de datos
        val distinctWeakSubtemas: List<Long> = weakSubtemasRaw.distinct()

        val sortedErrors = errorCountsMap.entries.sortedByDescending { it.value }.take(3)
        val dominantErrors = mutableListOf<String>()
        for (entry in sortedErrors) {
            dominantErrors.add(entry.key)
        }

        studySessionDao.completeSession(
            sessionId = sessionId,
            correctos = correctos,
            tiempoPromedioSeg = tiempoPromedio,
            weakSubtemas = distinctWeakSubtemas,
            dominantErrors = dominantErrors
        )

        val weakSubtemasStr = mutableListOf<String>()
        for (id in distinctWeakSubtemas) {
            weakSubtemasStr.add(id.toString())
        }

        return SesionResultado(
            sessionId = sessionId,
            totalReactivos = total,
            correctos = correctos,
            incorrectos = incorrectos,
            precision = precision,
            tiempoPromedioSeg = tiempoPromedio,
            subtemasDebiles = weakSubtemasStr,
            tiposErrorFrecuentes = dominantErrors,
            mensaje = if (precision >= 0.8f) "¡Excelente!" else "A repasar."
        )
    }

    override fun observeRecentSessions(limit: Int): Flow<List<StudySessionEntity>> = studySessionDao.getRecentSessions(limit)
    override fun observeCompletedSessionCount(): Flow<Int> = studySessionDao.observeCompletedSessionCount()

    override suspend fun observeWeakTopics(): Flow<List<SubtemaConDominio>> {
        val statesList = DomainState.estadosDebiles.toList()
        return userMasteryDao.getMasteryByStates(statesList).map { masteries ->
            val result = mutableListOf<SubtemaConDominio>()
            for (mastery in masteries) {
                result.add(
                    SubtemaConDominio(
                        subtema = OntologyNode(
                            id = mastery.subtemaId,
                            nodeType = "",
                            name = "Subtema ${mastery.subtemaId}",
                            description = null,
                            parentId = null,
                            weight = 1f,
                            isActive = true
                        ),
                        estadoDominio = mastery.estadoDominio,
                        precision = mastery.precision,
                        totalIntentos = mastery.totalIntentos,
                        velocidadPromedio = mastery.velocidadPromedio
                    )
                )
            }
            result
        }
    }

    override suspend fun getFrequentErrorTypes(limit: Int): List<Pair<String, Int>> {
        val counts = userMasteryDao.getGlobalErrorTypeCounts(limit)
        val result = mutableListOf<Pair<String, Int>>()
        for (c in counts) {
            result.add(Pair(c.errorType, c.count))
        }
        return result
    }

    override suspend fun getOverallStats(): OverallStats {
        val sessionCount = studySessionDao.getCompletedSessionCount()
        val overallAccuracy = studySessionDao.getOverallAccuracy() ?: 0f
        val dominadoCount = userMasteryDao.getMasteryByState(DomainState.DOMINADO).first().size
        val totalMastery = userMasteryDao.getMasteryCount()
        val brechaCount = userMasteryDao.observeAffectedSubtemaCount().first()

        return OverallStats(
            totalSesiones = sessionCount,
            precisionGeneral = overallAccuracy,
            subtemasDominados = dominadoCount,
            totalSubtemas = totalMastery,
            brechasActivas = brechaCount
        )
    }
}
