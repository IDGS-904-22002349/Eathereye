package com.optativesolutions.eathereye

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estado de la UI para la pantalla de configuración
data class SettingsUiState(
    val vocThresholds: Map<String, Float> = mapOf( // Umbrales por nombre de VOC
        "Acetona" to 10f,
        "Alcohol Isopropílico" to 8f
),
    val areNotificationsEnabled: Boolean = true,
    val isAlarmSoundOn: Boolean = false
)

class SettingsViewModel(private val settingsManager: SettingsManager) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    val userPreferencesFlow = settingsManager.settingsFlow

    fun onNotificationsToggle(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setNotificationsEnabled(isEnabled)
        }
        _uiState.update { it.copy(areNotificationsEnabled = isEnabled) }
    }

    fun onAlarmSoundToggle(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setAlarmSoundOn(isEnabled)
        }
        _uiState.update { it.copy(isAlarmSoundOn = isEnabled) }
    }

    fun onVocThresholdChange(vocName: String, newValue: Float) {
        _uiState.update { currentState ->
            val newThresholds = currentState.vocThresholds.toMutableMap()
            newThresholds[vocName] = newValue
            currentState.copy(vocThresholds = newThresholds)
        }
    }
}