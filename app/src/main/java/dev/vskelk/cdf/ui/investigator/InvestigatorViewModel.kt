package dev.vskelk.cdf.ui.investigator

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
