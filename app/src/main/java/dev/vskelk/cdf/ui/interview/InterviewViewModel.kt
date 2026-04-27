import kotlinx.coroutines.flow.StateFlow

class InterviewViewModel {
    private val _estado = MutableStateFlow(InvestigacionEstado.Idle)
    val estado: StateFlow<InvestigacionEstado> = _estado
}
