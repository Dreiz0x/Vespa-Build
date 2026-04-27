package dev.vskelk.cdf.ui.investigator

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.vskelk.cdf.core.database.dao.NormativeDao
import dev.vskelk.cdf.core.database.dao.QuarantineDao
import dev.vskelk.cdf.core.database.entity.ExtractionCertainty
import dev.vskelk.cdf.core.database.entity.NormativeFragmentEntity
import dev.vskelk.cdf.core.database.entity.QuarantineEntity
import dev.vskelk.cdf.core.database.entity.SourceType
import dev.vskelk.cdf.core.datastore.PreferencesDataSource
import dev.vskelk.cdf.core.domain.model.InvestigacionEstado
import dev.vskelk.cdf.core.domain.model.InvestigacionResult
import dev.vskelk.cdf.core.network.gemini.GeminiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import javax.inject.Inject

@HiltViewModel
class InvestigatorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val normativeDao: NormativeDao,
    private val preferencesDataSource: PreferencesDataSource,
    private val quarantineDao: QuarantineDao,
    private val geminiService: GeminiService
) : ViewModel() {

    private val _investigationState = MutableStateFlow<InvestigacionEstado>(InvestigacionEstado.Idle)
    val investigationState: StateFlow<InvestigacionEstado> = _investigationState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    init {
        PDFBoxResourceLoader.init(context)
    }

    fun investigar(prompt: String, pdfBytes: ByteArray?) {
        _investigationState.value = InvestigacionEstado.Acotando("Iniciando investigación...")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = preferencesDataSource.getApiKey("GEMINI") ?: return@launch
                val result = geminiService.generateStructuredJson(apiKey, "$prompt\n\n${_query.value}", pdfBytes)

                result.onSuccess { jsonString ->
                    withContext(Dispatchers.Main) {
                        val json = JSONObject(jsonString)
                        val confidence = json.optDouble("confidence_score", 0.0).toFloat()
                        val area = json.optString("spen_area", "SIN_CLASIFICAR")

                        if (confidence < 0.85f) {
                            quarantineDao.insertQuarantine(QuarantineEntity(
                                rawContent = jsonString, confidence = confidence,
                                spenArea = area, status = "PENDING_VALIDATION",
                                timestamp = System.currentTimeMillis()
                            ))
                            _investigationState.value = InvestigacionEstado.Validando("Baja confianza: $confidence")
                        } else {
                            normativeDao.insertFragment(NormativeFragmentEntity(
                                source = "Gemini", content = jsonString, articleRef = "Investigación",
                                status = "VIGENTE", sourceType = SourceType.LEY,
                                certainty = ExtractionCertainty.ALTA.name, areaExamen = area,
                                vigenciaDesde = System.currentTimeMillis()
                            ))
                            _investigationState.value = InvestigacionEstado.Completado(InvestigacionResult(jsonString, confidence))
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _investigationState.value = InvestigacionEstado.Error(e.message ?: "Error desconocido")
                }
            }
        }
    }
}
