package com.optativesolutions.eathereye

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// Estado de la UI para la pantalla de sensores/extracción
data class SensorsUiState(
    val isSystemActive: Boolean = false
)

class SensorsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SensorsUiState())
    val uiState = _uiState.asStateFlow()

    fun toggleExtractionSystem() {
        _uiState.update { currentState ->
            currentState.copy(isSystemActive = !currentState.isSystemActive)
        }
    }
}