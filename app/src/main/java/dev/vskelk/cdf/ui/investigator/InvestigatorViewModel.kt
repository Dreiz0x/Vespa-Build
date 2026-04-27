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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvestigatorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val normativeDao: NormativeDao
) : ViewModel() {

    init {
        PDFBoxResourceLoader.init(context)
    }

    fun ingestPdf(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val document = PDDocument.load(inputStream)
                val stripper = PDFTextStripper()
                val fullText = stripper.getText(document)
                document.close()

                chunkAndSave(fullText, uri.lastPathSegment ?: "unknown_source")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun chunkAndSave(text: String, source: String) {
        val chunkSize = 4000
        val overlap = 400
        var start = 0
        
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
            
            if (end == text.length) break
            start += (chunkSize - overlap)
        }
    }
}
