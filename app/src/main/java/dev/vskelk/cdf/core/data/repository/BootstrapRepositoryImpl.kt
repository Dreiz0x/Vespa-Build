package dev.vskelk.cdf.core.data.repository

import android.content.Context
import dev.vskelk.cdf.core.database.dao.*
import dev.vskelk.cdf.core.database.entity.*
import dev.vskelk.cdf.core.domain.model.*
import dev.vskelk.cdf.core.domain.repository.*
import dev.vskelk.cdf.core.datastore.PreferencesDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BootstrapRepositoryImpl @Inject constructor(
    private val context: Context,
    private val ontologyDao: OntologyDao,
    private val normativeDao: NormativeDao,
    private val reactivoDao: ReactivoDao,
    private val preferencesDataSource: PreferencesDataSource
) : BootstrapRepository {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    override suspend fun initialize() {
        bootstrapState.first()
    }

    override val bootstrapState: Flow<BootstrapState> = flow {
        emit(BootstrapState.Checking)
        try {
            val manifest = getManifestJson()
            val currentVersion = preferencesDataSource.seedVersionApplied.first()
            if (currentVersion != manifest.version || !meetsMinimumCounts(manifest)) {
                emit(BootstrapState.Seeding("Iniciando carga de datos...", 0f))
                loadSeedData { message, progress -> emit(BootstrapState.Seeding(message, progress)) }
                preferencesDataSource.setSeedVersionApplied(manifest.version)
                emit(BootstrapState.Ready)
            } else {
                emit(BootstrapState.Ready)
            }
        } catch (e: Exception) {
            emit(BootstrapState.Error(e.message ?: "Error desconocido", canRetry = true))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun loadSeedData(onProgress: suspend (String, Float) -> Unit) = withContext(Dispatchers.IO) {
        onProgress("Cargando fuentes...", 0.05f)
        val sources = loadNormativaSources()
        sources.forEach { normativeDao.insertSource(DocumentSourceEntity(id = it.id, nombreCompleto = it.name, abreviatura = it.code, tipo = it.type, isOficial = true, createdAt = it.active_since)) }

        onProgress("Cargando fragmentos...", 0.15f)
        val fragments = loadNormativaFragments()
        fragments.forEach { frag ->
            val src = sources.find { it.id == frag.source_id }
            normativeDao.insertFragment(NormativeFragmentEntity(id = frag.id, content = frag.content, source = src?.code ?: "UNKNOWN", articleRef = frag.article_ref, sourceType = src?.type ?: "LEY", certainty = ExtractionCertainty.ALTA, areaExamen = "TECNICO", versionId = frag.version_id, vigenciaDesde = frag.vigencia_desde, vigenciaHasta = frag.vigencia_hasta, status = frag.status, reemplazadoPorId = frag.replaced_by_id, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        }

        onProgress("Cargando ontología...", 0.4f)
        val nodes = loadOntologiaNodes()
        ontologyDao.insertCargo(CargoEntity(id = 1, nombre = "VOE", descripcion = "Cargo", areaExamen = "TECNICO", nivel = "Vocalía", isDefault = true))
        
        nodes.forEachIndexed { i, node ->
            ontologyDao.insertNode(OntologyNodeEntity(id = node.id, nodeType = node.node_type.uppercase(), name = node.label, description = node.description, parentId = node.parent_id, weight = node.confidence.toFloat(), displayOrder = i, isActive = node.is_active, updatedAt = System.currentTimeMillis()))
        }

        onProgress("Cargando reactivos...", 0.8f)
        val reactivos = loadReactivos()
        reactivos.forEach { reactivo ->
            reactivoDao.insertReactivo(ReactivoEntity(id = reactivo.id, enunciado = reactivo.enunciado, modulo = reactivo.modulo, examArea = reactivo.exam_area, temaId = reactivo.tema_id, subtemaId = reactivo.subtema_id ?: reactivo.tema_id, ontologyNodeId = reactivo.subtema_id, tipoReactivo = reactivo.tipo_reactivo, nivelCognitivo = reactivo.nivel_cognitivo, dificultad = reactivo.dificultad.toFloat(), patronErrorId = reactivo.patron_error_id, citaTextual = reactivo.cita_textual, vigenciaDesde = reactivo.vigencia_desde, vigenciaHasta = reactivo.vigencia_hasta, origen = reactivo.origen, status = reactivo.status, invalidationReason = reactivo.invalidation_reason, createdAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
        }
        onProgress("Carga completada", 1.0f)
    }

    private inline fun <reified T> parseJsonFile(filename: String): T {
        val jsonText = context.assets.open("seed/$filename").bufferedReader().use { it.readText() }
        return jsonParser.decodeFromString<T>(jsonText)
    }

    private fun getManifestJson(): SeedManifest = parseJsonFile("seed_manifest.json")
    private fun loadNormativaSources(): List<SeedNormativaSource> = try { parseJsonFile("normativa_sources.json") } catch (e: Exception) { emptyList() }
    private fun loadNormativaFragments(): List<SeedNormativaFragment> = try { parseJsonFile("normativa_fragments.json") } catch (e: Exception) { emptyList() }
    private fun loadOntologiaNodes(): List<SeedOntologiaNode> = try { parseJsonFile("ontologia.json") } catch (e: Exception) { emptyList() }
    private fun loadOntologiaEdges(): List<SeedOntologiaEdge> = try { parseJsonFile("ontologia_edges.json") } catch (e: Exception) { emptyList() }
    private fun loadReactivos(): List<SeedReactivo> = try { parseJsonFile("reactivos.json") } catch (e: Exception) { emptyList() }

    override suspend fun needsSeeding(): Boolean {
        val manifest = getManifestJson()
        return preferencesDataSource.seedVersionApplied.first() != manifest.version || !meetsMinimumCounts(manifest)
    }

    override suspend fun getSeedVersion(): String? = preferencesDataSource.seedVersionApplied.first().takeIf { it.isNotEmpty() }
    override suspend fun getManifest(): String = context.assets.open("seed/seed_manifest.json").bufferedReader().use { it.readText() }
    
    private suspend fun meetsMinimumCounts(manifest: SeedManifest): Boolean {
        return reactivoDao.getActiveReactivoCount() >= manifest.minReactivos && normativeDao.getVigenteFragmentCount() >= manifest.minNormativa && ontologyDao.getActiveNodeCount() >= manifest.minOntologia
    }
}
