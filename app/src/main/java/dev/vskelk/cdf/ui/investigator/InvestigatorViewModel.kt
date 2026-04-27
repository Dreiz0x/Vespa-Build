package dev.vskelk.cdf.ui.investigator

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.vskelk.cdf.core.database.dao.NormativeDao
import dev.vskelk.cdf.core.database.entity.ExtractionCertainty
import dev.vskelk.cdf.core.database.entity.NormativeFragmentEntity
import dev.vskelk.cdf.core.database.entity.SourceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class IngestionState {
    object Idle : IngestionState()
    data class Processing(val fileName: String, val blocks: Int = 0) : IngestionState()
    data class Success(val fileName: String, val totalBlocks: Int) : IngestionState()
    data class Error(val message: String) : IngestionState()
}

@HiltViewModel
class InvestigatorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val normativeDao: NormativeDao
) : ViewModel() {

    private val _ingestionState = MutableStateFlow<IngestionState>(IngestionState.Idle)
    val ingestionState: StateFlow<IngestionState> = _ingestionState.asStateFlow()

    private val _selectedFilter = MutableStateFlow("Todas")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    init {
        PDFBoxResourceLoader.init(context)
    }

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun ingestPdf(uri: Uri) {
        val fileName = uri.lastPathSegment ?: "Archivo PDF"
        _ingestionState.value = IngestionState.Processing(fileName)
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val fullText = stripper.getText(document)
                document.close()

                val blocks = chunkAndSave(fullText, fileName)
                _ingestionState.value = IngestionState.Success(fileName, blocks)
            } catch (e: Exception) {
                _ingestionState.value = IngestionState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    private suspend fun chunkAndSave(text: String, source: String): Int {
        val chunkSize = 4000
        val overlap = 400
        var start = 0
        var count = 0
        
        while (start < text.length) {
            val end = (start + chunkSize).coerceAtMost(text.length)
            val chunk = text.substring(start, end)
            
            val fragment = NormativeFragmentEntity(
                source = source,
                content = chunk,
                articleRef = "Local Ingestion",
                status = "VIGENTE",
                sourceType = SourceType.LEY,
                certainty = ExtractionCertainty.BAJA,
                areaExamen = "GENERAL",
                vigenciaDesde = System.currentTimeMillis()
            )
            normativeDao.insertFragment(fragment)
            count++
            
            if (end == text.length) break
            start += (chunkSize - overlap)
            
            if (_ingestionState.value is IngestionState.Processing) {
                _ingestionState.value = IngestionState.Processing(source, count)
            }
        }
        return count
    }
}
