package com.optativesolutions.eathereye

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Estado de la UI para la pantalla de configuración
data class SettingsUiState(
    val vocThresholds: Map<String, Float> = emptyMap(),
    val areNotificationsEnabled: Boolean = true,
    val isAlarmSoundOn: Boolean = false
)

class SettingsViewModel(private val settingsManager: SettingsManager) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // -> NUEVO: Observamos las preferencias guardadas y actualizamos la UI en cuanto cargan.
        //    Esto asegura que el slider muestre el valor guardado al abrir la pantalla.
        settingsManager.settingsFlow.onEach { userPreferences ->
            _uiState.update {
                it.copy(
                    areNotificationsEnabled = userPreferences.areNotificationsEnabled,
                    isAlarmSoundOn = userPreferences.isAlarmSoundOn,
                    vocThresholds = userPreferences.vocThresholds
                )
            }
        }.launchIn(viewModelScope)
    }

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

    fun onVocThresholdChange(vocKey: String, newValue: Float) {
        _uiState.update { currentState ->
            val newThresholds = currentState.vocThresholds.toMutableMap()
            newThresholds[vocKey] = newValue
            currentState.copy(vocThresholds = newThresholds)
        }
        viewModelScope.launch {
            settingsManager.setVocThreshold(vocKey, newValue)
        }
    }
}