package dev.vskelk.cdf.ui.quarantine

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class CuarentenaViewModel @Inject constructor() : ViewModel() {
    private val _estado = MutableStateFlow("Idle")
    val estado: StateFlow<String> = _estado
}
