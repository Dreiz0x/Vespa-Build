package dev.vskelk.cdf.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.vskelk.cdf.core.domain.model.BootstrapState
import dev.vskelk.cdf.core.domain.repository.AdaptiveRepository
import dev.vskelk.cdf.core.domain.repository.BootstrapRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Data Classes de Estado - Deben estar aquí o en un archivo visible
 */
data class SessionSummary(
    val id: Long,
    val correctos: Int,
    val total: Int,
    val modulo: String
)

data class MainUiState(
    val isLoading: Boolean = true,
    val progresoGeneral: Int = 0,
    val brechasDetectadas: Int = 0,
    val sesionesCompletadas: Int = 0,
    val subtemasDominados: Int = 0,
    val corpusVersion: String = "2.0",
    val pendientesInvestigador: Int = 0,
    val recientes: List<SessionSummary> = emptyList()
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val adaptiveRepository: AdaptiveRepository,
    private val bootstrapRepository: BootstrapRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkBootstrap()
        loadStats()
    }

    private fun checkBootstrap() {
        viewModelScope.launch {
            bootstrapRepository.bootstrapState.collect { state ->
                when (state) {
                    is BootstrapState.Seeding -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is BootstrapState.Ready -> {
                        _uiState.update { it.copy(isLoading = false) }
                        loadStats()
                    }
                    is BootstrapState.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            try {
                val stats = adaptiveRepository.getOverallStats()
                _uiState.update {
                    it.copy(
                        progresoGeneral = (stats.precisionGeneral * 100).toInt(),
                        brechasDetectadas = stats.brechasActivas,
                        sesionesCompletadas = stats.totalSesiones,
                        subtemasDominados = stats.subtemasDominados,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }

        viewModelScope.launch {
            adaptiveRepository.observeRecentSessions(5).collect { sessions ->
                _uiState.update { 
                    it.copy(recientes = sessions.map { s -> 
                        SessionSummary(s.id, s.correctos, s.totalReactivos, s.modulo) 
                    }) 
                }
            }
        }
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadStats()
    }
}
