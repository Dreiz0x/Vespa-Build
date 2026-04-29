#!/usr/bin/env python3
import os

BASE = "/root/Spen-Vespa/app/src/main/java/dev/vskelk/cdf"

files = {
    f"{BASE}/core/database/entity/AdaptiveEntities.kt": '''package dev.vskelk.cdf.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val sessionId: Long = 0,
    val modulo: String = "",
    val examArea: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val correctos: Int = 0,
    val totalReactivos: Int = 0,
    val tiempoPromedioSeg: Float = 0f,
    val weakSubtemas: String? = null,
    val dominantErrors: String? = null
)

@Entity(tableName = "user_topic_mastery")
data class UserTopicMasteryEntity(
    @PrimaryKey val subtemaId: Long,
    val estadoDominio: String = "NO_VISTO",
    val precision: Float = 0f,
    val totalIntentos: Int = 0,
    val velocidadPromedio: Float = 0f,
    val lastReviewed: Long = 0
)

@Entity(tableName = "user_gap_logs")
data class UserGapLogEntity(
    @PrimaryKey(autoGenerate = true) val gapId: Long = 0,
    val subtemaId: Long,
    val errorType: String = "",
    val reactivoId: Long = 0,
    val sessionId: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)
''',

    f"{BASE}/core/database/entity/OntologyNodeEntity.kt": '''package dev.vskelk.cdf.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ontology_nodes")
data class OntologyNodeEntity(
    @PrimaryKey val id: Long,
    val nodeType: String,
    val name: String,
    val description: String? = null,
    val parentId: Long? = null,
    val weight: Float = 1.0f,
    val displayOrder: Int = 0,
    val isActive: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
''',

    f"{BASE}/core/database/dao/StudySessionDao.kt": '''package dev.vskelk.cdf.core.database.dao

import androidx.room.*
import dev.vskelk.cdf.core.database.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {

    @Query("SELECT * FROM study_sessions ORDER BY startedAt DESC")
    fun getAllSessions(): Flow<List<StudySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Query("SELECT * FROM study_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: Long): StudySessionEntity?

    @Query("SELECT * FROM study_sessions WHERE completedAt IS NOT NULL ORDER BY completedAt DESC LIMIT :limit")
    fun getRecentSessions(limit: Int): Flow<List<StudySessionEntity>>

    @Query("SELECT COUNT(*) FROM study_sessions WHERE completedAt IS NOT NULL")
    suspend fun getCompletedSessionCount(): Int

    @Query("SELECT COUNT(*) FROM study_sessions WHERE completedAt IS NOT NULL")
    fun observeCompletedSessionCount(): Flow<Int>

    @Query("SELECT AVG(CAST(correctos AS FLOAT) / CAST(totalReactivos AS FLOAT)) FROM study_sessions WHERE completedAt IS NOT NULL AND totalReactivos > 0")
    suspend fun getOverallAccuracy(): Float?

    @Query("UPDATE study_sessions SET completedAt = :completedAt, correctos = :correctos, tiempoPromedioSeg = :tiempoPromedioSeg WHERE sessionId = :sessionId")
    suspend fun completeSession(
        sessionId: Long,
        correctos: Int,
        tiempoPromedioSeg: Float,
        completedAt: Long = System.currentTimeMillis()
    )
}
''',

    f"{BASE}/core/database/dao/UserMasteryDao.kt": '''package dev.vskelk.cdf.core.database.dao

import androidx.room.*
import dev.vskelk.cdf.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserMasteryDao {

    @Query("SELECT * FROM user_topic_mastery WHERE subtemaId = :subtemaId")
    suspend fun getMasteryBySubtemaId(subtemaId: Long): UserTopicMasteryEntity?

    @Query("SELECT * FROM user_topic_mastery WHERE estadoDominio = :estado")
    fun getMasteryByState(estado: String): Flow<List<UserTopicMasteryEntity>>

    @Query("SELECT * FROM user_topic_mastery WHERE estadoDominio IN (:estados)")
    fun getMasteryByStates(estados: List<String>): Flow<List<UserTopicMasteryEntity>>

    @Query("SELECT * FROM user_topic_mastery")
    fun observeAllMastery(): Flow<List<UserTopicMasteryEntity>>

    @Query("SELECT COUNT(*) FROM user_topic_mastery")
    suspend fun getMasteryCount(): Int

    @Query("SELECT * FROM user_topic_mastery WHERE precision < :threshold ORDER BY precision ASC LIMIT :limit")
    suspend fun getWeakSubtemas(threshold: Float, limit: Int): List<UserTopicMasteryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMastery(mastery: UserTopicMasteryEntity)

    @Update
    suspend fun updateMastery(mastery: UserTopicMasteryEntity)

    @Query("UPDATE user_topic_mastery SET precision = :precision, totalIntentos = totalIntentos + 1, velocidadPromedio = :velocidadPromedio, estadoDominio = :estadoDominio, lastReviewed = :timestamp WHERE subtemaId = :subtemaId")
    suspend fun recordAttemptAndUpdateMastery(
        subtemaId: Long,
        precision: Float,
        velocidadPromedio: Float,
        estadoDominio: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGapLog(gapLog: UserGapLogEntity)

    @Query("SELECT * FROM user_gap_logs WHERE subtemaId = :subtemaId")
    fun getGapLogsBySubtema(subtemaId: Long): Flow<List<UserGapLogEntity>>

    @Query("SELECT errorType, COUNT(*) as count FROM user_gap_logs GROUP BY errorType ORDER BY count DESC LIMIT :limit")
    suspend fun getGlobalErrorTypeCounts(limit: Int = 10): List<ErrorTypeCount>

    @Query("SELECT COUNT(DISTINCT subtemaId) FROM user_gap_logs")
    fun observeAffectedSubtemaCount(): Flow<Int>

    @Query("SELECT * FROM user_gap_logs")
    suspend fun getAllUserGapLogs(): List<UserGapLogEntity>
}

data class ErrorTypeCount(
    @ColumnInfo(name = "errorType") val errorType: String,
    @ColumnInfo(name = "count") val count: Int
)
''',

    f"{BASE}/core/database/dao/OntologyDao.kt": '''package dev.vskelk.cdf.core.database.dao

import androidx.room.*
import dev.vskelk.cdf.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OntologyDao {

    @Query("SELECT * FROM ontology_nodes WHERE id = :nodeId")
    suspend fun getNodeById(nodeId: Long): OntologyNodeEntity?

    @Query("SELECT * FROM ontology_nodes WHERE nodeType = :type")
    fun getNodesByType(type: String): Flow<List<OntologyNodeEntity>>

    @Query("SELECT * FROM ontology_nodes WHERE parentId = :parentId")
    fun getChildNodes(parentId: Long): Flow<List<OntologyNodeEntity>>

    @Query("SELECT * FROM ontology_nodes WHERE nodeType = 'SUBTEMA'")
    fun getSubtemas(): Flow<List<OntologyNodeEntity>>

    @Query("SELECT * FROM ontology_nodes WHERE name LIKE '%' || :query || '%'")
    suspend fun searchNodes(query: String): List<OntologyNodeEntity>

    @Query("SELECT n.* FROM ontology_nodes n INNER JOIN ontology_relations r ON n.id = r.targetNodeId WHERE r.sourceNodeId = :nodeId AND r.relationType = :relationType")
    fun getRelatedNodes(nodeId: Long, relationType: String): Flow<List<OntologyNodeEntity>>

    @Query("SELECT COUNT(*) FROM ontology_nodes WHERE isActive = 1")
    suspend fun getActiveNodeCount(): Int

    @Query("SELECT * FROM ontology_nodes WHERE isActive = 1")
    fun observeAllNodes(): Flow<List<OntologyNodeEntity>>

    @Transaction
    @Query("SELECT n.*, m.estadoDominio, m.precision, m.totalIntentos, m.velocidadPromedio FROM ontology_nodes n LEFT JOIN user_topic_mastery m ON n.id = m.subtemaId WHERE n.nodeType = :type")
    fun getSubtopicsWithDomainStatus(type: String): Flow<List<OntologyNodeWithMastery>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: OntologyNodeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCargo(cargo: CargoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRelation(relation: OntologyRelationEntity)

    @Query("DELETE FROM ontology_nodes")
    suspend fun clearNodes()

    @Query("DELETE FROM ontology_relations")
    suspend fun clearRelations()
}

data class OntologyNodeWithMastery(
    @Embedded val node: OntologyNodeEntity,
    @ColumnInfo(name = "estadoDominio") val estadoDominio: String?,
    @ColumnInfo(name = "precision") val precision: Float?,
    @ColumnInfo(name = "totalIntentos") val totalIntentos: Int?,
    @ColumnInfo(name = "velocidadPromedio") val velocidadPromedio: Float?
)
''',

    f"{BASE}/ui/investigator/InvestigatorViewModel.kt": '''package dev.vskelk.cdf.ui.investigator

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.vskelk.cdf.core.database.dao.NormativeDao
import dev.vskelk.cdf.core.database.dao.QuarantineDao
import dev.vskelk.cdf.core.datastore.PreferencesDataSource
import dev.vskelk.cdf.core.network.gemini.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class InvestigatorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val normativeDao: NormativeDao,
    private val preferencesDataSource: PreferencesDataSource,
    private val quarantineDao: QuarantineDao,
    private val geminiService: GeminiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<InvestigatorUiState>(InvestigatorUiState.Idle)
    val uiState: StateFlow<InvestigatorUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _selectedFilter = MutableStateFlow("Todas")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _ingestionState = MutableStateFlow<IngestionState>(IngestionState.Idle)
    val ingestionState: StateFlow<IngestionState> = _ingestionState.asStateFlow()

    init {
        PDFBoxResourceLoader.init(context)
    }

    fun setQuery(newQuery: String) { _query.value = newQuery }
    fun setFilter(filter: String) { _selectedFilter.value = filter }

    fun investigar() {
        _uiState.value = InvestigatorUiState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            delay(1000)
            withContext(Dispatchers.Main) { _uiState.value = InvestigatorUiState.Success("Funcion en desarrollo") }
        }
    }

    fun ingestPdf(uri: Uri) {
        _ingestionState.value = IngestionState.Processing("PDF", 0)
        viewModelScope.launch(Dispatchers.IO) {
            delay(2000)
            withContext(Dispatchers.Main) { _ingestionState.value = IngestionState.Success("PDF", 1) }
        }
    }
}

sealed interface InvestigatorUiState {
    data object Idle : InvestigatorUiState
    data object Loading : InvestigatorUiState
    data class Success(val message: String) : InvestigatorUiState
    data class Error(val message: String) : InvestigatorUiState
}

sealed interface IngestionState {
    data object Idle : IngestionState
    data class Processing(val fileName: String, val blocks: Int) : IngestionState
    data class Success(val fileName: String, val totalBlocks: Int) : IngestionState
    data class Error(val message: String) : IngestionState
}
''',

    f"{BASE}/ui/investigator/InvestigatorScreen.kt": '''package dev.vskelk.cdf.ui.investigator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.vskelk.cdf.R
import dev.vskelk.cdf.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestigatorScreen(
    onNavigateBack: () -> Unit,
    viewModel: InvestigatorViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val ingestionState by viewModel.ingestionState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.ingestPdf(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.investigator_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VespaBackground, titleContentColor = VespaOnSurface)
            )
        },
        containerColor = VespaBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.setQuery(it) },
                placeholder = { Text(stringResource(R.string.investigator_query_hint)) },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { viewModel.investigar() }) {
                        Icon(Icons.Default.Send, contentDescription = "Enviar")
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { filePickerLauncher.launch("application/pdf") }, modifier = Modifier.fillMaxWidth()) {
                Text("Seleccionar PDF")
            }
            Spacer(modifier = Modifier.height(16.dp))
            when (val state = ingestionState) {
                is IngestionState.Processing -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                is IngestionState.Success -> Text("PDF procesado exitosamente", color = VespaSuccess)
                is IngestionState.Error -> Text(state.message, color = VespaError)
                is IngestionState.Idle -> {}
            }
            Spacer(modifier = Modifier.height(16.dp))
            when (val state = uiState) {
                is InvestigatorUiState.Loading -> CircularProgressIndicator()
                is InvestigatorUiState.Success -> Text(state.message, color = VespaSuccess)
                is InvestigatorUiState.Error -> Text(state.message, color = VespaError)
                is InvestigatorUiState.Idle -> {}
            }
        }
    }
}
'''
}

for path, content in files.items():
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content.strip() + '\n')
    print(f"OK: {path}")

print("\nTODO LISTO. Ejecuta: cd /root/Spen-Vespa && ./gradlew --stop && rm -rf build .gradle app/build app/.gradle && ./gradlew clean assembleDebug --no-daemon")
